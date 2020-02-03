/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2020 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: 2011-2020 Datraverse B.V. <info@datraverse.com>
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
import java.io.IOException;

import org.savapage.core.SpException;
import org.savapage.core.system.SystemInfo;

/**
 * Uses Ghostscript to /prepress a PDF file, this will EmbedAllFonts by default.
 * <p>
 * The major difference between {@code /print} and {@code /prepress} is
 * "CannotEmbedFontPolicy", i.e. the policy Distiller uses if it cannot find, or
 * cannot embed, a font. {@code /print} has value "Warning" (Distiller displays
 * a warning and continues) and {@code /prepress} has value "Error" (Distiller
 * quits distilling the current job).
 * </p>
 * <p>
 * <b>However: warnings and errors are written to stdout and gs returns with
 * {@code rc == 0}.</b>
 * </p>
 * <p>
 * For example:
 *
 * <pre>
 * Can't find CMap Identity-UTF16-H building a CIDDecoding resource.
 *  **** Error: can't process embedded font stream,
 *       attempting to load the font using its name.
 *              Output may be incorrect.
 * </pre>
 * </p>
 *
 * @author Rijk Ravestein
 *
 */
public final class PdfToPrePress extends AbstractPdfRepair {

    /**
     *
     */
    public PdfToPrePress() {
        super();
    }

    /**
     *
     * @param createDir
     *            The directory location of the created file.
     */
    public PdfToPrePress(final File createDir) {
        super(createDir);
    }

    @Override
    protected String getOsCommand(final DocContentTypeEnum contentType,
            final File fileIn, final File fileOut) {

        final StringBuilder cmd = new StringBuilder(128);

        try {
            cmd.append(SystemInfo.Command.GS.cmd()).append(" -sOutputFile=\"")
                    .append(fileOut.getCanonicalPath())
                    .append("\" -sDEVICE=pdfwrite -q -dNOPAUSE -dBATCH") //
                    .append(" -dPDFSETTINGS=/prepress") //
                    .append(" \"").append(fileIn.getCanonicalPath())
                    .append("\"");
        } catch (IOException e) {
            throw new SpException(e.getMessage(), e);
        }

        return cmd.toString();
    }

    @Override
    protected String getUniqueFileNamePfx() {
        return "-prepress";
    }

}
