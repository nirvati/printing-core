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
package org.savapage.core.doc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.imageio.ImageIO;

import org.apache.commons.io.IOUtils;
import org.savapage.core.SpException;
import org.savapage.core.pdf.ITextCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.ImgWMF;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.RandomAccessFileOrArray;
import com.lowagie.text.pdf.codec.GifImage;
import com.lowagie.text.pdf.codec.TiffImage;

/**
 *
 * @author Datraverse B.V.
 */
public class ImageToPdf implements IStreamConverter {

    /**
     *
     */
    private static final Logger LOGGER = LoggerFactory
            .getLogger(ImageToPdf.class);

    @Override
    public final long convert(final DocContentTypeEnum contentType,
            final DocInputStream istrDoc, final OutputStream ostrPdf)
            throws Exception {

        toPdf(contentType, istrDoc, ostrPdf);
        return istrDoc.getBytesRead();
    }

    /**
     * Creates a PDF file from a standard image type such as BMP, EPS, GIF,
     * JPEG/JPG, PNG, TIFF and WMF.
     * <p>
     * See <a
     * href="http://www.geek-tutorials.com/java/itext/itext_image.php">this
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
            document =
                    new Document(ITextCreator.getDefaultPageSize(), marginLeft,
                            marginRight, marginTop, marginBottom);

            PdfWriter.getInstance(document, ostrPdf);
            document.open();

            com.lowagie.text.Image image;

            switch (contentType) {

            case BMP:
            case JPEG:
            case PNG:
                java.awt.Image awtImage = ImageIO.read(istrImage);
                image = com.lowagie.text.Image.getInstance(awtImage, null);
                addImagePage(document, marginLeft, marginRight, image);
                break;

            case GIF:
                GifImage img = new GifImage(istrImage);
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
                RandomAccessFileOrArray ra =
                        new RandomAccessFileOrArray(istrImage);
                int pages = TiffImage.getNumberOfPages(ra);
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
                ImgWMF wmf = new ImgWMF(IOUtils.toByteArray(istrImage));
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
     *
     * @param document
     * @param marginLeft
     * @param marginRight
     * @param image
     * @throws DocumentException
     */
    private void addImagePage(final Document document, final float marginLeft,
            final float marginRight, final com.lowagie.text.Image image)
            throws DocumentException {

        final boolean landscape = image.getWidth() > image.getHeight();

        if (landscape) {
            image.setRotation((float) (Math.PI * .5));
        }
        /*
         * Now the larger image that did not fit into current page's remaining
         * space will always be inserted as coding instructed, but it will
         * insert into next page instead of current page.
         */

        final Rectangle pageRect = document.getPageSize();
        final float pageWidthEffective =
                pageRect.getWidth() - marginLeft - marginRight;

        float scalePerc = 1.0f;

        if (image.getWidth() > pageWidthEffective) {
            scalePerc = 100 * (pageWidthEffective / image.getWidth());
            image.scalePercent(scalePerc);
        }
        document.add(image);
    }

    /**
     * Writes the inputStream to a File (for debug purposes).
     *
     * @param inputStream
     * @param file
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
