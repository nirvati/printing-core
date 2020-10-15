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
package org.savapage.core.rfid;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp;
import org.savapage.core.config.IConfigProp.Key;

/**
 *
 * @author Rijk Ravestein
 *
 */
public class RfidNumberFormat {

    public final static int MIN_LENGTH_HEX = 8;
    public final static int MIN_LENGTH_DEC = 10;

    private final static Pattern PATTERN_CHECKSUM =
            Pattern.compile("(\\d*)=\\d*");

    public enum Format {
        /**
         * Decimal.
         */
        DEC,
        /**
         * Hexadecimal.
         */
        HEX
    }

    /**
     * Typing of the first byte of an RFID number.
     */
    public enum FirstByte {
        /**
         * Least Significant Byte.
         */
        LSB,
        /**
         * Most Significant Byte.
         */
        MSB
    }

    private final Format format;
    private final FirstByte firstByte;

    /**
     * Creates an instance using the system defaults.
     */
    public RfidNumberFormat() {

        ConfigManager cm = ConfigManager.instance();

        if (cm.getConfigValue(Key.CARD_NUMBER_FIRST_BYTE)
                .equals(IConfigProp.CARD_NUMBER_FIRSTBYTE_V_LSB)) {
            this.firstByte = RfidNumberFormat.FirstByte.LSB;
        } else {
            this.firstByte = RfidNumberFormat.FirstByte.MSB;
        }

        if (cm.getConfigValue(Key.CARD_NUMBER_FORMAT)
                .equals(IConfigProp.CARD_NUMBER_FORMAT_V_HEX)) {
            this.format = RfidNumberFormat.Format.HEX;
        } else {
            this.format = RfidNumberFormat.Format.DEC;
        }
    }

    /**
     * @param format
     * @param firstByte
     */
    public RfidNumberFormat(Format format, FirstByte firstByte) {
        this.format = format;
        this.firstByte = firstByte;
    }

    /**
     * Checks if this is a valid card number.
     *
     * @param number
     *            The raw card number.
     * @return {@code true} if valid.
     */
    public boolean isNumberValid(final String number) {
        boolean isValid = true;
        if (StringUtils.isBlank(number)) {
            isValid = false;
        } else {
            try {
                getNormalizedNumber(number);
            } catch (NumberFormatException e) {
                isValid = false;
            }
        }
        return isValid;
    }

    /**
     * Gets the Rfid number as {@link Format#HEX} and {@link FirstByte#LSB}.
     *
     * <p>
     * NOTE: A '\' or '[' character seems to slip-in for decimal numbers at
     * Local (keyboard) Readers. Is this a JavaScript artifact? Anyway, all
     * these characters are removed from the number before processing further.
     *
     * </p>
     * <p>
     * Some card readers append header and trailer characters to the card
     * number, so these need to be removed to retrieve the real card number.
     * Examples:
     * </p>
     * <ul>
     * <li>WT20 card reader and numeric keypad: header ';' and trailer '?'</li>
     * <li>Cherry keyboards: header '*' and trailer '/'</li>
     * </ul>
     * <p>
     * Some readers report a checksum after the card number, separated by an
     * equals sign, e.g. 4234685818=8. This checksum part ("=8') will be
     * ignored.
     * </p>
     *
     * @param number
     *            The raw card number.
     * @return The normalized RFID number.
     * @throws NumberFormatException
     *             If the number is not in decimal or hex format, or when number
     *             string length is less than minimum required
     *             {@link #MIN_LENGTH_DEC} and {@link #MIN_LENGTH_HEX}.
     */
    public String getNormalizedNumber(final String number) {

        String numberRaw = number;

        if (StringUtils.isBlank(numberRaw)) {
            throw new NumberFormatException("card number is empty");
        }

        /*
         * Remove header char.
         */
        char first = numberRaw.charAt(0);
        if (first == ';' || first == '*') {
            numberRaw = numberRaw.substring(1);
        }

        /*
         * Remove header char.
         */
        char last = numberRaw.charAt(numberRaw.length() - 1);
        if (last == '?' || last == '/') {
            numberRaw = StringUtils.removeEnd(numberRaw, String.valueOf(last));
        }

        /*
         * Remove checksum.
         */
        Matcher matcher = PATTERN_CHECKSUM.matcher(numberRaw);
        if (matcher.find()) {
            numberRaw = matcher.group(1);
        }

        /*
         * Remove any '\' and '[' characters, which seems to slip-in for decimal
         * numbers at Local (keyboard) Readers (a JavaScript artifact?).
         */
        numberRaw = StringUtils.remove(numberRaw, '\\');
        numberRaw = StringUtils.remove(numberRaw, '[');

        /*
         * CHECK: length
         */
        int minLength;

        if (format == Format.HEX) {
            minLength = MIN_LENGTH_HEX;
        } else {
            minLength = MIN_LENGTH_DEC;
        }

        if (numberRaw.length() < minLength) {
            throw new NumberFormatException("card number [" + numberRaw
                    + "] length is shorter than minimum [" + minLength + "].");
        }

        /*
         * CHECK format.
         */
        int radix = 10;

        if (format == Format.HEX) {
            radix = 16;
        }

        long decimal = Long.parseLong(numberRaw, radix);

        /*
         *
         */
        if (format == Format.HEX && firstByte == FirstByte.LSB) {

            return numberRaw.toLowerCase();

        }

        if (firstByte == FirstByte.MSB) {
            decimal = Long.reverseBytes(decimal);
        }

        return StringUtils.removeEnd(Long.toHexString(decimal), "00000000");

    }

    /**
     *
     * @param value
     * @return {@code null} when not found.
     */
    public static FirstByte toFirstByte(final String value) {

        RfidNumberFormat.FirstByte firstByte = null;

        if (value.equals(IConfigProp.CARD_NUMBER_FIRSTBYTE_V_LSB)) {
            firstByte = RfidNumberFormat.FirstByte.LSB;
        } else if (value.equals(IConfigProp.CARD_NUMBER_FIRSTBYTE_V_MSB)) {
            firstByte = RfidNumberFormat.FirstByte.MSB;
        }
        return firstByte;
    }

    /**
     *
     * @param value
     * @return {@code null} when not found.
     */
    public static Format toFormat(final String value) {

        RfidNumberFormat.Format format = null;

        if (value.equals(IConfigProp.CARD_NUMBER_FORMAT_V_HEX)) {
            format = RfidNumberFormat.Format.HEX;
        } else if (value.equals(IConfigProp.CARD_NUMBER_FORMAT_V_DEC)) {
            format = RfidNumberFormat.Format.DEC;
        }
        return format;
    }

    public Format getFormat() {
        return format;
    }

    public FirstByte getFirstByte() {
        return firstByte;
    }

}
