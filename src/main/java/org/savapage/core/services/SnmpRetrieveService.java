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
package org.savapage.core.services;

import java.net.URI;
import java.util.Set;

import org.savapage.core.jpa.Printer;

/**
 *
 * @author Rijk Ravestein
 *
 */
public interface SnmpRetrieveService extends StatefulService {

    /**
     * Checks if SNMP retrieval for a printer host is active (claimed or
     * started).
     *
     * @param printerHost
     *            The host address of the printer.
     * @return {@code true} when active.
     */
    boolean isSnmpRetrieveActive(String printerHost);

    /**
     * Sets a claim on an SNMP retrieve for a printer.
     *
     * @param printerHost
     *            The host address of the printer.
     * @return {@code false}, when SNMP retrieval is already active.
     */
    boolean claimSnmpRetrieve(String printerHost);

    /**
     * Sets a lock on a claimed SNMP retrieve for a printer.
     *
     * @param printerHost
     *            The host address of the printer.
     * @return {@code true}, when lock was successful. {@code false}, when SNMP
     *         claim is not found, or already locked.
     */
    boolean lockSnmpRetrieve(String printerHost);

    /**
     * Releases a claimed or locked SNMP retrieve for a printer.
     *
     * @param printerHost
     *            The host address of the printer.
     * @return {@code true}, when release was successful.
     */
    boolean releaseSnmpRetrieve(String printerHost);

    /**
     * Starts SNMP retrieval for a set of URI candidates.
     *
     * @param cupsPrinterUris
     *            Set of CUPS printer URIs.
     */
    void probeSnmpRetrieve(Set<URI> cupsPrinterUris);

    /**
     * Triggers SNMP retrieval for a printer when time threshold is reached.
     *
     * @param printer
     *            The printer.
     */
    void probeSnmpRetrieveTrigger(Printer printer);

    /**
     * Retrieves SNMP for all printers.
     */
    void retrieveAll();

    /**
     * Retrieves SNMP for a single printer.
     *
     * @param printerID
     *            Primary database key of printer.
     */
    void retrieve(Long printerID);

}
