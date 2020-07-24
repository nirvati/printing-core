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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.StringUtils;
import org.savapage.core.ipp.IppProcessingException;
import org.savapage.core.ipp.attribute.IppAttrGroup;
import org.savapage.core.ipp.attribute.IppAttrValue;
import org.savapage.core.ipp.attribute.IppDictJobDescAttr;
import org.savapage.core.ipp.attribute.IppDictJobTemplateAttr;
import org.savapage.core.ipp.attribute.IppDictOperationAttr;
import org.savapage.core.ipp.encoding.IppDelimiterTag;
import org.savapage.core.jpa.IppQueue;
import org.savapage.core.jpa.User;
import org.savapage.core.print.server.DocContentPrintProcessor;
import org.savapage.core.system.SystemInfo;
import org.savapage.core.util.DateUtil;

/**
 *
 * @author Rijk Ravestein
 *
 */
public abstract class AbstractIppPrintJobReq extends AbstractIppRequest {

    /** */
    private static class JobIdCounter {
        /** */
        public static final AtomicInteger INSTANCE =
                new AtomicInteger((int) (SystemInfo.getStarttime()
                        / DateUtil.DURATION_MSEC_SECOND));
    }

    /*
     * Group 1: Operation Attributes
     *
     * Group 2: Job Template Attributes
     *
     * Group 3: Document Content
     */

    /** */
    private DocContentPrintProcessor printInProcessor = null;

    /**
     * Job-id: generated or requested.
     */
    private int jobId;

    /** */
    public AbstractIppPrintJobReq() {
    }

    /**
     * @return Job name for PrintIn object.
     */
    protected abstract String getPrintInJobName();

    /**
     * @return {@code true} if a job-id is generated. If {@code false}, the
     *         job-id is taken from the IPP Operation group attributes.
     */
    protected abstract boolean isJobIdGenerated();

    /**
     * @return The status code of the response.
     */
    protected abstract IppStatusCode getResponseStatusCode();

    /**
     * @return Print-in processor.
     */
    protected DocContentPrintProcessor getPrintInProcessor() {
        return this.printInProcessor;
    }

    /**
     * Just the attributes, not the print job data.
     *
     * @param operation
     *            IPP operation.
     * @param istr
     *            Input stream.
     * @throws IOException
     *             If error.
     */
    public void processAttributes(final AbstractIppJobOperation operation,
            final InputStream istr) throws IOException {

        final String authWebAppUser;

        if (operation.isAuthUserIppRequester()) {
            authWebAppUser = operation.getAuthenticatedUser();
        } else {
            authWebAppUser = null;
        }

        /*
         * Step 1: Create generic PrintIn handler.
         *
         * This should be a first action because this handler holds the deferred
         * exception.
         */
        this.printInProcessor =
                new DocContentPrintProcessor(operation.getQueue(),
                        operation.getOriginatorIp(), null, authWebAppUser);

        this.printInProcessor
                .setIppRoutinglistener(operation.getIppRoutingListener());

        /*
         * Step 2: Read the IPP attributes.
         */
        this.readAttributes(operation, istr);

        // Determine job-id.
        if (this.isJobIdGenerated()) {
            this.jobId = JobIdCounter.INSTANCE.incrementAndGet();
        } else {
            this.jobId = this.getJobIdAttr().intValue();
        }

        this.printInProcessor.setJobName(StringUtils.defaultString(
                this.getPrintInJobName(), this.getDocumentName()));

        /** */
        final String assignedUserId;

        if (operation.isAuthUserIppRequester()) {
            assignedUserId = operation.getAuthenticatedUser();
        } else {
            assignedUserId = this.getRequestingUserName();
        }

        this.printInProcessor.processAssignedUser(assignedUserId);
    }

    /**
     * @return {@code true} if trusted user is present.
     */
    public boolean isTrustedUser() {
        return this.printInProcessor.isTrustedUser();
    }

    /**
     * Selects IPP attributes for PrintIn data.
     *
     * @return IPP attribute key/value map.
     */
    protected final Map<String, String> selectIppPrintInData() {

        final Map<String, String> attr = new HashMap<>();

        for (final IppAttrGroup group : this.getAttrGroups()) {

            if (group.getDelimiterTag() == IppDelimiterTag.OPERATION_ATTR
                    || group.getDelimiterTag() == IppDelimiterTag.JOB_ATTR) {

                for (final IppAttrValue value : group.getAttributes()) {

                    if (value.getValues().size() == 1) {

                        final String kw = value.getAttribute().getKeyword();

                        switch (kw) {
                        // no breaks intended
                        case IppDictOperationAttr.ATTR_REQUESTING_USER_NAME:
                        case IppDictOperationAttr.ATTR_PRINTER_URI:
                        case IppDictOperationAttr.ATTR_JOB_ID:
                        case IppDictOperationAttr.ATTR_JOB_NAME:
                        case IppDictOperationAttr.ATTR_DOCUMENT_FORMAT:
                        case IppDictOperationAttr.ATTR_DOCUMENT_NAME:
                        case IppDictJobTemplateAttr.ATTR_ORIENTATION_REQUESTED:
                            attr.put(kw, value.getSingleValue());
                            break;
                        default:
                            // no code intended
                            break;
                        }
                    }
                }
            }
        }
        return attr;
    }

    /**
     *
     * @return The job-id attribute.
     */
    private Integer getJobIdAttr() {

        final IppAttrValue ippValue =
                this.getAttrValue(IppDictJobDescAttr.ATTR_JOB_ID);

        if (ippValue == null || ippValue.getValues().isEmpty()) {
            return null;
        }
        return Integer.valueOf(ippValue.getValues().get(0));
    }

    /**
     * @return The job name.
     */
    public String getJobName() {

        final IppAttrValue ippValue =
                this.getAttrValue(IppDictJobDescAttr.ATTR_JOB_NAME);

        if (ippValue == null || ippValue.getValues().isEmpty()) {
            return "";
        }
        return ippValue.getValues().get(0);
    }

    /**
     * @return The document name.
     */
    public String getDocumentName() {

        final IppAttrValue ippValue =
                this.getAttrValue(IppDictOperationAttr.ATTR_DOCUMENT_NAME);

        if (ippValue == null || ippValue.getValues().isEmpty()) {
            return "";
        }
        return ippValue.getValues().get(0);
    }

    /**
     * @param theJobId
     *            Job id.
     * @return Job URI.
     */
    public String getJobUri(final int theJobId) {
        return IppDictJobDescAttr.createJobUri(
                this.getAttrValue(IppDictOperationAttr.ATTR_PRINTER_URI)
                        .getValues().get(0),
                String.valueOf(theJobId));
    }

    /**
     * The user object from the database representing the user who printed this
     * job.
     *
     * @return {@code null} when unknown.
     */
    public User getUserDb() {
        return this.printInProcessor.getUserDb();
    }

    /**
     * @return The job-id (either generated, or passed as IPP operation
     *         attribute).
     */
    public int getJobId() {
        return this.jobId;
    }

    /**
     * @return {@code true} if deferred exception.
     */
    public boolean hasDeferredException() {
        return this.printInProcessor.getDeferredException() != null;
    }

    /**
     *
     * @return Exception.
     */
    public IppProcessingException getDeferredException() {
        final Exception ex = this.printInProcessor.getDeferredException();
        if (ex == null) {
            return null;
        }
        if (ex instanceof IppProcessingException) {
            return (IppProcessingException) ex;
        }
        return new IppProcessingException(
                IppProcessingException.StateEnum.INTERNAL_ERROR,
                ex.getMessage(), ex);
    }

    /**
     *
     * @param e
     *            Exception.
     */
    public void setDeferredException(final IppProcessingException e) {
        this.printInProcessor.setDeferredException(e);
    }

    /**
     * @return {@code true} if DRM error.
     */
    public boolean isDrmViolationDetected() {
        return this.printInProcessor.isDrmViolationDetected();
    }

    /**
     * Wraps the {@link DocContentPrintProcessor#evaluateErrorState(boolean)}
     * method.
     *
     * @param operation
     *            The {@link AbstractIppJobOperation}.
     * @throws IppProcessingException
     *             If IPP error.
     */
    public void evaluateErrorState(final AbstractIppJobOperation operation)
            throws IppProcessingException {

        final boolean isAuthorized = operation.isAuthorized();
        this.printInProcessor.evaluateErrorState(isAuthorized);

        if (hasDeferredException()) {
            throw getDeferredException();

        } else if (!isAuthorized) {

            final IppQueue queue = operation.getQueue();

            final StringBuilder msg = new StringBuilder();

            msg.append("User access denied to queue");

            msg.append(" \"/").append(queue.getUrlPath()).append("\"");
            if (queue.getDeleted().booleanValue()) {
                msg.append(" (deleted)");
            } else if (queue.getDisabled().booleanValue()) {
                msg.append(" (disabled)");
            }
            msg.append(".");

            throw new IppProcessingException(
                    IppProcessingException.StateEnum.UNAUTHORIZED,
                    msg.toString());
        }
    }

}
