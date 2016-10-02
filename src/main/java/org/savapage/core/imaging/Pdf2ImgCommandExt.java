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
package org.savapage.core.imaging;

import java.io.File;

/**
 *
 * @author Rijk Ravestein
 *
 */
public interface Pdf2ImgCommandExt extends Pdf2ImgCommand {

    /**
     * Creates an OS command for creating an (graphic) image of a page in a PDF
     * document.
     *
     * @param pdfFile
     *            The PDF source {@link File}.
     * @param landscape
     *            {@code true} if the PDF orientation of the PDF document is
     *            landscape.
     * @param rotation
     *            The PDF rotation the PDF inbox document.
     * @param imgFile
     *            The image target {@link File}.
     * @param pageOrdinal
     *            The zero-based ordinal page number in the PDF document.
     * @param resolution
     *            The resolution (density) in DPI (e.g. 72, 150, 300, 600).
     * @param rotate
     *            The rotation on the PDF inbox document set by the User.
     * @param imgWidth
     *            The image width in pixels. If {@code null} width is calculated
     *            by the command.
     * @return The OS command string.
     */
    String createCommand(File pdfFile, boolean landscape, int rotation,
            File imgFile, int pageOrdinal, int resolution, int rotate,
            Integer imgWidth);

}
