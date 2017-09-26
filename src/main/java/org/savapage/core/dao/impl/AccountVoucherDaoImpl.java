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
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Root;

import org.savapage.core.SpException;
import org.savapage.core.dao.AccountVoucherDao;
import org.savapage.core.jpa.Account.AccountTypeEnum;
import org.savapage.core.jpa.AccountVoucher;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class AccountVoucherDaoImpl extends GenericDaoImpl<AccountVoucher>
        implements AccountVoucherDao {

    @Override
    protected String getCountQuery() {
        return "SELECT COUNT(T.id) FROM AccountVoucher T";
    }

    @Override
    public int deleteExpired(final Date expiryDay) {
        /*
         * NOTE: The JPA 2.0 Criteria API does not currently support update and
         * delete operations.
         */
        final String jpql = "DELETE FROM AccountVoucher A "
                + "WHERE A.redeemedDate IS NULL "
                + "AND A.expiryDate <= :expiryDay";
        final Query query = getEntityManager().createQuery(jpql);

        query.setParameter("expiryDay", expiryDay);
        return query.executeUpdate();
    }

    @Override
    public int deleteBatch(final String batch) {
        /*
         * NOTE: The JPA 2.0 Criteria API does not currently support update and
         * delete operations.
         */
        final String jpql = "DELETE FROM AccountVoucher A "
                + "WHERE A.cardNumberBatch = :batch "
                + "AND A.redeemedDate IS NULL";
        final Query query = getEntityManager().createQuery(jpql);
        query.setParameter("batch", batch);
        return query.executeUpdate();
    }

    @Override
    public int expireBatch(final String batch, final Date expiryDay) {
        /*
         * NOTE: The JPA 2.0 Criteria API does not currently support update and
         * delete operations.
         */
        final String jpql =
                "UPDATE AccountVoucher A " + "SET A.expiryDate = :expiryDay "
                        + "WHERE A.cardNumberBatch = :batch "
                        + "AND A.redeemedDate IS NULL";
        final Query query = getEntityManager().createQuery(jpql);
        query.setParameter("batch", batch);
        query.setParameter("expiryDay", expiryDay);
        return query.executeUpdate();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<String> getBatches() {
        final String jpql = "SELECT DISTINCT A.cardNumberBatch "
                + "FROM AccountVoucher A ORDER BY A.cardNumberBatch";
        final Query query = getEntityManager().createQuery(jpql);
        return query.getResultList();
    }

    @Override
    public long getListCount(final ListFilter filter) {

        final StringBuilder jpql =
                new StringBuilder(JPSQL_STRINGBUILDER_CAPACITY);

        jpql.append("SELECT COUNT(A.id) FROM AccountVoucher A");

        applyListFilter(jpql, filter);

        Query query = createListQuery(jpql, filter);

        Number countResult = (Number) query.getSingleResult();
        return countResult.longValue();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<AccountVoucher> getListChunk(final ListFilter filter,
            final Integer startPosition, final Integer maxResults,
            final Field orderBy, final boolean sortAscending) {

        final StringBuilder jpql =
                new StringBuilder(JPSQL_STRINGBUILDER_CAPACITY);

        jpql.append("SELECT A FROM AccountVoucher A");

        applyListFilter(jpql, filter);

        //
        jpql.append(" ORDER BY ");

        switch (orderBy) {
        case DATE_EXPIRED:
            jpql.append("A.expiryDate");
            break;
        case DATE_USED:
            jpql.append("A.redeemedDate");
            break;
        case VALUE:
            jpql.append("A.valueAmount");
            break;
        case USER:
            // no break intended
        case NUMBER:
            // no break intended
        default:
            jpql.append("A.cardNumber");
            break;
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

        final String joinClause;

        if (filter.getUserId() != null) {
            joinClause = " JOIN A.accountTrx TRX "
                    + " JOIN TRX.account AA WHERE AA.id ="
                    + " (SELECT A.id FROM UserAccount UA" + " JOIN UA.user U"
                    + " JOIN UA.account A"
                    + " WHERE A.accountType = :accountType"
                    + " AND lower(U.userId) like :userId)";
        } else {
            joinClause = null;
        }

        int nWhere = 0;
        StringBuilder where = new StringBuilder();

        if (filter.getUsed() != null) {
            if (nWhere > 0) {
                where.append(" AND");
            }
            nWhere++;
            where.append(" A.redeemedDate IS ");
            if (filter.getUsed()) {
                where.append("NOT ");
            }
            where.append("NULL");
        }

        if (filter.getExpired() != null) {
            if (nWhere > 0) {
                where.append(" AND");
            }
            nWhere++;
            where.append(" A.expiryDate ");
            if (filter.getExpired()) {
                where.append("<=");
            } else {
                where.append(">");
            }
            where.append(" :dateNow");
        }

        if (filter.getDateFrom() != null) {
            if (nWhere > 0) {
                where.append(" AND");
            }
            nWhere++;
            where.append(" A.createdDate >= :dateFrom");
        }

        if (filter.getDateTo() != null) {
            if (nWhere > 0) {
                where.append(" AND");
            }
            nWhere++;
            where.append(" A.createdDate <= :dateTo");
        }

        if (filter.getBatch() != null) {
            if (nWhere > 0) {
                where.append(" AND");
            }
            nWhere++;
            where.append(" A.cardNumberBatch = :cardNumberBatch");
        }

        if (filter.getNumber() != null) {
            if (nWhere > 0) {
                where.append(" AND");
            }
            nWhere++;
            where.append(" lower(A.cardNumber) like :cardNumber");
        }

        if (filter.getVoucherType() != null) {
            if (nWhere > 0) {
                where.append(" AND");
            }
            nWhere++;
            where.append(" A.voucherType = :voucherType");
        }

        //

        if (joinClause != null) {
            jpql.append(joinClause);
            if (nWhere > 0) {
                jpql.append(" AND");
            }
        } else {
            if (nWhere > 0) {
                jpql.append(" WHERE");
            }
        }

        if (nWhere > 0) {
            jpql.append(where);
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
            query.setParameter("accountType", AccountTypeEnum.USER.toString());
            query.setParameter("userId",
                    String.format("%%%s%%", filter.getUserId().toLowerCase()));
        }

        if (filter.getExpired() != null) {
            if (filter.getDateNow() == null) {
                throw new SpException(
                        "Date Now is missing in filter for Expired selection");
            }
            query.setParameter("dateNow", filter.getDateNow());
        }
        if (filter.getDateFrom() != null) {
            query.setParameter("dateFrom", filter.getDateFrom());
        }
        if (filter.getDateTo() != null) {
            query.setParameter("dateTo", filter.getDateTo());
        }
        if (filter.getBatch() != null) {
            query.setParameter("cardNumberBatch", filter.getBatch());
        }
        if (filter.getNumber() != null) {
            query.setParameter("cardNumber",
                    String.format("%%%s%%", filter.getNumber().toLowerCase()));
        }
        if (filter.getVoucherType() != null) {
            query.setParameter("voucherType",
                    filter.getVoucherType().toString());
        }
        return query;
    }

    @Override
    public long countVouchersInBatch(final String batch) {
        ListFilter filter = new ListFilter();
        filter.setBatch(batch);
        return this.getListCount(filter);
    }

    @Override
    public AccountVoucher findByCardNumber(final String cardNumber) {
        /*
         * SELECT A from AccountVoucher A WHERE A.cardNumber=:cardNumber
         */
        final CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();

        final CriteriaQuery<AccountVoucher> q =
                cb.createQuery(AccountVoucher.class);

        final Root<AccountVoucher> c = q.from(AccountVoucher.class);
        q.select(c);

        final ParameterExpression<String> p = cb.parameter(String.class);
        q.where(cb.equal(c.get("cardNumber"), p));

        final TypedQuery<AccountVoucher> query =
                getEntityManager().createQuery(q);
        query.setParameter(p, cardNumber);

        AccountVoucher voucher = null;

        try {
            voucher = query.getSingleResult();
        } catch (NoResultException e) {
            voucher = null;
        }
        return voucher;
    }

}
