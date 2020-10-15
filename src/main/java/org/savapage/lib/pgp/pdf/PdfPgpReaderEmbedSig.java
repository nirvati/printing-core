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

import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reader for embedding PDF/PGP signature.
 *
 * @author Rijk Ravestein
 *
 */
public final class PdfPgpReaderEmbedSig extends PdfPgpReader {

    /** */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(PdfPgpReaderEmbedSig.class);

    /** */
    private final OutputStream ostrPdf;
    /** */
    private final byte[] pgpSignature;

    /**
     *
     * @param ostr
     *            The PGP signed PDF.
     * @param sig
     *            The ASCII armored PGP signature.
     */
    PdfPgpReaderEmbedSig(final OutputStream ostr, final byte[] sig) {
        this.ostrPdf = ostr;
        this.pgpSignature = sig;
    }

    @Override
    protected void onStart() throws IOException {
        // no code intended.
    }

    @Override
    protected void onPgpSignature(final byte[] pgpBytes) {
        // no code intended.
    }

    @Override
    protected void onPdfContent(final byte[] content) throws IOException {
        ostrPdf.write(content);
    }

    @Override
    protected void onPdfContent(final byte content) throws IOException {
        ostrPdf.write(content);
    }

    @Override
    protected void onEnd(final MessageDigest contentMessageDigest,
            final long contentBytes) {
        if (contentMessageDigest != null && LOGGER.isDebugEnabled()) {
            LOGGER.debug("Sign   : Plain PDF md5 [{}] [{}] bytes",
                    Hex.encodeHexString(contentMessageDigest.digest()),
                    contentBytes);
        }
    }

    @Override
    protected void onPdfEof() throws IOException {

        final String sig = String.format("%s%s", PDF_COMMENT_PFX,
                StringUtils.removeEnd(
                        StringUtils.replace(new String(this.pgpSignature), "\n",
                                "\n" + PDF_COMMENT_PFX),
                        PDF_COMMENT_PFX));

        ostrPdf.write(sig.getBytes());
    }

    @Override
    protected MessageDigest createMessageDigest() {
        if (LOGGER.isDebugEnabled()) {
            return DigestUtils.getMd5Digest();
        }
        return null;
    }

}
