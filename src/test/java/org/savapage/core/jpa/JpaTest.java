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
package org.savapage.core.jpa;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Iterator;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.junit.Test;
import org.savapage.core.dao.helpers.UserAttrEnum;
import org.savapage.core.jpa.tools.DbTools;
import org.savapage.core.jpa.xml.XAccountAttrV01;
import org.savapage.core.util.XmlParseHelper;

/**
 *
 * @author Datraverse B.V
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
                        "com.example." + testClass.getSimpleName()).getName()
                .equals(testClass.getName()));

    }

    /**
     * A test for Mantis #512.
     *
     * @throws IOException
     * @throws DocumentException
     */
    @Test
    public void testXmlValues() throws IOException, DocumentException {

        /*
         * row[i][0] is the input, row[i][1] is the expected output when writing
         * to XML and reading with a SAX parser.
         */
        final String[][] row = {
                //
                { "row\0", "row" }
                //
                , { "tab\t", "tab\t" }
                //
                , { "val\u0019", "val" }
        //
                };

        /*
         * XML document + root element
         */
        Document document = DocumentHelper.createDocument();

        final Element rootElement = document.addElement("root");

        /*
         * Input of row elements with invalid XML content.
         */
        for (int i = 0; i < row.length; i++) {
            final Element rowElement = rootElement.addElement("row");
            rowElement.setText(XmlParseHelper.removeIllegalChars(row[i][0]));
        }

        /*
         * Write the document.
         */
        final StringWriter writer = new StringWriter();
        document.write(writer);

        /*
         * Read the document with the SAXReader.
         */
        final SAXReader saxReader = new SAXReader();

        final StringReader reader = new StringReader(writer.toString());

        document = saxReader.read(reader);

        @SuppressWarnings("unchecked")
        final Iterator<Element> iterEntity = rootElement.elementIterator();

        int i = 0;

        while (iterEntity.hasNext()) {

            final Element elementEntity = iterEntity.next();
            final String value = elementEntity.getStringValue();

            assertTrue(value.equals(row[i][1]));

            i++;
        }

    }
}
