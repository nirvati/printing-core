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

import org.apache.commons.lang3.StringUtils;
import org.savapage.core.dao.DocLogDao;
import org.savapage.core.dao.enums.DocLogProtocolEnum;
import org.savapage.core.dao.enums.ExternalSupplierEnum;
import org.savapage.core.dao.enums.ExternalSupplierStatusEnum;
import org.savapage.core.ipp.attribute.IppAttr;
import org.savapage.core.ipp.attribute.IppAttrGroup;
import org.savapage.core.ipp.attribute.IppAttrValue;
import org.savapage.core.ipp.attribute.IppDictJobDescAttr;
import org.savapage.core.ipp.attribute.IppDictOperationAttr;
import org.savapage.core.ipp.attribute.syntax.IppBoolean;
import org.savapage.core.ipp.attribute.syntax.IppDateTime;
import org.savapage.core.ipp.attribute.syntax.IppInteger;
import org.savapage.core.ipp.attribute.syntax.IppJobState;
import org.savapage.core.ipp.attribute.syntax.IppUri;
import org.savapage.core.ipp.encoding.IppDelimiterTag;
import org.savapage.core.jpa.DocLog;
import org.savapage.core.jpa.IppQueue;
import org.savapage.core.services.ServiceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class IppGetJobsOperation extends AbstractIppOperation {

    /** type2 keyword. */
    public static final String ATTR_WHICH_JOBS = "which-jobs";

    /** Boolean. */
    public static final String ATTR_MY_JOBS = "my-jobs";

    /** integer(1:MAX). */
    public static final String ATTR_LIMIT = "limit";

    /**
     * 'completed': This includes any Job object whose state is 'completed',
     * 'canceled', or 'aborted'.
     */
    public static final String WHICH_JOB_COMPLETED = "completed";

    /**
     * 'not-completed': This includes any Job object whose state is 'pending',
     * 'processing', 'processing-stopped', or 'pending- held'.
     */
    public static final String WHICH_JOB_NOT_COMPLETED = "not-completed";

    /** */
    private static class IppGetJobsRequest extends AbstractIppRequest {

        @Override
        void process(final AbstractIppOperation operation,
                final InputStream istr) throws IOException {
            this.readAttributes(operation, istr);
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
        public final Integer getLimit() {
            IppAttrValue value = getAttrValue(ATTR_LIMIT);
            if (value == null) {
                return null;
            }
            return Integer.valueOf(value.getValues().get(0));
        }

        /**
         * If the client does not supply this attribute, the Printer object MUST
         * respond as if the client had supplied the attribute with a value of
         * 'not-completed'.
         *
         * @return {@link #WHICH_JOB_COMPLETED} or
         *         {@link #WHICH_JOB_NOT_COMPLETED}
         */
        public final String getWhichJobs() {
            IppAttrValue value = getAttrValue(ATTR_WHICH_JOBS);
            if (value == null) {
                return WHICH_JOB_NOT_COMPLETED;
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
            IppAttrValue value = getAttrValue(ATTR_MY_JOBS);
            if (value == null) {
                return true;
            }
            return value.getValues().get(0).equals(IppBoolean.TRUE);
        }

    }

    /** */
    private static class IppGetJobsResponse extends AbstractIppResponse {

        /** */
        private static final Logger LOGGER =
                LoggerFactory.getLogger(IppGetJobsResponse.class);

        /**
         * Minimal Job attributes.
         */
        private static final String[] ATTR_KEYWORDS_MINIMAL = {
                //
                IppDictJobDescAttr.ATTR_JOB_ID, //
                IppDictJobDescAttr.ATTR_JOB_URI };

        /**
         * Required Job attributes when "all" is requested.
         */
        private static final String[] ATTR_KEYWORDS_ALL = {
                //
                IppDictJobDescAttr.ATTR_JOB_ID, //
                IppDictJobDescAttr.ATTR_JOB_URI,

                IppDictJobDescAttr.ATTR_COMPRESSION_SUPPLIED,

                IppDictJobDescAttr.ATTR_DATE_TIME_AT_CREATION,
                IppDictJobDescAttr.ATTR_DATE_TIME_AT_PROCESSING,
                IppDictJobDescAttr.ATTR_DATE_TIME_AT_COMPLETED,

                IppDictJobDescAttr.ATTR_DOC_FORMAT_SUPPLIED,
                IppDictJobDescAttr.ATTR_DOCUMENT_NAME_SUPPLIED,

                IppDictJobDescAttr.ATTR_JOB_IMPRESSIONS,
                IppDictJobDescAttr.ATTR_JOB_IMPRESSIONS_COMPLETED,

                IppDictJobDescAttr.ATTR_JOB_NAME,
                IppDictJobDescAttr.ATTR_JOB_ORIGINATING_USER_NAME,

                IppDictJobDescAttr.ATTR_JOB_PRINTER_UP_TIME,
                IppDictJobDescAttr.ATTR_JOB_PRINTER_URI,

                IppDictJobDescAttr.ATTR_JOB_STATE,
                IppDictJobDescAttr.ATTR_JOB_STATE_REASONS,
                IppDictJobDescAttr.ATTR_JOB_STATE_MESSAGE,

                IppDictJobDescAttr.ATTR_JOB_UUID,

                IppDictJobDescAttr.ATTR_TIME_AT_CREATION,
                IppDictJobDescAttr.ATTR_TIME_AT_PROCESSING,
                IppDictJobDescAttr.ATTR_TIME_AT_COMPLETED
                //
        };

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

            if (!whichJobs.equals(WHICH_JOB_COMPLETED)
                    && !whichJobs.equals(WHICH_JOB_NOT_COMPLETED)) {

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

            // Ignore 'my-jobs'

            final String assignedUserId;

            if (operation.isAuthUserIppRequester) {
                assignedUserId = operation.authenticatedUser;
            } else {
                assignedUserId = request.getRequestingUserName();
            }

            final IppAttrValue reqAttrValue = request.getRequestedAttributes();
            final String[] requestedAttrKeywords;

            if (reqAttrValue == null) {
                requestedAttrKeywords = ATTR_KEYWORDS_MINIMAL;
            } else {
                final List<String> requestedAttrList = reqAttrValue.getValues();

                if (requestedAttrList.isEmpty()) {
                    requestedAttrKeywords = ATTR_KEYWORDS_MINIMAL;
                } else if (StringUtils
                        .defaultString(reqAttrValue.getSingleValue())
                        .equals("all")) {
                    requestedAttrKeywords = ATTR_KEYWORDS_ALL;
                } else {
                    requestedAttrKeywords = requestedAttrList
                            .toArray(new String[requestedAttrList.size()]);
                }
            }

            if (StringUtils.isNotBlank(assignedUserId)
                    && whichJobs.equals(WHICH_JOB_COMPLETED)) {

                // Impose limit.
                final int maxRows = 10; // TODO

                for (final DocLog obj : this.getDocLogList(assignedUserId,
                        operation.getQueue(), Integer.valueOf(maxRows))) {

                    attrGroups.add(this.createJobAttrGroup(request, obj,
                            requestedAttrKeywords));
                }
            }

            this.writeHeaderAndAttributes(operation, statusCode, attrGroups,
                    ostr, request.getAttributesCharset());
        }

        /**
         * Creates IPP Job Attr Group.
         *
         * @param request
         *            IPP request.
         * @param obj
         *            Document Log.
         * @param requestedAttrKeywords
         *            IPP Keywords to add.
         * @return IPP group.
         */
        private IppAttrGroup createJobAttrGroup(final IppGetJobsRequest request,
                final DocLog obj, final String[] requestedAttrKeywords) {
            //
            final String printerUptime =
                    String.valueOf(IppInteger.getPrinterUpTime());

            // TODO
            final String dateTimeNow =
                    IppDateTime.formatDate(ServiceContext.getTransactionDate());

            //
            final IppAttrGroup group =
                    new IppAttrGroup(IppDelimiterTag.JOB_ATTR);

            final IppDictJobDescAttr dict = IppDictJobDescAttr.instance();

            for (final String ippKeyword : requestedAttrKeywords) {

                final IppAttr attr = dict.getAttr(ippKeyword);
                if (attr == null) {
                    throw new IllegalStateException(
                            "IPP keyword [" + ippKeyword + "] not found.");
                }
                final IppAttrValue value = new IppAttrValue(attr);

                boolean isValueAssigned = true;

                switch (ippKeyword) {

                case IppDictJobDescAttr.ATTR_JOB_ID:
                    value.addValue(obj.getExternalId());
                    break;

                case IppDictJobDescAttr.ATTR_JOB_URI:
                    value.addValue(IppDictJobDescAttr.createJobUri(
                            request.getPrinterURI(), obj.getExternalId()));
                    break;

                case IppDictJobDescAttr.ATTR_COMPRESSION_SUPPLIED:
                    value.addValue("none");
                    break;

                case IppDictJobDescAttr.ATTR_DATE_TIME_AT_CREATION:
                    // no break intended.
                case IppDictJobDescAttr.ATTR_DATE_TIME_AT_PROCESSING:
                    // no break intended.
                case IppDictJobDescAttr.ATTR_DATE_TIME_AT_COMPLETED:
                    // TODO
                    value.addValue(dateTimeNow);
                    break;

                case IppDictJobDescAttr.ATTR_DOC_FORMAT_SUPPLIED:
                    // TODO
                    value.addValue(obj.getMimetype());
                    break;

                case IppDictJobDescAttr.ATTR_DOCUMENT_NAME_SUPPLIED:
                    // TODO
                    value.addValue(obj.getTitle());
                    break;

                case IppDictJobDescAttr.ATTR_JOB_IMPRESSIONS:
                    // no break intended.
                case IppDictJobDescAttr.ATTR_JOB_IMPRESSIONS_COMPLETED:
                    // Just a number.
                    value.addValue("100");
                    break;

                case IppDictJobDescAttr.ATTR_JOB_NAME:
                    value.addValue(obj.getTitle());
                    break;

                case IppDictJobDescAttr.ATTR_JOB_ORIGINATING_USER_NAME:
                    value.addValue(request.getRequestingUserName());
                    break;

                case IppDictJobDescAttr.ATTR_JOB_PRINTER_UP_TIME:
                    value.addValue(printerUptime);
                    break;

                case IppDictJobDescAttr.ATTR_JOB_PRINTER_URI:
                    value.addValue(request.getPrinterURI());
                    break;

                case IppDictJobDescAttr.ATTR_JOB_STATE:
                    value.addValue(IppJobState.STATE_COMPLETED);
                    break;

                case IppDictJobDescAttr.ATTR_JOB_STATE_REASONS:
                    value.addValue("job-completed-successfully");
                    break;

                case IppDictJobDescAttr.ATTR_JOB_STATE_MESSAGE:
                    value.addValue("OK");
                    break;

                case IppDictJobDescAttr.ATTR_JOB_UUID:
                    value.addValue(IppUri.getUrnUuid(obj.getUuid()));
                    break;

                case IppDictJobDescAttr.ATTR_TIME_AT_CREATION:
                    // no break intended.
                case IppDictJobDescAttr.ATTR_TIME_AT_PROCESSING:
                    // no break intended.
                case IppDictJobDescAttr.ATTR_TIME_AT_COMPLETED:
                    // TODO
                    value.addValue(printerUptime);
                    break;

                default:
                    isValueAssigned = false;
                    break;
                }

                if (isValueAssigned) {
                    group.addAttribute(value);
                }

            }
            return group;
        }

        /**
         * @param assignedUserId
         *            User ID.
         * @param queue
         *            The requested printer queue.
         * @param limit
         *            Max number of entries returned.
         * @return List.
         */
        private List<DocLog> getDocLogList(final String assignedUserId,
                final IppQueue queue, final Integer limit) {

            final DocLogDao.ListFilter filter = new DocLogDao.ListFilter();

            filter.setExternalSupplier(ExternalSupplierEnum.IPP_CLIENT);
            filter.setExternalStatus(ExternalSupplierStatusEnum.COMPLETED);
            filter.setProtocol(DocLogProtocolEnum.IPP);
            filter.setUserId(assignedUserId);
            filter.setIppQueueId(queue.getId());

            final DocLogDao dao = ServiceContext.getDaoContext().getDocLogDao();

            return dao.getListChunk(filter, null, limit,
                    DocLogDao.Field.DATE_CREATED, false);
        }

    }

    /** */
    private final IppGetJobsRequest request;
    /** */
    private final IppGetJobsResponse response;

    /** */
    private final IppQueue queue;

    /**
     * Authenticated user in Web App. If {@code null}, no authenticated user is
     * present.
     */
    private final String authenticatedUser;

    /**
     * If {@code true}, the {@link #authenticatedUser} overrules the IPP
     * {@link IppDictOperationAttr#ATTR_REQUESTING_USER_NAME}.
     */
    private final boolean isAuthUserIppRequester;

    /**
     *
     * @param queueReq
     *            The requested printer queue.
     * @param authUser
     *            The authenticated user id associated with the IPP client. If
     *            {@code null} there is NO authenticated user.
     * @param isAuthUserIppReq
     *            If {@code true}, the authUser overrules the IPP requesting
     *            user.
     */
    public IppGetJobsOperation(final IppQueue queueReq, final String authUser,
            final boolean isAuthUserIppReq) {
        super();

        this.queue = queueReq;
        this.authenticatedUser = authUser;
        this.isAuthUserIppRequester = isAuthUserIppReq;

        this.request = new IppGetJobsRequest();
        this.response = new IppGetJobsResponse();
    }

    /**
     * @return The requested printer queue.
     */
    public IppQueue getQueue() {
        return this.queue;
    }

    @Override
    protected void process(final InputStream istr, final OutputStream ostr)
            throws IOException {
        request.process(this, istr);
        response.process(this, request, ostr);
    }

}
