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
 * A facade to a PDF document creator.
 *
 * @author Rijk Ravestein
 *
 */
public interface PdfDocumentFacade {

    /**
     * Checks if the document is open.
     *
     * @return true if the document is open
     */
    boolean isOpen();

    /**
     * Opens the document.
     */
    void open();

    /**
     * Signals that an new page has to be started.
     *
     * @return true if the page was added, false if not.
     */
    boolean newPage();

    /**
     * Rotates the current page.
     *
     * @return {@code true} if rotation succeeded.
     */
    boolean rotatePage();

    /**
     * Gets page width.
     *
     * @return width.
     */
    float getPageWidth();

    /**
     * Gets page height.
     *
     * @return height.
     */
    float getPageHeight();

    /**
     * Gets the left margin.
     *
     * @return left margin.
     */
    float leftMargin();

    /**
     * Gets the right margin.
     *
     * @return right margin.
     */
    float rightMargin();

    /**
     * Gets the top margin.
     *
     * @return top margin.
     */
    float topMargin();

    /**
     * Gets the bottom margin.
     *
     * @return bottom margin.
     */
    float bottomMargin();

    /**
     * Adds an image to the Document.
     *
     * @param image
     *            Image to add
     * @return true if the element was added, false if not.
     */
    boolean add(PdfImageFacade image);
}
