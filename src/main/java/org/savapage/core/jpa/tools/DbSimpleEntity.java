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

import org.savapage.core.jpa.xml.XEntityVersion;

/**
 * Convenience wrapper for JPA entity names from {@link DbXEntityEnum}. These
 * names are typically used in PSQL to work around the infamous ORM (Hibernate)
 * N+1 problem. {@link XEntityVersion} JPA entities are "raw" attribute
 * versions: they do not have attribute types of related entities.
 *
 * @author Rijk Ravestein
 *
 */
public final class DbSimpleEntity {

    /** */
    public static final String ACCOUNT = DbXEntityEnum.ACCOUNT.psql();

    /** */
    public static final String ACCOUNT_TRX = DbXEntityEnum.ACCOUNT_TRX.psql();
    /** */
    public static final String ACCOUNT_VOUCHER =
            DbXEntityEnum.ACCOUNT_VOUCHER.psql();
    /** */
    public static final String COST_CHANGE = DbXEntityEnum.COST_CHANGE.psql();
    /** */
    public static final String POS_PURCHASE = DbXEntityEnum.POS_PURCHASE.psql();
    /** */
    public static final String POS_PURCHASE_ITEM =
            DbXEntityEnum.POS_PURCHASE_ITEM.psql();
    /** */
    public static final String DOC_LOG = DbXEntityEnum.DOC_LOG.psql();
    /** */
    public static final String DOC_IN = DbXEntityEnum.DOC_IN.psql();
    /** */
    public static final String DOC_OUT = DbXEntityEnum.DOC_OUT.psql();
    /** */
    public static final String DOC_IN_OUT = DbXEntityEnum.DOC_IN_OUT.psql();

    /** */
    public static final String PRINT_IN = DbXEntityEnum.PRINT_IN.psql();
    /** */
    public static final String PRINT_OUT = DbXEntityEnum.PRINT_OUT.psql();
    /** */
    public static final String PDF_OUT = DbXEntityEnum.PDF_OUT.psql();

    /** */
    public static final String USER = DbXEntityEnum.USER.psql();

    /** */
    public static final String USER_ACCOUNT = DbXEntityEnum.USER_ACCOUNT.psql();

    /** */
    private DbSimpleEntity() {
    }

}
