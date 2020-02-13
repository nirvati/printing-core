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
import org.savapage.core.util.NumberUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
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

    /**
     * Rotation of a landscape PDF image in a portrait PDF document.
     */
    private static final float PDF_IMG_LANDSCAPE_ROTATE =
            (float) (Math.PI * .5);

    @Override
    public long convert(final DocContentTypeEnum contentType,
            final DocInputStream istrDoc, final OutputStream ostrPdf)
            throws Exception {

        toPdf(contentType, istrDoc, ostrPdf);
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

        final float marginLeft = 50;
        final float marginRight = 50;
        final float marginTop = 50;
        final float marginBottom = 50;

        Document document = null;

        try {
            document = new Document(ITextPdfCreator.getDefaultPageSize(),
                    marginLeft, marginRight, marginTop, marginBottom);

            PdfWriter.getInstance(document, ostrPdf);
            document.open();

            com.itextpdf.text.Image image;

            switch (contentType) {

            case BMP:
            case JPEG:
            case PNG:
                final java.awt.Image awtImage = ImageIO.read(istrImage);
                image = com.itextpdf.text.Image.getInstance(awtImage, null);
                addImagePage(document, marginLeft, marginRight, image);
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
                    addImagePage(document, marginLeft, marginRight, image);
                }
                break;

            case TIFF:

                final RandomAccessSourceFactory raFactory =
                        new RandomAccessSourceFactory();

                final RandomAccessFileOrArray ra = new RandomAccessFileOrArray(
                        raFactory.createSource(istrImage));

                final int pages = TiffImage.getNumberOfPages(ra);

                for (int i = 0; i < pages; i++) {

                    if (i > 0) {
                        // Every page on a new PDF page.
                        document.newPage();
                    }
                    // One-based index.
                    image = TiffImage.getTiffImage(ra, i + 1);
                    addImagePage(document, marginLeft, marginRight, image);
                }
                break;

            case WMF:
                // UNDER CONSTRUCTION
                final ImgWMF wmf = new ImgWMF(IOUtils.toByteArray(istrImage));
                addImagePage(document, marginLeft, marginRight, wmf);
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
     * @param image
     *            {@link com.lowagie.text.Image}.
     * @return {@code true} if image has landscape orientation.
     */
    private static boolean isLandscapeImg(final com.lowagie.text.Image image) {
        return image.getWidth() > image.getHeight();
    }

    /**
     * @param image
     *            {@link com.itextpdf.text.Image}.
     * @return {@code true} if image has landscape orientation.
     */
    private static boolean isLandscapeImg(final com.itextpdf.text.Image image) {
        return image.getWidth() > image.getHeight();
    }

    /**
     * @param document
     *            {@link com.lowagie.text.Document}.
     * @return {@code true} if document has landscape orientation.
     */
    private static boolean
            isLandscapePdf(final com.lowagie.text.Document document) {
        return document.getPageSize().getWidth() > document.getPageSize()
                .getHeight();
    }

    /**
     * @param document
     *            {@link com.itextpdf.text.Document}.
     * @return {@code true} if document has landscape orientation.
     */
    private static boolean
            isLandscapePdf(final com.itextpdf.text.Document document) {
        return document.getPageSize().getWidth() > document.getPageSize()
                .getHeight();
    }

    /**
     * Calculates scaling percentage of PDF image to fit on PDF page.
     *
     * @param pageWidth
     *            PDF page width.
     * @param imageWidth
     *            Image width.
     * @param marginLeft
     *            Left margin on PDF page.
     * @param marginRight
     *            Right margin on PDF page.
     * @return Scaling percentage. {@code 1.0f} if no scaling.
     */
    private static float calcAddImageScalePerc(final float pageWidth,
            final float imageWidth, final float marginLeft,
            final float marginRight) {

        final float pageWidthEffective = pageWidth - marginLeft - marginRight;
        final float scalePerc;

        if (imageWidth > pageWidthEffective) {
            scalePerc =
                    NumberUtil.INT_HUNDRED * (pageWidthEffective / imageWidth);
        } else {
            scalePerc = 1.0f;
        }
        return scalePerc;
    }

    /**
     * Adds an image to the current a page of an PDF
     * {@link com.lowagie.text.Document}: Mozilla Public License.
     * <ul>
     * <li>A landscape image is rotated when PDF Document page is portrait.</li>
     * <li>The image is scaled to the document width.</li>
     * </ul>
     *
     * @param document
     *            a PDF {@link com.lowagie.text.Document} to add the image to.
     * @param marginLeft
     *            Left margin.
     * @param marginRight
     *            Right margin.
     * @param image
     *            The {@link com.lowagie.text.Image} to add
     * @throws com.lowagie.text.DocumentException
     *             When things go wrong.
     */
    public static void addImagePage(final com.lowagie.text.Document document,
            final float marginLeft, final float marginRight,
            final com.lowagie.text.Image image)
            throws com.lowagie.text.DocumentException {

        if (isLandscapeImg(image) && !isLandscapePdf(document)) {
            image.setRotation(PDF_IMG_LANDSCAPE_ROTATE);
        }

        image.scalePercent(
                calcAddImageScalePerc(document.getPageSize().getWidth(),
                        image.getWidth(), marginLeft, marginRight));
        /*
         * A larger image that does not fit into current page's remaining space
         * will be inserted as instructed, but will insert into next page
         * instead of current page.
         */
        document.add(image);
    }

    /**
     * Adds an image to the current a page of an PDF
     * {@link com.itextpdf.text.Document}: AGPL license.
     * <ul>
     * <li>A landscape image is rotated when PDF Document page is portrait.</li>
     * <li>The image is scaled to the document width.</li>
     * </ul>
     *
     * @param document
     *            a PDF {@link com.itextpdf.text.Document} to add the image to.
     * @param marginLeft
     *            Left margin.
     * @param marginRight
     *            Right margin.
     * @param image
     *            The {@link com.itextpdf.text.Image} to add
     * @throws DocumentException
     *             When things go wrong.
     */
    public static void addImagePage(final com.itextpdf.text.Document document,
            final float marginLeft, final float marginRight,
            final com.itextpdf.text.Image image) throws DocumentException {

        if (isLandscapeImg(image) && !isLandscapePdf(document)) {
            image.setRotation(PDF_IMG_LANDSCAPE_ROTATE);
        }

        image.scalePercent(
                calcAddImageScalePerc(document.getPageSize().getWidth(),
                        image.getWidth(), marginLeft, marginRight));
        /*
         * A larger image that does not fit into current page's remaining space
         * will be inserted as instructed, but will insert into next page
         * instead of current page.
         */
        document.add(image);
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
