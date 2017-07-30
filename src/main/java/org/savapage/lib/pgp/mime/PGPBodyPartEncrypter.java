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

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;

import org.apache.commons.io.IOUtils;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.savapage.lib.pgp.PGPBaseException;
import org.savapage.lib.pgp.PGPHelper;

/**
 * PGP/MIME sign and encrypt mail body part.
 *
 * @author Rijk Ravestein
 *
 */
public final class PGPBodyPartEncrypter extends PGPBodyPartProcessor {

    /**
     * The RFC 1747 (Security Multiparts for MIME) Control part.
     */
    private BodyPart controlPart;

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
        super(secretKey, secretKeyPassword);
        this.publicKeys = publicKeyList;
    }

    /**
     * Signs and Encrypts the content.
     *
     * @throws MessagingException
     *             When encryption errors.
     */
    public void encrypt() throws MessagingException {

        if (getContentPart() == null) {
            throw new PGPMimeException("content part is missing.");
        }

        final String embeddedFileName = "plain.txt";
        final Date embeddedFileDate = new Date();

        //
        InputStream contentStream = null;
        ByteArrayOutputStream contentStreamEncrypted = null;

        controlPart = new MimeBodyPart();
        controlPart.setContent("Version: 1\n", "application/pgp-encrypted");

        try {
            contentStream =
                    new ByteArrayInputStream(bodyPartAsString(getContentPart())
                            .getBytes(StandardCharsets.UTF_8));

            contentStreamEncrypted = new ByteArrayOutputStream();

            PGPHelper.instance().encryptOnePassSignature(contentStream,
                    contentStreamEncrypted, this.getSecretKey(),
                    this.getSecretKeyPassphrase(), this.publicKeys,
                    embeddedFileName, embeddedFileDate);

            final BodyPart encryptedPart = new MimeBodyPart();
            encryptedPart.setText(contentStreamEncrypted.toString());

            updateHeaders(encryptedPart);

            encryptedPart.setHeader("Content-Type", String.format(
                    "%s; name=encrypted.asc", encryptedPart.getContentType()));

            this.setProcessedPart(encryptedPart);

        } catch (IOException | PGPBaseException e) {
            throw new PGPMimeException(e.getMessage(), e);
        } finally {
            IOUtils.closeQuietly(contentStreamEncrypted);
            IOUtils.closeQuietly(contentStream);
        }
    }

    /**
     * @return The RFC 1747 (Security Multiparts for MIME) Control part.
     */
    public BodyPart getControlPart() {
        return this.controlPart;
    }

}
