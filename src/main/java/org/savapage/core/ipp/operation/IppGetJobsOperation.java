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
import org.savapage.core.ipp.attribute.syntax.IppBoolean;
import org.savapage.core.ipp.encoding.IppDelimiterTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class IppGetJobsOperation extends AbstractIppOperation {

    /** */
    private static class IppGetJobsRequest extends AbstractIppRequest {

        /**
         * 'completed': This includes any Job object whose state is 'completed',
         * 'canceled', or 'aborted'.
         */
        public static final String JOB_COMPLETED = "completed";

        /**
         * 'not-completed': This includes any Job object whose state is
         * 'pending', 'processing', 'processing-stopped', or 'pending- held'.
         */
        public static final String JOB_NOT_COMPLETED = "not-completed";

        @Override
        void process(final AbstractIppOperation operation,
                final InputStream istr) throws IOException {
            readAttributes(operation, istr);
        }

        /**
         * The maximum number of jobs that a client will receive from the
         * Printer even if "which-jobs" or "my-jobs" constrain which jobs are
         * returned.
         * <p>
         * If {@code null} the Printer object responds with all applicable jobs.
         * </p>
         *
         * @return {@code null} if NO limit.
         */
        public final String getLimit() {
            IppAttrValue value = getAttrValue("limit");
            if (value == null) {
                return null;
            }
            return value.getValues().get(0);
        }

        /**
         * If the client does not supply this attribute, the Printer object MUST
         * respond as if the client had supplied the attribute with a value of
         * 'not-completed'.
         *
         * @return {@link #JOB_COMPLETED} or {@link #JOB_NOT_COMPLETED}
         */
        public final String getWhichJobs() {
            IppAttrValue value = getAttrValue("which-jobs");
            if (value == null) {
                return JOB_NOT_COMPLETED;
            }
            return value.getValues().get(0);
        }

        /**
         * Indicates whether jobs from all users or just the jobs submitted by
         * the requesting user of this request are considered as candidate jobs
         * to be returned by the Printer object.
         *
         * @return {@code true} if jobs from THIS requesting user. {@code false}
         *         if jobs from ALL users.
         */
        public final boolean isMyJobs() {
            IppAttrValue value = getAttrValue("my-jobs");
            if (value == null) {
                return false;
            }
            return value.getValues().get(0).equals(IppBoolean.TRUE);
        }

    }

    /** */
    private static class IppGetJobsResponse extends AbstractIppResponse {

        private static final Logger LOGGER =
                LoggerFactory.getLogger(IppGetJobsResponse.class);

        /**
         *
         * @param operation
         * @param request
         * @param ostr
         * @throws IOException
         */
        public final void process(final IppGetJobsOperation operation,
                final IppGetJobsRequest request, final OutputStream ostr)
                throws IOException {

            IppStatusCode statusCode = IppStatusCode.OK;

            final List<IppAttrGroup> attrGroups = new ArrayList<>();

            /*
             * Group 1: Operation Attributes
             */
            attrGroups.add(this.createOperationGroup());

            /*
             * Group 2: Unsupported Attributes
             */
            final IppAttrGroup group =
                    new IppAttrGroup(IppDelimiterTag.UNSUPP_ATTR);
            attrGroups.add(group);

            /*
             * "which-jobs" (type2 keyword):
             *
             * If a client supplies some other value, the Printer object MUST
             * ...
             *
             * (1) copy the attribute and the unsupported value to the
             * Unsupported Attributes response group
             *
             * (2) reject the request and return the
             * 'client-error-attributes-or-values-not-supported' status code.
             */
            final String whichJobs = request.getWhichJobs();

            if (!whichJobs.equals(IppGetJobsRequest.JOB_COMPLETED)
                    && !whichJobs.equals(IppGetJobsRequest.JOB_NOT_COMPLETED)) {

                statusCode = IppStatusCode.CLI_NOTSUP;

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("[" + whichJobs + "] is not supported");
                }
                // TODO
            }

            /*
             * Groups 3 to N: Job Object Attributes
             *
             * The Printer object responds with one set of Job Object Attributes
             * for each returned Job object.
             *
             * The Printer object ignores (does not respond with) any requested
             * attribute or value which is not supported or which is restricted
             * by the security policy in force, including whether the requesting
             * user is the user that submitted the job (job originating user) or
             * not (see section 8).
             *
             * However, the Printer object MUST respond with the 'unknown' value
             * for any supported attribute (including all REQUIRED attributes)
             * for which the Printer object does not know the value, unless it
             * would violate the security policy. See the description of the
             * "out-of- band" values in the beginning of Section 4.1.
             *
             * Jobs are returned in the following order:
             *
             * - If the client requests all 'completed' Jobs (Jobs in the
             * 'completed', 'aborted', or 'canceled' states), then the Jobs are
             * returned newest to oldest (with respect to actual completion
             * time)
             *
             * - If the client requests all 'not-completed' Jobs (Jobs in the
             * 'pending', 'processing', 'pending-held', and 'processing-
             * stopped' states), then Jobs are returned in relative
             * chronological order of expected time to complete (based on
             * whatever scheduling algorithm is configured for the Printer
             * object).
             */

            if (request.isMyJobs()) {

                // final String reqUserName = request.getRequestingUserName();

                if (whichJobs.equals(IppGetJobsRequest.JOB_COMPLETED)) {
                    /*
                     * Jobs in the 'completed', 'aborted', or 'canceled' states
                     * are returned newest to oldest (with respect to actual
                     * completion time)
                     */
                } else {
                    /*
                     * Jobs in the 'pending', 'processing', 'pending-held', and
                     * 'processing- stopped' states are returned in relative
                     * chronological order of expected time to complete (based
                     * on whatever scheduling algorithm is configured for the
                     * Printer object).
                     */
                }
            }

            // IppGetJobAttrRsp.createGroupJobAttr(ostr, jobUri, jobId);

            // ---------------------------------------------------------------------
            this.writeHeaderAndAttributes(operation, statusCode, attrGroups,
                    ostr, request.getAttributesCharset());
        }

    }

    /** */
    private final IppGetJobsRequest request = new IppGetJobsRequest();
    /** */
    private final IppGetJobsResponse response = new IppGetJobsResponse();

    @Override
    protected void process(final InputStream istr, final OutputStream ostr)
            throws IOException {
        request.process(this, istr);
        response.process(this, request, ostr);
    }

}
