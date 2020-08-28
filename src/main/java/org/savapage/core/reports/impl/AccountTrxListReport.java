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
package org.savapage.core.reports.impl;

import java.util.Locale;
import java.util.Map;

import org.savapage.core.dao.helpers.AccountTrxPagerReq;

import net.sf.jasperreports.engine.JRDataSource;

/**
 * User List Report creator.
 *
 * @author Rijk Ravestein
 *
 */
public final class AccountTrxListReport extends ReportCreator {

    /**
     * The unique ID of this report.
     */
    public static final String REPORT_ID = "AccountTrxList";

    /**
     * Constructor.
     *
     * @param requestingUser
     *            The requesting user.
     * @param requestingUserAdmin
     *            {@code true} if requesting user is an administrator.
     * @param inputData
     *            The input data for the report.
     * @param locale
     *            {@link Locale} of the report.
     */
    public AccountTrxListReport(final String requestingUser,
            final boolean requestingUserAdmin, final String inputData,
            final Locale locale) {
        super(requestingUser, requestingUserAdmin, inputData, locale);
    }

    @Override
    protected JRDataSource onCreateDataSource(final String inputData,
            final Locale locale, final Map<String, Object> reportParameters) {

        final AccountTrxPagerReq request = AccountTrxPagerReq.read(inputData);

        this.onUserAuthentication(request.getSelect().getUserId());

        final AccountTrxDataSource dataSource =
                new AccountTrxDataSource(request, locale);

        reportParameters.put(REPORT_PARM_DATA_SELECTION,
                dataSource.getSelectionInfo());

        return dataSource;
    }

    @Override
    protected String getReportId() {
        return REPORT_ID;
    }

}
