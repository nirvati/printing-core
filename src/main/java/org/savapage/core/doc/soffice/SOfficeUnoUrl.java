/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2016 Datraverse B.V.
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
package org.savapage.core.doc.soffice;

/**
 * Encapsulates the UNO Interprocess Connection type and parameters.
 * <p>
 * Although OpenOffice supports two connection types: TCP sockets and named
 * pipes, we support sockets only. Reason: named pipes are only marginally
 * faster, but they require native libraries, which means setting
 * <em>java.library.path</em> when starting Java. E.g. on Linux:
 * {@code java -Djava.library.path=/opt/openoffice.org/ure/lib ...}
 *
 * <p>
 * See <a href=
 * "http://wiki.services.openoffice.org/wiki/Documentation/DevGuide/ProUNO/Opening_a_Connection">
 * Opening a Connection</a> in the OpenOffice.org Developer's Guide for more
 * details.
 *
 * @author Rijk Ravestein
 *
 */
public final class SOfficeUnoUrl {

    /**
     * The accept string.
     */
    private final String acceptString;

    /**
     * The connect string.
     */
    private final String connectString;

    /**
     * Constructor.
     *
     * @param accString
     *            The accept string.
     * @param connString
     *            The connect string.
     */
    private SOfficeUnoUrl(final String accString, final String connString) {
        this.acceptString = accString;
        this.connectString = connString;
    }

    /**
     * Creates a socket instance.
     *
     * @param port
     *            The port.
     * @return The {@link SOfficeUnoUrl}.
     */
    public static SOfficeUnoUrl socket(final int port) {
        String socketString = "socket,host=127.0.0.1,port=" + port;
        return new SOfficeUnoUrl(socketString, socketString + ",tcpNoDelay=1");
    }

    /**
     *
     * @return The accept string.
     */
    public String getAcceptString() {
        return acceptString;
    }

    /**
     *
     * @return The connect string.
     */
    public String getConnectString() {
        return connectString;
    }

    @Override
    public String toString() {
        return connectString;
    }

}
