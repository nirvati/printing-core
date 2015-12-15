/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2014 Datraverse B.V.
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.core.dao;

import java.util.Date;
import java.util.List;

import org.savapage.core.jpa.Account;
import org.savapage.core.jpa.DocLog;
import org.savapage.core.jpa.User;

/**
 *
 * @author Datraverse B.V.
 *
 */
public interface UserDao extends GenericDao<User> {

    /**
     * The reserved internal userid for the administrator. This user is NOT part
     * of the database.
     */
    String INTERNAL_ADMIN_USERID = "admin";

    /**
     * Field identifiers used for select and sort.
     */
    enum Field {
        USERID, EMAIL
    }

    /**
     *
     */
    class ListFilter {

        private Long userGroupId;
        private String containingIdText;
        private String containingNameText;
        private String containingEmailText;
        private Boolean internal;
        private Boolean admin;
        private Boolean person;
        private Boolean disabled;
        private Boolean deleted;

        public Long getUserGroupId() {
            return userGroupId;
        }

        public void setUserGroupId(Long userGroupId) {
            this.userGroupId = userGroupId;
        }

        public String getContainingIdText() {
            return containingIdText;
        }

        public void setContainingIdText(String containingIdText) {
            this.containingIdText = containingIdText;
        }

        public String getContainingNameText() {
            return containingNameText;
        }

        public void setContainingNameText(String containingNameText) {
            this.containingNameText = containingNameText;
        }

        public String getContainingEmailText() {
            return containingEmailText;
        }

        public void setContainingEmailText(String containingEmailText) {
            this.containingEmailText = containingEmailText;
        }

        public Boolean getInternal() {
            return internal;
        }

        public void setInternal(Boolean internal) {
            this.internal = internal;
        }

        public Boolean getAdmin() {
            return admin;
        }

        public void setAdmin(Boolean admin) {
            this.admin = admin;
        }

        public Boolean getPerson() {
            return person;
        }

        public void setPerson(Boolean person) {
            this.person = person;
        }

        public Boolean getDisabled() {
            return disabled;
        }

        public void setDisabled(Boolean disabled) {
            this.disabled = disabled;
        }

        public Boolean getDeleted() {
            return deleted;
        }

        public void setDeleted(Boolean deleted) {
            this.deleted = deleted;
        }

    }

    /**
     *
     * @param filter
     * @return
     */
    long getListCount(ListFilter filter);

    /**
     * <p>
     * BEWARE that, in case of an email filter, the {@link User#getEmails()} is
     * filled with the selected UserEmail objects only!
     * </p>
     *
     * @param filter
     * @param startPosition
     * @param maxResults
     * @param orderBy
     * @param sortAscending
     * @return
     */
    List<User> getListChunk(ListFilter filter, Integer startPosition,
            Integer maxResults, Field orderBy, boolean sortAscending);

    /**
     * Finds the active {@link User} and locks pessimistic. See
     * {@link #findActiveUserByUserId(String)}.
     *
     * @param userId
     *            The unique user id.
     * @return {@code null} when not found (or logically deleted).
     */
    User lockByUserId(String userId);

    /**
     * Finds an active (i.e. not logically deleted) {@link User} by user id,
     * when not found (or logically deleted) {@code null} is returned.
     *
     * @param userId
     *            The unique user id of the user.
     * @return The {@link User} instance, or {@code null} when not found (or
     *         logically deleted).
     */
    User findActiveUserByUserId(String userId);

    /**
     * Finds an active (i.e. not logically deleted) {@link User} by user id,
     * when not found (or logically deleted) the user is persisted as active
     * user into the database.
     * <p>
     * NOTE: when {@link User#getUserId()} is a reserved name like 'admin',
     * {@code null} is returned.
     * </p>
     *
     * @param userId
     *            The unique user id of the user.
     * @return The {@link User} instance or {@code null} when user was NOT
     *         inserted.
     */
    User findActiveUserByUserIdInsert(String userId);

    /**
     * Finds an active (i.e. not logically deleted) {@link User} by user id,
     * when not found (or logically deleted) the user is persisted as active
     * user into the database.
     * <p>
     * NOTE: when {@link User#getUserId()} is a reserved name like 'admin',
     * {@code null} is returned.
     * </p>
     *
     * @param user
     *            The {@link User} containing the unique user id.
     * @param insertDate
     *            The date.
     * @param insertedBy
     *            The actor.
     * @return The {@link User} instance or {@code null} when user was NOT
     *         inserted.
     */
    User findActiveUserByUserIdInsert(User user, Date insertDate,
            String insertedBy);

    /**
     * Finds a User by primary key of his {@link Account}.
     *
     * @param accountId
     *            Primary key of {@link Account}.
     * @return {@code null} when not found.
     */
    User findByAccount(Long accountId);

    /**
     * Resets the jobs, bytes and sheets totals to zero for all {@link User}
     * instances.
     *
     * @param resetDate
     *            The reset date.
     * @param resetBy
     *            The actor.
     */
    void resetTotals(Date resetDate, String resetBy);

    /**
     * Removes (cascade delete) logically deleted {@link User} objects, who do
     * not have any related {@link DocLog}.
     *
     * @return The number of removed users.
     */
    int pruneUsers();

    /**
     * Counts the number of active users.
     * <p>
     * NOTE: logically deleted users are excluded from the count.
     * </p>
     *
     * @return the number of active users.
     */
    long countActiveUsers();

}