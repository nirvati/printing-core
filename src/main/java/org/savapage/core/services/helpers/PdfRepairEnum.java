/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2019 Datraverse B.V.
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.core.services.helpers;

/**
 * PDF repair status.
 *
 * @author Rijk Ravestein
 *
 */
public enum PdfRepairEnum {
    /**
     * Document repaired.
     */
    DOC,
    /**
     * Document repair failed.
     */
    DOC_FAIL,
    /**
     * PDF font(s) repaired.
     */
    FONT,
    /**
     * PDF font(s) repair failed.
     */
    FONT_FAIL,
    /**
     * Not repaired.
     */
    NONE;

    /**
     * @return {@code true} if repaired.
     */
    public boolean isRepaired() {
        return this == DOC || this == FONT;
    }

    /**
     * @return {@code true} if repair failed.
     */
    public boolean isRepairFail() {
        return this == DOC_FAIL || this == FONT_FAIL;
    }

}
