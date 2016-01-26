/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2016 Datraverse B.V.
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
package org.savapage.core.services.impl;

import java.io.File;
import java.nio.file.Paths;
import java.util.Date;
import java.util.LinkedHashMap;

import org.apache.commons.lang3.StringUtils;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.doc.DocContent;
import org.savapage.core.imaging.EcoPrintPdfTaskPendingException;
import org.savapage.core.jpa.User;
import org.savapage.core.outbox.JobTicketInfoDto;
import org.savapage.core.print.proxy.AbstractProxyPrintReq.Status;
import org.savapage.core.print.proxy.ProxyPrintInboxReq;
import org.savapage.core.services.JobTicketService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.helpers.ProxyPrintInboxPattern;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class JobTicketServiceImpl extends AbstractService implements
        JobTicketService {

    public static final String FILENAME_EXT_JSON = "json";

    /**
     * Implementation of execution pattern for proxy printing from the user
     * inbox to job ticket.
     */
    private static final class ProxyPrintInbox extends ProxyPrintInboxPattern {

        private final JobTicketServiceImpl serviceImpl;

        /**
         * The date the proxy print was submitted.
         */
        private Date submitDate;

        /**
         *
         * @param service
         *            The parent service.
         */
        public ProxyPrintInbox(final JobTicketServiceImpl service) {
            this.serviceImpl = service;
        }

        @Override
        protected void onInit(User lockedUser, ProxyPrintInboxReq request) {
            this.submitDate = ServiceContext.getTransactionDate();
        }

        @Override
        protected void onExit(User lockedUser, ProxyPrintInboxReq request) {

            final String msgKey = "msg-user-print-jobticket";

            request.setStatus(Status.WAITING_FOR_RELEASE);
            request.setUserMsgKey(msgKey);
            request.setUserMsg(serviceImpl.localize(msgKey));
        }

        @Override
        protected File onReservePdfToGenerate(final User lockedUser) {
            return Paths.get(
                    ConfigManager.getJobTicketsHome().toString(),
                    String.format("%s.%s", java.util.UUID.randomUUID()
                            .toString(), DocContent.FILENAME_EXT_PDF)).toFile();
        }

        @Override
        protected void onPdfGenerated(final User lockedUser,
                final ProxyPrintInboxReq request,
                final LinkedHashMap<String, Integer> uuidPageCount,
                final File pdfGenerated) {

            /*
             * Create sibling json file with proxy print information.
             */
            final File jsonFile =
                    new File(StringUtils.removeEnd(
                            pdfGenerated.getAbsolutePath(),
                            DocContent.FILENAME_EXT_PDF).concat(
                            FILENAME_EXT_JSON));

            final JobTicketInfoDto dto = new JobTicketInfoDto();
            dto.setUserId(lockedUser.getId());

            // final OutboxJob job =
            // this.serviceImpl.createOutboxJob(request, this.submitDate,
            // this.expiryDate, pdfGenerated, uuidPageCount);
            //
            // this.outboxInfo.addJob(job.getFile(), job);
        }
    }

    @Override
    public void proxyPrintInbox(User lockedUser, ProxyPrintInboxReq request)
            throws EcoPrintPdfTaskPendingException {
        new ProxyPrintInbox(this).print(lockedUser, request);
    }

}
