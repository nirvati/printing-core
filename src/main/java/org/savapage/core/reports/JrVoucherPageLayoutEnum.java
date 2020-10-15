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
package org.savapage.core.reports;

import java.util.Locale;

import org.savapage.core.SpException;
import org.savapage.core.fonts.InternalFontFamilyEnum;

import net.sf.jasperreports.engine.design.JasperDesign;

/**
 *
 * @author Rijk Ravestein
 *
 */
public enum JrVoucherPageLayoutEnum {

    A4_2X5("A4 2x5"),
    //
    A4_3X5("A4 3x5"),
    //
    LETTER_2X5("Letter 2x5"),
    //
    LETTER_3X5("Letter 3x5");

    private final String name;

    private JrVoucherPageLayoutEnum(final String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    /**
     * Creates {@link JasperDesign}.
     *
     * @param defaultFontName
     * @param locale
     * @return
     */
    public JasperDesign createDesign(InternalFontFamilyEnum defaultFontName,
            Locale locale) {
        switch (this) {
        case A4_2X5:
            return JrVoucherPageDesign.createA4With2x5(defaultFontName, locale);
        case A4_3X5:
            return JrVoucherPageDesign.createA4With3x5(defaultFontName, locale);
        case LETTER_2X5:
            return JrVoucherPageDesign.createLetterWith2x5(defaultFontName,
                    locale);
        case LETTER_3X5:
            return JrVoucherPageDesign.createLetterWith3x5(defaultFontName,
                    locale);
        default:
            throw new SpException("No design available for " + this.toString());
        }
    }
}
