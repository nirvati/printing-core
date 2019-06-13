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
package org.savapage.core.jpa.schema;

import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.UniqueConstraint;

/**
 * A physical device of a certain type running one or more SavaPage functions.
 *
 * @author Rijk Ravestein
 *
 */
@Entity
@Table(name = DeviceV01.TABLE_NAME, indexes = { //
        @Index(name = "ix_device_1", columnList = "card_reader_id"),
        @Index(name = "ix_device_2", columnList = "printer_id"),
        @Index(name = "ix_device_3", columnList = "printer_group_id") },
        uniqueConstraints = {
                @UniqueConstraint(name = "uc_device_1",
                        columnNames = { "device_name" }),
                @UniqueConstraint(name = "uc_device_2",
                        columnNames = { "hostname", "device_type" }) })
public class DeviceV01 implements SchemaEntityVersion {

    // =========================================================================
    // DeviceV01 : start
    // =========================================================================
    public static final String TABLE_NAME = "tbl_device";

    @Id
    @Column(name = "device_id")
    @TableGenerator(name = "devicePropGen", table = SequenceV01.TABLE_NAME,
            pkColumnName = SequenceV01.COL_SEQUENCE_NAME,
            valueColumnName = SequenceV01.COL_SEQUENCE_NEXT_VALUE,
            pkColumnValue = TABLE_NAME, allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.TABLE,
            generator = "devicePropGen")
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

    /**
     * The optional EAGER association from TERMINAL to CARD_READER.
     */
    @OneToOne(cascade = { CascadeType.ALL }, fetch = FetchType.EAGER,
            optional = true)
    @JoinColumn(name = "card_reader_id", nullable = true,
            foreignKey = @ForeignKey(
                    name = "FK_DEVICE_TERMINAL_TO_CARD_READER"))
    private DeviceV01 cardReader;

    /**
     * The optional EAGER association from CARD_READER to TERMINAL.
     */
    @OneToOne(mappedBy = "cardReader", fetch = FetchType.EAGER, optional = true)
    private DeviceV01 cardReaderTerminal;

    /**
     * The optional EAGER association from TERMINAL to PRINTER.
     */
    @OneToOne(fetch = FetchType.EAGER, optional = true)
    @JoinColumn(name = "printer_id", nullable = true,
            foreignKey = @ForeignKey(name = "FK_DEVICE_TO_PRINTER"))
    private PrinterV01 printer;

    /**
     * The optional EAGER association from TERMINAL to PRINTER GROUP.
     */
    @OneToOne(fetch = FetchType.EAGER, optional = true)
    @JoinColumn(name = "printer_group_id", nullable = true,
            foreignKey = @ForeignKey(name = "FK_DEVICE_TO_PRINTER_GROUP"))
    private PrinterGroupV01 printerGroup;

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

    /**
     *
     * @return
     */
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

    public Boolean getDisabled() {
        return disabled;
    }

    public void setDisabled(Boolean disabled) {
        this.disabled = disabled;
    }

    public DeviceV01 getCardReader() {
        return cardReader;
    }

    public void setCardReader(DeviceV01 cardReader) {
        this.cardReader = cardReader;
    }

    public DeviceV01 getCardReaderTerminal() {
        return cardReaderTerminal;
    }

    public void setCardReaderTerminal(DeviceV01 cardReaderTerminal) {
        this.cardReaderTerminal = cardReaderTerminal;
    }

    public PrinterV01 getPrinter() {
        return printer;
    }

    public void setPrinter(PrinterV01 printer) {
        this.printer = printer;
    }

    public PrinterGroupV01 getPrinterGroup() {
        return printerGroup;
    }

    public void setPrinterGroup(PrinterGroupV01 printerGroup) {
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
