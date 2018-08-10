/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2018 Datraverse B.V.
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
package org.savapage.core.jpa;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

/**
 *
 * @author Rijk Ravestein
 *
 */
@Entity
@Table(name = UserAttr.TABLE_NAME)
public class UserAttr extends org.savapage.core.jpa.Entity {

    /** */
    public static final String TABLE_NAME = "tbl_user_attr";

    /** */
    public static final int COL_ATTRIB_VALUE_LENGTH = 2000;

    @Id
    @Column(name = "user_attr_id")
    @TableGenerator(name = "userAttrPropGen", table = Sequence.TABLE_NAME,
            pkColumnName = "SEQUENCE_NAME",
            valueColumnName = "SEQUENCE_NEXT_VALUE", pkColumnValue = TABLE_NAME,
            allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.TABLE,
            generator = "userAttrPropGen")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "attrib_name", length = 255, nullable = false,
            insertable = true, updatable = false)
    private String name;

    @Column(name = "attrib_value", length = COL_ATTRIB_VALUE_LENGTH,
            nullable = true, insertable = true, updatable = true)
    private String value;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
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
