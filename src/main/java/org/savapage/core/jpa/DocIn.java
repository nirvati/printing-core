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
package org.savapage.core.jpa;

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

/**
 * Input Document.
 *
 * @author Rijk Ravestein
 *
 */
@Entity
@Table(name = DocIn.TABLE_NAME)
public class DocIn extends org.savapage.core.jpa.Entity {

    /**
     *
     */
    public static final String TABLE_NAME = "tbl_doc_in";

    @Id
    @Column(name = "doc_in_id")
    @TableGenerator(name = "docinPropGen", table = Sequence.TABLE_NAME,
            pkColumnName = Sequence.COL_SEQUENCE_NAME,
            valueColumnName = Sequence.COL_SEQUENCE_NEXT_VALUE,
            pkColumnValue = TABLE_NAME, allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "docinPropGen")
    private Long id;

    /**
     * IP address of the originating user (creator).
     * <p>
     * We take IPv6 addresses into account.
     *
     * For IPv4 mapped IPv6 addresses, the string can be longer than 39
     * characters. For example:
     *
     * IPv6 (39 bytes) : ABCD:ABCD:ABCD:ABCD:ABCD:ABCD:ABCD:ABCD
     *
     * IPv4-mapped IPv6 (45 bytes) :
     *
     * ABCD:ABCD:ABCD:ABCD:ABCD:ABCD:192.168.158.190
     *
     * The last 32-bits (that correspond to IPv4 address) can need more than 10
     * characters.
     *
     * The correct maximum IPv6 string length, therefore, is 45.
     *
     * See:
     * "http://stackoverflow.com/questions/1076714/max-length-for-client-ip-address"
     * </p>
     */
    @Column(name = "originator_ip", length = 45, nullable = true,
            insertable = true, updatable = true)
    private String originatorIp;

    @OneToOne(mappedBy = "docIn", cascade = { CascadeType.ALL },
            fetch = FetchType.EAGER, optional = false)
    private DocLog docLog;

    /**
     * The optional EAGER PrintIn association.
     */
    @OneToOne(cascade = { CascadeType.ALL }, fetch = FetchType.EAGER,
            optional = true)
    @JoinColumn(name = "print_in_id", nullable = true,
            foreignKey = @ForeignKey(name = "FK_DOC_IN_TO_PRINT_IN"))
    private PrintIn printIn;

    /**
     * The LAZY DocInOut list.
     */
    @OneToMany(targetEntity = DocInOut.class, mappedBy = "docIn",
            cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<DocInOut> docsInOut;

    /**
     *
     * @param id
     */
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

    public DocLog getDocLog() {
        return docLog;
    }

    public void setDocLog(DocLog docLog) {
        this.docLog = docLog;
    }

    public PrintIn getPrintIn() {
        return printIn;
    }

    public void setPrintIn(PrintIn printIn) {
        this.printIn = printIn;
    }

    public List<DocInOut> getDocsInOut() {
        return docsInOut;
    }

    public void setDocsInOut(List<DocInOut> docsInOut) {
        this.docsInOut = docsInOut;
    }

}
