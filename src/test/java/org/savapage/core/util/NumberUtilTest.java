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

import java.util.Locale;

import org.junit.Test;

/**
 * Tests for {@link NumberUtil}.
 *
 * @author Rijk Ravestein
 *
 */
public class NumberUtilTest {

    /**
     * As explained <a href=
     * "https://programming.guide/java/formatting-byte-size-to-human-readable-format.html">here</a>.
     */
    @Test
    public void test1() {

        final Object[][] data = {
                // ---------------------------
                // Decimal point
                // ---------------------------
                { Long.valueOf(0), Locale.US, "0 B", "0 B" },
                //
                { Long.valueOf(27), Locale.US, "27 B", "27 B" },
                //
                { Long.valueOf(999), Locale.US, "999 B", "999 B" },
                //
                { Long.valueOf(1000), Locale.US, "1.0 kB", "1000 B" },
                //
                { Long.valueOf(1023), Locale.US, "1.0 kB", "1023 B" },
                //
                { Long.valueOf(1024), Locale.US, "1.0 kB", "1.0 KiB" },
                //
                { Long.valueOf(1728), Locale.US, "1.7 kB", "1.7 KiB" },
                //
                { Long.valueOf(1855425871872L), Locale.US, "1.9 TB",
                        "1.7 TiB" },
                //
                { Long.MAX_VALUE, Locale.US, "9.2 EB", "8.0 EiB" },
                // ---------------------------
                // Decimal comma
                // ---------------------------
                { Long.valueOf(0), Locale.GERMAN, "0 B", "0 B" },
                //
                { Long.valueOf(27), Locale.GERMAN, "27 B", "27 B" },
                //
                { Long.valueOf(999), Locale.GERMAN, "999 B", "999 B" },
                //
                { Long.valueOf(1000), Locale.GERMAN, "1,0 kB", "1000 B" },
                //
                { Long.valueOf(1023), Locale.GERMAN, "1,0 kB", "1023 B" },
                //
                { Long.valueOf(1024), Locale.GERMAN, "1,0 kB", "1,0 KiB" },
                //
                { Long.valueOf(1728), Locale.GERMAN, "1,7 kB", "1,7 KiB" },
                //
                { Long.valueOf(1855425871872L), Locale.GERMAN, "1,9 TB",
                        "1,7 TiB" },
                //
                { Long.MAX_VALUE, Locale.GERMAN, "9,2 EB", "8,0 EiB" }
                //
        };

        for (final Object[] item : data) {
            int i = 0;
            final Long byteCount = (Long) item[i++];
            final Locale locale = (Locale) item[i++];
            assertEquals(NumberUtil.humanReadableByteCountSI(locale,
                    byteCount.longValue()), item[i++]);
            assertEquals(NumberUtil.humanReadableByteCountBin(locale,
                    byteCount.longValue()), item[i++]);
        }
    }
}
