/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2020 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: 2011-2020 Datraverse B.V. <info@datraverse.com>
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
package org.savapage.core.i18n;

import java.util.Locale;

import org.savapage.core.util.LocaleHelper;

/**
 * Common UI Nouns.
 *
 * @author Rijk Ravestein
 *
 */
public enum NounEnum {

    /** */
    ADDRESS(true),
    /** */
    ARCHIVE,
    /** Financial position. */
    BALANCE,
    /** NFC card number. */
    CARD_NUMBER,
    /** */
    CLIENT(true),
    /** */
    COMMENT,
    /** */
    COST,
    /** */
    CREDIT_LIMIT,
    /** */
    DATABASE,
    /** */
    DATE(true),
    /** */
    DAY(true), WEEK(true), MONTH(true), YEAR(true),
    /** */
    DELEGATE(true),
    /** */
    DELEGATOR(true),
    /** */
    DESTINATION,
    /** */
    DISK_SPACE,
    /** */
    DOCUMENT(true),
    /** */
    DOWNLOAD(true),
    /** */
    EMAIL,
    /** */
    FILE(true),
    /** */
    FONT(true),
    /** */
    GROUP(true),
    /** */
    ID_NUMBER,
    /** */
    INPUT,
    /** */
    INVOICING,
    /** */
    JOURNAL,
    /** */
    LOCATION,
    /** */
    MANUFACTURER(true),
    /** */
    MODE(true),
    /** As in Type/Model. */
    MODEL(true),
    /** */
    NAME(true),
    /** */
    ORIGIN,
    /** */
    OUTPUT,
    /** */
    PASSWORD(true),
    /** */
    PERIOD(true),
    /** */
    PREFERRED_LIST,
    /** */
    PRINTER(true),
    /** */
    PROTOCOL(true),
    /** */
    QUEUE(true),
    /** */
    REFUND(true),
    /** */
    REMARK(true),
    /** */
    RENDERING,
    /** */
    REPORT,
    /** */
    SERVER(true),
    /** */
    SIGNATURE,
    /** */
    SORTING,
    /** */
    STATISTICS,
    /** */
    STATUS,
    /** */
    TERMINAL(true),
    /** */
    TIME,
    /** */
    TITLE,
    /** */
    TOTAL(true),
    /** */
    TRANSACTION(true),
    /** */
    TYPE(true),
    /** */
    USER(true);

    /**
     *
     */
    private static final String PLURAL_SUFFIX = "_P";

    /**
     *
     */
    private static final String SINGULAR_SUFFIX = "_S";

    /**
     * {@code true} when noun has a plural form.
     */
    private final boolean hasPlural;

    /**
     *
     */
    NounEnum() {
        this.hasPlural = false;
    }

    /**
     *
     * @param plural
     *            {@code true} when noun has a plural form.
     */
    NounEnum(final boolean plural) {
        this.hasPlural = plural;
    }

    /**
     * @param locale
     *            The {@link Locale}.
     * @return The localized text.
     */
    public String uiText(final Locale locale) {

        if (this.hasPlural) {
            return LocaleHelper.uiText(this, locale, SINGULAR_SUFFIX);
        }
        return LocaleHelper.uiText(this, locale);
    }

    /**
     * @param locale
     *            The {@link Locale}.
     * @param plural
     *            {@code true} if plural form.
     * @return The localized text.
     */
    public String uiText(final Locale locale, final boolean plural) {

        if (!this.hasPlural) {
            return LocaleHelper.uiText(this, locale);
        }

        final String sfx;

        if (plural) {
            sfx = PLURAL_SUFFIX;
        } else {
            sfx = SINGULAR_SUFFIX;
        }
        return LocaleHelper.uiText(this, locale, sfx);
    }

}
