/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
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
package org.savapage.core.ipp.operation;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.savapage.core.ipp.attribute.IppAttrValue;
import org.savapage.core.jpa.IppQueue;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class IppGetPrinterAttrOperation extends AbstractIppOperation {

    /**
     *
     */
    public static final String ATTR_GRP_NONE = "none";

    /**
     * The special group 'all' that includes all attributes that the
     * implementation supports for Printer objects.
     */
    public static final String ATTR_GRP_ALL = "all";

    /**
     * Subset of the Job Template attributes that apply to a Printer object (the
     * last two columns of the table in Section 4.2) that the implementation
     * supports for Printer objects.
     */
    public static final String ATTR_GRP_JOB_TPL = "job-template";

    /**
     * Subset of the attributes specified in Section 4.4 that the implementation
     * supports for Printer objects.
     */
    public static final String ATTR_GRP_PRINTER_DESC = "printer-description";

    /**
     *
     */
    public static final String ATTR_GRP_MEDIA_COL_DATABASE =
            "media-col-database";

    /** */
    private final IppGetPrinterAttrReq request;

    /** */
    private final IppGetPrinterAttrRsp response;

    /**
     *
     * @param queue
     *            The requested printer queue.
     */
    public IppGetPrinterAttrOperation(final IppQueue queue) {
        super();
        this.request = new IppGetPrinterAttrReq();
        this.response = new IppGetPrinterAttrRsp(queue);
    }

    /**
     * @return {@link IppAttrValue}.
     */
    public IppAttrValue getRequestedAttributes() {
        return request.getRequestedAttributes();
    }

    @Override
    protected void process(final InputStream istr, final OutputStream ostr)
            throws IOException {
        request.process(this, istr);
        response.process(this, request, ostr);
    }

}
