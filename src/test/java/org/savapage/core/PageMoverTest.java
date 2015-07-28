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

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.apache.log4j.BasicConfigurator;
import org.junit.Before;
import org.junit.Test;
import org.savapage.core.inbox.InboxInfoDto;
import org.savapage.core.inbox.InboxInfoDto.InboxJobRange;
import org.savapage.core.inbox.RangeAtom;
import org.savapage.core.services.helpers.InboxPageMover;

/**
 *
 * @author Datraverse B.V.
 *
 */
public class PageMoverTest {

    @Before
    public final void initTest() {
        /*
         * Set up a simple log4j configuration that logs on the console.
         */
        BasicConfigurator.configure();
    }

    /**
     *
     * @return
     */
    private InboxInfoDto createJobInfo(final Integer[] jobPages) {

        final InboxInfoDto jobinfo = new InboxInfoDto();

        int i = 0;

        for (int pages : jobPages) {

            final String name = "file_" + i;

            InboxInfoDto.InboxJob job = new InboxInfoDto.InboxJob();
            job.setFile(name + ".pdf");
            job.setTitle(name);
            job.setPages(pages);

            jobinfo.getJobs().add(job);

            InboxInfoDto.InboxJobRange range = new InboxInfoDto.InboxJobRange();
            range.setJob(i++);
            range.setRange(RangeAtom.FULL_PAGE_RANGE); // all pages

            jobinfo.getPages().add(range); // append

        }

        return jobinfo;
    }

    @Test
    public final void testDelete() throws IOException {

        final InboxInfoDto jobinfo = createJobInfo(new Integer[] { 5, 5 });

        int pages;
        /*
         * Delete pages 1,2
         */
        String nRanges = "1,2";

        pages = InboxPageMover.deletePages(null, jobinfo, nRanges);
        // System.out.println(jobinfo.prettyPrinted());
        assertTrue(pages == 2);
        assertTrue(jobinfo.getPages().get(0).getRange().equals("3-5"));
        assertTrue(jobinfo.getPages().get(1).getRange().equals("1-5"));

        /*
         * Delete pages 1,2
         */
        nRanges = "1,2";
        pages = InboxPageMover.deletePages(null, jobinfo, nRanges);
        assertTrue(pages == 2);
        assertTrue(jobinfo.getPages().get(0).getRange().equals("5-5"));
        assertTrue(jobinfo.getPages().get(1).getRange().equals("1-5"));

        /*
         * Delete pages 2
         */
        nRanges = "2";
        pages = InboxPageMover.deletePages(null, jobinfo, nRanges);
        assertTrue(pages == 1);
        assertTrue(jobinfo.getPages().get(0).getRange().equals("5-5"));
        assertTrue(jobinfo.getPages().get(1).getRange().equals("2-5"));
    }

    @Test
    public void testMove() throws IOException {

        final InboxInfoDto jobinfo = createJobInfo(new Integer[] { 5, 5 });

        /*
         * Move pages 1,2 to 3
         */
        String nRanges = "1,2";
        int nPage2Move2 = 3;
        int pages = InboxPageMover.movePages(null, jobinfo, nRanges, nPage2Move2);
        assertTrue(pages == 2);

        InboxJobRange range;

        range = jobinfo.getPages().get(0);
        assertTrue(range.getJob() == 0);
        assertTrue(range.getRange().equals("3-4"));

        range = jobinfo.getPages().get(1);
        assertTrue(range.getJob() == 0);
        assertTrue(range.getRange().equals("1-2"));

        range = jobinfo.getPages().get(2);
        assertTrue(range.getJob() == 0);
        assertTrue(range.getRange().equals("5-5"));

        range = jobinfo.getPages().get(3);
        assertTrue(range.getJob() == 1);
        assertTrue(range.getRange().equals("1-5"));

    }
}
