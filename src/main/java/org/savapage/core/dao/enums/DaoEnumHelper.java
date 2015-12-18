/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2015 Datraverse B.V.
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
package org.savapage.core.dao.enums;

import org.apache.commons.lang3.EnumUtils;
import org.savapage.core.jpa.DocLog;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class DaoEnumHelper {

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
     * Gets the External Supplier status from the {@link DocLog}.
     *
     * @param docLog
     *            The {@link DocLog}.
     * @return {@code null} when docLog {@code null} or no External Supplier or
     *         Status is present.
     */
    public static ExternalSupplierStatusEnum getExtSupplierStatus(
            final DocLog docLog) {
        if (docLog == null) {
            return null;
        }
        return EnumUtils.getEnum(ExternalSupplierStatusEnum.class,
                docLog.getExternalStatus());
    }

}
