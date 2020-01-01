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
package org.savapage.core.services;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import org.savapage.core.doc.store.DocStoreBranchEnum;
import org.savapage.core.doc.store.DocStoreConfig;
import org.savapage.core.doc.store.DocStoreException;
import org.savapage.core.doc.store.DocStoreTypeEnum;
import org.savapage.core.job.RunModeSwitch;
import org.savapage.core.jpa.DocLog;
import org.savapage.core.outbox.OutboxInfoDto.OutboxJobDto;
import org.savapage.core.pdf.PdfCreateInfo;
import org.savapage.core.print.proxy.AbstractProxyPrintReq;

/**
 *
 * @author Rijk Ravestein
 *
 */
public interface DocStoreService extends StatefulService {

    /**
     * Checks if store branch is enabled.
     *
     * @param store
     *            Type of store.
     * @param branch
     *            Branch in store.
     * @return {@code true} if enabled.
     */
    boolean isEnabled(DocStoreTypeEnum store, DocStoreBranchEnum branch);

    /**
     * Get the store branch configuration.
     *
     * @param store
     *            Type of store.
     * @param branch
     *            Branch in store.
     * @return The configuration.
     */
    DocStoreConfig getConfig(DocStoreTypeEnum store, DocStoreBranchEnum branch);

    /**
     * Checks if document is present in store branch.
     *
     * @param store
     *            Type of store.
     * @param branch
     *            Branch in store.
     * @param docLog
     *            The document log.
     * @return {@code true} if document is present.
     */
    boolean isDocPresent(DocStoreTypeEnum store, DocStoreBranchEnum branch,
            DocLog docLog);

    /**
     * Stores a proxy printed document.
     *
     * @param store
     *            Type of store.
     * @param request
     *            The {@link AbstractProxyPrintReq}.
     * @param docLog
     *            The {@link DocLog} persisted in the database.
     * @param createInfo
     *            The {@link PdfCreateInfo} with the PDF file sent to the
     *            printer.
     * @throws DocStoreException
     *             When IO errors.
     */
    void store(DocStoreTypeEnum store, AbstractProxyPrintReq request,
            DocLog docLog, PdfCreateInfo createInfo) throws DocStoreException;

    /**
     * Stores a (proxy) printed document.
     *
     * @param store
     *            Type of store.
     * @param job
     *            The {@link OutboxJobDto}.
     * @param docLog
     *            The {@link DocLog} persisted in the database.
     * @param pdfFile
     *            The PDF file sent to the printer. Is {@code null} for Copy Job
     *            Ticket print.
     * @throws DocStoreException
     *             When IO errors.
     */
    void store(DocStoreTypeEnum store, OutboxJobDto job, DocLog docLog,
            File pdfFile) throws DocStoreException;

    /**
     * Retrieves PDF of a logged document.
     *
     * @param store
     *            Type of store.
     * @param docLog
     *            The {@link DocLog} persisted in the database.
     * @return The PDF file.
     * @throws DocStoreException
     *             When PDF can not be retrieved.
     */
    File retrievePdf(DocStoreTypeEnum store, DocLog docLog)
            throws DocStoreException;

    /**
     * Retrieves job file of a logged document.
     *
     * @param store
     *            Type of store.
     * @param docLog
     *            The {@link DocLog} persisted in the database.
     * @return The job file.
     * @throws DocStoreException
     *             When job file can not be retrieved.
     * @throws IOException
     *             When JSON read error.
     */
    OutboxJobDto retrieveJob(DocStoreTypeEnum store, DocLog docLog)
            throws DocStoreException, IOException;

    /**
     * Cleans a store by removing old documents.
     *
     * @param store
     *            Type of store.
     * @param branch
     *            Branch in store.
     * @param cleaningDate
     *            Date cleaning takes place. Normally, this is the current date.
     * @param keepDays
     *            Number of days to keep documents in store.
     * @param runMode
     *            The run mode.
     * @return Number of removed documents.
     * @throws IOException
     *             If IO errors.
     */
    long clean(DocStoreTypeEnum store, DocStoreBranchEnum branch,
            Date cleaningDate, int keepDays, RunModeSwitch runMode)
            throws IOException;
}
