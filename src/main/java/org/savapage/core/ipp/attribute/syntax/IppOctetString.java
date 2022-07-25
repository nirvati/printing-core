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

import org.savapage.core.ipp.encoding.IppValueTag;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class IppOctetString extends AbstractIppAttrSyntax {

    /**
     * If {@code null} maximum is unlimited.
     */
    @SuppressWarnings("unused")
    final private Integer maxOctets;

    /** */
    private static class SingletonHolder {
        /** */
        public static final IppOctetString INSTANCE = new IppOctetString();
    }

    public IppOctetString() {
        this.maxOctets = null;
    }

    public IppOctetString(int maxOctets) {
        this.maxOctets = maxOctets;
    }

    /**
     * @return The singleton instance.
     */
    public static IppOctetString instance() {
        return SingletonHolder.INSTANCE;
    }

    @Override
    public IppValueTag getValueTag() {
        return IppValueTag.OCTETSTRING;
    }

    @Override
    public void write(final OutputStream ostr, final String value,
            final Charset charset) throws IOException {
        writeUsAscii(ostr, value);
    }

    /**
     *
     * @param bytes
     * @return
     */
    public static String read(byte[] bytes) {
        return readUsAscii(bytes);
    }
}
