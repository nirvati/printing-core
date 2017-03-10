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

import org.savapage.core.jpa.AccountTrx;
import org.savapage.core.jpa.DocLog;
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
    private static final AccountingService ACCOUNTING_SERVICE =
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

    @Override
    protected void onAccountTrx(final AccountTrx trx,
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
