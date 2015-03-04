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

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

/**
 * IPP Print Input Document.
 *
 * @author Datraverse B.V.
 *
 */
@Entity
@Table(name = PrintInV01.TABLE_NAME)
public class PrintInV01 implements SchemaEntityVersion {

    public static final String TABLE_NAME = "tbl_print_in";

    @Id
    @Column(name = "print_in_id")
    @TableGenerator(name = "printinPropGen", table = SequenceV01.TABLE_NAME,
            pkColumnName = "SEQUENCE_NAME",
            valueColumnName = "SEQUENCE_NEXT_VALUE",
            pkColumnValue = TABLE_NAME, allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.TABLE,
            generator = "printinPropGen")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "queue_id", nullable = false, foreignKey = @ForeignKey(
            name = "FK_PRINT_IN_TO_IPP_QUEUE"))
    private IppQueueV01 queue;

    @Column(name = "paper_size", length = 20, nullable = true,
            insertable = true, updatable = true)
    private String paperSize;

    @Column(name = "paper_height_mm", nullable = true, insertable = true,
            updatable = true)
    private Integer paperHeight;

    @Column(name = "paper_width_mm", nullable = true, insertable = true,
            updatable = true)
    private Integer paperWidth;

    @Column(name = "printed", nullable = false, insertable = true,
            updatable = true)
    private Boolean printed;

    @Column(name = "denied_reason", length = 30, nullable = true,
            insertable = true, updatable = true)
    private String deniedReason;

    @OneToOne(mappedBy = "printIn", cascade = { CascadeType.ALL },
            fetch = FetchType.EAGER, optional = false)
    private DocInV01 docIn;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public IppQueueV01 getQueue() {
        return queue;
    }

    public void setQueue(IppQueueV01 queue) {
        this.queue = queue;
    }

    public String getPaperSize() {
        return paperSize;
    }

    public void setPaperSize(String paperSize) {
        this.paperSize = paperSize;
    }

    public Integer getPaperHeight() {
        return paperHeight;
    }

    public void setPaperHeight(Integer paperHeight) {
        this.paperHeight = paperHeight;
    }

    public Integer getPaperWidth() {
        return paperWidth;
    }

    public void setPaperWidth(Integer paperWidth) {
        this.paperWidth = paperWidth;
    }

    public Boolean getPrinted() {
        return printed;
    }

    public void setPrinted(Boolean printed) {
        this.printed = printed;
    }

    public String getDeniedReason() {
        return deniedReason;
    }

    public void setDeniedReason(String deniedReason) {
        this.deniedReason = deniedReason;
    }

    public DocInV01 getDocIn() {
        return docIn;
    }

    public void setDocIn(DocInV01 docIn) {
        this.docIn = docIn;
    }

}
