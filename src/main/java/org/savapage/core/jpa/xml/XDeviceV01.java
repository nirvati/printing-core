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
package org.savapage.core.jpa.xml;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.savapage.core.jpa.schema.DeviceV01;

/**
 * A physical device of a certain type running one or more SavaPage functions.
 *
 * @author Datraverse B.V.
 *
 */
@Entity
@Table(name = DeviceV01.TABLE_NAME)
public class XDeviceV01 extends XEntityVersion {

    @Id
    @Column(name = "device_id")
    private Long id;

    /**
     * Single value device type.
     * <p>
     * TERMINAL | CARD_READER (SavaPage Terminal | Network Card Reader)
     * </p>
     */
    @Column(name = "device_type", length = 32, nullable = false)
    String deviceType;

    /**
     * Comma separated list of functions.
     * <p>
     * For CARD_READER: AUTH,INSTANT_PROXY_PRINT
     * </p>
     * <p>
     * For TERMINAL: SECURE_PROXY_PRINT,LOCAL_CARD_ASSOC,NETWORK_CARD_ASSOC
     * </p>
     */
    @Column(name = "device_function", length = 32)
    String deviceFunction;

    @Column(name = "device_name", length = 255, nullable = false)
    private String deviceName;

    @Column(name = "display_name", length = 255, nullable = false)
    private String displayName;

    @Column(name = "location", length = 255)
    private String location;

    @Column(name = "notes", length = 2000)
    private String notes;

    @Column(name = "disabled", nullable = false)
    private Boolean disabled = false;

    @Column(name = "card_reader_id")
    private Long cardReader;

    @Column(name = "printer_id", nullable = true)
    private Long printer;

    @Column(name = "printer_group_id", nullable = true)
    private Long printerGroup;

    @Column(name = "hostname", length = 45, nullable = false)
    private String hostname;

    @Column(name = "port")
    private Integer port;

    @Column(name = "created_date", nullable = false)
    private Date createdDate;

    @Column(name = "created_by", length = 50, nullable = false)
    private String createdBy;

    @Column(name = "modified_date")
    private Date modifiedDate;

    @Column(name = "modified_by", length = 50)
    private String modifiedBy;

    @Column(name = "last_usage_date")
    private Date lastUsageDate;

    @Override
    public final String xmlName() {
        return "Device";
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public String getDeviceFunction() {
        return deviceFunction;
    }

    public void setDeviceFunction(String deviceFunction) {
        this.deviceFunction = deviceFunction;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
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

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Long getCardReader() {
        return cardReader;
    }

    public void setCardReader(Long cardReader) {
        this.cardReader = cardReader;
    }

    public Long getPrinter() {
        return printer;
    }

    public void setPrinter(Long printer) {
        this.printer = printer;
    }

    public Long getPrinterGroup() {
        return printerGroup;
    }

    public void setPrinterGroup(Long printerGroup) {
        this.printerGroup = printerGroup;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public Boolean getDisabled() {
        return disabled;
    }

    public void setDisabled(Boolean disabled) {
        this.disabled = disabled;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Date getModifiedDate() {
        return modifiedDate;
    }

    public void setModifiedDate(Date modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

    public String getModifiedBy() {
        return modifiedBy;
    }

    public void setModifiedBy(String modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    public Date getLastUsageDate() {
        return lastUsageDate;
    }

    public void setLastUsageDate(Date lastUsageDate) {
        this.lastUsageDate = lastUsageDate;
    }

}
