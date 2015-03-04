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

import org.junit.Test;
import org.savapage.core.util.EmailValidator;

/**
 *
 * Adapted from <a href=
 * "http://www.mkyong.com/regular-expressions/how-to-validate-email-address-with-regular-expression/"
 * >www.mkyong.com<a/>
 *
 * @author Datraverse B.V.
 *
 */
public class EmailValidatorTest {

    private final EmailValidator emailValidator = new EmailValidator();

    @Test
    public void testValid() {
        String[] arrayValid =
                new String[] { "mkyong@yahoo.com", "mkyong-100@yahoo.com",
                        "mkyong.100@yahoo.com", "mkyong111@mkyong.com",
                        "mkyong-100@mkyong.net", "mkyong.100@mkyong.com.au",
                        "mkyong@1.com", "mkyong@gmail.com.com",
                        "mkyong+100@gmail.com", "mkyong-100@yahoo-test.com" };

        for (String temp : arrayValid) {
            boolean valid = emailValidator.validate(temp);
            // System.out.println("Email is valid : " + temp + " , " + valid);
            assertEquals(valid, true);
        }
    }

    @Test
    public void testInvalid() {
        String[] emails =
                new String[] { "mkyong", "mkyong@.com.my", "mkyong123@gmail.a",
                        "mkyong123@.com", "mkyong123@.com.com",
                        ".mkyong@mkyong.com", "mkyong()*@gmail.com",
                        "mkyong@%*.com", "mkyong..2002@gmail.com",
                        "mkyong.@gmail.com", "mkyong@mkyong@gmail.com",
                        "mkyong@gmail.com.1a" };

        for (String temp : emails) {
            boolean valid = emailValidator.validate(temp);
            // System.out.println("Email is valid : " + temp + " , " + valid);
            assertEquals(valid, false);
        }
    }

}
