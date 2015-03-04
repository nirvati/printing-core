/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2014 Datraverse B.V.
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

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.List;

import org.savapage.core.ipp.attribute.IppAttrGroup;
import org.savapage.core.ipp.encoding.IppDelimiterTag;
import org.savapage.core.ipp.encoding.IppEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Datraverse B.V.
 *
 */
public abstract class AbstractIppResponse extends IppMessageMixin {

    /**
     *
     */
    private static final Logger LOGGER = LoggerFactory
            .getLogger(AbstractIppResponse.class);

    /**
     * There are no operation responses that include document data.
     */

    /**
     * 3.1.6 Operation Response Status Codes and Status Messages
     *
     * Every operation response includes a REQUIRED "status-code" parameter and
     * an OPTIONAL "status-message" operation attribute, and an OPTIONAL
     * "detailed-status-message" operation attribute.
     *
     * The Print-URI and Send-URI response MAY include an OPTIONAL
     * "document-access-error" operation attribute.
     *
     *
     * 3.1.6.1 "status-code" (type2 enum) ...
     *
     * 3.1.6.2 "status-message" (text(255)) ...
     *
     * 3.1.6.3 "detailed-status-message" (text(MAX)) ...
     *
     * 3.1.6.4 "document-access-error" (text(MAX)) ...
     */

    /**
     * 3.1.7 Unsupported Attributes
     *
     * The Unsupported Attributes group contains attributes that are not
     * supported by the operation.
     *
     * This group is primarily for the job creation operations, but all
     * operations can return this group.
     *
     * A Printer object MUST include an Unsupported Attributes group in a
     * response if the status code is one of the following:
     *
     * <pre>
     * ’successful-ok-ignored-or-substituted-attributes’,
     * ’successful-ok-conflicting-attributes’,
     * ’client-error-attributes-or-values-not-supported’ or
     * ’client-error-conflicting-attributes’.
     * </pre>
     *
     * If the status code is one of the four specified in the preceding
     * paragraph, the Unsupported Attributes group MUST contain all of those
     * attributes and only those attributes that are:
     *
     * <pre>
     * a. an Operation or Job Template attribute supplied in the request, and
     * b. unsupported by the printer. See below for details on the three
     * categories "unsupported" attributes.
     * </pre>
     *
     * If the status code is one of those in the table in section 3.1.6.1, the
     * Unsupported Attributes group NEED NOT contain the unsupported parameter
     * or attribute indicated in that table.
     *
     * If the Printer object is not returning any Unsupported Attributes in the
     * response, the Printer object SHOULD omit Group 2 rather than sending an
     * empty group. However, a client MUST be able to accept an empty group.
     *
     * Unsupported attributes fall into three categories:
     *
     * <pre>
     * 1. The Printer object does not support the supplied attribute (no
     * matter what the attribute syntax or value).
     * 2. The Printer object does support the attribute, but does not
     * support some or all of the particular attribute syntaxes or
     * values supplied by the client (i.e., the Printer object does
     * not have those attribute syntaxes or values in its
     * corresponding "xxx-supported" attribute).
     * 3. The Printer object does support the attributes and values
     * supplied, but the particular values are in conflict with one
     * another, because they violate a constraint, such as not being
     * able to staple transparencies.
     * </pre>
     *
     * In the case of an unsupported attribute name, the Printer object returns
     * the client-supplied attribute with a substituted value of ’unsupported’.
     * This value’s syntax type is "out-of-band" and its encoding is defined by
     * special rules for "out-of-band" values in the "Encoding and Transport"
     * document [RFC2910]. Its value indicates no support for the attribute
     * itself (see the beginning of section 4.1).
     *
     * In the case of a supported attribute with one or more unsupported
     * attribute syntaxes or values, the Printer object simply returns the
     * client-supplied attribute with the unsupported attribute syntaxes or
     * values as supplied by the client. This indicates support for the
     * attribute, but no support for that particular attribute syntax or value.
     * If the client supplies a multi-valued attribute with more than one value
     * and the Printer object supports the attribute but only supports a subset
     * of the client-supplied attribute syntaxes or values, the Printer object
     *
     * MUST return only those attribute syntaxes or values that are unsupported.
     *
     * In the case of two (or more) supported attribute values that are in
     * conflict with one another (although each is supported independently, the
     * values conflict when requested together within the same job), the Printer
     * object MUST return all the values that it ignores or substitutes to
     * resolve the conflict, but not any of the values that it is still using.
     * The choice for exactly how to resolve the conflict is implementation
     * dependent. See sections 3.2.1.2 and 15. See The Implementer’s Guide
     * [IPP-IIG] for an example.
     *
     */

    protected void write(final AbstractIppOperation operation,
            final IppStatusCode status, List<IppAttrGroup> attrGroups,
            final OutputStream ostr, final Charset charset) throws IOException {

        // Header
        ostr.write(operation.getVersionMajor());
        ostr.write(operation.getVersionMinor());
        IppEncoder.writeInt16(ostr, status.asInt());
        IppEncoder.writeInt32(ostr, operation.getRequestId());

        LOGGER.trace("\nStatus: " + status.toString());

        // Attributes
        writeAttributes(attrGroups, ostr, charset);
        ostr.write(IppDelimiterTag.END_OF_ATTR.asInt());
    }

    /**
     *
     * @param attrGroups
     * @param ostr
     * @throws IOException
     */
    protected void writeAttributes(List<IppAttrGroup> attrGroups,
            final OutputStream ostr, final Charset charset) throws IOException {

        Writer traceLog = null;

        if (LOGGER.isTraceEnabled()) {
            traceLog = new StringWriter();
        }

        try {
            IppEncoder.writeAttributes(attrGroups, ostr, charset, traceLog);
        } finally {
            if (traceLog != null) {
                LOGGER.trace("\n+-----------------------------------+"
                        + "\n| Response: " + this.getClass().getSimpleName()
                        + "\n+-----------------------------------+" + traceLog
                        + "\n+-----------------------------------+");
            }
        }
    }

}
