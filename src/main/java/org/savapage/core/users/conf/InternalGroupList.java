/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2019 Datraverse B.V.
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
package org.savapage.core.users.conf;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.ServerPathEnum;
import org.savapage.core.users.AbstractUserSource.CommonUserComparator;
import org.savapage.core.users.CommonUser;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class InternalGroupList {

    /** */
    private static final String INTERNAL_GROUPS_TXT = "internal-groups.txt";

    /**
     * .
     */
    private static final class GroupReader extends ConfFileReader {

        private Set<String> groups;
        private Map<String, Boolean> groupsMember;
        private String user;

        @Override
        protected void onItem(final String group, final String userId) {

            if (this.user == null) {
                this.groups.add(group);
            } else {
                this.groupsMember.put(group,
                        Boolean.valueOf(userId.equals(this.user)));
            }
        }

        public Set<String> getGroups(final File file) throws IOException {
            this.groups = new HashSet<>();
            this.user = null;
            this.read(file);
            return groups;
        }

        public Map<String, Boolean> getGroups(final File file,
                final String userId) throws IOException {
            this.groupsMember = new HashMap<>();
            this.user = userId;
            this.read(file);
            return groupsMember;
        }
    }

    /**
     * .
     */
    private static final class GroupMemberReader extends ConfFileReader {

        private final String userGroup;

        final SortedSet<CommonUser> users =
                new TreeSet<>(new CommonUserComparator());

        GroupMemberReader(final String groupName) {
            this.userGroup = groupName;
        }

        @Override
        protected void onItem(final String group, final String userId) {
            if (group.equals(this.userGroup)) {
                final CommonUser commonUser = new CommonUser();
                commonUser.setUserName(userId);
                users.add(commonUser);
            }
        }

        public SortedSet<CommonUser> getMembers(final File file)
                throws IOException {
            this.read(file);
            return users;
        }
    }

    /**
     *
     */
    public InternalGroupList() {
    }

    private static File getFile() {
        return Paths
                .get(ConfigManager.getServerHome(),
                        ServerPathEnum.DATA_CONF.getPath(), INTERNAL_GROUPS_TXT)
                .toFile();
    }

    /**
     * Gets all the groups.
     *
     * @return The groups.
     * @throws IOException
     *             When IO errors reading the groups file.
     */
    public static Set<String> getGroups() throws IOException {
        return new GroupReader().getGroups(getFile());
    }

    /**
     * Gets the users in a group.
     *
     * @param groupName
     *            The group name.
     * @return The groups.
     * @throws IOException
     *             When IO errors reading the groups file.
     */
    public static SortedSet<CommonUser> getUsersInGroup(final String groupName)
            throws IOException {
        return new GroupMemberReader(groupName).getMembers(getFile());
    }

    /**
     * Gets all the groups with an indication if userId is found as a member.
     *
     * @param userId
     *            The user id.
     * @return The groups/membership map.
     * @throws IOException
     *             When IO errors reading the groups file.
     */
    public static Map<String, Boolean> getGroupsOfUser(final String userId)
            throws IOException {
        return new GroupReader().getGroups(getFile(), userId);
    }

}
