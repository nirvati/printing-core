/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2019 Datraverse B.V.
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
package org.savapage.ext.papercut.services;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import org.savapage.core.config.IConfigProp;
import org.savapage.core.dao.enums.ExternalSupplierEnum;
import org.savapage.core.dao.enums.PrintModeEnum;
import org.savapage.core.print.proxy.AbstractProxyPrintReq;
import org.savapage.core.services.StatefulService;
import org.savapage.core.services.helpers.ExternalSupplierInfo;
import org.savapage.ext.papercut.DelegatedPrintPeriodDto;
import org.savapage.ext.papercut.PaperCutAccountTrx;
import org.savapage.ext.papercut.PaperCutDb;
import org.savapage.ext.papercut.PaperCutDbProxy;
import org.savapage.ext.papercut.PaperCutException;
import org.savapage.ext.papercut.PaperCutIntegrationEnum;
import org.savapage.ext.papercut.PaperCutPrinterUsageLog;
import org.savapage.ext.papercut.PaperCutServerProxy;
import org.savapage.ext.papercut.PaperCutUser;

/**
 * Services for PaperCut Print Management Server.
 *
 * <p>
 * See: <a href="http://www.papercut.com">www.papercut.com</a>
 * </p>
 *
 * @author Rijk Ravestein
 *
 */
public interface PaperCutService extends StatefulService {

    /**
     * Checks if PaperCut Delegated or Personal Print is enabled (or none).
     *
     * @return {@link PaperCutIntegrationEnum}.
     */
    PaperCutIntegrationEnum getPrintIntegration();

    /**
     * Checks if PaperCut integration for Printer is applicable.
     *
     * @param printerName
     *            The CUPS printer name.
     * @return {@code false}, if PaperCut integration is disabled or printer is
     *         not managed by PaperCut.
     */
    boolean isExtPaperCutPrint(String printerName);

    /**
     * Checks if the status of a print job must be monitored in PaperCut.
     *
     * @param printerName
     *            The CUPS printer name.
     * @param isNonPersonalPrint
     *            If {@code true}, cost of print job is not charged on personal
     *            account, but on delegator account(s), and/or shared
     *            account(s).
     * @return {@code true} when print status must be monitored in PaperCut.
     */
    boolean isMonitorPaperCutPrintStatus(String printerName,
            boolean isNonPersonalPrint);

    /**
     * Prepares the {@link AbstractProxyPrintReq} for External PaperCut Print
     * Status monitoring and notification to an external supplier.
     * <p>
     * Note: all cost is set to zero, since cost is applied after PaperCut
     * reports that jobs are printed successfully.
     * </p>
     *
     * @param printReq
     *            The {@link AbstractProxyPrintReq}.
     * @param supplierInfo
     *            The {@link ExternalSupplierInfo}: when {@code null},
     *            {@link ExternalSupplierEnum#SAVAPAGE} is assumed.
     * @param printMode
     *            when {@code null}, {@link PrintModeEnum#PUSH} is assumed.
     */
    void prepareForExtPaperCut(AbstractProxyPrintReq printReq,
            ExternalSupplierInfo supplierInfo, PrintModeEnum printMode);

    /**
     * Prepares a {@link AbstractProxyPrintReq} for <i>retrying</i> with
     * External PaperCut Print Status monitoring and notification to an external
     * supplier.
     * <p>
     * Note: any cost is preserved.
     * </p>
     *
     * @param printReq
     *            The {@link AbstractProxyPrintReq}.
     * @param supplierInfo
     *            The {@link ExternalSupplierInfo}: when {@code null},
     *            {@link ExternalSupplierEnum#SAVAPAGE} is assumed.
     * @param printMode
     *            when {@code null}, {@link PrintModeEnum#PUSH} is assumed.
     * @return The {@link ExternalSupplierInfo} input, or when input
     *         {@code null}, the newly created instance.
     */
    ExternalSupplierInfo prepareForExtPaperCutRetry(
            AbstractProxyPrintReq printReq, ExternalSupplierInfo supplierInfo,
            PrintModeEnum printMode);

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
            String topAccountName, String subAccountName, BigDecimal adjustment,
            String comment) throws PaperCutException;

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
     * @param userAccountName
     *            Optional name of the user's personal account. If {@code null},
     *            the built-in default account is used. If multiple personal
     *            accounts is enabled the account name must be provided. *
     * @param adjustment
     *            The adjustment amount. Positive to add credit and negative to
     *            subtract.
     * @param comment
     *            A user defined comment to be associated with the transaction.
     *            This may be a null string.
     * @throws PaperCutException
     *             When the user (account) does not exist.
     */
    void adjustUserAccountBalance(PaperCutServerProxy papercut, String username,
            String userAccountName, BigDecimal adjustment, String comment)
            throws PaperCutException;

    /**
     * Gets the {@link PaperCutPrinterUsageLog} for unique document names.
     *
     * @param papercutDb
     *            The {@link PaperCutDbProxy}.
     * @param uniqueDocNames
     *            A set with document names.
     * @return A list with an {@link PaperCutPrinterUsageLog} object for each
     *         title in the input set.
     */
    List<PaperCutPrinterUsageLog> getPrinterUsageLog(PaperCutDbProxy papercutDb,
            Set<String> uniqueDocNames);

    /**
     * Creates a CSV file with Delegator Print costs.
     *
     * @param file
     *            The CSV file to create.
     * @param dto
     *            The {@link DelegatedPrintPeriodDto}.
     * @throws IOException
     *             When IO errors occur while writing the file.
     */
    void createDelegatorPrintCostCsv(File file, DelegatedPrintPeriodDto dto)
            throws IOException;

    /**
     * Gets the total number of user transactions.
     *
     * @param filter
     *            The transaction filter.
     * @return Number of transactions.
     */
    long getAccountTrxCount(PaperCutDb.TrxFilter filter);

    /**
     * Gets PaperCut personal account transactions.
     *
     * @param filter
     *            The transaction filter.
     * @param startPosition
     *            Zero-based start position of the chunk in total set. If
     *            {@code null}, start zero (0) is assumed.
     * @param maxResults
     *            Max chuck size. If {@code null}, size is unlimited.
     * @param orderBy
     *            The order-by field.
     * @param sortAscending
     *            If {@code true}, the list is sorted ascending on order-by
     *            field.
     * @return The list.
     */
    List<PaperCutAccountTrx> getAccountTrxListChunk(PaperCutDb.TrxFilter filter,
            Integer startPosition, Integer maxResults, PaperCutDb.Field orderBy,
            boolean sortAscending);

    /**
     * Recreates or closes PaperCut database connection pool depending on actual
     * {@link IConfigProp.Key} parameters.
     */
    void resetDbConnectionPool();
}
