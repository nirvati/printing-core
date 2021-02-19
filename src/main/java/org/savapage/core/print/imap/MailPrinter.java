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
package org.savapage.core.print.imap;

/**
 * Properties of the Mail Printer.
 *
 * @author Rijk Ravestein
 *
 */
public final class MailPrinter {

    /**
     * Status of the Google Cloud Printer.
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

    /**
     *
     */
    private MailPrinter() {
    }

    /**
     *
     * @return
     */
    synchronized public static State getState() {

        State state;

        if (thePrinterOnline) {

            state = State.ON_LINE;

        } else {

            state = State.OFF_LINE;
        }

        return state;
    }

    /**
     *
     * @return
     */
    synchronized public static void setOnline(boolean online) {
        thePrinterOnline = online;
    }

    /**
     * Is the printer online?
     *
     * @return
     */
    synchronized public static boolean isOnline() {
        return getState() == State.ON_LINE;
    }

}
