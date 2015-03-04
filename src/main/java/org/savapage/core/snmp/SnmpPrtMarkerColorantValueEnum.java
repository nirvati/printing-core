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
package org.savapage.core.snmp;

/**
 * The name of the color of this The name of the color of this colorant using
 * standardized string names from ISO 10175 (DPA) and ISO 10180 (SPDL).
 *
 * <a href="http://tools.ietf.org/html/rfc1759.html">RFC1759</a>
 *
 * @author Datraverse B.V.
 *
 */
public enum SnmpPrtMarkerColorantValueEnum {

    /**
     * .
     */
    OTHER("other"),

    /**
     * .
     */
    UNKNOWN("unknown"),

    /**
     * .
     */
    WHITE("white"),

    /**
     * .
     */
    RED("red"),

    /**
     * .
     */
    GREEN("green"),

    /**
     * .
     */
    BLUE("blue"),

    /**
     * .
     */
    CYAN("cyan"),

    /**
     * .
     */
    MAGENTA("magenta"),

    /**
     * .
     */
    YELLOW("yellow"),

    /**
     * .
     */
    BLACK("black");

    /**
     * .
     */
    private final String uiText;

    /**
     *
     * @param uiText
     */
    private SnmpPrtMarkerColorantValueEnum(final String uiText) {
        this.uiText = uiText;
    }

    /**
     *
     * @return The UI text.
     */
    public String getUiText() {
        return this.uiText;
    }

}
