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
package org.savapage.core.snmp;

import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.snmp4j.smi.OID;

/**
 * RFC2790: <a href="http://oid-info.com/get/1.3.6.1.2.1.25.3.5.1.2">{iso(1)
 * identified-organization(3) dod(6) internet(1) mgmt(2) mib-2(1) host(25)
 * hrDevice(3) hrPrinterTable(5) hrPrinterEntry(1)
 * hrPrinterDetectedErrorState(2)}</a>
 * <p>
 * <i> This object represents any error conditions detected by the printer. The
 * error conditions are encoded as bits in an octet string. The enum values have
 * bit values which can be used in an AND operation to check if an error state
 * is present.</i>
 * </p>
 * <p>
 * See <a href=
 * "http://stackoverflow.com/questions/27054622/how-to-read-snmp-oid-output"
 * >this</a> Q&A.
 * </p>
 *
 * @author Datraverse B.V.
 *
 */
public enum SnmpPrinterErrorStateEnum {

    /**
     * .
     */
    LOW_PAPER(0, 0x80), // 0
    /**
     * .
     */
    NO_PAPER(0, 0x40), // 1
    /**
     * .
     */
    LOW_TONER(0, 0x20), // 2
    /**
     * .
     */
    NO_TONER(0, 0x10), // 3
    /**
     * .
     */
    DOOR_OPEN(0, 0x08), // 4
    /**
     * .
     */
    JAMMED(0, 0x04), // 5
    /**
     * .
     */
    OFFLINE(0, 0x02), // 6
    /**
     * .
     */
    SERVICE_REQUESTED(0, 0x01), // 7

    /**
     * .
     */
    INPUT_TRAY_MISSING(1, 0x80), // 8
    /**
     * .
     */
    OUTPUT_TRAY_MISSING(1, 0x40), // 9
    /**
     * .
     */
    MARKER_SUPPLY_MISSING(1, 0x20), // 10
    /**
     * .
     */
    OUTPUT_NEAR_FULL(1, 0x10), // 11
    /**
     * .
     */
    OUTPUT_FULL(1, 0x08), // 12
    /**
     * .
     */
    INPUT_TRAY_EMPTY(1, 0x04), // 13
    /**
     * .
     */
    OVERDUE_PREVENT_MAINT(1, 0x02); // 14

    /**
     * {@code 0} is leftmost byte, {@code 1} is rightmost byte.
     */
    private final int iByte;

    /**
     * The bit-value.
     */
    private final int value;

    /**
     *
     * @param iByte
     *            {@code 0} is leftmost byte, {@code 1} is rightmost byte.
     * @param value
     *            The bit-value.
     */
    private SnmpPrinterErrorStateEnum(final int iByte, final int value) {
        this.iByte = iByte;
        this.value = value;
    }

    /**
     * Gets the set of error states from an SNMP octet string.
     *
     * @param octetString
     *            The octet string (octets are separated with a colon ':'
     *            character).
     * @return The set.
     */
    public static Set<SnmpPrinterErrorStateEnum> fromOctetString(
            final String octetString) {

        final StringTokenizer lineTokenizer =
                new StringTokenizer(octetString, ":");

        final int[] detectedErrorState = new int[lineTokenizer.countTokens()];

        int i = 0;

        while (lineTokenizer.hasMoreTokens()) {
            final String token = lineTokenizer.nextToken();
            if (StringUtils.isNotBlank(token)) {
                if (NumberUtils.isDigits(token)) {
                    detectedErrorState[i++] = Integer.parseInt(token, 16);
                }
            }
        }

        final Set<SnmpPrinterErrorStateEnum> set = new HashSet<>();

        for (int j = 0; j < detectedErrorState.length; j++) {

            for (SnmpPrinterErrorStateEnum enumValue : SnmpPrinterErrorStateEnum
                    .values()) {

                if (enumValue.iByte == j
                        && (detectedErrorState[j] & enumValue.value) > 0) {
                    set.add(enumValue);
                }
            }

        }

        return set;
    }

    /**
     *
     * @return {@link SnmpMibDict#OID_PRINTER_DETECTED_ERROR_STATE}.
     */
    public static OID getOID() {
        return SnmpMibDict.OID_PRINTER_DETECTED_ERROR_STATE;
    }
}
