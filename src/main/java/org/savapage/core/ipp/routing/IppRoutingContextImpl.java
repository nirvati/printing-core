/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2020 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: 2011-2020 Datraverse B.V. <info@datraverse.com>
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
package org.savapage.core.ipp.routing;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import org.savapage.core.jpa.IppQueue;
import org.savapage.core.util.FileSystemHelper;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class IppRoutingContextImpl implements IppRoutingContext {

    /**
     * User ID.
     */
    private String userId;

    /**
     * Originator IP address.
     */
    private String originatorIp;

    /**
     * URL path of {@link IppQueue#getUrlPath()}.
     */
    private String queueName;

    /**
     * Proxy Printer CUPS name.
     */
    private String printerName;

    /**
     * Proxy Printer display name.
     */
    private String printerDisplayName;

    /**
     * Print-in job name.
     */
    private String jobName;

    /**
     * Date of routing transaction.
     */
    private Date transactionDate;

    /**
     * The PDF file to print.
     */
    private File pdfToPrint;

    @Override
    public String getOriginatorIp() {
        return originatorIp;
    }

    @Override
    public String getUserId() {
        return userId;
    }

    /**
     * @param userId
     *            User ID
     */
    public void setUserId(final String userId) {
        this.userId = userId;
    }

    /**
     * @param originatorIp
     *            Originator IP address.
     */
    public void setOriginatorIp(final String originatorIp) {
        this.originatorIp = originatorIp;
    }

    @Override
    public String getQueueName() {
        return queueName;
    }

    /**
     * @param name
     *            URL path of {@link IppQueue#getUrlPath()}.
     */
    public void setQueueName(final String name) {
        this.queueName = name;
    }

    @Override
    public String getPrinterName() {
        return printerName;
    }

    /**
     * @param name
     *            Proxy Printer CUPS name.
     */
    public void setPrinterName(final String name) {
        this.printerName = name;
    }

    @Override
    public String getPrinterDisplayName() {
        return printerDisplayName;
    }

    /**
     * @param name
     *            Proxy Printer display name.
     */
    public void setPrinterDisplayName(final String name) {
        this.printerDisplayName = name;
    }

    @Override
    public String getJobName() {
        return jobName;
    }

    /**
     * @param name
     *            Print-in job name.
     */
    public void setJobName(final String name) {
        this.jobName = name;
    }

    @Override
    public Date getTransactionDate() {
        return transactionDate;
    }

    /**
     * @param date
     *            Date of routing transaction.
     */
    public void setTransactionDate(final Date date) {
        this.transactionDate = date;
    }

    @Override
    public File getPdfToPrint() {
        return pdfToPrint;
    }

    /**
     * @param file
     *            The PDF file to print.
     */
    public void setPdfToPrint(final File file) {
        this.pdfToPrint = file;
    }

    @Override
    public void replacePdfToPrint(final File newFile) throws IOException {
        FileSystemHelper.replaceWithNewVersion(this.pdfToPrint, newFile);
    }

}
