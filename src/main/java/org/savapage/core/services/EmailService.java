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
package org.savapage.core.services;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.savapage.core.circuitbreaker.CircuitBreakerException;
import org.savapage.core.config.CircuitBreakerEnum;
import org.savapage.core.services.helpers.email.EmailMsgParms;

/**
 *
 * @since 0.9.9
 * @author Datraverse B.V.
 *
 */
public interface EmailService {

    /**
     * Gets the MIME files from the email outbox.
     *
     * @return The with {@link Path} of the email outbox objects;
     */
    Path getOutboxMimeFilesPath();

    /**
     *
     * @return The GLOB for retrieving the email outbox MIME files.
     */
    String getOutboxMimeFileGlob();

    /**
     * Writes an email MIME file (RFC822 formatted) in the email outbox (the
     * message is not send).
     *
     * @param parms
     *            The {@link EmailMsgParms}.
     * @throws MessagingException
     *             When MIME content is invalid.
     * @throws IOException
     *             When IO error writing the MIME file.
     */
    void writeEmail(EmailMsgParms parms) throws MessagingException, IOException;

    /**
     * Sends an email.
     *
     * @param parms
     *            The {@link EmailMsgParms}.
     * @throws InterruptedException
     *             When the thread is interrupted.
     * @throws CircuitBreakerException
     *             When {@link CircuitBreakerEnum#SMTP_CONNECTION} is not
     *             closed.
     * @throws MessagingException
     *             When MIME content is invalid.
     * @throws IOException
     *             When IO error writing the MIME file.
     */
    void sendEmail(EmailMsgParms parms) throws InterruptedException,
            CircuitBreakerException, MessagingException, IOException;

    /**
     * Reads a MIME message from file (RFC822 formatted) and sends it.
     *
     * @param mimeFile
     *            A {@link File} with a MIME message as RFC822 format stream
     *            (Standard for ARPA Internet Text Messages).
     *
     * @return The {@link MimeMessage} sent.
     *
     * @throws FileNotFoundException
     *             When file is not found.
     * @throws MessagingException
     *             When MIME content is invalid.
     * @throws InterruptedException
     *             When the thread is interrupted.
     * @throws CircuitBreakerException
     *             When {@link CircuitBreakerEnum#SMTP_CONNECTION} is not
     *             closed.
     */
    MimeMessage sendEmail(File mimeFile) throws FileNotFoundException,
            MessagingException, InterruptedException, CircuitBreakerException;

}
