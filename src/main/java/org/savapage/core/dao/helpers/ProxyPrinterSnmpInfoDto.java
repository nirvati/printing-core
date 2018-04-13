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
package org.savapage.core.dao.helpers;

import java.util.Map;
import java.util.Set;

import org.savapage.core.dao.enums.ProxyPrinterSuppliesEnum;
import org.savapage.core.dto.AbstractDto;
import org.savapage.core.snmp.SnmpPrinterErrorStateEnum;
import org.savapage.core.snmp.SnmpPrtMarkerColorantValueEnum;

/**
 *
 * @author Rijk Ravestein
 *
 */
public class ProxyPrinterSnmpInfoDto extends AbstractDto {

    /** */
    private Integer vendor;

    /** */
    private String model;

    /** */
    private String serial;

    /** */
    private ProxyPrinterSuppliesEnum supplies;

    /** */
    private Map<SnmpPrtMarkerColorantValueEnum, Integer> markers;

    /** */
    private Set<SnmpPrinterErrorStateEnum> errorStates;

    /**
     *
     * @return
     */
    public ProxyPrinterSuppliesEnum getSupplies() {
        return supplies;
    }

    public void setSupplies(ProxyPrinterSuppliesEnum supplies) {
        this.supplies = supplies;
    }

    public Map<SnmpPrtMarkerColorantValueEnum, Integer> getMarkers() {
        return markers;
    }

    public void
            setMarkers(Map<SnmpPrtMarkerColorantValueEnum, Integer> colorants) {
        this.markers = colorants;
    }

    public Integer getVendor() {
        return vendor;
    }

    public void setVendor(Integer vendor) {
        this.vendor = vendor;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getSerial() {
        return serial;
    }

    public void setSerial(String serial) {
        this.serial = serial;
    }

    public Set<SnmpPrinterErrorStateEnum> getErrorStates() {
        return errorStates;
    }

    public void setErrorStates(Set<SnmpPrinterErrorStateEnum> errorStates) {
        this.errorStates = errorStates;
    }

}
