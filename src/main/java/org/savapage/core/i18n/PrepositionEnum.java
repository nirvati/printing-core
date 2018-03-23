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
package org.savapage.core.i18n;

import java.util.Locale;

import org.savapage.core.util.LocaleHelper;

/**
 * UI Prepositions in Noun context.
 *
 * @author Rijk Ravestein
 *
 */
public enum PrepositionEnum {

    /**
     * As in "Color changed from red to green."
     * <p>
     * Opposite of {@link #TO_STATE}.
     * </p>
     */
    FROM_STATE,
    /**
     * Opposite of {@link #FROM_STATE}.
     */
    TO_STATE,

    /**
     * As in "He was busy from June to July."
     * <p>
     * Opposite of {@link #TO_TIME}.
     * </p>
     */
    FROM_TIME,
    /**
     * Opposite of {@link #FROM_TIME}.
     */
    TO_TIME,

    /**
     * As in "He traveled from Amsterdam to Rome."
     * <p>
     * Opposite of {@link #TO_LOCATION}.
     * </p>
     */
    FROM_LOCATION,
    /**
     * Opposite of {@link #FROM_LOCATION}.
     */
    TO_LOCATION;

    /**
     * @param locale
     *            The {@link Locale}.
     * @return The localized text.
     */
    public String uiText(final Locale locale) {
        return LocaleHelper.uiText(this, locale);
    }

}
