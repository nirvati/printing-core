/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2016 Datraverse B.V.
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
package org.savapage.ext.papercut;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.savapage.core.concurrent.ReadWriteLockEnum;
import org.savapage.core.dao.DaoContext;
import org.savapage.core.dao.DocLogDao;
import org.savapage.core.dao.enums.DocLogProtocolEnum;
import org.savapage.core.dao.enums.ExternalSupplierEnum;
import org.savapage.core.dao.enums.ExternalSupplierStatusEnum;
import org.savapage.core.jpa.DocLog;
import org.savapage.core.services.ServiceContext;
import org.savapage.ext.ExtSupplierConnectException;
import org.savapage.ext.ExtSupplierException;
import org.savapage.ext.papercut.services.PaperCutService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class PaperCutPrintJobMonitor {

    /**
     * The logger.
     */
    private static final Logger LOGGER = LoggerFactory
            .getLogger(PaperCutPrintJobMonitor.class);

    /**
     * .
     */
    private static final PaperCutService PAPERCUT_SERVICE = ServiceContext
            .getServiceFactory().getPaperCutService();

    /**
     * .
     */
    private final PaperCutServerProxy papercutServerProxy;

    /**
     * .
     */
    private final PaperCutDbProxy papercutDbProxy;

    /**
     * .
     */
    private final DocLogDao.ListFilter listFilterPendingExt;

    /**
     * .
     */
    private final PaperCutPrintJobListener statusListener;

    /**
     *
     * @param externalSupplier
     * @param papercutServerProxy
     * @param papercutDbProxy
     * @param statusListener
     */
    public PaperCutPrintJobMonitor(final ExternalSupplierEnum externalSupplier,
            final PaperCutServerProxy papercutServerProxy,
            final PaperCutDbProxy papercutDbProxy,
            final PaperCutPrintJobListener statusListener) {

        this.listFilterPendingExt = new DocLogDao.ListFilter();
        this.listFilterPendingExt.setExternalSupplier(externalSupplier);
        this.listFilterPendingExt
                .setExternalStatus(ExternalSupplierStatusEnum.PENDING_EXT
                        .toString());
        this.listFilterPendingExt.setProtocol(DocLogProtocolEnum.IPP);

        this.papercutServerProxy = papercutServerProxy;
        this.papercutDbProxy = papercutDbProxy;

        this.statusListener = statusListener;
    }

    /**
     * Logs content of a {@link PaperCutPrinterUsageLog} object.
     *
     * @param usageLog
     *            The object to log.
     */
    private static void debugLog(final PaperCutPrinterUsageLog usageLog) {

        if (LOGGER.isDebugEnabled()) {
            final StringBuilder msg = new StringBuilder();
            msg.append(usageLog.getDocumentName()).append(" | printed [")
                    .append(usageLog.isPrinted()).append("] cancelled [")
                    .append(usageLog.isCancelled()).append("] deniedReason [")
                    .append(usageLog.getDeniedReason()).append("] usageCost[")
                    .append(usageLog.getUsageCost()).append("]");
            LOGGER.debug(msg.toString());
        }
    }

    /**
     *
     * @throws PaperCutException
     * @throws ExtSupplierException
     * @throws ExtSupplierConnectException
     */
    public final void process() throws PaperCutException, ExtSupplierException,
            ExtSupplierConnectException {

        final DocLogDao doclogDao =
                ServiceContext.getDaoContext().getDocLogDao();

        final Map<String, DocLog> uniquePaperCutDocNames = new HashMap<>();

        for (final DocLog docLog : doclogDao.getListChunk(listFilterPendingExt)) {
            uniquePaperCutDocNames.put(docLog.getTitle(), docLog);
        }

        if (uniquePaperCutDocNames.isEmpty()) {
            return;
        }

        final List<PaperCutPrinterUsageLog> papercutLogList =
                PAPERCUT_SERVICE.getPrinterUsageLog(papercutDbProxy,
                        uniquePaperCutDocNames.keySet());

        final Set<String> paperCutDocNamesHandled = new HashSet<>();

        for (final PaperCutPrinterUsageLog papercutLog : papercutLogList) {

            paperCutDocNamesHandled.add(papercutLog.getDocumentName());

            debugLog(papercutLog);

            final DocLog docLog =
                    uniquePaperCutDocNames.get(papercutLog.getDocumentName());

            final ExternalSupplierStatusEnum printStatus;
            boolean isDocumentTooLarge = false;

            /*
             * Database transaction.
             */
            ReadWriteLockEnum.DATABASE_READONLY.setReadLock(true);

            final DaoContext daoContext = ServiceContext.getDaoContext();

            try {

                if (!daoContext.isTransactionActive()) {
                    daoContext.beginTransaction();
                }

                if (papercutLog.isPrinted()) {

                    printStatus = ExternalSupplierStatusEnum.COMPLETED;

                    this.statusListener.onPaperCutPrintJobCompleted(
                            papercutServerProxy, docLog, papercutLog,
                            printStatus);

                } else {

                    if (papercutLog.getDeniedReason().contains("TIMEOUT")) {

                        printStatus = ExternalSupplierStatusEnum.EXPIRED;

                    } else if (papercutLog.getDeniedReason().contains(
                            "DOCUMENT_TOO_LARGE")) {

                        printStatus = ExternalSupplierStatusEnum.CANCELLED;
                        isDocumentTooLarge = true;

                    } else {
                        printStatus = ExternalSupplierStatusEnum.CANCELLED;
                    }

                    this.statusListener.onPaperCutPrintJobCancelled(docLog,
                            papercutLog, printStatus);
                }

                daoContext.commit();

            } finally {

                daoContext.rollback();

                ReadWriteLockEnum.DATABASE_READONLY.setReadLock(false);
            }

            this.statusListener.onPaperCutPrintJobProcessed(docLog,
                    papercutLog, printStatus, isDocumentTooLarge);

        } // end-for

        this.statusListener.onPaperCutPrintJobsNotFound(uniquePaperCutDocNames,
                paperCutDocNamesHandled);
    }

}
