/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2018 Datraverse B.V.
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
 * @author Rijk Ravestein
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
     * A " ,;:" separated list of printer group names.
     */
    @JsonProperty("printerGroups")
    String printerGroups;

    /**
     * A file with custom setting for PPD to IPP conversion and constraints.
     */
    @JsonProperty("ppdExtFile")
    String ppdExtFile;

    @JsonProperty("internal")
    Boolean internal;

    @JsonProperty("disabled")
    Boolean disabled;

    @JsonProperty("archiveDisabled")
    Boolean archiveDisabled;

    @JsonProperty("journalDisabled")
    Boolean journalDisabled;

    /**
     * Is (logically) deleted?
     */
    @JsonProperty("deleted")
    Boolean deleted;

    /**
     * Is Job Ticket Printer?
     */
    @JsonProperty("jobTicket")
    Boolean jobTicket;

    /**
     *
     */
    @JsonProperty("jobTicketGroup")
    String jobTicketGroup;

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

    public String getPpdExtFile() {
        return ppdExtFile;
    }

    public void setPpdExtFile(String ppdExtFile) {
        this.ppdExtFile = ppdExtFile;
    }

    public Boolean getInternal() {
        return internal;
    }

    public void setInternal(Boolean internal) {
        this.internal = internal;
    }

    public Boolean getDisabled() {
        return disabled;
    }

    public void setDisabled(Boolean disabled) {
        this.disabled = disabled;
    }

    public Boolean getArchiveDisabled() {
        return archiveDisabled;
    }

    public void setArchiveDisabled(Boolean archiveDisabled) {
        this.archiveDisabled = archiveDisabled;
    }

    public Boolean getJournalDisabled() {
        return journalDisabled;
    }

    public void setJournalDisabled(Boolean journalDisabled) {
        this.journalDisabled = journalDisabled;
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

    public Boolean getJobTicket() {
        return jobTicket;
    }

    public void setJobTicket(Boolean jobTicket) {
        this.jobTicket = jobTicket;
    }

    public String getJobTicketGroup() {
        return jobTicketGroup;
    }

    public void setJobTicketGroup(String jobTicketGroup) {
        this.jobTicketGroup = jobTicketGroup;
    }

}
