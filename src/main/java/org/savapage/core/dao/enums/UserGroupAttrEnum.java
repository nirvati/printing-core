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
package org.savapage.core.dao.enums;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import org.savapage.core.jpa.UserGroupAttr;

/**
 * {@link UserGroupAttr} names. See {@link UserGroupAttr#setName(String)}.
 *
 * @author Rijk Ravestein
 *
 */
public enum UserGroupAttrEnum {

    /**
     * A JSON value of {@link EnumSet} of {@link ACLRoleEnum}.
     */
    ACL_ROLES("acl.roles");

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
}
