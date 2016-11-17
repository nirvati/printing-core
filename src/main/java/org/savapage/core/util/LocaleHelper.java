/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2016 Datraverse B.V.
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
package org.savapage.core.util;

import java.io.File;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
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
    private static final String LOCALE_LANG_SPANISH = "es";

    private static final String LOCALE_CTRY_NL = "NL";
    private static final String LOCALE_CTRY_ES = "ES";

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
     * Gets as localized (short) date string of a Date.
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
        return Messages
                .loadXmlResource(value.getClass(),
                        value.getClass().getSimpleName(), locale)
                .getString(value.toString());
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
            case LOCALE_LANG_GERMAN:
                list.add(Locale.GERMANY);
                break;
            case LOCALE_LANG_ENGLISH:
                list.add(Locale.US);
                break;
            case LOCALE_LANG_SPANISH:
                list.add(new Locale(LOCALE_LANG_SPANISH, LOCALE_CTRY_ES));
                break;
            case LOCALE_LANG_FRENCH:
                list.add(Locale.FRANCE);
                break;
            case LOCALE_LANG_DUTCH:
                list.add(new Locale(LOCALE_LANG_DUTCH, LOCALE_CTRY_NL));
                break;
            default:
                break;
            }
        }

        return list;
    }

}
