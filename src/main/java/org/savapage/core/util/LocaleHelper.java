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

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.Locale;

/**
 *
 * @author Datraverse B.V.
 *
 */
public final class LocaleHelper {

    private final Locale locale;

    /**
     *
     * @param locale
     */
    public LocaleHelper(Locale locale) {
        this.locale = locale;
    }

    /**
     * Gets as localized (short) date string of a Date.
     *
     * @param date
     *            The date.
     * @return The localized date string.
     */
    public String getShortDate(final Date date) {
        return DateFormat.getDateInstance(DateFormat.SHORT, this.locale)
                .format(date);
    }

    /**
     * Gets as localized (medium) date string of a Date.
     *
     * @param date
     *            The date.
     * @return The localized date string.
     */
    public String getMediumDate(final Date date) {
        return DateFormat.getDateInstance(DateFormat.MEDIUM, this.locale)
                .format(date);
    }

    /**
     * Gets as localized (short) date string of a Date.
     *
     * @param date
     *            The date.
     * @return The localized date string.
     */
    public String getLongDate(final Date date) {
        return DateFormat.getDateInstance(DateFormat.LONG, this.locale)
                .format(date);
    }

    /**
     * Gets as localized string of a Number.
     *
     * @param number
     * @return The localized string.
     */
    public String getNumber(final long number) {
        return NumberFormat.getInstance(this.locale).format(number);
    }

    /**
     * Gets as localized short date/time string of a Date.
     *
     * @param date
     *            The date.
     * @return The localized short date/time string.
     */
    public String getShortDateTime(final Date date) {
        return DateFormat.getDateTimeInstance(DateFormat.SHORT,
                DateFormat.SHORT, this.locale).format(date);
    }

    /**
     * Gets as localized (long)date/(medium)time string of a Date.
     *
     * @param date
     *            The date.
     * @return The localized date/time string.
     */
    public String getLongMediumDateTime(final Date date) {
        return DateFormat.getDateTimeInstance(DateFormat.LONG,
                DateFormat.MEDIUM, this.locale).format(date);
    }

    /**
     *
     * @param decimal
     * @param fractionDigits
     * @param currencySymbol
     * @return
     * @throws ParseException
     */
    public String getCurrencyDecimal(final BigDecimal decimal,
            int fractionDigits, final String currencySymbol)
            throws ParseException {
        return BigDecimalUtil.localize(decimal, fractionDigits, this.locale,
                currencySymbol, true);
    }

    /**
     *
     * @param decimal
     * @param fractionDigits
     * @return
     * @throws ParseException
     */
    public String getDecimal(final BigDecimal decimal, int fractionDigits)
            throws ParseException {
        return getCurrencyDecimal(decimal, fractionDigits, "");
    }

    /**
     * Gets the localized user interface text of an {@link Enum} value.
     *
     * @param <E>
     *            The Enum class.
     * @param value
     *            The Enum value.
     * @param locale
     *            The {@link Locale}.
     * @return The localized text.
     */
    public static <E extends Enum<E>> String uiText(final Enum<E> value,
            final Locale locale) {
        return Messages
                .loadXmlResource(value.getClass(),
                        value.getClass().getSimpleName(), locale)
                .getString(value.toString());
    }

}
