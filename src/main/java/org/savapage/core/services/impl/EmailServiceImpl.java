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
package org.savapage.core.services.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.UUID;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.inject.Singleton;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.SendFailedException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

import org.apache.commons.lang3.StringUtils;
import org.savapage.core.SpInfo;
import org.savapage.core.circuitbreaker.CircuitBreaker;
import org.savapage.core.circuitbreaker.CircuitBreakerException;
import org.savapage.core.circuitbreaker.CircuitBreakerOperation;
import org.savapage.core.circuitbreaker.CircuitNonTrippingException;
import org.savapage.core.circuitbreaker.CircuitTrippingException;
import org.savapage.core.config.CircuitBreakerEnum;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.config.ServerPathEnum;
import org.savapage.core.services.EmailService;
import org.savapage.core.services.helpers.email.EmailMsgParms;
import org.savapage.core.util.FileSystemHelper;
import org.savapage.lib.pgp.PGPPublicKeyInfo;
import org.savapage.lib.pgp.PGPSecretKeyInfo;
import org.savapage.lib.pgp.mime.PGPBodyPartEncrypter;
import org.savapage.lib.pgp.mime.PGPBodyPartSigner;
import org.savapage.lib.pgp.mime.PGPMimeMultipart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rijk Ravestein
 *
 */
@Singleton
public final class EmailServiceImpl extends AbstractService
        implements EmailService {

    /** */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(EmailServiceImpl.class);

    /**
     *
     */
    private static final String MIME_MULTIPART_SUBTYPE_RELATED = "related";

    /**
     *
     */
    private static final String MIME_FILE_SUFFIX = "mime";

    /**
     * .
     */
    private static final String MIME_FILE_GLOB = "*." + MIME_FILE_SUFFIX;

    /**
     * Specifies the SSL protocols that will be enabled for SSL connections. The
     * property value is a whitespace separated list of tokens acceptable to the
     * javax.net.ssl.SSLSocket.setEnabledProtocols method.
     */
    private String smtpSSLProtocols;

    /**
     * Creates a session for <i>writing</i> the MIME message file.
     *
     * @return The {@link javax.mail.Session}.
     */
    private javax.mail.Session createMimeMsgSession() {
        return javax.mail.Session.getInstance(new Properties());
    }

    /**
     * Creates a session for <i>sending</i> the mail.
     *
     * @return The {@link javax.mail.Session}.
     */
    private javax.mail.Session createSendMailSession() {

        final ConfigManager conf = ConfigManager.instance();

        final String host = conf.getConfigValue(IConfigProp.Key.MAIL_SMTP_HOST);
        final String port = conf.getConfigValue(IConfigProp.Key.MAIL_SMTP_PORT);

        final String username =
                conf.getConfigValue(IConfigProp.Key.MAIL_SMTP_USER_NAME);

        final String password =
                conf.getConfigValue(IConfigProp.Key.MAIL_SMTP_PASSWORD);

        final String security =
                conf.getConfigValue(IConfigProp.Key.MAIL_SMTP_SECURITY);

        final boolean debug =
                conf.isConfigValue(IConfigProp.Key.MAIL_SMTP_DEBUG);

        /*
         * Create properties and get the default Session
         */
        final java.util.Properties props = new java.util.Properties();

        /*
         * Timeout (in milliseconds) for establishing the SMTP connection.
         */
        props.put("mail.smtp.connectiontimeout",
                conf.getConfigInt(Key.MAIL_SMTP_CONNECTIONTIMEOUT));

        /*
         * The timeout (milliseconds) for sending the mail messages.
         */
        props.put("mail.smtp.timeout",
                conf.getConfigInt(Key.MAIL_SMTP_TIMEOUT));

        //
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);

        javax.mail.Authenticator authenticator = null;

        if (StringUtils.isNotBlank(username)) {
            props.put("mail.smtp.auth", "true");
            authenticator = new javax.mail.Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password);
                }
            };
        }

        final boolean isSTARTTLS =
                security.equalsIgnoreCase(IConfigProp.SMTP_SECURITY_V_STARTTLS);
        final boolean isSSLTLS =
                security.equalsIgnoreCase(IConfigProp.SMTP_SECURITY_V_SSL);

        if (isSTARTTLS) {
            props.put("mail.smtp.starttls.enable", "true");
        } else if (isSSLTLS) {
            props.put("mail.smtp.socketFactory.port", port);
            props.put("mail.smtp.socketFactory.class",
                    "javax.net.ssl.SSLSocketFactory");
        }

        if ((isSTARTTLS || isSSLTLS)
                && StringUtils.isNotBlank(this.smtpSSLProtocols)) {
            props.put("mail.smtp.ssl.protocols", this.smtpSSLProtocols);
        }

        /*
         * Get a new session instance. Do NOT use the getDefaultInstance().
         */
        final javax.mail.Session session =
                javax.mail.Session.getInstance(props, authenticator);

        session.setDebug(debug);

        return session;
    }

    /**
     * <p>
     * See the <a href=
     * "https://javamail.java.net/nonav/docs/api/com/sun/mail/smtp/package-summary.html"
     * >JavaMail API documentation</a>.
     * </p>
     *
     * @param msgParms
     * @param isSync
     * @return
     * @throws MessagingException
     * @throws IOException
     */
    private MimeMessage createMimeMessage(final EmailMsgParms msgParms,
            final boolean isSync) throws MessagingException, IOException {

        final ConfigManager conf = ConfigManager.instance();

        final javax.mail.Session session;

        if (isSync) {
            session = this.createSendMailSession();
        } else {
            session = this.createMimeMsgSession();
        }

        /*
         * Create a message
         */
        final MimeMessage msg = new MimeMessage(session);

        // from
        final InternetAddress addrFrom = new InternetAddress();

        addrFrom.setAddress(
                conf.getConfigValue(IConfigProp.Key.MAIL_FROM_ADDRESS));
        addrFrom.setPersonal(
                conf.getConfigValue(IConfigProp.Key.MAIL_FROM_NAME));
        msg.setFrom(addrFrom);

        // reply-to
        if (conf.getConfigValue(
                IConfigProp.Key.MAIL_REPLY_TO_ADDRESS) != null) {
            final InternetAddress addrReplyTo = new InternetAddress();
            addrReplyTo.setAddress(
                    conf.getConfigValue(IConfigProp.Key.MAIL_REPLY_TO_ADDRESS));
            final String name =
                    conf.getConfigValue(IConfigProp.Key.MAIL_REPLY_TO_NAME);
            if (name != null) {
                addrReplyTo.setPersonal(name);
            }
            final InternetAddress[] addressReplyTo = { addrReplyTo };
            msg.setReplyTo(addressReplyTo);
        }

        // to
        final InternetAddress addrTo = new InternetAddress();
        addrTo.setAddress(msgParms.getToAddress());

        if (msgParms.getToName() != null) {
            addrTo.setPersonal(msgParms.getToName());
        }

        final InternetAddress[] address = { addrTo };
        msg.setRecipients(Message.RecipientType.TO, address);

        // subject
        msg.setSubject(msgParms.getSubject());

        // date
        msg.setSentDate(new java.util.Date());

        /*
         * Create the Multipart and its parts to it.
         */
        final MimeMultipart mp;

        if (msgParms.getCidMap().isEmpty()) {
            mp = new MimeMultipart();
        } else {
            mp = new MimeMultipart(MIME_MULTIPART_SUBTYPE_RELATED);
        }

        // create and fill the first message part
        final MimeBodyPart mbp1 = new MimeBodyPart();
        mbp1.setText(msgParms.getBody());
        mbp1.setHeader("Content-Type", msgParms.getContentType());

        mp.addBodyPart(mbp1);

        if (msgParms.getFileAttach() != null) {

            final MimeBodyPart mbp2 = new MimeBodyPart();

            /*
             * (1) attach
             */
            mbp2.attachFile(msgParms.getFileAttach());
            /*
             * (2) set the filename
             */
            if (msgParms.getFileName() != null) {
                mbp2.setFileName(msgParms.getFileName());
            }
            /*
             * (3) add
             */
            mp.addBodyPart(mbp2);
        }

        // Add CIDs to the multipart.
        for (final Entry<String, DataSource> entry : msgParms.getCidMap()
                .entrySet()) {

            final MimeBodyPart mbp = new MimeBodyPart();

            mbp.setDataHandler(new DataHandler(entry.getValue()));
            mbp.setContentID(String.format("<%s>", entry.getKey()));
            mbp.setDisposition(MimeBodyPart.INLINE);

            mp.addBodyPart(mbp);
        }

        msg.setContent(applyPgpMime(mp, msgParms.getPublicKeyList()));
        return msg;
    }

    /**
     * Applies PGP/MIME on the vanilla MimeMultipart when PGP/MIME parameters
     * are present.
     *
     * @param mp
     *            The vanilla MimeMultipart.
     * @param publicKeyList
     *            The List of PGP public keys to sign with.
     * @return The part to be used in the mail message, which is either the
     *         vanilla message, or the vanilla message processed (signed and
     *         optionally encrypted) to PGP/MIME.
     * @throws MessagingException
     *             When MIME message error.
     */
    private MimeMultipart applyPgpMime(final MimeMultipart mp,
            final List<PGPPublicKeyInfo> publicKeyList)
            throws MessagingException {

        final PGPSecretKeyInfo secretKeyInfo =
                ConfigManager.instance().getPGPSecretKeyInfo();

        if (secretKeyInfo == null || !ConfigManager.instance()
                .isConfigValue(Key.MAIL_PGP_MIME_SIGN)) {
            return mp;
        }

        if (publicKeyList == null || publicKeyList.isEmpty() || !ConfigManager
                .instance().isConfigValue(Key.MAIL_PGP_MIME_ENCRYPT)) {
            final PGPBodyPartSigner signer =
                    new PGPBodyPartSigner(secretKeyInfo);
            return PGPMimeMultipart.create(mp, signer);
        }

        final PGPBodyPartEncrypter encrypter =
                new PGPBodyPartEncrypter(secretKeyInfo, publicKeyList);
        return PGPMimeMultipart.create(mp, encrypter);
    }

    /**
     * Sends a {@link MimeMessage} using the
     * {@link CircuitBreakerEnum#SMTP_CONNECTION}.
     *
     * @param msg
     *            The a {@link MimeMessage}.
     * @throws CircuitBreakerException
     *             When {@link CircuitBreakerEnum#SMTP_CONNECTION} is not
     *             closed.
     * @throws InterruptedException
     *             When the thread is interrupted.
     */
    private static void sendMimeMessage(final MimeMessage msg)
            throws InterruptedException, CircuitBreakerException {

        final CircuitBreakerOperation operation =
                new CircuitBreakerOperation() {

                    @Override
                    public Object execute(final CircuitBreaker circuitBreaker) {

                        if (!ConfigManager.isConnectedToInternet()) {
                            throw new CircuitTrippingException(
                                    "Not connected to the Internet.");
                        }

                        try {
                            javax.mail.Transport.send(msg);
                        } catch (SendFailedException e) {
                            throw new CircuitNonTrippingException(e);
                        } catch (MessagingException e) {
                            throw new CircuitTrippingException(e);
                        }
                        return null;
                    }
                };

        final CircuitBreaker breaker = ConfigManager
                .getCircuitBreaker(CircuitBreakerEnum.SMTP_CONNECTION);

        breaker.execute(operation);

    }

    @Override
    public void writeEmail(final EmailMsgParms parms)
            throws MessagingException, IOException {

        final String fileBaseName =
                String.format("%d-%s.%s", System.currentTimeMillis(),
                        UUID.randomUUID().toString(), MIME_FILE_SUFFIX);

        final Path filePathTemp =
                Paths.get(ConfigManager.getAppTmpDir(), fileBaseName);

        try (FileOutputStream fos =
                new FileOutputStream(filePathTemp.toFile());) {

            final MimeMessage msg = this.createMimeMessage(parms, false);
            msg.writeTo(fos);

            fos.flush();
            fos.getFD().sync();
        }

        final Path filePath = Paths.get(ConfigManager.getServerHome(),
                ServerPathEnum.EMAIL_OUTBOX.getPath(), fileBaseName);

        FileSystemHelper.doAtomicFileMove(filePathTemp, filePath);
    }

    @Override
    public void sendEmail(final EmailMsgParms parms)
            throws InterruptedException, CircuitBreakerException,
            MessagingException, IOException {

        sendMimeMessage(this.createMimeMessage(parms, true));
    }

    @Override
    public MimeMessage sendEmail(final File mimeFile) throws MessagingException,
            InterruptedException, CircuitBreakerException, IOException {

        try (FileInputStream fis = new FileInputStream(mimeFile);) {
            final MimeMessage msg =
                    new MimeMessage(createSendMailSession(), fis);
            sendMimeMessage(msg);
            return msg;
        }

    }

    @Override
    public Path getOutboxMimeFilesPath() {
        return Paths.get(ConfigManager.getServerHome(),
                ServerPathEnum.EMAIL_OUTBOX.getPath());
    }

    @Override
    public String getOutboxMimeFileGlob() {
        return MIME_FILE_GLOB;
    }

    @Override
    public void start() {

        try {
            final SSLContext ctx = SSLContext.getDefault();
            final SSLParameters params = ctx.getDefaultSSLParameters();

            final StringBuilder protocols = new StringBuilder();
            for (final String protocol : params.getProtocols()) {
                protocols.append(" ").append(protocol);
            }
            this.smtpSSLProtocols = protocols.toString().trim();

            SpInfo.instance().log(String.format("SSL Mail protocols [%s]",
                    this.smtpSSLProtocols));

        } catch (NoSuchAlgorithmException e) {
            LOGGER.warn(String.format("No SSL Mail protocols found: %s",
                    e.getMessage()));
            this.smtpSSLProtocols = null;
        }

    }

    @Override
    public void shutdown() {
        // no code intended.
    }

}
