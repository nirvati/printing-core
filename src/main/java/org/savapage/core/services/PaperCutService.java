/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2014 Datraverse B.V.
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
package org.savapage.core.services;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import org.savapage.core.papercut.PaperCutDbProxy;
import org.savapage.core.papercut.PaperCutException;
import org.savapage.core.papercut.PaperCutPrinterUsageLog;
import org.savapage.core.papercut.PaperCutServerProxy;
import org.savapage.core.papercut.PaperCutUser;

/**
 * Services for PaperCut Print Management Server.
 *
 * <p>
 * See: <a href="http://www.papercut.com">www.papercut.com</a>
 * </p>
 *
 * @since 0.9.6
 * @author Datraverse B.V.
 *
 */
public interface PaperCutService {

    /**
     * Finds a PaperCut user.
     *
     * @param papercut
     *            The {@link PaperCutServerProxy}.
     * @param userId
     *            The unique user id of the user.
     * @return The {@link PaperCutUser} instance, or {@code null} when not
     *         found.
     */
    PaperCutUser findUser(PaperCutServerProxy papercut, String userId);

    /**
     * Lazy creates and adjusts a shared account's account balance by an
     * adjustment amount. An adjustment may be positive (add to the account) or
     * negative (subtract from the account).
     * <p>
     * Note: when the account does not exist it is created.
     * </p>
     *
     * @param papercut
     *            The {@link PaperCutServerProxy}.
     * @param topAccountName
     *            The full name of the top shared account to adjust (or create).
     * @param subAccountName
     *            The full name of the sub shared account to adjust (or create).
     *            Value can be {@code null}.
     * @param adjustment
     *            The adjustment amount. Positive to add credit and negative to
     *            subtract.
     * @param comment
     *            A user defined comment to associated with the transaction.
     *            This may be a {@code null} string.
     * @throws PaperCutException
     *             When the shared account could not be adjusted (created).
     */
    void lazyAdjustSharedAccount(PaperCutServerProxy papercut,
            String topAccountName, String subAccountName,
            BigDecimal adjustment, String comment) throws PaperCutException;

    /**
     * Adjusts a user's built-in/default account balance by an adjustment
     * amount. An adjustment may be positive (add to the user's account) or
     * negative (subtract from the account).
     *
     * @param papercut
     *            The {@link PaperCutServerProxy}.
     * @param username
     *            The username associated with the user who's account is to be
     *            adjusted.
     * @param adjustment
     *            The adjustment amount. Positive to add credit and negative to
     *            subtract.
     * @param comment
     *            A user defined comment to be associated with the transaction.
     *            This may be a null string.
     * @throws PaperCutException
     *             When the user (account) does not exist.
     */
    void adjustUserAccountBalance(PaperCutServerProxy papercut,
            String username, BigDecimal adjustment, String comment)
            throws PaperCutException;

    /**
     * Gets the {@link PaperCutPrinterUsageLog} for unique document names.
     *
     * @param papercut
     *            The {@link PaperCutDbProxy}.
     * @param uniqueDocNames
     *            A set with document names.
     * @return A list with an {@link PaperCutPrinterUsageLog} object for each
     *         title in the input set.
     */
    List<PaperCutPrinterUsageLog> getPrinterUsageLog(PaperCutDbProxy papercut,
            Set<String> uniqueDocNames);

}
