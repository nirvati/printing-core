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
package org.savapage.core.rfid;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.savapage.core.rfid.RfidNumberFormat;

/**
 *
 * @author Datraverse B.V.
 *
 */
public class RfidNumberFormatTest {

    @Test
    public void test01() {
        RfidNumberFormat format =
                new RfidNumberFormat(RfidNumberFormat.Format.DEC,
                        RfidNumberFormat.FirstByte.MSB);
        assertEquals(format.getNormalizedNumber("4234685818"), "7a2d68fc");
    }

    @Test
    public void test02() {
        RfidNumberFormat format =
                new RfidNumberFormat(RfidNumberFormat.Format.HEX,
                        RfidNumberFormat.FirstByte.LSB);
        assertEquals(format.getNormalizedNumber("fb31b1d0"), "fb31b1d0");
    }

    @Test
    public void test03() {
        RfidNumberFormat format =
                new RfidNumberFormat(RfidNumberFormat.Format.HEX,
                        RfidNumberFormat.FirstByte.MSB);
        assertEquals(format.getNormalizedNumber("fc682d7a"), "7a2d68fc");
    }

    @Test
    public void test04() {
        RfidNumberFormat format =
                new RfidNumberFormat(RfidNumberFormat.Format.HEX,
                        RfidNumberFormat.FirstByte.MSB);
        assertEquals(format.getNormalizedNumber("FC682D7A"), "7a2d68fc");
    }

    @Test
    public void test05() {
        RfidNumberFormat format =
                new RfidNumberFormat(RfidNumberFormat.Format.HEX,
                        RfidNumberFormat.FirstByte.MSB);
        assertEquals(format.getNormalizedNumber(";FC682D7A?"), "7a2d68fc");
    }

    @Test
    public void test06() {
        RfidNumberFormat format =
                new RfidNumberFormat(RfidNumberFormat.Format.DEC,
                        RfidNumberFormat.FirstByte.MSB);
        assertEquals(format.getNormalizedNumber("*4234685818/"), "7a2d68fc");
    }

    @Test(expected = NumberFormatException.class)
    public void test07() {
        RfidNumberFormat format =
                new RfidNumberFormat(RfidNumberFormat.Format.DEC,
                        RfidNumberFormat.FirstByte.MSB);
        format.getNormalizedNumber("fc682d7a");
    }

    @Test(expected = NumberFormatException.class)
    public void test08() {
        RfidNumberFormat format =
                new RfidNumberFormat(RfidNumberFormat.Format.HEX,
                        RfidNumberFormat.FirstByte.MSB);
        format.getNormalizedNumber("gg682g7g");
    }

    @Test
    public void test09() {
        RfidNumberFormat format =
                new RfidNumberFormat(RfidNumberFormat.Format.DEC,
                        RfidNumberFormat.FirstByte.MSB);
        assertEquals(format.getNormalizedNumber("*4234685818=6/"), "7a2d68fc");
    }

    @Test(expected = NumberFormatException.class)
    public void test10() {
        RfidNumberFormat format =
                new RfidNumberFormat(RfidNumberFormat.Format.HEX,
                        RfidNumberFormat.FirstByte.MSB);
        format.getNormalizedNumber("fc682d");
    }

    @Test
    public void test11() {
        RfidNumberFormat format =
                new RfidNumberFormat(RfidNumberFormat.Format.DEC,
                        RfidNumberFormat.FirstByte.MSB);
        assertEquals(format.getNormalizedNumber("3721\\106078"), "9e92cbdd");
    }

    @Test
    public void test12() {
        RfidNumberFormat format =
                new RfidNumberFormat(RfidNumberFormat.Format.HEX,
                        RfidNumberFormat.FirstByte.MSB);
        assertEquals(format.getNormalizedNumber("ddcb929e"), "9e92cbdd");
    }

}
