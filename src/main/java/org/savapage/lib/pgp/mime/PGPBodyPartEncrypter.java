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

import org.savapage.lib.pgp.PGPBaseException;
import org.savapage.lib.pgp.PGPHelper;
import org.savapage.lib.pgp.PGPPublicKeyInfo;
import org.savapage.lib.pgp.PGPSecretKeyInfo;

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
    private final List<PGPPublicKeyInfo> publicKeys;

    /**
     * Constructor.
     *
     * @param secretKeyInfo
     *            the {@link secretKeyInfo}
     * @param publicKeyList
     *            Public keys for encryption.
     */
    public PGPBodyPartEncrypter(final PGPSecretKeyInfo secretKeyInfo,
            final List<PGPPublicKeyInfo> publicKeyList) {
        super(secretKeyInfo);
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
        this.controlPart = new MimeBodyPart();
        this.controlPart.setContent("Version: 1\n",
                "application/pgp-encrypted");

        try (ByteArrayOutputStream contentStreamEncrypted =
                new ByteArrayOutputStream();

                InputStream contentStream = new ByteArrayInputStream(
                        bodyPartAsString(getContentPart())
                                .getBytes(StandardCharsets.UTF_8)); //
        ) {
            PGPHelper.instance().encryptOnePassSignature(contentStream,
                    contentStreamEncrypted, this.getSecretKeyInfo(),
                    this.publicKeys, embeddedFileName, embeddedFileDate, true);

            final BodyPart encryptedPart = new MimeBodyPart();
            encryptedPart.setText(contentStreamEncrypted.toString());

            updateHeaders(encryptedPart);

            encryptedPart.setHeader("Content-Type", String.format(
                    "%s; name=encrypted.asc", encryptedPart.getContentType()));

            this.setProcessedPart(encryptedPart);

        } catch (IOException | PGPBaseException e) {
            throw new PGPMimeException(e.getMessage(), e);
        }
    }

    /**
     * @return The RFC 1747 (Security Multiparts for MIME) Control part.
     */
    public BodyPart getControlPart() {
        return this.controlPart;
    }

}
