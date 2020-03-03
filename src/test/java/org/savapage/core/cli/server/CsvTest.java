/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2020 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: 2011-2020 Datraverse B.V. <info@datraverse.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
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
package org.savapage.core.cli.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.Charset;

import org.junit.Test;

import au.com.bytecode.opencsv.CSVReader;

/**
 *
 * @author Datraverse B.V.
 *
 */
public class CsvTest {

    /**
     *
     * @throws IOException
     */
    @Test
    public final void testEmptyValues() throws IOException {

        char separator = ',';

        String s = "1" + separator + separator + "3";

        StringReader sr = new StringReader(s);

        CSVReader csvReader = new CSVReader(sr, separator);
        String[] row = null;

        row = csvReader.readNext();

        assertEquals(row[0], "1");
        assertEquals(row[1], ""); // empty, not NULL
        assertEquals(row[2], "3");

        csvReader.close();
    }

    /**
     *
     * @throws IOException
     */
    @Test
    public void testFormat() throws IOException {
        testFormat("\r\n", '\t');
        testFormat("\r\n", ',');
        testFormat("\r", ',');
        testFormat("\r", '\t');
    }

    /**
     *
     * @param newLine
     * @param separator
     * @throws IOException
     */
    public final void testFormat(final String newLine, final char separator)
            throws IOException {

        final String content11 = "Jon" + separator + " \"maddog\" Hall";

        final String cell11 = "\"" + content11 + "\"";
        final String cell12 = "1.2";
        final String cell21 = "2.1";
        final String cell22 = "2.2";

        String s = cell11 + separator + cell12 + newLine;
        s += cell21 + separator + cell22;

        StringReader sr = new StringReader(s);

        CSVReader csvReader = new CSVReader(sr, separator);
        String[] row = null;

        row = csvReader.readNext();
        assertEquals(row[0], content11);
        assertEquals(row[1], cell12);

        row = csvReader.readNext();
        assertEquals(row[0], cell21);
        assertEquals(row[1], cell22);

        row = csvReader.readNext();
        assertNull(row);

        csvReader.close();
    }

    /**
     *
     * @throws IOException
     */
    @Test
    public final void testCharSet() throws IOException {
        testCharSet("/utf-8.csv", "utf-8", true);
        testCharSet("/iso8859-1.csv", "iso8859-1", false);
        testCharSet("/windows-1252.csv", "windows-1252", false);
    }

    /**
     *
     * @param filePath
     * @param charsetName
     * @param isCharsetExtended
     * @throws IOException
     */
    private void testCharSet(final String filePath, final String charsetName,
            boolean isCharsetExtended) throws IOException {

        InputStream is = getClass().getResourceAsStream(filePath);

        assertNotNull("Test file [" + filePath + "] missing", is);

        if (!Charset.isSupported(charsetName)) {
            fail("Charset [" + charsetName + "] is not supported.");
        }

        Reader reader = new InputStreamReader(is, charsetName);

        final char separator = ',';

        CSVReader csvReader = new CSVReader(reader, separator);
        String[] row = csvReader.readNext();

        assertEquals(row[0], "Ä");
        assertEquals(row[1], "Æ");
        if (isCharsetExtended) {
            assertEquals(row[2], "Ψ");
        } else {
            // Character "Ψ" is not available in 'charsetName'.
            assertFalse(row[2].equals("Ψ"));
        }

        csvReader.close();
    }

}
