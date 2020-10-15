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
package org.savapage.core.imaging;

import java.awt.Color;
import java.awt.image.BufferedImage;

/**
 * A filter for creating an Eco-friendly image for printing.
 * <p>
 * This filter scans image pixels one by one, horizontally by pixel line from
 * left to right, top down. For each traversed pixel the largest filter square
 * is calculated that exclusively contains filterable pixels with the traversed
 * pixel as top-left corner. This square is colored white (less the border).
 * </p>
 * <p>
 * Since anti-aliasing may be part of the filter border we try to correct things
 * to make sure that sans-serif letters with vertical bars, like "i,l,r", have a
 * sharp contour.
 * </p>
 *
 * @author Rijk Ravestein
 *
 */
public final class EcoImageFilterSquare extends EcoImageFilterMixin {

    /**
     * Shadow matrix to administer the image pixels that were handled in
     * filtered squares. When a cell value is {@code true} the image pixel was
     * processed in a filtered square.
     */
    private boolean[][] filterSquarePixels;

    /**
     * The image width in pixels.
     */
    private int imageWidth;

    /**
     * The image height in pixels.
     */
    private int imageHeight;

    /**
     * The total number of non-white pixels in the image.
     */
    private int totPixelsNonWhite;

    /**
     * The total number of eco filtered pixels in the image.
     */
    private int totPixelsFiltered;

    /**
     * The number of copy ahead max filter squares to imageOut.
     */
    private static final int INITIAL_COPY_AHEAD_MAX_FILTER_SQUARES = 5;

    /**
     * No anti-aliasing check.
     */
    private static final int ANTI_ALIASING_RGB_THRESHOLD_NONE = 0;

    /**
     * Gray.
     */
    // private static final int ANTI_ALIASING_RGB_THRESHOLD_GRAY =
    // 128 * 128 * 128;

    /**
     * A light gray.
     */
    private static final int ANTI_ALIASING_RGB_THRESHOLD_LIGHT =
            218 * 218 * 218;

    /**
     * A lighter gray.
     */
    // private static final int ANTI_ALIASING_RGB_THRESHOLD_LIGHTER =
    // 247 * 247 * 247;

    /**
     *
     */
    private final int antiAliasingRgbThreshold =
            ANTI_ALIASING_RGB_THRESHOLD_LIGHT;

    /**
     * The {@link Parms}.
     */
    private final Parms parms;

    /**
     *
     * @author Rijk Ravestein
     *
     */
    public static final class Parms {

        /**
         * The minimum width of a filter square.
         */
        private int filterSquareWidthMin;

        /**
         * The maximum width of a filter square.
         */
        private int filterSquareWidthMax;

        /**
         * The minimal border width of a filter square.
         */
        private int filterSquareBorderWidthMin = 1;

        /**
         * The fraction of a filter square width used as border.
         */
        private double filterSquareBorderFraction;

        /**
         *
         */
        private boolean convertToGrayscale;

        /**
         *
         * @return The minimum width of a filter square.
         */
        public int getFilterSquareWidthMin() {
            return filterSquareWidthMin;
        }

        /**
         *
         * @param minWidth
         *            The minimum width of a filter square.
         */
        public void setFilterSquareWidthMin(final int minWidth) {
            this.filterSquareWidthMin = minWidth;
        }

        /**
         *
         * @return The maximum width of a filter square.
         */
        public int getFilterSquareWidthMax() {
            return filterSquareWidthMax;
        }

        /**
         *
         * @param maxWidth
         *            The maximum width of a filter square.
         */
        public void setFilterSquareWidthMax(final int maxWidth) {
            this.filterSquareWidthMax = maxWidth;
        }

        /**
         *
         * @return The minimal border width of a filter square.
         */
        public int getFilterSquareBorderWidthMin() {
            return filterSquareBorderWidthMin;
        }

        /**
         *
         * @param minWidth
         *            The minimal border width of a filter square.
         */
        public void setFilterSquareBorderWidthMin(final int minWidth) {
            this.filterSquareBorderWidthMin = minWidth;
        }

        /**
         * @return The fraction of a filter square width used as border.
         */
        public double getFilterSquareBorderFraction() {
            return filterSquareBorderFraction;
        }

        /**
         *
         * @param fraction
         *            The fraction of a filter square width used as border.
         */
        public void setFilterSquareBorderFraction(final double fraction) {
            this.filterSquareBorderFraction = fraction;
        }

        public boolean isConvertToGrayscale() {
            return convertToGrayscale;
        }

        public void setConvertToGrayscale(boolean convertToGrayscale) {
            this.convertToGrayscale = convertToGrayscale;
        }

        /**
         *
         * @return The default {@link Parms}.
         */
        public static Parms createDefault() {

            final Parms parms = new Parms();

            // A value of "3" will even speckle 6pt fonts.
            parms.setFilterSquareWidthMin(3);

            parms.setFilterSquareWidthMax(12);
            parms.setFilterSquareBorderWidthMin(1);
            parms.setFilterSquareBorderFraction(0.25);

            parms.setConvertToGrayscale(false);

            return parms;
        }
    }

    /**
     *
     */
    public EcoImageFilterSquare() {
        this.parms = Parms.createDefault();
    }

    /**
     *
     * @param parms
     *            The {@link Parms}.
     */
    public EcoImageFilterSquare(final Parms parms) {
        this.parms = parms;
    }

    /**
     * Processes a pixel.
     *
     * @param image
     *            The {@link Buffered} to filter.
     * @param x
     *            X-coordinate of pixel.
     * @param y
     *            Y-coordinate of pixel.
     * @return {@code true} when a filter square was applied.
     */
    private boolean process(final BufferedImage image, final int x,
            final int y) {

        int filterSquareWidth = 0;

        boolean search = true;

        /*
         * Find the largest square that exclusively contains filter square
         * candidate pixels, on the diagonal starting at (x,y).
         */
        for (int iX = x, iY = y; search
                && filterSquareWidth <= this.parms.getFilterSquareWidthMax()
                && iX < this.imageWidth
                && iY < this.imageHeight; iX++, iY++, filterSquareWidth++) {

            for (int i = 0; search && i <= filterSquareWidth; i++) {

                if (!this.isFilterSquarePixel(image, x + i, iY)
                        || !this.isFilterSquarePixel(image, iX, y + i)) {
                    filterSquareWidth--;
                    search = false;
                }
            }
        }

        int filterRightX = x + filterSquareWidth;
        int filterBottomY = y + filterSquareWidth;

        if (filterSquareWidth < this.parms.getFilterSquareWidthMin()) {
            return false;
        }

        int filterSquareBorder = Double
                .valueOf(filterSquareWidth
                        * this.parms.getFilterSquareBorderFraction() + 0.5)
                .intValue();

        if (filterSquareBorder < this.parms.getFilterSquareBorderWidthMin()) {
            filterSquareBorder = this.parms.getFilterSquareBorderWidthMin();
            if (filterSquareWidth
                    - 2 * this.parms.getFilterSquareBorderWidthMin() < 1) {
                return false;
            }
        }

        final int filterSquareCenter = filterSquareWidth / 2;

        final int filterSquareCenterX = x + filterSquareCenter;
        final int filterSquareCenterY = y + filterSquareCenter;

        final int rgbCenter =
                image.getRGB(filterSquareCenterX, filterSquareCenterY);

        for (int iY = y; iY < filterBottomY; iY++) {

            final boolean borderHorz = iY < y + filterSquareBorder
                    || iY >= filterBottomY - filterSquareBorder;

            for (int iX = x; iX < filterRightX; iX++) {

                this.filterSquarePixels[iX][iY] = true;

                final boolean borderVert = iX < x + filterSquareBorder
                        || iX >= filterRightX - filterSquareBorder;

                if (borderHorz || borderVert) {
                    /*
                     * Since anti-aliasing may be part of the filter border we
                     * try to correct things to make sure that sans-serif
                     * letters with vertical bars, like "i,l,r", have a sharp
                     * contour.
                     */
                    final int rgbBorder = image.getRGB(iX, iY);

                    if (rgbBorder != rgbCenter) {

                        if (filterSquareBorder == 1) {
                            image.setRGB(iX, iY, rgbCenter);
                        } else if (borderHorz && (iY == y
                                || iY == y + filterSquareWidth - 1)) {
                            // outermost horizontal border pixels: noop.
                        } else if (borderVert && (iX == x
                                || iX == x + filterSquareWidth - 1)) {
                            // outermost vertical border pixels: noop.
                        } else {
                            image.setRGB(iX, iY, rgbCenter);
                        }
                    }

                    continue;
                }

                image.setRGB(iX, iY, RGB_WHITE);

                this.totPixelsFiltered++;
            }
        }

        return true;
    }

    /**
     * Checks whether an image pixel is a candidate for a filter square.
     *
     * @param image
     *            The image to check.
     * @param x
     *            X-coordinate of pixel.
     * @param y
     *            Y-coordinate of pixel.
     * @return {@code true} when pixel is a candidate for a filter square.
     */
    private boolean isFilterSquarePixel(final BufferedImage image, final int x,
            final int y) {

        // Already filtered?
        if (this.filterSquarePixels[x][y]) {
            return false;

        }
        final int rgb = image.getRGB(x, y);

        // Definitely no.
        if (rgb == RGB_WHITE) {
            return false;
        }

        // Definitely yes.
        if (rgb == RGB_BLACK) {
            return true;
        }

        // Maybe, depending on aliasing threshold.
        if (this.antiAliasingRgbThreshold == ANTI_ALIASING_RGB_THRESHOLD_NONE) {
            return true;
        }

        final Color color = new Color(rgb);
        final int antiAliasingIndex =
                color.getRed() * color.getGreen() * color.getBlue();

        return antiAliasingIndex <= this.antiAliasingRgbThreshold;
    }

    /**
     * Copies and optionally converts a pixel to grayscale from input to output
     * image.
     *
     * @param imageIn
     *            The input image.
     * @param imageOut
     *            The output image.
     * @param x
     *            Pixel x-coordinate.
     * @param y
     *            Pixel y-coordinate.
     */
    private void copyPixel(final BufferedImage imageIn,
            final BufferedImage imageOut, int x, int y) {

        Color color;
        int r;
        int g;
        int b;
        int grayPart;

        int rgb = imageIn.getRGB(x, y);

        if (rgb != RGB_WHITE) {
            this.totPixelsNonWhite++;
        }

        if (this.parms.isConvertToGrayscale()) {

            color = new Color(rgb);

            r = color.getRed();
            g = color.getGreen();
            b = color.getBlue();

            if (!(r == b && b == g)) {
                grayPart = (r + g + b) / 3;
                rgb = new Color(grayPart, grayPart, grayPart).getRGB();
            }
        }
        imageOut.setRGB(x, y, rgb);
    }

    @Override
    protected void filter(final BufferedImage imageIn,
            final BufferedImage imageOut) {

        this.totPixelsNonWhite = 0;
        this.totPixelsFiltered = 0;

        this.imageWidth = imageOut.getWidth();
        this.imageHeight = imageOut.getHeight();

        this.filterSquarePixels =
                new boolean[this.imageWidth][this.imageHeight];

        /*
         * Copy ahead a couple of max filter squares to imageOut.
         */
        int yCopy = 0;
        int xCopy = 0;

        for (yCopy = 0; yCopy < INITIAL_COPY_AHEAD_MAX_FILTER_SQUARES
                * this.parms.getFilterSquareWidthMax()
                && yCopy < this.imageHeight; yCopy++) {

            for (xCopy = 0; xCopy < this.imageWidth; xCopy++) {
                copyPixel(imageIn, imageOut, xCopy, yCopy);
            }
        }

        //
        for (int y = 0; y < this.imageHeight; y++, yCopy++) {

            xCopy = 0;

            int x = 0;

            while (x < this.imageWidth) {

                /*
                 * Advance to next filter square pixel and copy ahead on current
                 * to imageOut ahead-line in one go.
                 */
                for (; x < this.imageWidth && !this
                        .isFilterSquarePixel(imageOut, x, y); x++, xCopy++) {

                    if (xCopy < this.imageWidth && yCopy < this.imageHeight
                            && !this.filterSquarePixels[xCopy][yCopy]) {
                        copyPixel(imageIn, imageOut, xCopy, yCopy);
                    }
                }

                if (x == this.imageWidth) {
                    continue;
                }

                this.process(imageOut, x, y);

                x++;

                /*
                 * Copy ahead on (rest of) current to imageOut ahead-line.
                 */
                if (xCopy < this.imageWidth && yCopy < this.imageHeight
                        && !this.filterSquarePixels[xCopy][yCopy]) {
                    copyPixel(imageIn, imageOut, xCopy, yCopy);
                    xCopy++;
                }

            }
        }
    }

    @Override
    public double getFractionFiltered() {
        return (double) this.totPixelsFiltered / this.totPixelsNonWhite;
    }
}
