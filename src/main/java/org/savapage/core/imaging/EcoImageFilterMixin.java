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
import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;

/**
 * A filter strategy for creating an Eco-friendly PNG image for printing.
 *
 * @author Rijk Ravestein
 *
 */
public abstract class EcoImageFilterMixin implements EcoImageFilter {

    /**
     * .
     */
    private long startTime;

    /**
     * .
     */
    private long readTime;

    /**
     * .
     */
    private long filterTime;

    /**
     * .
     */
    private long writeTime;

    /**
     * .
     */
    private long totalTime;

    /**
     * .
     */
    protected static final int RGB_WHITE = Color.WHITE.getRGB();

    /**
     * .
     */
    protected static final int RGB_BLACK = Color.BLACK.getRGB();

    /**
     * Informal name of the output image format.
     *
     * @see {@link ImageIO#write(java.awt.image.RenderedImage, String, File)}.
     */
    private static final String IMAGE_FORMATNAME = "png";

    @Override
    public final BufferedImage filter(final File imageFile) throws IOException {

        this.startTime = System.currentTimeMillis();

        long timeWlk = this.startTime;
        final BufferedImage image = ImageIO.read(imageFile);
        this.readTime = System.currentTimeMillis() - timeWlk;

        //
        final BufferedImage imageOut = this.filter(image);

        //
        this.totalTime = System.currentTimeMillis() - this.startTime;

        return imageOut;
    }

    @Override
    public final double filter(final URL imageURL, final File fileOut)
            throws IOException {

        this.startTime = System.currentTimeMillis();

        long timeWlk = this.startTime;
        final BufferedImage image = ImageIO.read(imageURL);
        this.readTime = System.currentTimeMillis() - timeWlk;

        //
        final BufferedImage imageOut = this.filter(image);

        timeWlk = System.currentTimeMillis();
        ImageIO.write(imageOut, IMAGE_FORMATNAME, fileOut);
        this.writeTime = System.currentTimeMillis() - timeWlk;

        //
        this.totalTime = System.currentTimeMillis() - this.startTime;

        return getFractionFiltered();
    }

    /**
     * Filters an image.
     *
     * @param image
     *            The {@link BufferedImage} to filter.
     * @return The filtered {@link BufferedImage}.
     */
    private BufferedImage filter(final BufferedImage image) {

        final long filterStart = System.currentTimeMillis();

        final BufferedImage imageOut = new BufferedImage(image.getWidth(),
                image.getHeight(), BufferedImage.TYPE_INT_RGB);

        this.filter(image, imageOut);
        this.filterTime = System.currentTimeMillis() - filterStart;

        return imageOut;
    }

    /**
     * Filters an image.
     *
     * @param imageIn
     *            The {@link BufferedImage} to filter.
     * @param imageOut
     *            The filtered {@link BufferedImage}.
     */
    protected abstract void filter(final BufferedImage imageIn,
            final BufferedImage imageOut);

    @Override
    public final long getReadTime() {
        return readTime;
    }

    @Override
    public final long getFilterTime() {
        return filterTime;
    }

    @Override
    public final long getWriteTime() {
        return writeTime;
    }

    @Override
    public final long getTotalTime() {
        return totalTime;
    }

}
