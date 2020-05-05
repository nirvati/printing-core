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
package org.savapage.core.doc;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.savapage.core.json.PdfProperties;
import org.savapage.lib.pgp.PGPBaseException;
import org.savapage.lib.pgp.PGPPublicKeyInfo;
import org.savapage.lib.pgp.PGPSecretKeyInfo;
import org.savapage.lib.pgp.pdf.PdfPgpSignParms;
import org.savapage.lib.pgp.pdf.PdfPgpSigner;
import org.savapage.lib.pgp.pdf.PdfPgpVerifyUrl;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class PdfToPgpSignedPdf extends AbstractPdfConverter
        implements IPdfConverter, PdfPgpSignParms {

    /**
     * A unique suffix to type the kind of PDF convert.
     */
    private static final String OUTPUT_FILE_SFX = "pdfpgp";

    /** */
    private final PdfPgpSigner pdfPgpSigner;
    /** */
    private final PGPSecretKeyInfo secKeyInfo;
    /** */
    private final List<PGPPublicKeyInfo> pubKeyInfoList;
    /** */
    private final PGPPublicKeyInfo pubKeyInfoAuthor;
    /** */
    private final PdfPgpVerifyUrl verifyUrl;

    /** */
    private final PdfProperties pdfEncryption;

    /**
     * @param signer
     *            The {@link PdfPgpSigner}.
     * @param secKey
     *            Secure key of the signer.
     * @param pubKeySigner
     *            Public key of the creator/signer.
     * @param pubKeyAuthor
     *            Public key of the author ({@code null} when not available.
     * @param url
     *            The verification URL.
     * @param encryptionProps
     *            PDF properties used for encryption. If {@code null},
     *            encryption is not applicable.
     */
    public PdfToPgpSignedPdf(final PdfPgpSigner signer,
            final PGPSecretKeyInfo secKey, final PGPPublicKeyInfo pubKeySigner,
            final PGPPublicKeyInfo pubKeyAuthor, final PdfPgpVerifyUrl url,
            final PdfProperties encryptionProps) {

        super();

        this.pdfPgpSigner = signer;

        this.secKeyInfo = secKey;
        this.verifyUrl = url;

        this.pubKeyInfoList = new ArrayList<>();
        pubKeyInfoList.add(pubKeySigner);

        this.pubKeyInfoAuthor = pubKeyAuthor;

        this.pdfEncryption = encryptionProps;
    }

    @Override
    public File convert(final File pdfFile) throws IOException {

        final File pdfOut = this.getOutputFile(pdfFile);

        try {
            this.pdfPgpSigner.sign(pdfFile, pdfOut, this);
        } catch (PGPBaseException e) {
            throw new IOException(e);
        }

        return pdfOut;
    }

    @Override
    protected String getOutputFileSfx() {
        return OUTPUT_FILE_SFX;
    }

    @Override
    public PGPSecretKeyInfo getSecretKeyInfo() {
        return this.secKeyInfo;
    }

    @Override
    public PGPPublicKeyInfo getPubKeyAuthor() {
        return this.pubKeyInfoAuthor;
    }

    @Override
    public PdfPgpVerifyUrl getUrlBuilder() {
        return this.verifyUrl;
    }

    @Override
    public PdfProperties getEncryptionProps() {
        return this.pdfEncryption;
    }

    @Override
    public List<PGPPublicKeyInfo> getPubKeyInfoList() {
        return this.pubKeyInfoList;
    }

    @Override
    public boolean isEmbeddedSignature() {
        return false;
    }

}
