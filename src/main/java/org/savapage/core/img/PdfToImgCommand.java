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

/**
 *
 * @author Datraverse B.V.
 *
 */
public interface PdfToImgCommand {

    /**
     * Creates an OS command for creating an (graphic) image of a page in a PDF
     * document.
     *
     * @param pageOrdinal
     *            The zero-based ordinal page number in the PDF document.
     * @param isThumbnail
     *            {@code true} is image is a thumbnail.
     * @param rotate2Apply
     *            The rotation to be applied for this page. If {@code null}, no
     *            rotation is applied.
     * @param pdfFile
     *            The PDF source file.
     * @param imgFile
     *            The image target file.
     * @return The command string.
     */
    String createCommand(final Integer pageOrdinal, final boolean isThumbnail,
            final String rotate2Apply, final String pdfFile,
            final String imgFile);
}
