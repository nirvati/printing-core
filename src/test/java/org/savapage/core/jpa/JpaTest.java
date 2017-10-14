/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2017 Datraverse B.V.
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.core.jpa;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.junit.Test;
import org.savapage.core.dao.enums.UserAttrEnum;
import org.savapage.core.jpa.tools.DbTools;
import org.savapage.core.jpa.xml.XAccountAttrV01;
import org.savapage.core.util.XmlParseHelper;

/**
 *
 * @author Rijk Ravestein
 *
 */
public class JpaTest {

    @Test
    public void testUserAttrEnum() {

        for (final UserAttrEnum attr : UserAttrEnum.values()) {
            assertTrue(UserAttrEnum.asEnum(attr.getName()) == attr);
        }
    }

    @Test
    public void testDbTools() throws ClassNotFoundException {

        final Class<?> testClass = XAccountAttrV01.class;

        assertTrue(DbTools.getEntityClassFromXmlAttr(testClass.getSimpleName())
                .getName().equals(testClass.getName()));

        assertTrue(DbTools
                .getEntityClassFromXmlAttr(
                        "com.example." + testClass.getSimpleName())
                .getName().equals(testClass.getName()));

    }

    /**
     * A test for Mantis #512.
     *
     * @throws Exception
     */
    @Test
    public void testXmlValues() throws Exception {

        /*
         * row[i][0] is the input, row[i][1] is the expected output when writing
         * to XML and reading with a SAX parser.
         */
        final String[][] row = { //
                { "row\0", "row" }, //
                { "tab\t", "tab\t" }, //
                { "val\u0019", "val" } //
        };

        /*
         * XML document + root element
         */
        final ByteArrayOutputStream bostr = new ByteArrayOutputStream();

        final XMLOutputFactory factoryOut = XMLOutputFactory.newInstance();
        final XMLStreamWriter writer =
                factoryOut.createXMLStreamWriter(bostr, "UTF-8");

        writer.writeStartDocument("UTF-8", "1.0");
        writer.writeStartElement("root");

        /*
         * Input of row elements with invalid XML content.
         */
        for (int i = 0; i < row.length; i++) {
            writer.writeStartElement("row");
            writer.writeCharacters(
                    XmlParseHelper.removeIllegalChars(row[i][0]));
            writer.writeEndElement();
        }

        writer.writeEndElement();
        writer.writeEndDocument();
        writer.close();

        /*
         * Read the document.
         */
        final ByteArrayInputStream bistr =
                new ByteArrayInputStream(bostr.toByteArray());

        final XMLInputFactory factoryIn = XMLInputFactory.newInstance();
        final XMLStreamReader reader = factoryIn.createXMLStreamReader(bistr);

        reader.next(); // root element
        int readerPosition = reader.next(); // first row

        int i = 0;

        while (readerPosition == XMLStreamReader.START_ELEMENT) {

            readerPosition = reader.next();

            final StringBuilder result = new StringBuilder();

            while (readerPosition == XMLStreamReader.CHARACTERS) {
                result.append(reader.getText());
                readerPosition = reader.next();
            }

            final String value = result.toString();
            assertTrue(value.equals(row[i][1]));
            i++;

            readerPosition = reader.next();
        }
    }

}
