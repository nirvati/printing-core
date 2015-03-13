/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2014 Datraverse B.V.
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
package org.savapage.core.print.proxy;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.print.attribute.standard.MediaSizeName;

import org.savapage.core.inbox.InboxInfoDto;
import org.savapage.core.inbox.InboxInfoDto.InboxJob;
import org.savapage.core.inbox.InboxInfoDto.InboxJobRange;
import org.savapage.core.inbox.RangeAtom;
import org.savapage.core.services.InboxService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.util.MediaUtils;

/**
 *
 * @author Datraverse B.V.
 *
 */
public final class ProxyPrintJobChunkInfo {

    private final InboxInfoDto inboxInfo;

    private final List<ProxyPrintJobChunk> chunks = new ArrayList<>();

    /**
    *
    */
    private static final InboxService INBOX_SERVICE = ServiceContext
            .getServiceFactory().getInboxService();

    /**
     * Prevent public default instantiation.
     */
    @SuppressWarnings("unused")
    private ProxyPrintJobChunkInfo() {
        this.inboxInfo = null;
    }

    /**
     * Creates {@link ProxyPrintJobChunkInfo} with a single
     * {@link ProxyPrintJobChunk}. The {@link InboxInfoDto} is irrelevant.
     *
     * @param jobChunk
     *            The {@link ProxyPrintJobChunk} to add.
     */
    public ProxyPrintJobChunkInfo(final ProxyPrintJobChunk jobChunk) {
        this.inboxInfo = null;
        this.addChunk(jobChunk);
    }

    /**
     * Creates an ordinal list of {@link ProxyPrintJobChunk} with all inbox
     * jobs. Each entry on the list represents a chunk of pages that correspond
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

        this.inboxInfo =
                INBOX_SERVICE.filterInboxInfoPages(inboxInfoIn,
                        InboxJobRange.FULL_PAGE_RANGE);

        for (final InboxJobRange jobRange : this.inboxInfo.getPages()) {

            final int iJob = jobRange.getJob().intValue();
            final InboxJob inboxJob = inboxInfo.getJobs().get(iJob);
            final int nJobPages = inboxJob.getPages().intValue();

            final MediaSizeName mediaSizeNameWlk =
                    MediaUtils.getMediaSizeFromInboxMedia(inboxJob.getMedia());

            final ProxyPrintJobChunk printJobChunkWlk =
                    new ProxyPrintJobChunk();

            printJobChunkWlk.setMediaSizeName(mediaSizeNameWlk);

            this.addChunk(printJobChunkWlk);

            for (final RangeAtom rangeAtom : INBOX_SERVICE
                    .createSortedRangeArray(jobRange.getRange())) {

                final ProxyPrintJobChunkRange atom =
                        new ProxyPrintJobChunkRange();

                atom.pageBegin = rangeAtom.calcPageFrom();
                atom.pageEnd = rangeAtom.calcPageTo(nJobPages);
                atom.setJob(iJob);

                printJobChunkWlk.getRanges().add(atom);
            }
        }
    }

    /**
     * Creates an ordinal list of {@link ProxyPrintJobChunk} of the selected
     * pages of an inbox. Each entry on the list represents a chunk of pages
     * with the same media size.
     *
     * @param inboxInfoIn
     *            The {@link InboxInfoDto}.
     * @param selectedPageRanges
     *            The page ranges, e.g. "1-2,4,12-".
     */
    public ProxyPrintJobChunkInfo(final InboxInfoDto inboxInfoIn,
            final String selectedPageRanges) {

        this.inboxInfo =
                INBOX_SERVICE.filterInboxInfoPages(inboxInfoIn,
                        selectedPageRanges);

        /*
         * First Page.
         */
        ProxyPrintJobChunk printJobChunkWlk = null;

        MediaSizeName mediaSizeNameItem = null;
        MediaSizeName mediaSizeNameWlk = null;

        final Iterator<InboxJobRange> iterPages =
                inboxInfo.getPages().iterator();

        while (iterPages.hasNext()) {

            /*
             * Next page.
             */
            final InboxJobRange jobRange = iterPages.next();

            final int iJob = jobRange.getJob().intValue();
            final InboxJob inboxJob = inboxInfo.getJobs().get(iJob);
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

                this.addChunk(printJobChunkWlk);
            }

            for (final RangeAtom rangeAtom : INBOX_SERVICE
                    .createSortedRangeArray(jobRange.getRange())) {

                final ProxyPrintJobChunkRange atom =
                        new ProxyPrintJobChunkRange();

                atom.pageBegin = rangeAtom.calcPageFrom();
                atom.pageEnd = rangeAtom.calcPageTo(nJobPages);
                atom.setJob(iJob);

                printJobChunkWlk.getRanges().add(atom);
            }
        }
    }

    public List<ProxyPrintJobChunk> getChunks() {
        return chunks;
    }

    private void addChunk(final ProxyPrintJobChunk chunk) {
        this.chunks.add(chunk);
    }

    public InboxInfoDto getInboxInfo() {
        return inboxInfo;
    }

}
