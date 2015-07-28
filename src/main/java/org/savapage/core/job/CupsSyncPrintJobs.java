/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2014 Datraverse B.V.
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
package org.savapage.core.job;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.UnableToInterruptJobException;
import org.savapage.core.cometd.AdminPublisher;
import org.savapage.core.cometd.PubLevelEnum;
import org.savapage.core.cometd.PubTopicEnum;
import org.savapage.core.concurrent.ReadWriteLockEnum;
import org.savapage.core.dao.DaoContext;
import org.savapage.core.services.ProxyPrintService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.util.AppLogHelper;
import org.slf4j.LoggerFactory;

/**
 * Synchronizes CUPS job state with PrintOut jobs.
 * <p>
 * An update-lock is set to prevent that this job and {@link DocLogClean} run at
 * the same time.
 * </p>
 *
 * @author Datraverse B.V.
 */
public final class CupsSyncPrintJobs extends AbstractJob {

    @Override
    protected void onInterrupt() throws UnableToInterruptJobException {
    }

    @Override
    protected void onInit(final JobExecutionContext ctx) {
        ReadWriteLockEnum.DATABASE_READONLY.setReadLock(true);
        ReadWriteLockEnum.PRINT_OUT_HISTORY.setWriteLock(true);    }

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

        /*
         *
         */
        String msg = null;
        PubLevelEnum level = PubLevelEnum.INFO;

        final DaoContext daoContext = ServiceContext.getDaoContext();

        daoContext.beginTransaction();

        boolean rollback = true;

        publisher.publish(PubTopicEnum.CUPS, PubLevelEnum.INFO,
                localizeSysMsg("CupsSyncPrintJobs.start"));

        try {

            final int[] stats = proxyPrintService.syncPrintJobs();

            daoContext.commit();
            rollback = false;

            if (stats[0] > 0) {
                msg =
                        AppLogHelper.logInfo(getClass(),
                                "CupsSyncPrintJobs.success",
                                String.valueOf(stats[0]),
                                String.valueOf(stats[1]),
                                String.valueOf(stats[2]));
            } else {
                msg =
                        localizeSysMsg("CupsSyncPrintJobs.success",
                                String.valueOf(stats[0]),
                                String.valueOf(stats[1]),
                                String.valueOf(stats[2]));
            }

        } catch (Exception e) {

            LoggerFactory.getLogger(this.getClass()).error(e.getMessage(), e);

            level = PubLevelEnum.ERROR;
            msg =
                    AppLogHelper.logError(getClass(),
                            "CupsSyncPrintJobs.error", e.getMessage());
        } finally {

            if (rollback) {
                daoContext.rollback();
            }

        }

        if (msg != null) {
            publisher.publish(PubTopicEnum.CUPS, level, msg);
        }

    }

}
