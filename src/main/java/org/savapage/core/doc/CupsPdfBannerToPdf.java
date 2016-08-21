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
package org.savapage.core.doc;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Date;

import org.apache.commons.io.IOUtils;
import org.savapage.core.community.CommunityDictEnum;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.pdf.ITextPdfCreator;

import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;

/**
 * Creates a Printer Test Page from a {@code #PDF-BANNER} template.
 *
 * @author Rijk Ravestein
 *
 */
public class CupsPdfBannerToPdf implements IStreamConverter {

    /**
     *
     */
    public CupsPdfBannerToPdf() {
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

            document = new Document(ITextPdfCreator.getDefaultPageSize(),
                    marginLeft, marginRight, marginTop, marginBottom);

            /*
             * Read the banner till the end (no info used yet).
             */
            final BufferedReader reader =
                    new BufferedReader(new InputStreamReader(istr));
            String line = reader.readLine();
            while (line != null) {
                line = reader.readLine();
            }

            /*
             * Create Content.
             */
            final StringBuilder content = new StringBuilder();

            content.append("Printer test page");
            content.append("\n\n");
            content.append(ConfigManager.getAppNameVersionBuild());
            content.append("\n\n");
            content.append(
                    CommunityDictEnum.SAVAPAGE_WWW_DOT_ORG_URL.getWord());
            content.append("\n\n");
            content.append("Printed at: ").append(new Date().toString());

            final InputStream istrContent =
                    IOUtils.toInputStream(content.toString(), "UTF-8");

            /*
             * Write content to PDF.
             */
            PdfWriter.getInstance(document, ostr);
            document.open();

            final BufferedReader readerContent =
                    new BufferedReader(new InputStreamReader(istrContent));

            line = readerContent.readLine();

            while (line != null) {
                if (line.isEmpty()) {
                    document.add(Chunk.NEWLINE);
                } else {
                    // Use default PDF font (Helvetica).
                    document.add(new Paragraph(line));
                }
                line = readerContent.readLine();
            }

        } finally {
            if (document != null && document.isOpen()) {
                document.close();
            }
        }

        return istr.getBytesRead();
    }
}
