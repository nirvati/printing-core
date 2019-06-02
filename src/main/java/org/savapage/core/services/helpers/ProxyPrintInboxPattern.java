/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2018 Datraverse B.V.
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
import org.savapage.core.inbox.PdfOrientationInfo;
import org.savapage.core.jpa.User;
import org.savapage.core.pdf.PdfCreateInfo;
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
    protected abstract void onInit(User lockedUser, ProxyPrintInboxReq request);

    /**
     * Notifies termination of the proxy print.
     *
     * @param lockedUser
     *            The locked {@link User} who requested the print.
     * @param request
     *            The {@link ProxyPrintInboxReq}.
     */
    protected abstract void onExit(User lockedUser, ProxyPrintInboxReq request);

    /**
     * Reserves a unique file path for a PDF print file, obviously containing an
     * {@link UUID} in its file name. Note: the file is not created at this
     * point.
     *
     * @param lockedUser
     *            The locked {@link User} who requested the print.
     * @return The reserved PDF file to generate.
     */
    protected abstract File onReservePdfToGenerate(User lockedUser);

    /**
     * Notifies that PDF print file is generated for an inbox chunk.
     *
     * @param lockedUser
     *            The locked {@link User} who requested the print.
     * @param request
     *            The {@link ProxyPrintInboxReq}.
     * @param createInfo
     *            The {@link PdfCreateInfo}.
     * @param chunkIndex
     *            1-based index of chunkSize;
     * @param chunkSize
     *            Total number of chunks;
     */
    protected abstract void onPdfGenerated(User lockedUser,
            ProxyPrintInboxReq request, PdfCreateInfo createInfo,
            int chunkIndex, int chunkSize);

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
        final PrintScalingEnum orgPrintScaling =
                request.getPrintScalingOption();
        final String orgMediaOption = request.getMediaOption();
        final String orgMediaSourceOption = request.getMediaSourceOption();
        final ProxyPrintCostDto orgCostResult = request.getCostResult();
        final boolean orgDrm = request.isDrm();
        final Boolean orgLandscape = request.getLandscape();
        final PdfOrientationInfo orgOrientation = request.getPdfOrientation();

        try {

            if (request.getJobChunkInfo() == null) {

                final InboxInfoDto inboxInfo =
                        inboxService.readInboxInfo(lockedUser.getUserId());

                final InboxInfoDto filteredInboxInfo =
                        inboxService.filterInboxInfoPages(inboxInfo,
                                request.getPageRanges());

                request.setLandscape(
                        Boolean.valueOf(filteredInboxInfo.hasLandscape()));

                request.setPdfOrientation(
                        filteredInboxInfo.getFirstPdfOrientation());

                this.proxyPrintInboxChunk(lockedUser, request,
                        filteredInboxInfo, 1, 1);

            } else {

                final InboxInfoDto inboxInfo =
                        request.getJobChunkInfo().getFilteredInboxInfo();

                request.setLandscape(Boolean.valueOf(inboxInfo.hasLandscape()));

                final int chunkTotal =
                        request.getJobChunkInfo().getChunks().size();

                int chunkindex = 0;

                for (final ProxyPrintJobChunk chunk : request.getJobChunkInfo()
                        .getChunks()) {

                    chunkindex++;

                    /*
                     * Replace the request parameters with the chunk parameters.
                     */
                    request.setNumberOfPages(chunk.getNumberOfPages());
                    request.setPrintScalingOption(chunk.getPrintScaling());
                    request.setMediaOption(
                            chunk.getAssignedMedia().getIppKeyword());

                    if (chunk.getIppMediaSource() != null) {
                        request.setMediaSourceOption(chunk.getIppMediaSource());
                    } else {
                        request.setMediaSourceOption(
                                chunk.getAssignedMediaSource().getSource());
                    }

                    request.setCostResult(chunk.getCostResult());
                    request.setDrm(chunk.isDrm());

                    request.setPdfOrientation(
                            request.getJobChunkInfo().getPdfOrientation());

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
                    this.proxyPrintInboxChunk(lockedUser, request, inboxInfo,
                            chunkindex, chunkTotal);

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
            request.setPrintScalingOption(orgPrintScaling);
            request.setMediaOption(orgMediaOption);
            request.setMediaSourceOption(orgMediaSourceOption);
            request.setCostResult(orgCostResult);
            request.setDrm(orgDrm);
            request.setLandscape(orgLandscape);
            request.setPdfOrientation(orgOrientation);
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
     * @param chunkIndex
     *            1-based index of chunkSize;
     * @param chunkSize
     *            Total number of chunks;
     * @throws EcoPrintPdfTaskPendingException
     *             When {@link EcoPrintPdfTask} objects needed for this PDF are
     *             pending.
     */
    private void proxyPrintInboxChunk(final User lockedUser,
            final ProxyPrintInboxReq request, final InboxInfoDto inboxInfo,
            final int chunkIndex, final int chunkSize)
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
            pdfRequest.setBookletPageOrder(request.isLocalBooklet());

            pdfRequest.setApplyPdfProps(!APPLY_PDF_PROPS);
            pdfRequest.setApplyLetterhead(APPLY_LETTERHEAD);
            pdfRequest.setForPrinting(PDF_FOR_PRINTING);

            pdfRequest.setPrintDuplex(request.isDuplex());
            pdfRequest.setPrintNup(request.getNup());

            pdfRequest.setForPrintingFillerPages(!request.isBooklet()
                    && (request.isDuplex() || request.getNup() > 0));

            final PdfCreateInfo createInfo = OutputProducer.instance()
                    .generatePdf(pdfRequest, uuidPageCount, null);

            pdfFileGenerated = createInfo.getPdfFile();

            this.onPdfGenerated(lockedUser, request, createInfo, chunkIndex,
                    chunkSize);

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
