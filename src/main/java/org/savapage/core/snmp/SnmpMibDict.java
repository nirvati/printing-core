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

import org.snmp4j.smi.OID;

/**
 * SNMP MIB Dictionary.
 * <p>
 * See:
 * <a href="http://technet.microsoft.com/en-us/library/cc783142(v=ws.10).aspx">
 * How SNMP Works</a>.
 * </p>
 * <ul>
 * <li><a href="http://tools.ietf.org/html/rfc1213.html">RFC1213</a>: Management
 * Information Base for Network Management of TCP/IP-based internets: MIB-II
 * </li>
 * <li><a href="http://tools.ietf.org/html/rfc1759.html">RFC1759</a>: Printer
 * MIB</li>
 * <li><a href="http://tools.ietf.org/html/rfc2790.html">RFC2790</a>: Host
 * Resources MIB</li>
 * </ul>
 * </p>
 *
 * @author Rijk Ravestein
 *
 */
public final class SnmpMibDict {

    /**
     * The SNMP-related branches of the MIB tree are located in the internet
     * branch: {@code iso.org.dod.internet}
     */
    private static final String PFX_SNMP = "1.3.6.1";

    /**
     * The public MIB-II branch: {@code iso.org.dod.internet.mgmt.mib-2}
     */
    private static final String PFX_MIB_II = PFX_SNMP + ".2.1";

    /**
     * The public system branch: {@code iso.org.dod.internet.mgmt.mib-2.system}
     */
    private static final String PFX_SYSTEM = PFX_MIB_II + ".1";

    /**
     * The public system branch: {@code iso.org.dod.internet.mgmt.mib-2.host}
     */
    public static final String PFX_HOST = PFX_MIB_II + ".25";

    /**
     * .
     */
    public static final OID OID_PRINTER_DETECTED_ERROR_STATE =
            new OID(PFX_HOST + ".3.5.1.2.1");

    /**
     * RFC1213 : <a href="http://oid-info.com/get/1.3.6.1.2.1.1.1">
     * {@code iso.org.dod.internet.mgmt.mib-2.system.sysDescr}</a>
     * <p>
     * <i>A textual description of the entity. This value should include the
     * full name and version identification of the system's hardware type,
     * software operating-system, and networking software. It is mandatory that
     * this only contain printable ASCII characters.</i>
     * </p>
     */
    public static final OID OID_SYSTEM_DESCR_RFC1213 =
            new OID(PFX_SYSTEM + ".1.0");

    /**
     * RFC1213 : <a href="http://oid-info.com/get/1.3.6.1.2.1.1.2"> {iso(1)
     * identified-organization(3) dod(6) internet(1) mgmt(2) mib-2(1) system(1)
     * sysObjectID(2)}</a>
     * <p>
     * <i>The vendor's authoritative identification of the network management
     * subsystem contained in the entity. This value is allocated within the SMI
     * enterprises subtree (1.3.6.1.4.1) and provides an easy and unambiguous
     * means for determining `what kind of box' is being managed. For example,
     * if vendor `Flintstones, Inc.' was assigned the subtree 1.3.6.1.4.1.4242,
     * it could assign the identifier 1.3.6.1.4.1.4242.1.1 to its `Fred
     * Router'</i>
     * </p>
     */
    public static final OID OID_SYSTEM_OID = new OID(PFX_SYSTEM + ".2.0");

    /**
     * RFC1213: <a href="http://oid-info.com/get/1.3.6.1.2.1.1.3"> {iso(1)
     * identified-organization(3) dod(6) internet(1) mgmt(2) mib-2(1) system(1)
     * sysUpTime(3)}</a>
     * <p>
     * <i>The time (in hundredths of a second) since the network management
     * portion of the system was last re-initialized. </i>
     * </p>
     */
    public static final OID OID_SYSTEM_UPTIME = new OID(PFX_SYSTEM + ".3.0");

    /**
     * The textual identification of the contact person for this managed node,
     * together with information on how to contact this person.
     */
    public static final OID OID_SYSTEM_CONTACT = new OID(PFX_SYSTEM + ".4.0");

    /**
     * An administratively-assigned name for this managed node. By convention,
     * this is the node's fully-qualified domain name.
     */
    public static final OID OID_SYSTEM_NAME = new OID(PFX_SYSTEM + ".5.0");

    /**
     *
     */
    public static final OID OID_SYSTEM_LOCATION = new OID(PFX_SYSTEM + ".6.0");

    /**
     * RFC2790: <a href="http://oid-info.com/get/1.3.6.1.2.1.25.3.2.1.3">
     * {@code iso.org.dod.internet.mgmt.mib-2.host.hrDevice.hrDeviceTable.hrDeviceEntry.hrDeviceDescr}
     * </a>
     * <p>
     * <i>A textual description of this device, including the devices
     * manufacturer and revision, and optionally, its serial number.</i>
     * </p>
     */
    public static final OID OID_SYSTEM_DESCR_RFC2790 =
            new OID(PFX_HOST + ".3.2.1.3.1");

    /**
     * RFC2790: <a href="http://oid-info.com/get/1.3.6.1.2.1.25.3.5.1.1">
     * {iso(1) identified-organization(3) dod(6) internet(1) mgmt(2) mib-2(1)
     * host(25) hrDevice(3) hrPrinterTable(5) hrPrinterEntry(1)
     * hrPrinterStatus(1)} </a>
     * <p>
     * <i>The current status of this printer device.</i>
     * </p>
     */
    public static final OID OID_PRINTER_STATUS =
            new OID(PFX_HOST + ".3.5.1.1.1");

    /**
     * The public printers branch:
     * {@code iso.org.dod.internet.mgmt.mib-2.printers}
     */
    private static final String PFX_PRINTERS = PFX_MIB_II + ".43";

    /**
     * The private enterprises MIB branch:
     * {@code iso.org.dod.internet.mgmt.private.enterprises}
     * <p>
     * Enterprises are defined by IANA. See <a href=
     * "http://www.iana.org/assignments/enterprise-numbers/enterprise-numbers" >
     * SMI Network Management Private Enterprise Codes</a>.
     * </p>
     */
    public static final String PFX_ENTERPRISES = PFX_SNMP + ".4.1.";

    /**
     * The private MIB branch for Canon.
     */
    private static final String PFX_CANON =
            PFX_ENTERPRISES + SnmpPrinterVendorEnum.CANON.enterpriseAsString();

    /**
     * The private MIB branch for Epson.
     */
    private static final String PFX_EPSON =
            PFX_ENTERPRISES + SnmpPrinterVendorEnum.EPSON.enterpriseAsString();

    /**
     * The private MIB branch for HP.
     */
    private static final String PFX_HP =
            PFX_ENTERPRISES + SnmpPrinterVendorEnum.HP.enterpriseAsString();

    /**
     * The private MIB branch for KONICA MINOLTA HOLDINGS, INC.
     */
    @SuppressWarnings("unused")
    private static final String PFX_KONICA =
            PFX_ENTERPRISES + SnmpPrinterVendorEnum.KONICA.enterpriseAsString();

    /**
     * The private MIB branch for Kyocera.
     */
    @SuppressWarnings("unused")
    private static final String PFX_KYOCERA = PFX_ENTERPRISES
            + SnmpPrinterVendorEnum.KYOCERA.enterpriseAsString();

    /**
     * The private MIB branch for Lexmark.
     */
    private static final String PFX_LEXMARK = PFX_ENTERPRISES
            + SnmpPrinterVendorEnum.LEXMARK.enterpriseAsString();

    /**
     * The private MIB branch for Oki.
     */
    private static final String PFX_OKI =
            PFX_ENTERPRISES + SnmpPrinterVendorEnum.OKI.enterpriseAsString();

    /**
     * The private MIB branch for Ricoh.
     */
    private static final String PFX_RICOH =
            PFX_ENTERPRISES + SnmpPrinterVendorEnum.RICOH.enterpriseAsString();

    /**
     * The private MIB branch for Xerox.
     */
    @SuppressWarnings("unused")
    private static final String PFX_XEROX =
            PFX_ENTERPRISES + SnmpPrinterVendorEnum.XEROX.enterpriseAsString();

    /**
     * "A recorded serial number for this device that indexes some type device
     * catalog or inventory. This value is usually set by the device
     * manufacturer but the MIB supports the option of writing for this object
     * for site-specific administration of device inventory or tracking."
     */
    public static final OID OID_PRT_SERIAL_NR =
            new OID(PFX_PRINTERS + ".5.1.1.17.1");

    /**
     * Attributes of a marker supply. Entries may exist in the table for each
     * device index whose device type is printer.
     */
    public static final String PRT_MARKER_SUPPLIES_ENTRY =
            PFX_PRINTERS + ".11.1.1";

    /**
     * Attributes of a marker supply. Entries may exist in the table for each
     * device index whose device type is printer.
     */
    public static final OID OID_PRT_MARKER_SUPPLIES_ENTRY =
            new OID(PRT_MARKER_SUPPLIES_ENTRY);

    /**
     * The value of prtMarkerIndex corresponding to the marking sub-unit with
     * which this marker supply sub-unit is associated.
     */
    public static final OID OID_PRT_MARKER_SUPPLIES_MARKER_INDEX =
            new OID(PRT_MARKER_SUPPLIES_ENTRY + ".2");

    /**
     * The value of prtMarkerColorantIndex corresponding to the colorant with
     * which this marker supply sub-unit is associated. This value shall be 0 if
     * there is no colorant table.
     */
    public static final OID OID_PRT_MARKER_SUPPLIES_COLORANT_INDEX =
            new OID(PRT_MARKER_SUPPLIES_ENTRY + ".3");

    /**
     * prtMarkerSuppliesClass
     *
     * other(1), supplyThatIsConsumed(3), receptacleThatIsFilled(4)
     *
     * "Indicates whether this supply entity represents a supply container that
     * is consumed or a receptacle that is filled."
     *
     */
    public static final OID OID_PRT_MARKER_SUPPLIES_CLASS =
            new OID(PRT_MARKER_SUPPLIES_ENTRY + ".4");

    /**
     * prtMarkerSuppliesType
     *
     * other(1), unknown(2), toner(3), wasteToner(4), ink(5), inkCartridge(6),
     * inkRibbon(7), wasteInk(8), opc(9), developer(10), fuserOil(11),
     * solidWax(12), ribbonWax(13), wasteWax(14)
     *
     * The type of this supply.
     */
    public static final OID OID_PRT_MARKER_SUPPLIES_TYPE =
            new OID(PRT_MARKER_SUPPLIES_ENTRY + ".5");

    /**
     * prtMarkerSuppliesDescription
     *
     * "The description of this supply container/receptacle in the localization
     * specified by prtGeneralCurrentLocalization."
     */
    public static final OID OID_PRT_MARKER_SUPPLIES_DESCRIPTION =
            new OID(PRT_MARKER_SUPPLIES_ENTRY + ".6");

    /**
     * prtMarkerSuppliesSupplyUnit
     *
     * {iso(1) identified-organization(3) dod(6) internet(1) mgmt(2) mib-2(1)
     * printmib(43) prtMarkerSupplies(11) prtMarkerSuppliesTable(1)
     * prtMarkerSuppliesEntry(1) prtMarkerSuppliesSupplyUnit(7)}
     *
     * "Unit of this marker supply container/receptacle.
     */
    public static final OID OID_PRT_MARKER_SUPPLIES_SUPPLY_UNIT =
            new OID(PRT_MARKER_SUPPLIES_ENTRY + ".7");

    /**
     * prtMarkerSuppliesMaxCapacity
     *
     * The maximum capacity of this supply container/receptacle expressed in
     * SupplyUnit. If this supply container/receptacle can reliably sense this
     * value, the value is sensed by the printer and is read-only; otherwise,
     * the value may be written (by a Remote Control Panel or a Management
     * Application). The value (-1) means other and specifically indicates that
     * the sub-unit places no restrictions on this parameter. The value (-2)
     * means unknown.
     */
    public static final OID OID_PRT_MARKER_SUPPLIES_MAX_CAPACITY =
            new OID(PRT_MARKER_SUPPLIES_ENTRY + ".8");

    /**
     * (-1) means other and specifically indicates that the sub-unit places no
     * restrictions on this parameter.
     */
    public static final int PRT_MARKER_SUPPLIES_MAX_CAPACITY_UNRESTRICTED = -1;

    /**
     * A value of (-2) means unknown.
     */
    public static final int PRT_MARKER_SUPPLIES_MAX_CAPACITY_UNKNOWN = -2;

    /**
     * prtMarkerSuppliesLevel
     *
     * The current level if this supply is a container; the remaining space if
     * this supply is a receptacle. If this supply container/receptacle can
     * reliably sense this value, the value is sensed by the printer and is
     * read-only; otherwise, the value may be written (by a Remote Contol Panel
     * or a Management Application). The value (-1) means other and specifically
     * indicates that the sub-unit places no restrictions on this parameter. The
     * value (-2) means unknown. A value of (-3) means that the printer knows
     * that there is some supply/remaining space, respectively.
     */
    public static final OID OID_PRT_MARKER_SUPPLIES_LEVEL =
            new OID(PRT_MARKER_SUPPLIES_ENTRY + ".9");

    /**
     * (-1) means other and specifically indicates that the sub-unit places no
     * restrictions on this parameter.
     */
    public static final int PRT_MARKER_SUPPLIES_LEVEL_UNRESTRICTED = -1;

    /**
     * A value of (-2) means unknown.
     */
    public static final int PRT_MARKER_SUPPLIES_LEVEL_UNKNOWN = -2;

    /**
     * A value of (-3) means that the printer knows that the maximum capacity
     * has not been reached but the precise level is unknown.
     */
    public static final int PRT_MARKER_SUPPLIES_LEVEL_REMAINING = -3;

    /**
     * prtMarkerColorantEntry
     *
     * Attributes of a colorant available on the printer. Entries may exist in
     * the table for each device index whos device type is `printer.
     */
    private static final String PRT_MARKER_COLORANT_ENTRY =
            PFX_PRINTERS + ".12.1.1";

    /**
     * prtMarkerColorantMarkerIndex
     *
     * The value of prtMarkerIndex corresponding to the marker sub-unit with
     * which this colorant entry is associated.
     */
    public static final OID OID_PRT_MARKER_COLORANT_MARKER_INDEX =
            new OID(PRT_MARKER_COLORANT_ENTRY + ".2");

    /**
     * prtMarkerColorantRole
     *
     * other(1), process(3), spot(4)
     *
     * The role played by this colorant.
     */
    public static final OID OID_PRT_MARKER_COLORANT_ROLE =
            new OID(PRT_MARKER_COLORANT_ENTRY + ".3");

    /**
     * prtMarkerColorantValue
     *
     * The name of the color of this The name of the color of this colorant
     * using standardized string names from ISO 10175 (DPA) and ISO 10180 (SPDL)
     * which are:
     *
     * other unknown white red green blue cyan magenta yellow black
     *
     * Implementors may add additional string values. The naming conventions in
     * ISO 9070 are recommended in order to avoid potential name clashes
     */
    public static final OID OID_PRT_MARKER_COLORANT_VALUE =
            new OID(PRT_MARKER_COLORANT_ENTRY + ".4");

    /**
     * prtMarkerColorantTonality
     *
     * The distinct levels of tonality realizable by a marking sub-unit when
     * using this colorant. This value does not include the number of levels of
     * tonal difference that an interpreter can obtain by techniques such as
     * half toning. This value must be at least 2.
     */
    public static final OID OID_PRT_MARKER_COLORANT_TONALITY =
            new OID(PRT_MARKER_COLORANT_ENTRY + ".5");

    /**
     * .
     */
    @SuppressWarnings("unused")
    private static final OID OID_PRT_SERIAL_NR_HP =
            new OID(PFX_HP + ".2.3.9.4.2.1.1.3.3.0");

    /**
     * .
     */
    @SuppressWarnings("unused")
    private static final OID OID_PRT_SERIAL_NR_EPSON =
            new OID(PFX_EPSON + ".1.2.2.1.1.1.5.1");

    /**
     * .
     */
    @SuppressWarnings("unused")
    private static final OID OID_PRT_SERIAL_NR_RICOH =
            new OID(PFX_RICOH + ".3.2.1.2.1.4.0");

    /**
     * .
     */
    @SuppressWarnings("unused")
    private static final OID OID_PRT_SERIAL_NR_CANON =
            new OID(PFX_CANON + ".1.2.1.4.0");

    /**
     * .
     */
    @SuppressWarnings("unused")
    private static final OID OID_PRT_SERIAL_NR_OKI =
            new OID(PFX_OKI + ".1.1.1.1.11.1.10.45.0");

    /**
     * .
     */
    @SuppressWarnings("unused")
    private static final OID OID_PRT_SERIAL_NR_LEXMARK =
            new OID(PFX_LEXMARK + ".6.2.3.1.5.1");

    /**
     * RFC1759: <a href="http://oid-info.com/get/1.3.6.1.2.1.43.10.2.1.3">
     * {iso(1) identified-organization(3) dod(6) internet(1) mgmt(2) mib-2(1)
     * printmib(43) prtMarker(10) prtMarkerTable(2) prtMarkerEntry(1)
     * prtMarkerCounterUnit(3)}
     * <p>
     * Note the suffix ".1.1".
     * </p>
     */
    public static final OID OID_PRT_MARKER_COUNTER_UNIT =
            new OID(PFX_PRINTERS + ".10.2.1.3.1.1");

    /**
     * RFC1759: <a href="http://oid-info.com/get/1.3.6.1.2.1.43.10.2.1.4">
     * {iso(1) identified-organization(3) dod(6) internet(1) mgmt(2) mib-2(1)
     * printmib(43) prtMarker(10) prtMarkerTable(2) prtMarkerEntry(1)
     * prtMarkerLifeCount(4)}</a>
     * <p>
     * <i>The count of the number of units of measure counted during the life of
     * printer using units of measure as specified by CounterUnit. </i>
     * </p>
     * <p>
     * Note the suffix ".1.1".
     * </p>
     */
    public static final OID OID_PRT_MARKER_LIFE_COUNT =
            new OID(PFX_PRINTERS + ".10.2.1.4.1.1");

    /**
     * Prevent public instantiation.
     */
    private SnmpMibDict() {

    }

}
