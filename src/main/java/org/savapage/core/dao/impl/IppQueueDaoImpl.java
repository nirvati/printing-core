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

import java.util.Date;
import java.util.List;

import javax.persistence.NoResultException;
import javax.persistence.Query;

import org.savapage.core.dao.IppQueueDao;
import org.savapage.core.dao.enums.ReservedIppQueueEnum;
import org.savapage.core.dao.helpers.DaoBatchCommitter;
import org.savapage.core.jpa.IppQueue;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class IppQueueDaoImpl extends GenericDaoImpl<IppQueue>
        implements IppQueueDao {

    @Override
    protected String getCountQuery() {
        return "SELECT COUNT(T.id) FROM IppQueue T";
    }

    @Override
    public void resetTotals(final Date resetDate, final String resetBy) {

        final String jpql = "UPDATE IppQueue Q SET "
                + "Q.totalBytes = 0, Q.totalJobs = 0, " + "Q.totalPages = 0, "
                + "Q.resetDate = :resetDate, Q.resetBy = :resetBy";

        final Query query = getEntityManager().createQuery(jpql);

        query.setParameter("resetDate", resetDate);
        query.setParameter("resetBy", resetBy);

        query.executeUpdate();
    }

    @Override
    public IppQueue find(final ReservedIppQueueEnum reservedQueue) {
        return this.findByUrlPath(reservedQueue.getUrlPath());
    }

    @Override
    public IppQueue findByUrlPath(final String urlPath) {

        final String jpql =
                "SELECT Q FROM IppQueue Q WHERE Q.urlPath = :urlPath";

        final Query query = getEntityManager().createQuery(jpql);
        query.setParameter("urlPath", urlPath);

        IppQueue queue;

        try {
            queue = (IppQueue) query.getSingleResult();
        } catch (NoResultException e) {
            queue = null;
        }

        return queue;
    }

    @Override
    public int pruneQueues(final DaoBatchCommitter batchCommitter) {
        /*
         * NOTE: We do NOT use bulk delete with JPQL since we want the option to
         * roll back the deletions as part of a transaction, and we want to use
         * cascade deletion. Therefore we use the remove() method in
         * EntityManager to delete individual records instead (so cascaded
         * deleted are triggered).
         */
        int nCount = 0;

        final String jpql =
                "SELECT Q.id FROM IppQueue Q WHERE Q.deleted = true "
                        + "AND Q.printsIn IS EMPTY";

        final Query query = getEntityManager().createQuery(jpql);

        @SuppressWarnings("unchecked")
        final List<Long> list = query.getResultList();

        for (final Long id : list) {
            this.delete(this.findById(id));
            nCount++;
            batchCommitter.increment();
        }
        return nCount;
    }

    @Override
    public long getListCount(final ListFilter filter) {
        final StringBuilder jpql =
                new StringBuilder(JPSQL_STRINGBUILDER_CAPACITY);

        jpql.append("SELECT COUNT(Q.id) FROM IppQueue Q");

        applyListFilter(jpql, filter);

        final Query query = createListQuery(jpql.toString(), filter);
        final Number countResult = (Number) query.getSingleResult();

        return countResult.longValue();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<IppQueue> getListChunk(final ListFilter filter,
            final Integer startPosition, final Integer maxResults,
            final Field orderBy, final boolean sortAscending) {

        final StringBuilder jpql =
                new StringBuilder(JPSQL_STRINGBUILDER_CAPACITY);

        /**
         * #190: Do not use JOIN FETCH construct.
         */
        jpql.append("SELECT Q FROM IppQueue Q");

        applyListFilter(jpql, filter);

        //
        jpql.append(" ORDER BY ");

        if (orderBy == Field.URL_PATH) {
            jpql.append("Q.urlPath");
        } else {
            jpql.append("Q.urlPath");
        }

        if (!sortAscending) {
            jpql.append(" DESC");
        }

        //
        final Query query = createListQuery(jpql.toString(), filter);

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
     * Creates the List Query and sets the filter parameters.
     *
     * @param jpql
     *            The JPA query string.
     * @param filter
     *            The filter.
     * @return The query.
     */
    private Query createListQuery(final String jpql, final ListFilter filter) {

        final Query query = getEntityManager().createQuery(jpql);

        if (filter.getContainingText() != null) {
            query.setParameter("containingText", String.format("%%%s%%",
                    filter.getContainingText().toLowerCase()));
        }

        if (filter.getTrusted() != null) {
            query.setParameter("selTrusted", filter.getTrusted());
        }

        if (filter.getDisabled() != null) {
            query.setParameter("selDisabled", filter.getDisabled());
        }

        if (filter.getDeleted() != null) {
            query.setParameter("selDeleted", filter.getDeleted());
        }

        return query;
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

        if (filter.getContainingText() != null) {
            if (nWhere > 0) {
                where.append(" AND");
            }
            nWhere++;
            where.append(" lower(Q.urlPath) like :containingText");
        }

        if (filter.getTrusted() != null) {
            if (nWhere > 0) {
                where.append(" AND");
            }
            nWhere++;
            where.append(" Q.trusted = :selTrusted");
        }

        if (filter.getDisabled() != null) {
            if (nWhere > 0) {
                where.append(" AND");
            }
            nWhere++;
            where.append(" Q.disabled= :selDisabled");
        }

        if (filter.getDeleted() != null) {
            if (nWhere > 0) {
                where.append(" AND");
            }
            nWhere++;
            where.append(" Q.deleted= :selDeleted");
        }
        //
        if (nWhere > 0) {
            jpql.append(" WHERE ").append(where.toString());
        }

    }

}
