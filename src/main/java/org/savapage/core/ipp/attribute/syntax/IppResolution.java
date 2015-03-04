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
package org.savapage.core.ipp.attribute.syntax;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

import org.apache.commons.lang3.StringUtils;
import org.savapage.core.ipp.encoding.IppEncoder;
import org.savapage.core.ipp.encoding.IppValueTag;

/**
 * OCTET-STRING consisting of nine octets of 2 SIGNED-INTEGERs followed by a
 * SIGNED-BYTE.
 * <ul>
 * <li>The first SIGNED-INTEGER contains the value of cross feed direction
 * resolution.</li>
 * <li>The second SIGNED-INTEGER contains the value of feed direction
 * resolution.</li>
 * <li>The SIGNED-BYTE contains the units.</li>
 * </ul>
 *
 * @author Datraverse B.V.
 */
public class IppResolution extends AbstractIppAttrSyntax {

    /**
     * The SingletonHolder is loaded on the first execution of
     * {@link IppResolution#instance()} or the first access to
     * {@link SingletonHolder#INSTANCE}, not before.
     * <p>
     * <a href=
     * "http://en.wikipedia.org/wiki/Singleton_pattern#The_solution_of_Bill_Pugh"
     * >The Singleton solution of Bill Pugh</a>
     * </p>
     */
    private static class SingletonHolder {
        public static final IppResolution INSTANCE = new IppResolution();
    }

    /**
     * Dots per inch.
     */
    public static final int DPI = 3;

    /**
     * Dots per centimeter.
     */
    public static final int DPC = 4;

    /**
     * Dots per inch.
     */
    private static final String FORMAT_DPI = "DPI";

    /**
     * Dots per centimeter.
     */
    private static final String FORMAT_DPC = "DPC";

    private static final char FORMAT_X = 'x';

    /**
     * Gets the singleton instance.
     *
     * @return
     */
    public static IppResolution instance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     *
     * @param x
     * @param y
     * @param z
     * @return
     */
    private static String format(int x, int y, int z) {
        return String.format("%d%c%d %s", x, FORMAT_X, y,
                (z == DPI ? FORMAT_DPI : FORMAT_DPC));
    }

    /**
     *
     * @param resolution
     * @return
     */
    private static int[] parse(final String resolution) {
        int[] value = new int[3];

        String[] xyz = StringUtils.split(resolution); // split on ' '
        String[] xy = StringUtils.split(xyz[0], FORMAT_X);

        value[0] = Integer.parseInt(xy[0]);
        value[1] = Integer.parseInt(xy[1]);

        value[2] = xyz[1].equals(FORMAT_DPI) ? DPI : DPC;

        return value;
    }

    @Override
    public final IppValueTag getValueTag() {
        return IppValueTag.RESOLUTION;
    }

    @Override
    public final void write(final OutputStream ostr, final String value,
            final Charset charset) throws IOException {

        int[] resolution = parse(value);

        IppEncoder.writeInt16(ostr, 9); // length

        IppEncoder.writeInt32(ostr, resolution[0]);
        IppEncoder.writeInt32(ostr, resolution[1]);
        IppEncoder.writeInt8(ostr, resolution[2]);

    }

    /**
     * Reads encoded IPP bytes and constructs a formatted resolution.
     *
     * @param bytes
     *            The encoded IPP resolution.
     * @return The formatted resolution.
     */
    public static String read(byte[] bytes) {

        int x = IppEncoder.readInt32(bytes[0], bytes[1], bytes[2], bytes[3]);
        int y = IppEncoder.readInt32(bytes[4], bytes[5], bytes[6], bytes[7]);
        int z = bytes[8];

        return format(x, y, z);
    }

}
