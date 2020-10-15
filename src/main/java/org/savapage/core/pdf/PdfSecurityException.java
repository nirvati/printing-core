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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.core.pdf;

import org.savapage.core.i18n.PhraseEnum;

/**
 * An exception to report an encrypted PDF document.
 *
 * @author Rijk Ravestein
 *
 */
public final class PdfSecurityException extends PdfAbstractException {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     * {@code true} if printing is allowed.
     */
    private final boolean printingAllowed;

    /**
     *
     * @param message
     *            Message.
     * @param phrase
     *            Message for logging.
     * @param printAllowed
     *            {@code true} if printing is allowed.
     */
    public PdfSecurityException(final String message, final PhraseEnum phrase,
            final boolean printAllowed) {
        super(message, phrase);
        this.printingAllowed = printAllowed;
    }

    /**
     *
     * @return {@code true} if printing is allowed.
     */
    public boolean isPrintingAllowed() {
        return printingAllowed;
    }

}
