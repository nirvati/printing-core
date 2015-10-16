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
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.snmp4j.smi.OID;

/**
 *
 * @author Datraverse B.V.
 *
 */
public final class SnmpPrtMarkerColorantEntry {

    final private int index;
    final private int markerIndex;
    final private SnmpPrtMarkerColorantRoleEnum role;
    final private int tonality;
    final private SnmpPrtMarkerColorantValueEnum value;

    /**
     *
     * @param index
     * @param role
     * @param tonality
     * @param value
     */
    private SnmpPrtMarkerColorantEntry(final int index, final int markerIndex,
            final SnmpPrtMarkerColorantRoleEnum role, final int tonality,
            final SnmpPrtMarkerColorantValueEnum value) {
        this.index = index;
        this.markerIndex = markerIndex;
        this.role = role;
        this.tonality = tonality;
        this.value = value;
    }

    /**
     *
     * @return The unique value used by the printer to identify this colorant.
     */
    public int getIndex() {
        return index;
    }

    /**
     *
     * @return The value of prtMarkerIndex corresponding to the marker sub-unit
     *         with which this colorant entry is associated.
     */
    public int getMarkerIndex() {
        return markerIndex;
    }

    public SnmpPrtMarkerColorantRoleEnum getRole() {
        return role;
    }

    public int getTonality() {
        return tonality;
    }

    public SnmpPrtMarkerColorantValueEnum getValue() {
        return value;
    }

    /**
     *
     * @param client
     * @return
     */
    public static Map<Integer, SnmpPrtMarkerColorantEntry> retrieve(
            final SnmpClientSession client) {

        final Map<Integer, SnmpPrtMarkerColorantEntry> colorantMap =
                new HashMap<>();

        final OID[] oids =
                { SnmpMibDict.OID_PRT_MARKER_COLORANT_MARKER_INDEX,
                        SnmpMibDict.OID_PRT_MARKER_COLORANT_ROLE,
                        SnmpMibDict.OID_PRT_MARKER_COLORANT_TONALITY,
                        SnmpMibDict.OID_PRT_MARKER_COLORANT_VALUE };
        int i = 0;

        for (final List<String> list : client.getTableAsStrings(oids)) {

            final Integer index = Integer.valueOf(++i);

            /*
             * INVARIANT: all OID values (except the last) MUST be a number.
             */
            boolean isValid = true;

            for (int j = 0; j < oids.length - 1; j++) {

                if (!NumberUtils.isDigits(list.get(j))) {
                    isValid = false;
                    break;
                }
            }

            if (!isValid) {
                break;

            }

            //
            final String colorantValueUpper = list.get(3).toUpperCase();
            final SnmpPrtMarkerColorantValueEnum colorantValue;

            if (EnumUtils.isValidEnum(SnmpPrtMarkerColorantValueEnum.class,
                    colorantValueUpper)) {
                colorantValue =
                        SnmpPrtMarkerColorantValueEnum
                                .valueOf(colorantValueUpper);
            } else {
                colorantValue = SnmpPrtMarkerColorantValueEnum.UNKNOWN;
            }

            final SnmpPrtMarkerColorantEntry entry =
                    new SnmpPrtMarkerColorantEntry(index.intValue(),
                    //
                            Integer.valueOf(list.get(0)),
                            //
                            SnmpPrtMarkerColorantRoleEnum.asEnum(Integer
                                    .valueOf(list.get(1))),
                            //
                            Integer.valueOf(list.get(2)).intValue(),
                            //
                            colorantValue);

            colorantMap.put(index, entry);
        }

        return colorantMap;
    }
}
