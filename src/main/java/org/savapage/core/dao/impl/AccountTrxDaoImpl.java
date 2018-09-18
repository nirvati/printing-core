/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2018 Datraverse B.V.
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
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;

import org.savapage.core.dao.AccountTrxDao;
import org.savapage.core.dao.helpers.DaoBatchCommitter;
import org.savapage.core.jpa.Account;
import org.savapage.core.jpa.AccountTrx;
import org.savapage.core.jpa.User;
import org.savapage.core.jpa.UserAccount;
import org.savapage.core.jpa.tools.DbSimpleEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class AccountTrxDaoImpl extends GenericDaoImpl<AccountTrx>
        implements AccountTrxDao {

    /** */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(AccountTrxDaoImpl.class);

    @Override
    protected String getCountQuery() {
        return "SELECT COUNT(T.id) FROM AccountTrx T";
    }

    @Override
    public int eraseUser(final User user) {

        final String jpql = "UPDATE " + DbSimpleEntity.ACCOUNT_TRX
                + " X SET comment = null WHERE X.id IN"
                + " (SELECT TRX.id FROM " + DbSimpleEntity.USER_ACCOUNT + " UA"
                + " JOIN " + DbSimpleEntity.ACCOUNT_TRX
                + " TRX ON TRX.account = UA.account AND TRX.comment != null"
                + " WHERE UA.user = :user)";

        final Query query = getEntityManager().createQuery(jpql);
        query.setParameter("user", user.getId());
        return query.executeUpdate();
    }

    @Override
    public long getListCount(final ListFilter filter) {

        final StringBuilder jpql =
                new StringBuilder(JPSQL_STRINGBUILDER_CAPACITY);

        jpql.append("SELECT COUNT(TRX.id) FROM AccountTrx TRX");

        applyListFilter(jpql, filter);

        final Query query = createListQuery(jpql, filter);
        final Number countResult = (Number) query.getSingleResult();

        return countResult.longValue();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<AccountTrx> getListChunk(final ListFilter filter,
            final Integer startPosition, final Integer maxResults,
            final Field orderBy, final boolean sortAscending) {

        final StringBuilder jpql =
                new StringBuilder(JPSQL_STRINGBUILDER_CAPACITY);

        jpql.append("SELECT TRX FROM AccountTrx TRX");

        applyListFilter(jpql, filter);

        //
        jpql.append(" ORDER BY ");

        if (orderBy == Field.TRX_TYPE) {
            jpql.append("TRX.trxType");
        } else {
            jpql.append("TRX.transactionDate");
        }

        if (!sortAscending) {
            jpql.append(" DESC");
        }

        jpql.append(", TRX.id DESC");

        //
        final Query query = createListQuery(jpql, filter);

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

        final boolean filterAccountType = filter.getAccountType() != null;

        final StringBuilder joinClause = new StringBuilder();

        if (filter.getUserId() != null) {

            joinClause.append(" JOIN TRX.account AA WHERE AA.id ="
                    + " (SELECT A.id FROM UserAccount UA" + " JOIN UA.user U"
                    + " JOIN UA.account A" + " WHERE U.id = :userId");

            if (filterAccountType) {
                joinClause.append(" AND A.accountType = :accountType");
            }

            joinClause.append(")");

        } else if (filter.getAccountId() != null) {

            joinClause.append(" JOIN TRX.account AA WHERE AA.id = ")
                    .append(filter.getAccountId());

        } else if (filter.getDocLogId() != null) {

            joinClause.append(" WHERE TRX.docLog.id = :docLogId");

        } else {

            joinClause.append(" JOIN TRX.account AA");

            if (filterAccountType) {
                joinClause.append(" WHERE AA.accountType = :accountType");
            }

        }

        int nWhere = 0;
        final StringBuilder where = new StringBuilder();

        if (filter.getTrxType() != null) {
            if (nWhere > 0) {
                where.append(" AND");
            }
            nWhere++;
            where.append(" TRX.trxType = :trxType");
        }

        if (filter.getDateFrom() != null) {
            if (nWhere > 0) {
                where.append(" AND");
            }
            nWhere++;
            where.append(" TRX.transactionDate >= :dateFrom");
        }

        if (filter.getDateTo() != null) {
            if (nWhere > 0) {
                where.append(" AND");
            }
            nWhere++;
            where.append(" TRX.transactionDate <= :dateTo");
        }

        if (filter.getContainingCommentText() != null) {
            if (nWhere > 0) {
                where.append(" AND");
            }
            nWhere++;
            where.append(" lower(TRX.comment) like :containingText");
        }

        jpql.append(joinClause);

        if (nWhere > 0) {
            jpql.append(" AND").append(where);
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

        if (filter.getDocLogId() != null) {
            query.setParameter("docLogId", filter.getDocLogId());
        }

        if (filter.getAccountType() != null) {
            query.setParameter("accountType",
                    filter.getAccountType().toString());
        }
        if (filter.getUserId() != null) {
            query.setParameter("userId", filter.getUserId());
        }
        if (filter.getTrxType() != null) {
            query.setParameter("trxType", filter.getTrxType().toString());
        }
        if (filter.getDateFrom() != null) {
            query.setParameter("dateFrom", filter.getDateFrom());
        }
        if (filter.getDateTo() != null) {
            query.setParameter("dateTo", filter.getDateTo());
        }
        if (filter.getContainingCommentText() != null) {
            query.setParameter("containingText", String.format("%%%s%%",
                    filter.getContainingCommentText().toLowerCase()));
        }
        return query;
    }

    @Override
    public int cleanHistory(final Date dateBackInTime,
            final DaoBatchCommitter batchCommitter) {

        final String[] jpqlList = new String[2];
        final String psqlDateParm = "transactionDate";

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
                + " WHERE A.transactionDate <= :" + psqlDateParm + ")";

        /*
         * Step 2: Delete AccountTrx.
         *
         * Cascaded delete: PosPurchase, AccountVoucher
         */
        jpqlList[1] = "" //
                + "DELETE FROM " + DbSimpleEntity.ACCOUNT_TRX + " A "
                + " WHERE A.transactionDate <= :" + psqlDateParm;

        int nDeleted = 0;

        for (int i = 0; i < jpqlList.length; i++) {

            final String jpql = jpqlList[i];

            final Query query = getEntityManager().createQuery(jpql);
            query.setParameter(psqlDateParm, dateBackInTime);

            final int count = query.executeUpdate();

            if (i == 1) {
                nDeleted = count;
            }

            LOGGER.trace("|          step {}: {} ...", i + 1, count);

            batchCommitter.increment();
            batchCommitter.commit();

            LOGGER.trace("|               {}: {} committed.", i + 1, count);
        }

        if (nDeleted > 0) {
            this.cleanOrphaned(batchCommitter);
        }

        return nDeleted;
    }

    @Override
    public void cleanOrphaned(final DaoBatchCommitter batchCommitter) {

        final String[] jpqlList = new String[3];

        jpqlList[0] = "DELETE FROM " + DbSimpleEntity.POS_PURCHASE + " M"
                + " WHERE M.id IN" + " (SELECT P.id FROM "
                + DbSimpleEntity.POS_PURCHASE + " P" //
                + " LEFT JOIN " + DbSimpleEntity.ACCOUNT_TRX + " A"
                + " ON A.posPurchase = P.id" //
                + " WHERE A.posPurchase IS NULL)";

        jpqlList[1] = "DELETE FROM " + DbSimpleEntity.ACCOUNT_VOUCHER + " M"
                + " WHERE M.id IN" + " (SELECT V.id FROM "
                + DbSimpleEntity.ACCOUNT_VOUCHER + " V" //
                + " LEFT JOIN " + DbSimpleEntity.ACCOUNT_TRX + " A"
                + " ON A.accountVoucher = V.id" //
                + " WHERE A.accountVoucher IS NULL)";

        jpqlList[2] = "DELETE FROM " + DbSimpleEntity.COST_CHANGE + " M"
                + " WHERE M.id IN" + " (SELECT C.id FROM "
                + DbSimpleEntity.COST_CHANGE + " C" //
                + " LEFT JOIN " + DbSimpleEntity.ACCOUNT_TRX + " A"
                + " ON A.costChange = C.id" //
                + " WHERE A.costChange IS NULL)";

        for (int i = 0; i < jpqlList.length; i++) {

            final String jpql = jpqlList[i];

            final int count =
                    getEntityManager().createQuery(jpql).executeUpdate();

            LOGGER.trace("|               step {}: {} ...", i + 1, count);

            batchCommitter.increment();
            batchCommitter.commit();

            LOGGER.trace("|                    {}: {} committed.", i + 1,
                    count);
        }
    }

    @Override
    public AccountTrx findByExtId(final String extId) {

        final String jpql = "SELECT T FROM AccountTrx T WHERE extId = :extId";

        final Query query = getEntityManager().createQuery(jpql);

        query.setParameter("extId", extId);

        AccountTrx result = null;

        try {
            result = (AccountTrx) query.getSingleResult();
        } catch (NoResultException e) {
            result = null;
        }

        return result;
    }

    @Override
    public List<AccountTrx> findByExtMethodAddress(final String address) {

        final String jpql = "SELECT T FROM AccountTrx T "
                + "WHERE extMethodAddress = :extMethodAddress";

        final Query query = getEntityManager().createQuery(jpql);

        query.setParameter("extMethodAddress", address);

        @SuppressWarnings("unchecked")
        final List<AccountTrx> list = query.getResultList();

        return list;
    }

    @Override
    public TypedQuery<AccountTrx> getExportQuery(final User user) {

        final CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();

        final CriteriaQuery<AccountTrx> q = cb.createQuery(AccountTrx.class);
        final Root<AccountTrx> root = q.from(AccountTrx.class);
        final Join<Account, AccountTrx> rootAccount = root.join("account");

        // Subquery user accounts
        final Subquery<Long> sq = q.subquery(Long.class);
        final Root<UserAccount> userAccount = sq.from(UserAccount.class);
        final Join<Account, UserAccount> account = userAccount.join("account");

        final Path<User> pathUser = userAccount.join("user").get("id");
        final Predicate predicate = cb.equal(pathUser, user.getId());
        sq.where(predicate);

        final Path<Long> accountID = account.get("id");
        sq.select(accountID);
        q.where(cb.in(rootAccount.get("id")).value(sq));

        q.orderBy(cb.desc(root.get("transactionDate")));

        final CriteriaQuery<AccountTrx> select = q.select(root);

        return getEntityManager().createQuery(select);
    }

}
