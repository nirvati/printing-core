/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2016 Datraverse B.V.
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
package org.savapage.core.dao.enums;

import org.savapage.core.jpa.UserGroup;

/**
 * Reserved group names for {@link UserGroup}.
 * <p>
 * Note for developers: do NOT change the group name values, since it will
 * invalidate current database content.
 * </p>
 *
 * @author Rijk Ravestein
 *
 */
public enum ReservedUserGroupEnum {

    /**
     * All users.
     */
    ALL("!!All Users!!", "All Users"),
    /**
     * External users as synchronized from an external source.
     */
    EXTERNAL("!!External Users!!", "External Users"),
    /**
     * Internal users.
     */
    INTERNAL("!!Internal Users!!", "Internal Users");

    /**
     * The group name as used in the database.
     * <p>
     * Note for developers: do NOT change this value, since it will invalidate
     * current database content.
     * </p>
     */
    private final String groupName;

    /**
     * The group name as used in the UI.
     */
    private final String uiName;

    /**
     *
     * @param name
     *            The group name as used in the database.
     * @param ui
     *            The group name as used in the UI.
     */
    ReservedUserGroupEnum(final String name, final String ui) {
        this.groupName = name;
        this.uiName = ui;
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

    /**
     * @return the group name as used in the UI.
     */
    public String getUiName() {
        return uiName;
    }

    /**
     * Gets the {@link ReservedUserGroupEnum} from the database name.
     *
     * @param groupName
     *            The group name in the database.
     * @return {@code null} when not found.
     */
    public static ReservedUserGroupEnum fromDbName(final String groupName) {
        for (ReservedUserGroupEnum value : ReservedUserGroupEnum.values()) {
            if (groupName.equalsIgnoreCase(value.getGroupName())) {
                return value;
            }
        }
        return null;
    }

    /**
     * @return {@link Boolean#TRUE} when internal group, {@link Boolean#FALSE}
     *         when external group, otherwise {@code null}.
     */
    public Boolean isInternalExternal() {
        final Boolean internal;
        switch (this) {
        case EXTERNAL:
            internal = Boolean.FALSE;
            break;
        case INTERNAL:
            internal = Boolean.TRUE;
            break;
        default:
            internal = null;
        }
        return internal;
    }

}
