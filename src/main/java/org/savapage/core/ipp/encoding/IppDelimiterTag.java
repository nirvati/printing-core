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
package org.savapage.core.ipp.encoding;

import org.savapage.core.SpException;

/**
 * Enumeration of IPP delimiter tags, see <a
 * href="http://tools.ietf.org/html/rfc2910#section-3.5.1">RFC2910</a>.
 *
 * @author Datraverse B.V.
 */
public enum IppDelimiterTag {

    /**
     * Reserved for definition in a future IETF standards track document.
     */
    RESERVED_X00(0x00), RESERVED_X08(0x08), RESERVED_X09(0x09), RESERVED_X0A(
            0x0A), RESERVED_X0B(0x0B), RESERVED_X0C(0x0C), RESERVED_X0D(0x0D),
    RESERVED_X0E(0x0E), RESERVED_X0F(0x0F),

    /**
     * subscription-attributes-tag: RFC3995 Section 14.
     */
    SUBSCRIPTION_ATTR(0x06),

    /**
     * event-notification-attributes-tag: RFC3995 Section 14.
     */
    EVENT_NOTIFICATION_ATTR(0x07),

    /**
     * "operation-attributes-tag".
     */
    OPERATION_ATTR(0x01),

    /**
     * "printer-attributes-tag".
     */
    PRINTER_ATTR(0x04),

    /**
     * "job-attributes-tag".
     */
    JOB_ATTR(0x02),

    /**
     * "end-of-attributes-tag".
     * <p>
     * MUST occur exactly once in an operation. It MUST be the last
     * "delimiter-tag". If the operation has a document-content group, the
     * document data in that group MUST follow the "end-of-attributes-tag".
     * </p>
     * */
    END_OF_ATTR(0x03),

    /**
     * "unsupported-attributes-tag".
     */
    UNSUPP_ATTR(0x05);

    /**
     *
     */
    private int bitPattern = 0;

    /**
     * Creates an enum value from an integer.
     *
     * @param value
     *            The integer.
     */
    IppDelimiterTag(final int value) {
        this.bitPattern = value;
    }

    /**
     * Gets the integer representing this enum value.
     *
     * @return The integer.
     */
    public int asInt() {
        return this.bitPattern;
    }

    /**
     * Checks whether a value is a delimiter tag reserved for future use.
     *
     * @param value
     *            The value to check.
     * @return {@code true} when reserved for future use.
     */
    public static boolean isReservedForFutureUse(int value) {

        return (value == RESERVED_X00.asInt() || value == RESERVED_X08.asInt()
                || value == RESERVED_X09.asInt()
                || value == RESERVED_X0A.asInt()
                || value == RESERVED_X0B.asInt()
                || value == RESERVED_X0C.asInt()
                || value == RESERVED_X0D.asInt()
                || value == RESERVED_X0E.asInt() || value == RESERVED_X0F
                    .asInt());
    }

    /**
     *
     * @param value
     * @return
     */
    public static boolean isValidEnum(int value) {
        return value < 0x10;
    }

    /**
     *
     * @param value
     * @return
     */
    public static IppDelimiterTag asEnum(int value) {
        if (value == IppDelimiterTag.END_OF_ATTR.asInt()) {
            return END_OF_ATTR;
        } else if (value == IppDelimiterTag.JOB_ATTR.asInt()) {
            return JOB_ATTR;
        } else if (value == IppDelimiterTag.OPERATION_ATTR.asInt()) {
            return OPERATION_ATTR;
        } else if (value == IppDelimiterTag.PRINTER_ATTR.asInt()) {
            return PRINTER_ATTR;
        } else if (value == IppDelimiterTag.UNSUPP_ATTR.asInt()) {
            return UNSUPP_ATTR;
        } else if (value == IppDelimiterTag.RESERVED_X00.asInt()) {
            return RESERVED_X00;
        } else if (value == IppDelimiterTag.SUBSCRIPTION_ATTR.asInt()) {
            return SUBSCRIPTION_ATTR;
        } else if (value == IppDelimiterTag.EVENT_NOTIFICATION_ATTR.asInt()) {
            return EVENT_NOTIFICATION_ATTR;
        } else if (value == IppDelimiterTag.RESERVED_X08.asInt()) {
            return RESERVED_X08;
        } else if (value == IppDelimiterTag.RESERVED_X09.asInt()) {
            return RESERVED_X09;
        } else if (value == IppDelimiterTag.RESERVED_X0A.asInt()) {
            return RESERVED_X0A;
        } else if (value == IppDelimiterTag.RESERVED_X0B.asInt()) {
            return RESERVED_X0B;
        } else if (value == IppDelimiterTag.RESERVED_X0C.asInt()) {
            return RESERVED_X0C;
        } else if (value == IppDelimiterTag.RESERVED_X0D.asInt()) {
            return RESERVED_X0D;
        } else if (value == IppDelimiterTag.RESERVED_X0E.asInt()) {
            return RESERVED_X0E;
        } else if (value == IppDelimiterTag.RESERVED_X0F.asInt()) {
            return RESERVED_X0F;
        }
        throw new SpException("value [" + value
                + "] can not be converted to enum");
    }

}
