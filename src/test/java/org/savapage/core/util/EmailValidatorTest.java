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
package org.savapage.core.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 *
 * @author Rijk Ravestein
 *
 */
public class EmailValidatorTest {

    @Test
    public void testValid() {
        String[] arrayValid = new String[] { "john@example.com",
                "john-100@example.com", "john.100@example.com",
                "john111@john.com", "john-100@john.net", "john.100@john.com.au",
                "john@1.com", "john@mymail.com.com", "john+100@mymail.com",
                "john-100@example-test.com", "john@xxx.mymail.com",
                "john123@mymail.a", "john..2002@mymail.com", ".john@john.com",
                "john.@mymail.com" };

        for (String temp : arrayValid) {
            boolean valid = EmailValidator.validate(temp);
            // System.out.println("Email is valid : " + temp + " , " + valid);
            assertEquals(valid, true);
        }
    }

    @Test
    public void testInvalid() {
        String[] emails = new String[] { "john", "john@.com.my", "john123@.com",
                "john123@.com.com", "john()*@mymail.com", "john@%*.com",
                "john@john@mymail.com" };

        for (String temp : emails) {
            boolean valid = EmailValidator.validate(temp);
            // System.out.println("Email is valid : " + temp + " , " + valid);
            assertEquals(valid, false);
        }
    }

}
