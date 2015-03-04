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

import java.util.ArrayList;
import java.util.List;

import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.UnableToInterruptJobException;
import org.savapage.core.cometd.AdminPublisher;
import org.savapage.core.cometd.PubLevelEnum;
import org.savapage.core.cometd.PubTopicEnum;
import org.savapage.core.concurrent.ReadWriteLockEnum;
import org.savapage.core.ipp.attribute.IppAttrGroup;
import org.savapage.core.ipp.attribute.IppDictEventNotificationAttr;
import org.savapage.core.ipp.attribute.IppDictOperationAttr;
import org.savapage.core.ipp.encoding.IppDelimiterTag;
import org.savapage.core.ipp.operation.IppStatusCode;
import org.savapage.core.print.proxy.JsonProxyPrintJob;
import org.savapage.core.services.ProxyPrintService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.util.AppLogHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements the ippget polling mechanism to get IPP notifications.
 *
 * @author Datraverse B.V.
 *
 */
public final class IppGetNotifications extends AbstractJob {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(IppGetNotifications.class);

    /**
    *
    */
    private static final ProxyPrintService PROXY_PRINT_SERVICE = ServiceContext
            .getServiceFactory().getProxyPrintService();

    public static final String ATTR_REQUESTING_USER = "req-user";
    public static final String ATTR_SUBSCRIPTION_ID = "subsr-id";

    private String requestingUser;
    private String subscriptionId;

    /**
     * Default to 15 seconds.
     */
    private Integer notifyGetInterval;

    @Override
    protected void onInterrupt() throws UnableToInterruptJobException {
        // noop
    }

    @Override
    protected void onExecute(final JobExecutionContext ctx)
            throws JobExecutionException {

        final JobDataMap map = ctx.getJobDetail().getJobDataMap();

        if (map.containsKey(ATTR_REQUESTING_USER)) {
            requestingUser = map.getString(ATTR_REQUESTING_USER);
        }
        if (map.containsKey(ATTR_SUBSCRIPTION_ID)) {
            subscriptionId = map.getString(ATTR_SUBSCRIPTION_ID);
        }

        try {
            ReadWriteLockEnum.DATABASE_READONLY.setReadLock(true);

            final List<IppAttrGroup> response = new ArrayList<>();

            IppStatusCode statusCode =
                    PROXY_PRINT_SERVICE.getNotifications(requestingUser,
                            subscriptionId, response);

            if (statusCode == IppStatusCode.OK) {

                for (IppAttrGroup group : response) {
                    onGroup(group);
                }
            }

        } catch (Exception e) {

            LoggerFactory.getLogger(this.getClass()).error(e.getMessage(), e);

            final String msg =
                    AppLogHelper.logError(getClass(),
                            "IppGetNotifications.error", e.getMessage());

            AdminPublisher.instance().publish(PubTopicEnum.IPP,
                    PubLevelEnum.ERROR, msg);

        } finally {
            try {
                ReadWriteLockEnum.DATABASE_READONLY.setReadLock(false);
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
    }

    /**
     *
     * @param group
     * @throws Exception
     */
    protected void onGroup(final IppAttrGroup group) throws Exception {

        if (group.getDelimiterTag() == IppDelimiterTag.OPERATION_ATTR) {
            String interval =
                    group.getAttrSingleValue(IppDictOperationAttr.ATTR_NOTIFY_GET_INTERVAL);
            if (interval != null) {
                this.notifyGetInterval = Integer.valueOf(interval, 10);
            }
            return;
        }

        String event =
                group.getAttrSingleValue(IppDictEventNotificationAttr.ATTR_NOTIFY_SUBSCRIBED_EVENT);

        String strJobId =
                group.getAttrSingleValue(IppDictEventNotificationAttr.ATTR_NOTIFY_JOB_ID);

        if (strJobId != null) {

            final Integer jobId = Integer.valueOf(strJobId, 10);

            final String printerName =
                    group.getAttrSingleValue(IppDictEventNotificationAttr.ATTR_PRINTER_NAME);

            /*
             *
             */
            final JsonProxyPrintJob printJob =
                    PROXY_PRINT_SERVICE.retrievePrintJob(printerName, jobId);

            if (printJob == null) {

                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn("Notification of job [" + jobId
                            + "] from printer [" + printerName
                            + "] : job NOT found.");
                }

                final String msg =
                        AppLogHelper.logWarning(getClass(),
                                "IppGetNotifications.warn.job.notfound",
                                strJobId, printerName);

                AdminPublisher.instance().publish(PubTopicEnum.IPP,
                        PubLevelEnum.WARN, msg);

            } else {

                final Integer creation_time = printJob.getCreationTime();
                final Integer completed_time = printJob.getCompletedTime();

                final Integer jobState =
                        Integer.valueOf(
                                group.getAttrSingleValue(IppDictEventNotificationAttr.ATTR_JOB_STATE),
                                10);

                final Integer printer_state =
                        Integer.valueOf(
                                group.getAttrSingleValue(IppDictEventNotificationAttr.ATTR_PRINTER_STATE),
                                10);

                final String jobName =
                        group.getAttrSingleValue(IppDictEventNotificationAttr.ATTR_JOB_NAME);

                PROXY_PRINT_SERVICE.notificationRecipient().onJobEvent(event,
                        jobId, jobName, jobState, creation_time,
                        completed_time, printerName, printer_state);
            }
        }
    }

    @Override
    protected void onInit(final JobExecutionContext ctx) {
    }

    @Override
    protected void onExit(final JobExecutionContext ctx) {

        if (this.notifyGetInterval != null) {

            SpJobScheduler.instance().scheduleOneShotIppNotifications(
                    requestingUser, subscriptionId, this.notifyGetInterval);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Starting again after [" + this.notifyGetInterval
                        + "] seconds");
            }
        }

    }

}
