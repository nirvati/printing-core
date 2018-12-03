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

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.security.MessageDigest;
import java.security.Security;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPSignature;
import org.savapage.lib.pgp.PGPBaseException;
import org.savapage.lib.pgp.PGPHelper;
import org.savapage.lib.pgp.PGPPublicKeyInfo;
import org.savapage.lib.pgp.PGPSecretKeyInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Font.FontFamily;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.ColumnText;
import com.itextpdf.text.pdf.PdfAction;
import com.itextpdf.text.pdf.PdfFormField;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.PushbuttonField;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class PdfPgpHelper {

    /** */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(PdfPgpHelper.class);

    /** */
    private static final Font NORMAL_FONT_COURIER = new Font(
            Font.FontFamily.COURIER, 12, Font.NORMAL, BaseColor.DARK_GRAY);

    /**
     * The file name of the ASCII armored OnePass PGP signature stored in the
     * PDF.
     */
    private static final String PGP_PAYLOAD_FILE_NAME = "verification.asc";

    /**
     * Max size of PDF owner password.
     */
    private static final int PDF_OWNER_PASSWORD_SIZE = 32;

    /** */
    private static final boolean ASCII_ARMOR = true;

    /** */
    private static final class SingletonHolder {
        /** */
        static final PdfPgpHelper SINGLETON = new PdfPgpHelper();
    }

    /**
     * Singleton instantiation.
     */
    private PdfPgpHelper() {
        Security.addProvider(new BouncyCastleProvider());
    }

    /**
     * @return The singleton instance.
     */
    public static PdfPgpHelper instance() {
        return SingletonHolder.SINGLETON;
    }

    /**
     * Closes a Stamper ignoring exceptions.
     *
     * @param stamper
     *            The stamper.
     */
    private static void closeQuietly(final PdfStamper stamper) {
        if (stamper != null) {
            try {
                stamper.close();
            } catch (DocumentException | IOException e) {
                // no code intended.
            }
        }
    }

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
    public void sign(final File fileIn, final File fileOut,
            final PGPSecretKeyInfo secKeyInfo,
            final List<PGPPublicKeyInfo> pubKeyInfoList,
            final PdfPgpVerifyUrl urlBuilder, final boolean embeddedSignature)
            throws PGPBaseException {

        boolean verificationParms = false; // TODO

        PdfReader reader = null;
        PdfStamper stamper = null;

        final String ownerPw;

        if (verificationParms) {
            ownerPw = RandomStringUtils.random(PDF_OWNER_PASSWORD_SIZE, true,
                    true);
        } else {
            ownerPw = null;
        }

        try (InputStream pdfIn = new FileInputStream(fileIn);
                OutputStream pdfSigned = new FileOutputStream(fileOut);) {

            reader = new PdfReader(pdfIn);
            stamper = new PdfStamper(reader, pdfSigned);

            if (verificationParms) {
                stamper.setEncryption(null, ownerPw.getBytes(),
                        PdfWriter.ALLOW_PRINTING, PdfWriter.ENCRYPTION_AES_256
                                | PdfWriter.DO_NOT_ENCRYPT_METADATA);
            }

            final PdfWriter writer = stamper.getWriter();

            /*
             * Create the encrypted onepass signature.
             */
            final byte[] payload;

            if (verificationParms) {

                final InputStream istrPayload =
                        new ByteArrayInputStream(ownerPw.getBytes());
                final ByteArrayOutputStream bostrSignedEncrypted =
                        new ByteArrayOutputStream();

                PGPHelper.instance().encryptOnePassSignature(istrPayload,
                        bostrSignedEncrypted, secKeyInfo, pubKeyInfoList,
                        PGP_PAYLOAD_FILE_NAME, new Date(), ASCII_ARMOR);
                payload = bostrSignedEncrypted.toByteArray();

            } else {
                payload = null;
            }

            //
            final int iFirstPage = 1;
            final Rectangle rectPage = reader.getPageSize(iFirstPage);

            /*
             * Add button to open browser.
             */
            final Rectangle rect = new Rectangle(10,
                    rectPage.getTop() - 30 - 10, 100, rectPage.getTop() - 10);

            final PushbuttonField push =
                    new PushbuttonField(writer, rect, "openVerifyURL");

            push.setText("Verify . . .");

            push.setBackgroundColor(BaseColor.LIGHT_GRAY);
            push.setBorderColor(BaseColor.GRAY);
            push.setTextColor(BaseColor.DARK_GRAY);
            push.setFontSize(NORMAL_FONT_COURIER.getSize());
            push.setFont(NORMAL_FONT_COURIER.getBaseFont());
            push.setVisibility(PushbuttonField.VISIBLE_BUT_DOES_NOT_PRINT);

            final String urlVerify =
                    urlBuilder.build(secKeyInfo, payload).toExternalForm();

            final PdfFormField pushButton = push.getField();
            pushButton.setAction(new PdfAction(urlVerify));

            stamper.addAnnotation(pushButton, iFirstPage);

            /*
             *
             */
            final float fontSize = 8f;
            final Font font = new Font(FontFamily.COURIER, fontSize);

            final Phrase header =
                    new Phrase(secKeyInfo.formattedFingerPrint(), font);

            final float x = rect.getRight() + 20;
            final float y = rect.getBottom()
                    + (rect.getTop() - rect.getBottom()) / 2 - fontSize / 2;

            ColumnText.showTextAligned(stamper.getOverContent(iFirstPage),
                    Element.ALIGN_LEFT, header, x, y, 0);

            //
            stamper.close();
            reader.close();
            reader = null;

            /*
             * Append PGP signature of PDF as PDF comment.
             */
            final ByteArrayOutputStream ostrPdfSig =
                    new ByteArrayOutputStream();

            PGPHelper.instance().createSignature(new FileInputStream(fileOut),
                    ostrPdfSig, secKeyInfo, PGPHelper.CONTENT_SIGN_ALGORITHM,
                    ASCII_ARMOR);

            if (embeddedSignature) {

                final File fileOutSigned = new File(String.format("%s.%s",
                        fileOut.getPath(), UUID.randomUUID().toString()));

                final PdfPgpReaderSign readerForSigning = new PdfPgpReaderSign(
                        new FileOutputStream(fileOutSigned),
                        ostrPdfSig.toByteArray());

                readerForSigning.read(new FileInputStream(fileOut));

                fileOut.delete();
                FileUtils.moveFile(fileOutSigned, fileOut);

            } else {

                final Writer output =
                        new BufferedWriter(new FileWriter(fileOut, true));

                output.append(PdfPgpEmbedReader.PDF_COMMENT_PFX)
                        .append(new String(ostrPdfSig.toByteArray()).replace(
                                "\n",
                                "\n" + PdfPgpEmbedReader.PDF_COMMENT_PFX));
                output.close();
            }

        } catch (IOException | DocumentException e) {
            throw new PGPBaseException(e.getMessage(), e);
        } finally {
            closeQuietly(stamper);
            if (reader != null) {
                reader.close();
            }
        }
    }

    /**
     *
     * @author Rijk Ravestein
     *
     */
    private class PdfPgpReaderSign extends PdfPgpEmbedReader {

        private final OutputStream ostrPdf;
        private final byte[] pgpSignature;
        private MessageDigest md5Digest;
        private long md5ContenSize;

        /**
         *
         * @param ostr
         */
        PdfPgpReaderSign(final OutputStream ostr, final byte[] sig) {
            this.ostrPdf = ostr;
            this.pgpSignature = sig;
        }

        @Override
        protected void onStart() throws IOException {
            this.md5ContenSize = 0;
            if (LOGGER.isDebugEnabled()) {
                this.md5Digest = DigestUtils.getMd5Digest();
            } else {
                this.md5Digest = null;
            }
        }

        @Override
        protected void onPgpSignature(final byte[] pgpBytes) {
            // no code intended.
        }

        @Override
        protected void onPdfContent(final byte[] content) throws IOException {
            ostrPdf.write(content);
            this.md5ContenSize += content.length;
            if (this.md5Digest != null) {
                this.md5Digest.update(content);
            }
        }

        @Override
        protected void onPdfContent(final byte content) throws IOException {
            ostrPdf.write(content);
            this.md5ContenSize++;
            if (this.md5Digest != null) {
                this.md5Digest.update(content);
            }
        }

        @Override
        protected void onEnd() {
            if (this.md5Digest != null && LOGGER.isDebugEnabled()) {
                LOGGER.debug("Sign   : Plain PDF md5 [{}] [{}] bytes",
                        Hex.encodeHexString(this.md5Digest.digest()),
                        this.md5ContenSize);
            }
        }

        @Override
        protected void onPdfEof() throws IOException {

            final String sig = String.format("%s%s", PDF_COMMENT_PFX,
                    StringUtils.removeEnd(
                            StringUtils.replace(new String(this.pgpSignature),
                                    "\n", "\n" + PDF_COMMENT_PFX),
                            PDF_COMMENT_PFX));

            ostrPdf.write(sig.getBytes());
        }

    }

    /**
     * Verifies a PGP signed PDF file.
     *
     * @param pdfFileSigned
     *            Signed PDF as input.
     * @param signPublicKey
     *            The {@link PGPPublicKey} of the private key the PGP signature
     *            content was signed with.
     * @param embeddedSignature
     *            If {@code true}, signature if embedded just before %%EOF. If
     *            {@code false} signature if appended just after %%EOF.
     * @return The {@link PGPSignature}} if valid, or {@code null} when not.
     * @throws PGPBaseException
     *             When errors.
     * @throws IOException
     *             When File IO errors.
     */
    public PGPSignature verify(final File pdfFileSigned,
            final PGPPublicKey signPublicKey, final boolean embeddedSignature)
            throws PGPBaseException, IOException {

        try (InputStream istrPdf = new FileInputStream(pdfFileSigned);) {
            return this.verify(istrPdf, signPublicKey, embeddedSignature);
        }
    }

    /**
     * Verifies a PGP signed PDF file.
     *
     * @param istrPdfSigned
     *            Signed PDF document as input stream.
     * @param signPublicKey
     *            The {@link PGPPublicKey} of the private key the PGP signature
     *            content was signed with.
     * @param embeddedSignature
     *            If {@code true}, signature if embedded just before %%EOF. If
     *            {@code false} signature if appended just after %%EOF.
     * @return The {@link PGPSignature}} if valid, or {@code null} when not.
     * @throws PGPBaseException
     *             When errors.
     */
    public PGPSignature verify(final InputStream istrPdfSigned,
            final PGPPublicKey signPublicKey, final boolean embeddedSignature)
            throws PGPBaseException {
        if (embeddedSignature) {
            return this.verifyEmbedded(istrPdfSigned, signPublicKey);
        }
        return this.verifyAppended(istrPdfSigned, signPublicKey);
    }

    /**
     *
     * @author Rijk Ravestein
     *
     */
    private class PdfPgpReaderVerify extends PdfPgpEmbedReader {

        private final ByteArrayOutputStream ostrPdf;
        private byte[] pgpSignature;
        private MessageDigest md5Digest;
        private long md5ContenSize;

        /**
         *
         * @param ostr
         */
        PdfPgpReaderVerify(final ByteArrayOutputStream ostr) {
            this.ostrPdf = ostr;
        }

        @Override
        protected void onStart() throws IOException {
            if (LOGGER.isDebugEnabled()) {
                this.md5Digest = DigestUtils.getMd5Digest();
            } else {
                this.md5Digest = null;
            }
            md5ContenSize = 0;
        }

        @Override
        protected void onPgpSignature(final byte[] pgpBytes) {
            this.pgpSignature = pgpBytes;
        }

        @Override
        protected void onPdfContent(final byte[] content) throws IOException {
            ostrPdf.write(content);
            this.md5ContenSize += content.length;
            if (this.md5Digest != null) {
                this.md5Digest.update(content);
            }
        }

        @Override
        protected void onPdfContent(final byte content) throws IOException {
            ostrPdf.write(content);
            this.md5ContenSize++;
            if (this.md5Digest != null) {
                this.md5Digest.update(content);
            }
        }

        @Override
        protected void onEnd() {
            if (this.md5Digest != null && LOGGER.isDebugEnabled()) {
                LOGGER.debug("Verify : Calc Local- PDF md5 [{}] [{}] bytes",
                        Hex.encodeHexString(this.md5Digest.digest()),
                        this.md5ContenSize);
            }
        }

        public byte[] getPgpSignature() {
            return this.pgpSignature;
        }

        @Override
        protected void onPdfEof() throws IOException {
            // no code intended
        }

    }

    /**
     * Verifies a PGP signed PDF file.
     *
     * @param istrPdfSigned
     *            Signed PDF document as input stream.
     * @param signPublicKey
     *            The {@link PGPPublicKey} of the private key the PGP signature
     *            content was signed with.
     * @return The {@link PGPSignature}} if valid, or {@code null} when not.
     * @throws PGPBaseException
     *             When errors.
     */
    private PGPSignature verifyEmbedded(final InputStream istrPdfSigned,
            final PGPPublicKey signPublicKey) throws PGPBaseException {

        try (ByteArrayOutputStream ostrPdf = new ByteArrayOutputStream()) {

            final PdfPgpReaderVerify reader = new PdfPgpReaderVerify(ostrPdf);

            reader.read(istrPdfSigned);

            final byte[] pgpBytes = reader.getPgpSignature();

            if (pgpBytes == null) {
                throw new IllegalArgumentException("PGP signature not found.");
            }

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("\n{}", new String(pgpBytes));
            }

            if (PGPHelper.instance().verifySignature(
                    new ByteArrayInputStream(ostrPdf.toByteArray()),
                    new ByteArrayInputStream(pgpBytes), signPublicKey)) {

                return PGPHelper.instance()
                        .getSignature(new ByteArrayInputStream(pgpBytes));
            }
            return null;

        } catch (IOException e) {
            throw new PGPBaseException(e.getMessage(), e);
        }
    }

    /**
     * Verifies a PGP signed PDF file.
     *
     * @param istrPdfSigned
     *            Signed PDF document as input stream.
     * @param signPublicKey
     *            The {@link PGPPublicKey} of the private key the PGP signature
     *            content was signed with.
     * @return The {@link PGPSignature}} if valid, or {@code null} when not.
     * @throws PGPBaseException
     *             When errors.
     */
    private PGPSignature verifyAppended(final InputStream istrPdfSigned,
            final PGPPublicKey signPublicKey) throws PGPBaseException {

        try (ByteArrayOutputStream ostrPdf = new ByteArrayOutputStream()) {

            int n = istrPdfSigned.read();

            final StringBuffer appendedPgp = new StringBuffer();

            boolean isNewLine = true;

            while (n >= 0) {

                if (isNewLine && n == PdfPgpEmbedReader.INT_PDF_COMMENT) {

                    isNewLine = false;

                    /*
                     * Collect raw bytes! Do not collect on String, because
                     * bytes will be interpreted as Unicode.
                     */
                    final ByteArrayOutputStream bosAhead =
                            new ByteArrayOutputStream();

                    // Read till EOL or EOF.
                    while (n >= 0) {
                        bosAhead.write(n);
                        if (isNewLine) {
                            break;
                        }
                        // Read next
                        n = istrPdfSigned.read();
                        isNewLine = n == PdfPgpEmbedReader.INT_NEWLINE;
                    }

                    final byte[] bytesAhead = bosAhead.toByteArray();

                    /*
                     * At this point we do convert to String, so we can compare.
                     */
                    final String stringAhead = new String(bytesAhead);

                    if (stringAhead.startsWith(
                            PdfPgpEmbedReader.PDF_COMMENT_BEGIN_PGP_SIGNATURE)) {
                        appendedPgp.append(stringAhead);
                        break;
                    }

                    ostrPdf.write(bytesAhead);
                    bosAhead.close();

                } else {
                    isNewLine = n == PdfPgpEmbedReader.INT_NEWLINE;
                    ostrPdf.write((byte) n);
                }

                // Read next when not EOF.
                if (n >= 0) {
                    n = istrPdfSigned.read();
                }
            }

            if (LOGGER.isDebugEnabled()) {
                final byte[] tmp = ostrPdf.toByteArray();
                LOGGER.debug("Verify: Base PDF md5 [{}] [{}] bytes",
                        DigestUtils.md5Hex(tmp), tmp.length);
            }

            /*
             * Collect PGP signature.
             */
            final ByteArrayOutputStream bosSignature =
                    new ByteArrayOutputStream();

            if (appendedPgp.length() > 0) {

                // Skip first % character.
                bosSignature
                        .write(appendedPgp.toString().substring(1).getBytes());

                n = istrPdfSigned.read();

                while (n >= 0) {
                    if (n != PdfPgpEmbedReader.INT_PDF_COMMENT) {
                        bosSignature.write(n);
                    }
                    n = istrPdfSigned.read();
                }
            } else {
                bosSignature.close();
                throw new IllegalArgumentException("PGP signature not found.");
            }

            bosSignature.flush();
            bosSignature.close();

            final byte[] pgpBytes = bosSignature.toByteArray();

            if (PGPHelper.instance().verifySignature(
                    new ByteArrayInputStream(ostrPdf.toByteArray()),
                    new ByteArrayInputStream(pgpBytes), signPublicKey)) {

                return PGPHelper.instance()
                        .getSignature(new ByteArrayInputStream(pgpBytes));
            }
            return null;

        } catch (IOException e) {
            throw new PGPBaseException(e.getMessage(), e);
        }
    }

}
