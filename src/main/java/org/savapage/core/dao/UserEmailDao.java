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

import org.savapage.core.jpa.UserEmail;

/**
 *
 * @author Datraverse B.V.
 *
 */
public interface UserEmailDao extends GenericDao<UserEmail> {

    /**
     * The index of the primary {@link UserEmail}.
     */
    int INDEX_NUMBER_PRIMARY_EMAIL = 0;

    /**
     * Is this {@link UserEmail} a primary email?
     *
     * @param email
     *            The {@link UserEmail}.
     * @return {@code true} if email is primary.
     */
    boolean isPrimaryEmail(UserEmail email);

    /**
     * Makes email a primary {@link UserEmail}.
     *
     * @param email
     *            The {@link UserEmail}.
     */
    void assignPrimaryEmail(UserEmail email);

    /**
     * Finds a {@link UserEmail} by email.
     * <p>
     * When offered email is blank, {@code null} is returned.
     * </p>
     *
     * @param email
     *            The unique email address.
     * @return The {@link UserEmail} or {@code null} when not found.
     */
    UserEmail findByEmail(String email);

}
