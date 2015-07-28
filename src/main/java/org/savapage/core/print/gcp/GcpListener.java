/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2015 Datraverse B.V.
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
package org.savapage.core.print.gcp;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.mail.MessagingException;

import org.apache.commons.lang3.time.DateUtils;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.packet.Message.Type;
import org.jivesoftware.smack.packet.Packet;
import org.savapage.core.SpException;
import org.savapage.core.cometd.AdminPublisher;
import org.savapage.core.cometd.PubLevelEnum;
import org.savapage.core.cometd.PubTopicEnum;
import org.savapage.core.community.CommunityDictEnum;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.job.AbstractJob;
import org.savapage.core.job.GcpListenerJob;
import org.savapage.core.jpa.User;
import org.savapage.core.print.server.DocContentPrintException;
import org.savapage.core.print.server.PrintInResultEnum;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.UserService;
import org.savapage.core.services.helpers.EmailMsgParms;
import org.savapage.core.util.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listens to Google Cloud Printer for incoming print jobs, and processes these
 * jobs.
 *
 * @author Datraverse B.V.
 *
 */
public final class GcpListener {

    private static final String XMPP_SERVICENAME = "gmail.com";

    private static final Logger LOGGER = LoggerFactory
            .getLogger(GcpListener.class);

    private XMPPConnection xmppConnection = null;
    private PacketCollector jobCollector;

    /**
     * See <a href=
     * "http://stackoverflow.com/questions/3786825/java-volatile-boolean-vs-atomicboolean"
     * >this link</a> on volatile booleans.
     * <p>
     * "... use volatile fields when said field is ONLY UPDATED by its owner
     * thread and the value is only read by other threads ... you can think of
     * it as a publish/subscribe scenario where there are many observers but
     * only one publisher ...
     * </p>
     * <p>
     * "... However if those observers must perform some logic based on the
     * value of the field and then push back a new value then ... go with
     * Atomic* vars or locks or synchronized blocks .... In many concurrent
     * scenarios it boils down to get the value, compare it with another one and
     * update if necessary, hence the compareAndSet and getAndSet methods
     * present in the Atomic* classes."
     * </p>
     */
    private volatile boolean isProcessing = false;

    /**
     * Connects to XMPP with Jabber ID.
     * <p>
     * See <a
     * href="https://developers.google.com/cloud-print/docs/rawxmpp">https://
     * developers.google.com/cloud-print/docs/rawxmpp"</a>.
     * </p>
     * <p>
     * Printer status (online/offline) is detected through established XMPP
     * connection. If printer has no XMPP connection to the server, it's status
     * is offline.
     * </p>
     * <p>
     * Note: I have not yet discovered how to set a timeout on the
     * XMPPConnection.connect(). This connect can take up to 3 minutes when
     * internet connection is lost :-(
     * </p>
     * <p>
     * <i>As a workaround an HTTP (as opposed to HTTPS) connect on URL of the
     * XMPP server is used as a probe to check if the server can be reached.</i>
     * <p>
     *
     * @throws XMPPException
     * @throws IOException
     * @throws GcpAuthException
     */
    public void connect() throws XMPPException, IOException, GcpAuthException {
        /*
         * FULL_JID is the full XMPP JID (e.g. username@gmail.com/canon234),
         * BARE_JID is the bare XMPP JID (e.g. username@gmail.com).
         */
        final String bareJid = GcpPrinter.getOwnerId();
        final String googleId = GcpPrinter.getGcpXmppJid();
        final String accessToken = GcpClient.instance().getAccessToken();
        final String resourceName = bareJid + "/" + GcpPrinter.getProxyId();

        /*
         *
         */
        ConnectionConfiguration connConfig =
                new ConnectionConfiguration(XMPP_SERVICENAME);

        connConfig.setRosterLoadedAtLogin(false);
        connConfig.setSendPresence(false);
        connConfig.setReconnectionAllowed(false);

        xmppConnection = new XMPPConnection(connConfig);

        /*
         * Register OAuth2 support for XMPP authentication.
         */
        SASLAuthentication.registerSASLMechanism("X-OAUTH2",
                GoogleOAuth2SASLMechanism.class);
        SASLAuthentication.supportSASLMechanism("X-OAUTH2", 0);

        /*
         * Timeout on XMPPConnection.connect() cannot be set (?)
         *
         * Workaround: HTTP (as opposed to HTTPS) connect on URL of the XMPP
         * server is used as a probe to check if the server can be reached.
         */
        final String urlSpec =
                "http://" + xmppConnection.getHost() + ":"
                        + xmppConnection.getPort();
        final URL url = new URL(urlSpec);

        final HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setConnectTimeout(ConfigManager.instance().getConfigInt(
                Key.GC_CONNECT_TIMEOUT_SECS) * 1000);
        con.connect();
        con.disconnect();

        /*
         * Connect.
         */
        LOGGER.debug("Connecting...");
        xmppConnection.connect();
        LOGGER.debug("Connected.");

        /*
         * Login.
         */
        LOGGER.debug("Login...");
        xmppConnection.login(googleId, accessToken, resourceName);
        LOGGER.debug("Logged in.");

        /*
         * Subscribe to Google Cloud Print notifications
         */
        xmppConnection.sendPacket(new GcpSubscriptionIQ(googleId));

        /*
         * Subscription successful?
         */
        if (xmppConnection.isAuthenticated()) {
            LOGGER.debug("Subscribed to Print notifications.");
        } else {
            throw new SpException("Authenticated failed.");
        }

        /*
         * Setup a packet collector for incoming job notifications.
         */
        this.jobCollector =
                xmppConnection.createPacketCollector(new MessageTypeFilter(
                        Type.normal));

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Created packet collector for incoming"
                    + " job notifications.");
        }

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Connection is ready to use.");
        }
    }

    /**
     * Processes all jobs on the Google Cloud Print queue.
     * <p>
     * <b>INVARIANT</b>: this object must be connected as in the
     * {@link #connect()} method.
     *
     * @throws IOException
     * @throws MessagingException
     * @throws GcpPrinterNotFoundException
     * @throws GcpAuthException
     *
     * @throws Exception
     */
    public void processQueue() throws IOException, MessagingException,
            GcpPrinterNotFoundException, GcpAuthException {

        GcpFetchRsp rsp = GcpClient.instance().fetchJobs();

        if (rsp.isSuccess()) {
            for (GcpJob job : rsp.getJobs()) {
                processJob(job);
            }
        } else {
            /*
             * A logical failure is ambiguous: either there are NO print jobs,
             * OR the GCP printer has been removed.
             *
             * Check by getting AND storing the printer details.
             *
             * An exception is thrown when no printer is found.
             */
            GcpPrinter.store(GcpClient.instance().getPrinterDetails());
        }
    }

    /**
     * Processes a job.
     *
     * @param job
     * @throws IOException
     * @throws MessagingException
     * @throws GcpAuthException
     * @throws Exception
     */
    private void processJob(final GcpJob job) throws IOException,
            GcpAuthException {

        final String emailFrom = job.getOwnerId();

        if (LOGGER.isInfoEnabled()) {
            String info = "Google Cloud Print Job";
            info += "\n\tFrom         : " + emailFrom;
            info += "\n\tTitle        : " + job.getTitle();
            info += "\n\tContent-Type : " + job.getContentType();
            info += "\n\tPages        : " + job.getNumberOfPages();
            LOGGER.info(info);
        }

        GcpClient gcp = GcpClient.instance();

        UserService service =
                ServiceContext.getServiceFactory().getUserService();

        final User user = service.findUserByEmail(emailFrom);

        GcpControlRsp rsp = null;
        String nextJobStatus = null;

        if (user == null) {

            nextJobStatus = "CANCELLED";

            String msg = localize("gcp-user-not-found", emailFrom);

            AdminPublisher.instance().publish(PubTopicEnum.GCP_PRINT,
                    PubLevelEnum.INFO, msg);
            LOGGER.debug(msg);

            rsp = gcp.controlJobCancel(job.getId());

            /*
             *
             */
            final ConfigManager cm = ConfigManager.instance();
            final String subject =
                    cm.getConfigValue(Key.GCP_JOB_OWNER_UNKNOWN_CANCEL_MAIL_SUBJECT);
            final String body =
                    cm.getConfigValue(Key.GCP_JOB_OWNER_UNKNOWN_CANCEL_MAIL_BODY);

            this.sendEmail(emailFrom, subject, body);

        } else {

            PrintInResultEnum printResult;

            try {
                printResult = gcp.printJob(user, job);

                switch (printResult) {

                case OK:
                    nextJobStatus = "DONE";
                    rsp = gcp.controlJobDone(job.getId());
                    break;

                case USER_NOT_AUTHORIZED:
                    // no break intended
                default:

                    nextJobStatus = "CANCELLED";

                    String msg =
                            localize("gcp-user-not-authorized", emailFrom,
                                    user.getUserId());

                    AdminPublisher.instance().publish(PubTopicEnum.GCP_PRINT,
                            PubLevelEnum.INFO, msg);
                    LOGGER.debug(msg);

                    rsp = gcp.controlJobCancel(job.getId());

                    break;
                }

            } catch (DocContentPrintException e) {

                final String rejectedReason = e.getMessage();

                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("File [" + job.getTitle()
                            + "] rejected. Reason: " + rejectedReason);
                }

                final String subject =
                        CommunityDictEnum.SAVAPAGE.getWord()
                                + " Google Cloud Print: rejected file ["
                                + job.getTitle() + "]";
                final String body = "Reason: " + rejectedReason;

                this.sendEmail(emailFrom, subject, body);

            }

        }

        if (rsp != null && !rsp.isSuccess()) {
            throw new SpException(
                    "Failed to set Google Clound Print Job Status ("
                            + nextJobStatus + ")");
        }

    }

    /**
     * Sends an email.
     *
     * @param toAddress
     *            The email address.
     * @param subject
     *            The subject of the message.
     * @param body
     *            The body text with optional newline {@code \n} characters.
     */
    private void sendEmail(final String toAddress, final String subject,
            final String body) {

        try {

            final EmailMsgParms emailParms = new EmailMsgParms();

            emailParms.setToAddress(toAddress);
            emailParms.setSubject(subject);
            emailParms.setBody(body);

            ServiceContext.getServiceFactory().getEmailService()
                    .writeEmail(emailParms);

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Sent email to [" + toAddress + "] subject ["
                        + subject + "]");
            }

        } catch (MessagingException | IOException e) {
            LOGGER.error("Sending email to [" + toAddress + "] failed: "
                    + e.getMessage());
        }
    }

    /**
     * Tells whether an unrecoverable error occurred while using this object.
     *
     * @return {@link true} when an unrecoverable error occurred.
     */
    public boolean hasUnrecoverableError() {
        return false;
    }

    /**
     * Listens to incoming print jobs till dateMax or till printer is put
     * off-line.
     * <p>
     * <b>Note</b>: This is a blocking call that returns when the maximum
     * duration is reached. A {@link #disconnect()} is called before returning.
     * </p>
     *
     * @param maxDateSnapshot
     *            The timeout date/time after which this method returns.
     * @param waitForEventTimeoutSecs
     *            The interval in seconds to wait for GCP (printer) events
     *            within the session.
     * @return {@code true} when session is expired (maxDate was reached).
     * @throws InterruptedException
     * @throws MessagingException
     * @throws IOException
     * @throws GcpPrinterNotFoundException
     * @throws GcpAuthException
     */
    public boolean listen(final Date maxDate, int waitForEventTimeoutSecs)
            throws InterruptedException, IOException, MessagingException,
            GcpPrinterNotFoundException, GcpAuthException {

        /*
         * Deep copy !!
         */
        final Date maxDateSnapshot = new Date(maxDate.getTime());

        final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
        final long nextResultTimeOut = waitForEventTimeoutSecs * 1000;

        boolean isExpired = false;

        try {

            int nInterval = 0;

            final long maxTime = maxDateSnapshot.getTime();

            while (xmppConnection != null) {

                if (Thread.interrupted()) {
                    break;
                }

                /*
                 * INVARIANT: Google Cloud Print MUST be online.
                 */
                if (!GcpPrinter.isOnline()) {
                    LOGGER.trace("Google Cloud Printer is offline.");
                    break;
                }

                final Date now = new Date();

                /*
                 * INVARIANT: Work within session time limit (The Access Token
                 * the session was opened with MUST NOT be expired).
                 */
                if (now.getTime() > maxTime) {
                    isExpired = true;
                    break;
                }

                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Waiting ["
                            + (++nInterval)
                            + "] next ["
                            + dateFormat.format(DateUtils.addSeconds(now,
                                    waitForEventTimeoutSecs)) + "] till ["
                            + dateFormat.format(maxDateSnapshot) + "] ...");
                }
                /*
                 * Wait for Print Job to arrives or ... ?
                 */
                final Packet p = jobCollector.nextResult(nextResultTimeOut);

                if (p != null) {
                    onNotification(p);
                }

                /*
                 * Wait for processing to finish.
                 */
                waitForProcessing(1000L);
            }

        } finally {
            disconnect();
        }

        return isExpired;
    }

    /**
     * 2013-12-18 14:41:59,894 XMPP notification:
     *
     * <pre>
     * <message to="e486ab5d96b0ba4ffa098834fc8785d2@cloudprint.googleusercontent.com/datradrive@gmail.com/6d213a48-4a5c-4941-87f8-853915fdac48" from="cloudprint.google.com"><push xmlns="google:push"><data>NjFhZjVkMzEtZTA4YS0xZjczLWQzOTItNWVmYmVhN2Q1NGFm</data></push></message>
     * </pre>
     *
     * 2013-12-18 14:43:14,483 XMPP notification:
     *
     * <pre>
     * <message to="e486ab5d96b0ba4ffa098834fc8785d2@cloudprint.googleusercontent.com/datradrive@gmail.com/6d213a48-4a5c-4941-87f8-853915fdac48" from="cloudprint.google.com"><push xmlns="google:push"><data>NjFhZjVkMzEtZTA4YS0xZjczLWQzOTItNWVmYmVhN2Q1NGFm</data></push></message>
     * </pre>
     *
     * @param p
     * @throws MessagingException
     * @throws IOException
     * @throws GcpPrinterNotFoundException
     * @throws GcpAuthException
     * @throws Exception
     */
    public void onNotification(Packet p) throws IOException,
            MessagingException, GcpPrinterNotFoundException, GcpAuthException {

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("XMPP notification: " + p.toXML());
        }
        processQueue();
    }

    /**
     * Disconnects from the XMPP Google Cloud Print Notification.
     * <p>
     * <b>Important</b>: this method is synchronized since it can be called from
     * multiple threads. E.g. as result of an exception (in the finally block)
     * or as a result from a Quartz scheduler job interrupt. See the
     * {@link AbstractJob#interrupt()} implementation, and its handling in
     * {@link GcpListenerJob}.
     * </p>
     * <p>
     * Note: this method is idempotent, i.e. it can be called more than once
     * resulting in the same end-state.
     * </p>
     *
     * @throws InterruptedException
     *
     */
    public synchronized void disconnect() throws InterruptedException {

        /*
         * Wait for processing to finish.
         */
        waitForProcessing(1000L);

        if (xmppConnection != null) {

            xmppConnection.disconnect();
            xmppConnection = null;

            LOGGER.trace("disconnected");
        }
    }

    /**
     * Waits for processing to finish.
     *
     * @param millisInterval
     *            The sleep interval applied while {@link #isProcessing}.
     *
     * @throws InterruptedException
     */
    private void waitForProcessing(long millisInterval)
            throws InterruptedException {

        boolean waiting = this.isProcessing;

        if (waiting) {
            LOGGER.trace("waiting for processing to finish ...");
        }

        while (this.isProcessing) {
            Thread.sleep(millisInterval);
            LOGGER.trace("processing ...");
        }

        if (waiting) {
            LOGGER.trace("processing finished.");
        }
    }

    /**
     * Return a localized message string.
     * <p>
     * IMPORTANT: The locale from the application is used.
     * </p>
     *
     * @param key
     *            The key of the message.
     * @param args
     *            The placeholder arguments for the message template.
     *
     * @return The message text.
     */
    private String localize(final String key, final String... args) {
        return Messages.getMessage(getClass(), key, args);
    }

}
