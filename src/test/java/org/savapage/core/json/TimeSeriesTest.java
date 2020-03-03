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
package org.savapage.core.json;

import static org.junit.Assert.assertTrue;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.Test;

/**
 *
 * @author Datraverse B.V.
 *
 */
public class TimeSeriesTest {

    @Test
    public void testDaySeries() {
        final JsonRollingTimeSeries<Integer> data =
                new JsonRollingTimeSeries<>(TimeSeriesInterval.DAY, 7, 0);

        // ------------------------------
        // First observation
        // ------------------------------
        final Date now = new Date();

        data.addDataPoint(now, 1);
        assertTrue(data.getData().get(0) == 1);

        data.addDataPoint(now, 1);
        assertTrue(data.getData().get(0) == 2);

        assertTrue(data.getData().size() == 1);

        // assertTrue(data.getLastTime().longValue() == now.getTime());

        // ------------------------------
        // Next observation: 3 days after
        // ------------------------------
        final Date plus3 = DateUtils.addDays(now, 3);

        data.addDataPoint(plus3, 3);

        // assertTrue(data.getLastTime().longValue() == plus3.getTime());

        assertTrue(data.getData().get(0) == 3);
        assertTrue(data.getData().get(1) == 0);
        assertTrue(data.getData().get(2) == 0);
        assertTrue(data.getData().get(3) == 2);

        // ------------------------------
        // Next observation: 7 days after
        // ------------------------------
        final Date plus7 = DateUtils.addDays(plus3, 4);
        data.addDataPoint(plus7, 5);
        assertTrue(data.getData().get(0) == 5);
        assertTrue(data.getData().get(1) == 0);
        assertTrue(data.getData().get(2) == 0);
        assertTrue(data.getData().get(3) == 0);
        assertTrue(data.getData().get(4) == 3);
        assertTrue(data.getData().get(5) == 0);
        assertTrue(data.getData().get(6) == 0);

    }

    @Test
    public void testWeekSeries() throws ParseException {

        final SimpleDateFormat dateFormat =
                new SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss");

        testWeekSeries(dateFormat.parse("2013-12-01T00-00-00"));
    }

    @Test
    public void testWeekSeriesEndOfYear() throws ParseException {

        final SimpleDateFormat dateFormat =
                new SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss");

        testWeekSeries(dateFormat.parse("2013-12-08T00-00-00"));
    }

    /**
     * Adds observations at "now" and at +1 and +3 weeks.
     *
     * @param now
     */
    public void testWeekSeries(final Date now) {

        final JsonRollingTimeSeries<Integer> data =
                new JsonRollingTimeSeries<>(TimeSeriesInterval.WEEK, 4, 0);

        data.addDataPoint(now, 1);
        data.addDataPoint(now, 1);

        assertTrue(data.getData().get(0) == 2);

        // -----------------------
        final Date plus1 = DateUtils.addWeeks(now, 1);
        data.addDataPoint(plus1, 1);

        assertTrue(data.getData().size() == 2);
        assertTrue(data.getData().get(0) == 1);
        assertTrue(data.getData().get(1) == 2);

        // -----------------------
        final Date plus3 = DateUtils.addWeeks(now, 3);
        data.addDataPoint(plus3, 10);

        assertTrue(data.getData().size() == 4);
        assertTrue(data.getData().get(0) == 10);
        assertTrue(data.getData().get(1) == 0);
        assertTrue(data.getData().get(2) == 1);
        assertTrue(data.getData().get(3) == 2);

    }

    @Test
    public void testMonthSeries() {
        final JsonRollingTimeSeries<Integer> data =
                new JsonRollingTimeSeries<>(TimeSeriesInterval.MONTH, 3, 0);

        // -----------------------
        final Date now = new Date();
        data.addDataPoint(now, 1);
        data.addDataPoint(now, 1);

        assertTrue(data.getData().get(0) == 2);

        // -----------------------
        final Date plus1 = DateUtils.addMonths(now, 1);
        data.addDataPoint(plus1, 1);

        assertTrue(data.getData().size() == 2);
        assertTrue(data.getData().get(0) == 1);
        assertTrue(data.getData().get(1) == 2);

        // -----------------------
        final Date plus3 = DateUtils.addMonths(now, 3);
        data.addDataPoint(plus3, 10);
        assertTrue(data.getData().size() == 3);
        assertTrue(data.getData().get(0) == 10);
        assertTrue(data.getData().get(1) == 0);
        assertTrue(data.getData().get(2) == 1);

    }

}
