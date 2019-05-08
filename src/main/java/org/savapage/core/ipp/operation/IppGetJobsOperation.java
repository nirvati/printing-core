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
package org.savapage.core.ipp.operation;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 3.2.6 Get-Jobs Operation
 *
 * This REQUIRED operation allows a client to retrieve the list of Job objects
 * belonging to the target Printer object. The client may also supply a list of
 * Job attribute names and/or attribute group names. A group of Job object
 * attributes will be returned for each Job object that is returned.
 *
 * This operation is similar to the Get-Job-Attributes operation, except that
 * this Get-Jobs operation returns attributes from possibly more than one
 * object.
 *
 */
public class IppGetJobsOperation extends AbstractIppOperation {

    /**
     *
     */
    private final IppGetJobsReq request = new IppGetJobsReq();

    /**
     *
     */
    private final IppGetJobsRsp response = new IppGetJobsRsp();

    @Override
    protected final void process(final InputStream istr,
            final OutputStream ostr) throws IOException {
        request.process(istr);
        response.process(this, request, ostr);
    }

}
