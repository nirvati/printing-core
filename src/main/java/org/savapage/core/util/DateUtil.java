/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2019 Datraverse B.V.
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.core.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.format.TextStyle;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicLong;

import org.quartz.CronExpression;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class DateUtil {

    /**
     * The last time used by {@link #uniqueCurrentTime()}.
     */
    private static final AtomicLong LAST_TIME_MS = new AtomicLong();

    /**
     * A second in milliseconds.
     */
    public static final long DURATION_MSEC_SECOND = 1000;

    /**
     * A minute in milliseconds.
     */
    public static final long DURATION_MSEC_MINUTE = 60 * DURATION_MSEC_SECOND;

    /**
     * An hour in milliseconds.
     */
    public static final long DURATION_MSEC_HOUR = 60 * DURATION_MSEC_MINUTE;

    /**
     * A day in milliseconds.
     */
    public static final long DURATION_MSEC_DAY = 24 * DURATION_MSEC_HOUR;

    /**
     * A hundredth of a second in milliseconds.
     */
    public static final int MSEC_IN_HUNDREDTH_OF_SECOND = 10;

    /**
     * The number of seconds in a minute.
     */
    public static final int SECONDS_IN_MINUTE = 60;

    /**
     * The number of minutes in a hour.
     */
    public static final int MINUTES_IN_HOUR = 60;

    /**
     *
     */
    public static final int DAYS_IN_WEEK = 7;

    /**
     *
     */
    private DateUtil() {
    }

    /**
     * Gets a application wide unique current time in milliseconds.
     * <p>
     * See: <a href=
     * "http://stackoverflow.com/questions/9191288/creating-a-unique-timestamp-in-java">
     * stackoverflow.com</a>
     * </p>
     *
     * @return The unique current time in milliseconds.
     */
    public static long uniqueCurrentTime() {

        long now = System.currentTimeMillis();

        while (true) {
            long lastTime = LAST_TIME_MS.get();
            if (lastTime >= now) {
                now = lastTime + 1;
            }
            if (LAST_TIME_MS.compareAndSet(lastTime, now)) {
                return now;
            }
        }
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

    /**
     * Formats elapsed milliseconds into readable string.
     *
     * @param duration
     *            milliseconds
     * @return formatted string
     */
    public static String formatDuration(final long duration) {

        long durationSeconds = duration / DURATION_MSEC_SECOND;

        long days = durationSeconds / 86400;
        long hours = (durationSeconds % 86400) / 3600;
        long minutes = ((durationSeconds % 86400) % 3600) / 60;

        if (days == 0) {
            if (hours == 0) {
                if (minutes == 0) {
                    long seconds = ((durationSeconds % 86400) % 3600) % 60;
                    if (seconds == 0) {
                        return String.format("%dms", duration);
                    }
                    return String.format("%ds", seconds);
                }
                return String.format("%dm", minutes);
            }
            return String.format("%dh %dm", hours, minutes);
        }
        return String.format("%dd %dh", days, hours, minutes);
    }

    /**
     * Gets as localized short time string of a Date.
     *
     * @param date
     *            The date.
     * @param locale
     *            The {@link Locale}.
     * @return The localized short time string. For example: "16:30".
     */
    public static String localizedShortTime(final Date date,
            final Locale locale) {
        return DateFormat.getTimeInstance(DateFormat.SHORT, locale)
                .format(date);
    }

    /**
     * Gets number of seconds between two dates.
     *
     * @param d1
     *            First date.
     * @param d2
     *            Second date (later in time).
     * @return Seconds.
     */
    public static long secondsBetween(final Date d1, final Date d2) {
        return unitsBetween(d1, d2, DURATION_MSEC_SECOND);
    }

    /**
     * Gets number of minutes between two dates.
     *
     * @param d1
     *            First date.
     * @param d2
     *            Second date (later in time).
     * @return Seconds.
     */
    public static long minutesBetween(final Date d1, final Date d2) {
        return unitsBetween(d1, d2, DURATION_MSEC_MINUTE);
    }

    /**
     * Gets number of units between two dates.
     *
     * @param d1
     *            First date.
     * @param d2
     *            Second date (later in time).
     * @param unitMsec
     *            Unit in msecs.
     * @return Seconds.
     */
    private static long unitsBetween(final Date d1, final Date d2,
            final long unitMsec) {
        return (d2.getTime() - d1.getTime()) / unitMsec;
    }

    /**
     * Get the weekday ordinals of a CRON expression.
     *
     * @param exp
     *            The CRON expression.
     * @return The weekday ordinals, sorted ascending (Sunday is zero).
     */
    public static SortedSet<Integer>
            getWeekDayOrdinals(final CronExpression exp) {

        final SortedSet<Integer> weekDays = new TreeSet<>();
        final Calendar c = Calendar.getInstance();
        Date nextDate = new Date();
        for (int i = 0; i < DAYS_IN_WEEK; i++) {
            nextDate = exp.getNextValidTimeAfter(nextDate);
            c.setTime(nextDate);
            weekDays.add(c.get(Calendar.DAY_OF_WEEK) - 1);
        }
        return weekDays;
    }

    /**
     * Gets the localized weekday string of a set of weekday ordinalss in short
     * text style.
     *
     * @param weekDays
     *            The weekday ordinals (Sunday is zero).
     * @param locale
     *            The locale.
     * @return The localized weekday string in short text style.
     */
    public static String getWeekDayOrdinalsText(final Set<Integer> weekDays,
            final Locale locale) {

        final StringBuilder txt = new StringBuilder();

        for (final Integer weekDay : weekDays) {

            final DayOfWeek dayOfWeek;

            if (weekDay == 0) {
                dayOfWeek = DayOfWeek.SUNDAY;
            } else {
                dayOfWeek = DayOfWeek.of(weekDay);
            }

            if (txt.length() > 0) {
                txt.append(" ");
            }

            txt.append(dayOfWeek.getDisplayName(TextStyle.SHORT, locale));
        }
        return txt.toString();
    }

}
