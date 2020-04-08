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

import org.savapage.core.dao.enums.UserAttrEnum;
import org.savapage.core.jpa.User;
import org.savapage.core.jpa.UserAttr;

/**
 *
 * @author Rijk Ravestein
 *
 */
public interface UserAttrDao extends GenericDao<UserAttr> {

    /**
     * Fragment used in all rolling statistics attributes.
     */
    String STATS_ROLLING = "stats.rolling";

    /**
     * Finds a {@link UserAttr} of a {@link User} by attribute id.
     *
     * @param userDbKey
     *            The primary database key of {@link User}.
     * @param name
     *            The {@link UserAttrEnum}.
     * @return The {@link UserAttr} or {@code null} when not found.
     */
    UserAttr findByName(Long userDbKey, UserAttrEnum name);

    /**
     * Finds a {@link UserAttr} of a {@link User} by attribute name.
     *
     * @param userDbKey
     *            The primary database key of {@link User}.
     * @param name
     *            The {@link UserAttr#getName()}.
     * @return The {@link UserAttr} or {@code null} when not found.
     */
    UserAttr findByName(Long userDbKey, String name);

    /**
     * Finds the unique {@link UserAttr} combination of a {@link UserAttrEnum}
     * and value.
     *
     * @param name
     *            The {@link UserAttrEnum}.
     * @param value
     *            The unique value.
     * @return The {@link UserAttr} or {@code null} when not found.
     */
    UserAttr findByNameValue(UserAttrEnum name, String value);

    /**
     * Deletes rolling statistics for users.
     */
    void deleteRollingStats();

    /**
     * Returns attribute value as boolean.
     *
     * @see {@link UserAttr#getValue()}.
     * @param attr
     *            The {@link UserAttr} ({@code null} is allowed).
     * @return {@code true} When value is {@code true}.
     */
    boolean getBooleanValue(UserAttr attr);

    /**
     * Returns the database value of a boolean value.
     *
     * @param value
     *            The value.
     * @return The string representation of a boolean value
     */
    String getDbBooleanValue(boolean value);
}
