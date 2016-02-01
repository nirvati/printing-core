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

import java.util.Map;
import java.util.Set;

import org.savapage.core.dao.enums.ExternalSupplierStatusEnum;
import org.savapage.core.jpa.AccountTrx;
import org.savapage.core.jpa.DocLog;
import org.savapage.core.jpa.DocOut;
import org.savapage.core.jpa.PrintOut;
import org.savapage.ext.ExtSupplierConnectException;
import org.savapage.ext.ExtSupplierException;

/**
 *
 * @author Rijk Ravestein
 *
 */
public interface PaperCutPrintJobListener {

    /**
     *
     * Notifies a cancelled PaperCut print job.
     * <ul>
     * <li>The {@link DocLog} target and source is updated with the
     * {@link ExternalSupplierStatusEnum}.</li>
     * <li>Publish Admin messages.</li>
     * </ul>
     *
     * @param docLogOut
     *            The SavaPage {@link DocLog} target holding the {@link DocOut}
     *            with the {@link PrintOut}.
     * @param papercutLog
     *            The {@link PaperCutPrinterUsageLog}.
     * @param printStatus
     *            The {@link ExternalSupplierStatusEnum}.
     */
    void onPaperCutPrintJobCancelled(DocLog docLogOut,
            PaperCutPrinterUsageLog papercutLog,
            ExternalSupplierStatusEnum printStatus);

    /**
     * Notifies a completed PaperCut print job.
     * <ul>
     * <li>The Personal and Shared SmartSchool\Klas accounts are lazy adjusted
     * in PaperCut and SavaPage.</li>
     * <li>The {@link AccountTrx} objects are moved from the {@link DocLog}
     * source to the {@link DocLog} target.</li>
     * <li>The {@link DocLog} target is updated with the
     * {@link ExternalSupplierStatusEnum}.</li>
     * <li>Publish Admin messages.</li>
     * </ul>
     *
     * @param papercutServerProxy
     *            The {@link PaperCutServerProxy}.
     * @param docLogOut
     *            The SavaPage {@link DocLog} target holding the {@link DocOut}
     *            with the {@link PrintOut}.
     * @param papercutLog
     *            The {@link PaperCutPrinterUsageLog}.
     * @param printStatus
     *            The {@link ExternalSupplierStatusEnum}.
     * @throws PaperCutException
     *             When a PaperCut error occurs.
     */
    void onPaperCutPrintJobCompleted(
            final PaperCutServerProxy papercutServerProxy,
            final DocLog docLogOut, final PaperCutPrinterUsageLog papercutLog,
            final ExternalSupplierStatusEnum printStatus)
            throws PaperCutException;

    /**
     * Notifies pending SavaPage print jobs that cannot be found in PaperCut:
     * status is set to {@link ExternalSupplierStatusEnum#ERROR} when SavaPage
     * print job is more then {@link #MAX_DAYS_WAIT_FOR_PAPERCUT_PRINT_LOG} old.
     *
     * @param papercutDocNamesToFind
     *            The PaperCut documents to find.
     * @param papercutDocNamesFound
     *            The PaperCut documents found.
     */
    void onPaperCutPrintJobsNotFound(
            final Map<String, DocLog> papercutDocNamesToFind,
            final Set<String> papercutDocNamesFound);

    /**
     * Notifies that the PaperCut print job has been processed and changes are
     * committed to the SavaPage database.
     *
     * @param docLogOut
     *            The SavaPage {@link DocLog} target holding the {@link DocOut}
     *            with the {@link PrintOut}.
     * @param papercutLog
     *            The {@link PaperCutPrinterUsageLog}.
     * @param printStatus
     *            The {@link ExternalSupplierStatusEnum}.
     * @param isDocumentTooLarge
     *            {@code true} when document was too large to be processed.
     * @throws ExtSupplierException
     *             Error returned by external supplier.
     * @throws ExtSupplierConnectException
     *             Error connecting to external supplier.
     */
    void onPaperCutPrintJobProcessed(DocLog docLogOut,
            PaperCutPrinterUsageLog papercutLog,
            ExternalSupplierStatusEnum printStatus, boolean isDocumentTooLarge)
            throws ExtSupplierException, ExtSupplierConnectException;
}
