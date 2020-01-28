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

import java.net.InetAddress;

/**
 * Check if a single IPv4 or IPv6 address is in range of a CIDR.
 * <p>
 * See this <a href=
 * "https://stackoverflow.com/questions/577363/how-to-check-if-an-ip-address-is-from-a-particular-network-netmask-in-java/">stackoverflow</a>
 * question.
 * </p>
 */
public final class CidrChecker {

    /**
     * Number of bits in byte.
     */
    private static final int BITS_IN_BYTE = 8;

    /** */
    private static final String CIDR_SPLIT_STRING = "/";

    /** */
    private final InetAddress inetAddressCidr;

    /** */
    private final byte[] inetAdressCidrBytes;

    /**
     * Number of bits in IP mask.
     */
    private final int maskBits;

    /**
     * Number of bytes in IP mask.
     */
    private final int maskBytes;

    /**
     * @param ipAddress
     *            IP address.
     * @return {@link InetAddress}
     * @throws IllegalArgumentException
     *             If IP address is invalid.
     */
    private static InetAddress createInetAddress(final String ipAddress) {
        try {
            return InetAddress.getByName(ipAddress);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    String.format("IP %s is invalid.", ipAddress));
        }
    }

    /**
     *
     * @param cidr
     *            CIDR.
     * @throws IllegalArgumentException
     *             If IP or CIDR is invalid.
     */
    public CidrChecker(final String cidr) {

        if (cidr.indexOf(CIDR_SPLIT_STRING.charAt(0)) == 0) {
            throw new IllegalArgumentException(
                    String.format("CIDR %s is invalid.", cidr));
        }

        final String[] addressAndMask = cidr.split(CIDR_SPLIT_STRING);

        if (addressAndMask.length > 2) {
            throw new IllegalArgumentException(
                    String.format("CIDR %s is invalid.", cidr));
        }

        final String ipAddressCidr = addressAndMask[0];
        this.maskBits = Integer.parseInt(addressAndMask[1]);
        this.maskBytes = this.maskBits / BITS_IN_BYTE;

        this.inetAddressCidr = createInetAddress(ipAddressCidr);
        this.inetAdressCidrBytes = inetAddressCidr.getAddress();

        if (this.maskBytes > this.inetAdressCidrBytes.length) {
            throw new IllegalArgumentException(
                    String.format("CIDR %s is invalid.", cidr));
        }
    }

    /**
     * Checks if single IP address is in range of CIDR.
     *
     * @param ipAddress
     *            Single IP address to check.
     * @return {@code true} if IP address is in range.
     */
    public boolean isInRange(final String ipAddress) {

        final InetAddress inetAddr = createInetAddress(ipAddress);

        // Check Inet4Address/Inet6Address compatibility.
        if (!this.inetAddressCidr.getClass().equals(inetAddr.getClass())) {
            return false;
        }

        final byte[] inetAddrBytes = inetAddr.getAddress();

        // Check index bound.
        if (this.maskBytes > inetAddrBytes.length) {
            return false;
        }

        final byte finalByte = (byte) (0xFF00 >> (this.maskBits & 0x07));

        for (int i = 0; i < this.maskBytes; i++) {
            if (inetAddrBytes[i] != this.inetAdressCidrBytes[i]) {
                return false;
            }
        }

        if (finalByte != 0) {
            return (inetAddrBytes[this.maskBytes]
                    & finalByte) == (this.inetAdressCidrBytes[maskBytes]
                            & finalByte);
        }
        return true;
    }

}