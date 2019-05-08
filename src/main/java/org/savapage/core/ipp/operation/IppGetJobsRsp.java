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
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.savapage.core.ipp.attribute.IppAttr;
import org.savapage.core.ipp.attribute.IppAttrGroup;
import org.savapage.core.ipp.attribute.IppAttrValue;
import org.savapage.core.ipp.attribute.syntax.IppCharset;
import org.savapage.core.ipp.attribute.syntax.IppNaturalLanguage;
import org.savapage.core.ipp.encoding.IppDelimiterTag;
import org.savapage.core.ipp.encoding.IppEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Printer object returns all of the Job objects up to the number specified
 * by the "limit" attribute that match the criteria as defined by the attribute
 * values supplied by the client in the request. It is possible that no Job
 * objects are returned since there may literally be no Job objects at the
 * Printer, or there may be no Job objects that match the criteria supplied by
 * the client. If the client requests any Job attributes at all, there is a set
 * of Job Object Attributes returned for each Job object.
 *
 * It is not an error for the Printer to return 0 jobs. If the response returns
 * 0 jobs because there are no jobs matching the criteria, and the request would
 * have returned 1 or more jobs with a status code of 'successful-ok' if there
 * had been jobs matching the criteria, then the status code for 0 jobs MUST be
 * 'successful-ok'.
 */
public class IppGetJobsRsp extends AbstractIppResponse {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(IppGetJobAttrRsp.class);

    /**
     *
     * @param operation
     * @param request
     * @param ostr
     * @throws IOException
     */
    public final void process(final IppGetJobsOperation operation,
            final IppGetJobsReq request, final OutputStream ostr)
            throws IOException {

        IppStatusCode statusCode = IppStatusCode.OK;

        List<IppAttrGroup> attrGroups = new ArrayList<>();

        IppAttrGroup group = null;
        IppAttrValue value = null;
        IppAttr attr = null;

        /*
         * Group 1: Operation Attributes
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
         * as described in section 3.1.4.2.
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
         * "which-jobs" (type2 keyword):
         *
         * If a client supplies some other value, the Printer object MUST ...
         *
         * (1) copy the attribute and the unsupported value to the Unsupported
         * Attributes response group
         *
         * (2) reject the request and return the
         * 'client-error-attributes-or-values-not-supported' status code.
         */
        final String whichJobs = request.getWhichJobs();

        if (!whichJobs.equals(IppGetJobsReq.JOB_COMPLETED)
                && !whichJobs.equals(IppGetJobsReq.JOB_NOT_COMPLETED)) {

            statusCode = IppStatusCode.CLI_NOTSUP;

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("[" + whichJobs + "] is not supported");
            }
            // TODO
        }

        /*
         * Groups 3 to N: Job Object Attributes
         *
         * The Printer object responds with one set of Job Object Attributes for
         * each returned Job object.
         *
         * The Printer object ignores (does not respond with) any requested
         * attribute or value which is not supported or which is restricted by
         * the security policy in force, including whether the requesting user
         * is the user that submitted the job (job originating user) or not (see
         * section 8).
         *
         * However, the Printer object MUST respond with the 'unknown' value for
         * any supported attribute (including all REQUIRED attributes) for which
         * the Printer object does not know the value, unless it would violate
         * the security policy. See the description of the "out-of- band" values
         * in the beginning of Section 4.1.
         *
         * Jobs are returned in the following order:
         *
         * - If the client requests all 'completed' Jobs (Jobs in the
         * 'completed', 'aborted', or 'canceled' states), then the Jobs are
         * returned newest to oldest (with respect to actual completion time)
         *
         * - If the client requests all 'not-completed' Jobs (Jobs in the
         * 'pending', 'processing', 'pending-held', and 'processing- stopped'
         * states), then Jobs are returned in relative chronological order of
         * expected time to complete (based on whatever scheduling algorithm is
         * configured for the Printer object).
         */

        if (request.isMyJobs()) {

            // final String reqUserName = request.getRequestingUserName();

            if (whichJobs.equals(IppGetJobsReq.JOB_COMPLETED)) {
                /*
                 * Jobs in the 'completed', 'aborted', or 'canceled' states are
                 * returned newest to oldest (with respect to actual completion
                 * time)
                 */
            } else {
                /*
                 * Jobs in the 'pending', 'processing', 'pending-held', and
                 * 'processing- stopped' states are returned in relative
                 * chronological order of expected time to complete (based on
                 * whatever scheduling algorithm is configured for the Printer
                 * object).
                 */
            }
        }

        // IppGetJobAttrRsp.createGroupJobAttr(ostr, jobUri, jobId);

        // ---------------------------------------------------------------------
        write(ostr, operation, statusCode, attrGroups,
                request.getAttributesCharset());
    }

    /**
     *
     * @param ostr
     * @param operation
     * @param statusCode
     * @param attrGroups
     * @param charset
     * @throws IOException
     */
    protected final void write(final OutputStream ostr,
            final IppGetJobsOperation operation, final IppStatusCode statusCode,
            final List<IppAttrGroup> attrGroups, final Charset charset)
            throws IOException {

        ostr.write(operation.getVersionMajor());
        ostr.write(operation.getVersionMinor());

        IppEncoder.writeInt16(ostr, statusCode.asInt());
        IppEncoder.writeInt32(ostr, operation.getRequestId());

        writeAttributes(attrGroups, ostr, charset);
        ostr.write(IppDelimiterTag.END_OF_ATTR.asInt());
    }

}
