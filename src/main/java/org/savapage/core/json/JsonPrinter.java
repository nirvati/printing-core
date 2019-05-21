/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2019 Datraverse B.V.
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
package org.savapage.core.json;

import java.net.URI;

import org.savapage.core.dao.enums.ProxyPrintAuthModeEnum;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 *
 * @author Rijk Ravestein
 *
 */
public class JsonPrinter extends JsonAbstractBase {

    /**
     * The primary key of its database instance.
     */
    private Long dbKey;

    /**
     * User-friendly printer name.
     */
    private String alias;

    /**
     * Unique CUPS printer name.
     */
    private String name;

    /**
     *
     */
    private String location;

    /**
     * Indicates whether this printer is dedicated to a Terminal device.
     */
    private Boolean terminalSecured = Boolean.FALSE;

    /**
     * Indicates whether Print Authentication via a Network Card Reader is
     * required to print a job.
     */
    private Boolean readerSecured = Boolean.FALSE;

    /**
     * Indicates whether this printer is used to produce a job ticket.
     */
    private Boolean jobTicket = Boolean.FALSE;

    /**
     * Indicates whether Job Ticket Tags are enabled.
     */
    private Boolean jobTicketLabelsEnabled = Boolean.FALSE;

    /**
     * The unique name of the Card Reader (when reader secured).
     */
    private String readerName;

    private ProxyPrintAuthModeEnum authMode;

    @JsonIgnore
    private URI printerUri;

    /**
     *
     * @return
     */
    public Long getDbKey() {
        return dbKey;
    }

    public void setDbKey(Long dbKey) {
        this.dbKey = dbKey;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public Boolean getTerminalSecured() {
        return terminalSecured;
    }

    public void setTerminalSecured(Boolean terminalSecured) {
        this.terminalSecured = terminalSecured;
    }

    public Boolean getReaderSecured() {
        return readerSecured;
    }

    public void setReaderSecured(Boolean readerSecured) {
        this.readerSecured = readerSecured;
    }

    public Boolean getJobTicket() {
        return jobTicket;
    }

    public void setJobTicket(Boolean jobTicket) {
        this.jobTicket = jobTicket;
    }

    public Boolean getJobTicketLabelsEnabled() {
        return jobTicketLabelsEnabled;
    }

    public void setJobTicketLabelsEnabled(Boolean enabled) {
        this.jobTicketLabelsEnabled = enabled;
    }

    public String getReaderName() {
        return readerName;
    }

    public void setReaderName(String readerName) {
        this.readerName = readerName;
    }

    public ProxyPrintAuthModeEnum getAuthMode() {
        return authMode;
    }

    public void setAuthMode(ProxyPrintAuthModeEnum authMode) {
        this.authMode = authMode;
    }

    @JsonIgnore
    public URI getPrinterUri() {
        return printerUri;
    }

    @JsonIgnore
    public void setPrinterUri(final URI printerUri) {
        this.printerUri = printerUri;
    }

    public void copy(JsonPrinter copy) {
        copy.dbKey = this.dbKey;
        copy.name = this.name;
        copy.location = this.location;
        copy.alias = this.alias;
        copy.terminalSecured = this.terminalSecured;
        copy.readerSecured = this.readerSecured;
        copy.readerName = this.readerName;
        copy.authMode = this.authMode;
        copy.printerUri = this.printerUri;
        copy.jobTicket = this.jobTicket;
        copy.jobTicketLabelsEnabled = this.jobTicketLabelsEnabled;
    }

}
