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

    private final static int N_UP_1 = 1;
    private final static int N_UP_2 = 2;
    private final static int N_UP_4 = 4;
    private final static int N_UP_6 = 6;
    private final static int N_UP_9 = 9;

    /**
     *
     * @param nPages
     * @param nPagesPerSide
     * @param nCopies
     * @param duplex
     * @param pageCostOneSided
     * @param pageCostTwoSided
     * @param expectedPrintCost
     */
    public void testPrintCost(final int nPages, final int nPagesPerSide,
            final int nCopies, final boolean duplex,
            final String pageCostOneSided, final String pageCostTwoSided,
            final String discountPerc, final String expectedPrintCost) {

        assertTrue(AccountingServiceImpl.calcPrintJobCost(nPages,
                nPagesPerSide, nCopies, duplex,
                new BigDecimal(pageCostOneSided),
                new BigDecimal(pageCostTwoSided), new BigDecimal(discountPerc))
                .compareTo(new BigDecimal(expectedPrintCost)) == 0);
    }

    @Test
    public void testA() {
        // Discount
        testPrintCost(1, N_UP_1, 1, SINGLEX, "0.10", "0.07", "0.10", "0.09");
        testPrintCost(1, N_UP_1, 1, SINGLEX, "0.10", "0.07", "0.20", "0.08");
        testPrintCost(1, N_UP_1, 1, SINGLEX, "0.10", "0.07", "0.25", "0.075");
        testPrintCost(1, N_UP_1, 1, SINGLEX, "0.12", "0.07", "0.25", "0.09");
    }

    @Test
    public void testB() {

        testPrintCost(1, N_UP_1, 1, SINGLEX, "0.10", "0.07", "0.00", "0.10");
        testPrintCost(1, N_UP_2, 1, SINGLEX, "0.10", "0.07", "0.00", "0.10");
        testPrintCost(2, N_UP_2, 1, SINGLEX, "0.10", "0.07", "0.00", "0.10");
        testPrintCost(1, N_UP_4, 1, SINGLEX, "0.10", "0.07", "0.00", "0.10");
        testPrintCost(4, N_UP_4, 1, SINGLEX, "0.10", "0.07", "0.00", "0.10");

        //
        testPrintCost(2, N_UP_1, 1, SINGLEX, "0.10", "0.07", "0.00", "0.20");
        testPrintCost(3, N_UP_2, 1, SINGLEX, "0.10", "0.07", "0.00", "0.20");
        testPrintCost(10, N_UP_6, 1, SINGLEX, "0.10", "0.07", "0.00", "0.20");

        testPrintCost(2, N_UP_1, 2, SINGLEX, "0.10", "0.07", "0.00", "0.40");
        testPrintCost(3, N_UP_2, 2, SINGLEX, "0.10", "0.07", "0.00", "0.40");
        testPrintCost(4, N_UP_2, 2, SINGLEX, "0.10", "0.07", "0.00", "0.40");
        testPrintCost(10, N_UP_9, 2, SINGLEX, "0.10", "0.07", "0.00", "0.40");
        testPrintCost(18, N_UP_9, 2, SINGLEX, "0.10", "0.07", "0.00", "0.40");

        //
        testPrintCost(1, N_UP_1, 1, DUPLEX, "0.10", "0.07", "0.00", "0.10");
        testPrintCost(1, N_UP_2, 1, DUPLEX, "0.10", "0.07", "0.00", "0.10");
        testPrintCost(2, N_UP_2, 1, DUPLEX, "0.10", "0.07", "0.00", "0.10");
        testPrintCost(1, N_UP_4, 1, DUPLEX, "0.10", "0.07", "0.00", "0.10");
        testPrintCost(4, N_UP_4, 1, DUPLEX, "0.10", "0.07", "0.00", "0.10");

        testPrintCost(2, N_UP_1, 1, DUPLEX, "0.10", "0.07", "0.00", "0.14");
        testPrintCost(3, N_UP_2, 1, DUPLEX, "0.10", "0.07", "0.00", "0.14");
        testPrintCost(10, N_UP_6, 1, DUPLEX, "0.10", "0.07", "0.00", "0.14");

        testPrintCost(2, N_UP_1, 2, DUPLEX, "0.10", "0.07", "0.00", "0.28");
        testPrintCost(3, N_UP_2, 2, DUPLEX, "0.10", "0.07", "0.00", "0.28");
        testPrintCost(4, N_UP_2, 2, DUPLEX, "0.10", "0.07", "0.00", "0.28");
        testPrintCost(10, N_UP_9, 2, DUPLEX, "0.10", "0.07", "0.00", "0.28");
        testPrintCost(18, N_UP_9, 2, DUPLEX, "0.10", "0.07", "0.00", "0.28");

        testPrintCost(3, N_UP_1, 1, DUPLEX, "0.10", "0.07", "0.00", "0.24");
        testPrintCost(9, N_UP_4, 1, DUPLEX, "0.10", "0.07", "0.00", "0.24");

        testPrintCost(3, N_UP_1, 2, DUPLEX, "0.10", "0.07", "0.00", "0.48");
        testPrintCost(9, N_UP_4, 2, DUPLEX, "0.10", "0.07", "0.00", "0.48");

        testPrintCost(3, N_UP_1, 1, DUPLEX, "0.100", "0.075", "0.00", "0.250");
        testPrintCost(20, N_UP_9, 1, DUPLEX, "0.100", "0.075", "0.00", "0.250");
    }

}
