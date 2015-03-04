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
package org.savapage.core.jpa;

import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

import org.savapage.core.dao.helpers.DeviceTypeEnum;

/**
 * A physical device of a certain type running one or more SavaPage functions.
 *
 * @author Datraverse B.V.
 *
 */
@Entity
@Table(name = Device.TABLE_NAME)
public class Device extends org.savapage.core.jpa.Entity {

    public static final String TABLE_NAME = "tbl_device";

    @Id
    @Column(name = "device_id")
    @TableGenerator(name = "devicePropGen", table = Sequence.TABLE_NAME,
            //
            pkColumnName = "SEQUENCE_NAME",
            valueColumnName = "SEQUENCE_NEXT_VALUE",
            //
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

    // Do NOT use @Column 'unique = true': use @UniqueConstraint instead.
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
     * The optional EAGER association from {@link DeviceTypeEnum#TERMINAL} to
     * {@link DeviceTypeEnum#CARD_READER}.
     */
    @OneToOne(fetch = FetchType.EAGER, optional = true)
    @JoinColumn(name = "card_reader_id", nullable = true)
    private Device cardReader;

    /**
     * The (inverse) optional EAGER association from
     * {@link DeviceTypeEnum#CARD_READER} to {@link DeviceTypeEnum#TERMINAL}.
     * <p>
     * Note that there is NO JoinColumn, in case of a
     * {@link DeviceTypeEnum#CARD_READER} instance we simple want to "see" the
     * associated {@link DeviceTypeEnum#TERMINAL}.
     * </p>
     * <p>
     * IMPORTANT:
     * </p>
     * <p>
     * This construct does NOT guard the constraint, that a
     * {@link DeviceTypeEnum#CARD_READER} can only be assigned to ONE (1)
     * {@link DeviceTypeEnum#TERMINAL}, on a <i>database level</i>.
     * </p>
     * <p>
     * It is possible to assign a Card Reader to several Terminals. However,
     * when retrieving the Card Reader instance, Hibernate throws an exception
     * since the OneToOne relation is violated.
     * </p>
     * <p>
     * CONCLUSION:
     * </p>
     * <p>
     * This OneToOne constraint must be enforced <i>programmatically</i>.
     * </p>
     */
    @OneToOne(mappedBy = "cardReader", fetch = FetchType.EAGER, optional = true)
    private Device cardReaderTerminal;

    /**
     * The optional EAGER association from TERMINAL to PRINTER.
     * <p>
     * foreignKey = @ForeignKey(name = "FK_DEVICE_TO_PRINTER")
     * </p>
     */
    @OneToOne(fetch = FetchType.EAGER, optional = true)
    @JoinColumn(name = "printer_id", nullable = true)
    private Printer printer;

    /**
     * The optional EAGER association from TERMINAL to PRINTER GROUP.
     * <p>
     * foreignKey = @ForeignKey(name = "FK_DEVICE_TO_PRINTER_GROUP")
     * </p>
     */
    @OneToOne(fetch = FetchType.EAGER, optional = true)
    @JoinColumn(name = "printer_group_id", nullable = true)
    private PrinterGroup printerGroup;

    @Column(name = "hostname", length = 45, nullable = false)
    private String hostname;

    @Column(name = "port")
    private Integer port;

    /**
     * The LAZY DeviceAttr list.
     */
    @OneToMany(targetEntity = DeviceAttr.class, mappedBy = "device",
            cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<DeviceAttr> attributes;

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

    public Device getCardReader() {
        return cardReader;
    }

    public void setCardReader(Device cardReader) {
        this.cardReader = cardReader;
    }

    public Device getCardReaderTerminal() {
        return cardReaderTerminal;
    }

    public void setCardReaderTerminal(Device cardReaderTerminal) {
        this.cardReaderTerminal = cardReaderTerminal;
    }

    public Printer getPrinter() {
        return printer;
    }

    public void setPrinter(Printer printer) {
        this.printer = printer;
    }

    public PrinterGroup getPrinterGroup() {
        return printerGroup;
    }

    public void setPrinterGroup(PrinterGroup printerGroup) {
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

    public List<DeviceAttr> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<DeviceAttr> attributes) {
        this.attributes = attributes;
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
