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
package org.savapage.core.services.impl;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.savapage.core.dao.UserGroupMemberDao;
import org.savapage.core.dao.enums.ACLRoleEnum;
import org.savapage.core.dao.enums.UserAttrEnum;
import org.savapage.core.dao.enums.UserGroupAttrEnum;
import org.savapage.core.jpa.User;
import org.savapage.core.jpa.UserAttr;
import org.savapage.core.jpa.UserGroup;
import org.savapage.core.jpa.UserGroupAttr;
import org.savapage.core.services.AccessControlService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.util.JsonHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class AccessControlServiceImpl extends AbstractService implements
        AccessControlService {

    /**
     * The logger.
     */
    private static final Logger LOGGER = LoggerFactory
            .getLogger(AccessControlServiceImpl.class);

    /**
     * Checks if role is enabled in JSON String.
     *
     * @param json
     *            The JSON string.
     * @param role
     *            The {@link ACLRoleEnum};
     * @return {@code null} when undetermined.
     * @throws IOException
     *             When JSON string is invalid.
     */
    private static Boolean isRoleEnabledInJson(final String json,
            final ACLRoleEnum role) throws IOException {

        final Map<ACLRoleEnum, Boolean> map =
                JsonHelper.createEnumBooleanMap(ACLRoleEnum.class, json);

        final String key = role.toString();
        final Boolean value;

        if (map.containsKey(key)) {
            value = map.get(key);
        } else {
            value = null;
        }
        return value;
    }

    /**
     * Checks if User is authorized for a Role.
     *
     * @param user
     *            The {@link User}.
     * @param role
     *            The {@link ACLRoleEnum};
     * @return {@code true} when authorized.
     */
    private static boolean isUserAuthorized(final User user,
            final ACLRoleEnum role) {

        final UserAttr userAttr =
                userAttrDAO().findByName(user, UserAttrEnum.ACL_ROLES);

        if (userAttr != null) {
            try {
                final Boolean value =
                        isRoleEnabledInJson(userAttr.getValue(), role);
                if (value != null) {
                    return value.booleanValue();
                }
            } catch (IOException e) {
                // Try to remove the culprit.
                if (ServiceContext.getDaoContext().isTransactionActive()) {
                    userAttrDAO().delete(userAttr);
                    LOGGER.warn(String.format(
                            "%s [%s] has invalid value: %s (the object "
                                    + "is deleted from the database)",
                            UserAttr.class.getSimpleName(), userAttr.getName(),
                            userAttr.getValue()));
                }
            }
        }
        return false;
    }

    /**
     * Checks if UserGroup is authorized for a Role.
     *
     * @param group
     *            The {@link UserGroup}.
     * @param role
     *            The {@link ACLRoleEnum};
     * @return {@code true} when authorized.
     */
    private static boolean isGroupAuthorized(final UserGroup group,
            final ACLRoleEnum role) {

        final UserGroupAttr groupAttr =
                userGroupAttrDAO().findByName(group,
                        UserGroupAttrEnum.ACL_ROLES);

        if (groupAttr != null) {
            try {
                final Boolean value =
                        isRoleEnabledInJson(groupAttr.getValue(), role);
                if (value != null) {
                    return value.booleanValue();
                }

            } catch (IOException e) {
                // Try to remove the culprit.
                if (ServiceContext.getDaoContext().isTransactionActive()) {
                    userGroupAttrDAO().delete(groupAttr);
                    LOGGER.warn(String.format(
                            "%s [%s] has invalid value: %s (the object "
                                    + "is deleted from the database)",
                            UserGroupAttr.class.getSimpleName(),
                            groupAttr.getName(), groupAttr.getValue()));
                }
            }
        }
        return false;
    }

    @Override
    public boolean isAuthorized(final User user, final ACLRoleEnum role) {

        if (isUserAuthorized(user, role)) {
            return true;
        }

        /*
         * Check Group Memberships (explicit).
         */
        final UserGroupMemberDao.UserFilter filter =
                new UserGroupMemberDao.UserFilter();

        filter.setUserId(user.getId());

        final List<UserGroup> groupList =
                userGroupMemberDAO().getGroupChunk(filter, null, null,
                        UserGroupMemberDao.GroupField.GROUP_NAME, true);

        for (final UserGroup group : groupList) {
            if (isGroupAuthorized(group, role)) {
                return true;
            }
        }

        /*
         * Check Group Memberships (implicit).
         */
        final UserGroup group;

        if (user.getInternal().booleanValue()) {
            group = userGroupService().getInternalUserGroup();
        } else {
            group = userGroupService().getExternalUserGroup();
        }

        if (isGroupAuthorized(group, role)) {
            return true;
        }

        // All Users
        if (isGroupAuthorized(userGroupService().getAllUserGroup(), role)) {
            return true;
        }

        return false;
    }
}
