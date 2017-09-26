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

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Query;

import org.savapage.core.dao.UserGroupAccountDao;
import org.savapage.core.dto.SharedAccountDto;
import org.savapage.core.jpa.Account;
import org.savapage.core.jpa.UserGroupAccount;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class UserGroupAccountDaoImpl extends
        GenericDaoImpl<UserGroupAccount> implements UserGroupAccountDao {

    @Override
    protected String getCountQuery() {
        return "SELECT COUNT(T.id) FROM UserGroupAccount T";
    }

    /**
     * Applies the joins.
     *
     * @param jpql
     *            The JPA query string.
     */
    private void applyJoins(final StringBuilder jpql) {
        jpql.append("\nJOIN UGM.group UG");
        jpql.append("\nJOIN UGM.user U");
        jpql.append("\nJOIN UG.accounts UGA");
        jpql.append("\nJOIN UGA.account A");
    }

    /**
     * Applies the list filter to the JPQL string.
     *
     * @param jpql
     *            The JPA query string.
     * @param filter
     *            The {@link ListFilter}.
     */
    private void applyListFilter(final StringBuilder jpql,
            final ListFilter filter) {

        final StringBuilder where = new StringBuilder();

        int nWhere = 0;

        //
        if (nWhere > 0) {
            where.append(" AND");
        }
        nWhere++;
        where.append(" A.deleted = :selDeleted");

        //
        if (filter.getUserId() != null) {
            if (nWhere > 0) {
                where.append(" AND");
            }
            nWhere++;
            where.append(" U.id = :userId");
        }
        //
        if (filter.getContainingNameText() != null) {
            if (nWhere > 0) {
                where.append(" AND");
            }
            nWhere++;
            where.append(" A.nameLower like :containingNameText");
        }

        if (filter.getDisabled() != null) {
            if (nWhere > 0) {
                where.append(" AND");
            }
            nWhere++;
            where.append(" A.disabled = :selDisabled");
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
     *            The {@link ListFilter}.
     * @return The {@link Query}.
     */
    private Query createListQuery(final StringBuilder jpql,
            final ListFilter filter) {

        final Query query = getEntityManager().createQuery(jpql.toString());

        if (filter.getUserId() != null) {
            query.setParameter("userId", filter.getUserId());
        }

        if (filter.getContainingNameText() != null) {
            query.setParameter("containingNameText", String.format("%%%s%%",
                    filter.getContainingNameText().toLowerCase()));
        }

        query.setParameter("selDeleted", Boolean.FALSE);

        if (filter.getDisabled() != null) {
            query.setParameter("selDisabled", filter.getDisabled());
        }

        return query;
    }

    @Override
    public long getListCount(ListFilter filter) {

        final StringBuilder jpql =
                new StringBuilder(JPSQL_STRINGBUILDER_CAPACITY);

        jpql.append("SELECT COUNT(DISTINCT A) FROM UserGroupMember UGM");

        applyJoins(jpql);
        applyListFilter(jpql, filter);

        final Query query = createListQuery(jpql, filter);
        final Number countResult = (Number) query.getSingleResult();

        return countResult.longValue();
    }

    @Override
    public List<SharedAccountDto> getListChunk(ListFilter filter,
            Integer startPosition, Integer maxResults) {

        final StringBuilder jpql =
                new StringBuilder(JPSQL_STRINGBUILDER_CAPACITY);

        jpql.append("SELECT DISTINCT A FROM UserGroupMember UGM");

        applyJoins(jpql);
        applyListFilter(jpql, filter);

        final boolean sortAscending = true;

        jpql.append("\nORDER BY A.name");
        if (!sortAscending) {
            jpql.append(" DESC");
        }

        final Query query = createListQuery(jpql, filter);

        if (startPosition != null) {
            query.setFirstResult(startPosition);
        }
        if (maxResults != null) {
            query.setMaxResults(maxResults);
        }

        @SuppressWarnings("unchecked")
        final List<Account> resultList = query.getResultList();

        final List<SharedAccountDto> sharedAccounts = new ArrayList<>();

        for (final Account account : resultList) {

            final SharedAccountDto dto = new SharedAccountDto();

            dto.setId(account.getId());
            dto.setName(account.getName());

            final Account parent = account.getParent();
            if (parent != null) {
                dto.setParentId(parent.getId());
                dto.setParentName(parent.getName());
            }

            sharedAccounts.add(dto);
        }

        return sharedAccounts;
    }

}
