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
package org.savapage.core.jpa.schema;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.UniqueConstraint;

/**
 *
 * @author Datraverse B.V.
 *
 */
@Entity
@Table(name = UserGroupAttrV01.TABLE_NAME,
        uniqueConstraints = { @UniqueConstraint(name = "uc_user_group_attr_1",
                columnNames = { "user_group_id", "attrib_name" }) })
public class UserGroupAttrV01 implements SchemaEntityVersion {

    /**
     *
     */
    public static final String TABLE_NAME = "tbl_user_group_attr";

    @Id
    @Column(name = "user_group_attr_id")
    @TableGenerator(name = "userGroupAttrPropGen",
            table = SequenceV01.TABLE_NAME, pkColumnName = "SEQUENCE_NAME",
            valueColumnName = "SEQUENCE_NEXT_VALUE",
            pkColumnValue = TABLE_NAME, allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.TABLE,
            generator = "userGroupAttrPropGen")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_group_id", nullable = false,
            foreignKey = @ForeignKey(name = "FK_USER_GROUP_ATTR_TO_USER_GROUP"))
    private UserGroupV01 userGroup;

    @Column(name = "attrib_name", length = 255, nullable = false,
            insertable = true, updatable = false)
    private String name;

    @Column(name = "attrib_value", length = 2000, nullable = true,
            insertable = true, updatable = true)
    private String value;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public UserGroupV01 getUserGroup() {
        return userGroup;
    }

    public void setUserGroup(UserGroupV01 userGroup) {
        this.userGroup = userGroup;
    }

}
