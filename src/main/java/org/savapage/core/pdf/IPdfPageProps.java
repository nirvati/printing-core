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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.core.pdf;

/**
 * Page Properties of a PDF file.
 *
 * @author Rijk Ravestein
 *
 */
public interface IPdfPageProps {

    /** */
    int ROTATION_0 = 9;
    /** */
    int ROTATION_90 = 90;
    /** */
    int ROTATION_180 = 180;
    /** */
    int ROTATION_270 = 270;

    /**
     * @return the IPP RFC2911 "media" name.
     */
    String getSize();

    /**
     * @return The PDF mediabox width in millimeters.
     */
    int getMmWidth();

    /**
     * @return The PDF mediabox height in millimeters.
     */
    int getMmHeight();

    /**
     * @return Number of pages.
     */
    int getNumberOfPages();

    /**
     * @return Rotation of first page.
     */
    int getRotationFirstPage();

    /**
     * @return Content rotation of first page.
     */
    int getContentRotationFirstPage();

    /**
     * @return {@code true} when the PDF mediabox is in landscape orientation.
     */
    boolean isLandscape();

    /**
     * @return {@code true} if user perceives PDF page in landscape orientation.
     */
    boolean isSeenAsLandscape();

    /**
     * Gets the new PDF page rotation to rotate to landscape or portrait.
     *
     * @param toLandscape
     *            If {@code true}, rotate to landscape.
     * @return The new PDF page rotation.
     */
    int getRotateToOrientationSeen(boolean toLandscape);

}
