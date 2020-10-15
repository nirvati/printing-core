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
package org.savapage.core.util;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Common methods for handling {@link ResourceBundle}.
 *
 * @author Rijk Ravestein
 *
 */
public abstract class MessagesBundleMixin {

    /**
     * The base language available for all message files.
     */
    protected static final String BASE_MESSAGE_LANGUAGE = "en";

    /**
     *
     */
    protected static final Locale LOCALE_NO_LANGUAGE = new Locale("");

    /**
     * Determines the {@link Locale} to be used for the {@link ResourceBundle}.
     *
     * @param candidate
     *            The {@link Locale} candidate. If {@code null} the
     *            {@link Locale#getDefault()} is used.
     * @return The {@link Locale} to be used for the {@link ResourceBundle}.
     */
    protected static Locale determineLocale(final Locale candidate) {

        Locale localeWrk = candidate;

        if (candidate == null) {
            localeWrk = Locale.getDefault();
        }

        /*
         * Make sure the base locale language falls back to 'message.xml'
         */
        if (BASE_MESSAGE_LANGUAGE.equalsIgnoreCase(localeWrk.getLanguage())) {
            localeWrk = LOCALE_NO_LANGUAGE;
        }

        return localeWrk;
    }

    /**
     * @param packagz
     *            The {@link Package} as container of the resource bundle file.
     * @param resourceName
     *            The name of the resource bundle without the locale suffix and
     *            file extension.
     * @return The name of the resource bundle.
     */
    protected static String getResourceBundleBaseName(final Package packagz,
            final String resourceName) {

        final StringBuilder name = new StringBuilder(128);
        name.append(packagz.getName()).append('.').append(resourceName);
        return name.toString();
    }

    /**
     * Checks if the language of the {@link Locale} candidate matches the
     * language of the {@link ResourceBundle} candidate.
     *
     * @param localeCandidate
     *            The {@link Locale} candidate.
     * @param bundleCandidate
     *            The {@link ResourceBundle} candidate.
     * @return The alternative {@link Locale} to be used, or {@code null} when
     *         the candidates match.
     */
    protected static Locale checkAlternative(final Locale localeCandidate,
            final ResourceBundle bundleCandidate) {

        if (localeCandidate.getLanguage()
                .equals(bundleCandidate.getLocale().getLanguage())) {
            return null;
        }
        /*
         * The language of the ResourceBundle candiadate is different from the
         * language of the candidate locale, we switch to the default resource
         * bundle, i.e. the one without a locale.
         */
        return LOCALE_NO_LANGUAGE;
    }

}
