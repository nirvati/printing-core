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
package org.savapage.core.dao.enums;

import org.apache.commons.lang3.EnumUtils;
import org.savapage.core.jpa.DocLog;
import org.savapage.core.jpa.PrintOut;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class DaoEnumHelper {

    /**
     * Static methods only.
     */
    private DaoEnumHelper() {
    }

    /**
     * Gets the {@link ExternalSupplierEnum} from the {@link DocLog}.
     *
     * @param docLog
     *            The {@link DocLog}.
     * @return {@code null} when docLog {@code null} or no External Supplier is
     *         present.
     */
    public static ExternalSupplierEnum getExtSupplier(final DocLog docLog) {
        if (docLog == null) {
            return null;
        }
        return EnumUtils.getEnum(ExternalSupplierEnum.class,
                docLog.getExternalSupplier());
    }

    /**
     * Gets the {@link ExternalSupplierStatusEnum} from the {@link DocLog}.
     *
     * @param docLog
     *            The {@link DocLog}.
     * @return {@code null} when docLog {@code null} or no External Supplier
     *         Status is present.
     */
    public static ExternalSupplierStatusEnum
            getExtSupplierStatus(final DocLog docLog) {
        if (docLog == null) {
            return null;
        }
        return EnumUtils.getEnum(ExternalSupplierStatusEnum.class,
                docLog.getExternalStatus());
    }

    /**
     * Gets the {@link PrintModeEnum} from the {@link PrintOut}.
     *
     * @param printOut
     *            The {@link PrintOut}.
     * @return {@code null} when printOut {@code null} or no Print Mode is
     *         present.
     */
    public static PrintModeEnum getPrintMode(final PrintOut printOut) {
        if (printOut == null) {
            return null;
        }
        return EnumUtils.getEnum(PrintModeEnum.class, printOut.getPrintMode());
    }
}
