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
package org.savapage.core.doc;

import java.io.File;

import org.savapage.core.SpException;

/**
 * A {@link ExecMode#SINGLE_THREADED} implementation of Libre Office PDF
 * conversion.
 *
 * @author Rijk Ravestein
 *
 */
public class OfficeToPdf extends AbstractFileConverter {

    /**
     * Constructor.
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

}
