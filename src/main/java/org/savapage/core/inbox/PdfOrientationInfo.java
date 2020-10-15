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
package org.savapage.core.inbox;

import org.savapage.core.pdf.PdfPageRotateHelper;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Information about PDF page orientation (portrait, landscape) and (user) page
 * rotation.
 *
 * @author Rijk Ravestein
 *
 */
@JsonInclude(Include.NON_NULL)
public class PdfOrientationInfo {

    /**
     * {@code true} if the PDF page orientation of the PDF inbox document is
     * landscape.
     */
    private boolean landscape;

    /**
     * The PDF page rotation.
     */
    private Integer rotation;

    /**
     * The PDF page content rotation (can be {@code null}).
     */
    private Integer contentRotation;

    /**
     * The rotation on the PDF inbox document set by the User.
     */
    private Integer rotate;

    public boolean getLandscape() {
        return landscape;
    }

    public void setLandscape(boolean landscape) {
        this.landscape = landscape;
    }

    /**
     * @return The PDF rotation of the PDF inbox document.
     */
    public Integer getRotation() {
        return rotation;
    }

    /**
     * @param rotation
     *            The PDF rotation the PDF inbox document.
     */
    public void setRotation(Integer rotation) {
        this.rotation = rotation;
    }

    /**
     * @return The PDF page content rotation (can be {@code null}).
     */
    public Integer getContentRotation() {
        return contentRotation;
    }

    /**
     * @param rotation
     *            The PDF page content rotation (can be {@code null}).
     */
    public void setContentRotation(Integer contentRotation) {
        this.contentRotation = contentRotation;
    }

    /**
     * @return The rotation on the PDF inbox document set by the User.
     */
    public Integer getRotate() {
        return rotate;
    }

    /**
     * @param rotate
     *            The rotation on the PDF inbox document set by the User.
     */
    public void setRotate(Integer rotate) {
        this.rotate = rotate;
    }

    /**
     *
     * @return A new object with default values.
     */
    public static PdfOrientationInfo createDefault() {
        final PdfOrientationInfo obj = new PdfOrientationInfo();
        obj.setLandscape(false);
        obj.setRotate(PdfPageRotateHelper.PDF_ROTATION_0);
        obj.setRotation(PdfPageRotateHelper.PDF_ROTATION_0);
        obj.setContentRotation(null);
        return obj;
    }
}
