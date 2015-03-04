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
import org.savapage.core.services.PrinterGroupService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.util.AppLogHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Removes PrinterGroup that are not in use anymore.
 *
 * @author Datraverse B.V.
 */
public final class PrinterGroupClean extends AbstractJob {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(PrinterGroupClean.class);

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
    public void onExecute(final JobExecutionContext ctx)
            throws JobExecutionException {

        String msgParm = null;
        PubLevelEnum level = PubLevelEnum.INFO;
        int nRemoved = 0;

        final DaoContext daoContext = ServiceContext.getDaoContext();

        try {
            daoContext.beginTransaction();

            PrinterGroupService service =
                    ServiceContext.getServiceFactory().getPrinterGroupService();

            nRemoved = service.prunePrinterGroups();

            if (nRemoved > 0) {
                msgParm = String.valueOf(nRemoved);
            }

            daoContext.commit();

        } catch (Exception e) {

            daoContext.rollback();

            level = PubLevelEnum.ERROR;
            msgParm = e.getMessage();

            LoggerFactory.getLogger(this.getClass()).error(e.getMessage(), e);

        }

        /*
         *
         */
        try {

            if (msgParm != null) {

                String msg = null;

                if (level == PubLevelEnum.INFO) {
                    if (nRemoved == 1) {
                        msg =
                                AppLogHelper.logInfo(getClass(),
                                        "PrinterGroupClean.success.single");
                    } else {
                        msg =
                                AppLogHelper.logInfo(getClass(),
                                        "PrinterGroupClean.success.plural",
                                        msgParm);
                    }

                } else {
                    msg =
                            AppLogHelper.logError(getClass(),
                                    "PrinterGroupClean.error", msgParm);
                }

                AdminPublisher.instance().publish(PubTopicEnum.DB, level, msg);
            }

        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

}
