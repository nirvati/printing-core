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
import javax.persistence.UniqueConstraint;

/**
 *
 * @author Rijk Ravestein
 *
 */
@Entity
@Table(name = PrinterV01.TABLE_NAME, uniqueConstraints = { @UniqueConstraint(
        columnNames = { "printer_name" }, name = "uc_printer_1") })
public class PrinterV01 implements SchemaEntityVersion {

    /**
     *
     */
    public static final String TABLE_NAME = "tbl_printer";

    @Id
    @Column(name = "printer_id")
    @TableGenerator(name = "printerPropGen", table = SequenceV01.TABLE_NAME,
            pkColumnName = SequenceV01.COL_SEQUENCE_NAME,
            valueColumnName = SequenceV01.COL_SEQUENCE_NEXT_VALUE,
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

    @Column(name = "location", length = 255, nullable = true, insertable = true,
            updatable = true)
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
    @OneToMany(targetEntity = PrintOutV01.class, mappedBy = "printer",
            cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PrintOutV01> printsOut;

    /**
     * The LAZY PrinterAttr list.
     */
    @OneToMany(targetEntity = PrinterAttrV01.class, mappedBy = "printer",
            cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PrinterAttrV01> attributes;

    /**
     * The LAZY PrinterGroupMember list.
     */
    @OneToMany(targetEntity = PrinterGroupMemberV01.class, mappedBy = "printer",
            cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PrinterGroupMemberV01> printerGroupMembers;

    /**
     * The LAZY Device list.
     */
    @OneToMany(targetEntity = DeviceV01.class, mappedBy = "printer",
            fetch = FetchType.LAZY)
    private List<DeviceV01> devices;

    @Column(name = "notes", length = 2000)
    private String notes;

    @Column(name = "default_cost", nullable = false, precision = 10, scale = 6)
    private BigDecimal defaultCost = BigDecimal.ZERO;

    /**
     * SIMPLE | SIZE_TABLE
     */
    @Column(name = "charge_type", length = 20, nullable = false)
    private String chargeType = "SIMPLE";

    /**
     * for future use
     */
    @Column(name = "server_name", length = 255)
    private String serverName;

    /**
     * For future use.
     * <p>
     * Enum: GRAYSCALE | COLOR_STANDARD | COLOR_PAGE_LEVEL
     * </p>
     */
    @Column(name = "color_detection_mode", length = 20, nullable = false)
    private String colorDetectionMode = "GRAYSCALE";

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

    public List<PrintOutV01> getPrintsOut() {
        return printsOut;
    }

    public void setPrintsOut(List<PrintOutV01> printsOut) {
        this.printsOut = printsOut;
    }

    public List<PrinterAttrV01> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<PrinterAttrV01> attributes) {
        this.attributes = attributes;
    }

    public List<PrinterGroupMemberV01> getPrinterGroupMembers() {
        return printerGroupMembers;
    }

    public void setPrinterGroupMembers(
            List<PrinterGroupMemberV01> printerGroupMembers) {
        this.printerGroupMembers = printerGroupMembers;
    }

    public List<DeviceV01> getDevices() {
        return devices;
    }

    public void setDevices(List<DeviceV01> devices) {
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
