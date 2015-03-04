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

import org.savapage.core.jpa.schema.DocInOutV01;

/**
 * Relation between Input and Output Document.
 *
 * @author Datraverse B.V.
 *
 */
@Entity
@Table(name = DocInOutV01.TABLE_NAME)
public class XDocInOutV01 extends XEntityVersion {

    @Id
    @Column(name = "doc_in_out_id")
    private Long id;

    @Column(name = "doc_in_id", nullable = false)
    private Long docIn;

    @Column(name = "doc_out_id", nullable = false)
    private Long docOut;

    @Column(name = "total_pages", nullable = true, insertable = true,
            updatable = true)
    private Integer numberOfPages;

    @Override
    public final String xmlName() {
        return "DocInOut";
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public Integer getNumberOfPages() {
        return numberOfPages;
    }

    public void setNumberOfPages(Integer numberOfPages) {
        this.numberOfPages = numberOfPages;
    }

    public Long getDocIn() {
        return docIn;
    }

    public void setDocIn(Long docIn) {
        this.docIn = docIn;
    }

    public Long getDocOut() {
        return docOut;
    }

    public void setDocOut(Long docOut) {
        this.docOut = docOut;
    }

}
