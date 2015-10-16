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
package org.savapage.core.dto;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.savapage.core.json.rpc.impl.ResultAttribute;
import org.savapage.core.snmp.SnmpMibDict;
import org.savapage.core.snmp.SnmpPrinterErrorStateEnum;
import org.savapage.core.snmp.SnmpPrinterStatusEnum;
import org.savapage.core.snmp.SnmpPrinterVendorEnum;
import org.savapage.core.snmp.SnmpPrtMarkerColorantEntry;
import org.savapage.core.snmp.SnmpPrtMarkerCounterUnitEnum;
import org.savapage.core.snmp.SnmpPrtMarkerSuppliesEntry;
import org.savapage.core.snmp.SnmpPrtMarkerSuppliesSupplyUnitEnum;
import org.savapage.core.snmp.SnmpPrtMarkerSuppliesTypeEnum;
import org.savapage.core.util.LocaleHelper;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 *
 * @author Datraverse B.V.
 *
 */
public final class PrinterSnmpDto extends AbstractDto {

    private SnmpPrinterVendorEnum vendor;
    private String systemDescription;
    private String serialNumber;
    private SnmpPrinterStatusEnum printerStatus;
    private Integer markerLifeCount;
    private SnmpPrtMarkerCounterUnitEnum markerCounterUnit;
    private Map<Integer, SnmpPrtMarkerColorantEntry> markerColorants;
    private Map<SnmpPrtMarkerSuppliesTypeEnum, List<SnmpPrtMarkerSuppliesEntry>> suppliesEntries;
    private Date dateStarted;
    private Set<SnmpPrinterErrorStateEnum> errorStates;

    public SnmpPrinterVendorEnum getVendor() {
        return vendor;
    }

    public void setVendor(SnmpPrinterVendorEnum vendor) {
        this.vendor = vendor;
    }

    public String getSystemDescription() {
        return systemDescription;
    }

    public void setSystemDescription(String systemDescription) {
        this.systemDescription = systemDescription;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public SnmpPrinterStatusEnum getPrinterStatus() {
        return printerStatus;
    }

    public void setPrinterStatus(SnmpPrinterStatusEnum printerStatus) {
        this.printerStatus = printerStatus;
    }

    public Integer getMarkerLifeCount() {
        return markerLifeCount;
    }

    public void setMarkerLifeCount(Integer markerLifeCount) {
        this.markerLifeCount = markerLifeCount;
    }

    public SnmpPrtMarkerCounterUnitEnum getMarkerCounterUnit() {
        return markerCounterUnit;
    }

    public void setMarkerCounterUnit(
            SnmpPrtMarkerCounterUnitEnum markerCounterUnit) {
        this.markerCounterUnit = markerCounterUnit;
    }

    public Map<Integer, SnmpPrtMarkerColorantEntry> getMarkerColorants() {
        return markerColorants;
    }

    public void setMarkerColorants(
            Map<Integer, SnmpPrtMarkerColorantEntry> markerColorants) {
        this.markerColorants = markerColorants;
    }

    public Map<SnmpPrtMarkerSuppliesTypeEnum, List<SnmpPrtMarkerSuppliesEntry>>
            getSuppliesEntries() {
        return suppliesEntries;
    }

    public
            void
            setSuppliesEntries(
                    Map<SnmpPrtMarkerSuppliesTypeEnum, List<SnmpPrtMarkerSuppliesEntry>> suppliesEntries) {
        this.suppliesEntries = suppliesEntries;
    }

    public Date getDateStarted() {
        return dateStarted;
    }

    public void setDateStarted(Date dateStarted) {
        this.dateStarted = dateStarted;
    }

    public Set<SnmpPrinterErrorStateEnum> getErrorStates() {
        return errorStates;
    }

    public void setErrorStates(Set<SnmpPrinterErrorStateEnum> errorStates) {
        this.errorStates = errorStates;
    }

    /**
     *
     * @param locale
     * @return
     */
    public List<ResultAttribute> asAttributes() {

        final List<ResultAttribute> list = new ArrayList<>();

        final LocaleHelper helper = new LocaleHelper(Locale.ENGLISH);
        StringBuilder builder;

        String attrValueWlk;

        //
        attrValueWlk = null;
        if (this.getVendor() != null) {
            attrValueWlk = this.getVendor().toString();
        }

        list.add(new ResultAttribute("Vendor", attrValueWlk));

        //
        list.add(new ResultAttribute("Description", this.getSystemDescription()));
        list.add(new ResultAttribute("Serial#", this.getSerialNumber()));

        //
        attrValueWlk = null;
        if (this.getPrinterStatus() != null) {
            attrValueWlk = this.getPrinterStatus().getUiText();
        }

        list.add(new ResultAttribute("Status", attrValueWlk));

        if (this.getErrorStates() != null && !this.getErrorStates().isEmpty()) {

            builder = new StringBuilder(128);

            for (final SnmpPrinterErrorStateEnum state : this.getErrorStates()) {
                builder.append(String.format("%s ", state.toString()));
            }
            list.add(new ResultAttribute("Error state", builder.toString()));
        }

        //
        attrValueWlk = null;
        if (this.getDateStarted() != null) {
            attrValueWlk = helper.getLongMediumDateTime(this.getDateStarted());
        }
        list.add(new ResultAttribute("Uptime since", attrValueWlk));

        //
        attrValueWlk = null;
        if (this.getMarkerLifeCount() != null) {

            builder = new StringBuilder(128);
            builder.append(this.getMarkerLifeCount());
            if (this.getMarkerCounterUnit() != null) {
                builder.append(" ").append(
                        this.getMarkerCounterUnit().getUiText());
            }
            attrValueWlk = builder.toString();
        }
        list.add(new ResultAttribute("Life count", attrValueWlk));

        //
        if (this.getSuppliesEntries() != null) {
            for (final Entry<SnmpPrtMarkerSuppliesTypeEnum, List<SnmpPrtMarkerSuppliesEntry>> entry : this
                    .getSuppliesEntries().entrySet()) {

                for (final SnmpPrtMarkerSuppliesEntry supplies : entry
                        .getValue()) {

                    builder = new StringBuilder(128);

                    //
                    final int suppliesLevel = supplies.getLevel();
                    final String suppliesLevelTxt;

                    if (suppliesLevel == SnmpMibDict.PRT_MARKER_SUPPLIES_LEVEL_UNRESTRICTED) {
                        suppliesLevelTxt = "UNRESTRICTED";
                    } else if (suppliesLevel == SnmpMibDict.PRT_MARKER_SUPPLIES_LEVEL_UNKNOWN) {
                        suppliesLevelTxt = "UNKNOWN";
                    } else if (suppliesLevel == SnmpMibDict.PRT_MARKER_SUPPLIES_LEVEL_REMAINING) {
                        suppliesLevelTxt = "REMAINING";
                    } else {
                        suppliesLevelTxt = String.valueOf(suppliesLevel);
                    }

                    builder.append(String.format("%s", suppliesLevelTxt));

                    //
                    final boolean showMaxCapacity;
                    final SnmpPrtMarkerSuppliesSupplyUnitEnum supplyUnit =
                            supplies.getSupplyUnit();

                    if (supplyUnit != null) {
                        builder.append(String.format(" %s",
                                supplyUnit.toString()));
                        showMaxCapacity =
                                supplyUnit != SnmpPrtMarkerSuppliesSupplyUnitEnum.PERCENT;
                    } else {
                        showMaxCapacity = true;
                    }

                    if (showMaxCapacity) {

                        final int maxCapacity = supplies.getMaxCapacity();
                        final String maxCapacityTxt;

                        if (maxCapacity == SnmpMibDict.PRT_MARKER_SUPPLIES_MAX_CAPACITY_UNRESTRICTED) {
                            maxCapacityTxt = "UNRESTRICTED";
                        } else if (maxCapacity == SnmpMibDict.PRT_MARKER_SUPPLIES_MAX_CAPACITY_UNKNOWN) {
                            maxCapacityTxt = "UNKNOWN";
                        } else {
                            maxCapacityTxt = String.valueOf(maxCapacity);
                        }

                        builder.append(String.format(" of %s MAX",
                                maxCapacityTxt));
                    }

                    //
                    final SnmpPrtMarkerColorantEntry colorant =
                            supplies.getColorantEntry();

                    if (colorant != null) {
                        builder.append(String.format(" %s", colorant.getValue()
                                .toString()));
                    }

                    // builder.append(String.format(" [IS %s]", supplies
                    // .getSuppliesClass().toString()));

                    list.add(new ResultAttribute(supplies.getDescription(),
                            builder.toString()));
                }
            }
        }

        //
        return list;
    }

    /**
     *
     * @return The formatted text.
     * @throws IOException
     *             When an IO error occurs.
     */
    @JsonIgnore
    public String asFormattedText() throws IOException {

        final StringWriter writer = new StringWriter(1024);
        final String lineSep = System.getProperty("line.separator");

        for (ResultAttribute attr : asAttributes()) {
            writer.append(
                    String.format("%-25s: %s", attr.getKey(), attr.getValue()))
                    .append(lineSep);
        }

        return writer.toString();
    }

}
