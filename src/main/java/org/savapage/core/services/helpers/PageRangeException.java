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
package org.savapage.core.services.helpers;

import java.util.Locale;

import org.savapage.core.util.LocaleHelper;

/**
 * A page ranges exception related to total number of pages.
 *
 * @author Rijk Ravestein
 *
 */
public final class PageRangeException extends Exception {

    /** */
    public enum Reason {
        /** Syntax error. */
        SYNTAX,
        /** Logical error. */
        RANGE
    };

    /** */
    private final Reason reason;
    /** */
    private final int totPages;
    /** */
    private final String pageRanges;

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new {@link PageRangeException}.
     *
     * @param cause
     *            The cause.
     */
    public PageRangeException(final Reason reason, final int totPages,
            final String pageRanges) {
        super();
        this.reason = reason;
        this.totPages = totPages;
        this.pageRanges = pageRanges;
    }

    @Override
    public String getMessage() {
        return getMessage(Locale.ENGLISH);
    }

    /**
     * Gets a localized message.
     *
     * @param locale
     *            The locale.
     * @return The message.
     */
    public String getMessage(final Locale locale) {

        if (this.reason == Reason.SYNTAX) {
            return LocaleHelper.uiText(this.getClass(), locale, "syntax-error",
                    String.valueOf(this.pageRanges));
        }
        return LocaleHelper.uiText(this.getClass(), locale, "range-error",
                this.pageRanges, String.valueOf(this.totPages));
    }
}
