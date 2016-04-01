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
package org.savapage.core.util;

/**
 *
 * @author Datraverse B.V.
 */
public final class XmlParseHelper {

    /**
     * A regular expression for invalid XML 1.0 characters.
     * <p>
     * See this <a href="http://stackoverflow.
     * com/questions/4237625/removing-invalid-
     * xml-characters-from-a-string-in-java">Stackoverflow</a> question.
     * </p>
     * <p>
     * Valid characters are:
     * </p>
     * <p>
     * {@code #x9 | #xA | #xD | [#x20-#xD7FF] | [#xE000-#xFFFD] | [#x10000-#x10FFFF]}
     * </p>
     */
    private static final String XML_1_0_PATTERN_INVALID_CHARS = "[^"
            + "\u0009\r\n" + "\u0020-\uD7FF" + "\uE000-\uFFFD"
            + "\ud800\udc00-\udbff\udfff" + "]";

    /**
     * A regular expression for invalid XML 1.1 characters.
     * <p>
     * See this <a href="http://stackoverflow.
     * com/questions/4237625/removing-invalid-
     * xml-characters-from-a-string-in-java">Stackoverflow</a> question.
     * </p>
     * <p>
     * Valid characters are:
     * </p>
     * <p>
     * {@code [#x1-#xD7FF] | [#xE000-#xFFFD] | [#x10000-#x10FFFF]}
     * </p>
     */
    @SuppressWarnings("unused")
    private static final String XML_1_1_PATTERN_INVALID_CHARS = "[^"
            + "\u0001-\uD7FF" + "\uE000-\uFFFD" + "\ud800\udc00-\udbff\udfff"
            + "]+";

    /**
     *
     */
    private XmlParseHelper() {
    }

    /**
     * Removes illegal XML 1.0 characters from a String.
     *
     * @param input
     *            The input String.
     * @return The resulting string.
     */
    public static String removeIllegalChars(final String input) {
        return input.replaceAll(XML_1_0_PATTERN_INVALID_CHARS, "");
    }

}
