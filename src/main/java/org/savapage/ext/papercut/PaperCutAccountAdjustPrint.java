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
import org.savapage.core.jpa.AccountTrx;
import org.savapage.core.jpa.DocIn;
import org.savapage.core.jpa.DocLog;
import org.savapage.core.jpa.DocOut;
import org.savapage.core.services.AccountingService;
import org.savapage.core.services.ServiceContext;
import org.slf4j.Logger;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class PaperCutAccountAdjustPrint
        extends PaperCutAccountAdjustPattern {

    /**
     * .
     */
    protected static final AccountingService ACCOUNTING_SERVICE =
            ServiceContext.getServiceFactory().getAccountingService();

    /**
     *
     * @param papercutServerProxy
     *            The {@link PaperCutServerProxy}.
     * @param papercutAccountResolver
     *            The {@link PaperCutAccountResolver}.
     * @param logger
     *            The {@link Logger} listening to log events.
     */
    public PaperCutAccountAdjustPrint(
            final PaperCutServerProxy papercutServerProxy,
            final PaperCutAccountResolver papercutAccountResolver,
            final Logger logger) {

        super(papercutServerProxy, papercutAccountResolver, logger);
    }

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
    public void process(final DocLog docLogTrx, final DocLog docLogOut,
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

            /*
             * PaperCut account adjustment.
             */
            final BigDecimal papercutAdjustment = weightedCost.negate();

            this.onAdjustSharedAccount(trx, trxCommentProcessor,
                    papercutAdjustment);

            /*
             * Notify SavaPage.
             */
            this.onAccountTrx(trx, weightedCost, isDocInAccountTrx, docLogOut);
        }

        this.onExit(trxCommentProcessor, weightTotalCost.negate());
    }

    /**
     * Notifies an account transaction that was adjusted in PaperCut.
     *
     * @param trx
     *            The {@link AccountTrx}.
     * @param weightedCost
     *            The weighted cost.
     * @param isDocInAccountTrx
     *            {@code true} when the {@link AccountTrx} is linked with the
     *            {@link DocLog} of the {@link DocIn}, {@code false} when linked
     *            with the {@link DocLog} of the {@link DocOut}.
     * @param docLogOut
     *            The {@link DocLog} container of the {@link DocOut} object.
     */
    private void onAccountTrx(final AccountTrx trx,
            final BigDecimal weightedCost, final boolean isDocInAccountTrx,
            final DocLog docLogOut) {

        final DocLog trxDocLog;

        if (isDocInAccountTrx) {
            // Move from DocLog source to target.
            trxDocLog = docLogOut;
        } else {
            trxDocLog = null;
        }

        ACCOUNTING_SERVICE.chargeAccountTrxAmount(trx, weightedCost, trxDocLog);
    }

}
