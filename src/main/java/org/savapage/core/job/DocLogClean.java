/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2017 Datraverse B.V.
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.core.job;

import java.time.Duration;
import java.util.Calendar;
import java.util.Date;

import javax.persistence.EntityManager;

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
import org.savapage.core.jpa.AccountVoucher;
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
     * Cleans up in 6 steps.
     * <p>
     * Step 1 and 2 delete (remove) {@link DocLog}, {@link DocOut} and
     * {@link DocIn} objects (including CASCADED deletes of {@link AccountTrx},
     * {@link PrintIn}, {@link DocInOut}, {@link PrintOut} and {@link PdfOut})
     * dating from daysBackInTime and older.
     * </p>
     * <p>
     * Step 3 removes {@link AccountTrx} objects (not related to an
     * {@link DocLog} object) dating from daysBackInTime and older.
     * </p>
     * <p>
     * Step 4, 5 and 6 remove logically deleted {@link User}, {@link Printer}
     * and {@link IppQueue} instances that do NOT have any related
     * {@link DocLog} anymore.
     * </p>
     * <p>
     * Step 7 removes {@link Account} instances (cascade delete) that are
     * <i>logically</i> deleted, and which do <i>not</i> have any related
     * {@link AccountTrx}.
     * </p>
     * <p>
     * <b>IMPORTANT</b>: After each step a commit is done.
     * </p>
     * <p>
     * <b>NOTE</b>: Records deleted with JPQL don't participate in
     * all-or-nothing transactions nor trigger cascading deletion for child
     * records.
     * </p>
     * <p>
     * That is why we don't use bulk delete with JPQL, since we want the option
     * to roll back the deletions as part of a transaction and use cascade
     * deletion. We use the remove() method in EntityManager to delete
     * individual records instead.
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

            cleanStep2DocOut(docClean, publisher, dateBackInTime,
                    batchCommitter);
            cleanStep3DocIn(docClean, publisher, dateBackInTime,
                    batchCommitter);
        }

        cleanStep4PruneUsers(docClean, publisher, batchCommitter);
        cleanStep5PrunePrinters(docClean, publisher, batchCommitter);
        cleanStep6PruneQueues(docClean, publisher, batchCommitter);
        cleanStep7PruneAccounts(docClean, publisher, batchCommitter);
    }

    /**
     * <b>Step 1</b>: Removes {@link AccountTrx} instances dating from
     * daysBackInTime and older.
     *
     * <p>
     * Note: For each removed {@link AccountTrx} any associated
     * {@link AccountVoucher} instance is deleted by cascade.
     * </p>
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

        final AccountTrxDao dao =
                ServiceContext.getDaoContext().getAccountTrxDao();

        if (docClean != null) {
            SpInfo.instance().log(String.format(
                    "| Cleaning AccountTrx from total [%d] ...", dao.count()));
            publisher.publish(PubTopicEnum.DB, PubLevelEnum.INFO,
                    docClean.localizeSysMsg("AccountTrxClean.start"));
        }

        batchCommitter.lazyOpen();
        final int nDeleted = dao.cleanHistory(dateBackInTime, batchCommitter);
        final Duration duration = batchCommitter.close();

        if (docClean == null) {
            return;
        }

        SpInfo.instance().log(String.format(
                "|   Cleaned [%d] AccountTrx in %s: new total [%d]", nDeleted,
                DateUtil.formatDuration(duration.toMillis()), dao.count()));

        if (nDeleted == 0) {
            publisher.publish(PubTopicEnum.DB, PubLevelEnum.INFO,
                    docClean.localizeSysMsg("AccountTrxClean.success.zero"));
        } else if (nDeleted == 1) {
            final String msg = AppLogHelper.logInfo(docClean.getClass(),
                    "AccountTrxClean.success.single");
            publisher.publish(PubTopicEnum.DB, PubLevelEnum.INFO, msg);
        } else {
            final String msg = AppLogHelper.logInfo(docClean.getClass(),
                    "AccountTrxClean.success.plural", String.valueOf(nDeleted));
            publisher.publish(PubTopicEnum.DB, PubLevelEnum.INFO, msg);
        }

    }

    /**
     * <b>Step 2</b>: Removes {@link DocLog} instances dating from
     * daysBackInTime and older which DO have a {@link DocOut} association.
     * <p>
     * IMPORTANT: Deleted DocInOut instances in Step 1, need to be committed
     * first, so this step will get "clean" DocInOut relations.
     * </p>
     * <p>
     * Note: For each removed {@link DocLog} the associated {@link DocOut}
     * instance and {@link AccountTrx} instances are deleted by cascade.
     * </p>
     *
     * @param docClean
     * @param publisher
     * @param dateBackInTime
     *            Date back in time.
     * @param batchCommitter
     *            The {@link DaoBatchCommitter}.
     * @return the number of deleted DocLog/DocOut objects.
     */
    private static void cleanStep2DocOut(final DocLogClean docClean,
            final AdminPublisher publisher, final Date dateBackInTime,
            final DaoBatchCommitter batchCommitter) {

        final DocLogDao dao = ServiceContext.getDaoContext().getDocLogDao();

        if (docClean != null) {
            SpInfo.instance().log(String.format(
                    "| Cleaning DocLog with DocOut" + " from total [%d] ...",
                    dao.count()));

            publisher.publish(PubTopicEnum.DB, PubLevelEnum.INFO,
                    docClean.localizeSysMsg("DocOutLogClean.start"));
        }

        batchCommitter.lazyOpen();
        int nDeleted = dao.cleanDocOutHistory(dateBackInTime, batchCommitter);
        final Duration duration = batchCommitter.close();

        if (docClean == null) {
            return;
        }

        SpInfo.instance().log(String.format(
                "|   Cleaned [%d] DocLog with DocOut"
                        + " in %s: new total [%d]",
                nDeleted, DateUtil.formatDuration(duration.toMillis()),
                dao.count()));

        if (nDeleted == 0) {
            publisher.publish(PubTopicEnum.DB, PubLevelEnum.INFO,
                    docClean.localizeSysMsg("DocOutLogClean.success.zero"));
        } else if (nDeleted == 1) {
            final String msg = AppLogHelper.logInfo(docClean.getClass(),
                    "DocOutLogClean.success.single");
            publisher.publish(PubTopicEnum.DB, PubLevelEnum.INFO, msg);
        } else {
            final String msg = AppLogHelper.logInfo(docClean.getClass(),
                    "DocOutLogClean.success.plural", String.valueOf(nDeleted));
            publisher.publish(PubTopicEnum.DB, PubLevelEnum.INFO, msg);
        }
    }

    /**
     * Step 3: Removes {@link DocLog} instances dating from daysBackInTime and
     * older with a {@link DocIn} association which do NOT have related
     * {@link DocInOut} instances.
     * <p>
     * IMPORTANT: DocInOut instances were deleted (and committed) in Step 1, so
     * this step will get "clean" DocInOut relations.
     * </p>
     * <p>
     * Note: For each removed {@link DocLog} the associated {@link DocIn}
     * instance and {@link AccountTrx} instances are deleted by cascade.
     * </p>
     *
     * @param docClean
     * @param publisher
     * @param dateBackInTime
     * @param batchCommitter
     *            The {@link DaoBatchCommitter}.
     */
    private static void cleanStep3DocIn(final DocLogClean docClean,
            final AdminPublisher publisher, final Date dateBackInTime,
            final DaoBatchCommitter batchCommitter) {

        final DocLogDao dao = ServiceContext.getDaoContext().getDocLogDao();

        if (docClean != null) {
            SpInfo.instance().log(String.format(
                    "| Cleaning DocLog with DocIn" + " from total [%d] ...",
                    dao.count()));

            publisher.publish(PubTopicEnum.DB, PubLevelEnum.INFO,
                    docClean.localizeSysMsg("DocInLogClean.start"));
        }

        batchCommitter.lazyOpen();
        int nDeleted = dao.cleanDocInHistory(dateBackInTime, batchCommitter);
        final Duration duration = batchCommitter.close();

        if (docClean == null) {
            return;
        }

        SpInfo.instance()
                .log(String.format(
                        "|   Cleaned [%d] DocLog with DocIn"
                                + " in %s: new total [%d]",
                        nDeleted, DateUtil.formatDuration(duration.toMillis()),
                        dao.count()));

        if (nDeleted == 0) {
            publisher.publish(PubTopicEnum.DB, PubLevelEnum.INFO,
                    docClean.localizeSysMsg("DocInLogClean.success.zero"));
        } else if (nDeleted == 1) {
            final String msg = AppLogHelper.logInfo(docClean.getClass(),
                    "DocInLogClean.success.single");
            publisher.publish(PubTopicEnum.DB, PubLevelEnum.INFO, msg);
        } else {
            final String msg = AppLogHelper.logInfo(docClean.getClass(),
                    "DocInLogClean.success.plural", String.valueOf(nDeleted));
            publisher.publish(PubTopicEnum.DB, PubLevelEnum.INFO, msg);
        }
    }

    /**
     * <b>Step 4</b>: A wrapper for {@link UserDao#pruneUsers()}.
     *
     * @param docClean
     * @param publisher
     * @param batchCommitter
     *            The {@link DaoBatchCommitter}.
     */
    private static void cleanStep4PruneUsers(final DocLogClean docClean,
            final AdminPublisher publisher,
            final DaoBatchCommitter batchCommitter) {

        final UserDao dao = ServiceContext.getDaoContext().getUserDao();

        if (docClean != null) {
            SpInfo.instance().log(String.format(
                    "| Cleaning User from total [%d] ...", dao.count()));
            publisher.publish(PubTopicEnum.DB, PubLevelEnum.INFO,
                    docClean.localizeSysMsg("DeletedUserClean.start"));
        }

        batchCommitter.lazyOpen();
        int nDeleted = dao.pruneUsers(batchCommitter);
        final Duration duration = batchCommitter.close();

        if (docClean == null) {
            return;
        }

        SpInfo.instance().log(String.format(
                "|   Cleaned [%d] User in %s: new total [%d]", nDeleted,
                DateUtil.formatDuration(duration.toMillis()), dao.count()));

        if (nDeleted == 0) {
            publisher.publish(PubTopicEnum.DB, PubLevelEnum.INFO,
                    docClean.localizeSysMsg("DeletedUserClean.success.zero"));
        } else if (nDeleted == 1) {
            final String msg = AppLogHelper.logInfo(docClean.getClass(),
                    "DeletedUserClean.success.single");
            publisher.publish(PubTopicEnum.DB, PubLevelEnum.INFO, msg);
        } else {
            final String msg = AppLogHelper.logInfo(docClean.getClass(),
                    "DeletedUserClean.success.plural",
                    String.valueOf(nDeleted));
            publisher.publish(PubTopicEnum.DB, PubLevelEnum.INFO, msg);
        }
    }

    /**
     * <b>Step 5</b>: A wrapper for {@link Printer#prunePrinters(EntityManager)}
     * .
     *
     * @param docClean
     * @param publisher
     * @param batchCommitter
     *            The {@link DaoBatchCommitter}.
     */
    private static void cleanStep5PrunePrinters(final DocLogClean docClean,
            final AdminPublisher publisher,
            final DaoBatchCommitter batchCommitter) {

        final PrinterDao dao = ServiceContext.getDaoContext().getPrinterDao();

        if (docClean != null) {
            SpInfo.instance().log(String.format(
                    "| Cleaning Printer from total [%d] ...", dao.count()));
            publisher.publish(PubTopicEnum.DB, PubLevelEnum.INFO,
                    docClean.localizeSysMsg("DeletedPrinterClean.start"));
        }

        batchCommitter.lazyOpen();
        int nDeleted = dao.prunePrinters(batchCommitter);
        final Duration duration = batchCommitter.close();

        if (docClean == null) {
            return;
        }

        SpInfo.instance().log(String.format(
                "|   Cleaned [%d] Printer in %s: new total [%d]", nDeleted,
                DateUtil.formatDuration(duration.toMillis()), dao.count()));

        if (nDeleted == 0) {
            publisher.publish(PubTopicEnum.DB, PubLevelEnum.INFO, docClean
                    .localizeSysMsg("DeletedPrinterClean.success.zero"));
        } else if (nDeleted == 1) {
            final String msg = AppLogHelper.logInfo(docClean.getClass(),
                    "DeletedPrinterClean.success.single");
            publisher.publish(PubTopicEnum.DB, PubLevelEnum.INFO, msg);
        } else {
            final String msg = AppLogHelper.logInfo(docClean.getClass(),
                    "DeletedPrinterClean.success.plural",
                    String.valueOf(nDeleted));
            publisher.publish(PubTopicEnum.DB, PubLevelEnum.INFO, msg);
        }
    }

    /**
     * <b>Step 6</b>: A wrapper for {@link IppQueue#pruneQueues(EntityManager)}.
     *
     * @param docClean
     * @param publisher
     * @param batchCommitter
     *            The {@link DaoBatchCommitter}.
     */
    private static void cleanStep6PruneQueues(final DocLogClean docClean,
            final AdminPublisher publisher,
            final DaoBatchCommitter batchCommitter) {

        final IppQueueDao dao = ServiceContext.getDaoContext().getIppQueueDao();

        if (docClean != null) {
            SpInfo.instance().log(String.format(
                    "| Cleaning IppQueue from total [%d] ...", dao.count()));
            publisher.publish(PubTopicEnum.DB, PubLevelEnum.INFO,
                    docClean.localizeSysMsg("DeletedQueueClean.start"));
        }

        batchCommitter.lazyOpen();
        int nDeleted = dao.pruneQueues(batchCommitter);
        final Duration duration = batchCommitter.close();

        if (docClean == null) {
            return;
        }

        SpInfo.instance().log(String.format(
                "|   Cleaned [%d] IppQueue in %s: new total [%d]", nDeleted,
                DateUtil.formatDuration(duration.toMillis()), dao.count()));

        if (nDeleted == 0) {
            publisher.publish(PubTopicEnum.DB, PubLevelEnum.INFO,
                    docClean.localizeSysMsg("DeletedQueueClean.success.zero"));
        } else if (nDeleted == 1) {
            final String msg = AppLogHelper.logInfo(docClean.getClass(),
                    "DeletedQueueClean.success.single");
            publisher.publish(PubTopicEnum.DB, PubLevelEnum.INFO, msg);
        } else {
            final String msg = AppLogHelper.logInfo(docClean.getClass(),
                    "DeletedQueueClean.success.plural",
                    String.valueOf(nDeleted));
            publisher.publish(PubTopicEnum.DB, PubLevelEnum.INFO, msg);
        }
    }

    /**
     * <b>Step 7</b>:
     *
     * @param docClean
     * @param publisher
     * @param batchCommitter
     *            The {@link DaoBatchCommitter}.
     */
    private static void cleanStep7PruneAccounts(final DocLogClean docClean,
            final AdminPublisher publisher,
            final DaoBatchCommitter batchCommitter) {

        final AccountDao dao = ServiceContext.getDaoContext().getAccountDao();

        if (docClean != null) {
            SpInfo.instance().log(String.format(
                    "| Cleaning Account from total [%d] ...", dao.count()));
            publisher.publish(PubTopicEnum.DB, PubLevelEnum.INFO,
                    docClean.localizeSysMsg("AccountClean.start"));
        }

        batchCommitter.lazyOpen();
        final int nDeleted = dao.pruneAccounts(batchCommitter);
        final Duration duration = batchCommitter.close();

        if (docClean == null) {
            return;
        }

        SpInfo.instance().log(String.format(
                "|   Cleaned [%d] Account in %s: new total [%d]", nDeleted,
                DateUtil.formatDuration(duration.toMillis()), dao.count()));

        if (nDeleted == 0) {
            publisher.publish(PubTopicEnum.DB, PubLevelEnum.INFO,
                    docClean.localizeSysMsg("AccountClean.success.zero"));
        } else if (nDeleted == 1) {
            final String msg = AppLogHelper.logInfo(docClean.getClass(),
                    "AccountClean.success.single");
            publisher.publish(PubTopicEnum.DB, PubLevelEnum.INFO, msg);
        } else {
            final String msg = AppLogHelper.logInfo(docClean.getClass(),
                    "AccountClean.success.plural", String.valueOf(nDeleted));
            publisher.publish(PubTopicEnum.DB, PubLevelEnum.INFO, msg);
        }
    }
}
