/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2017 Datraverse B.V.
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
package org.savapage.core.jpa.tools;

import org.savapage.core.jpa.xml.XAccountTrxV01;
import org.savapage.core.jpa.xml.XAccountVoucherV01;
import org.savapage.core.jpa.xml.XCostChangeV01;
import org.savapage.core.jpa.xml.XDocInOutV01;
import org.savapage.core.jpa.xml.XDocInV01;
import org.savapage.core.jpa.xml.XDocLogV01;
import org.savapage.core.jpa.xml.XDocOutV01;
import org.savapage.core.jpa.xml.XEntityVersion;
import org.savapage.core.jpa.xml.XPdfOutV01;
import org.savapage.core.jpa.xml.XPosPurchaseItemV01;
import org.savapage.core.jpa.xml.XPosPurchaseV01;
import org.savapage.core.jpa.xml.XPrintInV01;
import org.savapage.core.jpa.xml.XPrintOutV01;
import org.savapage.core.jpa.xml.XUserAccountV01;
import org.savapage.core.jpa.xml.XUserV01;

/**
 * The actually used {@link XEntityVersion} classes.
 *
 * @author Rijk Ravestein
 *
 */
public enum DbXEntityEnum {

    /** */
    COST_CHANGE(XCostChangeV01.class),

    /** */
    DOC_LOG(XDocLogV01.class),
    /** */
    DOC_IN(XDocInV01.class),
    /** */
    DOC_OUT(XDocOutV01.class),
    /** */
    DOC_IN_OUT(XDocInOutV01.class),

    /** */
    PRINT_IN(XPrintInV01.class),
    /** */
    PRINT_OUT(XPrintOutV01.class),
    /** */
    PDF_OUT(XPdfOutV01.class),

    /** */
    ACCOUNT_TRX(XAccountTrxV01.class),
    /** */
    ACCOUNT_VOUCHER(XAccountVoucherV01.class),
    /** */
    POS_PURCHASE(XPosPurchaseV01.class),
    /** */
    POS_PURCHASE_ITEM(XPosPurchaseItemV01.class),

    /** */
    USER(XUserV01.class),

    /** */
    USER_ACCOUNT(XUserAccountV01.class);

    /** */
    private final Class<? extends XEntityVersion> clazz;

    /**
     *
     * @param entity
     *            The {@link XEntityVersion}.
     */
    DbXEntityEnum(final Class<? extends XEntityVersion> entity) {
        this.clazz = entity;
    }

    /**
     * @return The PSQL entity name.
     */
    public String psql() {
        return this.clazz.getSimpleName();
    }

}
