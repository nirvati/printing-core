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
package org.savapage.core.users.conf;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.savapage.core.config.ConfigManager;
import org.savapage.core.users.AbstractUserSource.CommonUserComparator;
import org.savapage.core.users.CommonUser;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class InternalGroupList {

    /**
     * .
     */
    private static final class GroupReader extends ConfFileReader {

        private final Set<String> groups = new HashSet<>();

        @Override
        public void onItem(final String group, final String user) {
            groups.add(group);
        }

        public Set<String> getGroups(final File file) throws IOException {
            this.read(file);
            return groups;
        }
    }

    /**
     * .
     */
    private static final class GroupMemberReader extends ConfFileReader {

        private final String userGroup;

        final SortedSet<CommonUser> users =
                new TreeSet<>(new CommonUserComparator());

        public GroupMemberReader(final String groupName) {
            this.userGroup = groupName;
        }

        @Override
        public void onItem(final String group, final String userId) {
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
                        ConfigManager.SERVER_REL_PATH_INTERNAL_GROUPS_TXT)
                .toFile();
    }

    /**
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
     * @return The groups.
     * @throws IOException
     *             When IO errors reading the groups file.
     */
    public static SortedSet<CommonUser> getUsersInGroup(final String groupName)
            throws IOException {
        return new GroupMemberReader(groupName).getMembers(getFile());
    }
}
