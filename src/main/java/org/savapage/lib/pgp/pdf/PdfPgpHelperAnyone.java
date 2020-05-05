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

import java.awt.Color;
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
import org.savapage.core.doc.PdfToAnyone;
import org.savapage.core.pdf.ITextHelperV2;
import org.savapage.core.util.FileSystemHelper;
import org.savapage.lib.pgp.PGPBaseException;
import org.savapage.lib.pgp.PGPHelper;
import org.savapage.lib.pgp.PGPPublicKeyInfo;
import org.savapage.lib.pgp.PGPSecretKeyInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PRStream;
import com.lowagie.text.pdf.PdfAction;
import com.lowagie.text.pdf.PdfAnnotation;
import com.lowagie.text.pdf.PdfArray;
import com.lowagie.text.pdf.PdfDictionary;
import com.lowagie.text.pdf.PdfFileSpecification;
import com.lowagie.text.pdf.PdfFormField;
import com.lowagie.text.pdf.PdfName;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfStamper;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.PushbuttonField;

/**
 * PDF/PGP helper using {@link com.lowagie} to create PDF file.
 *
 * @author Rijk Ravestein
 *
 */
public final class PdfPgpHelperAnyone extends AbstractPdfPgpHelper {

    /** */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(PdfPgpHelperAnyone.class);

    /** */
    private static final Font NORMAL_FONT_COURIER =
            new Font(Font.COURIER, 12, Font.NORMAL, Color.DARK_GRAY);

    /**
     * Rectangle with zero space.
     */
    private static final Rectangle RECT_ZERO = new Rectangle(0, 0);

    /** */
    private static final class SingletonHolder {
        /** */
        static final PdfPgpHelperAnyone SINGLETON = new PdfPgpHelperAnyone();
    }

    /**
     * Singleton instantiation.
     */
    private PdfPgpHelperAnyone() {
        super();
    }

    /**
     * @return The singleton instance.
     */
    public static PdfPgpHelperAnyone instance() {
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

                for (final Object obj : refs.getKeys()) {

                    final PdfName key = (PdfName) obj;

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

        try {
            FileSystemHelper.replaceWithNewVersion(fileIn,
                    new PdfToAnyone().convert(fileIn));
        } catch (IOException e) {
            throw new PGPBaseException(e.getMessage());
        }

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
                        ITextHelperV2.getPermissions(
                                parms.getEncryptionProps().getAllow()));
            } else if (encryptOwnerPwAsURLParm) {
                stamper.setEncryption(null, ownerPw.getBytes(),
                        PdfWriter.ALLOW_PRINTING, PdfWriter.ENCRYPTION_AES_128
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

            push.setBackgroundColor(Color.LIGHT_GRAY);
            push.setBorderColor(Color.GRAY);
            push.setTextColor(Color.DARK_GRAY);
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
            final Font font = new Font(Font.COURIER, fontSize);

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
