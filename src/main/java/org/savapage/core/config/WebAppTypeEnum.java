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
package org.savapage.core.config;

/**
 * The type of Web Application.
 *
 * @author Rijk Ravestein
 *
 */
public enum WebAppTypeEnum {
    /**
     * The Admin WebApp.
     */
    ADMIN("Admin"),

    /**
     * Job Tickets WebApp.
     */
    JOBTICKETS("Job Tickets"),

    /**
     * Mail Tickets WebApp.
     */
    MAILTICKETS("Mail Tickets"),

    /**
     * The Payment WebApp.
     */
    PAYMENT("Payment"),

    /**
     * The Point-of-Sale WebApp.
     */
    POS("POS"),

    /**
     * The Print Site WebApp.
     */
    PRINTSITE("Print Site"),

    /**
     * The User WebApp.
     */
    USER("User"),

    /**
     * The WebApp type is undefined (unknown).
     */
    UNDEFINED("Unknown");

    /** */
    private final String uiText;

    /**
     *
     * @param text
     *            The UI text.
     */
    WebAppTypeEnum(final String text) {
        this.uiText = text;
    }

    /**
     * @return The UI text.
     */
    public String getUiText() {
        return uiText;
    }

    /**
     * @return {@code true} if this represents the User Web App or a subclassed
     *         variant.
     */
    public boolean isUserTypeOrVariant() {
        return this == USER || this == MAILTICKETS || this == PAYMENT;
    }

}
