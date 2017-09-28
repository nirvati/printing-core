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

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.UnableToInterruptJobException;
import org.savapage.core.cometd.AdminPublisher;
import org.savapage.core.cometd.PubLevelEnum;
import org.savapage.core.cometd.PubTopicEnum;
import org.savapage.core.concurrent.ReadWriteLockEnum;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.dao.helpers.DaoBatchCommitter;
import org.savapage.core.services.ProxyPrintService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.helpers.SyncPrintJobsResult;
import org.savapage.core.util.AppLogHelper;
import org.slf4j.LoggerFactory;

/**
 * Synchronizes CUPS job state with PrintOut jobs.
 * <p>
 * An update-lock is set to prevent that this job and {@link DocLogClean} run at
 * the same time.
 * </p>
 *
 * @author Rijk Ravestein
 *
 */
public final class CupsSyncPrintJobs extends AbstractJob {

    @Override
    protected void onInterrupt() throws UnableToInterruptJobException {
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

    @Override
    protected void onExecute(final JobExecutionContext ctx)
            throws JobExecutionException {

        final ProxyPrintService proxyPrintService =
                ServiceContext.getServiceFactory().getProxyPrintService();

        final AdminPublisher publisher = AdminPublisher.instance();

        String msg = null;
        PubLevelEnum level = PubLevelEnum.INFO;

        final DaoBatchCommitter batchCommitter = ServiceContext.getDaoContext()
                .createBatchCommitter(ConfigManager.getDaoBatchChunkSize());

        publisher.publish(PubTopicEnum.CUPS, PubLevelEnum.INFO,
                localizeSysMsg("CupsSyncPrintJobs.start"));

        try {
            batchCommitter.lazyOpen();
            final SyncPrintJobsResult syncResult =
                    proxyPrintService.syncPrintJobs(batchCommitter);
            batchCommitter.close();

            if (syncResult.getJobsActive() > 0) {
                msg = AppLogHelper.logInfo(getClass(),
                        "CupsSyncPrintJobs.success",
                        String.valueOf(syncResult.getJobsActive()),
                        String.valueOf(syncResult.getJobsUpdated()),
                        String.valueOf(syncResult.getJobsNotFound()));
            } else {
                msg = localizeSysMsg("CupsSyncPrintJobs.success",
                        String.valueOf(syncResult.getJobsActive()),
                        String.valueOf(syncResult.getJobsUpdated()),
                        String.valueOf(syncResult.getJobsNotFound()));
            }

        } catch (Exception e) {

            batchCommitter.rollback();

            LoggerFactory.getLogger(this.getClass()).error(e.getMessage(), e);
            level = PubLevelEnum.ERROR;
            msg = AppLogHelper.logError(getClass(), "CupsSyncPrintJobs.error",
                    e.getMessage());
        }

        if (msg != null) {
            publisher.publish(PubTopicEnum.CUPS, level, msg);
        }

    }
}
