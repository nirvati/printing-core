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
package org.savapage.core.util;

import java.util.Random;

/**
 *
 * @author Datraverse B.V.
 */
public final class NumberUtil {

    private final static int UNIT_SI_1000 = 1000;
    private final static int UNIT_BINARY_1024 = 1024;

    /**
     *
     */
    private NumberUtil() {
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
     * Formats a byte count into a human readable form.
     * <p>
     * <a href=
     * "http://stackoverflow.com/questions/3758606/how-to-convert-byte-size-into
     * - human-readable-format-in-java
     * ">http://stackoverflow.com/questions/3758606/how-to-convert-byte-size-int
     * o - human-readable-format-in-java</a>
     * </p>
     *
     * @param bytes
     *            The number of bytes.
     * @param si
     *            {@code true} when SI units (1000), or {@code false} when
     *            binary units (1024).
     * @return The formatted count.
     */
    public static String humanReadableByteCount(final long bytes,
            final boolean si) {

        final int unit;

        if (si) {
            unit = UNIT_SI_1000;
        } else {
            unit = UNIT_BINARY_1024;
        }

        if (bytes < unit) {
            return bytes + " B";
        }
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre =
                (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

}
