/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2014 Datraverse B.V.
 * Author: Rijk Ravestein.
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
package org.savapage.core.dao;

import org.savapage.core.jpa.UserNumber;

/**
 *
 * @author Datraverse B.V.
 *
 */
public interface UserNumberDao extends GenericDao<UserNumber> {

    /**
     * The index of the primary {@link UserNumber}.
     */
    int INDEX_NUMBER_PRIMARY_NUMBER = 0;

    /**
     * Is this {@link UserNumber} a primary number?
     *
     * @param number
     *            The {@link UserNumber}.
     * @return {@code true} if number is primary.
     */
    boolean isPrimaryNumber(UserNumber number);

    /**
     * Makes number a primary {@link UserNumber}.
     *
     * @param number
     *            The {@link UserNumber}.
     */
    void assignPrimaryNumber(UserNumber number);

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

}
