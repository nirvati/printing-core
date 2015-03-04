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
 *
 * @author Datraverse B.V.
 *
 */
public class OfficeToPdf extends AbstractFileConverter {

    /**
     *
     */
    private static volatile Boolean cachedInstallIndication = null;

    /**
     *
     * @return The name of the office suite.
     */
    public static String name() {
        return "LibreOffice";
    }

    /**
     * For now, we use in {@link ExecMode#SINGLE_THREADED}, since an OpenOffice
     * headless instance is single-threaded. In the future we might scale up to
     * a pool LibreOffice instances.
     */
    public OfficeToPdf() {
        super(ExecMode.SINGLE_THREADED);
    }

    @Override
    protected final File getOutputFile(final File fileIn) {
        return getFileSibling(fileIn, DocContentTypeEnum.PDF);
    }

    @Override
    protected final String getOsCommand(final DocContentTypeEnum contentType,
            final File fileIn, final File fileOut) {

        return "libreoffice --headless --convert-to "
                + DocContent.FILENAME_EXT_PDF + ":"
                + getOoOutputFilterName(contentType) + " "
                + fileIn.getAbsolutePath() + " --outdir " + fileOut.getParent();
    }

    /**
     *
     * @param contentType
     * @return
     */
    private String getOoOutputFilterName(final DocContentTypeEnum contentType) {
        switch (contentType) {

        case RTF:
        case DOC:
        case DOCX:
        case ODT:
        case SXW:
            return "writer_pdf_Export";

        case XLS:
        case XLSX:
        case ODS:
        case SXC:
            return "calc_pdf_Export";

        case PPT:
        case PPTX:
        case ODP:
        case SXI:
            return "impress_pdf_Export";

        default:
            throw new SpException("No LibreOffice output filter "
                    + "found for content type [" + contentType + "]");
        }

    }

    /**
     * Finds out if LibreOffice is installed using the indication from cache,
     * i.e. the result of the last {@link #getLibreOfficeVersion()} call. If the
     * cache is null {@link #getLibreOfficeVersion()} is called ad-hoc to find
     * out.
     *
     * @return {@code true} if installed.
     */
    public static boolean lazyIsInstalled() {
        if (cachedInstallIndication == null) {
            getLibreOfficeVersion();
        }
        return cachedInstallIndication;
    }

    /**
     * Retrieves the LibreOffice version from the system.
     *
     * @return The version string(s) or {@code null} when LibreOffice is not
     *         installed.
     */
    public static String getLibreOfficeVersion() {

        final String cmd = "libreoffice --version";

        final ICommandExecutor exec = CommandExecutor.createSimple(cmd);

        try {

            final int rc = exec.executeCommand();

            cachedInstallIndication = (rc == 0);

            if (!cachedInstallIndication) {
                return null;
            }
            return exec.getStandardOutputFromCommand().toString();

        } catch (Exception e) {
            throw new SpException(e);
        }
    }

}
