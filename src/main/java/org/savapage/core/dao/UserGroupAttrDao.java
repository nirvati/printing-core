/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2016 Datraverse B.V.
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

import org.savapage.core.dao.enums.UserGroupAttrEnum;
import org.savapage.core.jpa.UserGroup;
import org.savapage.core.jpa.UserGroupAttr;

/**
 *
 * @author Rijk Ravestein
 *
 */
public interface UserGroupAttrDao extends GenericDao<UserGroupAttr> {

    /**
     * Finds a {@link UserGroupAttr} of a {@link UserGroup} by attribute id.
     *
     * @param group
     *            The {@link UserGroup}.
     * @param name
     *            The {@link UserGroupAttrEnum}.
     * @return The {@link UserGroupAttr} or {@code null} when not found.
     */
    UserGroupAttr findByName(UserGroup group, UserGroupAttrEnum name);

}
