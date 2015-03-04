/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2014 Datraverse B.V.
 * Authors: Rijk Ravestein.
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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 * @author Datraverse B.V.
 *
 */
@JsonInclude(Include.NON_NULL)
public class ProxyPrinterDto extends AbstractDto {

    /**
     * primary key
     */
    @JsonProperty("id")
    Long id;

    /**
     * unique printer name
     */
    @JsonProperty("printerName")
    String printerName;

    @JsonProperty("displayName")
    String displayName;

    @JsonProperty("location")
    String location;

    /**
     * A comma separated list of printer group names.
     */
    @JsonProperty("printerGroups")
    String printerGroups;

    @JsonProperty("disabled")
    Boolean disabled;

    /**
     * Is (logically) deleted?
     */
    @JsonProperty("deleted")
    Boolean deleted;

    /**
     * Is present in CUPS?
     */
    @JsonProperty("present")
    Boolean present;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPrinterName() {
        return printerName;
    }

    public void setPrinterName(String printerName) {
        this.printerName = printerName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getPrinterGroups() {
        return printerGroups;
    }

    public void setPrinterGroups(String printerGroups) {
        this.printerGroups = printerGroups;
    }

    public Boolean getDisabled() {
        return disabled;
    }

    public void setDisabled(Boolean disabled) {
        this.disabled = disabled;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }

    public Boolean getPresent() {
        return present;
    }

    public void setPresent(Boolean present) {
        this.present = present;
    }

}
