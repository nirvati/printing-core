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
package org.savapage.core.users;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.savapage.core.SpException;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.jpa.User;
import org.savapage.core.json.JsonAbstractBase;
import org.savapage.core.rfid.RfidNumberFormat;
import org.savapage.core.system.CommandExecutor;
import org.savapage.core.system.ICommandExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Datraverse B.V.
 *
 */
public final class UnixUserSource extends AbstractUserSource
        implements IUserSource, IExternalUserAuthenticator {

    /**
     *
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(UnixUserSource.class);

    private static String STDOUT_TRUE = "Y";

    /**
     *
     */
    private static class PamExecResponse extends JsonAbstractBase {

        private boolean valid;
        private String error;

        public static PamExecResponse create(final String json) {
            return create(PamExecResponse.class, json);
        }

        public boolean isValid() {
            return valid;
        }

        public void setValid(boolean valid) {
            this.valid = valid;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }

    }

    /**
     *
     * @return
     */
    private static String getModulePamPath() {
        return ConfigManager.getServerBinHome() + "/savapage-pam";
    }

    /**
     *
     * @return
     */
    private static String getModuleNssPath() {
        return ConfigManager.getServerBinHome() + "/savapage-nss";
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

        User user = null;

        /*
         * Note: the input on stdin is passed as second argument.
         */
        final ICommandExecutor exec = CommandExecutor.create(getModulePamPath(),
                uid + "\n" + password);

        try {
            if (exec.executeCommand() != 0) {
                LOGGER.error(exec.getStandardErrorFromCommand().toString());
                throw new SpException(
                        "user [" + uid + "] could not be validated.");
            }

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(exec.getStandardOutputFromCommand().toString());
            }

            final PamExecResponse jsonResponse = PamExecResponse
                    .create(exec.getStandardOutputFromCommand().toString());

            if (jsonResponse.isValid()) {
                user = new User();
                user.setUserId(uid);
            }

        } catch (Exception e) {
            throw new SpException(e);
        }

        return user;
    }

    @Override
    public SortedSet<String> getGroups() {

        final SortedSet<String> sset =
                new TreeSet<>(new IgnoreCaseComparator());

        final ICommandExecutor exec =
                CommandExecutor.create(getModuleNssPath() + " --user-groups");

        try {
            if (exec.executeCommand() != 0) {
                LOGGER.error(exec.getStandardErrorFromCommand().toString());
                throw new SpException("groups could not be retrieved");
            }

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(exec.getStandardOutputFromCommand().toString());
            }

            final StringTokenizer tokenizer = new StringTokenizer(
                    exec.getStandardOutputFromCommand().toString());

            while (tokenizer.hasMoreTokens()) {
                sset.add(tokenizer.nextToken());
            }

        } catch (Exception e) {
            throw new SpException(e);
        }

        return sset;
    }

    @Override
    public SortedSet<CommonUser> getUsers() {
        return getUsers(null);
    }

    @Override
    public SortedSet<CommonUser> getUsersInGroup(final String group) {
        return getUsersInGroup(group, false);
    }

    @Override
    public SortedSet<CommonUser> getUsersInGroup(final String groupName,
            final boolean nested) {
        return getUsers(groupName);
    }

    /**
     * Gets all users or users from a group.
     *
     * @param group
     *            The name of the group, or {@code null} if all users are to be
     *            retrieved.
     * @return The sorted {@link CommonUser} instances.
     */
    private SortedSet<CommonUser> getUsers(final String group) {

        final SortedSet<CommonUser> sset =
                new TreeSet<>(new CommonUserComparator());

        final String args;

        if (group == null) {
            args = " --users";
        } else {
            args = " --user-group-members " + group;
        }

        final ICommandExecutor exec =
                CommandExecutor.create(getModuleNssPath() + args);

        try {
            if (exec.executeCommand() == 0) {

                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace(
                            exec.getStandardOutputFromCommand().toString());
                }

                final StringTokenizer lineTokenizer = new StringTokenizer(
                        exec.getStandardOutputFromCommand().toString(), "\n");

                while (lineTokenizer.hasMoreTokens()) {
                    sset.add(parseUserDetails(lineTokenizer.nextToken()));
                }

            } else {

                if (group == null) {
                    LOGGER.error(exec.getStandardErrorFromCommand().toString());
                    throw new SpException("Users could not be retrieved");
                }

                /*
                 * This can happen when we changed from LDAP to UNIX: the LDAP
                 * user group will not be defined in UNIX.
                 *
                 * See Mantis #387.
                 */
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("Users of UNIX group [" + group
                            + "] could not be retrieved : "
                            + exec.getStandardErrorFromCommand().toString());
                }

            }

        } catch (Exception e) {
            throw new SpException(e);
        }

        return sset;
    }

    /**
     *
     * @param line
     * @return
     */
    private CommonUser parseUserDetails(final String line) {

        final CommonUser user = new CommonUser();

        final StringTokenizer lineTokenizer = new StringTokenizer(line, "\t");

        int iWord = 0;

        while (lineTokenizer.hasMoreTokens()) {

            final String word = lineTokenizer.nextToken();

            if (0 == iWord) {
                user.setUserName(asDbUserId(word));
                user.setExternalUserName(word);
                user.setFullName(word);
            } else if (1 == iWord) {
                StringTokenizer wordTokenizer = new StringTokenizer(word, ",");
                if (wordTokenizer.hasMoreTokens()) {
                    user.setFullName(wordTokenizer.nextToken());
                }
            }
            iWord++;
        }

        user.setEmail(null); // cannot be parsed

        return user;
    }

    @Override
    public boolean isGroupPresent(final String groupName) {

        final String args = " --is-user-group " + groupName;

        final ICommandExecutor exec =
                CommandExecutor.create(getModuleNssPath() + args);

        try {
            if (exec.executeCommand() != 0) {
                LOGGER.error(exec.getStandardErrorFromCommand().toString());
                throw new SpException(args);
            }

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(exec.getStandardOutputFromCommand().toString());
            }

            return exec.getStandardOutputFromCommand().toString()
                    .startsWith(STDOUT_TRUE);

        } catch (Exception e) {
            throw new SpException(e);
        }
    }

    @Override
    public boolean isUserInGroup(final String uid, final String group) {

        final String args = " --is-user-group-member " + group + " " + uid;

        final ICommandExecutor exec =
                CommandExecutor.create(getModuleNssPath() + args);

        try {

            if (exec.executeCommand() != 0) {

                LOGGER.error(exec.getStandardErrorFromCommand().toString());

                final String msg = "user [" + uid + "] of group [" + group
                        + "] could not be retrieved";

                throw new SpException(msg);
            }

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(exec.getStandardOutputFromCommand().toString());
            }

            return exec.getStandardOutputFromCommand().toString()
                    .startsWith(STDOUT_TRUE);

        } catch (Exception e) {
            throw new SpException(e);
        }
    }

    @Override
    public CommonUser getUser(final String uid) {

        final String args = " --user-details " + uid;

        final ICommandExecutor exec =
                CommandExecutor.create(getModuleNssPath() + args);

        try {

            if (exec.executeCommand() != 0) {

                LOGGER.error(exec.getStandardErrorFromCommand().toString());

                final String msg =
                        "details of user [" + uid + "] could not be retrieved";
                throw new SpException(msg);
            }

            final String line = exec.getStandardOutputFromCommand().toString();

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(line);
            }

            return parseUserDetails(line);

        } catch (Exception e) {
            throw new SpException(e);
        }
    }

    @Override
    public RfidNumberFormat createRfidNumberFormat() {
        return null;
    }

    @Override
    public boolean isIdNumberProvided() {
        return false;
    }

    @Override
    public boolean isCardNumberProvided() {
        return false;
    }

    @Override
    public boolean isEmailProvided() {
        return false;
    }

    @Override
    public List<String> getGroupHierarchy(final String parentGroup,
            final boolean indent) {
        final List<String> list = new ArrayList<>();
        return list;
    }

}
