/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2020 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: 2011-2020 Datraverse B.V. <info@datraverse.com>
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
package org.savapage.core.doc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.imageio.ImageIO;

import org.apache.commons.io.IOUtils;
import org.savapage.core.SpException;
import org.savapage.core.pdf.ITextPdfCreator;
import org.savapage.core.pdf.facade.PdfDocumentAGPL;
import org.savapage.core.pdf.facade.PdfDocumentFacade;
import org.savapage.core.pdf.facade.PdfImageFacade;
import org.savapage.core.util.NumberUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.itextpdf.text.Document;
import com.itextpdf.text.ImgWMF;
import com.itextpdf.text.io.RandomAccessSourceFactory;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.RandomAccessFileOrArray;
import com.itextpdf.text.pdf.codec.GifImage;
import com.itextpdf.text.pdf.codec.TiffImage;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class ImageToPdf implements IStreamConverter {

    /** */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(ImageToPdf.class);

    /** */
    private static final float PDF_MARGIN_LEFT = 50;
    /** */
    private static final float PDF_MARGIN_RIGHT = 50;
    /** */
    private static final float PDF_MARGIN_TOP = 50;
    /** */
    private static final float PDF_MARGIN_BOTTOM = 50;

    /**
     * Rotation of a landscape PDF image in a portrait PDF document.
     */
    private static final float PDF_IMG_LANDSCAPE_ROTATE =
            (float) (Math.PI * .5);

    /** */
    private static final boolean LANDSCAPE_ROTATE_OF_IMAGE = false;
    /** */
    private static final boolean LANDSCAPE_ROTATE_OF_PAGE =
            !LANDSCAPE_ROTATE_OF_IMAGE;

    @Override
    public long convert(final DocContentTypeEnum contentType,
            final DocInputStream istrDoc, final OutputStream ostrPdf)
            throws Exception {

        this.toPdf(contentType, istrDoc, ostrPdf);
        return istrDoc.getBytesRead();
    }

    /**
     * Creates a PDF file from a standard image type such as BMP, EPS, GIF,
     * JPEG/JPG, PNG, TIFF and WMF.
     * <p>
     * See
     * <a href="http://www.geek-tutorials.com/java/itext/itext_image.php">this
     * tutorial</a>.
     * </p>
     *
     * @param contentType
     *            The content type of the input stream.
     * @param istrImage
     *            The input stream with the image.
     * @param ostrPdf
     *            The output stream for the generated PDF.
     * @throws Exception
     *             If error.
     */
    private void toPdf(final DocContentTypeEnum contentType,
            final InputStream istrImage, final OutputStream ostrPdf)
            throws Exception {

        Document document = null;

        try {
            document = new Document(ITextPdfCreator.getDefaultPageSize(),
                    PDF_MARGIN_LEFT, PDF_MARGIN_RIGHT, PDF_MARGIN_TOP,
                    PDF_MARGIN_BOTTOM);

            PdfWriter.getInstance(document, ostrPdf);

            final PdfDocumentAGPL documentFacade =
                    new PdfDocumentAGPL(document);

            /*
             * At this point do NOT perform "document.open()" : it is important
             * to wait after the page size (and margins) are set. The first page
             * is initialized when you open() the document, all following pages
             * are initialized when a newPage() occurs.
             */

            com.itextpdf.text.Image image;

            switch (contentType) {

            case BMP:
            case JPEG:
            case PNG:
                final java.awt.Image awtImage = ImageIO.read(istrImage);
                image = com.itextpdf.text.Image.getInstance(awtImage, null);
                addImagePage(documentFacade, documentFacade.create(image));
                break;

            case GIF:
                final GifImage img = new GifImage(istrImage);
                int frameCount = img.getFrameCount();
                /*
                 * For animated GIF extract every frames of it and display
                 * series of static images.
                 */
                for (int i = 0; i < frameCount; i++) {
                    // One-based index.
                    image = img.getImage(i + 1);
                    addImagePage(documentFacade, documentFacade.create(image));
                }
                break;

            case TIFF:

                final RandomAccessSourceFactory raFactory =
                        new RandomAccessSourceFactory();

                final RandomAccessFileOrArray ra = new RandomAccessFileOrArray(
                        raFactory.createSource(istrImage));

                final int pages = TiffImage.getNumberOfPages(ra);

                for (int i = 0; i < pages; i++) {
                    // One-based index.
                    image = TiffImage.getTiffImage(ra, i + 1);
                    addImagePage(documentFacade, documentFacade.create(image));
                }
                break;

            case WMF:
                // UNDER CONSTRUCTION
                final ImgWMF wmf = new ImgWMF(IOUtils.toByteArray(istrImage));
                addImagePage(documentFacade, documentFacade.create(wmf));
                break;

            default:
                throw new SpException("[" + contentType + "] is NOT supported");
            }

            /*
             * (see tutorial link above): Important: Note that if you are
             * inserting images of different width and height on a same pages,
             * you may sometimes get unexpected result of the images position
             * and inserted order. The first image to be inserted is not always
             * appear on top of the next image. This is because when the image
             * is too large to be inserted into current page's remaining space,
             * iText will try to insert next smaller image that fit the space
             * first. However, you can turn off this default feature by adding
             * the following code:
             */

            // PdfWriter.getInstance(document,
            // new FileOutputStream("SimpleImages.pdf"));
            // writer.setStrictImageSequence(true);
            // document.open();

        } finally {
            if (document != null && document.isOpen()) {
                document.close();
            }
        }
    }

    /**
     * Calculates scaling percentage of PDF image to fit on PDF page.
     *
     * @param pageWidth
     *            PDF page width.
     * @param pageHeight
     *            PDF page height.
     * @param imageWidth
     *            Image width.
     * @param imageHeight
     *            Image height.
     * @param marginLeft
     *            Left margin on PDF page.
     * @param marginRight
     *            Right margin on PDF page.
     * @return Scaling percentage, or {@code null} if no scaling.
     */
    private static Float calcAddImageScalePerc(final float pageWidth,
            final float pageHeight, final float imageWidth,
            final float imageHeight, final float marginLeft,
            final float marginRight) {

        final float pageWidthEffective = pageWidth - marginLeft - marginRight;
        final Float scalePercW;
        if (imageWidth > pageWidthEffective) {
            scalePercW = Float.valueOf(
                    NumberUtil.INT_HUNDRED * (pageWidthEffective / imageWidth));
        } else {
            scalePercW = null;
        }

        final float pageHeightEffective = pageHeight - marginLeft - marginRight;
        final Float scalePercH;
        if (imageHeight > pageHeightEffective) {
            scalePercH = Float.valueOf(NumberUtil.INT_HUNDRED
                    * (pageHeightEffective / imageHeight));
        } else {
            scalePercH = null;
        }
        final Float scalePerc;
        if (scalePercW != null || scalePercH != null) {
            if (scalePercW == null) {
                scalePerc = scalePercH;
            } else if (scalePercH == null) {
                scalePerc = scalePercW;
            } else if (scalePercH.floatValue() < scalePercW.floatValue()) {
                scalePerc = scalePercH;
            } else {
                scalePerc = scalePercW;
            }
        } else {
            scalePerc = null;
        }

        return scalePerc;
    }

    /**
     * @return {@code true} if landscape rotate is applied to the PDF page,
     *         {@code false} if applied to the image.
     */
    private static boolean isRotatePageToLandscape() {
        return LANDSCAPE_ROTATE_OF_PAGE;
    }

    /**
     * Adds an image to the current a page of a PDF document. The page is
     * rotated for the best image fit and the image is downscaled accordingly.
     * The document is lazy opened or a new page is started.
     *
     * @param document
     *            PDF document to add the image to.
     * @param image
     *            PDF image to add.
     */
    public static void addImagePage(final PdfDocumentFacade document,
            final PdfImageFacade image) {

        final Float[] floats = getImagePageRotateScale(document, image);

        if (floats[0] != null) {
            image.scalePercent(floats[0].floatValue());
        }
        if (floats[1] != null) {
            if (isRotatePageToLandscape()) {
                document.rotatePage();
            } else {
                image.setRotation(floats[1].floatValue());
            }
        }
        /*
         * It is important to change the page size (and margins) before the page
         * is initialized. The first page is initialized when you open() the
         * document, all following pages are initialized when a newPage()
         * occurs.
         */
        if (document.isOpen()) {
            document.newPage();
        } else {
            document.open();
        }
        /*
         * A larger image that does not fit into current page's remaining space
         * will be inserted as instructed, but will insert into next page
         * instead of current page.
         */
        document.add(image);
    }

    /**
     * @param document
     *            PDF document to add the image to.
     * @param image
     *            PDF image to add.
     * @return Float[0] is image scaling, or {@code null} if not applicable.
     *         Float[1] is image rotation, or {@code null} if not applicable.
     */
    private static Float[] getImagePageRotateScale( //
            final PdfDocumentFacade document, final PdfImageFacade image) {

        final float pdfWidth = document.getPageWidth();
        final float pdfHeight = document.getPageHeight();

        final float marginLeft = document.leftMargin();
        final float marginRight = document.rightMargin();

        final float marginTop = document.topMargin();
        final float marginBottom = document.bottomMargin();

        final float imgWidth = image.getWidth();
        final float imgHeight = image.getHeight();

        final Float scalePercentPlain = calcAddImageScalePerc(pdfWidth,
                pdfHeight, imgWidth, imgHeight, marginLeft, marginRight);

        final Float scalePercent;
        final Float rotate;

        if (scalePercentPlain == null) {
            scalePercent = null;
            rotate = null;
        } else {
            final boolean isLandscapePdf = pdfWidth > pdfHeight;
            final boolean isLandscapeImg = imgWidth > imgHeight;

            if (isLandscapeImg && !isLandscapePdf) {

                final Float scalePercentRotate =
                        calcAddImageScalePerc(pdfWidth, pdfHeight, imgHeight,
                                imgWidth, marginBottom, marginTop);

                if (scalePercentRotate == null) {
                    scalePercent = scalePercentPlain;
                    rotate = null;
                } else {
                    rotate = PDF_IMG_LANDSCAPE_ROTATE;
                    scalePercent = scalePercentRotate;
                }
            } else {
                scalePercent = scalePercentPlain;
                rotate = null;
            }
        }
        return new Float[] { scalePercent, rotate };
    }

    /**
     * Writes the inputStream to a File (for debug purposes).
     *
     * @param inputStream
     *            The {@link InputStream}.
     * @param file
     *            The {@link File} to stream to.
     * @throws IOException
     *             When read errors.
     */
    @SuppressWarnings("unused")
    private void streamToFile(final InputStream inputStream, final File file)
            throws IOException {

        OutputStream outputStream = null;

        try {

            outputStream = new FileOutputStream(file);

            int read = 0;
            final byte[] bytes = new byte[1024];

            while ((read = inputStream.read(bytes)) != -1) {
                outputStream.write(bytes, 0, read);
            }

        } finally {
            if (outputStream != null) {
                try {
                    // outputStream.flush();
                    outputStream.close();
                } catch (IOException e) {
                    LOGGER.error(e.getMessage(), e);
                }

            }
        }
    }

}
