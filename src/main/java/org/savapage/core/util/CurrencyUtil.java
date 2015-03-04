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

import java.util.Currency;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp;
import org.savapage.core.config.IConfigProp.Key;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Datraverse B.V.
 *
 */
public class CurrencyUtil {

    private final static String UNKNOWN_CURRENCY_SYMBOL = "?";

    private final static Logger LOGGER = LoggerFactory
            .getLogger(CurrencyUtil.class);

    /**
     * Gets the Currency symbol, i.e:
     * <ul>
     * <li>An EMPTY string when currency may NOT be shown</li>
     * <li>A custom symbol as set in
     * {@link IConfigProp.Key#USER_FIN_CURRENCY_SYMBOL_CUSTOM}</li>
     * <li>The currency belonging to {@link Locale#getCountry()}</li>
     * <li>{@link #UNKNOWN_CURRENCY_SYMBOL} when currency symbol is not
     * available.</li>
     * </ul>
     *
     * @return The currency symbol.
     */
    public static String getCurrencySymbol(final Locale locale) {

        String currencySymbol = "";

        final ConfigManager cm = ConfigManager.instance();

        if (cm.isConfigValue(Key.USER_FIN_CURRENTCY_SYMBOL_SHOWS)) {

            currencySymbol =
                    cm.getConfigValue(Key.USER_FIN_CURRENCY_SYMBOL_CUSTOM);

            if (StringUtils.isBlank(currencySymbol)) {

                Currency currency = null;

                try {

                    currency = Currency.getInstance(locale);

                } catch (IllegalArgumentException e1) {
                    /*
                     * Locale is NOT a supported ISO 3166 country. Try the
                     * default locale instead.
                     */
                    final Locale standardLocale = Locale.getDefault();

                    LOGGER.debug("Country [" + locale.getCountry()
                            + "] of Locale [" + locale.getDisplayName()
                            + "] is NOT a supported ISO 3166 country."
                            + " Using Country [" + standardLocale.getCountry()
                            + "] from Default Locale ["
                            + standardLocale.getDisplayName() + "]");

                    try {

                        currency = Currency.getInstance(standardLocale);

                    } catch (IllegalArgumentException e2) {

                        LOGGER.warn("Country [" + standardLocale.getCountry()
                                + "] of Default Locale ["
                                + standardLocale.getDisplayName()
                                + "] is NOT a supported ISO 3166 country."
                                + " Using currency symbol ["
                                + UNKNOWN_CURRENCY_SYMBOL + "]");
                    }
                }

                if (currency == null) {
                    /*
                     * This territory does not have a currency (like Antartica)
                     * or an IllegalArgumentException occurred.
                     */
                    currencySymbol = UNKNOWN_CURRENCY_SYMBOL;
                } else {
                    currencySymbol = currency.getSymbol();
                }

            }

        }
        return currencySymbol;
    }

}
