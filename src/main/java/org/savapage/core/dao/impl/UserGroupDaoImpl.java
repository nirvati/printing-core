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
package org.savapage.core.dao.impl;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.savapage.core.dao.UserGroupDao;
import org.savapage.core.dao.enums.ReservedUserGroupEnum;
import org.savapage.core.dao.enums.UserGroupAttrEnum;
import org.savapage.core.jpa.UserGroup;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class UserGroupDaoImpl extends GenericDaoImpl<UserGroup>
        implements UserGroupDao {

    @Override
    protected String getCountQuery() {
        return "SELECT COUNT(T.id) FROM UserGroup T";
    }

    @Override
    public ReservedUserGroupEnum findReservedGroup(final Long userGroupId) {
        final UserGroup userGroup = findById(userGroupId);
        if (userGroup == null) {
            return null;
        }
        return ReservedUserGroupEnum.fromDbName(userGroup.getGroupName());
    }

    @Override
    public UserGroup find(final ReservedUserGroupEnum reservedGroup) {
        return this.findByName(reservedGroup.getGroupName());
    }

    @Override
    public long getListCount(final ListFilter filter) {

        final StringBuilder jpql =
                new StringBuilder(JPSQL_STRINGBUILDER_CAPACITY);

        jpql.append("SELECT COUNT(C.id) FROM ");

        if (filter.getAclRole() == null) {
            jpql.append("UserGroup C");
        } else {
            jpql.append("UserGroupAttr A JOIN A.userGroup C");
        }
        applyListFilter(jpql, filter);

        final Query query = createListQuery(jpql, filter);
        final Number countResult = (Number) query.getSingleResult();
        return countResult.longValue();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<UserGroup> getListChunk(final ListFilter filter,
            final Integer startPosition, final Integer maxResults,
            final Field orderBy, final boolean sortAscending) {

        final StringBuilder jpql =
                new StringBuilder(JPSQL_STRINGBUILDER_CAPACITY);

        jpql.append("SELECT C FROM ");

        if (filter.getAclRole() == null) {
            jpql.append("UserGroup C");
        } else {
            jpql.append("UserGroupAttr A JOIN A.userGroup C");
        }

        applyListFilter(jpql, filter);

        //
        jpql.append(" ORDER BY ");

        if (orderBy == Field.NAME) {
            jpql.append("C.groupName");
        } else {
            jpql.append("C.groupName");
        }

        if (!sortAscending) {
            jpql.append(" DESC");
        }

        //
        final Query query = createListQuery(jpql, filter);

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
    private void applyListFilter(final StringBuilder jpql,
            final ListFilter filter) {

        final StringBuilder where = new StringBuilder();

        int nWhere = 0;

        if (filter.getAclRole() != null) {
            if (nWhere > 0) {
                where.append(" AND");
            }
            nWhere++;
            where.append(" A.name = :roleName AND A.value like :jsonRoleValue");
        }

        if (filter.getContainingText() != null) {
            if (nWhere > 0) {
                where.append(" AND");
            }
            nWhere++;
            where.append(" lower(C.groupName) like :containingText");
        }

        if (filter.getGroupIds() != null) {
            if (nWhere > 0) {
                where.append(" AND");
            }
            nWhere++;
            where.append(" C.id IN :groupIds");
        }

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
     *            The {@link ListFilter}.
     * @return The {@link Query}.
     */
    private Query createListQuery(final StringBuilder jpql,
            final ListFilter filter) {

        final Query query = getEntityManager().createQuery(jpql.toString());

        if (filter.getAclRole() != null) {

            query.setParameter("roleName",
                    UserGroupAttrEnum.ACL_ROLES.getName());

            final StringBuilder like = new StringBuilder();

            /*
             * INVARIANT: JSON string does NOT contain whitespace.
             */
            like.append("%\"").append(filter.getAclRole().toString())
                    .append("\":").append(Boolean.TRUE.toString()).append("%");

            query.setParameter("jsonRoleValue", like.toString());
        }

        if (filter.getContainingText() != null) {
            query.setParameter("containingText", String.format("%%%s%%",
                    filter.getContainingText().toLowerCase()));
        }

        if (filter.getGroupIds() != null) {
            query.setParameter("groupIds", filter.getGroupIds());
        }

        return query;
    }

    @Override
    public UserGroup findByName(final String groupName) {

        /*
         * select s from UserGroup s where groupName=:groupName
         */

        final String attributeName = "groupName";
        final String parameterName = attributeName;

        final EntityManager em = getEntityManager();

        final CriteriaBuilder cb = em.getCriteriaBuilder();

        final CriteriaQuery<UserGroup> query = cb.createQuery(UserGroup.class);

        final Root<UserGroup> root = query.from(UserGroup.class);
        query.where(cb.equal(root.<String> get(attributeName),
                cb.parameter(String.class, parameterName)));

        final TypedQuery<UserGroup> tq = em.createQuery(query);

        tq.setParameter(parameterName, groupName);

        UserGroup group = null;
        try {
            group = tq.getSingleResult();
        } catch (NoResultException e) {
            // noop
        }
        return group;

    }

}
