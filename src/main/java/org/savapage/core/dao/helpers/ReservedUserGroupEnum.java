/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2014 Datraverse B.V.
 * Authors: Rijk Ravestein.
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
package org.savapage.core.dao.helpers;

import org.savapage.core.jpa.UserGroup;

/**
 * Reserved group names for {@link UserGroup}.
 * <p>
 * Note for developers: do NOT change the group name values, since it will
 * invalidate current database content.
 * </p>
 *
 * @author Datraverse B.V.
 * @since 0.9.6
 */
public enum ReservedUserGroupEnum {

    /**
     * External users as synchronized from an external source.
     */
    EXTERNAL("!!External Users!!"),
    /**
     * Internal users.
     */
    INTERNAL("!!Internal Users!!");

    /**
     * The group name as used in the database.
     * <p>
     * Note for developers: do NOT change this value, since it will invalidate
     * current database content.
     * </p>
     */
    private final String groupName;

    private ReservedUserGroupEnum(final String groupName) {
        this.groupName = groupName;
    }

    /**
     * Gets the unique database group name as used in
     * {@link UserGroup#getGroupName()} and
     * {@link UserGroup#setGroupName(String)}.
     *
     * @return The group name.
     */
    public String getGroupName() {
        return groupName;
    }

}
