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

import org.savapage.core.SpException;
import org.savapage.core.dao.UserDao;
import org.savapage.core.dao.enums.ReservedUserGroupEnum;
import org.savapage.core.dao.enums.UserGroupAttrEnum;
import org.savapage.core.dao.helpers.DaoBatchCommitter;
import org.savapage.core.jpa.AccountTrx;
import org.savapage.core.jpa.User;
import org.savapage.core.jpa.tools.DbSimpleEntity;
import org.savapage.core.services.ServiceContext;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class UserDaoImpl extends GenericDaoImpl<User> implements UserDao {

    /**
     *
     */
    public UserDaoImpl() {
    }

    @Override
    protected String getCountQuery() {
        return "SELECT COUNT(T.id) FROM User T";
    }

    @Override
    public User findByAccount(final Long accountId) {

        final String jpql = "SELECT UA.user FROM UserAccount UA "
                + "JOIN UA.account A WHERE A.id = :accountId";

        final Query query = getEntityManager().createQuery(jpql.toString());

        query.setParameter("accountId", accountId);

        User user;

        try {
            user = (User) query.getSingleResult();
        } catch (NoResultException e) {
            user = null;
        }
        return user;
    }

    @Override
    public User lockByUserId(final String userId) {
        User user = findActiveUserByUserId(userId);
        if (user != null) {
            user = lock(user.getId());
        }
        return user;
    }

    /**
     *
     * @param jpql
     */
    private void appendAclJoin(final StringBuilder jpql) {

        jpql.append(" LEFT JOIN UserAttr UA ON UA.user = U "
                + "AND UA.name = :roleName");

        jpql.append(" LEFT JOIN UserGroupMember UGM ON UGM.user = U");

        jpql.append(" LEFT JOIN UserGroupAttr UGA ON UGA.userGroup = UGM.group "
                + "AND UGA.name = :roleNameGroup");
    }

    @Override
    public long getListCount(final ListFilter filter) {

        final StringBuilder jpql =
                new StringBuilder(JPSQL_STRINGBUILDER_CAPACITY);

        jpql.append("SELECT COUNT(*) FROM User X WHERE X.id IN "
                + "(SELECT DISTINCT U.id FROM ");

        if (filter.getUserGroupId() == null) {
            jpql.append("User U");
        } else {
            jpql.append("UserGroupMember M JOIN M.user U JOIN M.group G");
        }

        if (filter.getAclFilter() != null) {
            appendAclJoin(jpql);
        }

        if (filter.getContainingEmailText() != null) {
            jpql.append(" JOIN U.emails E");
        }

        applyListFilter(jpql, filter);

        jpql.append(")");

        final Query query = createListQuery(jpql, filter);
        final Number countResult = (Number) query.getSingleResult();

        return countResult.longValue();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<User> getListChunk(final ListFilter filter,
            final Integer startPosition, final Integer maxResults,
            final Field orderBy, final boolean sortAscending) {

        final StringBuilder jpql =
                new StringBuilder(JPSQL_STRINGBUILDER_CAPACITY);

        /*
         * NOTE: We need a DISTINCT to prevent a User/UserEmail cartesian
         * product.
         */
        jpql.append("SELECT DISTINCT U FROM ");

        if (filter.getUserGroupId() == null) {
            jpql.append("User U");
        } else {
            jpql.append("UserGroupMember M JOIN M.user U JOIN M.group G");
        }

        if (filter.getAclFilter() != null) {
            appendAclJoin(jpql);
        }

        if (filter.getContainingEmailText() == null) {
            /*
             * Since when we do NOT filter on email address we ALSO want the
             * Users WITHOUT an email address.
             */
            jpql.append(" LEFT");
        }

        /*
         * Since the UserEmail is JPA annotated as LAZY fetch, we ALWAYS want to
         * explicitly FETCH the emails, so the caller of the method has access
         * to the emails out of JPA session context.
         *
         * BEWARE that, in case of an email filter the emails list is filled
         * with the selected UserEmail objects only !!!!
         */
        jpql.append(" JOIN FETCH U.emails E");

        //
        applyListFilter(jpql, filter);

        //
        jpql.append(" ORDER BY ");

        if (orderBy == Field.USERID) {
            jpql.append("U.userId");
        } else {
            jpql.append("E.address");
        }

        if (!sortAscending) {
            jpql.append(" DESC");
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
     *            The StringBuilder to append to.
     * @param filter
     *            The filter.
     */
    private void applyListFilter(final StringBuilder jpql,
            final ListFilter filter) {

        StringBuilder where = new StringBuilder();

        int nWhere = 0;

        if (filter.getUserGroupId() != null) {
            nWhere++;
            where.append(" G.id = :userGroupId");
        }

        if (filter.getContainingIdText() != null) {
            if (nWhere > 0) {
                where.append(" AND");
            }
            nWhere++;
            where.append(" lower(U.userId) like :containingIdText");
        }

        if (filter.getContainingNameText() != null) {
            if (nWhere > 0) {
                where.append(" AND");
            }
            nWhere++;
            where.append(" lower(U.fullName) like :containingNameText");
        }

        if (filter.getContainingEmailText() != null) {
            if (nWhere > 0) {
                where.append(" AND");
            }
            nWhere++;
            where.append(" lower(E.address) like :containingEmailText");
        }

        if (filter.getInternal() != null) {
            if (nWhere > 0) {
                where.append(" AND");
            }
            nWhere++;
            where.append(" U.internal = :selInternal");
        }

        if (filter.getAdmin() != null) {
            if (nWhere > 0) {
                where.append(" AND");
            }
            nWhere++;
            where.append(" U.admin = :selAdmin");
        }

        if (filter.getPerson() != null) {
            if (nWhere > 0) {
                where.append(" AND");
            }
            nWhere++;
            where.append(" U.person = :selPerson");
        }

        if (filter.getDisabled() != null) {
            if (nWhere > 0) {
                where.append(" AND");
            }
            nWhere++;
            where.append(" U.disabledPrintIn = :selDisabled");
        }

        if (filter.getDeleted() != null) {
            if (nWhere > 0) {
                where.append(" AND");
            }
            nWhere++;
            where.append(" U.deleted = :selDeleted");
        }

        if (filter.getAclFilter() != null) {
            if (nWhere > 0) {
                where.append(" AND");
            }
            nWhere++;

            where.append("(");

            if (filter.getAclFilter().isAclUserExternal()
                    || filter.getAclFilter().isAclUserInternal()) {

                where.append("(" + "UA.name = null AND UGA.name = null");

                if (filter.getAclFilter().isAclUserExternal()
                        && filter.getAclFilter().isAclUserInternal()) {
                    // All users are authorized by default.

                } else if (filter.getAclFilter().isAclUserExternal()) {
                    // Only external users are authorized by default.
                    where.append(" AND U.internal = false");
                } else if (filter.getAclFilter().isAclUserInternal()) {
                    // Only internal users are authorized by default.
                    where.append(" AND U.internal = true");
                }

                where.append(") OR");

            }

            where.append(" (UA.name != null AND UA.value LIKE :jsonRole"
                    + " AND UA.value LIKE :jsonRoleValue)");

            where.append(" OR (UA.name = null AND UGA.value LIKE :jsonRole"
                    + " AND UGA.value LIKE :jsonRoleValue)");

            where.append(" OR (UA.name != null AND UA.value NOT LIKE :jsonRole"
                    + " AND UGA.value LIKE :jsonRoleValue)");

            where.append(")");
        }

        if (nWhere > 0) {
            jpql.append(" WHERE ").append(where.toString());
        }

    }

    /**
     * Creates the List Query and sets the filter parameters.
     *
     * @param jpql
     * @param filter
     * @return The query.
     */
    private Query createListQuery(final StringBuilder jpql,
            final ListFilter filter) {

        final Query query = getEntityManager().createQuery(jpql.toString());

        if (filter.getAclFilter() != null) {

            query.setParameter("roleName",
                    UserGroupAttrEnum.ACL_ROLES.getName());

            query.setParameter("roleNameGroup",
                    UserGroupAttrEnum.ACL_ROLES.getName());

            /*
             * INVARIANT: JSON string does NOT contain whitespace.
             */
            final String jsonRole = String.format("\"%s\"",
                    filter.getAclFilter().getAclRole().toString());

            query.setParameter("jsonRole", String.format("%%%s%%", jsonRole));
            query.setParameter("jsonRoleValue", String.format("%%%s:%s%%",
                    jsonRole, Boolean.TRUE.toString()));

        }

        if (filter.getUserGroupId() != null) {
            query.setParameter("userGroupId", filter.getUserGroupId());
        }

        if (filter.getContainingIdText() != null) {
            query.setParameter("containingIdText", String.format("%%%s%%",
                    filter.getContainingIdText().toLowerCase()));
        }
        if (filter.getContainingNameText() != null) {
            query.setParameter("containingNameText", String.format("%%%s%%",
                    filter.getContainingNameText().toLowerCase()));
        }
        if (filter.getContainingEmailText() != null) {
            query.setParameter("containingEmailText", String.format("%%%s%%",
                    filter.getContainingEmailText().toLowerCase()));
        }
        if (filter.getInternal() != null) {
            query.setParameter("selInternal", filter.getInternal());
        }
        if (filter.getAdmin() != null) {
            query.setParameter("selAdmin", filter.getAdmin());
        }
        if (filter.getPerson() != null) {
            query.setParameter("selPerson", filter.getPerson());
        }
        if (filter.getDisabled() != null) {
            query.setParameter("selDisabled", filter.getDisabled());
        }
        if (filter.getDeleted() != null) {
            query.setParameter("selDeleted", filter.getDeleted());
        }

        return query;
    }

    @Override
    public void resetTotals(final Date resetDate, final String resetBy) {

        final String jpql = "UPDATE User U SET " + "U.numberOfPrintInJobs = 0, "
                + "U.numberOfPrintInPages = 0, "
                + "U.numberOfPrintInBytes = 0, " + "U.numberOfPdfOutJobs = 0, "
                + "U.numberOfPdfOutPages = 0, " + "U.numberOfPdfOutBytes = 0,"
                + "U.numberOfPrintOutJobs = 0, "
                + "U.numberOfPrintOutPages = 0, "
                + "U.numberOfPrintOutSheets = 0, "
                + "U.numberOfPrintOutEsu = 0, " + "U.numberOfPrintOutBytes = 0,"
                + "U.resetDate = :resetDate, U.resetBy = :resetBy";

        final Query query = getEntityManager().createQuery(jpql);

        query.setParameter("resetDate", resetDate);
        query.setParameter("resetBy", resetBy);

        query.executeUpdate();
    }

    /**
     * Counts the number of user {@link AccountTrx} instances.
     *
     * @param userDbKey
     *            The database primary key of a {@link User}.
     * @return The count.
     */
    private long getUserAccountTrxCount(final Long userDbKey) {
        final String jpql = "SELECT COUNT(TRX.id) FROM "
                + DbSimpleEntity.USER_ACCOUNT + " UA" + " JOIN "
                + DbSimpleEntity.ACCOUNT_TRX + " TRX "
                + "ON TRX.account = UA.account" + " WHERE UA.user = :user";

        final Query query = getEntityManager().createQuery(jpql);
        query.setParameter("user", userDbKey);

        final Number countResult = (Number) query.getSingleResult();
        return countResult.longValue();
    }

    @Override
    public int pruneUsers(final DaoBatchCommitter batchCommitter) {
        /*
         * NOTE: We do NOT use bulk delete with JPQL since we want the option to
         * roll back the deletions as part of a transaction, and we want to use
         * CASCADE deletion.
         *
         * Therefore we use the remove() method in EntityManager to delete
         * individual records instead (so CASCADED deletes are triggered).
         */
        int nDeleted = 0;

        final String jpql = "SELECT U.id FROM User U WHERE U.deleted = true "
                + "AND U.docLog IS EMPTY";

        final Query query = getEntityManager().createQuery(jpql);

        @SuppressWarnings("unchecked")
        final List<Long> list = query.getResultList();

        for (final Long id : list) {
            /*
             * Do NOT delete a user when AccountTrx are present. Reason: when
             * printing was charged via Delegated Print, no DocLog are present
             * for this user. Or, user could have redeemed a voucher, and never
             * have printed. Ergo: When AccountTrx are completely cleaned, user
             * can be deleted.
             */
            if (getUserAccountTrxCount(id) > 0) {
                continue;
            }
            this.delete(this.findById(id));
            nDeleted++;
            batchCommitter.increment();
        }
        return nDeleted;
    }

    @Override
    public long countActiveUsers() {
        return countActiveUsers(ReservedUserGroupEnum.ALL);
    }

    @Override
    public long countActiveUsers(final ReservedUserGroupEnum userGroupEnum) {

        final StringBuilder psql = new StringBuilder();

        psql.append(
                "SELECT COUNT(U.id) FROM User U" + " WHERE U.deleted = false");

        switch (userGroupEnum) {
        case ALL:
            // noop
            break;
        case EXTERNAL:
            psql.append(" AND U.internal = false");
            break;
        case INTERNAL:
            psql.append(" AND U.internal = true");
            break;
        default:
            throw new SpException(String.format("%s.%s not handled",
                    ReservedUserGroupEnum.class.getSimpleName(),
                    userGroupEnum.toString()));
        }
        final Query query = getEntityManager().createQuery(psql.toString());
        final Number countResult = (Number) query.getSingleResult();
        return countResult.longValue();
    }

    @Override
    public User findActiveUserById(final Long id) {
        final User user = this.findById(id);
        if (user.getDeleted()) {
            return null;
        }
        return user;
    }

    @Override
    public User findActiveUserByUserId(final String userId) {
        return readUser(userId, null, null, null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<User> checkActiveUserByUserId(final String userId) {
        final Query query = this.createActiveUserQuery(userId);
        return query.getResultList();
    }

    @Override
    public User findActiveUserByUserIdInsert(final String userId) {

        final User userToInsert = new User();
        userToInsert.setUserId(userId);

        return findActiveUserByUserIdInsert(userToInsert,
                ServiceContext.getTransactionDate(), ServiceContext.getActor());
    }

    @Override
    public User findActiveUserByUserIdInsert(final User user,
            final Date insertDate, final String insertedBy) {

        if (user.getUserId().equals(INTERNAL_ADMIN_USERID)) {
            return null;
        }

        return readUser(user.getUserId(), user, insertDate, insertedBy);
    }

    /**
     * @param userId
     *            The unique name of the user.
     * @return A {@link Query} to find the active user(s) by unique name.
     */
    private Query createActiveUserQuery(final String userId) {

        final String jpql = "SELECT U FROM User U WHERE U.userId = :userId "
                + "AND U.deleted = false";

        final Query query = getEntityManager().createQuery(jpql);
        query.setParameter("userId", userId);
        return query;
    }

    /**
     * Reads the user from database, when not found (or logically deleted) the
     * value of userToInsert determines how to proceed.
     *
     * @param userId
     *            The unique name of the user.
     * @param userToInsert
     *            When NOT {@code null}, ad hoc insert of this user instance
     *            into the database when userId does not exist.
     * @param insertDate
     *            The date.
     * @param insertedBy
     *            The actor.
     * @return The user instance from the database, or null when not found (or
     *         logically deleted).
     */
    private User readUser(final String userId, final User userToInsert,
            final Date insertDate, final String insertedBy) {

        final boolean lazyInsertRow = userToInsert != null;

        final Query query = this.createActiveUserQuery(userId);

        User user = null;

        try {

            user = (User) query.getSingleResult();

        } catch (NoResultException e) {

            if (lazyInsertRow) {

                user = userToInsert;

                user.setCreatedBy(insertedBy);
                user.setCreatedDate(insertDate);

                if (user.getExternalUserName() == null) {
                    user.setExternalUserName(user.getUserId());
                }

                this.create(user);
            }
        }
        return user;
    }

}
