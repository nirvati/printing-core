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
package org.savapage.core.print.imap;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;

import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessageRemovedException;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.event.MessageCountAdapter;
import javax.mail.event.MessageCountEvent;
import javax.mail.event.MessageCountListener;
import javax.mail.internet.InternetAddress;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.time.DateUtils;
import org.savapage.core.SpException;
import org.savapage.core.UnavailableException;
import org.savapage.core.circuitbreaker.CircuitBreakerException;
import org.savapage.core.cometd.AdminPublisher;
import org.savapage.core.cometd.PubLevelEnum;
import org.savapage.core.cometd.PubTopicEnum;
import org.savapage.core.community.CommunityDictEnum;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.dao.UserDao;
import org.savapage.core.dao.enums.ACLOidEnum;
import org.savapage.core.dao.enums.DocLogProtocolEnum;
import org.savapage.core.dao.enums.ReservedIppQueueEnum;
import org.savapage.core.doc.DocContent;
import org.savapage.core.i18n.AdjectiveEnum;
import org.savapage.core.i18n.NounEnum;
import org.savapage.core.job.AbstractJob;
import org.savapage.core.job.MailPrintListenerJob;
import org.savapage.core.jpa.User;
import org.savapage.core.print.server.DocContentPrintException;
import org.savapage.core.print.server.DocContentPrintReq;
import org.savapage.core.print.server.DocContentPrintRsp;
import org.savapage.core.services.AccessControlService;
import org.savapage.core.services.EmailService;
import org.savapage.core.services.JobTicketService;
import org.savapage.core.services.QueueService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.UserService;
import org.savapage.core.services.helpers.email.EmailMsgParms;
import org.savapage.core.template.dto.TemplateDtoCreator;
import org.savapage.core.template.email.MailPrintTicketReceived;
import org.savapage.core.util.DateUtil;
import org.savapage.core.util.InetUtils;
import org.savapage.core.util.Messages;
import org.savapage.core.util.NumberUtil;
import org.savapage.lib.pgp.PGPBaseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.mail.imap.IMAPFolder;

/**
 * The adapter which receives {@link MessageCountEvent} messages from the IMAP
 * host.
 * <p>
 * Prerequisites:
 * <ul>
 * <li>IMAP host MUST support the IDLE Command (RFC2177).</li>
 * </ul>
 * </p>
 * <p>
 * References:
 * <ul>
 * <li><a href="http://www.isode.com/whitepapers/imap-idle.html">IMAP IDLE: The
 * best approach for 'push' email</a></li>
 * <li><a href=
 * "https://javamail.java.net/nonav/docs/api/com/sun/mail/imap/package-summary.html"
 * >IMAP protocol properties</a>. Note that if you're using the "imaps" protocol
 * to access IMAP over SSL, all the properties would be named "mail.imaps.*".
 * </li>
 * </ul>
 * </p>
 *
 * @author Rijk Ravestein
 *
 */
public final class MailPrintListener extends MessageCountAdapter {

    /** */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(MailPrintListener.class);

    /** */
    private static final AccessControlService ACCESS_CONTROL_SERVICE =
            ServiceContext.getServiceFactory().getAccessControlService();
    /** */
    private static final EmailService EMAIL_SERVICE =
            ServiceContext.getServiceFactory().getEmailService();
    /** */
    private static final JobTicketService JOBTICKET_SERVICE =
            ServiceContext.getServiceFactory().getJobTicketService();
    /** */
    private static final UserService USER_SERVICE =
            ServiceContext.getServiceFactory().getUserService();
    /** */
    private static final QueueService QUEUE_SERVICE =
            ServiceContext.getServiceFactory().getQueueService();
    /** */
    private static final UserDao USER_DAO =
            ServiceContext.getDaoContext().getUserDao();

    /**
     *
     */
    final private String host;
    final private int port;
    final private String inboxFolder;
    final private String trashFolder;
    final private String security;
    final private String protocol;

    final private Properties props = new Properties();
    final private boolean imapDebug;

    /**
     * Socket connection timeout value in milliseconds. Default is infinite
     * timeout.
     */
    private Integer connectionTimeout =
            IConfigProp.IMAP_CONNECTION_TIMEOUT_V_DEFAULT;

    /**
     * Socket I/O timeout value in milliseconds. Default is infinite timeout.
     */
    private Integer timeout = IConfigProp.IMAP_TIMEOUT_V_DEFAULT;

    private Store store = null;
    private Folder inbox = null;
    private Folder trash = null;

    private MessageCountListener messageCountListener = this;

    /**
     * The thread to keep the idle connection alive.
     */
    private Thread keepConnectionAlive = null;

    /**
     * Tells whether the configured IMAP server supports the IDLE extension. Is
     * {@link null} when unknown. Support is known after the
     * {@link #listen(int, int)} method is performed.
     */
    private Boolean idleSupported = null;

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
     *
     */
    public MailPrintListener(String host, int port, String security,
            String inboxFolder, String trashFolder, boolean imapDebug) {
        this.host = host;
        this.port = port;
        this.security = security;
        this.inboxFolder = inboxFolder;
        this.trashFolder = trashFolder;
        this.imapDebug = imapDebug;
        this.protocol = getProtocol(this.security);
    }

    @SuppressWarnings("unused")
    private MailPrintListener() {
        this.host = null;
        this.port = 0;
        this.inboxFolder = null;
        this.trashFolder = null;
        this.security = null;
        this.protocol = null;
        this.imapDebug = false;
    }

    private static String getProtocol(final String security) {
        return security.equals(IConfigProp.IMAP_SECURITY_V_NONE) ? "imap"
                : "imaps";
    }

    /**
     * Instantiates using the configuration settings.
     *
     * @param cm
     *            The configuration manager.
     */
    public MailPrintListener(ConfigManager cm) {
        this.host = cm.getConfigValue(Key.PRINT_IMAP_HOST);
        this.port = cm.getConfigInt(Key.PRINT_IMAP_PORT);
        this.security = cm.getConfigValue(Key.PRINT_IMAP_SECURITY);
        this.inboxFolder = cm.getConfigValue(Key.PRINT_IMAP_INBOX_FOLDER);
        this.trashFolder = cm.getConfigValue(Key.PRINT_IMAP_TRASH_FOLDER);
        this.imapDebug = cm.isConfigValue(Key.PRINT_IMAP_DEBUG);
        this.connectionTimeout =
                cm.getConfigInt(Key.PRINT_IMAP_CONNECTION_TIMEOUT_MSECS);
        this.timeout = cm.getConfigInt(Key.PRINT_IMAP_TIMEOUT_MSECS);
        this.protocol = getProtocol(this.security);
    }

    /**
     *
     * @param bValue
     */
    public void disableAuthLogin(final boolean bValue) {
        this.props.put("mail." + this.protocol + ".auth.login.disable",
                bValue ? "true" : "false");
    }

    /**
     *
     * @param bValue
     */
    public void disableAuthPlain(final boolean bValue) {
        this.props.put("mail." + this.protocol + ".auth.plain.disable",
                bValue ? "true" : "false");
    }

    /**
     *
     * @param bValue
     */
    public void disableAuthNtlm(final boolean bValue) {
        this.props.put("mail." + this.protocol + ".auth.ntlm.disable",
                bValue ? "true" : "false");
    }

    @Override
    public void messagesRemoved(final MessageCountEvent ev) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("IMAP notification: [{}] message(s) REMOVED from [{}]",
                    ev.getMessages().length, this.inboxFolder);
        }
    }

    /**
     * Callback from {@link MessageCountAdapter}.
     * <p>
     * Since, an exception in this method does not stop the listener, we log the
     * exception and explicitly {@link #disconnect()}.
     * </p>
     */
    @Override
    public void messagesAdded(final MessageCountEvent ev) {

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("IMAP notification: [{}] message(s) ADDED to [{}]",
                    ev.getMessages().length, this.inboxFolder);
        }

        boolean hasException = true;

        try {
            this.processMessages(ev.getMessages());
            hasException = false;
        } catch (MessagingException | IOException e) {
            LOGGER.error(e.getMessage(), e);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }

        if (hasException) {
            try {
                this.disconnect();
            } catch (MessagingException | InterruptedException e) {
                LOGGER.error(e.getMessage(), e);
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
    }

    /**
     * Connects to the IMAP server and opens the necessary folders.
     *
     * @param username
     * @param password
     * @throws MessagingException
     *             When the connection cannot be established or folders can not
     *             be opened.
     */
    public void connect(final String username, final String password)
            throws MessagingException {

        this.store = null;
        this.inbox = null;
        this.trash = null;

        this.props.put("mail." + this.protocol + ".connectiontimeout",
                this.connectionTimeout);
        this.props.put("mail." + this.protocol + ".timeout", this.timeout);

        final boolean isSTARTTLS = this.security
                .equalsIgnoreCase(IConfigProp.IMAP_SECURITY_V_STARTTLS);
        final boolean isSSLTLS =
                this.security.equalsIgnoreCase(IConfigProp.SMTP_SECURITY_V_SSL);

        if (isSTARTTLS) {
            props.put("mail." + this.protocol + ".starttls.enable", "true");
        } else if (isSSLTLS) {
            // Mantis #1184
            props.put("mail." + this.protocol + ".socketFactory.port",
                    this.port);
            props.put("mail." + this.protocol + ".socketFactory.class",
                    "javax.net.ssl.SSLSocketFactory");
        }

        if (isSTARTTLS || isSSLTLS) {
            // Mantis #1184
            try {
                props.put("mail." + this.protocol + ".ssl.protocols",
                        InetUtils.getDefaultSSLProtocols());
            } catch (NoSuchAlgorithmException e) {
                LOGGER.warn(String.format("No SSL protocols found: %s",
                        e.getMessage()));
            }
        }

        final Session session = Session.getInstance(this.props, null);
        session.setDebug(this.imapDebug);

        this.store = session.getStore(this.protocol);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Connecting to {} [{}]@{}:{} (timeout {} millis) ...",
                    this.protocol.toUpperCase(), username, this.host, this.port,
                    this.connectionTimeout);
        }

        this.store.connect(this.host, this.port, username, password);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Connected.");
            LOGGER.debug("Opening folders ...");
        }

        /*
         * Get Inbox folder.
         */
        this.inbox = store.getFolder(inboxFolder);
        if (this.inbox == null) {
            throw new SpException(
                    "IMAP folder [" + this.inboxFolder + "] does not exist.");
        }

        /*
         * Get Trash folder.
         */
        trash = store.getFolder(trashFolder);
        if (trash == null) {
            throw new SpException(
                    "IMAP folder [" + this.trashFolder + "] does not exist.");
        }

        /*
         * Open Inbox and Trash folders.
         */
        this.inbox.open(Folder.READ_WRITE);
        this.trash.open(Folder.HOLDS_MESSAGES);

        if (!(this.inbox instanceof IMAPFolder)) {
            throw new SpException(
                    "[" + this.inboxFolder + "] is not an IMAP folder.");
        }

        if (!(this.trash instanceof IMAPFolder)) {
            throw new SpException(
                    "[" + this.inboxFolder + "] is not an IMAP folder.");
        }

        LOGGER.debug("Folders opened.");
    }

    /**
     * Waits for processing to finish.
     *
     * @param millisInterval
     *            The sleep interval applied while {@link #isProcessing}.
     *
     * @throws InterruptedException
     *             When thread has been interrupted.
     */
    private void waitForProcessing(final long millisInterval)
            throws InterruptedException {

        final boolean waiting = this.isProcessing;

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
     * Disconnects from the IMAP server.
     * <p>
     * <b>Important</b>: this method is synchronized since it can be called from
     * multiple threads. E.g. as result of an exception (in the finally block)
     * or as a result from a Quartz scheduler job interrupt. See the
     * {@link AbstractJob#interrupt()} implementation, and its handling in
     * {@link MailPrintListenerJob}.
     * </p>
     * <p>
     * Note: this method is idempotent, i.e. it can be called more than once
     * resulting in the same end-state.
     * </p>
     *
     * @throws MessagingException
     * @throws InterruptedException
     */
    public synchronized void disconnect()
            throws MessagingException, InterruptedException {

        int nActions = 0;

        /*
         * Shutdown keep alive thread.
         */
        if (this.keepConnectionAlive != null
                && this.keepConnectionAlive.isAlive()) {
            nActions++;
            this.keepConnectionAlive.interrupt();
            this.keepConnectionAlive = null;
        }

        /*
         * Remove the listener
         */
        if (this.messageCountListener != null && this.inbox != null) {
            this.inbox.removeMessageCountListener(this.messageCountListener);
            this.messageCountListener = null;
            nActions++;
        }

        /*
         * Wait for processing to finish.
         */
        this.waitForProcessing(1000L);

        /*
         * Close the IMAP folders.
         */
        final boolean expungeDeleted = true; // needed !!!

        if (this.inbox != null && this.inbox.isOpen()) {

            this.inbox.close(expungeDeleted);

            LOGGER.trace("Closed folder [{}]", this.inboxFolder);

            this.inbox = null;
            nActions++;
        }

        if (this.trash != null && this.trash.isOpen()) {

            this.trash.close(expungeDeleted);

            LOGGER.trace("Closed folder [{}]", this.trashFolder);

            this.trash = null;
            nActions++;
        }

        /*
         * Close the IMAP store.
         */
        if (this.store != null && this.store.isConnected()) {

            this.store.close();

            LOGGER.trace("Closed the store.");

            this.store = null;
            nActions++;
        }

        if (nActions > 0) {
            LOGGER.debug("Disconnected.");
        }
    }

    /**
     * Tells whether an unrecoverable error occurred while using this object.
     *
     * @return {@link true} when an unrecoverable error occurred.
     */
    public boolean hasUnrecoverableError() {
        if (this.idleSupported == null) {
            return false;
        }
        return !this.idleSupported.booleanValue();
    }

    /**
     * Tells whether the configured IMAP server supports the IDLE extension.
     * Support is known after the {@link #listen(int, int)} method is performed.
     *
     * @return {@link null} when unknown.
     */
    public Boolean isIdleSupported() {
        return idleSupported;
    }

    /**
     * @return IMAP host.
     */
    public String getHost() {
        return host;
    }

    /**
     * Listens to incoming messages for a maximum duration.
     * <p>
     * <b>Note</b>: This is a blocking call that returns when the maximum
     * duration is reached. A {@link #disconnect()} is called before returning.
     * </p>
     *
     * @param sessionHeartbeatSecs
     *            The keep alive interval in seconds.
     * @param sessionDurationSecs
     *            The duration after which this method returns.
     * @throws MessagingException
     * @throws InterruptedException
     *             When thread has been interrupted.
     */
    public void listen(final int sessionHeartbeatSecs,
            final int sessionDurationSecs)
            throws MessagingException, InterruptedException {

        final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
        try {

            this.idleSupported = null;
            this.messageCountListener = null;
            this.inbox.addMessageCountListener(this);
            this.messageCountListener = this;

            int nInterval = 0;

            final IMAPFolder watchFolder = (IMAPFolder) inbox;

            this.keepConnectionAlive = new Thread(
                    new MailPrintHeartbeat(watchFolder, sessionHeartbeatSecs),
                    MailPrintHeartbeat.class.getSimpleName());

            this.keepConnectionAlive.start();

            final long timeMax = System.currentTimeMillis()
                    + DateUtil.DURATION_MSEC_SECOND * sessionDurationSecs;

            final Date dateMax = new Date(timeMax);

            while (!Thread.interrupted()) {

                if (!watchFolder.isOpen()) {
                    break;
                }

                final Date now = new Date();

                if (now.getTime() > timeMax) {
                    break;
                }

                if (!ConfigManager.isMailPrintEnabled()) {
                    LOGGER.trace("Mail Print disabled by administrator.");
                    break;
                }

                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Waiting [{}] next [{}] till [{}] ...",
                            (++nInterval),
                            dateFormat.format(DateUtils.addSeconds(now,
                                    sessionHeartbeatSecs)),
                            dateFormat.format(dateMax));
                }
                /*
                 * This call returns when a Message[] arrives or when an IMAP
                 * NOOP command is issued by the ImapKeepAliveActor.
                 *
                 * While the MessageCountAdapter call-backs handle the
                 * Message[], the idle() method returns immediately.
                 *
                 * That is why we wait for the processing to finish before we
                 * call idle() again.
                 */
                try {
                    watchFolder.idle(true);
                    /*
                     * At this point we know that IDLE is supported by the
                     * server.
                     */
                    idleSupported = Boolean.TRUE;

                } catch (IllegalStateException e) {
                    /*
                     * The folder isn't open.
                     */
                    if (this.keepConnectionAlive == null) {
                        /*
                         * This makes sense when are disconnecting (application
                         * is closing down)...
                         */
                        break;
                    }

                    throw e;

                } catch (MessagingException e) {
                    /*
                     * If the server doesn't support the IDLE extension, we get
                     * upon the FIRST idle() call (when idleSupported == null).
                     */
                    if (this.idleSupported == null) {
                        idleSupported = Boolean.FALSE;
                    }
                    /*
                     * At this point, idleSupported can be true or false. In
                     * both cases we re-throw the exception.
                     *
                     * In case idleSupported == true the connection might be
                     * dropped by the server, i.e.:
                     *
                     * BYE JavaMail Exception: java.io.IOException: Connection
                     * dropped by server?
                     */
                    throw e;
                }
                /*
                 * Wait for processing to finish.
                 */
                this.waitForProcessing(DateUtil.DURATION_MSEC_SECOND);
            }

        } finally {
            this.disconnect();
        }
    }

    /**
     * Deletes a message.
     *
     * @param message
     *            {@link Message}.
     * @param nMsg
     *            1-based message ordinal.
     * @throws MessagingException
     */
    private void deleteMessage(final Message message, final int nMsg)
            throws MessagingException {

        if (!message.isExpunged()) {
            try {
                if (message.isSet(Flags.Flag.DELETED)) {
                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace("Message #{} already deleted", nMsg);
                    }
                } else {
                    message.setFlag(Flags.Flag.DELETED, true);
                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace("Message #{} deleted", nMsg);
                    }
                }
                this.inbox.expunge();

            } catch (MessageRemovedException e) {
                /*
                 * The MessageRemovedException is thrown if an invalid method is
                 * invoked on an expunged Message. The only valid methods on an
                 * expunged Message are <code>isExpunged()</code> and
                 * <code>getMessageNumber()</code>.
                 */
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn("Message #{} ALREADY removed", nMsg);
                }
            }
        }
    }

    /**
     * Moves messages to the Trash folder.
     *
     * @param message
     *            {@link Message}.
     * @param nMsg
     *            1-based message ordinal.
     * @throws MessagingException
     */
    private void moveToTrash(final Message message, final int nMsg)
            throws MessagingException {

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Message #{} moved to trash folder", nMsg);
        }
        final Message[] messages = { message };
        this.inbox.copyMessages(messages, trash);
        this.deleteMessage(message, nMsg);
    }

    /**
     * Processes email messages.
     *
     * @param messages
     *            The array of email messages.
     * @throws MessagingException
     * @throws IOException
     */
    private void processMessages(final Message[] messages)
            throws MessagingException, IOException {

        final boolean isMoveToTrash = ConfigManager.instance()
                .isConfigValue(Key.PRINT_IMAP_TRASH_FOLDER_ENABLE);

        this.isProcessing = true;

        int nMsg = 0;

        try {
            for (final Message message : messages) {

                if (message.isExpunged()) {
                    LOGGER.warn("Message #{} skipped. Reason: expunged.",
                            (++nMsg));
                    continue;
                }

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Message #{}", (++nMsg));
                }

                try {
                    this.processMessage(message);
                } finally {
                    if (isMoveToTrash) {
                        this.moveToTrash(message, nMsg);
                    } else {
                        this.deleteMessage(message, nMsg);
                    }
                }
            }

        } finally {
            this.isProcessing = false;
        }
    }

    /**
     * @param user
     *            User (can be {@code null}).
     * @return {@code true} if user has permission to journal a PrintIn
     *         document.
     */
    private static boolean hasInboxJournalPermission(final User user) {
        if (user == null) {
            return false;
        }
        return ACCESS_CONTROL_SERVICE.hasAccess(user,
                ACLOidEnum.U_QUEUE_JOURNAL);
    }

    /**
     * Processes an IMAP message.
     * <p>
     * INVARIANT (TO BE IMPLEMENTED):
     * <ul>
     * <li>Processing MUST be <i>idempotent</i>. When the same message is
     * offered a second time, any effect that was established by previous
     * processing is leading and will NOT be overwritten, i.e. the message will
     * NOT be processed, but WILL be moved to trash.</li>
     * </ul>
     * </p>
     *
     * @param message
     *
     * @throws MessagingException
     * @throws IOException
     */
    private void processMessage(final Message message)
            throws MessagingException, IOException {

        final Locale locale = ConfigManager.getDefaultLocale();
        final String mailPrintWord =
                CommunityDictEnum.MAIL_PRINT.getWord(locale);

        ServiceContext.resetTransactionDate();

        final String from =
                new InternetAddress(InternetAddress.toString(message.getFrom()))
                        .getAddress();

        if (LOGGER.isTraceEnabled()) {
            final StringBuilder trace = new StringBuilder();
            trace.append("\n\tFrom     : ").append(from);
            trace.append("\n\tSubject  : ").append(message.getSubject());
            trace.append("\n\tSent     : ").append(message.getSentDate());
            trace.append("\n\tReceived : ").append(message.getReceivedDate());
            trace.append("\n\tSize     : ").append(message.getSize());
            LOGGER.trace(trace.toString());
        }

        final ConfigManager cm = ConfigManager.instance();

        final String imapTicketOperator =
                ConfigManager.getMailPrintTicketOperator();

        final User user;
        final boolean isImapTicket;

        if (StringUtils.isNotBlank(imapTicketOperator)) {

            final User targetUser;

            if (cm.isConfigValue(Key.PRINT_IMAP_TICKET_INCLUDE_KNOWN_USERS)) {
                targetUser = null;
            } else {
                // returns null if not found.
                targetUser = USER_SERVICE.findActiveUserByEmail(from);
            }

            if (targetUser == null) {
                // returns null if not found.
                final User operator =
                        USER_DAO.findActiveUserByUserId(imapTicketOperator);
                isImapTicket = hasInboxJournalPermission(operator);
                if (isImapTicket) {
                    user = operator;
                } else {
                    user = null;
                }
            } else {
                user = targetUser;
                isImapTicket = false;
            }
        } else {
            user = USER_SERVICE.findActiveUserByEmail(from);
            isImapTicket = false;
        }

        final boolean printInDisabled =
                user != null && BooleanUtils.isTrue(user.getDisabledPrintIn());

        if (user == null || printInDisabled) {

            final String userMsg;
            final String pubMsg;

            if (printInDisabled) {
                userMsg = localize("user-msg-body-account-disabled");
                pubMsg = localize("pub-account-disabled", mailPrintWord,
                        user.getUserId(), from);
                LOGGER.warn("PrintIn disabled for [{}] [{}]", user.getUserId(),
                        from);
            } else {
                userMsg = localize("user-msg-body-email-unknown");
                pubMsg = localize("pub-no-user-found", mailPrintWord, from);
                LOGGER.warn("No user found for [{}]", from);
            }

            this.sendEmail(from, localize("user-msg-subject-denied",
                    CommunityDictEnum.SAVAPAGE.getWord(), mailPrintWord),
                    localize("user-msg-header-not-authorized"), userMsg);

            AdminPublisher.instance().publish(PubTopicEnum.MAILPRINT,
                    PubLevelEnum.WARN, pubMsg);
        } else {

            if (LOGGER.isTraceEnabled()) {
                if (isImapTicket) {
                    LOGGER.trace("Mail Print Ticket for [{}] to operator [{}]",
                            from, user.getUserId(), from);
                } else {
                    LOGGER.trace("User [{}] belongs to [{}]", user.getUserId(),
                            from);
                }
            }
            /*
             * Iterate the attachments.
             */
            final Object content = message.getContent();

            final MutableInt nAttachments = new MutableInt(0);

            if (content instanceof Multipart) {

                final int maxPrintedAllowed =
                        cm.getConfigInt(Key.PRINT_IMAP_MAX_FILES);

                final long maxBytesAllowed =
                        cm.getConfigLong(Key.PRINT_IMAP_MAX_FILE_MB) * 1024
                                * 1024;

                final Multipart multipart = (Multipart) content;
                final MutableInt nPrinted = new MutableInt(0);

                for (int i = 0; i < multipart.getCount(); i++) {

                    final String mailPrintTicket;
                    if (isImapTicket) {
                        mailPrintTicket =
                                JOBTICKET_SERVICE.createTicketNumber();
                    } else {
                        mailPrintTicket = null;
                    }

                    // Print
                    this.printMessagePart(from, user, mailPrintTicket,
                            multipart, i, nAttachments, nPrinted,
                            maxPrintedAllowed, maxBytesAllowed);
                }
            }

            if (nAttachments.intValue() == 0) {
                this.sendEmailPubLog(from, localize("user-msg-subject-rejected",
                        CommunityDictEnum.SAVAPAGE.getWord(), mailPrintWord),
                        AdjectiveEnum.REJECTED.uiText(locale),
                        localize("user-msg-body-content-notfound"),
                        String.format("Mail Print from [%s] rejected: "
                                + "no file attachment.", from));
            }

        }
    }

    /**
     * Sends an email.
     *
     * @param eToAddress
     *            The email address.
     * @param eSubject
     *            The subject of the message.
     * @param eHeaderText
     *            The email content header text.
     * @param eContent
     *            The email body text with optional newline {@code \n}
     *            characters. *
     * @param msgPubLog
     *            Warning message for {@link AdminPublisher} and logger.
     */
    private void sendEmailPubLog(final String eToAddress, final String eSubject,
            final String eHeaderText, final String eContent,
            final String msgPubLog) {
        this.sendEmail(eToAddress, eSubject, eHeaderText, eContent);
        AdminPublisher.instance().publish(PubTopicEnum.MAILPRINT,
                PubLevelEnum.WARN, msgPubLog);
        LOGGER.warn(msgPubLog);
    }

    /**
     * Prints the message part attachment.
     * <p>
     * When DocContentPrintException or another reason for rejection occurs an
     * email is send to the user.
     * </p>
     *
     * @param originatorEmail
     * @param user
     * @param mailPrintTicket
     * @param multipart
     * @param iPart
     *            Zero-based index of Part to print.
     * @param nAttachments
     *            Running total of file attachments.
     * @param nPrinted
     *            Running total of successful prints.
     * @param maxPrintedAllowed
     * @param maxBytesAllowed
     *
     * @return {@code false} if message is rejected.
     * @throws MessagingException
     * @throws IOException
     */
    private boolean printMessagePart(final String originatorEmail,
            final User user, final String mailPrintTicket,
            final Multipart multipart, final int iPart,
            final MutableInt nAttachments, final MutableInt nPrinted,
            final int maxPrintedAllowed, final long maxBytesAllowed)
            throws MessagingException, IOException {

        final Locale locale = ConfigManager.getDefaultLocale();

        final int nParts = multipart.getCount();

        final Part part = multipart.getBodyPart(iPart);
        final String fileName = part.getFileName();

        final String contentType = part.getContentType()
                .replaceAll("\\s*[\\r\\n]+\\s*", "").trim().toLowerCase();

        /*
         * From the JavaDoc
         *
         * Return the size of the content of this part in bytes. Return -1 if
         * the size cannot be determined.
         *
         * Note that the size may not be an exact measure of the content size
         * and may or may not account for any transfer encoding of the content.
         * The size is appropriate for display in a user interface to give the
         * user a rough idea of the size of this part.
         */
        final int partSize = part.getSize();

        /*
         * ***********************************************************
         * IMPORTANT: do NOT use part.getContent()
         *
         * Since this will throw and exception with message saying
         * "Unknown image type IMAGE/JPEG". Somehow javax.activation cannot
         * handle certain mime-types. IMAGE/PNG is no problem though. Why?
         *
         * WORKAROUND: part.getInputStream()
         * ***********************************************************
         */
        if (LOGGER.isTraceEnabled()) {
            final StringBuilder trace = new StringBuilder();
            trace.append("[").append(iPart + 1).append("/").append(nParts);
            trace.append("] [").append(contentType).append("]");
            if (fileName != null) {
                trace.append(" file [").append(fileName).append("]");
            }
            trace.append(" size [").append(partSize).append("]");
            LOGGER.trace(trace.toString());
        }

        String rejectedReason = null;

        if (fileName == null) {
            // No attachment, just the message body.
            rejectedReason = null;
        } else {
            nAttachments.increment();
            // Check number of attachments.
            if (nPrinted.intValue() < maxPrintedAllowed) {
                /*
                 * Check attachment size
                 */
                if (partSize < maxBytesAllowed) {

                    try {
                        ServiceContext.resetTransactionDate();

                        final DocContentPrintReq docContentPrintReq =
                                new DocContentPrintReq();

                        docContentPrintReq.setContentType(
                                DocContent.getContentTypeFromFile(fileName));
                        docContentPrintReq.setFileName(fileName);
                        docContentPrintReq.setOriginatorEmail(originatorEmail);
                        docContentPrintReq.setMailPrintTicket(mailPrintTicket);
                        docContentPrintReq.setPreferredOutputFont(null);
                        docContentPrintReq.setProtocol(DocLogProtocolEnum.IMAP);
                        docContentPrintReq.setTitle(fileName);

                        final DocContentPrintRsp docContentPrintRsp =
                                QUEUE_SERVICE.printDocContent(
                                        ReservedIppQueueEnum.MAILPRINT,
                                        user.getUserId(), docContentPrintReq,
                                        part.getInputStream());

                        nPrinted.increment();

                        if (LOGGER.isTraceEnabled()) {
                            LOGGER.trace("{} : {}/{} processing time {}",
                                    originatorEmail, iPart + 1, nParts,
                                    DateUtil.formatDuration(new Date().getTime()
                                            - ServiceContext
                                                    .getTransactionDate()
                                                    .getTime()));
                        }

                        if (mailPrintTicket != null) {
                            this.notifyMailTicketByEmail(docContentPrintReq,
                                    docContentPrintRsp, user);
                        }

                    } catch (DocContentPrintException e) {
                        rejectedReason = e.getMessage();
                    } catch (UnavailableException e) {
                        if (e.getState() == UnavailableException.State.TEMPORARY) {
                            rejectedReason = localize(
                                    "user-msg-file-denied-temp-unavailable");
                        } else {
                            rejectedReason = localize(
                                    "user-msg-file-denied-unavailable");
                        }
                    }

                } else {
                    rejectedReason = localize("user-msg-file-denied-max-size",
                            NumberUtil.humanReadableByteCountSI(locale,
                                    maxBytesAllowed));
                }

            } else {
                rejectedReason = localize("user-msg-file-denied-max-number",
                        String.valueOf(maxPrintedAllowed));
            }
        }

        if (rejectedReason != null) {

            final String mailPrintWord =
                    CommunityDictEnum.MAIL_PRINT.getWord(locale);

            final String fileNameMsg = StringUtils.defaultString(fileName, "-");

            AdminPublisher.instance().publish(PubTopicEnum.MAILPRINT,
                    PubLevelEnum.WARN,
                    localize("pub-file-rejected", mailPrintWord,
                            originatorEmail, fileNameMsg, rejectedReason));

            LOGGER.warn("[{}] file [{}] rejected. Reason: {}", originatorEmail,
                    fileNameMsg, rejectedReason);

            final String mailSubject;
            final String mailContent;

            if (fileName == null) {
                mailSubject = localize("user-msg-subject-rejected",
                        CommunityDictEnum.SAVAPAGE.getWord(), mailPrintWord);
                mailContent = rejectedReason;
            } else {
                mailSubject = localize("user-msg-subject-file-rejected",
                        CommunityDictEnum.SAVAPAGE.getWord(), mailPrintWord,
                        fileName);
                mailContent = String.format("%s \"%s\" : %s",
                        NounEnum.FILE.uiText(locale), fileName, rejectedReason);
            }

            this.sendEmail(originatorEmail, mailSubject,
                    AdjectiveEnum.REJECTED.uiText(locale), mailContent);
        }

        return rejectedReason == null;
    }

    /**
     * Notifies Mail Ticket requester by email.
     *
     * @param req
     *            The print request.
     * @param rsp
     *            The print response.
     * @param ticketOperator
     *            Operator.
     */
    private void notifyMailTicketByEmail(final DocContentPrintReq req,
            final DocContentPrintRsp rsp, final User ticketOperator) {

        final MailPrintTicketReceived tpl = new MailPrintTicketReceived(
                ConfigManager.getServerCustomEmailTemplateHome(),
                TemplateDtoCreator.templateMailTicketDto(req, rsp,
                        StringUtils.defaultIfBlank(ticketOperator.getFullName(),
                                ticketOperator.getUserId())));

        final boolean asHtml = ConfigManager.instance()
                .isConfigValue(Key.PRINT_IMAP_TICKET_REPLY_CONTENT_TYPE_HTML);

        final EmailMsgParms emailParms = EmailMsgParms.create(
                req.getOriginatorEmail(), tpl, Locale.getDefault(), asHtml);

        try {
            EMAIL_SERVICE.sendEmail(emailParms);
        } catch (InterruptedException | CircuitBreakerException
                | MessagingException | IOException | PGPBaseException e) {
            LOGGER.error("Sending Mail Print Ticket email to [{}] failed: {}",
                    req.getOriginatorEmail(), e.getMessage());
        }
    }

    /**
     * Sends an email.
     *
     * @param toAddress
     *            The email address.
     * @param subject
     *            The subject of the message.
     * @param headerText
     *            The content header text.
     * @param content
     *            The body text with optional newline {@code \n} characters.
     */
    private void sendEmail(final String toAddress, final String subject,
            final String headerText, final String content) {

        try {
            final EmailMsgParms emailParms = new EmailMsgParms();

            emailParms.setToAddress(toAddress);
            emailParms.setSubject(subject);
            emailParms.setBodyInStationary(headerText, content,
                    Locale.getDefault(), true);

            EMAIL_SERVICE.writeEmail(emailParms);

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Sent email to [{}] subject [{}]", toAddress,
                        subject);
            }
        } catch (MessagingException | IOException | PGPBaseException e) {
            LOGGER.error("Sending email to [{}] failed: {}", toAddress,
                    e.getMessage());
        }
    }

    /**
     * Return a localized message string. IMPORTANT: The locale from the
     * application is used.
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

    /**
     * Processes all messages from the Inbox, and moves them to the Trash folder
     * when done.
     * <p>
     * <b>INVARIANT</b>: this object must be connected as in the
     * {@link #connect(String, String)} method.
     * </p>
     * <p>
     * Started with <a href=
     * "http://www.hiteshagrawal.com/java/reading-imap-server-emails-using-java"
     * >this example</a>.
     * </p>
     * <p>
     * <a href="http://markmail.org/message/2poy2jtw2nvz6eua">This post</a>
     * states: <i> Per GMail's IMAP client settings pages (in the Help Center),
     * the IMAP "delete" command is being used to remove the current label, or
     * "archive" in the case of inbox. To actually "delete" something from an
     * IMAP session, you need to move the message to the "[GMail]\Trash" folder.
     * </i>
     * </p>
     * <p>
     * See <a href=
     * "http://www.java2s.com/Code/Java/Email/MOVEmessagesbetweenmailboxes.htm"
     * >MOVE messages between mailboxes</a>.
     * </p>
     *
     * @throws MessagingException
     * @throws IOException
     *
     */
    public void processInbox() throws MessagingException, IOException {
        LOGGER.trace("Checking inbox ...");
        this.processMessages(this.inbox.getMessages());
    }

    private int inboxMessageCount() throws MessagingException {
        return this.inbox.getMessageCount();
    }

    private int trashMessageCount() throws MessagingException {
        return this.trash.getMessageCount();
    }

    /**
     * Tests the IMAP connection using the configuration settings and returns
     * number of Inbox and Trash messages.
     *
     * @param nMessagesInbox
     *            Number of Inbox messages.
     * @param nMessagesTrash
     *            Number of Trash messages.
     * @throws Exception
     *             When test fails.
     */
    public static void test(final MutableInt nMessagesInbox,
            final MutableInt nMessagesTrash) throws Exception {

        final ConfigManager cm = ConfigManager.instance();
        final MailPrintListener listener = new MailPrintListener(cm);
        try {
            listener.connect(cm.getConfigValue(Key.PRINT_IMAP_USER_NAME),
                    cm.getConfigValue(Key.PRINT_IMAP_PASSWORD));
            nMessagesInbox.setValue(listener.inboxMessageCount());
            nMessagesTrash.setValue(listener.trashMessageCount());
        } finally {
            listener.disconnect();
        }
    }
}
