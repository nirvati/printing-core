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
package org.savapage.core.reports;

import java.util.Locale;

import org.savapage.core.config.ConfigManager;
import org.savapage.core.util.Messages;

/**
 *
 * @author Datraverse B.V.
 *
 */
public abstract class AbstractJrDataSource {

    private final Locale locale;

    private int userBalanceDecimals;
    //private String currencySymbol;

    /**
     *
     * @param locale
     */
    protected AbstractJrDataSource(Locale locale) {
        this.locale = locale;
        this.setUserBalanceDecimals(ConfigManager.getUserBalanceDecimals());
        //this.setCurrencySymbol(CurrencyUtil.getCurrencySymbol(this.locale));
    }

    /**
     * Localizes and format a string with placeholder arguments.
     *
     * @param key
     *            The key from the XML resource file
     * @param objects
     *            The values to fill the placeholders.
     * @return The localized string.
     */
    protected final String localized(final String key, final String... objects) {
        return Messages.getMessage(this.getClass(), this.locale, key, objects);
    }

    public Locale getLocale() {
        return locale;
    }

    public int getUserBalanceDecimals() {
        return userBalanceDecimals;
    }

    public void setUserBalanceDecimals(int userBalanceDecimals) {
        this.userBalanceDecimals = userBalanceDecimals;
    }

}
