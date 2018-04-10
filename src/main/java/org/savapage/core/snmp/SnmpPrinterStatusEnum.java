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
 * See <a href="http://oid-info.com/get/1.3.6.1.2.1.25.3.5.1.1">OID</a>.
 *
 * @author Rijk Ravestein
 *
 */
public enum SnmpPrinterStatusEnum {

    /**
     * .
     */
    OTHER(1, "Other"),
    /**
     * .
     */
    UNKNOWN(2, "Unknown"),
    /**
     * .
     */
    IDLE(3, "Idle"),
    /**
     * .
     */
    PRINTING(4, "Printing"),
    /**
     * .
     */
    WARMUP(5, "Warmup");

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
        private final Map<Integer, SnmpPrinterStatusEnum> enumLookup =
                new HashMap<Integer, SnmpPrinterStatusEnum>();

        /**
        *
        */
        public Lookup() {
            for (SnmpPrinterStatusEnum value : SnmpPrinterStatusEnum.values()) {
                enumLookup.put(value.value, value);
            }
        }

        /**
         *
         * @param key
         *            The key (number).
         * @return The enum.
         */
        public SnmpPrinterStatusEnum get(final Integer key) {
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
    private SnmpPrinterStatusEnum(final int value, final String uiText) {
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
     * Gets the {@link SnmpPrinterStatusEnum} from the status number.
     *
     * @param number
     *            The SNMP status number.
     * @return The {@link SnmpPrinterStatusEnum} or {@code null} when not found.
     */
    public static SnmpPrinterStatusEnum asEnum(final Integer number) {
        return LookupHolder.INSTANCE.get(number);
    }

}
