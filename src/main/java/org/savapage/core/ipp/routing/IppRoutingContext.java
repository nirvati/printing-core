/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2019 Datraverse B.V.
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
package org.savapage.core.ipp.routing;

import java.io.File;
import java.io.IOException;

import org.savapage.core.jpa.IppQueue;

/**
 *
 * @author Rijk Ravestein
 *
 */
public interface IppRoutingContext {

    /**
     * @return Originator IP address.
     */
    String getOriginatorIp();

    /**
     * @return URL path of {@link IppQueue#getUrlPath()}.
     */
    String getUrlPath();

    /**
     * @return The PDF file to print.
     */
    File getPdfToPrint();

    /**
     * Moves new PDF version to PDF file to print.
     *
     * @param newFile
     *            The new version of the PDF file to print.
     * @throws IOException
     *             If error.
     */
    void replacePdfToPrint(File newFile) throws IOException;
}