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

import java.util.List;
import java.util.SortedSet;

import org.savapage.core.rfid.RfidNumberFormat;

/**
 *
 * @author Rijk Ravestein
 *
 */
public interface IUserSource {

    /**
     * Get user groups, sorted ascending (case insensitive) by group name.
     *
     * @return The groups or an empty set when no groups are found.
     */
    SortedSet<CommonUserGroup> getGroups();

    /**
     * Gets details of a user group from the user source.
     *
     * @param groupName
     *            The unique group name.
     * @return The user group details or {@code null} when the user group was
     *         not found in the user source.
     */
    CommonUserGroup getGroup(String groupName);

    /**
     * Gets hierarchy of group names.
     * <p>
     * This method is for diagnostic purposes. Nested groups are only supported
     * by Active Directory, all other user sources return an empty list.
     * </p>
     *
     * @param parentGroup
     *            The parent group.
     * @param formatted
     *            When {@code true} groups formatted, i.e. the DN name and group
     *            name are indented (with leading space) according to nesting
     *            level. When {@code false} a flat list of nested groups is
     *            returned.
     * @return The list with group names (space) indented according to the
     *         nesting level.
     */
    List<String> getGroupHierarchy(String parentGroup, boolean formatted);

    /**
     * Checks if a group is present.
     *
     * @param groupName
     *            The name of the group.
     * @return {@code true} if group is present.
     */
    boolean isGroupPresent(String groupName);

    /**
     * Is the user member of group?
     *
     * @param uid
     *            The user id.
     * @param groupName
     *            The group name.
     * @return {@code true} if user is member of group.
     */
    boolean isUserInGroup(String uid, String groupName);

    /**
     * Gets all the users from the user source, sorted ascending (case
     * sensitive) by user name.
     *
     * @return The sorted set of users.
     */
    SortedSet<CommonUser> getUsers();

    /**
     * Gets details of a user from the user source.
     *
     * @param uid
     *            The user id.
     * @return The user details or {@code null} when the user was not found in
     *         the user source.
     */
    CommonUser getUser(String uid);

    /**
     * Gets all the users from the user source belonging to a group, sorted
     * ascending (case sensitive) by user name.
     * <p>
     * Note: Users from nested groups (if supported) are NOT accumulated.
     * </p>
     *
     * @param groupName
     *            The group name.
     * @return The sorted set of users.
     */
    SortedSet<CommonUser> getUsersInGroup(String groupName);

    /**
     * Gets all the users from the user source belonging to a group, sorted
     * ascending (case sensitive) by user name.
     *
     * @param groupName
     *            The group name.
     * @param nested
     *            If {@code true}, then accumulate members from nested groups.
     *            Note: nested groups are supported by Active Directory only.
     * @return The sorted set of users.
     */
    SortedSet<CommonUser> getUsersInGroup(String groupName, boolean nested);

    /**
     * Creates the formatter to convert Card Numbers from the source to the
     * normalized form.
     *
     * @return {@code null} when no Card Numbers are provided by the user
     *         source.
     */
    RfidNumberFormat createRfidNumberFormat();

    /**
     *
     * @return {@code true} when User Source provides a User ID Number.
     */
    boolean isIdNumberProvided();

    /**
     *
     * @return {@code true} when User Source provides a User Card Number.
     */
    boolean isCardNumberProvided();

    /**
     *
     * @return {@code true} when User Source provides a User Email address.
     */
    boolean isEmailProvided();

}
