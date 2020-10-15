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

import java.util.Currency;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class CurrencyUtil {

    /**
     * The Bitcoin currency code.
     */
    public static final String CURRENCY_CODE_BITCOIN = "BTC";

    /**
     * The Euro currency code.
     */
    public static final String CURRENCY_CODE_EUR = "EUR";

    /**
     * Hide constructor.
     */
    private CurrencyUtil() {
    }

    /**
     * Gets the currency symbol for the ISO currency code according to a
     * {@link Locale}, or for the non-ISO "BTC" bitcoin currency code.
     *
     * @param currencyCode
     *            The currency code.
     * @param locale
     *            The {@link Locale}.
     * @return The currency symbol (or code).
     */
    public static String getCurrencySymbol(final String currencyCode,
            final Locale locale) {

        final String currencyCodeWrk = StringUtils.defaultString(currencyCode);

        if (currencyCodeWrk.equals(CURRENCY_CODE_BITCOIN)) {
            return "Ƀ";
        }
        if (currencyCodeWrk.equals(CURRENCY_CODE_EUR)) {
            return "€";
        }

        try {
            return Currency.getInstance(currencyCodeWrk).getSymbol(locale);
        } catch (Exception e) {
            return currencyCodeWrk;
        }
    }

}
