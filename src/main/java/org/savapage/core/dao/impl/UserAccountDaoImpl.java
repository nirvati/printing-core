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

import java.util.List;

import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.savapage.core.dao.UserAccountDao;
import org.savapage.core.jpa.Account;
import org.savapage.core.jpa.User;
import org.savapage.core.jpa.UserAccount;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class UserAccountDaoImpl extends GenericDaoImpl<UserAccount>
        implements UserAccountDao {

    @Override
    protected String getCountQuery() {
        return "SELECT COUNT(T.id) FROM UserAccount T";
    }

    @Override
    public UserAccount findByUserId(final Long id,
            final Account.AccountTypeEnum accountType) {
        /*
         * select s from UserAccount s where s.user.id=:id and
         * s.account.accountType=:accountType
         */

        final CriteriaBuilder criteriaBuilder =
                getEntityManager().getCriteriaBuilder();
        final CriteriaQuery<Object> criteriaQuery =
                criteriaBuilder.createQuery();

        final Root<UserAccount> from = criteriaQuery.from(UserAccount.class);

        final Path<User> pathUser = from.join("user").get("id");
        final Path<Account> pathAccount =
                from.join("account").get("accountType");

        final Predicate predicate1 = criteriaBuilder.equal(pathUser, id);
        final Predicate predicate2 =
                criteriaBuilder.equal(pathAccount, accountType.toString());

        criteriaQuery.where(criteriaBuilder.and(predicate1, predicate2));

        final CriteriaQuery<Object> select = criteriaQuery.select(from);

        final TypedQuery<Object> typedQuery =
                getEntityManager().createQuery(select);

        UserAccount userAccount;

        try {
            userAccount = (UserAccount) typedQuery.getSingleResult();
        } catch (NoResultException e) {
            userAccount = null;
        }

        return userAccount;
    }

    @Override
    public UserAccount findByActiveUserId(final String userId,
            final Account.AccountTypeEnum accountType) {
        /*
         * select s from UserAccount s where s.user.userId=:userId and
         * s.account.accountType=:accountType and s.user.deleted=FALSE
         * s.account.deleted=FALSE
         */

        final CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();

        final CriteriaQuery<Object> criteriaQuery = cb.createQuery();

        final Root<UserAccount> from = criteriaQuery.from(UserAccount.class);

        final Path<User> pathUser = from.join("user").get("userId");
        final Path<Account> pathAccount =
                from.join("account").get("accountType");

        final Path<Boolean> isDeletedUser = from.join("user").get("deleted");
        final Path<Boolean> isDeletedAccount =
                from.join("account").get("deleted");

        final Predicate predicate1 = cb.equal(pathUser, userId);
        final Predicate predicate2 =
                cb.equal(pathAccount, accountType.toString());
        final Predicate predicate3 =
                cb.and(cb.isFalse(isDeletedAccount), cb.isFalse(isDeletedUser));

        criteriaQuery.where(cb.and(predicate1, predicate2, predicate3));

        final CriteriaQuery<Object> select = criteriaQuery.select(from);

        final TypedQuery<Object> typedQuery =
                getEntityManager().createQuery(select);

        UserAccount userAccount;

        try {
            userAccount = (UserAccount) typedQuery.getSingleResult();
        } catch (NoResultException e) {
            userAccount = null;
        }

        return userAccount;
    }

    @Override
    public UserAccount findByAccountId(final Long id) {

        /*
         * select s from UserAccount s where s.account.id=:id
         */
        final CriteriaBuilder criteriaBuilder =
                getEntityManager().getCriteriaBuilder();
        final CriteriaQuery<Object> criteriaQuery =
                criteriaBuilder.createQuery();

        final Root<UserAccount> from = criteriaQuery.from(UserAccount.class);

        final Path<Account> pathAccount = from.join("account").get("id");

        final Predicate predicate = criteriaBuilder.equal(pathAccount, id);
        criteriaQuery.where(predicate);

        final CriteriaQuery<Object> select = criteriaQuery.select(from);

        final TypedQuery<Object> typedQuery =
                getEntityManager().createQuery(select);

        UserAccount userAccount;

        try {
            userAccount = (UserAccount) typedQuery.getSingleResult();
        } catch (NoResultException e) {
            userAccount = null;
        }

        return userAccount;
    }

    @Override
    public List<UserAccount> findByUserId(final Long id) {

        /*
         * select s from UserAccount s where s.user.id=:id
         */
        final CriteriaBuilder criteriaBuilder =
                getEntityManager().getCriteriaBuilder();

        final CriteriaQuery<UserAccount> criteriaQuery =
                criteriaBuilder.createQuery(UserAccount.class);

        final Root<UserAccount> from = criteriaQuery.from(UserAccount.class);

        final Path<User> pathUser = from.join("user").get("id");

        final Predicate predicate = criteriaBuilder.equal(pathUser, id);
        criteriaQuery.where(predicate);

        final CriteriaQuery<UserAccount> select = criteriaQuery.select(from);

        final TypedQuery<UserAccount> typedQuery =
                getEntityManager().createQuery(select);

        return typedQuery.getResultList();
    }

}
