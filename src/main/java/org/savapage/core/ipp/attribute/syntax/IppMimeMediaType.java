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
 * Internet Media Type (sometimes called MIME type) as defined by <a
 * href="http://tools.ietf.org/html/rfc2046">RFC2046</a> and registered
 * according to the procedures of <a
 * href="http://tools.ietf.org/html/rfc2048">RFC2048</a> for identifying a
 * document format.
 * <p>
 * The value MAY include a charset, or other, parameter, depending on the
 * specification of the Media Type in the <a
 * href="http://tools.ietf.org/html/rfc2911#ref-IANA-MT">IANA Registry</a>.
 * Although most other IPP syntax types allow for only lower-cased values, this
 * syntax type allows for mixed-case values which are case-insensitive.
 * </p>
 * <p>
 * Examples are:
 *
 * <pre>
 * 'text/html': An HTML document
 * 'text/plain': A plain text document in US-ASCII (RFC 2046
 *    indicates that in the absence of the charset parameter MUST
 *    mean US-ASCII rather than simply unspecified) [RFC2046].
 * 'text/plain; charset=US-ASCII':  A plain text document in US-ASCII
 *    [52, 56].
 * 'text/plain; charset=ISO-8859-1':  A plain text document in ISO
 *    8859-1 (Latin 1) [ISO8859-1].
 * 'text/plain; charset=utf-8':  A plain text document in ISO 10646
 *    represented as UTF-8 [RFC2279]
 *
 * 'application/postscript':  A PostScript document [RFC2046]
 *
 * 'application/vnd.hp-PCL':  A PCL document [IANA-MT] (charset
 *    escape sequence embedded in the document data)
 *
 * 'application/pdf':  Portable Document Format - see IANA MIME Media
 *    Type registry
 *
 * 'application/octet-stream': Auto-sense - see section 4.1.9.1
 *
 * </pre>
 *
 * </p>
 *
 * @author Datraverse B.V.
 */
public class IppMimeMediaType extends AbstractIppAttrSyntax {

    /**
     * Portable Document Format
     */
    public static final String PDF = "application/pdf";

    /**
     * The SingletonHolder is loaded on the first execution of
     * {@link IppMimeMediaType#instance()} or the first access to
     * {@link SingletonHolder#INSTANCE}, not before.
     * <p>
     * <a href=
     * "http://en.wikipedia.org/wiki/Singleton_pattern#The_solution_of_Bill_Pugh"
     * >The Singleton solution of Bill Pugh</a>
     * </p>
     */
    private static class SingletonHolder {
        public static final IppMimeMediaType INSTANCE = new IppMimeMediaType();
    }

    /**
     * Gets the singleton instance.
     *
     * @return
     */
    public static IppMimeMediaType instance() {
        return SingletonHolder.INSTANCE;
    }

    @Override
    public final IppValueTag getValueTag() {
        return IppValueTag.MIMETYPE;
    }

    @Override
    public final void write(final OutputStream ostr, final String value,
            final Charset charset) throws IOException {
        /*
         * Ignore the offered charset, use US_ASCII instead.
         */
        writeUsAscii(ostr, value);
    }

}
