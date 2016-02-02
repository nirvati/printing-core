/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2016 Datraverse B.V.
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
package org.savapage.ext.papercut;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.savapage.core.concurrent.ReadWriteLockEnum;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.dao.AccountDao;
import org.savapage.core.dao.AccountTrxDao;
import org.savapage.core.dao.DaoContext;
import org.savapage.core.dao.DocInOutDao;
import org.savapage.core.dao.DocLogDao;
import org.savapage.core.dao.UserAccountDao;
import org.savapage.core.dao.enums.DocLogProtocolEnum;
import org.savapage.core.dao.enums.ExternalSupplierEnum;
import org.savapage.core.dao.enums.ExternalSupplierStatusEnum;
import org.savapage.core.jpa.Account.AccountTypeEnum;
import org.savapage.core.jpa.AccountTrx;
import org.savapage.core.jpa.DocIn;
import org.savapage.core.jpa.DocLog;
import org.savapage.core.jpa.DocOut;
import org.savapage.core.jpa.PrintOut;
import org.savapage.core.jpa.User;
import org.savapage.core.services.AccountingService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.util.DateUtil;
import org.savapage.ext.ExtSupplierConnectException;
import org.savapage.ext.ExtSupplierException;
import org.savapage.ext.papercut.services.PaperCutService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rijk Ravestein
 *
 */
public abstract class PaperCutPrintMonitorPattern {

    /**
     * The logger.
     */
    private static final Logger LOGGER = LoggerFactory
            .getLogger(PaperCutPrintMonitorPattern.class);

    /**
     * Days after which a SavaPage print to PaperCut is set to error when the
     * PaperCut print log is not found.
     */
    private static final int MAX_DAYS_WAIT_FOR_PAPERCUT_PRINT_LOG = 5;

    /**
     * .
     */
    private static final AccountingService ACCOUNTING_SERVICE = ServiceContext
            .getServiceFactory().getAccountingService();

    /**
     * .
     */
    private static final PaperCutService PAPERCUT_SERVICE = ServiceContext
            .getServiceFactory().getPaperCutService();

    /**
     * .
     */
    private final PaperCutServerProxy papercutServerProxy;

    /**
     * .
     */
    private final PaperCutDbProxy papercutDbProxy;

    /**
     * .
     */
    private final DocLogDao.ListFilter listFilterPendingExt;

    /**
     * .
     */
    private final PaperCutPrintJobListener statusListener;

    /**
     * @return {@code true} when account transaction candidates are linked with
     *         the {@link DocLog} of the {@link DocIn}, {@code} when linked with
     *         the {@link DocLog} of the {@link DocOut}.
     */
    protected abstract boolean isDocInAccountTrx();

    /**
     * @return PaperCut is configured with Multiple Personal Accounts, and this
     *         is the account name to use for personal transactions.
     */
    protected abstract String getUserAccountName();

    /**
     *
     * @param externalSupplier
     *            The {@link ExternalSupplierEnum}.
     * @param serverProxy
     *            The {@link PaperCutServerProxy}.
     * @param dbProxy
     *            The {@link PaperCutDbProxy}.
     * @param listener
     *            The {@link PaperCutPrintJobListener}.
     */
    protected PaperCutPrintMonitorPattern(
            final ExternalSupplierEnum externalSupplier,
            final PaperCutServerProxy serverProxy,
            final PaperCutDbProxy dbProxy,
            final PaperCutPrintJobListener listener) {

        this.listFilterPendingExt = new DocLogDao.ListFilter();
        this.listFilterPendingExt.setExternalSupplier(externalSupplier);
        this.listFilterPendingExt
                .setExternalStatus(ExternalSupplierStatusEnum.PENDING_EXT
                        .toString());
        this.listFilterPendingExt.setProtocol(DocLogProtocolEnum.IPP);

        this.papercutServerProxy = serverProxy;
        this.papercutDbProxy = dbProxy;

        this.statusListener = listener;
    }

    /**
     * Logs content of a {@link PaperCutPrinterUsageLog} object.
     *
     * @param usageLog
     *            The object to log.
     */
    private static void debugLog(final PaperCutPrinterUsageLog usageLog) {

        if (LOGGER.isDebugEnabled()) {
            final StringBuilder msg = new StringBuilder();
            msg.append(usageLog.getDocumentName()).append(" | printed [")
                    .append(usageLog.isPrinted()).append("] cancelled [")
                    .append(usageLog.isCancelled()).append("] deniedReason [")
                    .append(usageLog.getDeniedReason()).append("] usageCost[")
                    .append(usageLog.getUsageCost()).append("]");
            LOGGER.debug(msg.toString());
        }
    }

    /**
     * Gets the {@link DocLog} from the input or output depending on what is
     * leading for account transactions.
     *
     * @param docLogOut
     *            The DocLog with linked {@link DocOut}.
     * @param docLogIn
     *            The DocLog with linked {@link DocIn}.
     * @return The leading DocLog.
     */
    private DocLog
            getDocLogForTrx(final DocLog docLogOut, final DocLog docLogIn) {
        if (this.isDocInAccountTrx()) {
            return docLogIn;
        } else {
            return docLogOut;
        }
    }

    /**
     * Processes pending jobs printed to a PaperCut managed printer.
     *
     * @throws PaperCutException
     * @throws ExtSupplierException
     * @throws ExtSupplierConnectException
     */
    public final void process() throws PaperCutException, ExtSupplierException,
            ExtSupplierConnectException {

        final DocLogDao doclogDao =
                ServiceContext.getDaoContext().getDocLogDao();

        final Map<String, DocLog> uniquePaperCutDocNames = new HashMap<>();

        for (final DocLog docLog : doclogDao.getListChunk(listFilterPendingExt)) {
            uniquePaperCutDocNames.put(docLog.getTitle(), docLog);
        }

        if (uniquePaperCutDocNames.isEmpty()) {
            return;
        }

        final List<PaperCutPrinterUsageLog> papercutLogList =
                PAPERCUT_SERVICE.getPrinterUsageLog(papercutDbProxy,
                        uniquePaperCutDocNames.keySet());

        final Set<String> paperCutDocNamesHandled = new HashSet<>();

        for (final PaperCutPrinterUsageLog papercutLog : papercutLogList) {

            paperCutDocNamesHandled.add(papercutLog.getDocumentName());

            debugLog(papercutLog);

            final DocLog docLog =
                    uniquePaperCutDocNames.get(papercutLog.getDocumentName());

            final ExternalSupplierStatusEnum printStatus;
            boolean isDocumentTooLarge = false;

            /*
             * Database transaction.
             */
            ReadWriteLockEnum.DATABASE_READONLY.setReadLock(true);

            final DaoContext daoContext = ServiceContext.getDaoContext();

            try {

                if (!daoContext.isTransactionActive()) {
                    daoContext.beginTransaction();
                }

                if (papercutLog.isPrinted()) {

                    printStatus = ExternalSupplierStatusEnum.COMPLETED;

                    this.processPrintJobCompleted(docLog, papercutLog);

                } else {

                    if (papercutLog.getDeniedReason().contains("TIMEOUT")) {

                        printStatus = ExternalSupplierStatusEnum.EXPIRED;

                    } else if (papercutLog.getDeniedReason().contains(
                            "DOCUMENT_TOO_LARGE")) {

                        printStatus = ExternalSupplierStatusEnum.CANCELLED;
                        isDocumentTooLarge = true;

                    } else {
                        printStatus = ExternalSupplierStatusEnum.CANCELLED;
                    }

                    this.processPrintJobCancelled(docLog, papercutLog,
                            printStatus);
                }

                daoContext.commit();

            } finally {

                daoContext.rollback();

                ReadWriteLockEnum.DATABASE_READONLY.setReadLock(false);
            }

            this.statusListener.onPaperCutPrintJobProcessed(docLog,
                    papercutLog, printStatus, isDocumentTooLarge);

        } // end-for

        this.processPrintJobsNotFound(uniquePaperCutDocNames,
                paperCutDocNamesHandled);
    }

    /**
     * Notifies pending SavaPage print jobs that cannot be found in PaperCut:
     * status is set to {@link ExternalSupplierStatusEnum#ERROR} when SavaPage
     * print job is more then {@link #MAX_DAYS_WAIT_FOR_PAPERCUT_PRINT_LOG} old.
     *
     * @param papercutDocNamesToFind
     *            The PaperCut documents to find.
     * @param papercutDocNamesFound
     *            The PaperCut documents found.
     */
    private void processPrintJobsNotFound(
            final Map<String, DocLog> papercutDocNamesToFind,
            final Set<String> papercutDocNamesFound) {

        final DocLogDao doclogDao =
                ServiceContext.getDaoContext().getDocLogDao();

        for (final String docName : papercutDocNamesToFind.keySet()) {

            if (papercutDocNamesFound.contains(docName)) {
                continue;
            }

            final DocLog docLog = papercutDocNamesToFind.get(docName);

            final long docAge =
                    ServiceContext.getTransactionDate().getTime()
                            - docLog.getCreatedDate().getTime();

            if (docAge < MAX_DAYS_WAIT_FOR_PAPERCUT_PRINT_LOG
                    * DateUtil.DURATION_MSEC_DAY) {
                continue;
            }

            /*
             * Database transaction.
             */
            ReadWriteLockEnum.DATABASE_READONLY.setReadLock(true);

            final DaoContext daoContext = ServiceContext.getDaoContext();

            try {

                if (!daoContext.isTransactionActive()) {
                    daoContext.beginTransaction();
                }

                docLog.setExternalStatus(ExternalSupplierStatusEnum.ERROR
                        .toString());

                doclogDao.update(docLog);

                daoContext.commit();

            } finally {

                daoContext.rollback();

                ReadWriteLockEnum.DATABASE_READONLY.setReadLock(false);
            }

            this.statusListener.onPaperCutPrintJobNotFound(docName, docAge);
        }
    }

    /**
     * <p>
     * IMPORTANT: the accumulated weight of the individual Account transactions
     * need NOT be the same as the number of copies (since parts of the printing
     * costs may be charged to multiple accounts).
     * </p>
     *
     * @param docLogOut
     *            The DocLog with linked {@link DocOut}.
     * @param docLogIn
     *            The DocLog with linked {@link DocIn}.
     * @return The weight.
     */
    protected abstract int getAccountTrxWeightTotal(DocLog docLogOut,
            DocLog docLogIn);

    /**
     * @return The {@link Logger}.
     */
    protected abstract Logger getLogger();

    /**
     *
     * @return
     */
    protected abstract String getSharedParentAccountName();

    /**
     *
     * @return
     */
    protected abstract String getSharedJobsAccountName();

    /**
     * Gets the klas (group) name from the SavaPage account name. When the
     * account is "composed", the klas (group) must be parsed.
     * <p>
     * Note: The klas (group) name is needed to compose the comment of a newly
     * created PaperCut account transaction.
     * </p>
     *
     * @param accountName
     *            The (composed) SavaPage account name.
     * @return The klas (group) name.
     */
    protected abstract String getKlasFromAccountName(String accountName);

    /**
     * @param docLogOut
     *            The DocLog with linked {@link DocOut}.
     *
     * @return The single DocLog source (if present and relevant).
     */
    private DocLog getDocLogIn(final DocLog docLogOut) {

        if (this.isDocInAccountTrx()) {
            final DocInOutDao docInOutDao =
                    ServiceContext.getDaoContext().getDocInOutDao();

            return docInOutDao.findDocOutSource(docLogOut.getDocOut().getId());
        }
        return null;
    }

    /**
     * Processes a cancelled PaperCut print job.
     * <ul>
     * <li>The {@link DocLog} target and (conditionally the) source is updated
     * with the {@link ExternalSupplierStatusEnum}.</li>
     * <li>Publish Admin messages.</li>
     * </ul>
     *
     * @param docLogOut
     *            The SavaPage {@link DocLog} target holding the {@link DocOut}
     *            with the {@link PrintOut}.
     * @param papercutLog
     *            The {@link PaperCutPrinterUsageLog}.
     * @param printStatus
     *            The {@link ExternalSupplierStatusEnum}.
     */
    private void processPrintJobCancelled(final DocLog docLogOut,
            final PaperCutPrinterUsageLog papercutLog,
            final ExternalSupplierStatusEnum printStatus) {

        /*
         * Update print status in the DocLog (with linked DocOut).
         */
        final DocLogDao doclogDao =
                ServiceContext.getDaoContext().getDocLogDao();

        docLogOut.setExternalStatus(printStatus.toString());
        doclogDao.update(docLogOut);

        /*
         * Update the single DocLog source (if present and relevant).
         */
        final DocLog docLogIn = this.getDocLogIn(docLogOut);

        if (docLogIn != null) {
            docLogIn.setExternalStatus(printStatus.toString());
            doclogDao.update(docLogIn);
        }
    }

    /**
     * Processes a completed PaperCut print job.
     * <ul>
     * <li>The Personal and Shared [Parent]\[UserGroup] accounts are lazy
     * adjusted in PaperCut and SavaPage.</li>
     * <li>Conditionally the {@link AccountTrx} objects are moved from the
     * {@link DocLog} source to the {@link DocLog} target.</li>
     * <li>The {@link DocLog} target is updated with the
     * {@link ExternalSupplierStatusEnum}.</li>
     * <li>Publish Admin messages.</li>
     * </ul>
     *
     * @param docLogOut
     *            The SavaPage {@link DocLog} target holding the {@link DocOut}
     *            with the {@link PrintOut}.
     * @param papercutLog
     *            The {@link PaperCutPrinterUsageLog}.
     * @throws PaperCutException
     *             When a PaperCut error occurs.
     */

    private void processPrintJobCompleted(final DocLog docLogOut,
            final PaperCutPrinterUsageLog papercutLog) throws PaperCutException {

        final AccountDao accountDao =
                ServiceContext.getDaoContext().getAccountDao();

        final AccountTrxDao accountTrxDao =
                ServiceContext.getDaoContext().getAccountTrxDao();

        final DocLogDao doclogDao =
                ServiceContext.getDaoContext().getDocLogDao();

        /*
         * Get the single DocLog source (if present and relevant).
         */
        final DocLog docLogIn = this.getDocLogIn(docLogOut);

        //
        final DocLog docLogTrx = this.getDocLogForTrx(docLogOut, docLogIn);

        //
        final PrintOut printOutLog = docLogOut.getDocOut().getPrintOut();

        //
        final String indicatorDuplex;

        if (printOutLog.getDuplex().booleanValue()) {
            indicatorDuplex = DelegatedPrintCommentSyntax.INDICATOR_DUPLEX_ON;
        } else {
            indicatorDuplex = DelegatedPrintCommentSyntax.INDICATOR_DUPLEX_OFF;
        }

        //
        final String indicatorColor;

        if (printOutLog.getGrayscale().booleanValue()) {
            indicatorColor = DelegatedPrintCommentSyntax.INDICATOR_COLOR_OFF;
        } else {
            indicatorColor = DelegatedPrintCommentSyntax.INDICATOR_COLOR_ON;
        }

        final String indicatorPaperSize =
                DelegatedPrintCommentSyntax
                        .convertToPaperSizeIndicator(printOutLog.getPaperSize());

        final String indicatorExternalId =
                StringUtils.defaultString(docLogOut.getExternalId(),
                        printOutLog.getCupsJobId().toString());

        /*
         * Any transactions?
         */
        final List<AccountTrx> trxList = docLogTrx.getTransactions();

        if (trxList == null || trxList.isEmpty()) {

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format(
                        "No DocLog transactions found for [%s] [%s]",
                        docLogOut.getTitle(), docLogOut.getId().toString()));
            }

            return;
        }

        /*
         * Get total number of copies from the external data and use as weight
         * total. IMPORTANT: the accumulated weight of the individual Account
         * transactions need NOT be the same as the number of copies (since
         * parts of the printing costs may be charged to multiple accounts).
         */
        final int weightTotal =
                this.getAccountTrxWeightTotal(docLogOut, docLogIn);

        /*
         * Total printing cost reported by PaperCut.
         */
        final BigDecimal papercutUsageCost =
                BigDecimal.valueOf(papercutLog.getUsageCost());

        /*
         * Number of decimals for decimal scaling.
         */
        final int scale = ConfigManager.getFinancialDecimalsInDatabase();

        /*
         * Number of pages in the printed document.
         */
        final Integer numberOfDocumentPages = docLogTrx.getNumberOfPages();

        /*
         * Create transaction comment with job info.
         */
        final String requestingUserId = docLogTrx.getUser().getUserId();

        final StringBuilder jobTrxComment = new StringBuilder();

        // user | copies | pages
        jobTrxComment
                .append(DelegatedPrintCommentSyntax.FIELD_SEPARATOR_FIRST)
                .append(requestingUserId)
                //
                .append(DelegatedPrintCommentSyntax.FIELD_SEPARATOR)
                .append(weightTotal)
                //
                .append(DelegatedPrintCommentSyntax.FIELD_SEPARATOR)
                .append(numberOfDocumentPages);

        // ... | A4 | S | G | id
        jobTrxComment.append(DelegatedPrintCommentSyntax.FIELD_SEPARATOR);

        DelegatedPrintCommentSyntax.appendIndicatorFields(jobTrxComment,
                indicatorPaperSize, indicatorDuplex, indicatorColor,
                indicatorExternalId);

        /*
         * Adjust the Personal and Shared Accounts in PaperCut and update the
         * SavaPage AccountTrx's.
         */
        int weightTotalJobTrx = 0;

        for (final AccountTrx trx : trxList) {

            final int weight = trx.getTransactionWeight().intValue();

            final BigDecimal weightedCost =
                    ACCOUNTING_SERVICE.calcWeightedAmount(papercutUsageCost,
                            weightTotal, weight, scale);

            final org.savapage.core.jpa.Account account = trx.getAccount();

            /*
             * PaperCut account adjustment.
             */
            final BigDecimal papercutAdjustment = weightedCost.negate();

            final AccountTypeEnum accountType =
                    AccountTypeEnum.valueOf(account.getAccountType());

            if (accountType == AccountTypeEnum.SHARED) {

                /*
                 * Adjust Shared [Parent]/[klas|group] Account.
                 */
                final String topAccountName = account.getParent().getName();
                final String subAccountName = account.getName();

                final String klasName =
                        this.getKlasFromAccountName(subAccountName);

                // requester | copies | pages
                final StringBuilder klasTrxComment = new StringBuilder();

                klasTrxComment
                        .append(DelegatedPrintCommentSyntax.FIELD_SEPARATOR_FIRST)
                        .append(requestingUserId)
                        //
                        .append(DelegatedPrintCommentSyntax.FIELD_SEPARATOR)
                        .append(weight)
                        //
                        .append(DelegatedPrintCommentSyntax.FIELD_SEPARATOR)
                        .append(numberOfDocumentPages);

                // ... | A4 | S | G | id
                klasTrxComment
                        .append(DelegatedPrintCommentSyntax.FIELD_SEPARATOR);

                DelegatedPrintCommentSyntax.appendIndicatorFields(
                        klasTrxComment, indicatorPaperSize, indicatorDuplex,
                        indicatorColor, indicatorExternalId);

                // ... | document | comment
                klasTrxComment
                        //
                        .append(DelegatedPrintCommentSyntax.FIELD_SEPARATOR)
                        .append(docLogTrx.getTitle())
                        //
                        .append(DelegatedPrintCommentSyntax.FIELD_SEPARATOR)
                        .append(StringUtils.defaultString(docLogTrx
                                .getLogComment()))
                        //
                        .append(DelegatedPrintCommentSyntax.FIELD_SEPARATOR_LAST);

                if (this.getLogger().isDebugEnabled()) {

                    this.getLogger().debug(
                            String.format("PaperCut shared account [%s] "
                                    + "adjustment [%s] comment: %s",
                                    this.papercutServerProxy
                                            .composeSharedAccountName(
                                                    topAccountName,
                                                    subAccountName),
                                    papercutAdjustment.toPlainString(),
                                    klasTrxComment.toString()));
                }

                PAPERCUT_SERVICE.lazyAdjustSharedAccount(
                        this.papercutServerProxy, topAccountName,
                        subAccountName, papercutAdjustment,
                        klasTrxComment.toString());

                // ... | user@class-n | copies-n
                jobTrxComment
                        .append(DelegatedPrintCommentSyntax.FIELD_SEPARATOR)
                        .append(requestingUserId)
                        //
                        .append(DelegatedPrintCommentSyntax.USER_CLASS_SEPARATOR)
                        .append(klasName)
                        //
                        .append(DelegatedPrintCommentSyntax.FIELD_SEPARATOR)
                        .append(weight);

                weightTotalJobTrx += weight;

            } else {

                final StringBuilder userCopiesComment = new StringBuilder();

                // class | requester | copies | pages
                userCopiesComment
                        .append(DelegatedPrintCommentSyntax.FIELD_SEPARATOR_FIRST)
                        .append(StringUtils.defaultString(trx.getExtDetails(),
                                DelegatedPrintCommentSyntax.DUMMY_KLAS))
                        //
                        .append(DelegatedPrintCommentSyntax.FIELD_SEPARATOR)
                        .append(requestingUserId)
                        //
                        .append(DelegatedPrintCommentSyntax.FIELD_SEPARATOR)
                        .append(weight)
                        //
                        .append(DelegatedPrintCommentSyntax.FIELD_SEPARATOR)
                        .append(numberOfDocumentPages);

                //
                // ... | A4 | S | G | id
                userCopiesComment
                        .append(DelegatedPrintCommentSyntax.FIELD_SEPARATOR);

                DelegatedPrintCommentSyntax.appendIndicatorFields(
                        userCopiesComment, indicatorPaperSize, indicatorDuplex,
                        indicatorColor, indicatorExternalId);

                // ... | document | comment
                userCopiesComment
                        //
                        .append(DelegatedPrintCommentSyntax.FIELD_SEPARATOR)
                        .append(docLogTrx.getTitle())
                        //
                        .append(DelegatedPrintCommentSyntax.FIELD_SEPARATOR)
                        .append(StringUtils.defaultString(docLogTrx
                                .getLogComment()))
                        //
                        .append(DelegatedPrintCommentSyntax.FIELD_SEPARATOR_LAST);

                //
                final UserAccountDao userAccountDao =
                        ServiceContext.getDaoContext().getUserAccountDao();

                /*
                 * Get the user of the transaction.
                 */
                final User user =
                        userAccountDao
                                .findByAccountId(trx.getAccount().getId())
                                .getUser();
                /*
                 * Adjust Personal Account.
                 */
                if (this.getLogger().isDebugEnabled()) {

                    this.getLogger().debug(
                            String.format("PaperCut personal account [%s] "
                                    + "adjustment [%s] comment [%s]",
                                    user.getUserId(),
                                    papercutAdjustment.toPlainString(),
                                    userCopiesComment.toString()));
                }

                PAPERCUT_SERVICE.adjustUserAccountBalance(
                        this.papercutServerProxy, user.getUserId(),
                        this.getUserAccountName(), papercutAdjustment,
                        userCopiesComment.toString());
            }

            /*
             * Update Account.
             */
            account.setBalance(account.getBalance().subtract(weightedCost));
            accountDao.update(account);

            /*
             * Update AccountTrx.
             */
            trx.setAmount(papercutAdjustment);
            trx.setBalance(account.getBalance());

            trx.setTransactedBy(ServiceContext.getActor());
            trx.setTransactionDate(ServiceContext.getTransactionDate());

            if (this.isDocInAccountTrx()) {
                // Move from DocLog source to target.
                trx.setDocLog(docLogOut);
            }

            accountTrxDao.update(trx);
        }

        /*
         * Create a transaction in the shared Jobs account with a comment of
         * formatted job data.
         */

        // ... |
        if (weightTotalJobTrx != weightTotal) {

            jobTrxComment
                    .append(DelegatedPrintCommentSyntax.FIELD_SEPARATOR)
                    .append(requestingUserId)
                    //
                    .append(DelegatedPrintCommentSyntax.USER_CLASS_SEPARATOR)
                    .append(DelegatedPrintCommentSyntax.DUMMY_KLAS)
                    //
                    .append(DelegatedPrintCommentSyntax.FIELD_SEPARATOR)
                    .append(weightTotal - weightTotalJobTrx);
        }

        // ... | document | comment
        jobTrxComment.append(DelegatedPrintCommentSyntax.FIELD_SEPARATOR)
                .append(docLogTrx.getTitle())
                //
                .append(DelegatedPrintCommentSyntax.FIELD_SEPARATOR)
                .append(StringUtils.defaultString(docLogTrx.getLogComment()))
                //
                .append(DelegatedPrintCommentSyntax.FIELD_SEPARATOR_LAST);

        PAPERCUT_SERVICE.lazyAdjustSharedAccount(this.papercutServerProxy, this
                .getSharedParentAccountName(), this.getSharedJobsAccountName(),
                papercutUsageCost.negate(), StringUtils.abbreviate(
                        jobTrxComment.toString(),
                        PaperCutDbProxy.COL_LEN_TXN_COMMENT));

        /*
         * DocLog updates.
         */
        final String externalStatus =
                ExternalSupplierStatusEnum.COMPLETED.toString();

        docLogOut.setExternalStatus(externalStatus);

        if (this.isDocInAccountTrx()) {
            /*
             * Move the AccountTrx list from DocLog source to target.
             */
            docLogOut.setTransactions(trxList);

            docLogIn.setExternalStatus(externalStatus);
            docLogIn.setTransactions(null);
            doclogDao.update(docLogIn);
        }

        doclogDao.update(docLogOut);
    }
}
