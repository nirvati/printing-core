/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.core.services.impl;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.BooleanUtils;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.dao.UserGroupMemberDao;
import org.savapage.core.dao.enums.ACLOidEnum;
import org.savapage.core.dao.enums.ACLPermissionEnum;
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
public final class AccessControlServiceImpl extends AbstractService
        implements AccessControlService {

    /**
     * The logger.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(AccessControlServiceImpl.class);

    /**
     * Checks if role is enabled in JSON String.
     *
     * @param json
     *            The JSON string.
     * @param role
     *            The {@link ACLRoleEnum};
     * @return {@code true} when authorized, {@code false} when not,
     *         {@code null} when undetermined.
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
     * @return {@code true} when authorized, {@code false} when not,
     *         {@code null} when undetermined.
     */
    private static Boolean isUserAuthorized(final User user,
            final ACLRoleEnum role) {

        final UserAttr userAttr =
                userAttrDAO().findByName(user, UserAttrEnum.ACL_ROLES);

        if (userAttr != null) {
            try {
                return isRoleEnabledInJson(userAttr.getValue(), role);
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
        return null;
    }

    /**
     * Checks if UserGroup is authorized for a Role.
     *
     * @param group
     *            The {@link UserGroup}.
     * @param role
     *            The {@link ACLRoleEnum};
     * @return {@code true} when authorized, {@code false} when not,
     *         {@code null} when undetermined.
     */
    private static Boolean isGroupAuthorized(final UserGroup group,
            final ACLRoleEnum role) {

        final UserGroupAttr groupAttr = userGroupAttrDAO().findByName(group,
                UserGroupAttrEnum.ACL_ROLES);

        if (groupAttr != null) {
            try {
                return isRoleEnabledInJson(groupAttr.getValue(), role);
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
        return null;
    }

    @Override
    public boolean isAuthorized(final User user, final ACLRoleEnum role) {

        final Boolean isUserAuth = isUserAuthorized(user, role);

        if (isUserAuth != null) {
            return isUserAuth.booleanValue();
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
            final Boolean isGroupAuth = isGroupAuthorized(group, role);
            if (isGroupAuth != null) {
                return isGroupAuth.booleanValue();
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

        final Boolean isGroupAuth = isGroupAuthorized(group, role);
        if (isGroupAuth != null) {
            return isGroupAuth.booleanValue();
        }

        // All Users
        if (BooleanUtils.isTrue(isGroupAuthorized(
                userGroupService().getAllUserGroup(), role))) {
            return true;
        }

        return false;
    }

    @Override
    public boolean hasAccess(final User user, final ACLRoleEnum role) {

        if (role == ACLRoleEnum.PRINT_DELEGATE && !ConfigManager.instance()
                .isConfigValue(Key.PROXY_PRINT_DELEGATE_ENABLE)) {
            return false;
        }

        return isAuthorized(user, role);
    }

    /**
     * Gets the OID privileges from a JSON String.
     *
     * @param json
     *            The JSON string.
     * @param oid
     *            The OID;
     * @return {@code null} when undetermined.
     * @throws IOException
     *             When JSON string is invalid.
     */
    private static Integer getOidPrivilegesFromJson(final String json,
            final ACLOidEnum oid) throws IOException {

        final Map<ACLOidEnum, Integer> map =
                JsonHelper.createEnumIntegerMap(ACLOidEnum.class, json);

        return map.get(oid);
    }

    /**
     * Get the User permissions for an OID.
     *
     * @param user
     *            The user.
     * @param attrEnum
     *            The attribute to read.
     * @param oid
     *            The OID
     * @return {@code null} when undetermined.
     */
    private static Integer getUserPrivileges(final User user,
            final UserAttrEnum attrEnum, final ACLOidEnum oid) {

        final UserAttr userAttr = userAttrDAO().findByName(user, attrEnum);

        if (userAttr != null) {
            try {
                return getOidPrivilegesFromJson(userAttr.getValue(), oid);
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
        return null;
    }

    /**
     * Get the UserGroup privileges for an OID.
     *
     * @param group
     *            The {@link UserGroup}.
     * @param attrEnum
     *            The attribute to read.
     * @param oid
     *            The OID
     * @return {@code null} when undetermined.
     */
    private Integer getGroupPrivileges(final UserGroup group,
            final UserGroupAttrEnum attrEnum, final ACLOidEnum oid) {

        final UserGroupAttr groupAttr =
                userGroupAttrDAO().findByName(group, attrEnum);

        if (groupAttr != null) {
            try {
                return getOidPrivilegesFromJson(groupAttr.getValue(), oid);
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
        return null;
    }

    @Override
    public boolean hasUserPermission(final User user, final ACLOidEnum oid,
            final ACLPermissionEnum perm) {

        final Integer privileges = getUserPrivileges(user, oid);

        if (privileges == null) {
            return true;
        }

        return perm.isPresent(privileges);
    }

    @Override
    public boolean hasUserAccess(final User user, final ACLOidEnum oid) {

        final Integer privileges = getUserPrivileges(user, oid);

        if (privileges == null) {
            return true;
        }

        return privileges.intValue() != 0;
    }

    @Override
    public boolean hasUserPermission(final List<ACLPermissionEnum> perms,
            final ACLPermissionEnum permRequested) {

        for (final ACLPermissionEnum perm : perms) {
            if (perm == permRequested) {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<ACLPermissionEnum> getUserPermission(final User user,
            final ACLOidEnum oid) {

        final Integer userPrivileges = getUserPrivileges(user, oid);

        if (userPrivileges == null) {
            return null;
        }
        return ACLPermissionEnum.asList(userPrivileges.intValue());
    }

    @Override
    public Integer getUserPrivileges(final User user, final ACLOidEnum oid) {

        Integer userPrivileges =
                getUserPrivileges(user, UserAttrEnum.ACL_OIDS_USER, oid);

        if (userPrivileges != null) {
            return userPrivileges;
        }

        final UserGroupAttrEnum groupAttrEnum = UserGroupAttrEnum.ACL_OIDS_USER;

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
            userPrivileges = getGroupPrivileges(group, groupAttrEnum, oid);
            if (userPrivileges != null) {
                return userPrivileges;
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

        userPrivileges = getGroupPrivileges(group, groupAttrEnum, oid);
        if (userPrivileges != null) {
            return userPrivileges;
        }

        // All Users
        return getGroupPrivileges(userGroupService().getAllUserGroup(),
                groupAttrEnum, oid);
    }

}
