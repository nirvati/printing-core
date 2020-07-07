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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.core.ipp.operation;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * This REQUIRED operation allows a client to request the values of attributes
 * of a Job object and it is almost identical to the Get-Printer-Attributes
 * operation (see section 3.2.5). The only differences are that the operation is
 * directed at a Job object rather than a Printer object, there is no
 * "document-format" operation attribute used when querying a Job object, and
 * the returned attribute group is a set of Job object attributes rather than a
 * set of Printer object attributes.
 *
 * For Jobs, the possible names of attribute groups are:
 *
 * - 'job-template': the subset of the Job Template attributes that apply to a
 * Job object (the first column of the table in Section 4.2) that the
 * implementation supports for Job objects.
 *
 * - 'job-description': the subset of the Job Description attributes specified
 * in Section 4.3 that the implementation supports for Job objects.
 *
 * - 'all': the special group 'all' that includes all attributes that the
 * implementation supports for Job objects.
 *
 * Since a client MAY request specific attributes or named groups, there is a
 * potential that there is some overlap. For example, if a client requests,
 * 'job-name' and 'job-description', the client is actually requesting the
 * "job-name" attribute once by naming it explicitly, and once by inclusion in
 * the 'job-description' group. In such cases, the Printer object NEED NOT
 * return the attribute only once in the response even if it is requested
 * multiple times. The client SHOULD NOT request the same attribute in multiple
 * ways.
 *
 * It is NOT REQUIRED that a Job object support all attributes belonging to a
 * group (since some attributes are OPTIONAL). However it is REQUIRED that each
 * Job object support all these group names.
 *
 * @author Rijk Ravestein
 *
 */
public class IppGetJobAttrOperation extends AbstractIppOperation {

    /**
     *
     */
    private final IppGetJobAttrReq request = new IppGetJobAttrReq();

    /**
     *
     */
    private final IppGetJobAttrRsp response = new IppGetJobAttrRsp();

    @Override
    protected final void process(final InputStream istr,
            final OutputStream ostr) throws IOException {
        request.process(this, istr);
        response.process(this, request, ostr);
    }

}
