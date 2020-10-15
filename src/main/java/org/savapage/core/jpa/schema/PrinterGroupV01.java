/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2020 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: © 2020 Datraverse B.V. <info@datraverse.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
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
import java.util.List;

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
@Table(name = PrinterGroupV01.TABLE_NAME,
        uniqueConstraints = {
                @UniqueConstraint(columnNames = { "group_name" },
                        name = "uc_printer_group_1"),
                @UniqueConstraint(columnNames = { "display_name" },
                        name = "uc_printer_group_2") })
public class PrinterGroupV01 implements SchemaEntityVersion {

    public static final String TABLE_NAME = "tbl_printer_group";

    @Id
    @Column(name = "printer_group_id")
    @TableGenerator(name = "printerGroupPropGen",
            table = SequenceV01.TABLE_NAME,
            //
            pkColumnName = SequenceV01.COL_SEQUENCE_NAME,
            valueColumnName = SequenceV01.COL_SEQUENCE_NEXT_VALUE,
            //
            pkColumnValue = TABLE_NAME, allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.TABLE,
            generator = "printerGroupPropGen")
    private Long id;

    @Column(name = "group_name", length = 255, nullable = false)
    private String groupName;

    @Column(name = "display_name", length = 255, nullable = false)
    private String displayName;

    /**
     * The LAZY Device list.
     */
    @OneToMany(targetEntity = DeviceV01.class, mappedBy = "printerGroup",
            fetch = FetchType.LAZY)
    private List<DeviceV01> devices;

    /**
     * The LAZY member list.
     */
    @OneToMany(targetEntity = PrinterGroupMemberV01.class, mappedBy = "group",
            fetch = FetchType.LAZY)
    private List<PrinterGroupMemberV01> members;

    /**
     *
     */
    @Column(name = "created_date", nullable = false)
    private Date createdDate;

    @Column(name = "created_by", length = 50, nullable = false)
    private String createdBy;

    @Column(name = "modified_date")
    private Date modifiedDate;

    @Column(name = "modified_by", length = 50)
    private String modifiedBy;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public List<DeviceV01> getDevices() {
        return devices;
    }

    public void setDevices(List<DeviceV01> devices) {
        this.devices = devices;
    }

    public List<PrinterGroupMemberV01> getMembers() {
        return members;
    }

    public void setMembers(List<PrinterGroupMemberV01> members) {
        this.members = members;
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

}
