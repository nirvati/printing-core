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

/**
 * A POS transaction.
 *
 * @author Rijk Ravestein
 *
 */
public abstract class PosTransactionDto extends AbstractDto {

    public enum DeliveryEnum {
        /** NO delivery. */
        NONE,
        /** Send to primary email of User. */
        EMAIL
    }

    /**
     * User ID subject of the transaction.
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

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String formatAmount() {
        return String.format("%s.%s", this.amountMain, amountCents);
    }
}
