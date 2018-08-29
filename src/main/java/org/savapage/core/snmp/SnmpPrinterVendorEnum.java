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
package org.savapage.core.snmp;

/**
 * Private enterprises for MIB branch.
 * <p>
 * Enterprises are defined by IANA. See <a href=
 * "http://www.iana.org/assignments/enterprise-numbers/enterprise-numbers" >SMI
 * Network Management Private Enterprise Codes</a>.
 * </p>
 *
 * @author Rijk Ravestein
 *
 */
public enum SnmpPrinterVendorEnum {

    /**
     * .
     */
    CANON(1602, "Canon"),

    /**
     * .
     */
    EPSON(1248, "Epson"),

    /**
    *
    */
    HP(11, "HP"),

    /**
    *
    */
    KONICA(18334, "Konica"),

    /**
     * .
     */
    KYOCERA(1347, "Kyocera"),

    /**
     * .
     */
    LEXMARK(641, "Lexmark"),

    /**
     * Wes Hardaker: hardaker&users.sourceforge.net.
     */
    NET_SNMP(8072, "net-snmp"),

    /**
     * .
     */
    OKI(2001, "OKI"),

    /**
     * .
     */
    RICOH(367, "Ricoh"),

    /**
     * .
     */
    XEROX(253, "Xerox");

    /** */
    private final int enterprise;

    /** */
    private final String uiText;

    /**
     *
     * @param number
     *            The enterprise number.
     * @param text
     *            Text in UI.
     */
    SnmpPrinterVendorEnum(final int number, final String text) {
        this.enterprise = number;
        this.uiText = text;
    }

    /**
     *
     * @return The enterprise number as string.
     */
    public String enterpriseAsString() {
        return String.valueOf(enterprise);
    }

    /**
     * @return The UI text.
     */
    public String getUiText() {
        return uiText;
    }

    /**
     *
     * @param enterprise
     *            The enterprise number.
     * @return The enum, or {@code null} when not found.
     */
    public static SnmpPrinterVendorEnum
            fromEnterprise(final Integer enterprise) {

        if (enterprise != null) {
            for (final SnmpPrinterVendorEnum enumVal : SnmpPrinterVendorEnum
                    .values()) {
                if (enterprise.intValue() == enumVal.enterprise) {
                    return enumVal;
                }
            }
        }
        return null;
    }

}
