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

import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.UnableToInterruptJobException;
import org.savapage.core.cometd.AdminPublisher;
import org.savapage.core.cometd.PubLevelEnum;
import org.savapage.core.cometd.PubTopicEnum;
import org.savapage.core.print.gcp.GcpAuthException;
import org.savapage.core.print.gcp.GcpClient;
import org.savapage.core.print.gcp.GcpGetAuthTokensRsp;
import org.savapage.core.print.gcp.GcpPollForAuthCodeRsp;
import org.savapage.core.print.gcp.GcpPrinter;
import org.savapage.core.print.gcp.GcpRegisterPrinterRsp;
import org.savapage.core.util.AppLogHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Datraverse B.V.
 *
 */
public final class GcpRegisterJob extends AbstractJob {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(GcpRegisterJob.class);

    public static final String KEY_POLLING_URL =
            GcpRegisterPrinterRsp.KEY_POLLING_URL;

    public static final String KEY_TOKEN_DURATION =
            GcpRegisterPrinterRsp.KEY_TOKEN_DURATION;

    @Override
    protected void onInit(final JobExecutionContext ctx) {
    }

    @Override
    protected void onInterrupt() throws UnableToInterruptJobException {
        LOGGER.debug("interrupt...");
    }

    @Override
    protected void onExecute(final JobExecutionContext ctx)
            throws JobExecutionException {

        AdminPublisher adminPub = null;

        try {

            GcpPrinter.setOnline(false);

            adminPub = AdminPublisher.instance();

            final JobDataMap map = ctx.getJobDetail().getJobDataMap();

            final String pollingUrl = map.getString(KEY_POLLING_URL);
            final Integer tokenDuration = map.getIntValue(KEY_TOKEN_DURATION);

            adminPub.publish(PubTopicEnum.GCP_REGISTER, PubLevelEnum.INFO,
                    localizeSysMsg("GcpPollForAuthCode.started"));

            final GcpClient gcp = GcpClient.instance();

            long now = System.currentTimeMillis();
            final long maxTime = now + (tokenDuration * 1000);

            /*
             * https://developers.google.com/cloud-print/docs/devguide#
             * authcodeobtain
             *
             * "Polling should not be more frequent than once every 30 seconds."
             */
            final long pollFrequency = 30 * 1000;

            /*
             * Give user some time to react...
             */
            final long initialWait = 10 * 1000;

            GcpPollForAuthCodeRsp pollRsp = null;

            /*
             * STEP #1: Poll for the authentication code (user claimed the
             * printer).
             */
            long sleep = initialWait;

            while (now < maxTime) {
                /*
                 * Start with a sleep to give user some time to react.
                 */
                Thread.sleep(sleep);

                pollRsp = gcp.pollForAuthCode(pollingUrl);

                if (pollRsp.isSuccess()) {
                    break;
                }
                /*
                 * next try ...
                 */
                sleep = pollFrequency;
                pollRsp = null;
                now = System.currentTimeMillis();
            }

            if (pollRsp != null && pollRsp.isSuccess()) {

                adminPub.publish(PubTopicEnum.GCP_REGISTER, PubLevelEnum.INFO,
                        localizeSysMsg("GcpPollForAuthCode.success"));

                GcpPrinter.store(pollRsp);

                /*
                 * STEP #2: Get the Refresh and Access tokens.
                 */
                GcpGetAuthTokensRsp authTokensRsp =
                        gcp.getAuthTokens(pollRsp.getAuthorizationCode());

                adminPub.publish(PubTopicEnum.GCP_REGISTER, PubLevelEnum.INFO,
                        localizeSysMsg("GcpGetAuthTokens.success"));
                /*
                 * Note: next statement throws an exception when authentication
                 * failed.
                 */
                GcpPrinter.store(authTokensRsp);

            } else {
                adminPub.publish(PubTopicEnum.GCP_REGISTER, PubLevelEnum.WARN,
                        localizeSysMsg("GcpPollForAuthCode.timeout"));
            }

        } catch (GcpAuthException e) {

            final String msg =
                    AppLogHelper.logError(getClass(), "GcpRegister.error",
                            "Failed to get Auth Tokens (" + e.getMessage()
                                    + ")");

            adminPub.publish(PubTopicEnum.GCP_REGISTER, PubLevelEnum.ERROR, msg);

        } catch (Exception e) {

            LoggerFactory.getLogger(this.getClass()).error(e.getMessage(), e);

            final String msg =
                    AppLogHelper.logError(getClass(), "GcpRegister.error",
                            e.getMessage());

            if (adminPub != null) {
                adminPub.publish(PubTopicEnum.GCP_REGISTER, PubLevelEnum.ERROR,
                        msg);
            }

        }
    }

    @Override
    protected void onExit(final JobExecutionContext ctx) {
    }

}
