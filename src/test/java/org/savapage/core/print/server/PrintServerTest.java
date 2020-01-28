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
package org.savapage.core.print.server;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.savapage.core.util.InetUtils;

/**
 *
 * @author Rijk Ravestein
 *
 */
public class PrintServerTest {

    /**
     * Testing IPv4 addresses.
     */
    @Test
    public void testIPv4() {

        assertTrue(InetUtils.isIpAddrInCidrRanges("", "192.168.1.35"));

        assertTrue(InetUtils.isIpAddrInCidrRanges("192.168.1.35/32",
                "192.168.1.35"));

        assertFalse(InetUtils.isIpAddrInCidrRanges("192.168.1.35/32",
                "192.168.1.36"));

        assertTrue(InetUtils.isIpAddrInCidrRanges(
                "192.168.1.36/32 ; 192.168.1.35/32", "192.168.1.35"));

        assertTrue(InetUtils.isIpAddrInCidrRanges(
                "192.168.1.36/32 , 192.168.1.35/32", "192.168.1.36"));

        assertFalse(InetUtils.isIpAddrInCidrRanges(
                "192.168.1.36/32 192.168.1.35/32", "192.168.1.37"));

        assertFalse(InetUtils.isIpAddrInCidrRanges(
                "2021:661:4cc6:1:5561:96ff:ca11:d4a/64", "192.168.1.37"));

        /*
         * http://www.subnet-calculator.com/cidr.php
         *
         * 172.16.0.0/24 represents the given IPv4 address and its associated
         * routing prefix 172.16.0.0, or equivalently, its subnet mask
         * 255.255.255.0. This represents the host address range 172.16.0.0 -
         * 172.16.0.255.
         */
        assertTrue(
                InetUtils.isIpAddrInCidrRanges("172.16.0.0/24", "172.16.0.0"));

        assertTrue(
                InetUtils.isIpAddrInCidrRanges("172.16.0.0/24", "172.16.0.1"));

        assertTrue(InetUtils.isIpAddrInCidrRanges("172.16.0.0/24",
                "172.16.0.254"));

        assertTrue(InetUtils.isIpAddrInCidrRanges("172.16.0.0/24",
                "172.16.0.255"));

        /*
         * 172.16.0.0/27 : 172.16.0.0 - 172.16.0.31
         */
        assertTrue(
                InetUtils.isIpAddrInCidrRanges("172.16.0.0/27", "172.16.0.0"));

        assertTrue(
                InetUtils.isIpAddrInCidrRanges("172.16.0.0/27", "172.16.0.31"));

        assertFalse(
                InetUtils.isIpAddrInCidrRanges("172.16.0.0/27", "172.16.0.32"));
    }

    /**
     * Testing IPv6 addresses.
     */
    @Test
    public void testIPv6() {

        assertTrue(InetUtils.isIpAddrInCidrRanges("",
                "2021:661:4cc6:1:5561:96ff:ca11:d4a"));

        assertTrue(InetUtils.isIpAddrInCidrRanges(
                "2021:661:4cc6:1:5561:96ff:ca11:d4a/128",
                "2021:661:4cc6:1:5561:96ff:ca11:d4a"));

        assertFalse(InetUtils.isIpAddrInCidrRanges(
                "2021:661:4cc6:1:5561:96ff:ca11:d4a/128",
                "2021:661:4cc6:1:5561:96ff:ca11:ccc"));

        assertTrue(InetUtils.isIpAddrInCidrRanges(
                "2099:661:4cc6:1:5561:96ff:ca11:d4a/128"
                        + " , 2021:661:4cc6:1:5561:96ff:ca11:d4a/128",
                "2021:661:4cc6:1:5561:96ff:ca11:d4a"));

        assertTrue(InetUtils.isIpAddrInCidrRanges(
                "2021:661:4cc6:1:5561:96ff:ca11:d4a/128"
                        + " 2099:661:4cc6:1:5561:96ff:ca11:d4a/128",
                "2021:661:4cc6:1:5561:96ff:ca11:d4a"));

        assertFalse(InetUtils.isIpAddrInCidrRanges(
                "2021:661:4cc6:1:5561:96ff:ca11:d4a/128"
                        + " 2099:661:4cc6:1:5561:96ff:ca11:d4a/128",
                "3021:661:4cc6:1:5561:96ff:ca11:d4a"));

        // mixed
        assertFalse(InetUtils.isIpAddrInCidrRanges("192.168.1.1/8",
                "2021:661:4cc6:1:5561:96ff:ca11:d4a"));

        assertTrue(InetUtils.isIpAddrInCidrRanges(
                "192.168.1.1/8 , 2021:661:4cc6:1:5561:96ff:ca11:d4a/64",
                "2021:661:4cc6:1:5561:96ff:ca11:d4a"));

        /*
         * https://dnschecker.org/ipv6-cidr-to-range.php
         */
        assertTrue(InetUtils.isIpAddrInCidrRanges(
                "2021:661:4cc6:1:5561:96ff::/96",
                "2021:661:4cc6:1:5561:96ff:ca11:d4a"));

        assertFalse(InetUtils.isIpAddrInCidrRanges(
                "2021:661:4cc6:1:5561:96ff::/96",
                "2021:661:4cc6:1:5561:96ef:ca11:d4a"));

        // first in range
        assertTrue(InetUtils.isIpAddrInCidrRanges(
                "2021:661:4cc6:1:5561:96ff:ca11:d00/120",
                "2021:661:4cc6:1:5561:96ff:ca11:d00"));
        // last in range
        assertTrue(InetUtils.isIpAddrInCidrRanges(
                "2021:661:4cc6:1:5561:96ff:ca11:d00/120",
                "2021:661:4cc6:1:5561:96ff:ca11:dff"));
        // out of range
        assertFalse(InetUtils.isIpAddrInCidrRanges(
                "2021:661:4cc6:1:5561:96ff:ca11:d00/120",
                "2021:661:4cc6:1:5561:96ff:ca11:cff"));
        // out of range
        assertFalse(InetUtils.isIpAddrInCidrRanges(
                "2021:661:4cc6:1:5561:96ff:ca11:d00/120",
                "2021:661:4cc6:1:5561:96ff:ca11:eff"));

        // first in range
        assertTrue(InetUtils.isIpAddrInCidrRanges(
                "2021:661:4cc6:1:5561:96ff:ca11:c00/119",
                "2021:661:4cc6:1:5561:96ff:ca11:c00"));
        // last in range
        assertTrue(InetUtils.isIpAddrInCidrRanges(
                "2021:661:4cc6:1:5561:96ff:ca11:c00/119",
                "2021:661:4cc6:1:5561:96ff:ca11:dff"));

    }
}
