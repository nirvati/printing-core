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
package org.savapage.core.users;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.savapage.core.rfid.RfidNumberFormat;

/**
 * An empty user source.
 *
 * @author Rijk Ravestein
 *
 */
public final class NoUserSource extends AbstractUserSource
        implements IUserSource {

    @Override
    public SortedSet<CommonUserGroup> getGroups() {
        final SortedSet<CommonUserGroup> sset =
                new TreeSet<>(new CommonUserGroupComparator());
        return sset;
    }

    @Override
    public SortedSet<CommonUser> getUsers() {
        final SortedSet<CommonUser> sset =
                new TreeSet<>(new CommonUserComparator());
        return sset;
    }

    @Override
    public SortedSet<CommonUser> getUsersInGroup(final String group) {
        return getUsersInGroup(group, false);
    }

    @Override
    public SortedSet<CommonUser> getUsersInGroup(final String groupName,
            final boolean nested) {
        final SortedSet<CommonUser> sset =
                new TreeSet<>(new CommonUserComparator());
        return sset;
    }

    @Override
    public boolean isUserInGroup(final String uid, final String group) {
        return true;
    }

    @Override
    public CommonUser getUser(final String uid) {
        return null;
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
    public boolean isGroupPresent(final String groupName) {
        return false;
    }

    @Override
    public List<String> getGroupHierarchy(final String parentGroup,
            final boolean indent) {
        final List<String> list = new ArrayList<>();
        return list;
    }

    @Override
    public CommonUserGroup getGroup(final String groupName) {
        return null;
    }

}
