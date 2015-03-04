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

/**
 * Relation between Input and Output Document.
 *
 * @author Datraverse B.V.
 *
 */
@Entity
@Table(name = DocInOutV01.TABLE_NAME)
public class DocInOutV01 implements SchemaEntityVersion {

    /**
     *
     */
    public static final String TABLE_NAME = "tbl_doc_in_out";

    @Id
    @Column(name = "doc_in_out_id")
    @TableGenerator(name = "docInOutPropGen", table = SequenceV01.TABLE_NAME,
            pkColumnName = "SEQUENCE_NAME",
            valueColumnName = "SEQUENCE_NEXT_VALUE",
            pkColumnValue = TABLE_NAME, allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.TABLE,
            generator = "docInOutPropGen")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "doc_in_id", nullable = false, foreignKey = @ForeignKey(
            name = "FK_DOC_IN_OUT_TO_DOC_IN"))
    private DocInV01 docIn;

    @ManyToOne
    @JoinColumn(name = "doc_out_id", nullable = false,
            foreignKey = @ForeignKey(name = "FK_DOC_IN_OUT_TO_DOC_OUT"))
    private DocOutV01 docOut;

    @Column(name = "total_pages", nullable = true, insertable = true,
            updatable = true)
    private Integer numberOfPages;

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

    public Integer getNumberOfPages() {
        return numberOfPages;
    }

    public void setNumberOfPages(Integer numberOfPages) {
        this.numberOfPages = numberOfPages;
    }

    public DocOutV01 getDocOut() {
        return docOut;
    }

    public void setDocOut(DocOutV01 docOut) {
        this.docOut = docOut;
    }

    public DocInV01 getDocIn() {
        return docIn;
    }

    public void setDocIn(DocInV01 docIn) {
        this.docIn = docIn;
    }

}
