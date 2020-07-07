/*
 * This file is part of the SavaPage project <http://savapage.org>.
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.core.ipp.attribute.syntax;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.savapage.core.SpException;
import org.savapage.core.ipp.encoding.IppEncoder;
import org.savapage.core.ipp.encoding.IppValueTag;
import org.savapage.core.util.DateUtil;

/**
 * OCTET-STRING consisting of eleven octets whose contents are defined by
 * "DateAndTime" in <a href="http://tools.ietf.org/html/rfc1903">RFC1903</a>.
 *
 * <pre>
 * DISPLAY-HINT "2d-1d-1d,1d:1d:1d.1d,1a1d:1d"
 *
 * field  octets  contents                  range
 * -----  ------  --------                  -----
 *   1      1-2   year                      0..65536
 *   2       3    month                     1..12
 *   3       4    day                       1..31
 *   4       5    hour                      0..23
 *   5       6    minutes                   0..59
 *   6       7    seconds                   0..60
 *                (use 60 for leap-second)
 *   7       8    deci-seconds              0..9
 *   8       9    direction from UTC        '+' / '-'
 *   9      10    hours from UTC            0..11
 *  10      11    minutes from UTC          0..59
 *
 *  For example, Tuesday May 26, 1992 at 1:30:15 PM EDT would be
 *  displayed as:
 *
 *  1992-5-26,13:30:15.0,-4:0
 *
 *  Note that if only local time is known, then timezone
 *  information (fields 8-10) is not present."
 *
 *  SYNTAX       OCTET STRING (SIZE (8 | 11))
 * </pre>
 *
 * @author Datraverse B.V.
 */
public class IppDateTime extends AbstractIppAttrSyntax {

    /**
     * NOTE: {@link SimpleDateFormat} is NOT thread safe, so we can NOT have a
     * static instance.
     */
    private static final String DATE_FORMAT_PATTERN =
            "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

    /**
     * The SingletonHolder is loaded on the first execution of
     * {@link IppDateTime#instance()} or the first access to
     * {@link SingletonHolder#INSTANCE}, not before.
     * <p>
     * <a href=
     * "http://en.wikipedia.org/wiki/Singleton_pattern#The_solution_of_Bill_Pugh"
     * >The Singleton solution of Bill Pugh</a>
     * </p>
     */
    private static class SingletonHolder {
        public static final IppDateTime INSTANCE = new IppDateTime();
    }

    /**
     * Gets the singleton instance.
     *
     * @return
     */
    public static IppDateTime instance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     *
     * @param date
     * @return
     */
    public static String formatDate(final Date date) {
        final DateFormat formatter = new SimpleDateFormat(DATE_FORMAT_PATTERN);
        return formatter.format(date);
    }

    /**
     *
     * @param date
     * @return
     * @throws ParseException
     */
    public static Date parseDate(final String date) throws ParseException {
        final DateFormat formatter = new SimpleDateFormat(DATE_FORMAT_PATTERN);
        return formatter.parse(date);
    }

    @Override
    public final IppValueTag getValueTag() {
        return IppValueTag.DATETIME;
    }

    @Override
    public final void write(final OutputStream ostr, final String formattedDate,
            final Charset charset) throws IOException {

        final DateFormat formatter = new SimpleDateFormat(DATE_FORMAT_PATTERN);

        Calendar calendar = new GregorianCalendar();

        try {
            calendar.setTime(formatter.parse(formattedDate));
        } catch (ParseException e) {
            throw new SpException(e);
        }

        IppEncoder.writeInt16(ostr, 11); // length

        IppEncoder.writeInt16(ostr, calendar.get(Calendar.YEAR));
        IppEncoder.writeInt8(ostr, calendar.get(Calendar.MONTH));
        IppEncoder.writeInt8(ostr, calendar.get(Calendar.DAY_OF_MONTH) + 1);

        IppEncoder.writeInt8(ostr, calendar.get(Calendar.HOUR_OF_DAY));
        IppEncoder.writeInt8(ostr, calendar.get(Calendar.MINUTE));
        IppEncoder.writeInt8(ostr, calendar.get(Calendar.SECOND));

        IppEncoder.writeInt8(ostr, calendar.get(Calendar.MILLISECOND)
                / DateUtil.MSEC_IN_DECI_SECOND);

        //
        final int zoneOffset = calendar.get(Calendar.ZONE_OFFSET);
        final int zoneMsec;
        final char zoneDirection;
        if (zoneOffset < 0) {
            zoneDirection = '-';
            zoneMsec = -zoneOffset;
        } else {
            zoneDirection = '+';
            zoneMsec = zoneOffset;
        }

        // Direction from UTC.
        IppEncoder.writeInt8(ostr, zoneDirection);

        // Hours from UTC
        IppEncoder.writeInt8(ostr,
                (int) (zoneMsec / DateUtil.DURATION_MSEC_HOUR));
        // Minutes from UTC
        IppEncoder.writeInt8(ostr,
                (int) ((zoneMsec % DateUtil.DURATION_MSEC_HOUR)
                        / DateUtil.SECONDS_IN_MINUTE));
    }

    /**
     * Reads encoded IPP bytes and constructs a formatted date.
     *
     * @see {@link #formatDate(Date)}
     * @see {@link #parseDate(String)}
     * @param bytes
     *            The encoded IPP datetime.
     * @return The formatted date.
     */
    public static String read(byte[] bytes) {
        Calendar calendar = new GregorianCalendar();
        calendar.set(IppEncoder.readInt16(bytes[0], bytes[1]), // year
                (bytes[2]) - 1, // month
                bytes[3], // day
                bytes[4], // hour
                bytes[5], // minute
                bytes[6] // second
        );
        return formatDate(calendar.getTime());
    }

}
