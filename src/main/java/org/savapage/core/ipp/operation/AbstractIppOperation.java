/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2015 Datraverse B.V.
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
package org.savapage.core.ipp.operation;

import java.io.InputStream;
import java.io.OutputStream;

import org.savapage.core.ipp.encoding.IppEncoder;
import org.savapage.core.jpa.IppQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Datraverse B.V.
 *
 */
public abstract class AbstractIppOperation {

    private final static Logger LOGGER = LoggerFactory
            .getLogger(AbstractIppOperation.class);

    private int versionMajor;
    private int versionMinor;
    private int requestId;

    public int getVersionMajor() {
        return versionMajor;
    }

    public void setVersionMajor(int versionMajor) {
        this.versionMajor = versionMajor;
    }

    public int getVersionMinor() {
        return versionMinor;
    }

    public void setVersionMinor(int versionMinor) {
        this.versionMinor = versionMinor;
    }

    public int getRequestId() {
        return requestId;
    }

    public void setRequestId(int requestId) {
        this.requestId = requestId;
    }

    /**
     *
     * @param reader
     * @param ostr
     * @throws Exception
     */
    abstract void process(final InputStream istr, final OutputStream ostr)
            throws Exception;

    /**
     * Handles an IPP printing request.
     *
     * @param remoteAddr
     *            The client IP address.
     * @param queue
     *            The print queue. Can be {@code null} is no queue matches the
     *            URI.
     * @param requestedQueueUrlPath
     *            The requested URL path.
     * @param istr
     *            The IPP input stream.
     * @param ostr
     *            The IPP output stream.
     * @param hasPrintAccessToQueue
     *            Indicates if client has access to printing. When {@code false}
     *            , printing is NOT allowed.
     * @param trustedIppClientUserId
     *            The trusted user id on the IPP client. If {@code null} there
     *            is NO trusted user.
     * @return The {@link IppOperationId}, or {@code null} when requested
     *         operation is not supported.
     * @throws Exception
     *             When an error occurred.
     */
    public static IppOperationId handle(final String remoteAddr,
            final IppQueue queue, final String requestedQueueUrlPath,
            final InputStream istr, final OutputStream ostr,
            final boolean hasPrintAccessToQueue,
            final String trustedIppClientUserId) throws Exception {

        // -----------------------------------------------
        // | version-number (2 bytes - required)
        // -----------------------------------------------
        final int versionMajor = istr.read();
        final int versionMinor = istr.read();

        // -----------------------------------------------
        // | operation-id (request) or status-code (response)
        // | (2 bytes - required)
        // -----------------------------------------------
        final int operationId = IppEncoder.readInt16(istr);

        // -----------------------------------------------
        // | request-id (4 bytes - required)
        // -----------------------------------------------
        final int requestId = IppEncoder.readInt32(istr);

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("+---------------------------------"
                    + "-------------------------------------+");
            LOGGER.trace("| " + IppOperationId.asEnum(operationId).toString());
            LOGGER.trace("+---------------------------"
                    + "-------------------------------------------+");
        }
        /*
         *
         */
        final AbstractIppOperation operation;

        if (operationId == IppOperationId.PRINT_JOB.asInt()) {
            operation =
                    new IppPrintJobOperation(remoteAddr, queue,
                            hasPrintAccessToQueue, trustedIppClientUserId);
        } else if (operationId == IppOperationId.VALIDATE_JOB.asInt()) {
            operation =
                    new IppValidateJobOperation(remoteAddr, queue,
                            requestedQueueUrlPath, hasPrintAccessToQueue,
                            trustedIppClientUserId);
        } else if (operationId == IppOperationId.GET_PRINTER_ATTR.asInt()) {
            operation = new IppGetPrinterAttrOperation();
        } else if (operationId == IppOperationId.GET_JOBS.asInt()) {
            operation = new IppGetJobsOperation();
        } else if (operationId == IppOperationId.CANCEL_JOB.asInt()) {
            operation = new IppCancelJobOperation();
        } else if (operationId == IppOperationId.GET_JOB_ATTR.asInt()) {
            operation = new IppGetJobAttrOperation();
        } else {
            operation = null;
        }

        final IppOperationId ippOperationId;

        if (operation == null) {

            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("operationId [" + operationId
                        + "] is NOT supported");
            }

            ippOperationId = null;

        } else {

            // Set attributes.
            operation.setVersionMajor(versionMajor);
            operation.setVersionMinor(versionMinor);
            operation.setRequestId(requestId);

            // Process the IPP printing request.
            operation.process(istr, ostr);

            ippOperationId = IppOperationId.asEnum(operationId);
        }

        return ippOperationId;
    }
}
