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
 * @author Datraverse B.V.
 *
 */
public class IppAttr {

    protected static final boolean REQUIRED = true;
    protected static final boolean OPTIONAL = false;

    /*
     * The value of an attribute in a response (but not in a request) MAY be one
     * of the "out-of-band" values whose special encoding rules are defined in
     * the "Encoding and Transport" document [RFC2910]. Standard "out-of-band"
     * values are:
     *
     * 'unknown': The attribute is supported by the IPP object, but the value is
     * unknown to the IPP object for some reason.
     *
     * 'unsupported': The attribute is unsupported by the IPP object. This value
     * MUST be returned only as the value of an attribute in the Unsupported
     * Attributes Group.
     *
     * 'no-value': The attribute is supported by the Printer object, but the
     * administrator has not yet configured a value.
     *
     * Thus clients MUST NOT supply attributes with "out-of-band" values.
     *
     * All attributes in a response MUST have one or more values as defined in
     * Sections 4.2 to 4.4 or a single "out-of-band" value.
     */

    /**
     * Unique keyword (name) identifying the attribute.
     * <p>
     * Note: Not only are keywords used to identify attributes, but one of the
     * attribute syntaxes described below is "keyword" so that some attributes
     * have keyword values. Therefore, these attributes are defined as having an
     * attribute syntax that is a set of keywords.
     * </p>
     */
    private final String keyword;
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
