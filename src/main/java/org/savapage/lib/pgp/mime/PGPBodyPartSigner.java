/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2018 Datraverse B.V.
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

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;

import org.savapage.lib.pgp.PGPBaseException;
import org.savapage.lib.pgp.PGPHashAlgorithmEnum;
import org.savapage.lib.pgp.PGPHelper;
import org.savapage.lib.pgp.PGPSecretKeyInfo;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class PGPBodyPartSigner extends PGPBodyPartProcessor {

    /**
     *
     */
    private final PGPHashAlgorithmEnum hashAlgorithm =
            PGPHashAlgorithmEnum.SHA256;

    /**
     *
     * @param secretKeyInfo
     *            the {@link secretKeyInfo}
     */
    public PGPBodyPartSigner(final PGPSecretKeyInfo secretKeyInfo) {
        super(secretKeyInfo);
    }

    /**
     *
     * @return The hash algorithm.
     */
    public PGPHashAlgorithmEnum getHashAlgorithm() {
        return hashAlgorithm;
    }

    /**
     *
     * @throws PGPMimeException
     *             When error.
     */
    public void sign() throws PGPMimeException {

        if (getContentPart() == null) {
            throw new PGPMimeException("content part is missing.");
        }

        try (ByteArrayOutputStream contentStreamSigned =
                new ByteArrayOutputStream();
                InputStream contentStream = new ByteArrayInputStream(
                        bodyPartAsString(getContentPart())
                                .getBytes(StandardCharsets.UTF_8));) {

            PGPHelper.instance().createSignature(contentStream,
                    contentStreamSigned, this.getSecretKeyInfo(),
                    this.hashAlgorithm, true);

            final BodyPart signedPart = new MimeBodyPart();
            signedPart.setContent(contentStreamSigned.toString(),
                    "application/pgp-signature");
            updateHeaders(signedPart);
            signedPart.setHeader("Content-Type",
                    "application/pgp-signature; " + "name=signature.asc");

            this.setProcessedPart(signedPart);

        } catch (IOException | MessagingException | PGPBaseException e) {
            throw new PGPMimeException(e.getMessage(), e);
        }
    }

}
