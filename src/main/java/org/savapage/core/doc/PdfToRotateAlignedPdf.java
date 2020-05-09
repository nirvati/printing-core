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
package org.savapage.core.doc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;

import org.savapage.core.pdf.PdfPageRotateHelper;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfDictionary;
import com.itextpdf.text.pdf.PdfName;
import com.itextpdf.text.pdf.PdfNumber;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;

/**
 * Converts a PDF file to a PDF with pages rotated to a requested orientation.
 *
 * @author Rijk Ravestein
 *
 */
public final class PdfToRotateAlignedPdf extends AbstractPdfConverter
        implements IPdfConverter {

    /**
     * A unique suffix to type the kind of PDF convert.
     */
    private static final String OUTPUT_FILE_SFX = "rotated";

    /**
     * If {@code true}, pages are aligned to landscape orientation.
     */
    private final boolean alignLandscape;

    /**
     * A set with one-based PDF page numbers to be rotated to same orientation
     * as first page in a PDF document.
     */
    private final Set<Integer> pageNumbers;

    /**
     * @param alignToLandscape
     *            If {@code true}, pages are aligned to landscape orientation.
     * @param pages
     *            A set with one-based PDF page numbers to be rotated to same
     *            orientation as first page in a PDF document.
     */
    public PdfToRotateAlignedPdf(final boolean alignToLandscape,
            final Set<Integer> pages) {
        super();
        this.alignLandscape = alignToLandscape;
        this.pageNumbers = pages;
    }

    @Override
    public File convert(final File pdfFile) throws IOException {

        final File pdfOut = this.getOutputFile(pdfFile);
        final OutputStream ostr = new FileOutputStream(pdfOut);

        final PdfReader reader = new PdfReader(new FileInputStream(pdfFile));
        PdfStamper stamper = null;

        try {
            stamper = new PdfStamper(reader, ostr);

            for (final Integer entry : this.pageNumbers) {

                final int nPage = entry.intValue();

                final int alignedRotation = PdfPageRotateHelper
                        .getAlignedRotation(reader, this.alignLandscape, nPage);

                final PdfDictionary pageDict = reader.getPageN(nPage);
                pageDict.put(PdfName.ROTATE, new PdfNumber(alignedRotation));
            }

        } catch (DocumentException e) {
            stamper = null;
        } finally {
            if (stamper != null) {
                try {
                    stamper.close();
                } catch (DocumentException e) {
                    // noop
                }
            }
            reader.close();
        }
        return pdfOut;
    }

    @Override
    protected String getOutputFileSfx() {
        return OUTPUT_FILE_SFX;
    }

}
