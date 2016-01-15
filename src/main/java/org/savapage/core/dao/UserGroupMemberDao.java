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
package org.savapage.core.dao;

import java.util.List;

import org.savapage.core.jpa.User;
import org.savapage.core.jpa.UserGroup;
import org.savapage.core.jpa.UserGroupMember;

/**
 *
 * @author Datraverse B.V.
 *
 */
public interface UserGroupMemberDao extends GenericDao<UserGroupMember> {

    /**
     * Field identifiers used for select and sort.
     */
    enum GroupField {
        GROUP_NAME
    }

    /**
     * Field identifiers used for select and sort.
     */
    enum UserField {
        USER_NAME
    }

    /**
     * Filter to select a User.
     */
    class UserFilter {

        private Long userId;

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }
    }

    /**
     * Filter to select a Group.
     */
    class GroupFilter {

        private Long groupId;

        public Long getGroupId() {
            return groupId;
        }

        public void setGroupId(Long groupId) {
            this.groupId = groupId;
        }

    }

    /**
     * Gets the number of groups a user belongs to.
     *
     * @param filter
     *            The filter.
     * @return The number of filtered entries.
     */
    long getGroupCount(UserFilter filter);

    /**
     * Gets the number of user members of a group.
     *
     * @param groupId
     *            The ID of the {@link UserGroup}.
     * @return The number of members.
     */
    long getUserCount(Long groupId);

    /**
     * Gets the number of user members of a group.
     *
     * @param filter
     *            The filter.
     * @return The number of filtered entries.
     */
    long getUserCount(GroupFilter filter);

    /**
     *
     * Gets a chunk of user groups a user belongs to.
     *
     * @param filter
     *            The filter.
     * @param startPosition
     *            The zero-based start position of the chunk related to the
     *            total number of rows. If {@code null} the chunk starts with
     *            the first row.
     * @param maxResults
     *            The maximum number of rows in the chunk. If {@code null}, then
     *            ALL (remaining rows) are returned.
     * @param orderBy
     *            The sort field.
     * @param sortAscending
     *            {@code true} when sorted ascending.
     * @return The chunk.
     */
    List<UserGroup> getGroupChunk(UserFilter filter, Integer startPosition,
            Integer maxResults, GroupField orderBy, boolean sortAscending);

    /**
     *
     * Gets a chunk of user members of a group.
     *
     * @param filter
     *            The filter.
     * @param startPosition
     *            The zero-based start position of the chunk related to the
     *            total number of rows. If {@code null} the chunk starts with
     *            the first row.
     * @param maxResults
     *            The maximum number of rows in the chunk. If {@code null}, then
     *            ALL (remaining rows) are returned.
     * @param orderBy
     *            The sort field.
     * @param sortAscending
     *            {@code true} when sorted ascending.
     * @return The chunk.
     */
    List<User> getUserChunk(GroupFilter filter, Integer startPosition,
            Integer maxResults, UserField orderBy, boolean sortAscending);

    /**
     * Gets the full {@link UserGroupMember} list of a {@link UserGroup} sorted
     * (ascending) by user name.
     *
     * @param groupId
     *            The primary key of the {@link UserGroup}.
     * @return The list.
     */
    List<UserGroupMember> getGroupMembers(Long groupId);

    /**
     * Delete all member of a group.
     *
     * @param groupId
     *            The primary key of the the group.
     * @return The number of deleted group members.
     */
    int deleteGroup(Long groupId);
}
