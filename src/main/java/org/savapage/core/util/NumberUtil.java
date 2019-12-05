/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2019 Datraverse B.V.
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
package org.savapage.core.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Locale;
import java.util.Random;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class NumberUtil {

    /**  */
    public static final int RADIX_10 = 10;

    /**  */
    public static final int INT_HUNDRED = 100;

    /** */
    public static final int INT_THOUSAND = 1000;

    /**
     *
     */
    private NumberUtil() {
    }

    /**
     * Converts a BigDecimal to a BigInteger.
     *
     * @param bd
     *            The {@link BigDecimal}.
     * @return {@code null} if this BigDecimal has a nonzero fractional part.
     */
    public static BigInteger toBigIntegerExact(final BigDecimal bd) {
        try {
            return bd.toBigIntegerExact();
        } catch (ArithmeticException ex) {
            return null;
        }
    }

    /**
     * Gets a random number within a number range.
     *
     * @param min
     *            The range minimum.
     * @param max
     *            The range maximum.
     * @return the random number in the range.
     */
    public static int getRandomNumber(final int min, final int max) {
        return new Random().nextInt(max - min + 1) + min;
    }

    /**
     * Formats byte size to human readable format. SI (1 k = 1,000).
     *
     * <p>
     * As explained <a href=
     * "https://programming.guide/java/formatting-byte-size-to-human-readable-format.html">here</a>
     * and on <a href=
     * "https://stackoverflow.com/questions/3758606/how-to-convert-byte-size-into
     * - human-readable-format-in-java ">stackoverflow</a>.
     * </p>
     *
     * @param locale
     *            {@link Locale}.
     * @param bytes
     *            Number of bytes.
     * @return The formatted count.
     */
    public static String humanReadableByteCountSI(final Locale locale,
            final long bytes) {
        // @formatter:off
        String s = bytes < 0 ? "-" : "";
        long b = bytes == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(bytes);
        return b < 1000L ? bytes + " B"
                : b < 999_950L ? String.format(locale, "%s%.1f kB", s, b / 1e3)
                : (b /= 1000) < 999_950L ? String.format(locale, "%s%.1f MB", s, b / 1e3)
                : (b /= 1000) < 999_950L ? String.format(locale, "%s%.1f GB", s, b / 1e3)
                : (b /= 1000) < 999_950L ? String.format(locale, "%s%.1f TB", s, b / 1e3)
                : (b /= 1000) < 999_950L ? String.format(locale, "%s%.1f PB", s, b / 1e3)
                : String.format(locale, "%s%.1f EB", s, b / 1e6);
        // @formatter:on
    }

    /**
     * Formats byte size to human readable format. Binary (1 K = 1,024).
     *
     * <p>
     * As explained <a href=
     * "https://programming.guide/java/formatting-byte-size-to-human-readable-format.html">here</a>
     * and on <a href=
     * "https://stackoverflow.com/questions/3758606/how-to-convert-byte-size-into
     * - human-readable-format-in-java ">stackoverflow</a>.
     * </p>
     *
     * @param locale
     *            {@link Locale}.
     * @param bytes
     *            Number of bytes.
     * @return The formatted count.
     */
    public static String humanReadableByteCountBin(final Locale locale,
            final long bytes) {
        // @formatter:off
        long b = bytes == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(bytes);
        return b < 1024L ? bytes + " B"
                : b < 0xfffccccccccccccL >> 40 ? String.format(locale, "%.1f KiB", bytes / 0x1p10)
                : b < 0xfffccccccccccccL >> 30 ? String.format(locale, "%.1f MiB", bytes / 0x1p20)
                : b < 0xfffccccccccccccL >> 20 ? String.format(locale, "%.1f GiB", bytes / 0x1p30)
                : b < 0xfffccccccccccccL >> 10 ? String.format(locale, "%.1f TiB", bytes / 0x1p40)
                : b < 0xfffccccccccccccL ? String.format(locale, "%.1f PiB", (bytes >> 10) / 0x1p40)
                : String.format(locale, "%.1f EiB", (bytes >> 20) / 0x1p40);
        // @formatter:on
    }
}
