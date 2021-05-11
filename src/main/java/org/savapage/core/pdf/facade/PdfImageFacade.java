/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2021 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: © 2021 Datraverse B.V. <info@datraverse.com>
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
package org.savapage.core.pdf.facade;

/**
 * A facade to a PDF image.
 *
 * @author Rijk Ravestein
 *
 */
public interface PdfImageFacade {

    /**
     * Gets the width of the image.
     *
     * @return the width.
     */
    float getWidth();

    /**
     * Gets the height of the image.
     *
     * @return the height.
     */
    float getHeight();

    /**
     * Scale the image to a certain percentage.
     *
     * @param percent
     *            the scaling percentage
     */
    void scalePercent(float percent);

    /**
     * Sets the rotation of the image in radians.
     *
     * @param rotation
     *            rotation in radians
     */
    void setRotation(float rotation);

}
