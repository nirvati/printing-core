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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.savapage.core.util.LocaleHelper;

/**
 * Object Identity Objects (OID). A <i>object identity</i> is domain object
 * identifier to define access for.
 * <p>
 * Enum values prefixed with {@code U_} are OIDs for role "User". Values
 * prefixed with {@code A_} are OIDs for role "Admin".
 * </p>
 *
 * @author Rijk Ravestein
 *
 */
public enum ACLOidEnum {

    /**
     * Details of authenticated user.
     */
    U_USER(EnumSet.of(ACLPermissionEnum.READER)),

    /**
     * The user inbox (SafePages).
     */
    U_INBOX(EnumSet.of(ACLPermissionEnum.READER, ACLPermissionEnum.EDITOR),
            EnumSet.of(ACLPermissionEnum.DOWNLOAD, ACLPermissionEnum.SEND)),

    /**
     * Letterhead.
     */
    U_LETTERHEAD(EnumSet.of(ACLPermissionEnum.READER, ACLPermissionEnum.EDITOR));

    /**
     *
     */
    private static final EnumSet<ACLPermissionEnum> PERMS_NONE =
            EnumSet.noneOf(ACLPermissionEnum.class);

    /**
     * The role permissions that can be selected to grant access for.
     */
    private final EnumSet<ACLPermissionEnum> permissionRoles;

    /**
     * The object permission that can be selected by role
     * {@link ACLPermissionEnum#READER}.
     */
    private final EnumSet<ACLPermissionEnum> readerPermissions;

    /**
     * The object permission that can be selected by role
     * {@link ACLPermissionEnum#EDITOR}.
     */
    private final EnumSet<ACLPermissionEnum> editorPermissions;

    /**
     * @param permRoles
     *            The role permissions that can be selected to grant access for.
     */
    ACLOidEnum(final EnumSet<ACLPermissionEnum> permRoles) {
        this.permissionRoles = permRoles;
        this.readerPermissions = null;
        this.editorPermissions = null;
    }

    /**
     *
     * @param permRoles
     *            The role permissions that can be selected to grant access for.
     * @param permsReader
     *            The object permission that can be selected by role
     *            {@link ACLPermissionEnum#READER}.
     */
    ACLOidEnum(final EnumSet<ACLPermissionEnum> permRoles,
            final EnumSet<ACLPermissionEnum> permsReader) {
        this.permissionRoles = permRoles;
        this.readerPermissions = permsReader;
        this.editorPermissions = null;
    }

    /**
     *
     * @param permRoles
     *            The role permissions that can be selected to grant access for.
     * @param permsReader
     *            The object permission that can be selected by role
     *            {@link ACLPermissionEnum#READER}.
     * @param permsEditor
     *            The object permission that can be selected by role
     *            {@link ACLPermissionEnum#EDITOR}.
     */
    ACLOidEnum(final EnumSet<ACLPermissionEnum> permRoles,
            final EnumSet<ACLPermissionEnum> permsReader,
            final EnumSet<ACLPermissionEnum> permsEditor) {
        this.permissionRoles = permRoles;
        this.readerPermissions = permsReader;
        this.editorPermissions = permsEditor;
    }

    /**
     * @param locale
     *            The {@link Locale}.
     * @return The localized text.
     */
    public String uiText(final Locale locale) {
        return LocaleHelper.uiText(this, locale);
    }

    /**
     * @return The object permission that can be selected by role
     *         {@link ACLPermissionEnum#READER}.
     */
    public EnumSet<ACLPermissionEnum> getReaderPermissions() {
        if (readerPermissions == null) {
            return PERMS_NONE;
        }
        return readerPermissions;
    }

    /**
     * @return The object permission that can be selected by role
     *         {@link ACLPermissionEnum#EDITOR}.
     */
    public EnumSet<ACLPermissionEnum> getEditorPermissions() {
        if (editorPermissions == null) {
            return PERMS_NONE;
        }
        return editorPermissions;
    }

    /**
     * @return The role permissions that can be selected to grant access for.
     */
    public EnumSet<ACLPermissionEnum> getPermissionRoles() {
        if (permissionRoles == null) {
            return PERMS_NONE;
        }
        return permissionRoles;
    }

    /**
     *
     * @return
     */
    public static EnumSet<ACLOidEnum> getUserOids() {
        return EnumSet.allOf(ACLOidEnum.class);
    }

    /**
     *
     * @return
     */
    public static EnumSet<ACLOidEnum> getAdminOids() {
        return EnumSet.noneOf(ACLOidEnum.class);
    }

    /**
     *
     * @param mapIn
     * @return
     */
    public static Map<ACLOidEnum, ACLPermissionEnum>
            asMapRole(final Map<ACLOidEnum, Integer> mapIn) {

        final Map<ACLOidEnum, ACLPermissionEnum> mapOut = new HashMap<>();

        for (final Entry<ACLOidEnum, Integer> entry : mapIn.entrySet()) {
            mapOut.put(entry.getKey(),
                    ACLPermissionEnum.asRole(entry.getValue().intValue()));
        }
        return mapOut;
    }

    /**
     *
     * @param mapIn
     * @return
     */
    public static Map<ACLOidEnum, List<ACLPermissionEnum>>
            asMapPerms(final Map<ACLOidEnum, Integer> mapIn) {

        final Map<ACLOidEnum, List<ACLPermissionEnum>> mapOut = new HashMap<>();

        for (final Entry<ACLOidEnum, Integer> entry : mapIn.entrySet()) {
            mapOut.put(entry.getKey(),
                    ACLPermissionEnum.asList(entry.getValue().intValue()));
        }
        return mapOut;
    }

    /**
     *
     * @param mapIn
     * @return
     */
    public static Map<ACLOidEnum, List<ACLPermissionEnum>>
            asMapPermsReader(final Map<ACLOidEnum, Integer> mapIn) {

        final Map<ACLOidEnum, List<ACLPermissionEnum>> mapOut = new HashMap<>();

        for (final Entry<ACLOidEnum, Integer> entry : mapIn.entrySet()) {
            mapOut.put(entry.getKey(), ACLPermissionEnum
                    .asPermsReader(entry.getValue().intValue()));
        }
        return mapOut;
    }

    /**
     *
     * @param mapIn
     * @return
     */
    public static Map<ACLOidEnum, List<ACLPermissionEnum>>
            asMapPermsEditor(final Map<ACLOidEnum, Integer> mapIn) {

        final Map<ACLOidEnum, List<ACLPermissionEnum>> mapOut = new HashMap<>();

        for (final Entry<ACLOidEnum, Integer> entry : mapIn.entrySet()) {
            mapOut.put(entry.getKey(), ACLPermissionEnum
                    .asPermsEditor(entry.getValue().intValue()));
        }
        return mapOut;
    }

    /**
     *
     * @param mapIn
     * @return
     */
    public static Map<ACLOidEnum, Integer> asMapPrivilege(
            final Map<ACLOidEnum, List<ACLPermissionEnum>> mapIn) {

        final Map<ACLOidEnum, Integer> mapOut = new HashMap<>();

        for (final Entry<ACLOidEnum, List<ACLPermissionEnum>> entry : mapIn
                .entrySet()) {
            mapOut.put(entry.getKey(),
                    ACLPermissionEnum.asPrivilege(entry.getValue()));
        }
        return mapOut;
    }

}
