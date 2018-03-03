/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2018 Datraverse B.V.
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.core.community;

import java.util.Locale;

import org.savapage.core.util.LocaleHelper;

/**
 * A dictionary of common words.
 *
 * @author Rijk Ravestein
 *
 */
public enum CommunityDictEnum {

    /**
     * The single word (tm) community name: DO NOT CHANGE THIS NAME.
     */
    SAVAPAGE("SavaPage"),

    /**
     * A short slogan.
     */
    SAVAPAGE_SLOGAN("Open Print Portal"),

    /** */
    SAVAPAGE_DOT_ORG("savapage.org"),

    /** */
    SAVAPAGE_WWW_DOT_ORG("www.savapage.org"),

    /** */
    SAVAPAGE_WWW_DOT_ORG_URL("https://www.savapage.org"),

    /** */
    COMMUNITY_SOURCE_CODE_URL("https://gitlab.com/savapage"),

    /** */
    COMMUNITY("Community"),

    /** */
    DATRAVERSE_BV("Datraverse B.V."),

    /** */
    DATRAVERSE_BV_URL("https://www.datraverse.com"),

    /**
     * URL European Union.
     */
    EU_URL("https://europa.eu/"),

    /**
     * URL of GDPR. Note: SSL Server Certificate issued to Organization (O):
     * European Commission.
     */
    EU_GDPR_URL("https://ec.europa.eu/info/law/law-topic/data-protection_en"),

    /** */
    EU_FULL_TXT("European Union"),

    /** */
    EU_GDPR_FULL_TXT("General Data Protection Regulation"),

    /** */
    SAVAPAGE_SUPPORT("SavaPage Support"),

    /** */
    SAVAPAGE_SUPPORT_URL("https://support.savapage.org"),

    /** */
    MEMBER("Member"),

    /** */
    MEMBER_CARD("Member Card"),

    /** */
    MEMBERSHIP("Membership"),

    /** */
    VISITOR("Visitor"),

    /** */
    VISITING_GUEST("Visiting Guest"),

    /**  */
    CARD_HOLDER,

    /**  */
    WEB_PRINT,

    /**  */
    INTERNET_PRINT,

    /**  */
    ECO_PRINT,

    /**  */
    PROXY_PRINT,

    /**  */
    MAIL_PRINT,

    /**  */
    PARTICIPANTS,

    /**  */
    USERS;

    /**
     * .
     */
    private final String word;

    /**
     * {@code true} when this term must be translated for internalization.
     */
    private final boolean translatable;

    /**
     * Constructor.
     *
     * @param word
     *            The unique non-translatable word for the dictionary entry.
     */
    CommunityDictEnum(final String word) {
        this.word = word;
        this.translatable = false;
    }

    /**
     * Constructor.
     *
     * @param translatable
     */
    CommunityDictEnum() {
        this.word = null;
        this.translatable = true;
    }

    /**
     * @param locale
     *            The {@link Locale}.
     * @return The localized text.
     */
    public String getWord(final Locale locale) {
        if (this.translatable) {
            return LocaleHelper.uiText(this, locale);
        }
        return word;
    }

    /**
     * @return The default localized text.
     */
    public String getWord() {
        return this.getWord(Locale.getDefault());
    }

}
