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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.print.attribute.standard.MediaSizeName;

import org.savapage.core.dao.PrinterDao;
import org.savapage.core.dto.IppMediaSourceCostDto;
import org.savapage.core.inbox.InboxInfoDto;
import org.savapage.core.ipp.IppMediaSizeEnum;
import org.savapage.core.ipp.attribute.syntax.IppKeyword;
import org.savapage.core.jpa.Printer;
import org.savapage.core.jpa.User;
import org.savapage.core.print.proxy.ProxyPrintException;
import org.savapage.core.print.proxy.ProxyPrintInboxReq;
import org.savapage.core.print.proxy.ProxyPrintJobChunk;
import org.savapage.core.print.proxy.ProxyPrintJobChunkInfo;
import org.savapage.core.services.InboxService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.util.MediaUtils;
import org.savapage.core.util.NumberUtil;

/**
 * Chunks a {@link ProxyPrintInboxReq} in separate print jobs per media-source.
 *
 * @author Rijk Ravestein
 *
 */
public final class ProxyPrintInboxReqChunker {

    /**
     * TODO: should be configuration item (?).
     */
    private static final boolean IS_UNIQUE_MEDIASOURCE_REQUIRED = false;

    /**
    *
    */
    private static final InboxService INBOX_SERVICE =
            ServiceContext.getServiceFactory().getInboxService();

    /**
     *
     */
    private final User lockedUser;

    /**
     *
     */
    private final ProxyPrintInboxReq request;

    /**
     *
     */
    private final PageScalingEnum requestedPageScaling;

    /**
     * Prevent default instantiation.
     */
    @SuppressWarnings("unused")
    private ProxyPrintInboxReqChunker() {
        this.lockedUser = null;
        this.request = null;
        this.requestedPageScaling = null;
    }

    /**
     *
     * @param lockedUser
     *            The requesting {@link User}, which should be locked.
     * @param request
     *            The {@link ProxyPrintInboxReq} to be chunked.
     * @param pageScaling
     *            The {@link PageScalingEnum}.
     */
    public ProxyPrintInboxReqChunker(final User lockedUser,
            final ProxyPrintInboxReq request,
            final PageScalingEnum pageScaling) {

        this.lockedUser = lockedUser;
        this.request = request;
        this.requestedPageScaling = pageScaling;
    }

    /**
     * @return The standard prefix for error messages.
     */
    private String getErrorMessagePfx() {
        return String.format("Print for user \"%s\" on printer \"%s\"",
                this.lockedUser.getUserId(), this.request.getPrinterName());
    }

    /**
     * Finds the matching printer {@link IppMediaSourceCostDto} for a
     * {@link IppMediaSizeEnum}.
     *
     * @param printerAttrLookup
     *            The {@link PrinterAttrLookup}.
     * @param inboxIppMedia
     *            The {@link IppMediaSizeEnum}.
     * @return The {@link IppMediaSourceCostDto}
     * @throws ProxyPrintException
     *             When proxy printer is not fully configured to support this
     *             request.
     */
    private IppMediaSourceCostDto getMediaSourceForMedia(
            final PrinterAttrLookup printerAttrLookup,
            final IppMediaSizeEnum inboxIppMedia) throws ProxyPrintException {

        final IppMediaSourceCostDto mediaSourceForMedia;

        final List<IppMediaSourceCostDto> mediaSourceCostList =
                printerAttrLookup.findMediaSourcesForMedia(inboxIppMedia);

        /*
         * INVARIANT: At least one (1) media-source MUST be available that
         * matches the single media of the inbox.
         */
        if (mediaSourceCostList.size() == 0) {

            throw new ProxyPrintException(String.format(
                    "%s no media-source for media [%s]", getErrorMessagePfx(),
                    inboxIppMedia.getIppKeyword()));
        }

        if (IS_UNIQUE_MEDIASOURCE_REQUIRED) {

            if (mediaSourceCostList.size() > 1) {
                /*
                 * INVARIANT: If printer has media sources defined, a unique
                 * media-source MUST be available that matches the single media
                 * of the inbox.
                 */
                throw new ProxyPrintException(String.format(
                        "%s no unique media-source for media [%s]"
                                + " (%d sources found)",
                        getErrorMessagePfx(), inboxIppMedia.getIppKeyword(),
                        mediaSourceCostList.size()));
            }

            mediaSourceForMedia = mediaSourceCostList.get(0);

        } else {

            final int iMediaSource;

            if (mediaSourceCostList.size() == 1) {

                iMediaSource = 0;

            } else {
                /*
                 * Pick a random media-source.
                 */
                iMediaSource = NumberUtil.getRandomNumber(0,
                        mediaSourceCostList.size() - 1);
            }

            mediaSourceForMedia = mediaSourceCostList.get(iMediaSource);
        }

        return mediaSourceForMedia;
    }

    /**
     * Chunks the {@link ProxyPrintInboxReq} in separate print jobs per
     * media-source or per vanilla inbox job.
     * <p>
     * As a result the original request parameters "media", "media-source" and
     * "fit-to-page" are set or corrected, and
     * {@link ProxyPrintInboxReq#getJobChunkInfo()} will have at least one (1)
     * {@link ProxyPrintJobChunk}.
     * </p>
     *
     * @param chunkVanillaJobs
     *            When {@code true} a {@link ProxyPrintJobChunk} is created for
     *            each job (of a vanilla inbox)
     * @param iVanillaJob
     *            The zero-based ordinal of the single vanilla job to print. If
     *            {@code null}, all vanilla jobs are printed.
     * @param vanillaJobPageRanges
     *            The job scope page ranges, e.g. "1-2,4,12-".
     * @throws ProxyPrintException
     *             When proxy printer is not fully configured to support this
     *             request, or when vanilla job chunking is requested and the
     *             inbox is not vanilla.
     */
    public void chunk(final boolean chunkVanillaJobs, final Integer iVanillaJob,
            final String vanillaJobPageRanges) throws ProxyPrintException {

        final PrinterDao printerDao =
                ServiceContext.getDaoContext().getPrinterDao();

        final Printer printer = printerDao.findByName(request.getPrinterName());

        final PrinterAttrLookup printerAttrLookup =
                new PrinterAttrLookup(printer);

        final InboxInfoDto inboxInfo =
                INBOX_SERVICE.getInboxInfo(lockedUser.getUserId());

        final String requestedMediaSource = request.getMediaSourceOption();
        final String requestedMedia = request.getMediaOption();

        /*
         * Do all inbox jobs have the same IPP media?
         */
        final IppMediaSizeEnum inboxIppMedia =
                INBOX_SERVICE.checkSingleInboxMedia(inboxInfo);

        final boolean isSingleInboxMedia = inboxIppMedia != null;

        final boolean areMediaSourcesDefined =
                printerAttrLookup.isMediaSourcePresent();

        /*
         * To find out...
         */
        final IppMediaSizeEnum assignedMedia;
        final IppMediaSourceCostDto assignedMediaSource;

        /*
         * Validate requested 'media-source' and 'media' against inbox.
         */
        if (!areMediaSourcesDefined) {

            assignedMediaSource = null;

            if (isSingleInboxMedia) {
                assignedMedia = inboxIppMedia;
            } else {
                assignedMedia = null;
            }

        } else if (requestedMediaSource == null
                || requestedMediaSource.equals(IppKeyword.MEDIA_SOURCE_AUTO)) {

            /*
             * No media-source or 'auto' media-source selected.
             */
            if (isSingleInboxMedia) {

                assignedMediaSource = getMediaSourceForMedia(printerAttrLookup,
                        inboxIppMedia);
                assignedMedia = inboxIppMedia;

            } else {

                assignedMediaSource = null;
                assignedMedia = null;

            }

        } else if (requestedMediaSource
                .equals(IppKeyword.MEDIA_SOURCE_MANUAL)) {

            /*
             * INVARIANT: 'media' MUST be specified for 'manual' print.
             */
            if (requestedMedia == null) {
                throw new ProxyPrintException(
                        String.format("%s no media specified for manual print.",
                                getErrorMessagePfx()));
            }

            assignedMedia = IppMediaSizeEnum.find(requestedMedia);

            /*
             * INVARIANT: requested 'media-source' MUST be present.
             */
            if (assignedMedia == null) {
                throw new ProxyPrintException(
                        String.format("%s media [%s] unknown.",
                                getErrorMessagePfx(), requestedMedia));
            }

            assignedMediaSource = printerAttrLookup.getMediaSourceManual();

        } else {

            /*
             * Get the media-source and related media.
             */
            assignedMediaSource = printerAttrLookup
                    .get(new PrinterDao.MediaSourceAttr(requestedMediaSource));
            /*
             * INVARIANT: requested 'media-source' MUST be present.
             */
            if (assignedMediaSource == null) {
                throw new ProxyPrintException(
                        String.format("%s media source [%s] unknown.",
                                getErrorMessagePfx(), requestedMediaSource));
            }

            final String media = assignedMediaSource.getMedia().getMedia();

            assignedMedia = IppMediaSizeEnum.find(media);

            /*
             * INVARIANT: media of requested 'media-source' MUST be present.
             */
            if (assignedMedia == null) {
                throw new ProxyPrintException(String.format(
                        "%s media [%s] of media source [%s] unknown.",
                        getErrorMessagePfx(), media, requestedMediaSource));
            }
        }

        /*
         * Options to determine.
         */
        final IppMediaSourceCostDto determinedMediaSource;
        final IppMediaSizeEnum determinedMedia;

        /*
         * Create the chunks depending on "vanilla" requirements.
         */
        final ProxyPrintJobChunkInfo printJobChunkInfo;

        if (chunkVanillaJobs) {

            if (iVanillaJob == null) {
                printJobChunkInfo = new ProxyPrintJobChunkInfo(inboxInfo);
            } else {
                printJobChunkInfo = new ProxyPrintJobChunkInfo(inboxInfo,
                        iVanillaJob.intValue(), vanillaJobPageRanges);
            }
        } else {
            printJobChunkInfo = new ProxyPrintJobChunkInfo(inboxInfo,
                    request.getPageRanges());
        }

        request.setJobChunkInfo(printJobChunkInfo);

        /*
         * Determine options.
         */
        if (!areMediaSourcesDefined) {

            determinedMediaSource = assignedMediaSource;
            determinedMedia = assignedMedia;

        } else if (assignedMediaSource == null) {

            /*
             * No (single) media-source assigned: check the print job chunks.
             */
            final Map<String, IppMediaSourceCostDto> collectedMediaSources =
                    new HashMap<>();

            final IppMediaSourceCostDto[] pageChunkMediaSourceCost =
                    new IppMediaSourceCostDto[printJobChunkInfo.getChunks()
                            .size()];

            int i = 0;

            for (final ProxyPrintJobChunk printJobChunk : printJobChunkInfo
                    .getChunks()) {

                final MediaSizeName mediaSizeNameWlk =
                        printJobChunk.getMediaSizeName();

                /*
                 * Skip chunks with custom media size.
                 */
                if (mediaSizeNameWlk == null) {
                    continue;
                }

                final IppMediaSizeEnum ippMediaSize =
                        IppMediaSizeEnum.find(mediaSizeNameWlk);

                final IppMediaSourceCostDto ippMediaSourceCostWlk =
                        getMediaSourceForMedia(printerAttrLookup, ippMediaSize);

                pageChunkMediaSourceCost[i++] = ippMediaSourceCostWlk;

                collectedMediaSources.put(ippMediaSourceCostWlk.getSource(),
                        ippMediaSourceCostWlk);
            }

            /*
             * INVARIANT: there MUST be at least one (1) unique media-source
             * object available that matches the media of the PDF document.
             */
            if (collectedMediaSources.isEmpty()) {

                throw new ProxyPrintException(String.format(
                        "%s: no media-source matches.", getErrorMessagePfx()));
            }

            if (collectedMediaSources.size() == 1) {

                determinedMediaSource = collectedMediaSources.entrySet()
                        .iterator().next().getValue();

                determinedMedia = IppMediaSizeEnum
                        .find(determinedMediaSource.getMedia().getMedia());

            } else {

                determinedMediaSource = null;
                determinedMedia = null;

                i = 0;

                for (final ProxyPrintJobChunk printJobChunk : printJobChunkInfo
                        .getChunks()) {

                    final IppMediaSourceCostDto ippMediaSourceCostWlk =
                            pageChunkMediaSourceCost[i++];

                    setPrintJobChunk(printJobChunk, ippMediaSourceCostWlk,
                            IppMediaSizeEnum.find(ippMediaSourceCostWlk
                                    .getMedia().getMedia()));
                }
            }

        } else {
            determinedMediaSource = assignedMediaSource;
            determinedMedia = assignedMedia;
        }

        if (!areMediaSourcesDefined) {

            for (final ProxyPrintJobChunk printJobChunk : printJobChunkInfo
                    .getChunks()) {

                setPrintJobChunk(printJobChunk, determinedMediaSource,
                        determinedMedia);
            }

            request.setMediaOption(determinedMedia.getIppKeyword());

        } else if (determinedMediaSource != null) {
            /*
             * We have a SINGLE media-source.
             */
            for (final ProxyPrintJobChunk printJobChunk : printJobChunkInfo
                    .getChunks()) {

                setPrintJobChunk(printJobChunk, determinedMediaSource,
                        determinedMedia);
            }
            request.setMediaSourceOption(determinedMediaSource.getSource());
            request.setMediaOption(determinedMedia.getIppKeyword());
        }
    }

    /**
     *
     * @param printJobChunk
     * @param mediaSource
     * @param mediaSize
     */
    private void setPrintJobChunk(final ProxyPrintJobChunk printJobChunk,
            final IppMediaSourceCostDto mediaSource,
            final IppMediaSizeEnum mediaSize) {

        printJobChunk.setAssignedMediaSource(mediaSource);
        printJobChunk.setAssignedMedia(mediaSize);

        final boolean fitToPage;

        if (this.requestedPageScaling == PageScalingEnum.CROP) {

            fitToPage = false;

        } else if (printJobChunk.getMediaSizeName() == null) {

            fitToPage = true;

        } else {

            final int compare = MediaUtils.compareMediaSize(
                    printJobChunk.getMediaSizeName(),
                    printJobChunk.getAssignedMedia().getMediaSizeName());

            if (compare == 0) {
                fitToPage = false;
            } else if (compare < 0) {
                fitToPage = this.requestedPageScaling == PageScalingEnum.EXPAND;
            } else {
                fitToPage = this.requestedPageScaling == PageScalingEnum.SHRINK;
            }

        }

        printJobChunk.setFitToPage(fitToPage);
    }

}
