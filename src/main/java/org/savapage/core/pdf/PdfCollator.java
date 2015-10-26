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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.savapage.core.print.proxy.ProxyPrintSheetsCalcParms;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfCopy;
import com.itextpdf.text.pdf.PdfDictionary;
import com.itextpdf.text.pdf.PdfName;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfWriter;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class PdfCollator {

    /**
     * The logger.
     */
    private static final Logger LOGGER = LoggerFactory
            .getLogger(PdfCollator.class);

    /**
     * The {@link PdfReader} containing a single blank page to be used to insert
     * into the collated result.
     */
    private PdfReader singleBlankPagePdfReader;

    /**
     * Private instance only.
     */
    private PdfCollator() {
    }

    /**
     * Lazy creates a 1-page {@link PdfReader} with one (1) blank page.
     * <p>
     * Note: in-memory size of the document is approx. 886 bytes.
     * </p>
     *
     * @param pageSize
     *            The size of the page.
     * @return The {@link PdfReader}.
     * @throws DocumentException
     *             When error creating the PDF document.
     * @throws IOException
     *             When IO errors creating the reader.
     */
    private PdfReader getBlankPageReader(final Rectangle pageSize)
            throws DocumentException, IOException {

        if (this.singleBlankPagePdfReader == null) {

            final ByteArrayOutputStream ostr = new ByteArrayOutputStream();

            final Document document = new Document(pageSize);

            PdfWriter.getInstance(document, ostr);
            document.open();

            /*
             * IMPORTANT: Paragraph MUST have content (if not, a close() of the
             * document throws an exception because no pages are detected):
             * therefore we use a single space as content.
             */
            document.add(new Paragraph(" "));

            document.close();

            this.singleBlankPagePdfReader =
                    new PdfReader(new ByteArrayInputStream(ostr.toByteArray()));
        }

        return this.singleBlankPagePdfReader;
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
     * Checks if a page in {@link PdfReader} has {@link PdfName#CONTENTS}.
     *
     * @param reader
     *            The {@link PdfReader}.
     * @param nPage
     *            The 1-based page ordinal.
     * @return {@code true} when page has content
     */
    private static boolean isPageContentsPresent(final PdfReader reader,
            final int nPage) {
        final PdfDictionary pageDict = reader.getPageN(nPage);
        return pageDict != null && pageDict.get(PdfName.CONTENTS) != null;
    }

    /**
     * Closes resources.
     */
    private void close() {
        if (this.singleBlankPagePdfReader != null) {
            this.singleBlankPagePdfReader.close();
            this.singleBlankPagePdfReader = null;
        }
    }

    /**
     * Adds a blank page to the {@link PdfCopy}.
     * <p>
     * NOTE: {@link PdfCopy#addPage(Rectangle, int)} is <b>not</b> used to add
     * the blank page, since CUPS 1.7.2 (qpdf 5.1.1-1) will report 'Exception:
     * unknown object type inspecting /Contents key in page dictionary': this
     * error is fixed in qpdf 5.1.2-3.
     * </p>
     *
     * @param collatedPdfCopy
     *            The {@link PdfCopy} append the blank page to.
     * @throws DocumentException
     *             When error creating the PDF document.
     * @throws IOException
     *             When IO errors creating the reader.
     */
    private void addBlankPage(final PdfCopy collatedPdfCopy)
            throws IOException, DocumentException {
        collatedPdfCopy.addPage(collatedPdfCopy.getImportedPage(
                this.getBlankPageReader(collatedPdfCopy.getPageSize()), 1));
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
     * @return The number of pages in the collated document.
     * @throws IOException
     *             When IO errors.
     */
    public static int collate(final ProxyPrintSheetsCalcParms calcParms,
            final File fileIn, final File fileOut) throws IOException {

        int nTotalOutPages = 0;

        final Document targetDocument = new Document();

        final PdfCollator pdfCollator = new PdfCollator();

        try {

            final PdfCopy collatedPdfCopy =
                    new PdfCopy(targetDocument, new FileOutputStream(fileOut));

            targetDocument.open();

            final PdfReader pdfReader =
                    new PdfReader(new FileInputStream(fileIn));

            final int nPagesMax = pdfReader.getNumberOfPages();

            final int nBlankPagesToAppend =
                    calcBlankCollatePagesToAppend(calcParms);

            for (int j = 0; j < calcParms.getNumberOfCopies(); j++) {

                if (j > 0) {

                    for (int k = 0; k < nBlankPagesToAppend; k++) {
                        pdfCollator.addBlankPage(collatedPdfCopy);
                        nTotalOutPages++;
                    }
                }

                for (int nPage = 1; nPage <= nPagesMax; nPage++) {

                    if (isPageContentsPresent(pdfReader, nPage)) {

                        collatedPdfCopy.addPage(collatedPdfCopy
                                .getImportedPage(pdfReader, nPage));

                    } else {
                        /*
                         * Replace page without /Contents with our own blank
                         * content. Reason: CUPS 1.7.2 (qpdf 5.1.1-1) will
                         * report: 'Exception: unknown object type inspecting
                         * /Contents key in page dictionary': this error is
                         * fixed in qpdf 5.1.2-3.
                         */
                        pdfCollator.addBlankPage(collatedPdfCopy);

                        if (LOGGER.isInfoEnabled()) {
                            LOGGER.info(String.format(
                                    "File [%s] page [%d] has NO /Contents: "
                                            + "replaced by blank content.",
                                    fileIn.getName(), nPage));
                        }
                    }
                    nTotalOutPages++;
                }
            }

            targetDocument.close();

        } catch (DocumentException e) {
            throw new IOException(e.getMessage(), e);
        } finally {
            pdfCollator.close();
        }

        return nTotalOutPages;
    }
}
