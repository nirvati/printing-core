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
package org.savapage.core.dao.enums;

import java.util.Locale;

import org.savapage.core.jpa.DocLog;
import org.savapage.core.util.LocaleHelper;

/**
 * Generic status of (proxy) print request from External Supplier.
 * <ul>
 * <li>Do NOT changes enum names since they are stored in the database.</li>
 * <li>Enum names must fit the size of the
 * {@link DocLog#setExternalStatus(String)} column.</li>
 * </ul>
 *
 * @author Rijk Ravestein
 *
 */
public enum ExternalSupplierStatusEnum {

    /**
     * .
     */
    CANCELLED,

    /**
     * .
     */
    COMPLETED,

    /**
     * .
     */
    ERROR,

    /**
     * A PENDING print expired.
     */
    EXPIRED,

    /**
     * Pending in SavaPage als Job Ticket or HOLD print.
     */
    PENDING,

    /**
     * The {@link #PENDING} status is cancelled by a User (Hold) or Job Ticket
     * operator.
     * <p>
     * NOTE: This is an <i>intermediate</i> (technical) status, picked up by a
     * monitor, communicated to the external supplier, and finalized to status
     * {@link #CANCELLED}.
     * </p>
     */
    PENDING_CANCEL,

    /**
     * The {@link #PENDING} status is changed by the release of a User (Hold) or
     * Job Ticket.
     * <p>
     * NOTE: This is an <i>intermediate</i> (technical) status, picked up by a
     * monitor, communicated to the external supplier, and finalized to status
     * {@link #COMPLETED}.
     * </p>
     */
    PENDING_COMPLETE,

    /**
     * Proxy Print request is pending in an external system like PaperCut.
     */
    PENDING_EXT;

    /**
     * @param locale
     *            The {@link Locale}.
     * @return The localized text.
     */
    public String uiText(final Locale locale) {
        return LocaleHelper.uiText(this, locale);
    }

    /**
     * Checks if this status is a failure.
     *
     * @return {@code true} when state is CANCELED, ERROR or EXPIRED
     */
    public boolean isFailure() {
        return this == CANCELLED || this == ERROR || this == EXPIRED;
    }

    /**
     * Checks if this status is a finished.
     *
     * @return {@code true} when state is COMPLETED, CANCELED, ERROR or EXPIRED
     */
    public boolean isFinished() {
        return this == COMPLETED || this.isFailure();
    }
}
