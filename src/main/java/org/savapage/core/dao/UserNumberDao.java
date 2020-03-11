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

import org.savapage.core.jpa.UserNumber;

/**
 *
 * @author Rijk Ravestein
 *
 */
public interface UserNumberDao extends GenericDao<UserNumber> {

    /**
     * The index of the primary {@link UserNumber}.
     */
    int INDEX_NUMBER_PRIMARY_NUMBER = 0;

    /**
     * The index of the YubiKey {@link UserNumber}.
     */
    int INDEX_NUMBER_YUBIKEY_NUMBER = 100;

    /**
     * Is this {@link UserNumber} a primary number?
     *
     * @param number
     *            The {@link UserNumber}.
     * @return {@code true} if number is primary.
     */
    boolean isPrimaryNumber(UserNumber number);

    /**
     * Is this {@link UserNumber} a YubiKey Public ID?
     *
     * @param number
     *            The {@link UserNumber}.
     * @return {@code true} if number is YubiKey Public ID.
     */
    boolean isYubiKeyPubID(UserNumber number);

    /**
     * Gets the YubiKey Public ID from a {@link UserNumber}.
     *
     * @param number
     *            The {@link UserNumber}.
     * @return {@code null} when not a YubiKey Public ID.
     */
    String getYubiKeyPubID(UserNumber number);

    /**
     * Composes the number stored in database for the YubiKey Public ID.
     *
     * @param publicId
     *            the YubiKey Public ID.
     * @return The composed database number value.
     */
    String composeYubiKeyDbNumber(String publicId);

    /**
     * Makes number a primary {@link UserNumber}.
     *
     * @param number
     *            The {@link UserNumber}.
     */
    void assignPrimaryNumber(UserNumber number);

    /**
     * Makes number a YubiKey {@link UserNumber}.
     *
     * @param number
     *            The {@link UserNumber}.
     */
    void assignYubiKeyNumber(UserNumber number);

    /**
     * Finds a {@link UserNumber} by number.
     * <p>
     * When offered number is blank, {@code null} is returned.
     * </p>
     *
     * @param number
     *            The unique ID number.
     * @return The {@link UserNumber} or {@code null} when not found.
     */
    UserNumber findByNumber(String number);

    /**
     * Finds a {@link UserNumber} by YubiKey Public ID.
     *
     * @param yubiKeyID
     *            The public ID of the YubiKey
     * @return The {@link UserNumber} or {@code null} when not found.
     */
    UserNumber findByYubiKeyPubID(String yubiKeyID);
}
