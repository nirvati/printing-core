/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2020 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: 2011-2020 Datraverse B.V. <info@datraverse.com>
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
package org.savapage.core.dao.impl;

import java.util.List;

import javax.persistence.Query;

import org.savapage.core.dao.UserGroupMemberDao;
import org.savapage.core.dao.enums.UserAttrEnum;
import org.savapage.core.jpa.User;
import org.savapage.core.jpa.UserGroup;
import org.savapage.core.jpa.UserGroupMember;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class UserGroupMemberDaoImpl
        extends GenericDaoImpl<UserGroupMember> implements UserGroupMemberDao {

    @Override
    protected String getCountQuery() {
        return "SELECT COUNT(T.id) FROM UserGroupMember T";
    }

    @Override
    public int deleteGroup(final Long groupId) {

        final String jpql =
                "DELETE UserGroupMember U WHERE U.group.id = :groupId";
        final Query query = getEntityManager().createQuery(jpql);
        query.setParameter("groupId", groupId);
        return query.executeUpdate();
    }

    @Override
    public long getGroupCount(final UserFilter filter) {

        final StringBuilder jpql =
                new StringBuilder(JPSQL_STRINGBUILDER_CAPACITY);

        jpql.append("SELECT COUNT(U.id) FROM UserGroupMember U");

        applyGroupFilter(jpql, filter);

        final Query query = createGroupQuery(jpql, filter);

        final Number countResult = (Number) query.getSingleResult();
        return countResult.longValue();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<UserGroup> getGroupChunk(final UserFilter filter,
            final Integer startPosition, final Integer maxResults,
            final GroupField orderBy, final boolean sortAscending) {

        final StringBuilder jpql =
                new StringBuilder(JPSQL_STRINGBUILDER_CAPACITY);

        jpql.append("SELECT U.group FROM UserGroupMember U");

        applyGroupFilter(jpql, filter);

        //
        jpql.append(" ORDER BY ");

        if (orderBy == GroupField.GROUP_NAME) {
            jpql.append("U.group.groupName");
        } else {
            jpql.append("U.group.groupName");
        }

        if (!sortAscending) {
            jpql.append(" DESC");
        }

        //
        final Query query = createGroupQuery(jpql, filter);

        //
        if (startPosition != null) {
            query.setFirstResult(startPosition);
        }
        if (maxResults != null) {
            query.setMaxResults(maxResults);
        }

        return query.getResultList();
    }

    /**
     * Applies the list filter to the JPQL string.
     *
     * @param jpql
     *            The {@link StringBuilder} to append to.
     * @param filter
     *            The filter.
     */
    private void applyGroupFilter(final StringBuilder jpql,
            final UserFilter filter) {

        final StringBuilder where = new StringBuilder();

        int nWhere = 0;

        if (nWhere > 0) {
            where.append(" AND");
        }
        nWhere++;
        where.append(" U.user.id = :userId");

        if (nWhere > 0) {
            jpql.append(" WHERE ").append(where.toString());
        }

    }

    /**
     * Creates the List Query and sets the filter parameters.
     *
     * @param jpql
     *            The JPA query string.
     * @param filter
     *            The {@link UserFilter}.
     * @return The {@link Query}.
     */
    private Query createGroupQuery(final StringBuilder jpql,
            final UserFilter filter) {

        final Query query = getEntityManager().createQuery(jpql.toString());

        query.setParameter("userId", filter.getUserId());

        return query;
    }

    /**
     * Appends JOIN statements.
     *
     * @param jpql
     *            String to append on.
     * @param filter
     *            The filter.
     */
    private void applyJoin(final StringBuilder jpql, final GroupFilter filter) {

        if (filter.getAclRoleNotFalse() != null) {
            jpql.append(" LEFT JOIN UserAttr UA ON UA.user = U.user "
                    + "AND UA.name = :roleName");
        }
    }

    @Override
    public long getUserCount(final GroupFilter filter) {

        final StringBuilder jpql =
                new StringBuilder(JPSQL_STRINGBUILDER_CAPACITY);

        jpql.append("SELECT COUNT(U.id) FROM UserGroupMember U");

        applyJoin(jpql, filter);
        applyUserFilter(jpql, filter);

        final Query query = createUserQuery(jpql, filter);

        final Number countResult = (Number) query.getSingleResult();
        return countResult.longValue();

    }

    @SuppressWarnings("unchecked")
    @Override
    public List<User> getUserChunk(final GroupFilter filter,
            final Integer startPosition, final Integer maxResults,
            final UserField orderBy, final boolean sortAscending) {

        final StringBuilder jpql =
                new StringBuilder(JPSQL_STRINGBUILDER_CAPACITY);

        jpql.append("SELECT U.user FROM UserGroupMember U");

        applyJoin(jpql, filter);
        applyUserFilter(jpql, filter);

        //
        jpql.append(" ORDER BY ");

        if (orderBy == UserField.USER_NAME) {
            jpql.append("U.user.userId");
        } else {
            jpql.append("U.user.userId");
        }

        if (!sortAscending) {
            jpql.append(" DESC");
        }

        //
        final Query query = createUserQuery(jpql, filter);

        //
        if (startPosition != null) {
            query.setFirstResult(startPosition);
        }
        if (maxResults != null) {
            query.setMaxResults(maxResults);
        }

        return query.getResultList();
    }

    /**
     * Applies the list filter to the JPQL string.
     *
     * @param jpql
     *            The {@link StringBuilder} to append to.
     * @param filter
     *            The filter.
     */
    private void applyUserFilter(final StringBuilder jpql,
            final GroupFilter filter) {

        final StringBuilder where = new StringBuilder();

        int nWhere = 0;

        if (nWhere > 0) {
            where.append(" AND");
        }
        nWhere++;
        where.append(" U.group.id = :groupId");

        if (nWhere > 0) {
            where.append(" AND");
        }
        nWhere++;
        where.append(" U.user.deleted = :deleted");

        //
        if (filter.getDisabledPrintOut() != null) {
            if (nWhere > 0) {
                where.append(" AND");
            }
            nWhere++;
            where.append(" U.user.disabledPrintOut = :disabledPrintOut");
        }

        if (filter.getAclRoleNotFalse() != null) {
            if (nWhere > 0) {
                where.append(" AND");
            }
            nWhere++;
            where.append(" (UA.name = null OR UA.value NOT LIKE :jsonRole"
                    + " OR UA.value LIKE :jsonRoleValue)");
        }

        //
        if (nWhere > 0) {
            jpql.append(" WHERE ").append(where.toString());
        }
    }

    /**
     * Creates the List Query and sets the filter parameters.
     *
     * @param jpql
     *            The JPA query string.
     * @param filter
     *            The {@link GroupFilter}.
     * @return The {@link Query}.
     */
    private Query createUserQuery(final StringBuilder jpql,
            final GroupFilter filter) {

        final Query query = getEntityManager().createQuery(jpql.toString());

        query.setParameter("groupId", filter.getGroupId());
        query.setParameter("deleted", Boolean.FALSE);

        if (filter.getDisabledPrintOut() != null) {
            query.setParameter("disabledPrintOut",
                    filter.getDisabledPrintOut());
        }

        if (filter.getAclRoleNotFalse() != null) {

            query.setParameter("roleName", UserAttrEnum.ACL_ROLES.getName());

            /*
             * INVARIANT: JSON string does NOT contain whitespace.
             */
            final String jsonRole = String.format("\"%s\"",
                    filter.getAclRoleNotFalse().toString());

            query.setParameter("jsonRole", String.format("%%%s%%", jsonRole));
            query.setParameter("jsonRoleValue", String.format("%%%s:%s%%",
                    jsonRole, Boolean.TRUE.toString()));
        }

        return query;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<UserGroupMember> getGroupMembers(final Long groupId) {

        final String jpql = "SELECT U FROM UserGroupMember U "
                + "WHERE U.group.id = :groupId AND U.user.deleted = :deleted "
                + "ORDER BY U.user.userId";

        final Query query = getEntityManager().createQuery(jpql);

        query.setParameter("groupId", groupId);
        query.setParameter("deleted", Boolean.FALSE);

        return query.getResultList();
    }

    @Override
    public boolean isUserInGroup(final String groupName, final String userId) {

        final String jpql = "SELECT count(U.id) FROM UserGroupMember U "
                + "WHERE U.group.groupName = :groupName"
                + " AND U.user.userId= :userId";

        final Query query = getEntityManager().createQuery(jpql.toString());
        query.setParameter("groupName", groupName);
        query.setParameter("userId", userId);

        return ((Number) query.getSingleResult()).longValue() == 1;
    }

}
