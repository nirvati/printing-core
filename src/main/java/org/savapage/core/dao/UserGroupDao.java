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

import org.savapage.core.dao.helpers.ReservedUserGroupEnum;
import org.savapage.core.jpa.UserGroup;

/**
 *
 * @author Datraverse B.V.
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
         * Property name.
         */
        NAME
    }

    /**
     *
     */
    class ListFilter {

        private String containingText;

        public String getContainingText() {
            return containingText;
        }

        public void setContainingText(String containingText) {
            this.containingText = containingText;
        }

    }

    /**
     *
     * @param filter
     * @return
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
     * Finds a reserved {@link UserGroup}.
     *
     * @param reservedGroup
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
