/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2018 Datraverse B.V.
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
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.dao.enums.AppLogLevelEnum;
import org.savapage.core.services.AtomFeedService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.util.AppLogHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class AtomFeedJob extends AbstractJob {

    /** */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(AtomFeedJob.class);

    @Override
    protected void onInterrupt() throws UnableToInterruptJobException {
        // noop
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
         * Return if this is a scheduled (not a one-shot) job and Admin Atom
         * Feed is DISABLED.
         */
        if (ctx.getJobDetail().getKey().getGroup()
                .equals(SpJobScheduler.JOB_GROUP_SCHEDULED)
                && !cm.isConfigValue(Key.FEED_ATOM_ADMIN_ENABLE)) {
            return;
        }

        String msg = null;
        PubLevelEnum level = PubLevelEnum.INFO;

        try {
            final AtomFeedService service =
                    ServiceContext.getServiceFactory().getAtomFeedService();

            service.refreshAdminFeed();

            msg = AppLogHelper.logInfo(getClass(), "AtomFeed.success");

        } catch (Exception e) {

            level = PubLevelEnum.ERROR;
            LOGGER.error(e.getMessage(), e);

            msg = AppLogHelper.logError(getClass(), "AtomFeed.error",
                    e.getMessage());

            AppLogHelper.log(AppLogLevelEnum.ERROR, msg);
        }

        AdminPublisher.instance().publish(PubTopicEnum.FEED, level, msg);
    }

}
