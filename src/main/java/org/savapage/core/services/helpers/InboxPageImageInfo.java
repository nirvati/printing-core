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
package org.savapage.core.services.helpers;

import java.util.Base64;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class InboxPageImageInfo {

    /**
     * Basename of the PDF file.
     * <p>
     * For example: c3e7af09-2cd3-4c92-afc2-788faf09a0ce.pdf
     * </p>
     */
    private String file;

    /**
     * Number of pages of PDF file.
     */
    private int numberOfPages;

    /**
     * Zero-based page number WITHIN the job file.
     */
    private int pageInFile;

    /**
     * {@code true} if the PDF orientation of the PDF inbox document is
     * landscape.
     */
    private boolean landscape;

    /**
     * The PDF rotation the PDF inbox document.
     */
    private int rotation;

    /**
     * The rotation on the PDF inbox document set by the User.
     */
    private int rotate;

    /**
     * Base64 encoded SVG overlay. If {@code null}, no overlay is present.
     */
    private String overlaySVG64;

    /**
     *
     * @return Basename of the PDF file.
     */
    public String getFile() {
        return file;
    }

    /**
     *
     * @param file
     *            Basename of the PDF file.
     */
    public void setFile(String file) {
        this.file = file;
    }

    /**
     * @return Number of pages of PDF file.
     */
    public int getNumberOfPages() {
        return numberOfPages;
    }

    /**
     * @param numberOfPages
     *            Number of pages of PDF file.
     */
    public void setNumberOfPages(int numberOfPages) {
        this.numberOfPages = numberOfPages;
    }

    /**
     *
     * @return Zero-based page number WITHIN the job file.
     */
    public int getPageInFile() {
        return pageInFile;
    }

    /**
     *
     * @param pageInFile
     *            Zero-based page number WITHIN the job file.
     */
    public void setPageInFile(int pageInFile) {
        this.pageInFile = pageInFile;
    }

    /**
     *
     * @return {@code true} if the PDF orientation of the PDF inbox document is
     *         landscape.
     */
    public boolean isLandscape() {
        return landscape;
    }

    /**
     *
     * @param landscape
     *            {@code true} if the PDF orientation of the PDF inbox document
     *            is landscape.
     */
    public void setLandscape(boolean landscape) {
        this.landscape = landscape;
    }

    /**
     *
     * @return The PDF rotation the PDF inbox document.
     */
    public int getRotation() {
        return rotation;
    }

    /**
     *
     * @param rotation
     *            The PDF rotation the PDF inbox document.
     */
    public void setRotation(int rotation) {
        this.rotation = rotation;
    }

    /**
     *
     * @return The rotation on the PDF inbox document set by the User.
     */
    public int getRotate() {
        return rotate;
    }

    /**
     *
     * @param rotate
     *            The rotation on the PDF inbox document set by the User.
     */
    public void setRotate(int rotate) {
        this.rotate = rotate;
    }

    /**
     * @return Base64 encoded SVG overlay. If {@code null}, no overlay is
     *         present.
     */
    public String getOverlaySVG64() {
        return overlaySVG64;
    }

    /**
     * @param overlaySVG64
     *            Base64 encoded SVG overlay. If {@code null}, no overlay is
     *            present.
     */
    public void setOverlaySVG64(String overlaySVG64) {
        this.overlaySVG64 = overlaySVG64;
    }

    /**
     * @return SVG overlay. If {@code null}, no overlay is present.
     */
    public String getOverlaySVG() {
        if (this.overlaySVG64 == null) {
            return null;
        }
        return new String(Base64.getDecoder().decode(this.overlaySVG64));
    }

}
