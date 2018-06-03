/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2018 Datraverse B.V.
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
import java.io.IOException;

import org.apache.commons.io.FilenameUtils;
import org.savapage.core.SpException;

/**
 * Converts a PDF file to grayscale PDF.
 *
 * @author Rijk Ravestein
 *
 */
public class PdfToGrayscale extends AbstractFileConverter {

    /**
     * The directory location of the created file (can be {@code null}).
     */
    private final File createHome;

    /**
     *
     */
    public PdfToGrayscale() {
        super(ExecMode.MULTI_THREADED);
        this.createHome = null;
    }

    /**
     *
     * @param createDir
     *            The directory location of the created file.
     */
    public PdfToGrayscale(final File createDir) {
        super(ExecMode.MULTI_THREADED);
        this.createHome = createDir;
    }

    @Override
    protected final File getOutputFile(final File fileIn) {

        final StringBuilder builder = new StringBuilder(128);

        if (this.createHome == null) {
            builder.append(fileIn.getParent());
        } else {
            builder.append(this.createHome.getAbsolutePath());
        }

        builder.append(File.separator)
                .append(FilenameUtils.getBaseName(fileIn.getAbsolutePath()))
                .append("-grayscale.")
                .append(DocContent.getFileExtension(DocContentTypeEnum.PDF));

        return new File(builder.toString());
    }

    @Override
    protected final String getOsCommand(final DocContentTypeEnum contentType,
            final File fileIn, final File fileOut) {

        final StringBuilder cmd = new StringBuilder(128);

        try {
            /*
             * See #598
             */
            cmd.append("gs -sOutputFile=\"").append(fileOut.getCanonicalPath())
                    .append("\" -sDEVICE=pdfwrite -dNOPAUSE -dBATCH")
                    .append(" -sColorConversionStrategy=Gray")
                    .append(" -sProcessColorModel=DeviceGray")
                    // Needed for gs 9.10
                    // http://bugs.ghostscript.com/show_bug.cgi?id=694608
                    .append(" -dPDFUseOldCMS=false")
                    //
                    .append(" -dCompatibilityLevel=1.4 \"")
                    .append(fileIn.getCanonicalPath()).append("\" < /dev/null");
        } catch (IOException e) {
            throw new SpException(e.getMessage(), e);
        }

        return cmd.toString();
    }
}
