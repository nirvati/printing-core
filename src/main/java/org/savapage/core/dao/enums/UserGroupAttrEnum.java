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
package org.savapage.core.dao.enums;

import java.util.HashMap;
import java.util.Map;

import org.savapage.core.SpException;
import org.savapage.core.jpa.UserGroupAttr;

/**
 * {@link UserGroupAttr} names. See {@link UserGroupAttr#setName(String)}.
 *
 * @author Rijk Ravestein
 *
 */
public enum UserGroupAttrEnum {

    /**
     * A JSON value of {@link Map} with key {@link ACLRoleEnum} and value
     * {@link Boolean}. When a {@link ACLRoleEnum} key is not present the value
     * is indeterminate.
     */
    ACL_ROLES("acl.roles"),

    /**
     * OIDS for Role "User": A JSON value of a {@link Map} with key
     * {@link ACLOidEnum}. Value {@link Integer} is a bitwise OR of
     * {@link ACLPermissionEnum#getPermission()} values that hold the granted
     * access. When a {@link ACLOidEnum} key is not present in the map the
     * access is indeterminate.
     */
    ACL_OIDS_USER("acl.oids.user"),

    /**
     * OIDS for Role "Admin": A JSON value of a {@link Map} with key
     * {@link ACLOidEnum}. Value {@link Integer} is a bitwise OR of
     * {@link ACLPermissionEnum#getPermission()} values that hold the granted
     * access. When a {@link ACLOidEnum} key is not present in the map the
     * access is indeterminate.
     */
    ACL_OIDS_ADMIN("acl.oids.admin");

    /**
     * Lookup {@link UserGroupAttrEnum} by database name.
     */
    private static class Lookup {

        /**
         *
         */
        private final Map<String, UserGroupAttrEnum> enumLookup =
                new HashMap<String, UserGroupAttrEnum>();

        /**
         *
         */
        public Lookup() {
            for (UserGroupAttrEnum value : UserGroupAttrEnum.values()) {
                enumLookup.put(value.name, value);
            }
        }

        /**
         *
         * @param key
         *            The key (name).
         * @return The enum.
         */
        public UserGroupAttrEnum get(final String key) {
            return enumLookup.get(key);
        }
    }

    /**
     * The name used in the database.
     */
    private final String name;

    /**
     * Ensure one-time initialization on class loading.
     */
    private static class LookupHolder {
        public static final Lookup INSTANCE = new Lookup();
    }

    /**
     *
     * @param name
     *            The database name.
     * @return The {@link UserGroupAttrEnum}.
     */
    public static UserGroupAttrEnum asEnum(final String name) {
        return LookupHolder.INSTANCE.get(name);
    }

    /**
     *
     * @param name
     *            The database name.
     */
    private UserGroupAttrEnum(final String name) {
        this.name = name;
    }

    /**
     *
     * @return The database name.
     */
    public final String getName() {
        return this.name;
    }

    /**
     * Gets the enum value based on the {@link ACLOidEnum} role.
     *
     * @param oid
     *            The {@link ACLOidEnum}
     * @return The enum value.
     */
    public static UserGroupAttrEnum valueOf(final ACLOidEnum oid) {
        if (oid.isUserRole()) {
            return ACL_OIDS_USER;
        } else if (oid.isAdminRole()) {
            return ACL_OIDS_ADMIN;
        }
        throw new SpException(
                String.format("No role found for %s.", oid.toString()));
    }

}
