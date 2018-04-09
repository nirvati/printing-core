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
package org.savapage.core.services.helpers;

import org.savapage.core.jpa.Printer;

/**
 * Query parameters for printer SNMP.
 *
 * @author Rijk Ravestein
 *
 */
public final class SnmpPrinterQueryDto {

    /**
     * The host of the printer device URI.
     */
    private String uriHost;

    /**
     * The printer.
     */
    private Printer printer;

    /**
     * @return The host of the printer device URI.
     */
    public String getUriHost() {
        return uriHost;
    }

    /**
     * @param uriHost
     *            The host of the printer device URI.
     */
    public void setUriHost(String uriHost) {
        this.uriHost = uriHost;
    }

    /**
     *
     * @return The printer.
     */
    public Printer getPrinter() {
        return printer;
    }

    /**
     *
     * @param printer
     *            The printer.
     */
    public void setPrinter(Printer printer) {
        this.printer = printer;
    }

}
