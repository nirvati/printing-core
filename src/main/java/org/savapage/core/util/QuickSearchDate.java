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
package org.savapage.core.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 *
 * @author Datraverse B.V.
 *
 */
public final class QuickSearchDate {

    final Date date;

    /**
     *
     * @param quickDate
     * @throws ParseException
     */
    public QuickSearchDate(final String quickDate) throws ParseException {
        this.date = toDate(quickDate);
    }

    /**
     *
     * @return
     */
    public Date getDate() {
        return this.date;

    }

    /**
     * Converts a "quick date" to a date object.
     *
     * @param quickDate
     * @return The {@link Date}.
     * @throws ParseException
     */
    public static Date toDate(final String quickDate) throws ParseException {

        String quickDateWork = quickDate;

        while (quickDateWork.length() < 4) {
            quickDateWork += '0';
        }

        for (final String format : new String[] { "yyyyMMddHHmm", "yyyyMMddHH",
                "yyyyMMdd", "yyyyMM", "yyyy" }) {

            if (quickDateWork.length() >= format.length()) {

                final DateFormat dateFormat = new SimpleDateFormat(format);

                dateFormat.setLenient(false);

                return dateFormat.parse(quickDateWork.substring(0,
                        format.length()));
            }
        }
        throw new ParseException(quickDateWork, 0);
    }

}
