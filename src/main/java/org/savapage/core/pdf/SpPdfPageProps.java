/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2014 Datraverse B.V.
 * Author: Rijk Ravestein.
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
 * @author Datraverse B.V.
 */
public class SpPdfPageProps {

    /**
     * The IPP RFC2911 "media" name.
     */
    private String size;

    int mmWidth = 0;
    int mmHeight = 0;
    int numberOfPages = 0;

    /**
     * @return the IPP RFC2911 "media" name.
     */
    public String getSize() {
        return size;
    }

    /**
     * Sets the IPP RFC2911 "media" name.
     *
     * @param size
     *            the IPP RFC2911 "media" name.
     */
    public void setSize(String size) {
        this.size = size;
    }

    public int getMmWidth() {
        return mmWidth;
    }

    public void setMmWidth(int mmWidth) {
        this.mmWidth = mmWidth;
    }

    public int getMmHeight() {
        return mmHeight;
    }

    public void setMmHeight(int mmHeight) {
        this.mmHeight = mmHeight;
    }

    public int getNumberOfPages() {
        return numberOfPages;
    }

    public void setNumberOfPages(int numberOfPages) {
        this.numberOfPages = numberOfPages;
    }

    /**
     * Creates the {@link SpPdfPageProps} of an PDF document.
     *
     * @param filePathPdf
     *            The PDF document file path.
     * @return The {@link SpPdfPageProps}.
     * @throws PdfSecurityException
     *             When encrypted or password protected PDF document.
     */
    public static SpPdfPageProps create(final String filePathPdf)
            throws PdfSecurityException {
        return AbstractPdfCreator.pageProps(filePathPdf);
    }
}
