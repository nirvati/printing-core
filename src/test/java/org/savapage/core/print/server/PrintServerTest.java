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
package org.savapage.core.print.server;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.savapage.core.util.InetUtils;

/**
 *
 * @author Datraverse B.V.
 *
 */
public class PrintServerTest {

    @Test
    public void test() {

        assertTrue(InetUtils.isIp4AddrInCidrRanges("192.168.1.35/32",
                "192.168.1.35"));

        assertFalse(InetUtils.isIp4AddrInCidrRanges("192.168.1.35/32",
                "192.168.1.36"));

        assertTrue(InetUtils.isIp4AddrInCidrRanges(
                "192.168.1.36/32 ; 192.168.1.35/32", "192.168.1.35"));

        assertTrue(InetUtils.isIp4AddrInCidrRanges(
                "192.168.1.36/32 , 192.168.1.35/32", "192.168.1.36"));

        assertFalse(InetUtils.isIp4AddrInCidrRanges(
                "192.168.1.36/32:192.168.1.35/32", "192.168.1.37"));

        /*
         * http://www.subnet-calculator.com/cidr.php
         *
         * 172.16.0.0/24 represents the given IPv4 address and its associated
         * routing prefix 172.16.0.0, or equivalently, its subnet mask
         * 255.255.255.0. This represents the host address range 172.16.0.0 -
         * 172.16.0.255.
         */
        assertTrue(InetUtils.isIp4AddrInCidrRanges("172.16.0.0/24",
                "172.16.0.0"));

        assertTrue(InetUtils.isIp4AddrInCidrRanges("172.16.0.0/24",
                "172.16.0.1"));

        assertTrue(InetUtils.isIp4AddrInCidrRanges("172.16.0.0/24",
                "172.16.0.254"));

        assertTrue(InetUtils.isIp4AddrInCidrRanges("172.16.0.0/24",
                "172.16.0.255"));

        /*
         * 172.16.0.0/27 : 172.16.0.0 - 172.16.0.31
         */
        assertTrue(InetUtils.isIp4AddrInCidrRanges("172.16.0.0/27",
                "172.16.0.0"));

        assertTrue(InetUtils.isIp4AddrInCidrRanges("172.16.0.0/27",
                "172.16.0.31"));

        assertFalse(InetUtils.isIp4AddrInCidrRanges("172.16.0.0/27",
                "172.16.0.32"));

    }

}
