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
package org.savapage.core.ipp;

/**
 * printer-type (type2 enum)
 * <p>
 * The printer-type attribute specifies printer type and capability bits for the
 * printer or class. The default value is computed from internal state
 * information and the PPD file for the printer.
 * </p>
 *
 * @author Datraverse B.V.
 *
 */
public class IppPrinterType {

    private int value = 0;

    /**
     *
     */
    public enum BitEnum {
        /**
         * Is a printer class.
         */
        PRINTER_CLASS(0x00000001),

        /**
         * Is a remote destination.
         */
        REMOTE_DESTINATION(0x00000002),

        /**
         * Can print in black.
         */
        CAN_PRINT_IN_BLACK(0x00000004),

        /**
         * Can print in color.
         */
        CAN_PRINT_IN_COLOR(0x00000008),

        /**
         * Can print on both sides of the page in hardware.
         */
        CAN_PRINT_DUPLEX(0x00000010),

        /**
         * Can staple output.
         */
        CAN_STAPLE_OUTPUT(0x00000020),

        /**
         * Can do fast copies in hardware.
         */
        CAN_DO_FAST_COPIES(0x00000040),

        /**
         * Can do fast copy collation in hardware.
         */
        CAN_DO_COPY_COLLATION(0x00000080),

        /**
         * Can punch output.
         */
        CAN_PUNCH_OUTPUT(0x00000100),

        /**
         * Can cover output.
         */
        CAN_COVER_OUTPUT(0x00000200),

        /**
         * Can bind output.
         */
        CAN_BIND_OUTPUT(0x00000400),

        /**
         * Can sort output.
         */
        CAN_SORT_OUTPUT(0x00000800),

        /**
         * Can handle media up to US-Legal/A4.
         */
        CAN_HANDLE_MEDIA_UPTO_LEGAL_A4(0x00001000),

        /**
         * Can handle media from US-Legal/A4 to ISO-C/A2.
         */
        CAN_HANDLE_MEDIA_FROM_A4_TO_A2(0x00002000),

        /**
         * Can handle media larger than ISO-C/A2.
         */
        CAN_HANDLE_MEDIA_GT_A2(0x00004000),

        /**
         * Can handle user-defined media sizes.
         */
        CAN_HANDLE_USER_DEFINED_MEDIA(0x00008000),

        /**
         * Is an implicit (server-generated) class.
         */
        IMPLICIT_CLASS(0x00010000);

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
        BitEnum(final int value) {
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

    }

    /**
     *
     */
    public IppPrinterType() {

    }

    /**
     *
     * @param value
     */
    public IppPrinterType(int value) {
        this.value = value;
    }

    /**
     *
     */
    public int set(BitEnum bit) {
        value |= bit.asInt();
        return value;
    }

    /**
     *
     * @param bit
     * @return
     */
    public boolean is(BitEnum bit) {
        return (value & bit.asInt()) != 0;
    }

    /**
     *
     * @return
     */
    public int getValue() {
        return value;
    }
}
