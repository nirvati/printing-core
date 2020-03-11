/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2020 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: 2011-2020 Datraverse B.V. <info@datraverse.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
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

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.savapage.core.dao.helpers.AggregateResult;
import org.savapage.core.dao.helpers.DaoBatchCommitter;
import org.savapage.core.jpa.Account;
import org.savapage.core.jpa.Account.AccountTypeEnum;
import org.savapage.core.jpa.AccountTrx;

/**
 *
 * @author Rijk Ravestein
 *
 */
public interface AccountDao extends GenericDao<Account> {

    /**
     * Field identifiers used for select and sort.
     */
    enum Field {

        /**
         * Account type.
         */
        ACCOUNT_TYPE,
        /**
         * The account name.
         */
        NAME
    }

    /**
     * Empty placeholder for now.
     */
    class ListFilter {

        private AccountTypeEnum accountType;
        private AccountTypeEnum accountTypeExtra;

        private String containingNameText;
        private Boolean deleted;

        public AccountTypeEnum getAccountType() {
            return accountType;
        }

        public void setAccountType(AccountTypeEnum accountType) {
            this.accountType = accountType;
        }

        public AccountTypeEnum getAccountTypeExtra() {
            return accountTypeExtra;
        }

        public void setAccountTypeExtra(AccountTypeEnum accountTypeExtra) {
            this.accountTypeExtra = accountTypeExtra;
        }

        public String getContainingNameText() {
            return containingNameText;
        }

        public void setContainingNameText(String containingNameText) {
            this.containingNameText = containingNameText;
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
     *            The {@link ListFilter}.
     * @return The number of filtered instances.
     */
    long getListCount(ListFilter filter);

    /**
     *
     * @param filter
     * @param startPosition
     * @param maxResults
     * @param orderBy
     * @param sortAscending
     * @return The list.
     */
    List<Account> getListChunk(ListFilter filter, Integer startPosition,
            Integer maxResults, Field orderBy, boolean sortAscending);

    /**
     * Finds the active (i.e. not logically deleted)
     * {@link AccountTypeEnum#SHARED} top-level {@link Account} by its unique
     * name.
     * <p>
     * Note: The name might not be unique in the database as such, but there
     * MUST only be one (1) <i>active</i> instance with that name.
     * </p>
     *
     * @param name
     *            The unique active account name.
     * @return The {@link Account} instance or {@code null} when not found.
     */
    Account findActiveSharedAccountByName(String name);

    /**
     * Finds the active (i.e. not logically deleted) top-level {@link Account}
     * of {@link AccountTypeEnum} by its unique name.
     * <p>
     * Note: The name might not be unique in the database as such, but there
     * MUST only be one (1) <i>active</i> instance with that name.
     * </p>
     *
     * @param name
     *            The unique active account name.
     * @param accountType
     *            The {@link AccountTypeEnum}.
     * @return The {@link Account} instance or {@code null} when not found.
     */
    Account findActiveAccountByName(String name, AccountTypeEnum accountType);

    /**
     * Finds an active (i.e. not logically deleted)
     * {@link AccountTypeEnum#SHARED} child {@link Account} by its unique name.
     *
     * @param parentId
     *            The primary key of the parent {@link Account}.
     * @param name
     *            The unique active account name.
     * @return The {@link Account} instance or {@code null} when not found.
     */
    Account findActiveSharedChildAccountByName(Long parentId, String name);

    /**
     * Creates an active {@link Account} from a template, using the unique
     * account name.
     *
     * @param accountName
     *            The unique active name of the account
     * @param accountTemplate
     *            The {@link Account} template.
     * @return The newly created {@link Account}.
     */
    Account createFromTemplate(String accountName, Account accountTemplate);

    /**
     * Removes {@link Account} instances (cascade delete) that are
     * <i>logically</i> deleted, and which do <i>not</i> have any related
     * {@link AccountTrx}.
     *
     * @param batchCommitter
     *            The {@link DaoBatchCommitter}.
     * @return The number of removed {@link Account} instances.
     */
    int pruneAccounts(DaoBatchCommitter batchCommitter);

    /**
     * Gets balance statistics.
     *
     * @param userAccounts
     *            If {@code true} user accounts are totaled, if {@code false}
     *            shared accounts are totaled.
     * @param debit
     *            If {@code true} balances GT zero are totaled, if {@code false}
     *            balances LT zero are totaled.
     * @return The {@link AggregateResult}.
     */
    AggregateResult<BigDecimal> getBalanceStats(boolean userAccounts,
            boolean debit);

    /**
     *
     * @param parentId
     *            The primary key of the parent {@link Account}.
     * @return The number of sub accounts of a parent.
     */
    long countSubAccounts(Long parentId);

    /**
     * @param parentId
     *            The primary key of the parent {@link Account}.
     * @return The sub accounts of a parent.
     */
    List<Account> getSubAccounts(Long parentId);

    /**
     *
     * @param parentId
     *            The primary key of the parent {@link Account}.
     * @param deletedDate
     *            The delete date.
     * @param deletedBy
     *            The delete actor.
     * @return The number of sub accounts that were updated.
     */
    int logicalDeleteSubAccounts(Long parentId, Date deletedDate,
            String deletedBy);

    /**
     * Sets the logical delete attributes of an {@link Account} (no database
     * update is performed).
     *
     * @param account
     *            The {@link Account}.
     * @param deletedDate
     *            The delete date.
     * @param deletedBy
     *            The delete actor.
     * @param deletedBy
     */
    void setLogicalDelete(Account account, Date deletedDate, String deletedBy);
}
