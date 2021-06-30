/*
 * This file is part of the SavaPage project <http://savapage.org>.
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.core.doc;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import javax.print.attribute.standard.MediaSizeName;

import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.ContentNode;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.PrettyXmlSerializer;
import org.htmlcleaner.TagNode;
import org.savapage.core.util.MediaUtils;
import org.xhtmlrenderer.pdf.ITextRenderer;

/**
 * Create a PDF file from HTML using
 * {@link org.xhtmlrenderer.pdf.ITextRenderer}.
 *
 * @author Rijk Ravestein
 *
 */
public final class HtmlToPdf implements IStreamConverter {

    /** */
    private static final String HTML_PAGE_SIZE_A4 = "A4";
    /** */
    private static final String HTML_PAGE_SIZE_LETTER = "Letter";

    @Override
    public long convert(final DocContentTypeEnum contentType,
            final DocInputStream istrDoc, final OutputStream ostrPdf)
            throws Exception {

        /*
         * Clean up the HTML to be well formed.
         */
        final HtmlCleaner cleaner = new HtmlCleaner();
        final CleanerProperties props = cleaner.getProperties();

        final TagNode node = cleaner.clean(istrDoc);

        /*
         *
         */
        final MediaSizeName mediaSizeName = MediaUtils.getDefaultMediaSize();

        final String pageSize;

        if (mediaSizeName == MediaSizeName.NA_LETTER) {
            pageSize = HTML_PAGE_SIZE_LETTER;
        } else {
            pageSize = HTML_PAGE_SIZE_A4;
        }

        addPageSize(node, pageSize);

        /*
         * Create a buffer to hold the cleaned up HTML.
         */
        final ByteArrayOutputStream bostr = new ByteArrayOutputStream();
        new PrettyXmlSerializer(props).writeToStream(node, bostr);

        /*
         * Create the PDF.
         */
        final ITextRenderer renderer = new ITextRenderer();
        renderer.setDocumentFromString(new String(bostr.toByteArray()));
        renderer.layout();

        /*
         *
         */
        renderer.createPDF(ostrPdf);

        /*
         * Finishing up.
         */
        renderer.finishPDF();
        bostr.flush();

        return istrDoc.getBytesRead();
    }

    /**
     *
     * @param nodeRoot
     * @param pageSize
     *            E.g. "A4" or "Letter"
     */
    private void addPageSize(final TagNode nodeRoot, final String pageSize) {

        TagNode nodeHead = nodeRoot.findElementByName("head", false);

        if (nodeHead != null) {
            TagNode style = new TagNode("style");
            Map<String, String> attributes = new HashMap<>();
            attributes.put("type", "text/css");
            style.setAttributes(attributes);
            style.insertChild(0,
                    new ContentNode("@page {size: " + pageSize + ";}"));
            nodeHead.insertChild(0, style);
        }
    }
}
