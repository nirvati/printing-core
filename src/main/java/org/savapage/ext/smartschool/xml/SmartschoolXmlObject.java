/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2016 Datraverse B.V.
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

import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

/**
 * Smartschool XML Object.
 *
 * @author Rijk Ravestein
 *
 */
public abstract class SmartschoolXmlObject {

    /**
     * Serializes the object to an XML string.
     *
     * @return The XML string.
     * @throws JAXBException
     *             When XML serialization goes wrong.
     */
    public final String asXmlString() throws JAXBException {
        final StringWriter writer = new StringWriter();
        this.writeXml(writer);
        return writer.toString();
    }

    /**
     * Serializes the object to XML to a {@link Writer}.
     *
     * @param writer
     *            The {@link Writer} to serialize to.
     * @throws JAXBException
     *             When XML serialization goes wrong.
     */
    public final void writeXml(final Writer writer) throws JAXBException {
        final JAXBContext jc = JAXBContext.newInstance(this.getClass());
        final Marshaller m = jc.createMarshaller();
        m.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
        m.marshal(this, writer);
    }

    /**
     * Creates a typed {@link SmartschoolXmlObject} object from a XML string.
     *
     * @param <E>
     *            The {@link SmartschoolXmlObject} object.
     * @param clazz
     *            The {@link SmartschoolXmlObject} subclass.
     * @param xml
     *            The XML string.
     * @return The instance.
     * @throws JAXBException
     *             When JAXB error.
     */
    public static <E extends SmartschoolXmlObject> E create(
            final Class<E> clazz, final String xml) throws JAXBException {
        return create(clazz, new StringReader(xml));
    }

    /**
     * Creates a typed {@link SmartschoolXmlObject} object from a XML string.
     *
     * @param <E>
     *            The {@link SmartschoolXmlObject} object.
     * @param clazz
     *            The {@link SmartschoolXmlObject} subclass.
     * @param reader
     *            The XML {@link Reader}.
     * @return The instance.
     * @throws JAXBException
     *             When JAXB error.
     */
    @SuppressWarnings("unchecked")
    public static <E extends SmartschoolXmlObject> E create(
            final Class<E> clazz, final Reader reader) throws JAXBException {

        final JAXBContext jc = JAXBContext.newInstance(clazz);
        final Unmarshaller u = jc.createUnmarshaller();
        return (E) u.unmarshal(reader);
    }

}
