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
package org.savapage.core.img;

import org.savapage.core.doc.DocContent;

/**
 * Command using Poppler.
 * <p>
 * IMPORTANT: older versions of {@code pdftoppm}, like 0.12.4 (c) 2009, do NOT
 * have the {@code -jpeg} switch.
 * </p>
 *
 * @author Datraverse B.V.
 *
 */
public class PopplerPdfToImgCommand implements PdfToImgCommand {

    /**
     *
     */
    private static final int STRINGBUILDER_CAPACITY = 256;

    @Override
    public final String createCommand(final Integer pageOrdinal,
            final boolean isThumbnail, final String rotate2Apply,
            final String pdfFile, final String imgFile) {

        final Integer pageOneBased = pageOrdinal + 1;

        final StringBuilder cmdBuffer =
                new StringBuilder(STRINGBUILDER_CAPACITY);

        cmdBuffer.append("pdftoppm -r 72 -f ").append(pageOneBased)
                .append(" -l ").append(pageOneBased).append(" -scale-to ");

        if (isThumbnail) {
            cmdBuffer.append(ImageUrl.THUMBNAIL_WIDTH);
        } else {
            cmdBuffer.append(ImageUrl.BROWSER_PAGE_WIDTH);
        }

        if (ImageUrl.FILENAME_EXT_IMAGE
                .equalsIgnoreCase(DocContent.FILENAME_EXT_JPG)) {
            cmdBuffer.append(" -jpeg ");
        } else {
            cmdBuffer.append(" -png ");
        }

        cmdBuffer.append("\"").append(pdfFile).append("\"");

        /*
         * Apply rotate?
         */
        if (rotate2Apply == null || rotate2Apply.equals("0")) {
            cmdBuffer.append(" > ");
        } else {
            cmdBuffer.append(" | convert -rotate ").append(rotate2Apply)
                    .append(" - ");
        }
        cmdBuffer.append("\"").append(imgFile).append("\"");

        final String command = cmdBuffer.toString();

        return command;
    }

}
