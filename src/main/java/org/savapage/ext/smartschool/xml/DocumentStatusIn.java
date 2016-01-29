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
package org.savapage.ext.smartschool.xml;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.savapage.ext.smartschool.SmartschoolPrintStatusEnum;

/**
 * Document status to send to Smartschool.
 *
 * @author Rijk Ravestein
 *
 */
@XmlRootElement(name = "status")
public final class DocumentStatusIn extends SmartschoolXmlObject {

    private final String type = "print";
    private String documentId;
    private String code;
    private String comment;

    @XmlAttribute
    public String getType() {
        return type;
    }

    @XmlElement(name = "id", required = true)
    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    @XmlElement(required = true)
    public String getCode() {
        return code;
    }

    /**
     * @param code
     *            The code as in {@link SmartschoolPrintStatusEnum#getXmlText()}
     *            .
     */
    public void setCode(String code) {
        this.code = code;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

}
