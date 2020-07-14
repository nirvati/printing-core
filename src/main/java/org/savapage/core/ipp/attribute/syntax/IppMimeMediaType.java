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
 * Internet Media Type (sometimes called MIME type) as defined by
 * <a href="http://tools.ietf.org/html/rfc2046">RFC2046</a> and registered
 * according to the procedures of
 * <a href="http://tools.ietf.org/html/rfc2048">RFC2048</a> for identifying a
 * document format.
 *
 * @author Rijk Ravestein
 *
 */
public final class IppMimeMediaType extends AbstractIppAttrSyntax {

    /**
     * Portable Document Format.
     */
    public static final String PDF = "application/pdf";

    /** */
    private static class SingletonHolder {
        /** */
        public static final IppMimeMediaType INSTANCE = new IppMimeMediaType();
    }

    /**
     * @return The singleton instance.
     */
    public static IppMimeMediaType instance() {
        return SingletonHolder.INSTANCE;
    }

    @Override
    public IppValueTag getValueTag() {
        return IppValueTag.MIMETYPE;
    }

    @Override
    public void write(final OutputStream ostr, final String value,
            final Charset charset) throws IOException {
        /*
         * Ignore the offered charset, use US_ASCII instead.
         */
        writeUsAscii(ostr, value);
    }

}
