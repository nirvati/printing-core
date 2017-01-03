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

import org.savapage.core.dao.AccountDao;
import org.savapage.core.dao.AccountTrxDao;
import org.savapage.core.jpa.Account;
import org.savapage.core.jpa.AccountTrx;
import org.savapage.core.jpa.DocLog;
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
     *
     */
    private static final AccountDao ACCOUNT_DAO =
            ServiceContext.getDaoContext().getAccountDao();

    /**
     *
     */
    private static final AccountTrxDao ACCOUNT_TRX_DAO =
            ServiceContext.getDaoContext().getAccountTrxDao();

    /**
     *
     * @param papercutServerProxy
     * @param papercutAccountResolver
     * @param logger
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

        final Account account = trx.getAccount();

        account.setBalance(account.getBalance().subtract(weightedCost));
        ACCOUNT_DAO.update(account);

        /*
         * Update AccountTrx.
         */
        trx.setAmount(weightedCost.negate());
        trx.setBalance(account.getBalance());

        trx.setTransactedBy(ServiceContext.getActor());
        trx.setTransactionDate(ServiceContext.getTransactionDate());

        if (isDocInAccountTrx) {
            // Move from DocLog source to target.
            trx.setDocLog(docLogOut);
        }

        ACCOUNT_TRX_DAO.update(trx);
    }

}
