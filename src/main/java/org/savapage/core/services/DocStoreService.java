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
package org.savapage.core.services;

import java.io.IOException;
import java.util.Date;

import org.savapage.core.doc.store.DocStoreBranchEnum;
import org.savapage.core.doc.store.DocStoreConfig;
import org.savapage.core.doc.store.DocStoreException;
import org.savapage.core.doc.store.DocStoreTypeEnum;
import org.savapage.core.job.RunModeSwitch;
import org.savapage.core.jpa.DocLog;
import org.savapage.core.pdf.PdfCreateInfo;
import org.savapage.core.print.proxy.AbstractProxyPrintReq;

/**
 *
 * @author Rijk Ravestein
 *
 */
public interface DocStoreService {

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
