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

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.UnableToInterruptJobException;
import org.savapage.core.cometd.AdminPublisher;
import org.savapage.core.cometd.PubLevelEnum;
import org.savapage.core.cometd.PubTopicEnum;
import org.savapage.core.concurrent.ReadWriteLockEnum;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.dao.DaoContext;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.util.AppLogHelper;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class AppLogClean extends AbstractJob {

    @Override
    protected void onInterrupt() throws UnableToInterruptJobException {
    }

    @Override
    protected void onInit(final JobExecutionContext ctx) {
        ReadWriteLockEnum.DATABASE_READONLY.setReadLock(true);
    }

    @Override
    protected void onExit(final JobExecutionContext ctx) {
        ReadWriteLockEnum.DATABASE_READONLY.setReadLock(false);
    }

    @Override
    public void onExecute(final JobExecutionContext ctx)
            throws JobExecutionException {

        final ConfigManager cm = ConfigManager.instance();

        /*
         * Return if this is a scheduled (not a one-shot) job and DeleteAppLog
         * is DISABLED.
         */
        if (ctx.getJobDetail().getKey().getGroup()
                .equals(SpJobScheduler.JOB_GROUP_SCHEDULED)
                && !cm.isConfigValue(Key.DELETE_APP_LOG)) {
            return;
        }

        /*
         * Return if days LT zero.
         */
        final int daysBackInTime = cm.getConfigInt(Key.DELETE_APP_LOG_DAYS);

        if (daysBackInTime <= 0) {
            return;
        }

        /*
         *
         */
        String msgParm = null;
        PubLevelEnum level = PubLevelEnum.INFO;
        int nDeleted = 0;

        final DaoContext daoContext = ServiceContext.getDaoContext();

        try {
            daoContext.beginTransaction();

            nDeleted = ServiceContext.getDaoContext().getAppLogDao()
                    .clean(daysBackInTime);

            if (nDeleted > 0) {
                msgParm = String.valueOf(nDeleted);
            }

            daoContext.commit();

        } catch (Exception e) {

            daoContext.rollback();

            level = PubLevelEnum.ERROR;
            msgParm = e.getMessage();

            LoggerFactory.getLogger(this.getClass()).error(e.getMessage(), e);
        }

        try {

            if (msgParm != null) {

                String msg = null;

                if (level == PubLevelEnum.INFO) {
                    if (nDeleted == 1) {
                        msg = AppLogHelper.logInfo(getClass(),
                                "AppLogClean.success.single");
                    } else {
                        msg = AppLogHelper.logInfo(getClass(),
                                "AppLogClean.success.plural", msgParm);
                    }

                } else {
                    msg = AppLogHelper.logError(getClass(), "AppLogClean.error",
                            msgParm);
                }

                AdminPublisher.instance().publish(PubTopicEnum.DB, level, msg);
            }

        } catch (Exception e) {
            // no code intended
        }
    }

}
