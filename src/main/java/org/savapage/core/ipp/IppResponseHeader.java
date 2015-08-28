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
package org.savapage.core.ipp;

import java.io.IOException;
import java.io.InputStream;

import org.savapage.core.ipp.encoding.IppEncoder;
import org.savapage.core.ipp.operation.IppStatusCode;

/**
 *
 * @author Datraverse B.V.
 *
 */
public final class IppResponseHeader {

    private int versionMajor;
    private int versionMinor;
    private int requestId;
    private IppStatusCode statusCode;

    /**
     * Reads header of an IPP response.
     *
     * @param istr
     *            The input stream.
     * @return The IPP status-code.
     * @throws IOException
     *             When IO errors.
     */
    public IppStatusCode read(final InputStream istr) throws IOException {

        // -----------------------------------------------
        // | version-number (2 bytes - required)
        // -----------------------------------------------
        setVersionMajor(istr.read());
        setVersionMinor(istr.read());

        // -----------------------------------------------
        // | operation-id (request) or status-code (response)
        // | (2 bytes - required)
        // -----------------------------------------------
        statusCode = IppStatusCode.asEnum(IppEncoder.readInt16(istr));

        // -----------------------------------------------
        // | request-id (4 bytes - required)
        // -----------------------------------------------
        setRequestId(IppEncoder.readInt32(istr));

        return statusCode;
    }

    /**
     * @return The IPP major version.
     */
    public int getVersionMajor() {
        return versionMajor;
    }

    public IppStatusCode getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(IppStatusCode statusCode) {
        this.statusCode = statusCode;
    }

    /**
     *
     * @param versionMajor
     *            The IPP major version.
     */
    public void setVersionMajor(int versionMajor) {
        this.versionMajor = versionMajor;
    }

    /**
     * @return The IPP minor version.
     */
    public int getVersionMinor() {
        return versionMinor;
    }

    /**
     *
     * @param versionMinor
     *            The IPP minor version.
     */
    public void setVersionMinor(int versionMinor) {
        this.versionMinor = versionMinor;
    }

    public int getRequestId() {
        return requestId;
    }

    public void setRequestId(int requestId) {
        this.requestId = requestId;
    }

}
