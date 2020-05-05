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
import java.util.Date;
import java.util.UUID;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.savapage.core.pdf.ITextHelperV5;
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
import com.itextpdf.text.pdf.PRStream;
import com.itextpdf.text.pdf.PdfAction;
import com.itextpdf.text.pdf.PdfAnnotation;
import com.itextpdf.text.pdf.PdfArray;
import com.itextpdf.text.pdf.PdfDictionary;
import com.itextpdf.text.pdf.PdfFileSpecification;
import com.itextpdf.text.pdf.PdfFormField;
import com.itextpdf.text.pdf.PdfName;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.PushbuttonField;

/**
 * PDF/PGP helper using {@link com.itextpdf}.
 *
 * @author Rijk Ravestein
 *
 */
public final class PdfPgpHelper extends AbstractPdfPgpHelper {

    /** */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(PdfPgpHelper.class);

    /** */
    private static final Font NORMAL_FONT_COURIER = new Font(
            Font.FontFamily.COURIER, 12, Font.NORMAL, BaseColor.DARK_GRAY);

    /**
     * Rectangle with zero space.
     */
    private static final Rectangle RECT_ZERO = new Rectangle(0, 0);

    /** */
    private static final class SingletonHolder {
        /** */
        static final PdfPgpHelper SINGLETON = new PdfPgpHelper();
    }

    /**
     * Singleton instantiation.
     */
    private PdfPgpHelper() {
        super();
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
     *
     * @param stamper
     *            The PDF stamper.
     * @param pubKey
     *            PGP Public key.
     * @param fileDisplay
     *            The file information that is presented to the user.
     * @param fileDescript
     *            File description.
     * @throws IOException
     *             If IO error.
     */
    private static void attachFile(final PdfStamper stamper,
            final PGPPublicKey pubKey, final String fileDisplay,
            final String fileDescript) throws IOException {
        /*
         * Attach Public Key of PDF Creator.
         */
        final int compressionLevel = 0;
        final PdfWriter writer = stamper.getWriter();

        final PdfFileSpecification fsPubKey = PdfFileSpecification.fileEmbedded(
                writer, null, fileDisplay, PGPHelper.encodeArmored(pubKey),
                PGP_MIMETYPE_ASCII_ARMOR, null, compressionLevel);
        fsPubKey.addDescription(fileDescript, false);

        final PdfAnnotation annotPdfSig;
        writer.addFileAttachment(fsPubKey);
        annotPdfSig = new PdfAnnotation(writer, RECT_ZERO);
        stamper.addAnnotation(annotPdfSig, 1);
    }

    @Override
    protected PGPPublicKeyInfo getPubKeyAuthor(final File pdfFile)
            throws IOException, PGPBaseException {

        final PdfReader reader = new PdfReader(pdfFile.getAbsolutePath());

        try {
            final PdfDictionary root = reader.getCatalog();
            final PdfDictionary documentnames = root.getAsDict(PdfName.NAMES);
            final PdfDictionary embeddedfiles =
                    documentnames.getAsDict(PdfName.EMBEDDEDFILES);

            final PdfArray filespecs = embeddedfiles.getAsArray(PdfName.NAMES);

            for (int i = 0; i < filespecs.size();) {

                filespecs.getAsString(i++);

                final PdfDictionary filespec = filespecs.getAsDict(i++);
                final PdfDictionary refs = filespec.getAsDict(PdfName.EF);

                for (final PdfName key : refs.getKeys()) {

                    final String attachmentName =
                            filespec.getAsString(key).toString();

                    if (!attachmentName.equals(PGP_PUBKEY_FILENAME_AUTHOR)) {
                        continue;
                    }

                    final PRStream stream = (PRStream) PdfReader
                            .getPdfObject(refs.getAsIndirectObject(key));

                    return PGPHelper.instance()
                            .readPublicKey(new ByteArrayInputStream(
                                    PdfReader.getStreamBytes(stream)));
                }
            }
        } finally {
            reader.close();
        }

        return null;
    }

    @Override
    public void sign(final File fileIn, final File fileOut,
            final PdfPgpSignParms parms) throws PGPBaseException {

        final PGPSecretKeyInfo secKeyInfo = parms.getSecretKeyInfo();
        final PGPPublicKeyInfo pubKeyAuthor = parms.getPubKeyAuthor();

        final boolean encryptOwnerPwAsURLParm = !ENCRYPT_OWNER_PW_AS_URL_PARM;

        final String ownerPwGiven = getOwnerPassword(parms);
        final String ownerPw;

        if (encryptOwnerPwAsURLParm) {
            if (StringUtils.isBlank(ownerPwGiven)) {
                ownerPw = RandomStringUtils.random(PDF_OWNER_PASSWORD_SIZE,
                        true, true);
            } else {
                ownerPw = ownerPwGiven;
            }
        } else {
            ownerPw = ownerPwGiven;
        }

        PdfReader reader = null;
        PdfStamper stamper = null;

        try (InputStream pdfIn = new FileInputStream(fileIn);
                OutputStream pdfSigned = new FileOutputStream(fileOut);) {

            reader = new PdfReader(pdfIn);
            stamper = new PdfStamper(reader, pdfSigned);

            // Encrypt?
            if (parms.getEncryptionProps() != null) {
                stamper.setEncryption(true,
                        parms.getEncryptionProps().getPw().getUser(), ownerPw,
                        ITextHelperV5.getPermissions(
                                parms.getEncryptionProps().getAllow()));
            } else if (encryptOwnerPwAsURLParm) {
                stamper.setEncryption(null, ownerPw.getBytes(),
                        PdfWriter.ALLOW_PRINTING, PdfWriter.ENCRYPTION_AES_256
                                | PdfWriter.DO_NOT_ENCRYPT_METADATA);
            }

            final PdfWriter writer = stamper.getWriter();

            /*
             * Create the encrypted onepass signature.
             */
            final byte[] payload;

            if (encryptOwnerPwAsURLParm) {

                final InputStream istrPayload =
                        new ByteArrayInputStream(ownerPw.getBytes());
                final ByteArrayOutputStream bostrSignedEncrypted =
                        new ByteArrayOutputStream();

                PGPHelper.instance().encryptOnePassSignature(istrPayload,
                        bostrSignedEncrypted, secKeyInfo,
                        parms.getPubKeyInfoList(), PGP_PAYLOAD_FILE_NAME,
                        new Date(), ASCII_ARMOR);
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

            final String urlVerify = parms.getUrlBuilder()
                    .build(secKeyInfo, payload).toExternalForm();

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

            /*
             * Attach Public Key of PDF Creator and (optionally) Author.
             */
            attachFile(stamper, secKeyInfo.getPublicKey(),
                    PGP_PUBKEY_FILENAME_CREATOR, "PGP Public key of Creator.");

            if (pubKeyAuthor != null) {
                attachFile(stamper, pubKeyAuthor.getMasterKey(),
                        PGP_PUBKEY_FILENAME_AUTHOR,
                        "PGP Public key of Author.");
            }
            //
            stamper.close();
            reader.close();
            reader = null;

            /*
             * Embed or append PGP signature of PDF as PDF comment.
             */
            final ByteArrayOutputStream ostrPdfSig =
                    new ByteArrayOutputStream();

            PGPHelper.instance().createSignature(new FileInputStream(fileOut),
                    ostrPdfSig, secKeyInfo, PGPHelper.CONTENT_SIGN_ALGORITHM,
                    ASCII_ARMOR);

            if (parms.isEmbeddedSignature()) {

                final File fileOutSigned = new File(String.format("%s.%s",
                        fileOut.getPath(), UUID.randomUUID().toString()));

                final PdfPgpReader readerForSig = new PdfPgpReaderEmbedSig(
                        new FileOutputStream(fileOutSigned),
                        ostrPdfSig.toByteArray());

                readerForSig.read(new FileInputStream(fileOut));

                fileOut.delete();
                FileUtils.moveFile(fileOutSigned, fileOut);

            } else {

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Sign  : File PDF md5 [{}] [{}] bytes",
                            DigestUtils.md5Hex(new FileInputStream(fileOut)),
                            FileUtils.sizeOf(fileOut));
                }

                final Writer output =
                        new BufferedWriter(new FileWriter(fileOut, true));

                output.append(PdfPgpReader.PDF_COMMENT_PFX).append(
                        new String(ostrPdfSig.toByteArray()).replace("\n",
                                "\n" + PdfPgpReader.PDF_COMMENT_PFX));
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

}
