/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2015 Datraverse B.V.
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
package org.savapage.core.services.helpers;

import java.io.IOException;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.savapage.core.dto.PrinterSnmpDto;
import org.savapage.core.snmp.SnmpClientSession;
import org.savapage.core.snmp.SnmpConnectException;
import org.savapage.core.snmp.SnmpMibDict;
import org.savapage.core.snmp.SnmpPrinterErrorStateEnum;
import org.savapage.core.snmp.SnmpPrinterStatusEnum;
import org.savapage.core.snmp.SnmpPrtMarkerColorantEntry;
import org.savapage.core.snmp.SnmpPrtMarkerCounterUnitEnum;
import org.savapage.core.snmp.SnmpPrtMarkerSuppliesEntry;
import org.savapage.core.snmp.SnmpVersion;
import org.savapage.core.util.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snmp4j.smi.OID;

/**
 *
 * @author Datraverse B.V.
 *
 */
public final class PrinterSnmpReader {

    /**
     * .
     */
    private static final Logger LOGGER = LoggerFactory
            .getLogger(PrinterSnmpReader.class);

    /**
     * .
     */
    private PrinterSnmpReader() {

    }

    /**
     * Retrieves SNMP printer info using default port and community.
     *
     * @param ipAddress
     *            The IP address of the printer
     * @return The {@link PrinterSnmpDto}.
     * @throws SnmpConnectException
     *             When connection errors occur.
     */
    public static PrinterSnmpDto read(final String ipAddress)
            throws SnmpConnectException {
        return read(ipAddress, SnmpClientSession.DEFAULT_PORT_READ,
                SnmpClientSession.DEFAULT_COMMUNITY, null);
    }

    /**
     * Retrieves SNMP printer info.
     *
     * @param ipAddress
     *            The IP address of the printer
     * @param port
     *            The SNMP port.
     * @param community
     *            The SNMP community.
     * @param version
     *            The {@link SnmpVersion} ({@code null} when undetermined).
     * @return The {@link PrinterSnmpDto}.
     * @throws SnmpConnectException
     *             When connection errors occur.
     */
    public static PrinterSnmpDto read(final String ipAddress, final int port,
            final String community, final SnmpVersion version)
            throws SnmpConnectException {

        final PrinterSnmpDto info = new PrinterSnmpDto();

        final SnmpClientSession client =
                new SnmpClientSession(String.format("udp:%s/%d", ipAddress,
                        port), community, version);

        try {
            client.init();
        } catch (IOException e) {
            throw new SnmpConnectException(e.getMessage(), e);
        }

        // ----- Printer Status
        OID oidWlk = SnmpMibDict.OID_PRINTER_STATUS;

        try {
            Integer intValue;
            String strValue;

            // -----
            info.setVendor(client.getVendor());

            // ----- Printer Status
            oidWlk = SnmpMibDict.OID_PRINTER_STATUS;
            intValue = client.getAsInt(oidWlk);

            if (intValue != null) {
                info.setPrinterStatus(SnmpPrinterStatusEnum.asEnum(intValue));
            }

            //
            oidWlk = SnmpMibDict.OID_SYSTEM_UPTIME;
            intValue = client.getAsInt(oidWlk);

            if (intValue != null) {
                // Note: the uptime evaluates to a negative value.
                info.setDateStarted(DateUtils.addMilliseconds(new Date(),
                        intValue.intValue()
                                * DateUtil.MSEC_IN_HUNDREDTH_OF_SECOND));
            }

            // ----- Marker life count
            oidWlk = SnmpMibDict.OID_PRT_MARKER_COUNTER_UNIT;
            intValue = client.getAsInt(oidWlk);

            if (intValue != null) {
                info.setMarkerCounterUnit(SnmpPrtMarkerCounterUnitEnum
                        .asEnum(intValue));
            }

            oidWlk = SnmpMibDict.OID_PRT_MARKER_LIFE_COUNT;
            info.setMarkerLifeCount(client.getAsInt(oidWlk));

            // -----
            info.setMarkerColorants(SnmpPrtMarkerColorantEntry.retrieve(client));

            //
            info.setSuppliesEntries(SnmpPrtMarkerSuppliesEntry.retrieve(client,
                    info.getMarkerColorants()));

            // -----
            oidWlk = SnmpMibDict.OID_SYSTEM_DESCR_RFC2790;
            strValue = client.getAsString(oidWlk);

            if (StringUtils.isBlank(strValue)) {
                oidWlk = SnmpMibDict.OID_SYSTEM_DESCR_RFC1213;
                strValue = client.getAsString(oidWlk);
            }

            info.setSystemDescription(strValue);

            // -----
            oidWlk = SnmpMibDict.OID_PRT_SERIAL_NR;
            info.setSerialNumber(client.getAsString(oidWlk));

            // -----
            oidWlk = SnmpPrinterErrorStateEnum.getOID();
            strValue = client.getAsString(oidWlk);

            if (strValue != null) {
                info.setErrorStates(SnmpPrinterErrorStateEnum
                        .fromOctetString(strValue));
            }

        } catch (SnmpConnectException e) {

            throw e;

        } catch (Exception e) {
            LOGGER.error(
                    String.format("OID [%s] : %s ", oidWlk.toString(),
                            e.getMessage()), e);
        } finally {
            try {
                client.exit();
            } catch (IOException e) {
                throw new SnmpConnectException(e.getMessage(), e);
            }
        }

        return info;
    }
}
