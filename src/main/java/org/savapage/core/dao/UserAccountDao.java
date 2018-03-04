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

import java.util.List;

import org.savapage.core.jpa.Account;
import org.savapage.core.jpa.UserAccount;

/**
 *
 * @author Rijk Ravestein
 *
 */
public interface UserAccountDao extends GenericDao<UserAccount> {

    /**
     * Finds the {@link UserAccount} for a {@link User}.
     *
     * @param id
     *            The primary key of the {@link User}.
     * @param accountType
     *            Type of account.
     * @return {@code null} when not found.
     */
    UserAccount findByUserId(Long id, Account.AccountTypeEnum accountType);

    /**
     * Gets list of {@link UserAccount} objects for a {@link User}.
     *
     * @param id
     *            The primary key of the {@link User}.
     * @return The list of objects.
     */
    List<UserAccount> findByUserId(Long id);

    /**
     * Finds the {@link UserAccount} for an active (non-deleted) {@link User}
     * and active (non-deleted) {@link Account}.
     *
     * @param userId
     *            The unique user id.
     * @param accountType
     *            Type of account.
     * @return {@code null} when not found.
     */
    UserAccount findByActiveUserId(String userId,
            Account.AccountTypeEnum accountType);

    /**
     * Finds the {@link UserAccount} for a {@link Account}.
     *
     * @param id
     *            The id of the account, see {@link Account#getId()}.
     * @return {@code null} when not found.
     */
    UserAccount findByAccountId(Long id);

}
