/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2014 Datraverse B.V.
 * Authors: Rijk Ravestein.
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.core.dao.impl;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import javax.persistence.NoResultException;
import javax.persistence.Query;

import org.savapage.core.dao.AccountDao;
import org.savapage.core.dao.helpers.AggregateResult;
import org.savapage.core.jpa.Account;
import org.savapage.core.jpa.Account.AccountTypeEnum;

/**
 *
 * @author Datraverse B.V.
 *
 */
public final class AccountDaoImpl extends GenericDaoImpl<Account> implements
        AccountDao {

    @Override
    public int pruneAccounts() {

        final String jpql =
                "SELECT A FROM Account A WHERE A.deleted = true "
                        + "AND A.transactions IS EMPTY";

        final Query query = getEntityManager().createQuery(jpql);

        @SuppressWarnings("unchecked")
        final List<Account> list = query.getResultList();

        int nDeleted = 0;

        for (final Account account : list) {
            // cascaded delete
            this.delete(account);
            nDeleted++;
        }
        return nDeleted;
    }

    @Override
    public Account findActiveAccountByName(final String name,
            final AccountTypeEnum accountType) {

        final String jpql =
                "SELECT A FROM Account A WHERE A.name = :name"
                        + " AND A.accountType = :accountType"
                        + " AND A.deleted = false";

        final Query query = getEntityManager().createQuery(jpql);

        query.setParameter("name", name);
        query.setParameter("accountType", accountType.toString());

        Account account = null;

        try {
            account = (Account) query.getSingleResult();
        } catch (NoResultException e) {
            account = null;
        }
        return account;
    }

    @Override
    public Account findActiveSharedAccountByName(final String name) {
        return this.findActiveAccountByName(name, AccountTypeEnum.SHARED);
    }

    @Override
    public Account findActiveSharedChildAccountByName(final Long parentId,
            final String name) {

        final String jpql =
                "SELECT A FROM Account A JOIN A.parent P"
                        + " WHERE A.name = :name" + " AND P.id = :parentId"
                        + " AND A.accountType = :accountType"
                        + " AND A.deleted = false";

        final Query query = getEntityManager().createQuery(jpql);

        query.setParameter("name", name);
        query.setParameter("parentId", parentId);
        query.setParameter("accountType", AccountTypeEnum.SHARED.toString());

        Account account = null;

        try {
            account = (Account) query.getSingleResult();
        } catch (NoResultException e) {
            account = null;
        }
        return account;
    }

    @Override
    public Account createFromTemplate(final String accountName,
            final Account accountTemplate) {

        final Account account = new Account();

        /*
         * Set identifying attributes.
         */
        account.setParent(accountTemplate.getParent());
        account.setName(accountName);
        account.setNameLower(accountName.toLowerCase());

        /*
         * Copy data from template.
         */
        account.setBalance(accountTemplate.getBalance());
        account.setOverdraft(accountTemplate.getOverdraft());
        account.setRestricted(accountTemplate.getRestricted());
        account.setUseGlobalOverdraft(accountTemplate.getUseGlobalOverdraft());

        account.setAccountType(accountTemplate.getAccountType());
        account.setComments(accountTemplate.getComments());
        account.setInvoicing(accountTemplate.getInvoicing());
        account.setDeleted(false);
        account.setDisabled(false);

        account.setCreatedBy(accountTemplate.getCreatedBy());
        account.setCreatedDate(accountTemplate.getCreatedDate());

        this.create(account);

        return account;
    }

    @Override
    public AggregateResult<BigDecimal> getBalanceStats(
            final boolean userAccounts, final boolean debit) {

        final StringBuilder jpql = new StringBuilder();

        jpql.append("SELECT count(*), sum(A.balance), "
                + "min(A.balance), max(A.balance), avg(A.balance) "
                + "FROM Account A WHERE A.deleted = false AND A.accountType ");

        if (userAccounts) {
            jpql.append("is not");
        } else {
            jpql.append("=");
        }

        jpql.append(" :accountType AND A.balance ");

        if (debit) {
            jpql.append("> 0");
        } else {
            jpql.append("< 0");
        }

        final Query query = getEntityManager().createQuery(jpql.toString());

        query.setParameter("accountType", AccountTypeEnum.SHARED.toString());

        try {
            final Object[] result = (Object[]) query.getSingleResult();

            return new AggregateResult<BigDecimal>((Long) result[0],
                    (BigDecimal) result[1], (BigDecimal) result[2],
                    (BigDecimal) result[3], (Double) result[4]);

        } catch (NoResultException e) {
            return new AggregateResult<BigDecimal>();
        }

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

        StringBuilder where = new StringBuilder();

        int nWhere = 0;

        if (filter.getAccountType() != null
                || filter.getAccountTypeExtra() != null) {

            if (nWhere > 0) {
                where.append(" AND");
            }
            nWhere++;

            where.append(" ACC.accountType IN(");

            boolean first = false;

            if (filter.getAccountType() != null) {
                first = true;
                where.append(" :accountType");
            }

            if (filter.getAccountTypeExtra() != null) {
                if (first) {
                    where.append(",");
                }
                where.append(":accountTypeExtra");
            }

            where.append(")");
        }

        if (filter.getContainingNameText() != null) {
            if (nWhere > 0) {
                where.append(" AND");
            }
            nWhere++;
            where.append(" ACC.nameLower like :containingNameText");
        }

        if (filter.getDeleted() != null) {
            if (nWhere > 0) {
                where.append(" AND");
            }
            nWhere++;
            where.append(" ACC.deleted = :selDeleted");
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

        if (filter.getAccountType() != null) {
            query.setParameter("accountType", filter.getAccountType()
                    .toString());
        }

        if (filter.getAccountTypeExtra() != null) {
            query.setParameter("accountTypeExtra", filter.getAccountTypeExtra()
                    .toString());
        }

        if (filter.getContainingNameText() != null) {
            query.setParameter("containingNameText", "%"
                    + filter.getContainingNameText().toLowerCase() + "%");
        }

        if (filter.getDeleted() != null) {
            query.setParameter("selDeleted", filter.getDeleted());
        }

        return query;
    }

    @Override
    public long getListCount(final ListFilter filter) {

        final StringBuilder jpql =
                new StringBuilder(JPSQL_STRINGBUILDER_CAPACITY);

        jpql.append("SELECT COUNT(ACC.id) FROM Account ACC");

        applyListFilter(jpql, filter);

        final Query query = createListQuery(jpql, filter);
        final Number countResult = (Number) query.getSingleResult();

        return countResult.longValue();
    }

    @Override
    public List<Account> getListChunk(ListFilter filter, Integer startPosition,
            Integer maxResults, Field orderBy, boolean sortAscending) {

        final StringBuilder jpql =
                new StringBuilder(JPSQL_STRINGBUILDER_CAPACITY);

        jpql.append("SELECT ACC FROM Account ACC");

        if (orderBy == Field.NAME) {
            jpql.append(" LEFT JOIN ACC.parent P ");
        }

        applyListFilter(jpql, filter);

        //
        jpql.append(" ORDER BY ");

        if (orderBy == Field.ACCOUNT_TYPE) {

            jpql.append("ACC.accountType");

        } else if (orderBy == Field.NAME) {

            jpql.append("COALESCE(P.name, ACC.name)");
            if (!sortAscending) {
                jpql.append(" DESC");
            }

            jpql.append(", COALESCE(P.id, ACC.id)");
            if (!sortAscending) {
                jpql.append(" DESC");
            }

            jpql.append(", COALESCE(P.id, 0)");
            jpql.append(", ACC.name");

        } else {
            jpql.append("ACC.accountType");
        }

        if (!sortAscending) {
            jpql.append(" DESC");
        }

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

    @Override
    public long countSubAccounts(final Long parentId) {

        final StringBuilder jpql =
                new StringBuilder(JPSQL_STRINGBUILDER_CAPACITY);

        jpql.append("SELECT COUNT(ACC.id) FROM Account ACC JOIN ACC.parent P");
        jpql.append(" WHERE P.id = ").append(parentId);

        final Query query = getEntityManager().createQuery(jpql.toString());
        final Number countResult = (Number) query.getSingleResult();

        return countResult.longValue();
    }

    @Override
    public List<Account> getSubAccounts(final Long parentId) {

        final StringBuilder jpql =
                new StringBuilder(JPSQL_STRINGBUILDER_CAPACITY);

        jpql.append("SELECT ACC FROM Account ACC JOIN ACC.parent P");
        jpql.append(" WHERE P.id = ").append(parentId);

        final Query query = getEntityManager().createQuery(jpql.toString());
        return query.getResultList();
    }

    @Override
    public void setLogicalDelete(final Account account, final Date deletedDate,
            final String deletedBy) {

        account.setDeleted(Boolean.TRUE);
        account.setDeletedDate(deletedDate);
        account.setModifiedDate(deletedDate);
        account.setModifiedBy(deletedBy);
    }

    @Override
    public int logicalDeleteSubAccounts(final Long parentId,
            final Date deletedDate, final String deletedBy) {

        final StringBuilder jpql =
                new StringBuilder(JPSQL_STRINGBUILDER_CAPACITY);

        jpql.append("UPDATE Account A SET " + "A.deleted = :deleted, "
                + "A.deletedDate = :deletedDate, "
                + "A.modifiedDate = :modifiedDate, "
                + "A.modifiedBy = :modifiedBy " + "WHERE A.parent.id = :id");

        final Query query = getEntityManager().createQuery(jpql.toString());

        query.setParameter("id", parentId);
        query.setParameter("deleted", Boolean.TRUE);
        query.setParameter("deletedDate", deletedDate);
        query.setParameter("modifiedDate", deletedDate);
        query.setParameter("modifiedBy", deletedBy);

        return query.executeUpdate();
    }

}
