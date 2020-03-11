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

import java.util.Date;
import java.util.List;

import org.savapage.core.jpa.AccountVoucher;

/**
 *
 * @author Datraverse B.V.
 *
 */
public interface AccountVoucherDao extends GenericDao<AccountVoucher> {

    /**
     * The Voucher type.
     * <p>
     * Use {@link DbVoucherType#toString()} for
     * {@link AccountVoucher#setVoucherType(String)}.
     * </p>
     */
    enum DbVoucherType {
        /**
         * A card.
         */
        CARD,
        /**
         * A transaction.
         */
        TRX
    }

    /**
     * Field identifiers used for select and sort.
     */
    enum Field {
        NUMBER, VALUE, USER, DATE_USED, DATE_EXPIRED
    }

    /**
     *
     */
    class ListFilter {

        private String batch = null;
        private String number = null;
        private String userId = null;

        private Boolean used = null;
        private Boolean expired = null;

        private DbVoucherType voucherType;
        private Date dateFrom;
        private Date dateTo;
        private Date dateNow;

        public DbVoucherType getVoucherType() {
            return voucherType;
        }

        public void setVoucherType(DbVoucherType voucherType) {
            this.voucherType = voucherType;
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

        public Date getDateNow() {
            return dateNow;
        }

        public void setDateNow(Date dateNow) {
            this.dateNow = dateNow;
        }

        public String getBatch() {
            return batch;
        }

        public void setBatch(String batch) {
            this.batch = batch;
        }

        public String getNumber() {
            return number;
        }

        public void setNumber(String number) {
            this.number = number;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public Boolean getUsed() {
            return used;
        }

        public void setUsed(Boolean used) {
            this.used = used;
        }

        public Boolean getExpired() {
            return expired;
        }

        public void setExpired(Boolean expired) {
            this.expired = expired;
        }

    }

    /**
     *
     * @param filter
     * @return
     */
    long getListCount(final ListFilter filter);

    /**
     *
     * @param filter
     * @param startPosition
     * @param maxResults
     * @param orderBy
     * @param sortAscending
     * @return
     */
    List<AccountVoucher> getListChunk(final ListFilter filter,
            final Integer startPosition, final Integer maxResults,
            final Field orderBy, final boolean sortAscending);

    /**
     * Counts the number of {@link AccountVoucher} instances in a batch.
     *
     * @param batch
     *            The unique ID of the batch.
     * @return {@code zero} when no cards are found in the batch, meaning the
     *         batch never existed or was deleted.
     */
    long countVouchersInBatch(final String batch);

    /**
     * Returns the set of batch IDs currently in use.
     *
     * @return
     */
    List<String> getBatches();

    /**
     * Deletes all vouchers with an {@link AccountVoucher#getExpiryDate()} LTE
     * the expiryDay parameter.
     * <p>
     * Note that expiry dates are handled as DAY dates (with hours and minutes
     * EQ zero).
     * </p>
     *
     * @param expiryDay
     *            The expiry day criterion.
     * @return
     */
    int deleteExpired(final Date expiryDay);

    /**
     * Deletes all vouchers in a batch which have not yet been redeemed.
     *
     * @param batch
     *            The unique ID of the batch.
     * @return The number of vouchers that were deleted.
     */
    int deleteBatch(final String batch);

    /**
     * Sets the expiry date of all vouchers in a batch which have not yet been
     * redeemed.
     *
     * @param batch
     * @return
     */
    int expireBatch(final String batch, final Date expiryDay);

    /**
     *
     * @param cardNumber
     * @return {@code null} when not found.
     */
    AccountVoucher findByCardNumber(final String cardNumber);

}
