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

import org.savapage.core.ipp.encoding.IppValueTag;

/**
 * Syntax for a natural language and optionally a country.
 * <p>
 * The values for this syntax type are defined by RFC 1766 [RFC1766]. Though
 * RFC1766 requires that the values be case-insensitive US-ASCII [ASCII], IPP
 * requires all lower case to simplify comparing by IPP clients and Printer
 * objects. Examples include:
 * </p>
 * <ul>
 * <li>'en': for English</li>
 * <li>'en-us': for US English</li>
 * <li>'fr': for French</li>
 * <li>'de': for German</li>
 * </ul>
 * <p>
 * The maximum length of 'naturalLanguage' values used to represent IPP
 * attribute values is 63 octets.
 * </p>
 *
 * @author Datraverse B.V.
 */
public class IppNaturalLanguage extends AbstractIppAttrSyntax {

    /**
     * The SingletonHolder is loaded on the first execution of
     * {@link IppNaturalLanguage#instance()} or the first access to
     * {@link SingletonHolder#INSTANCE}, not before.
     * <p>
     * <a href=
     * "http://en.wikipedia.org/wiki/Singleton_pattern#The_solution_of_Bill_Pugh"
     * >The Singleton solution of Bill Pugh</a>
     * </p>
     */
    private static class SingletonHolder {
        public static final IppNaturalLanguage INSTANCE =
                new IppNaturalLanguage();
    }

    /**
     * Gets the singleton instance.
     *
     * @return
     */
    public static IppNaturalLanguage instance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     * 63 ('MAX') octets
     */
    public static final int MAX = 63;

    @Override
    public final IppValueTag getValueTag() {
        return IppValueTag.NATULANG;
    }

    @Override
    public final void write(final OutputStream ostr, final String value,
            final Charset charset) throws IOException {
        writeUsAscii(ostr, value);
    }

}
