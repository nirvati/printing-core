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

/**
 * Private enterprises for MIB branch.
 * <p>
 * Enterprises are defined by IANA. See <a href=
 * "http://www.iana.org/assignments/enterprise-numbers/enterprise-numbers" >SMI
 * Network Management Private Enterprise Codes</a>.
 * </p>
 *
 * @author Datraverse B.V.
 *
 */
public enum SnmpPrinterVendorEnum {

    /**
     * .
     */
    CANON("1602"),

    /**
     * .
     */
    EPSON("1248"),

    /**
    *
    */
    HP("11"),

    /**
   *
   */
    KONICA("18334"),

    /**
     * .
     */
    KYOCERA("1347"),

    /**
     * .
     */
    LEXMARK("641"),

    /**
     * .
     */
    OKI("2001"),

    /**
     * .
     */
    RICOH("367");

    /**
     *
     */
    final String enterprise;

    /**
     *
     * @param enterprise
     */
    private SnmpPrinterVendorEnum(final String enterprise) {
        this.enterprise = enterprise;
    }

    public String getEnterprise() {
        return enterprise;
    }

}
