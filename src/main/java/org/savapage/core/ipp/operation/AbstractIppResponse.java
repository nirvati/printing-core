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
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.List;

import org.savapage.core.ipp.attribute.IppAttr;
import org.savapage.core.ipp.attribute.IppAttrGroup;
import org.savapage.core.ipp.attribute.IppAttrValue;
import org.savapage.core.ipp.attribute.IppDictOperationAttr;
import org.savapage.core.ipp.attribute.syntax.IppCharset;
import org.savapage.core.ipp.attribute.syntax.IppNaturalLanguage;
import org.savapage.core.ipp.encoding.IppDelimiterTag;
import org.savapage.core.ipp.encoding.IppEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rijk Ravestein
 *
 */
public abstract class AbstractIppResponse {

    /**
     *
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(AbstractIppResponse.class);

    /**
     * Determines IPP status code for this IPP Response.
     *
     * @param operation
     *            IPP operation.
     * @param request
     *            IPP request.
     * @return IppStatusCode.
     */
    protected IppStatusCode determineStatusCode(
            final AbstractIppOperation operation,
            final AbstractIppRequest request) {

        if (!operation.isRequestIdValid() || !request.areOperationAttrValid()) {
            return IppStatusCode.CLI_BADREQ;
        } else if (!operation.isIPPVersionSupported()) {
            return IppStatusCode.SRV_BADVER;
        }
        return IppStatusCode.OK;
    }

    /**
     * Creates Group with standard Operation attributes.
     *
     * @return {@link IppAttrGroup}.
     */
    protected final IppAttrGroup createOperationGroup() {

        IppAttrValue value = null;
        IppAttr attr = null;

        final IppAttrGroup group =
                new IppAttrGroup(IppDelimiterTag.OPERATION_ATTR);

        attr = new IppAttr(IppDictOperationAttr.ATTR_ATTRIBUTES_CHARSET,
                new IppCharset());
        value = new IppAttrValue(attr);
        value.addValue("utf-8");
        group.addAttribute(value);

        attr = new IppAttr(IppDictOperationAttr.ATTR_ATTRIBUTES_NATURAL_LANG,
                new IppNaturalLanguage());
        value = new IppAttrValue(attr);
        value.addValue("en-us");
        group.addAttribute(value);

        return group;
    }

    /**
     * @param operation
     *            IPP operation.
     * @param status
     *            Status.
     * @param attrGroups
     *            IPP attribute groups.
     * @param ostr
     *            IPP output stream.
     * @param charset
     *            Character set.
     * @throws IOException
     *             If error.
     */
    protected final void write(final AbstractIppOperation operation,
            final IppStatusCode status, final List<IppAttrGroup> attrGroups,
            final OutputStream ostr, final Charset charset) throws IOException {

        writeHeaderAndAttributes(operation, status, attrGroups, ostr, charset);
    }

    /**
     * @param operation
     *            IPP operation.
     * @param status
     *            Status.
     * @param ostr
     *            IPP output stream.
     * @throws IOException
     *             If error.
     */
    protected final void writeHeader(final AbstractIppOperation operation,
            final IppStatusCode status, final OutputStream ostr)
            throws IOException {

        ostr.write(operation.getVersionMajor());
        ostr.write(operation.getVersionMinor());
        IppEncoder.writeInt16(ostr, status.asInt());
        IppEncoder.writeInt32(ostr, operation.getRequestId());
    }

    /**
     * @param operation
     *            IPP operation.
     * @param status
     *            Status.
     * @param attrGroups
     *            IPP attribute groups.
     * @param ostr
     *            IPP output stream.
     * @param charset
     *            Character set.
     * @throws IOException
     *             If error.
     */
    protected final void writeHeaderAndAttributes(
            final AbstractIppOperation operation, final IppStatusCode status,
            final List<IppAttrGroup> attrGroups, final OutputStream ostr,
            final Charset charset) throws IOException {

        Writer traceLog = null;

        if (LOGGER.isTraceEnabled()) {
            traceLog = new StringWriter();
        }

        try {
            // Header
            this.writeHeader(operation, status, ostr);

            // Attributes
            IppEncoder.writeAttributes(attrGroups, ostr, charset, traceLog);

            // End-of-attr
            ostr.write(IppDelimiterTag.END_OF_ATTR.asInt());

        } finally {
            if (traceLog != null) {
                final String header = String.format(
                        "HEADER :" + "\n  IPP/%d.%d" + "\n  request-id [%d]"
                                + "\n  status: %s [%d]",
                        operation.getVersionMajor(),
                        operation.getVersionMinor(), operation.getRequestId(),
                        status.toString(), status.asInt());

                LOGGER.trace("\n+-----------------------------------+"
                        + "\n| Response: " + this.getClass().getSimpleName()
                        + "\n+-----------------------------------+\n" + header
                        + traceLog + "\n+-----------------------------------+");
            }
        }
    }

}
