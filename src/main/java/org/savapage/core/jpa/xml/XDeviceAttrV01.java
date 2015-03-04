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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.savapage.core.jpa.schema.DeviceAttrV01;

/**
 * Attribute for a Device.
 *
 * @author Datraverse B.V.
 *
 */
@Entity
@Table(name = DeviceAttrV01.TABLE_NAME)
public class XDeviceAttrV01 extends XEntityVersion {

    @Id
    @Column(name = "device_attr_id")
    private Long id;

    @Column(name = "device_id", nullable = false)
    private Long device;

    @Column(name = "attrib_name", length = 255, nullable = false,
            insertable = true, updatable = false)
    private String name;

    @Column(name = "attrib_value", length = 2000, nullable = true,
            insertable = true, updatable = true)
    private String value;

    @Override
    public final String xmlName() {
        return "DeviceAttr";
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDevice() {
        return device;
    }

    public void setDevice(Long device) {
        this.device = device;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

}
