/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2014 Datraverse B.V.
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
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import javax.print.attribute.Size2DSyntax;
import javax.print.attribute.standard.MediaSize;

import org.apache.tika.metadata.DublinCore;
import org.savapage.core.SpException;
import org.savapage.core.community.CommunityDictEnum;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.fonts.InternalFontFamilyEnum;
import org.savapage.core.json.PdfProperties;
import org.savapage.core.util.MediaUtils;

import com.lowagie.text.BadElementException;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PRIndirectReference;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfCopy;
import com.lowagie.text.pdf.PdfDictionary;
import com.lowagie.text.pdf.PdfException;
import com.lowagie.text.pdf.PdfImportedPage;
import com.lowagie.text.pdf.PdfName;
import com.lowagie.text.pdf.PdfNumber;
import com.lowagie.text.pdf.PdfObject;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfStamper;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.xml.xmp.DublinCoreSchema;
import com.lowagie.text.xml.xmp.PdfSchema;
import com.lowagie.text.xml.xmp.XmpArray;
import com.lowagie.text.xml.xmp.XmpSchema;
import com.lowagie.text.xml.xmp.XmpWriter;

/**
 *
 * @author Datraverse B.V.
 *
 */
public final class ITextCreator extends AbstractPdfCreator {

    private String pdfFileCopy = null;
    private Document myDocument = null;
    private PdfCopy myCopy = null;
    private PdfReader myReader = null;
    private String myJobRanges = null;
    private String myRotate = null;
    private boolean myRemoveGraphics = false;
    private PdfStamper myStamper = null;

    /**
     * Create a {@link BaseFont} for an {@link InternalFontFamilyEnum}.
     *
     * @param The
     *            {@link InternalFontFamilyEnum}.
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
     *
     * @return
     */
    public static Rectangle getDefaultPageSize() {

        Rectangle pageSize = null;

        final String papersize =
                ConfigManager.instance().getConfigValue(
                        Key.SYS_DEFAULT_PAPER_SIZE);

        if (papersize.equals(IConfigProp.PAPERSIZE_V_SYSTEM)) {

            float[] size =
                    MediaSize.getMediaSizeForName(
                            MediaUtils.getHostDefaultMediaSize()).getSize(
                            Size2DSyntax.INCH);

            pageSize = new Rectangle(size[0] * 72, size[1] * 72);

        } else if (papersize.equals(IConfigProp.PAPERSIZE_V_LETTER)) {
            pageSize = PageSize.LETTER;
        } else if (papersize.equals(IConfigProp.PAPERSIZE_V_A4)) {
            pageSize = PageSize.A4;
        }
        return pageSize;
    }

    /**
     *
     * @param filePathPdf
     * @return
     */
    @Override
    public final int getNumberOfPagesInPdfFile(final String filePathPdf) {

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
     * @param rectB
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
     *
     * @param mediabox
     * @return
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
    public final List<SpPdfPageProps>
            getPageSizeChunks(final String filePathPdf)
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

        } catch (com.lowagie.text.exceptions.BadPasswordException e) {
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
    public final SpPdfPageProps getPageProps(final String filePathPdf)
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

        } catch (com.lowagie.text.exceptions.BadPasswordException e) {
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
        pdfFileCopy = pdfFile + ".tmp";
        try {
            myDocument = new Document();
            myCopy = new PdfCopy(myDocument, new FileOutputStream(pdfFileCopy));
            myDocument.open();
        } catch (Exception e) {
            throw new SpException(e);
        }
    }

    @Override
    protected void onExit() throws Exception {
    }

    @Override
    protected void onInitJob(String jobPfdName, final String rotate)
            throws Exception {
        myReader = new PdfReader(jobPfdName);
        myRotate = rotate;
        myJobRanges = "";
    }

    @Override
    protected void onProcessJobPages(int nPageFrom, int nPageTo,
            boolean removeGraphics) throws Exception {
        if (!myJobRanges.isEmpty()) {
            myJobRanges += ",";
        }
        myJobRanges += String.valueOf(nPageFrom);
        if (nPageFrom != nPageTo) {
            myJobRanges += "-" + String.valueOf(nPageTo);
        }
        myRemoveGraphics = removeGraphics;
    }

    @Override
    protected void onExitJob() throws Exception {

        myReader.selectPages(myJobRanges);
        int pages = myReader.getNumberOfPages();

        for (int i = 0; i < pages;) {

            ++i;

            /*
             * Rotate for BOTH export and printing.
             */
            if (myRotate != null) {
                final int rotate = Integer.valueOf(myRotate);
                if (rotate != 0) {
                    PdfDictionary pageDict = myReader.getPageN(i);
                    pageDict.put(PdfName.ROTATE, new PdfNumber(rotate));
                }
            }

            PdfImportedPage page = myCopy.getImportedPage(myReader, i);
            myCopy.addPage(page);
        }
        myReader.close();
        myReader = null;
    }

    @Override
    protected void onExitJobs() throws Exception {
        myDocument.close();
        myDocument = null;
    }

    @Override
    protected void onStampLetterhead(String pdfLetterhead) throws Exception {

    }

    @Override
    protected void onCompress() throws Exception {
        /*
         * iText uses compression by default, but you can set full compression,
         * which make it PDF 1.5 version.
         */
        myStamper.setFullCompression();
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
            myStamper.setEncryption(bStrength, userPass, ownerPass,
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
            myStamper.setEncryption(bStrength, null, null, iPermissions);
        } catch (DocumentException e) {
            throw new SpException(e);
        }
    }

    @Override
    protected void onProcessFinally() {

        if (myDocument != null) {
            myDocument.close();
            myDocument = null;
        }
        if (myReader != null) {
            myReader.close();
            myReader = null;
        }

        if (myStamper != null) {
            try {
                myStamper.close();
            } catch (Exception e) {
                // TODO
            } finally {
                myStamper = null;
            }
        }

        new File(pdfFileCopy).delete();

    }

    /**
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    protected void
            onStampMetaDataForExport(Calendar now, PdfProperties propPdf) {

        java.util.HashMap info = myReader.getInfo();

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

        info.put("Creator", CommunityDictEnum.SAVAPAGE.getWord() + " "
                + ConfigManager.getAppVersion());
        info.put("Producer", CommunityDictEnum.SAVAPAGE.getWord() + " "
                + ConfigManager.getAppVersion());

        if (propPdf.getApply().getKeywords()) {
            info.put("Keywords", propPdf.getDesc().getKeywords());
        }

        myStamper.setMoreInfo(info);

    }

    @SuppressWarnings("unchecked")
    // @Override
            /**
             * UNDER CONSTRUCTION - Mantis #113 - NOT working, why ??
             *
             * @param now
             * @param propPdf
             */
            protected
            void onStampMetaDataForExportTEST(Calendar now,
                    PdfProperties propPdf) {

        /*
         * Mantis #113
         */
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        XmpWriter xmp = null;

        try {

            xmp = new XmpWriter(os);

            XmpSchema dc = new DublinCoreSchema();

            /*
             * Title
             */
            XmpArray title = new XmpArray(XmpArray.UNORDERED);
            title.add(propPdf.getDesc().getSubject());
            title.add("XMP");
            title.add("XMP & Metadata");
            dc.setProperty(DublinCore.TITLE.toString(), title);

            /*
             * Subject
             */
            if (propPdf.getApply().getSubject()) {
                XmpArray subject = new XmpArray(XmpArray.UNORDERED);
                subject.add(propPdf.getDesc().getSubject());
                subject.add("XMP & Metadata");
                subject.add("Metadata");
                dc.setProperty(DublinCore.SUBJECT.toString(), subject);
            }

            /*
             * Creator
             */
            XmpArray creator = new XmpArray(XmpArray.UNORDERED);
            creator.add(CommunityDictEnum.SAVAPAGE.getWord() + " "
                    + ConfigManager.getAppVersion());
            creator.add("XMP & Metadata");
            creator.add("Metadata");
            // dc.setProperty(DublinCore.CREATOR, creator);

            /*
             *
             */
            xmp.addRdfDescription(dc);

            /*
             * Author ?
             */

            // info.put("Author", propPdf.getDesc().getAuthor());

            PdfSchema pdf = new PdfSchema();

            pdf.setProperty(PdfSchema.VERSION, "1.4");

            pdf.setProperty(
                    PdfSchema.PRODUCER,
                    CommunityDictEnum.SAVAPAGE.getWord() + " "
                            + ConfigManager.getAppVersion());

            if (propPdf.getApply().getKeywords()) {
                pdf.setProperty(PdfSchema.KEYWORDS, propPdf.getDesc()
                        .getKeywords());
            }

            xmp.addRdfDescription(pdf);

            /*
             *
             */
            xmp.close();
            myStamper.setXmpMetadata(os.toByteArray());
            xmp = null;

        } catch (IOException e) {
            throw new SpException(e);
        } finally {
            if (xmp != null) {
                try {
                    xmp.close();
                } catch (IOException e) {
                    // no code intended
                }
            }
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    protected void onStampMetaDataForPrinting(Calendar now,
            PdfProperties propPdf) {

        java.util.HashMap info = myReader.getInfo();

        info.put("Title", propPdf.getDesc().getTitle());
        info.put("Subject", "FOR PRINTING PURPOSES ONLY");
        info.put("Author", CommunityDictEnum.SAVAPAGE.getWord());

        info.put("Creator", CommunityDictEnum.SAVAPAGE.getWord() + " "
                + ConfigManager.getAppVersion());
        info.put("Producer", CommunityDictEnum.SAVAPAGE.getWord() + " "
                + ConfigManager.getAppVersion());

        // info.setModificationDate(now);
        if (propPdf.getApply().getKeywords()) {
            info.put("Keywords", propPdf.getDesc().getKeywords());
        }

        myStamper.setMoreInfo(info);
    }

    @Override
    protected void onStampRotateForPrinting() throws Exception {
        /*
         * No code intended: rotation is done in exitStamping.
         */
    }

    @Override
    protected void onInitStamp() throws Exception {
        myReader = new PdfReader(pdfFileCopy);
        myStamper = new PdfStamper(myReader, new FileOutputStream(pdfFile));
    }

    @Override
    protected void onExitStamp() throws Exception {

        if (myRemoveGraphics) {
            minifyPdfImages();
        }

        final boolean isLetterhead = myPdfFileLetterhead != null;

        if (!(myForPrinting || isLetterhead)) {
            return;
        }

        PdfReader letterheadReader = null;

        try {

            int nLetterheadPage = 0;
            int nLetterheadPageMax = 1;

            Rectangle lhPageSize = null;
            PdfImportedPage letterheadPage = null;

            if (isLetterhead) {
                letterheadReader = new PdfReader(myPdfFileLetterhead);
                nLetterheadPageMax = myLetterheadJob.getPages();
            }

            /*
             * Iterate over document's pages.
             */
            int nDocPages = myReader.getNumberOfPages();

            for (int i = 0; i < nDocPages;) {

                ++i;

                /*
                 * Check if we rotated this page to show it in landscape
                 * orientation.
                 */
                final int pageRotation = myReader.getPageRotation(i);
                final boolean isPageRotated = pageRotation != 0;

                /*
                 * Gets the page size without taking rotation into account.
                 */
                Rectangle docPageSize = myReader.getPageSize(i);

                /*
                 * Virtual page width and height used for calculating the
                 * letterhead translate an rotate.
                 */
                float virtualPageWidth;
                float virtualPageHeight;

                if (isPageRotated) {
                    /*
                     * Since this is a landscape page in portrait orientation:
                     * we swap height and width for calculation of the translate
                     * and rotate of the letterhead.
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
                if (myForPrinting) {

                    rotateLetterhead =
                            isPageRotated
                                    || (virtualPageHeight < virtualPageWidth);

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

                        PdfDictionary pageDict = myReader.getPageN(i);
                        pageDict.put(PdfName.ROTATE, new PdfNumber(rotate));
                    }
                }

                /*
                 * Apply (rotated) letterhead
                 */
                if (isLetterhead) {

                    /*
                     * If the letterhead document has more than one page, each
                     * page of the letterhead is applied to the corresponding
                     * page of the output document. If the output document has
                     * more pages than the letterhead, then the final letterhead
                     * page is repeated across these remaining pages of the
                     * output document.
                     */
                    if (nLetterheadPage < nLetterheadPageMax) {

                        nLetterheadPage++;
                        lhPageSize =
                                letterheadReader.getPageSize(nLetterheadPage);

                        /*
                         * Create a PdfTemplate from the nth page of the
                         * letterhead (PdfImportedPage is derived from
                         * PdfTemplate).
                         */
                        letterheadPage =
                                myStamper.getImportedPage(letterheadReader,
                                        nLetterheadPage);
                    }

                    /*
                     * Scale letterhead page and move it so that it fits within
                     * the document's page (if document's page is cropped, this
                     * scale might not be small enough).
                     */

                    float hScale = virtualPageWidth / lhPageSize.getWidth();

                    float vScale = virtualPageHeight / lhPageSize.getHeight();

                    /*
                     * Proportional scaling: pick the smallest scaling factor.
                     */
                    float sX = (hScale < vScale) ? hScale : vScale;
                    float sY = sX;

                    float tX =
                            (float) ((virtualPageWidth - lhPageSize.getWidth()
                                    * sX) / 2.0);

                    float tY =
                            (float) ((virtualPageHeight - lhPageSize
                                    .getHeight() * sY) / 2.0);

                    /*
                     * Add letterhead page as a layer ...
                     */
                    PdfContentByte contentByte = null;

                    if (myLetterheadJob.getForeground()) {
                        /*
                         * ... 'on top of' the page content.
                         */
                        contentByte = myStamper.getOverContent(i);
                    } else {
                        /*
                         * ... 'underneath' the page content.
                         */
                        contentByte = myStamper.getUnderContent(i);
                    }

                    if (rotateLetterhead) {

                        /*
                         * There is one serious caveat when you rotate an
                         * object: the coordinate of the rotation pivot is (0,
                         * 0).
                         *
                         * If you rotate something, you have to watch out that
                         * it is not rotated 'off' your page.
                         *
                         * You may have to perform a translation to keep the
                         * object on the page. Of course you can combine
                         * translation (tX, tY), scaling (sX, sY) rotation
                         * (angle) in one matrix:
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
        } finally {
            if (letterheadReader != null) {
                letterheadReader.close();
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

        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        img.setRGB(0, 0, Color.WHITE.getRGB());

        java.awt.Image awtImg =
                java.awt.Toolkit.getDefaultToolkit().createImage(
                        img.getSource());

        return com.lowagie.text.Image.getInstance(awtImg, null);
    }

    /**
     *
     * @throws PdfException
     * @throws DocumentException
     * @throws IOException
     */
    private void minifyPdfImages() throws PdfException, DocumentException,
            IOException {

        PdfReader pdf = myReader;
        int n = pdf.getNumberOfPages();

        PdfStamper stp = myStamper;

        final Image imgPixel = createOnePixel();

        for (int j = 0; j < n; j++) {

            PdfWriter writer = stp.getWriter();

            PdfDictionary pg = pdf.getPageN(j + 1);

            PdfDictionary res =
                    (PdfDictionary) PdfReader.getPdfObject(pg
                            .get(PdfName.RESOURCES));

            PdfDictionary xobj =
                    (PdfDictionary) PdfReader.getPdfObject(res
                            .get(PdfName.XOBJECT));

            if (xobj == null) {
                continue;
            }

            for (Iterator it = xobj.getKeys().iterator(); it.hasNext();) {

                PdfObject obj = xobj.get((PdfName) it.next());

                if (obj.isIndirect()) {

                    PdfDictionary tg =
                            (PdfDictionary) PdfReader.getPdfObject(obj);

                    if (tg != null) {

                        PdfName type =
                                (PdfName) PdfReader.getPdfObject(tg
                                        .get(PdfName.SUBTYPE));

                        if (PdfName.IMAGE.equals(type)) {

                            PdfReader.killIndirect(obj);
                            Image maskImage = imgPixel.getImageMask();

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

}
