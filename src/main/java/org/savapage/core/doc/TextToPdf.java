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
package org.savapage.core.doc;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;

import org.savapage.core.fonts.FontLocation;
import org.savapage.core.fonts.InternalFontFamilyEnum;
import org.savapage.core.pdf.ITextPdfCreator;

import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.Font;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfWriter;

/**
 *
 * @author Datraverse B.V.
 *
 */
public class TextToPdf implements IStreamConverter {

    /**
     *
     */
    private final InternalFontFamilyEnum internalFont;

    /**
     *
     * @param font
     *            The font of the PDF output.
     */
    public TextToPdf(final InternalFontFamilyEnum font) {
        this.internalFont = font;
    }

    @Override
    public final long convert(final DocContentTypeEnum contentType,
            final DocInputStream istr, final OutputStream ostr)
            throws Exception {

        final float marginLeft = 50;
        final float marginRight = 50;
        final float marginTop = 50;
        final float marginBottom = 50;

        Document document = null;

        try {

            document =
                    new Document(ITextPdfCreator.getDefaultPageSize(),
                            marginLeft, marginRight, marginTop, marginBottom);

            final Font fontWrk;

            if (this.internalFont != null
                    && FontLocation.isFontPresent(this.internalFont)) {
                /*
                 * Get the font: for now fixed to DejaVu.
                 */
                final BaseFont bf =
                        ITextPdfCreator.createFont(this.internalFont);
                /*
                 * Use default font size, instead of e.g. new Font(bf, 20);
                 */
                fontWrk = new Font(bf);
            } else {
                fontWrk = null;
            }

            /*
             *
             */
            PdfWriter.getInstance(document, ostr);
            document.open();

            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(istr));

            String line = reader.readLine();

            while (line != null) {
                if (line.isEmpty()) {
                    document.add(Chunk.NEWLINE);
                } else if (fontWrk == null) {
                    // Fall-back to default PDF font (Helvetica).
                    document.add(new Paragraph(line));
                } else {
                    document.add(new Paragraph(line, fontWrk));
                }
                line = reader.readLine();
            }

        } finally {
            if (document != null && document.isOpen()) {
                document.close();
            }
        }

        return istr.getBytesRead();
    }
}
