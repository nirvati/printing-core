/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2020 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: © 2020 Datraverse B.V. <info@datraverse.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.core.job;

import java.time.Duration;
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.lang3.time.DateUtils;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.UnableToInterruptJobException;
import org.savapage.core.SpInfo;
import org.savapage.core.cometd.AdminPublisher;
import org.savapage.core.cometd.PubLevelEnum;
import org.savapage.core.cometd.PubTopicEnum;
import org.savapage.core.concurrent.ReadWriteLockEnum;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.dao.AccountDao;
import org.savapage.core.dao.AccountTrxDao;
import org.savapage.core.dao.DocLogDao;
import org.savapage.core.dao.IppQueueDao;
import org.savapage.core.dao.PrinterDao;
import org.savapage.core.dao.UserDao;
import org.savapage.core.dao.helpers.DaoBatchCommitter;
import org.savapage.core.jpa.Account;
import org.savapage.core.jpa.AccountTrx;
import org.savapage.core.jpa.DocIn;
import org.savapage.core.jpa.DocInOut;
import org.savapage.core.jpa.DocLog;
import org.savapage.core.jpa.DocOut;
import org.savapage.core.jpa.IppQueue;
import org.savapage.core.jpa.PdfOut;
import org.savapage.core.jpa.PrintIn;
import org.savapage.core.jpa.PrintOut;
import org.savapage.core.jpa.Printer;
import org.savapage.core.jpa.User;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.util.AppLogHelper;
import org.savapage.core.util.DateUtil;
import org.slf4j.LoggerFactory;

/**
 * Cleans-up the document log.
 * <p>
 * An update-lock is set to prevent that this job and {@link CupsSyncPrintJobs}
 * run at the same time.
 * </p>
 *
 * @author Rijk Ravestein
 *
 */
public final class DocLogClean extends AbstractJob {

    @Override
    protected void onInterrupt() throws UnableToInterruptJobException {
        // noop
    }

    @Override
    protected void onInit(final JobExecutionContext ctx) {
        ReadWriteLockEnum.DATABASE_READONLY.setReadLock(true);
        ReadWriteLockEnum.PRINT_OUT_HISTORY.setWriteLock(true);
    }

    @Override
    protected void onExit(final JobExecutionContext ctx) {
        ReadWriteLockEnum.PRINT_OUT_HISTORY.setWriteLock(false);
        ReadWriteLockEnum.DATABASE_READONLY.setReadLock(false);
    }

    /**
     * Database history clean-up in 8 steps.
     * <ul>
     *
     * <li>Step 1 removes {@link AccountTrx} objects (not related to a
     * {@link DocLog} object) dating from daysBackInTime and older, including
     * related objects.</li>
     *
     * <li>Step 2 removes {@link AccountTrx} objects related to {@link DocLog}
     * objects, dating from daysBackInTime and older, including related
     * objects.</li>
     *
     * <li>Step 3 and 4 delete {@link DocLog}, instances dating from
     * daysBackInTime and older, including related {@link DocOut},
     * {@link DocIn}, {@link PrintIn}, {@link DocInOut}, {@link PrintOut} and
     * {@link PdfOut} objects.</li>
     *
     * <li>Step 5, 6 and 7 remove logically deleted {@link User},
     * {@link Printer} and {@link IppQueue} instances that do NOT have any
     * related {@link DocLog} anymore.</li>
     *
     * <li>Step 8 removes {@link Account} instances (cascade delete) that are
     * <i>logically</i> deleted, and which do <i>not</i> have any related
     * {@link AccountTrx}.</li>
     *
     * </ul>
     *
     * <b>IMPORTANT</b>: After each step a commit is done.
     * <p>
     * <b>REMEMBER</b>: <i>Records deleted with JPQL don't trigger cascading
     * deletion for child records.</i>
     * </p>
     */
    @Override
    public void onExecute(final JobExecutionContext ctx)
            throws JobExecutionException {

        final ConfigManager cm = ConfigManager.instance();

        final boolean isDelAccountTrx =
                cm.isConfigValue(Key.DELETE_ACCOUNT_TRX_LOG);

        final boolean isDelDocLog = cm.isConfigValue(Key.DELETE_DOC_LOG);

        /*
         * INVARIANT: for a scheduled (not a one-shot) job, DeleteDocLog AND
         * DeleteAccountTrxLog MUST both be enabled.
         */
        if (ctx.getJobDetail().getKey().getGroup()
                .equals(SpJobScheduler.JOB_GROUP_SCHEDULED) && !isDelAccountTrx
                && !isDelDocLog) {
            return;
        }

        Integer daysBackAccountTrx = null;
        Integer daysBackDocLog = null;

        if (isDelAccountTrx) {
            final int days = cm.getConfigInt(Key.DELETE_ACCOUNT_TRX_DAYS);
            if (days > 0) {
                daysBackAccountTrx = days;
            }
        }

        if (isDelDocLog) {
            final int days = cm.getConfigInt(Key.DELETE_DOC_LOG_DAYS);
            if (days > 0) {
                daysBackDocLog = days;
            }
        }

        /*
         * INVARIANT: Return if both days LT zero.
         */
        if (daysBackAccountTrx == null && daysBackDocLog == null) {
            return;
        }

        final DaoBatchCommitter batchCommitter = ServiceContext.getDaoContext()
                .createBatchCommitter(ConfigManager.getDaoBatchChunkSize());

        final AdminPublisher publisher = AdminPublisher.instance();

        try {

            clean(this, publisher, daysBackAccountTrx, daysBackDocLog,
                    batchCommitter);

        } catch (Exception e) {

            batchCommitter.rollback();

            LoggerFactory.getLogger(this.getClass()).error(e.getMessage(), e);

            final String msg = AppLogHelper.logError(getClass(),
                    "DocLogClean.error", e.getMessage());
            publisher.publish(PubTopicEnum.DB, PubLevelEnum.ERROR, msg);

        }
    }

    /**
     * Cleans up {@link DocLog} and {@link AccountTrx} log. This is a
     * convenience method to execute the job outside the Quarz job context.
     * <p>
     * IMPORTANT: this method manages its OWN commit scope, the client caller
     * must NOT begin(), commit() or rollback() transactions.
     * </p>
     *
     * @param daysBackInTime
     */
    public static void clean(final int daysBackInTime) {

        final DaoBatchCommitter batchCommitter = ServiceContext.getDaoContext()
                .createBatchCommitter(ConfigManager.getDaoBatchChunkSize());
        try {
            clean(null, null, daysBackInTime, daysBackInTime, batchCommitter);
        } catch (Exception e) {
            batchCommitter.rollback();
        }
    }

    /**
     *
     * @param docClean
     *            {@code null} when NOT run in {@link DocLogClean} context.
     * @param publisher
     *            {@code null} when NOT run in {@link DocLogClean} context.
     * @param daysBackInTimeAccountTrx
     *            When {@code null} {@link AccountTrx} is NOT cleaned.
     * @param daysBackInTimeDocLog
     *            When {@code null} {@link DocLog} is NOT cleaned.
     * @param batchCommitter
     *            The {@link DaoBatchCommitter}.
     */
    private static void clean(final DocLogClean docClean,
            final AdminPublisher publisher,
            final Integer daysBackInTimeAccountTrx,
            final Integer daysBackInTimeDocLog,
            final DaoBatchCommitter batchCommitter) {

        if (daysBackInTimeAccountTrx != null) {

            final Date dateBackInTime = DateUtils.truncate(
                    DateUtils.addDays(new Date(), -daysBackInTimeAccountTrx),
                    Calendar.DAY_OF_MONTH);

            cleanStep1AccountTrx(docClean, publisher, dateBackInTime,
                    batchCommitter);
        }

        if (daysBackInTimeDocLog != null) {

            final Date dateBackInTime = DateUtils.truncate(
                    DateUtils.addDays(new Date(), -daysBackInTimeDocLog),
                    Calendar.DAY_OF_MONTH);

            cleanStep2DocAccountTrx(docClean, publisher, dateBackInTime,
                    batchCommitter);
            cleanStep3DocOut(docClean, publisher, dateBackInTime,
                    batchCommitter);
            cleanStep4DocIn(docClean, publisher, dateBackInTime,
                    batchCommitter);
        }

        cleanStep5PruneUsers(docClean, publisher, batchCommitter);
        cleanStep6PrunePrinters(docClean, publisher, batchCommitter);
        cleanStep7PruneQueues(docClean, publisher, batchCommitter);
        cleanStep8PruneAccounts(docClean, publisher, batchCommitter);
    }

    /**
     * @param publisher
     * @param msgKey
     * @param entity
     * @param rowsBefore
     */
    private void onCleanStepBegin(final String entity, final long rowsBefore,
            final AdminPublisher publisher, final String pubMsgKeyBase) {

        SpInfo.instance().log(
                String.format("| Cleaning %s [%d] ...", entity, rowsBefore));

        publisher.publish(PubTopicEnum.DB, PubLevelEnum.INFO,
                this.localizeSysMsg(String.format("%s.start", pubMsgKeyBase)));
    }

    /**
     *
     * @param entity
     * @param duration
     *            {@code null} when cleaning was not performed.
     * @param nDeleted
     * @param publisher
     * @param pubMsgKeyBase
     */
    private void onCleanStepEnd(final String entity, final Duration duration,
            final int nDeleted, final AdminPublisher publisher,
            final String pubMsgKeyBase) {

        final String formattedDuration;
        if (duration == null) {
            formattedDuration = "-";
        } else {
            formattedDuration = DateUtil.formatDuration(duration.toMillis());
        }

        SpInfo.instance().log(String.format("|          %s : %d %s cleaned.",
                formattedDuration, nDeleted, entity));

        if (nDeleted == 0) {

            publisher.publish(PubTopicEnum.DB, PubLevelEnum.INFO,
                    this.localizeSysMsg(
                            String.format("%s.success.zero", pubMsgKeyBase)));

        } else if (nDeleted == 1) {

            final String msg = AppLogHelper.logInfo(this.getClass(),
                    String.format("%s.success.single", pubMsgKeyBase));

            publisher.publish(PubTopicEnum.DB, PubLevelEnum.INFO, msg);

        } else {

            final String msg = AppLogHelper.logInfo(this.getClass(),
                    String.format("%s.success.plural", pubMsgKeyBase),
                    String.valueOf(nDeleted));
            publisher.publish(PubTopicEnum.DB, PubLevelEnum.INFO, msg);
        }
    }

    /**
     * A wrapper for
     * {@link AccountTrxDao#cleanHistory(Date, DaoBatchCommitter)}.
     *
     * @param docClean
     *            {@code null} when NOT run in {@link DocLogClean} context.
     * @param publisher
     *            {@code null} when NOT run in {@link DocLogClean} context.
     * @param dateBackInTime
     *            History border date.
     * @param batchCommitter
     *            The {@link DaoBatchCommitter}.
     */
    private static void cleanStep1AccountTrx(final DocLogClean docClean,
            final AdminPublisher publisher, final Date dateBackInTime,
            final DaoBatchCommitter batchCommitter) {

        final String entity = "AccountTrx";
        final String pubMsgKeyBase = "AccountTrxClean";

        final AccountTrxDao dao =
                ServiceContext.getDaoContext().getAccountTrxDao();

        final long rowsBefore = dao.count();

        if (docClean != null) {
            docClean.onCleanStepBegin(entity, rowsBefore, publisher,
                    pubMsgKeyBase);
        }

        final boolean performClean = rowsBefore > 0;
        final int nDeleted;
        final Duration duration;

        if (performClean) {
            batchCommitter.lazyOpen();
            nDeleted = dao.cleanHistory(dateBackInTime, batchCommitter);
            duration = batchCommitter.close();
        } else {
            nDeleted = 0;
            duration = null;
        }

        if (docClean != null) {
            docClean.onCleanStepEnd(entity, duration, nDeleted, publisher,
                    pubMsgKeyBase);
        }
    }

    /**
     * A wrapper for
     * {@link DocLogDao#cleanAccountTrxHistory(Date, DaoBatchCommitter)}.
     *
     * @param docClean
     *            {@code null} when NOT run in {@link DocLogClean} context.
     * @param publisher
     *            {@code null} when NOT run in {@link DocLogClean} context.
     * @param dateBackInTime
     *            History border date.
     * @param batchCommitter
     *            The {@link DaoBatchCommitter}.
     */
    private static void cleanStep2DocAccountTrx(final DocLogClean docClean,
            final AdminPublisher publisher, final Date dateBackInTime,
            final DaoBatchCommitter batchCommitter) {

        final String entity = "DocLog/AccountTrx";
        final String pubMsgKeyBase = "AccountTrxClean";

        final AccountTrxDao daoAccountTrx =
                ServiceContext.getDaoContext().getAccountTrxDao();

        final long rowsBefore = daoAccountTrx.count();

        if (docClean != null) {
            docClean.onCleanStepBegin(entity, rowsBefore, publisher,
                    pubMsgKeyBase);
        }

        final DocLogDao daoDocLog =
                ServiceContext.getDaoContext().getDocLogDao();

        final boolean performClean;

        if (rowsBefore > 0) {
            performClean = daoDocLog.count() > 0;
        } else {
            performClean = false;
        }

        final int nDeleted;
        final Duration duration;

        if (performClean) {
            batchCommitter.lazyOpen();
            nDeleted = daoDocLog.cleanAccountTrxHistory(dateBackInTime,
                    batchCommitter);
            duration = batchCommitter.close();
        } else {
            nDeleted = 0;
            duration = null;
        }

        if (docClean != null) {
            docClean.onCleanStepEnd(entity, duration, nDeleted, publisher,
                    pubMsgKeyBase);
        }
    }

    /**
     * A wrapper for
     * {@link DocLogDao#cleanDocOutHistory(Date, DaoBatchCommitter)}.
     *
     * @param docClean
     *            {@code null} when NOT run in {@link DocLogClean} context.
     * @param publisher
     *            {@code null} when NOT run in {@link DocLogClean} context.
     * @param dateBackInTime
     *            History border date.
     * @param batchCommitter
     *            The {@link DaoBatchCommitter}.
     */
    private static void cleanStep3DocOut(final DocLogClean docClean,
            final AdminPublisher publisher, final Date dateBackInTime,
            final DaoBatchCommitter batchCommitter) {

        final String entity = "DocLog/DocOut";
        final String pubMsgKeyBase = "DocOutLogClean";

        final DocLogDao dao = ServiceContext.getDaoContext().getDocLogDao();

        final long rowsBefore = dao.count();

        if (docClean != null) {
            docClean.onCleanStepBegin(entity, rowsBefore, publisher,
                    pubMsgKeyBase);
        }

        final boolean performClean = rowsBefore > 0;
        final int nDeleted;
        final Duration duration;

        if (performClean) {
            batchCommitter.lazyOpen();
            nDeleted = dao.cleanDocOutHistory(dateBackInTime, batchCommitter);
            duration = batchCommitter.close();

        } else {
            nDeleted = 0;
            duration = null;
        }

        if (docClean != null) {
            docClean.onCleanStepEnd(entity, duration, nDeleted, publisher,
                    pubMsgKeyBase);
        }
    }

    /**
     * A wrapper for
     * {@link DocLogDao#cleanDocInHistory(Date, DaoBatchCommitter)}.
     *
     * @param docClean
     *            {@code null} when NOT run in {@link DocLogClean} context.
     * @param publisher
     *            {@code null} when NOT run in {@link DocLogClean} context.
     * @param dateBackInTime
     *            History border date.
     * @param batchCommitter
     *            The {@link DaoBatchCommitter}.
     */
    private static void cleanStep4DocIn(final DocLogClean docClean,
            final AdminPublisher publisher, final Date dateBackInTime,
            final DaoBatchCommitter batchCommitter) {

        final String entity = "DocLog/DocIn";
        final String pubMsgKeyBase = "DocInLogClean";

        final DocLogDao dao = ServiceContext.getDaoContext().getDocLogDao();

        final long rowsBefore = dao.count();

        if (docClean != null) {
            docClean.onCleanStepBegin(entity, rowsBefore, publisher,
                    pubMsgKeyBase);
        }

        final boolean performClean = rowsBefore > 0;
        final int nDeleted;
        final Duration duration;

        if (performClean) {

            batchCommitter.lazyOpen();
            nDeleted = dao.cleanDocInHistory(dateBackInTime, batchCommitter);
            duration = batchCommitter.close();

        } else {
            nDeleted = 0;
            duration = null;
        }

        if (docClean != null) {
            docClean.onCleanStepEnd(entity, duration, nDeleted, publisher,
                    pubMsgKeyBase);
        }
    }

    /**
     * A wrapper for {@link UserDao#pruneUsers(DaoBatchCommitter)}.
     *
     * @param docClean
     *            {@code null} when NOT run in {@link DocLogClean} context.
     * @param publisher
     *            {@code null} when NOT run in {@link DocLogClean} context.
     * @param batchCommitter
     *            The {@link DaoBatchCommitter}.
     */
    private static void cleanStep5PruneUsers(final DocLogClean docClean,
            final AdminPublisher publisher,
            final DaoBatchCommitter batchCommitter) {

        final String entity = "User";
        final String pubMsgKeyBase = "DeletedUserClean";

        final UserDao dao = ServiceContext.getDaoContext().getUserDao();

        final long rowsBefore = dao.count();

        if (docClean != null) {
            docClean.onCleanStepBegin(entity, rowsBefore, publisher,
                    pubMsgKeyBase);
        }

        final boolean performClean = rowsBefore > 0;
        final int nDeleted;
        final Duration duration;

        if (performClean) {
            batchCommitter.lazyOpen();
            nDeleted = dao.pruneUsers(batchCommitter);
            duration = batchCommitter.close();
        } else {
            nDeleted = 0;
            duration = null;
        }

        if (docClean != null) {
            docClean.onCleanStepEnd(entity, duration, nDeleted, publisher,
                    pubMsgKeyBase);
        }
    }

    /**
     * A wrapper for {@link PrinterDao#prunePrinters(DaoBatchCommitter)}.
     *
     * @param docClean
     *            {@code null} when NOT run in {@link DocLogClean} context.
     * @param publisher
     *            {@code null} when NOT run in {@link DocLogClean} context.
     * @param batchCommitter
     *            The {@link DaoBatchCommitter}.
     */
    private static void cleanStep6PrunePrinters(final DocLogClean docClean,
            final AdminPublisher publisher,
            final DaoBatchCommitter batchCommitter) {

        final String entity = "Printer";
        final String pubMsgKeyBase = "DeletedPrinterClean";

        final PrinterDao dao = ServiceContext.getDaoContext().getPrinterDao();

        final long rowsBefore = dao.count();

        if (docClean != null) {
            docClean.onCleanStepBegin(entity, rowsBefore, publisher,
                    pubMsgKeyBase);
        }

        final boolean performClean = rowsBefore > 0;
        final int nDeleted;
        final Duration duration;

        if (performClean) {
            batchCommitter.lazyOpen();
            nDeleted = dao.prunePrinters(batchCommitter);
            duration = batchCommitter.close();
        } else {
            nDeleted = 0;
            duration = null;
        }

        if (docClean != null) {
            docClean.onCleanStepEnd(entity, duration, nDeleted, publisher,
                    pubMsgKeyBase);
        }
    }

    /**
     * A wrapper for {@link IppQueueDao#pruneQueues(DaoBatchCommitter)}.
     *
     * @param docClean
     *            {@code null} when NOT run in {@link DocLogClean} context.
     * @param publisher
     *            {@code null} when NOT run in {@link DocLogClean} context.
     * @param batchCommitter
     *            The {@link DaoBatchCommitter}.
     */
    private static void cleanStep7PruneQueues(final DocLogClean docClean,
            final AdminPublisher publisher,
            final DaoBatchCommitter batchCommitter) {

        final String entity = "IppQueue";
        final String pubMsgKeyBase = "DeletedQueueClean";

        final IppQueueDao dao = ServiceContext.getDaoContext().getIppQueueDao();

        final long rowsBefore = dao.count();

        if (docClean != null) {
            docClean.onCleanStepBegin(entity, rowsBefore, publisher,
                    pubMsgKeyBase);
        }

        final boolean performClean = rowsBefore > 0;
        final int nDeleted;
        final Duration duration;

        if (performClean) {
            batchCommitter.lazyOpen();
            nDeleted = dao.pruneQueues(batchCommitter);
            duration = batchCommitter.close();
        } else {
            nDeleted = 0;
            duration = null;
        }

        if (docClean != null) {
            docClean.onCleanStepEnd(entity, duration, nDeleted, publisher,
                    pubMsgKeyBase);
        }
    }

    /**
     * A wrapper for {@link AccountDao#pruneAccounts(DaoBatchCommitter)}.
     *
     * @param docClean
     *            {@code null} when NOT run in {@link DocLogClean} context.
     * @param publisher
     *            {@code null} when NOT run in {@link DocLogClean} context.
     * @param batchCommitter
     *            The {@link DaoBatchCommitter}.
     */
    private static void cleanStep8PruneAccounts(final DocLogClean docClean,
            final AdminPublisher publisher,
            final DaoBatchCommitter batchCommitter) {

        final String entity = "Account";
        final String pubMsgKeyBase = "AccountClean";

        final AccountDao dao = ServiceContext.getDaoContext().getAccountDao();

        final long rowsBefore = dao.count();

        if (docClean != null) {
            docClean.onCleanStepBegin(entity, rowsBefore, publisher,
                    pubMsgKeyBase);
        }

        final boolean performClean = rowsBefore > 0;
        final int nDeleted;
        final Duration duration;

        if (performClean) {
            batchCommitter.lazyOpen();
            nDeleted = dao.pruneAccounts(batchCommitter);
            duration = batchCommitter.close();
        } else {
            nDeleted = 0;
            duration = null;
        }

        if (docClean != null) {
            docClean.onCleanStepEnd(entity, duration, nDeleted, publisher,
                    pubMsgKeyBase);
        }
    }

}
