/*
 * This file is part of the SavaPage project <https:/www.savapage.org>.
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
package org.savapage.core.imaging;

import org.savapage.core.pdf.PdfPageRotateHelper;
import org.savapage.core.system.SystemInfo;

/**
 * @deprecated Use {@link Pdf2ImgCairoCmd}. See Mantis #326, #1079.
 *             <p>
 *             Command using GhostScript.
 *             <p>
 *
 * @author Rijk Ravestein
 *
 */
@Deprecated
public final class Pdf2PngGhostScriptCmd implements Pdf2ImgCommand {

    /** */
    private static final int STRINGBUILDER_CAPACITY = 256;

    @Override
    public String createCommand(final CreateParms parms) {
        final int pageOneBased = parms.getPageOrdinal() + 1;

        final StringBuilder cmdBuffer =
                new StringBuilder(STRINGBUILDER_CAPACITY);

        cmdBuffer.append(SystemInfo.Command.GS.cmd())
                .append(" -dNumRenderingThreads=4 -sDEVICE=pngalpha")
                .append(" -dNOPAUSE -dFirstPage=").append(pageOneBased)
                .append(" -dLastPage=").append(pageOneBased)
                .append(" -sOutputFile=- -r").append(parms.getResolution())
                .append(" -q \"").append(parms.getPdfFile().getAbsolutePath())
                .append("\" -c quit");

        /*
         * Apply rotate?
         */
        if (parms.getRotate() == PdfPageRotateHelper.PDF_ROTATION_0
                .intValue()) {
            cmdBuffer.append(" > ");
        } else {
            cmdBuffer.append(" | ").append(SystemInfo.Command.CONVERT.cmd())
                    .append(" -rotate ").append(parms.getRotate())
                    .append(" - ");
        }

        cmdBuffer.append("\"").append(parms.getImgFile().getAbsolutePath())
                .append("\"");

        final String command = cmdBuffer.toString();

        return command;
    }

}
