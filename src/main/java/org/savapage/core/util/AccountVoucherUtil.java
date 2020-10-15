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

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.RandomStringUtils;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class AccountVoucherUtil {

    /**
     * Generates a {@link Set} of formatted voucher numbers.
     *
     * @param nVouchers
     *            The number of vouchers to generate.
     * @param batchId
     *            A batch ID is a user defined ID or number assigned to all
     *            vouchers in a batch. The batch ID prefixes all voucher numbers
     *            and is used to identify the source of a card. A unique number
     *            should be assigned to each batch.
     * @return The {@link Set} of generated voucher numbers.
     */
    public static Set<String> generateVoucherNumbers(final String batchId,
            int nVouchers) {

        Set<String> voucherSet = new HashSet<>();

        for (int iVoucher = 0; iVoucher < nVouchers;) {

            final StringBuilder buffer = new StringBuilder();

            for (int i = 0; i < 4; i++) {

                if (i > 0) {
                    buffer.append("-");
                }
                buffer.append(RandomStringUtils.randomNumeric(4));
            }

            final String voucher = batchId + "-" + buffer.toString();

            if (!voucherSet.contains(voucher)) {
                voucherSet.add(voucher);
                iVoucher++;
            }
        }

        return voucherSet;
    }
}
