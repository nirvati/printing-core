/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2018 Datraverse B.V.
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.core.util;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.util.SubnetUtils;
import org.savapage.core.SpException;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class InetUtils {

    /**
     * Prefix of local-loop IP addresses.
     * <p>
     * Note: Debian based distros have 127.0.0.1 (localhost) and 127.0.1.1
     * (host_name.domain_name host_name) defined in {@code /etc/hosts}
     * </p>
     */
    private static final String IP_LOOP_BACK_ADDR_PREFIX = "127.0.";

    /**
     *
     */
    private static final String IP_LOOP_BACK_ADDR = "127.0.0.1";

    /**
     * No public instantiation.
     */
    private InetUtils() {
    }

    /**
     * Checks if localhost port is in use.
     *
     * @param port
     *            The IP port.
     * @return {@code true} when in us.
     */
    public static boolean isPortInUse(int port) {
        boolean inUse = true;
        Socket socket = null;
        try {
            socket = new Socket(IP_LOOP_BACK_ADDR, port);
        } catch (Exception e) {
            inUse = false;
        } finally {
            IOUtils.closeQuietly(socket);
        }
        return inUse;
    }

    /**
     * Gets the {@code hostname} of the host system this application is running
     * on.
     *
     * @return The hostname.
     */
    public static String getServerHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            throw new SpException(e.getMessage(), e);
        }
    }

    /**
     * Gets the assigned (static or dynamic) IPv4 address (no loop back address)
     * of the host system this application is running on, or the loop back
     * address when no assigned address is found.
     *
     * @return The local host IPv4 address.
     * @throws UnknownHostException
     *             When non-loop IPv4 address could not be found or I/O errors
     *             are encountered when. getting the network interfaces.
     */
    public static String getServerHostAddress() throws UnknownHostException {

        final String ipAddress = InetAddress.getLocalHost().getHostAddress();

        if (!ipAddress.startsWith(IP_LOOP_BACK_ADDR_PREFIX)) {
            return ipAddress;
        }

        /*
         * Traverse all network interfaces on this machine.
         */
        final Enumeration<NetworkInterface> networkEnum;

        try {
            networkEnum = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            throw new UnknownHostException(e.getMessage());
        }

        while (networkEnum != null && networkEnum.hasMoreElements()) {

            final NetworkInterface inter = networkEnum.nextElement();

            /*
             * Traverse all addresses for this interface.
             */
            final Enumeration<InetAddress> enumAddr = inter.getInetAddresses();

            while (enumAddr.hasMoreElements()) {

                final InetAddress addr = enumAddr.nextElement();

                /*
                 * IPv4 addresses only.
                 */
                if (addr instanceof Inet4Address) {

                    if (!addr.getHostAddress()
                            .startsWith(IP_LOOP_BACK_ADDR_PREFIX)) {
                        /*
                         * Bingo, this is a non-loop back address.
                         */
                        return addr.getHostAddress();
                    }
                }
            }
        }

        /*
         * No non-loop back IP v4 addresses found: return loop back address.
         */
        return IP_LOOP_BACK_ADDR;
    }

    /**
     * Checks if an IPv4 address in range of a collection of CIDRs.
     * <p>
     * Note: "127.0.0.1" is considered in range.
     * </p>
     *
     * @param cidrRanges
     *            CIDRs separated by any of the characters ' ' (space), ','
     *            (comma), ':' (colon) or ';' (semicolon).
     * @param ipAddr
     *            The IPv4 address.
     * @return {@code true} if in range of at least one (1) CIDR.
     */
    public static boolean isIp4AddrInCidrRanges(final String cidrRanges,
            final String ipAddr) {

        boolean inrange =
                StringUtils.isBlank(cidrRanges) || ipAddr.equals("127.0.0.1");

        if (!inrange) {

            for (final String cidr : StringUtils.split(cidrRanges, " ,;:")) {

                final SubnetUtils utils = new SubnetUtils(cidr);

                /*
                 * setInclusiveHostCount(true) makes CIDR 192.168.1.43/32 a
                 * range of just one (1) IP address.
                 */
                utils.setInclusiveHostCount(true);

                final SubnetUtils.SubnetInfo info = utils.getInfo();
                inrange = info.isInRange(ipAddr);

                if (inrange) {
                    break;
                }
            }
        }

        return inrange;
    }

    /**
     * Checks if an IPv4 address is a public (global) IP address.
     *
     * @param ip
     *            The IPv4 address.
     * @return {@code true} if address is a public.
     */
    public static boolean isPublicAddress(final String ip) {

        final Inet4Address address;

        try {

            address = (Inet4Address) InetAddress.getByName(ip);

        } catch (UnknownHostException exception) {
            return false;
        }

        return !(address.isSiteLocalAddress() || address.isAnyLocalAddress()
                || address.isLinkLocalAddress() || address.isLoopbackAddress()
                || address.isMulticastAddress());
    }

}
