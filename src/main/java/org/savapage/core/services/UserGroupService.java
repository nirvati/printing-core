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
package org.savapage.core.services;

import java.io.IOException;

import org.savapage.core.dao.helpers.DaoBatchCommitter;
import org.savapage.core.dto.UserGroupPropertiesDto;
import org.savapage.core.jpa.UserGroup;
import org.savapage.core.json.rpc.AbstractJsonRpcMethodResponse;
import org.savapage.core.json.rpc.JsonRpcMethodError;
import org.savapage.core.json.rpc.impl.ResultListQuickSearchItem;
import org.savapage.core.json.rpc.impl.ResultListStrings;

/**
 *
 * @author Datraverse B.V.
 *
 */
public interface UserGroupService {

    /**
     * Creates the reserved user groups when they do not exist.
     */
    void lazyCreateReservedGroups();

    /**
     * Gets the reserved external {@link UserGroup}.
     *
     * @return The {@link UserGroup} or {@code null} when not found.
     */
    UserGroup getExternalUserGroup();

    /**
     * Gets the reserved internal {@link UserGroup}.
     *
     * @return The {@link UserGroup} or {@code null} when not found.
     */
    UserGroup getInternalUserGroup();

    /**
     * Checks if group name is reserved.
     *
     * @param groupName
     *            The group name.
     * @return {@code true} if group name is reserved.
     */
    boolean isReservedGroupName(String groupName);

    /**
     * Lists UserGroups sorted by groupName.
     *
     * @param startIndex
     *            0-based index of the first item in the resulting data.items.
     * @param itemsPerPage
     *            The number of items in the result. This is not necessarily the
     *            size of the data.items array. I.e. in the last page of items,
     *            the size of data.items may be less than itemsPerPage. However
     *            the size of data.items should not exceed itemsPerPage.
     * @return The JSON-RPC Return message: {@link ResultListQuickSearchItem} or
     *         an {@link JsonRpcMethodError};
     * @throws IOException
     *             When something goes wrong.
     */
    AbstractJsonRpcMethodResponse listUserGroups(Integer startIndex,
            Integer itemsPerPage) throws IOException;

    /**
     * Retrieves the groups a user belongs to, sorted by group name.
     *
     * @param userName
     *            The unique user name.
     * @param startIndex
     *            0-based index of the first item in the resulting data.items.
     * @param itemsPerPage
     *            The number of items in the result. This is not necessarily the
     *            size of the data.items array. I.e. in the last page of items,
     *            the size of data.items may be less than itemsPerPage. However
     *            the size of data.items should not exceed itemsPerPage.
     * @return The JSON-RPC Return message: {@link ResultListQuickSearchItem} or
     *         an {@link JsonRpcMethodError};
     * @throws IOException
     *             When something goes wrong.
     */
    AbstractJsonRpcMethodResponse listUserGroupMemberships(String userName,
            Integer startIndex, Integer itemsPerPage) throws IOException;

    /**
     * Retrieves the members of a group, sorted by user name.
     *
     * @param groupName
     *            The unique group name.
     * @param startIndex
     *            0-based index of the first item in the resulting data.items.
     * @param itemsPerPage
     *            The number of items in the result. This is not necessarily the
     *            size of the data.items array. I.e. in the last page of items,
     *            the size of data.items may be less than itemsPerPage. However
     *            the size of data.items should not exceed itemsPerPage.
     * @return The JSON-RPC Return message: {@link ResultListQuickSearchItem} or
     *         an {@link JsonRpcMethodError};
     * @throws IOException
     *             When something goes wrong.
     */
    AbstractJsonRpcMethodResponse listUserGroupMembers(String groupName,
            Integer startIndex, Integer itemsPerPage) throws IOException;

    /**
     * Lists groups from the user source sorted by groupName.
     *
     * @return The JSON-RPC Return message: {@link ResultListStrings} or an
     *         {@link JsonRpcMethodError};
     * @throws IOException
     *             When something goes wrong.
     */
    AbstractJsonRpcMethodResponse listUserSourceGroups() throws IOException;

    /**
     * Gets hierarchy nested groups within a group. The list with group names
     * (space) indented according to the nesting level.
     * <p>
     * This method is for diagnostic purposes. Nested groups are only supported
     * by Active Directory, all other user sources return an empty list.
     * </p>
     *
     * @param groupName
     *            The unique parent group name.
     * @return The JSON-RPC Return message: {@link ResultListStrings} or an
     *         {@link JsonRpcMethodError};
     * @throws IOException
     *             When something goes wrong.
     */
    AbstractJsonRpcMethodResponse listUserSourceGroupNesting(String groupName)
            throws IOException;

    /**
     * Lists the members of a user source group sorted by userName.
     *
     * @param groupName
     *            The name of the group.
     * @param nested
     *            If {@code true}, then accumulate members from nested groups
     *            (Active Directory only).
     * @return The JSON-RPC Return message: {@link ResultListStrings} or an
     *         {@link JsonRpcMethodError};
     * @throws IOException
     *             When something goes wrong.
     */
    AbstractJsonRpcMethodResponse listUserSourceGroupMembers(String groupName,
            boolean nested) throws IOException;

    /**
     * Adds a user group from the external user source. All synchronized
     * external users belonging to this group are added as member.
     *
     * @param batchCommitter
     *            The {@link DaoBatchCommitter}.
     * @param groupName
     *            The name of the group to add.
     * @return The JSON-RPC Return message (either a result or an error);
     * @throws IOException
     *             When something goes wrong.
     */
    AbstractJsonRpcMethodResponse addUserGroup(
            DaoBatchCommitter batchCommitter, String groupName)
            throws IOException;

    /**
     * Updates one or more properties for an existing Internal or External User
     * Group.
     *
     * @param dto
     *            The {@link UserGroupPropertiesDto}.
     * @return The JSON-RPC Return message (either a result or an error);
     * @throws IOException
     *             When something went wrong.
     */
    AbstractJsonRpcMethodResponse setUserGroupProperties(
            final UserGroupPropertiesDto dto) throws IOException;

    /**
     * Synchronizes a user group with the external user source. Synchronized
     * external users are added or removed as member.
     *
     * @param batchCommitter
     *            The {@link DaoBatchCommitter}.
     * @param groupName
     *            The name of the group to add.
     * @return The JSON-RPC Return message (either a result or an error);
     * @throws IOException
     *             When something goes wrong.
     */
    AbstractJsonRpcMethodResponse syncUserGroup(
            DaoBatchCommitter batchCommitter, String groupName)
            throws IOException;

    /**
     * Deletes a user group.
     *
     * @param groupName
     *            The name of the group to delete.
     *
     * @return The JSON-RPC Return message (either a result or an error);
     *
     * @throws IOException
     *             When something goes wrong.
     */
    AbstractJsonRpcMethodResponse deleteUserGroup(String groupName)
            throws IOException;

    /**
     * Deletes a user group.
     *
     * @param groupId
     *            The database key of the group to delete.
     *
     * @return The JSON-RPC Return message (either a result or an error);
     *
     * @throws IOException
     *             When something goes wrong.
     */
    AbstractJsonRpcMethodResponse deleteUserGroup(Long groupId)
            throws IOException;

}
