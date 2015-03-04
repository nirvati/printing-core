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
package org.savapage.services;

import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;

import org.junit.Test;
import org.savapage.core.services.impl.AccountingServiceImpl;

/**
 *
 * @author Datraverse B.V.
 *
 */
public class AccountingServiceTest {

    private final static boolean DUPLEX = true;
    private final static boolean SINGLEX = !DUPLEX;

    /**
     *
     * @param nPages
     * @param nCopies
     * @param duplex
     * @param pageCostOneSided
     * @param pageCostTwoSided
     * @param expectedPrintCost
     */
    public void testPrintCost(int nPages, int nCopies, boolean duplex,
            String pageCostOneSided, String pageCostTwoSided,
            String expectedPrintCost) {

        assertTrue(AccountingServiceImpl.calcPrintJobCost(nPages, nCopies,
                duplex, new BigDecimal(pageCostOneSided),
                new BigDecimal(pageCostTwoSided)).equals(
                new BigDecimal(expectedPrintCost)));
    }

    @Test
    public void test() {

        testPrintCost(1, 1, SINGLEX, "0.10", "0.07", "0.10");
        testPrintCost(2, 1, SINGLEX, "0.10", "0.07", "0.20");
        testPrintCost(2, 2, SINGLEX, "0.10", "0.07", "0.40");

        testPrintCost(1, 1, DUPLEX, "0.10", "0.07", "0.10");
        testPrintCost(2, 1, DUPLEX, "0.10", "0.07", "0.14");
        testPrintCost(2, 2, DUPLEX, "0.10", "0.07", "0.28");

        testPrintCost(3, 1, DUPLEX, "0.10", "0.07", "0.24");
        testPrintCost(3, 2, DUPLEX, "0.10", "0.07", "0.48");

        testPrintCost(3, 1, DUPLEX, "0.100", "0.075", "0.250");

    }

}
