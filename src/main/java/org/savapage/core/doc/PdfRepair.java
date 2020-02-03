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
 * Tries to repair a corrupted PDF file, embedding all fonts along the way.
 *
 * @see <a href="https://issues.savapage.org/view.php?id=1011">Mantis #1011</a>
 *
 * @author Rijk Ravestein
 *
 */
public final class PdfRepair extends AbstractPdfRepair {

    /**
     * Tries to repair a corrupted PDF file, embedding all fonts along the way.
     */
    public PdfRepair() {
        super();
    }

    /**
     *
     * @param createDir
     *            The directory location of the created file.
     */
    public PdfRepair(final File createDir) {
        super(createDir);
    }

    @Override
    protected String getOsCommand(final DocContentTypeEnum contentType,
            final File fileIn, final File fileOut) {

        final StringBuilder cmd = new StringBuilder(128);

        try {
            cmd.append(SystemInfo.Command.PDFTOCAIRO.cmd()).append(" -pdf \"")
                    .append(fileIn.getCanonicalPath()).append("\" \"")
                    .append(fileOut.getCanonicalPath()).append("\"");
        } catch (IOException e) {
            throw new SpException(e.getMessage(), e);
        }

        return cmd.toString();
    }

    @Override
    protected String getUniqueFileNamePfx() {
        return "-repair";
    }

}
