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
package org.savapage.core.i18n;

import java.util.Locale;

import org.savapage.core.util.LocaleHelper;

/**
 * Common phrases.
 *
 * @author Rijk Ravestein
 *
 */
public enum PhraseEnum {

    /** */
    ACTION_CANNOT_BE_UNDONE,

    /** */
    ACTIVATE_CARD_READER,

    /** */
    CERT_EXPIRED_ON,
    /** */
    CERT_EXPIRES_ON,
    /** */
    CERT_VALID_UNTIL,

    /** */
    MAIL_SENT,

    /** */
    NOT_FOUND,

    /** */
    PDF_INVALID,
    /** */
    PDF_ENCRYPTED_UNSUPPORTED,

    /** */
    PDF_FONTS_NONE,
    /** */
    PDF_FONTS_ALL_EMBEDDED,
    /** */
    PDF_FONTS_ALL_NON_EMBEDDED,
    /** */
    PDF_FONTS_ALL_STANDARD,
    /** */
    PDF_FONTS_SOME_NON_EMBEDDED,
    /** */
    PDF_FONTS_STANDARD_OR_EMBEDDED,

    /** */
    PDF_PASSWORD_UNSUPPORTED,
    /** */
    PDF_PRINTING_NOT_ALLOWED,
    /** */
    PDF_REPAIR_FAILED,
    /** */
    PDF_XFA_UNSUPPORTED,

    /** Question. */
    Q_DELETE_DOCUMENT,

    /** Question. */
    Q_REPLACE_SECRET_CODE,

    /** */
    REALTIME_ACTIVITY,
    /** */
    SELECT_AND_SORT,
    /** */
    SWIPE_CARD,
    /** */
    SYS_MAINTENANCE,
    /** */
    SYS_TEMP_UNAVAILABLE,
    /** */
    USER_DELETE_WARNING,
    /** */
    WINDOWS_DRIVER_MSG;

    /**
     * @param locale
     *            The {@link Locale}.
     * @return The localized text.
     */
    public String uiText(final Locale locale) {
        return LocaleHelper.uiText(this, locale);
    }

    /**
     * Get the localized UI text with argument. To be used for:
     * {@link #CERT_EXPIRED_ON}, {@link #CERT_EXPIRES_ON},
     * {@link #CERT_VALID_UNTIL}.
     *
     * @param locale
     *            The {@link Locale}.
     * @param args
     *            The arguments.
     * @return The localized text.
     */
    public String uiText(final Locale locale, final String... args) {
        switch (this) {
        case CERT_EXPIRED_ON:
        case CERT_EXPIRES_ON:
        case CERT_VALID_UNTIL:
            return LocaleHelper.uiTextArgs(this, locale, args);
        default:
            throw new IllegalArgumentException(String
                    .format("%s does not support arguments.", this.toString()));
        }

    }

}
