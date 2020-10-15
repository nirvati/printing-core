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
package org.savapage.core.print.proxy;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class BasePrintSheetCalcParms
        implements ProxyPrintSheetsCalcParms {

    private int numberOfCopies;
    private int numberOfPages;
    private int nup = 1;
    boolean duplex;

    @Override
    public boolean isDuplex() {
        return duplex;
    }

    @Override
    public int getNup() {
        return nup;
    }

    @Override
    public int getNumberOfPages() {
        return numberOfPages;
    }

    @Override
    public int getNumberOfCopies() {
        return numberOfCopies;
    }

    @Override
    public boolean isOddOrEvenSheets() {
        return false;
    }

    @Override
    public boolean isCoverPageBefore() {
        return false;
    }

    @Override
    public boolean isCoverPageAfter() {
        return false;
    }

    public void setNumberOfCopies(int numberOfCopies) {
        this.numberOfCopies = numberOfCopies;
    }

    public void setNumberOfPages(int numberOfPages) {
        this.numberOfPages = numberOfPages;
    }

    public void setNup(int nup) {
        this.nup = nup;
    }

    public void setDuplex(boolean duplex) {
        this.duplex = duplex;
    }

}
