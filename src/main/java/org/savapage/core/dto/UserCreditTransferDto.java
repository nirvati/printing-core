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

import org.savapage.core.dao.enums.AccountTrxTypeEnum;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Information for a {@link AccountTrxTypeEnum#TRANSFER}.
 *
 * @author Rijk Ravestein
 *
 */
public final class UserCreditTransferDto extends AbstractDto {

    /**
     *
     */
    @JsonProperty("userFrom")
    private String userIdFrom;

    /**
     *
     */
    @JsonProperty("userTo")
    private String userIdTo;

    /**
     * The main amount.
     */
    private String amountMain;

    /**
     * The amount cents.
     */
    private String amountCents;

    /**
     *
     */
    private String comment;

    public String getUserIdFrom() {
        return userIdFrom;
    }

    public void setUserIdFrom(String userIdFrom) {
        this.userIdFrom = userIdFrom;
    }

    public String getUserIdTo() {
        return userIdTo;
    }

    public void setUserIdTo(String userIdTo) {
        this.userIdTo = userIdTo;
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

}
