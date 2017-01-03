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
package org.savapage.ext.papercut.job;

import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.dao.enums.ExternalSupplierEnum;
import org.savapage.core.jpa.Account.AccountTypeEnum;
import org.savapage.core.jpa.DocLog;
import org.savapage.ext.papercut.PaperCutAccountResolver;
import org.savapage.ext.papercut.PaperCutDbProxy;
import org.savapage.ext.papercut.PaperCutHelper;
import org.savapage.ext.papercut.PaperCutPrintJobListener;
import org.savapage.ext.papercut.PaperCutPrintMonitorPattern;
import org.savapage.ext.papercut.PaperCutServerProxy;
import org.slf4j.Logger;

/**
 * Monitoring PaperCut print status of jobs issued from
 * {@link ExternalSupplierEnum#SAVAPAGE}.
 *
 * @author Rijk Ravestein
 *
 */
public final class PaperCutPrintMonitor extends PaperCutPrintMonitorPattern {

    /**
     * .
     */
    private final Logger logger;

    /**
     *
     * @param papercutServerProxy
     *            The {@link PaperCutServerProxy}.
     * @param papercutDbProxy
     *            The {@link PaperCutDbProxy}.
     * @param statusListener
     *            The {@link PaperCutPrintJobListener}.
     * @param loggerListener
     *            The logger listening to log events.
     */
    protected PaperCutPrintMonitor(
            final PaperCutServerProxy papercutServerProxy,
            final PaperCutDbProxy papercutDbProxy,
            final PaperCutPrintJobListener statusListener,
            final Logger loggerListener) {

        super(ExternalSupplierEnum.SAVAPAGE, papercutServerProxy,
                papercutDbProxy, statusListener);
        this.logger = loggerListener;
    }

    /**
     * @return As in {@link PaperCutAccountResolver#getUserAccountName()}.
     */
    public static String getAccountNameUser() {
        return ConfigManager.instance().getConfigValue(
                Key.PROXY_PRINT_DELEGATE_PAPERCUT_ACCOUNT_PERSONAL);
    }

    @Override
    public String getUserAccountName() {
        return getAccountNameUser();
    }

    /**
     * @return As in
     *         {@link PaperCutAccountResolver#getSharedParentAccountName()}.
     */
    public static String getSharedAccountNameParent() {
        return ConfigManager.instance().getConfigValue(
                Key.PROXY_PRINT_DELEGATE_PAPERCUT_ACCOUNT_SHARED_PARENT);
    }

    @Override
    public String getSharedParentAccountName() {
        return getSharedAccountNameParent();
    }

    /**
     * @return As in {@link PaperCutAccountResolver#getSharedJobsAccountName()}.
     */
    public static String getSharedAccountNameJobs() {
        return ConfigManager.instance().getConfigValue(
                Key.PROXY_PRINT_DELEGATE_PAPERCUT_ACCOUNT_SHARED_CHILD_JOBS);
    }

    @Override
    public String getSharedJobsAccountName() {
        return getSharedAccountNameJobs();
    }

    @Override
    protected int getAccountTrxWeightTotal(final DocLog docLogOut,
            final DocLog docLogIn) {
        return docLogOut.getDocOut().getPrintOut().getNumberOfCopies();
    }

    /**
     * As {@link PaperCutAccountResolver#getKlasFromAccountName(String)}.
     *
     * @param accountName
     *            The composed account name.
     * @return The extracted klas.
     */
    public static String extractKlasFromAccountName(final String accountName) {
        return PaperCutHelper.decomposeSharedAccountName(accountName);
    }

    @Override
    public String getKlasFromAccountName(final String accountName) {
        return extractKlasFromAccountName(accountName);
    }

    @Override
    protected Logger getLogger() {
        return this.logger;
    }

    @Override
    protected boolean isDocInAccountTrx() {
        return false;
    }

    /**
     * As
     * {@link PaperCutAccountResolver#composeSharedSubAccountName(AccountTypeEnum, String)}
     * .
     *
     * @param accountType
     *            The SavaPage account type.
     * @param accountName
     *            The SavaPage account name.
     * @return The composed PaperCut name.
     */
    public static String createSharedSubAccountName(
            final AccountTypeEnum accountType, final String accountName) {
        return PaperCutHelper.composeSharedAccountName(accountType,
                accountName);
    }

    @Override
    public String composeSharedSubAccountName(final AccountTypeEnum accountType,
            final String accountName) {
        return createSharedSubAccountName(accountType, accountName);
    }

}
