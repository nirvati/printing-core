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
package org.savapage.core.dao;

import org.savapage.core.jpa.UserCard;

/**
 *
 * @author Rijk Ravestein
 *
 */
public interface UserCardDao extends GenericDao<UserCard> {

    /**
     * The index of the primary {@link UserCard).
     */
    int INDEX_NUMBER_PRIMARY_CARD = 0;

    /**
     * Is this {@link UserCard} a primary card?
     *
     * @param card
     *            The {@link UserCard}.
     * @return {@code true} if card is primary.
     */
    boolean isPrimaryCard(UserCard card);

    /**
     * Makes card a primary {@link UserCard}.
     *
     * @param card
     *            The {@link UserCard}.
     */
    void assignPrimaryCard(UserCard card);

    /**
     * Finds a {@link UserCard} by Card Number.
     * <p>
     * When offered Card Number is blank, {@code null} is returned.
     * </p>
     *
     * @param cardNumber
     *            The unique card number.
     * @return The {@link UserCard} or {@code null} when not found.
     */
    UserCard findByCardNumber(String cardNumber);
}
