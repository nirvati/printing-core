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
import java.nio.file.Paths;

import org.apache.commons.io.FilenameUtils;

/**
 *
 * @author Rijk Ravestein
 *
 */
public abstract class AbstractPdfConverter {

    /**
     * The directory location of the created file (can be {@code null}).
     */
    private final File createHome;

    /**
     *
     */
    protected AbstractPdfConverter() {
        this.createHome = null;
    }

    /**
     *
     * @param createDir
     *            The directory location of the created file.
     */
    protected AbstractPdfConverter(final File createDir) {
        this.createHome = createDir;
    }

    /**
     *
     * @return A unique suffix to type the kind of PDF convert.
     */
    protected abstract String getOutputFileSfx();

    /**
     * Gets PDF output file.
     *
     * @param fileIn
     *            The PDF input file.
     * @return The file.
     */
    protected final File getOutputFile(final File fileIn) {

        final String homeDir;

        if (this.createHome == null) {
            homeDir = fileIn.getParent();
        } else {
            homeDir = this.createHome.getAbsolutePath();
        }

        final StringBuilder fileName = new StringBuilder(128);
        fileName.append(FilenameUtils.getBaseName(fileIn.getAbsolutePath()))
                .append('-').append(this.getOutputFileSfx()).append('.')
                .append(DocContent.getFileExtension(DocContentTypeEnum.PDF));

        return Paths.get(homeDir, fileName.toString()).toFile();
    }

}
