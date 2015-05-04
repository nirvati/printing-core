/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2015 Datraverse B.V.
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.core.pdf;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import javax.print.attribute.Size2DSyntax;
import javax.print.attribute.standard.MediaSize;

import org.savapage.core.SpException;
import org.savapage.core.community.CommunityDictEnum;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.fonts.InternalFontFamilyEnum;
import org.savapage.core.json.PdfProperties;
import org.savapage.core.util.MediaUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.itextpdf.text.BadElementException;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.ExceptionConverter;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PRIndirectReference;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfCopy;
import com.itextpdf.text.pdf.PdfDictionary;
import com.itextpdf.text.pdf.PdfException;
import com.itextpdf.text.pdf.PdfImportedPage;
import com.itextpdf.text.pdf.PdfName;
import com.itextpdf.text.pdf.PdfNumber;
import com.itextpdf.text.pdf.PdfObject;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.parser.ContentByteUtils;
import com.itextpdf.text.pdf.parser.FilteredTextRenderListener;
import com.itextpdf.text.pdf.parser.PdfContentStreamProcessor;

/**
 * PDF Creator using the iText AGPL version.
 *
 * @author Datraverse B.V.
 *
 */
public final class ITextPdfCreator extends AbstractPdfCreator {

    private static final int ITEXT_POINTS_PER_INCH = 72;

    /**
     * The logger.
     */
    private static final Logger LOGGER = LoggerFactory
            .getLogger(ITextPdfCreator.class);

    private String targetPdfCopyFilePath = null;
    private Document targetDocument = null;
    private PdfCopy targetPdfCopy = null;
    private PdfStamper targetStamper = null;

    private PdfReader readerWlk = null;
    private PdfReader letterheadReader = null;

    private String jobRangesWlk = null;
    private String jobRotationWlk = null;

    private boolean isRemoveGraphics = false;

    /**
     * Create URL links by default.
     */
    private final boolean isAnnotateUrls = true;

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
     * Gets the {@link Rectangle} of default page size.
     *
     * @return {@code null} when default page size not found.
     */
    public static Rectangle getDefaultPageSize() {

        final Rectangle pageSize;

        final String papersize =
                ConfigManager.instance().getConfigValue(
                        Key.SYS_DEFAULT_PAPER_SIZE);

        if (papersize.equals(IConfigProp.PAPERSIZE_V_SYSTEM)) {

            float[] size =
                    MediaSize.getMediaSizeForName(
                            MediaUtils.getHostDefaultMediaSize()).getSize(
                            Size2DSyntax.INCH);

            pageSize =
                    new Rectangle(size[0] * ITEXT_POINTS_PER_INCH, size[1]
                            * ITEXT_POINTS_PER_INCH);

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
     * Checks if rectangles have same size (orientation portrait/landscape is
     * ignored).
     *
     * @param rectA
     *            The first {@link Rectangle}.
     * @param rectB
     *            The second {@link Rectangle}.
     * @return {@code true} if same size.
     */
    private boolean isSameSize(final Rectangle rectA, final Rectangle rectB) {

        final boolean isSame;

        if (rectA.getWidth() == rectB.getWidth()
                && rectA.getHeight() == rectB.getHeight()) {
            // portrait == portrait, landscape = landscape
            isSame = true;
        } else if (rectA.getWidth() == rectB.getHeight()
                && rectA.getHeight() == rectB.getWidth()) {
            // portrait == landscape
            isSame = true;
        } else {
            isSame = false;
        }
        return isSame;

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

        final int[] size = MediaUtils.getMediaWidthHeight(pageFormat);

        int iSizeWidth = 0;
        int iSizeHeight = 1;

        /*
         * Correct for landscape orientation.
         */
        if (mediabox.getWidth() > mediabox.getHeight()) {
            iSizeWidth = 1;
            iSizeHeight = 0;
        }

        final SpPdfPageProps pageProps = new SpPdfPageProps();

        pageProps.setMmWidth(size[iSizeWidth]);
        pageProps.setMmHeight(size[iSizeHeight]);
        pageProps.setSize(MediaUtils.getMediaSizeName(pageFormat));

        return pageProps;
    }

    @Override
    public List<SpPdfPageProps> getPageSizeChunks(final String filePathPdf)
            throws PdfSecurityException {

        final List<SpPdfPageProps> pagePropsList = new ArrayList<>();

        PdfReader reader = null;

        try {
            /*
             * Instantiating/opening can throw a BadPasswordException.
             */
            reader = new PdfReader(filePathPdf);

            if (reader.isEncrypted()) {
                throw new PdfSecurityException("Encrypted PDF not supported.");
            }

            final int nPagesTot = reader.getNumberOfPages();

            /*
             * First Page.
             */
            SpPdfPageProps pagePropsItem = null;
            Rectangle rectangleItem = null;
            Rectangle rectangleWlk = null;

            int nPagesItem = 0;
            int i = 0;

            if (nPagesTot > 0) {
                // Note: page index is 1-based.
                rectangleWlk = reader.getPageSize(i + 1);

                nPagesItem = 0;
                rectangleItem = rectangleWlk;
                pagePropsItem = this.createPageProps(rectangleWlk);
                pagePropsList.add(pagePropsItem);
            }
            /*
             * Process pages.
             */
            for (; i < nPagesTot; i++) {

                /*
                 * Next page.
                 */
                rectangleWlk = reader.getPageSize(i + 1);

                /*
                 * New page size.
                 */
                if (!this.isSameSize(rectangleItem, rectangleWlk)) {

                    // flush pending item
                    pagePropsItem.setNumberOfPages(nPagesItem);

                    // create new
                    nPagesItem = 0;
                    rectangleItem = rectangleWlk;
                    pagePropsItem = this.createPageProps(rectangleWlk);
                    pagePropsList.add(pagePropsItem);
                }

                nPagesItem++;
            }

            if (pagePropsItem != null) {
                // flush last pending item
                pagePropsItem.setNumberOfPages(nPagesItem);
            }

        } catch (com.itextpdf.text.exceptions.BadPasswordException e) {
            throw new PdfSecurityException(
                    "Password protected PDF not supported.");
        } catch (IOException e) {
            throw new SpException(e);

        } finally {

            if (reader != null) {
                reader.close();
            }
        }

        return pagePropsList;
    }

    @Override
    public SpPdfPageProps getPageProps(final String filePathPdf)
            throws PdfSecurityException {

        SpPdfPageProps pageProps = null;
        PdfReader reader = null;

        try {
            /*
             * Instantiating/opening can throw a BadPasswordException.
             */
            reader = new PdfReader(filePathPdf);

            if (reader.isEncrypted()) {
                throw new PdfSecurityException("Encrypted PDF not supported.");
            }

            pageProps = this.createPageProps(reader.getPageSize(1));
            pageProps.setNumberOfPages(reader.getNumberOfPages());

        } catch (com.itextpdf.text.exceptions.BadPasswordException e) {
            throw new PdfSecurityException(
                    "Password protected PDF not supported.");
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

        this.targetPdfCopyFilePath = pdfFile + ".tmp";

        try {
            this.targetDocument = new Document();
            this.targetPdfCopy =
                    new PdfCopy(this.targetDocument, new FileOutputStream(
                            this.targetPdfCopyFilePath));
            this.targetDocument.open();
        } catch (Exception e) {
            throw new SpException(e);
        }
    }

    @Override
    protected void onExit() throws Exception {
    }

    @Override
    protected void onInitJob(final String jobPfdName, final String rotation)
            throws Exception {
        this.readerWlk = new PdfReader(jobPfdName);
        this.jobRotationWlk = rotation;
        this.jobRangesWlk = "";
    }

    @Override
    protected void onProcessJobPages(final int nPageFrom, final int nPageTo,
            final boolean removeGraphics) throws Exception {

        if (!this.jobRangesWlk.isEmpty()) {
            this.jobRangesWlk += ",";
        }

        this.jobRangesWlk += String.valueOf(nPageFrom);

        if (nPageFrom != nPageTo) {
            this.jobRangesWlk += "-" + String.valueOf(nPageTo);
        }

        this.isRemoveGraphics = removeGraphics;
    }

    @Override
    protected void onExitJob() throws Exception {

        this.readerWlk.selectPages(this.jobRangesWlk);
        int pages = this.readerWlk.getNumberOfPages();

        for (int i = 0; i < pages;) {

            ++i;

            /*
             * Rotate for BOTH export and printing.
             */
            if (this.jobRotationWlk != null) {
                final int rotate = Integer.valueOf(this.jobRotationWlk);
                if (rotate != 0) {
                    PdfDictionary pageDict = this.readerWlk.getPageN(i);
                    pageDict.put(PdfName.ROTATE, new PdfNumber(rotate));
                }
            }

            final PdfImportedPage page =
                    this.targetPdfCopy.getImportedPage(this.readerWlk, i);
            this.targetPdfCopy.addPage(page);
        }
        this.targetPdfCopy.freeReader(this.readerWlk);
        this.readerWlk.close();
        this.readerWlk = null;
    }

    @Override
    protected void onExitJobs() throws Exception {
        this.targetDocument.close();
        this.targetDocument = null;
    }

    @Override
    protected void onStampLetterhead(final String pdfLetterhead)
            throws Exception {

    }

    @Override
    protected void onCompress() throws Exception {
        /*
         * iText uses compression by default, but you can set full compression,
         * which make it PDF 1.5 version.
         */
        this.targetStamper.setFullCompression();
    }

    @Override
    protected void onStampEncryptionForExport(final PdfProperties propPdf,
            final String ownerPass, final String userPass,
            final boolean hasVisitorText) {

        PdfProperties.PdfAllow allow = propPdf.getAllow();

        int iPermissions = 0;

        boolean bStrength = true; // 128 bit: TODO

        propPdf.getEncryption();

        if (allow.getAssembly()) {
            iPermissions |= PdfWriter.ALLOW_ASSEMBLY;
        }
        if (allow.getCopy()) {
            iPermissions |= PdfWriter.ALLOW_COPY;
        }
        if (allow.getCopyForAccess()) {
            iPermissions |= PdfWriter.ALLOW_SCREENREADERS;
        }
        if (allow.getFillin()) {
            iPermissions |= PdfWriter.ALLOW_FILL_IN;
        }
        if (allow.getPrinting()) {
            iPermissions |= PdfWriter.ALLOW_PRINTING;
        }
        if (allow.getDegradedPrinting()) {
            iPermissions |= PdfWriter.ALLOW_DEGRADED_PRINTING;
        }

        if (!hasVisitorText) {
            if (allow.getModifyContents()) {
                iPermissions |= PdfWriter.ALLOW_MODIFY_CONTENTS;
            }
            if (allow.getModifyAnnotations()) {
                iPermissions |= PdfWriter.ALLOW_MODIFY_ANNOTATIONS;
            }
        }

        try {
            this.targetStamper.setEncryption(bStrength, userPass, ownerPass,
                    iPermissions);
        } catch (DocumentException e) {
            throw new SpException(e);
        }

    }

    @Override
    protected void onStampEncryptionForPrinting() {
        /*
         * For security reasons, just printing is allowed.
         */
        int iPermissions = 0;
        boolean bStrength = true; // 128 bit

        iPermissions |= PdfWriter.ALLOW_PRINTING;
        iPermissions |= PdfWriter.ALLOW_DEGRADED_PRINTING;

        try {
            this.targetStamper.setEncryption(bStrength, null, null,
                    iPermissions);
        } catch (DocumentException e) {
            throw new SpException(e);
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

        if (this.readerWlk != null) {
            this.readerWlk.close();
            this.readerWlk = null;
        }

        new File(this.targetPdfCopyFilePath).delete();
    }

    /**
     *
     * @return The Creator string visible in the PDF properties of PDF Reader.
     */
    private String getCreatorString() {
        return String.format("%s %s • %s • %s",
                CommunityDictEnum.SAVAPAGE.getWord(),
                ConfigManager.getAppVersion(),
                CommunityDictEnum.LIBRE_PRINT_MANAGEMENT.getWord(),
                CommunityDictEnum.SAVAPAGE_DOT_ORG.getWord());
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected void onStampMetaDataForExport(final Calendar now,
            final PdfProperties propPdf) {

        java.util.HashMap info = this.readerWlk.getInfo();

        info.put("Title", propPdf.getDesc().getTitle());

        if (propPdf.getApply().getSubject()) {
            info.put("Subject", propPdf.getDesc().getSubject());
        }

        info.put("Author", propPdf.getDesc().getAuthor());

        /*
         * info.setCreationDate(now);
         *
         * info.setModificationDate(now);
         */

        info.put("Creator", this.getCreatorString());

        if (propPdf.getApply().getKeywords()) {
            info.put("Keywords", propPdf.getDesc().getKeywords());
        }

        this.targetStamper.setMoreInfo(info);

    }

    @Override
    protected void onStampMetaDataForPrinting(final Calendar now,
            final PdfProperties propPdf) {

        java.util.HashMap<String, String> info = this.readerWlk.getInfo();

        info.put("Title", propPdf.getDesc().getTitle());
        info.put("Subject", "FOR PRINTING PURPOSES ONLY");
        info.put("Author", CommunityDictEnum.SAVAPAGE.getWord());

        info.put("Creator", this.getCreatorString());

        // info.setModificationDate(now);
        if (propPdf.getApply().getKeywords()) {
            info.put("Keywords", propPdf.getDesc().getKeywords());
        }

        this.targetStamper.setMoreInfo(info);
    }

    @Override
    protected void onStampRotateForPrinting() throws Exception {
        /*
         * No code intended: rotation is done in exitStamping.
         */
    }

    @Override
    protected void onInitStamp() throws Exception {
        this.readerWlk = new PdfReader(this.targetPdfCopyFilePath);
        this.targetStamper =
                new PdfStamper(this.readerWlk, new FileOutputStream(pdfFile));
    }

    @Override
    protected void onExitStamp() throws Exception {

        if (this.isRemoveGraphics) {
            minifyPdfImages();
        }

        if (this.isAnnotateUrls && !isForPrinting()) {
            annotateUrls();
        }

        final boolean isLetterhead = myPdfFileLetterhead != null;

        if (!(isForPrinting() || isLetterhead)) {
            return;
        }

        int nLetterheadPage = 0;
        int nLetterheadPageMax = 1;

        Rectangle lhPageSize = null;
        PdfImportedPage letterheadPage = null;

        if (isLetterhead) {
            this.letterheadReader = new PdfReader(myPdfFileLetterhead);
            nLetterheadPageMax = myLetterheadJob.getPages();
        }

        /*
         * Iterate over document's pages.
         */
        int nDocPages = this.readerWlk.getNumberOfPages();

        for (int i = 0; i < nDocPages;) {

            ++i;

            /*
             * Check if we rotated this page to show it in landscape
             * orientation.
             */
            final int pageRotation = this.readerWlk.getPageRotation(i);
            final boolean isPageRotated = pageRotation != 0;

            /*
             * Gets the page size without taking rotation into account.
             */
            Rectangle docPageSize = this.readerWlk.getPageSize(i);

            /*
             * Virtual page width and height used for calculating the letterhead
             * translate an rotate.
             */
            float virtualPageWidth;
            float virtualPageHeight;

            if (isPageRotated) {
                /*
                 * Since this is a landscape page in portrait orientation: we
                 * swap height and width for calculation of the translate and
                 * rotate of the letterhead.
                 */
                virtualPageWidth = docPageSize.getHeight();
                virtualPageHeight = docPageSize.getWidth();
            } else {
                virtualPageWidth = docPageSize.getWidth();
                virtualPageHeight = docPageSize.getHeight();
            }

            boolean rotateLetterhead = false;

            /*
             * Rotate (back) to portrait for printing
             */
            if (isForPrinting()) {

                rotateLetterhead =
                        isPageRotated || (virtualPageHeight < virtualPageWidth);

                if (rotateLetterhead) {

                    int rotate;

                    if (isPageRotated) {
                        /*
                         * We rotate back to its original.
                         */
                        rotate = 0;
                    } else {
                        rotate = 270;
                    }

                    PdfDictionary pageDict = this.readerWlk.getPageN(i);
                    pageDict.put(PdfName.ROTATE, new PdfNumber(rotate));
                }
            }

            /*
             * Apply (rotated) letterhead
             */
            if (isLetterhead) {

                /*
                 * If the letterhead document has more than one page, each page
                 * of the letterhead is applied to the corresponding page of the
                 * output document. If the output document has more pages than
                 * the letterhead, then the final letterhead page is repeated
                 * across these remaining pages of the output document.
                 */
                if (nLetterheadPage < nLetterheadPageMax) {

                    nLetterheadPage++;
                    lhPageSize =
                            this.letterheadReader.getPageSize(nLetterheadPage);

                    /*
                     * Create a PdfTemplate from the nth page of the letterhead
                     * (PdfImportedPage is derived from PdfTemplate).
                     */
                    letterheadPage =
                            this.targetStamper.getImportedPage(
                                    this.letterheadReader, nLetterheadPage);
                }

                /*
                 * Scale letterhead page and move it so that it fits within the
                 * document's page (if document's page is cropped, this scale
                 * might not be small enough).
                 */

                float hScale = virtualPageWidth / lhPageSize.getWidth();

                float vScale = virtualPageHeight / lhPageSize.getHeight();

                /*
                 * Proportional scaling: pick the smallest scaling factor.
                 */
                float sX = (hScale < vScale) ? hScale : vScale;
                float sY = sX;

                float tX =
                        (float) ((virtualPageWidth - lhPageSize.getWidth() * sX) / 2.0);

                float tY =
                        (float) ((virtualPageHeight - lhPageSize.getHeight()
                                * sY) / 2.0);

                /*
                 * Add letterhead page as a layer ...
                 */
                PdfContentByte contentByte = null;

                if (myLetterheadJob.getForeground()) {
                    /*
                     * ... 'on top of' the page content.
                     */
                    contentByte = this.targetStamper.getOverContent(i);
                } else {
                    /*
                     * ... 'underneath' the page content.
                     */
                    contentByte = this.targetStamper.getUnderContent(i);
                }

                if (rotateLetterhead) {

                    /*
                     * There is one serious caveat when you rotate an object:
                     * the coordinate of the rotation pivot is (0, 0).
                     *
                     * If you rotate something, you have to watch out that it is
                     * not rotated 'off' your page.
                     *
                     * You may have to perform a translation to keep the object
                     * on the page. Of course you can combine translation (tX,
                     * tY), scaling (sX, sY) rotation (angle) in one matrix:
                     *
                     * NOTE:
                     *
                     * (1) The (0,0) origin in a PDF document is LOWER LEFT.
                     */

                    // ----------------------------------------------------
                    // ROTATE angle + SCALE + TRANSLATE
                    // ----------------------------------------------------
                    // a : sX * Math.cos(angle)
                    // b : sY * Math.sin(angle)
                    // c : -sX * Math.sin(angle)
                    // d : sY * Math.cos(angle)
                    // e : tX
                    // f : tY
                    // ----------------------------------------------------

                    /*
                     * NOTE: rotation is anti-clockwise, so an angle of 90
                     * degrees will work out as -90 degrees.
                     */

                    //
                    // Initial
                    //
                    // +---------------+
                    // | . . . . . . . |
                    // | . . . . . . . |
                    // | . . . . . . . |
                    // +---------+ . . |
                    // | x x x x | . . |
                    // | x x x x | . . |
                    // | x x x x | . . |
                    // | x x x x | . . |
                    // | x x x x | . . |
                    // | x x x x | . . |
                    // | x x x x | . . |
                    // 0,0-------+-----+
                    //
                    //
                    // Rotation (90 degrees)
                    //
                    // . . . . . . . . +---------------+
                    // . . . . . . . . | . . . . . . . |
                    // . . . . . . . . | . . . . . . . |
                    // . . . . . . . . | . . . . . . . |
                    // . . . . . . . . | . . . . . . . |
                    // . . . . . . . . | . . . . . . . |
                    // . . . . . . . . | . . . . . . . |
                    // +---------------+ . . . . . . . |
                    // | x x x x x x x | . . . . . . . |
                    // | x x x x x x x | . . . . . . . |
                    // | x x x x x x x | . . . . . . . |
                    // | x x x x x x x | . . . . . . . |
                    // +-------------0,0---------------+
                    //
                    //
                    // Translate:
                    //
                    // +---------------+
                    // | . . . . . . . |
                    // | . . . . . . . |
                    // | . . . . . . . |
                    // +---------------+
                    // | x x x x x x x |
                    // | x x x x x x x |
                    // | x x x x x x x |
                    // | x x x x x x x |
                    // +---------------+
                    // | . . . . . . . |
                    // | . . . . . . . |
                    // | . . . . . . . |
                    // 0,0-------------+
                    //

                    final double angle = Math.PI * .5; // 90 degree

                    float sin = (float) Math.sin(angle);
                    float cos = (float) Math.cos(angle);

                    contentByte.addTemplate(letterheadPage,
                    //
                            sX * cos,
                            //
                            sY * sin,
                            //
                            -sX * sin,
                            //
                            sY * cos,
                            //
                            lhPageSize.getHeight() * sY,
                            //
                            tX);

                } else {
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
                    contentByte.addTemplate(letterheadPage,
                    //
                            sX,
                            //
                            0,
                            //
                            0,
                            //
                            sY,
                            //
                            tX,
                            //
                            tY);
                }
            }

        }
    }

    /**
     *
     * Creates a one-pixel white image.
     * <p>
     * See <a
     * href="http://www.javamex.com/tutorials/graphics/bufferedimage.shtml">this
     * tutorial</a<.
     * </p>
     *
     * @throws IOException
     * @throws BadElementException
     */
    private static Image createOnePixel() throws BadElementException,
            IOException {

        final BufferedImage img =
                new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        img.setRGB(0, 0, Color.WHITE.getRGB());

        final java.awt.Image awtImg =
                java.awt.Toolkit.getDefaultToolkit().createImage(
                        img.getSource());

        return com.itextpdf.text.Image.getInstance(awtImg, null);
    }

    /**
     *
     * @throws PdfException
     * @throws DocumentException
     * @throws IOException
     */
    private void minifyPdfImages() throws PdfException, DocumentException,
            IOException {

        final PdfReader pdf = this.readerWlk;
        final int n = pdf.getNumberOfPages();
        final PdfStamper stp = this.targetStamper;

        final Image imgPixel = createOnePixel();

        for (int j = 0; j < n; j++) {

            final PdfWriter writer = stp.getWriter();

            final PdfDictionary pg = pdf.getPageN(j + 1);

            final PdfDictionary res =
                    (PdfDictionary) PdfReader.getPdfObject(pg
                            .get(PdfName.RESOURCES));

            final PdfDictionary xobj =
                    (PdfDictionary) PdfReader.getPdfObject(res
                            .get(PdfName.XOBJECT));

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

                        final PdfName type =
                                (PdfName) PdfReader.getPdfObject(tg
                                        .get(PdfName.SUBTYPE));

                        if (PdfName.IMAGE.equals(type)) {

                            PdfReader.killIndirect(obj);
                            final Image maskImage = imgPixel.getImageMask();

                            if (maskImage != null) {
                                writer.addDirectImageSimple(maskImage);
                            }

                            writer.addDirectImageSimple(imgPixel,
                                    (PRIndirectReference) obj);
                        }
                    }
                }
            }
        }
    }

    /**
     *
     * @throws IOException
     */
    private void annotateUrls() throws IOException {

        final PdfReader reader = this.readerWlk;
        final PdfStamper stamper = this.targetStamper;
        int pageCount = reader.getNumberOfPages();

        for (int i = 1; i <= pageCount; i++) {

            final ITextPdfUrlAnnotator delegate =
                    new ITextPdfUrlAnnotator(stamper, i);

            final FilteredTextRenderListener listener =
                    new FilteredTextRenderListener(delegate);

            final PdfContentStreamProcessor processor =
                    new PdfContentStreamProcessor(listener);

            final PdfDictionary pageDic = reader.getPageN(i);

            final PdfDictionary resourcesDic =
                    pageDic.getAsDict(PdfName.RESOURCES);

            try {
                final byte[] content =
                        ContentByteUtils.getContentBytesForPage(reader, i);

                processor.processContent(content, resourcesDic);

            } catch (ExceptionConverter e) {
                // TODO
                LOGGER.warn(String.format("%s [%s]", e.getClass()
                        .getSimpleName(), e.getMessage()));
            }

            // Flush remaining text
            delegate.checkCollectedText();
        }

    }
}