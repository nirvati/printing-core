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
package org.savapage.core.inbox;

import java.util.List;
import java.util.regex.Pattern;

/**
 *
 * @author Rijk Ravestein
 *
 */
public class RangeAtom {

    /**
     * The full page range (comprising all job pages).
     */
    public static final String FULL_PAGE_RANGE = "";

    public Integer pageBegin;
    public Integer pageEnd;

    /**
     * Creates a {@link RangeAtom} from text.
     *
     * @param range
     *            The range string. For example: "1-5", "1", "1-".
     * @return The {@link RangeAtom};
     */
    public static RangeAtom fromText(final String value) {

        final RangeAtom atom;

        if (value.equals(FULL_PAGE_RANGE)) {
            return new RangeAtom();
        }

        final Pattern p = Pattern.compile("^\\d+$");

        String begin = null;
        String[] range = value.split("\\-", -1);

        switch (range.length) {

        case 1:

            begin = range[0].trim();

            if (!begin.isEmpty() && p.matcher(begin).matches()) {
                atom = new RangeAtom();
                atom.pageBegin = Integer.parseInt(begin);
                atom.pageEnd = atom.pageBegin;
            } else {
                atom = null;
            }
            break;

        case 2:

            begin = range[0].trim();
            if (begin.isEmpty()) {
                begin = "1";
            }

            String end = range[1].trim();
            if (end.isEmpty()) {
                end = null;
            }

            if (!p.matcher(begin).matches()) {
                atom = null;
                break;
            }

            if (end != null && !p.matcher(end).matches()) {
                atom = null;
                break;
            }

            final Integer pageBegin = Integer.parseInt(begin);

            if (end == null) {

                atom = new RangeAtom();
                atom.pageBegin = pageBegin;

            } else {

                final Integer pageEnd = Integer.parseInt(end);

                if (pageEnd < pageBegin) {
                    atom = null;
                } else {
                    atom = new RangeAtom();
                    atom.pageBegin = pageBegin;
                    atom.pageEnd = pageEnd;
                }
            }

            break;

        default:
            atom = null;
        }

        if (atom == null) {
            throw new IllegalArgumentException(
                    "range \"" + value + "\" has invalid syntax");
        }

        return atom;
    }

    /**
     * For example: "1-5", "1".
     */
    public String asText() {
        return (pageBegin == null ? "1" : pageBegin) + "-"
                + (pageEnd == null ? "" : pageEnd);
    }

    public int calcPageFrom() {
        return (pageBegin == null ? 1 : pageBegin);
    }

    public int calcPageTo() {
        return calcPageTo(pageBegin);
    }

    /**
     *
     * @return The number of pages in the range.
     */
    public int calcPages() {
        return this.calcPageTo() - this.calcPageFrom() + 1;
    }

    /**
     * Calculates the end page.
     *
     * @param dfault
     *            The default end page when {@link #pageEnd} is not defined.
     * @return The calculated end page.
     */
    public int calcPageTo(int dfault) {
        return (pageEnd == null ? dfault : pageEnd);
    }

    /**
     * Creates a string representation of an array of ranges.
     *
     * Every object in the array is a range with one-based from and to page:
     *
     * @param ranges
     *            The list of ranges.
     * @return The string representation
     */
    public static String asText(final List<RangeAtom> ranges) {

        String txt = "";

        boolean first = true;

        for (final RangeAtom range : ranges) {

            if (first) {
                first = false;
            } else {
                txt += ',';
            }

            if (range.pageBegin.equals(range.pageEnd)) {
                txt += range.pageBegin;

            } else if (range.pageBegin != 1 || range.pageEnd != null) {

                txt += range.pageBegin.toString() + '-';

                if (range.pageEnd != null) {
                    txt += range.pageEnd;
                }
            }
        }

        return txt;
    }

}
