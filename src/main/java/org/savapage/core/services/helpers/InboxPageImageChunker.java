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
package org.savapage.core.services.helpers;

import java.util.Iterator;
import java.util.List;

import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.imaging.ImageUrl;
import org.savapage.core.inbox.InboxInfoDto;
import org.savapage.core.inbox.InboxInfoDto.InboxJobRange;
import org.savapage.core.inbox.PageImages;
import org.savapage.core.inbox.RangeAtom;
import org.savapage.core.services.InboxService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.impl.InboxServiceImpl;
import org.savapage.core.util.DateUtil;
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

    /**
     * The job expiration period in milliseconds.
     */
    private long msecJobExpiry;

    /**
     * The number of milliseconds before job expiration when a job is signaled
     * as nearing expiration. When zero (0) expiration is <i>not</i> signaled.
     */
    private long msecJobExpirySignal;

    /**
     * The job expiration reference time in milliseconds (the current time).
     *
     */
    private long msecCurrentTime;

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
     * The collected {@link PageImages}.
     */
    private final PageImages pageImages;

    /**
     * Accumulating tracing info.
     */
    private final StringBuilder chunkTrace = new StringBuilder();

    /*
     * Process "walk" variables.
     */

    /**
     * Iterator over all inbox page ranges.
     */
    private Iterator<InboxJobRange> pageRangesIterWlk;

    private boolean isEofWlk;

    private boolean initPageRangesWlk;
    private boolean initPageRangeWlk;
    private boolean initPageChunkWlk;

    private boolean nextPageRangesWlk;
    private boolean nextPageRangeWlk;
    private boolean nextPageChunkWlk;

    /**
     * Overall page counter.
     */
    private int nPageCountWlk;

    private InboxJobRange pageRangesWlk;

    private Iterator<RangeAtom> pageRangeAtomIterWlk;
    private RangeAtom pageRangeAtomWlk;

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

    /**
     * Number of pages in a chunked page.
     */
    private int nChunkedPagesWlk;

    /**
     * Number of overlay pages in a chunked page.
     */
    private int nChunkedOverlayPagesWlk;

    private boolean bScanPageWlk;
    private boolean bScanPageChunkWlk;

    private ImageUrl imgUrlChunkWlk;

    /**
     * {@link ImageUrl} template for atoms in a page range.
     */
    private ImageUrl imgUrlPageRangesWlk;

    /**
     *
     */
    private int pageUrlParmChunkWlk;

    private int iWlk;
    private int iBeginWlk;
    private int iEndWlk;

    private int nStartNextChunkWlk;

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
         * Create and initialize with jobs info.
         */
        this.pageImages = new PageImages();

        for (final InboxInfoDto.InboxJob jobIn : inboxInfo.getJobs()) {
            this.pageImages.addJob(jobIn.getTitle(), jobIn.getPages(),
                    jobIn.getRotate(), jobIn.getLandscapeView(), jobIn.getDrm(),
                    jobIn.getMedia());
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

        msecCurrentTime = System.currentTimeMillis();

        /*
         * Signaling expired jobs.
         */
        final ConfigManager cm = ConfigManager.instance();

        msecJobExpiry = cm.getConfigInt(Key.PRINT_IN_JOB_EXPIRY_MINS, 0)
                * DateUtil.DURATION_MSEC_MINUTE;

        if (msecJobExpiry > 0) {
            msecJobExpirySignal = ConfigManager.instance().getConfigInt(
                    Key.WEBAPP_USER_PRINT_IN_JOB_EXPIRY_SIGNAL_MINS, 0)
                    * DateUtil.DURATION_MSEC_MINUTE;
        } else {
            msecJobExpirySignal = 0;
        }

        //
        pageRangesIterWlk = inboxInfo.getPages().iterator();

        isEofWlk = !pageRangesIterWlk.hasNext();

        initPageRangesWlk = true;
        initPageRangeWlk = true;
        initPageChunkWlk = true;

        nPageCountWlk = 1;

        pageRangesWlk = null;
        if (!isEofWlk) {
            pageRangesWlk = pageRangesIterWlk.next(); // first page
        }

        // ---------------------------------------
        // Walking variables used in the loop
        // ---------------------------------------
        pageRangeAtomIterWlk = null;
        pageRangeAtomWlk = null;

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
        nChunkedOverlayPagesWlk = 0;

        bScanPageWlk = false;
        bScanPageChunkWlk = false;

        imgUrlChunkWlk = null;
        imgUrlPageRangesWlk = null;

        pageUrlParmChunkWlk = 0;
        iWlk = 0;
        iBeginWlk = 0;
        iEndWlk = 0;

        nStartNextChunkWlk = 0;
        nextPageRangeWlk = false;
        nextPageRangesWlk = false;
        nextPageChunkWlk = false;
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
     * Creates a generic {@link ImageUrl} for a job.
     *
     * @param job
     *            The inbox job.
     * @return The {@link ImageUrl}.
     */
    private ImageUrl createJobImageUrl(final InboxInfoDto.InboxJob job) {

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

        initPageRangesWlk = false;

        final InboxJobRange page = pageRangesWlk;

        //
        iJobWlk = page.getJob();
        jobWlk = inboxInfo.getJobs().get(iJobWlk);

        //
        imgUrlPageRangesWlk = createJobImageUrl(jobWlk);

        bScanPageWlk = InboxServiceImpl.isScanJobFilename(jobWlk.getFile());

        final List<RangeAtom> rangeAtomList;

        if (bScanPageWlk) {
            rangeAtomList = INBOX_SERVICE.createSortedRangeArray("1");
        } else {
            rangeAtomList =
                    INBOX_SERVICE.createSortedRangeArray(page.getRange());
        }

        pageRangeAtomIterWlk = rangeAtomList.iterator();

        if (pageRangeAtomIterWlk.hasNext()) {
            pageRangeAtomWlk = pageRangeAtomIterWlk.next();
        } else {
            pageRangeAtomWlk = null;
        }

        initPageRangeWlk = true;
    }

    /**
     *
     */
    private void onInitPageRange() {

        initPageRangeWlk = false;

        final Integer start = pageRangeAtomWlk.pageBegin;

        if (start == null) {
            iBeginWlk = 0;
        } else {
            iBeginWlk = start.intValue() - 1;
        }

        //
        final Integer end = pageRangeAtomWlk.pageEnd;

        if (end == null) {
            iEndWlk = jobWlk.getPages();
        } else {
            iEndWlk = end.intValue();
        }

        iWlk = iBeginWlk;

        pageImages.addPagesSelected(iJobWlk, iEndWlk - iBeginWlk);
    }

    /**
     * .
     */
    private void onInitPageChunk() {

        initPageChunkWlk = false;

        //
        iJobChunkWlk = iJobWlk;
        jobChunkWlk = jobWlk;

        //
        nChunkStartWlk = nPageCountWlk;
        nChunkedPagesWlk = 0;
        nChunkedOverlayPagesWlk = 0;

        bScanPageChunkWlk = bScanPageWlk;

        imgUrlChunkWlk = new ImageUrl(imgUrlPageRangesWlk);

        pageUrlParmChunkWlk = iWlk;

        /*
         * Calc the next end-of-chunk.
         */
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

        if (jobWlk.getOverlay() != null
                && jobWlk.getOverlay().containsKey(Integer.valueOf(iWlk))) {
            nChunkedOverlayPagesWlk++;
        }

        iWlk++;
        nextPageRangeWlk = !(iWlk < iEndWlk);
        nextPageRangesWlk = false;
    }

    /**
     * Next chunked page.
     */
    private void onNextChunkedPage() {

        nChunkedPagesWlk++;

        nPageCountWlk++;

        nextPageChunkWlk = nPageCountWlk == nStartChunkPre
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
        if (pageRangeAtomIterWlk.hasNext()) {
            pageRangeAtomWlk = pageRangeAtomIterWlk.next(); // next page-range
            initPageRangeWlk = true;
        } else {
            pageRangeAtomWlk = null;
            nextPageRangesWlk = true;
        }
    }

    /**
     *
     */
    private void onNextPageRanges() {
        isEofWlk = !pageRangesIterWlk.hasNext();

        if (isEofWlk) {
            pageRangesIterWlk = null;
            nextPageChunkWlk = true;
        } else {
            pageRangesWlk = pageRangesIterWlk.next(); // next page (ranges)
            initPageRangesWlk = true;
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

        pageTmp.setOverlay(Boolean.valueOf(
                jobChunkWlk.getOverlay() != null && jobChunkWlk.getOverlay()
                        .containsKey(Integer.valueOf(pageUrlParmChunkWlk))));

        if (pageTmp.getOverlay()) {
            pageTmp.setOverlaySVG64(jobChunkWlk.getOverlay()
                    .get(Integer.valueOf(pageUrlParmChunkWlk)).getSvg64());
        }

        pageTmp.setMedia(jobChunkWlk.getMedia());
        pageTmp.setPages(nChunkedPagesWlk);
        pageTmp.setOverlayPages(nChunkedOverlayPagesWlk);

        if (msecJobExpirySignal > 0) {
            pageTmp.setExpiryTime(jobChunkWlk.getCreatedTime() + msecJobExpiry);
            pageTmp.setExpiryTimeSignal(msecJobExpirySignal);
        } else {
            pageTmp.setExpiryTime(Long.valueOf(0));
        }

        pageImages.getPages().add(pageTmp);

        if (LOGGER.isTraceEnabled()) {
            chunkTrace.append("\n[").append(nChunkStartWlk).append("-")
                    .append((nPageCountWlk - 1)).append(":")
                    .append(nChunkedPagesWlk + "] [").append(pageTmp.getUrl())
                    .append("]");
        }

        initPageChunkWlk = true;
    }

    /**
     *
     * @return The {@link PageImages}.
     */
    private PageImages process() {

        this.onInit();

        while (!isEofWlk) {

            if (initPageRangesWlk) {
                this.onInitPageRanges();
            }
            if (initPageRangeWlk) {
                this.onInitPageRange();
            }
            if (initPageChunkWlk) {
                this.onInitPageChunk();
            }

            this.onProcessPage();

            this.onNextPage();
            this.onNextChunkedPage();

            if (nextPageRangeWlk) {
                this.onNextPageRange();
            }
            if (nextPageRangesWlk) {
                this.onNextPageRanges();
            }
            if (nextPageChunkWlk) {
                this.onNextPageChunk();
            }
        }

        this.onExit();

        return pageImages;
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
