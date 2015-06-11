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

import java.math.BigDecimal;
import java.util.List;

import org.savapage.core.dao.helpers.AggregateResult;
import org.savapage.core.jpa.Account;
import org.savapage.core.jpa.Account.AccountTypeEnum;
import org.savapage.core.jpa.AccountTrx;

/**
 *
 * @author Datraverse B.V.
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
        ACCOUNT_TYPE
    }

    /**
     * Empty placeholder for now.
     */
    class ListFilter {

    }

    /**
     *
     * @param filter
     *            The {@link ListFilter}.
     * @return The number of filtered instances.
     */
    long getListCount(final ListFilter filter);

    /**
     *
     * @param filter
     * @param startPosition
     * @param maxResults
     * @param orderBy
     * @param sortAscending
     * @return The list.
     */
    List<Account> getListChunk(final ListFilter filter,
            final Integer startPosition, final Integer maxResults,
            final Field orderBy, final boolean sortAscending);

    /**
     * Finds an active (i.e. not logically deleted)
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
     * @return The number of removed {@link Account} instances.
     */
    int pruneAccounts();

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

}
