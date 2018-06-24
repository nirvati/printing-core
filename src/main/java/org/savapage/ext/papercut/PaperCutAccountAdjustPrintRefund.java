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

import org.savapage.core.jpa.AccountTrx;
import org.savapage.core.jpa.CostChange;
import org.savapage.core.jpa.DocLog;
import org.slf4j.Logger;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class PaperCutAccountAdjustPrintRefund
        extends PaperCutAccountAdjustPattern {

    /**
     *
     * @param papercutServerProxy
     *            The {@link PaperCutServerProxy}.
     * @param papercutAccountResolver
     *            The {@link PaperCutAccountResolver}.
     * @param logger
     *            The {@link Logger} listening to log events.
     */
    public PaperCutAccountAdjustPrintRefund(
            final PaperCutServerProxy papercutServerProxy,
            final PaperCutAccountResolver papercutAccountResolver,
            final Logger logger) {

        super(papercutServerProxy, papercutAccountResolver, logger);
    }

    /**
     * Creates PaperCut transactions from a SavaPage refund {@link CostChange}.
     *
     * @param chg
     *            The {@link CostChange}.
     * @throws PaperCutException
     *             When a PaperCut error occurs.
     */
    public void process(final CostChange chg) throws PaperCutException {

        final DocLog docLog = chg.getDocLog();

        final BigDecimal weightTotalCost = chg.getChgAmount();

        final int weightTotal =
                docLog.getDocOut().getPrintOut().getNumberOfCopies();

        final BigDecimal costPerCopy = ACCOUNTING_SERVICE
                .calcCostPerPrintedCopy(weightTotalCost, weightTotal);

        /*
         * Create transaction comment processor.
         */
        final PaperCutPrintCommentProcessor trxCommentProcessor =
                new PaperCutPrintCommentProcessor(docLog, docLog, weightTotal,
                        true);

        trxCommentProcessor.initProcess();

        /*
         * Adjust the Personal and Shared Accounts in PaperCut.
         */
        for (final AccountTrx trx : chg.getTransactions()) {
            final BigDecimal papercutAdjustment = trx.getAmount();
            onAdjustSharedAccount(trx, trxCommentProcessor, papercutAdjustment,
                    costPerCopy);
        }

        this.onExit(trxCommentProcessor, weightTotalCost);
    }

}
