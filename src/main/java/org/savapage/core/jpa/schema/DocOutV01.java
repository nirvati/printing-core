/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2015 Datraverse B.V.
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
 * Output Document.
 *
 * @author Datraverse B.V.
 *
 */
@Entity
@Table(name = DocOutV01.TABLE_NAME)
public class DocOutV01 implements SchemaEntityVersion {

    /**
     *
     */
    public static final String TABLE_NAME = "tbl_doc_out";

    @Id
    @Column(name = "doc_out_id")
    @TableGenerator(name = "docOutPropGen", table = SequenceV01.TABLE_NAME,
            pkColumnName = "SEQUENCE_NAME",
            valueColumnName = "SEQUENCE_NEXT_VALUE",
            pkColumnValue = TABLE_NAME, allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.TABLE,
            generator = "docOutPropGen")
    private Long id;

    /**
     */
    @Column(name = "destination", length = 255, nullable = true,
            insertable = true, updatable = true)
    private String destination;

    @Column(name = "signature", length = 50, nullable = false,
            insertable = true, updatable = true)
    private String signature;

    @Column(name = "letterhead", nullable = true, insertable = true,
            updatable = true)
    private Boolean letterhead;

    @Column(name = "ecoprint", nullable = false, insertable = true,
            updatable = true)
    private Boolean ecoPrint;

    @Column(name = "remove_graphics", nullable = false, insertable = true,
            updatable = true)
    private Boolean removeGraphics;

    @OneToOne(mappedBy = "docOut", cascade = { CascadeType.ALL },
            fetch = FetchType.EAGER, optional = false)
    private DocLogV01 docLog;

    /**
     * The optional EAGER PrintOut association.
     */
    @OneToOne(cascade = { CascadeType.ALL }, fetch = FetchType.EAGER,
            optional = true)
    @JoinColumn(name = "print_out_id", nullable = true,
            foreignKey = @ForeignKey(name = "FK_DOC_OUT_TO_PRINT_OUT"))
    private PrintOutV01 printOut;

    /**
     * The optional EAGER PdfOut association.
     */
    @OneToOne(cascade = { CascadeType.ALL }, fetch = FetchType.EAGER,
            optional = true)
    @JoinColumn(name = "pdf_out_id", nullable = true, foreignKey = @ForeignKey(
            name = "FK_DOC_OUT_TO_PDF_OUT"))
    private PdfOutV01 pdfOut;

    /**
     * The LAZY DocInOut list.
     */
    @OneToMany(targetEntity = DocInOutV01.class, mappedBy = "docOut",
            cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<DocInOutV01> docsInOut;

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

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public Boolean getLetterhead() {
        return letterhead;
    }

    public void setLetterhead(Boolean letterhead) {
        this.letterhead = letterhead;
    }

    public Boolean getEcoPrint() {
        return ecoPrint;
    }

    public void setEcoPrint(Boolean ecoPrint) {
        this.ecoPrint = ecoPrint;
    }

    public Boolean getRemoveGraphics() {
        return removeGraphics;
    }

    public void setRemoveGraphics(Boolean removeGraphics) {
        this.removeGraphics = removeGraphics;
    }

    public DocLogV01 getDocLog() {
        return docLog;
    }

    public void setDocLog(DocLogV01 docLog) {
        this.docLog = docLog;
    }

    public PrintOutV01 getPrintOut() {
        return printOut;
    }

    public void setPrintOut(PrintOutV01 printOut) {
        this.printOut = printOut;
    }

    public PdfOutV01 getPdfOut() {
        return pdfOut;
    }

    public void setPdfOut(PdfOutV01 pdfOut) {
        this.pdfOut = pdfOut;
    }

    public List<DocInOutV01> getDocsInOut() {
        return docsInOut;
    }

    public void setDocsInOut(List<DocInOutV01> docsInOut) {
        this.docsInOut = docsInOut;
    }

}
