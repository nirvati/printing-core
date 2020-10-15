/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2020 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: © 2020 Datraverse B.V. <info@datraverse.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
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

import org.savapage.core.dao.enums.ExternalSupplierStatusEnum;
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
     * Notifies about a processing step.
     *
     * @return {@code false} is all processing must be terminated.
     */
    boolean onPaperCutPrintJobProcessingStep();

    /**
     * Notifies pending SavaPage print job that could not be found in PaperCut
     * and where document status is set (and committed) to
     * {@link ExternalSupplierStatusEnum#ERROR}.
     *
     * @see {@link DocLog#setExternalStatus(String)}.
     *
     * @param docName
     *            The name of the document.
     * @param docAge
     *            Age of the document in milliseconds.
     */
    void onPaperCutPrintJobNotFound(String docName, long docAge);

    /**
     * Notifies that a PaperCut print job has been processed and changes are
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
