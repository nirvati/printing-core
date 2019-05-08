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
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.savapage.core.ipp.attribute.IppAttr;
import org.savapage.core.ipp.attribute.IppAttrGroup;
import org.savapage.core.ipp.attribute.IppAttrValue;
import org.savapage.core.ipp.attribute.IppDictJobDescAttr;
import org.savapage.core.ipp.attribute.syntax.IppCharset;
import org.savapage.core.ipp.attribute.syntax.IppJobState;
import org.savapage.core.ipp.attribute.syntax.IppNaturalLanguage;
import org.savapage.core.ipp.encoding.IppDelimiterTag;
import org.savapage.core.ipp.encoding.IppEncoder;

/**
 *
 */
public class IppGetJobAttrRsp extends AbstractIppResponse {

    /**
     *
     * @param operation
     * @param ostr
     * @param request
     * @throws IOException
     *             If IO error.
     */
    public final void process(final IppGetJobAttrOperation operation,
            final IppGetJobAttrReq request, final OutputStream ostr)
            throws IOException {

        List<IppAttrGroup> attrGroups = new ArrayList<>();

        IppAttrGroup group = null;
        IppAttrValue value = null;
        IppAttr attr = null;

        /*
         * Group 1: Operation Attributes
         *
         *
         * Status Message:
         *
         * In addition to the REQUIRED status code returned in every response,
         * the response OPTIONALLY includes a "status-message" (text(255))
         * and/or a "detailed-status-message" (text(MAX)) operation attribute as
         * described in sections 13 and 3.1.6.
         *
         *
         * Natural Language and Character Set:
         *
         * The "attributes-charset" and "attributes-natural-language" attributes
         * as described in section 3.1.4.2. The "attributes- natural-language"
         * MAY be the natural language of the Job object, rather than the one
         * requested.
         */
        group = new IppAttrGroup(IppDelimiterTag.OPERATION_ATTR);
        attrGroups.add(group);

        attr = new IppAttr("attributes-charset", new IppCharset());
        value = new IppAttrValue(attr);
        value.addValue("utf-8");
        group.addAttribute(value);

        attr = new IppAttr("attributes-natural-language",
                new IppNaturalLanguage());
        value = new IppAttrValue(attr);
        value.addValue("en-us");
        group.addAttribute(value);

        /*
         * Group 2: Unsupported Attributes
         *
         * See section 3.1.7 for details on returning Unsupported Attributes.
         *
         * The response NEED NOT contain the "requested-attributes" operation
         * attribute with any supplied values (attribute keywords) that were
         * requested by the client but are not supported by the IPP object. If
         * the Printer object does return unsupported attributes referenced in
         * the "requested-attributes" operation attribute and that attribute
         * included group names, such as 'all', the unsupported attributes MUST
         * NOT include attributes described in the standard but not supported by
         * the implementation.
         */
        group = new IppAttrGroup(IppDelimiterTag.UNSUPP_ATTR);
        attrGroups.add(group);

        /*
         * Group 3: Job Object Attributes
         *
         * This is the set of requested attributes and their current values. The
         * IPP object ignores (does not respond with) any requested attribute or
         * value which is not supported or which is restricted by the security
         * policy in force, including whether the requesting user is the user
         * that submitted the job (job originating user) or not (see section 8).
         * However, the IPP object MUST respond with the 'unknown' value for any
         * supported attribute (including all REQUIRED attributes) for which the
         * IPP object does not know the value, unless it would violate the
         * security policy. See the description of the "out-of-band" values in
         * the beginning of Section 4.1.
         */
        attrGroups.add(createGroupJobAttr(ostr, request.getJobUri(),
                request.getJobId()));

        // ---------------------------------------------------------------------
        // Response: Header
        ostr.write(operation.getVersionMajor());
        ostr.write(operation.getVersionMinor());

        // StatusCode - ignored some attributes
        IppEncoder.writeInt16(ostr, IppStatusCode.OK.asInt());

        //
        IppEncoder.writeInt32(ostr, operation.getRequestId());

        writeAttributes(attrGroups, ostr, request.getAttributesCharset());

        ostr.write(IppDelimiterTag.END_OF_ATTR.asInt());

    }

    /**
     *
     * @param ostr
     * @param jobUri
     * @param jobId
     * @return
     */
    public static final IppAttrGroup createGroupJobAttr(final OutputStream ostr,
            final String jobUri, final String jobId) {

        IppAttrGroup group = new IppAttrGroup(IppDelimiterTag.JOB_ATTR);
        IppAttrValue value = null;
        IppAttr attr = null;

        attr = IppDictJobDescAttr.instance().getAttr("job-uri");
        value = new IppAttrValue(attr);
        value.addValue(jobUri);
        group.addAttribute(value);

        attr = IppDictJobDescAttr.instance().getAttr("job-id");
        value = new IppAttrValue(attr);
        value.addValue(jobId);
        group.addAttribute(value);

        attr = IppDictJobDescAttr.instance().getAttr("job-state");
        value = new IppAttrValue(attr);
        value.addValue(IppJobState.STATE_COMPLETED);
        group.addAttribute(value);

        attr = IppDictJobDescAttr.instance().getAttr("job-state-reasons");
        value = new IppAttrValue(attr);
        value.addValue("job-completed-successfully");
        group.addAttribute(value);

        return group;
    }

}
