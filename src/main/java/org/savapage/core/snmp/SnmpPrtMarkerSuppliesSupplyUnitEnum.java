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
 * Unit of marker supply container/receptacle.
 *
 * <a href="http://tools.ietf.org/html/rfc1759.html">RFC1759</a>
 *
 * @author Datraverse B.V.
 *
 */
public enum SnmpPrtMarkerSuppliesSupplyUnitEnum {

    /**
     * Not in RFC 1759
     */
    OTHER(1, "other"),
    /**
     * Not in RFC 1759
     */
    UNKNOWN(2, "unknown"),
    /**
     * .
     */
    TEN_THOUSANDTHS_OF_INCHES(3, "inch^-4"),
    /**
     * .
     */
    MICROMETERS(4, "micrometers"),
    /**
     * Not in RFC 1759
     */
    IMPRESSIONS(7, "impressions"),
    /**
     * Not in RFC 1759
     */
    SHEETS(8, "sheets"),
    /**
     * Not in RFC 1759
     */
    HOURS(11, "hours"),
    /**
     * .
     */
    THOUSANDTHS_OF_OUNCES(12, "ounces^-3"),
    /**
     * .
     */
    TENTHS_OF_GRAMS(13, "grams^-1"),
    /**
     * .
     */
    HUNDRETHS_OF_FLUID_OUNCES(14, "fluid ounces^-3"),
    /**
     * .
     */
    TENTHS_OF_MILLILITERS(15, "mm^-1"),
    /**
     * Not in RFC 1759
     */
    FEET(16, "feet"),
    /**
     * Not in RFC 1759
     */
    METERS(17, "meters"),

    // ----------------------------------------
    // Values for Finisher MIB
    // ----------------------------------------

    /**
     * E.g. #staples. Not in RFC 1759
     */
    ITEMS(18, "items"),
    /**
     * Not in RFC 1759
     */
    PERCENT(19, "percent");

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
        private final Map<Integer, SnmpPrtMarkerSuppliesSupplyUnitEnum> enumLookup =
                new HashMap<Integer, SnmpPrtMarkerSuppliesSupplyUnitEnum>();

        /**
       *
       */
        public Lookup() {
            for (SnmpPrtMarkerSuppliesSupplyUnitEnum value : SnmpPrtMarkerSuppliesSupplyUnitEnum
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
        public SnmpPrtMarkerSuppliesSupplyUnitEnum get(final Integer key) {
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
    private SnmpPrtMarkerSuppliesSupplyUnitEnum(final int value,
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
     * Gets the {@link SnmpPrtMarkerSuppliesSupplyUnitEnum} from the status
     * number.
     *
     * @param number
     *            The SNMP status number.
     * @return The {@link SnmpPrtMarkerSuppliesSupplyUnitEnum} or {@code null}
     *         when not found.
     */
    public static SnmpPrtMarkerSuppliesSupplyUnitEnum asEnum(
            final Integer number) {
        return LookupHolder.INSTANCE.get(number);
    }

}
