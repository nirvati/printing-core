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
package org.savapage.core.doc.soffice;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.savapage.core.doc.MimeTypeEnum;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class SOfficeDocFormatRegistryDefault
        extends SOfficeDocFormatRegistrySimple {

    /**
     *
     * @param docFormat
     * @param family
     * @param filter
     */
    private static void putStorePropFilterName(final SOfficeDocFormat docFormat,
            final SOfficeDocFamilyEnum family, final Object filter) {
        docFormat.putStoreProperties(family,
                Collections.singletonMap("FilterName", filter));
    }

    /**
     *
     * @param name
     * @param extension
     * @param mediaType
     * @return
     */
    private SOfficeDocFormat createAddFormat(final String name,
            final String extension, final String mediaType) {
        final SOfficeDocFormat docFormat =
                new SOfficeDocFormat(name, extension, mediaType);
        addFormat(docFormat);
        return docFormat;
    }

    /**
     *
     */
    public SOfficeDocFormatRegistryDefault() {

        /*
         * PDF
         */
        final SOfficeDocFormat pdf = createAddFormat("Portable Document Format",
                "pdf", MimeTypeEnum.APPLICATION_PDF.getWord());

        putStorePropFilterName(pdf, SOfficeDocFamilyEnum.TEXT,
                "writer_pdf_Export");
        putStorePropFilterName(pdf, SOfficeDocFamilyEnum.SPREADSHEET,
                "calc_pdf_Export");
        putStorePropFilterName(pdf, SOfficeDocFamilyEnum.PRESENTATION,
                "impress_pdf_Export");
        putStorePropFilterName(pdf, SOfficeDocFamilyEnum.DRAWING,
                "draw_pdf_Export");

        /*
         * SWF
         */
        final SOfficeDocFormat swf = createAddFormat("Macromedia Flash", "swf",
                "application/x-shockwave-flash");

        putStorePropFilterName(swf, SOfficeDocFamilyEnum.PRESENTATION,
                "impress_flash_Export");
        putStorePropFilterName(swf, SOfficeDocFamilyEnum.DRAWING,
                "draw_flash_Export");

        /*
         * XHTML : disabled because it's not always available
         */

        //
        // DocumentFormat xhtml = new DocumentFormat("XHTML", "xhtml",
        // "application/xhtml+xml");
        // xhtml.setStoreProperties(DocumentFamily.TEXT,
        // Collections.singletonMap("FilterName", "XHTML Writer File"));
        // xhtml.setStoreProperties(DocumentFamily.SPREADSHEET,
        // Collections.singletonMap("FilterName", "XHTML Calc File"));
        // xhtml.setStoreProperties(DocumentFamily.PRESENTATION,
        // Collections.singletonMap("FilterName", "XHTML Impress File"));
        // addFormat(xhtml);

        /*
         * HTML is treated as Text when supplied as input, but as an output it
         * is also available for exporting Spreadsheet and Presentation formats.
         */
        final SOfficeDocFormat html = createAddFormat("HTML", "html",
                MimeTypeEnum.TEXT_HTML.getWord());

        html.setInputFamily(SOfficeDocFamilyEnum.TEXT);

        putStorePropFilterName(html, SOfficeDocFamilyEnum.TEXT,
                "HTML (StarWriter)");
        putStorePropFilterName(html, SOfficeDocFamilyEnum.SPREADSHEET,
                "HTML (StarCalc)");
        putStorePropFilterName(html, SOfficeDocFamilyEnum.PRESENTATION,
                "impress_html_Export");

        /*
         * ODT.
         */
        final SOfficeDocFormat odt = createAddFormat("OpenDocument Text", "odt",
                "application/vnd.oasis.opendocument.text");

        odt.setInputFamily(SOfficeDocFamilyEnum.TEXT);

        putStorePropFilterName(odt, SOfficeDocFamilyEnum.TEXT, "writer8");

        /*
         * SXW
         */
        final SOfficeDocFormat sxw =
                createAddFormat("OpenOffice.org 1.0 Text Document", "sxw",
                        "application/vnd.sun.xml.writer");

        sxw.setInputFamily(SOfficeDocFamilyEnum.TEXT);

        putStorePropFilterName(sxw, SOfficeDocFamilyEnum.TEXT,
                "StarOffice XML (Writer)");

        /*
         * DOC
         */
        final SOfficeDocFormat doc =
                createAddFormat("Microsoft Word", "doc", "application/msword");

        doc.setInputFamily(SOfficeDocFamilyEnum.TEXT);

        putStorePropFilterName(doc, SOfficeDocFamilyEnum.TEXT, "MS Word 97");

        /*
         * DOCX
         */
        final SOfficeDocFormat docx = createAddFormat("Microsoft Word 2007 XML",
                "docx", "application/vnd.openxmlformats-officedocument"
                        + ".wordprocessingml.document");

        docx.setInputFamily(SOfficeDocFamilyEnum.TEXT);

        /*
         * RTF
         */
        final SOfficeDocFormat rtf =
                createAddFormat("Rich Text Format", "rtf", "text/rtf");

        rtf.setInputFamily(SOfficeDocFamilyEnum.TEXT);

        putStorePropFilterName(rtf, SOfficeDocFamilyEnum.TEXT,
                "Rich Text Format");

        /*
         * WPD
         */
        final SOfficeDocFormat wpd = createAddFormat("WordPerfect", "wpd",
                "application/wordperfect");

        wpd.setInputFamily(SOfficeDocFamilyEnum.TEXT);

        /*
         * TXT
         */
        final SOfficeDocFormat txt =
                createAddFormat("Plain Text", "txt", "text/plain");

        txt.setInputFamily(SOfficeDocFamilyEnum.TEXT);

        final Map<String, Object> txtLoadAndStoreProperties =
                new LinkedHashMap<String, Object>();
        txtLoadAndStoreProperties.put("FilterName", "Text (encoded)");
        txtLoadAndStoreProperties.put("FilterOptions", "utf8");
        txt.setLoadProperties(txtLoadAndStoreProperties);
        txt.putStoreProperties(SOfficeDocFamilyEnum.TEXT,
                txtLoadAndStoreProperties);

        /*
         * WIKI
         */
        final SOfficeDocFormat wikitext =
                createAddFormat("MediaWiki wikitext", "wiki", "text/x-wiki");

        putStorePropFilterName(wikitext, SOfficeDocFamilyEnum.TEXT,
                "MediaWiki");

        /*
         * ODS
         */
        final SOfficeDocFormat ods = createAddFormat("OpenDocument Spreadsheet",
                "ods", "application/vnd.oasis.opendocument.spreadsheet");

        ods.setInputFamily(SOfficeDocFamilyEnum.SPREADSHEET);

        putStorePropFilterName(ods, SOfficeDocFamilyEnum.SPREADSHEET, "calc8");

        /*
         * SXC
         */
        final SOfficeDocFormat sxc =
                createAddFormat("OpenOffice.org 1.0 Spreadsheet", "sxc",
                        "application/vnd.sun.xml.calc");

        sxc.setInputFamily(SOfficeDocFamilyEnum.SPREADSHEET);

        putStorePropFilterName(sxc, SOfficeDocFamilyEnum.SPREADSHEET,
                "StarOffice XML (Calc)");

        /*
         * XLS
         */
        final SOfficeDocFormat xls = createAddFormat("Microsoft Excel", "xls",
                "application/vnd.ms-excel");

        xls.setInputFamily(SOfficeDocFamilyEnum.SPREADSHEET);

        putStorePropFilterName(xls, SOfficeDocFamilyEnum.SPREADSHEET,
                "MS Excel 97");

        /*
         * XLSX
         */
        final SOfficeDocFormat xlsx =
                createAddFormat("Microsoft Excel 2007 XML", "xlsx",
                        "application/vnd.openxmlformats-officedocument"
                                + ".spreadsheetml.sheet");

        xlsx.setInputFamily(SOfficeDocFamilyEnum.SPREADSHEET);

        /*
         * CSV
         */
        final SOfficeDocFormat csv =
                createAddFormat("Comma Separated Values", "csv", "text/csv");

        csv.setInputFamily(SOfficeDocFamilyEnum.SPREADSHEET);

        final Map<String, Object> csvLoadAndStoreProperties =
                new LinkedHashMap<String, Object>();
        csvLoadAndStoreProperties.put("FilterName",
                "Text - txt - csv (StarCalc)");

        // Field Separator: ','; Text Delimiter: '"'
        csvLoadAndStoreProperties.put("FilterOptions", "44,34,0"); //
        csv.setLoadProperties(csvLoadAndStoreProperties);
        csv.putStoreProperties(SOfficeDocFamilyEnum.SPREADSHEET,
                csvLoadAndStoreProperties);

        /*
         * TSV
         */
        final SOfficeDocFormat tsv = createAddFormat("Tab Separated Values",
                "tsv", "text/tab-separated-values");

        tsv.setInputFamily(SOfficeDocFamilyEnum.SPREADSHEET);

        final Map<String, Object> tsvLoadAndStoreProperties =
                new LinkedHashMap<String, Object>();
        tsvLoadAndStoreProperties.put("FilterName",
                "Text - txt - csv (StarCalc)");
        // Field Separator: '\t'; Text Delimiter: '"'
        tsvLoadAndStoreProperties.put("FilterOptions", "9,34,0");
        tsv.setLoadProperties(tsvLoadAndStoreProperties);
        tsv.putStoreProperties(SOfficeDocFamilyEnum.SPREADSHEET,
                tsvLoadAndStoreProperties);

        /*
         *
         */
        final SOfficeDocFormat odp =
                createAddFormat("OpenDocument Presentation", "odp",
                        "application/vnd.oasis.opendocument.presentation");

        odp.setInputFamily(SOfficeDocFamilyEnum.PRESENTATION);

        putStorePropFilterName(odp, SOfficeDocFamilyEnum.PRESENTATION,
                "impress8");

        // SXI
        final SOfficeDocFormat sxi =
                createAddFormat("OpenOffice.org 1.0 Presentation", "sxi",
                        "application/vnd.sun.xml.impress");

        sxi.setInputFamily(SOfficeDocFamilyEnum.PRESENTATION);

        putStorePropFilterName(sxi, SOfficeDocFamilyEnum.PRESENTATION,
                "StarOffice XML (Impress)");

        // PPT
        final SOfficeDocFormat ppt = createAddFormat("Microsoft PowerPoint",
                "ppt", "application/vnd.ms-powerpoint");

        ppt.setInputFamily(SOfficeDocFamilyEnum.PRESENTATION);

        putStorePropFilterName(ppt, SOfficeDocFamilyEnum.PRESENTATION,
                "MS PowerPoint 97");

        // PPTX
        final SOfficeDocFormat pptx =
                createAddFormat("Microsoft PowerPoint 2007 XML", "pptx",
                        "application/vnd.openxmlformats-officedocument"
                                + ".presentationml.presentation");

        pptx.setInputFamily(SOfficeDocFamilyEnum.PRESENTATION);

        // ODG
        final SOfficeDocFormat odg = createAddFormat("OpenDocument Drawing",
                "odg", "application/vnd.oasis.opendocument.graphics");

        odg.setInputFamily(SOfficeDocFamilyEnum.DRAWING);

        putStorePropFilterName(odg, SOfficeDocFamilyEnum.DRAWING, "draw8");

        // SVG
        final SOfficeDocFormat svg = createAddFormat("Scalable Vector Graphics",
                "svg", "image/svg+xml");

        putStorePropFilterName(svg, SOfficeDocFamilyEnum.DRAWING,
                "draw_svg_Export");
    }

}
