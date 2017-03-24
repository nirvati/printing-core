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

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.persistence.Query;

import org.apache.commons.lang3.time.DateUtils;
import org.savapage.core.dao.AppLogDao;
import org.savapage.core.jpa.AppLog;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class AppLogDaoImpl extends GenericDaoImpl<AppLog>
        implements AppLogDao {

    @Override
    public long getListCount(final ListFilter filter) {

        final StringBuilder jpql =
                new StringBuilder(JPSQL_STRINGBUILDER_CAPACITY);

        jpql.append("SELECT COUNT(A.id) FROM AppLog A");

        applyListFilter(jpql, filter);

        Query query = createListQuery(jpql, filter);

        Number countResult = (Number) query.getSingleResult();
        return countResult.longValue();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<AppLog> getListChunk(final ListFilter filter,
            final Integer startPosition, final Integer maxResults,
            final Field orderBy, final boolean sortAscending) {

        final StringBuilder jpql =
                new StringBuilder(JPSQL_STRINGBUILDER_CAPACITY);

        jpql.append("SELECT A FROM AppLog A");

        applyListFilter(jpql, filter);

        //
        jpql.append(" ORDER BY ");

        if (orderBy == Field.LEVEL) {
            jpql.append("A.logLevel");
        } else {
            jpql.append("A.logDate");
        }

        if (!sortAscending) {
            jpql.append(" DESC");
        }

        //
        Query query = createListQuery(jpql, filter);

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
     *            The JPA query string.
     * @param filter
     *            The {@link ListFilter}.
     */
    private void applyListFilter(final StringBuilder jpql,
            final ListFilter filter) {

        int nWhere = 0;
        final StringBuilder where = new StringBuilder();

        if (filter.getLevel() != null) {
            if (nWhere > 0) {
                where.append(" AND");
            }
            nWhere++;
            where.append("  A.logLevel = :level");
        }

        if (filter.getDateFrom() != null) {
            if (nWhere > 0) {
                where.append(" AND");
            }
            nWhere++;
            where.append(" A.logDate >= :dateFrom");
        }

        if (filter.getDateTo() != null) {
            if (nWhere > 0) {
                where.append(" AND");
            }
            nWhere++;
            where.append(" A.logDate <= :dateTo");
        }

        if (filter.getContainingText() != null) {
            if (nWhere > 0) {
                where.append(" AND");
            }
            nWhere++;
            where.append(" lower(A.message) like :containingText");
        }

        if (nWhere > 0) {
            jpql.append(" WHERE").append(where);
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

        if (filter.getLevel() != null) {
            query.setParameter("level", filter.getLevel().toString());
        }
        if (filter.getDateFrom() != null) {
            query.setParameter("dateFrom", filter.getDateFrom());
        }
        if (filter.getDateTo() != null) {
            query.setParameter("dateTo", filter.getDateTo());
        }
        if (filter.getContainingText() != null) {
            query.setParameter("containingText", String.format("%%%s%%",
                    filter.getContainingText().toLowerCase()));
        }

        return query;
    }

    @Override
    public int clean(final int daysBackInTime) {

        if (daysBackInTime < 0) {
            throw new IllegalArgumentException(
                    "daysBackInTime must be GE than 0");
        }
        /*
         * Go back in time and truncate.
         */
        final Date dateBackInTime = DateUtils.truncate(
                DateUtils.addDays(new Date(), -daysBackInTime),
                Calendar.DAY_OF_MONTH);

        final String jpql = "DELETE FROM AppLog WHERE logDate <= :logdate";
        final Query query = getEntityManager().createQuery(jpql);
        query.setParameter("logdate", dateBackInTime);

        return query.executeUpdate();
    }

}
