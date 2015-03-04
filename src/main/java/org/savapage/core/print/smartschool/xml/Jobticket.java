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
package org.savapage.core.print.smartschool.xml;

import java.io.StringReader;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author Datraverse B.V.
 *
 */
@XmlRootElement
public class Jobticket {

    private String location;
    private String type;

    private Requestinfo requestinfo;

    private Documents documents;

    @XmlAttribute
    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    @XmlAttribute
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Requestinfo getRequestinfo() {
        return requestinfo;
    }

    public void setRequestinfo(Requestinfo requestinfo) {
        this.requestinfo = requestinfo;
    }

    public Documents getDocuments() {
        return documents;
    }

    public void setDocuments(Documents documents) {
        this.documents = documents;
    }

    /**
     * Create a {@link Jobticket} object from XML.
     *
     * @param xml
     *            The XML string.
     * @return The {@link Jobticket} object.
     * @throws JAXBException
     *             When JAXB error.
     */
    public static Jobticket createFromXml(final String xml)
            throws JAXBException {
        final JAXBContext jc = JAXBContext.newInstance(Jobticket.class);
        final Unmarshaller u = jc.createUnmarshaller();
        StringReader reader = new StringReader(xml);
        return (Jobticket) u.unmarshal(reader);
    }

}
