/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.core.services.helpers;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.savapage.core.LetterheadNotFoundException;
import org.savapage.core.PostScriptDrmException;
import org.savapage.core.SpException;
import org.savapage.core.imaging.EcoPrintPdfTask;
import org.savapage.core.imaging.EcoPrintPdfTaskPendingException;
import org.savapage.core.inbox.InboxInfoDto;
import org.savapage.core.inbox.InboxInfoDto.InboxJobRange;
import org.savapage.core.inbox.OutputProducer;
import org.savapage.core.jpa.User;
import org.savapage.core.pdf.PdfCreateRequest;
import org.savapage.core.print.proxy.ProxyPrintInboxReq;
import org.savapage.core.print.proxy.ProxyPrintJobChunk;
import org.savapage.core.services.InboxService;
import org.savapage.core.services.ServiceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An execution pattern for proxy printing from the user inbox.
 *
 * @author Rijk Ravestein
 *
 */
public abstract class ProxyPrintInboxPattern {

    /**
     * The logger.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(ProxyPrintInboxPattern.class);

    /**
     * Helper constant.
     */
    protected static final boolean APPLY_PDF_PROPS = true;

    /**
     * Helper constant.
     */
    protected static final boolean APPLY_LETTERHEAD = true;

    /**
     * Helper constant.
     */
    protected static final boolean PDF_FOR_PRINTING = true;

    /**
     * Notifies initialization of the proxy print.
     *
     * @param lockedUser
     *            The locked {@link User} who requested the print.
     * @param request
     *            The {@link ProxyPrintInboxReq}.
     */
    protected abstract void onInit(final User lockedUser,
            final ProxyPrintInboxReq request);

    /**
     * Notifies termination of the proxy print.
     *
     * @param lockedUser
     *            The locked {@link User} who requested the print.
     * @param request
     *            The {@link ProxyPrintInboxReq}.
     */
    protected abstract void onExit(final User lockedUser,
            final ProxyPrintInboxReq request);

    /**
     * Reserves a unique file path for a PDF print file, obviously containing an
     * {@link UUID} in its file name. Note: the file is not created at this
     * point.
     *
     * @param lockedUser
     *            The locked {@link User} who requested the print.
     * @return The reserved PDF file to generate.
     */
    protected abstract File onReservePdfToGenerate(final User lockedUser);

    /**
     * Notifies that PDF print file is generated for an inbox chunk.
     *
     * @param lockedUser
     *            The locked {@link User} who requested the print.
     * @param request
     *            The {@link ProxyPrintInboxReq}.
     * @param uuidPageCount
     *            Object filled with the number of selected pages per input file
     *            UUID.
     * @param pdfGenerated
     *            The generated PDF file.
     */
    protected abstract void onPdfGenerated(final User lockedUser,
            final ProxyPrintInboxReq request,
            final LinkedHashMap<String, Integer> uuidPageCount,
            final File pdfGenerated);

    /**
     *
     * @param lockedUser
     *            The locked {@link User} who requested the print.
     * @param request
     *            The {@link ProxyPrintInboxReq}.
     * @throws EcoPrintPdfTaskPendingException
     *             When EcoPrint task is pending.
     */
    public final void print(final User lockedUser,
            final ProxyPrintInboxReq request)
                    throws EcoPrintPdfTaskPendingException {

        final InboxService inboxService =
                ServiceContext.getServiceFactory().getInboxService();

        this.onInit(lockedUser, request);

        /*
         * When printing the chunks, the container request parameters are
         * replaced by chunk values. So, we save the original request parameters
         * here, and restore them afterwards.
         */
        final String orgJobName = request.getJobName();
        final int orgNumberOfPages = request.getNumberOfPages();
        final Boolean orgFitToPage = request.getFitToPage();
        final String orgMediaOption = request.getMediaOption();
        final String orgMediaSourceOption = request.getMediaSourceOption();
        final BigDecimal orgCost = request.getCost();
        final boolean orgDrm = request.isDrm();

        try {

            if (request.getJobChunkInfo() == null) {

                final InboxInfoDto inboxInfo =
                        inboxService.readInboxInfo(lockedUser.getUserId());

                final InboxInfoDto filteredInboxInfo =
                        inboxService.filterInboxInfoPages(inboxInfo,
                                request.getPageRanges());

                this.proxyPrintInboxChunk(lockedUser, request,
                        filteredInboxInfo);

            } else {

                final InboxInfoDto inboxInfo =
                        request.getJobChunkInfo().getFilteredInboxInfo();

                for (final ProxyPrintJobChunk chunk : request.getJobChunkInfo()
                        .getChunks()) {

                    /*
                     * Replace the request parameters with the chunk parameters.
                     */
                    request.setNumberOfPages(chunk.getNumberOfPages());
                    request.setFitToPage(chunk.getFitToPage());
                    request.setMediaOption(
                            chunk.getAssignedMedia().getIppKeyword());
                    request.setMediaSourceOption(
                            chunk.getAssignedMediaSource().getSource());
                    request.setCost(chunk.getCost());
                    request.setDrm(chunk.isDrm());

                    if (StringUtils.isBlank(orgJobName)) {
                        request.setJobName(chunk.getJobName());
                    }

                    /*
                     * Save the original pages.
                     */
                    final ArrayList<InboxJobRange> orgPages =
                            inboxService.replaceInboxInfoPages(inboxInfo,
                                    chunk.getRanges());

                    /*
                     * Proxy print the chunk.
                     */
                    this.proxyPrintInboxChunk(lockedUser, request, inboxInfo);

                    /*
                     * Restore the original pages.
                     */
                    inboxInfo.setPages(orgPages);
                }
            }

            this.onExit(lockedUser, request);

        } finally {
            /*
             * Restore the original request parameters.
             */
            request.setJobName(orgJobName);
            request.setNumberOfPages(orgNumberOfPages);
            request.setFitToPage(orgFitToPage);
            request.setMediaOption(orgMediaOption);
            request.setMediaSourceOption(orgMediaSourceOption);
            request.setCost(orgCost);
            request.setDrm(orgDrm);
        }
    }

    /**
     * Proxy prints a single inbox chunk to the outbox.
     *
     * @param lockedUser
     *            The requesting {@link User}, which should be locked.
     * @param request
     *            The {@link ProxyPrintInboxReq}.
     * @param inboxInfo
     *            The (filtered) {@link InboxInfoDto}.
     * @throws EcoPrintPdfTaskPendingException
     *             When {@link EcoPrintPdfTask} objects needed for this PDF are
     *             pending.
     */
    private void proxyPrintInboxChunk(final User lockedUser,
            final ProxyPrintInboxReq request, final InboxInfoDto inboxInfo)
                    throws EcoPrintPdfTaskPendingException {

        /*
         * Generate the PDF file.
         */
        File pdfFileGenerated = null;

        boolean fileCreated = false;

        try {

            final String pdfFileName =
                    this.onReservePdfToGenerate(lockedUser).getAbsolutePath();

            final LinkedHashMap<String, Integer> uuidPageCount =
                    new LinkedHashMap<>();

            final PdfCreateRequest pdfRequest = new PdfCreateRequest();

            pdfRequest.setUserObj(lockedUser);
            pdfRequest.setPdfFile(pdfFileName);
            pdfRequest.setInboxInfo(inboxInfo);
            pdfRequest.setRemoveGraphics(request.isRemoveGraphics());

            pdfRequest.setEcoPdfShadow(request.isEcoPrintShadow());
            pdfRequest.setGrayscale(request.isConvertToGrayscale());

            pdfRequest.setApplyPdfProps(!APPLY_PDF_PROPS);
            pdfRequest.setApplyLetterhead(APPLY_LETTERHEAD);
            pdfRequest.setForPrinting(PDF_FOR_PRINTING);

            pdfFileGenerated = OutputProducer.instance().generatePdf(pdfRequest,
                    uuidPageCount, null);

            this.onPdfGenerated(lockedUser, request, uuidPageCount,
                    pdfFileGenerated);

            fileCreated = true;

        } catch (LetterheadNotFoundException | PostScriptDrmException e) {

            throw new SpException(e.getMessage());

        } finally {

            if (!fileCreated && pdfFileGenerated != null
                    && pdfFileGenerated.exists()) {
                if (pdfFileGenerated.delete()) {
                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace("deleted file [" + pdfFileGenerated + "]");
                    }
                } else {
                    LOGGER.error(
                            "delete of file [" + pdfFileGenerated + "] FAILED");
                }
            }
        }
    }

}
