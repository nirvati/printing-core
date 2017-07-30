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

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.lang3.StringUtils;

/**
 * PGP encrypted MimeMultipart.
 *
 * @author Rijk Ravestein
 *
 */
public final class PGPMimeMultipart extends MimeMultipart {

    /**
     * RFC3156.
     */
    private static final String CONTENTTYPE_SIGN_PROTOCOL =
            "application/pgp-signature";

    /**
     *
     */
    private static final String CONTENTTYPE_SIGN_MIME_TYPE = "multipart/signed";

    /**
     *
     */
    private static final String CONTENTTYPE_ENCRYPT_PROTOCOL =
            "application/pgp-encrypted";

    /**
     *
     */
    private static final String CONTENTTYPE_ENCRYPT_MIME_TYPE =
            "multipart/encrypted";

    /**
     * Create instance with content type.
     *
     * @param contentType
     *            The content type.
     * @throws MessagingException
     *             When header update fails.
     */
    private PGPMimeMultipart(final String contentType)
            throws MessagingException {
        super();
        this.contentType = contentType;
        updateHeaders();
    }

    @Override
    public void writeTo(final OutputStream out)
            throws MessagingException, IOException {

        // (1) Update headers.
        updateHeaders();

        // (2) Business as usual.
        super.writeTo(out);
    }

    /**
     * Creates instance of {@code bodyPart} using {@code encrypter}.
     *
     * @param bodyPart
     *            The part to encrypt.
     * @param encrypter
     *            The PGP/MIME encrypter.
     * @return The {@link PGPMimeMultipart} instance.
     * @throws MessagingException
     *             If encryption fails.
     */
    public static PGPMimeMultipart create(final BodyPart bodyPart,
            final PGPBodyPartEncrypter encrypter) throws MessagingException {

        encrypter.setContentPart(bodyPart);
        return create(encrypter);
    }

    /**
     * Creates instance of {@code bodyPart} using {@code signer}.
     *
     * @param bodyPart
     *            The part to encrypt.
     * @param signer
     *            The PGP/MIME signer.
     * @return The {@link PGPMimeMultipart} instance.
     * @throws MessagingException
     *             If encryption fails.
     */
    public static PGPMimeMultipart create(final BodyPart bodyPart,
            final PGPBodyPartSigner signer) throws MessagingException {

        signer.setContentPart(bodyPart);
        return create(signer);
    }

    /**
     * Creates instance of {@code multiPart} using {@code encrypter}.
     *
     * @param multiPart
     *            The part to encrypt.
     * @param encrypter
     *            The PGP/MIME encrypter.
     * @return The {@link PGPMimeMultipart} instance.
     * @throws MessagingException
     *             If encryption fails.
     */
    public static PGPMimeMultipart create(final MimeMultipart multiPart,
            final PGPBodyPartEncrypter encrypter) throws MessagingException {

        final BodyPart bodyPart = new MimeBodyPart();
        bodyPart.setContent(multiPart);
        return create(bodyPart, encrypter);
    }

    /**
     * Creates instance of {@code multiPart} using {@code signer}.
     *
     * @param multiPart
     *            The part to encrypt.
     * @param signer
     *            The PGP/MIME signer.
     * @return The {@link PGPMimeMultipart} instance.
     * @throws MessagingException
     *             If encryption fails.
     */
    public static PGPMimeMultipart create(final MimeMultipart multiPart,
            final PGPBodyPartSigner signer) throws MessagingException {

        final BodyPart bodyPart = new MimeBodyPart();
        bodyPart.setContent(multiPart);
        return create(bodyPart, signer);
    }

    /**
     * Creates instance using {@code encrypter}.
     *
     * @param encrypter
     *            The PGP/MIME encrypter.
     * @return The {@link PGPMimeMultipart} instance.
     * @throws MessagingException
     *             If encryption fails.
     */
    private static PGPMimeMultipart create(final PGPBodyPartEncrypter encrypter)
            throws MessagingException {

        encrypter.encrypt();

        final String boundery = String.format("encrypted.%s",
                StringUtils.replace(UUID.randomUUID().toString(), "-", ""));

        final String contentType =
                String.format("%s; protocol=\"%s\"; boundary=\"%s\"",
                        CONTENTTYPE_ENCRYPT_MIME_TYPE,
                        CONTENTTYPE_ENCRYPT_PROTOCOL, boundery);

        final PGPMimeMultipart mpart = new PGPMimeMultipart(contentType);

        mpart.addBodyPart(encrypter.getControlPart(), 0);
        mpart.addBodyPart(encrypter.getProcessedPart(), 1);

        return (mpart);
    }

    /**
     * Creates instance using {@code signer}.
     *
     * @param signer
     *            The PGP/MIME signer.
     * @return The {@link PGPMimeMultipart} instance.
     * @throws MessagingException
     *             If encryption fails.
     */
    private static PGPMimeMultipart create(final PGPBodyPartSigner signer)
            throws MessagingException {

        signer.sign();

        final String boundery = String.format("signed.%s",
                StringUtils.replace(UUID.randomUUID().toString(), "-", ""));

        final String contentType =
                String.format("%s; micalg=%s; protocol=\"%s\"; boundary=\"%s\"",
                        CONTENTTYPE_SIGN_MIME_TYPE,
                        signer.getHashAlgorithm().getMicalg(),
                        CONTENTTYPE_SIGN_PROTOCOL, boundery);

        final PGPMimeMultipart mpart = new PGPMimeMultipart(contentType);

        mpart.addBodyPart(signer.getContentPart(), 0);
        mpart.addBodyPart(signer.getProcessedPart(), 1);

        return mpart;
    }

}
