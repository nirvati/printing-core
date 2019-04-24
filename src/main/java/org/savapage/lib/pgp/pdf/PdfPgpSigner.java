/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2019 Datraverse B.V.
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
package org.savapage.lib.pgp.pdf;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.bouncycastle.openpgp.PGPPublicKey;
import org.savapage.lib.pgp.PGPBaseException;
import org.savapage.lib.pgp.PGPPublicKeyInfo;
import org.savapage.lib.pgp.PGPSecretKeyInfo;

/**
 * PDF/PGP Signer interface.
 *
 * @author Rijk Ravestein
 *
 */
public interface PdfPgpSigner {

    /**
     * Appends PGP signature to a PDF as % comment, and adds Verify button with
     * one-pass signed/encrypted Verification Payload URL. Note: the payload is
     * the PDF owner password.
     *
     * @param fileIn
     *            The PDF to sign.
     * @param fileOut
     *            The signed PDF.
     * @param secKeyInfo
     *            The secret key to sign with.
     * @param pubKeyAuthor
     *            Public key of the author ({@code null} when not available.
     * @param pubKeyInfoList
     *            The public keys to encrypt with.
     * @param urlBuilder
     *            The verification URL builder.
     * @param embeddedSignature
     *            If {@code true}, signature if embedded just before %%EOF. If
     *            {@code false} signature if appended just after %%EOF.
     *
     * @throws PGPBaseException
     *             When error.
     */
    void sign(File fileIn, File fileOut, PGPSecretKeyInfo secKeyInfo,
            PGPPublicKeyInfo pubKeyAuthor,
            List<PGPPublicKeyInfo> pubKeyInfoList, PdfPgpVerifyUrl urlBuilder,
            boolean embeddedSignature) throws PGPBaseException;

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
    PdfPgpSignatureInfo verify(File pdfFileSigned, PGPPublicKey signPublicKey)
            throws PGPBaseException, IOException;

}
