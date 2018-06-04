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
package org.savapage.core.doc;

import java.io.File;

import org.savapage.core.SpException;
import org.savapage.core.system.CommandExecutor;
import org.savapage.core.system.ICommandExecutor;

/**
 * Converts an XPS file to PDF.
 *
 * @author Datraverse B.V.
 *
 */
public class XpsToPdf extends AbstractDocFileConverter {

    /**
     *
     */
    private static volatile Boolean cachedInstallIndication = null;

    /**
     *
     */
    private static final String XPSTOPDF = "xpstopdf";

    /**
     *
     */
    public XpsToPdf() {
        super(ExecMode.MULTI_THREADED);
    }

    @Override
    protected final File getOutputFile(final File fileIn) {
        return getFileSibling(fileIn, DocContentTypeEnum.PDF);
    }

    @Override
    protected final String getOsCommand(final DocContentTypeEnum contentType,
            final File fileIn, final File fileOut) {

        return XPSTOPDF + " " + fileIn.getAbsolutePath() + " "
                + fileOut.getAbsolutePath() + " 2>/dev/null";
    }

    public static String name() {
        return XPSTOPDF;
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

        final String cmd = "which " + XPSTOPDF;

        ICommandExecutor exec = CommandExecutor.createSimple(cmd);

        try {
            cachedInstallIndication = (exec.executeCommand() == 0);
            return cachedInstallIndication;
        } catch (Exception e) {
            throw new SpException(e);
        }

    }

}
