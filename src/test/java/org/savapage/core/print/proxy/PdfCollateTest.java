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
package org.savapage.core.print.proxy;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.savapage.core.pdf.PdfPrintCollector;

/**
 * Test cases for
 * {@link PdfPrintCollector#calcBlankAppendPagesOfCopy(ProxyPrintSheetsCalcParms)}.
 * .
 *
 * @author Datraverse B.V.
 *
 */
public class PdfCollateTest {

    /**
     * Calculates the extra blank pages to append to a PDF copy in a collated
     * sequence.
     *
     * @param calcParms
     *            The {@link ProxyPrintSheetsCalcParms}.
     * @return The number of extra blank pages to append to each copy.
     */
    public static int calcBlankCollatePagesToAppend(
            final ProxyPrintSheetsCalcParms calcParms) {
        return PdfPrintCollector.calcBlankAppendPagesOfCopy(calcParms);
    }

    private static final boolean DUPLEX = true;

    /**
     *
     * @param pages
     * @param nup
     * @param duplex
     * @return
     */
    private static BasePrintSheetCalcParms createParms(final int copies,
            final int pages, final int nup, final boolean duplex) {
        final BasePrintSheetCalcParms parms = new BasePrintSheetCalcParms();

        parms.setNumberOfCopies(copies);
        parms.setNumberOfPages(pages);
        parms.setNup(nup);
        parms.setDuplex(duplex);

        return parms;
    }

    @Test
    public void test4Up() {

        final int nUp = 4;

        assertEquals("1 copy, 4 pages, 4-up, singlex", 0,
                calcBlankCollatePagesToAppend(createParms(1, 4, nUp, !DUPLEX)));

        assertEquals("2 copy, 3 pages, 4-up, singlex", 1,
                calcBlankCollatePagesToAppend(createParms(2, 3, nUp, !DUPLEX)));

        assertEquals("10 copies, 4 pages, 4-up, singlex", 0,
                calcBlankCollatePagesToAppend(createParms(10, 4, nUp, !DUPLEX)));

        assertEquals("1 copy, 3 pages, 4-up, singlex", 1,
                calcBlankCollatePagesToAppend(createParms(1, 3, nUp, !DUPLEX)));

        assertEquals("5 copies, 5 pages, 4-up, singlex", 3,
                calcBlankCollatePagesToAppend(createParms(1, 5, nUp, !DUPLEX)));

        //
        assertEquals("1 copy, 4 pages, 4-up, duplex", 4,
                calcBlankCollatePagesToAppend(createParms(1, 4, nUp, DUPLEX)));

        assertEquals("1 copy, 3 pages, 4-up, duplex", 5,
                calcBlankCollatePagesToAppend(createParms(1, 3, nUp, DUPLEX)));

        assertEquals("3 copies, 5 pages, 4-up, duplex", 3,
                calcBlankCollatePagesToAppend(createParms(1, 5, nUp, DUPLEX)));
    }

    @Test
    public void test2Up() {

        final int nUp = 2;

        assertEquals("1 copy, 4 pages, 2-up, singlex", 0,
                calcBlankCollatePagesToAppend(createParms(1, 4, nUp, !DUPLEX)));

        assertEquals("10 copies, 4 pages, 2-up, singlex", 0,
                calcBlankCollatePagesToAppend(createParms(10, 4, nUp, !DUPLEX)));

        assertEquals("1 copy, 3 pages, 2-up, singlex", 1,
                calcBlankCollatePagesToAppend(createParms(1, 3, nUp, !DUPLEX)));

        assertEquals("5 copy, 5 pages, 2-up, singlex", 1,
                calcBlankCollatePagesToAppend(createParms(1, 3, nUp, !DUPLEX)));

        //
        assertEquals("1 copy, 4 pages, 2-up, duplex", 0,
                calcBlankCollatePagesToAppend(createParms(1, 4, nUp, DUPLEX)));

        assertEquals("1 copy, 3 pages, 2-up, duplex", 1,
                calcBlankCollatePagesToAppend(createParms(1, 3, nUp, DUPLEX)));

        assertEquals("3 copies, 5 pages, 2-up, duplex", 1,
                calcBlankCollatePagesToAppend(createParms(1, 3, nUp, DUPLEX)));
    }

    @Test
    public void test1Up() {

        final int nUp = 1;

        assertEquals("1 copy, 4 pages, 1-up, singlex", 0,
                calcBlankCollatePagesToAppend(createParms(1, 4, nUp, !DUPLEX)));

        assertEquals("10 copies, 4 pages, 1-up, singlex", 0,
                calcBlankCollatePagesToAppend(createParms(10, 4, nUp, !DUPLEX)));

        assertEquals("1 copy, 3 pages, 1-up, singlex", 0,
                calcBlankCollatePagesToAppend(createParms(1, 3, nUp, !DUPLEX)));

        //
        assertEquals("1 copy, 4 pages, 1-up, duplex", 0,
                calcBlankCollatePagesToAppend(createParms(1, 4, nUp, DUPLEX)));

        assertEquals("10 copies, 4 pages, 1-up, duplex", 0,
                calcBlankCollatePagesToAppend(createParms(10, 4, nUp, DUPLEX)));

        assertEquals("1 copy, 3 pages, 1-up, duplex", 1,
                calcBlankCollatePagesToAppend(createParms(1, 3, nUp, DUPLEX)));

    }

}
