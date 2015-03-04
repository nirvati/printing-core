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
import org.savapage.core.config.ConfigManager;
import org.savapage.core.jpa.tools.DbTools;
import org.savapage.core.util.AppLogHelper;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Datraverse B.V.
 */
public final class DbDerbyOptimize extends AbstractJob {

    @Override
    protected void onInterrupt() throws UnableToInterruptJobException {
        // noop
    }

    @Override
    protected void onInit(final JobExecutionContext ctx) {
        ReadWriteLockEnum.DATABASE_READONLY.setWriteLock(true);
    }

    @Override
    protected void onExit(final JobExecutionContext ctx) {
        ReadWriteLockEnum.DATABASE_READONLY.setWriteLock(false);
    }

    @Override
    protected void onExecute(final JobExecutionContext ctx)
            throws JobExecutionException {

        if (!ConfigManager.isDbInternal()) {
            return;
        }

        final AdminPublisher publisher = AdminPublisher.instance();

        /*
         *
         */
        publisher.publish(PubTopicEnum.DB, PubLevelEnum.INFO,
                localizeMsg("DbDerbyOptimize.start"));

        String msg = null;
        PubLevelEnum level = PubLevelEnum.INFO;

        try {

            DbTools.optimizeDbInternal();
            msg = AppLogHelper.logInfo(getClass(), "DbDerbyOptimize.success");

        } catch (Exception e) {

            LoggerFactory.getLogger(this.getClass()).error(e.getMessage(), e);

            level = PubLevelEnum.ERROR;
            msg =
                    AppLogHelper.logError(getClass(), "DbDerbyOptimize.error",
                            e.getMessage());
        }

        publisher.publish(PubTopicEnum.DB, level, msg);

    }

}
