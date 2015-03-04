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
package org.savapage.core.img;

/**
 *
 * @author Datraverse B.V.
 *
 */
public class ImageMagickPdfToImgCommand implements PdfToImgCommand {

    /**
     *
     */
    private static final int STRINGBUILDER_CAPACITY = 256;

    @Override
    public final String createCommand(final Integer pageOrdinal,
            final boolean isThumbnail, final String rotate2Apply,
            final String pdfFile, final String imgFile) {

        final StringBuilder command = new StringBuilder(STRINGBUILDER_CAPACITY);

        command.append("convert");

        if (isThumbnail) {

            command.append(" -thumbnail ").append(ImageUrl.THUMBNAIL_WIDTH);

        } else {
            /*
             * http://www.imagemagick.org/script/command-line-options.php#density
             *
             * -density width
             *
             * -density widthxheight
             *
             * Set the horizontal and vertical resolution of an image for
             * rendering to devices.
             *
             * This option specifies the image resolution to store while
             * encoding a raster image or the canvas resolution while rendering
             * (reading) vector formats such as Postscript, PDF, WMF, and SVG
             * into a raster image...
             *
             * The default resolution is 72 dots per inch, which is equivalent
             * to one point per pixel (Macintosh and Postscript standard).
             * Computer screens are normally 72 or 96 dots per inch, while
             * printers typically support 150, 300, 600, or 1200 dots per inch.
             *
             * To determine the resolution of your display, use a ruler to
             * measure the width of your screen in inches, and divide by the
             * number of horizontal pixels (1024 on a 1024x768 display).
             */

            /*
             * -density 150 : OK for Desktop, but sometimes results in dump
             *
             * Ghostscript 9.04: ./base/gxcpath.c(218): Attempt to share (local)
             * segments of clip path 0x7fffff0cacc0!
             *
             * See Mantis #147.
             */

            /*
             * Solving Mantis #147 with trial-and-error: -density 140
             *
             * OK for Desktop + solves Mantis#147
             */
            command.append(" -density 140 ");
        }

        /*
         * Use -flatten option to prevent transparent 'black' background in Mac
         * OS X Leopard.
         */
        command.append(" -flatten");

        /*
         * Use "line" or "plane" to create an interlaced PNG or GIF or
         * progressive JPEG image.
         */
        // command.append(" -interlace line");

        /*
         * Apply rotate?
         */
        if (rotate2Apply != null && !rotate2Apply.equals("0")) {
            command.append(" -rotate ").append(rotate2Apply);
        }

        command.append(" \"").append(pdfFile).append("[").append(pageOrdinal)
                .append("]").append("\" \"").append(imgFile).append("\"");

        return command.toString();
    }

}
