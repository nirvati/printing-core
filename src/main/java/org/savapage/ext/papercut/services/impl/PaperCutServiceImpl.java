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
package org.savapage.ext.papercut.services.impl;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.savapage.core.SpInfo;
import org.savapage.core.cometd.AdminPublisher;
import org.savapage.core.cometd.PubLevelEnum;
import org.savapage.core.cometd.PubTopicEnum;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp;
import org.savapage.core.dao.enums.DaoEnumHelper;
import org.savapage.core.dao.enums.ExternalSupplierEnum;
import org.savapage.core.dao.enums.PrintModeEnum;
import org.savapage.core.jpa.DocLog;
import org.savapage.core.jpa.PrintOut;
import org.savapage.core.print.proxy.AbstractProxyPrintReq;
import org.savapage.core.print.proxy.ProxyPrintJobChunk;
import org.savapage.core.services.helpers.ExternalSupplierInfo;
import org.savapage.core.services.helpers.PrintSupplierData;
import org.savapage.core.services.helpers.ProxyPrintCostDto;
import org.savapage.core.services.helpers.ThirdPartyEnum;
import org.savapage.core.services.impl.AbstractService;
import org.savapage.ext.papercut.DelegatedPrintPeriodDto;
import org.savapage.ext.papercut.PaperCutAccountTrx;
import org.savapage.ext.papercut.PaperCutDb;
import org.savapage.ext.papercut.PaperCutDbProxy;
import org.savapage.ext.papercut.PaperCutDbProxyPool;
import org.savapage.ext.papercut.PaperCutException;
import org.savapage.ext.papercut.PaperCutHelper;
import org.savapage.ext.papercut.PaperCutIntegrationEnum;
import org.savapage.ext.papercut.PaperCutPrinterUsageLog;
import org.savapage.ext.papercut.PaperCutServerProxy;
import org.savapage.ext.papercut.PaperCutUser;
import org.savapage.ext.papercut.services.PaperCutService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class PaperCutServiceImpl extends AbstractService
        implements PaperCutService {

    /**
     * The logger.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(PaperCutServiceImpl.class);

    /** */
    private PaperCutDbProxyPool dbProxyPool = null;

    @Override
    public boolean isExtPaperCutPrint(final String printerName) {
        /*
         * Is printer managed by PaperCut?
         */
        final ThirdPartyEnum thirdParty =
                proxyPrintService().getExtPrinterManager(printerName);

        if (thirdParty == null || thirdParty != ThirdPartyEnum.PAPERCUT) {
            return false;
        }

        /*
         * PaperCut Print Monitoring enabled?
         */
        if (!ConfigManager.isPaperCutPrintEnabled()) {
            return false;
        }

        return true;
    }

    @Override
    public PaperCutIntegrationEnum getPrintIntegration() {

        if (ConfigManager.isPaperCutPrintEnabled()) {

            final ConfigManager cm = ConfigManager.instance();

            if (cm.isConfigValue(IConfigProp.Key.PROXY_PRINT_DELEGATE_ENABLE)) {
                if (cm.isConfigValue(
                        IConfigProp.Key.PROXY_PRINT_DELEGATE_PAPERCUT_ENABLE)) {
                    return PaperCutIntegrationEnum.DELEGATED_PRINT;
                }
            } else {
                if (cm.isConfigValue(
                        IConfigProp.Key.PROXY_PRINT_PERSONAL_PAPERCUT_ENABLE)) {
                    return PaperCutIntegrationEnum.PERSONAL_PRINT;
                }
            }
        }

        return PaperCutIntegrationEnum.NONE;
    }

    @Override
    public boolean isMonitorPaperCutPrintStatus(final String printerName,
            final boolean isNonPersonalPrintJob) {

        if (!this.isExtPaperCutPrint(printerName)) {
            return false;
        }

        final PaperCutIntegrationEnum integration = this.getPrintIntegration();

        if (isNonPersonalPrintJob) {
            return integration == PaperCutIntegrationEnum.DELEGATED_PRINT;
        } else {
            return integration == PaperCutIntegrationEnum.DELEGATED_PRINT
                    || integration == PaperCutIntegrationEnum.PERSONAL_PRINT;
        }
    }

    @Override
    public boolean isExtPaperCutPrintRefund(final DocLog docLog) {

        if (!ConfigManager.isPaperCutPrintEnabled()) {
            return false;
        }

        final Boolean extDataClientCostTrx; // Mantis #1023
        final ThirdPartyEnum extDataClient;

        if (docLog.getExternalData() == null) {

            extDataClient = null;
            extDataClientCostTrx = null;

        } else {

            final PrintSupplierData extData =
                    PrintSupplierData.createFromData(docLog.getExternalData());

            extDataClient = extData.getClient();

            if (extDataClient == ThirdPartyEnum.PAPERCUT) {
                extDataClientCostTrx = extData.getClientCostTrx();
            } else {
                extDataClientCostTrx = null;
            }
        }

        /*
         * DocLog must be related to PaperCut client. But, relax for legacy
         * objects.
         */
        if (extDataClient != null && extDataClient != ThirdPartyEnum.PAPERCUT) {
            return false;
        }

        /*
         * extDataClientCostTrx is null for legacy objects.
         */
        if (extDataClientCostTrx != null) {
            return extDataClientCostTrx.booleanValue();
        }

        /*
         * Handle legacy objects.
         */
        final PrintOut printOut = docLog.getDocOut().getPrintOut();
        final PrintModeEnum printMode = DaoEnumHelper.getPrintMode(printOut);

        final boolean isJobTicket =
                EnumSet.of(PrintModeEnum.TICKET, PrintModeEnum.TICKET_C,
                        PrintModeEnum.TICKET_E).contains(printMode);

        final boolean isExtPaperCutPrinter =
                this.isExtPaperCutPrint(printOut.getPrinter().getPrinterName());

        final boolean isRefundable;

        if (isJobTicket) {
            /*
             * Since Job Ticket cost are leading, and PaperCut transactions were
             * created by SavaPage for a PaperCut managed printer.
             *
             * Now, we assume PaperCut management of printer did not change
             * since the original print.
             */
            isRefundable = isExtPaperCutPrinter;

        } else {
            /*
             * Whether PaperCut transactions were created by SavaPage was
             * dependent on printer being PaperCut managed, and PaperCut
             * Integration mode being DELEGATED_PRINT.
             *
             * Now, we assume PaperCut management of printer, and PaperCut
             * Integration mode, did not change since the original print.
             */
            isRefundable = isExtPaperCutPrinter && this.getPrintIntegration() //
            == PaperCutIntegrationEnum.DELEGATED_PRINT;
        }

        return isRefundable;
    }

    @Override
    public ExternalSupplierInfo

            createExternalSupplierInfo(final AbstractProxyPrintReq printReq) {

        final ExternalSupplierInfo supplierInfo;

        supplierInfo = new ExternalSupplierInfo();
        supplierInfo.setId(printReq.getJobTicketTag());
        supplierInfo.setSupplier(ExternalSupplierEnum.SAVAPAGE);

        final int weightTotal;

        if (printReq.getAccountTrxInfoSet() == null) {
            /*
             * Personal Print.
             */
            weightTotal = printReq.getNumberOfCopies();
        } else {
            /*
             * Delegated Print.
             */
            weightTotal = printReq.getAccountTrxInfoSet().getWeightTotal();
        }

        final PrintSupplierData printSupplierData = new PrintSupplierData();

        printSupplierData.setClient(ThirdPartyEnum.PAPERCUT);
        printSupplierData.setWeightTotal(Integer.valueOf(weightTotal));

        supplierInfo.setData(printSupplierData);

        return supplierInfo;
    }

    /**
     * Prepares the base properties of a {@link AbstractProxyPrintReq}, and
     * {@link ExternalSupplierInfo} for External PaperCut Print Status
     * monitoring and notification to an external supplier.
     *
     * @param printReq
     *            {@link AbstractProxyPrintReq}.
     * @param supplierInfo
     *            {@link ExternalSupplierInfo}.
     * @param printMode
     *            {@link PrintModeEnum}.
     */
    private void prepareForExtPaperCutCommon(
            final AbstractProxyPrintReq printReq,
            final ExternalSupplierInfo supplierInfo,
            final PrintModeEnum printMode) {

        supplierInfo.setStatus(
                PaperCutHelper.getInitialPendingJobStatus().toString());

        printReq.setPrintMode(printMode);
        printReq.setSupplierInfo(supplierInfo);
    }

    @Override
    public void prepareForExtPaperCutRetry(final AbstractProxyPrintReq printReq,
            final ExternalSupplierInfo supplierInfo,
            final PrintModeEnum printMode) {

        prepareForExtPaperCutCommon(printReq, supplierInfo, printMode);
        printReq.setJobName(PaperCutHelper
                .renewProxyPrintJobNameUUID(printReq.getJobName()));
    }

    @Override
    public void prepareForExtPaperCut(final AbstractProxyPrintReq printReq,
            final ExternalSupplierInfo supplierInfo,
            final PrintModeEnum printMode) {

        prepareForExtPaperCutCommon(printReq, supplierInfo, printMode);

        /*
         * Encode job name into PaperCut format.
         */
        if (supplierInfo.getAccount() == null) {
            printReq.setJobName(PaperCutHelper
                    .encodeProxyPrintJobName(printReq.getJobName()));
        } else {
            printReq.setJobName(PaperCutHelper.encodeProxyPrintJobName(
                    supplierInfo.getAccount(), supplierInfo.getId(),
                    printReq.getJobName()));
        }

        /*
         * Set all cost to zero, since cost is applied after PaperCut reports
         * that jobs are printed successfully.
         */
        printReq.setCostResult(new ProxyPrintCostDto());

        if (printReq.getJobChunkInfo() != null) {
            for (final ProxyPrintJobChunk chunk : printReq.getJobChunkInfo()
                    .getChunks()) {
                chunk.setCostResult(new ProxyPrintCostDto());
                chunk.setJobName(PaperCutHelper
                        .encodeProxyPrintJobName(chunk.getJobName()));
            }
        }
    }

    @Override
    public PaperCutUser findUser(final PaperCutServerProxy papercut,
            final String userId) {
        return papercut.getUser(userId);
    }

    @Override
    public void lazyAdjustSharedAccount(final PaperCutServerProxy papercut,
            final String topAccountName, final String subAccountName,
            final BigDecimal adjustment, final String comment)
            throws PaperCutException {

        try {

            papercut.adjustSharedAccountAccountBalance(topAccountName,
                    subAccountName, adjustment.doubleValue(), comment);

        } catch (PaperCutException e) {

            final String composedSharedAccountName = papercut
                    .composeSharedAccountName(topAccountName, subAccountName);

            if (LOGGER.isInfoEnabled()) {

                LOGGER.info(String.format(
                        "Shared account [%s] does not exist: added new.",
                        composedSharedAccountName));
            }

            papercut.addNewSharedAccount(topAccountName, subAccountName);

            papercut.adjustSharedAccountAccountBalance(topAccountName,
                    subAccountName, adjustment.doubleValue(), comment);

            AdminPublisher.instance().publish(PubTopicEnum.PAPERCUT,
                    PubLevelEnum.CLEAR,
                    String.format("PaperCut account '%s' created.",
                            composedSharedAccountName));
        }
    }

    @Override
    public void adjustUserAccountBalance(final PaperCutServerProxy papercut,
            final String username, final String userAccountName,
            final BigDecimal adjustment, final String comment)
            throws PaperCutException {

        papercut.adjustUserAccountBalance(username, userAccountName,
                adjustment.doubleValue(), comment);
    }

    @Override
    public List<PaperCutPrinterUsageLog> getPrinterUsageLog(
            final PaperCutDbProxy papercut, final Set<String> uniqueDocNames) {
        return papercut.getPrinterUsageLog(papercut.getConnection(),
                uniqueDocNames);
    }

    @Override
    public void createDelegatorPrintCostCsv(final File file,
            final DelegatedPrintPeriodDto dto) throws IOException {

        Connection connection = null;

        try {
            connection = this.dbProxyPool.openConnection();
            this.dbProxyPool.getDelegatorPrintCostCsv(connection, file, dto);
        } finally {
            this.dbProxyPool.closeConnection(connection);
        }
    }

    @Override
    public void resetDbConnectionPool() {
        this.shutdown();
        this.start();
    }

    @Override
    public void start() {

        if (this.dbProxyPool != null) {
            throw new IllegalStateException(
                    "Database connection pool is already started.");
        }

        if (ConfigManager.isPaperCutPrintEnabled()) {
            this.dbProxyPool =
                    new PaperCutDbProxyPool(ConfigManager.instance(), true);
            SpInfo.instance().log("PaperCut database connection pool created.");
        }
    }

    @Override
    public void shutdown() {
        if (this.dbProxyPool != null) {
            this.dbProxyPool.close();
            this.dbProxyPool = null;
            SpInfo.instance().log("PaperCut database connection pool closed.");
        }
    }

    @Override
    public long getAccountTrxCount(final PaperCutDb.TrxFilter filter) {

        Connection connection = null;

        try {
            connection = this.dbProxyPool.openConnection();
            return this.dbProxyPool.getAccountTrxCount(connection, filter);
        } finally {
            if (connection != null) {
                this.dbProxyPool.closeConnection(connection);
            }
        }
    }

    @Override
    public List<PaperCutAccountTrx> getAccountTrxListChunk(
            final PaperCutDb.TrxFilter filter, final Integer startPosition,
            final Integer maxResults, final PaperCutDb.Field orderBy,
            final boolean sortAscending) {

        Connection connection = null;

        try {
            connection = this.dbProxyPool.openConnection();
            return this.dbProxyPool.getAccountTrxChunk(connection, filter,
                    startPosition, maxResults, orderBy, sortAscending);
        } finally {
            if (connection != null) {
                this.dbProxyPool.closeConnection(connection);
            }
        }
    }

}
