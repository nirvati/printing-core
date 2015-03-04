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
package org.savapage.core.dao.helpers;

import org.savapage.core.SpException;
import org.savapage.core.jpa.PrintIn;

/**
 * Enum encapsulation of database field: see
 * {@link PrintIn#setDeniedReason(String)}.
 *
 * @author Datraverse B.V.
 *
 */
public enum PrintInDeniedReasonEnum {

    /**
     * Denied because of DRM restrictions.
     */
    DRM("DRM");

    /**
     * The value as used in the database.
     */
    private final String dbValue;

    /**
     *
     * @param value
     *            The value as used in the database.
     */
    private PrintInDeniedReasonEnum(final String value) {
        this.dbValue = value;
    }

    /**
     * @return The value as used in the database.
     */
    public String toDbValue() {
        return this.dbValue;
    }

    /**
     * Parses the database string value to the {@link PrintInDeniedReasonEnum}.
     *
     * @param value
     *            The value as used in the database.
     * @return The enum or {@code null} when the input value is {@code null}.
     */
    public static PrintInDeniedReasonEnum parseDbValue(final String value) {

        PrintInDeniedReasonEnum theEnum = null;

        if (value != null) {

            if (value.equals(PrintInDeniedReasonEnum.DRM.toDbValue())) {
                theEnum = PrintInDeniedReasonEnum.DRM;
            } else {
                throw new SpException("Reason [" + value
                        + "] cannot be converted to enum");
            }
        }

        return theEnum;
    }
}
