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
package org.savapage.core.snmp;

import java.util.HashMap;
import java.util.Map;

/**
 * The type of supply.
 *
 * See n<a href="http://tools.ietf.org/html/rfc1759.html">RFC1759</a> and
 * <a href="http://www.iana.org/assignments/ianaprinter-mib/ianaprinter-mib"
 * >IANA</a>
 *
 * @author Rijk Ravestein
 *
 */
public enum SnmpPrtMarkerSuppliesTypeEnum {

    /**
     * Zero value NOT specified in RFC1759.
     */
    UNDEFINED(0, "undefined"),
    /**
     * .
     */
    OTHER(1, "other"),
    /**
     * .
     */
    UNKNOWN(2, "unknown"),
    /**
     * .
     */
    TONER(3, "toner"),
    /**
     * .
     */
    WASTETONER(4, "wasteToner"),
    /**
     * .
     */
    INK(5, "ink"),
    /**
     * .
     */
    INKCARTRIDGE(6, "inkCartridge"),
    /**
     * .
     */
    INKRIBBON(7, "inkRibbon"),
    /**
     * .
     */
    WASTEINK(8, "wasteInk"),
    /**
     * photo conductor
     */
    OPC(9, "opc"),
    /**
     * .
     */
    DEVELOPER(10, "developer"),
    /**
     * .
     */
    FUSEROIL(11, "fuserOil"),
    /**
     * .
     */
    SOLIDWAX(12, "solidWax"),
    /**
     * .
     */
    RIBBONWAX(13, "ribbonWax"),
    /**
     * .
     */
    WASTEWAX(14, "wasteWax"),

    /**
     * Not in RFC 1759
     */
    FUSER(15, "fuser"),

    /**
     * Not in RFC 1759
     */
    CORONAWIRE(16, "coronaWire"),
    /**
     * Not in RFC 1759
     */
    FUSEROILWICK(17, "fuserOilWick"),
    /**
     * Not in RFC 1759
     */
    CLEANERUNIT(18, "cleanerUnit"),
    /**
     * Not in RFC 1759
     */
    FUSERCLEANINGPAD(19, "fuserCleaningPad"),
    /**
     * Not in RFC 1759
     */
    TRANSFERUNIT(20, "transferUnit"),
    /**
     * Not in RFC 1759
     */
    TONERCARTRIDGE(21, "tonerCartridge"),
    /**
     * Not in RFC 1759
     */
    FUSEROILER(22, "fuserOiler"),

    // End of values for Printer MIB
    // Values for Finisher MIB

    /**
     * Not in RFC 1759
     */
    WATER(23, "water"),

    /**
     * Not in RFC 1759
     */
    WASTEWATER(24, "wasteWater"),
    /**
     * Not in RFC 1759
     */
    GLUEWATERADDITIVE(25, "glueWaterAdditive"),
    /**
     * Not in RFC 1759
     */
    WASTEPAPER(26, "wastePaper"),
    /**
     * Not in RFC 1759
     */
    BINDINGSUPPLY(27, "bindingSupply"),
    /**
     * Not in RFC 1759
     */
    BANDINGSUPPLY(28, "bandingSupply"),
    /**
     * Not in RFC 1759
     */
    STITCHINGWIRE(29, "stitchingWire"),
    /**
     * Not in RFC 1759
     */
    SHRINKWRAP(30, "shrinkWrap"),
    /**
     * Not in RFC 1759
     */
    PAPERWRAP(31, "paperWrap"),
    /**
     * Not in RFC 1759
     */
    STAPLES(32, "staples"),
    /**
     * Not in RFC 1759
     */
    INSERTS(33, "inserts"),
    /**
     * Not in RFC 1759
     */
    COVERS(34, "covers"),
    /**
     * Not in RFC 3805
     */
    MATTETONER(35, "matteToner"),
    /**
     * Not in RFC 3805
     */
    MATTEINK(36, "matteInk");

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
        private final Map<Integer, SnmpPrtMarkerSuppliesTypeEnum> enumLookup =
                new HashMap<Integer, SnmpPrtMarkerSuppliesTypeEnum>();

        /**
         * .
         */
        public Lookup() {
            for (SnmpPrtMarkerSuppliesTypeEnum value : SnmpPrtMarkerSuppliesTypeEnum
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
        public SnmpPrtMarkerSuppliesTypeEnum get(final Integer key) {
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
    private SnmpPrtMarkerSuppliesTypeEnum(final int value,
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
     * Gets the {@link SnmpPrtMarkerSuppliesTypeEnum} from the status number.
     *
     * @param number
     *            The SNMP integer
     * @return The {@link SnmpPrtMarkerSuppliesTypeEnum} or {@code null} when
     *         not found.
     */
    public static SnmpPrtMarkerSuppliesTypeEnum asEnum(final Integer number) {
        return LookupHolder.INSTANCE.get(number);
    }

}
