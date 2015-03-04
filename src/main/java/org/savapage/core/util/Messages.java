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

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generic message loader and parser. This class looks for
 * {@code message_<locale>.xml} files in the same directory as the reference
 * class, which is passed as parameter to all public methods.
 * <p>
 * NOTE: When a {@code message_<locale>.properties} files is already loaded in
 * the cache, the content of this file is used instead of the XML variant. See
 * {@link ResourceBundle#getBundle(String, Locale, ClassLoader, java.util.ResourceBundle.Control)}
 * .
 * </p>
 *
 * @author Datraverse B.V.
 *
 */
public final class Messages {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(Messages.class);

    /**
     *
     */
    private static final String DEFAULT_XML_RESOURCE = "messages";

    /**
     *
     */
    private static final String DEFAULT_LOCALE_LANGUAGE = "en";

    /**
     *
     */
    private static final Locale LOCALE_NO_LANGUAGE = new Locale("");

    private Messages() {

    }

    /**
     *
     * @param klasse
     * @param xmlResource
     * @param locale
     * @return
     */
    public static ResourceBundle loadResource(Class<? extends Object> klasse,
            final String xmlResource, Locale locale) {

        final String bundleName =
                klasse.getPackage().getName() + "." + xmlResource;

        Locale localeWrk = locale;

        if (locale == null) {
            localeWrk = Locale.getDefault();
        }

        /*
         * Make sure the default locale language falls back to 'message.xml'
         */
        if (DEFAULT_LOCALE_LANGUAGE.equalsIgnoreCase(localeWrk.getLanguage())) {
            localeWrk = LOCALE_NO_LANGUAGE;
        }

        return ResourceBundle.getBundle(bundleName, localeWrk,
                klasse.getClassLoader(), new XMLResourceBundleControl());
    }

    /**
     *
     * @param klasse
     * @param locale
     * @param key
     * @return
     */
    private static String loadMessagePattern(Class<? extends Object> klasse,
            Locale locale, String key) {
        return loadResource(klasse, DEFAULT_XML_RESOURCE, locale)
                .getString(key);
    }

    public static String getMessage(Class<? extends Object> klasse, String key,
            String[] args) {
        return getMessage(klasse, null, key, args);
    }

    /**
     *
     * @param klasse
     * @param locale
     * @param key
     * @param args
     * @return
     */
    public static String getMessage(Class<? extends Object> klasse,
            final Locale locale, final String key, final String... args) {

        final String pattern = loadMessagePattern(klasse, locale, key);

        String msg;

        if ((args == null) || args.length == 0) {

            msg = pattern;

        } else {
            try {
                /*
                 * Add an extra apostrophe ' to the MessageFormat pattern String
                 * to ensure the ' character is displayed.
                 */
                msg =
                        MessageFormat.format(pattern.replace("\'", "\'\'"),
                                (Object[]) args);

            } catch (IllegalArgumentException e) {

                LOGGER.error("Error parsing message pattern [" + pattern
                        + "] Locale [" + locale + "] : " + e.getMessage());
                /*
                 * use a substitute message
                 */
                msg = "[" + key + "]";
            }
        }
        return msg;
    }

    /**
     *
     * @param klasse
     * @param key
     * @return
     */
    public static boolean
            containsKey(Class<? extends Object> klasse, String key) {
        return loadResource(klasse, DEFAULT_XML_RESOURCE, null)
                .containsKey(key);
    }

    /**
     *
     * @param klasse
     * @param key
     * @return
     */
    public static boolean containsKey(Class<? extends Object> klasse,
            String key, Locale locale) {
        return loadResource(klasse, DEFAULT_XML_RESOURCE, locale).containsKey(
                key);
    }

}
