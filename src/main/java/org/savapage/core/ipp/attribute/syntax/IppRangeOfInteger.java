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
package org.savapage.core.ipp.attribute.syntax;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.StringTokenizer;

import org.savapage.core.SpException;
import org.savapage.core.ipp.encoding.IppEncoder;
import org.savapage.core.ipp.encoding.IppValueTag;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class IppRangeOfInteger extends AbstractIppAttrSyntax {

    /** */
    private static class SingletonHolder {
        /** */
        public static final IppRangeOfInteger INSTANCE =
                new IppRangeOfInteger();
    }

    /**
     * Gets the singleton instance.
     *
     * @return
     */
    public static IppRangeOfInteger instance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     *
     * @param min
     * @param max
     * @return
     */
    public static String format(int min, int max) {
        return String.valueOf(min) + ":" + String.valueOf(max);
    }

    @Override
    public IppValueTag getValueTag() {
        return IppValueTag.INTRANGE;
    }

    @Override
    public void write(final OutputStream ostr, final String formattedMinMax,
            final Charset charset) throws IOException {
        /*
         * Eight octets consisting of 2 SIGNED-INTEGERs. The first
         * SIGNED-INTEGER contains the lower bound and the second SIGNED-INTEGER
         * contains the upper bound.
         */
        StringTokenizer st = new StringTokenizer(formattedMinMax, ":");
        if (st.countTokens() != 2) {
            throw new SpException("value [" + formattedMinMax
                    + "] is not a valid RangeOfInteger");
        }
        IppEncoder.writeInt16(ostr, 8);
        while (st.hasMoreTokens()) {
            IppEncoder.writeInt32(ostr, Integer.parseInt(st.nextToken()));
        }
    }

}
