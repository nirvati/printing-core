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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.savapage.core.dto.AbstractDto;
import org.snmp4j.smi.OID;

/**
 *
 * @author Datraverse B.V.
 *
 */
public final class SnmpPrtMarkerSuppliesEntry extends AbstractDto {

    private String description;
    private int maxCapacity;
    private int level;
    private SnmpPrtMarkerSuppliesClassEnum suppliesClass;
    private SnmpPrtMarkerSuppliesTypeEnum suppliesType;
    private SnmpPrtMarkerSuppliesSupplyUnitEnum supplyUnit;
    private SnmpPrtMarkerColorantEntry colorantEntry;

    /**
     */
    private SnmpPrtMarkerSuppliesEntry() {
    }

    public String getDescription() {
        return description;
    }

    public int getMaxCapacity() {
        return maxCapacity;
    }

    public int getLevel() {
        return level;
    }

    public SnmpPrtMarkerSuppliesClassEnum getSuppliesClass() {
        return suppliesClass;
    }

    public SnmpPrtMarkerSuppliesTypeEnum getSuppliesType() {
        return suppliesType;
    }

    public SnmpPrtMarkerColorantEntry getColorantEntry() {
        return colorantEntry;
    }

    public SnmpPrtMarkerSuppliesSupplyUnitEnum getSupplyUnit() {
        return supplyUnit;
    }

    /**
     *
     * @param client
     * @param colorants
     * @return
     */
    public static
            Map<SnmpPrtMarkerSuppliesTypeEnum, List<SnmpPrtMarkerSuppliesEntry>>
            retrieve(final SnmpClientSession client,
                    final Map<Integer, SnmpPrtMarkerColorantEntry> colorants) {

        final Map<SnmpPrtMarkerSuppliesTypeEnum, List<SnmpPrtMarkerSuppliesEntry>> map =
                new HashMap<>();

        for (final List<String> list : client.getTableAsStrings(new OID[] {
                SnmpMibDict.OID_PRT_MARKER_SUPPLIES_DESCRIPTION,
                SnmpMibDict.OID_PRT_MARKER_SUPPLIES_CLASS,
                SnmpMibDict.OID_PRT_MARKER_SUPPLIES_TYPE,
                SnmpMibDict.OID_PRT_MARKER_SUPPLIES_SUPPLY_UNIT,
                SnmpMibDict.OID_PRT_MARKER_SUPPLIES_COLORANT_INDEX,
                SnmpMibDict.OID_PRT_MARKER_SUPPLIES_MAX_CAPACITY,
                SnmpMibDict.OID_PRT_MARKER_SUPPLIES_LEVEL })) {

            final SnmpPrtMarkerSuppliesEntry entry =
                    new SnmpPrtMarkerSuppliesEntry();

            int iWlk = 0;
            entry.description = list.get(iWlk);

            iWlk++;
            entry.suppliesClass =
                    SnmpPrtMarkerSuppliesClassEnum.asEnum(Integer.valueOf(list
                            .get(iWlk)));

            iWlk++;
            entry.suppliesType =
                    SnmpPrtMarkerSuppliesTypeEnum.asEnum(Integer.valueOf(list
                            .get(iWlk)));

            iWlk++;
            entry.supplyUnit =
                    SnmpPrtMarkerSuppliesSupplyUnitEnum.asEnum(Integer
                            .valueOf(list.get(iWlk)));

            iWlk++;
            entry.colorantEntry =
                    colorants.get(Integer.valueOf(list.get(iWlk)).intValue());

            iWlk++;
            entry.maxCapacity = Integer.valueOf(list.get(iWlk)).intValue();

            iWlk++;
            entry.level = Integer.valueOf(list.get(iWlk)).intValue();

            //
            final List<SnmpPrtMarkerSuppliesEntry> suppliesEntries;

            if (map.containsKey(entry.suppliesType)) {
                suppliesEntries = map.get(entry.suppliesType);
            } else {
                suppliesEntries = new ArrayList<>();
                map.put(entry.suppliesType, suppliesEntries);
            }

            suppliesEntries.add(entry);

        }

        return map;
    }
}
