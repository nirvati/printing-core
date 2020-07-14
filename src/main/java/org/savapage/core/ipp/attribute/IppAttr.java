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

import org.savapage.core.ipp.attribute.syntax.AbstractIppAttrSyntax;

/**
 * An IPP attribute definition identified by a keyword and typed by an
 * {@link AbstractIppAttrSyntax}.
 *
 * <p>
 * Most attributes are defined to have a single attribute syntax. However, a few
 * attributes (e.g., "job-sheet", "media", "job-hold- until") are defined to
 * have several attribute syntaxes, depending on the value.
 * </p>
 * <p>
 * These multiple attribute syntaxes are separated by the "|" character in the
 * sub-section heading to indicate the choice.
 * </p>
 * <p>
 * Since each value MUST be tagged as to its attribute syntax in the protocol, a
 * single-valued attribute instance may have any one of its attribute syntaxes
 * and a multi-valued attribute instance may have a mixture of its defined
 * attribute syntaxes.
 * </p>
 *
 * @author Rijk Ravestein
 *
 */
public final class IppAttr {

    /** */
    protected static final boolean REQUIRED = true;
    /** */
    protected static final boolean OPTIONAL = false;

    /**
     * Unique keyword (name) identifying the attribute.
     */
    private final String keyword;
    /** */
    private final AbstractIppAttrSyntax syntax;

    /**
     *
     * @param keyword
     * @param syntax
     */
    public IppAttr(final String keyword, final AbstractIppAttrSyntax syntax) {
        this.keyword = keyword;
        this.syntax = syntax;
    }

    /**
     * Create a new instance with a new keyword and a shallow copy of the
     * {@link AbstractIppAttrSyntax}.
     *
     * @param keyword
     *            The keyword of the new copy.
     * @return The new instance.
     */
    public IppAttr copy(final String keyword) {
        return new IppAttr(keyword, this.getSyntax());
    }

    /**
     *
     * @return
     */
    public String getKeyword() {
        return keyword;
    }

    /**
     * Gets the syntax of the attribute.
     *
     * @return The syntax.
     */
    public AbstractIppAttrSyntax getSyntax() {
        return syntax;
    }

}
