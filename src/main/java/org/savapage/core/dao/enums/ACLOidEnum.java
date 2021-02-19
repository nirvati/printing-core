/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2020 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: © 2020 Datraverse B.V. <info@datraverse.com>
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
package org.savapage.core.dao.enums;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.savapage.core.i18n.NounEnum;
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

    /** */
    A_ABOUT(EnumSet.of(ACLPermissionEnum.READER, ACLPermissionEnum.EDITOR)),

    /** */
    A_ACCOUNTS(EnumSet.of(ACLPermissionEnum.READER, ACLPermissionEnum.EDITOR)),

    /** */
    A_CONFIG_EDITOR(EnumSet.of(ACLPermissionEnum.READER, //
            ACLPermissionEnum.EDITOR)),

    /** */
    A_DASHBOARD(EnumSet.of(ACLPermissionEnum.READER, ACLPermissionEnum.EDITOR)),

    /** */
    A_DEVICES(EnumSet.of(ACLPermissionEnum.READER, ACLPermissionEnum.EDITOR)),

    /** */
    A_REPORTS(EnumSet.of(ACLPermissionEnum.READER)),

    /** */
    A_DOCUMENTS(EnumSet.of(ACLPermissionEnum.READER)),

    /** */
    A_LOG(EnumSet.of(ACLPermissionEnum.READER)),

    /** */
    A_OPTIONS(EnumSet.of(ACLPermissionEnum.READER, ACLPermissionEnum.EDITOR)),

    /** */
    A_PRINTERS(EnumSet.of(ACLPermissionEnum.READER, ACLPermissionEnum.EDITOR)),

    /** */
    A_QUEUES(EnumSet.of(ACLPermissionEnum.READER, ACLPermissionEnum.EDITOR)),

    /** */
    A_TRANSACTIONS(EnumSet.of(ACLPermissionEnum.READER)),

    /** */
    A_USERS(EnumSet.of(ACLPermissionEnum.READER, ACLPermissionEnum.EDITOR)),

    /** */
    A_USER_GROUPS(EnumSet.of(ACLPermissionEnum.READER, //
            ACLPermissionEnum.EDITOR)),

    /** */
    A_VOUCHERS(EnumSet.of(ACLPermissionEnum.READER, ACLPermissionEnum.EDITOR)),

    /**
     * Details of authenticated user.
     */
    U_USER(EnumSet.of(ACLPermissionEnum.READER)),

    /**
     * The user inbox (SafePages).
     */
    U_INBOX(EnumSet.of(ACLPermissionEnum.READER, ACLPermissionEnum.EDITOR), //
            EnumSet.of(ACLPermissionEnum.JOURNAL, ACLPermissionEnum.DOWNLOAD,
                    ACLPermissionEnum.SEND, ACLPermissionEnum.SIGN)),

    /**
     * Financial.
     */
    U_FINANCIAL(EnumSet.of(ACLPermissionEnum.READER, ACLPermissionEnum.EDITOR)),

    /**
     * Letterhead.
     */
    U_LETTERHEAD(EnumSet.of(ACLPermissionEnum.READER, //
            ACLPermissionEnum.EDITOR)),

    /**
     * Can personal account be used for printing.
     */
    U_PERSONAL_PRINT(EnumSet.of(ACLPermissionEnum.READER)),

    /**
     * PrintOut Archive.
     */
    U_PRINT_ARCHIVE(EnumSet.of(ACLPermissionEnum.EDITOR),
            EnumSet.of(ACLPermissionEnum.SELECT)),

    /**
     * PrintOut Journal.
     */
    U_PRINT_JOURNAL(EnumSet.of(ACLPermissionEnum.READER));

    /**
     * OIDs for user role. The enum order is the top to bottom order in the UI.
     */
    private static final ACLOidEnum[] USER_ENUMS_ARRAY = new ACLOidEnum[] {
            U_INBOX, U_USER, U_PERSONAL_PRINT, U_PRINT_JOURNAL, U_PRINT_ARCHIVE,
            U_FINANCIAL, U_LETTERHEAD };

    /**
     * OIDs for user role. The enum order is lost.
     */
    private static final EnumSet<ACLOidEnum> USER_ENUMS =
            EnumSet.copyOf(Arrays.asList(USER_ENUMS_ARRAY));

    /**
     * OIDs for administrator role. The enum order is the top to bottom order in
     * the UI.
     */
    private static final ACLOidEnum[] ADMIN_ENUMS_ARRAY = new ACLOidEnum[] {
            A_DASHBOARD, A_USERS, A_USER_GROUPS, A_ACCOUNTS, A_TRANSACTIONS,
            A_QUEUES, A_PRINTERS, A_DEVICES, A_REPORTS, A_OPTIONS, A_DOCUMENTS,
            A_LOG, A_ABOUT, A_VOUCHERS, A_CONFIG_EDITOR };

    /**
     * OIDs for administrator role. The enum order is lost.
     */
    private static final EnumSet<ACLOidEnum> ADMIN_ENUMS =
            EnumSet.copyOf(Arrays.asList(ADMIN_ENUMS_ARRAY));

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
        switch (this) {
        case U_LETTERHEAD:
            return NounEnum.LETTERHEAD.uiText(locale);
        default:
            return LocaleHelper.uiText(this, locale);
        }
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
        return USER_ENUMS;
    }

    /**
     *
     * @return
     */
    public static EnumSet<ACLOidEnum> getAdminOids() {
        return ADMIN_ENUMS;
    }

    /**
     * @return The UI ordered list.
     */
    public static List<ACLOidEnum> getUserOidList() {
        return Arrays.asList(USER_ENUMS_ARRAY);
    }

    /**
     * @return The UI ordered list.
     */
    public static List<ACLOidEnum> getAdminOidList() {
        return Arrays.asList(ADMIN_ENUMS_ARRAY);
    }

    /**
     * @return {@code true} when this is an OID for a User role.
     */
    public boolean isUserRole() {
        return getUserOids().contains(this);
    }

    /**
     * @return {@code true} when this is an OID for an Administrator role.
     */
    public boolean isAdminRole() {
        return getAdminOids().contains(this);
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
     * Converts permissions to privileges.
     *
     * @param mapIn
     *            The permissions (can be {@code null}).
     * @return The privileges, or an empty map when mapIn is {@code null}.
     */
    public static Map<ACLOidEnum, Integer> asMapPrivilege(
            final Map<ACLOidEnum, List<ACLPermissionEnum>> mapIn) {

        final Map<ACLOidEnum, Integer> mapOut = new HashMap<>();

        if (mapIn != null) {
            for (final Entry<ACLOidEnum, List<ACLPermissionEnum>> entry : mapIn
                    .entrySet()) {
                mapOut.put(entry.getKey(),
                        ACLPermissionEnum.asPrivilege(entry.getValue()));
            }
        }
        return mapOut;
    }

}
