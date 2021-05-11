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

import com.itextpdf.text.Image;

/**
 * Facade to {@link com.itextpdf.text.Image}: AGPL license.
 *
 * @author Rijk Ravestein
 *
 */
public final class PdfImageAGPL implements PdfImageFacade {

    /** */
    private final Image image;

    /**
     * Constructor.
     *
     * @param img
     *            {@link com.itextpdf.text.Image}.
     */
    public PdfImageAGPL(final Image img) {
        this.image = img;
    }

    @Override
    public float getWidth() {
        return this.image.getWidth();
    }

    @Override
    public float getHeight() {
        return this.image.getHeight();
    }

    @Override
    public void scalePercent(final float percent) {
        this.image.scalePercent(percent);
    }

    @Override
    public void setRotation(final float rotation) {
        this.image.setRotation(rotation);
    }

    /**
     * @return {@link com.itextpdf.text.Image}.
     */
    public Image getImage() {
        return image;
    }

}
