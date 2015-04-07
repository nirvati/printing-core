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
package org.savapage.core.community;

/**
 * A dictionary of words that are to be used in all internalizations.
 *
 * @author Datraverse B.V.
 *
 */
public enum CommunityDictEnum {

    /**
     * The single word (tm) community name: DO NOT CHANGE THIS NAME.
     */
    SAVAPAGE("SavaPage"),

    /**
     * .
     */
    COMMUNITY_SOURCE_CODE_URL("https://gitlab.com/savapage"),

    /**
     * .
     */
    COMMUNITY("Community"),

    /**
     * .
     */
    DATRAVERSE_BV("Datraverse B.V."),

    /**
     * .
     */
    DATRAVERSE_BV_URL("http://www.datraverse.com/en"),

    /**
     * .
     */
    SAVAPAGE_SUPPORT("SavaPage Support"),

    /**
     * .
     */
    FELLOW("Fellow"),

    /**
     * .
     */
    FELLOWSHIP("Fellowship"),

    /**
     * .
     */
    MEMBER("Member"),

    /**
     * .
     */
    MEMBER_CARD("Member Card"),

    /**
     * .
     */
    MEMBERSHIP("Membership"),

    /**
     * .
     */
    PARTICIPANTS("Participants", true),

    /**
     * .
     */
    USERS("Users", true),

    /**
     * .
     */
    VISITOR("Visitor"),

    /**
     * .
     */
    VISITING_GUEST("Visiting Guest");

    /**
     * .
     */
    private final String word;

    /**
     *
     */
    private final boolean translatable;

    /**
     * Constructor.
     *
     * @param word
     *            The unique non-translatable word for the dictionary entry.
     */
    private CommunityDictEnum(final String word) {
        this.word = word;
        this.translatable = false;
    }

    /**
     * Constructor.
     *
     * @param word
     *            The unique word for the dictionary entry.
     * @param translatable
     *            {@code true} when this term must be translated for
     *            internalization.
     */
    private CommunityDictEnum(final String word, final boolean translatable) {
        this.word = word;
        this.translatable = translatable;
    }

    public String getWord() {
        return word;
    }

}
