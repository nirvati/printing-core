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
 * The 'charset' attribute syntax is a standard identifier for a charset. <a
 * href="http://tools.ietf.org/html/rfc2911">RFC2911</a>
 * <p>
 * A charset is a coded character set and encoding scheme. Charsets are used for
 * labeling certain document contents and 'text' and 'name' attribute values.
 * The syntax and semantics of this attribute syntax are specified in RFC2046
 * and contained in the IANA character-set Registry [IANA-CS] according to the
 * IANA procedures [RFC2278]. Though RFC2046 requires that the values be
 * case-insensitive US-ASCII [ASCII], IPP requires all lower case values in IPP
 * attributes to simplify comparing by IPP clients and Printer objects. When a
 * character-set in the IANA registry has more than one name (alias), the name
 * labeled as "(preferred MIME name)", if present, MUST be used.
 * </p>
 * <p>
 * The maximum length of 'charset' values used to represent IPP attribute values
 * is 63 octets.
 * </p>
 * <p>
 * Some examples are:
 * </p>
 * <p>
 * 'utf-8': ISO 10646 Universal Multiple-Octet Coded Character Set (UCS)
 * represented as the UTF-8 [RFC2279] transfer encoding scheme in which US-ASCII
 * is a subset charset.
 * </p>
 * <p>
 * 'us-ascii': 7-bit American Standard Code for Information Interchange (ASCII),
 * ANSI X3.4-1986 [ASCII]. That standard defines US-ASCII, but RFC 2045
 * [RFC2045] eliminates most of the control characters from conformant usage in
 * MIME and IPP.
 * </p>
 * <p>
 * 'iso-8859-1': 8-bit One-Byte Coded Character Set, Latin Alphabet Nr 1
 * [ISO8859-1]. That standard defines a coded character set that is used by
 * Latin languages in the Western Hemisphere and Western Europe. US-ASCII is a
 * subset charset.
 *
 * @author Datraverse B.V.
 */
public class IppCharset extends AbstractIppAttrSyntax {

    /**
     * The SingletonHolder is loaded on the first execution of
     * {@link IppCharset#instance()} or the first access to
     * {@link SingletonHolder#INSTANCE}, not before.
     * <p>
     * <a href=
     * "http://en.wikipedia.org/wiki/Singleton_pattern#The_solution_of_Bill_Pugh"
     * >The Singleton solution of Bill Pugh</a>
     * </p>
     */
    private static class SingletonHolder {
        public static final IppCharset INSTANCE = new IppCharset();
    }

    /**
     * Gets the singleton instance.
     *
     * @return
     */
    public static IppCharset instance() {
        return SingletonHolder.INSTANCE;
    }

    @Override
    public final IppValueTag getValueTag() {
        return IppValueTag.CHARSET;
    }

    @Override
    public final void write(final OutputStream ostr, final String value,
            final Charset charset) throws IOException {
        writeUsAscii(ostr, value);
    }

}
