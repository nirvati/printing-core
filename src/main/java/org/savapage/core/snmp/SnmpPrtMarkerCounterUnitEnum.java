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

import java.util.HashMap;
import java.util.Map;

/**
 * The unit that will be used by the printer when reporting counter values for
 * this marking sub-unit. The time units of measure are provided for a device
 * like a strip recorder that does not or cannot track the physical dimensions
 * of the media and does not use characters, lines or sheets.
 *
 * <a href="http://tools.ietf.org/html/rfc1759.html">RFC1759</a>
 *
 * @author Datraverse B.V.
 *
 */
public enum SnmpPrtMarkerCounterUnitEnum {

    /**
     * .
     */
    TEN_THOUSANDTHS_OF_INCHES(3, "inch^-4"),
    /**
     * .
     */
    MICROMETERS(4, "micrometers"),
    /**
     * .
     */
    CHARACTERS(5, "characters"),
    /**
     * .
     */
    LINES(6, "lines"),
    /**
     * .
     */
    IMPRESSIONS(7, "impressions"),
    /**
     * .
     */
    SHEETS(8, "sheets"),
    /**
     * .
     */
    DOTROW(9, "dotrow"),
    /**
     * .
     */
    HOURS(11, "hours"),
    /**
     * .
     */
    FEET(16, "feet"),
    /**
     * .
     */
    METERS(17, "meters");

    /**
     *
     */
    private final int value;

    /**
     *
     */
    private final String uiText;

    /**
   *
   */
    private static class Lookup {

        /**
       *
       */
        private final Map<Integer, SnmpPrtMarkerCounterUnitEnum> enumLookup =
                new HashMap<Integer, SnmpPrtMarkerCounterUnitEnum>();

        /**
       *
       */
        public Lookup() {
            for (SnmpPrtMarkerCounterUnitEnum value : SnmpPrtMarkerCounterUnitEnum
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
        public SnmpPrtMarkerCounterUnitEnum get(final Integer key) {
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
    private SnmpPrtMarkerCounterUnitEnum(final int value, final String uiText) {
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
     * Gets the {@link SnmpPrtMarkerCounterUnitEnum} from the status number.
     *
     * @param number
     *            The SNMP status number.
     * @return The {@link SnmpPrtMarkerCounterUnitEnum} or {@code null} when not
     *         found.
     */
    public static SnmpPrtMarkerCounterUnitEnum asEnum(final Integer number) {
        return LookupHolder.INSTANCE.get(number);
    }

}
