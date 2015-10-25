/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2015 Datraverse B.V.
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
package org.savapage.core.print.smartschool;

import org.apache.commons.lang3.StringUtils;

/**
 * Syntax used for Smartschool transaction comment in PaperCut.
 *
 * @author Rijk Ravestein
 *
 */
public final class SmartSchoolCommentSyntax {

    /**
     * .
     */
    public static final String JOB_NAME_INFO_SEPARATOR = ".";

    /**
     * Symbol for a dummy klas.
     */
    public static final String DUMMY_KLAS = "-";

    /**
     * .
     */
    private static final String FIELD_SEPARATOR_CHAR = "|";

    /**
     * .
     */
    public static final String FIELD_SEPARATOR = " " + FIELD_SEPARATOR_CHAR
            + " ";

    /**
     * .
     */
    public static final String FIELD_SEPARATOR_FIRST = "";

    /**
     * .
     */
    public static final String FIELD_SEPARATOR_LAST = "";

    /**
     * .
     */
    public static final char USER_CLASS_SEPARATOR = '@';

    /**
     * Duplex.
     */
    public static final String INDICATOR_DUPLEX_ON = "D";

    /**
     * Singlex.
     */
    public static final String INDICATOR_DUPLEX_OFF = "S";

    /**
     * Color.
     */
    public static final String INDICATOR_COLOR_ON = "C";

    /**
     * Grayscale.
     */
    public static final String INDICATOR_COLOR_OFF = "G";

    /**
     * Hide instantiation.
     */
    private SmartSchoolCommentSyntax() {

    }

    /**
     * Returns a short upper-case indicator of a paper size description.
     *
     * <pre>
     * "aa-bb"      --> "BB"
     * "aa"         --> "AA"
     * "is-a4"      --> "A4"
     * "na-letter"  --> "LETTER"
     * </pre>
     *
     * @param papersize
     *            The full paper size description.
     * @return The paper size indicator.
     */
    public static String convertToPaperSizeIndicator(final String papersize) {

        int index = StringUtils.indexOf(papersize, '-');

        if (index == StringUtils.INDEX_NOT_FOUND) {
            return papersize.toUpperCase();
        }
        index++;
        if (index == papersize.length()) {
            return "";
        }
        return StringUtils.substring(papersize, index).toUpperCase();
    }

    /**
     * Appends indicator fields to {@link StringBuilder} without leading and
     * trailing field separator. Example:
     * <p>
     * {@code "A4 | D | C | 243"}
     * </p>
     *
     * @param str
     *            The {@link StringBuilder} to append to.
     * @param indicatorPaperSize
     * @param indicatorDuplex
     * @param indicatorColor
     * @param indicatorExternalId
     * @return The {@link StringBuilder} that was appended to.
     */
    public static StringBuilder appendIndicatorFields(final StringBuilder str,
            final String indicatorPaperSize, final String indicatorDuplex,
            final String indicatorColor, final String indicatorExternalId) {

        str.append(indicatorPaperSize).append(FIELD_SEPARATOR)
                .append(indicatorDuplex).append(FIELD_SEPARATOR)
                .append(indicatorColor).append(FIELD_SEPARATOR)
                .append(indicatorExternalId);
        return str;
    }
}
