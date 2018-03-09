/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2018 Datraverse B.V.
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
package org.savapage.core.services;

import java.util.EnumSet;
import java.util.List;

import org.savapage.core.config.ConfigManager;
import org.savapage.core.dao.enums.ACLOidEnum;
import org.savapage.core.dao.enums.ACLPermissionEnum;
import org.savapage.core.dao.enums.ACLRoleEnum;
import org.savapage.core.jpa.User;
import org.savapage.core.jpa.UserGroup;
import org.savapage.core.jpa.UserGroupMember;

/**
 *
 * @author Rijk Ravestein
 *
 */
public interface AccessControlService {

    /**
     * Checks if {@link User} has access to a Role. First {@link ConfigManager}
     * is consulted if role is enabled, then the
     * {@link #isAuthorized(User, ACLRoleEnum)} method is used to check user
     * authorization.
     *
     * @param user
     *            The {@link User}.
     * @param role
     *            The {@link ACLRoleEnum};
     * @return {@code true} when user has access to role.
     */
    boolean hasAccess(User user, ACLRoleEnum role);

    /**
     * Checks if {@link User} is authorized for a Role. Checks are done
     * bottom-up, starting at the {@link User} and moving up to the
     * {@link UserGroup} objects where user is {@link UserGroupMember} of. The
     * first encountered object with a defined {@link ACLRoleEnum} is used for
     * the check. When no reference object is found, the user is not authorized.
     *
     * @param user
     *            The {@link User}.
     * @param role
     *            The {@link ACLRoleEnum};
     * @return {@code true} when authorized for role.
     */
    boolean isAuthorized(User user, ACLRoleEnum role);

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
    Boolean isGroupAuthorized(UserGroup group, ACLRoleEnum role);

    /**
     * Checks if {@link User} has access to an OID. Checks are done bottom-up,
     * starting at the {@link User} and moving up to the {@link UserGroup}
     * objects where user is {@link UserGroupMember} of. The first encountered
     * object with a defined {@link ACLOidEnum} is used for the check. When no
     * reference object is found, the user <b>has</b> access.
     * <p>
     * NOTE: User has access when {@link ConfigManager#isInternalAdmin(String)}
     * and {@link ACLOidEnum#isAdminRole()}.
     * </p>
     *
     * @param user
     *            The {@link User}.
     * @param oid
     *            The OID.
     * @return {@code true} when access.
     */
    boolean hasAccess(User user, ACLOidEnum oid);

    /**
     * Checks if {@link User} has permission for an OID. Checks are done
     * bottom-up, starting at the {@link User} and moving up to the
     * {@link UserGroup} objects where user is {@link UserGroupMember} of. The
     * first encountered object with a defined {@link ACLOidEnum} is used for
     * the check. When no reference object is found, the user <b>is</b>
     * authorized.
     * <p>
     * NOTE: User has permission when
     * {@link ConfigManager#isInternalAdmin(String)} and
     * {@link ACLOidEnum#isAdminRole()}.
     * </p>
     *
     * @param user
     *            The {@link User}.
     * @param oid
     *            The OID.
     * @param perm
     *            The requested permission.
     * @return {@code true} when permitted.
     */
    boolean hasPermission(User user, ACLOidEnum oid, ACLPermissionEnum perm);

    /**
     * Checks if requested permission is part of permission list.
     *
     * @param perms
     *            The permissions.
     * @param permRequested
     *            The requested permission.
     * @return {@code true} when permitted.
     */
    boolean hasPermission(List<ACLPermissionEnum> perms,
            ACLPermissionEnum permRequested);

    /**
     * Gets the OID permissions for an OID a User.
     * <p>
     * NOTE: {@code null} is returned when
     * {@link ConfigManager#isInternalAdmin(String)} and
     * {@link ACLOidEnum#isAdminRole()}.
     * </p>
     *
     * @param user
     *            The {@link User}.
     * @param oid
     *            The OID.
     * @return {@code null} when undetermined.
     */
    List<ACLPermissionEnum> getPermission(User user, ACLOidEnum oid);

    /**
     * Gets the OID privileges bitmask for an OID a User.
     * <p>
     * NOTE: {@code null} is returned when
     * {@link ConfigManager#isInternalAdmin(String)} and
     * {@link ACLOidEnum#isAdminRole()}.
     * </p>
     *
     * @param user
     *            The {@link User}.
     * @param oid
     *            The OID.
     * @return {@code null} when undetermined.
     */
    Integer getPrivileges(User user, ACLOidEnum oid);

    /**
     * @return The {@link ACLRoleEnum} values that are granted access when
     *         indeterminate at "All User" top level.
     */
    EnumSet<ACLRoleEnum> getTopIndeterminateGranted();

}
