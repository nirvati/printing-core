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
import java.util.ArrayList;
import java.util.List;

import org.savapage.core.ipp.attribute.IppAttrGroup;
import org.savapage.core.jpa.IppQueue;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class IppIdentifyPrinterOperation extends AbstractIppOperation {

    /** */
    private static final class IppIdentifyPrinterRequest
            extends AbstractIppRequest {

        @Override
        public void process(final AbstractIppOperation operation,
                final InputStream istr) throws IOException {
            this.readAttributes(operation, istr);
        }

    }

    /** */
    private static final class IppIdentifyPrinterResponse
            extends AbstractIppResponse {

        /**
         * @param queue
         *            The requested printer queue (can be {@code null});
         */
        IppIdentifyPrinterResponse(final IppQueue queue) {
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
        public void process(final IppIdentifyPrinterOperation operation,
                final IppIdentifyPrinterRequest request,
                final OutputStream ostr) throws IOException {

            final IppStatusCode ippStatusCode =
                    this.determineStatusCode(operation, request);

            final List<IppAttrGroup> attrGroups = new ArrayList<>();

            /**
             * Group 1: Operation Attributes
             */
            attrGroups.add(this.createOperationGroup());

            if (ippStatusCode == IppStatusCode.OK) {
                // TODO
            }

            this.writeHeaderAndAttributes(operation, ippStatusCode, attrGroups,
                    ostr, request.getAttributesCharset());
        }

    }

    /** */
    private final IppIdentifyPrinterRequest request;
    /** */
    private final IppIdentifyPrinterResponse response;

    /**
     *
     * @param queue
     *            The requested printer queue.
     */
    public IppIdentifyPrinterOperation(final IppQueue queue) {
        super();
        this.request = new IppIdentifyPrinterRequest();
        this.response = new IppIdentifyPrinterResponse(queue);
    }

    @Override
    protected void process(final InputStream istr, final OutputStream ostr)
            throws IOException {
        request.process(this, istr);
        response.process(this, request, ostr);
    }

}
