/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2020 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: 2011-2020 Datraverse B.V. <info@datraverse.com>
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
package org.savapage.core.dao.enums;

import java.util.Locale;

import org.savapage.core.jpa.AccountTrx;
import org.savapage.core.util.LocaleHelper;

/**
 * The trigger (reason why) for a credit assignment or change.
 * <p>
 * The string value of this enum is stored in the database. Therefore the length
 * of the enum value is limited. See {@link AccountTrx#COL_TRX_TYPE_LENGTH}.
 * </p>
 *
 * @author Rijk Ravestein
 *
 */
public enum AccountTrxTypeEnum {

    /**
     * "Manual adjustment".
     */
    ADJUST,

    /**
     * "Manual transaction" (deposit funds at point-of-sale).
     */
    DEPOSIT,

    /**
     * Funds transferred via a Payment Gateway.
     */
    GATEWAY,

    /**
     * "Initial allocation".
     */
    INITIAL,

    /**
     * PDF download.
     */
    PDF_OUT,

    /**
     *
     */
    PRINT_IN,

    /**
     * A proxy print.
     */
    PRINT_OUT,

    /**
     * Purchase at point-of-sale.
     */
    PURCHASE,

    /**
     * Used for both the "send" and "receive" part of a "move" transaction: a
     * user transferring (part of) his account balance to the account of a
     * fellow user.
     */
    TRANSFER,

    /**
     * "Voucher use".
     */
    VOUCHER;

    /**
     * @param locale
     *            The {@link Locale}.
     * @return The localized text.
     */
    public String uiText(final Locale locale) {
        return LocaleHelper.uiText(this, locale);
    }

}
