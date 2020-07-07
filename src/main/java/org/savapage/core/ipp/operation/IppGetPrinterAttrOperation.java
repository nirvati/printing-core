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
 * This REQUIRED operation allows a client to request the values of the
 * attributes of a Printer object. In the request, the client supplies the set
 * of Printer attribute names and/or attribute group names in which the
 * requester is interested. In the response, the Printer object returns a
 * corresponding attribute set with the appropriate attribute values filled in.
 *
 * <p>
 * For Printer objects, the possible names of attribute groups are:
 * <ul>
 * <li>'job-template': the subset of the Job Template attributes that apply to a
 * Printer object (the last two columns of the table in Section 4.2) that the
 * implementation supports for Printer objects.</li>
 *
 * <li>'printer-description': the subset of the attributes specified in Section
 * 4.4 that the implementation supports for Printer objects.</li>
 *
 * <li>'all': the special group 'all' that includes all attributes that the
 * implementation supports for Printer objects.</li>
 *
 * </ul>
 * </p>
 *
 * @author Rijk Ravestein
 *
 */
public class IppGetPrinterAttrOperation extends AbstractIppOperation {

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

    /**
     *
     */
    private final IppGetPrinterAttrReq request = new IppGetPrinterAttrReq();

    /**
     *
     */
    private final IppGetPrinterAttrRsp response;

    public IppGetPrinterAttrOperation() {
        super();
        this.response = new IppGetPrinterAttrRsp(null);
    }

    /**
     *
     * @param queue
     *            The requested printer queue.
     */
    public IppGetPrinterAttrOperation(final IppQueue queue) {
        super();
        this.response = new IppGetPrinterAttrRsp(queue);
    }

    /**
     * @return {@link IppAttrValue}.
     */
    public IppAttrValue getRequestedAttributes() {
        return request.getRequestedAttributes();
    }

    /*
     * Since a client MAY request specific attributes or named groups, there is
     * a potential that there is some overlap. For example, if a client
     * requests, 'printer-name' and 'all', the client is actually requesting the
     * "printer-name" attribute twice: once by naming it explicitly, and once by
     * inclusion in the 'all' group. In such cases, the Printer object NEED NOT
     * return each attribute only once in the response even if it is requested
     * multiple times. The client SHOULD NOT request the same attribute in
     * multiple ways.
     *
     * It is NOT REQUIRED that a Printer object support all attributes belonging
     * to a group (since some attributes are OPTIONAL). However, it is REQUIRED
     * that each Printer object support all group names.
     */

    @Override
    protected final void process(final InputStream istr,
            final OutputStream ostr) throws IOException {
        request.process(this, istr);
        response.process(this, request, ostr);
    }

}
