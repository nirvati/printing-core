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

import org.savapage.core.SpException;
import org.savapage.core.system.CommandExecutor;
import org.savapage.core.system.ICommandExecutor;
import org.savapage.core.system.SystemInfo;

/**
 * Converts an XPS file to PDF.
 *
 * @author Rijk Ravestein
 *
 */
public final class XpsToPdf extends AbstractDocFileConverter {

    /**
     *
     */
    private static volatile Boolean cachedInstallIndication = null;

    /**
     *
     */
    public XpsToPdf() {
        super(ExecMode.MULTI_THREADED);
    }

    @Override
    protected ExecType getExecType() {
        return ExecType.ADVANCED;
    }

    @Override
    protected File getOutputFile(final File fileIn) {
        return getFileSibling(fileIn, DocContentTypeEnum.PDF);
    }

    @Override
    protected String getOsCommand(final DocContentTypeEnum contentType,
            final File fileIn, final File fileOut) {

        return SystemInfo.Command.XPSTOPDF.cmdLineExt(fileIn.getAbsolutePath(),
                fileOut.getAbsolutePath());
    }

    /**
     *
     * @return name.
     */
    public static String name() {
        return SystemInfo.Command.XPSTOPDF.cmd();
    }

    /**
     * Finds out if {@code xpstopdf} is installed using the indication from
     * cache, i.e. the result of the last {@link #isInstalled()} call. If the
     * cache is null {@link #isInstalled()} is called ad-hoc to find out.
     *
     * @return {@code true} if installed.
     */
    public static boolean lazyIsInstalled() {

        if (cachedInstallIndication == null) {
            return isInstalled();
        }
        return cachedInstallIndication;
    }

    /**
     * Finds out if {@code xpstopdf} is installed by executing a host command.
     *
     * @return {@code true} if installed.
     */
    public static boolean isInstalled() {

        final String cmd = SystemInfo.Command.WHICH
                .cmdLine(SystemInfo.Command.XPSTOPDF.cmd());

        ICommandExecutor exec = CommandExecutor.createSimple(cmd);

        try {
            cachedInstallIndication = (exec.executeCommand() == 0);
            return cachedInstallIndication;
        } catch (Exception e) {
            throw new SpException(e);
        }

    }

    @Override
    public boolean notifyStdOutMsg() {
        return this.hasStdout();
    }

}
