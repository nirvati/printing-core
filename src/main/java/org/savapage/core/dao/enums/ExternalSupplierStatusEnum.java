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

import java.util.Locale;

import org.savapage.core.util.Messages;
import org.savapage.ext.smartschool.SmartschoolPrintStatusEnum;

/**
 * Generic status of (proxy) print request from External Supplier. Also see
 * {@link SmartschoolPrintStatusEnum}.
 * <p>
 * NOTE: Do NOT changes enum text since they are stored in the database.
 * </p>
 *
 * @author Rijk Ravestein
 *
 */
public enum ExternalSupplierStatusEnum {

    /**
     * .
     */
    CANCELLED,

    /**
     * .
     */
    COMPLETED,

    /**
     * .
     */
    ERROR,

    /**
     * A PENDING print expired.
     */
    EXPIRED,

    /**
     * Pending in SavaPage.
     */
    PENDING,

    /**
     * Proxy Print request is pending in an external system like PaperCut.
     */
    PENDING_EXT;

    /**
     * @param locale
     *            The {@link Locale}.
     * @return The localized text.
     */
    public String uiText(final Locale locale) {
        return Messages.loadXmlResource(this.getClass(),
                this.getClass().getSimpleName(), locale).getString(
                this.toString());
    }
}
