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
package org.savapage.core.services.helpers;

import org.savapage.core.jpa.Account;

/**
 * A weighted {@link Account} transaction with free format details.
 *
 * @author Rijk Ravestein
 *
 */
public class AccountTrxInfo {

    /**
    *
    */
    private Account account;

    /**
     * Mathematical weight of the transaction in the context of a transaction
     * set.
     */
    private Integer weight;

    /**
     * The divider used on {@link #weight}, for calculating cost and copies.
     */
    private Integer weightUnit;

    /**
     * Free format details from external source.
     */
    private String extDetails;

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public Integer getWeight() {
        return weight;
    }

    public void setWeight(Integer weight) {
        this.weight = weight;
    }

    /**
     * @return The divider used on {@link #weight}, for calculating cost and
     *         copies.
     */
    public Integer getWeightUnit() {
        return weightUnit;
    }

    /**
     * @param weightUnit
     *            The divider used on {@link #weight}, for calculating cost and
     *            copies.
     */
    public void setWeightUnit(Integer weightUnit) {
        this.weightUnit = weightUnit;
    }

    public String getExtDetails() {
        return extDetails;
    }

    public void setExtDetails(String extDetails) {
        this.extDetails = extDetails;
    }

}
