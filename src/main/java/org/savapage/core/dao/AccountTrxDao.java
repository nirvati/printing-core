/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2017 Datraverse B.V.
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

import org.savapage.core.dao.enums.AccountTrxTypeEnum;
import org.savapage.core.jpa.Account.AccountTypeEnum;
import org.savapage.core.jpa.AccountTrx;
import org.savapage.core.jpa.AccountVoucher;
import org.savapage.core.jpa.User;

/**
 *
 * @author Rijk Ravestein
 *
 */
public interface AccountTrxDao extends GenericDao<AccountTrx> {

    /**
     * Field identifiers used for select and sort.
     */
    enum Field {
        /**
         * Transaction date.
         */
        TRX_DATE,

        /**
         * Transaction type.
         */
        TRX_TYPE
    }

    /**
     *
     */
    class ListFilter {

        private Long userId;
        private Long accountId;
        private Long docLogId;

        private AccountTypeEnum accountType;
        private AccountTrxTypeEnum trxType;
        private Date dateFrom;
        private Date dateTo;
        private String containingCommentText;

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public Long getAccountId() {
            return accountId;
        }

        public void setAccountId(Long accountId) {
            this.accountId = accountId;
        }

        public Long getDocLogId() {
            return docLogId;
        }

        public void setDocLogId(Long docLogId) {
            this.docLogId = docLogId;
        }

        public AccountTypeEnum getAccountType() {
            return accountType;
        }

        public void setAccountType(AccountTypeEnum accountType) {
            this.accountType = accountType;
        }

        public AccountTrxTypeEnum getTrxType() {
            return trxType;
        }

        public void setTrxType(AccountTrxTypeEnum trxType) {
            this.trxType = trxType;
        }

        public Date getDateFrom() {
            return dateFrom;
        }

        public void setDateFrom(Date dateFrom) {
            this.dateFrom = dateFrom;
        }

        public Date getDateTo() {
            return dateTo;
        }

        public void setDateTo(Date dateTo) {
            this.dateTo = dateTo;
        }

        public String getContainingCommentText() {
            return containingCommentText;
        }

        public void setContainingCommentText(String containingCommentText) {
            this.containingCommentText = containingCommentText;
        }
    }

    /**
     * Finds a {@link AccountTrx} external id.
     *
     * @param extId
     *            The external id. The {@link User}.
     * @return The {@link AccountTrx} or {@code null} when not found.
     */
    AccountTrx findByExtId(String extId);

    /**
     * Finds a list of {@link AccountTrx} instances with the same external
     * method address. See {@link AccountTrx#getExtMethodAddress()}.
     *
     * @param address
     *            The address.
     * @return The list.
     */
    List<AccountTrx> findByExtMethodAddress(String address);

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
    List<AccountTrx> getListChunk(final ListFilter filter,
            final Integer startPosition, final Integer maxResults,
            final Field orderBy, final boolean sortAscending);

    /**
     * Removes {@link AccountTrx} instances dating from daysBackInTime and
     * older.
     * <p>
     * Note: For each removed {@link AccountTrx} any associated
     * {@link AccountVoucher} instance is deleted by cascade.
     * </p>
     *
     * @param dateBackInTime
     *            The transaction date criterion.
     * @return The number of deleted instances.
     */
    int cleanHistory(Date dateBackInTime);

}
