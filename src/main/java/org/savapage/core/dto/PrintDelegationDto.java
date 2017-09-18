/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2017 Datraverse B.V.
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.core.dto;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Print Delegation data.
 *
 * @author Rijk Ravestein
 *
 */
/**
 * @author Rijk Ravestein
 *
 */
@JsonInclude(Include.NON_NULL)
public final class PrintDelegationDto extends AbstractDto {

    public enum DelegatorAccountEnum {
        GROUP, USER, SHARED
    }

    @JsonInclude(Include.NON_NULL)
    public static final class DelegatorAccount {

        @JsonProperty("type")
        private DelegatorAccountEnum accountType;

        @JsonProperty("id")
        private Long accountId;

        /**
         * The number of users.
         */
        private Integer userCount;

        /**
         * The number of copies per user.
         */
        private Integer userCopies;

        public DelegatorAccountEnum getAccountType() {
            return accountType;
        }

        public void setAccountType(DelegatorAccountEnum accountType) {
            this.accountType = accountType;
        }

        public Long getAccountId() {
            return accountId;
        }

        public void setAccountId(Long accountId) {
            this.accountId = accountId;
        }

        /**
         * @return The number of users.
         */
        public Integer getUserCount() {
            return userCount;
        }

        /**
         * @param userCount
         *            The number of users.
         */
        public void setUserCount(Integer userCount) {
            this.userCount = userCount;
        }

        /**
         * @return The number of copies per user.
         */
        public Integer getUserCopies() {
            return userCopies;
        }

        /**
         * @param userCopies
         *            The number of copies per user.
         */
        public void setUserCopies(Integer userCopies) {
            this.userCopies = userCopies;
        }

    }

    private String name;

    /**
     * {@link DelegatorAccount} objects by
     * {@link DelegatorAccount#getAccountId()} for groups on any
     * {@link DelegatorAccountEnum} account.
     */
    private Map<Long, DelegatorAccount> groups;

    /**
     * {@link DelegatorAccount} objects by
     * {@link DelegatorAccount#getAccountId()} for individual users on a
     * {@link DelegatorAccountEnum#USER} account.
     */
    private Map<Long, DelegatorAccount> users;

    /**
     * {@link DelegatorAccount} objects by
     * {@link DelegatorAccount#getAccountId()} for extra copies on a
     * {@link DelegatorAccountEnum#SHARED} account.
     */
    private Map<Long, DelegatorAccount> copies;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return {@link DelegatorAccount} objects by
     *         {@link DelegatorAccount#getAccountId()} for groups on any
     *         {@link DelegatorAccountEnum} account.
     */
    public Map<Long, DelegatorAccount> getGroups() {
        return groups;
    }

    /**
     * @param groups
     *            {@link DelegatorAccount} objects by
     *            {@link DelegatorAccount#getAccountId()} for groups on any
     *            {@link DelegatorAccountEnum} account.
     */
    public void setGroups(Map<Long, DelegatorAccount> groups) {
        this.groups = groups;
    }

    /**
     * @return {@link DelegatorAccount} objects by
     *         {@link DelegatorAccount#getAccountId()} for individual users on a
     *         {@link DelegatorAccountEnum#USER} account.
     */
    public Map<Long, DelegatorAccount> getUsers() {
        return users;
    }

    /**
     * @param users
     *            {@link DelegatorAccount} objects by
     *            {@link DelegatorAccount#getAccountId()} for individual users
     *            on a {@link DelegatorAccountEnum#USER} account.
     */
    public void setUsers(Map<Long, DelegatorAccount> users) {
        this.users = users;
    }

    /**
     * @return {@link DelegatorAccount} objects by
     *         {@link DelegatorAccount#getAccountId()} for extra copies on a
     *         {@link DelegatorAccountEnum#SHARED} account.
     */
    public Map<Long, DelegatorAccount> getCopies() {
        return copies;
    }

    /**
     * @param copies
     *            {@link DelegatorAccount} objects by
     *            {@link DelegatorAccount#getAccountId()} for extra copies on a
     *            {@link DelegatorAccountEnum#SHARED} account.
     */
    public void setCopies(Map<Long, DelegatorAccount> copies) {
        this.copies = copies;
    }

}
