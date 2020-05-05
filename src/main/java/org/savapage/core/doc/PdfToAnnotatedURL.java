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

import org.savapage.core.json.PdfProperties;
import org.savapage.core.pdf.ITextHelperV5;
import org.savapage.core.pdf.ITextPdfUrlAnnotator;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;

/**
 * Converts a PDF file to a PDF with annotated URL with optional PDF encryption.
 *
 * @author Rijk Ravestein
 *
 */
public final class PdfToAnnotatedURL extends AbstractPdfConverter
        implements IPdfConverter {

    /**
     * A unique suffix to type the kind of PDF convert.
     */
    private static final String OUTPUT_FILE_SFX = "annotated-url";

    /** */
    private final boolean pdfEncryption;

    /** */
    private PdfProperties.PdfAllow pdfAllow;
    /** */
    private String pdfOwnerPass;
    /** */
    private String pdfUserPass;

    /**
     *
     */
    public PdfToAnnotatedURL() {
        super();
        this.pdfEncryption = false;
    }

    /**
     *
     * @param allow
     *            PDF permissions.
     * @param ownerPass
     *            PDF owner password.
     * @param userPass
     *            PDF user password.
     */
    public PdfToAnnotatedURL(final PdfProperties.PdfAllow allow,
            final String ownerPass, final String userPass) {
        super();
        this.pdfEncryption = true;
        this.pdfAllow = allow;
        this.pdfOwnerPass = ownerPass;
        this.pdfUserPass = userPass;
    }

    /**
     *
     * @param createDir
     *            The directory location of the created file.
     */
    public PdfToAnnotatedURL(final File createDir) {
        super(createDir);
        this.pdfEncryption = false;
    }

    @Override
    public File convert(final File pdfFile) throws IOException {

        final File pdfOut = this.getOutputFile(pdfFile);
        final OutputStream ostr = new FileOutputStream(pdfOut);

        final PdfReader reader = new PdfReader(new FileInputStream(pdfFile));
        PdfStamper stamper = null;

        try {
            stamper = new PdfStamper(reader, ostr);

            if (this.pdfEncryption) {
                stamper.setEncryption(true, this.pdfUserPass, this.pdfOwnerPass,
                        ITextHelperV5.getPermissions(pdfAllow));
            }

            ITextPdfUrlAnnotator.annotate(reader, stamper);

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
