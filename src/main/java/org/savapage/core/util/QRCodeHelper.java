/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2019 Datraverse B.V.
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
package org.savapage.core.util;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Hashtable;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.lowagie.text.BadElementException;
import com.lowagie.text.Image;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class QRCodeHelper {

    /**
     * PDF points per inch.
     */
    private static final float PDF_POINTS_PER_INCH = 72;

    /**
     * PDF image DPI. 72 points == 96 pixels.
     */
    private static final float PDF_DPI = 96;

    /**
     * Pixels in PDF point.
     */
    private static final float PDF_POINT_TO_IMG_PIXEL =
            PDF_DPI / PDF_POINTS_PER_INCH;

    /**
     * PDF points in pixel.
     */
    private static final float IMG_PIXEL_TO_PDF_POINT =
            PDF_POINTS_PER_INCH / PDF_DPI;

    /**
     * Millimeters per inch.
     */
    private static final float MM_PER_INCH = 25.4f;

    /**
     * Inches per millimeter.
     */
    private static final float INCH_PER_MM = 1.0f / MM_PER_INCH;

    /**
     * @param mm
     *            Millimeters.
     * @return Inches of millimeters.
     */
    public static float mmToInch(final int mm) {
        return mm * INCH_PER_MM;
    }

    /**
     * @param px
     *            Pixels.
     * @return PDF Points of pixels.
     */
    public static float pdfPXToPoints(final int px) {
        return px * IMG_PIXEL_TO_PDF_POINT;
    }

    /**
     * @param mm
     *            Millimeters.
     * @return Pixels of millimeters.
     */
    public static float pdfMMToPX(final int mm) {
        return mmToInch(mm) * PDF_POINTS_PER_INCH * PDF_POINT_TO_IMG_PIXEL;
    }

    /**
     * @param mm
     *            Millimeters.
     * @return Points of millimeters.
     */
    public static float pdfMMToPoints(final int mm) {
        return pdfMMToPX(mm) * IMG_PIXEL_TO_PDF_POINT;
    }

    /**
     * @param px
     *            Pixels.
     * @return Millimeters of pixels.
     */
    public static float pdfPXToMM(final int px) {
        return (MM_PER_INCH * pdfPXToPoints(px)) / PDF_POINTS_PER_INCH;
    }

    /**
     * Utility class.
     */
    private QRCodeHelper() {
    }

    /**
     * Creates a scaled PDF background image with fill color.
     *
     * @param mmWidth
     *            Width in millimeters.
     * @param color
     *            The fill color.
     * @throws QRCodeException
     *             If error.
     * @return PDF image.
     */
    public static com.lowagie.text.Image createPdfImageBackground(
            final int mmWidth, final Color color) throws QRCodeException {

        final int pxWidth = (int) pdfMMToPX(mmWidth);

        final BufferedImage image =
                new BufferedImage(pxWidth, pxWidth, BufferedImage.TYPE_INT_RGB);
        final Graphics2D g2d = image.createGraphics();
        g2d.setPaint(color);
        g2d.fillRect(0, 0, image.getWidth(), image.getHeight());
        g2d.dispose();

        return createPdfImage(image, pdfMMToPoints(mmWidth));
    }

    /**
     * Creates a scaled plain QR code PDF image without margins or quiet zone.
     *
     * @param qrCode
     *            QR text.
     * @param mmWidth
     *            Width in millimeters.
     * @throws QRCodeException
     *             If error.
     * @return PDF image.
     */
    public static com.lowagie.text.Image createPdfImage(final String qrCode,
            final int mmWidth) throws QRCodeException {

        final int qrcodeWidthPx = (int) pdfMMToPX(mmWidth);
        final int qrDots = numberOfQRDots(qrCode);
        /*
         * (1) Create QR code, using width of largest multiple of logical dots,
         * so no margins and quiet zone are generated.
         */
        final int squareWidth = qrcodeWidthPx - qrcodeWidthPx % qrDots;
        /*
         * (2) Scale to PDF image to the requested mmWidth.
         */
        return createPdfImage(createImage(qrCode, squareWidth, 0),
                pdfMMToPoints(mmWidth));
    }

    /**
     * Creates a scaled to widthPoints PDF image from a {@link BufferedImage}.
     *
     * @param image
     *            Image.
     * @param widthPoints
     *            PDF width points.
     * @return Scaled PDF image.
     * @throws QRCodeException
     *             If error.
     */
    private static com.lowagie.text.Image
            createPdfImage(final BufferedImage image, final float widthPoints)
                    throws QRCodeException {
        try {
            final Image codeQRImage = Image.getInstance(image, null);
            codeQRImage.scaleToFit(widthPoints, widthPoints);
            return codeQRImage;
        } catch (BadElementException | IOException e) {
            throw new QRCodeException(e.getMessage());
        }
    }

    /**
     * Creates a black and white QR code image.
     * <ul>
     * <li>The image consists of "black squares" arranged in a square grid on a
     * white background.</li>
     * <li>The more extensive the encrypted data, the more squares are needed on
     * a single row.</li>
     * <li>The number of pixels used to paint one (1) square depends on the
     * requested width of the QR image.</li>
     * <li>If an image width of 31px is requested to encode data that need 29
     * squares on a row, each square will be 1x1px, and the image will have a
     * 1px empty margin at its borders.</li>
     * <li>If a 100x100px image is requested for the same data, each square will
     * be 3x3px, and T,R,L,B margins will be 6,6,7,7.</li>
     * <li>If a 28x28px image is requested (or anything less than 29x29px) for
     * the same data, each square will be the minimum 1x1px, and the image will
     * end up being 29x29px without border margins.</li>
     * <li>The quiet zone will add extra border margins.</li>
     * </ul>
     *
     * @param codeText
     *            QR text.
     * @param squareWidth
     *            Width and height in pixels of the resulting image.
     * @param quietZone
     *            Quiet Zone, in pixels. Use {@code null} for default zone
     *            (2px).
     * @throws QRCodeException
     *             If error.
     * @return {@link BufferedImage}. The size of the image might be greater
     *         than requested (see remarks above).
     */
    public static BufferedImage createImage(final String codeText,
            final int squareWidth, final Integer quietZone)
            throws QRCodeException {

        final Hashtable<EncodeHintType, Object> hintMap = new Hashtable<>();

        hintMap.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);
        if (quietZone != null) {
            hintMap.put(EncodeHintType.MARGIN, quietZone);
        }

        final BitMatrix bitMatrix =
                getQRBitMatrix(codeText, squareWidth, quietZone);
        final int bitMatrixWidth = bitMatrix.getWidth();

        final BufferedImage image = new BufferedImage(bitMatrixWidth,
                bitMatrixWidth, BufferedImage.TYPE_INT_RGB);

        image.createGraphics();

        final Graphics2D graphics = (Graphics2D) image.getGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, bitMatrixWidth, bitMatrixWidth);
        graphics.setColor(Color.BLACK);

        for (int i = 0; i < bitMatrixWidth; i++) {
            for (int j = 0; j < bitMatrixWidth; j++) {
                if (bitMatrix.get(i, j)) {
                    graphics.fillRect(i, j, 1, 1);
                }
            }
        }
        return image;
    }

    /**
     * Gets {@link BitMatrix} representing encoded QR code image.
     *
     * @param codeText
     *            QR text.
     * @param squareWidth
     *            Width and height in pixels.
     * @param quietZone
     *            quietZone, in pixels. Use {@code null} for default zone.
     * @throws QRCodeException
     *             If error.
     * @return {@link BitMatrix}.
     */
    public static BitMatrix getQRBitMatrix(final String codeText,
            final int squareWidth, final Integer quietZone)
            throws QRCodeException {

        final Hashtable<EncodeHintType, Object> hintMap = new Hashtable<>();

        hintMap.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);
        if (quietZone != null) {
            hintMap.put(EncodeHintType.MARGIN, quietZone);
        }

        final QRCodeWriter qrCodeWriter = new QRCodeWriter();

        try {
            return qrCodeWriter.encode(codeText, BarcodeFormat.QR_CODE,
                    squareWidth, squareWidth, hintMap);
        } catch (WriterException e) {
            throw new QRCodeException(e.getMessage());
        }
    }

    /**
     * Gets number of logical dots per QR code row.
     * <p>
     * <i>Width one (1) and quiet zone zero (0) is used to enforce minimal
     * qrCode {@link BitMatrix}.</i>
     * </p>
     *
     * @param qrCode
     *            QR code text.
     * @throws QRCodeException
     *             If error.
     * @return Number of dots per QR code row.
     */
    public static int numberOfQRDots(final String qrCode)
            throws QRCodeException {
        return getQRBitMatrix(qrCode, 1, 0).getWidth();
    }

}
