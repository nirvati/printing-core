/*
 * This file is part of the SavaPage project <http://savapage.org>.
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.core.doc;

import java.io.File;

/**
 * Converts a document data stream to PDF file.
 *
 * @author Rijk Ravestein
 *
 */
public interface IDocStreamConverter extends IDocConverter {

    /**
     * Converts a document data stream to PDF.
     *
     * @param contentType
     *            The content type of the input stream.
     * @param istr
     *            The document input stream.
     * @param filePdf
     *            The PDF file.
     *
     * @return The number of bytes read from the input stream.
     *
     * @throws Exception
     */
    long convert(DocContentTypeEnum contentType, DocInputStream istr,
            File filePdf) throws Exception;

}
