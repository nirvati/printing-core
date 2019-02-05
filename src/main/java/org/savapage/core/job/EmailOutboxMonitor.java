/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2019 Datraverse B.V.
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

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.ParseException;
import java.util.Locale;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.UnableToInterruptJobException;
import org.savapage.core.SpException;
import org.savapage.core.circuitbreaker.CircuitBreaker;
import org.savapage.core.circuitbreaker.CircuitBreakerException;
import org.savapage.core.cometd.AdminPublisher;
import org.savapage.core.cometd.PubLevelEnum;
import org.savapage.core.cometd.PubTopicEnum;
import org.savapage.core.config.CircuitBreakerEnum;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.services.EmailService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.util.BigDecimalUtil;
import org.savapage.core.util.DateUtil;
import org.savapage.core.util.NumberUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class EmailOutboxMonitor extends AbstractJob {

    /**
     * The logger.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(EmailOutboxMonitor.class);

    /**
     * Number of seconds after restarting this job after an exception occurs.
     */
    private static final int RESTART_SECS_AFTER_EXCEPTION = 60;

    /** */
    private static final long MAX_MONITOR_MSEC = DateUtil.DURATION_MSEC_HOUR;

    /**
     * Milliseconds to wait before starting this job again.
     */
    private long millisUntilNextInvocation;

    /**
     * The {@link CircuitBreaker}.
     */
    private CircuitBreaker breaker;

    @Override
    protected void onInterrupt() throws UnableToInterruptJobException {
        LOGGER.debug("Interrupted.");
    }

    @Override
    protected void onInit(final JobExecutionContext ctx) {

        this.breaker = ConfigManager
                .getCircuitBreaker(CircuitBreakerEnum.SMTP_CONNECTION);

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(localizeLogMsg("EmailOutboxMonitor.started"));
        }

        AdminPublisher.instance().publish(PubTopicEnum.SMTP, PubLevelEnum.INFO,
                localizeSysMsg("EmailOutboxMonitor.started"));
    }

    @Override
    protected void onExecute(final JobExecutionContext ctx)
            throws JobExecutionException {

        try {

            this.pollEmailOutbox();
            this.millisUntilNextInvocation = 1 * DateUtil.DURATION_MSEC_SECOND;

        } catch (CircuitBreakerException t) {

            this.millisUntilNextInvocation = this.breaker.getMillisUntilRetry();

        } catch (Exception t) {

            this.millisUntilNextInvocation = RESTART_SECS_AFTER_EXCEPTION
                    * DateUtil.DURATION_MSEC_SECOND;

            AdminPublisher.instance().publish(PubTopicEnum.SMTP,
                    PubLevelEnum.ERROR,
                    localizeSysMsg("EmailOutboxMonitor.error", t.getMessage()));

            LOGGER.error(t.getMessage(), t);
        }
    }

    @Override
    protected void onExit(final JobExecutionContext ctx) {

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(localizeLogMsg("EmailOutboxMonitor.stopped"));
        }

        final AdminPublisher publisher = AdminPublisher.instance();

        if (this.isInterrupted()) {

            publisher.publish(PubTopicEnum.SMTP, PubLevelEnum.INFO,
                    localizeSysMsg("EmailOutboxMonitor.stopped"));

        } else if (this.breaker.isCircuitDamaged()) {

            publisher.publish(PubTopicEnum.SMTP, PubLevelEnum.ERROR,
                    localizeSysMsg("EmailOutboxMonitor.stopped"));

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

                    pubMsg = localizeSysMsg("EmailOutboxMonitor.restart",
                            BigDecimalUtil.localize(BigDecimal.valueOf(seconds),
                                    Locale.getDefault(), false));
                } catch (ParseException e) {
                    throw new SpException(e.getMessage());
                }

            } else {
                pubMsg = localizeSysMsg("EmailOutboxMonitor.stopped");
            }

            publisher.publish(PubTopicEnum.SMTP, pubLevel, pubMsg);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Starting again after ["
                        + this.millisUntilNextInvocation + "] milliseconds");
            }

            SpJobScheduler.instance().scheduleOneShotEmailOutboxMonitor(
                    this.millisUntilNextInvocation);
        }
    }

    /**
     * Sends all MIME messages in the {@link DirectoryStream} as email batch.
     *
     * @param emailService
     *            The {@link EmailService}.
     * @param dirStream
     *            The {@link DirectoryStream}.
     * @param msecSendInterval
     *            Interval (milliseconds) between message sends.
     * @throws IOException
     *             When IO errors occur.
     * @throws CircuitBreakerException
     *             When SMTP circuit is broken.
     */
    private void sendMimeFileBatch(final EmailService emailService,
            final DirectoryStream<Path> dirStream)
            throws IOException, CircuitBreakerException {

        // Single instance used for all MIME messages (batch).
        Transport transport = null;

        long msecSendInterval = DateUtil.DURATION_MSEC_SECOND;

        try {
            /*
             * Iterate over paths in the directory and read/send stored MIME
             * files.
             */
            for (final Path p : dirStream) {

                if (this.isInterrupted()) {
                    break;
                }

                final BasicFileAttributes attrs =
                        Files.readAttributes(p, BasicFileAttributes.class);

                if (!attrs.isRegularFile()) {
                    continue;
                }

                try {
                    Thread.sleep(msecSendInterval);

                    if (transport == null) {

                        // Lazy create session and connect.
                        final javax.mail.Session session =
                                emailService.createSendMailSession();
                        transport = session.getTransport();
                        transport.connect();

                        // ... and get send interval for next cycle.
                        final ConfigManager cm = ConfigManager.instance();
                        msecSendInterval = cm.getConfigLong(
                                Key.MAIL_OUTBOX_SEND_INTERVAL_MSEC);
                    }

                    // Send email.
                    final MimeMessage mimeMsg =
                            emailService.sendEmail(transport, p.toFile());

                    // Logging.
                    final String msgKey = "EmailOutboxMonitor.mailsent";
                    final String subject = mimeMsg.getSubject();
                    final String sendTo =
                            mimeMsg.getRecipients(Message.RecipientType.TO)[0]
                                    .toString();
                    final String mailSize = NumberUtil
                            .humanReadableByteCount(mimeMsg.getSize(), true);

                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug(localizeLogMsg(msgKey, subject, sendTo,
                                mailSize));
                    }

                    AdminPublisher.instance().publish(PubTopicEnum.SMTP,
                            PubLevelEnum.INFO,
                            localizeSysMsg(msgKey, subject, sendTo, mailSize));

                } catch (MessagingException e) {

                    LOGGER.error(e.getMessage(), e);

                    AdminPublisher.instance().publish(PubTopicEnum.SMTP,
                            PubLevelEnum.ERROR, e.getMessage());

                } catch (InterruptedException e) {
                    break;
                }

                Files.delete(p);
            }

        } finally {

            if (transport != null) {
                try {
                    transport.close();
                } catch (final MessagingException e) {
                    // ignore
                }
            }
        }
    }

    /**
     * Polls the email outbox for messages to send in batch.
     * <p>
     * Note: traditional polling is chosen above {@link WatchService} because
     * sending mails is less time critical and is simpler to implement.
     * </p>
     *
     * @throws IOException
     *             When IO errors occur.
     * @throws CircuitBreakerException
     *             When SMTP circuit is broken.
     */
    private void pollEmailOutbox() throws IOException, CircuitBreakerException {

        final ConfigManager cm = ConfigManager.instance();

        final EmailService emailService =
                ServiceContext.getServiceFactory().getEmailService();

        final long msecStart = System.currentTimeMillis();

        int i = 0;

        while (!this.isInterrupted()) {

            final long msecHeartbeat =
                    cm.getConfigLong(Key.MAIL_OUTBOX_POLL_HEARTBEAT_MSEC);

            try {
                Thread.sleep(msecHeartbeat);
            } catch (InterruptedException e) {
                break;
            }

            if (this.isInterrupted()) {
                break;
            }

            i++;

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(String.format("Email Watch [%d]", i));
            }

            try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(
                    emailService.getOutboxMimeFilesPath(),
                    emailService.getOutboxMimeFileGlob());) {

                this.sendMimeFileBatch(emailService, dirStream);
            }

            /*
             * STOP if the max monitor time has elapsed.
             */
            final long timeElapsed =
                    System.currentTimeMillis() + msecHeartbeat - msecStart;

            if (timeElapsed >= MAX_MONITOR_MSEC) {

                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Email Watch: time elapsed.");
                }
                break;
            }

        } // end-while
    }

}
