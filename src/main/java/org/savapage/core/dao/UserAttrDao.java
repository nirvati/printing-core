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

import org.savapage.core.dao.helpers.UserAttrEnum;
import org.savapage.core.jpa.User;
import org.savapage.core.jpa.UserAttr;

/**
 *
 * @author Datraverse B.V.
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
     * @param user
     *            The {@link User}.
     * @param name
     *            The {@link UserAttrEnum}.
     * @return The {@link UserAttr} or {@code null} when not found.
     */
    UserAttr findByName(User user, UserAttrEnum name);

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

}
