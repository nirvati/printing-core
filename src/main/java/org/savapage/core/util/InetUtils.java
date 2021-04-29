/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2020 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: © 2020 Datraverse B.V. <info@datraverse.com>
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

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.util.SubnetUtils;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.ssl.SSLContextBuilder;
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

    /** */
    private static final String IPV4_LOOP_BACK_ADDR = "127.0.0.1";

    /** */
    public static final String LOCAL_HOST = "localhost";

    /** */
    private static final String LOCAL_SUFFIX = ".local";

    /**
     * Prefix of Docker bridge network interface.
     */
    private static final String NETWORKINTERFACE_NAME_PFX_DOCKER = "docker";

    /**
     * Prefix of libvirt bridge network interface.
     */
    private static final String NETWORKINTERFACE_NAME_PFX_LIBVIRT = "virbr";

    /**
     * Prefix of a tunnel network interface.
     */
    private static final String NETWORKINTERFACE_NAME_PFX_TUNNEL = "tun";

    /** */
    public static final String URL_PROTOCOL_HTTP = "http";
    /** */
    public static final String URL_PROTOCOL_HTTPS = "https";

    /**
     * A hostname verifier that always returns true. NOTE: This verifier is
     * useful for testing/debugging and should not be used in production.
     */
    static final class DebugHostVerifier implements HostnameVerifier {

        /**
         * Singleton instance.
         */
        public static final DebugHostVerifier INSTANCE =
                new DebugHostVerifier();

        @Override
        public boolean verify(final String hostname, final SSLSession session) {
            System.out.println("Checking: " + hostname + " in");
            try {
                final Certificate[] cert = session.getPeerCertificates();
                for (int i = 0; i < cert.length; i++) {
                    System.out.println(cert[i]);
                }
            } catch (SSLPeerUnverifiedException e) {
                return false;
            }
            return true;
        }
    }

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
    public static boolean isPortInUse(final int port) {
        boolean inUse;
        try (Socket socket = new Socket(IPV4_LOOP_BACK_ADDR, port);) {
            inUse = true;
        } catch (Exception e) {
            inUse = false;
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
     * Gets the assigned (static or dynamic) IPv4 addresses (no loop back
     * address) of the host system this application is running on.
     *
     * @return Set of local host IPv4 (no loop back) address.
     * @throws UnknownHostException
     *             When non-loop IPv4 address could not be found or I/O errors
     *             are encountered when getting the network interfaces.
     */
    public static Set<String> getServerHostAddresses()
            throws UnknownHostException {

        final Set<String> ipAddrSet = new HashSet<>();

        // Traverse all network interfaces on this machine.
        final Enumeration<NetworkInterface> networkEnum;

        try {
            networkEnum = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            throw new UnknownHostException(e.getMessage());
        }

        while (networkEnum != null && networkEnum.hasMoreElements()) {

            final NetworkInterface inter = networkEnum.nextElement();

            final String nicName = inter.getName().toLowerCase();

            if (nicName.startsWith(NETWORKINTERFACE_NAME_PFX_DOCKER)
                    || nicName.startsWith(NETWORKINTERFACE_NAME_PFX_LIBVIRT)
                    || nicName.startsWith(NETWORKINTERFACE_NAME_PFX_TUNNEL)) {
                continue;
            }

            // Traverse all addresses for this interface.
            final Enumeration<InetAddress> enumAddr = inter.getInetAddresses();

            while (enumAddr.hasMoreElements()) {
                final InetAddress addr = enumAddr.nextElement();
                // IPv4 addresses only.
                if (addr instanceof Inet4Address) {
                    if (!addr.getHostAddress()
                            .startsWith(IP_LOOP_BACK_ADDR_PREFIX)) {
                        ipAddrSet.add(addr.getHostAddress());
                    }
                }
            }
        }
        return ipAddrSet;
    }

    /**
     * Check if IP address is (one of) the server's IP address(es).
     *
     * @param ipAddress
     *            IP address to check.
     * @return {@code true} when server IP address.
     * @throws UnknownHostException
     *             If error getting the network interfaces.
     */
    public static boolean isServerHostAddress(final String ipAddress)
            throws UnknownHostException {

        for (final String addr : getServerHostAddresses()) {
            if (addr.equals(ipAddress)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the assigned (static or dynamic) IPv4 address (no loop back address)
     * of the host system this application is running on, or the loop back
     * address when no assigned address is found.
     *
     * @return The local host IPv4 address.
     * @throws UnknownHostException
     *             When non-loop IPv4 address could not be found or I/O errors
     *             are encountered when getting the network interfaces.
     */
    public static String getServerHostAddress() throws UnknownHostException {

        final String ipAddress = InetAddress.getLocalHost().getHostAddress();

        if (!ipAddress.startsWith(IP_LOOP_BACK_ADDR_PREFIX)) {
            return ipAddress;
        }

        final Set<String> serverAddresses = getServerHostAddresses();

        if (serverAddresses.isEmpty()) {
            // No non-loop back address found: return loop back address.
            return IPV4_LOOP_BACK_ADDR;
        }
        // Get the first one.
        return serverAddresses.iterator().next();
    }

    /**
     * Checks if an IPv4 or IPv6 address is in range of a collection of CIDRs.
     * <p>
     * Note: "127.0.0.1" is considered in range.
     * </p>
     *
     * @param cidrRanges
     *            CIDRs separated by any of the characters ' ' (space), ','
     *            (comma) or ';' (semicolon).
     * @param ipAddr
     *            The IPv4 or IPv6 address.
     * @return {@code true} if in range of at least one (1) CIDR.
     */
    public static boolean isIpAddrInCidrRanges(final String cidrRanges,
            final String ipAddr) {

        final String ipAddrWrk = ipAddr.trim();

        boolean inrange = StringUtils.isBlank(cidrRanges)
                || ipAddrWrk.equals(IPV4_LOOP_BACK_ADDR);

        if (!inrange) {
            for (final String cidr : StringUtils.split(cidrRanges, " ,;")) {
                inrange = new CidrChecker(cidr.trim()).isInRange(ipAddrWrk);
                if (inrange) {
                    break;
                }
            }
        }
        return inrange;
    }

    /**
     * @param cidrSet
     *            Set of CIDR ranges.
     * @return {@code true} if valid.
     */
    public static boolean isCidrSetValid(final String cidrSet) {
        try {
            /*
             * Probe with a non-localhost address.
             */
            isIpAddrInCidrRanges(cidrSet, "10.0.0.1");
            return true;
        } catch (final Throwable thr) {
            return false;
        }
    }

    /**
     * @param ipAddress
     *            IP address.
     * @return {@code true} if valid.
     */
    public static boolean isInetAddressValid(final String ipAddress) {
        try {
            InetAddress.getByName(ipAddress);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Checks if an IPv4 address in range of a collection of CIDRs.
     * <p>
     * Note: "127.0.0.1" is considered in range.
     * </p>
     *
     * @deprecated Use {@link #isIpAddrInCidrRanges(String, String)} instead.
     *
     * @param cidrRanges
     *            CIDRs separated by any of the characters ' ' (space), ','
     *            (comma), ':' (colon) or ';' (semicolon).
     * @param ipAddr
     *            The IPv4 address.
     * @return {@code true} if in range of at least one (1) CIDR.
     */
    @Deprecated
    public static boolean isIp4AddrInCidrRanges(final String cidrRanges,
            final String ipAddr) {

        final String ipAddrWrk = ipAddr.trim();

        boolean inrange = StringUtils.isBlank(cidrRanges)
                || ipAddrWrk.equals(IPV4_LOOP_BACK_ADDR);

        if (!inrange) {

            for (final String cidr : StringUtils.split(cidrRanges, " ,;:")) {

                final SubnetUtils utils = new SubnetUtils(cidr);

                /*
                 * setInclusiveHostCount(true) makes CIDR 192.168.1.43/32 a
                 * range of just one (1) IP address.
                 */
                utils.setInclusiveHostCount(true);

                final SubnetUtils.SubnetInfo info = utils.getInfo();
                inrange = info.isInRange(ipAddrWrk);

                if (inrange) {
                    break;
                }
            }
        }
        return inrange;
    }

    /**
     * Checks if an IP address is a public (global) IP address.
     *
     * @param ip
     *            The IPv4 or IPv6 address.
     * @return {@code true} if address is a public.
     */
    public static boolean isPublicAddress(final String ip) {

        final InetAddress address;

        try {

            address = InetAddress.getByName(ip);

        } catch (UnknownHostException exception) {
            return false;
        }

        return !(address.isSiteLocalAddress() || address.isAnyLocalAddress()
                || address.isLinkLocalAddress() || address.isLoopbackAddress()
                || address.isMulticastAddress());
    }

    /**
     * Checks if browser host points to intranet host address of this SavaPage
     * server instance.
     *
     * @param browserHost
     *            The HTML browser host address.
     * @return {@code true} when browser host points to intranet host address of
     *         this server instance.
     */
    public static boolean isIntranetBrowserHost(final String browserHost) {

        try {
            return browserHost.equals(LOCAL_HOST)
                    || browserHost.equals(IPV4_LOOP_BACK_ADDR)
                    || browserHost.endsWith(LOCAL_SUFFIX)
                    || getServerHostAddress().equals(browserHost);
        } catch (UnknownHostException e) {
            return false;
        }
    }

    /**
     * @return Array of SSL protocols like "TLSv1.3", "TLSv1.2", "TLSv1.1",
     *         "TLSv1" from SSLContext default instance.
     * @throws NoSuchAlgorithmException
     *             If SSLContext default instance is not present.
     */
    public static String[] getDefaultSSLProtocolArray()
            throws NoSuchAlgorithmException {
        final SSLContext ctx = SSLContext.getDefault();
        final SSLParameters params = ctx.getDefaultSSLParameters();
        return params.getProtocols();
    }

    /**
     * @return A space separated list of SSL protocols from SSLContext default
     *         instance. For example: {@code "TLSv1.3 TLSv1.2"} or
     *         {@code "TLSv1.2 TLSv1.1 TLSv1"}
     * @throws NoSuchAlgorithmException
     *             If SSLContext default instance is not present.
     */
    public static String getDefaultSSLProtocols()
            throws NoSuchAlgorithmException {
        final StringBuilder protocols = new StringBuilder();
        for (final String protocol : getDefaultSSLProtocolArray()) {
            protocols.append(" ").append(protocol);
        }
        return protocols.toString().trim();
    }

    /**
     * Creates an {@link SSLContext} that trusts self-signed SSL certificates.
     *
     * @return The SSLContext.
     */
    public static SSLContext createSslContextTrustSelfSigned() {

        final SSLContextBuilder builder = new SSLContextBuilder();

        try {
            return builder
                    .loadTrustMaterial(null, TrustSelfSignedStrategy.INSTANCE)
                    .build();
        } catch (NoSuchAlgorithmException | KeyStoreException
                | KeyManagementException e) {
            throw new IllegalStateException(e.getMessage());
        }
    }

    /**
     * Gets HostnameVerifier that turns hostname verification off.
     *
     * @return The verifier.
     */
    public static HostnameVerifier getHostnameVerifierTrustAll() {
        return NoopHostnameVerifier.INSTANCE;
    }

    /**
     * Gets HostnameVerifier that turns hostname verification off and prints
     * hostname certs on stdout.
     * <p>
     * NOTE: This verifier is useful for testing/debugging and should not be
     * used in production.
     * </p>
     *
     * @return The verifier.
     */
    public static HostnameVerifier getHostnameVerifierTrustAllDebug() {
        return DebugHostVerifier.INSTANCE;
    }

}
