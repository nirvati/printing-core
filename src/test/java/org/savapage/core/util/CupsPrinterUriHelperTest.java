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

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Test;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class CupsPrinterUriHelperTest {

    @Test
    public void test1() throws URISyntaxException {

        final String[][] data = { //
                { "socket://192.168.1.190", "192.168.1.190" }, //
                { "papercut:socket://192.168.1.190", "192.168.1.190" }, //
                { "savaspool:/tmp/savaspool-cn-ir-adv-5535", null }, //
                { "cups-pdf:/", null }, //
                { "file:///dev/null", null }, //
                { "papercut:file:///dev/null", null }, //
                { "ipp://localhost:8080/printers/public", "localhost" }, //
                { "hp:/net/ENVY_7640_series?ip=192.168.1.190",
                        "192.168.1.190" }, //
        };

        for (final String[] item : data) {
            assertEquals(CupsPrinterUriHelper.resolveHost(new URI(item[0])),
                    item[1]);
        }

    }
}
