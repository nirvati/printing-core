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
import java.io.IOException;

import org.apache.commons.io.FilenameUtils;
import org.savapage.core.SpException;

/**
 * Converts a PDF file to grayscale PDF.
 *
 * @author Datraverse B.V.
 *
 */
public class PdfToGrayscale extends AbstractFileConverter {

    /**
     *
     */
    public PdfToGrayscale() {
        super(ExecMode.MULTI_THREADED);
    }

    @Override
    protected final File getOutputFile(final File fileIn) {

        final StringBuilder builder = new StringBuilder(128);

        builder.append(fileIn.getParent()).append(File.separator)
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
            cmd.append("gs -sOutputFile=\"").append(fileOut.getCanonicalPath())
                    .append("\" -sDEVICE=pdfwrite")
                    .append(" -sColorConversionStrategy=Gray")
                    .append(" -sProcessColorModel=DeviceGray")
                    .append(" -dCompatibilityLevel=1.4 \"")
                    .append(fileIn.getCanonicalPath()).append("\" < /dev/null");
        } catch (IOException e) {
            throw new SpException(e.getMessage(), e);
        }

        return cmd.toString();
    }
}
