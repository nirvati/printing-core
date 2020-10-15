/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2020 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: © 2020 Datraverse B.V. <info@datraverse.com>
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
package org.savapage.core.dto;

import org.savapage.core.jpa.PosPurchase;

/**
 * Information for an {@link PosPurchase} deposit.
 *
 * @author Rijk Ravestein
 *
 */
public class PosDepositDto extends AbstractDto {

    public static enum DeliveryEnum {
        /**
         * NO delivery
         */
        NONE,
        /**
         * Send to primary email of User.
         */
        EMAIL
    }

    /**
     * The user who deposited the amount.
     */
    private String userId;

    /**
     * The deposited main amount.
     */
    private String amountMain;

    /**
     * The deposited amount cents.
     */
    private String amountCents;

    private String comment;

    private String paymentType;

    private DeliveryEnum receiptDelivery;

    private String userEmail;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getAmountMain() {
        return amountMain;
    }

    public void setAmountMain(String amountMain) {
        this.amountMain = amountMain;
    }

    public String getAmountCents() {
        return amountCents;
    }

    public void setAmountCents(String amountCents) {
        this.amountCents = amountCents;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(String paymentType) {
        this.paymentType = paymentType;
    }

    public DeliveryEnum getReceiptDelivery() {
        return receiptDelivery;
    }

    public void setReceiptDelivery(DeliveryEnum receiptDelivery) {
        this.receiptDelivery = receiptDelivery;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

}
