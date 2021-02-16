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
package org.savapage.core.services.helpers;

import java.util.Date;

import org.savapage.core.pdf.IPdfPageProps;

/**
 * Content information about a document printed to SavaPage including
 * {@link AccountTrxInfoSet} and {@link ExternalSupplierInfo}.
 *
 * @author Rijk Ravestein
 *
 */
public final class DocContentPrintInInfo {

    /** */
    private Date printInDate;

    /**
     * .
     */
    private final boolean drmViolationDetected = false;

    /**
     * .
     */
    private boolean drmRestricted = false;

    /**
     * {@code null} if no PDF document.
     */
    private PdfRepairEnum pdfRepair;

    /**
     * .
     */
    private String mimetype;

    /**
     * .
     */
    private String jobName;

    /**
     * .
     */
    private String logComment;

    /**
     * .
     */
    private String originatorIp;

    /**
     * .
     */
    private String originatorEmail;

    /**
     * .
     */
    private IPdfPageProps pageProps;

    /**
     * .
     */
    private long jobBytes;

    /**
     * .
     */
    private java.util.UUID uuidJob;

    /**
     * .
     */
    private ExternalSupplierInfo supplierInfo;

    /**
     * .
     */
    private AccountTrxInfoSet accountTrxInfoSet;

    /**
     * @return Date of print-in.
     */
    public Date getPrintInDate() {
        return printInDate;
    }

    public void setPrintInDate(Date printInDate) {
        this.printInDate = printInDate;
    }

    public boolean isDrmRestricted() {
        return drmRestricted;
    }

    public void setDrmRestricted(boolean drmRestricted) {
        this.drmRestricted = drmRestricted;
    }

    /**
     * @return {@code null} if no PDF document.
     */
    public PdfRepairEnum getPdfRepair() {
        return pdfRepair;
    }

    /**
     * @param pdfRepair
     *            {@code null} if no PDF document.
     */
    public void setPdfRepair(PdfRepairEnum pdfRepair) {
        this.pdfRepair = pdfRepair;
    }

    public String getMimetype() {
        return mimetype;
    }

    public void setMimetype(String mimetype) {
        this.mimetype = mimetype;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public String getLogComment() {
        return logComment;
    }

    public void setLogComment(String logComment) {
        this.logComment = logComment;
    }

    public String getOriginatorIp() {
        return originatorIp;
    }

    public void setOriginatorIp(String originatorIp) {
        this.originatorIp = originatorIp;
    }

    public String getOriginatorEmail() {
        return originatorEmail;
    }

    public void setOriginatorEmail(String originatorEmail) {
        this.originatorEmail = originatorEmail;
    }

    public IPdfPageProps getPageProps() {
        return pageProps;
    }

    public void setPageProps(IPdfPageProps pageProps) {
        this.pageProps = pageProps;
    }

    public long getJobBytes() {
        return jobBytes;
    }

    public void setJobBytes(long jobBytes) {
        this.jobBytes = jobBytes;
    }

    public java.util.UUID getUuidJob() {
        return uuidJob;
    }

    public void setUuidJob(java.util.UUID uuidJob) {
        this.uuidJob = uuidJob;
    }

    public boolean isDrmViolationDetected() {
        return drmViolationDetected;
    }

    public ExternalSupplierInfo getSupplierInfo() {
        return supplierInfo;
    }

    public void setSupplierInfo(ExternalSupplierInfo supplierInfo) {
        this.supplierInfo = supplierInfo;
    }

    public AccountTrxInfoSet getAccountTrxInfoSet() {
        return accountTrxInfoSet;
    }

    public void setAccountTrxInfoSet(AccountTrxInfoSet accountTrxInfoSet) {
        this.accountTrxInfoSet = accountTrxInfoSet;
    }

    /**
     * @return {@code true} if PDF document and repair failed.
     */
    public boolean isPdfRepairFail() {
        return this.pdfRepair != null && this.pdfRepair.isRepairFail();
    }
}
