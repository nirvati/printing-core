/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2015 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.core.services.impl;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.time.DateUtils;
import org.savapage.core.PerformanceLogger;
import org.savapage.core.SpException;
import org.savapage.core.cometd.AdminPublisher;
import org.savapage.core.cometd.PubLevelEnum;
import org.savapage.core.cometd.PubTopicEnum;
import org.savapage.core.concurrent.ReadWriteLockEnum;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.crypto.CryptoUser;
import org.savapage.core.dao.DaoContext;
import org.savapage.core.dao.helpers.AccountTrxTypeEnum;
import org.savapage.core.dao.helpers.DocLogProtocolEnum;
import org.savapage.core.dao.helpers.PrintInDeniedReasonEnum;
import org.savapage.core.doc.DocContent;
import org.savapage.core.ipp.IppJobStateEnum;
import org.savapage.core.jpa.Account.AccountTypeEnum;
import org.savapage.core.jpa.DocIn;
import org.savapage.core.jpa.DocInOut;
import org.savapage.core.jpa.DocLog;
import org.savapage.core.jpa.DocOut;
import org.savapage.core.jpa.Entity;
import org.savapage.core.jpa.IppQueue;
import org.savapage.core.jpa.PdfOut;
import org.savapage.core.jpa.PrintIn;
import org.savapage.core.jpa.PrintOut;
import org.savapage.core.jpa.Printer;
import org.savapage.core.jpa.User;
import org.savapage.core.jpa.UserAccount;
import org.savapage.core.json.JsonRollingTimeSeries;
import org.savapage.core.json.TimeSeriesInterval;
import org.savapage.core.msg.UserMsgIndicator;
import org.savapage.core.pdf.SpPdfPageProps;
import org.savapage.core.print.proxy.ProxyPrintJobStatusMonitor;
import org.savapage.core.print.proxy.ProxyPrintJobStatusPrintOut;
import org.savapage.core.services.DocLogService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.helpers.AccountTrxInfoSet;
import org.savapage.core.services.helpers.DocContentPrintInInfo;
import org.savapage.core.services.helpers.ExternalSupplierInfo;
import org.savapage.core.util.DateUtil;

/**
 *
 * @author Datraverse B.V.
 *
 */
public final class DocLogServiceImpl extends AbstractService implements
        DocLogService {

    /**
     * Max points for {@link TimeSeriesInterval#DAY}.
     */
    private static final int TIME_SERIES_INTERVAL_DAY_MAX_POINTS = 40;

    /**
     * Max points for {@link TimeSeriesInterval#WEEK}.
     */
    private static final int TIME_SERIES_INTERVAL_WEEK_MAX_POINTS = 5;

    /**
     * Max points for {@link TimeSeriesInterval#MONTH}.
     */
    private static final int TIME_SERIES_INTERVAL_MONTH_MAX_POINTS = 5;

    @Override
    public final String generateSignature(final DocLog docLog) {

        final String message =
                DateUtil.dateAsIso8601(docLog.getCreatedDate())
                        + docLog.getUser().getUserId() + docLog.getTitle()
                        + docLog.getUuid();
        try {
            return CryptoUser.createHmac(message, false);
        } catch (UnsupportedEncodingException e) {
            throw new SpException("generateSignature failed.", e);
        }
    }

    @Override
    public void applyCreationDate(final DocLog docLog, final Date date) {
        docLog.setCreatedDate(date);
        docLog.setCreatedDay(DateUtils.truncate(date, Calendar.DAY_OF_MONTH));
    }

    @Override
    public void logDocOut(final User user, final DocOut docOut) {
        logDocOut(user, docOut, new AccountTrxInfoSet());
    }

    @Override
    public void logDocOut(final User user, final DocOut docOut,
            final AccountTrxInfoSet accountTrxInfoSet) {

        final Date perfStartTime = PerformanceLogger.startTime();

        //
        final PrintOut printOut = docOut.getPrintOut();

        final int printOutPages;

        if (printOut != null) {
            printOutPages =
                    docOut.getDocLog().getNumberOfPages()
                            * printOut.getNumberOfCopies();
        } else {
            printOutPages = 0;
        }

        /*
         * Commit #1: Create DocLog and update User statistics.
         */
        commitDocOutAndStatsUser(user, docOut, accountTrxInfoSet, printOutPages);

        /*
         * AFTER the DocOut is committed we can notify the
         * ProxyPrintJobStatusMonitor (if this is a proxy print).
         */
        if (printOut != null) {

            final IppJobStateEnum jobState =
                    IppJobStateEnum.asEnum(printOut.getCupsJobState());

            final ProxyPrintJobStatusPrintOut jobStatus =
                    new ProxyPrintJobStatusPrintOut(printOut.getPrinter()
                            .getPrinterName(), printOut.getCupsJobId(),
                            printOut.getDocOut().getDocLog().getTitle(),
                            jobState);

            jobStatus.setCupsCreationTime(printOut.getCupsCreationTime());
            jobStatus.setCupsCompletedTime(printOut.getCupsCompletedTime());

            ProxyPrintJobStatusMonitor.notify(jobStatus);
        }

        /*
         * Commit #2: Update Printer statistics.
         */
        if (printOut != null) {
            commitDocOutStatsPrinter(docOut, printOutPages);
        }

        /*
         * Commit #3: Update global statistics.
         */
        commitDocOutStatsGlobal(docOut);

        //
        PerformanceLogger.log(this.getClass(), "logDocOut", perfStartTime,
                user.getUserId());
    }

    /**
     * Commits the create of a {@link DocLog} containing the {@link DocOut)
     * object and statistics update for a locked {@link User}. See Mantis #430.
     *
     * @param user
     *            The {@link User}
     * @param docOut
     *            The {@link DocOut} container.
     * @param accountTrxInfoSet
     *            The {@link AccountTrxInfoSet}. If {@code null} the
     *            {@link AccountTypeEnum#USER} is used for accounting.
     */
    private void commitDocOutAndStatsUser(final User user, final DocOut docOut,
            final AccountTrxInfoSet accountTrxInfoSet, final int printOutPages) {

        final Date perfStartTime = PerformanceLogger.startTime();

        final DocLog docLog = docOut.getDocLog();

        final String actor = Entity.ACTOR_SYSTEM;
        final Date now = docLog.getCreatedDate();

        ServiceContext.setActor(actor);
        ServiceContext.setTransactionDate(now);

        final DaoContext daoContext = ServiceContext.getDaoContext();

        /*
         * We need a transaction.
         */
        if (!daoContext.isTransactionActive()) {
            daoContext.beginTransaction();
        }

        boolean isCommitted = false;

        try {
            /*
             * User LOCK.
             */
            final User lockedUser;

            if (userDAO().isLocked(user)) {
                lockedUser = user;
            } else {
                lockedUser = userDAO().lock(user.getId());
            }

            /*
             * Create DocLog.
             */
            docLogDAO().create(docLog);

            /*
             * Update User totals: PdfOut.
             */
            final PdfOut pdfOut = docOut.getPdfOut();

            if (pdfOut != null) {

                /*
                 * User - totals
                 */
                userService().addPdfOutJobTotals(lockedUser, now,
                        docLog.getNumberOfPages(), docLog.getNumberOfBytes());

                /*
                 * UserAttr - totals
                 */
                userService().logPdfOut(lockedUser, now,
                        docLog.getNumberOfPages(), docLog.getNumberOfBytes());
            }

            /*
             * Update User totals: PrintOut.
             */
            final PrintOut printOut = docOut.getPrintOut();

            if (printOut != null) {

                /*
                 * User - totals
                 */
                userService().addPrintOutJobTotals(lockedUser, now,
                        printOutPages, printOut.getNumberOfSheets(),
                        printOut.getNumberOfEsu(), docLog.getNumberOfBytes());

                /*
                 * UserAttr - totals
                 */
                userService().logPrintOut(lockedUser, now, printOutPages,
                        printOut.getNumberOfSheets(),
                        printOut.getNumberOfEsu(), docLog.getNumberOfBytes());

                /*
                 * Account Transactions.
                 */
                if (docLog.getCostOriginal().compareTo(BigDecimal.ZERO) != 0) {

                    if (accountTrxInfoSet == null) {

                        final UserAccount userAccount =
                                accountingService().lazyGetUserAccount(
                                        lockedUser, AccountTypeEnum.USER);

                        accountingService().createAccountTrx(
                                userAccount.getAccount(), docLog,
                                AccountTrxTypeEnum.PRINT_OUT);

                    } else {

                        accountingService().createAccountTrxs(
                                accountTrxInfoSet, docLog,
                                AccountTrxTypeEnum.PRINT_OUT);
                    }

                }
            }

            ServiceContext.getDaoContext().getUserDao().update(lockedUser);

            //
            daoContext.commit();
            isCommitted = true;

        } finally {
            if (!isCommitted) {
                daoContext.rollback();
            }
        }

        PerformanceLogger.log(this.getClass(), "commitDocOutAndStatsUser",
                perfStartTime, user.getUserId());
    }

    /**
     * Commits the global {@link DocOut} statistics to the database.
     * <p>
     * Note: This method is performed in a critical section, with
     * {@link ReadWriteLockEnum#DOC_OUT_STATS} write lock, and has its own
     * database transaction. See Mantis #430.
     * </p>
     *
     * @param docOut
     *            The {@link DocOut} container.
     */
    private void commitDocOutStatsGlobal(final DocOut docOut) {

        final DocLog docLog = docOut.getDocLog();

        final ConfigManager cm = ConfigManager.instance();

        final String actor = Entity.ACTOR_SYSTEM;
        final Date now = docLog.getCreatedDate();

        ServiceContext.setActor(actor);
        ServiceContext.setTransactionDate(now);

        JsonRollingTimeSeries<Integer> statsPages = null;
        JsonRollingTimeSeries<Long> statsBytes = null;
        JsonRollingTimeSeries<Long> statsEsu = null;

        IConfigProp.Key key = null;

        final DaoContext daoContext = ServiceContext.getDaoContext();

        // ----------------------------
        // Begin Critical Section
        // ----------------------------
        ReadWriteLockEnum.DOC_OUT_STATS.setWriteLock(true);

        daoContext.beginTransaction();

        boolean isCommitted = false;

        try {

            /*
             * Pdf - totals
             */
            final PdfOut pdfOut = docOut.getPdfOut();

            if (pdfOut != null) {

                /*
                 * ConfigProperty - running totals
                 */
                statsPages =
                        new JsonRollingTimeSeries<>(TimeSeriesInterval.DAY,
                                TIME_SERIES_INTERVAL_DAY_MAX_POINTS, 0);
                statsPages.addDataPoint(Key.STATS_PDF_OUT_ROLLING_DAY_PAGES,
                        now, docLog.getNumberOfPages());
                /*
                 *
                 */
                statsPages =
                        new JsonRollingTimeSeries<>(TimeSeriesInterval.WEEK,
                                TIME_SERIES_INTERVAL_WEEK_MAX_POINTS, 0);
                statsPages.addDataPoint(Key.STATS_PDF_OUT_ROLLING_WEEK_PAGES,
                        now, docLog.getNumberOfPages());
                //
                statsBytes =
                        new JsonRollingTimeSeries<>(TimeSeriesInterval.WEEK,
                                TIME_SERIES_INTERVAL_WEEK_MAX_POINTS, 0L);
                statsBytes.addDataPoint(Key.STATS_PDF_OUT_ROLLING_WEEK_BYTES,
                        now, docLog.getNumberOfBytes());
                /*
                 *
                 */
                statsPages =
                        new JsonRollingTimeSeries<>(TimeSeriesInterval.MONTH,
                                TIME_SERIES_INTERVAL_MONTH_MAX_POINTS, 0);
                statsPages.addDataPoint(Key.STATS_PDF_OUT_ROLLING_MONTH_PAGES,
                        now, docLog.getNumberOfPages());
                //
                statsBytes =
                        new JsonRollingTimeSeries<>(TimeSeriesInterval.MONTH,
                                TIME_SERIES_INTERVAL_MONTH_MAX_POINTS, 0L);
                statsBytes.addDataPoint(Key.STATS_PDF_OUT_ROLLING_MONTH_BYTES,
                        now, docLog.getNumberOfBytes());
                /*
                 *
                 */
                key = Key.STATS_TOTAL_PDF_OUT_PAGES;
                cm.updateConfigKey(key,
                        cm.getConfigLong(key) + docLog.getNumberOfPages(),
                        actor);

                key = Key.STATS_TOTAL_PDF_OUT_BYTES;
                cm.updateConfigKey(key,
                        cm.getConfigLong(key) + docLog.getNumberOfBytes(),
                        actor);
            }

            /*
             * Printer - totals
             */
            final PrintOut printOut = docOut.getPrintOut();

            if (printOut != null) {

                final int printOutPages =
                        docLog.getNumberOfPages()
                                * printOut.getNumberOfCopies();
                /*
                 * ConfigProperty - running totals
                 */
                statsPages =
                        new JsonRollingTimeSeries<>(TimeSeriesInterval.DAY,
                                TIME_SERIES_INTERVAL_DAY_MAX_POINTS, 0);
                statsPages.addDataPoint(Key.STATS_PRINT_OUT_ROLLING_DAY_PAGES,
                        now, printOutPages);
                /*
                 *
                 */
                statsPages =
                        new JsonRollingTimeSeries<>(TimeSeriesInterval.WEEK,
                                TIME_SERIES_INTERVAL_WEEK_MAX_POINTS, 0);
                statsPages.addDataPoint(Key.STATS_PRINT_OUT_ROLLING_WEEK_PAGES,
                        now, printOutPages);
                //
                statsPages =
                        new JsonRollingTimeSeries<>(TimeSeriesInterval.WEEK,
                                TIME_SERIES_INTERVAL_WEEK_MAX_POINTS, 0);
                statsPages.addDataPoint(
                        Key.STATS_PRINT_OUT_ROLLING_WEEK_SHEETS, now,
                        printOut.getNumberOfSheets());
                //
                statsEsu =
                        new JsonRollingTimeSeries<>(TimeSeriesInterval.WEEK,
                                TIME_SERIES_INTERVAL_WEEK_MAX_POINTS, 0L);
                statsEsu.addDataPoint(Key.STATS_PRINT_OUT_ROLLING_WEEK_ESU,
                        now, printOut.getNumberOfEsu());
                //
                statsBytes =
                        new JsonRollingTimeSeries<>(TimeSeriesInterval.WEEK,
                                TIME_SERIES_INTERVAL_WEEK_MAX_POINTS, 0L);
                statsBytes.addDataPoint(Key.STATS_PRINT_OUT_ROLLING_WEEK_BYTES,
                        now, docLog.getNumberOfBytes());
                /*
                 *
                 */
                statsPages =
                        new JsonRollingTimeSeries<>(TimeSeriesInterval.MONTH,
                                TIME_SERIES_INTERVAL_MONTH_MAX_POINTS, 0);
                statsPages.addDataPoint(
                        Key.STATS_PRINT_OUT_ROLLING_MONTH_PAGES, now,
                        printOutPages);
                //
                statsPages =
                        new JsonRollingTimeSeries<>(TimeSeriesInterval.MONTH,
                                TIME_SERIES_INTERVAL_MONTH_MAX_POINTS, 0);
                statsPages.addDataPoint(
                        Key.STATS_PRINT_OUT_ROLLING_MONTH_SHEETS, now,
                        printOut.getNumberOfSheets());
                //
                statsEsu =
                        new JsonRollingTimeSeries<>(TimeSeriesInterval.MONTH,
                                TIME_SERIES_INTERVAL_MONTH_MAX_POINTS, 0L);
                statsEsu.addDataPoint(Key.STATS_PRINT_OUT_ROLLING_MONTH_ESU,
                        now, printOut.getNumberOfEsu());
                //
                statsBytes =
                        new JsonRollingTimeSeries<>(TimeSeriesInterval.MONTH,
                                TIME_SERIES_INTERVAL_MONTH_MAX_POINTS, 0L);
                statsBytes.addDataPoint(
                        Key.STATS_PRINT_OUT_ROLLING_MONTH_BYTES, now,
                        docLog.getNumberOfBytes());
                /*
                 *
                 */
                key = Key.STATS_TOTAL_PRINT_OUT_PAGES;
                cm.updateConfigKey(key, cm.getConfigLong(key) + printOutPages,
                        actor);

                key = Key.STATS_TOTAL_PRINT_OUT_SHEETS;
                cm.updateConfigKey(key,
                        cm.getConfigLong(key) + printOut.getNumberOfSheets(),
                        actor);

                key = Key.STATS_TOTAL_PRINT_OUT_ESU;
                cm.updateConfigKey(key,
                        cm.getConfigLong(key) + printOut.getNumberOfEsu(),
                        actor);

                key = Key.STATS_TOTAL_PRINT_OUT_BYTES;
                cm.updateConfigKey(key,
                        cm.getConfigLong(key) + docLog.getNumberOfBytes(),
                        actor);
            }
            //
            daoContext.commit();
            isCommitted = true;

        } finally {
            try {
                if (!isCommitted) {
                    daoContext.rollback();
                }
            } finally {
                ReadWriteLockEnum.DOC_OUT_STATS.setWriteLock(false);
            }
        }
        // ----------------------------
        // End Critical Section
        // ----------------------------
    }

    /**
     * Commits the global PrintIn statistics to the database.
     * <p>
     * Note: This method is performed in a critical section, with
     * {@link ReadWriteLockEnum#DOC_IN_STATS} write lock, and has its own
     * database transaction. See Mantis #483.
     * </p>
     *
     * @param docLog
     *            The {@link DocLog} with the numbers.
     */
    private void commitPrintInStatsGlobal(final DocLog docLog) {

        final DaoContext daoContext = ServiceContext.getDaoContext();

        // ----------------------------
        // Begin Critical Section
        // ----------------------------
        ReadWriteLockEnum.DOC_IN_STATS.setWriteLock(true);

        boolean rollbackTrx = false;

        try {

            /*
             * Transaction.
             */
            daoContext.beginTransaction();
            rollbackTrx = true;

            /*
             * ConfigProperty - running totals
             */
            final Date now = ServiceContext.getTransactionDate();
            final String actor = Entity.ACTOR_SYSTEM;

            IConfigProp.Key key = null;

            JsonRollingTimeSeries<Integer> statsPages = null;
            JsonRollingTimeSeries<Long> statsBytes = null;

            /*
             * .
             */
            statsPages =
                    new JsonRollingTimeSeries<>(TimeSeriesInterval.DAY,
                            TIME_SERIES_INTERVAL_DAY_MAX_POINTS, 0);
            statsPages.addDataPoint(Key.STATS_PRINT_IN_ROLLING_DAY_PAGES, now,
                    docLog.getNumberOfPages());

            /*
             * .
             */
            statsPages =
                    new JsonRollingTimeSeries<>(TimeSeriesInterval.WEEK,
                            TIME_SERIES_INTERVAL_WEEK_MAX_POINTS, 0);
            statsPages.addDataPoint(Key.STATS_PRINT_IN_ROLLING_WEEK_PAGES, now,
                    docLog.getNumberOfPages());
            //
            statsBytes =
                    new JsonRollingTimeSeries<>(TimeSeriesInterval.WEEK,
                            TIME_SERIES_INTERVAL_WEEK_MAX_POINTS, 0L);
            statsBytes.addDataPoint(Key.STATS_PRINT_IN_ROLLING_WEEK_BYTES, now,
                    docLog.getNumberOfBytes());

            /*
             * .
             */
            statsPages =
                    new JsonRollingTimeSeries<>(TimeSeriesInterval.MONTH,
                            TIME_SERIES_INTERVAL_MONTH_MAX_POINTS, 0);
            statsPages.addDataPoint(Key.STATS_PRINT_IN_ROLLING_MONTH_PAGES,
                    now, docLog.getNumberOfPages());
            //
            statsBytes =
                    new JsonRollingTimeSeries<>(TimeSeriesInterval.MONTH,
                            TIME_SERIES_INTERVAL_MONTH_MAX_POINTS, 0L);
            statsBytes.addDataPoint(Key.STATS_PRINT_IN_ROLLING_MONTH_BYTES,
                    now, docLog.getNumberOfBytes());

            /*
             *
             */
            final ConfigManager cm = ConfigManager.instance();

            key = Key.STATS_TOTAL_PRINT_IN_PAGES;
            cm.updateConfigKey(key,
                    cm.getConfigLong(key) + docLog.getNumberOfPages(), actor);
            //
            key = Key.STATS_TOTAL_PRINT_IN_BYTES;
            cm.updateConfigKey(key,
                    cm.getConfigLong(key) + docLog.getNumberOfBytes(), actor);

            /*
             * Commit
             */
            daoContext.commit();
            rollbackTrx = false;

        } finally {

            try {
                if (rollbackTrx) {
                    daoContext.rollback();
                }
            } finally {
                ReadWriteLockEnum.DOC_IN_STATS.setWriteLock(false);
            }
        }
        // ----------------------------
        // End Critical Section
        // ----------------------------
    }

    /**
     * Commits the {@link PrintOut} statistics for a {@link Printer} to the
     * database.
     * <p>
     * Note: This method has its own database transaction with locked
     * {@link Printer}. See Mantis #430.
     * </p>
     *
     * @param docOut
     *            The {@link DocOut} container.
     * @param printOutPages
     *            The number of document pages times the number of copies.
     */
    private void commitDocOutStatsPrinter(final DocOut docOut,
            final int printOutPages) {

        final DocLog docLog = docOut.getDocLog();
        final PrintOut printOut = docOut.getPrintOut();

        final String actor = Entity.ACTOR_SYSTEM;
        final Date now = docLog.getCreatedDate();

        ServiceContext.setActor(actor);
        ServiceContext.setTransactionDate(now);

        final DaoContext daoContext = ServiceContext.getDaoContext();

        boolean rollbackTrx = false;

        try {

            daoContext.beginTransaction();
            rollbackTrx = true;

            /*
             * Printer LOCK.
             */
            final Printer lockedPrinter =
                    printerDAO()
                            .lock(docOut.getPrintOut().getPrinter().getId());

            printerService().addJobTotals(lockedPrinter,
                    docLog.getCreatedDate(), printOutPages,
                    printOut.getNumberOfSheets(), printOut.getNumberOfEsu(),
                    docLog.getNumberOfBytes());

            printerDAO().update(lockedPrinter);

            /*
             * PrinterAttr - totals
             */
            printerService().logPrintOut(lockedPrinter, now, printOutPages,
                    printOut.getNumberOfSheets(), printOut.getNumberOfEsu());

            //
            daoContext.commit();
            rollbackTrx = false;

        } finally {
            if (rollbackTrx) {
                daoContext.rollback();
            }
        }
    }

    /**
     * Commits the PrintIn statistics for a {@link IppQueue} to the database.
     * <p>
     * Note: This method has its own database transaction with locked
     * {@link IppQueue}. See Mantis #483.
     * </p>
     *
     * @param queue
     *            The {@link IppQueue}.
     * @param docLog
     *            The {@link DocLog} with the numbers.
     */
    private void commitPrintInStatsQueue(final IppQueue queue,
            final DocLog docLog) {

        final DaoContext daoContext = ServiceContext.getDaoContext();

        boolean rollbackTrx = false;

        try {

            daoContext.beginTransaction();
            rollbackTrx = true;

            // Queue LOCK.
            final IppQueue ippQueueLocked = ippQueueDAO().lock(queue.getId());

            queueService().addJobTotals(ippQueueLocked,
                    docLog.getCreatedDate(), docLog.getNumberOfPages(),
                    docLog.getNumberOfBytes());

            ippQueueDAO().update(ippQueueLocked);

            queueService().logPrintIn(ippQueueLocked,
                    ServiceContext.getTransactionDate(),
                    docLog.getNumberOfPages());

            //
            daoContext.commit();
            rollbackTrx = false;

        } finally {
            if (rollbackTrx) {
                daoContext.rollback();
            }
        }
    }

    @Override
    public void logPrintIn(final User userDb, final IppQueue queue,
            final DocLogProtocolEnum protocol,
            final DocContentPrintInInfo printInInfo) {

        final Date perfStartTime = PerformanceLogger.startTime();

        final DaoContext daoContext = ServiceContext.getDaoContext();

        final SpPdfPageProps pageProps = printInInfo.getPageProps();

        final boolean isPrinted = pageProps != null;

        PrintInDeniedReasonEnum deniedReason = null;

        if (printInInfo.isDrmViolationDetected()) {
            deniedReason = PrintInDeniedReasonEnum.DRM;
        }

        /*
         * DocLog
         */
        final DocLog docLog = new DocLog();

        this.applyCreationDate(docLog, ServiceContext.getTransactionDate());

        docLog.setMimetype(printInInfo.getMimetype());
        docLog.setDrmRestricted(printInInfo.isDrmRestricted());
        docLog.setNumberOfBytes(printInInfo.getJobBytes());

        if (pageProps != null) {
            docLog.setNumberOfPages(pageProps.getNumberOfPages());
        }

        docLog.setTitle(printInInfo.getJobName());
        docLog.setLogComment(printInInfo.getLogComment());
        docLog.setUser(userDb);
        docLog.setUuid(printInInfo.getUuidJob().toString());
        docLog.setDeliveryProtocol(protocol.getDbName());

        /*
         * External supplier.
         */
        final ExternalSupplierInfo supplierInfo = printInInfo.getSupplierInfo();

        if (supplierInfo != null) {
            docLog.setExternalId(supplierInfo.getId());
            docLog.setExternalStatus(supplierInfo.getStatus());
            docLog.setExternalSupplier(supplierInfo.getSupplier().toString());
            if (supplierInfo.getData() != null) {
                docLog.setExternalData(supplierInfo.getData().dataAsString());
            }
        }

        /*
         * DocIn.
         */
        final String originatorIp = printInInfo.getOriginatorIp();

        final DocIn docIn = new DocIn();
        docIn.setOriginatorIp(originatorIp);

        docIn.setDocLog(docLog);
        docLog.setDocIn(docIn);

        /*
         * Update Global statistics (see Mantis #483).
         */
        if (isPrinted) {
            commitPrintInStatsGlobal(docLog);
        }

        /*
         * Transaction with User lock.
         */
        boolean rollbackTrx = false;

        try {

            daoContext.beginTransaction();
            rollbackTrx = true;

            /*
             * Account transactions?
             */
            final AccountTrxInfoSet accountTrxInfoSet =
                    printInInfo.getAccountTrxInfoSet();

            if (accountTrxInfoSet != null) {
                accountingService().createAccountTrxs(accountTrxInfoSet,
                        docLog, AccountTrxTypeEnum.PRINT_IN);
            }

            /*
             * PrintIn
             */
            final PrintIn printIn = new PrintIn();
            printIn.setQueue(queue);
            printIn.setPrinted(isPrinted);

            if (deniedReason != null) {
                printIn.setDeniedReason(deniedReason.toDbValue());
            }

            printIn.setDocIn(docIn);
            //
            if (pageProps != null) {
                printIn.setPaperHeight(pageProps.getMmHeight());
                printIn.setPaperSize(pageProps.getSize());
                printIn.setPaperWidth(pageProps.getMmWidth());
            }
            //
            docIn.setPrintIn(printIn);

            printInDAO().create(printIn);

            if (isPrinted) {

                /*
                 * User and UserAttr - totals: User LOCK.
                 */
                final User user = userDAO().lock(docLog.getUser().getId());

                userService().addPrintInJobTotals(user,
                        docLog.getCreatedDate(), docLog.getNumberOfPages(),
                        docLog.getNumberOfBytes());

                userDAO().update(user);

                userService().logPrintIn(userDb,
                        ServiceContext.getTransactionDate(),
                        docLog.getNumberOfPages(), docLog.getNumberOfBytes());

            }

            /*
             * Commit
             */
            daoContext.commit();
            rollbackTrx = false;

        } finally {
            if (rollbackTrx) {
                daoContext.rollback();
            }
        }

        /*
         * Transaction with IppQueue lock (Mantis #483).
         */
        if (isPrinted) {
            commitPrintInStatsQueue(queue, docLog);
        }

        /*
         * Notification stuff (to Admin WebApp and User).
         */
        final String userId = userDb.getUserId();

        String originator = originatorIp;

        if (protocol == DocLogProtocolEnum.IMAP
                || protocol == DocLogProtocolEnum.GCP) {
            originator = printInInfo.getOriginatorEmail();
        }

        if (isPrinted) {

            final String msgKey;

            if (pageProps.getNumberOfPages() == 1) {
                msgKey = "pub-user-print-in-success-one";
            } else {
                msgKey = "pub-user-print-in-success-multiple";
            }

            AdminPublisher.instance().publish(
                    PubTopicEnum.USER,
                    PubLevelEnum.INFO,
                    localize(msgKey, userId,
                            String.valueOf(pageProps.getNumberOfPages()), "/"
                                    + queue.getUrlPath(), originator));

        } else {

            final String reasonKey;

            if (deniedReason == PrintInDeniedReasonEnum.DRM) {
                reasonKey = "print-in-denied-drm-restricted";
            } else {
                reasonKey = "print-in-denied-unknown";
            }

            AdminPublisher.instance().publish(
                    PubTopicEnum.USER,
                    PubLevelEnum.WARN,
                    localize("pub-user-print-in-denied", userId,
                            "/" + queue.getUrlPath(), originator,
                            localize(reasonKey)));

            /*
             * Write this message.
             */
            try {
                UserMsgIndicator.write(userId, docLog.getCreatedDate(),
                        UserMsgIndicator.Msg.PRINT_IN_DENIED, null);
            } catch (IOException e) {
                throw new SpException("Error writing user message.", e);
            }
        }

        PerformanceLogger.log(this.getClass(), "logPrintIn", perfStartTime,
                userDb.getUserId());
    }

    @Override
    public void resetPagometers(final String resetBy,
            final boolean resetDashboard, final boolean resetQueues,
            final boolean resetPrinters, final boolean resetUsers) {

        final ConfigManager cm = ConfigManager.instance();

        ReadWriteLockEnum.DOC_OUT_STATS.setWriteLock(true);
        ReadWriteLockEnum.DOC_IN_STATS.setWriteLock(true);

        final Date resetDate = new Date();

        try {

            /*
             * Dashboard
             */
            if (resetDashboard) {

                Key[] series = {
                /* */
                Key.STATS_PRINT_IN_ROLLING_DAY_PAGES,
                /* */
                Key.STATS_PRINT_IN_ROLLING_WEEK_PAGES,
                /* */
                Key.STATS_PRINT_IN_ROLLING_WEEK_BYTES,
                /* */
                Key.STATS_PRINT_IN_ROLLING_MONTH_PAGES,
                /* */
                Key.STATS_PRINT_IN_ROLLING_MONTH_BYTES,

                /* */
                Key.STATS_PDF_OUT_ROLLING_DAY_PAGES,
                /* */
                Key.STATS_PDF_OUT_ROLLING_WEEK_PAGES,
                /* */
                Key.STATS_PDF_OUT_ROLLING_WEEK_BYTES,
                /* */
                Key.STATS_PDF_OUT_ROLLING_MONTH_PAGES,
                /* */
                Key.STATS_PDF_OUT_ROLLING_MONTH_BYTES,

                /* */
                Key.STATS_PRINT_OUT_ROLLING_DAY_PAGES,
                /* */
                Key.STATS_PRINT_OUT_ROLLING_WEEK_PAGES,
                /* */
                Key.STATS_PRINT_OUT_ROLLING_WEEK_SHEETS,
                /* */
                Key.STATS_PRINT_OUT_ROLLING_WEEK_ESU,
                /* */
                Key.STATS_PRINT_OUT_ROLLING_WEEK_BYTES,
                /* */
                Key.STATS_PRINT_OUT_ROLLING_MONTH_PAGES,
                /* */
                Key.STATS_PRINT_OUT_ROLLING_MONTH_SHEETS,
                /* */
                Key.STATS_PRINT_OUT_ROLLING_MONTH_ESU,
                /* */
                Key.STATS_PRINT_OUT_ROLLING_MONTH_BYTES };

                // -----------------------
                Key[] counters = {
                /* */
                Key.STATS_TOTAL_PDF_OUT_PAGES,
                /* */
                Key.STATS_TOTAL_PDF_OUT_BYTES,
                /* */
                Key.STATS_TOTAL_PRINT_IN_PAGES,
                /* */
                Key.STATS_TOTAL_PRINT_IN_BYTES,
                /* */
                Key.STATS_TOTAL_PRINT_OUT_PAGES,
                /* */
                Key.STATS_TOTAL_PRINT_OUT_SHEETS,
                /* */
                Key.STATS_TOTAL_PRINT_OUT_ESU,
                /* */
                Key.STATS_TOTAL_PRINT_OUT_BYTES,

                };

                for (Key key : series) {
                    cm.updateConfigKey(key, "", resetBy);
                }

                for (Key key : counters) {
                    cm.updateConfigKey(key, 0L, resetBy);
                }

                cm.updateConfigKey(Key.STATS_TOTAL_RESET_DATE,
                        resetDate.getTime(), resetBy);
            }

            /*
             * Queues
             */
            if (resetQueues) {
                ippQueueDAO().resetTotals(resetDate, resetBy);
                ippQueueAttrDAO().deleteRollingStats();
            }

            /*
             * Printers
             */
            if (resetPrinters) {
                printerDAO().resetTotals(resetDate, resetBy);
                printerAttrDAO().deleteRollingStats();
            }

            /*
             * Users
             */
            if (resetUsers) {
                userDAO().resetTotals(resetDate, resetBy);
                userAttrDAO().deleteRollingStats();
            }

        } finally {

            ReadWriteLockEnum.DOC_IN_STATS.setWriteLock(false);
            ReadWriteLockEnum.DOC_OUT_STATS.setWriteLock(false);
        }
    }

    @Override
    public void collectData4DocOut(final User user, final DocLog docLogCollect,
            final File pdfFile,
            final LinkedHashMap<String, Integer> uuidPageCount)
            throws IOException {

        this.applyCreationDate(docLogCollect,
                ServiceContext.getTransactionDate());

        docLogCollect.setUser(user);
        docLogCollect.setUuid(java.util.UUID.randomUUID().toString());
        docLogCollect.setMimetype(DocContent.MIMETYPE_PDF);
        docLogCollect.setNumberOfBytes(Files.size(Paths.get(pdfFile
                .getAbsolutePath())));

        //
        final List<DocInOut> inoutList = new ArrayList<>();

        int numberOfPages = 0;

        for (Map.Entry<String, Integer> entry : uuidPageCount.entrySet()) {

            final String uuid = entry.getKey();

            /*
             * INVARIANT: docLogIn is NOT null (see Mantis #268)
             */

            final DocLog docLogIn = docLogDAO().findByUuid(user.getId(), uuid);

            final DocIn docIn = docLogIn.getDocIn();

            final DocInOut docInOut = new DocInOut();

            docInOut.setNumberOfPages(entry.getValue());
            docInOut.setDocIn(docIn);
            docInOut.setDocOut(docLogCollect.getDocOut());

            numberOfPages += docInOut.getNumberOfPages();

            inoutList.add(docInOut);
        }

        docLogCollect.setNumberOfPages(numberOfPages);
        docLogCollect.getDocOut().setDocsInOut(inoutList);

        docLogCollect.getDocOut().setSignature(
                this.generateSignature(docLogCollect));

    }

}
