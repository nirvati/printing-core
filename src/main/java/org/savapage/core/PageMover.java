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
package org.savapage.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.savapage.core.config.ConfigManager;
import org.savapage.core.inbox.InboxInfoDto;
import org.savapage.core.inbox.RangeAtom;
import org.savapage.core.inbox.InboxInfoDto.InboxJobRange;
import org.savapage.core.services.InboxService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.impl.InboxServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Datraverse B.V.
 *
 */
public final class PageMover {

    /**
     * The logger.
     */
    private static final Logger LOGGER = LoggerFactory
            .getLogger(PageMover.class);

    /**
    *
    */
    private static final InboxService INBOX_SERVICE = ServiceContext
            .getServiceFactory().getInboxService();

    /**
     *
     */
    private InboxInfoDto jobinfo;

    /**
     *
     */
    private String nRanges;

    /**
     * 1-based page ordinal to move at. If this ordinal is negative, the move is
     * NOT executed, and this method works as a DELETE.
     */
    private int nPage2Move2;

    /**
     *
     */
    private boolean deleteRanges;

    /**
     *
     */
    private String workdir;

    /**
     *
     */
    private String user;

    /**
     *
     */
    private boolean isUpdateUser = false;

    /**
     * The resulting new job pages.
     */
    private final ArrayList<InboxInfoDto.InboxJobRange> jobPagesNew =
            new ArrayList<>();

    /**
     * The job pages moved.
     */
    private final List<InboxInfoDto.InboxJobRange> jobPagesMoved =
            new ArrayList<>();

    /**
     *
     */
    private Integer iJobPageNew2Move2 = null;

    /**
     *
     */
    private int nPagesTot;

    /**
     * The ranges which should be moved.
     */
    private List<RangeAtom> moveRanges;

    /**
     * The ranges which are actually moved.
     */
    private List<RangeAtom> movedJobPageRanges = new ArrayList<RangeAtom>();

    /**
     * Iterator for the ranges to move.
     */
    private Iterator<RangeAtom> moveRangesIter;

    /**
     * Iterator for the job pages.
     */
    private Iterator<InboxJobRange> jobPagesIter;

    /**
     *
     */
    private boolean bNextMoveRange = false;

    /**
     *
     */
    private boolean bNextPageRange = false;

    /**
     * The "running" move range.
     */
    private RangeAtom moveRangeAtom = null;

    /**
     *
     */
    private int nRelMoveBegin = 0;

    /**
     *
     */
    private int nRelMoveEnd = 0;

    /**
     *
     */
    private InboxJobRange jobPage = null;

    /**
     *
     */
    private PageMover() {
    }

    /**
     * Constructor.
     *
     * @param user
     * @param jobinfo
     * @param nRanges
     */
    private PageMover(final String user, final InboxInfoDto jobinfo,
            final String nRanges) {

        this.user = user;
        this.jobinfo = jobinfo;
        this.nRanges = nRanges;
        this.isUpdateUser = user != null;

        if (this.isUpdateUser) {
            this.workdir = ConfigManager.getUserHomeDir(user);
        } else {
            this.workdir = null;
        }
    }

    /**
     * Gets the number of pages moved.
     *
     * @return The number of pages moved.
     */
    private int getPagesMoved() {

        int nTotPagesCut = 0;

        if (nPage2Move2 < 0) { // pages deleted
            nTotPagesCut =
                    nPagesTot - INBOX_SERVICE.calcNumberOfPagesInJobs(jobinfo);
        } else { // pages moved
            nTotPagesCut =
                    INBOX_SERVICE.calcNumberOfPages(jobinfo.getJobs(),
                            jobPagesMoved);
        }
        return nTotPagesCut;
    }

    /**
     * Move page ranges.
     *
     * @param user
     *            The userid. Is {@code null} if result of the operation is NOT
     *            user related, so the new {@link InboxInfoDto} is NOT updated
     *            for in a user's homedir (workdir).
     * @param jobinfo
     *            The {@link InboxInfoDto} object, which gets updated after the
     *            move.
     * @param nRanges
     *            string with lpr style (1-based) page ranges.
     * @param nPage2Move2
     *            1-based page ordinal to move at. If this ordinal is negative,
     *            the move is NOT executed, and this method works as a DELETE.
     * @return The number of moved pages.
     */
    public static int movePages(final String user, final InboxInfoDto jobinfo,
            final String nRanges, final int nPage2Move2) {

        PageMover mover = new PageMover(user, jobinfo, nRanges);
        mover.deleteRanges = false;
        mover.nPage2Move2 = nPage2Move2;
        mover.exec();
        return mover.getPagesMoved();
    }

    /**
     * Delete page ranges.
     *
     * @param user
     *            The userid. Is {@code null} if result of the operation is NOT
     *            user related, so the new {@link InboxInfoDto} is NOT updated
     *            for in a user's homedir (workdir).
     * @param jobinfo
     *            The {@link InboxInfoDto} object, which gets updated after the
     *            move.
     * @param nRanges
     *            string with lpr style (1-based) page ranges.
     * @return The number of deleted pages.
     */
    public static int deletePages(final String user,
            final InboxInfoDto jobinfo, final String nRanges) {

        PageMover mover = new PageMover(user, jobinfo, nRanges);
        mover.deleteRanges = true;
        mover.nPage2Move2 = -1;
        mover.exec();
        return mover.getPagesMoved();
    }

    /**
     *
     */
    private void nextMoveRangeAtom() {

        moveRangeAtom = null;
        nRelMoveBegin = 0;
        nRelMoveEnd = 0;

        if (moveRangesIter.hasNext()) {
            moveRangeAtom = moveRangesIter.next();
            nRelMoveBegin =
                    (moveRangeAtom.pageBegin == null) ? 1
                            : moveRangeAtom.pageBegin;
            nRelMoveEnd =
                    (moveRangeAtom.pageEnd == null) ? nPagesTot
                            : moveRangeAtom.pageEnd;
        }
    }

    /**
     *
     */
    private void nextJobPageRange() {
        jobPage = null;
        if (jobPagesIter.hasNext()) {
            jobPage = jobPagesIter.next();
        }
    }

    /**
     *
     */
    private RangeAtom pageRangeAtom = null;

    /**
     *
     */
    private List<RangeAtom> preservedJobPageRanges = null;

    /**
     *
     */
    private List<RangeAtom> pageRanges = null;

    /**
     *
     */
    private Iterator<RangeAtom> pageRangesIter = null;

    /**
     *
     */
    private boolean bPageRangeNative = true;

    /**
     *
     */
    private int nJobPageBegin = -1;

    /**
     *
     */
    private int nJobPageEnd = -1;

    /**
     *
     */
    private int nRelPageBegin = -1;

    /**
     *
     */
    private int nRelPageBeginFirst = -1;

    /**
     *
     */
    private int nRelPageEnd = -1;

    /**
     *
     */
    private int curJobIndex = -1;

    /**
     *
     */
    private void getFirstJobPageRangeAtom() {

        pageRangeAtom = null;
        preservedJobPageRanges = null;

        pageRanges = null;
        pageRangesIter = null;

        bPageRangeNative = true;

        nJobPageBegin = -1;
        nJobPageEnd = -1;
        nRelPageBegin = -1;
        nRelPageBeginFirst = -1;
        nRelPageEnd = -1;

        curJobIndex = -1;

        if (jobPage != null) {

            bPageRangeNative = true;

            curJobIndex = jobPage.getJob();
            preservedJobPageRanges = new ArrayList<RangeAtom>();

            pageRanges =
                    INBOX_SERVICE.createSortedRangeArray(jobPage.getRange());

            pageRangesIter = pageRanges.iterator();

            if (pageRangesIter.hasNext()) {

                pageRangeAtom = pageRangesIter.next();
                if (pageRangeAtom.pageEnd == null) {
                    pageRangeAtom.pageEnd =
                            this.jobinfo.getJobs().get(jobPage.getJob())
                                    .getPages();
                }

                nJobPageBegin =
                        (pageRangeAtom.pageBegin == null) ? 1
                                : pageRangeAtom.pageBegin;
                nJobPageEnd =
                        (pageRangeAtom.pageEnd == null) ? this.jobinfo
                                .getJobs().get(jobPage.getJob()).getPages()
                                : pageRangeAtom.pageEnd;
                nRelPageBegin = 1;
                nRelPageBeginFirst = nRelPageBegin;
                nRelPageEnd = nRelPageBegin + (nJobPageEnd - nJobPageBegin);
            } else {
                pageRangeAtom = null;
            }
        }
    }

    /**
     * Move to the next {@link #pageRangeAtom}.
     */
    private void nextJobPageRangeAtom() {
        if (pageRangesIter.hasNext()) {
            pageRangeAtom = pageRangesIter.next();
            if (pageRangeAtom.pageEnd == null) {
                pageRangeAtom.pageEnd =
                        jobinfo.getJobs().get(jobPage.getJob()).getPages();
            }
        } else {
            pageRangeAtom = null;
        }
    }

    /**
     *
     */
    private void onInit() {

        if (LOGGER.isTraceEnabled()) {

            LOGGER.trace("+--------------------------------------------------");

            if (this.deleteRanges) {
                LOGGER.trace("| DELETE pages [" + nRanges + "]");
            } else {
                LOGGER.trace("| MOVE pages [" + nRanges + "] at page ["
                        + nPage2Move2 + "]");
            }
            LOGGER.trace("+--------------------------------------------------");
        }

        this.nPagesTot = INBOX_SERVICE.calcNumberOfPagesInJobs(this.jobinfo);
        this.moveRanges = INBOX_SERVICE.createSortedRangeArray(nRanges);

        /*
         * Read first moveRange.
         */
        this.moveRangesIter = this.moveRanges.iterator();
        nextMoveRangeAtom();

        /*
         * Read first job page-range.
         */
        final List<InboxJobRange> jobPages = this.jobinfo.getPages();
        this.jobPagesIter = jobPages.iterator();
        nextJobPageRange();

        /*
         * Read first job page-range atom.
         */
        getFirstJobPageRangeAtom();
    }

    /**
     * No RangeAtom objects left to move.
     */
    private void onEndOfMoveRangeAtoms() {
        /*
         * Preserve pageRange.
         */
        preservedJobPageRanges.add(pageRangeAtom);
        bNextPageRange = true;

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("  no pages left to move");
            LOGGER.trace(String.format(
                    "    -> preservedJobPageRanges.add [%d:%d]",
                    pageRangeAtom.pageBegin, pageRangeAtom.pageEnd));
        }
    }

    /**
     *
     */
    private void onMoveBeforeJob() {

        bNextMoveRange = true;

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(String.format("  move [%d-%d] is BEFORE "
                    + "[%d-%d] : next Move RangeAtom", nRelMoveBegin,
                    nRelMoveEnd, nRelPageBegin, nRelPageEnd));
        }
    }

    /**
     *
     */
    private void onMoveAfterJob() {
        /*
         * preserve pageRange
         */
        preservedJobPageRanges.add(pageRangeAtom);
        bNextPageRange = true;

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(String.format("  move [%d-%d] is AFTER [%d-%d]",
                    nRelMoveBegin, nRelMoveEnd, nRelPageBegin, nRelPageEnd));
            LOGGER.trace(String.format(
                    "    -> preservedJobPageRanges.add [%d:%d]",
                    pageRangeAtom.pageBegin, pageRangeAtom.pageEnd));
        }
    }

    /**
     *
     */
    private void onJobWithinMove() {
        /*
         * move pageRange
         */
        movedJobPageRanges.add(pageRangeAtom);
        bNextPageRange = true;

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(String.format("  move [%d-%d] is WITHIN "
                    + "or SAME as [%d-%d]", nRelMoveBegin, nRelMoveEnd,
                    nRelPageBegin, nRelPageEnd));
            LOGGER.trace(String.format("    -> movedJobPageRanges.add [%d:%d]",
                    pageRangeAtom.pageBegin, pageRangeAtom.pageEnd));
        }
    }

    /**
     *
     */
    private void onMoveWithinJob() {

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(String.format("  move [%d-%d] is WITHIN "
                    + "[%d-%d] : split ...", nRelMoveBegin, nRelMoveEnd,
                    nRelPageBegin, nRelPageEnd));
        }

        /*
         * Preserve pre-pageRange.
         */
        final RangeAtom pageRangePre = new RangeAtom();

        if (bPageRangeNative) {
            pageRangePre.pageBegin = nJobPageBegin;
        } else {
            pageRangePre.pageBegin = pageRangeAtom.pageBegin;
        }
        pageRangePre.pageEnd =
                pageRangePre.pageBegin + nRelMoveBegin - nRelPageBegin - 1;

        /*
         * If the move starts on first page of the range there is no 'pre' to
         * preserve.
         */
        if (pageRangePre.pageBegin <= pageRangePre.pageEnd) {
            preservedJobPageRanges.add(pageRangePre);
        }

        /*
         * Move mid-pageRange.
         */
        final RangeAtom pageRangeMid = new RangeAtom();

        pageRangeMid.pageBegin = pageRangePre.pageEnd + 1;
        pageRangeMid.pageEnd =
                pageRangeMid.pageBegin + nRelMoveEnd - nRelMoveBegin;

        movedJobPageRanges.add(pageRangeMid);

        /*
         * Preserve post-pageRange.
         */
        final RangeAtom pageRangePost = new RangeAtom();

        pageRangePost.pageBegin = pageRangeMid.pageEnd + 1;
        pageRangePost.pageEnd = nJobPageEnd;

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(String.format("    ... pre [%d-%d] - mid [%d-%d]"
                    + " - post [%d-%d]", pageRangePre.pageBegin,
                    pageRangePre.pageEnd, pageRangeMid.pageBegin,
                    pageRangeMid.pageEnd, pageRangePost.pageBegin,
                    pageRangePost.pageEnd));
        }

        if (pageRangePost.pageBegin <= pageRangePost.pageEnd) {
            /*
             * Push the 'post' range forward.
             */
            bPageRangeNative = false;

            pageRangeAtom = pageRangePost;

            nRelPageBegin = nRelMoveEnd + 1;

            nRelPageEnd =
                    nRelPageBegin + pageRangePost.pageEnd
                            - pageRangePost.pageBegin;

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(String.format("    ... next Job Rel [%d-%d]",
                        nRelPageBegin, nRelPageEnd));
            }

        } else {
            /*
             * Move ends on last page of the range. No 'post' to push forward,
             * just get the next native range.
             */
            bNextPageRange = true;
        }
    }

    /**
     *
     */
    private void exec() {

        onInit();

        /*
         * Balanced line between moveRanges and jobPages
         */
        while (pageRangeAtom != null) {

            bNextMoveRange = false;
            bNextPageRange = false;

            String move;
            if (moveRangeAtom == null) {
                move = "[End-of-Move]";
            } else {
                move = "Move [" + moveRangeAtom.toString() + "]";
            }

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Job [" + this.curJobIndex + "] Abs ["
                        + pageRangeAtom.toString() + "] Rel ["
                        + this.nRelPageBegin + "-" + this.nRelPageEnd + "] "
                        + "| " + move);
            }

            // ---------------------------------------------------------------
            // NO moveRangeAtom
            // ---------------------------------------------------------------
            if (moveRangeAtom == null) {
                onEndOfMoveRangeAtoms();
            }
            // ---------------------------------------------------------------
            // moveRangeAtom BEFORE pageRange
            // ---------------------------------------------------------------
            else if (nRelMoveEnd < nRelPageBegin) {
                onMoveBeforeJob();
            }
            // ---------------------------------------------------------------
            // moveRange AFTER pageRange
            // ---------------------------------------------------------------
            else if (nRelPageEnd < nRelMoveBegin) {
                onMoveAfterJob();
            }
            // ---------------------------------------------------------------
            // pageRange WITHIN or SAME as moveRange
            // ---------------------------------------------------------------
            else if (nRelMoveBegin <= nRelPageBegin
                    && nRelPageEnd <= nRelMoveEnd) {
                onJobWithinMove();
            }
            // ---------------------------------------------------------------
            // moveRange WITHIN pageRange
            // ---------------------------------------------------------------
            else if (nRelPageBegin <= nRelMoveBegin
                    && nRelMoveEnd <= nRelPageEnd) {
                onMoveWithinJob();
            }
            // ---------------------------------------------------------------
            // moveRange STARTS BEFORE or ON pageRange START
            // ---------------------------------------------------------------
            else if (nRelMoveBegin <= nRelPageBegin) {

                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace(String.format("  move [%d-%d] STARTS "
                            + "BEFORE or ON [%d-%d] START", nRelMoveBegin,
                            nRelMoveEnd, nRelPageBegin, nRelPageEnd));
                }

                // move pre-pageRange

                // TODO ??

                // next moveRange
                bNextPageRange = true;
            }
            // ---------------------------------------------------------------
            // moveRange ENDS AFTER or ON pageRange END
            // ---------------------------------------------------------------
            else if (nRelPageEnd <= nRelMoveEnd) {

                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace(String.format("  move [%d-%d] ENDS "
                            + "AFTER or ON [%d-%d] END ", nRelMoveBegin,
                            nRelMoveEnd, nRelPageBegin, nRelPageEnd));
                }

                // move post-pageRange

                // TODO ??

                // next pageRange
                bNextPageRange = true;
            }

            // ---------------------------------------------------------
            // Next move range
            // ---------------------------------------------------------
            if (bNextMoveRange) {
                nextMoveRangeAtom();
            }

            // ---------------------------------------------------------
            // Next page range
            // ---------------------------------------------------------
            if (bNextPageRange) {

                nextJobPageRangeAtom();

                boolean bNewJob = false;

                // ---------------------------------------------------------
                // End-of-page-ranges: Flush Current job an go to Next job
                // ---------------------------------------------------------
                if (pageRangeAtom == null) {
                    bNewJob = true;
                    onEndOfJobPageRangeAtoms();
                }

                if (pageRangeAtom != null) {

                    pageRangeAtom.pageBegin =
                            (pageRangeAtom.pageBegin == null) ? 1
                                    : pageRangeAtom.pageBegin;

                    pageRangeAtom.pageEnd =
                            (pageRangeAtom.pageEnd == null) ? jobinfo.getJobs()
                                    .get(jobPage.getJob()).getPages()
                                    : pageRangeAtom.pageEnd;

                    nJobPageBegin = pageRangeAtom.pageBegin;
                    nJobPageEnd = pageRangeAtom.pageEnd;

                    nRelPageBegin = nRelPageEnd;
                    nRelPageBegin++;

                    if (bNewJob) {
                        nRelPageBeginFirst = nRelPageBegin;
                    }
                    nRelPageEnd = nRelPageBegin + nJobPageEnd - nJobPageBegin;
                }

                bPageRangeNative = true;
            }

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("----------------------------------------------");
            }

        } // end-while

        onExit();
    }

    /**
     *
     */
    private void onExit() {

        if (nPagesTot < nPage2Move2) {
            iJobPageNew2Move2 = nPagesTot + 1;
        } else if (iJobPageNew2Move2 == null) {
            iJobPageNew2Move2 = 0;
        }

        if (this.deleteRanges) {

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("DELETED ["
                        + InboxServiceImpl.getPageRangeAsText(jobPagesMoved)
                        + "]");
            }

        } else {

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("MOVED ["
                        + InboxServiceImpl.getPageRangeAsText(jobPagesMoved)
                        + "] TO index [" + iJobPageNew2Move2 + "]");
            }

            if (iJobPageNew2Move2 < jobPagesNew.size()) {
                jobPagesNew.addAll(iJobPageNew2Move2, jobPagesMoved);
            } else {
                jobPagesNew.addAll(jobPagesMoved);
            }
        }

        // -------------------------------------------------------------
        // Store new page ordering and return
        // -------------------------------------------------------------
        if (!jobPagesMoved.isEmpty()) {

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Before : "
                        + InboxServiceImpl.getPageRangeAsText(jobinfo
                                .getPages()));
            }

            jobinfo.setPages(jobPagesNew);

            optimizeJobs(jobinfo);

            if (isUpdateUser) {
                if (nPage2Move2 < 0) {
                    jobinfo = INBOX_SERVICE.pruneJobs(workdir, user, jobinfo);
                }
            }

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("After  : "
                        + InboxServiceImpl.getPageRangeAsText(jobinfo
                                .getPages()));
            }

            if (isUpdateUser) {
                INBOX_SERVICE.storeInboxInfo(user, jobinfo);
            }
        }

    }

    /**
     *
     */
    private void onEndOfJobPageRangeAtoms() {

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("  end-of-ordinal-range");
        }

        /*
         * Flush the preserved ranges for this "job", and find the insert point.
         */
        if (!preservedJobPageRanges.isEmpty()) {

            List<RangeAtom> preservedJobPageRangesWork =
                    new ArrayList<RangeAtom>();

            int begin = nRelPageBeginFirst;

            for (RangeAtom range : preservedJobPageRanges) {

                int end = begin + range.pageEnd - range.pageBegin;

                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace(String.format(
                            "    preserved range [%d:%d-%d] " + "[%d-%d]",
                            jobPage.getJob(), range.pageBegin, range.pageEnd,
                            nRelPageBeginFirst, end));
                }

                /*
                 * Insert point?
                 */
                if (begin <= nPage2Move2 && nPage2Move2 <= end) {

                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace("      Insert point found.");
                    }

                    if (nPage2Move2 == begin) {

                        /*
                         * The insert point is right at the start of this
                         * preserved range.
                         *
                         * Add the previous preserved ranges which were
                         * accumulated up till now in the
                         * preservedJobPageRangesWork to the jobPagesNew ...
                         */
                        if (!preservedJobPageRangesWork.isEmpty()) {

                            InboxInfoDto.InboxJobRange jobPageNew =
                                    new InboxInfoDto.InboxJobRange();
                            jobPageNew.setJob(jobPage.getJob());
                            jobPageNew.setRange(RangeAtom
                                    .asText(preservedJobPageRangesWork));

                            jobPagesNew.add(jobPageNew);

                            if (LOGGER.isTraceEnabled()) {
                                LOGGER.trace(String.format(
                                        "    -> jobPagesNew.add [%s]",
                                        jobPageNew.getRange()));
                            }

                            /*
                             * start with fresh copy
                             */
                            preservedJobPageRangesWork.clear();
                        }
                        /*
                         * ... and add this range to the
                         */
                        preservedJobPageRangesWork.add(range);

                        if (LOGGER.isTraceEnabled()) {
                            LOGGER.trace(String.format(
                                    "      -> work.add [%s] (begin)",
                                    range.toString()));
                        }

                    } else {
                        /*
                         * Split range, because the insert point is in the midst
                         * of a range.
                         */

                        /*
                         * Split 1st part + append on preserved jobs
                         */
                        RangeAtom rangePre = new RangeAtom();
                        rangePre.pageBegin = range.pageBegin;
                        rangePre.pageEnd =
                                range.pageBegin + (nPage2Move2 - begin - 1);

                        preservedJobPageRangesWork.add(rangePre);

                        if (LOGGER.isTraceEnabled()) {
                            LOGGER.trace(String.format(
                                    "      -> work.add [%s] (pre)",
                                    rangePre.toString()));
                        }

                        InboxInfoDto.InboxJobRange jobPageNew =
                                new InboxInfoDto.InboxJobRange();
                        jobPageNew.setJob(jobPage.getJob());

                        jobPageNew.setRange(RangeAtom
                                .asText(preservedJobPageRangesWork));

                        jobPagesNew.add(jobPageNew);

                        if (LOGGER.isTraceEnabled()) {
                            LOGGER.trace(String.format(
                                    "      -> jobPagesNew.add [%s]",
                                    jobPageNew.getRange()));
                        }
                        /*
                         * 2nd part
                         */
                        preservedJobPageRangesWork = new ArrayList<RangeAtom>();
                        RangeAtom rangePost = new RangeAtom();
                        rangePost.pageBegin = rangePre.pageEnd + 1;
                        rangePost.pageEnd = range.pageEnd;

                        preservedJobPageRangesWork.add(rangePost);

                        if (LOGGER.isTraceEnabled()) {
                            LOGGER.trace(String.format(
                                    "    -> work.add [%s] (post)",
                                    rangePost.toString()));
                        }
                    }

                    iJobPageNew2Move2 = jobPagesNew.size();

                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace(String.format("      -> MOVE AT "
                                + "page [%d] IN " + "pageRange [%d:%d-%d] "
                                + "iJobPageNew2Move2 [%d] ", nPage2Move2,
                                curJobIndex, begin, end, iJobPageNew2Move2));
                    }

                } else {
                    /*
                     * No insert point, add this range to the work area.
                     */
                    preservedJobPageRangesWork.add(range);

                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace(String.format("      -> work.add [%d:%d]",
                                range.pageBegin, range.pageEnd));
                    }
                }

                begin = end + 1;
            }

            /*
             * Flush the preservedJobPageRangesWork to the jobPagesNew
             */
            InboxInfoDto.InboxJobRange jobPageNew =
                    new InboxInfoDto.InboxJobRange();
            jobPageNew.setJob(jobPage.getJob());
            jobPageNew.setRange(RangeAtom.asText(preservedJobPageRangesWork));

            jobPagesNew.add(jobPageNew);

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(String.format("    -> jobPagesNew.add [%s]",
                        jobPageNew.getRange()));
            }

            /*
             * Start with fresh copy of the preserved job page ranges.
             */
            preservedJobPageRanges = new ArrayList<RangeAtom>();
        }

        // ----------------------------------------
        // Accumulate the moved pages to the total.
        // ----------------------------------------
        if (!movedJobPageRanges.isEmpty()) {

            InboxInfoDto.InboxJobRange jobPageMoved =
                    new InboxInfoDto.InboxJobRange();

            jobPageMoved.setJob(jobPage.getJob());
            jobPageMoved.setRange(RangeAtom.asText(movedJobPageRanges));

            jobPagesMoved.add(jobPageMoved);

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(String.format("    -> jobPagesMoved.add [%s]",
                        jobPageMoved.getRange()));
            }

            movedJobPageRanges = new ArrayList<RangeAtom>();

        }

        // ----------------------------------------
        // Next jobPage
        // ----------------------------------------
        if (jobPagesIter.hasNext()) {
            jobPage = jobPagesIter.next();
        } else {
            jobPage = null;
        }

        // first range within page
        if (jobPage != null) {
            curJobIndex = jobPage.getJob();

            pageRanges =
                    INBOX_SERVICE.createSortedRangeArray(jobPage.getRange());
            pageRangesIter = pageRanges.iterator();
            if (pageRangesIter.hasNext()) {
                pageRangeAtom = pageRangesIter.next();
                if (pageRangeAtom.pageEnd == null) {
                    pageRangeAtom.pageEnd =
                            jobinfo.getJobs().get(jobPage.getJob()).getPages();
                }
            } else {
                pageRangeAtom = null;
            }
        }

        bPageRangeNative = true;
    }

    /**
     * Optimizes (de-fragmentizes) the JobInfo by merging adjacent ranges.
     *
     * @param jobs
     *            The object to optimize.
     * @return The same object as the parameter.
     */
    public static InboxInfoDto optimizeJobs(final InboxInfoDto jobs) {

        final ArrayList<InboxInfoDto.InboxJobRange> rangesNew =
                new ArrayList<>();

        final ArrayList<InboxInfoDto.InboxJobRange> rangesOld = jobs.getPages();

        /*
         * Init
         */
        InboxInfoDto.InboxJobRange rangeOld = null;
        List<RangeAtom> rangeAtomsOld = null;
        RangeAtom rangeAtomOld = null;

        int i = 0;
        int j = 0;

        int nPageMergeFrom = 0;
        int nPageMergeTo = 0;

        int jobCur = 0;
        int jobPrv = 0;

        /*
         * Initial read
         */
        if (i < rangesOld.size()) {

            rangeOld = rangesOld.get(i);

            jobCur = rangeOld.getJob();

            rangeAtomsOld =
                    INBOX_SERVICE.createSortedRangeArray(rangeOld.getRange());
            j = 0;
            if (j < rangeAtomsOld.size()) {
                rangeAtomOld = rangeAtomsOld.get(j);
                nPageMergeFrom = rangeAtomOld.calcPageFrom();
                nPageMergeTo = rangeAtomOld.calcPageTo();
            }
        }

        /*
         *
         */
        while (rangeAtomOld != null) {

            /*
             * Read next
             */

            jobPrv = jobCur;

            rangeAtomOld = null;
            j++;

            if (j < rangeAtomsOld.size()) {
                rangeAtomOld = rangeAtomsOld.get(j);
            } else {
                i++;
                if (i < rangesOld.size()) {

                    rangeOld = rangesOld.get(i);

                    jobCur = rangeOld.getJob();

                    rangeAtomsOld =
                            INBOX_SERVICE.createSortedRangeArray(rangeOld
                                    .getRange());

                    j = 0;
                    if (j < rangeAtomsOld.size()) {
                        rangeAtomOld = rangeAtomsOld.get(j);
                    }
                }
            }

            /*
             * Level breaks
             */
            boolean flush = false;
            int nPageFrom = 0;
            int nPageTo = 0;

            if (rangeAtomOld == null) {
                flush = true;
            } else {
                nPageFrom = rangeAtomOld.calcPageFrom();
                nPageTo = rangeAtomOld.calcPageTo();
                if (jobCur == jobPrv) {
                    if (nPageMergeTo + 1 == nPageFrom) {
                        nPageMergeTo = nPageTo;
                    } else {
                        flush = true;
                    }
                } else {
                    flush = true;
                }
            }

            if (flush) {
                /*
                 * deep copy (just to be sure)
                 */
                RangeAtom atom = new RangeAtom();
                atom.pageBegin = nPageMergeFrom;
                atom.pageEnd = nPageMergeTo;

                InboxInfoDto.InboxJobRange prunedPage =
                        new InboxInfoDto.InboxJobRange();
                prunedPage.setJob(jobPrv);
                prunedPage.setRange(atom.asText());
                rangesNew.add(prunedPage);
                /*
                 *
                 */
                nPageMergeFrom = nPageFrom;
                nPageMergeTo = nPageTo;
            }

        }
        jobs.setPages(rangesNew);
        return jobs;
    }

}
