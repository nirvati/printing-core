/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2020 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: 2011-2020 Datraverse B.V. <info@datraverse.com>
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
package org.savapage.core;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import javax.print.attribute.standard.MediaSizeName;

import org.apache.log4j.BasicConfigurator;
import org.junit.Before;
import org.junit.Test;
import org.savapage.core.inbox.InboxInfoDto;
import org.savapage.core.inbox.InboxInfoDto.InboxJob;
import org.savapage.core.inbox.InboxInfoDto.InboxJobRange;
import org.savapage.core.inbox.RangeAtom;
import org.savapage.core.print.proxy.ProxyPrintException;
import org.savapage.core.print.proxy.ProxyPrintJobChunk;
import org.savapage.core.print.proxy.ProxyPrintJobChunkInfo;
import org.savapage.core.services.InboxService;
import org.savapage.core.services.ServiceContext;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class InboxInfoTest {

    /**
    *
    */
    private static final InboxService INBOX_SERVICE =
            ServiceContext.getServiceFactory().getInboxService();

    @Before
    public void initTest() {
        /*
         * Set up a simple log4j configuration that logs on the console.
         */
        BasicConfigurator.configure();
    }

    /**
     *
     * @param jobPages
     * @param createdTimes
     * @return
     */
    private InboxInfoDto createJobInfo(final Integer[] jobPages,
            final Long[] createdTimes) {

        final InboxInfoDto jobinfo = new InboxInfoDto();

        int i = 0;

        for (int pages : jobPages) {

            final String name = "file_" + i;

            InboxJob job = new InboxJob();

            job.setFile(name + ".pdf");
            job.setTitle(name);
            job.setPages(pages);
            job.setCreatedTime(createdTimes[i]);

            jobinfo.getJobs().add(job);

            final InboxJobRange range = new InboxInfoDto.InboxJobRange();
            range.setJob(i++);
            range.setRange(RangeAtom.FULL_PAGE_RANGE);

            jobinfo.getPages().add(range); // append

        }

        return jobinfo;
    }

    /**
     *
     * @param fastInfo
     */
    private void validate(final InboxInfoDto fastInfo) {

        int iJobRange = 0;

        for (final InboxJobRange range : fastInfo.getPages()) {
            assertTrue(range.getJob().intValue() == iJobRange++);
        }

    }

    @Test
    public void testFastProxyPrintAll() throws IOException {

        final Date now = new Date();
        final int expiryMins = 10;

        final InboxInfoDto jobInfo = createJobInfo(new Integer[] { 5, 5 },
                new Long[] { now.getTime(),
                        now.getTime() - expiryMins * 60 * 1000 });

        final InboxInfoDto fastInfo =
                INBOX_SERVICE.pruneForFastProxyPrint(jobInfo, now, expiryMins);

        assertTrue(fastInfo.getJobs().size() == 2);
        assertTrue(fastInfo.getPages().size() == 2);

        assertTrue(fastInfo == jobInfo);

        validate(fastInfo);
    }

    @Test
    public void testFastProxyPrintPreviewed() throws IOException {

        final Date now = new Date();
        final int expiryMins = 5;

        final InboxInfoDto jobInfo = createJobInfo(new Integer[] { 5, 5 },
                new Long[] { now.getTime() - expiryMins * 60 * 1000 - 1,
                        now.getTime() - expiryMins * 60 * 1000 - 1 });

        jobInfo.setLastPreviewTime(now.getTime() - expiryMins * 60 * 1000);

        final InboxInfoDto fastInfo =
                INBOX_SERVICE.pruneForFastProxyPrint(jobInfo, now, expiryMins);

        assertTrue(fastInfo.getJobs().size() == 2);
        assertTrue(fastInfo.getPages().size() == 2);

        assertTrue(fastInfo == jobInfo);

        validate(fastInfo);
    }

    @Test
    public void testFastProxyPrintPart() throws IOException {

        final Date now = new Date();
        final int expiryMins = 5;

        final InboxInfoDto jobInfo = createJobInfo(new Integer[] { 5, 5 },
                new Long[] { now.getTime(),
                        now.getTime() - expiryMins * 60 * 1000 - 1 });

        final InboxInfoDto fastInfo =
                INBOX_SERVICE.pruneForFastProxyPrint(jobInfo, now, expiryMins);

        assertTrue(fastInfo.getJobs().size() == 2);
        assertTrue(fastInfo.getPages().size() == 1);
        assertTrue(fastInfo != jobInfo);

        validate(fastInfo);

    }

    @Test
    public void testFastProxyPrintNone() throws IOException {

        final Date now = new Date();
        final int expiryMins = 5;

        final InboxInfoDto jobInfo = createJobInfo(new Integer[] { 5, 5 },
                new Long[] { now.getTime() - expiryMins * 60 * 1000 - 1,
                        now.getTime() - expiryMins * 60 * 1000 - 1 });

        final InboxInfoDto fastInfo =
                INBOX_SERVICE.pruneForFastProxyPrint(jobInfo, now, expiryMins);

        assertTrue(fastInfo.getJobs().size() == 2);
        assertTrue(fastInfo.getPages().size() == 0);
        assertTrue(fastInfo != jobInfo);

        validate(fastInfo);

    }

    @Test
    public void testInboxInfoFiltering() {

        final InboxInfoDto inboxInfo =
                createJobInfo(new Integer[] { 5, 5 }, new Long[] { 0L, 0L });

        assertTrue(inboxInfo.getJobs().size() == 2);
        assertTrue(inboxInfo.getPages().size() == 2);

        final InboxInfoDto inboxInfoFiltered =
                INBOX_SERVICE.filterInboxInfoPages(inboxInfo, "4-6");

        assertTrue(inboxInfoFiltered.getJobs().size() == 2);
        assertTrue(inboxInfoFiltered.getPages().size() == 2);

        assertTrue(inboxInfoFiltered.getPages().get(0).getJob() == 0);
        assertTrue(inboxInfoFiltered.getPages().get(1).getJob() == 1);

        RangeAtom range;

        //
        range = INBOX_SERVICE.createSortedRangeArray(
                inboxInfoFiltered.getPages().get(0).getRange()).get(0);

        assertTrue(range.pageBegin == 4);
        assertTrue(range.pageEnd == 5);

        //
        range = INBOX_SERVICE.createSortedRangeArray(
                inboxInfoFiltered.getPages().get(1).getRange()).get(0);

        assertTrue(range.pageBegin == 1);
        assertTrue(range.pageEnd == 1);

    }

    @Test
    public void testInboxInfoFiltering2() {

        final InboxInfoDto inboxInfo =
                createJobInfo(new Integer[] { 1, 1 }, new Long[] { 0L, 0L });

        assertTrue(inboxInfo.getJobs().size() == 2);
        assertTrue(inboxInfo.getPages().size() == 2);

        final InboxInfoDto inboxInfoFiltered =
                INBOX_SERVICE.filterInboxInfoPages(inboxInfo, "1-2");

        assertTrue(inboxInfoFiltered.getJobs().size() == 2);
        assertTrue(inboxInfoFiltered.getPages().size() == 2);

        assertTrue(inboxInfoFiltered.getPages().get(0).getJob() == 0);
        assertTrue(inboxInfoFiltered.getPages().get(1).getJob() == 1);

        RangeAtom range;

        //
        range = INBOX_SERVICE.createSortedRangeArray(
                inboxInfoFiltered.getPages().get(0).getRange()).get(0);

        assertTrue(range.pageBegin == 1);
        assertTrue(range.pageEnd == null);

        //
        range = INBOX_SERVICE.createSortedRangeArray(
                inboxInfoFiltered.getPages().get(1).getRange()).get(0);

        assertTrue(range.pageBegin == 1);
        assertTrue(range.pageEnd == null);

    }

    @Test
    public void testInboxInfoFiltering3() {

        final InboxInfoDto inboxInfo =
                createJobInfo(new Integer[] { 1, 1 }, new Long[] { 0L, 0L });

        assertTrue(inboxInfo.getJobs().size() == 2);
        assertTrue(inboxInfo.getPages().size() == 2);

        final InboxInfoDto inboxInfoFiltered =
                INBOX_SERVICE.filterInboxInfoPages(inboxInfo, "1,2");

        assertTrue(inboxInfoFiltered.getJobs().size() == 2);
        assertTrue(inboxInfoFiltered.getPages().size() == 2);

        assertTrue(inboxInfoFiltered.getPages().get(0).getJob() == 0);
        assertTrue(inboxInfoFiltered.getPages().get(1).getJob() == 1);

        RangeAtom range;

        //
        range = INBOX_SERVICE.createSortedRangeArray(
                inboxInfoFiltered.getPages().get(0).getRange()).get(0);

        assertTrue(range.pageBegin == 1);
        assertTrue(range.pageEnd == 1);

        //
        range = INBOX_SERVICE.createSortedRangeArray(
                inboxInfoFiltered.getPages().get(1).getRange()).get(0);

        assertTrue(range.pageBegin == 1);
        assertTrue(range.pageEnd == 1);

    }

    @Test
    public void testInboxInfoFiltering4() {

        final InboxInfoDto inboxInfo =
                createJobInfo(new Integer[] { 2 }, new Long[] { 0L });

        assertTrue(inboxInfo.getJobs().size() == 1);
        assertTrue(inboxInfo.getPages().size() == 1);

        final InboxInfoDto inboxInfoFiltered = INBOX_SERVICE
                .filterInboxInfoPages(inboxInfo, RangeAtom.FULL_PAGE_RANGE);

        assertTrue(inboxInfoFiltered.getJobs().size() == 1);
        assertTrue(inboxInfoFiltered.getPages().size() == 1);

        assertTrue(inboxInfoFiltered.getPages().get(0).getJob() == 0);

        final RangeAtom range =
                INBOX_SERVICE
                        .createSortedRangeArray(
                                inboxInfoFiltered.getPages().get(0).getRange())
                        .get(0);

        assertTrue(range.pageBegin == 1);
        assertTrue(range.pageEnd == null);
    }

    @Test
    public void testInboxInfoFiltering5() {

        final InboxInfoDto inboxInfo =
                createJobInfo(new Integer[] { 2 }, new Long[] { 0L });

        assertTrue(inboxInfo.getJobs().size() == 1);
        assertTrue(inboxInfo.getPages().size() == 1);

        final InboxInfoDto inboxInfoFiltered =
                INBOX_SERVICE.filterInboxInfoPages(inboxInfo, "1,2");

        assertTrue(inboxInfoFiltered.getJobs().size() == 1);
        assertTrue(inboxInfoFiltered.getPages().size() == 1);

        assertTrue(inboxInfoFiltered.getPages().get(0).getJob() == 0);

        RangeAtom range =
                INBOX_SERVICE
                        .createSortedRangeArray(
                                inboxInfoFiltered.getPages().get(0).getRange())
                        .get(0);

        assertTrue(range.pageBegin == 1);
        assertTrue(range.pageEnd == 2);

    }

    @Test
    public void testInboxInfoFiltering6() {

        final InboxInfoDto inboxInfo =
                createJobInfo(new Integer[] { 2 }, new Long[] { 0L });

        assertTrue(inboxInfo.getJobs().size() == 1);
        assertTrue(inboxInfo.getPages().size() == 1);

        final InboxInfoDto inboxInfoFiltered =
                INBOX_SERVICE.filterInboxInfoPages(inboxInfo, "1-2");

        assertTrue(inboxInfoFiltered.getJobs().size() == 1);
        assertTrue(inboxInfoFiltered.getPages().size() == 1);

        assertTrue(inboxInfoFiltered.getPages().get(0).getJob() == 0);

        RangeAtom range =
                INBOX_SERVICE
                        .createSortedRangeArray(
                                inboxInfoFiltered.getPages().get(0).getRange())
                        .get(0);

        assertTrue(range.pageBegin == 1);
        assertTrue(range.pageEnd == null);
    }

    @Test
    public void testInboxInfoFiltering7() {

        final InboxInfoDto inboxInfo =
                createJobInfo(new Integer[] { 2 }, new Long[] { 0L });

        assertTrue(inboxInfo.getJobs().size() == 1);
        assertTrue(inboxInfo.getPages().size() == 1);

        final InboxInfoDto inboxInfoFiltered =
                INBOX_SERVICE.filterInboxInfoPages(inboxInfo, "1-");

        // PageMover.optimizeJobs(

        assertTrue(inboxInfoFiltered.getJobs().size() == 1);
        assertTrue(inboxInfoFiltered.getPages().size() == 1);

        assertTrue(inboxInfoFiltered.getPages().get(0).getJob() == 0);

        RangeAtom range =
                INBOX_SERVICE
                        .createSortedRangeArray(
                                inboxInfoFiltered.getPages().get(0).getRange())
                        .get(0);

        assertTrue(range.pageBegin == 1);
        assertTrue(range.pageEnd == null);
    }

    @Test
    public void testInboxInfoFiltering8() {

        final InboxInfoDto inboxInfo = createJobInfo(new Integer[] { 5, 10, 5 },
                new Long[] { 0L, 0L, 0L });

        assertTrue(inboxInfo.getJobs().size() == 3);
        assertTrue(inboxInfo.getPages().size() == 3);

        final InboxInfoDto inboxInfoFiltered =
                INBOX_SERVICE.filterInboxInfoPages(inboxInfo, "4-5,16-");

        // PageMover.optimizeJobs(

        assertTrue(inboxInfoFiltered.getJobs().size() == 3);
        assertTrue(inboxInfoFiltered.getPages().size() == 2);

        assertTrue(inboxInfoFiltered.getPages().get(0).getJob() == 0);
        assertTrue(inboxInfoFiltered.getPages().get(1).getJob() == 2);

        InboxJobRange jobRange;
        List<RangeAtom> ranges;
        RangeAtom range;

        //
        jobRange = inboxInfoFiltered.getPages().get(0);
        ranges = INBOX_SERVICE.createSortedRangeArray(jobRange.getRange());

        assertTrue(ranges.size() == 1);

        range = ranges.get(0);

        assertTrue(range.pageBegin == 4);
        assertTrue(range.pageEnd == 5);

        //
        jobRange = inboxInfoFiltered.getPages().get(1);
        ranges = INBOX_SERVICE.createSortedRangeArray(jobRange.getRange());

        assertTrue(ranges.size() == 1);

        range = ranges.get(0);

        assertTrue(range.pageBegin == 1);
        assertTrue(range.pageEnd == 5);

    }

    @Test
    public void testInboxInfoFiltering9() {

        /*
         * Prepare.
         */
        final InboxInfoDto inboxInfo = new InboxInfoDto();

        int iJob = 0;

        // Job 0
        InboxJob job = new InboxJob();
        inboxInfo.getJobs().add(job);

        String name = "file_" + iJob;
        job.setFile(name + ".pdf");
        job.setTitle(name);
        job.setMedia(MediaSizeName.ISO_A4.toString());

        job.setPages(5);

        // Job 1
        iJob++;
        job = new InboxJob();
        inboxInfo.getJobs().add(job);

        name = "file_" + iJob;
        job.setFile(name + ".pdf");
        job.setTitle(name);
        job.setMedia(MediaSizeName.ISO_A3.toString());

        job.setPages(10);

        // Job 2
        iJob++;
        job = new InboxJob();
        inboxInfo.getJobs().add(job);

        name = "file_" + iJob;
        job.setFile(name + ".pdf");
        job.setTitle(name);
        job.setMedia(MediaSizeName.ISO_A4.toString());

        job.setPages(5);

        // ------------
        // Ranges
        // ------------
        InboxJobRange jobRange;

        // 0
        jobRange = new InboxInfoDto.InboxJobRange();
        inboxInfo.getPages().add(jobRange);
        jobRange.setJob(0);
        jobRange.setRange(RangeAtom.FULL_PAGE_RANGE);

        // 1
        jobRange = new InboxInfoDto.InboxJobRange();
        inboxInfo.getPages().add(jobRange);
        jobRange.setJob(1);
        jobRange.setRange(RangeAtom.FULL_PAGE_RANGE);

        // 2
        jobRange = new InboxInfoDto.InboxJobRange();
        inboxInfo.getPages().add(jobRange);
        jobRange.setJob(2);
        jobRange.setRange(RangeAtom.FULL_PAGE_RANGE);

        // ------------
        // TEST
        // ------------
        assertTrue(inboxInfo.getJobs().size() == 3);
        assertTrue(inboxInfo.getPages().size() == 3);

        final InboxInfoDto inboxInfoFiltered =
                INBOX_SERVICE.filterInboxInfoPages(inboxInfo, "4-5,16-");

        // PageMover.optimizeJobs(

        assertTrue(inboxInfoFiltered.getJobs().size() == 3);
        assertTrue(inboxInfoFiltered.getPages().size() == 2);

        assertTrue(inboxInfoFiltered.getPages().get(0).getJob() == 0);
        assertTrue(inboxInfoFiltered.getPages().get(1).getJob() == 2);

        List<RangeAtom> ranges;
        RangeAtom range;

        //
        jobRange = inboxInfoFiltered.getPages().get(0);
        ranges = INBOX_SERVICE.createSortedRangeArray(jobRange.getRange());

        assertTrue(ranges.size() == 1);

        range = ranges.get(0);

        assertTrue(range.pageBegin == 4);
        assertTrue(range.pageEnd == 5);

        //
        jobRange = inboxInfoFiltered.getPages().get(1);
        ranges = INBOX_SERVICE.createSortedRangeArray(jobRange.getRange());

        assertTrue(ranges.size() == 1);

        range = ranges.get(0);

        assertTrue(range.pageBegin == 1);
        assertTrue(range.pageEnd == 5);

    }

    @Test
    public void testInboxInfoFiltering10() {

        /*
         * Prepare.
         */
        final InboxInfoDto inboxInfo = new InboxInfoDto();

        int iJob = 0;

        // Job 0
        InboxJob job = new InboxJob();
        inboxInfo.getJobs().add(job);

        String name = "file_" + iJob;
        job.setFile(name + ".pdf");
        job.setTitle(name);
        job.setMedia(MediaSizeName.ISO_A4.toString());

        job.setPages(5);

        // Job 1
        iJob++;
        job = new InboxJob();
        inboxInfo.getJobs().add(job);

        name = "file_" + iJob;
        job.setFile(name + ".pdf");
        job.setTitle(name);
        job.setMedia(MediaSizeName.ISO_A3.toString());

        job.setPages(10);

        // Job 2
        iJob++;
        job = new InboxJob();
        inboxInfo.getJobs().add(job);

        name = "file_" + iJob;
        job.setFile(name + ".pdf");
        job.setTitle(name);
        job.setMedia(MediaSizeName.ISO_A4.toString());

        job.setPages(5);

        // ------------
        // Ranges
        // ------------
        InboxJobRange jobRange;

        // 0
        jobRange = new InboxInfoDto.InboxJobRange();
        inboxInfo.getPages().add(jobRange);
        jobRange.setJob(0);
        jobRange.setRange("4-5");

        // 1
        jobRange = new InboxInfoDto.InboxJobRange();
        inboxInfo.getPages().add(jobRange);
        jobRange.setJob(1);
        jobRange.setRange("6-10");

        // 2
        jobRange = new InboxInfoDto.InboxJobRange();
        inboxInfo.getPages().add(jobRange);
        jobRange.setJob(2);
        jobRange.setRange("2-3");

        // ------------
        // TEST
        // ------------
        assertTrue(inboxInfo.getJobs().size() == 3);
        assertTrue(inboxInfo.getPages().size() == 3);

        final InboxInfoDto inboxInfoFiltered =
                INBOX_SERVICE.filterInboxInfoPages(inboxInfo, "2,4-5,7-8");

        // PageMover.optimizeJobs(

        assertTrue(inboxInfoFiltered.getJobs().size() == 3);
        assertTrue(inboxInfoFiltered.getPages().size() == 4);

        List<RangeAtom> ranges;
        RangeAtom range;

        //
        jobRange = inboxInfoFiltered.getPages().get(0);
        assertTrue(jobRange.getJob() == 0);
        ranges = INBOX_SERVICE.createSortedRangeArray(jobRange.getRange());

        assertTrue(ranges.size() == 1);

        range = ranges.get(0);

        assertTrue(range.pageBegin == 5);
        assertTrue(range.pageEnd == 5);

        //
        jobRange = inboxInfoFiltered.getPages().get(1);
        assertTrue(jobRange.getJob() == 1);
        ranges = INBOX_SERVICE.createSortedRangeArray(jobRange.getRange());

        assertTrue(ranges.size() == 1);

        range = ranges.get(0);

        assertTrue(range.pageBegin == 7);
        assertTrue(range.pageEnd == 8);

        //
        jobRange = inboxInfoFiltered.getPages().get(2);
        assertTrue(jobRange.getJob() == 1);
        ranges = INBOX_SERVICE.createSortedRangeArray(jobRange.getRange());

        assertTrue(ranges.size() == 1);

        range = ranges.get(0);

        assertTrue(range.pageBegin == 10);
        assertTrue(range.pageEnd == 10);

        //
        jobRange = inboxInfoFiltered.getPages().get(3);
        assertTrue(jobRange.getJob() == 2);
        ranges = INBOX_SERVICE.createSortedRangeArray(jobRange.getRange());

        assertTrue(ranges.size() == 1);

        range = ranges.get(0);

        assertTrue(range.pageBegin == 2);
        assertTrue(range.pageEnd == 2);

    }

    @Test
    public void testPrintJobChunkInfo1() {

        final InboxInfoDto inboxInfo = createJobInfo(new Integer[] { 5, 10, 5 },
                new Long[] { 0L, 0L, 0L });

        inboxInfo.getJobs().get(0).setMedia(MediaSizeName.ISO_A4.toString());
        inboxInfo.getJobs().get(1).setMedia(MediaSizeName.ISO_A3.toString());
        inboxInfo.getJobs().get(2).setMedia(MediaSizeName.ISO_A4.toString());

        final ProxyPrintJobChunkInfo chunkInfo =
                new ProxyPrintJobChunkInfo(inboxInfo, "4-5,16-");

        assertTrue(chunkInfo.getFilteredInboxInfo().getJobs().size() == 3);

        assertTrue(chunkInfo.getChunks().size() == 1);
        assertTrue(chunkInfo.getChunks().get(0).getNumberOfPages() == 7);
        assertTrue(chunkInfo.getChunks().get(0)
                .getMediaSizeName() == MediaSizeName.ISO_A4);

        RangeAtom atom;

        atom = chunkInfo.getChunks().get(0).getRanges().get(0);
        assertTrue(atom.pageBegin == 4 && atom.pageEnd == 5);

        atom = chunkInfo.getChunks().get(0).getRanges().get(1);
        assertTrue(atom.pageBegin == 1 && atom.pageEnd == 5);
    }

    @Test
    public void testPrintJobChunkInfo2() {

        final InboxInfoDto inboxInfo = createJobInfo(new Integer[] { 5, 10, 5 },
                new Long[] { 0L, 0L, 0L });

        inboxInfo.getJobs().get(0).setMedia(MediaSizeName.ISO_A4.toString());
        inboxInfo.getJobs().get(1).setMedia(MediaSizeName.ISO_A3.toString());
        inboxInfo.getJobs().get(2).setMedia(MediaSizeName.ISO_A4.toString());

        final ProxyPrintJobChunkInfo chunkInfo =
                new ProxyPrintJobChunkInfo(inboxInfo, "4-5,9-10,16-");

        assertTrue(chunkInfo.getFilteredInboxInfo().getJobs().size() == 3);

        assertTrue(chunkInfo.getChunks().size() == 3);

        ProxyPrintJobChunk chunk;
        RangeAtom atom;

        chunk = chunkInfo.getChunks().get(0);
        assertTrue(chunk.getNumberOfPages() == 2);
        assertTrue(chunk.getMediaSizeName() == MediaSizeName.ISO_A4);

        atom = chunk.getRanges().get(0);
        assertTrue(atom.pageBegin == 4 && atom.pageEnd == 5);

        //
        chunk = chunkInfo.getChunks().get(1);
        assertTrue(chunk.getNumberOfPages() == 2);
        assertTrue(chunk.getMediaSizeName() == MediaSizeName.ISO_A3);

        atom = chunk.getRanges().get(0);
        assertTrue(atom.pageBegin == 4 && atom.pageEnd == 5);

        //
        chunk = chunkInfo.getChunks().get(2);
        assertTrue(chunk.getNumberOfPages() == 5);
        assertTrue(chunk.getMediaSizeName() == MediaSizeName.ISO_A4);

        atom = chunk.getRanges().get(0);
        assertTrue(atom.pageBegin == 1 && atom.pageEnd == 5);
    }

    @Test
    public void testPrintJobChunkInfo3() {

        final InboxInfoDto inboxInfo =
                createJobInfo(new Integer[] { 2 }, new Long[] { 0L });

        inboxInfo.getJobs().get(0).setMedia(MediaSizeName.ISO_A4.toString());

        final ProxyPrintJobChunkInfo chunkInfo = new ProxyPrintJobChunkInfo(
                inboxInfo, RangeAtom.FULL_PAGE_RANGE);

        assertTrue(chunkInfo.getFilteredInboxInfo().getJobs().size() == 1);

        assertTrue(chunkInfo.getChunks().size() == 1);
        assertTrue(chunkInfo.getChunks().get(0)
                .getMediaSizeName() == MediaSizeName.ISO_A4);

    }

    @Test
    public void testPrintJobChunkInfo4() {

        /*
         * Prepare.
         */
        final InboxInfoDto inboxInfo = new InboxInfoDto();

        int iJob = 0;

        // Jobs
        InboxJob job = new InboxJob();
        inboxInfo.getJobs().add(job);

        String name = "file_" + iJob;
        job.setFile(name + ".pdf");
        job.setTitle(name);
        job.setMedia(MediaSizeName.ISO_A4.toString());

        job.setPages(2);

        // Ranges
        InboxJobRange range = new InboxInfoDto.InboxJobRange();
        inboxInfo.getPages().add(range);

        range.setJob(iJob);
        range.setRange(RangeAtom.FULL_PAGE_RANGE);

        /*
         * Test.
         */
        final ProxyPrintJobChunkInfo chunkInfo = new ProxyPrintJobChunkInfo(
                inboxInfo, RangeAtom.FULL_PAGE_RANGE);

        assertTrue(chunkInfo.getFilteredInboxInfo().getJobs().size() == 1);
        assertTrue(chunkInfo.getChunks().size() == 1);
        assertTrue(chunkInfo.getChunks().get(0)
                .getMediaSizeName() == MediaSizeName.ISO_A4);

        RangeAtom atom = chunkInfo.getChunks().get(0).getRanges().get(0);

        assertTrue(atom.pageBegin == 1 && atom.pageEnd == 2);

    }

    @Test
    public void testPrintJobChunkInfo5() {

        /*
         * Prepare.
         */
        final InboxInfoDto inboxInfo = new InboxInfoDto();

        int iJob = 0;

        // Job 0
        InboxJob job = new InboxJob();
        inboxInfo.getJobs().add(job);

        String name = "file_" + iJob;
        job.setFile(name + ".pdf");
        job.setTitle(name);
        job.setMedia(MediaSizeName.ISO_A4.toString());

        job.setPages(5);

        // Job 1
        iJob++;
        job = new InboxJob();
        inboxInfo.getJobs().add(job);

        name = "file_" + iJob;
        job.setFile(name + ".pdf");
        job.setTitle(name);
        job.setMedia(MediaSizeName.ISO_A3.toString());

        job.setPages(10);

        // Job 2
        iJob++;
        job = new InboxJob();
        inboxInfo.getJobs().add(job);

        name = "file_" + iJob;
        job.setFile(name + ".pdf");
        job.setTitle(name);
        job.setMedia(MediaSizeName.ISO_A4.toString());

        job.setPages(5);

        // ------------
        // Ranges
        // ------------
        InboxJobRange jobRange;

        // 0
        jobRange = new InboxInfoDto.InboxJobRange();
        inboxInfo.getPages().add(jobRange);
        jobRange.setJob(0);
        jobRange.setRange("4-5");

        // 1
        jobRange = new InboxInfoDto.InboxJobRange();
        inboxInfo.getPages().add(jobRange);
        jobRange.setJob(1);
        jobRange.setRange("6-10");

        // 2
        jobRange = new InboxInfoDto.InboxJobRange();
        inboxInfo.getPages().add(jobRange);
        jobRange.setJob(2);
        jobRange.setRange("2-3");

        // ------------
        // TEST
        // ------------
        final ProxyPrintJobChunkInfo chunkInfo =
                new ProxyPrintJobChunkInfo(inboxInfo, "2,4-5,7-8");

        assertTrue(chunkInfo.getFilteredInboxInfo().getJobs().size() == 3);
        assertTrue(chunkInfo.getChunks().size() == 3);

        //
        ProxyPrintJobChunk chunk;
        RangeAtom atom;

        //
        chunk = chunkInfo.getChunks().get(0);
        assertTrue(chunk.getMediaSizeName() == MediaSizeName.ISO_A4);
        assertTrue(chunk.getRanges().size() == 1);

        atom = chunk.getRanges().get(0);
        assertTrue(atom.pageBegin == 5 && atom.pageEnd == 5);

        //
        chunk = chunkInfo.getChunks().get(1);
        assertTrue(chunk.getMediaSizeName() == MediaSizeName.ISO_A3);

        assertTrue(chunk.getRanges().size() == 2);

        atom = chunk.getRanges().get(0);
        assertTrue(atom.pageBegin == 7 && atom.pageEnd == 8);

        atom = chunk.getRanges().get(1);
        assertTrue(atom.pageBegin == 10 && atom.pageEnd == 10);

        //
        chunk = chunkInfo.getChunks().get(2);
        assertTrue(chunk.getMediaSizeName() == MediaSizeName.ISO_A4);
        assertTrue(chunk.getRanges().size() == 1);

        atom = chunk.getRanges().get(0);
        assertTrue(atom.pageBegin == 2 && atom.pageEnd == 2);

    }

    @Test
    public void testPrintJobChunkInfo6() {

        /*
         * Prepare.
         */
        final InboxInfoDto inboxInfo = new InboxInfoDto();

        int iJob = 0;

        // Jobs
        InboxJob job = new InboxJob();
        inboxInfo.getJobs().add(job);

        String name = "file_" + iJob;
        job.setFile(name + ".pdf");
        job.setTitle(name);
        job.setMedia(MediaSizeName.ISO_A4.toString());

        job.setPages(2);

        // Ranges
        InboxJobRange range = new InboxInfoDto.InboxJobRange();
        inboxInfo.getPages().add(range);

        range.setJob(iJob);
        range.setRange("2");

        /*
         * Test.
         */
        final ProxyPrintJobChunkInfo chunkInfo = new ProxyPrintJobChunkInfo(
                inboxInfo, RangeAtom.FULL_PAGE_RANGE);

        assertTrue(chunkInfo.getFilteredInboxInfo().getJobs().size() == 1);
        assertTrue(chunkInfo.getChunks().size() == 1);
        assertTrue(chunkInfo.getChunks().get(0)
                .getMediaSizeName() == MediaSizeName.ISO_A4);

        RangeAtom atom = chunkInfo.getChunks().get(0).getRanges().get(0);

        assertTrue(atom.pageBegin == 2 && atom.pageEnd == 2);
    }

    @Test
    public void testPrintJobChunkInfo7() {

        /*
         * Prepare.
         */
        final InboxInfoDto inboxInfo = new InboxInfoDto();

        int iJob = 0;

        // Job 0
        InboxJob job = new InboxJob();
        inboxInfo.getJobs().add(job);

        String name = "file_" + iJob;
        job.setFile(name + ".pdf");
        job.setTitle(name);
        job.setMedia(MediaSizeName.ISO_A4.toString());

        job.setPages(5);

        // Job 1
        iJob++;
        job = new InboxJob();
        inboxInfo.getJobs().add(job);

        name = "file_" + iJob;
        job.setFile(name + ".pdf");
        job.setTitle(name);
        job.setMedia(MediaSizeName.ISO_A4.toString());

        job.setPages(10);

        // Job 2
        iJob++;
        job = new InboxJob();
        inboxInfo.getJobs().add(job);

        name = "file_" + iJob;
        job.setFile(name + ".pdf");
        job.setTitle(name);
        job.setMedia(MediaSizeName.ISO_A4.toString());

        job.setPages(3);

        // ------------
        // Ranges
        // ------------
        InboxJobRange jobRange;

        // 0
        jobRange = new InboxInfoDto.InboxJobRange();
        inboxInfo.getPages().add(jobRange);
        jobRange.setJob(0);
        jobRange.setRange(RangeAtom.FULL_PAGE_RANGE);

        // 1
        jobRange = new InboxInfoDto.InboxJobRange();
        inboxInfo.getPages().add(jobRange);
        jobRange.setJob(1);
        jobRange.setRange(RangeAtom.FULL_PAGE_RANGE);

        // 2
        jobRange = new InboxInfoDto.InboxJobRange();
        inboxInfo.getPages().add(jobRange);
        jobRange.setJob(2);
        jobRange.setRange(RangeAtom.FULL_PAGE_RANGE);

        // ------------
        // TEST
        // ------------
        ProxyPrintJobChunkInfo chunkInfo;
        try {
            chunkInfo = new ProxyPrintJobChunkInfo(inboxInfo);
        } catch (ProxyPrintException e) {
            chunkInfo = null;
        }

        assertTrue(chunkInfo != null);

        assertTrue(chunkInfo.getFilteredInboxInfo().getJobs().size() == 3);
        assertTrue(chunkInfo.getChunks().size() == 3);

        //
        ProxyPrintJobChunk chunk;
        RangeAtom atom;

        //
        chunk = chunkInfo.getChunks().get(0);
        assertTrue(chunk.getMediaSizeName() == MediaSizeName.ISO_A4);
        assertTrue(chunk.getRanges().size() == 1);

        atom = chunk.getRanges().get(0);
        assertTrue(atom.pageBegin == 1 && atom.pageEnd == 5);

        //
        chunk = chunkInfo.getChunks().get(1);
        assertTrue(chunk.getMediaSizeName() == MediaSizeName.ISO_A4);

        assertTrue(chunk.getRanges().size() == 1);

        atom = chunk.getRanges().get(0);
        assertTrue(atom.pageBegin == 1 && atom.pageEnd == 10);

        //
        chunk = chunkInfo.getChunks().get(2);
        assertTrue(chunk.getMediaSizeName() == MediaSizeName.ISO_A4);
        assertTrue(chunk.getRanges().size() == 1);

        atom = chunk.getRanges().get(0);
        assertTrue(atom.pageBegin == 1 && atom.pageEnd == 3);

    }

    @Test
    public void testPrintJobChunkInfo8() {

        /*
         * Prepare.
         */
        final InboxInfoDto inboxInfo = new InboxInfoDto();

        int iJob = 0;

        // Job 0
        InboxJob job = new InboxJob();
        inboxInfo.getJobs().add(job);

        String name = "file_" + iJob;
        job.setFile(name + ".pdf");
        job.setTitle(name);
        job.setMedia(MediaSizeName.ISO_A4.toString());

        job.setPages(5);

        // Job 1
        iJob++;
        job = new InboxJob();
        inboxInfo.getJobs().add(job);

        name = "file_" + iJob;
        job.setFile(name + ".pdf");
        job.setTitle(name);
        job.setMedia(MediaSizeName.ISO_A3.toString());

        job.setPages(10);

        // Job 2
        iJob++;
        job = new InboxJob();
        inboxInfo.getJobs().add(job);

        name = "file_" + iJob;
        job.setFile(name + ".pdf");
        job.setTitle(name);
        job.setMedia(MediaSizeName.ISO_A4.toString());

        job.setPages(5);

        // ------------
        // Ranges
        // ------------
        InboxJobRange jobRange;

        // 0
        jobRange = new InboxInfoDto.InboxJobRange();
        inboxInfo.getPages().add(jobRange);
        jobRange.setJob(0);
        jobRange.setRange(RangeAtom.FULL_PAGE_RANGE);

        // 1
        jobRange = new InboxInfoDto.InboxJobRange();
        inboxInfo.getPages().add(jobRange);
        jobRange.setJob(1);
        jobRange.setRange("6-10");

        // 2
        jobRange = new InboxInfoDto.InboxJobRange();
        inboxInfo.getPages().add(jobRange);
        jobRange.setJob(2);
        jobRange.setRange(RangeAtom.FULL_PAGE_RANGE);

        // ------------
        // TEST
        // ------------
        boolean thrown = false;
        try {
            new ProxyPrintJobChunkInfo(inboxInfo);
        } catch (ProxyPrintException e) {
            thrown = true;
        }

        assertTrue(thrown);

    }

    @Test
    public void testIsVanilla1() {

        /*
         * Prepare.
         */
        final InboxInfoDto inboxInfo = new InboxInfoDto();

        int iJob = 0;

        // Job 0
        InboxJob job = new InboxJob();
        inboxInfo.getJobs().add(job);

        String name = "file_" + iJob;
        job.setFile(name + ".pdf");
        job.setTitle(name);
        job.setMedia(MediaSizeName.ISO_A4.toString());

        job.setPages(5);

        // Job 1
        iJob++;
        job = new InboxJob();
        inboxInfo.getJobs().add(job);

        name = "file_" + iJob;
        job.setFile(name + ".pdf");
        job.setTitle(name);
        job.setMedia(MediaSizeName.ISO_A3.toString());

        job.setPages(10);

        // Job 2
        iJob++;
        job = new InboxJob();
        inboxInfo.getJobs().add(job);

        name = "file_" + iJob;
        job.setFile(name + ".pdf");
        job.setTitle(name);
        job.setMedia(MediaSizeName.ISO_A4.toString());

        job.setPages(5);

        // ------------
        // Ranges
        // ------------
        InboxJobRange jobRange;

        // 0
        jobRange = new InboxInfoDto.InboxJobRange();
        inboxInfo.getPages().add(jobRange);
        jobRange.setJob(0);
        jobRange.setRange(RangeAtom.FULL_PAGE_RANGE);

        // 1
        jobRange = new InboxInfoDto.InboxJobRange();
        inboxInfo.getPages().add(jobRange);
        jobRange.setJob(1);
        jobRange.setRange(RangeAtom.FULL_PAGE_RANGE);

        // 2
        jobRange = new InboxInfoDto.InboxJobRange();
        inboxInfo.getPages().add(jobRange);
        jobRange.setJob(2);
        jobRange.setRange(RangeAtom.FULL_PAGE_RANGE);

        // ------------
        // TEST
        // ------------
        assertTrue(INBOX_SERVICE.isInboxVanilla(inboxInfo));

    }

    @Test
    public void testIsVanilla2() {

        /*
         * Prepare.
         */
        final InboxInfoDto inboxInfo = new InboxInfoDto();

        int iJob = 0;

        // Job 0
        InboxJob job = new InboxJob();
        inboxInfo.getJobs().add(job);

        String name = "file_" + iJob;
        job.setFile(name + ".pdf");
        job.setTitle(name);
        job.setMedia(MediaSizeName.ISO_A4.toString());

        job.setPages(5);

        // Job 1
        iJob++;
        job = new InboxJob();
        inboxInfo.getJobs().add(job);

        name = "file_" + iJob;
        job.setFile(name + ".pdf");
        job.setTitle(name);
        job.setMedia(MediaSizeName.ISO_A3.toString());

        job.setPages(10);

        // Job 2
        iJob++;
        job = new InboxJob();
        inboxInfo.getJobs().add(job);

        name = "file_" + iJob;
        job.setFile(name + ".pdf");
        job.setTitle(name);
        job.setMedia(MediaSizeName.ISO_A4.toString());

        job.setPages(5);

        // ------------
        // Ranges
        // ------------
        InboxJobRange jobRange;

        // 0
        jobRange = new InboxInfoDto.InboxJobRange();
        inboxInfo.getPages().add(jobRange);
        jobRange.setJob(2);
        jobRange.setRange(RangeAtom.FULL_PAGE_RANGE);

        // 1
        jobRange = new InboxInfoDto.InboxJobRange();
        inboxInfo.getPages().add(jobRange);
        jobRange.setJob(1);
        jobRange.setRange(RangeAtom.FULL_PAGE_RANGE);

        // 2
        jobRange = new InboxInfoDto.InboxJobRange();
        inboxInfo.getPages().add(jobRange);
        jobRange.setJob(0);
        jobRange.setRange(RangeAtom.FULL_PAGE_RANGE);

        // ------------
        // TEST
        // ------------
        assertTrue(!INBOX_SERVICE.isInboxVanilla(inboxInfo));
    }

    @Test
    public void testIsVanilla3() {

        /*
         * Prepare.
         */
        final InboxInfoDto inboxInfo = new InboxInfoDto();

        int iJob = 0;

        // Job 0
        InboxJob job = new InboxJob();
        inboxInfo.getJobs().add(job);

        String name = "file_" + iJob;
        job.setFile(name + ".pdf");
        job.setTitle(name);
        job.setMedia(MediaSizeName.ISO_A4.toString());

        job.setPages(5);

        // Job 1
        iJob++;
        job = new InboxJob();
        inboxInfo.getJobs().add(job);

        name = "file_" + iJob;
        job.setFile(name + ".pdf");
        job.setTitle(name);
        job.setMedia(MediaSizeName.ISO_A3.toString());

        job.setPages(10);

        // Job 2
        iJob++;
        job = new InboxJob();
        inboxInfo.getJobs().add(job);

        name = "file_" + iJob;
        job.setFile(name + ".pdf");
        job.setTitle(name);
        job.setMedia(MediaSizeName.ISO_A4.toString());

        job.setPages(5);

        // ------------
        // Ranges
        // ------------
        InboxJobRange jobRange;

        // 0
        jobRange = new InboxInfoDto.InboxJobRange();
        inboxInfo.getPages().add(jobRange);
        jobRange.setJob(0);
        jobRange.setRange("1-");

        // 1
        jobRange = new InboxInfoDto.InboxJobRange();
        inboxInfo.getPages().add(jobRange);
        jobRange.setJob(1);
        jobRange.setRange("1-10");

        // 2
        jobRange = new InboxInfoDto.InboxJobRange();
        inboxInfo.getPages().add(jobRange);
        jobRange.setJob(2);
        jobRange.setRange(RangeAtom.FULL_PAGE_RANGE);

        // ------------
        // TEST
        // ------------
        assertTrue(INBOX_SERVICE.isInboxVanilla(inboxInfo));

    }

}
