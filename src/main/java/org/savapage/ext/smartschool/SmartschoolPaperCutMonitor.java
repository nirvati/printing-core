/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2016 Datraverse B.V.
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
package org.savapage.ext.smartschool;

import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.dao.enums.ExternalSupplierEnum;
import org.savapage.core.jpa.Account.AccountTypeEnum;
import org.savapage.core.jpa.DocLog;
import org.savapage.core.services.ServiceContext;
import org.savapage.ext.papercut.PaperCutDbProxy;
import org.savapage.ext.papercut.PaperCutPrintJobListener;
import org.savapage.ext.papercut.PaperCutPrintMonitorPattern;
import org.savapage.ext.papercut.PaperCutServerProxy;
import org.savapage.ext.smartschool.services.SmartschoolService;
import org.slf4j.Logger;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class SmartschoolPaperCutMonitor extends
        PaperCutPrintMonitorPattern {

    /**
     * .
     */
    private static final SmartschoolService SMARTSCHOOL_SERVICE =
            ServiceContext.getServiceFactory().getSmartSchoolService();

    /**
     * @param papercutServerProxy
     *            The {@link PaperCutServerProxy}.
     * @param papercutDbProxy
     *            The {@link PaperCutDbProxy}.
     * @param statusListener
     *            The {@link PaperCutPrintJobListener}.
     */
    public SmartschoolPaperCutMonitor(
            final PaperCutServerProxy papercutServerProxy,
            final PaperCutDbProxy papercutDbProxy,
            final PaperCutPrintJobListener statusListener) {

        super(ExternalSupplierEnum.SMARTSCHOOL, papercutServerProxy,
                papercutDbProxy, statusListener);
    }

    @Override
    protected String getUserAccountName() {
        return ConfigManager.instance().getConfigValue(
                Key.SMARTSCHOOL_PAPERCUT_ACCOUNT_PERSONAL);
    }

    @Override
    protected String getSharedParentAccountName() {
        return SMARTSCHOOL_SERVICE.getSharedParentAccountName();
    }

    @Override
    protected String getSharedJobsAccountName() {
        return SMARTSCHOOL_SERVICE.getSharedJobsAccountName();
    }

    @Override
    protected int getAccountTrxWeightTotal(final DocLog docLogOut,
            final DocLog docLogIn) {
        /*
         * Get total number of copies from the external data and use as weight
         * total.
         */
        final SmartschoolPrintInData externalPrintInData =
                SmartschoolPrintInData.createFromData(docLogIn
                        .getExternalData());

        return externalPrintInData.getCopies().intValue();
    }

    @Override
    protected String getKlasFromAccountName(final String subAccountName) {
        return SMARTSCHOOL_SERVICE
                .getKlasFromComposedAccountName(subAccountName);
    }

    @Override
    protected Logger getLogger() {
        return SmartschoolLogger.getLogger();
    }

    @Override
    protected boolean isDocInAccountTrx() {
        return true;
    }

    @Override
    protected String composeSharedSubAccountName(
            final AccountTypeEnum accountType, final String accountName) {
        /*
         * The account name is already in composed format.
         */
        return accountName;
    }

}
