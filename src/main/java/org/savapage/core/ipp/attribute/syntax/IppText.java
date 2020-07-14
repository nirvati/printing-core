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
 * A text attribute is an attribute whose value is a sequence of zero or more
 * characters encoded in a maximum of 1023 ('MAX') octets.
 *
 * @author Rijk Ravestein
 *
 */
public final class IppText extends AbstractIppAttrSyntax {

    /** */
    private static class SingletonHolder {
        /** */
        public static final IppText INSTANCE = new IppText();
    }

    /*
     * ... 'text' is used only for brevity; the formal interpretation of 'text'
     * is: 'textWithoutLanguage | textWithLanguage'.
     *
     * That is, for any attribute defined in this document using the 'text'
     * attribute syntax, all IPP objects and clients MUST support both the
     * 'textWithoutLanguage' and 'textWithLanguage' attribute syntaxes.
     *
     * However, in actual usage and protocol execution, objects and clients
     * accept and return only one of the two syntax per attribute. The syntax
     * 'text' never appears "on-the-wire".
     */

    /**
     * 1023 ('MAX') octets
     */
    public static final int MAX = 1023;

    int maxSize = MAX;

    public IppText() {
        this.maxSize = MAX;
    }

    public IppText(int maxSize) {
        this.maxSize = maxSize;
    }

    /**
     * @return The singleton instance.
     */
    public static IppText instance() {
        return SingletonHolder.INSTANCE;
    }

    @Override
    public IppValueTag getValueTag() {
        /*
         * Generally, one natural language applies to all text attributes in a
         * given request or response. The language is indicated by the
         * "attributes-natural-language" operation attribute defined in section
         * 3.1.4 or "attributes-natural-language" job attribute defined in
         * section 4.3.20, and there is no need to identify the natural language
         * for each text string on a value-by-value basis.
         */
        return IppValueTag.TEXTWOLANG;
    }

    @Override
    public void write(final OutputStream ostr, final String value,
            final Charset charset) throws IOException {
        write(ostr, value.getBytes(charset));
    }

}
