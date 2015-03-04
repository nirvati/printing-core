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
package org.savapage.core.inbox;

import java.util.List;

/**
 *
 * @author Datraverse B.V.
 *
 */
public class RangeAtom {

    public Integer pageBegin;
    public Integer pageEnd;

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
