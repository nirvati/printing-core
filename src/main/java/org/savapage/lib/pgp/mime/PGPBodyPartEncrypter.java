/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2017 Datraverse B.V.
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
package org.savapage.lib.pgp.mime;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

import javax.activation.DataHandler;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.internet.ContentDisposition;
import javax.mail.internet.ContentType;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimePart;
import javax.mail.internet.MimeUtility;

import org.apache.commons.io.IOUtils;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.savapage.lib.pgp.PGPBaseException;
import org.savapage.lib.pgp.PGPHelper;

import com.sun.mail.util.CRLFOutputStream;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class PGPBodyPartEncrypter {

    /** */
    private BodyPart contentPart;

    /** */
    private BodyPart controlPart;

    /** */
    private BodyPart encryptedPart;

    /**
     * Secret key used for signing.
     */
    private final PGPSecretKey secretKeyForSigning;

    /** Passphrase for secret key. */
    private final String passphrase;

    /**
     * Public keys used for encryption.
     */
    private final List<PGPPublicKey> publicKeys;

    /**
     * Constructor.
     *
     * @param secretKey
     *            Secret key for signing.
     * @param secretKeyPassword
     *            The password of the secret (private) key.
     * @param publicKeyList
     *            Public keys for encryption.
     */
    public PGPBodyPartEncrypter(final PGPSecretKey secretKey,
            final String secretKeyPassword,
            final List<PGPPublicKey> publicKeyList) {

        this.secretKeyForSigning = secretKey;
        this.passphrase = secretKeyPassword;
        this.publicKeys = publicKeyList;
    }

    /**
     * Gets body part as string.
     *
     * @param bodyPart
     *            The body part
     * @return {@link BodyPart} as string.
     * @throws IOException
     *             When output stream error.
     * @throws MessagingException
     *             When mail message error.
     */
    private static String bodyPartAsString(final BodyPart bodyPart)
            throws IOException, MessagingException {

        updateHeaders(bodyPart);

        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final CRLFOutputStream nlos = new CRLFOutputStream(bos);

        try {
            bodyPart.writeTo(nlos);
        } finally {
            nlos.close();
        }
        return bos.toString();
    }

    /**
     * Recursively updates the headers of a {@link MimeMultipart} or
     * {@link MimePart}.
     * <p>
     * This method is based on private {@code updateHeaders()} code from the
     * {@link MimeBodyPart} and the {@link MimeMultipart} implementation from
     * SUN.
     * </p>
     *
     * @param part
     *            Either a {@link MimeMultipart} or {@link MimePart}.
     * @throws MessagingException
     *             When mail message error.
     */
    private static void updateHeaders(final Object part)
            throws MessagingException {

        if (part instanceof MimeMultipart) {
            MimeMultipart mmp = (MimeMultipart) part;
            for (int i = 0; i < mmp.getCount(); i++) {
                // recurse
                updateHeaders(mmp.getBodyPart(i));
            }
        } else if (part instanceof MimePart) {
            MimePart mp = (MimePart) part;
            DataHandler dh = mp.getDataHandler();
            if (dh == null) { // Huh ?
                return;
            }

            try {
                String type = dh.getContentType();
                boolean composite = false;

                ContentType cType = new ContentType(type);
                if (cType.match("multipart/*")) {
                    // If multipart, recurse
                    composite = true;
                    Object o = dh.getContent();
                    updateHeaders(o);
                } else if (cType.match("message/rfc822")) {
                    composite = true;
                }

                // Now, let's update our own headers ...
                // Content-type, but only if we don't already have one
                if (mp.getHeader("Content-Type") == null) {
                    /*
                     * Pull out "filename" from Content-Disposition, and use
                     * that to set the "name" parameter. This is to satisfy
                     * older MUAs (DtMail, Roam and probably a bunch of others).
                     */
                    String s = mp.getHeader("Content-Disposition", null);
                    if (s != null) {
                        // Parse the header ..
                        ContentDisposition cd = new ContentDisposition(s);
                        String filename = cd.getParameter("filename");
                        if (filename != null) {
                            cType.setParameter("name", filename);
                            type = cType.toString();
                        }
                    }
                    mp.setHeader("Content-Type", type);
                }

                // Content-Transfer-Encoding, but only if we don't
                // already have one
                if (!composite // not allowed on composite parts
                        && (mp.getHeader(
                                "Content-Transfer-Encoding") == null)) {
                    mp.setHeader("Content-Transfer-Encoding",
                            MimeUtility.getEncoding(dh));
                }
            } catch (IOException e) {
                throw new MessagingException(e.getMessage(), e);
            }
        }
    }

    /**
     * Encrypts the content.
     *
     * @throws MessagingException
     *             When encryption errors.
     */
    public void encrypt() throws MessagingException {

        if (contentPart == null) {
            throw new PGPMimeException("No content part to sign.");
        }

        final String embeddedFileName = "plain.txt";
        final Date embeddedFileDate = new Date();

        //
        ByteArrayOutputStream targetStream = null;
        InputStream contentStream = null;

        controlPart = new MimeBodyPart();
        controlPart.setContent("Version: 1\n", "application/pgp-encrypted");

        try {
            final String bodyPartStr = bodyPartAsString(contentPart);

            targetStream = new ByteArrayOutputStream();

            contentStream = new ByteArrayInputStream(
                    bodyPartStr.getBytes(StandardCharsets.UTF_8));

            PGPHelper.instance().encryptOnePassSignature(contentStream,
                    targetStream, this.secretKeyForSigning, this.passphrase,
                    this.publicKeys, embeddedFileName, embeddedFileDate);

            encryptedPart = new MimeBodyPart();
            encryptedPart.setText(targetStream.toString());

            updateHeaders(encryptedPart);

            encryptedPart.setHeader("Content-Type", String.format(
                    "%s; name=encrypted.asc", encryptedPart.getContentType()));

        } catch (IOException | PGPBaseException e) {
            throw new PGPMimeException(e.getMessage(), e);
        } finally {
            IOUtils.closeQuietly(targetStream);
            IOUtils.closeQuietly(contentStream);
        }
    }

    /**
     * @param part
     *            The content part.
     */
    public void setContentPart(final BodyPart part) {
        this.contentPart = part;
    }

    /**
     * @return The content part.
     */
    public BodyPart getContentPart() {
        return this.contentPart;
    }

    /**
     * @return The encrypted part.
     */
    public BodyPart getEncryptedPart() {
        return this.encryptedPart;
    }

    /**
     * @return The control part.
     */
    public BodyPart getControlPart() {
        return this.controlPart;
    }

}
