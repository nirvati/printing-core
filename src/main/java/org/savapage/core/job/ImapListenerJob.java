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

import java.io.IOException;

import javax.mail.MessagingException;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.UnableToInterruptJobException;
import org.savapage.core.circuitbreaker.CircuitBreaker;
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
import org.savapage.core.print.imap.ImapListener;
import org.savapage.core.util.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Datraverse B.V.
 *
 */
public final class ImapListenerJob extends AbstractJob {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(ImapListenerJob.class);

    private MailPrintCircuitOperation circuitOperation = null;

    /**
     * Milliseconds to wait before starting this job again.
     */
    private long millisUntilNextInvocation;

    /**
     *
     * @author Datraverse B.V.
     *
     */
    private static class MailPrintCircuitOperation implements
            CircuitBreakerOperation {

        private final ImapListenerJob parentJob;

        private ImapListener listener = null;

        /**
         *
         * @param theJob
         */
        public MailPrintCircuitOperation(final ImapListenerJob parentJob) {
            this.parentJob = parentJob;
        }

        @Override
        public Object execute(final CircuitBreaker circuitBreaker) {

            try {
                final ConfigManager cm = ConfigManager.instance();

                final String username =
                        cm.getConfigValue(Key.PRINT_IMAP_USER_NAME);
                final String password =
                        cm.getConfigValue(Key.PRINT_IMAP_PASSWORD);

                final int sessionHeartbeatSecs =
                        cm.getConfigInt(Key.PRINT_IMAP_SESSION_HEARTBEAT_SECS);
                final int sessionDurationSecs =
                        cm.getConfigInt(Key.PRINT_IMAP_SESSION_DURATION_SECS);

                /*
                 *
                 */
                this.listener = new ImapListener(cm);

                this.listener.connect(username, password);

                AdminPublisher.instance().publish(PubTopicEnum.MAILPRINT,
                        PubLevelEnum.INFO,
                        this.parentJob.localizeMsg("ImapListener.started"));

                this.listener.processInbox();

                /*
                 * At this point we can inform the breaker we are up and
                 * running.
                 */
                circuitBreaker.closeCircuit();

                /*
                 * Blocking...
                 */
                this.listener.listen(sessionHeartbeatSecs, sessionDurationSecs);

                if (!this.listener.isIdleSupported()) {

                    final String msg =
                            localizedMessage("ImapListener.idle-not-supported",
                                    this.listener.getHost());

                    throw new CircuitDamagingException(msg);
                }

            } catch (MessagingException | IOException e) {

                throw new CircuitTrippingException(e);

            } catch (InterruptedException e) {

                throw new CircuitNonTrippingException(e);

            } catch (CircuitDamagingException e) {

                throw e;

            } catch (Exception t) {

                throw new CircuitDamagingException(t);

            } finally {

                if (this.listener != null) {
                    try {
                        this.listener.disconnect();
                    } catch (Exception e) {
                        LOGGER.error(e.getMessage(), e);
                    }
                }
            }
            //
            return null;
        }

        /**
         *
         * @param messageKey
         * @param args
         * @return
         */
        private String localizedMessage(String messageKey, String... args) {
            return Messages.getMessage(this.parentJob.getClass(),
                    ConfigManager.getDefaultLocale(), messageKey, args);
        }

        /**
         * @throws InterruptedException
         * @throws MessagingException
         *
         */
        public void onInterrupt() throws MessagingException,
                InterruptedException {
            if (this.listener != null) {
                this.listener.disconnect();
            }

        }
    }

    @Override
    protected void onInit(final JobExecutionContext ctx) {
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

    @Override
    protected void onExecute(final JobExecutionContext ctx)
            throws JobExecutionException {

        try {
            final CircuitBreaker breaker =
                    ConfigManager
                            .getCircuitBreaker(CircuitBreakerEnum.MAILPRINT_CONNECTION);

            this.circuitOperation = new MailPrintCircuitOperation(this);

            this.millisUntilNextInvocation = breaker.getMillisUntilRetry();
            breaker.execute(this.circuitOperation);
            this.millisUntilNextInvocation = 1000L;

        } catch (Exception t) {
            // noop
        }

    }

    @Override
    protected void onExit(final JobExecutionContext ctx) {

        AdminPublisher.instance().publish(PubTopicEnum.MAILPRINT,
                PubLevelEnum.INFO, localizeMsg("ImapListener.stopped"));

        if (!isInterrupted()
                && ConfigManager.isPrintImapEnabled()
                && !ConfigManager.getCircuitBreaker(
                        CircuitBreakerEnum.MAILPRINT_CONNECTION)
                        .isCircuitDamaged()) {

            SpJobScheduler.instance().scheduleOneShotImapListener(
                    this.millisUntilNextInvocation);

            LOGGER.debug("Starting again after ["
                    + this.millisUntilNextInvocation + "] milliseconds");

        }

    }

}
