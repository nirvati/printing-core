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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.core.snmp;

import java.util.Locale;

import org.savapage.core.util.LocaleHelper;

/**
 * The name of the color of this The name of the color of this colorant using
 * standardized string names from ISO 10175 (DPA) and ISO 10180 (SPDL).
 *
 * <a href="http://tools.ietf.org/html/rfc1759.html">RFC1759</a>
 *
 * @author Rijk Ravestein
 *
 */
public enum SnmpPrtMarkerColorantValueEnum {

    /**
     * .
     */
    OTHER("silver"),

    /**
     * .
     */
    UNKNOWN("silver"),

    /**
     * .
     */
    WHITE("white"),

    /**
     * .
     */
    RED("#FD1F08"),

    /**
     * .
     */
    GREEN("#00A885"),

    /**
     * .
     */
    BLUE("#2C82C9"),

    /**
     * .
     */
    CYAN("#00A0C6"),

    /**
     * .
     */
    MAGENTA("#DE0184"),

    /**
     * .
     */
    YELLOW("#FDDF05"),

    /**
     * .
     */
    BLACK("#0A0A0A");

    /**
     * .
     */
    private final String htmlColor;

    /**
     *
     * @param color
     *            The HTML color;
     */
    SnmpPrtMarkerColorantValueEnum(final String color) {
        this.htmlColor = color;
    }

    /**
     *
     * @return A "user friendly" HTML color string like "#123456".
     */
    public String getHtmlColor() {
        return htmlColor;
    }

    /**
     * @param locale
     *            The {@link Locale}.
     * @return The localized text.
     */
    public String uiText(final Locale locale) {
        return LocaleHelper.uiText(this, locale);
    }

}
