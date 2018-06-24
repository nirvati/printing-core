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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.core.services.helpers;

import java.util.ArrayList;
import java.util.List;

/**
 * A unit of weighted {@link AccountTrxInfo} objects.
 *
 * @author Rijk Ravestein
 *
 */
public final class AccountTrxInfoSet {

    /**
     * .
     */
    private final int weightTotal;

    /**
     * The number of units (e.g. printed copies).
     */
    private final int unitTotal;

    /**
     * .
     */
    private List<AccountTrxInfo> accountTrxInfoList = new ArrayList<>();

    /**
     *
     * @param weightTot
     *            The weight total. IMPORTANT: This total need NOT be the same
     *            as the accumulated weight of the individual Account
     *            transactions. For example: parts of the printing costs may be
     *            charged to (personal and shared) multiple accounts.
     *
     */
    public AccountTrxInfoSet(final int weightTot) {
        this.weightTotal = weightTot;
        this.unitTotal = weightTot;
    }

    /**
     *
     * @param weightTot
     *            The weight total. IMPORTANT: This total need NOT be the same
     *            as the accumulated weight of the individual Account
     *            transactions. For example: parts of the printing costs may be
     *            charged to (personal and shared) multiple accounts.
     * @param unitTot
     *            The number of units (e.g. printed copies).
     */
    public AccountTrxInfoSet(final int weightTot, final int unitTot) {
        this.weightTotal = weightTot;
        this.unitTotal = unitTot;
    }

    /**
     *
     * @return The list of {@link AccountTrxInfo} objects.
     */
    public List<AccountTrxInfo> getAccountTrxInfoList() {
        return accountTrxInfoList;
    }

    /**
     *
     * @param list
     *            The list of {@link AccountTrxInfo} objects.
     */
    public void setAccountTrxInfoList(final List<AccountTrxInfo> list) {
        this.accountTrxInfoList = list;
    }

    /**
     * Gets the weights total.
     *
     * <p>
     * IMPORTANT: This total need NOT be the same as the accumulated weight of
     * the individual Account transactions. For example: parts of the printing
     * costs may be charged to (personal and shared) multiple accounts.
     * </p>
     *
     * @return The weight total.
     */
    public int getWeightTotal() {
        return weightTotal;
    }

    /**
     * @return The number of units (e.g. printed copies).
     */
    public int getUnitTotal() {
        return unitTotal;
    }

}
