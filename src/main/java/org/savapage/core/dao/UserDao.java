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
package org.savapage.core.dao;

import java.util.Date;
import java.util.List;

import org.savapage.core.dao.enums.ACLRoleEnum;
import org.savapage.core.dao.enums.ReservedUserGroupEnum;
import org.savapage.core.dao.helpers.DaoBatchCommitter;
import org.savapage.core.jpa.Account;
import org.savapage.core.jpa.DocLog;
import org.savapage.core.jpa.User;
import org.savapage.core.jpa.UserAttr;
import org.savapage.core.jpa.UserGroupAttr;

/**
 *
 * @author Rijk Ravestein
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

    /** */
    class ACLFilter {

        private ACLRoleEnum aclRole;
        private boolean aclUserInternal;
        private boolean aclUserExternal;

        public ACLRoleEnum getAclRole() {
            return aclRole;
        }

        public void setAclRole(ACLRoleEnum aclRole) {
            this.aclRole = aclRole;
        }

        public boolean isAclUserInternal() {
            return aclUserInternal;
        }

        public void setAclUserInternal(boolean aclUserInternal) {
            this.aclUserInternal = aclUserInternal;
        }

        public boolean isAclUserExternal() {
            return aclUserExternal;
        }

        public void setAclUserExternal(boolean aclUserExternal) {
            this.aclUserExternal = aclUserExternal;
        }
    }

    /** */
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

        /**
         * The {@link ACLRoleEnum} as present in ({@link UserAttr} or in any
         * {@link UserGroupAttr} where user is member of.
         */
        private ACLFilter aclFilter;

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

        public ACLFilter getAclFilter() {
            return aclFilter;
        }

        public void setAclFilter(ACLFilter aclFilter) {
            this.aclFilter = aclFilter;
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
     *            The filter.
     * @param startPosition
     *            The zero-based start position of the chunk related to the
     *            total number of rows. If {@code null} the chunk starts with
     *            the first row.
     * @param maxResults
     *            The maximum number of rows in the chunk. If {@code null}, then
     *            ALL (remaining rows) are returned.
     * @param orderBy
     *            The sort field.
     * @param sortAscending
     *            {@code true} when sorted ascending.
     * @return The chunk.
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
     * Finds an active (i.e. not logically deleted) {@link User} by id, when not
     * found (or logically deleted) {@code null} is returned.
     *
     * @param id
     *            The primary id of the user.
     * @return The {@link User} instance, or {@code null} when not found (or
     *         logically deleted).
     */
    User findActiveUserById(Long id);

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
     * Checks the active (i.e. not logically deleted) {@link User} by user id.
     * When not found (or logically deleted) an empty list is returned.
     * <p>
     * NOTE: A returned list with more than one (1) element signals an
     * <b>inconsistent</b> state.
     * </p>
     *
     * @param userId
     *            The unique user id.
     * @return The list of active users found.
     */
    List<User> checkActiveUserByUserId(final String userId);

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
     * @param batchCommitter
     *            The {@link DaoBatchCommitter}.
     *
     * @return The number of removed users.
     */
    int pruneUsers(DaoBatchCommitter batchCommitter);

    /**
     * Counts the number of active users.
     * <p>
     * NOTE: logically deleted users are excluded from the count.
     * </p>
     *
     * @return the number of active users.
     */
    long countActiveUsers();

    /**
     * Counts the number of active users in a {@link ReservedUserGroupEnum}.
     * <p>
     * NOTE: logically deleted users are excluded from the count.
     * </p>
     *
     * @param userGroupEnum
     *            The {@link ReservedUserGroupEnum}.
     * @return the number of active users.
     */
    long countActiveUsers(ReservedUserGroupEnum userGroupEnum);

}