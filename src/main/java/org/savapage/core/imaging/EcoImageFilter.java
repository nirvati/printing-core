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

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 * A filter for creating an Eco-friendly image for printing.
 *
 * @author Rijk Ravestein
 *
 */
public interface EcoImageFilter {

    /**
     * Filters an image to a {@link File}.
     *
     * @param imageIn
     *            The {@link URL} of the input image.
     * @param imageOut
     *            The filtered output {@link File}.
     * @return The fraction of non-white pixels that were filtered.
     * @throws IOException
     *             When IO errors.
     */
    double filter(URL imageIn, File imageOut) throws IOException;

    /**
     * Filters an image {@link File} to a {@link BufferedImage}.
     *
     * @param imageIn
     *            The input image {@link File}.
     * @return The filtered output as {@link BufferedImage}.
     * @throws IOException
     *             When IO errors.
     */
    BufferedImage filter(File imageIn) throws IOException;

    /**
     * @return The processing time (milliseconds) for reading the input image.
     */
    long getReadTime();

    /**
     * @return The processing time (milliseconds) for filtering the image.
     */
    long getFilterTime();

    /**
     * @return The processing time (milliseconds) for writing the output image
     *         to file.
     */
    long getWriteTime();

    /**
     * @return The total processing time (milliseconds) of the filter operation.
     */
    long getTotalTime();

    /**
     * @return The fraction of non-white pixels that were filtered.
     */
    double getFractionFiltered();

}
