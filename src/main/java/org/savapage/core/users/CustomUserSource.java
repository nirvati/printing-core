/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2022 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: © 2022 Datraverse B.V. <info@datraverse.com>
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
package org.savapage.core.users;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeSet;

import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.savapage.core.SpException;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.jpa.User;
import org.savapage.core.rfid.RfidNumberFormat;
import org.savapage.core.system.CommandExecutor;
import org.savapage.core.system.ICommandExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class CustomUserSource extends AbstractUserSource
        implements IUserSource, IExternalUserAuthenticator {

    /** */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(UnixUserSource.class);

    /** */
    private static final String STDOUT_TRUE = "Y";

    /** */
    private static final String STDOUT_OK = "OK";
    /** */
    private static final String STDOUT_ERROR = "ERROR";

    /** */
    private static final String ARG_LIST_USERS = "--list-users";
    /** */
    private static final String ARG_LIST_GROUPS = "--list-groups";
    /** */
    private static final String ARG_LIST_GROUP_MEMBERS = "--list-group-members";

    /** */
    private static final String ARG_LIST_GET_USER_DETAILS =
            "--get-user-details";
    /** */
    private static final String ARG_LIST_GET_GROUP_DETAILS =
            "--get-group-details";
    /** */
    private static final String ARG_IS_USER_IN_GROUP = "--is-user-in-group";

    /** */
    private static final String ARG_IS_GROUP_PRESENT = "--is-group-present";

    /** */
    private static final String ARG_RFID_FORMAT = "--rfid-format";

    /** */
    private static final int IDX_USER_NAME = 0;
    /** */
    private static final int IDX_USER_FULL_NAME = 1;
    /** */
    private static final int IDX_USER_EMAIL = 2;
    /** */
    private static final int IDX_USER_CARD_NUMBER = 3;
    /** */
    private static final int IDX_USER_ID_NUMBER = 4;

    /** */
    private static final int IDX_GROUP_NAME = 0;
    /** */
    private static final int IDX_GROUP_FULL_NAME = 1;

    /** */
    private static final ConfigManager CONFIG_MANAGER =
            ConfigManager.instance();

    /**
     * @return Path of auth program.
     */
    private static String getCustomAuthPath() {
        return String.format("%s/%s", ConfigManager.getServerExtHome(),
                CONFIG_MANAGER.getConfigValue(Key.AUTH_CUSTOM_USER_AUTH));
    }

    /**
     * @return Path of sync program.
     */
    private static String getCustomSyncPath() {
        return String.format("%s/%s", ConfigManager.getServerExtHome(),
                CONFIG_MANAGER.getConfigValue(Key.AUTH_CUSTOM_USER_SYNC));
    }

    @Override
    public String asDbUserId(final String userId) {
        return asDbUserId(userId, false);
    }

    @Override
    public User authenticate(final String uid, final String password) {

        if (StringUtils.isBlank(uid) || StringUtils.isBlank(password)) {
            return null;
        }

        // Note: the input on stdin is passed as second argument.
        final ICommandExecutor exec = CommandExecutor.create(
                getCustomAuthPath(), String.format("%s\n%s\n", uid, password));

        User user = null;

        try {
            if (exec.executeCommand() != 0) {
                LOGGER.error(exec.getStandardError());
                throw new SpException(
                        "User [" + uid + "] could not be validated.");
            }

            final String rsp = exec.getStandardOutput();

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(rsp);
            }

            if (rsp.equals(STDOUT_OK)) {
                user = new User();
                user.setUserId(uid);
            }

        } catch (Exception e) {
            throw new SpException(e);
        }

        return user;
    }

    /**
     * Creates a command executor.
     *
     * @param args
     *            arguments.
     * @return command executor
     * @throws SpException
     *             IF error.
     */
    private static ICommandExecutor executeSyncCommand(final String args) {

        final String cmd = String.format("%s %s", getCustomSyncPath(), args);
        final ICommandExecutor exec = CommandExecutor.create(cmd);

        try {
            if (exec.executeCommand() != 0) {
                LOGGER.error("{}\n{}", cmd, exec.getStandardError());
                throw new SpException(cmd);
            }
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("{}\n{}", cmd, exec.getStandardOutput());
            }
        } catch (Exception e) {
            throw new SpException(e);
        }
        return exec;
    }

    @Override
    public SortedSet<CommonUserGroup> getGroups() {

        final SortedSet<CommonUserGroup> sset =
                new TreeSet<>(new CommonUserGroupComparator());

        final StringTokenizer tokenizer = new StringTokenizer(
                executeSyncCommand(ARG_LIST_GROUPS).getStandardOutput(), "\n");

        while (tokenizer.hasMoreTokens()) {
            sset.add(this.parseUserGroupDetails(tokenizer.nextToken()));
        }
        return sset;
    }

    /**
     *
     * @param line
     *            The stdout line.
     * @return The common user group.
     */
    private CommonUserGroup parseUserGroupDetails(final String line) {

        final StringTokenizer lineTokenizer = new StringTokenizer(line, "\t");

        String name = null;
        String fullName = null;
        int iWord = 0;

        while (lineTokenizer.hasMoreTokens()) {
            final String word = lineTokenizer.nextToken();
            switch (iWord) {
            case IDX_GROUP_NAME:
                name = word;
                break;
            case IDX_GROUP_FULL_NAME:
                fullName = word;
                break;
            default:
                break;
            }
            iWord++;
        }

        if (StringUtils.isBlank(name)) {
            return null;
        }
        if (StringUtils.isBlank(fullName)) {
            return new CommonUserGroup(name);
        }
        return new CommonUserGroup(name, fullName);
    }

    @Override
    public CommonUserGroup getGroup(final String groupName) {
        final StringTokenizer tokenizer = new StringTokenizer(
                executeSyncCommand(String.format("%s \"%s\"",
                        ARG_LIST_GET_GROUP_DETAILS, groupName))
                                .getStandardOutput(),
                "\n");
        while (tokenizer.hasMoreTokens()) {
            return this.parseUserGroupDetails(tokenizer.nextToken());
        }
        return null;
    }

    @Override
    public List<String> getGroupHierarchy(final String parentGroup,
            final boolean formatted) {
        final List<String> list = new ArrayList<>();
        return list;
    }

    @Override
    public boolean isGroupPresent(final String groupName) {
        return executeSyncCommand(
                String.format("%s \"%s\"", ARG_IS_GROUP_PRESENT, groupName))
                        .getStandardOutput().equals(STDOUT_TRUE);
    }

    @Override
    public boolean isUserInGroup(final String uid, final String groupName) {
        return executeSyncCommand(String.format("%s \"%s\" \"%s\"",
                ARG_IS_USER_IN_GROUP, groupName, uid)).getStandardOutput()
                        .equals(STDOUT_TRUE);
    }

    /**
     *
     * @param line
     *            The stdout line.
     * @return The common user.
     */
    private CommonUser parseUserDetails(final String line) {

        final CommonUser user = new CommonUser();

        user.setEnabled(true);

        final StringTokenizer lineTokenizer = new StringTokenizer(line, "\t");

        int iWord = 0;

        while (lineTokenizer.hasMoreTokens()) {

            final String word = lineTokenizer.nextToken();

            switch (iWord) {
            case IDX_USER_NAME:
                user.setUserName(asDbUserId(word));
                user.setExternalUserName(word);
                user.setFullName(word);
                break;
            case IDX_USER_FULL_NAME:
                StringTokenizer wordTokenizer = new StringTokenizer(word, ",");
                if (wordTokenizer.hasMoreTokens()) {
                    user.setFullName(wordTokenizer.nextToken());
                }
                break;
            case IDX_USER_EMAIL:
                if (StringUtils.isNoneBlank(word)) {
                    user.setEmail(word);
                }
                break;
            case IDX_USER_CARD_NUMBER:
                if (StringUtils.isNoneBlank(word)) {
                    user.setCardNumber(word);
                }
                break;
            case IDX_USER_ID_NUMBER:
                if (StringUtils.isNoneBlank(word)) {
                    user.setIdNumber(word);
                }
                break;
            default:
                break;
            }

            iWord++;
        }

        return user;
    }

    @Override
    public SortedSet<CommonUser> getUsers() {

        final SortedSet<CommonUser> sset =
                new TreeSet<>(new CommonUserComparator());

        final StringTokenizer tokenizer = new StringTokenizer(
                executeSyncCommand(ARG_LIST_USERS).getStandardOutput(), "\n");

        while (tokenizer.hasMoreTokens()) {
            sset.add(this.parseUserDetails(tokenizer.nextToken()));
        }

        return sset;
    }

    @Override
    public CommonUser getUser(final String uid) {
        final StringTokenizer tokenizer = new StringTokenizer(
                executeSyncCommand(String.format("%s \"%s\"",
                        ARG_LIST_GET_USER_DETAILS, uid)).getStandardOutput(),
                "\n");
        while (tokenizer.hasMoreTokens()) {
            return this.parseUserDetails(tokenizer.nextToken());
        }
        return null;
    }

    @Override
    public SortedSet<CommonUser> getUsersInGroup(final String groupName) {
        final SortedSet<CommonUser> sset =
                new TreeSet<>(new CommonUserComparator());

        final StringTokenizer tokenizer = new StringTokenizer(
                executeSyncCommand(String.format("%s \"%s\"",
                        ARG_LIST_GROUP_MEMBERS, groupName)).getStandardOutput(),
                "\n");

        while (tokenizer.hasMoreTokens()) {
            sset.add(this.parseUserDetails(tokenizer.nextToken()));
        }
        return sset;
    }

    @Override
    public SortedSet<CommonUser> getUsersInGroup(final String groupName,
            final boolean nested) {
        return this.getUsersInGroup(groupName);
    }

    @Override
    public RfidNumberFormat createRfidNumberFormat() {

        if (!this.isCardNumberProvided()) {
            return null;
        }

        final StringTokenizer tokenizer = new StringTokenizer(
                executeSyncCommand(ARG_RFID_FORMAT).getStandardOutput(), "\n");

        RfidNumberFormat.FirstByte firstByte = null;
        RfidNumberFormat.Format format = null;

        while (tokenizer.hasMoreTokens()) {

            final String line = tokenizer.nextToken();
            final StringTokenizer lineTokenizer =
                    new StringTokenizer(line, "\t");

            int iWord = 0;

            while (lineTokenizer.hasMoreTokens()) {

                final String word = lineTokenizer.nextToken();

                switch (iWord) {
                case 0:
                case 1:
                    if (firstByte == null) {
                        firstByte = EnumUtils.getEnum(
                                RfidNumberFormat.FirstByte.class, word);
                    }
                    if (format == null) {
                        format = EnumUtils
                                .getEnum(RfidNumberFormat.Format.class, word);
                    }
                    break;
                default:
                    break;
                }
                iWord++;
            }
        }

        if (firstByte == null || format == null) {
            return null;
        }
        return new RfidNumberFormat(format, firstByte);
    }

    @Override
    public boolean isIdNumberProvided() {
        return true;
    }

    @Override
    public boolean isCardNumberProvided() {
        return true;
    }

    @Override
    public boolean isEmailProvided() {
        return true;
    }

}
