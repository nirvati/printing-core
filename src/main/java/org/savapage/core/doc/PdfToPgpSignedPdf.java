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
package org.savapage.core.doc;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.savapage.lib.pgp.PGPBaseException;
import org.savapage.lib.pgp.PGPPublicKeyInfo;
import org.savapage.lib.pgp.PGPSecretKeyInfo;
import org.savapage.lib.pgp.pdf.PdfPgpVerifyUrl;
import org.savapage.lib.pgp.pdf.PdfPgpHelper;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class PdfToPgpSignedPdf extends AbstractPdfConverter
        implements IPdfConverter {

    /**
     * A unique suffix to type the kind of PDF convert.
     */
    private static final String OUTPUT_FILE_SFX = "pdfpgp";

    /** */
    private final PGPSecretKeyInfo secKeyInfo;;
    /** */
    private final List<PGPPublicKeyInfo> pubKeyInfoList;
    /** */
    private final PdfPgpVerifyUrl verifyUrl;

    /**
     *
     * @param secKey
     *            Secure key of the signer.
     * @param pubKeyInfoSigner
     *            Public key of the signer.
     * @param url
     *            The verification URL.
     */
    public PdfToPgpSignedPdf(final PGPSecretKeyInfo secKey,
            final PGPPublicKeyInfo pubKeyInfoSigner,
            final PdfPgpVerifyUrl url) {

        super();

        this.secKeyInfo = secKey;
        this.verifyUrl = url;

        this.pubKeyInfoList = new ArrayList<>();
        pubKeyInfoList.add(pubKeyInfoSigner);
    }

    @Override
    public File convert(final File pdfFile) throws IOException {

        final File pdfOut = this.getOutputFile(pdfFile);

        try {
            PdfPgpHelper.instance().sign(pdfFile, pdfOut, this.secKeyInfo,
                    this.pubKeyInfoList, this.verifyUrl);
        } catch (PGPBaseException e) {
            throw new IOException(e);
        }

        return pdfOut;
    }

    @Override
    protected String getOutputFileSfx() {
        return OUTPUT_FILE_SFX;
    }

}
