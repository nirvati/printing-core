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
 *
 * <a href="http://tools.ietf.org/html/rfc1759.html">RFC1759</a>
 *
 * @author Rijk Ravestein
 *
 */
public enum SnmpPrtMarkerColorantRoleEnum {

    /**
     * .
     */
    OTHER(1, "other"),
    /**
     * .
     */
    PROCESS(3, "process"),
    /**
     * .
     */
    SPOT(4, "spot");

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
        private final Map<Integer, SnmpPrtMarkerColorantRoleEnum> enumLookup =
                new HashMap<Integer, SnmpPrtMarkerColorantRoleEnum>();

        /**
         * .
         */
        public Lookup() {
            for (SnmpPrtMarkerColorantRoleEnum value : SnmpPrtMarkerColorantRoleEnum
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
        public SnmpPrtMarkerColorantRoleEnum get(final Integer key) {
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
    private SnmpPrtMarkerColorantRoleEnum(final int value,
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
     * Gets the {@link SnmpPrtMarkerColorantRoleEnum} from the status number.
     *
     * @param number
     *            The SNMP integer
     * @return The {@link SnmpPrtMarkerColorantRoleEnum} or {@code null} when
     *         not found.
     */
    public static SnmpPrtMarkerColorantRoleEnum asEnum(final Integer number) {
        return LookupHolder.INSTANCE.get(number);
    }

}
