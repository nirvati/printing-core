/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2018 Datraverse B.V.
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.core.snmp;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * <a href="http://tools.ietf.org/html/rfc1759.html">RFC1759</a>.
 *
 * @author Rijk Ravestein
 *
 */
public enum SnmpPrtMarkerSuppliesClassEnum {

    /**
     * Zero value NOT specified in RFC1759.
     */
    UNDEFINED(0, "undefined"),

    /**
     * .
     */
    OTHER(1, "other"),

    /**
     * Supply that is consumed.
     */
    CONSUMED(3, "consumed"),

    /**
     * Receptacle that is filled.
     */
    FILLED(4, "filled");

    /**
     * .
     */
    private final int value;

    /**
     * .
     */
    private final String uiText;

    /**
     * .
     */
    private static class Lookup {

        /**
         * .
         */
        private final Map<Integer, SnmpPrtMarkerSuppliesClassEnum> enumLookup =
                new HashMap<Integer, SnmpPrtMarkerSuppliesClassEnum>();

        /**
         * .
         */
        public Lookup() {
            for (SnmpPrtMarkerSuppliesClassEnum value : SnmpPrtMarkerSuppliesClassEnum
                    .values()) {
                enumLookup.put(value.value, value);
            }
        }

        /**
         *
         * @param key
         *            The key (number).
         * @return The enum.
         */
        public SnmpPrtMarkerSuppliesClassEnum get(final Integer key) {
            return enumLookup.get(key);
        }
    }

    /**
     * Ensure one-time initialization on class loading.
     */
    private static class LookupHolder {
        /**
        *
        */
        public static final Lookup INSTANCE = new Lookup();
    }

    /**
     *
     * @param value
     * @param uiText
     */
    private SnmpPrtMarkerSuppliesClassEnum(final int value,
            final String uiText) {
        this.value = value;
        this.uiText = uiText;
    }

    /**
     *
     * @return The UI text.
     */
    public String getUiText() {
        return this.uiText;
    }

    /**
     * Gets the {@link SnmpPrtMarkerSuppliesClassEnum} from the status number.
     *
     * @param number
     *            The SNMP integer
     * @return The {@link SnmpPrtMarkerSuppliesClassEnum} or {@code null} when
     *         not found.
     */
    public static SnmpPrtMarkerSuppliesClassEnum asEnum(final Integer number) {
        return LookupHolder.INSTANCE.get(number);
    }

}
