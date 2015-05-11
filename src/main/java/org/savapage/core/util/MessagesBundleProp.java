/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2015 Datraverse B.V.
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

import java.util.Locale;
import java.util.PropertyResourceBundle;

/**
 * Helper class for loading a {@link PropertyResourceBundle}.
 *
 * @author Datraverse B.V.
 *
 */
public final class MessagesBundleProp extends MessagesBundleMixin {

    /**
     * @param packagz
     *            The {@link Package} as container of the resource bundle file.
     * @param resourceName
     *            The name of the resource bundle file.
     *
     * @return The name of the resource bundle.
     */
    protected static String getResourceBundleBaseName(final Package packagz,
            final String resourceName) {
        final StringBuilder name = new StringBuilder(128);
        name.append(packagz.getName()).append('.').append(resourceName);
        return name.toString();
    }

    /**
     * Gets a best match {@link PropertyResourceBundle}.
     *
     * @param packagz
     *            The {@link Package} as container of the resource bundle file.
     * @param resourceName
     *            The name of the resource bundle file.
     * @param locale
     *            The {@link Locale} to match.
     * @return The best match {@link PropertyResourceBundle}.
     */
    public static PropertyResourceBundle getResourceBundle(
            final Package packagz, final String resourceName,
            final Locale locale) {

        final String resourceBundleBaseName =
                getResourceBundleBaseName(packagz, resourceName);

        Locale localeUsed = locale;

        /*
         * Make sure the base locale language falls back to the bundle basename.
         */
        if (BASE_MESSAGE_LANGUAGE.equalsIgnoreCase(localeUsed.getLanguage())) {
            localeUsed = LOCALE_NO_LANGUAGE;
        }

        // First match.
        PropertyResourceBundle resourceBundle =
                (PropertyResourceBundle) PropertyResourceBundle.getBundle(
                        resourceBundleBaseName, localeUsed);

        /*
         * If the language of the ResourceBundle found is different from the
         * language of the requested locale, we switch to the default resource
         * bundle, i.e. the one without a locale.
         */
        if (!localeUsed.getLanguage().equals(
                resourceBundle.getLocale().getLanguage())) {

            localeUsed = LOCALE_NO_LANGUAGE;

            // Second trial.
            resourceBundle =
                    (PropertyResourceBundle) PropertyResourceBundle.getBundle(
                            resourceBundleBaseName, localeUsed);
        }

        return resourceBundle;
    }
}
