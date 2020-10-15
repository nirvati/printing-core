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
package org.savapage.core.util;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class BitcoinUtil {

    /**
     * The BTC precision (number of decimals).
     */
    public static final int BTC_DECIMALS = 8;

    /**
     * The number of satoshi in one (1) BTC.
     */
    public static final long SATOSHIS_IN_BTC = 100000000L;

    /**
     * The number of μBTC (micro-bitcoin) in one (1) BTC.
     */
    public static final long MICRO_IN_BTC = 1000000L;

    /**
     * The number of mBTC (milli-bitcoin) in one (1) BTC.
     */
    public static final long MILLI_IN_BTC = 1000L;

    /**
     *
     */
    private BitcoinUtil() {
    }

}
