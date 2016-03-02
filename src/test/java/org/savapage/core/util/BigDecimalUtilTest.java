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
package org.savapage.core.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.Locale;

import org.junit.Test;

/**
 *
 * @author Datraverse B.V.
 *
 */
public class BigDecimalUtilTest {

    final static String EURO = "€";

    @Test
    public void test1() throws ParseException {

        final String decimal = "2.343.298,09324798";
        final String decimalCurrency = EURO + " " + decimal;
        final Locale locale = new Locale("nl", "NL");

        BigDecimal bigDecimal;

        bigDecimal = BigDecimalUtil.parse(decimal, locale, false, true);
        assertEquals(BigDecimalUtil.localize(bigDecimal, 8, locale, true),
                decimal);

        bigDecimal = BigDecimalUtil.parse(decimalCurrency, locale, true, true);
        assertEquals(
                BigDecimalUtil.localize(bigDecimal, 8, locale, true, true),
                decimalCurrency);

    }

    @Test
    public void test1a() throws ParseException {

        final String decimal = "2343298,09324798";
        final String decimalCurrency = EURO + " " + decimal;
        final Locale locale = new Locale("nl", "NL");

        BigDecimal bigDecimal;

        bigDecimal = BigDecimalUtil.parse(decimal, locale, false, false);
        assertEquals(BigDecimalUtil.localize(bigDecimal, 8, locale, false),
                decimal);

        bigDecimal = BigDecimalUtil.parse(decimalCurrency, locale, true, false);
        assertEquals(
                BigDecimalUtil.localize(bigDecimal, 8, locale, true, false),
                decimalCurrency);

    }

    @Test
    public void test2() throws ParseException {

        BigDecimal bigDecimal;
        String strDecimal;

        final Locale locale = new Locale("nl", "NL");

        bigDecimal = new BigDecimal("10.15");
        strDecimal = BigDecimalUtil.localize(bigDecimal, 2, locale, true, true);

        assertEquals(strDecimal, EURO + " 10,15");

    }

    @Test
    public void test3() throws ParseException {

        BigDecimal bigDecimal;
        String strDecimal;

        final Locale locale = new Locale("nl");

        bigDecimal = new BigDecimal("10.15");
        strDecimal = BigDecimalUtil.localize(bigDecimal, 2, locale, false);

        assertEquals(strDecimal, "10,15");

    }

    @Test
    public void test4() throws ParseException {

        assertTrue(BigDecimalUtil.isValid("10.15"));
        assertFalse(BigDecimalUtil.isValid("10,15"));

    }

    @Test
    public void test5() throws ParseException {

        final Locale locale = new Locale("en");

        String strDecimal =
                BigDecimalUtil.parse("10,15", locale, false, true)
                        .toPlainString();
        assertEquals(strDecimal, "1015");

        assertTrue(BigDecimalUtil.isValid("10.15", locale, true));
        assertTrue(BigDecimalUtil.isValid("10,15", locale, true));

    }

    @Test
    public void test6() throws ParseException {

        final Locale locale = new Locale("nl");

        String strDecimal =
                BigDecimalUtil.parse("10.15", locale, false, true)
                        .toPlainString();
        assertEquals(strDecimal, "1015");

        assertTrue(BigDecimalUtil.isValid("10,15", locale, true));
        assertTrue(BigDecimalUtil.isValid("10.15", locale, true));

    }

    @Test
    public void test7() throws ParseException {

        final Locale locale = new Locale("nl");

        String strDecimal =
                BigDecimalUtil.parse("10.15", locale, false, true)
                        .toPlainString();
        assertEquals(strDecimal, "1015");

        assertTrue(BigDecimalUtil.isValid("10,15", locale, true));
        assertTrue(BigDecimalUtil.isValid("10.15", locale, true));

    }

    @Test
    public void test8() throws ParseException {

        final Locale locale = new Locale("nl");

        assertFalse(BigDecimalUtil.isValid("kanniet", locale, false));
        /*
         * Why is "10,5kanniet" and "10,kanniet,6" valid ??
         */
        // assertFalse(BigDecimalUtil.isValid("10,kanniet,6", locale, false));

    }

    @Test
    public void test9() throws NumberFormatException, ParseException {

        final Locale locale = new Locale("en");

        final String[][] strDecimalTest = {
                //
                { "-0.000001", "2", "-0.000001" },
                //
                { "0.000000", "2", "0.00" },
                //
                { "0.000000", "3", "0.000" },
                //
                { "-0.000001", "4", "-0.000001" },
                //
                { "0.000001", "3", "0.000001" },
                //
                { "-7.000001", "2", "-7.000001" },
                //
                { "10.580000", "2", "10.58" },
                //
                { "10.000000", "2", "10.00" }
        //
                };

        for (final String[] test : strDecimalTest) {
            final String localized =
                    BigDecimalUtil.localizeMinimalPrecision(new BigDecimal(
                            test[0]), Integer.valueOf(test[1]).intValue(),
                            locale, false);
            assertEquals(localized, test[2]);
        }

    }
}
