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

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

/**
 *
 * @author Datraverse B.V.
 */
@Entity
@Table(name = Printer.TABLE_NAME)
public class Printer extends org.savapage.core.jpa.Entity {

    /**
     *
     */
    public static final String TABLE_NAME = "tbl_printer";

    /**
     *
     */
    public static enum ChargeType {
        SIMPLE, MEDIA
    }

    /**
     *
     */
    public static enum ColorDetectionMode {
        GRAYSCALE, COLOR_STANDARD, COLOR_PAGE_LEVEL
    }

    @Id
    @Column(name = "printer_id")
    @TableGenerator(name = "printerPropGen", table = Sequence.TABLE_NAME,
            pkColumnName = "SEQUENCE_NAME",
            valueColumnName = "SEQUENCE_NEXT_VALUE",
            pkColumnValue = TABLE_NAME, allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.TABLE,
            generator = "printerPropGen")
    private Long id;

    @Column(name = "printer_name", length = 255, nullable = false,
            insertable = true, updatable = true)
    private String printerName;

    @Column(name = "display_name", length = 255, nullable = false,
            insertable = true, updatable = true)
    private String displayName;

    @Column(name = "location", length = 255, nullable = true,
            insertable = true, updatable = true)
    private String location;

    @Column(name = "deleted", nullable = false, insertable = true,
            updatable = true)
    private Boolean deleted = false;

    @Column(name = "deleted_date", nullable = true, insertable = true,
            updatable = true)
    private Date deletedDate;

    @Column(name = "disabled", nullable = false, insertable = true,
            updatable = true)
    private Boolean disabled = false;

    @Column(name = "disabled_date", nullable = true, insertable = true,
            updatable = true)
    private Date disabledDate;

    @Column(name = "created_date", nullable = false, insertable = true,
            updatable = true)
    private Date createdDate;

    @Column(name = "created_by", length = 50, nullable = false,
            insertable = true, updatable = true)
    private String createdBy;

    @Column(name = "modified_date", nullable = true, insertable = true,
            updatable = true)
    private Date modifiedDate;

    @Column(name = "modified_by", length = 50, nullable = true,
            insertable = true, updatable = true)
    private String modifiedBy;

    @Column(name = "reset_by", length = 50, nullable = true, insertable = true,
            updatable = true)
    private String resetBy;

    @Column(name = "reset_date", nullable = true, insertable = true,
            updatable = true)
    private Date resetDate;

    @Column(name = "total_jobs", nullable = false, insertable = true,
            updatable = true)
    private Integer totalJobs = 0;

    @Column(name = "total_pages", nullable = false, insertable = true,
            updatable = true)
    private Integer totalPages = 0;

    @Column(name = "total_sheets", nullable = false, insertable = true,
            updatable = true)
    private Integer totalSheets = 0;

    @Column(name = "total_esu", nullable = false, insertable = true,
            updatable = true)
    private Long totalEsu = 0L;

    @Column(name = "total_bytes", nullable = false, insertable = true,
            updatable = true)
    private Long totalBytes = 0L;

    @Column(name = "last_usage_date", nullable = true, insertable = true,
            updatable = true)
    private Date lastUsageDate;

    /**
     * The LAZY PrintOut list.
     */
    @OneToMany(targetEntity = PrintOut.class, mappedBy = "printer",
            cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PrintOut> printsOut;

    /**
     * The LAZY PrinterAttr list.
     */
    @OneToMany(targetEntity = PrinterAttr.class, mappedBy = "printer",
            cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PrinterAttr> attributes;

    /**
     * The LAZY PrinterGroupMember list.
     */
    @OneToMany(targetEntity = PrinterGroupMember.class, mappedBy = "printer",
            cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PrinterGroupMember> printerGroupMembers;

    /**
     * The LAZY Device list.
     */
    @OneToMany(targetEntity = Device.class, mappedBy = "printer",
            fetch = FetchType.LAZY)
    private List<Device> devices;

    /**
     *
     */
    @Column(name = "notes", length = 2000)
    private String notes;

    @Column(name = "default_cost", nullable = false, precision = 10, scale = 6)
    private BigDecimal defaultCost = BigDecimal.ZERO;

    /**
     * {@link ChargeType}.
     */
    @Column(name = "charge_type", length = 20, nullable = false)
    private String chargeType = ChargeType.SIMPLE.toString();

    /**
     * for future use
     */
    @Column(name = "server_name", length = 255)
    private String serverName;

    /**
     * For future use.
     * <p>
     * {@link ColorDetectionMode}.
     * </p>
     */
    @Column(name = "color_detection_mode", length = 20, nullable = false)
    private String colorDetectionMode = ColorDetectionMode.GRAYSCALE.toString();

    /**
     *
     * @param id
     */
    public void setId(final Long id) {
        this.id = id;
    }

    /**
     * Gets the primary key.
     *
     * @return the key.
     */
    public final Long getId() {
        return id;
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

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }

    public Date getDeletedDate() {
        return deletedDate;
    }

    public void setDeletedDate(Date deletedDate) {
        this.deletedDate = deletedDate;
    }

    public Boolean getDisabled() {
        return disabled;
    }

    public void setDisabled(Boolean disabled) {
        this.disabled = disabled;
    }

    public Date getDisabledDate() {
        return disabledDate;
    }

    public void setDisabledDate(Date disabledDate) {
        this.disabledDate = disabledDate;
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

    public String getResetBy() {
        return resetBy;
    }

    public void setResetBy(String resetBy) {
        this.resetBy = resetBy;
    }

    public Date getResetDate() {
        return resetDate;
    }

    public void setResetDate(Date resetDate) {
        this.resetDate = resetDate;
    }

    public Integer getTotalJobs() {
        return totalJobs;
    }

    public void setTotalJobs(Integer totalJobs) {
        this.totalJobs = totalJobs;
    }

    public Integer getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(Integer totalPages) {
        this.totalPages = totalPages;
    }

    public Integer getTotalSheets() {
        return totalSheets;
    }

    public void setTotalSheets(Integer totalSheets) {
        this.totalSheets = totalSheets;
    }

    public Long getTotalEsu() {
        return totalEsu;
    }

    public void setTotalEsu(Long totalEsu) {
        this.totalEsu = totalEsu;
    }

    public Long getTotalBytes() {
        return totalBytes;
    }

    public void setTotalBytes(Long totalBytes) {
        this.totalBytes = totalBytes;
    }

    public Date getLastUsageDate() {
        return lastUsageDate;
    }

    public void setLastUsageDate(Date lastUsageDate) {
        this.lastUsageDate = lastUsageDate;
    }

    public List<PrintOut> getPrintsOut() {
        return printsOut;
    }

    public void setPrintsOut(List<PrintOut> printsOut) {
        this.printsOut = printsOut;
    }

    public List<PrinterAttr> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<PrinterAttr> attributes) {
        this.attributes = attributes;
    }

    public List<PrinterGroupMember> getPrinterGroupMembers() {
        return printerGroupMembers;
    }

    public void setPrinterGroupMembers(
            List<PrinterGroupMember> printerGroupMembers) {
        this.printerGroupMembers = printerGroupMembers;
    }

    public List<Device> getDevices() {
        return devices;
    }

    public void setDevices(List<Device> devices) {
        this.devices = devices;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public BigDecimal getDefaultCost() {
        return defaultCost;
    }

    public void setDefaultCost(BigDecimal defaultCost) {
        this.defaultCost = defaultCost;
    }

    public String getChargeType() {
        return chargeType;
    }

    public void setChargeType(String chargeType) {
        this.chargeType = chargeType;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getColorDetectionMode() {
        return colorDetectionMode;
    }

    public void setColorDetectionMode(String colorDetectionMode) {
        this.colorDetectionMode = colorDetectionMode;
    }

}
