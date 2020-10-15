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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.UniqueConstraint;

/**
 *
 * @author Rijk Ravestein
 *
 */
@Entity
@Table(name = UserEmailV01.TABLE_NAME, indexes = { //
        @Index(name = "ix_user_email_1", columnList = "user_id, address"),
        @Index(name = "ix_user_email_2", columnList = "user_id") },
        uniqueConstraints = {
                @UniqueConstraint(columnNames = { "address" },
                        name = "uc_user_email_1"),
                @UniqueConstraint(columnNames = { "user_id", "index_number" },
                        name = "uc_user_email_2") })
public class UserEmailV01 implements SchemaEntityVersion {

    /**
     *
     */
    public static final String TABLE_NAME = "tbl_user_email";

    @Id
    @Column(name = "user_email_id")
    @TableGenerator(name = "userEmailPropGen", table = SequenceV01.TABLE_NAME,
            //
            pkColumnName = SequenceV01.COL_SEQUENCE_NAME,
            valueColumnName = SequenceV01.COL_SEQUENCE_NEXT_VALUE,
            //
            pkColumnValue = TABLE_NAME, allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.TABLE,
            generator = "userEmailPropGen")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "FK_USER_EMAIL_TO_USER"))
    private UserV01 user;

    @Column(name = "index_number", nullable = false)
    private Integer indexNumber;

    @Column(name = "address", length = 255, nullable = false)
    private String address;

    /**
     * "Normally, a mailbox is comprised of two parts: (1) an optional display
     * name that indicates the name of the recipient (which could be a person or
     * a system) that could be displayed to the user of a mail application, and
     * (2) an addr-spec address enclosed in angle brackets ("<" and ">"). "
     * <p>
     * <a href="http://tools.ietf.org/html/rfc2822>RFC2822</a>
     * </p>
     */
    @Column(name = "display_name", length = 255, nullable = true)
    private String displayName;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UserV01 getUser() {
        return user;
    }

    public void setUser(UserV01 user) {
        this.user = user;
    }

    public Integer getIndexNumber() {
        return indexNumber;
    }

    public void setIndexNumber(Integer indexNumber) {
        this.indexNumber = indexNumber;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

}
