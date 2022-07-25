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
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.savapage.core.SpException;
import org.savapage.core.dao.enums.ExternalSupplierEnum;
import org.savapage.core.ipp.attribute.IppAttr;
import org.savapage.core.ipp.attribute.IppAttrGroup;
import org.savapage.core.ipp.attribute.IppAttrValue;
import org.savapage.core.ipp.attribute.IppDictJobDescAttr;
import org.savapage.core.ipp.attribute.IppDictOperationAttr;
import org.savapage.core.ipp.attribute.syntax.IppDateTime;
import org.savapage.core.ipp.attribute.syntax.IppInteger;
import org.savapage.core.ipp.attribute.syntax.IppJobState;
import org.savapage.core.ipp.attribute.syntax.IppKeyword;
import org.savapage.core.ipp.attribute.syntax.IppResolution;
import org.savapage.core.ipp.attribute.syntax.IppUri;
import org.savapage.core.ipp.encoding.IppDelimiterTag;
import org.savapage.core.ipp.helpers.IppPrintInData;
import org.savapage.core.jpa.DocLog;
import org.savapage.core.services.DocLogService;
import org.savapage.core.services.ServiceContext;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class IppGetJobAttrOperation extends AbstractIppOperation {

    /** */
    private static final DocLogService DOCLOG_SERVICE =
            ServiceContext.getServiceFactory().getDocLogService();

    /** */
    private static class IppGetJobAttrRequest extends AbstractIppRequest {

        private String jobId;
        private String jobUri;
        private String jobUuid;
        private String jobName;
        private String documentName;
        private String documentFormat;

        private IppPrintInData printInData;
        private DocLog docLog;

        @Override
        void process(final AbstractIppOperation operation,
                final InputStream istr) throws IOException {

            readAttributes(operation, istr);

            //
            this.jobId = getAttrValue(IppDictJobDescAttr.ATTR_JOB_ID)
                    .getValues().get(0);

            this.jobUri = IppDictJobDescAttr.createJobUri(
                    this.getAttrValue(IppDictOperationAttr.ATTR_PRINTER_URI)
                            .getValues().get(0),
                    this.jobId);

            //
            this.docLog = DOCLOG_SERVICE.getSuppliedDocLog(
                    ExternalSupplierEnum.IPP_CLIENT,
                    this.getRequestingUserName(), this.jobId, null);

            if (this.docLog != null && this.docLog.getExternalData() != null) {
                this.printInData =
                        IppPrintInData.createFromData(docLog.getExternalData());

            } else {
                this.printInData = null;
            }

            this.jobName = this.retrieveJobName();
            this.documentName = this.retrieveDocumentName();
            this.documentFormat = this.retrieveDocumentFormat();

            if (docLog != null) {
                this.jobUuid = docLog.getUuid();
            } else {
                this.jobUuid = UUID.randomUUID().toString();
            }
        }

        /**
         * @return job-uuid.
         */
        public String getJobUid() {
            return this.jobUuid;
        }

        /**
         * @return job-id.
         */
        public String getJobId() {
            return this.jobId;
        }

        /**
         * @return job-uri.
         */
        public String getJobUri() {
            return this.jobUri;
        }

        /**
         * @return job-name.
         */
        public String getJobName() {
            return this.jobName;
        }

        /**
         * @return document-name.
         */
        public String getDocumentName() {
            return this.documentName;
        }

        /**
         * @return document-format.
         */
        public String getDocumentFormat() {
            return this.documentFormat;
        }

        public String retrieveJobName() {
            String value = null;
            if (this.printInData != null) {
                value = this.printInData.getJobName();
            }
            if (value == null && this.docLog != null) {
                value = this.docLog.getTitle();
            }
            return StringUtils.defaultString(value);
        }

        public String retrieveDocumentName() {
            String value = null;
            if (this.printInData != null) {
                value = this.printInData.getDocumentName();
            }
            if (value == null && this.docLog != null) {
                value = this.docLog.getTitle();
            }
            return StringUtils.defaultString(value);
        }

        public String retrieveDocumentFormat() {
            String value = null;
            if (this.printInData != null) {
                value = this.printInData.getDocumentFormat();
            }
            if (value == null && this.docLog != null) {
                value = this.docLog.getMimetype();
            }
            return StringUtils.defaultString(value);
        }

    }

    private static class IppGetJobAttrResponse extends AbstractIppResponse {

        /**
         * Job Description attributes..
         */
        private static final String[] ATTR_JOB_DESC_KEYWORDS = {
                //
                IppDictJobDescAttr.ATTR_JOB_URI, //
                IppDictJobDescAttr.ATTR_JOB_ID,
                IppDictJobDescAttr.ATTR_JOB_UUID,
                IppDictJobDescAttr.ATTR_JOB_STATE,
                IppDictJobDescAttr.ATTR_JOB_STATE_REASONS,
                IppDictJobDescAttr.ATTR_JOB_STATE_MESSAGE,
                //
                IppDictJobDescAttr.ATTR_JOB_NAME,
                IppDictJobDescAttr.ATTR_DOCUMENT_NAME_SUPPLIED,
                IppDictJobDescAttr.ATTR_JOB_ORIGINATING_USER_NAME,
                IppDictJobDescAttr.ATTR_JOB_PRINTER_UP_TIME,
                //
                IppDictJobDescAttr.ATTR_JOB_IMPRESSIONS,
                IppDictJobDescAttr.ATTR_JOB_IMPRESSIONS_COMPLETED,

                IppDictJobDescAttr.ATTR_JOB_PRINTER_URI,
                //
                IppDictJobDescAttr.ATTR_TIME_AT_CREATION,
                IppDictJobDescAttr.ATTR_TIME_AT_PROCESSING,
                IppDictJobDescAttr.ATTR_TIME_AT_COMPLETED,
                //
                IppDictJobDescAttr.ATTR_DATE_TIME_AT_CREATION,
                IppDictJobDescAttr.ATTR_DATE_TIME_AT_PROCESSING,
                IppDictJobDescAttr.ATTR_DATE_TIME_AT_COMPLETED,
                //
                IppDictJobDescAttr.ATTR_COMPRESSION_SUPPLIED,

                IppDictJobDescAttr.ATTR_SIDES, //
                IppDictJobDescAttr.ATTR_MEDIA,
                IppDictJobDescAttr.ATTR_PRINT_COLOR_MODE,
                IppDictJobDescAttr.ATTR_PRINT_QUALITY,
                IppDictJobDescAttr.ATTR_PRINT_CONTENT_OPTIMIZE,
                IppDictJobDescAttr.ATTR_PRINT_RENDERING_INTENT,
                IppDictJobDescAttr.ATTR_PRINTER_RESOLUTION,
                IppDictJobDescAttr.ATTR_DOC_FORMAT_SUPPLIED
                //
        };

        /**
         *
         * @param operation
         * @param ostr
         * @param request
         * @throws IOException
         *             If IO error.
         */
        public final void process(final IppGetJobAttrOperation operation,
                final IppGetJobAttrRequest request, final OutputStream ostr)
                throws IOException {

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
             * Group 3: Job Object Attributes
             */
            attrGroups.add(createGroupJobAttr(request));

            // StatusCode OK : ignored some attributes
            writeHeaderAndAttributes(operation, IppStatusCode.OK, attrGroups,
                    ostr, request.getAttributesCharset());
        }

        /**
         * @param request
         *            IPP request.
         * @return IPP group.
         */
        public static final IppAttrGroup
                createGroupJobAttr(final IppGetJobAttrRequest request) {

            final String printerURI = request.getPrinterURI();
            final String requestingUserName = request.getRequestingUserName();

            final IppDictJobDescAttr dict = IppDictJobDescAttr.instance();

            final IppAttrGroup group =
                    new IppAttrGroup(IppDelimiterTag.JOB_ATTR);

            final String printerUptime =
                    String.valueOf(IppInteger.getPrinterUpTime());

            // TODO
            final String dateTimeNow =
                    IppDateTime.formatDate(ServiceContext.getTransactionDate());

            for (final String ippKw : ATTR_JOB_DESC_KEYWORDS) {

                final IppAttr attr = dict.getAttr(ippKw);

                if (attr == null) {
                    throw new IllegalStateException(
                            "IPP keyword [" + ippKw + "] not found.");
                }

                final IppAttrValue value = new IppAttrValue(attr);

                switch (ippKw) {

                case IppDictJobDescAttr.ATTR_JOB_URI:
                    value.addValue(request.getJobUri());
                    break;

                case IppDictJobDescAttr.ATTR_JOB_ID:
                    value.addValue(request.getJobId());
                    break;

                case IppDictJobDescAttr.ATTR_JOB_UUID:
                    value.addValue(IppUri.getUrnUuid(request.getJobUid()));
                    break;

                case IppDictJobDescAttr.ATTR_JOB_PRINTER_URI:
                    value.addValue(printerURI);
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

                case IppDictJobDescAttr.ATTR_JOB_NAME:
                    value.addValue(request.getJobName());
                    break;

                case IppDictJobDescAttr.ATTR_DOCUMENT_NAME_SUPPLIED:
                    value.addValue(request.getDocumentName());
                    break;

                case IppDictJobDescAttr.ATTR_JOB_ORIGINATING_USER_NAME:
                    value.addValue(requestingUserName);
                    break;

                case IppDictJobDescAttr.ATTR_JOB_PRINTER_UP_TIME:
                    value.addValue(printerUptime);
                    break;

                case IppDictJobDescAttr.ATTR_JOB_IMPRESSIONS:
                    // no break intended.
                case IppDictJobDescAttr.ATTR_JOB_IMPRESSIONS_COMPLETED:
                    // Just a number.
                    value.addValue("100");
                    break;

                case IppDictJobDescAttr.ATTR_TIME_AT_CREATION:
                    // no break intended.
                case IppDictJobDescAttr.ATTR_TIME_AT_PROCESSING:
                    // no break intended.
                case IppDictJobDescAttr.ATTR_TIME_AT_COMPLETED:
                    // TODO
                    value.addValue(printerUptime);
                    break;

                case IppDictJobDescAttr.ATTR_DATE_TIME_AT_CREATION:
                    // no break intended.
                case IppDictJobDescAttr.ATTR_DATE_TIME_AT_PROCESSING:
                    // no break intended.
                case IppDictJobDescAttr.ATTR_DATE_TIME_AT_COMPLETED:
                    // TODO
                    value.addValue(dateTimeNow);
                    break;

                case IppDictJobDescAttr.ATTR_COMPRESSION_SUPPLIED:
                    value.addValue("none");
                    break;

                case IppDictJobDescAttr.ATTR_SIDES:
                    value.addValue(IppKeyword.SIDES_ONE_SIDED);
                    break;

                case IppDictJobDescAttr.ATTR_MEDIA:
                    value.addValue(IppGetPrinterAttrRsp.IPP_MEDIA_DEFAULT
                            .getIppKeyword());
                    break;

                case IppDictJobDescAttr.ATTR_PRINT_COLOR_MODE:
                    value.addValue(IppKeyword.PRINT_COLOR_MODE_AUTO);
                    break;

                case IppDictJobDescAttr.ATTR_PRINT_QUALITY:
                    value.addValue(IppKeyword.PRINT_QUALITY_HIGH);
                    break;

                case IppDictJobDescAttr.ATTR_PRINT_CONTENT_OPTIMIZE:
                    value.addValue("auto");
                    break;

                case IppDictJobDescAttr.ATTR_PRINT_RENDERING_INTENT:
                    value.addValue("auto");
                    break;

                case IppDictJobDescAttr.ATTR_PRINTER_RESOLUTION:
                    value.addValue(IppResolution.DPI_600X600);
                    break;

                case IppDictJobDescAttr.ATTR_DOC_FORMAT_SUPPLIED:
                    value.addValue(request.getDocumentFormat());
                    break;

                default:
                    throw new SpException(
                            "Unhandled IPP keyword [" + ippKw + "].");
                }

                group.addAttribute(value);
            }

            return group;
        }

    }

    /** */
    private final IppGetJobAttrRequest request = new IppGetJobAttrRequest();

    /** */
    private final IppGetJobAttrResponse response = new IppGetJobAttrResponse();

    @Override
    protected void process(final InputStream istr, final OutputStream ostr)
            throws IOException {
        request.process(this, istr);
        response.process(this, request, ostr);
    }

}
