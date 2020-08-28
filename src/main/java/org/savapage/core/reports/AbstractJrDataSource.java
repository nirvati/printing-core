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
package org.savapage.core.reports;

import java.util.Locale;

import org.savapage.core.config.ConfigManager;
import org.savapage.core.util.Messages;

/**
 *
 * @author Rijk Ravestein
 *
 */
public abstract class AbstractJrDataSource {

    private final Locale locale;

    private int userBalanceDecimals;

    /**
     *
     * @param locale
     */
    protected AbstractJrDataSource(Locale locale) {
        this.locale = locale;
        this.setUserBalanceDecimals(ConfigManager.getUserBalanceDecimals());
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
    protected final String localized(final String key,
            final String... objects) {
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
