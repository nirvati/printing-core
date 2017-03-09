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

import org.savapage.core.dao.DocLogDao;
import org.savapage.core.dao.enums.ExternalSupplierEnum;
import org.savapage.core.dao.enums.ExternalSupplierStatusEnum;
import org.savapage.core.jpa.DocLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class DocLogDaoImpl extends GenericDaoImpl<DocLog>
        implements DocLogDao {

    /**
     * .
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(DocLogDaoImpl.class);

    @Override
    public DocLog findByUuid(final Long userId, final String uuid) {

        final String jpql = "SELECT D FROM DocLog D JOIN D.user U"
                + " WHERE U.id = :userId AND D.uuid = :uuid";

        final Query query = getEntityManager().createQuery(jpql);

        query.setParameter("userId", userId);
        query.setParameter("uuid", uuid);

        DocLog docLog = null;

        try {
            docLog = (DocLog) query.getSingleResult();
        } catch (NoResultException e) {
            docLog = null;
        }

        return docLog;
    }

    @Override
    public int cleanDocOutHistory(final Date dateBackInTime) {

        final String jpql = "SELECT D FROM DocLog D WHERE "
                + "docOut IS NOT NULL AND createdDay <= :createdDay";

        final Query query = getEntityManager().createQuery(jpql);

        query.setParameter("createdDay", dateBackInTime);

        @SuppressWarnings("unchecked")
        final List<DocLog> listOut = query.getResultList();

        int nDeleted = 0;

        for (final DocLog docLog : listOut) {
            // cascaded delete
            this.delete(docLog);
            nDeleted++;
        }

        return nDeleted;
    }

    @Override
    public int cleanDocInHistory(final Date dateBackInTime) {

        final String jpql = "SELECT D FROM DocLog D WHERE "
                + "docIn IS NOT NULL AND createdDay <= :createdDay";

        final Query query = getEntityManager().createQuery(jpql);
        query.setParameter("createdDay", dateBackInTime);

        @SuppressWarnings("unchecked")
        final List<DocLog> listIn = query.getResultList();

        int nDeleted = 0;

        for (final DocLog docLog : listIn) {
            if (docLog.getDocIn().getDocsInOut().isEmpty()) {
                // cascaded delete
                this.delete(docLog);
                nDeleted++;
            }
        }
        return nDeleted;
    }

    @Override
    public long getListCount(final ListFilter filter) {

        final StringBuilder jpql =
                new StringBuilder(JPSQL_STRINGBUILDER_CAPACITY);

        jpql.append("SELECT COUNT(D.id) FROM DocLog D");

        applyListFilter(jpql, filter);

        final Query query = createListQuery(jpql, filter);
        final Number countResult = (Number) query.getSingleResult();

        return countResult.longValue();
    }

    @Override
    public List<DocLog> getListChunk(final ListFilter filter) {
        return getListChunk(filter, null, null, null, true);
    }

    @Override
    public List<DocLog> getListChunk(final ListFilter filter,
            final Integer startPosition, final Integer maxResults) {
        return getListChunk(filter, startPosition, maxResults, null, true);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<DocLog> getListChunk(final ListFilter filter,
            final Integer startPosition, final Integer maxResults,
            final Field orderBy, final boolean sortAscending) {

        final StringBuilder jpql =
                new StringBuilder(JPSQL_STRINGBUILDER_CAPACITY);

        jpql.append("SELECT D FROM DocLog D");

        applyListFilter(jpql, filter);

        //
        if (orderBy != null) {
            jpql.append(" ORDER BY ");

            if (orderBy == Field.DATE_CREATED) {
                jpql.append("D.createdDate");
            } else {
                jpql.append("D.createdDate");
            }

            if (!sortAscending) {
                jpql.append(" DESC");
            }
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(jpql.toString());
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
     *            The JPA query string.
     * @param filter
     *            The {@link ListFilter}.
     */
    private void applyListFilter(final StringBuilder jpql,
            final ListFilter filter) {

        int nWhere = 0;
        final StringBuilder where = new StringBuilder();

        if (filter.getExternalSupplier() != null) {
            if (nWhere > 0) {
                where.append(" AND");
            }
            nWhere++;
            where.append(" D.externalSupplier = :externalSupplier");
        }

        if (filter.getProtocol() != null) {
            if (nWhere > 0) {
                where.append(" AND");
            }
            nWhere++;
            where.append(" D.deliveryProtocol = :deliveryProtocol");
        }

        if (filter.getExternalStatus() != null) {
            if (nWhere > 0) {
                where.append(" AND");
            }
            nWhere++;
            where.append(" D.externalStatus = :externalStatus");
        }

        if (filter.getExternalId() != null) {
            if (nWhere > 0) {
                where.append(" AND");
            }
            nWhere++;
            where.append(" D.externalId = :externalId");
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

        if (filter.getProtocol() != null) {
            query.setParameter("deliveryProtocol",
                    filter.getProtocol().getDbName());
        }

        if (filter.getExternalSupplier() != null) {
            query.setParameter("externalSupplier",
                    filter.getExternalSupplier().toString());
        }

        if (filter.getExternalStatus() != null) {
            query.setParameter("externalStatus",
                    filter.getExternalStatus().toString());
        }

        if (filter.getExternalId() != null) {
            query.setParameter("externalId", filter.getExternalId());
        }

        return query;
    }

    @Override
    public boolean updateExtSupplier(final Long docLogId,
            final ExternalSupplierEnum extSupplier,
            final ExternalSupplierStatusEnum extStatus,
            final String documentTitle) {

        //
        final String externalSupplier;

        if (extSupplier == null) {
            externalSupplier = null;
        } else {
            externalSupplier = extSupplier.toString();
        }

        final String externalStatus;
        if (extStatus == null) {
            externalStatus = null;
        } else {
            externalStatus = extStatus.toString();
        }

        final StringBuilder jpql = new StringBuilder();

        jpql.append("UPDATE DocLog SET externalSupplier = :externalSupplier"
                + ", externalStatus = :externalStatus");

        if (documentTitle != null) {
            jpql.append(", title = :title");
        }

        jpql.append(" WHERE id = :id");

        final Query query = getEntityManager().createQuery(jpql.toString());

        query.setParameter("externalSupplier", externalSupplier)
                .setParameter("externalStatus", externalStatus)
                .setParameter("id", docLogId);

        if (documentTitle != null) {
            query.setParameter("title", documentTitle);
        }

        return executeSingleRowUpdate(query);
    }

}
