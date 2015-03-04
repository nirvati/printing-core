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
 * The 'name' attribute syntax is essentially the same as 'text', including the
 * REQUIRED support of UTF-8 except that the sequence of characters is limited
 * so that its encoded form MUST NOT exceed 255 (MAX) octets.
 *
 * @author Datraverse B.V.
 */
public class IppName extends AbstractIppAttrSyntax {

    /**
     * The SingletonHolder is loaded on the first execution of
     * {@link IppName#instance()} or the first access to
     * {@link SingletonHolder#INSTANCE}, not before.
     * <p>
     * <a href=
     * "http://en.wikipedia.org/wiki/Singleton_pattern#The_solution_of_Bill_Pugh"
     * >The Singleton solution of Bill Pugh</a>
     * </p>
     */
    private static class SingletonHolder {
        public static final IppName INSTANCE = new IppName();
    }

    public static final int MAX = 255;

    int maxSize = MAX;

    public IppName() {
        this.maxSize = MAX;
    }

    public IppName(int maxSize) {
        this.maxSize = maxSize;
    }

    /**
     * Gets the singleton instance.
     *
     * @return
     */
    public static IppName instance() {
        return SingletonHolder.INSTANCE;
    }

    @Override
    public final IppValueTag getValueTag() {
        return IppValueTag.NAMEWOLANG;
    }

    @Override
    public final void write(final OutputStream ostr, final String value,
            final Charset charset) throws IOException {
        write(ostr, value.getBytes(charset));
    }

}
