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

import org.savapage.core.ipp.attribute.IppDictJobTemplateAttr;
import org.savapage.core.ipp.attribute.syntax.IppKeyword;
import org.savapage.core.jpa.PrintOut;
import org.savapage.core.services.ProxyPrintService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.util.LocaleHelper;

/**
 * UI Nouns for {@link PrintOut}.
 *
 * @author Rijk Ravestein
 *
 */
public enum PrintOutNounEnum {

    /** */
    JOB(true), ACCOUNT(true), PAGE(true), COPY(true), SHEET(true),

    /** */
    FINISHING(true), TRAY(true), SETTING(true),

    /** */
    BOOKLET,

    /** */
    JOB_SHEET(true),

    /**  */
    COLOR(IppDictJobTemplateAttr.ATTR_PRINT_COLOR_MODE, //
            IppKeyword.PRINT_COLOR_MODE_COLOR),

    /**  */
    GRAYSCALE(IppDictJobTemplateAttr.ATTR_PRINT_COLOR_MODE, //
            IppKeyword.PRINT_COLOR_MODE_MONOCHROME),

    /** */
    DUPLEX,

    /** */
    SIMPLEX,

    /** */
    PORTRAIT, LANDSCAPE,

    /**
     * Pass N as argument to get the UI text.
     */
    N_UP;

    /**
     *
     */
    private static final ProxyPrintService PROXYPRINT_SERVICE =
            ServiceContext.getServiceFactory().getProxyPrintService();

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

    /** */
    private final String ippAttr;

    /** */
    private final String ippChoice;

    /**
     *
     */
    PrintOutNounEnum() {
        this.hasPlural = false;
        this.ippAttr = null;
        this.ippChoice = null;
    }

    /**
     *
     */
    PrintOutNounEnum(final boolean plural) {
        this.hasPlural = plural;
        this.ippAttr = null;
        this.ippChoice = null;
    }

    /**
     *
     */
    PrintOutNounEnum(final String attr, final String choice) {
        this.hasPlural = false;
        this.ippAttr = attr;
        this.ippChoice = choice;
    }

    /**
     * @param locale
     *            The {@link Locale}.
     * @return The localized text.
     */
    public String uiText(final Locale locale) {

        if (this.ippAttr != null) {
            return PROXYPRINT_SERVICE.localizePrinterOptValue(locale,
                    this.ippAttr, this.ippChoice);
        }

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

    /**
     * Get the localized UI text with argument. To be used for {@link #N_UP}.
     *
     * @param locale
     *            The {@link Locale}.
     * @param args
     *            The arguments.
     * @return The localized text.
     */
    public String uiText(final Locale locale, final String... args) {
        if (this == N_UP) {
            return LocaleHelper.uiTextArgs(this, locale, args);
        }
        throw new IllegalArgumentException(String
                .format("%s does not support arguments.", this.toString()));
    }
}
