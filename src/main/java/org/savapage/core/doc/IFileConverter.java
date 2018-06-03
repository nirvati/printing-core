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

import org.savapage.core.UnavailableException;

/**
 * Converts a file to PDF.
 *
 * @author Rijk Ravestein
 *
 */
public interface IFileConverter extends IDocConverter {

    /**
     * Converts a file to PDF. Unless implemented otherwise, the PDF file is
     * created in the directory of the input file, and has the same basename.
     * <p>
     * NOTE: When a exception is thrown the created PDF (if present) is deleted.
     * </p>
     *
     * @param contentType
     *            The content type of the input stream.
     * @param file
     *            The input file.
     *
     * @return The PDF output file.
     *
     * @throws DocContentToPdfException
     *             When anything goes wrong.
     * @throws UnavailableException
     *             When convert service is (temporary) unavailable.
     */
    File convert(DocContentTypeEnum contentType, File file)
            throws DocContentToPdfException, UnavailableException;

}
