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

import org.savapage.core.jpa.schema.DocOutV01;

/**
 * Output Document.
 *
 * @author Datraverse B.V.
 *
 */
@Entity
@Table(name = DocOutV01.TABLE_NAME)
public class XDocOutV01 extends XEntityVersion {

    @Id
    @Column(name = "doc_out_id")
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

    @Column(name = "print_out_id", nullable = true)
    private Long printOut;

    @Column(name = "pdf_out_id", nullable = true)
    private Long pdfOut;

    @Override
    public final String xmlName() {
        return "DocOut";
    }

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

    public Long getPrintOut() {
        return printOut;
    }

    public void setPrintOut(Long printOut) {
        this.printOut = printOut;
    }

    public Long getPdfOut() {
        return pdfOut;
    }

    public void setPdfOut(Long pdfOut) {
        this.pdfOut = pdfOut;
    }

}
