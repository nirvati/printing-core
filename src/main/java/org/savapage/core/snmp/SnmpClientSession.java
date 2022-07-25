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
package org.savapage.core.snmp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.Variable;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.snmp4j.util.DefaultPDUFactory;
import org.snmp4j.util.TableEvent;
import org.snmp4j.util.TableUtils;

/**
 * Based on this short
 * <a href="http://www.jayway.com/2010/05/21/introduction-to-snmp4j/">
 * Introduction to snmp4j</a>.
 *
 * @author Rijk Ravestein
 *
 */
public final class SnmpClientSession {

    /**
     * Core object.
     */
    private Snmp snmp = null;

    /**
     * Information about where the data should be fetched and how.
     */
    private final CommunityTarget target = new CommunityTarget();

    /**
     * Number of retries.
     */
    private static final int TARGET_RETRIES = 2;

    /**
     * Time-out in milliseconds.
     */
    private static final int TARGET_TIMEOUT = 1500;

    /**
     * Default SNMP port for Read and Other operations.
     */
    public static final int DEFAULT_PORT_READ = 161;

    /**
     * Default SNMP port for the trap generation.
     */
    public static final int DEFAULT_PORT_TRAP = 162;

    /**
     * The default community.
     */
    public static final String DEFAULT_COMMUNITY = "public";

    /**
     * Constructor for {@code public} community.
     *
     * @param address
     *            Port 161 is used for Read and Other operations Port 162 is
     *            used for the trap generation. For example:
     *            {@code "udp:10.10.3.38/161"}
     */
    public SnmpClientSession(final String address) {
        this(address, DEFAULT_COMMUNITY, null, TARGET_RETRIES, TARGET_TIMEOUT);
    }

    /**
     * Constructor typically used when using
     * <a href="http://snmpsim.sourceforge.net/">SNMP Agent Simulator</a>.
     *
     * @param address
     *            A simulator address like {@code "udp:192.168.1.54/1161"}
     * @param community
     *            The community like {@code "recorded/printer.10.10.3.38"}
     * @param version
     *            The {@link SnmpVersionEnum} ({@code null} when undetermined).
     * @param retries
     *            Number of retries.
     * @param timeout
     *            Time-out in milliseconds.
     */
    public SnmpClientSession(final String address, final String community,
            final SnmpVersionEnum version, final int retries,
            final int timeout) {

        final Address targetAddress = GenericAddress.parse(address);

        this.target.setCommunity(new OctetString(community));
        this.target.setAddress(targetAddress);
        this.target.setRetries(retries);
        this.target.setTimeout(timeout);

        if (version != null) {
            this.target.setVersion(version.getVersion());
        }
    }

    /**
     * Starts the SNMP session.
     *
     * @throws IOException
     *             When listening on {@link TransportMapping} fails.
     */
    public void init() throws IOException {

        if (this.snmp != null) {
            exit();
        }

        final DefaultUdpTransportMapping transport =
                new DefaultUdpTransportMapping();
        this.snmp = new Snmp(transport);

        /*
         * If you forget the listen() method you will not get any answers
         * because the communication is asynchronous and the listen() method
         * listens for answers.
         */
        transport.listen();
    }

    /**
     *
     * @throws IOException
     *             When closing {@link Snmp} fails.
     */
    public void exit() throws IOException {
        if (this.snmp != null) {
            this.snmp.close();
            this.snmp = null;
        }
    }

    /**
     * Method which takes a single OID and returns the response from the agent
     * as a String.
     *
     * @param oid
     * @return {@code null} when OID is not found.
     * @throws SnmpConnectException
     */
    public String getAsString(final OID oid) throws SnmpConnectException {
        final VariableBinding binding = this.getResponse(oid);
        if (binding == null) {
            return null;
        }
        return binding.getVariable().toString();
    }

    /**
     * Method which takes a single OID and returns the response from the agent
     * as an integer.
     *
     * @param oid
     * @return {@code null} when OID is not found.
     * @throws SnmpConnectException
     *             When SNMP connection fails.
     */
    public Integer getAsInt(final OID oid) throws SnmpConnectException {
        final VariableBinding binding = this.getResponse(oid);
        if (binding == null) {
            return null;
        }
        return Integer.valueOf(binding.getVariable().toInt());
    }

    /**
     * Method which takes a single OID and returns the response from the agent
     * as an integer.
     *
     * @param oid
     * @return {@code null} when OID is not found.
     * @throws SnmpConnectException
     *             When SNMP connection fails.
     */
    public OctetString getAsOctetString(final OID oid)
            throws SnmpConnectException {
        final VariableBinding binding = this.getResponse(oid);
        if (binding == null) {
            return null;
        }
        return new OctetString(binding.toValueString());
    }

    /**
     * Method which takes a single OID and returns the response from the agent
     * as a long.
     *
     * @param oid
     * @return {@code null} when OID is not found.
     * @throws SnmpConnectException
     *             When SNMP connection fails.
     */
    public Long getAsLong(final OID oid) throws SnmpConnectException {
        final VariableBinding binding = this.getResponse(oid);
        if (binding == null) {
            return null;
        }
        return Long.valueOf(binding.getVariable().toLong());
    }

    /**
     *
     * @return The IANA enterprise number.
     * @throws SnmpConnectException
     *             When SNMP connection fails.
     */
    public Integer getEnterprise() throws SnmpConnectException {

        final String systemOID = this.getAsString(SnmpMibDict.OID_SYSTEM_OID);

        if (systemOID == null
                || !systemOID.startsWith(SnmpMibDict.PFX_ENTERPRISES)) {
            return null;
        }

        final String[] tokens = StringUtils.split(
                StringUtils.removeStart(systemOID, SnmpMibDict.PFX_ENTERPRISES),
                '.');

        if (tokens.length == 0) {
            return null;
        }

        return Integer.valueOf(tokens[0]);
    }

    /**
     * This method is capable of handling multiple OIDs.
     *
     * @param oid
     *            The {@link OID}.
     * @return The {@link PDU} response or {@code null} when OID is not found.
     * @throws SnmpConnectException
     *             When SNMP connection fails.
     */
    private VariableBinding getResponse(final OID oid)
            throws SnmpConnectException {
        final PDU response = this.getResponse(new OID[] { oid });

        if (response.getErrorStatus() != SnmpConstants.SNMP_ERROR_SUCCESS) {
            return null;
        }

        if (oid.toString().equals(response.get(0).getOid().toString())) {
            return response.get(0);
        }
        return null;
    }

    /**
     * This method is capable of handling multiple OIDs.
     *
     * @param oids
     * @return The {@link PDU} response.
     * @throws SnmpConnectException
     *             When SNMP connection fails.
     */
    private PDU getResponse(final OID oids[]) throws SnmpConnectException {

        final PDU pdu = new PDU();

        for (final OID oid : oids) {
            pdu.add(new VariableBinding(oid));
        }

        pdu.setType(PDU.GET);

        ResponseEvent event;
        try {
            event = snmp.send(pdu, this.target, null);
        } catch (IOException e) {
            throw new SnmpConnectException(e.getMessage(), e);
        }

        if (event == null) {
            throw new SnmpConnectException("GET timed out");
        }

        final Exception ex = event.getError();

        if (ex != null) {
            throw new SnmpConnectException(ex.getMessage(), ex);
        }

        if (event.getResponse() == null) {
            throw new SnmpConnectException(
                    String.format("no response from [%s]",
                            this.target.getAddress().toString()));
        }

        return event.getResponse();
    }

    /**
     *
     */
    public List<List<String>> getTableAsStrings(final OID[] oids) {

        final TableUtils tUtils = new TableUtils(snmp, new DefaultPDUFactory());

        final List<TableEvent> events =
                tUtils.getTable(this.target, oids, null, null);

        final List<List<String>> list = new ArrayList<List<String>>();

        for (final TableEvent event : events) {

            if (event.isError()) {
                throw new RuntimeException(event.getErrorMessage());
            }

            final List<String> strList = new ArrayList<String>();

            for (final VariableBinding vb : event.getColumns()) {

                if (vb != null) {

                    final Variable variable = vb.getVariable();

                    if (variable != null) {
                        strList.add(variable.toString());
                    }
                }
            }

            if (!strList.isEmpty()) {
                list.add(strList);
            }

        }
        return list;
    }

}
