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
package org.savapage.core.ipp.attribute;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.savapage.core.ipp.attribute.syntax.AbstractIppAttrSyntax;

/**
 * A wrapper for an {@link IppAttr} with a list of values.
 *
 * <p>
 * TODO: Each value in an attribute can have a different type.
 * </p>
 *
 * @author Rijk Ravestein
 *
 */
public final class IppAttrValue {

    private IppAttr attribute;

    private List<String> values = new ArrayList<>();

    /**
     *
     * @param keyword
     * @param syntax
     */
    public IppAttrValue(final String keyword,
            final AbstractIppAttrSyntax syntax) {
        this.setAttribute(new IppAttr(keyword, syntax));
    }

    /**
     *
     * @param attribute
     */
    public IppAttrValue(IppAttr attribute) {
        this.setAttribute(attribute);
    }

    public List<String> getValues() {
        return values;
    }

    /**
     * Gets the first (and only) value on the list.
     *
     * @return {@code null} when the list of values does NOT have exactly ONE
     *         element.
     */
    public String getSingleValue() {
        String value = null;
        if (values.size() == 1) {
            value = values.get(0);
        }
        return value;
    }

    public void setValues(List<String> values) {
        this.values = values;
    }

    public void addValue(final String value) {
        values.add(value);
    }

    /**
     *
     * @param value
     * @return
     */
    public boolean removeValue(final String value) {
        /*
         * Just in case, remove all occurrences.
         */
        boolean found = false;
        while (values.remove(value)) {
            found = true;
        }
        return found;
    }

    public IppAttr getAttribute() {
        return attribute;
    }

    public void setAttribute(IppAttr attribute) {
        this.attribute = attribute;
    }

    /**
     * Writes attribute value to the output stream.
     *
     * TODO: Each value in an attribute can have a different type!!!
     *
     * @param ostr
     * @param value
     * @param charset
     * @throws IOException
     */
    public void write(final OutputStream ostr, final String value,
            final Charset charset) throws IOException {
        getAttribute().getSyntax().write(ostr, value, charset);
    }

}
