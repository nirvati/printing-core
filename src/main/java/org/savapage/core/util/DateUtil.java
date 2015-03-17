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
package org.savapage.core.util;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 *
 * @author Datraverse B.V.
 */
public final class DateUtil {

    /**
     * A second in milliseconds.
     */
    public static final long DURATION_MSEC_SECOND = 1000;

    /**
     * A minute in milliseconds.
     */
    public static final long DURATION_MSEC_MINUTE = 60 * DURATION_MSEC_SECOND;


    /**
     * A hundredth of a second in milliseconds.
     */
    public static final int MSEC_IN_HUNDREDTH_OF_SECOND = 10;


    /**
     *
     */
    private DateUtil() {
    }

    /**
     * Converts a {@link Date} to a ISO8601 formatted date string.
     * <p>
     * Example: {@code 20141206T124325}
     * </p>
     *
     * @param date
     *            The date to convert.
     * @return The ISO8601 formatted date string.
     */
    public static String dateAsIso8601(final Date date) {
        if (date == null) {
            return "";
        }
        return new SimpleDateFormat("yyyyMMdd'T'HHmmss").format(date);
    }

    /**
     * Formats a {@link Date} to a user friendly formatted date-time string.
     * <p>
     * Example: {@code 2014-12-06T12:43:25}
     * </p>
     *
     * @param date
     *            The date to convert.
     * @return The formatted date string.
     */
    public static String formattedDateTime(final Date date) {
        if (date == null) {
            return "";
        }
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(date);
    }

}
