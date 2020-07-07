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
package org.savapage.core.doc;

import java.io.File;
import java.nio.file.Paths;

import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.ServerPathEnum;
import org.savapage.core.system.SystemInfo;

/**
 * Encapsulates {@code cupsfilter} command to convert file to PDF.
 *
 * @author Rijk Ravestein
 *
 */
public abstract class CUPSFilterToPdf extends AbstractDocFileConverter {

    /**
     * MIME type of input file.
     */
    private final String mimetype;

    /**
     *
     * @param contentType
     *            Content type of input file.
     */
    protected CUPSFilterToPdf(final DocContentTypeEnum contentType) {
        super(ExecMode.MULTI_THREADED);
        this.mimetype = DocContent.getMimeType(contentType);
    }

    @Override
    protected final ExecType getExecType() {
        return ExecType.ADVANCED;
    }

    @Override
    protected final File getOutputFile(final File fileIn) {
        return getFileSibling(fileIn, DocContentTypeEnum.PDF);
    }

    /**
     * TODO.
     *
     * @return PPD file.
     */
    private File getPPD() {
        return Paths
                .get(ConfigManager.getServerHome(),
                        ServerPathEnum.CLIENT.getPath(), "SAVAPAGE.ppd")
                .toFile();
    }

    @Override
    protected final String getOsCommand(final DocContentTypeEnum contentType,
            final File fileIn, final File fileOut) {
        final String cmd = SystemInfo.Command.CUPSFILTER.cmdLineExt("-p",
                this.getPPD().getAbsolutePath(), "-i", this.mimetype,
                fileIn.getAbsolutePath(), ">", fileOut.getAbsolutePath());
        return cmd;
    }

    @Override
    protected final boolean reportStderr() {
        /*
         * `cupsfilter` writes DEBUG messages to stderr (!) How to suppress
         * them? For now, ignore them.
         */
        return false;
    }

    @Override
    public final boolean notifyStdOutMsg() {
        return false;
    }

}
