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

import java.util.List;
import java.util.Set;

import org.savapage.core.dao.enums.ACLRoleEnum;
import org.savapage.core.dao.enums.ReservedUserGroupEnum;
import org.savapage.core.jpa.UserGroup;

/**
 *
 * @author Rijk Ravestein
 *
 */
public interface UserGroupDao extends GenericDao<UserGroup> {

    /**
     *
     */
    enum SchedulePeriodEnum {
        NONE, DAILY, WEEKLY, MONTHLY, CUSTOM
    }

    /**
     * Field identifiers used for select and sort.
     */
    enum Field {
        /**
         * Group name.
         */
        ID,
        /**
         * Group full name.
         */
        NAME
    }

    /**
     *
     */
    class ListFilter {

        private String containingIdText;
        private String containingNameText;
        private String containingNameOrIdText;

        private ACLRoleEnum aclRole;
        private Set<Long> groupIds;

        public String getContainingIdText() {
            return containingIdText;
        }

        public void setContainingIdText(String containingIdText) {
            this.containingIdText = containingIdText;
        }

        public String getContainingNameText() {
            return containingNameText;
        }

        public void setContainingNameText(String containingNameText) {
            this.containingNameText = containingNameText;
        }

        public String getContainingNameOrIdText() {
            return containingNameOrIdText;
        }

        public void setContainingNameOrIdText(String containingNameOrIdText) {
            this.containingNameOrIdText = containingNameOrIdText;
        }

        public ACLRoleEnum getAclRole() {
            return aclRole;
        }

        public void setAclRole(ACLRoleEnum aclRole) {
            this.aclRole = aclRole;
        }

        public Set<Long> getGroupIds() {
            return groupIds;
        }

        public void setGroupIds(Set<Long> groupIds) {
            this.groupIds = groupIds;
        }

    }

    /**
     *
     * @param filter
     *            The {@link ListFilter}.
     * @return The number of filtered rows.
     */
    long getListCount(ListFilter filter);

    /**
     *
     * Gets a chunk of user groups.
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
    List<UserGroup> getListChunk(ListFilter filter, Integer startPosition,
            Integer maxResults, Field orderBy, boolean sortAscending);

    /**
     * Finds the {@link ReservedUserGroupEnum} by primary key.
     *
     * @param id
     *            The primary key.
     * @return The {@link ReservedUserGroupEnum}, or {@code null} when not
     *         found.
     */
    ReservedUserGroupEnum findReservedGroup(Long id);

    /**
     * Finds a reserved {@link UserGroup} by enum.
     *
     * @param reservedGroup
     *            The {@link ReservedUserGroupEnum}.
     * @return The {@link UserGroup}, or {@code null} when not found.
     */
    UserGroup find(ReservedUserGroupEnum reservedGroup);

    /**
     * Finds a {@link UserGroup} by name.
     *
     * @param groupName
     *            The group name.
     * @return The {@link UserGroup}, or {@code null} when not found.
     */
    UserGroup findByName(String groupName);

}
