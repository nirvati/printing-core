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

import org.savapage.core.jpa.schema.DocInV01;

/**
 * Input Document.
 *
 * @author Rijk Ravestein
 *
 */
@Entity
@Table(name = DocInV01.TABLE_NAME)
public class XDocInV01 extends XEntityVersion {

    @Id
    @Column(name = "doc_in_id")
    private Long id;

    @Column(name = "originator_ip", length = 45, nullable = true,
            insertable = true, updatable = true)
    private String originatorIp;

    /**
     * The optional EAGER PrintIn association.
     */
    @Column(name = "print_in_id", nullable = true)
    private Long printIn;

    @Override
    public final String xmlName() {
        return "DocIn";
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public String getOriginatorIp() {
        return originatorIp;
    }

    public void setOriginatorIp(String originatorIp) {
        this.originatorIp = originatorIp;
    }

    public Long getPrintIn() {
        return printIn;
    }

    public void setPrintIn(Long printIn) {
        this.printIn = printIn;
    }

}
