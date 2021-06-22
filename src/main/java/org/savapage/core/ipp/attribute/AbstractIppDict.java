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

import java.util.HashMap;
import java.util.Map;

import org.savapage.core.ipp.attribute.syntax.AbstractIppAttrSyntax;
import org.savapage.core.ipp.attribute.syntax.IppBoolean;
import org.savapage.core.ipp.attribute.syntax.IppCharset;
import org.savapage.core.ipp.attribute.syntax.IppDateTime;
import org.savapage.core.ipp.attribute.syntax.IppEnum;
import org.savapage.core.ipp.attribute.syntax.IppInteger;
import org.savapage.core.ipp.attribute.syntax.IppKeyword;
import org.savapage.core.ipp.attribute.syntax.IppMimeMediaType;
import org.savapage.core.ipp.attribute.syntax.IppName;
import org.savapage.core.ipp.attribute.syntax.IppNaturalLanguage;
import org.savapage.core.ipp.attribute.syntax.IppOctetString;
import org.savapage.core.ipp.attribute.syntax.IppOutOfBand;
import org.savapage.core.ipp.attribute.syntax.IppRangeOfInteger;
import org.savapage.core.ipp.attribute.syntax.IppResolution;
import org.savapage.core.ipp.attribute.syntax.IppText;
import org.savapage.core.ipp.attribute.syntax.IppUri;
import org.savapage.core.ipp.attribute.syntax.IppUriScheme;
import org.savapage.core.ipp.encoding.IppValueTag;
import org.savapage.core.services.helpers.PpdExtFileReader;

/**
 * IPP attribute dictionary on attribute name.
 *
 * @author Rijk Ravestein
 *
 */
public abstract class AbstractIppDict {

    /**
     * Dictionary on attribute keyword.
     */
    private final Map<String, IppAttr> dictionary = new HashMap<>();

    /**
     * The prefix for Custom SavaPage IPP attributes.
     */
    protected static final String ORG_SAVAPAGE_ATTR_PFX = "org.savapage-";

    /**
     * Keyword name prefix for Custom User Extensions. NOTE: these are
     * <b>not</b> "native" SavaPage IPP extensions, but installation specific
     * extensions configured by SavaPage Community Members.
     */
    protected static final String ORG_SAVAPAGE_EXT_ATTR_PFX =
            "org.savapage.ext-";

    /**
     * Keyword name prefix for Custom Internal Extensions.
     */
    protected static final String ORG_SAVAPAGE_INT_ATTR_PFX =
            "org.savapage.int-";

    /**
     *
     * @param attributes
     */
    protected final void init(final IppAttr[] attributes) {
        for (IppAttr attribute : attributes) {
            dictionary.put(attribute.getKeyword(), attribute);
        }
    }

    /**
     * @param keyword
     *            The IPP attribute keyword (name).
     * @return {@code true} when keyword is a custom SavaPage attribute, i.e.
     *         whose name starts with {@link #ORG_SAVAPAGE_ATTR_PFX} .
     */
    public static boolean isCustomAttr(final String keyword) {
        return keyword.startsWith(ORG_SAVAPAGE_ATTR_PFX);
    }

    /**
     * @param keyword
     *            The IPP attribute keyword (name).
     * @return {@code true} when keyword is a custom SavaPage INT attribute,
     *         i.e. whose name starts with {@link #ORG_SAVAPAGE_INT_ATTR_PFX}.
     */
    public static boolean isCustomIntAttr(final String keyword) {
        return keyword.startsWith(ORG_SAVAPAGE_INT_ATTR_PFX);
    }

    /**
     * @param keyword
     *            The IPP attribute keyword (name).
     * @return {@code true} when keyword is a custom SavaPage EXT attribute,
     *         i.e. whose name starts with {@link #ORG_SAVAPAGE_EXT_ATTR_PFX}.
     */
    public static boolean isCustomExtAttr(final String keyword) {
        return keyword.startsWith(ORG_SAVAPAGE_EXT_ATTR_PFX);
    }

    /**
     * @param value
     *            The IPP Ext attribute value.
     * @return {@code true} when value is a 'none' value of custom SavaPage EXT
     *         attribute, i.e. whose name starts with
     *         {@link #ORG_SAVAPAGE_EXT_ATTR_PFX} .
     */
    public static boolean isCustomExtAttrValueNone(final String value) {
        return value.equals(IppKeyword.ORG_SAVAPAGE_EXT_ATTR_NONE);
    }

    /**
     * @param keyword
     *            The IPP attribute keyword (name).
     * @return {@code true} when keyword is a SavaPage attribute, i.e. whose
     *         name starts with
     *         {@link IppDictJobTemplateAttr#ORG_SAVAPAGE_ATTR_PFX_COVER}.
     */
    public static boolean isJobCoverAttr(final String keyword) {
        return keyword.startsWith(
                IppDictJobTemplateAttr.ORG_SAVAPAGE_ATTR_COVER_BACK_TYPE)
                || keyword.startsWith(
                        IppDictJobTemplateAttr.ORG_SAVAPAGE_ATTR_COVER_FRONT_TYPE);
    }

    /**
     * @param value
     *            The IPP attribute value.
     * @return {@code true} when value is a 'no-cover' value of SavaPage
     *         attribute, i.e. whose name starts with
     *         {@link IppDictJobTemplateAttr#ORG_SAVAPAGE_ATTR_PFX_COVER}.
     */
    public static boolean isJobCoverAttrValueNoCover(final String value) {
        return value.equals(IppKeyword.ORG_SAVAPAGE_ATTR_COVER_TYPE_NO_COVER);
    }

    /**
     * Creates an {@link IppAttr} object for a PPD option.
     * <p>
     * See {@link PpdExtFileReader}.
     * </p>
     *
     * @param ppdOption
     *            The PPD option (name).
     * @return The attribute object.
     */
    public final IppAttr createPpdOptionAttr(final String ppdOption) {
        /*
         * We always use IppText syntax when passing PPD option as IPP
         * attribute.
         */
        return new IppAttr(ppdOption, IppText.instance());
    }

    /**
     * Returns the default {@link IppAttr} object for the attribute keyword.
     *
     * @param keyword
     *            The IPP attribute keyword (name).
     * @return The attribute object or {@code null} when attribute is not
     *         supported.
     */
    public final IppAttr getAttr(final String keyword) {
        return dictionary.get(keyword);
    }

    /**
     * Returns the {@link IppAttr} object for the attribute keyword typed with a
     * value tag.
     * <p>
     * This method is typically used when reading attributes from a byte stream,
     * i.e. when keyword and value tag is known.
     * </p>
     *
     * @param keyword
     *            The IPP attribute keyword (name).
     * @param valueTag
     *            The {@link IppValueTag}.
     * @return The attribute object or {@code null} when attribute is not
     *         supported.
     */
    public abstract IppAttr getAttr(String keyword, IppValueTag valueTag);

    /**
     * Creates a default {@link IppAttr}.
     *
     * @param keyword
     *            The attribute name (keyword).
     * @param valueTag
     *            The {@link IppValueTag}.
     * @return The {@link IppAttr}.
     */
    public static IppAttr createAttr(final String keyword,
            final IppValueTag valueTag) {

        AbstractIppAttrSyntax syntax;

        switch (valueTag) {
        case BEGCOLLECTION:
            syntax = null; // no syntax needed
            break;
        case BOOLEAN:
            syntax = IppBoolean.instance();
            break;
        case CHARSET:
            syntax = IppCharset.instance();
            break;
        case DATETIME:
            syntax = IppDateTime.instance();
            break;
        case ENDCOLLECTION:
            syntax = null; // no syntax needed
            break;
        case ENUM:
            syntax = IppEnum.instance();
            break;
        case INTEGER:
            syntax = IppInteger.instance();
            break;
        case INTRANGE:
            syntax = IppRangeOfInteger.instance();
            break;
        case KEYWORD:
            syntax = IppKeyword.instance();
            break;
        case MEMBERATTRNAME:
            syntax = null; // no syntax needed
            break;
        case MIMETYPE:
            syntax = IppMimeMediaType.instance();
            break;
        case NAMEWLANG:
            syntax = IppName.instance();
            break;
        case NAMEWOLANG:
            syntax = IppName.instance();
            break;
        case NATULANG:
            syntax = IppNaturalLanguage.instance();
            break;
        case NONE:
            syntax = new IppOutOfBand(valueTag);
            break;
        case OCTETSTRING:
            syntax = IppOctetString.instance();
            break;
        case RESOLUTION:
            syntax = IppResolution.instance();
            break;
        case TEXTWLANG:
            syntax = IppText.instance();
            break;
        case TEXTWOLANG:
            syntax = IppText.instance();
            break;
        case UNKNOWN:
            syntax = new IppOutOfBand(valueTag);
            break;
        case UNSUPPORTED:
            syntax = new IppOutOfBand(valueTag);
            break;
        case URI:
            syntax = IppUri.instance();
            break;
        case URISCHEME:
            syntax = IppUriScheme.instance();
            break;
        default:
            syntax = null;
            break;
        }

        final IppAttr attr;

        if (syntax == null) {
            attr = null;
        } else {
            attr = new IppAttr(keyword, syntax);
        }

        return attr;
    }

}
