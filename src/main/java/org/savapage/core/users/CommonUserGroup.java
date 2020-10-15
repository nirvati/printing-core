/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2020 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: © 2020 Datraverse B.V. <info@datraverse.com>
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
package org.savapage.core.users;

import org.savapage.core.jpa.UserGroup;

/**
 *
 * @author Rijk Ravestein
 *
 */
public class CommonUserGroup {

    /** */
    private final String groupName;
    /** */
    private final String fullName;

    /**
     *
     * @param name
     *            The group name.
     */
    public CommonUserGroup(final String name) {
        this.groupName = name;
        this.fullName = null;
    }

    /**
     *
     * @param name
     *            The group name.
     * @param full
     *            The full name.
     */
    public CommonUserGroup(final String name, final String full) {
        this.groupName = name;
        this.fullName = full;
    }

    /**
     * @return The group name.
     */
    public String getGroupName() {
        return groupName;
    }

    /**
     * @return The full group name.
     */
    public String getFullName() {
        return fullName;
    }

    /**
     * Creates a {@link UserGroup} from this {@link CommonUserGroup} instance.
     *
     * @return {@link UserGroup}.
     */
    public UserGroup createUserGroup() {

        final UserGroup group = new UserGroup();
        group.setGroupName(this.getGroupName());

        return group;
    }

}
