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
package org.savapage.core.jpa.xml;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.savapage.core.jpa.schema.UserEmailV01;

/**
 *
 * @author Rijk Ravestein
 *
 */
@Entity
@Table(name = UserEmailV01.TABLE_NAME)
public class XUserEmailV01 extends XEntityVersion {

    @Id
    @Column(name = "user_email_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long user;

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

    @Override
    public final String xmlName() {
        return "UserEMail";
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUser() {
        return user;
    }

    public void setUser(Long user) {
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
