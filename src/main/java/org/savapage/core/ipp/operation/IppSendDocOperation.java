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

import org.apache.commons.lang3.StringUtils;
import org.savapage.core.dao.enums.DocLogProtocolEnum;
import org.savapage.core.dao.enums.ExternalSupplierEnum;
import org.savapage.core.dao.enums.ExternalSupplierStatusEnum;
import org.savapage.core.ipp.helpers.IppPrintInData;
import org.savapage.core.jpa.DocLog;
import org.savapage.core.jpa.IppQueue;
import org.savapage.core.print.server.DocContentPrintProcessor;
import org.savapage.core.services.DocLogService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.helpers.ExternalSupplierInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class IppSendDocOperation extends IppPrintJobOperation {

    /** */
    private static final DocLogService DOC_LOG_SERVICE =
            ServiceContext.getServiceFactory().getDocLogService();

    /**
     *
     * @author Rijk Ravestein
     *
     */
    private static final class IppSendDocReq extends AbstractIppPrintJobReq {

        /** */
        private static final Logger LOGGER =
                LoggerFactory.getLogger(IppSendDocReq.class);

        /** */
        private IppStatusCode responseStatusCode;

        /** */
        IppSendDocReq() {
            super();
            this.responseStatusCode = IppStatusCode.OK;
        }

        @Override
        protected String getPrintInJobName() {
            return this.getJobName();
        }

        @Override
        protected boolean isJobIdGenerated() {
            return false;
        }

        @Override
        void process(final AbstractIppOperation operation,
                final InputStream istr) throws IOException {

            final DocContentPrintProcessor processor =
                    this.getPrintInProcessor();

            final String userId = processor.getUserDb().getUserId();

            final DocLog docLog = DOC_LOG_SERVICE.getSuppliedDocLog(
                    ExternalSupplierEnum.IPP_CLIENT, userId,
                    String.valueOf(this.getJobId()),
                    ExternalSupplierStatusEnum.PENDING);

            if (docLog == null || docLog.getExternalData() == null) {
                LOGGER.warn("DocLog not found: user [{}] job-id [{}]", userId,
                        this.getJobId());
                this.responseStatusCode = IppStatusCode.CLI_NOTFND;
                return;
            }

            final IppPrintInData createJobData =
                    IppPrintInData.createFromData(docLog.getExternalData());

            // Append document data
            final IppPrintInData sendDocData = this.createIppPrintInData();

            createJobData.setAttrJob(sendDocData.getAttrOperation());

            final ExternalSupplierInfo supplierInfo =
                    new ExternalSupplierInfo();

            supplierInfo.setSupplier(ExternalSupplierEnum.IPP_CLIENT);
            supplierInfo.setData(createJobData);
            supplierInfo.setId(String.valueOf(this.getJobId()));
            supplierInfo
                    .setStatus(ExternalSupplierStatusEnum.COMPLETED.toString());

            if (StringUtils.isBlank(StringUtils.defaultString(
                    this.getPrintInJobName(), docLog.getTitle()))) {
                processor.setJobName(docLog.getTitle());
            }

            processor.setPrintInParent(docLog);
            processor.process(istr, supplierInfo, DocLogProtocolEnum.IPP, null,
                    null, null);
        }

        @Override
        protected IppStatusCode getResponseStatusCode() {
            return this.responseStatusCode;
        }

    }

    /**
     *
     * @author Rijk Ravestein
     *
     */
    private static final class IppSendDocRsp extends AbstractIppPrintJobRsp {
    }

    /**
     * @param queue
     *            The print queue.
     * @param authUser
     *            The authenticated user id associated with the IPP client. If
     *            {@code null} there is NO authenticated user.
     * @param isAuthUserIppRequester
     *            If {@code true}, the authUser overrules the IPP requesting
     *            user.
     * @param ctx
     *            The operation context.
     */
    public IppSendDocOperation(final IppQueue queue, final String authUser,
            final boolean isAuthUserIppRequester,
            final IppOperationContext ctx) {

        super(queue, authUser, isAuthUserIppRequester, ctx, new IppSendDocReq(),
                new IppSendDocRsp());
    }

}
