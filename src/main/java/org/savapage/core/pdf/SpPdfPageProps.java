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
public final class SpPdfPageProps implements IPdfPageProps {

    /** */
    private String size;

    /** */
    private int mmWidth = 0;
    /** */
    private int mmHeight = 0;
    /** */
    private int numberOfPages = 0;

    /** */
    private int rotationFirstPage = 0;
    /** */
    private int contentRotationFirstPage = 0;

    @Override
    public String getSize() {
        return size;
    }

    /**
     * Sets the IPP RFC2911 "media" name.
     *
     * @param size
     *            the IPP RFC2911 "media" name.
     */
    public void setSize(final String size) {
        this.size = size;
    }

    @Override
    public int getMmWidth() {
        return mmWidth;
    }

    /**
     * @param width
     *            The PDF mediabox width in millimeters.
     */
    public void setMmWidth(final int width) {
        this.mmWidth = width;
    }

    @Override
    public int getMmHeight() {
        return mmHeight;
    }

    /**
     * @param height
     *            The PDF mediabox height in millimeters.
     */
    public void setMmHeight(final int height) {
        this.mmHeight = height;
    }

    @Override
    public int getNumberOfPages() {
        return numberOfPages;
    }

    /**
     *
     * @param nPages
     *            Number of pages.
     */
    public void setNumberOfPages(final int nPages) {
        this.numberOfPages = nPages;
    }

    @Override
    public int getRotationFirstPage() {
        return rotationFirstPage;
    }

    /**
     * @param rotation
     *            Rotation of first page.
     */
    public void setRotationFirstPage(final int rotation) {
        this.rotationFirstPage = rotation;
    }

    @Override
    public int getContentRotationFirstPage() {
        return contentRotationFirstPage;
    }

    /**
     * @param rotation
     *            Content rotation of first page.
     */
    public void setContentRotationFirstPage(final int rotation) {
        this.contentRotationFirstPage = rotation;
    }

    @Override
    public boolean isLandscape() {
        return this.mmHeight < this.mmWidth;
    }

    @Override
    public boolean isSeenAsLandscape() {
        return PdfPageRotateHelper.isSeenAsLandscape(this.isLandscape(),
                this.getRotationFirstPage());
    }

    @Override
    public int getRotateToOrientationSeen(final boolean toLandscape) {
        return PdfPageRotateHelper.rotateToOrientationSeen(toLandscape,
                this.isLandscape(), this.getRotationFirstPage(),
                this.getContentRotationFirstPage());
    }

    /**
     * Creates the {@link SpPdfPageProps} of an PDF document.
     *
     * @param filePathPdf
     *            The PDF document file path.
     * @return The {@link SpPdfPageProps}.
     * @throws PdfValidityException
     *             When invalid PDF document.
     * @throws PdfSecurityException
     *             When encrypted PDF document.
     * @throws PdfPasswordException
     *             When password protected PDF document.
     * @throws PdfUnsupportedException
     *             When unsupported PDF document.
     */
    public static SpPdfPageProps create(final String filePathPdf)
            throws PdfValidityException, PdfSecurityException,
            PdfPasswordException, PdfUnsupportedException {
        return AbstractPdfCreator.pageProps(filePathPdf);
    }

}
