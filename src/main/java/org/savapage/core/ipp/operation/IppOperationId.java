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
package org.savapage.core.ipp.operation;

import org.savapage.core.SpException;

/**
 * IPP Operation IDs.
 * <p>
 * <a href="http://tools.ietf.org/html/rfc2911#section-4.4.15">RFC2911</a>.
 * </p>
 * <p>
 * CUPS provides
 * <a href="http://www.cups.org/documentation.php/spec-ipp.html">16 extension
 * operations </a> in addition to most of the standard IPP and registered
 * extension operations.
 * </p>
 *
 * @author Rijk Ravestein
 *
 */
public enum IppOperationId {

    /**  */
    PRINT_JOB(0x02),

    /**  */
    PRINT_URI(0x03),

    /**  */
    VALIDATE_JOB(0x04),

    /**  */
    CREATE_JOB(0x05),

    /**  */
    SEND_DOC(0x06),

    /**  */
    SEND_URI(0x07),

    /**  */
    CANCEL_JOB(0x08),

    /**  */
    GET_JOB_ATTR(0x09),

    /**  */
    GET_JOBS(0x0A),

    /**  */
    GET_PRINTER_ATTR(0x0B),

    /**  */
    HOLD_JOB(0x0C),

    /**  */
    RELEASE_JOB(0x0D),

    /**  */
    RESTART_JOB(0x0E),

    // 0x000F reserved for a future operation

    /**  */
    PAUSE_PRINTER(0x10),

    /**  */
    RESUME_PRINTER(0x11),

    /**  */
    PURGE_JOBS(0x12),

    /*
     * 0x0013-0x3FFF : reserved for future IETF standards track operations (see
     * section 6.4).
     */

    /**
     * CUPS 1.2 : Creates a subscription associated with a printer or the
     * server: RFC3995 section 7.1.
     */
    CREATE_PRINTER_SUBSCRIPTIONS(0x16),

    /**
     * CUPS 1.2 : Creates a subscription associated with a job: RFC3995 section
     * 7.1.
     */
    CREATE_JOB_SUBSCRIPTIONS(0x17),

    /**
     * CUPS 1.2 : Gets the attributes for a subscription [RFC3995].
     */
    GET_SUBSCRIPTION_ATTRIBUTES(0x18),

    /**
     * CUPS 1.2 : Gets the attributes for zero or more subscriptions [RFC3995].
     */
    GET_SUBSCRIPTIONS(0x19),

    /**
     * CUPS 1.2 : Renews a subscription. [RFC3995].
     */
    RENEW_SUBSCRIPTION(0x1A),

    /**
     * CUPS 1.2 : Cancels a subscription. [RFC3995].
     */
    CANCEL_SUBSCRIPTION(0x1B),

    /**
     * CUPS 1.2 : Get notification events for ippget subscriptions. [RFC3995].
     */
    GET_NOTIFICATIONS(0x1C),

    /**
     * PWG 5100.11: IPP Everywhere Required operation.
     */
    CANCEL_MY_JOBS(0x39),

    /**
     * PWG 5100.11: IPP Everywhere Required operation.
     */
    CLOSE_JOB(0x3B),

    /**
     * PWG 5100.11: IPP Everywhere Required operation.
     */
    IDENTIFY_PRINTER(0x3C),

    /*
     * 0x4000-0x8FFF : IPP Vendor Operation Codes.
     * https://www.pwg.org/ipp/opcodes/ippopcodes.html
     */

    /**
     * Observed when printing IPP/1.x from MS Windows. No documentation found.
     */
    MICROSOFT_UNDOCUMENTED(0x4000),

    /** CUPS 1.0 : Get the default destination. */
    CUPS_GET_DEFAULT(0x4001),

    /** CUPS 1.0 : Get all of the available printers. */
    CUPS_GET_PRINTERS(0x4002),

    /** CUPS 1.0 : Add or modify a printer. */
    CUPS_ADD_MODIFY_PRINTER(0x4003),

    /** CUPS 1.0 : Delete a printer. */
    CUPS_DELETE_PRINTER(0x4004),

    /** CUPS 1.0 : Get all of the available printer classes. */
    CUPS_GET_CLASSES(0x4005),

    /** CUPS 1.0 : Add or modify a printer class. */
    CUPS_ADD_MODIFY_CLASS(0x4006),

    /** CUPS 1.0 : Delete a printer class. */
    CUPS_DELETE_CLASS(0x4007),

    /** CUPS 1.0 : Accept jobs on a printer or printer class. */
    CUPS_ACCEPT_JOBS(0x4008),

    /** CUPS 1.0 : Reject jobs on a printer or printer class. */
    CUPS_REJECT_JOBS(0x4009),

    /** CUPS 1.0 : Set the default destination. */
    CUPS_SET_DEFAULT(0x400A),

    /** CUPS 1.1 : Get all of the available devices. */
    CUPS_GET_DEVICES(0x400B),

    /** CUPS 1.1 : Get all of the available PPDs. */
    CUPS_GET_PPDS(0x400C),

    /** CUPS 1.1 : Move a job to a different printer. */
    CUPS_MOVE_JOB(0x400D),

    /** CUPS 1.2 : Authenticate a job for printing. */
    CUPS_AUTHENTICATE_JOB(0x400E),

    /** CUPS 1.3 : Get a PPD file. */
    CUPS_GET_PPD(0x400F),

    /** CUPS 1.4 : Get a document file from a job. */
    CUPS_GET_DOCUMENT(0x4027);

    /**
     * Operation code.
     */
    private int opcode = 0;

    /**
     * Supported IPP/1.x operations.
     */
    private static IppOperationId[] supportedV1 = new IppOperationId[] {
            //
            GET_PRINTER_ATTR, //
            PRINT_JOB, //
            GET_JOB_ATTR, //
            GET_JOBS, //
            CANCEL_JOB, //
            VALIDATE_JOB //
    };

    /**
     * Supported IPP/2.x operations.
     */
    private static IppOperationId[] supportedV2 = new IppOperationId[] {
            //
            IDENTIFY_PRINTER, //
            CREATE_JOB, //
            SEND_DOC, //
            CANCEL_MY_JOBS, //
            CLOSE_JOB //
    };

    /**
     * Creates an enum value from an integer.
     *
     * @param value
     *            The integer.
     */
    IppOperationId(final int value) {
        this.opcode = value;
    }

    /**
     * Gets the integer representing this enum value.
     *
     * @return The integer.
     */
    public int asInt() {
        return this.opcode;
    }

    /**
     * Converts int to enum.
     *
     * Note: these are only the values used by SavaPage in print server role.
     *
     * @param value
     *            numeric code.
     * @return {@link IppOperationId}.
     */
    public static IppOperationId asEnum(final int value) {
        if (value == IppOperationId.PRINT_JOB.asInt()) {
            return PRINT_JOB;
        } else if (value == IppOperationId.PRINT_URI.asInt()) {
            return PRINT_URI;
        } else if (value == IppOperationId.VALIDATE_JOB.asInt()) {
            return VALIDATE_JOB;
        } else if (value == IppOperationId.CREATE_JOB.asInt()) {
            return CREATE_JOB;
        } else if (value == IppOperationId.SEND_DOC.asInt()) {
            return SEND_DOC;
        } else if (value == IppOperationId.SEND_URI.asInt()) {
            return SEND_URI;
        } else if (value == IppOperationId.CANCEL_JOB.asInt()) {
            return CANCEL_JOB;
        } else if (value == IppOperationId.GET_JOB_ATTR.asInt()) {
            return GET_JOB_ATTR;
        } else if (value == IppOperationId.GET_JOBS.asInt()) {
            return GET_JOBS;
        } else if (value == IppOperationId.GET_PRINTER_ATTR.asInt()) {
            return GET_PRINTER_ATTR;
        } else if (value == IppOperationId.HOLD_JOB.asInt()) {
            return HOLD_JOB;
        } else if (value == IppOperationId.RELEASE_JOB.asInt()) {
            return RELEASE_JOB;
        } else if (value == IppOperationId.RESTART_JOB.asInt()) {
            return RESTART_JOB;
        } else if (value == IppOperationId.PAUSE_PRINTER.asInt()) {
            return PAUSE_PRINTER;
        } else if (value == IppOperationId.RESUME_PRINTER.asInt()) {
            return RESUME_PRINTER;
        } else if (value == IppOperationId.PURGE_JOBS.asInt()) {
            return PURGE_JOBS;
        } else if (value == IppOperationId.CANCEL_MY_JOBS.asInt()) {
            return CANCEL_MY_JOBS;
        } else if (value == IppOperationId.CLOSE_JOB.asInt()) {
            return CLOSE_JOB;
        } else if (value == IppOperationId.IDENTIFY_PRINTER.asInt()) {
            return IDENTIFY_PRINTER;
        } else if (value == IppOperationId.CUPS_GET_PRINTERS.asInt()) {
            return CUPS_GET_PRINTERS;
        } else if (value == IppOperationId.CUPS_GET_DEFAULT.asInt()) {
            return CUPS_GET_DEFAULT;
        }
        throw new SpException(
                String.format("Value [%d] can not be converted to %s.", value,
                        IppOperationId.class.getSimpleName()));
    }

    /**
     * @return Supported IPP/1.x operations.
     */
    public static IppOperationId[] supportedV1() {
        return supportedV1;
    }

    /**
     * @return Supported IPP/2.x operations.
     */
    public static IppOperationId[] supportedV2() {
        return supportedV2;
    }

}
