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
package org.savapage.lib.pgp.pdf;

import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reader for verifying for embedded or appended PDF/PGP signature.
 *
 * @author Rijk Ravestein
 *
 */
public final class PdfPgpReaderVerify extends PdfPgpReader {

    /** */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(PdfPgpReaderVerify.class);

    /** */
    private final OutputStream ostrPdf;
    /** */
    private byte[] pgpSignature;

    /**
     *
     * @param ostr
     *            Output stream to write PDF content to. The PGP signature is
     *            excluded from this content.
     */
    PdfPgpReaderVerify(final OutputStream ostr) {
        this.ostrPdf = ostr;
    }

    @Override
    protected void onStart() throws IOException {
        // no code intended
    }

    @Override
    protected void onPgpSignature(final byte[] pgpBytes) {
        this.pgpSignature = pgpBytes;
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
            LOGGER.debug("Verify : Calc Local- PDF md5 [{}] [{}] bytes",
                    Hex.encodeHexString(contentMessageDigest.digest()),
                    contentBytes);
        }
    }

    /**
     * @return The parsed PGP signature.
     */
    public byte[] getPgpSignature() {
        return this.pgpSignature;
    }

    @Override
    protected void onPdfEof() throws IOException {
        // no code intended
    }

    @Override
    protected MessageDigest createMessageDigest() {
        if (LOGGER.isDebugEnabled()) {
            return DigestUtils.getMd5Digest();
        }
        return null;
    }

}
