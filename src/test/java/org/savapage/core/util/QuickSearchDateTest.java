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
package org.savapage.core.util;

import static org.junit.Assert.assertEquals;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.junit.Test;

/**
 *
 * @author Rijk Ravestein
 *
 */
public class QuickSearchDateTest {

    private static final String DATE_FORMAT_PATTERN = "yyyy-MM-dd'|'HH:mm:ss";
    private final DateFormat dateFormat =
            new SimpleDateFormat(DATE_FORMAT_PATTERN);

    /**
     *
     * @param in
     * @param out
     * @throws ParseException
     */
    private void test(String quickDate, String quickDateFormatted)
            throws ParseException {
        assertEquals(dateFormat.format(QuickSearchDate.toDate(quickDate)),
                quickDateFormatted);
    }

    @Test
    public void testFull() throws ParseException {

        test("2014", "2014-01-01|00:00:00");
        test("201403", "2014-03-01|00:00:00");
        test("20140331", "2014-03-31|00:00:00");
        test("2014033109", "2014-03-31|09:00:00");
        test("201403311305", "2014-03-31|13:05:00");
    }

    @Test
    public void testPartial() throws ParseException {

        test("2", "2000-01-01|00:00:00");
        test("20", "2000-01-01|00:00:00");
        test("201", "2010-01-01|00:00:00");

        test("2014033", "2014-03-01|00:00:00");
        test("201403310", "2014-03-31|00:00:00");
        test("20140331130", "2014-03-31|13:00:00");
    }

    @Test(expected = ParseException.class)
    public void testExpection() throws ParseException {
        for (final String quickDate : new String[] { "", "0", "201499",
                "20140332", "2014033125", "201403311360" }) {
            QuickSearchDate.toDate(quickDate);
        }
    }

}
