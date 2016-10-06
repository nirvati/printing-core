/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2016 Datraverse B.V.
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfCopy;
import com.itextpdf.text.pdf.PdfReader;

/**
 * Collects multiple copies of a PDF document into one (1) PDF document for
 * proxy printing.
 *
 * @author Rijk Ravestein
 *
 */
public final class PdfPrintCollector {

    /**
     * The {@link PdfReader} containing a single blank page to be used to insert
     * into the collected result.
     */
    private PdfReader singleBlankPagePdfReader;

    /**
     * Private instance only.
     */
    private PdfPrintCollector() {
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
            this.singleBlankPagePdfReader =
                    ITextPdfCreator.createBlankPageReader(pageSize);
        }

        return this.singleBlankPagePdfReader;
    }

    /**
     * Calculates the number of printed sheets (including copies).
     *
     * @param request
     *            The {@link ProxyPrintSheetsCalcParms}.
     * @param blankFillerPages
     *            The total number of blank filler pages appended between
     *            logical jobs (proxy print only).
     * @return The number of printed sheets.
     */
    public static int calcNumberOfPrintedSheets(
            final ProxyPrintSheetsCalcParms request,
            final int blankFillerPages) {

        return calcNumberOfPrintedSheets(
                request.getNumberOfPages() + blankFillerPages,
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
     * sequence of copies.
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
    public static int calcBlankAppendPagesOfCopy(
            final ProxyPrintSheetsCalcParms calcParms) {

        /*
         * The pages needed for a full single copy.
         */
        int nPagesNeeded = calcNumberOfPrintedSheets(calcParms, 0)
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
     * error is fixed in qpdf 5.1.2-3. See Mantis #614.
     * </p>
     *
     * @param collectedPdfCopy
     *            The {@link PdfCopy} append the blank page to.
     * @throws DocumentException
     *             When error creating the PDF document.
     * @throws IOException
     *             When IO errors creating the reader.
     */
    private void addBlankPage(final PdfCopy collectedPdfCopy)
            throws IOException, DocumentException {
        collectedPdfCopy.addPage(collectedPdfCopy.getImportedPage(
                this.getBlankPageReader(collectedPdfCopy.getPageSize()), 1));
    }

    /**
     * Collects multiple copies of a single PDF input file into a single PDF
     * output file.
     *
     * @param calcParms
     *            The {@link ProxyPrintSheetsCalcParms}.
     * @param collate
     *            If {@code true} output must be collated.
     * @param fileIn
     *            The PDF input file.
     * @param fileOut
     *            The PDF output file.
     * @return The number of pages in the collated document.
     * @throws IOException
     *             When IO errors.
     */
    public static int collect(final ProxyPrintSheetsCalcParms calcParms,
            final boolean collate, final File fileIn, final File fileOut)
            throws IOException {

        int nTotalOutPages = 0;

        final Document targetDocument = new Document();

        final PdfPrintCollector pdfCollector = new PdfPrintCollector();

        try {

            final PdfCopy collectedPdfCopy =
                    new PdfCopy(targetDocument, new FileOutputStream(fileOut));

            targetDocument.open();

            final PdfReader pdfReader =
                    new PdfReader(new FileInputStream(fileIn));

            final int nBlankPagesToAppend =
                    calcBlankAppendPagesOfCopy(calcParms);

            if (collate) {
                nTotalOutPages = collectCollated(pdfCollector, collectedPdfCopy,
                        pdfReader, calcParms, nBlankPagesToAppend);
            } else {
                nTotalOutPages =
                        collectUncollated(pdfCollector, collectedPdfCopy,
                                pdfReader, calcParms, nBlankPagesToAppend);
            }

            targetDocument.close();

        } catch (DocumentException e) {
            throw new IOException(e.getMessage(), e);
        } finally {
            pdfCollector.close();
        }

        return nTotalOutPages;
    }

    /**
     * Collects collated output.
     *
     * @param pdfCollector
     *            The {@link PdfPrintCollector}.
     * @param collectedPdfCopy
     *            The {@link PdfCopy} to collect the pages on.
     * @param pdfReader
     *            The {@link PdfReader}.
     * @param calcParms
     *            The {@link ProxyPrintSheetsCalcParms}.
     * @param nBlankPagesToAppend
     *            The number of blank pages to append to a single document copy.
     * @return The number of pages in the collated document.
     * @throws IOException
     *             When IO errors.
     * @throws DocumentException
     *             When PDF errors.
     */
    private static int collectCollated(final PdfPrintCollector pdfCollector,
            final PdfCopy collectedPdfCopy, final PdfReader pdfReader,
            final ProxyPrintSheetsCalcParms calcParms,
            final int nBlankPagesToAppend)
            throws IOException, DocumentException {

        final int nPagesMax = pdfReader.getNumberOfPages();

        int nTotalOutPages = 0;

        for (int j = 0; j < calcParms.getNumberOfCopies(); j++) {

            if (j > 0) {

                for (int k = 0; k < nBlankPagesToAppend; k++) {
                    pdfCollector.addBlankPage(collectedPdfCopy);
                    nTotalOutPages++;
                }
            }

            for (int nPage = 1; nPage <= nPagesMax; nPage++) {
                collectPage(pdfCollector, collectedPdfCopy, pdfReader, nPage);
                nTotalOutPages++;
            }
        }
        return nTotalOutPages;
    }

    /**
     * Collects uncollated output.
     *
     * @param pdfCollector
     *            The {@link PdfPrintCollector}.
     * @param collectedPdfCopy
     *            The {@link PdfCopy} to collect the pages on.
     * @param pdfReader
     *            The {@link PdfReader}.
     * @param calcParms
     *            The {@link ProxyPrintSheetsCalcParms}.
     * @param nBlankPagesToAppend
     *            The number of blank pages to append to a single document copy.
     * @return The number of pages in the collated document.
     * @throws IOException
     *             When IO errors.
     * @throws DocumentException
     *             When PDF errors.
     */
    private static int collectUncollated(final PdfPrintCollector pdfCollector,
            final PdfCopy collectedPdfCopy, final PdfReader pdfReader,
            final ProxyPrintSheetsCalcParms calcParms,
            final int nBlankPagesToAppend)
            throws IOException, DocumentException {

        /*
         * The number of pages in the un-collated sequence.
         */
        final int nPageSequence;

        if (calcParms.isDuplex()) {
            nPageSequence = 2 * calcParms.getNup();
        } else {
            nPageSequence = calcParms.getNup();
        }

        /*
         * The number of pages in the source document.
         */
        final int nPagesDoc = pdfReader.getNumberOfPages();

        int nTotalOutPages = 0;

        /*
         * Traverse all source document pages.
         */
        for (int nPage = 1; nPage <= nPagesDoc; nPage += nPageSequence) {

            /*
             * Calc page sequence for each copy.
             */
            final int nPageFrom = nPage;
            final int nPageTo = nPage + nPageSequence - 1;

            /*
             * Collect page sequence for each copy.
             */
            for (int i = 0; i < calcParms.getNumberOfCopies(); i++) {

                int iSequence = 0;
                int nPageWlk = nPageFrom;

                while (nPageWlk <= nPageTo && nPageWlk <= nPagesDoc) {

                    collectPage(pdfCollector, collectedPdfCopy, pdfReader,
                            nPageWlk);

                    nTotalOutPages++;

                    nPageWlk++;
                    iSequence++;
                }

                /*
                 * When this is NOT the last sequence of the collected
                 * document...
                 */
                if (iSequence < nPageSequence
                        && i + 1 < calcParms.getNumberOfCopies()) {
                    /*
                     * ... append blank pages.
                     */
                    for (int k = 0; k < nBlankPagesToAppend; k++) {
                        pdfCollector.addBlankPage(collectedPdfCopy);
                        nTotalOutPages++;
                    }
                }
            }
        }

        return nTotalOutPages;
    }

    /**
     * Collects a page.
     *
     * @param pdfCollector
     *            The {@link PdfPrintCollector}.
     * @param collectedPdfCopy
     *            The {@link PdfCopy} to collect the pages on.
     * @param pdfReader
     *            The {@link PdfReader}.
     * @param nPage
     *            The 1-based page ordinal in the PDF reader.
     * @throws IOException
     *             When IO errors.
     * @throws DocumentException
     *             When PDF errors.
     */
    private static void collectPage(final PdfPrintCollector pdfCollector,
            final PdfCopy collectedPdfCopy, final PdfReader pdfReader,
            final int nPage) throws IOException, DocumentException {

        final boolean pageContentsPresent =
                ITextPdfCreator.isPageContentsPresent(pdfReader, nPage);

        if (pageContentsPresent) {

            collectedPdfCopy.addPage(
                    collectedPdfCopy.getImportedPage(pdfReader, nPage));

        } else {
            /*
             * Replace page without /Contents with our own blank content. See
             * Mantis #614.
             */
            pdfCollector.addBlankPage(collectedPdfCopy);
        }
    }
}
