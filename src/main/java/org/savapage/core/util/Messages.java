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

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generic XML message loader and parser. This class looks for an
 * {@code message_<locale>.xml} file in the same directory as the requester
 * class, which is passed as parameter to all public methods.
 *
 * @author Datraverse B.V.
 *
 */
public final class Messages extends MessagesBundleMixin {

    /**
     * .
     */
    private static final Logger LOGGER = LoggerFactory
            .getLogger(Messages.class);

    /**
     *
     */
    private static final String DEFAULT_XML_RESOURCE = "messages";

    /**
     *
     */
    private Messages() {
    }

    /**
     * Loads a {@link ResourceBundle}.
     *
     * @param reqClass
     *            The requester {@link Class}.
     * @param resourceName
     *            The name of the resource bundle without the locale suffix and
     *            file extension.
     * @param locale
     *            The {@link Locale}.
     * @return The {@link ResourceBundle}.
     */
    public static ResourceBundle loadXmlResource(
            final Class<? extends Object> reqClass, final String resourceName,
            final Locale locale) {
        return getResourceBundle(reqClass, resourceName, locale,
                new XMLResourceBundleControl());
    }

    /**
     * Loads a {@link ResourceBundle}.
     * <p>
     * NOTE: When a {@code message_<locale>.properties} files is already loaded
     * in the cache, the content of this file is used instead of the XML
     * variant. See
     * {@link ResourceBundle#getBundle(String, Locale, ClassLoader, java.util.ResourceBundle.Control)}
     * .
     * </p>
     *
     * @param reqClass
     *            The requester {@link Class}.
     * @param resourceName
     *            The name of the resource bundle without the locale suffix and
     *            file extension.
     * @param candidate
     *            The {@link Locale} candidate.
     * @param control
     *            The {@link XMLResourceBundleControl}.
     * @return The {@link ResourceBundle}.
     */
    private static ResourceBundle getResourceBundle(
            final Class<? extends Object> reqClass, final String resourceName,
            final Locale candidate, final XMLResourceBundleControl control) {

        final String bundleName =
                getResourceBundleBaseName(reqClass.getPackage(), resourceName);

        Locale locale = determineLocale(candidate);

        ResourceBundle bundle =
                ResourceBundle.getBundle(bundleName, locale,
                        reqClass.getClassLoader(), control);

        locale = checkAlternative(locale, bundle);

        if (locale != null) {
            bundle =
                    ResourceBundle.getBundle(bundleName, locale,
                            reqClass.getClassLoader(), control);
        }

        return bundle;
    }

    /**
     * Gets the localized message from {@code message*.xml} residing in the same
     * package as the requester class.
     *
     * @param reqClass
     *            The requester {@link Class}.
     * @param locale
     *            The {@link Locale}.
     * @param key
     *            The message key.
     * @return The message.
     */
    private static String loadMessagePattern(
            final Class<? extends Object> reqClass, final Locale locale,
            final String key) {
        return loadXmlResource(reqClass, DEFAULT_XML_RESOURCE, locale)
                .getString(key);
    }

    /**
     * Gets the default locale (system) message from {@code message*.xml}
     * residing in the same package as the requester class.
     *
     * @param reqClass
     *            The requester {@link Class}.
     * @param key
     *            The message key.
     * @param args
     *            The message arguments.
     * @return The message.
     */
    public static String getMessage(final Class<? extends Object> reqClass,
            final String key, final String[] args) {
        return getMessage(reqClass, null, key, args);
    }

    /**
     * Gets the default locale (system) message from {@code message*.xml}
     * residing in the same package as the requester class.
     *
     * @param reqClass
     *            The requester {@link Class}.
     * @param key
     *            The message key.
     * @param args
     *            The message arguments.
     * @return The message.
     */
    public static String getSystemMessage(
            final Class<? extends Object> reqClass, final String key,
            final String... args) {
        return getMessage(reqClass, null, key, args);
    }

    /**
     * Gets the locale message for technical logging from {@code message.xml}
     * residing in the same package as the requester class.
     *
     * @param reqClass
     *            The requester {@link Class}.
     * @param key
     *            The message key.
     * @param args
     *            The message arguments.
     * @return The message.
     */
    public static String getLogFileMessage(
            final Class<? extends Object> reqClass, final String key,
            final String... args) {
        return getMessage(reqClass, LOCALE_NO_LANGUAGE, key, args);
    }

    /**
     * Gets the localized message from {@code message*.xml} residing in the same
     * package as the requester class.
     *
     * @param reqClass
     *            The requester {@link Class}.
     * @param locale
     *            The {@link Locale}.
     * @param key
     *            The message key.
     * @param args
     *            The message arguments.
     * @return The message.
     */
    public static String getMessage(final Class<? extends Object> reqClass,
            final Locale locale, final String key, final String... args) {

        final String pattern = loadMessagePattern(reqClass, locale, key);

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
     * Checks if if default (system) message key exists.
     *
     * @param reqClass
     *            The requester {@link Class}.
     * @param key
     *            The message key.
     * @return {@code true} if default (system) message key exists.
     */
    public static boolean containsKey(final Class<? extends Object> reqClass,
            final String key) {
        return loadXmlResource(reqClass, DEFAULT_XML_RESOURCE, null)
                .containsKey(key);
    }

    /**
     * Checks if locale message key exists.
     *
     * @param reqClass
     *            The requester {@link Class}.
     * @param key
     *            The message key.
     * @param locale
     *            The {@link Locale}.
     * @return {@code true} if locale message key exists.
     */
    public static boolean containsKey(final Class<? extends Object> reqClass,
            final String key, final Locale locale) {
        return loadXmlResource(reqClass, DEFAULT_XML_RESOURCE, locale)
                .containsKey(key);
    }

}
