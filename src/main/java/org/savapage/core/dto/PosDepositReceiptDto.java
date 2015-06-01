/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2014 Datraverse B.V.
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.core.dto;

import org.savapage.core.jpa.AccountTrx;
import org.savapage.core.jpa.PosPurchase;

/**
 * Information for an {@link PosPurchase} deposit.
 *
 * @author Datraverse B.V.
 *
 */
public class PosDepositReceiptDto extends PosDepositDto {

    private AccountTrx accountTrx;

    private String userFullName;

    private String plainAmount;

    private String receiptNumber;

    private Long transactionDate;

    private String transactedBy;

    public String getUserFullName() {
        return userFullName;
    }

    public void setUserFullName(String userFullName) {
        this.userFullName = userFullName;
    }

    public String getPlainAmount() {
        return plainAmount;
    }

    public void setPlainAmount(String plainAmount) {
        this.plainAmount = plainAmount;
    }

    public String getReceiptNumber() {
        return receiptNumber;
    }

    public void setReceiptNumber(String receiptNumber) {
        this.receiptNumber = receiptNumber;
    }

    public Long getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(Long transactionDate) {
        this.transactionDate = transactionDate;
    }

    public String getTransactedBy() {
        return transactedBy;
    }

    public void setTransactedBy(String transactedBy) {
        this.transactedBy = transactedBy;
    }

    public AccountTrx getAccountTrx() {
        return accountTrx;
    }

    public void setAccountTrx(AccountTrx accountTrx) {
        this.accountTrx = accountTrx;
    }

}
