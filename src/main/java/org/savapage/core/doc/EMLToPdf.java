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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

/**
 * Create a PDF file from {@link DocContentTypeEnum#EML}.
 *
 * @author Rijk Ravestein
 *
 */
public final class EMLToPdf implements IStreamConverter {

    @Override
    public long convert(final DocContentTypeEnum contentType,
            final DocInputStream istrDoc, final OutputStream ostrPdf)
            throws Exception {
        /*
         * Step 1 : EML to HTML.
         */
        final EMLToHtml toHtml = new EMLToHtml();
        final ByteArrayOutputStream ostrHtml = new ByteArrayOutputStream();
        final long length = toHtml.convert(contentType, istrDoc, ostrHtml);

        /*
         * Step 2 : HTML to PDF.
         */
        try (DocInputStream istrHtml = new DocInputStream(
                new ByteArrayInputStream(ostrHtml.toByteArray()))) {

            final IStreamConverter conv;

            if (WkHtmlToPdf.isAvailable()) {
                conv = new WkHtmlToPdf();
            } else {
                conv = new HtmlToPdf();
            }
            conv.convert(contentType, istrHtml, ostrPdf);
        }
        return length;
    }

}
