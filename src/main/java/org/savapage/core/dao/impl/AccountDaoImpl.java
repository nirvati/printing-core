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

import java.util.List;

import javax.persistence.NoResultException;
import javax.persistence.Query;

import org.savapage.core.dao.AccountDao;
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
    public Account findActiveSharedAccountByName(final String name) {

        final String jpql =
                "SELECT A FROM Account A WHERE A.name = :name"
                        + " AND A.accountType = :accountType"
                        + " AND A.deleted = false";

        final Query query = getEntityManager().createQuery(jpql);

        query.setParameter("name", name);
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

}
