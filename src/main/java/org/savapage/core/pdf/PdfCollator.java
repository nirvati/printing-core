/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2015 Datraverse B.V.
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.savapage.core.print.proxy.ProxyPrintSheetsCalcParms;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfCopy;
import com.itextpdf.text.pdf.PdfReader;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class PdfCollator {

    /**
     * No instance allowed.
     */
    private PdfCollator() {

    }

    /**
     * Calculates the number of printed sheets (including copies).
     *
     * @param request
     *            The {@link ProxyPrintSheetsCalcParms}.
     * @return The number of printed sheets.
     */
    public static int calcNumberOfPrintedSheets(
            final ProxyPrintSheetsCalcParms request) {

        return calcNumberOfPrintedSheets(request.getNumberOfPages(),
                request.getNumberOfCopies(), request.isDuplex(),
                request.getNup(), request.isOddOrEvenSheets(),
                request.isCoverPageBefore(), request.isCoverPageAfter());
    }

    /**
     * Calculates the number of printed sheets.
     *
     * @param numberOfPages
     *            The number of pages in the document.
     * @param copies
     *            The number of copies to print.
     * @param duplex
     * @param nUp
     * @param oddOrEvenSheets
     * @param coverPageBefore
     * @param coverPageAfter
     * @return The number of printed sheets.
     */
    public static int calcNumberOfPrintedSheets(final int numberOfPages,
            final int copies, final boolean duplex, final int nUp,
            final boolean oddOrEvenSheets, final boolean coverPageBefore,
            final boolean coverPageAfter) {

        int nPages = numberOfPages;

        // NOTE: the order of handling the print options is important.

        if (nPages <= nUp) {
            nPages = 1;
        } else if (nUp != 1) {
            nPages = (nPages / nUp) + (nPages % nUp);
        }

        /*
         * (2) Odd or even pages?
         */
        if (oddOrEvenSheets) {
            nPages /= 2;
        }

        /*
         * Sheets
         */
        int nSheets = nPages;

        /*
         * (3) Duplex
         */
        if (duplex) {
            nSheets = (nSheets / 2) + (nSheets % 2);
        }

        /*
         * (4) Copies
         */
        nSheets *= copies;

        /*
         * (5) Jobs Sheets
         */
        if (coverPageBefore) {
            // cover page (before)
            nSheets++;
        }
        if (coverPageAfter) {
            // cover page (after)
            nSheets++;
        }

        return nSheets;
    }

    /**
     * Check whether {@link ProxyPrintSheetsCalcParms} will give collation in
     * {@link #collate(ProxyPrintSheetsCalcParms, File, File)}.
     *
     * @param calcParms
     *            The {@link ProxyPrintSheetsCalcParms}.
     * @return {@code true} when input parameter will give collation.
     */
    public static boolean
            isCollatable(final ProxyPrintSheetsCalcParms calcParms) {

        return calcParms.getNumberOfCopies() > 1
                && (calcParms.isDuplex() || calcParms.getNup() > 1);
    }

    /**
     * Calculates the extra blank pages to append to a single PDF copy in a
     * collated sequence of copies.
     * <p>
     * IMPORTANT: {@link ProxyPrintSheetsCalcParms#isOddOrEvenSheets()},
     * {@link ProxyPrintSheetsCalcParms#isCoverPageBefore()} and
     * {@link ProxyPrintSheetsCalcParms#isCoverPageAfter()} are <b>not</b> taken
     * into consideration.
     * </p>
     *
     * @param calcParms
     *            The {@link ProxyPrintSheetsCalcParms}.
     * @return The number of extra blank pages to append to each copy.
     */
    public static int calcBlankCollatePagesToAppend(
            final ProxyPrintSheetsCalcParms calcParms) {

        /*
         * The pages needed for a full single copy.
         */
        int nPagesNeeded =
                calcNumberOfPrintedSheets(calcParms)
                        / calcParms.getNumberOfCopies();

        if (calcParms.getNup() > 1) {
            nPagesNeeded *= calcParms.getNup();
        }

        if (calcParms.isDuplex()) {
            nPagesNeeded *= 2;
        }

        /*
         * Return the pages we are short on the full single copy.
         */
        return nPagesNeeded - calcParms.getNumberOfPages();
    }

    /**
     * Collates multiple copies of a single PDF input file into a single PDF
     * output file.
     *
     * @param calcParms
     *            The {@link ProxyPrintSheetsCalcParms}.
     * @param fileIn
     *            The PDF input file.
     * @param fileOut
     *            The PDF output file.
     * @throws IOException
     *             When IO errors.
     */
    public static int collate(final ProxyPrintSheetsCalcParms calcParms,
            final File fileIn, final File fileOut) throws IOException {

        int nTotalOutPages = 0;

        final Document targetDocument = new Document();

        try {

            final PdfCopy collatedPdfCopy =
                    new PdfCopy(targetDocument, new FileOutputStream(fileOut));

            targetDocument.open();

            final PdfReader readerWlk =
                    new PdfReader(new FileInputStream(fileIn));

            final int nPagesMax = readerWlk.getNumberOfPages();

            final int nBlankPagesToAppend =
                    calcBlankCollatePagesToAppend(calcParms);

            for (int j = 0; j < calcParms.getNumberOfCopies(); j++) {

                if (j > 0) {
                    for (int k = 0; k < nBlankPagesToAppend; k++) {
                        collatedPdfCopy.addPage(collatedPdfCopy.getPageSize(),
                                0);
                        nTotalOutPages++;
                    }
                }

                for (int i = 0; i < nPagesMax; i++) {
                    collatedPdfCopy.addPage(collatedPdfCopy.getImportedPage(
                            readerWlk, i + 1));
                    nTotalOutPages++;
                }
            }

            targetDocument.close();

        } catch (DocumentException e) {
            throw new IOException(e.getMessage(), e);
        }

        return nTotalOutPages;
    }
}
