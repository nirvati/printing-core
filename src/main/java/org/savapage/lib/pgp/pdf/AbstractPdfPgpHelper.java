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
package org.savapage.lib.pgp.pdf;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPSignature;
import org.savapage.lib.pgp.PGPBaseException;
import org.savapage.lib.pgp.PGPHelper;
import org.savapage.lib.pgp.PGPPublicKeyInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract PDF/PGP helper.
 *
 * @author Rijk Ravestein
 *
 */
public abstract class AbstractPdfPgpHelper implements PdfPgpSigner {

    /** */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(AbstractPdfPgpHelper.class);

    /**
     * The file name of the ASCII armored OnePass PGP signature stored in the
     * PDF.
     */
    protected static final String PGP_PAYLOAD_FILE_NAME = "verification.asc";

    /**
     * Max size of PDF owner password.
     */
    protected static final int PDF_OWNER_PASSWORD_SIZE = 32;

    /** */
    protected static final boolean ASCII_ARMOR = true;

    /**
     * Name of PDF attachment of ASCII armored public key of PDF Creator
     * (Signer).
     */
    protected static final String PGP_PUBKEY_FILENAME_CREATOR = "creator.asc";

    /**
     * Name of PDF attachment of ASCII armored public key of PDF Author.
     */
    protected static final String PGP_PUBKEY_FILENAME_AUTHOR = "author.asc";

    /**
     * The PGP mime-type for Armored Encrypted File.
     */
    protected static final String PGP_MIMETYPE_ASCII_ARMOR =
            "application/pgp-encrypted";

    /**
     * Encrypt owner password as extra parameter in PDF/PGP verification URL.
     */
    protected static final boolean ENCRYPT_OWNER_PW_AS_URL_PARM = true;

    /**
     * Singleton instantiation.
     */
    protected AbstractPdfPgpHelper() {
        Security.addProvider(new BouncyCastleProvider());
    }

    /**
     * Gets Public Key attachment of Author from PDF file.
     *
     * @param pdfFile
     *            The signed PDF file.
     * @return {@code null} when not found.
     * @throws IOException
     *             If IO error.
     * @throws PGPBaseException
     *             If PGP error.
     */
    protected abstract PGPPublicKeyInfo getPubKeyAuthor(File pdfFile)
            throws IOException, PGPBaseException;

    /**
     * Verifies a PGP signed (appended or embedded) PDF file.
     *
     * @param pdfFileSigned
     *            Signed PDF as input.
     * @param signPublicKey
     *            The {@link PGPPublicKey} of the private key the PGP signature
     *            content was signed with.
     * @return The {@link PdfPgpSignatureInfo}}.
     * @throws PGPBaseException
     *             When errors.
     * @throws IOException
     *             When File IO errors.
     */
    @Override
    public PdfPgpSignatureInfo verify(final File pdfFileSigned,
            final PGPPublicKey signPublicKey)
            throws PGPBaseException, IOException {

        final PdfPgpSignatureInfo sigInfo;

        try (InputStream istrPdf = new FileInputStream(pdfFileSigned);) {
            sigInfo = this.verify(istrPdf, signPublicKey);
        }

        if (sigInfo.isValid()) {
            sigInfo.setPubKeyAuthor(getPubKeyAuthor(pdfFileSigned));
        }

        return sigInfo;
    }

    /**
     *
     * @param parms
     *            parameters.
     * @return {@code null} when not present.
     */
    protected static String getOwnerPassword(final PdfPgpSignParms parms) {
        if (parms.getEncryptionProps() == null) {
            return null;
        }
        return parms.getEncryptionProps().getPw().getOwner();
    }

    /**
     * Verifies a PGP signed (appended or embedded) PDF file.
     *
     * @param istrPdfSigned
     *            Signed PDF document as input stream.
     * @param trustedPublicKey
     *            The trusted {@link PGPPublicKey}.
     * @return The {@link PdfPgpSignatureInfo}}.
     * @throws PGPBaseException
     *             When errors.
     */
    private PdfPgpSignatureInfo verify(final InputStream istrPdfSigned,
            final PGPPublicKey trustedPublicKey) throws PGPBaseException {

        try (ByteArrayOutputStream ostrPdf = new ByteArrayOutputStream()) {

            final PdfPgpReaderVerify reader = new PdfPgpReaderVerify(ostrPdf);

            reader.read(istrPdfSigned);

            final byte[] pgpBytes = reader.getPgpSignature();

            if (pgpBytes == null) {
                throw new IllegalArgumentException("No signature found");
            }

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("\n{}", new String(pgpBytes));
            }

            final PGPHelper helper = PGPHelper.instance();

            final PGPSignature sig =
                    helper.getSignature(new ByteArrayInputStream(pgpBytes));

            final boolean isValid = PGPHelper.instance().verifySignature(
                    new ByteArrayInputStream(ostrPdf.toByteArray()), sig,
                    trustedPublicKey);

            return new PdfPgpSignatureInfo(sig, isValid);

        } catch (IOException e) {
            throw new PGPBaseException(e.getMessage(), e);
        }
    }

}
