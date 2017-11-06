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

import org.savapage.core.SpInfo;
import org.savapage.core.dao.DocLogDao;
import org.savapage.core.dao.enums.ExternalSupplierEnum;
import org.savapage.core.dao.enums.ExternalSupplierStatusEnum;
import org.savapage.core.dao.helpers.DaoBatchCommitter;
import org.savapage.core.jpa.DocLog;
import org.savapage.core.jpa.User;
import org.savapage.core.jpa.tools.DbSimpleEntity;
import org.savapage.core.services.ServiceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class DocLogDaoImpl extends GenericDaoImpl<DocLog>
        implements DocLogDao {

    @Override
    protected String getCountQuery() {
        return "SELECT COUNT(T.id) FROM DocLog T";
    }

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
    public int cleanAccountTrxHistory(final Date dateBackInTime,
            final DaoBatchCommitter batchCommitter) {

        final String[] jpqlList = new String[2];
        final String psqlDateParm = "createdDay";

        /*
         * Step 1: Delete PosPurchaseItem.
         */
        jpqlList[0] = "" //
                + "DELETE FROM " + DbSimpleEntity.POS_PURCHASE_ITEM + " M"
                + " WHERE M.id IN" + " (SELECT PI.id FROM "
                + DbSimpleEntity.POS_PURCHASE_ITEM + " PI" //
                + " JOIN " + DbSimpleEntity.POS_PURCHASE
                + " P ON P.id = PI.purchase" //
                + " JOIN " + DbSimpleEntity.ACCOUNT_TRX
                + " A ON A.posPurchase = P.id" //
                + " JOIN " + DbSimpleEntity.DOC_LOG + " L ON A.docLog = L.id"
                + " WHERE L.createdDay <= :" + psqlDateParm + ")";

        /*
         * Step 2: Delete AccountTrx.
         */
        jpqlList[1] = "" //
                + "DELETE FROM " + DbSimpleEntity.ACCOUNT_TRX + " M"
                + " WHERE M.id IN" + " (SELECT A.id FROM "
                + DbSimpleEntity.ACCOUNT_TRX + " A" //
                + " JOIN " + DbSimpleEntity.DOC_LOG + " L ON A.docLog = L.id"
                + " WHERE L.createdDay <= :" + psqlDateParm + ")";

        final int nDeleted = this.cleanHistory(jpqlList, dateBackInTime,
                psqlDateParm, 1, batchCommitter);

        if (nDeleted > 0) {
            ServiceContext.getDaoContext().getAccountTrxDao()
                    .cleanOrphaned(batchCommitter);
        }
        return nDeleted;
    }

    @Override
    public int cleanDocInHistory(final Date dateBackInTime,
            final DaoBatchCommitter batchCommitter) {

        String[] jpqlList = new String[3];
        final String psqlDateParm = "createdDay";

        /*
         * Step 1: Delete DocInOut.
         */
        jpqlList[0] = "" //
                + "DELETE FROM " + DbSimpleEntity.DOC_IN_OUT + " M"
                + " WHERE M.id IN" + " (SELECT IO.id FROM "
                + DbSimpleEntity.DOC_IN_OUT + " IO" //
                + " JOIN " + DbSimpleEntity.DOC_IN + " I ON I.id = IO.docIn" //
                + " JOIN " + DbSimpleEntity.DOC_LOG + " L ON L.docIn = I.id" //
                + " WHERE L.createdDay <= :" + psqlDateParm + ")";

        /*
         * Step 2: CostChange.
         */
        jpqlList[1] = "" //
                + "DELETE FROM " + DbSimpleEntity.COST_CHANGE + " M"
                + " WHERE M.id IN" + " (SELECT C.id FROM "
                + DbSimpleEntity.COST_CHANGE + " C" //
                + " JOIN " + DbSimpleEntity.DOC_LOG + " L ON L.id = C.docLog" //
                + " WHERE C.docLog IS NOT NULL" //
                + " AND L.docIn IS NOT NULL" //
                + " AND L.createdDay <= :" + psqlDateParm + ")";

        /*
         * Step 3: DocLog.
         */
        jpqlList[2] = "" //
                + "DELETE FROM " + DbSimpleEntity.DOC_LOG + " L "
                + " WHERE L.docIn IS NOT NULL AND L.createdDay <= :"
                + psqlDateParm;

        final int nDeleted = this.cleanHistory(jpqlList, dateBackInTime,
                psqlDateParm, 2, batchCommitter);

        if (nDeleted > 0) {

            jpqlList = new String[2];

            /*
             * Step 4: Delete orphaned: DocIn, PrintIn
             */
            jpqlList[0] = "DELETE FROM " + DbSimpleEntity.DOC_IN + " M"
                    + " WHERE M.id IN" + " (SELECT I.id FROM "
                    + DbSimpleEntity.DOC_IN + " I" //
                    + " LEFT JOIN " + DbSimpleEntity.DOC_LOG + " L"
                    + " ON L.docIn = I.id" //
                    + " WHERE L.docIn IS NULL)";

            jpqlList[1] = "DELETE FROM " + DbSimpleEntity.PRINT_IN + " M"
                    + " WHERE M.id IN" + " (SELECT P.id FROM "
                    + DbSimpleEntity.PRINT_IN + " P" //
                    + " LEFT JOIN " + DbSimpleEntity.DOC_IN + " I"
                    + " ON I.printIn = P.id" //
                    + " WHERE I.printIn IS NULL)";

            int i = 0;

            for (final String jpql : jpqlList) {

                final int count =
                        getEntityManager().createQuery(jpql).executeUpdate();

                i++;

                SpInfo.instance().log(String
                        .format("|               step %d: %d ...", i, count));

                batchCommitter.increment();
                batchCommitter.commit();

                SpInfo.instance().log(String.format(
                        "|                    %d: %d committed.", i, count));
            }
        }
        return nDeleted;
    }

    @Override
    public int cleanDocOutHistory(final Date dateBackInTime,
            final DaoBatchCommitter batchCommitter) {

        final String[] jpqlList = new String[3];
        final String psqlDateParm = "createdDay";

        /*
         * Step 1: Delete DocInOut.
         */
        jpqlList[0] = "" //
                + "DELETE FROM " + DbSimpleEntity.DOC_IN_OUT + " M"
                + " WHERE M.id IN" + " (SELECT IO.id FROM "
                + DbSimpleEntity.DOC_IN_OUT + " IO" //
                + " JOIN " + DbSimpleEntity.DOC_OUT + " O ON O.id = IO.docOut"
                + " JOIN " + DbSimpleEntity.DOC_LOG + " L ON L.docOut = O.id"
                + " WHERE L.createdDay <= :" + psqlDateParm + ")";

        /*
         * Step 2: CostChange.
         */
        jpqlList[1] = "" //
                + "DELETE FROM " + DbSimpleEntity.COST_CHANGE + " M"
                + " WHERE M.id IN" + " (SELECT C.id FROM "
                + DbSimpleEntity.COST_CHANGE + " C" //
                + " JOIN " + DbSimpleEntity.DOC_LOG + " L ON L.id = C.docLog" //
                + " WHERE C.docLog IS NOT NULL" //
                + " AND L.docOut IS NOT NULL" //
                + " AND L.createdDay <= :" + psqlDateParm + ")";

        /*
         * Step 3: DocLog.
         */
        jpqlList[2] = "" //
                + "DELETE FROM " + DbSimpleEntity.DOC_LOG + " L "
                + " WHERE L.docOut IS NOT NULL AND L.createdDay <= :"
                + psqlDateParm;

        final int nDeleted = this.cleanHistory(jpqlList, dateBackInTime,
                psqlDateParm, 2, batchCommitter);

        if (nDeleted > 0) {
            /*
             * Step 4: Delete orphaned: DocOut, PrintOut, PdfOut
             */
            final String[] jpqlListOrphan = new String[3];

            jpqlListOrphan[0] = "DELETE FROM " + DbSimpleEntity.DOC_OUT + " M"
                    + " WHERE M.id IN" + " (SELECT O.id FROM "
                    + DbSimpleEntity.DOC_OUT + " O" //
                    + " LEFT JOIN " + DbSimpleEntity.DOC_LOG + " L"
                    + " ON L.docOut = O.id" //
                    + " WHERE L.docOut IS NULL)";

            jpqlListOrphan[1] = "DELETE FROM " + DbSimpleEntity.PRINT_OUT + " M"
                    + " WHERE M.id IN" + " (SELECT P.id FROM "
                    + DbSimpleEntity.PRINT_OUT + " P" //
                    + " LEFT JOIN " + DbSimpleEntity.DOC_OUT + " O"
                    + " ON O.printOut = P.id" //
                    + " WHERE O.printOut IS NULL)";

            jpqlListOrphan[2] = "DELETE FROM " + DbSimpleEntity.PDF_OUT + " M"
                    + " WHERE M.id IN" + " (SELECT P.id FROM "
                    + DbSimpleEntity.PDF_OUT + " P" //
                    + " LEFT JOIN " + DbSimpleEntity.DOC_OUT + " O"
                    + " ON O.pdfOut = P.id" //
                    + " WHERE O.pdfOut IS NULL)";

            int i = 0;

            for (final String jpql : jpqlListOrphan) {

                final int count =
                        getEntityManager().createQuery(jpql).executeUpdate();

                i++;

                SpInfo.instance().log(String
                        .format("|               step %d: %d ...", i, count));

                batchCommitter.increment();
                batchCommitter.commit();

                SpInfo.instance().log(String.format(
                        "|                    %d: %d committed.", i, count));
            }
        }

        return nDeleted;
    }

    /**
     *
     * @param jpqlList
     * @param dateBackInTime
     * @param dateBackInTimeParm
     * @param iDeleted
     * @param batchCommitter
     * @return
     */
    private int cleanHistory(final String[] jpqlList, final Date dateBackInTime,
            final String dateBackInTimeParm, final int iDeleted,
            final DaoBatchCommitter batchCommitter) {

        int nDeleted = 0;

        for (int i = 0; i < jpqlList.length; i++) {

            final String jpql = jpqlList[i];

            final Query query = getEntityManager().createQuery(jpql);
            query.setParameter(dateBackInTimeParm, dateBackInTime);

            final int count = query.executeUpdate();
            if (i == iDeleted) {
                nDeleted = count;
            }

            SpInfo.instance().log(
                    String.format("|          step %d: %d ...", i + 1, count));

            batchCommitter.increment();
            batchCommitter.commit();

            SpInfo.instance().log(String
                    .format("|               %d: %d committed.", i + 1, count));
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

        } else if (filter.getContainingExternalIdText() != null) {

            if (nWhere > 0) {
                where.append(" AND");
            }
            nWhere++;
            where.append(" lower(D.externalId) like :containingExternalIdText");
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
        } else if (filter.getContainingExternalIdText() != null) {
            query.setParameter("containingExternalIdText", String.format(
                    "%%%s%%",
                    filter.getContainingExternalIdText().toLowerCase()));
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

    @Override
    public int eraseUser(final User user) {
        final String jpql = "UPDATE " + DbSimpleEntity.DOC_LOG
                + " SET title = null, logComment = null "
                + " WHERE user = :user)";
        final Query query = getEntityManager().createQuery(jpql);
        query.setParameter("user", user.getId());
        return query.executeUpdate();
    }

}
