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
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.savapage.core.ipp.attribute.IppAttrGroup;
import org.savapage.core.ipp.attribute.IppAttrValue;
import org.savapage.core.ipp.attribute.IppDictOperationAttr;
import org.savapage.core.ipp.encoding.IppEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rijk Ravestein
 *
 */
public abstract class AbstractIppRequest extends IppMessageMixin {

    /** */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(AbstractIppRequest.class);

    /** */
    private static final Charset MY_CHARSET = Charset.forName("US-ASCII");

    /**
     *
     */
    private List<IppAttrGroup> attrGroups = new ArrayList<>();

    /*
     * The only operations that support supplying the document data within an
     * operation request are Print-Job and Send- Document.
     */

    /*
     * When an IPP object receives a request to perform an operation it does not
     * support, it returns the ’server-error-operation-not-supported’ status
     * code (see section 13.1.5.2). An IPP object is non-conformant if it does
     * not support a REQUIRED operation.
     */

    /*
     * The operation target attributes are REQUIRED operation attributes that
     * MUST be included in every operation request. Like the charset and natural
     * language attributes (see section 3.1.4), the operation target attributes
     * are specially ordered operation attributes. In all cases, the operation
     * target attributes immediately follow the "attributes-charset" and
     * "attributes-natural-language" attributes within the operation attribute
     * group, however the specific ordering rules are:
     *
     * - In the case where there is only one operation target attribute (i.e.,
     * either only the "printer-uri" attribute or only the "job-uri" attribute),
     * that attribute MUST be the third attribute in the operation attributes
     * group.
     *
     * - In the case where Job operations use two operation target attributes
     * (i.e., the "printer-uri" and "job-id" attributes), the "printer-uri"
     * attribute MUST be the third attribute and the "job-id" attribute MUST be
     * the fourth attribute.
     *
     * In all cases, the target URIs contained within the body of IPP operation
     * requests and responses must be in absolute format rather than relative
     * format (a relative URL identifies a resource with the scope of the HTTP
     * server, but does not include scheme, host or port).
     *
     * The following rules apply to the use of port numbers in URIs that
     * identify IPP objects:
     *
     * 1. If the URI scheme allows the port number to be explicitly included in
     * the URI string, and a port number is specified within the URI, then that
     * port number MUST be used by the client to contact the IPP object.
     *
     * 2. If the URI scheme allows the port included in the URI string, and a
     * within the URI, then default port scheme MUST be used by the client
     * number to be explicitly port number is not specified number implied by
     * that URI to contact the IPP object.
     *
     * 3. If the URI scheme does not allow an explicit port number to be
     * specified within the URI, then the default port number implied by that
     * URI MUST be used by the client to contact the IPP object.
     */

    /*
     * 3.1.8 Versions
     *
     * Each operation request and response carries with it a "version- number
     * " parameter. Each value of the "version-number" is in the form "X.Y"
     * where X is the major version number and Y is the minor version number. By
     * including a version number in the client request, it allows the client to
     * identify which version of IPP it is interested in using, i.e., the
     * version whose conformance requirements the client may be depending upon
     * the Printer to meet.
     *
     * If the IPP object does not support that major version number supplied by
     * the client, i.e., the major version field of the "version-number"
     * parameter does not match any of the values of the Printer’s "ipp-
     * versions-supported" (see section 4.4.14), the object MUST respond with a
     * status code of ’server-error-version-not-supported’ along with the
     * closest version number that is supported (see section 13.1.5.4). If the
     * major version number is supported, but the minor version number is not,
     * the IPP object SHOULD accept and attempt to perform the request (or
     * reject the request if the operation is not supported), else it rejects
     * the request and returns the ’server- error-version-not-supported’ status
     * code. In all cases, the IPP object MUST return the "version-number" that
     * it supports that is closest to the version number supplied by the client
     * in the request.
     *
     * There is no version negotiation per se. However, if after receiving a
     * ’server-error-version-not-supported’ status code from an IPP object, a
     * client SHOULD try again with a different version number. A client MAY
     * also determine the versions supported either from a directory that
     * conforms to Appendix E (see section 16) or by querying the Printer
     * object’s "ipp-versions-supported" attribute (see section 4.4.14) to
     * determine which versions are supported.
     *
     * An IPP object implementation MUST support version ’1.1’, i.e., meet the
     * conformance requirements for IPP/1.1 as specified in this document and
     * [RFC2910]. It is recommended that IPP object implementations accept any
     * request with the major version ’1’ (or reject the request if the
     * operation is not supported).
     *
     * There is only one notion of "version number" that covers both IPP Model
     * and IPP Protocol changes. Thus the version number MUST change when
     * introducing a new version of the Model and Semantics document (this
     * document) or a new version of the "Encoding and Transport" document
     * [RFC2910].
     *
     * ...
     *
     */

    /**
     * Processes IPP stream.
     *
     * @param istr
     *            The stream to process.
     * @throws IOException
     *             If IO error.
     */
    abstract void process(InputStream istr) throws IOException;

    /**
     * Generic read of attributes.
     *
     * @param istr
     * @throws IOException
     */
    protected void readAttributes(final InputStream istr) throws IOException {

        Writer traceLog = null;

        if (LOGGER.isTraceEnabled()) {
            traceLog = new StringWriter();
        }

        attrGroups = IppEncoder.readAttributes(istr, traceLog);

        if (traceLog != null) {
            LOGGER.trace("\n+----------------------------------------+"
                    + "\n| Request: " + this.getClass().getSimpleName()
                    + "\n+----------------------------------------+"
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

    public final IppAttrValue getRequestedAttributes() {
        return getAttrValue(IppDictOperationAttr.ATTR_REQUESTED_ATTRIBUTES);
    }

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
     * @return the IPP attribute "requesting-user-name" or {@code null} when not
     *         found.
     */
    public final String getRequestingUserName() {
        final IppAttrValue val =
                getAttrValue(IppDictOperationAttr.ATTR_REQUESTING_USER_NAME);
        if (val != null && val.getValues().size() == 1) {
            return val.getValues().get(0);
        }
        return null;
    }

}
