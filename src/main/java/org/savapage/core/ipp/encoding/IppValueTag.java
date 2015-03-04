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
package org.savapage.core.ipp.encoding;

import org.savapage.core.SpException;

/**
 * Enumeration of IPP Value Tags, see <a
 * href="http://tools.ietf.org/html/rfc2910#section-3.5.2">RFC2910</a>.
 *
 * @author Datraverse B.V.
 *
 */
public enum IppValueTag {

    /**
     * "out-of-band" value: unsupported value.
     */
    UNSUPPORTED(0x10),

    /**
     * Reserved for 'default' for definition in a future IETF standards track
     * document
     */
    RESERVED_X11(0x11),

    /** "out-of-band" value: unknown value. */
    UNKNOWN(0x12),

    /** "out-of-band" value: no value. */
    NONE(0x13),

    /**
     * Reserved for "out-of-band" values in future IETF standards track
     * documents.
     */
    RESERVED_X14(0x14), RESERVED_X15(0x15), RESERVED_X16(0x16), RESERVED_X17(
            0x17), RESERVED_X18(0x18), RESERVED_X19(0x19), RESERVED_X1A(0x1A), RESERVED_X1B(
            0x1B), RESERVED_X1C(0x1C), RESERVED_X1D(0x1D), RESERVED_X1E(0x1E), RESERVED_X1F(
            0x1F),
    /**
     * Reserved for definition in a future IETF standards track document (0x20
     * is reserved for "generic integer" if it should ever be needed).
     */
    RESERVED_X20(0x20),

    /** integer. */
    INTEGER(0x21),
    /** boolean. */
    BOOLEAN(0x22),
    /** enumeration. */
    ENUM(0x23),

    /**
     * Reserved for integer types for definition in future IETF standards track
     * documents.
     */
    RESERVED_X24(0x24), RESERVED_X25(0x25), RESERVED_X26(0x26), RESERVED_X27(
            0x27), RESERVED_X28(0x28), RESERVED_X29(0x29), RESERVED_X2A(0x2A), RESERVED_X2B(
            0x2B), RESERVED_X2C(0x2C), RESERVED_X2D(0x2D), RESERVED_X2E(0x2E), RESERVED_X2F(
            0x2F),

    /** octetString. */
    OCTETSTRING(0x30),

    /** dateTime. */
    DATETIME(0x31),

    /** resolution. */
    RESOLUTION(0x32),

    /** rangeOfInteger. */
    INTRANGE(0x33),

    /**
     * rfc3382: begCollection: Begin the collection attribute value.
     */
    BEGCOLLECTION(0x34),

    /** textWithLanguage. */
    TEXTWLANG(0x35),

    /** nameWithLanguage. */
    NAMEWLANG(0x36),

    /**
     * rfc3382: endCollection. End the collection attribute value. Also see:
     * {@link #MEMBERATTRNAME}, {@link #BEGCOLLECTION}.
     */
    ENDCOLLECTION(0x37),

    /**
     * Reserved for octetString type definitions in future IETF standards track
     * documents
     */
    RESERVED_X38(0x38), RESERVED_X39(0x39), RESERVED_X3A(0x3A), RESERVED_X3B(
            0x3B), RESERVED_X3C(0x3C), RESERVED_X3D(0x3D), RESERVED_X3E(0x3E), RESERVED_X3F(
            0x3F),

    /**
     * Reserved for definition in a future IETF standards track document (0x40
     * is reserved for "generic character-string" if it should ever be needed).
     */
    RESERVED_X40(0x40),

    /** textWithoutLanguage. */
    TEXTWOLANG(0x41),

    /** nameWithoutLanguage. */
    NAMEWOLANG(0x42),

    /** keyword. */
    KEYWORD(0x44),

    /** URI. */
    URI(0x45),
    /** uriScheme. */
    URISCHEME(0x46),

    /** charset. */
    CHARSET(0x47),

    /** naturalLanguage. */
    NATULANG(0x48),

    /** mimeMediaType. */
    MIMETYPE(0x49),

    /**
     * rfc3382: memberAttrName. The value is the name of the collection member
     * attribute. Also see: {@link #BEGCOLLECTION}, {@link #ENDCOLLECTION}.
     */
    MEMBERATTRNAME(0x4A);

    /*
     * 0x4A-0x5F : reserved for character string type definitions in future IETF
     * standards track documents.
     */

    /*
     * The values 0x60-0xFF are reserved for future type definitions in IETF
     * standards track documents.
     */

    /**
     *
     */
    private int bitPattern = 0;

    /**
     * Creates an enum value from an integer.
     *
     * @param value
     *            The integer.
     */
    IppValueTag(final int value) {
        this.bitPattern = value;
    }

    /**
     * Gets the integer representing this enum value.
     *
     * @return The integer.
     */
    public int asInt() {
        return this.bitPattern;
    }

    /**
     *
     * @param value
     * @return
     */
    public static IppValueTag asEnum(final int value) {
        if (value == IppValueTag.UNKNOWN.asInt()) {
            return UNKNOWN;
        } else if (value == IppValueTag.NONE.asInt()) {
            return NONE;
        } else if (value == IppValueTag.INTEGER.asInt()) {
            return INTEGER;
        } else if (value == IppValueTag.BOOLEAN.asInt()) {
            return BOOLEAN;
        } else if (value == IppValueTag.ENUM.asInt()) {
            return ENUM;
        } else if (value == IppValueTag.OCTETSTRING.asInt()) {
            return OCTETSTRING;
        } else if (value == IppValueTag.DATETIME.asInt()) {
            return DATETIME;
        } else if (value == IppValueTag.RESOLUTION.asInt()) {
            return RESOLUTION;
        } else if (value == IppValueTag.INTRANGE.asInt()) {
            return INTRANGE;
        } else if (value == IppValueTag.TEXTWLANG.asInt()) {
            return TEXTWLANG;
        } else if (value == IppValueTag.NAMEWLANG.asInt()) {
            return NAMEWLANG;
        } else if (value == IppValueTag.TEXTWOLANG.asInt()) {
            return TEXTWOLANG;
        } else if (value == IppValueTag.NAMEWOLANG.asInt()) {
            return NAMEWOLANG;
        } else if (value == IppValueTag.KEYWORD.asInt()) {
            return KEYWORD;
        } else if (value == IppValueTag.URI.asInt()) {
            return URI;
        } else if (value == IppValueTag.URISCHEME.asInt()) {
            return URISCHEME;
        } else if (value == IppValueTag.CHARSET.asInt()) {
            return CHARSET;
        } else if (value == IppValueTag.NATULANG.asInt()) {
            return NATULANG;
        } else if (value == IppValueTag.MIMETYPE.asInt()) {
            return MIMETYPE;
        } else if (value == IppValueTag.UNSUPPORTED.asInt()) {
            return UNSUPPORTED;
        } else if (value == IppValueTag.BEGCOLLECTION.asInt()) {
            return BEGCOLLECTION;
        } else if (value == IppValueTag.MEMBERATTRNAME.asInt()) {
            return MEMBERATTRNAME;
        } else if (value == IppValueTag.ENDCOLLECTION.asInt()) {
            return ENDCOLLECTION;
        }

        throw new SpException("value [" + value
                + "] can not be converted to enum");
    }

}
