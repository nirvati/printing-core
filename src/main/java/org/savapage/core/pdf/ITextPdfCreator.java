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
package org.savapage.core.pdf;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.print.attribute.Size2DSyntax;
import javax.print.attribute.standard.MediaSize;
import javax.print.attribute.standard.MediaSizeName;

import org.savapage.core.SpException;
import org.savapage.core.UnavailableException;
import org.savapage.core.community.CommunityDictEnum;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.doc.DocContentToPdfException;
import org.savapage.core.doc.DocContentTypeEnum;
import org.savapage.core.doc.IDocFileConverter;
import org.savapage.core.doc.PdfRepair;
import org.savapage.core.doc.PdfToAnnotatedURL;
import org.savapage.core.doc.PdfToBooklet;
import org.savapage.core.doc.PdfToGrayscale;
import org.savapage.core.doc.PdfToRotateAlignedPdf;
import org.savapage.core.doc.SvgToPdf;
import org.savapage.core.fonts.InternalFontFamilyEnum;
import org.savapage.core.i18n.PhraseEnum;
import org.savapage.core.json.PdfProperties;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.util.MediaUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.itextpdf.awt.geom.AffineTransform;
import com.itextpdf.text.BadElementException;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.exceptions.InvalidPdfException;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PRIndirectReference;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfCopy;
import com.itextpdf.text.pdf.PdfDictionary;
import com.itextpdf.text.pdf.PdfEncryptor;
import com.itextpdf.text.pdf.PdfException;
import com.itextpdf.text.pdf.PdfImportedPage;
import com.itextpdf.text.pdf.PdfName;
import com.itextpdf.text.pdf.PdfNumber;
import com.itextpdf.text.pdf.PdfObject;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.XfaForm;

/**
 * PDF Creator using the iText AGPL version.
 *
 * @author Rijk Ravestein
 *
 */
public final class ITextPdfCreator extends AbstractPdfCreator {

    /** */
    public static final String PDF_INFO_KEY_TITLE = "Title";
    /** */
    public static final String PDF_INFO_KEY_SUBJECT = "Subject";
    /** */
    public static final String PDF_INFO_KEY_AUTHOR = "Author";
    /** */
    public static final String PDF_INFO_KEY_CREATOR = "Creator";
    /** */
    public static final String PDF_INFO_KEY_KEYWORDS = "Keywords";

    /** */
    private static final int ITEXT_POINTS_PER_INCH = 72;

    /**
     * The logger.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(ITextPdfCreator.class);

    private String targetPdfCopyFilePath;
    private Document targetDocument;

    /** */
    private PdfCopy targetPdfCopy;

    /**
     * Zero-based page number walker in {@link #targetPdfCopy}.
     */
    private int targetPdfCopyPageWlk;

    /**
     * A set with one-based page numbers that need page orientation change to
     * align to orientation of first page in overall PDF document.
     */
    private Set<Integer> targetPdfCopyPages2Align;

    /** */
    private PdfStamper targetStamper;

    private PdfReader readerWlk;
    private PdfReader letterheadReader;
    private List<PdfReader> overlayReaders;

    private StringBuilder jobRangesWlk;

    /**
     * The user rotate of the current job.
     */
    private Integer jobUserRotateWlk;

    private File jobPdfFileWlk;

    private boolean isRemoveGraphics = false;

    /**
     * The {@link PdfReader} containing a single blank page to be used to
     * replace a page with no /Contents.
     */
    private PdfReader singleBlankPagePdfReader;

    /**
     * Create URL links by default.
     */
    private boolean isAnnotateUrls = true;

    /** */
    private boolean onExitAnnotateUrls = false;

    /** */
    private boolean onExitStampEncryption = false;

    /** */
    private PdfProperties.PdfAllow pdfAllow;
    /** */
    private String pdfOwnerPass;
    /** */
    private String pdfUserPass;

    /**
     * {@code true} if the created pdf is to be converted to grayscale onExit.
     */
    private boolean onExitConvertToGrayscale = false;

    /**
     * {@code true} if PDF has to be repaired onExit.
     */
    private boolean onExitRepairPdf = false;

    /**
     * {@code true} if PDF with page porder for 2-up duplex booklet is to be
     * created.
     */
    private boolean onExitBookletPageOrder = false;

    /**
     * .
     */
    private Boolean firstPageSeenAsLandscape;

    /**
     * Create a {@link BaseFont} for an {@link InternalFontFamilyEnum}.
     *
     * @param internalFont
     *            The {@link InternalFontFamilyEnum}.
     * @return The {@link BaseFont} instance.
     */
    public static BaseFont
            createFont(final InternalFontFamilyEnum internalFont) {
        try {
            return BaseFont.createFont(internalFont.fontFilePath(),
                    BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
        } catch (DocumentException | IOException e) {
            throw new SpException(e);
        }
    }

    /**
     * Gets the {@link Rectangle} of {@link MediaSizeName}.
     *
     * @param mediaSizeName
     *            The {@link MediaSizeName}.
     *
     * @return {@code null} when default page size not found.
     */
    public static Rectangle getPageSize(final MediaSizeName mediaSizeName) {

        final Rectangle pageSize;

        final float[] size = MediaSize.getMediaSizeForName(mediaSizeName)
                .getSize(Size2DSyntax.INCH);

        if (size == null) {
            pageSize = null;
        } else {
            pageSize = new Rectangle(size[0] * ITEXT_POINTS_PER_INCH,
                    size[1] * ITEXT_POINTS_PER_INCH);
        }
        return pageSize;
    }

    /**
     * Gets the {@link Rectangle} of default page size.
     *
     * @return {@code null} when default page size not found.
     */
    public static Rectangle getDefaultPageSize() {

        final Rectangle pageSize;

        final String papersize = ConfigManager.instance()
                .getConfigValue(Key.SYS_DEFAULT_PAPER_SIZE);

        if (papersize.equals(IConfigProp.PAPERSIZE_V_SYSTEM)) {
            pageSize = getPageSize(MediaUtils.getHostDefaultMediaSize());
        } else if (papersize.equals(IConfigProp.PAPERSIZE_V_LETTER)) {
            pageSize = PageSize.LETTER;
        } else if (papersize.equals(IConfigProp.PAPERSIZE_V_A4)) {
            pageSize = PageSize.A4;
        } else {
            pageSize = null;
        }
        return pageSize;
    }

    /**
     * Checks if a page in {@link PdfReader} has {@link PdfName#CONTENTS}.
     *
     * @param reader
     *            The {@link PdfReader}.
     * @param nPage
     *            The 1-based page ordinal.
     * @return {@code true} when page has contents.
     */
    public static boolean isPageContentsPresent(final PdfReader reader,
            final int nPage) {
        final PdfDictionary pageDict = reader.getPageN(nPage);
        return pageDict != null && pageDict.get(PdfName.CONTENTS) != null;
    }

    /**
     * Creates a 1-page {@link PdfReader} with one (1) blank page.
     * <p>
     * Note: in-memory size of the document is approx. 886 bytes.
     * </p>
     *
     * @param pageSize
     *            The size of the page.
     * @return The {@link PdfReader}.
     * @throws DocumentException
     *             When error creating the PDF document.
     * @throws IOException
     *             When IO errors creating the reader.
     */
    public static PdfReader createBlankPageReader(final Rectangle pageSize)
            throws DocumentException, IOException {

        final ByteArrayOutputStream ostr = new ByteArrayOutputStream();

        final Document document = new Document(pageSize);

        PdfWriter.getInstance(document, ostr);
        document.open();

        /*
         * IMPORTANT: Paragraph MUST have content (if not, a close() of the
         * document throws an exception because no pages are detected):
         * therefore we use a single space as content.
         */
        document.add(new Paragraph(" "));

        document.close();

        return new PdfReader(new ByteArrayInputStream(ostr.toByteArray()));
    }

    /**
     *
     * @param filePathPdf
     *            The full path of the PDF file.
     * @return The number of pages in the PDF file.
     */
    @Override
    public int getNumberOfPagesInPdfFile(final String filePathPdf) {

        PdfReader reader = null;
        try {
            reader = new PdfReader(filePathPdf);
            return reader.getNumberOfPages();
        } catch (IOException e) {
            throw new SpException(e);
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }

    /**
     * Gets the PDF page properties from the media box {@link Rectangle}.
     *
     * @param mediabox
     *            The mediabox {@link Rectangle}.
     * @return The {@link pPdfPageProps}.
     */
    private SpPdfPageProps createPageProps(final Rectangle mediabox) {

        final PageFormat pageFormat = new PageFormat();
        final Paper paper = new Paper();

        paper.setSize(mediabox.getWidth(), mediabox.getHeight());
        pageFormat.setPaper(paper);

        // NOTE: the size in returned in PORTRAIT mode.
        final int[] size = MediaUtils.getMediaWidthHeight(pageFormat);

        int iSizeWidth = 0;
        int iSizeHeight = 1;

        /*
         * Since the size is in portrait mode, we swap height and width when the
         * PDF mediabox reports landscape orientation.
         */
        if (mediabox.getWidth() > mediabox.getHeight()) {
            iSizeWidth = 1;
            iSizeHeight = 0;
        }

        final SpPdfPageProps pageProps = new SpPdfPageProps();

        pageProps.setMmWidth(size[iSizeWidth]);
        pageProps.setMmHeight(size[iSizeHeight]);
        pageProps.setSize(MediaUtils.getMediaSizeName(pageFormat));
        pageProps.setRotationFirstPage(mediabox.getRotation());

        return pageProps;
    }

    /**
     * Checks if XFA form is present in PDF.
     *
     * @param reader
     *            The PDF reader.
     * @return {@code true} if XFA form is present.
     */
    private static boolean isPdfXfaPresent(final PdfReader reader) {
        return XfaForm.getXfaObject(reader) != null;
    }

    @Override
    public SpPdfPageProps getPageProps(final String filePathPdf)
            throws PdfSecurityException, PdfValidityException,
            PdfPasswordException, PdfUnsupportedException {

        SpPdfPageProps pageProps = null;
        PdfReader reader = null;

        final int firstPage = 1;

        try {
            /*
             * Instantiating/opening can throw a BadPasswordException or
             * InvalidPdfException, which are subclasses of IOException: map
             * these exception to our own variants.
             */
            reader = new PdfReader(filePathPdf);

            if (reader.isEncrypted()) {

                final int permissions = (int) reader.getPermissions();
                final boolean isPrintingAllowed = PdfEncryptor
                        .isPrintingAllowed(permissions)
                        || PdfEncryptor.isDegradedPrintingAllowed(permissions);

                final PhraseEnum phrase;
                if (isPrintingAllowed) {
                    phrase = PhraseEnum.PDF_ENCRYPTED_UNSUPPORTED;
                } else {
                    phrase = PhraseEnum.PDF_PRINTING_NOT_ALLOWED;
                }

                throw new PdfSecurityException(
                        phrase.uiText(ServiceContext.getLocale()), phrase,
                        isPrintingAllowed);
            }

            if (isPdfXfaPresent(reader)) {
                throw new PdfUnsupportedException(
                        PhraseEnum.PDF_XFA_UNSUPPORTED.uiText(Locale.ENGLISH),
                        PhraseEnum.PDF_XFA_UNSUPPORTED
                                .uiText(ServiceContext.getLocale()),
                        PhraseEnum.PDF_XFA_UNSUPPORTED);
            }

            pageProps = this.createPageProps(reader.getPageSize(firstPage));

            pageProps.setNumberOfPages(reader.getNumberOfPages());
            pageProps.setRotationFirstPage(reader.getPageRotation(firstPage));

            final AffineTransform ctm =
                    PdfPageRotateHelper.getPdfPageCTM(reader, firstPage);

            pageProps.setContentRotationFirstPage(
                    PdfPageRotateHelper.getPageContentRotation(ctm).intValue());

        } catch (com.itextpdf.text.exceptions.BadPasswordException e) {
            throw new PdfPasswordException(
                    PhraseEnum.PDF_PASSWORD_UNSUPPORTED
                            .uiText(ServiceContext.getLocale()),
                    PhraseEnum.PDF_PASSWORD_UNSUPPORTED);

        } catch (InvalidPdfException e) {
            throw new PdfValidityException(e.getMessage(),
                    PhraseEnum.PDF_INVALID.uiText(ServiceContext.getLocale()),
                    PhraseEnum.PDF_INVALID);

        } catch (IOException e) {
            throw new SpException(e);

        } finally {
            if (reader != null) {
                reader.close();
            }
        }

        return pageProps;
    }

    @Override
    protected void onInit() {

        this.onExitConvertToGrayscale = this.isGrayscalePdf();
        this.onExitBookletPageOrder = this.isBookletPageOrder();
        this.onExitRepairPdf = this.isRepairPdf();

        this.targetPdfCopyFilePath = String.format("%s.tmp", this.pdfFile);
        this.targetPdfCopyPageWlk = 0;
        if (this.isForPrinting() && this.isApplyLetterhead()) {
            this.targetPdfCopyPages2Align = new HashSet<>();
        }

        this.firstPageSeenAsLandscape = null;

        this.isAnnotateUrls =
                !this.isForPrinting() && !this.onExitConvertToGrayscale
                        && !this.onExitBookletPageOrder;

        this.onExitAnnotateUrls = false;
        this.onExitStampEncryption = false;

        try {
            final OutputStream ostr =
                    new FileOutputStream(this.targetPdfCopyFilePath);

            this.targetDocument = new Document();

            this.targetPdfCopy = new PdfCopy(this.targetDocument, ostr);

            this.targetDocument.open();

        } catch (Exception e) {
            throw new SpException(e);
        }
    }

    @Override
    protected void onExit() throws Exception {
    }

    @Override
    protected void onPdfGenerated(final File pdfFile) throws Exception {

        if (this.onExitConvertToGrayscale) {
            replaceWithConvertedPdf(pdfFile,
                    new PdfToGrayscale().convert(pdfFile));
        }
        if (this.onExitRepairPdf) {
            replaceWithConvertedPdf(pdfFile, new PdfRepair().convert(pdfFile));
        }
        if (this.onExitBookletPageOrder) {
            replaceWithConvertedPdf(pdfFile,
                    new PdfToBooklet().convert(pdfFile));
        }

        /*
         * Ad-hoc rotate?
         */
        if (this.targetPdfCopyPages2Align != null
                && this.targetPdfCopyPages2Align.size() > 0) {
            replaceWithConvertedPdf(pdfFile,
                    new PdfToRotateAlignedPdf(this.firstPageSeenAsLandscape,
                            this.targetPdfCopyPages2Align).convert(pdfFile));
        }

        /*
         * Annotate URLs including Letterhead. Note: optional PDF encryption is
         * part of last action.
         */
        if (this.onExitAnnotateUrls) {

            final PdfToAnnotatedURL converter;

            if (this.onExitStampEncryption) {
                converter = new PdfToAnnotatedURL(this.pdfAllow,
                        this.pdfOwnerPass, this.pdfUserPass);
            } else {
                converter = new PdfToAnnotatedURL();
            }

            replaceWithConvertedPdf(pdfFile, converter.convert(pdfFile));
        }
    }

    @Override
    protected void onInitJob(final String jobPfdName, final Integer userRotate)
            throws Exception {

        this.jobPdfFileWlk = new File(jobPfdName);
        this.readerWlk = new PdfReader(jobPfdName);

        this.jobUserRotateWlk = userRotate;
        this.jobRangesWlk = new StringBuilder();
    }

    @Override
    protected void onProcessJobPages(final int nPageFrom, final int nPageTo,
            final boolean removeGraphics) throws Exception {

        this.isRemoveGraphics = removeGraphics;

        if (this.jobRangesWlk.length() > 0) {
            this.jobRangesWlk.append(",");
        }

        this.jobRangesWlk.append(nPageFrom);

        if (nPageFrom != nPageTo) {
            this.jobRangesWlk.append("-").append(nPageTo);
        }
    }

    /**
     * Lazy creates a 1-page {@link PdfReader} with one (1) blank page.
     * <p>
     * Note: in-memory size of the document is approx. 886 bytes.
     * </p>
     *
     * @param pageSize
     *            The size of the page.
     * @return The {@link PdfReader}.
     * @throws DocumentException
     *             When error creating the PDF document.
     * @throws IOException
     *             When IO errors creating the reader.
     */
    private PdfReader getBlankPageReader(final Rectangle pageSize)
            throws DocumentException, IOException {

        if (this.singleBlankPagePdfReader == null) {
            this.singleBlankPagePdfReader =
                    ITextPdfCreator.createBlankPageReader(pageSize);
        }
        return this.singleBlankPagePdfReader;
    }

    /**
     * @param nPage
     *            One-based page number in resulting PDF document.
     * @param pageRotationCur
     *            Current page rotation.
     * @param pageRotationUser
     *            Rotation requested by user.
     * @param alignedRotation
     *            Rotation needed to become same orientation as 1st printed
     *            page.
     * @return Rotation to be applied for this page <b>now</b>.
     * @throws IOException
     *             When IO error.
     */
    private int onExitJobPagePrintAlign(final int nPage,
            final int pageRotationCur, final int pageRotationUser,
            final int alignedRotation) throws IOException {

        if (this.isApplyLetterhead()) {

            if (pageRotationUser == pageRotationCur) {
                /*
                 * If alignment is needed, perform later.
                 */
                if (alignedRotation != pageRotationCur) {
                    this.targetPdfCopyPages2Align.add(Integer.valueOf(nPage));
                }
                /*
                 * Use current orientation to correctly position letterhead.
                 */
                return pageRotationCur;
            }

            /*
             * User requested a different orientation: perform alignment later
             * and use user requested orientation to correctly position
             * letterhead.
             */
            this.targetPdfCopyPages2Align.add(Integer.valueOf(nPage));

            return pageRotationUser;

        } else {
            /*
             * No letterhead: user requested orientation is irrelevant. Use
             * aligned rotation.
             */
            return alignedRotation;
        }
    }

    @Override
    protected void onExitJob(final int blankPagesToAppend) throws Exception {

        this.readerWlk.selectPages(this.jobRangesWlk.toString());

        /*
         * Lazy initialize on first page of first job.
         */
        if (this.isForPrinting() && this.firstPageSeenAsLandscape == null) {

            final int firstPage = 1;

            final AffineTransform ctm = PdfPageRotateHelper
                    .getPdfPageCTM(this.readerWlk, firstPage);

            final int page1Rotation = this.readerWlk.getPageRotation(firstPage);
            final boolean page1Landscape = PdfPageRotateHelper
                    .isLandscapePage(this.readerWlk.getPageSize(firstPage));

            this.firstPageSeenAsLandscape =
                    PdfPageRotateHelper.isSeenAsLandscape(ctm, page1Rotation,
                            page1Landscape, this.jobUserRotateWlk);

            this.firstPageOrientationInfo =
                    PdfPageRotateHelper.getOrientationInfo(ctm, page1Rotation,
                            page1Landscape, this.jobUserRotateWlk);
        }

        final int pages = this.readerWlk.getNumberOfPages();

        /*
         * Traverse pages.
         */
        for (int nPage =
                1; nPage <= pages; nPage++, this.targetPdfCopyPageWlk++) {

            final int pageRotationCur = this.readerWlk.getPageRotation(nPage);

            final int pageRotationUser = PdfPageRotateHelper
                    .applyUserRotate(pageRotationCur, this.jobUserRotateWlk);

            // Page rotation to apply now.
            final int pageRotationNew;

            if (this.isForPrinting() && this.targetPdfCopyPageWlk > 0) {

                final int alignedRotation =
                        PdfPageRotateHelper.getAlignedRotation(this.readerWlk,
                                this.firstPageSeenAsLandscape, nPage);

                pageRotationNew = this.onExitJobPagePrintAlign(
                        this.targetPdfCopyPageWlk + 1, pageRotationCur,
                        pageRotationUser, alignedRotation);
            } else {
                pageRotationNew = pageRotationUser;
            }

            if (pageRotationCur != pageRotationNew) {
                final PdfDictionary pageDict = this.readerWlk.getPageN(nPage);
                pageDict.put(PdfName.ROTATE, new PdfNumber(pageRotationNew));
            }

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Page {}: rotate {}->{} | landscape [{}]", nPage,
                        pageRotationCur, pageRotationNew,
                        PdfPageRotateHelper.isLandscapePage(
                                this.readerWlk.getPageSize(nPage)));
            }

            final PdfImportedPage importedPage;

            if (isPageContentsPresent(this.readerWlk, nPage)) {

                importedPage = this.targetPdfCopy
                        .getImportedPage(this.readerWlk, nPage);

            } else {
                /*
                 * Replace page without /Contents with our own blank content.
                 * See Mantis #614.
                 */
                importedPage = this.targetPdfCopy.getImportedPage(this
                        .getBlankPageReader(this.targetPdfCopy.getPageSize()),
                        1);

                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info(String.format(
                            "File [%s] page [%d] has NO /Contents: "
                                    + "replaced by blank content.",
                            this.jobPdfFileWlk.getName(), nPage));
                }
            }
            this.targetPdfCopy.addPage(importedPage);
        }

        //
        for (int i =
                0; i < blankPagesToAppend; i++, this.targetPdfCopyPageWlk++) {
            this.targetPdfCopy.addPage(this.targetPdfCopy.getImportedPage(
                    this.getBlankPageReader(this.targetPdfCopy.getPageSize()),
                    1));
        }

        this.targetPdfCopy.freeReader(this.readerWlk);

        this.readerWlk.close();
        this.readerWlk = null;
    }

    @Override
    protected void onExitJobs() throws Exception {

        if (this.targetDocument.isOpen()) {
            this.targetDocument.close();
        }

        this.targetDocument = null;
    }

    @Override
    protected void onStampLetterhead(final String pdfLetterhead)
            throws Exception {
        this.letterheadReader = new PdfReader(pdfLetterhead);
        this.onExitAnnotateUrls =
                this.isAnnotateUrls && hasFonts(this.letterheadReader);
    }

    @Override
    protected void onCompress() throws Exception {
        /*
         * iText uses compression by default, but you can set full compression,
         * which make it a PDF 1.5 version.
         */
        this.targetStamper.setFullCompression();
    }

    @Override
    protected void onStampEncryptionForExport(
            final PdfProperties.PdfAllow allow, final String ownerPass,
            final String userPass) {

        final int iPermissions = ITextHelperV5.getPermissions(allow);

        this.onExitStampEncryption = this.onExitAnnotateUrls;

        if (this.onExitStampEncryption) {
            this.pdfAllow = allow;
            this.pdfOwnerPass = ownerPass;
            this.pdfUserPass = userPass;
        } else {
            try {
                this.targetStamper.setEncryption(true, userPass, ownerPass,
                        iPermissions);
            } catch (DocumentException e) {
                throw new SpException(e);
            }
        }
    }

    @Override
    protected void onProcessFinally() {

        if (this.targetDocument != null) {
            this.targetDocument.close();
            this.targetDocument = null;
        }

        // Important: Close stamper before closing the reader(s).
        if (this.targetStamper != null) {
            try {
                this.targetStamper.close();
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            } finally {
                this.targetStamper = null;
            }
        }

        if (this.letterheadReader != null) {
            this.letterheadReader.close();
        }

        if (this.overlayReaders != null) {
            for (final PdfReader rdr : this.overlayReaders) {
                rdr.close();
            }
        }

        if (this.readerWlk != null) {
            this.readerWlk.close();
            this.readerWlk = null;
        }

        new File(this.targetPdfCopyFilePath).delete();
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected void onStampMetaDataForExport(final Calendar now,
            final PdfProperties propPdf) {

        java.util.HashMap info = this.readerWlk.getInfo();

        info.put(PDF_INFO_KEY_TITLE, propPdf.getDesc().getTitle());

        if (propPdf.getApply().getSubject()) {
            info.put(PDF_INFO_KEY_SUBJECT, propPdf.getDesc().getSubject());
        }

        info.put(PDF_INFO_KEY_AUTHOR, propPdf.getDesc().getAuthor());

        /*
         * info.setCreationDate(now);
         *
         * info.setModificationDate(now);
         */

        info.put(PDF_INFO_KEY_CREATOR, getCreatorString());

        if (propPdf.getApply().getKeywords()) {
            info.put(PDF_INFO_KEY_KEYWORDS, propPdf.getDesc().getKeywords());
        }

        this.targetStamper.setMoreInfo(info);
    }

    @Override
    protected void onStampMetaDataForPrinting(final Calendar now,
            final PdfProperties propPdf) {

        java.util.HashMap<String, String> info = this.readerWlk.getInfo();

        info.put(PDF_INFO_KEY_TITLE, propPdf.getDesc().getTitle());
        info.put(PDF_INFO_KEY_SUBJECT, "FOR PRINTING PURPOSES ONLY");
        info.put(PDF_INFO_KEY_AUTHOR, CommunityDictEnum.SAVAPAGE.getWord());

        info.put(PDF_INFO_KEY_CREATOR, getCreatorString());

        // info.setModificationDate(now);
        if (propPdf.getApply().getKeywords()) {
            info.put(PDF_INFO_KEY_KEYWORDS, propPdf.getDesc().getKeywords());
        }

        this.targetStamper.setMoreInfo(info);
    }

    @Override
    protected void onInitStamp() throws Exception {
        this.readerWlk = new PdfReader(this.targetPdfCopyFilePath);
        this.targetStamper =
                new PdfStamper(this.readerWlk, new FileOutputStream(pdfFile));
    }

    /**
     *
     * @param reader
     *            PDF reader.
     * @return {@code true} if PDF document contains fonts.
     * @throws IOException
     *             If IO error.
     */
    private static boolean hasFonts(final PdfReader reader) throws IOException {
        return PdfDocumentFonts.create(reader).getFonts().size() > 0;
    }

    @Override
    protected void onExitStamp(final Map<Integer, String> pageOverlay)
            throws Exception {

        if (this.isRemoveGraphics) {
            minifyPdfImages();
        }

        if (!pageOverlay.isEmpty()) {
            this.applyOverlay(pageOverlay);
        }

        if (this.isApplyLetterhead()) {
            applyLetterhead();
        }

        if (this.isAnnotateUrls && !this.onExitAnnotateUrls) {
            ITextPdfUrlAnnotator.annotate(this.readerWlk, this.targetStamper);
        }
    }

    /**
     * Applies the page overlays.
     *
     * @param pageOverlay
     *            Base64 encoded SVG overlay (value) for one-based ordinal pages
     *            (key) of the overall PDF document.
     * @throws IOException
     *             If IO error.
     */
    private void applyOverlay(final Map<Integer, String> pageOverlay)
            throws IOException {

        final IDocFileConverter converter = new SvgToPdf();

        this.overlayReaders = new ArrayList<>();

        final int nDocPages = this.readerWlk.getNumberOfPages();
        int nPageWlk;

        for (int i = 0; i < nDocPages; i++) {

            nPageWlk = i + 1;

            final String svg64 = pageOverlay.get(Integer.valueOf(nPageWlk));
            if (svg64 == null) {
                continue;
            }

            final File tempFileSVG = File.createTempFile("temp-", ".svg");

            File tempFilePDF = null;
            PdfReader overlayReader = null;

            try (FileOutputStream fostr = new FileOutputStream(tempFileSVG)) {

                fostr.write(Base64.getDecoder().decode(svg64));
                fostr.close();

                tempFilePDF =
                        converter.convert(DocContentTypeEnum.SVG, tempFileSVG);

                overlayReader = new PdfReader(tempFilePDF.getAbsolutePath());

                final PdfImportedPage importedPage =
                        this.targetStamper.getImportedPage(overlayReader, 1);

                // Add overlay 'on top of' the page content.
                final PdfContentByte contentByte =
                        this.targetStamper.getOverContent(nPageWlk);

                // ----------------------------------------------------
                // SCALE + TRANSLATE
                // ----------------------------------------------------
                // a : sX (scale, in x-direction)
                // b : 0
                // c : 0
                // d : sY (scale, in y-direction)
                // e : tX (translate, moves e pixels in x-direction)
                // f : tY (translate, moves f pixels in y-direction)
                // ----------------------------------------------------
                final float sX = 1.0f;
                final float sY = sX;

                contentByte.addTemplate(importedPage, sX, 0, 0, sY, 0, 0);

            } catch (DocContentToPdfException | UnavailableException e) {
                throw new SpException(e.getMessage());
            } finally {
                if (overlayReader != null) {
                    this.overlayReaders.add(overlayReader);
                }
                tempFileSVG.delete();
                if (tempFilePDF != null) {
                    tempFilePDF.delete();
                }
            }
        } // for-loop
    }

    /**
     * Applies the letterhead.
     *
     * @throws IOException
     *             If IO error.
     */
    private void applyLetterhead() throws IOException {

        int nLetterheadPage = 0;
        int nLetterheadPageMax = 1;

        PdfImportedPage letterheadPage = null;

        nLetterheadPageMax = myLetterheadJob.getPages();

        /*
         * Iterate over document's pages.
         */
        final int nDocPages = this.readerWlk.getNumberOfPages();

        Rectangle rectLetterheadPageWlk = null;
        int nPageWlk;

        for (int i = 0; i < nDocPages; i++) {

            nPageWlk = i + 1;

            /*
             * If the letterhead document has more than one page, each page of
             * the letterhead is applied to the corresponding page of the output
             * document. If the output document has more pages than the
             * letterhead, then the final letterhead page is repeated across
             * these remaining pages of the output document.
             */
            if (nLetterheadPage < nLetterheadPageMax) {

                nLetterheadPage++;
                rectLetterheadPageWlk =
                        this.letterheadReader.getPageSize(nLetterheadPage);

                /*
                 * Create a PdfTemplate from the nth page of the letterhead
                 * (PdfImportedPage is derived from PdfTemplate).
                 */
                letterheadPage = this.targetStamper.getImportedPage(
                        this.letterheadReader, nLetterheadPage);
            }

            /*
             * The letterhead page is added as a layer, 'on top of' or
             * 'underneath' the page content.
             */
            final PdfContentByte contentByte;

            if (myLetterheadJob.getForeground()) {
                contentByte = this.targetStamper.getOverContent(nPageWlk);
            } else {
                contentByte = this.targetStamper.getUnderContent(nPageWlk);
            }

            final Rectangle rectDocPage = this.readerWlk.getPageSize(nPageWlk);
            final int pageRotation = this.readerWlk.getPageRotation(nPageWlk);

            final boolean swapWidhtHeight;

            if (pageRotation == PdfPageRotateHelper.PDF_ROTATION_0.intValue()
                    || pageRotation == PdfPageRotateHelper.PDF_ROTATION_180
                            .intValue()) {

                swapWidhtHeight = false;

            } else if (pageRotation == PdfPageRotateHelper.PDF_ROTATION_90
                    .intValue()
                    || pageRotation == PdfPageRotateHelper.PDF_ROTATION_270
                            .intValue()) {

                swapWidhtHeight = true;

            } else {
                throw new IllegalStateException(String
                        .format("Invalid page rotation [%d]", pageRotation));
            }

            /*
             * Virtual page width and height used for calculating the letterhead
             * translate and rotate.
             */
            final float virtualDocPageWidth;
            final float virtualDocPageHeight;

            if (swapWidhtHeight) {
                virtualDocPageWidth = rectDocPage.getHeight();
                virtualDocPageHeight = rectDocPage.getWidth();
            } else {
                virtualDocPageWidth = rectDocPage.getWidth();
                virtualDocPageHeight = rectDocPage.getHeight();
            }

            /*
             * Scale letterhead page and move it so it fits within the
             * document's page (if document's page is cropped, this scale might
             * not be small enough).
             */
            final float hScale =
                    virtualDocPageWidth / rectLetterheadPageWlk.getWidth();
            final float vScale =
                    virtualDocPageHeight / rectLetterheadPageWlk.getHeight();

            /*
             * Proportional scaling: pick the smallest scaling factor.
             */
            final float sX;

            if (hScale < vScale) {
                sX = hScale;
            } else {
                sX = vScale;
            }

            final float sY = sX;

            final float tX = (float) ((virtualDocPageWidth
                    - rectLetterheadPageWlk.getWidth() * sX) / 2.0);

            final float tY = (float) ((virtualDocPageHeight
                    - rectLetterheadPageWlk.getHeight() * sY) / 2.0);

            // ----------------------------------------------------
            // SCALE + TRANSLATE
            // ----------------------------------------------------
            // a : sX (scale, in x-direction)
            // b : 0
            // c : 0
            // d : sY (scale, in y-direction)
            // e : tX (translate, moves e pixels in x-direction)
            // f : tY (translate, moves f pixels in y-direction)
            // ----------------------------------------------------
            contentByte.addTemplate(letterheadPage, sX, 0, 0, sY, tX, tY);
        }
    }

    /**
     *
     * Creates a one-pixel white image.
     * <p>
     * See
     * <a href="http://www.javamex.com/tutorials/graphics/bufferedimage.shtml">
     * this tutorial</a<.
     * </p>
     *
     * @throws IOException
     * @throws BadElementException
     */
    private static Image createOnePixel()
            throws BadElementException, IOException {

        final BufferedImage img =
                new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        img.setRGB(0, 0, Color.WHITE.getRGB());

        final java.awt.Image awtImg = java.awt.Toolkit.getDefaultToolkit()
                .createImage(img.getSource());

        return com.itextpdf.text.Image.getInstance(awtImg, null);
    }

    /**
     *
     * @throws PdfException
     * @throws DocumentException
     * @throws IOException
     */
    private void minifyPdfImages()
            throws PdfException, DocumentException, IOException {

        final PdfReader pdf = this.readerWlk;
        final int n = pdf.getNumberOfPages();
        final PdfStamper stp = this.targetStamper;

        final Image imgPixel = createOnePixel();

        for (int j = 0; j < n; j++) {

            final PdfWriter writer = stp.getWriter();

            final PdfDictionary pg = pdf.getPageN(j + 1);

            final PdfDictionary res = (PdfDictionary) PdfReader
                    .getPdfObject(pg.get(PdfName.RESOURCES));

            final PdfDictionary xobj = (PdfDictionary) PdfReader
                    .getPdfObject(res.get(PdfName.XOBJECT));

            if (xobj == null) {
                continue;
            }

            for (final Iterator<PdfName> it = xobj.getKeys().iterator(); it
                    .hasNext();) {

                final PdfObject obj = xobj.get(it.next());

                if (obj.isIndirect()) {

                    final PdfDictionary tg =
                            (PdfDictionary) PdfReader.getPdfObject(obj);

                    if (tg != null) {

                        final PdfName type = (PdfName) PdfReader
                                .getPdfObject(tg.get(PdfName.SUBTYPE));

                        if (PdfName.IMAGE.equals(type)) {

                            PdfReader.killIndirect(obj);
                            final Image maskImage = imgPixel.getImageMask();

                            if (maskImage != null) {
                                writer.addDirectImageSimple(maskImage);
                            } else {
                                // When this happens, the original image is
                                // still visible.
                            }

                            writer.addDirectImageSimple(imgPixel,
                                    (PRIndirectReference) obj);
                        }
                    }
                }
            }
        }
    }

}
