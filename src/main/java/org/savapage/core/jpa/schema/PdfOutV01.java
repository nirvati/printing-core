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

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

/**
 * PDF Output Document.
 *
 * @author Rijk Ravestein
 *
 */
@Entity
@Table(name = PdfOutV01.TABLE_NAME)
public class PdfOutV01 implements SchemaEntityVersion {

    public static final String TABLE_NAME = "tbl_pdf_out";

    @Id
    @Column(name = "pdf_out_id")
    @TableGenerator(name = "pdfOutPropGen", table = SequenceV01.TABLE_NAME,
            pkColumnName = SequenceV01.COL_SEQUENCE_NAME,
            valueColumnName = SequenceV01.COL_SEQUENCE_NEXT_VALUE,
            pkColumnValue = TABLE_NAME, allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.TABLE,
            generator = "pdfOutPropGen")
    private Long id;

    @Column(name = "author", length = 255, nullable = true, insertable = true,
            updatable = true)
    private String author;

    @Column(name = "subject", length = 255, nullable = true, insertable = true,
            updatable = true)
    private String subject;

    @Column(name = "keywords", length = 255, nullable = true, insertable = true,
            updatable = true)
    private String keywords;

    @Column(name = "password_user", length = 255, nullable = true,
            insertable = true, updatable = true)
    private String passwordUser;

    @Column(name = "password_owner", length = 255, nullable = true,
            insertable = true, updatable = true)
    private String passwordOwner;

    @Column(name = "encrypted", nullable = false, insertable = true,
            updatable = true)
    private Boolean encrypted;

    @OneToOne(mappedBy = "pdfOut", cascade = { CascadeType.ALL },
            fetch = FetchType.EAGER, optional = false)
    private DocOutV01 docOut;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    public String getPasswordUser() {
        return passwordUser;
    }

    public void setPasswordUser(String passwordUser) {
        this.passwordUser = passwordUser;
    }

    public String getPasswordOwner() {
        return passwordOwner;
    }

    public void setPasswordOwner(String passwordOwner) {
        this.passwordOwner = passwordOwner;
    }

    public Boolean getEncrypted() {
        return encrypted;
    }

    public void setEncrypted(Boolean encrypted) {
        this.encrypted = encrypted;
    }

    public DocOutV01 getDocOut() {
        return docOut;
    }

    public void setDocOut(DocOutV01 docOut) {
        this.docOut = docOut;
    }

}
