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
import java.lang.management.ManagementFactory;
import java.nio.charset.Charset;

import org.savapage.core.ipp.encoding.IppEncoder;
import org.savapage.core.ipp.encoding.IppValueTag;
import org.savapage.core.util.DateUtil;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class IppInteger extends AbstractIppAttrSyntax {

    public static final int MIN = -2 ^ 31;
    public static final int MAX = 2 ^ 31 - 1;

    int min = MIN;
    int max = MAX;

    private IppInteger() {
    }

    /** */
    private static class SingletonHolder {
        /** */
        public static final IppInteger INSTANCE = new IppInteger();
    }

    public IppInteger(int min) {
        this.min = min;
    }

    public IppInteger(int min, int max) {
        this.min = min;
        this.max = max;
    }

    /**
     * @return The singleton instance.
     */
    public static IppInteger instance() {
        return SingletonHolder.INSTANCE;
    }

    @Override
    public IppValueTag getValueTag() {
        return IppValueTag.INTEGER;
    }

    @Override
    public void write(final OutputStream ostr, final String value,
            final Charset charset) throws IOException {
        IppEncoder.writeInt16(ostr, 4); // length
        IppEncoder.writeInt32(ostr, Integer.parseInt(value)); // value
    }

    /**
     * @return IPP Printer uptime in seconds.
     */
    public static int getPrinterUpTime() {
        return (int) (ManagementFactory.getRuntimeMXBean().getUptime()
                / DateUtil.DURATION_MSEC_SECOND);
    }
}
