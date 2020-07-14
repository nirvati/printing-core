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
import java.util.ArrayList;
import java.util.List;

import org.savapage.core.ipp.attribute.IppAttrGroup;
import org.savapage.core.ipp.attribute.IppAttrValue;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class IppCancelMyJobsOperation extends AbstractIppOperation {

    /** */
    private static final class IppCancelMyJobsRequest
            extends AbstractIppRequest {

        @Override
        public void process(final AbstractIppOperation operation,
                final InputStream istr) throws IOException {
            this.readAttributes(operation, istr);
        }

    }

    /** */
    private static final class IppCancelMyJobsResponse
            extends AbstractIppResponse {

        /** */
        IppCancelMyJobsResponse() {
        }

        /**
         *
         * @param operation
         *            IPP operation.
         * @param request
         *            IPP request.
         * @param ostr
         *            IPP output stream.
         * @throws IOException
         *             If error.
         */
        public void process(final IppCancelMyJobsOperation operation,
                final IppCancelMyJobsRequest request, final OutputStream ostr)
                throws IOException {

            IppStatusCode ippStatusCode =
                    this.determineStatusCode(operation, request);

            final List<IppAttrGroup> attrGroups = new ArrayList<>();

            /**
             * Group 1: Operation Attributes
             */
            attrGroups.add(this.createOperationGroup());

            if (ippStatusCode == IppStatusCode.OK) {
                // All submitted jobs are inherently completed, so this cancel
                // operation is not possible.
                ippStatusCode = IppStatusCode.CLI_NOTPOS;
            }

            this.writeHeaderAndAttributes(operation, ippStatusCode, attrGroups,
                    ostr, request.getAttributesCharset());
        }

    }

    /** */
    private final IppCancelMyJobsRequest request;
    /** */
    private final IppCancelMyJobsResponse response;

    /** */
    public IppCancelMyJobsOperation() {
        super();
        this.request = new IppCancelMyJobsRequest();
        this.response = new IppCancelMyJobsResponse();
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
