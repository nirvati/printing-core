/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2017 Datraverse B.V.
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
package org.savapage.core.json.rpc;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Rijk Ravestein
 *
 */
public enum JsonRpcMethodName {

    /**  */
    ADD_INTERNAL_USER("addInternalUser"),

    /**  */
    ADD_USER_GROUP("addUserGroup"),

    /**  */
    AUTH_USER_SOURCE("authUserSource"),

    /**  */
    CHANGE_BASE_CURRENCY("changeBaseCurrency"),

    /**  */
    DELETE_USER("deleteUser"),

    /**  */
    DELETE_USER_GROUP("deleteUserGroup"),

    /**  */
    ERASE_USER("eraseUser"),

    /**  */
    GET_CONFIG_PROPERTY("getConfigProperty"),

    /**  */
    LIST_USERS("listUsers"),

    /**  */
    LIST_USER_GROUPS("listUserGroups"),

    /**  */
    LIST_USER_GROUP_MEMBERS("listUserGroupMembers"),

    /**  */
    LIST_USER_GROUP_MEMBERSHIPS("listUserGroupMemberships"),

    /**  */
    LIST_USER_SOURCE_GROUPS("listUserSourceGroups"),

    /**  */
    LIST_USER_SOURCE_GROUP_MEMBERS("listUserSourceGroupMembers"),

    /**  */
    LIST_USER_SOURCE_GROUP_NESTING("listUserSourceGroupNesting"),

    /**  */
    PRINTER_ACCESS_CONTROL("printerAccessControl"),

    /**  */
    PRINTER_SNMP("printerSnmp"),

    /**  */
    SET_CONFIG_PROPERTY("setConfigProperty"),

    /**  */
    SET_USER_PROPERTIES("setUserProperties"),

    /**  */
    SET_USER_GROUP_PROPERTIES("setUserGroupProperties"),

    /**  */
    SYNC_USER_GROUP("syncUserGroup"),

    /**  */
    SYNC_USERS_AND_GROUPS("syncUsersAndGroups"),

    /**  */
    SYSTEM_STATUS("systemStatus");

    /**
     * Developer-friendly method name.
     */
    private String methodName;

    /**
     *
     * @author Rijk Ravestein
     *
     */
    private static class Lookup {

        private final Map<String, JsonRpcMethodName> enumLookup =
                new HashMap<>();

        public Lookup() {
            for (JsonRpcMethodName value : JsonRpcMethodName.values()) {
                enumLookup.put(value.methodName, value);
            }
        }

        public JsonRpcMethodName get(final String key) {
            return enumLookup.get(key);
        }
    }

    /**
     * Ensure one-time initialization on class class.
     */
    private static class LookupHolder {
        public static final Lookup INSTANCE = new Lookup();
    }

    public static JsonRpcMethodName asEnum(final String methodName) {
        return LookupHolder.INSTANCE.get(methodName);
    }

    private JsonRpcMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getMethodName() {
        return this.methodName;

    }
}
