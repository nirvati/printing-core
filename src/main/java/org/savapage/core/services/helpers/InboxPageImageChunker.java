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
package org.savapage.core.services.helpers;

import java.util.Iterator;
import java.util.List;

import org.savapage.core.imaging.ImageUrl;
import org.savapage.core.inbox.InboxInfoDto;
import org.savapage.core.inbox.InboxInfoDto.InboxJobRange;
import org.savapage.core.inbox.PageImages;
import org.savapage.core.inbox.RangeAtom;
import org.savapage.core.services.InboxService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.impl.InboxServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Batch process to chunk the inbox to {@link PageImages}.
 *
 * @author Rijk Ravestein
 *
 */
public final class InboxPageImageChunker {

    /**
     * The logger.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(InboxPageImageChunker.class);

    /**
     * Number of page chunks.
     */
    private static final int MAX_PAGE_CHUNKS = 7;

    /**
     * Number of pages to show in detail.
     */
    private static final int MAX_DETAIL_PAGES = 5;

    /**
     * .
     */
    private static final InboxService INBOX_SERVICE =
            ServiceContext.getServiceFactory().getInboxService();

    /*
     * Input parameters.
     */
    private final String user;
    private final String uniqueUrlValue;
    private final boolean base64;

    /*
     * Derived parameters.
     */
    private final InboxInfoDto inboxInfo;
    private final int nPagesTot;
    private final int nFirstDetailPage;
    private final int nPagesInChunkPre;
    private final int nPagesInChunkPost;
    private final int nStartChunkPre;
    private final int nStartChunkPost;
    private final int nPagesToChunk;
    private final int nPagesInChunk;

    /**
     * The return value.
     */
    private final PageImages pageImagesOut;

    /**
     * Accumulating tracing info.
     */
    private final StringBuilder chunkTrace = new StringBuilder();

    /*
     * Process variables.
     */
    private Iterator<InboxJobRange> pagesIterWlk;

    private boolean bEofWlk;
    private boolean bPageRangesInitWlk;
    private boolean bPageChunkInitWlk;

    /**
     * Overall page counter.
     */
    private int nPageCountWlk;

    private InboxJobRange pageIterWlk;

    boolean bPageRangeInitWlk = true;

    private Iterator<RangeAtom> rangesIterWlk;
    private RangeAtom rangeIterWlk;
    private InboxInfoDto.InboxJob jobWlk;
    private Integer iJobWlk;

    /**
     * The job index of the first page in a chunk.
     */
    private Integer iJobChunkWlk;

    /**
     * The job of the first page in a chunk.
     */
    private InboxInfoDto.InboxJob jobChunkWlk;

    private int nChunkStartWlk;
    private int nChunkedPagesWlk;

    private boolean bScanPageWlk;
    private boolean bScanPageChunkWlk;

    private ImageUrl imgUrlChunkWlk;
    private ImageUrl imgUrlPageWlk;

    private int pageUrlParmChunkWlk;
    private int iWlk;
    private int iBeginWlk;
    private int iEndWlk;

    private List<RangeAtom> rangesWlk;

    private int nStartNextChunkWlk;
    private boolean bNextPageRangeWlk;
    private boolean bNextPageRangesWlk;
    private boolean bNextPageChunkWlk;

    /**
     *
     * @param user
     *            The unique user id to get the SafePages for.
     * @param firstDetailPage
     *            The first page of the detail sequence: null or LT or EQ to
     *            zero indicates the default first detail page.
     * @param uniqueUrlValue
     *            Value to make the output page URL's unique, so the browser
     *            will not use its cache, but will retrieve the image from the
     *            server again.
     * @param base64
     *            {@code true}: create image URL for inline BASE64 embedding.
     */
    private InboxPageImageChunker(final String user,
            final Integer firstDetailPage, final String uniqueUrlValue,
            final boolean base64) {

        /*
         * Input parameters.
         */
        this.user = user;
        this.uniqueUrlValue = uniqueUrlValue;
        this.base64 = base64;

        /*
         * Derived parameters.
         */
        this.inboxInfo = INBOX_SERVICE.getInboxInfo(user);
        this.nPagesTot = INBOX_SERVICE.calcNumberOfPagesInJobs(inboxInfo);

        //
        if (null == firstDetailPage || firstDetailPage.intValue() <= 0) {

            final int nPageCandidate = nPagesTot - MAX_DETAIL_PAGES + 1;

            if (nPageCandidate < 1) {
                this.nFirstDetailPage = 1;
            } else {
                this.nFirstDetailPage = nPageCandidate;
            }
        } else {
            this.nFirstDetailPage = firstDetailPage.intValue();
        }

        //
        if (nFirstDetailPage <= MAX_DETAIL_PAGES) {
            nPagesInChunkPre = nFirstDetailPage - 1;
        } else {
            nPagesInChunkPre = MAX_DETAIL_PAGES;
        }

        //
        if ((nFirstDetailPage + 2 * MAX_DETAIL_PAGES - 1) > nPagesTot) {
            nPagesInChunkPost =
                    nPagesTot - (nFirstDetailPage + MAX_DETAIL_PAGES - 1);
        } else {
            nPagesInChunkPost = MAX_DETAIL_PAGES;
        }

        // ----------------------------
        nStartChunkPre = nFirstDetailPage - nPagesInChunkPre;
        nStartChunkPost = nFirstDetailPage + MAX_DETAIL_PAGES;

        // ----------------------------------------------------------------
        // Calculate number of pages in regular chunk
        // Algorithm UNDER CONSTRUCTION
        // ----------------------------------------------------------------
        nPagesToChunk = nPagesTot - nPagesInChunkPre - nPagesInChunkPost;

        final int nPagesInChunkCandidate = nPagesToChunk / MAX_PAGE_CHUNKS;

        if (nPagesInChunkCandidate < MAX_DETAIL_PAGES) {
            nPagesInChunk = MAX_DETAIL_PAGES;
        } else {
            nPagesInChunk = nPagesInChunkCandidate;
        }

        /*
         * Create and initialize return object.
         */
        this.pageImagesOut = new PageImages();

        for (final InboxInfoDto.InboxJob jobIn : inboxInfo.getJobs()) {
            this.pageImagesOut.addJob(jobIn.getTitle(), jobIn.getPages(),
                    jobIn.getRotate(), jobIn.getDrm(), jobIn.getMedia());
        }
    }

    /**
     *
     */
    private void onInit() {

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("MAX_PAGE_CHUNKS  [" + MAX_PAGE_CHUNKS + "]");
            LOGGER.trace("MAX_DETAIL_PAGES [" + MAX_DETAIL_PAGES + "]");
            LOGGER.trace("nPagesTot [" + nPagesTot + "]");
            LOGGER.trace("nFirstDetailPage [" + nFirstDetailPage + "]");
            LOGGER.trace("nPagesInChunkPre [" + nPagesInChunkPre + "]");
            LOGGER.trace("nPagesInChunkPost [" + nPagesInChunkPost + "]");
            LOGGER.trace("nStartChunkPre [" + nStartChunkPre + "]");
            LOGGER.trace("nStartChunkPost [" + nStartChunkPost + "]");
            LOGGER.trace("nPagesToChunk [" + nPagesToChunk + "]");
            LOGGER.trace("nPagesInChunk [" + nPagesInChunk + "]");
        }

        pagesIterWlk = inboxInfo.getPages().iterator();

        bEofWlk = !pagesIterWlk.hasNext();
        bPageRangesInitWlk = true;
        bPageChunkInitWlk = true;

        nPageCountWlk = 1; // overall page counter

        pageIterWlk = null;
        if (!bEofWlk) {
            pageIterWlk = pagesIterWlk.next(); // first page
        }

        bPageRangeInitWlk = true;

        // ---------------------------------------
        // Walking variables used in the loop
        // ---------------------------------------
        rangesIterWlk = null;
        rangeIterWlk = null;

        jobWlk = null;
        iJobWlk = null;

        /*
         * The job index of the first page in a chunk.
         */
        iJobChunkWlk = null;
        /*
         * The job of the first page in a chunk.
         */
        jobChunkWlk = null;

        nChunkStartWlk = nPageCountWlk;
        nChunkedPagesWlk = 0;

        bScanPageWlk = false;
        bScanPageChunkWlk = false;

        imgUrlChunkWlk = null;
        imgUrlPageWlk = null;

        pageUrlParmChunkWlk = 0;
        iWlk = 0;
        iBeginWlk = 0;
        iEndWlk = 0;

        rangesWlk = null;

        nStartNextChunkWlk = 0;
        bNextPageRangeWlk = false;
        bNextPageRangesWlk = false;
        bNextPageChunkWlk = false;
    }

    /**
     * .
     */
    private void onExit() {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(chunkTrace.toString());
        }
    }

    /**
     *
     * @param job
     *            The inbox job.
     * @return The {@link ImageUrl}.
     */
    private ImageUrl createImageUrl(final InboxInfoDto.InboxJob job) {

        final ImageUrl imgUrlPage = new ImageUrl();

        imgUrlPage.setUser(this.user);
        imgUrlPage.setJob(job.getFile());
        imgUrlPage.setThumbnail(true);
        imgUrlPage.setBase64(base64);

        final String rotate = job.getRotate();

        if (rotate != null && !rotate.equals("0")) {
            imgUrlPage.setRotate(job.getRotate());
        }

        if (!uniqueUrlValue.isEmpty()) {
            imgUrlPage.setNocache(uniqueUrlValue);
        }

        return imgUrlPage;
    }

    /**
     *
     */
    private void onInitPageRanges() {

        bPageRangesInitWlk = false;

        final InboxJobRange page = pageIterWlk;

        iJobWlk = page.getJob();
        jobWlk = inboxInfo.getJobs().get(iJobWlk);

        imgUrlPageWlk = createImageUrl(jobWlk);

        bScanPageWlk = InboxServiceImpl.isScanJobFilename(jobWlk.getFile());

        if (bScanPageWlk) {
            rangesWlk = INBOX_SERVICE.createSortedRangeArray("1");
        } else {
            rangesWlk = INBOX_SERVICE.createSortedRangeArray(page.getRange());
        }

        rangesIterWlk = rangesWlk.iterator();

        if (rangesIterWlk.hasNext()) {
            rangeIterWlk = rangesIterWlk.next();
        } else {
            rangeIterWlk = null;
        }

        bPageRangeInitWlk = true;
    }

    /**
     *
     */
    private void onInitPageRange() {

        bPageRangeInitWlk = false;

        final Integer start = rangeIterWlk.pageBegin;

        if (start == null) {
            iBeginWlk = 0;
        } else {
            iBeginWlk = start.intValue() - 1;
        }

        //
        final Integer end = rangeIterWlk.pageEnd;

        if (end == null) {
            iEndWlk = jobWlk.getPages();
        } else {
            iEndWlk = end.intValue();
        }

        iWlk = iBeginWlk;

        pageImagesOut.addPagesSelected(iJobWlk, iEndWlk - iBeginWlk);
    }

    /**
     * .
     */
    private void onInitPageChunk() {

        bPageChunkInitWlk = false;

        iJobChunkWlk = iJobWlk;
        jobChunkWlk = jobWlk;

        nChunkStartWlk = nPageCountWlk;
        nChunkedPagesWlk = 0;

        bScanPageChunkWlk = bScanPageWlk;
        imgUrlChunkWlk = new ImageUrl(imgUrlPageWlk);

        pageUrlParmChunkWlk = iWlk;

        // calc the next end-of-chunk
        if (nPageCountWlk == nStartChunkPre) {
            nStartNextChunkWlk = nPageCountWlk + nPagesInChunkPre;
        } else if (nPageCountWlk == nStartChunkPost) {
            nStartNextChunkWlk = nPageCountWlk + nPagesInChunkPost;
        } else {
            nStartNextChunkWlk = nPageCountWlk + nPagesInChunk;
        }
    }

    /**
     * Process the inbox page.
     */
    private void onProcessPage() {
        // no code intended
    }

    /**
     * Next page from the inbox.
     */
    private void onNextPage() {
        iWlk++;
        bNextPageRangeWlk = !(iWlk < iEndWlk);
        bNextPageRangesWlk = false;
    }

    /**
     * Next chunked page.
     */
    private void onNextChunkedPage() {

        nChunkedPagesWlk++;

        nPageCountWlk++;

        bNextPageChunkWlk = nPageCountWlk == nStartChunkPre
                || nPageCountWlk == nStartChunkPost
                || ((nFirstDetailPage <= nPageCountWlk)
                        && nPageCountWlk < (nFirstDetailPage
                                + MAX_DETAIL_PAGES))
                || nPageCountWlk == nStartNextChunkWlk;
    }

    /**
     *
     */
    private void onNextPageRange() {
        if (rangesIterWlk.hasNext()) {
            rangeIterWlk = rangesIterWlk.next(); // next page-range
            bPageRangeInitWlk = true;
        } else {
            rangeIterWlk = null;
            bNextPageRangesWlk = true;
        }
    }

    /**
     *
     */
    private void onNextPageRanges() {
        bEofWlk = !pagesIterWlk.hasNext();

        if (bEofWlk) {
            pagesIterWlk = null;
            bNextPageChunkWlk = true;
        } else {
            pageIterWlk = pagesIterWlk.next(); // next page (ranges)
            bPageRangesInitWlk = true;
        }
    }

    /**
    *
    */
    private void onNextPageChunk() {

        final PageImages.PageImage pageTmp = new PageImages.PageImage();

        /*
         * Flush current chunk.
         */
        if (!bScanPageChunkWlk) {
            imgUrlChunkWlk.setPage(String.valueOf(pageUrlParmChunkWlk));
        }

        pageTmp.setUrl(imgUrlChunkWlk.composeImageUrl());

        pageTmp.setJob(iJobChunkWlk);
        pageTmp.setRotate(jobChunkWlk.getRotate());
        pageTmp.setDrm(jobChunkWlk.getDrm());
        pageTmp.setMedia(jobChunkWlk.getMedia());
        pageTmp.setPages(nChunkedPagesWlk);

        pageImagesOut.getPages().add(pageTmp);

        if (LOGGER.isTraceEnabled()) {
            chunkTrace.append("\n[").append(nChunkStartWlk).append("-")
                    .append((nPageCountWlk - 1)).append(":")
                    .append(nChunkedPagesWlk + "] [").append(pageTmp.getUrl())
                    .append("]");
        }

        bPageChunkInitWlk = true;
    }

    /**
     *
     * @return The {@link PageImages}.
     */
    private PageImages process() {

        this.onInit();

        while (!bEofWlk) {

            if (bPageRangesInitWlk) {
                this.onInitPageRanges();
            }
            if (bPageRangeInitWlk) {
                this.onInitPageRange();
            }
            if (bPageChunkInitWlk) {
                this.onInitPageChunk();
            }

            this.onProcessPage();

            this.onNextPage();
            this.onNextChunkedPage();

            if (bNextPageRangeWlk) {
                this.onNextPageRange();
            }
            if (bNextPageRangesWlk) {
                this.onNextPageRanges();
            }
            if (bNextPageChunkWlk) {
                this.onNextPageChunk();
            }
        }

        this.onExit();

        return pageImagesOut;
    }

    /**
     * Chunks the inbox to {@link PageImages}.
     *
     * @param user
     *            The unique user id to get the SafePages for.
     * @param firstDetailPage
     *            The first page of the detail sequence: null or LT or EQ to
     *            zero indicates the default first detail page.
     * @param uniqueUrlValue
     *            Value to make the output page URL's unique, so the browser
     *            will not use its cache, but will retrieve the image from the
     *            server again.
     * @param base64
     *            {@code true}: create image URL for inline BASE64 embedding.
     * @return The {@link PageImages}.
     */
    public static PageImages chunk(final String user,
            final Integer firstDetailPage, final String uniqueUrlValue,
            final boolean base64) {
        return new InboxPageImageChunker(user, firstDetailPage, uniqueUrlValue,
                base64).process();
    }

}
