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
import java.util.ResourceBundle;

/**
 *
 * @author Datraverse B.V.
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
     *
     * @param klasse
     * @param xmlResource
     * @param locale
     * @return
     */
    protected static ResourceBundle loadResource(
            Class<? extends Object> klasse, final String xmlResource,
            final Locale locale, final XMLResourceBundleControl control) {

        final String bundleName =
                klasse.getPackage().getName() + "." + xmlResource;

        Locale localeWrk = locale;

        if (locale == null) {
            localeWrk = Locale.getDefault();
        }

        /*
         * Make sure the base locale language falls back to 'message.xml'
         */
        if (BASE_MESSAGE_LANGUAGE.equalsIgnoreCase(localeWrk.getLanguage())) {
            localeWrk = LOCALE_NO_LANGUAGE;
        }

        ResourceBundle bundle =
                ResourceBundle.getBundle(bundleName, localeWrk,
                        klasse.getClassLoader(), control);

        if (!localeWrk.getLanguage().equals(bundle.getLocale().getLanguage())) {

            localeWrk = LOCALE_NO_LANGUAGE;

            bundle =
                    ResourceBundle.getBundle(bundleName, localeWrk,
                            klasse.getClassLoader(), control);
        }

        return bundle;
    }

}
