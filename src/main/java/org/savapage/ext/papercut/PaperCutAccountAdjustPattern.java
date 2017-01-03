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
package org.savapage.ext.papercut;

import java.math.BigDecimal;

import org.savapage.core.config.ConfigManager;
import org.savapage.core.dao.UserAccountDao;
import org.savapage.core.jpa.Account.AccountTypeEnum;
import org.savapage.core.jpa.AccountTrx;
import org.savapage.core.jpa.DocIn;
import org.savapage.core.jpa.DocLog;
import org.savapage.core.jpa.DocOut;
import org.savapage.core.jpa.User;
import org.savapage.core.services.AccountingService;
import org.savapage.core.services.ServiceContext;
import org.savapage.ext.papercut.services.PaperCutService;
import org.slf4j.Logger;

/**
 *
 * @author Rijk Ravestein
 *
 */
public abstract class PaperCutAccountAdjustPattern {

    /**
     * .
     */
    private static final UserAccountDao USER_ACCOUNT_DAO =
            ServiceContext.getDaoContext().getUserAccountDao();
    /**
     * .
     */
    private static final AccountingService ACCOUNTING_SERVICE =
            ServiceContext.getServiceFactory().getAccountingService();

    /**
     * .
     */
    private static final PaperCutService PAPERCUT_SERVICE =
            ServiceContext.getServiceFactory().getPaperCutService();

    private final PaperCutServerProxy papercutServerProxy;
    private final PaperCutAccountResolver papercutAccountResolver;
    private final Logger logger;

    /**
     *
     * @param serverProxy
     *            The {@link PaperCutServerProxy}.
     * @param accountResolver
     *            The {@link PaperCutAccountResolver}.
     * @param logListener
     *            The {@link Logger} listening to log events.
     */
    protected PaperCutAccountAdjustPattern(
            final PaperCutServerProxy serverProxy,
            final PaperCutAccountResolver accountResolver,
            final Logger logListener) {
        this.papercutServerProxy = serverProxy;
        this.papercutAccountResolver = accountResolver;
        this.logger = logListener;
    }

    /**
     * Notifies an account transaction that was adjusted in PaperCut.
     *
     * @param trx
     *            The {@link AccountTrx}.
     * @param weightedCost
     *            The weighted cost.
     * @param isDocInAccountTrx
     *            {@code true} when the {@link AccountTrx} is linked
     *            with the {@link DocLog} of the {@link DocIn}, {@code false}
     *            when linked with the {@link DocLog} of the {@link DocOut}.
     * @param docLogOut
     *            The {@link DocLog} container of the {@link DocOut} object.
     */
    protected abstract void onAccountTrx(final AccountTrx trx,
            final BigDecimal weightedCost, final boolean isDocInAccountTrx,
            final DocLog docLogOut);

    /**
     * Creates PaperCut transactions from SavaPage {@link AccountTrx} objects.
     *
     * @param docLogTrx
     *            The {@link DocLog} container of the {@link AccountTrx}
     *            objects.
     * @param docLogOut
     *            The {@link DocLog} container of the {@link DocOut} object.
     * @param isDocInAccountTrx
     *            {@code true} when account transaction candidates are linked
     *            with the {@link DocLog} of the {@link DocIn}, {@code false}
     *            when linked with the {@link DocLog} of the {@link DocOut}.
     * @param weightTotalCost
     *            The printing cost total.
     * @param weightTotal
     *            Total number of copies printed.
     *
     * @throws PaperCutException
     *             When a PaperCut error occurs.
     */
    public final void process(final DocLog docLogTrx, final DocLog docLogOut,
            final boolean isDocInAccountTrx, final BigDecimal weightTotalCost,
            final int weightTotal) throws PaperCutException {

        /*
         * Number of decimals for decimal scaling.
         */
        final int scale = ConfigManager.getFinancialDecimalsInDatabase();

        /*
         * Create transaction comment processor.
         */
        final PaperCutPrintCommentProcessor trxCommentProcessor =
                new PaperCutPrintCommentProcessor(docLogTrx, docLogOut,
                        weightTotal);

        trxCommentProcessor.initProcess();

        /*
         * Adjust the Personal and Shared Accounts in PaperCut and update the
         * SavaPage AccountTrx's.
         */
        for (final AccountTrx trx : docLogTrx.getTransactions()) {

            final int weight = trx.getTransactionWeight().intValue();

            final BigDecimal weightedCost =
                    ACCOUNTING_SERVICE.calcWeightedAmount(weightTotalCost,
                            weightTotal, weight, scale);

            final org.savapage.core.jpa.Account account = trx.getAccount();

            /*
             * PaperCut account adjustment.
             */
            final BigDecimal papercutAdjustment = weightedCost.negate();

            final AccountTypeEnum accountType =
                    AccountTypeEnum.valueOf(account.getAccountType());

            if (accountType == AccountTypeEnum.SHARED
                    || accountType == AccountTypeEnum.GROUP) {

                /*
                 * Adjust Shared [Parent]/[klas|group|shared] Account.
                 */

                /*
                 * NOTE: Ignore account.getParent().getName(), which is SavaPage
                 * internal, but use the top account name as used in PaperCut.
                 */
                final String topAccountName =
                        papercutAccountResolver.getSharedParentAccountName();

                final String subAccountName =
                        papercutAccountResolver.composeSharedSubAccountName(
                                accountType, account.getName());

                final String klasName = papercutAccountResolver
                        .getKlasFromAccountName(subAccountName);

                final String klasTrxComment =
                        trxCommentProcessor.processKlasTrx(klasName, weight);

                if (logger.isDebugEnabled()) {

                    logger.debug(String.format(
                            "PaperCut shared account [%s] "
                                    + "adjustment [%s] comment: %s",
                            papercutServerProxy.composeSharedAccountName(
                                    topAccountName, subAccountName),
                            papercutAdjustment.toPlainString(),
                            klasTrxComment));
                }

                PAPERCUT_SERVICE.lazyAdjustSharedAccount(papercutServerProxy,
                        topAccountName, subAccountName, papercutAdjustment,
                        klasTrxComment);

            } else {

                final String userCopiesComment = trxCommentProcessor
                        .processUserTrx(trx.getExtDetails(), weight);

                /*
                 * Get the user of the transaction.
                 */
                final User user = USER_ACCOUNT_DAO
                        .findByAccountId(trx.getAccount().getId()).getUser();
                /*
                 * Adjust Personal Account.
                 */
                if (logger.isDebugEnabled()) {

                    logger.debug(String.format(
                            "PaperCut personal account [%s] "
                                    + "adjustment [%s] comment [%s]",
                            user.getUserId(),
                            papercutAdjustment.toPlainString(),
                            userCopiesComment.toString()));
                }

                try {
                    PAPERCUT_SERVICE.adjustUserAccountBalance(
                            papercutServerProxy, user.getUserId(),
                            papercutAccountResolver.getUserAccountName(),
                            papercutAdjustment, userCopiesComment.toString());

                } catch (PaperCutException e) {
                    logger.error(String.format(
                            "PaperCut adjustment [%s] skipped: %s",
                            papercutAdjustment.toPlainString(),
                            e.getMessage()));
                }
            }

            /*
             * Notify implementers.
             */
            onAccountTrx(trx, weightedCost, isDocInAccountTrx, docLogOut);
        }

        /*
         * Create a transaction in the shared Jobs account with a comment of
         * formatted job data.
         */
        final String jobTrxComment = trxCommentProcessor.exitProcess();

        PAPERCUT_SERVICE.lazyAdjustSharedAccount(papercutServerProxy,
                papercutAccountResolver.getSharedParentAccountName(),
                papercutAccountResolver.getSharedJobsAccountName(),
                weightTotalCost.negate(), jobTrxComment);
    }

}
