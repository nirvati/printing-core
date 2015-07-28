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

import org.jivesoftware.smack.XMPPException;
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
import org.savapage.core.print.gcp.GcpAuthException;
import org.savapage.core.print.gcp.GcpClient;
import org.savapage.core.print.gcp.GcpListener;
import org.savapage.core.print.gcp.GcpPrinter;
import org.savapage.core.print.gcp.GcpPrinterNotFoundException;
import org.savapage.core.print.server.DocContentPrintException;
import org.savapage.core.util.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Datraverse B.V.
 *
 */
public final class GcpListenerJob extends AbstractJob implements
        CircuitBreakerOperation {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(GcpListenerJob.class);

    /**
     * Milliseconds to wait before starting this job again.
     */
    private long millisUntilNextInvocation;

    /**
     *
     */
    private GcpListener listener;

    @Override
    protected void onInit(final JobExecutionContext ctx) {
        GcpPrinter.reset();
        GcpPrinter.setOnline(true);
    }

    @Override
    protected void onInterrupt() throws UnableToInterruptJobException {

        LOGGER.debug("Interrupted.");

        if (listener != null) {
            try {
                listener.disconnect();
            } catch (Exception e) {
                LOGGER.error(e.getMessage());
            }
        }
    }

    @Override
    public Object execute(final CircuitBreaker circuitBreaker) {

        try {
            /*
             * Endless loop till exception or expired session...
             */
            while (listen(circuitBreaker))
                ;

        } catch (GcpAuthException e) {

            throw new CircuitDamagingException(
                    "Failed to refresh Access Token (" + e.getMessage() + ")");

        } catch (GcpPrinterNotFoundException e) {

            throw new CircuitDamagingException(
                    localizedMessage("GcpListener.printer-not-found"));

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
            final CircuitBreaker breaker =
                    ConfigManager
                            .getCircuitBreaker(CircuitBreakerEnum.GCP_CONNECTION);

            this.millisUntilNextInvocation = breaker.getMillisUntilRetry();
            breaker.execute(this);
            this.millisUntilNextInvocation = 1000L;

        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }

    }

    /**
     * @param circuitBreaker
     * @throws Exception
     * @return {@code true} when session expired (i.e. was NOT interrupted, e.g.
     *         by setting the GCP Printer OFFLINE).
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
            MessagingException, InterruptedException, DocContentPrintException,
            GcpAuthException {

        /*
         * "As a first step whenever the printer comes online, it should check
         * in with the CloudPrint service and sync the printer's status and
         * capabilities with the listing in the cloud.
         *
         * This way the client does not need to maintain state with regard to
         * what has been registered, and needs to retain only the Auth token
         * provided when the user authenticated."
         *
         * Note: next statement throws an exception when the printer is NOT
         * found.
         */
        GcpPrinter.store(GcpClient.instance().getPrinterDetails());

        /*
         * Start the listener and connect.
         */
        listener = new GcpListener();
        listener.connect();

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(localizeLogMsg("GcpListener.started"));
        }

        AdminPublisher.instance().publish(PubTopicEnum.GCP_PRINT,
                PubLevelEnum.INFO, localizeSysMsg("GcpListener.started"));

        /*
         * At this point we can inform the breaker we are up and running.
         */
        circuitBreaker.closeCircuit();

        /*
         * Initial processing of queued print jobs.
         */
        listener.processQueue();

        /*
         * Start listening ...
         */
        final boolean isExpired =
                listener.listen(
                        GcpPrinter.getAccessTokenExpiry(),
                        ConfigManager.instance().getConfigInt(
                                Key.GCP_EVENT_TIMEOUT_SECS));

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(localizeLogMsg("GcpListener.stopped"));
        }

        AdminPublisher.instance().publish(PubTopicEnum.GCP_PRINT,
                PubLevelEnum.INFO, localizeSysMsg("GcpListener.stopped"));

        return isExpired;
    }

    @Override
    protected void onExit(final JobExecutionContext ctx) {

        GcpPrinter.setOnline(false);

        //
        if (!isInterrupted()
                && ConfigManager.isGcpEnabled()
                && !ConfigManager.getCircuitBreaker(
                        CircuitBreakerEnum.GCP_CONNECTION).isCircuitDamaged()) {

            SpJobScheduler.instance().scheduleOneShotImapListener(
                    this.millisUntilNextInvocation);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Starting again after ["
                        + this.millisUntilNextInvocation + "] milliseconds");
            }
        }

    }

}
