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
package org.savapage.core.print.proxy;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.print.attribute.standard.MediaSizeName;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.savapage.core.inbox.InboxInfoDto;
import org.savapage.core.inbox.InboxInfoDto.InboxJob;
import org.savapage.core.inbox.InboxInfoDto.InboxJobRange;
import org.savapage.core.inbox.RangeAtom;
import org.savapage.core.services.InboxService;
import org.savapage.core.services.ProxyPrintService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.util.MediaUtils;

/**
 * Page sequence chunks of a proxy print request.
 *
 * @author Rijk Ravestein
 *
 */
public final class ProxyPrintJobChunkInfo {

    private final InboxInfoDto filteredInboxInfo;

    private final List<ProxyPrintJobChunk> chunks = new ArrayList<>();

    /**
     * .
     */
    private static final InboxService INBOX_SERVICE =
            ServiceContext.getServiceFactory().getInboxService();

    /**
     * .
     */
    private static final ProxyPrintService PROXY_PRINT_SERVICE =
            ServiceContext.getServiceFactory().getProxyPrintService();

    /**
     * Prevent public default instantiation.
     */
    @SuppressWarnings("unused")
    private ProxyPrintJobChunkInfo() {
        this.filteredInboxInfo = null;
    }

    /**
     * Creates {@link ProxyPrintJobChunkInfo} with a single
     * {@link ProxyPrintJobChunk}. The {@link InboxInfoDto} is irrelevant.
     *
     * @param jobChunk
     *            The {@link ProxyPrintJobChunk} to add.
     */
    public ProxyPrintJobChunkInfo(final ProxyPrintJobChunk jobChunk) {
        this.filteredInboxInfo = null;
        this.addChunk(jobChunk);
    }

    /**
     * Creates an ordinal list of {@link ProxyPrintJobChunk} with all inbox
     * jobs: each entry on the list represents a chunk of pages that correspond
     * with a single inbox job.
     *
     * @param inboxInfoIn
     *            The {@link InboxInfoDto}.
     * @throws ProxyPrintException
     *             When the {@link InboxInfoDto} is NOT vanilla.
     */
    public ProxyPrintJobChunkInfo(final InboxInfoDto inboxInfoIn)
            throws ProxyPrintException {

        if (!INBOX_SERVICE.isInboxVanilla(inboxInfoIn)) {
            throw new ProxyPrintException("Inbox was edited by user");
        }

        this.filteredInboxInfo = INBOX_SERVICE.filterInboxInfoPages(inboxInfoIn,
                RangeAtom.FULL_PAGE_RANGE);

        for (final InboxJobRange jobRange : this.filteredInboxInfo.getPages()) {

            final int iJob = jobRange.getJob().intValue();
            final InboxJob inboxJob = filteredInboxInfo.getJobs().get(iJob);
            final int nJobPages = inboxJob.getPages().intValue();

            final MediaSizeName mediaSizeNameWlk =
                    MediaUtils.getMediaSizeFromInboxMedia(inboxJob.getMedia());

            final ProxyPrintJobChunk printJobChunkWlk =
                    new ProxyPrintJobChunk();

            printJobChunkWlk.setJobName(inboxJob.getTitle());
            printJobChunkWlk.setMediaSizeName(mediaSizeNameWlk);
            printJobChunkWlk.setDrm(BooleanUtils.isTrue(inboxJob.getDrm()));

            this.addChunk(printJobChunkWlk);

            addJobRangesToJobChunk(printJobChunkWlk, iJob, nJobPages, jobRange);
        }
    }

    /**
     * Creates an ordinal list of {@link ProxyPrintJobChunk} with all page
     * ranges of a single vanilla inbox job: each entry on the list represents a
     * chunk of pages that correspond to a single inbox job.
     *
     * @param inboxInfoIn
     *            The {@link InboxInfoDto}.
     * @param iVanillaJob
     *            The zero-based ordinal of the single vanilla job to chunk.
     * @param vanillaJobPageRanges
     *            The job scope page ranges, e.g. "1-2,4,12-".
     * @throws ProxyPrintException
     *             When the {@link InboxInfoDto} is NOT vanilla.
     */
    public ProxyPrintJobChunkInfo(final InboxInfoDto inboxInfoIn,
            final int iVanillaJob, final String vanillaJobPageRanges)
            throws ProxyPrintException {

        if (!INBOX_SERVICE.isInboxVanilla(inboxInfoIn)) {
            throw new ProxyPrintException("Inbox was edited by user");
        }

        this.filteredInboxInfo = INBOX_SERVICE.filterInboxInfoPages(inboxInfoIn,
                RangeAtom.FULL_PAGE_RANGE);

        for (final InboxJobRange jobRange : this.filteredInboxInfo.getPages()) {

            final int iJob = jobRange.getJob().intValue();

            if (iJob != iVanillaJob) {
                continue;
            }

            final InboxJob inboxJob = filteredInboxInfo.getJobs().get(iJob);
            final int nJobPages = inboxJob.getPages().intValue();

            final MediaSizeName mediaSizeNameWlk =
                    MediaUtils.getMediaSizeFromInboxMedia(inboxJob.getMedia());

            final ProxyPrintJobChunk printJobChunkWlk =
                    new ProxyPrintJobChunk();

            printJobChunkWlk.setJobName(inboxJob.getTitle());
            printJobChunkWlk.setMediaSizeName(mediaSizeNameWlk);
            printJobChunkWlk.setDrm(inboxJob.getDrm());

            this.addChunk(printJobChunkWlk);

            final InboxJobRange jobRangeWork;

            if (StringUtils.isBlank(vanillaJobPageRanges)) {
                jobRangeWork = jobRange;
            } else {
                jobRangeWork = new InboxJobRange();
                jobRangeWork.setRange(vanillaJobPageRanges);
            }

            addJobRangesToJobChunk(printJobChunkWlk, iJob, nJobPages,
                    jobRangeWork);

            break;
        }
    }

    /**
     * Creates an ordinal list of {@link ProxyPrintJobChunk} of the selected
     * pages of an inbox: each entry on the list represents a chunk of pages
     * with the same media size.
     *
     * @param inboxInfoIn
     *            The {@link InboxInfoDto}.
     * @param selectedPageRanges
     *            The inbox scope page ranges, e.g. "1-2,4,12-".
     */
    public ProxyPrintJobChunkInfo(final InboxInfoDto inboxInfoIn,
            final String selectedPageRanges) {

        this.filteredInboxInfo = INBOX_SERVICE.filterInboxInfoPages(inboxInfoIn,
                selectedPageRanges);

        /*
         * First Page.
         */
        ProxyPrintJobChunk printJobChunkWlk = null;

        MediaSizeName mediaSizeNameItem = null;
        MediaSizeName mediaSizeNameWlk = null;

        boolean isDrm = false;

        final Iterator<InboxJobRange> iterPages =
                filteredInboxInfo.getPages().iterator();

        while (iterPages.hasNext()) {

            /*
             * Next page.
             */
            final InboxJobRange jobRange = iterPages.next();

            final int iJob = jobRange.getJob().intValue();
            final InboxJob inboxJob = filteredInboxInfo.getJobs().get(iJob);
            final int nJobPages = inboxJob.getPages().intValue();

            mediaSizeNameWlk =
                    MediaUtils.getMediaSizeFromInboxMedia(inboxJob.getMedia());
            /*
             * New page size?
             */
            if (printJobChunkWlk == null
                    || mediaSizeNameItem != mediaSizeNameWlk) {

                // create new
                mediaSizeNameItem = mediaSizeNameWlk;

                printJobChunkWlk = new ProxyPrintJobChunk();
                printJobChunkWlk.setMediaSizeName(mediaSizeNameWlk);

                // Initialize DRM.
                isDrm = BooleanUtils.isTrue(inboxJob.getDrm());

                // Note: do NOT set chunk job name.

                this.addChunk(printJobChunkWlk);
            }

            // Overwrite chunk with DRM of current job.
            printJobChunkWlk
                    .setDrm(isDrm || BooleanUtils.isTrue(inboxJob.getDrm()));

            addJobRangesToJobChunk(printJobChunkWlk, iJob, nJobPages, jobRange);
        }

        if (this.chunks.size() == 1) {
            this.chunks.get(0).setLogicalJobPages(
                    PROXY_PRINT_SERVICE.getLogicalJobPages(filteredInboxInfo));
        }
    }

    /**
     * Adds job ranges to a {@link ProxyPrintJobChunk}.
     *
     * @param printJobChunkWlk
     *            The {@link ProxyPrintJobChunk} to add the ranges to.
     * @param iJob
     *            The zero-based job ordinal.
     * @param nJobPages
     *            The number of pages in the job
     * @param jobRange
     *            The {@link InboxJobRange}.
     */
    private static void addJobRangesToJobChunk(
            final ProxyPrintJobChunk printJobChunkWlk, final int iJob,
            final int nJobPages, final InboxJobRange jobRange) {

        for (final RangeAtom rangeAtom : INBOX_SERVICE
                .createSortedRangeArray(jobRange.getRange())) {

            final ProxyPrintJobChunkRange atom = new ProxyPrintJobChunkRange();

            atom.pageBegin = rangeAtom.calcPageFrom();
            atom.pageEnd = rangeAtom.calcPageTo(nJobPages);
            atom.setJob(iJob);

            printJobChunkWlk.getRanges().add(atom);
        }
    }

    public List<ProxyPrintJobChunk> getChunks() {
        return chunks;
    }

    private void addChunk(final ProxyPrintJobChunk chunk) {
        this.chunks.add(chunk);
    }

    /**
     * Gets the filtered {@link InboxInfoDto}. Note that this object might not
     * have the same content as the original persistent equivalent.
     *
     * @return The filtered {@link InboxInfoDto}.
     */
    public InboxInfoDto getFilteredInboxInfo() {
        return filteredInboxInfo;
    }

}
