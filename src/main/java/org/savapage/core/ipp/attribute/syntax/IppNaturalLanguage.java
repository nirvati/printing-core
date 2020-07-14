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
 * Syntax for a natural language and optionally a country.
 * <p>
 * The values for this syntax type are defined by RFC 1766 [RFC1766]. Though
 * RFC1766 requires that the values be case-insensitive US-ASCII [ASCII], IPP
 * requires all lower case to simplify comparing by IPP clients and Printer
 * objects.
 *
 * @author Rijk Ravestein
 *
 */
public final class IppNaturalLanguage extends AbstractIppAttrSyntax {

    /** */
    private static class SingletonHolder {
        /** */
        public static final IppNaturalLanguage INSTANCE =
                new IppNaturalLanguage();
    }

    /**
     * @return The singleton instance.
     */
    public static IppNaturalLanguage instance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     * 63 ('MAX') octets
     */
    public static final int MAX = 63;

    @Override
    public IppValueTag getValueTag() {
        return IppValueTag.NATULANG;
    }

    @Override
    public void write(final OutputStream ostr, final String value,
            final Charset charset) throws IOException {
        writeUsAscii(ostr, value);
    }

}
