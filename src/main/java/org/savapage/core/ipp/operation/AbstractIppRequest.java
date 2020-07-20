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
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.savapage.core.ipp.attribute.IppAttrGroup;
import org.savapage.core.ipp.attribute.IppAttrValue;
import org.savapage.core.ipp.attribute.IppDictOperationAttr;
import org.savapage.core.ipp.encoding.IppDelimiterTag;
import org.savapage.core.ipp.encoding.IppEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rijk Ravestein
 *
 */
public abstract class AbstractIppRequest {

    /** */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(AbstractIppRequest.class);

    /** */
    private static final Charset MY_CHARSET = Charset.forName("US-ASCII");

    /**
     *
     */
    private List<IppAttrGroup> attrGroups = new ArrayList<>();

    /**
     * Processes IPP stream.
     *
     * @param operation
     *            IPP operation.
     * @param istr
     *            The stream to process.
     * @throws IOException
     *             If IO error.
     */
    abstract void process(AbstractIppOperation operation, InputStream istr)
            throws IOException;

    /**
     * Generic read of attributes.
     *
     * @param operation
     *            Requesting IPP operation.
     * @param istr
     *            IPP input stream.
     * @throws IOException
     *             If error occurred.
     */
    protected void readAttributes(final AbstractIppOperation operation,
            final InputStream istr) throws IOException {

        Writer traceLog = null;

        if (LOGGER.isTraceEnabled()) {
            traceLog = new StringWriter();
        }

        attrGroups = IppEncoder.readAttributes(istr, traceLog);

        if (traceLog != null) {
            final String header = String.format(
                    "HEADER :" + "\n  IPP/%d.%d" + "\n  request-id [%d]",
                    operation.getVersionMajor(), operation.getVersionMinor(),
                    operation.getRequestId());

            LOGGER.trace("\n+----------------------------------------+"
                    + "\n| Request: " + this.getClass().getSimpleName()
                    + "\n+----------------------------------------+\n" + header
                    + traceLog.toString());
        }
    }

    public List<IppAttrGroup> getAttrGroups() {
        return attrGroups;
    }

    public final void setAttrGroups(final List<IppAttrGroup> groups) {
        this.attrGroups = groups;
    }

    /**
     *
     * @return
     */
    public final Charset getAttributesCharset() {
        return MY_CHARSET;
    }

    /**
     * @return {@code null} when not present.
     */
    public final IppAttrValue getRequestedAttributes() {
        return this
                .getAttrValue(IppDictOperationAttr.ATTR_REQUESTED_ATTRIBUTES);
    }

    /**
     * @param name
     *            IPP attribute name.
     * @return Attribute value, or {@code null} when attribute is not present.
     */
    public final IppAttrValue getAttrValue(final String name) {
        IppAttrValue value = null;
        for (IppAttrGroup group : attrGroups) {
            value = group.getAttrValue(name);
            if (value != null) {
                break;
            }
        }
        return value;
    }

    /**
     * @param name
     *            IPP attribute name.
     * @return Single attribute value, or {@code null} when attribute is not present.
     */
    private String getAttrSingleValue(final String name) {
        final IppAttrValue val = getAttrValue(name);
        if (val != null && val.getValues().size() == 1) {
            return val.getValues().get(0);
        }
        return null;
    }

    /**
     * @return the IPP attribute "requesting-user-name" or {@code null} when not
     *         found.
     */
    public final String getRequestingUserName() {
        return this.getAttrSingleValue(
                IppDictOperationAttr.ATTR_REQUESTING_USER_NAME);
    }

    /**
     * @return the IPP attribute "printer-uri" or {@code null} when not found.
     */
    public final String getPrinterURI() {
        return this.getAttrSingleValue(IppDictOperationAttr.ATTR_PRINTER_URI);
    }

    /**
     * RFC 8011 4.1.4 The "attributes-charset" attribute MUST be the first
     * attribute in the group, and the "attributes-natural-language" attribute
     * MUST be the second attribute in the group.
     *
     * @return {@code true} operation attributes are present and valid.
     */
    public boolean areOperationAttrValid() {

        String charset = null;
        String language = null;

        for (IppAttrGroup group : attrGroups) {

            if (group.getDelimiterTag()
                    .equals(IppDelimiterTag.OPERATION_ATTR)) {

                for (final IppAttrValue value : group.getAttributes()) {
                    final String kw = value.getAttribute().getKeyword();
                    if (kw.equals(
                            IppDictOperationAttr.ATTR_ATTRIBUTES_CHARSET)) {
                        if (language != null) {
                            return false;
                        }
                        charset = value.getSingleValue();
                    } else if (kw.equals(
                            IppDictOperationAttr.ATTR_ATTRIBUTES_NATURAL_LANG)) {
                        if (charset == null) {
                            return false;
                        }
                        language = value.getSingleValue();

                    }
                }
            }
        }
        return charset != null && language != null;
    }

}
