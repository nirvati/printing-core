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
package org.savapage.ext.smartschool;

/**
 * Properties of the SmartSchool Printer.
 *
 * @author Datraverse B.V.
 *
 */
public final class SmartschoolPrinter {

    /**
     * Status of the SmartSchool Printer.
     */
    public static enum State {

        /**
         * Printer is off-line.
         */
        OFF_LINE,

        /**
         * Printer is on-line.
         */
        ON_LINE
    }

    private static boolean thePrinterOnline = true;

    private static boolean thePrinterBlocked = false;

    /**
     *
     */
    private SmartschoolPrinter() {
    }

    /**
     * @return The {@link State} of the printer.
     */
    public static synchronized State getState() {

        State state;

        if (thePrinterOnline) {

            state = State.ON_LINE;

        } else {

            state = State.OFF_LINE;
        }

        return state;
    }

    /**
     * @param online
     *            {@code true} when printer is online.
     */
    public static synchronized void setOnline(final boolean online) {
        thePrinterOnline = online;
    }

    /**
     * @return {@code true} when printer is online.
     */
    public static synchronized boolean isOnline() {
        return getState() == State.ON_LINE;
    }

    /**
     * @param blocked
     *            {@code true} when printer is blocked due to Community
     *            Membership status.
     */
    public static synchronized void setBlocked(final boolean blocked) {
        thePrinterBlocked = blocked;
    }

    /**
     * @return {@code true} when printer is blocked due to Community Membership
     *         status.
     */
    public static synchronized boolean isBlocked() {
        return thePrinterBlocked;
    }
}
