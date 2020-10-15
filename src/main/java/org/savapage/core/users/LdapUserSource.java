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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;

import org.apache.commons.lang3.StringUtils;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.config.IConfigProp.LdapTypeEnum;

/**
 * Standard LDAP User Source for all {@link LdapTypeEnum}, <b>except</b> for
 * {@link LdapTypeEnum#ACTD}).
 *
 * @author Rijk Ravestein
 *
 */
public class LdapUserSource extends LdapUserSourceMixin {

    /**
     *
     * @param ldapType
     *            The {@link LdapTypeEnum}.
     */
    public LdapUserSource(final LdapTypeEnum ldapType) {
        super(ldapType);
    }

    @Override
    protected final String createUserNameSearchPattern() {

        String pattern = getLdapConfigValue(Key.LDAP_SCHEMA_USER_NAME_SEARCH);

        if (StringUtils.isBlank(pattern)) {

            final StringBuilder builder = new StringBuilder();

            builder.append("(")
                    .append(getLdapConfigValue(Key.LDAP_SCHEMA_USER_NAME_FIELD))
                    .append("={0})");

            pattern = builder.toString();
        }

        return pattern;
    }

    @Override
    protected final String getUserNameSearchExpression(final String userName) {
        return MessageFormat.format(this.getUserNameSearchPattern(), userName);
    }

    @Override
    protected final boolean isUserGroupMember(final Attributes attributes)
            throws NamingException {
        /*
         * Since nested groups are not supported, we assume every member is a
         * user.
         */
        return true;
    }

    @Override
    protected final boolean allowDisabledUsers() {
        return false;
    }

    @Override
    protected final boolean isUserEnabled(final Attributes userAttributes)
            throws NamingException {
        return true;
    }

    @Override
    public final List<String> getGroupHierarchy(final String parentGroup,
            final boolean indent) {
        final List<String> list = new ArrayList<>();
        return list;
    }

    @Override
    public final SortedSet<CommonUser> getUsersInGroup(final String groupName,
            final boolean nested) {
        return this.getUsersInGroup(groupName);
    }

}
