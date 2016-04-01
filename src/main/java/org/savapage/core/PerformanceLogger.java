/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2016 Datraverse B.V.
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

import java.text.SimpleDateFormat;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A dedicated logger for simple performance statistics.
 *
 * @author Rijk Ravestein
 *
 */
public final class PerformanceLogger {

    /**
     * .
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(PerformanceLogger.class);

    private static final String DATEFORMAT_PATTERN = "yyyy-MM-dd'\t'HH:mm:ss.S";

    private static final String TIMEFORMAT_PATTERN = "HH:mm:ss.S";

    private PerformanceLogger() {
    }

    /**
     * Gets the start time for a
     * {@link PerformanceLogger#log(Class, String, Long, String)}.
     *
     * @return {@code null} when NOT enabled.
     */
    public static Date startTime() {

        if (isEnabled()) {
            return new Date();
        }
        return null;
    }

    /**
     *
     * @param caller
     * @param method
     * @param timeStart
     * @param msg
     */
    public static <T extends Object> void log(final Class<T> caller,
            final String method, final Date timeStart, final String msg) {

        if (timeStart != null && isEnabled()) {

            final Date now = new Date();

            final SimpleDateFormat dateFormat =
                    new SimpleDateFormat(DATEFORMAT_PATTERN);

            LOGGER.info(
                    String.format("%s\t%s\t%s\t%s\t%s\t%s\t%s",
                            dateFormat.format(now),
                            new SimpleDateFormat(TIMEFORMAT_PATTERN)
                                    .format(timeStart),
                            String.format("%.4f",
                                    (float) (now.getTime()
                                            - timeStart.getTime()) / 10000),
                            caller.getSimpleName(), method, msg,
                            Thread.currentThread().getName()));
        }
    }

    /**
     *
     * @return
     */
    public static boolean isEnabled() {
        return LOGGER.isTraceEnabled();
    }

}
