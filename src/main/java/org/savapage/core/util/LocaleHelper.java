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

import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.savapage.core.SpException;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class LocaleHelper {

    private static final String LOCALE_LANG_ENGLISH = "en";
    private static final String LOCALE_LANG_DUTCH = "nl";
    private static final String LOCALE_LANG_FRENCH = "fr";
    private static final String LOCALE_LANG_GERMAN = "de";
    private static final String LOCALE_LANG_POLISH = "pl";
    private static final String LOCALE_LANG_RUSSIAN = "ru";
    private static final String LOCALE_LANG_SPANISH = "es";

    private static final String LOCALE_CTRY_NL = "NL";
    private static final String LOCALE_CTRY_ES = "ES";
    private static final String LOCALE_CTRY_PL = "PL";
    private static final String LOCALE_CTRY_RU = "RU";

    /**
     *
     */
    private final Locale locale;

    /**
     * Construct.
     *
     * @param locale
     *            The locale.
     */
    public LocaleHelper(final Locale locale) {
        this.locale = locale;
    }

    /**
     * Gets as localized (short) date string of a Date.
     *
     * @param date
     *            The date.
     * @return The localized date string.
     */
    public String getShortDate(final Date date) {
        return DateFormat.getDateInstance(DateFormat.SHORT, this.locale)
                .format(date);
    }

    /**
     * Gets as localized (medium) date string of a Date.
     *
     * @param date
     *            The date.
     * @return The localized date string.
     */
    public String getMediumDate(final Date date) {
        return DateFormat.getDateInstance(DateFormat.MEDIUM, this.locale)
                .format(date);
    }

    /**
     * Gets as localized (long) date string of a Date.
     *
     * @param date
     *            The date.
     * @return The localized date string.
     */
    public String getLongDate(final Date date) {
        return DateFormat.getDateInstance(DateFormat.LONG, this.locale)
                .format(date);
    }

    /**
     * Gets as localized string of a Number.
     *
     * @param number
     *            The number.
     * @return The localized string.
     */
    public String getNumber(final long number) {
        return NumberFormat.getInstance(this.locale).format(number);
    }

    /**
     * Gets as localized short date/time string of a Date.
     *
     * @param date
     *            The date.
     * @return The localized short date/time string.
     */
    public String getShortDateTime(final Date date) {
        return DateFormat.getDateTimeInstance(DateFormat.SHORT,
                DateFormat.SHORT, this.locale).format(date);
    }

    /**
     * Gets as localized (long)date/(medium)time string of a Date.
     *
     * @param date
     *            The date.
     * @return The localized date/time string.
     */
    public String getLongMediumDateTime(final Date date) {
        return DateFormat.getDateTimeInstance(DateFormat.LONG,
                DateFormat.MEDIUM, this.locale).format(date);
    }

    /**
     *
     * @param decimal
     * @param fractionDigits
     * @param currencySymbol
     * @return
     * @throws ParseException
     */
    public String getCurrencyDecimal(final BigDecimal decimal,
            int fractionDigits, final String currencySymbol)
            throws ParseException {
        return BigDecimalUtil.localize(decimal, fractionDigits, this.locale,
                currencySymbol, true);
    }

    /**
     *
     * @param decimal
     * @param fractionDigits
     * @return
     * @throws ParseException
     */
    public String getDecimal(final BigDecimal decimal, int fractionDigits)
            throws ParseException {
        return getCurrencyDecimal(decimal, fractionDigits, "");
    }

    /**
     * Formats a BigDecimal as exact integer, or scaled with 2 decimals.
     *
     * @param bd
     *            The {@link BigDecimal}.
     * @return The formatted string.
     */
    public String asExactIntegerOrScaled(final BigDecimal bd) {
        final BigInteger bi = NumberUtil.toBigIntegerExact(bd);
        if (bi == null) {
            try {
                return getDecimal(bd.setScale(2, RoundingMode.HALF_UP), 2);
            } catch (ParseException e) {
                throw new SpException(e.getMessage());
            }
        }
        return bi.toString();
    }

    /**
     * Gets the (customized) localized user interface text of an {@link Enum}
     * value.
     *
     * @param <E>
     *            The Enum class.
     * @param value
     *            The Enum value.
     * @param locale
     *            The {@link Locale}.
     * @return The localized text.
     */
    private static <E extends Enum<E>> String uiTextCustom(final Enum<E> value,
            final Locale locale) {
        return uiTextCustom(value, locale, null);
    }

    /**
     * Gets the (customized) localized user interface text of an {@link Enum}
     * value.
     *
     * @param <E>
     *            The Enum class.
     * @param value
     *            The Enum value.
     * @param locale
     *            The {@link Locale}.
     * @param suffix
     *            The value suffix to be appended to the Enum string value.
     * @return The localized text.
     */
    private static <E extends Enum<E>> String uiTextCustom(final Enum<E> value,
            final Locale locale, final String suffix) {

        final String key;
        if (StringUtils.isBlank(suffix)) {
            key = value.toString();
        } else {
            key = String.format("%s%s", value.toString(), suffix);
        }

        ResourceBundle bundle = getResourceBundleCustom(value, locale);

        if (bundle == null || !bundle.containsKey(key)) {
            bundle = getResourceBundle(value, locale);
        }
        return bundle.getString(key);
    }

    /**
     * Gets the localized user interface text of an {@link Enum} value.
     *
     * @param <E>
     *            The Enum class.
     * @param value
     *            The Enum value.
     * @param locale
     *            The {@link Locale}.
     * @return The localized text.
     */
    public static <E extends Enum<E>> String uiText(final Enum<E> value,
            final Locale locale) {
        if (Messages.isCustomI18nEnabled()) {
            return uiTextCustom(value, locale);
        }
        return getResourceBundle(value, locale).getString(value.toString());
    }

    /**
     * Gets the localized user interface text of an {@link Enum} value with
     * arguments.
     *
     * @param <E>
     *            The Enum class.
     * @param value
     *            The Enum value.
     * @param locale
     *            The {@link Locale}.
     * @param args
     *            The arguments.
     * @return The localized text.
     */
    public static <E extends Enum<E>> String uiTextArgs(final Enum<E> value,
            final Locale locale, final String... args) {
        if (Messages.isCustomI18nEnabled()) {
            return Messages.formatMessage(uiTextCustom(value, locale), args);
        }
        return Messages.formatMessage(uiText(value, locale), args);
    }

    /**
     * Gets the localized user interface text of an {@link Enum} value.
     *
     * @param <E>
     *            The Enum class.
     * @param value
     *            The Enum value.
     * @param locale
     *            The {@link Locale}.
     * @param suffix
     *            The value suffix to be appended to the Enum string value.
     * @return The localized text.
     */
    public static <E extends Enum<E>> String uiText(final Enum<E> value,
            final Locale locale, final String suffix) {
        if (Messages.isCustomI18nEnabled()) {
            return uiTextCustom(value, locale, suffix);
        }
        return getResourceBundle(value, locale)
                .getString(String.format("%s%s", value.toString(), suffix));
    }

    /**
     * Gets the resource bundle of an enum.
     *
     * @param <E>
     *            The Enum class.
     * @param value
     *            The Enum value.
     * @param locale
     *            The {@link Locale}.
     * @return The {@link ResourceBundle}.
     */
    private static <E extends Enum<E>> ResourceBundle
            getResourceBundle(final Enum<E> value, final Locale locale) {
        return Messages.loadXmlResource(value.getClass(),
                value.getClass().getSimpleName(), locale);
    }

    /**
     * Gets the XML resource bundle of an Exception class.
     *
     * @param <E>
     *            The Exception class.
     * @param clazz
     *            The class.
     * @param locale
     *            The {@link Locale}.
     * @return The {@link ResourceBundle}.
     */
    private static <E extends Exception> ResourceBundle
            getResourceBundle(final Class<E> clazz, final Locale locale) {
        return Messages.loadXmlResource(clazz, clazz.getSimpleName(), locale);
    }

    /**
     * @param <E>
     *            The Enum class.
     * @param value
     *            Enum value.
     * @param locale
     *            The locale.
     * @return {@code null} when resource is not found.
     */
    private static <E extends Enum<E>> ResourceBundle
            getResourceBundleCustom(final Enum<E> value, final Locale locale) {
        try {
            return Messages.loadXmlResourceCustom(value.getClass(), locale);
        } catch (MissingResourceException e) {
            // no code intended;
        }
        return null;
    }

    /**
     * @param <E>
     *            The Object class.
     * @param clazz
     *            Class value.
     * @param locale
     *            The locale.
     * @return {@code null} when resource is not found.
     */
    private static <E extends Object> ResourceBundle
            getResourceBundleCustom(final Class<E> clazz, final Locale locale) {
        try {
            return Messages.loadXmlResourceCustom(clazz.getClass(), locale);
        } catch (MissingResourceException e) {
            // no code intended;
        }
        return null;
    }

    /**
     * Gets the localized user interface text from XML resource bundle of class
     * with arguments.
     *
     * @param <E>
     *            The Exception class.
     * @param clazz
     *            The class.
     * @param locale
     *            The {@link Locale}.
     * @param key
     *            The message key.
     * @param args
     *            The arguments.
     * @return The localized text.
     */
    public static <E extends Exception> String uiText(final Class<E> clazz,
            final Locale locale, final String key, final String... args) {

        if (Messages.isCustomI18nEnabled()) {
            final ResourceBundle bundle =
                    getResourceBundleCustom(clazz, locale);
            if (bundle != null && bundle.containsKey(key)) {
                return Messages.formatMessage(bundle.getString(key), args);
            }
        }
        return Messages.formatMessage(
                getResourceBundle(clazz, locale).getString(key), args);
    }

    /**
     * Gets the sibling locale variant of a file. A locale variant is formatted
     * as {@code name_xx_XX.ext} where 'xx' is the language code and 'XX' the
     * country/region code.
     *
     * @param file
     *            The plain file without locale naming.
     * @param locale
     *            The {@link Locale}.
     * @return The locale variant of the file, or the input file when no locale
     *         variant is found.
     */
    public static File getLocaleFile(final File file, final Locale locale) {

        final String lang = StringUtils.defaultString(locale.getLanguage());
        final String ctry = StringUtils.defaultString(locale.getCountry());

        if (StringUtils.isBlank(lang)) {
            return file;
        }

        final String filePathBase =
                FilenameUtils.removeExtension(file.getAbsolutePath());

        final String filePathExt = FilenameUtils.getExtension(file.getName());

        if (StringUtils.isNotBlank(ctry)) {

            final File fileLoc = new File(String.format("%s_%s_%s.%s",
                    filePathBase, lang, ctry, filePathExt));

            if (fileLoc.exists()) {
                return fileLoc;
            }
        }

        final File fileLoc = new File(
                String.format("%s_%s.%s", filePathBase, lang, filePathExt));

        if (fileLoc.exists()) {
            return fileLoc;
        }

        return file;
    }

    /**
     *
     * @return The comma separated list of supported languages.
     */
    private static String getSupportedLanguages() {

        final StringBuilder list = new StringBuilder();

        list.append(Locale.GERMANY.getLanguage()).append(',');
        list.append(Locale.US.getLanguage()).append(',');
        list.append(Locale.FRANCE.getLanguage()).append(',');
        list.append(LOCALE_LANG_SPANISH).append(',');
        list.append(LOCALE_LANG_POLISH).append(',');
        list.append(LOCALE_LANG_RUSSIAN).append(',');
        list.append(LOCALE_LANG_DUTCH);

        return list.toString();
    }

    /**
     *
     * @return The {@link Locale} list of available languages.
     */
    public static List<Locale> getAvailableLanguages() {

        final List<Locale> list = new ArrayList<>();

        String langAvailable = ConfigManager.instance()
                .getConfigValue(Key.WEBAPP_LANGUAGE_AVAILABLE).trim();

        if (StringUtils.isBlank(langAvailable)) {
            langAvailable = getSupportedLanguages();
        }

        for (final String lang : StringUtils.split(langAvailable, " ,;:")) {
            switch (lang.toLowerCase()) {
            case LOCALE_LANG_DUTCH:
                list.add(new Locale(LOCALE_LANG_DUTCH, LOCALE_CTRY_NL));
                break;
            case LOCALE_LANG_ENGLISH:
                list.add(Locale.US);
                break;
            case LOCALE_LANG_FRENCH:
                list.add(Locale.FRANCE);
                break;
            case LOCALE_LANG_GERMAN:
                list.add(Locale.GERMANY);
                break;
            case LOCALE_LANG_SPANISH:
                list.add(new Locale(LOCALE_LANG_SPANISH, LOCALE_CTRY_ES));
                break;
            case LOCALE_LANG_POLISH:
                list.add(new Locale(LOCALE_LANG_POLISH, LOCALE_CTRY_PL));
                break;
            case LOCALE_LANG_RUSSIAN:
                list.add(new Locale(LOCALE_LANG_RUSSIAN, LOCALE_CTRY_RU));
                break;
            default:
                break;
            }
        }

        return list;
    }

}
