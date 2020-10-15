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
package org.savapage.core.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.savapage.core.SpException;

/**
 * Utility methods for {@link BigDecimal} conversions.
 *
 * @author Rijk Ravestein
 *
 */
public final class BigDecimalUtil {

    /**
     * Translates a formatted decimal value to a {@link BigDecimal}.
     *
     * @param localizedDecimal
     *            The localized decimal.
     * @param locale
     *            The {@link Locale} of the formatted decimal.
     * @param currency
     *            {@code true} when formatted decimal contains a currency
     *            symbol.
     * @param groupingUsed
     *            {@code true} when grouping is used (like {@code 1,000.00} in
     *            {@code en-US}).
     * @return The BigDecimal.
     * @throws ParseException
     *             When formatted decimal is invalid.
     */
    public static BigDecimal parse(final String localizedDecimal,
            final Locale locale, boolean currency, boolean groupingUsed)
            throws ParseException {
        final DecimalFormat df = getDecimalFormat(locale, currency);
        df.setParseBigDecimal(true);
        df.setGroupingUsed(groupingUsed);
        return (BigDecimal) df.parse(localizedDecimal);
    }

    /**
     * Localizes a {@link BigDecimal}.
     *
     * @param decimal
     * @param locale
     * @param groupingUsed
     * @return
     * @throws ParseException
     */
    public static String localize(final BigDecimal decimal, final Locale locale,
            boolean groupingUsed) throws ParseException {
        final DecimalFormat df = getDecimalFormat(locale, false);
        df.setParseBigDecimal(true);
        df.setGroupingUsed(groupingUsed);
        return df.format(decimal);
    }

    /**
     * Localizes a {@link BigDecimal}.
     *
     * @param decimal
     * @param fractionDigits
     * @param locale
     * @return The localized string.
     * @throws ParseException
     */
    public static String localize(final BigDecimal decimal, int fractionDigits,
            final Locale locale, boolean groupingUsed) throws ParseException {
        return localize(decimal, fractionDigits, locale, false, groupingUsed);
    }

    /**
     * Localizes a {@link BigDecimal} (throwing unchecked exception).
     *
     * @param decimal
     * @param fractionDigits
     * @param locale
     * @return The localized string.
     */
    public static String localizeUc(final BigDecimal decimal,
            int fractionDigits, final Locale locale, boolean groupingUsed) {
        try {
            return localize(decimal, fractionDigits, locale, false,
                    groupingUsed);
        } catch (ParseException e) {
            throw new SpException(e);
        }
    }

    /**
     * Localizes a {@link BigDecimal}.
     *
     * @param decimal
     * @param fractionDigits
     * @param locale
     * @param currency
     *            {@code true} when formatted decimal must contain a currency
     *            symbol.
     * @param groupingUsed
     *            {@code true} when grouping is used (like {@code 1,000.00} in
     *            {@code en-US}).
     * @return
     * @throws ParseException
     */
    public static String localize(final BigDecimal decimal, int fractionDigits,
            final Locale locale, boolean currency, boolean groupingUsed)
            throws ParseException {

        final DecimalFormat df = getDecimalFormat(locale, currency);

        df.setParseBigDecimal(true);
        df.setGroupingUsed(groupingUsed);
        df.setMinimumFractionDigits(fractionDigits);
        df.setMaximumFractionDigits(fractionDigits);

        return df.format(decimal);
    }

    /**
     * Gets the right-sized precision of a {@link BigDecimal}.
     *
     * @param decimal
     *            The {@link BigDecimal} to localize.
     * @param fractionDigitsMinimum
     *            The minimum number of fraction digits in the result.
     * @return the right-sized precision.
     */
    private static int getRightSizedPrecision(final BigDecimal decimal,
            final int fractionDigitsMinimum) {

        if (decimal.compareTo(BigDecimal.ZERO) != 0) {

            final BigDecimal min = BigDecimal.ONE
                    .divide(BigDecimal.TEN.pow(fractionDigitsMinimum));

            final BigDecimal abs = decimal.abs();
            final BigDecimal decimalsOnly =
                    abs.subtract(abs.setScale(0, RoundingMode.DOWN));

            if (decimalsOnly.compareTo(BigDecimal.ZERO) != 0
                    && decimalsOnly.compareTo(min) < 0) {
                return decimal.scale();
            }
        }
        return fractionDigitsMinimum;
    }

    /**
     * Localizes a {@link BigDecimal} with a minimal precision.
     *
     * @param decimal
     *            The {@link BigDecimal} to localize.
     * @param fractionDigitsMinimum
     *            The minimum number of fraction digits in the result.
     * @param locale
     *            The {@link Locale}.
     * @param currencySymbol
     *            The currency symbol to prepend to the formatted decimal.
     * @param groupingUsed
     *            {@code true} when integer part is grouped.
     * @return The localized decimal as string.
     * @throws ParseException
     *             When parsing fails.
     */
    public static String localizeMinimalPrecision(final BigDecimal decimal,
            final int fractionDigitsMinimum, final Locale locale,
            final String currencySymbol, final boolean groupingUsed)
            throws ParseException {

        return localize(decimal,
                getRightSizedPrecision(decimal, fractionDigitsMinimum), locale,
                currencySymbol, groupingUsed);
    }

    /**
     * Localizes a {@link BigDecimal} with a minimal precision.
     *
     * @param decimal
     *            The {@link BigDecimal} to localize.
     * @param fractionDigitsMinimum
     *            The minimum number of fraction digits in the result.
     * @param locale
     *            The {@link Locale}.
     * @param groupingUsed
     *            {@code true} when integer part is grouped.
     * @return The localized decimal as string.
     * @throws ParseException
     *             When parsing fails.
     */
    public static String localizeMinimalPrecision(final BigDecimal decimal,
            final int fractionDigitsMinimum, final Locale locale,
            final boolean groupingUsed) throws ParseException {
        return localize(decimal,
                getRightSizedPrecision(decimal, fractionDigitsMinimum), locale,
                groupingUsed);
    }

    /**
     * Localizes a {@link BigDecimal}.
     *
     * @param decimal
     *            The {@link BigDecimal} to localize.
     * @param fractionDigits
     *            The number of fraction digits in the result.
     * @param locale
     *            The {@link Locale}.
     * @param currencySymbol
     *            The currency symbol to prepend to the formatted decimal.
     * @param groupingUsed
     *            {@code true} when integer part is grouped.
     * @return The localized string.
     * @throws ParseException
     *             When parsing fails.
     */
    public static String localize(final BigDecimal decimal,
            final int fractionDigits, final Locale locale,
            final String currencySymbol, final boolean groupingUsed)
            throws ParseException {
        final StringBuilder txt = new StringBuilder();

        if (StringUtils.isNotBlank(currencySymbol)) {
            txt.append(currencySymbol).append(" ");
        }
        return txt
                .append(localize(decimal, fractionDigits, locale, groupingUsed))
                .toString();
    }

    /**
     *
     * @param locale
     * @param currency
     *            {@code true} if the currency symbol of the locale must be
     *            used.
     * @return
     */
    private static DecimalFormat getDecimalFormat(final Locale locale,
            boolean currency) {
        final NumberFormat numberFormat;
        if (currency) {
            numberFormat = NumberFormat.getCurrencyInstance(locale);
        } else {
            numberFormat = NumberFormat.getInstance(locale);
        }
        return (DecimalFormat) numberFormat;
    }

    /**
     * Translates a localized decimal to a {@link BigDecimal} plain string (as
     * described in {@link BigDecimal#toPlainString()}).
     *
     * @param localizedDecimal
     *            The localized decimal.
     * @param locale
     *            The {@link Locale} of the localized decimal.
     * @param groupingUsed
     *            {@code true} when grouping is used (like {@code 1,000.00} in
     *            {@code en-US}).
     * @return The {@link BigDecimal} plain string.
     * @throws ParseException
     *             When format of localized decimal is invalid.
     */
    public static String toPlainString(final String localizedDecimal,
            final Locale locale, final boolean groupingUsed)
            throws ParseException {
        return BigDecimalUtil.toPlainString(BigDecimalUtil
                .parse(localizedDecimal, locale, false, groupingUsed));
    }

    /**
     * Gets the plain string representation of a {@link BigDecimal}. This is a
     * wrapper facade of {@link BigDecimal#toPlainString()}.
     *
     * @param value
     *            The {@link BigDecimal}.
     * @return The plain string.
     */
    public static String toPlainString(final BigDecimal value) {
        return value.toPlainString();
    }

    /**
     * Checks if the plain string representation of a {@link BigDecimal} is
     * valid. See {@link BigDecimal#toPlainString()}.
     *
     * @param plainString
     *            The plain string representation of a {@link BigDecimal}.
     * @return {@code true} when valid.
     */
    public static boolean isValid(final String plainString) {
        boolean valid = true;
        try {
            new BigDecimal(plainString);
        } catch (NumberFormatException e) {
            valid = false;
        }
        return valid;
    }

    /**
     * Checks if the locale string representation of a {@link BigDecimal} is
     * valid.
     *
     * @param localeString
     *            The locale string representation of a {@link BigDecimal}.
     * @param locale
     *            The {@link Locale}.
     * @param groupingUsed
     *            {@code true} when grouping is used (like {@code 1,000.00} in
     *            {@code en-US}).
     * @return {@code true} when valid.
     */
    public static boolean isValid(final String localeString,
            final Locale locale, boolean groupingUsed) {
        boolean valid = true;
        try {
            parse(localeString, locale, false, groupingUsed);
        } catch (ParseException e) {
            valid = false;
        }
        return valid;
    }

    /**
     * Gets the {@link BigDecimal} from a plain string representation (as
     * described in {@link BigDecimal#toPlainString()}).
     *
     * @param plainString
     *            The plain string representation of a {@link BigDecimal}.
     * @return The {@link BigDecimal}.
     */
    public static BigDecimal valueOf(final String plainString) {
        String value = plainString;
        if (StringUtils.isBlank(value)) {
            value = "0.0";
        }
        return new BigDecimal(value);
    }

}
