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

import java.io.IOException;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.Locale;

import javax.mail.MessagingException;

import org.jivesoftware.smack.XMPPException;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.UnableToInterruptJobException;
import org.savapage.core.SpException;
import org.savapage.core.circuitbreaker.CircuitBreaker;
import org.savapage.core.circuitbreaker.CircuitBreakerException;
import org.savapage.core.circuitbreaker.CircuitBreakerOperation;
import org.savapage.core.circuitbreaker.CircuitDamagingException;
import org.savapage.core.circuitbreaker.CircuitNonTrippingException;
import org.savapage.core.circuitbreaker.CircuitTrippingException;
import org.savapage.core.cometd.AdminPublisher;
import org.savapage.core.cometd.PubLevelEnum;
import org.savapage.core.cometd.PubTopicEnum;
import org.savapage.core.config.CircuitBreakerEnum;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.print.gcp.GcpAuthException;
import org.savapage.core.print.gcp.GcpClient;
import org.savapage.core.print.gcp.GcpListener;
import org.savapage.core.print.gcp.GcpPrinter;
import org.savapage.core.print.gcp.GcpPrinterNotFoundException;
import org.savapage.core.print.server.DocContentPrintException;
import org.savapage.core.util.BigDecimalUtil;
import org.savapage.core.util.DateUtil;
import org.savapage.core.util.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class GcpListenerJob extends AbstractJob {

    /**
     * The logger.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(GcpListenerJob.class);

    /**
     * Number of seconds after restarting this job after an exception occurs.
     */
    private static final int RESTART_SECS_AFTER_EXCEPTION = 60;

    /**
     * Milliseconds to wait before starting this job again.
     */
    private long millisUntilNextInvocation;

    /**
     * The {@link CircuitBreaker}.
     */
    private CircuitBreaker breaker;

    /**
     * .
     */
    private GcpCircuitOperation circuitOperation = null;

    /**
     *
     * @author Datraverse B.V.
     *
     */
    private static class GcpCircuitOperation
            implements CircuitBreakerOperation {

        /**
         *
         */
        private final GcpListenerJob parentJob;

        /**
         * .
         */
        private GcpListener listener;

        /**
         *
         * @param theJob
         */
        public GcpCircuitOperation(final GcpListenerJob parentJob) {
            this.parentJob = parentJob;
        }

        @Override
        public Object execute(CircuitBreaker circuitBreaker) {

            try {

                this.listen(circuitBreaker);

            } catch (GcpAuthException e) {

                throw new CircuitDamagingException(
                        "Failed to refresh Access Token (" + e.getMessage()
                                + ")");

            } catch (GcpPrinterNotFoundException e) {

                throw new CircuitDamagingException(this.parentJob
                        .localizedMessage("GcpListener.printer-not-found"));

            } catch (MessagingException | IOException | XMPPException e) {

                GcpPrinter.reset();

                throw new CircuitTrippingException(e);

            } catch (InterruptedException e) {

                throw new CircuitNonTrippingException(e);

            } catch (Exception t) {

                throw new CircuitDamagingException(t);

            }

            return null;
        }

        /**
         * @param circuitBreaker
         * @throws Exception
         * @return {@code true} when session expired (i.e. was NOT interrupted,
         *         e.g. by setting the GCP Printer OFFLINE).
         * @throws IOException
         * @throws GcpPrinterNotFoundException
         * @throws XMPPException
         * @throws MessagingException
         * @throws InterruptedException
         * @throws DocContentPrintException
         * @throws GcpAuthException
         */
        private boolean listen(final CircuitBreaker circuitBreaker)
                throws IOException, GcpPrinterNotFoundException, XMPPException,
                MessagingException, InterruptedException,
                DocContentPrintException, GcpAuthException {

            /*
             * "As a first step whenever the printer comes online, it should
             * check in with the CloudPrint service and sync the printer's
             * status and capabilities with the listing in the cloud.
             *
             * This way the client does not need to maintain state with regard
             * to what has been registered, and needs to retain only the Auth
             * token provided when the user authenticated."
             *
             * Note: next statement throws an exception when the printer is NOT
             * found.
             */
            GcpPrinter.store(GcpClient.instance().getPrinterDetails());

            /*
             * Start the listener and connect.
             */
            this.listener = new GcpListener();
            this.listener.connect();

            /*
             * At this point we can inform the breaker we are up and running.
             */
            circuitBreaker.closeCircuit();

            /*
             * Initial processing: catch up with queued print jobs.
             */
            this.listener.processQueue();

            /*
             * Logging and messages
             */
            final Date tokenExpiryDate = GcpPrinter.getAccessTokenExpiry();

            final String restartTime = DateFormat
                    .getTimeInstance(DateFormat.SHORT, Locale.getDefault())
                    .format(tokenExpiryDate);

            if (LOGGER.isInfoEnabled()) {
                LOGGER.info(this.parentJob.localizeLogMsg("GcpListener.started",
                        restartTime));
            }

            AdminPublisher.instance().publish(PubTopicEnum.GCP_PRINT,
                    PubLevelEnum.INFO, this.parentJob.localizeSysMsg(
                            "GcpListener.started", restartTime));

            /*
             * Start listening ...
             */
            return listener.listen(tokenExpiryDate, ConfigManager.instance()
                    .getConfigInt(Key.GCP_EVENT_TIMEOUT_SECS));
        }

        /**
         * @throws InterruptedException
         */
        public void onInterrupt() throws InterruptedException {
            if (this.listener != null) {
                this.listener.disconnect();
            }
        }

    }

    @Override
    protected void onInit(final JobExecutionContext ctx) {

        this.breaker = ConfigManager
                .getCircuitBreaker(CircuitBreakerEnum.GCP_CONNECTION);

        GcpPrinter.reset();
        GcpPrinter.setOnline(true);
    }

    @Override
    protected void onInterrupt() throws UnableToInterruptJobException {

        LOGGER.debug("Interrupted.");

        if (this.circuitOperation != null) {
            try {
                this.circuitOperation.onInterrupt();
            } catch (Exception e) {
                LOGGER.error(e.getMessage());
            }
        }
    }

    /**
     *
     * @param messageKey
     *            The message key.
     * @param args
     *            The arguments.
     * @return The localized message.
     */
    private String localizedMessage(final String messageKey,
            final String... args) {
        return Messages.getMessage(this.getClass(),
                ConfigManager.getDefaultLocale(), messageKey, args);
    }

    @Override
    protected void onExecute(final JobExecutionContext ctx)
            throws JobExecutionException {

        try {
            this.circuitOperation = new GcpCircuitOperation(this);
            this.breaker.execute(this.circuitOperation);
            this.millisUntilNextInvocation = 1 * DateUtil.DURATION_MSEC_SECOND;

        } catch (CircuitBreakerException t) {

            this.millisUntilNextInvocation = this.breaker.getMillisUntilRetry();

        } catch (Exception t) {

            this.millisUntilNextInvocation = RESTART_SECS_AFTER_EXCEPTION
                    * DateUtil.DURATION_MSEC_SECOND;

            AdminPublisher.instance().publish(PubTopicEnum.GCP_PRINT,
                    PubLevelEnum.ERROR,
                    localizeSysMsg("GcpListener.error", t.getMessage()));

            LOGGER.error(t.getMessage(), t);
        }

    }

    @Override
    protected void onExit(final JobExecutionContext ctx) {

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(localizeLogMsg("GcpListener.stopped"));
        }

        GcpPrinter.setOnline(false);

        final AdminPublisher publisher = AdminPublisher.instance();

        if (this.isInterrupted() || !ConfigManager.isGcpEnabled()) {

            publisher.publish(PubTopicEnum.GCP_PRINT, PubLevelEnum.INFO,
                    localizeSysMsg("GcpListener.stopped"));

        } else if (this.breaker.isCircuitDamaged()) {

            publisher.publish(PubTopicEnum.GCP_PRINT, PubLevelEnum.ERROR,
                    localizeSysMsg("GcpListener.stopped"));

        } else {

            final PubLevelEnum pubLevel;
            final String pubMsg;

            if (this.breaker.isCircuitClosed()) {
                pubLevel = PubLevelEnum.INFO;
            } else {
                pubLevel = PubLevelEnum.WARN;
                this.millisUntilNextInvocation =
                        this.breaker.getMillisUntilRetry();
            }

            if (this.millisUntilNextInvocation > DateUtil.DURATION_MSEC_SECOND) {

                try {

                    final double seconds =
                            (double) this.millisUntilNextInvocation
                                    / DateUtil.DURATION_MSEC_SECOND;

                    pubMsg = localizeSysMsg("GcpListener.restart",
                            BigDecimalUtil.localize(BigDecimal.valueOf(seconds),
                                    Locale.getDefault(), false));
                } catch (ParseException e) {
                    throw new SpException(e.getMessage());
                }

            } else {
                pubMsg = localizeSysMsg("GcpListener.stopped");
            }

            publisher.publish(PubTopicEnum.GCP_PRINT, pubLevel, pubMsg);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Starting again after ["
                        + this.millisUntilNextInvocation + "] milliseconds");
            }

            SpJobScheduler.instance()
                    .scheduleOneShotGcpListener(this.millisUntilNextInvocation);
        }

    }

}
