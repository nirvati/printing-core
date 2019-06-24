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
import java.util.Hashtable;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class QRCodeHelper {

    /**
     * Utility class.
     */
    private QRCodeHelper() {
    }

    /**
     * Creates a QR code image.
     *
     * @param codeText
     *            QR text.
     * @param squareWidth
     *            Width and height in pixels.
     * @param quietZone
     *            quietZone, in pixels. Use {@code null} for default zone.
     * @throws QRCodeException
     *             If error.
     * @return {@link BufferedImage}.
     */
    public static BufferedImage createImage(final String codeText,
            final int squareWidth, final Integer quietZone)
            throws QRCodeException {

        final Hashtable<EncodeHintType, Object> hintMap = new Hashtable<>();

        hintMap.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);
        if (quietZone != null) {
            hintMap.put(EncodeHintType.MARGIN, quietZone);
        }

        final QRCodeWriter qrCodeWriter = new QRCodeWriter();

        BitMatrix byteMatrix;
        try {
            byteMatrix = qrCodeWriter.encode(codeText, BarcodeFormat.QR_CODE,
                    squareWidth, squareWidth, hintMap);
        } catch (WriterException e) {
            throw new QRCodeException(e.getMessage());
        }

        final int byteMatrixWidth = byteMatrix.getWidth();

        final BufferedImage image = new BufferedImage(byteMatrixWidth,
                byteMatrixWidth, BufferedImage.TYPE_INT_RGB);

        image.createGraphics();

        final Graphics2D graphics = (Graphics2D) image.getGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, byteMatrixWidth, byteMatrixWidth);
        graphics.setColor(Color.BLACK);

        for (int i = 0; i < byteMatrixWidth; i++) {
            for (int j = 0; j < byteMatrixWidth; j++) {
                if (byteMatrix.get(i, j)) {
                    graphics.fillRect(i, j, 1, 1);
                }
            }
        }
        return image;
    }
}
