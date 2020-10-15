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

import java.io.File;
import java.io.IOException;
import java.security.Security;
import java.util.List;

import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.savapage.lib.pgp.PGPBaseException;
import org.savapage.lib.pgp.PGPHelper;
import org.savapage.lib.pgp.PGPSecretKeyInfo;

/**
 * PGP/MIME helper methods.
 *
 * @author Rijk Ravestein
 *
 */
public final class PGPMimeHelper {

    /** */
    private static final class SingletonHolder {
        /** */
        static final PGPMimeHelper SINGLETON = new PGPMimeHelper();
    }

    /**
     * Singleton instantiation.
     */
    private PGPMimeHelper() {
        Security.addProvider(new BouncyCastleProvider());
    }

    /**
     * @return The singleton instance.
     */
    public static PGPMimeHelper instance() {
        return SingletonHolder.SINGLETON;
    }

    /**
     * Creates a PGP signed and encrypted {@link Multipart} from body content
     * and file attachments.
     *
     * @param bodyContent
     *            The body content.
     * @param secretKeyInfo
     *            The Secret key container used for signing.
     * @param signPublicKeyList
     *            The list of public PGP key files, used for encryption.
     * @param attachments
     *            The mail attachments.
     * @return The signed and encrypted {@link Multipart}.
     * @throws MessagingException
     *             When mail message error.
     * @throws IOException
     *             When mail attachment {@link File} error.
     * @throws PGPMimeException
     *             When error occurs.
     */
    public Multipart createSignedEncrypted(final String bodyContent,
            final PGPSecretKeyInfo secretKeyInfo,
            final List<File> signPublicKeyList, final List<File> attachments)
            throws PGPMimeException {

        final PGPHelper helper = PGPHelper.instance();

        try {
            final PGPBodyPartEncrypter encrypter = new PGPBodyPartEncrypter(
                    secretKeyInfo, helper.getPublicKeyList(signPublicKeyList));

            final MimeBodyPart mbp = new MimeBodyPart();

            mbp.setText(bodyContent);

            final PGPMimeMultipart mme;

            if (attachments.isEmpty()) {
                mme = PGPMimeMultipart.create(mbp, encrypter);
            } else {
                final MimeMultipart mmultip = new MimeMultipart();

                mmultip.addBodyPart(mbp);

                for (final File attachment : attachments) {
                    final MimeBodyPart mbp2 = new MimeBodyPart();
                    mbp2.attachFile(attachment);
                    mbp2.setFileName(attachment.getName());
                    mmultip.addBodyPart(mbp2);
                }

                mme = PGPMimeMultipart.create(mmultip, encrypter);
            }

            return mme;

        } catch (MessagingException | IOException | PGPBaseException e) {
            throw new PGPMimeException(e.getMessage(), e);
        }
    }

    /**
     * Creates a PGP signed {@link Multipart} from body content and file
     * attachments.
     *
     * @param bodyContent
     *            The body content.
     * @param secretKeyInfo
     *            The Secret key container used for signing.
     * @param attachments
     *            The mail attachments.
     * @return The signed {@link Multipart}.
     * @throws PGPMimeException
     *             When error occurs.
     */
    public Multipart createSigned(final String bodyContent,
            final PGPSecretKeyInfo secretKeyInfo, final List<File> attachments)
            throws PGPMimeException {

        try {
            final PGPBodyPartSigner signer =
                    new PGPBodyPartSigner(secretKeyInfo);

            final MimeBodyPart mbp = new MimeBodyPart();

            mbp.setText(bodyContent);

            final PGPMimeMultipart mme;

            if (attachments.isEmpty()) {
                mme = PGPMimeMultipart.create(mbp, signer);
            } else {
                final MimeMultipart mmultip = new MimeMultipart();

                mmultip.addBodyPart(mbp);

                for (final File attachment : attachments) {
                    final MimeBodyPart mbp2 = new MimeBodyPart();
                    mbp2.attachFile(attachment);
                    mbp2.setFileName(attachment.getName());
                    mmultip.addBodyPart(mbp2);
                }

                mme = PGPMimeMultipart.create(mmultip, signer);
            }

            return mme;

        } catch (MessagingException | IOException e) {
            throw new PGPMimeException(e.getMessage(), e);
        }

    }

}
