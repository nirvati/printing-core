/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2018 Datraverse B.V.
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
import java.math.RoundingMode;

import org.savapage.core.dao.UserAccountDao;
import org.savapage.core.jpa.Account;
import org.savapage.core.jpa.Account.AccountTypeEnum;
import org.savapage.core.jpa.AccountTrx;
import org.savapage.core.jpa.User;
import org.savapage.core.jpa.UserAccount;
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

    /** */
    private static final UserAccountDao USER_ACCOUNT_DAO =
            ServiceContext.getDaoContext().getUserAccountDao();

    /** */
    protected static final AccountingService ACCOUNTING_SERVICE =
            ServiceContext.getServiceFactory().getAccountingService();

    /** */
    private static final PaperCutService PAPERCUT_SERVICE =
            ServiceContext.getServiceFactory().getPaperCutService();

    /** */
    private final PaperCutServerProxy papercutServerProxy;
    /** */
    private final PaperCutAccountResolver papercutAccountResolver;
    /** */
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
     * Notifies a shared account adjustment to be done in PaperCut.
     *
     * @param trx
     *            The transaction.
     * @param trxCommentProcessor
     *            The comment processor.
     * @param papercutAdjustment
     *            PaperCut adjustment (just this transaction).
     * @param costPerCopy
     *            Cost per copy of print job (all copies).
     * @throws PaperCutException
     *             When error.
     */
    protected final void onAdjustSharedAccount(final AccountTrx trx,
            final PaperCutPrintCommentProcessor trxCommentProcessor,
            final BigDecimal papercutAdjustment, final BigDecimal costPerCopy)
            throws PaperCutException {

        final Account account = trx.getAccount();

        final int copies;

        /*
         * PaperCut account adjustment.
         */
        final AccountTypeEnum accountType =
                AccountTypeEnum.valueOf(account.getAccountType());

        if (accountType == AccountTypeEnum.SHARED
                || accountType == AccountTypeEnum.GROUP) {

            /*
             * Adjust Shared [Parent]/[klas|group|shared] Account.
             */

            final String accountNameParent;

            if (accountType == AccountTypeEnum.SHARED
                    && account.getParent() != null) {
                accountNameParent = account.getParent().getName();
            } else {
                accountNameParent = null;
            }

            /*
             * Get the top account name as used in PaperCut.
             */
            final String topAccountName =
                    papercutAccountResolver.getSharedParentAccountName();

            final String subAccountName =
                    papercutAccountResolver.composeSharedSubAccountName(
                            accountType, account.getName(), accountNameParent);

            final String klasName = papercutAccountResolver
                    .getKlasFromAccountName(subAccountName);

            if (costPerCopy.compareTo(BigDecimal.ZERO) == 0) {
                copies = 0;
            } else {
                copies = ACCOUNTING_SERVICE
                        .calcPrintedCopies(papercutAdjustment, costPerCopy, 0)
                        .intValue();
            }

            final String klasTrxComment =
                    trxCommentProcessor.processKlasTrx(klasName, copies);

            if (logger.isDebugEnabled()) {

                logger.debug(String.format(
                        "PaperCut shared account [%s] "
                                + "adjustment [%s] comment: %s",
                        papercutServerProxy.composeSharedAccountName(
                                topAccountName, subAccountName),
                        papercutAdjustment.toPlainString(), klasTrxComment));
            }

            PAPERCUT_SERVICE.lazyAdjustSharedAccount(papercutServerProxy,
                    topAccountName, subAccountName, papercutAdjustment,
                    klasTrxComment);

        } else {

            if (costPerCopy.compareTo(BigDecimal.ZERO) == 0) {
                copies = 0;
            } else {
                final BigDecimal copiesDecimal = ACCOUNTING_SERVICE
                        .calcPrintedCopies(papercutAdjustment, costPerCopy, 2);
                copies = copiesDecimal.setScale(0, RoundingMode.CEILING)
                        .intValue();
            }

            final String userCopiesComment = trxCommentProcessor
                    .processUserTrx(trx.getExtDetails(), copies);

            /*
             * Get the user of the transaction.
             */
            final UserAccount userAccount =
                    USER_ACCOUNT_DAO.findByAccountId(trx.getAccount().getId());

            if (userAccount == null) {
                throw new IllegalStateException(String.format(
                        "User Account not found: %s", userCopiesComment));
            }

            final User user = userAccount.getUser();

            /*
             * Adjust Personal Account.
             */
            if (logger.isDebugEnabled()) {

                logger.debug(String.format(
                        "PaperCut personal account [%s] "
                                + "adjustment [%s] comment [%s]",
                        user.getUserId(), papercutAdjustment.toPlainString(),
                        userCopiesComment.toString()));
            }

            try {
                PAPERCUT_SERVICE.adjustUserAccountBalance(papercutServerProxy,
                        user.getUserId(),
                        papercutAccountResolver.getUserAccountName(),
                        papercutAdjustment, userCopiesComment.toString());

            } catch (PaperCutException e) {
                logger.error(String.format(
                        "PaperCut adjustment [%s] skipped: %s",
                        papercutAdjustment.toPlainString(), e.getMessage()));
            }
        }
    }

    /**
     * Notifies exit of process.
     *
     * @param trxCommentProcessor
     *            The {@link PaperCutPrintCommentProcessor}.
     * @param totalAdjustment
     *            The total adjustment.
     * @throws PaperCutException
     *             When a PaperCut error occurs.
     */
    protected final void onExit(
            final PaperCutPrintCommentProcessor trxCommentProcessor,
            final BigDecimal totalAdjustment) throws PaperCutException {
        /*
         * Create a transaction in the shared Jobs account with a comment of
         * formatted job data.
         */
        final String jobTrxComment = trxCommentProcessor.exitProcess();

        PAPERCUT_SERVICE.lazyAdjustSharedAccount(papercutServerProxy,
                papercutAccountResolver.getSharedParentAccountName(),
                papercutAccountResolver.getSharedJobsAccountName(),
                totalAdjustment, jobTrxComment);
    }

}
