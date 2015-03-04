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

import org.savapage.core.fonts.InternalFontFamilyEnum;
import org.savapage.core.pdf.ITextCreator;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfWriter;

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
                    new Document(ITextCreator.getDefaultPageSize(), marginLeft,
                            marginRight, marginTop, marginBottom);

            /*
             * Get the font: for now fixed to DejaVu.
             */
            final BaseFont bf = ITextCreator.createFont(this.internalFont);

            /*
             * Use default font size, instead of e.g. new Font(bf, 20);
             */
            final Font fontWrk = new Font(bf);
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
