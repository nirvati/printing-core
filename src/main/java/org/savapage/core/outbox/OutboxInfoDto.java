/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2017 Datraverse B.V.
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
package org.savapage.core.outbox;

import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.savapage.core.dto.AbstractDto;
import org.savapage.core.inbox.PdfOrientationInfo;
import org.savapage.core.ipp.IppJobStateEnum;
import org.savapage.core.ipp.helpers.IppOptionMap;
import org.savapage.core.jpa.PrintOut;
import org.savapage.core.services.helpers.AccountTrxInfo;
import org.savapage.core.services.helpers.AccountTrxInfoSet;
import org.savapage.core.services.helpers.ExternalSupplierInfo;
import org.savapage.core.services.helpers.ProxyPrintCostDto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

/**
 *
 * @author Rijk Ravestein
 *
 */
@JsonInclude(Include.NON_NULL)
public final class OutboxInfoDto extends AbstractDto {

    /**
     *
     * @author Rijk Ravestein
     *
     */
    @JsonInclude(Include.NON_NULL)
    public static final class LocaleInfo {

        private String cost;
        private String submitTime;
        private String expiryTime;
        private String remainTime;

        public String getCost() {
            return cost;
        }

        public void setCost(String cost) {
            this.cost = cost;
        }

        public String getSubmitTime() {
            return submitTime;
        }

        public void setSubmitTime(String submitTime) {
            this.submitTime = submitTime;
        }

        public String getExpiryTime() {
            return expiryTime;
        }

        public void setExpiryTime(String expiryTime) {
            this.expiryTime = expiryTime;
        }

        public String getRemainTime() {
            return remainTime;
        }

        public void setRemainTime(String remainTime) {
            this.remainTime = remainTime;
        }

    }

    /**
     * A weighted {@link Account} transaction with free format details.
     * <p>
     * NOTE: This class has a similar purpose as {@link AccountTrxInfo}, but
     * contains the primary key of {@link Account} instead of the object itself.
     * </p>
     *
     * @author Rijk Ravestein
     *
     */
    @JsonInclude(Include.NON_NULL)
    public static final class OutboxAccountTrxInfo {

        /**
         * Primary key of the {@link Account}.
         */
        private long accountId;

        /**
         * Mathematical weight of the transaction in the context of a
         * transaction set.
         */
        private int weight;

        /**
         * Free format details from external source.
         */
        private String extDetails;

        /**
         *
         * @return
         */
        public long getAccountId() {
            return accountId;
        }

        public void setAccountId(long accountId) {
            this.accountId = accountId;
        }

        public int getWeight() {
            return weight;
        }

        public void setWeight(int weight) {
            this.weight = weight;
        }

        public String getExtDetails() {
            return extDetails;
        }

        public void setExtDetails(String extDetails) {
            this.extDetails = extDetails;
        }
    }

    /**
     * A unit of weighted {@link OutboxAccountTrxInfo} objects.
     * <p>
     * NOTE: This class has a similar purpose as {@link AccountTrxInfoSet}, but
     * contains {@link OutboxAccountTrxInfo} objects that have the primary key
     * of {@link Account} instead of the {@link Account} object itself.
     * </p>
     *
     * @author Rijk Ravestein
     *
     */
    @JsonInclude(Include.NON_NULL)
    public static final class OutboxAccountTrxInfoSet {

        /**
         * The weight total. IMPORTANT: This total need NOT be the same as the
         * accumulated weight of the individual Account transactions. For
         * example: parts of the printing costs may be charged to (personal and
         * shared) multiple accounts.
         */
        private int weightTotal;

        /**
         * .
         */
        private List<OutboxAccountTrxInfo> transactions;

        public int getWeightTotal() {
            return weightTotal;
        }

        public void setWeightTotal(int weightTotal) {
            this.weightTotal = weightTotal;
        }

        public List<OutboxAccountTrxInfo> getTransactions() {
            return transactions;
        }

        public void setTransactions(List<OutboxAccountTrxInfo> transactions) {
            this.transactions = transactions;
        }

    }

    /**
    *
    */
    @JsonInclude(Include.NON_NULL)
    public static class OutboxJobBaseDto {

        private String ticketNumber;
        private String jobName;
        private String comment;

        private boolean drm;
        private boolean ecoPrint;
        private boolean collate;

        private int copies;
        private int pages;

        public String getTicketNumber() {
            return ticketNumber;
        }

        public void setTicketNumber(String ticketNumber) {
            this.ticketNumber = ticketNumber;
        }

        public String getJobName() {
            return jobName;
        }

        public void setJobName(String jobName) {
            this.jobName = jobName;
        }

        public int getCopies() {
            return copies;
        }

        public void setCopies(int copies) {
            this.copies = copies;
        }

        public int getPages() {
            return pages;
        }

        public void setPages(int pages) {
            this.pages = pages;
        }

        public boolean isDrm() {
            return drm;
        }

        public void setDrm(boolean drm) {
            this.drm = drm;
        }

        public boolean isEcoPrint() {
            return ecoPrint;
        }

        public void setEcoPrint(boolean ecoPrint) {
            this.ecoPrint = ecoPrint;
        }

        public boolean isCollate() {
            return collate;
        }

        public void setCollate(boolean collate) {
            this.collate = collate;
        }

        public String getComment() {
            return comment;
        }

        public void setComment(String comment) {
            this.comment = comment;
        }

    }

    /**
     *
     */
    @JsonInclude(Include.NON_NULL)
    public static final class OutboxJobDto extends OutboxJobBaseDto {

        /**
         * The primary database key of the {@link User}. Is {@code null} when in
         * User outbox.
         */
        private Long userId;

        /**
         * The primary database key of the {@link PrintOut}. Is {@code null}
         * when in User outbox.
         */
        private Long printOutId;

        /**
         *
         */
        @JsonIgnore
        private IppJobStateEnum ippJobState;

        /**
         * .
         */
        private ExternalSupplierInfo externalSupplierInfo;

        private String file;

        /**
         * Name of the Job Ticket printer.
         */
        private String printerJobTicket;

        /**
         * Name of the redirect printer. If {@code null} ticket is not printed
         * yet.
         */
        private String printerRedirect;

        /**
         * The total number of blank filler pages appended between logical
         * sub-jobs (proxy print only).
         */
        private int fillerPages;

        private int sheets;
        private boolean removeGraphics;
        private ProxyPrintCostDto costResult;
        private long submitTime;
        private long expiryTime;
        private Boolean fitToPage;

        /**
         * {@code true} when one of the job pages has landscape orientation.
         * {@code null} when unknown.
         */
        private Boolean landscape;

        /**
         * The PDF orientation of the first page to be proxy printed.
         */
        private PdfOrientationInfo pdfOrientation;

        private Map<String, String> optionValues;

        private LocaleInfo localeInfo;

        /**
         * Note: {@link LinkedHashMap} is insertion ordered.
         */
        private LinkedHashMap<String, Integer> uuidPageCount;

        /**
         *
         */
        private OutboxAccountTrxInfoSet accountTransactions;

        /**
         * Constructor.
         */
        public OutboxJobDto() {
            this.costResult = new ProxyPrintCostDto();
        }

        //
        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public Long getPrintOutId() {
            return printOutId;
        }

        public void setPrintOutId(Long printOutId) {
            this.printOutId = printOutId;
        }

        @JsonIgnore
        public IppJobStateEnum getIppJobState() {
            return ippJobState;
        }

        @JsonIgnore
        public void setIppJobState(IppJobStateEnum ippJobState) {
            this.ippJobState = ippJobState;
        }

        public ExternalSupplierInfo getExternalSupplierInfo() {
            return externalSupplierInfo;
        }

        public void setExternalSupplierInfo(
                ExternalSupplierInfo externalSupplierInfo) {
            this.externalSupplierInfo = externalSupplierInfo;
        }

        public String getFile() {
            return file;
        }

        public void setFile(String file) {
            this.file = file;
        }

        public String getPrinterJobTicket() {
            return printerJobTicket;
        }

        public void setPrinterJobTicket(String printerName) {
            this.printerJobTicket = printerName;
        }

        public String getPrinterRedirect() {
            return printerRedirect;
        }

        public void setPrinterRedirect(String printerName) {
            this.printerRedirect = printerName;
        }

        public int getFillerPages() {
            return fillerPages;
        }

        public void setFillerPages(int fillerPages) {
            this.fillerPages = fillerPages;
        }

        public int getSheets() {
            return sheets;
        }

        public void setSheets(int sheets) {
            this.sheets = sheets;
        }

        public boolean isRemoveGraphics() {
            return removeGraphics;
        }

        public void setRemoveGraphics(boolean removeGraphics) {
            this.removeGraphics = removeGraphics;
        }

        public ProxyPrintCostDto getCostResult() {
            return costResult;
        }

        public void setCostResult(ProxyPrintCostDto costResult) {
            this.costResult = costResult;
        }

        @JsonIgnore
        public BigDecimal getCostTotal() {
            return this.costResult.getCostTotal();
        }

        public LocaleInfo getLocaleInfo() {
            return localeInfo;
        }

        public void setLocaleInfo(LocaleInfo localeInfo) {
            this.localeInfo = localeInfo;
        }

        /**
         * @return The time the job was submitted as in {@link Date#getTime()}.
         */
        public long getSubmitTime() {
            return submitTime;
        }

        /**
         * Sets the time the job was submitted.
         *
         * @param submitTime
         *            Time as in {@link Date#getTime()}.
         */
        public void setSubmitTime(long submitTime) {
            this.submitTime = submitTime;
        }

        /**
         *
         * @return The time the job expires as in {@link Date#getTime()}.
         */
        public long getExpiryTime() {
            return expiryTime;
        }

        /**
         * Sets the time the job was expires.
         *
         * @param expiryTime
         *            Time as in {@link Date#getTime()}.
         */
        public void setExpiryTime(long expiryTime) {
            this.expiryTime = expiryTime;
        }

        public Boolean getFitToPage() {
            return fitToPage;
        }

        public void setFitToPage(Boolean fitToPage) {
            this.fitToPage = fitToPage;
        }

        /**
         * @return {@code true} when one of the job pages has landscape
         *         orientation. {@code null} when unknown.
         */
        public Boolean getLandscape() {
            return landscape;
        }

        /**
         * @param landscape
         *            {@code true} when one of the job pages has landscape
         *            orientation. {@code null} when unknown.
         */
        public void setLandscape(Boolean landscape) {
            this.landscape = landscape;
        }

        /**
         * @return The PDF orientation of the first page to be proxy printed.
         */
        public PdfOrientationInfo getPdfOrientation() {
            return pdfOrientation;
        }

        /**
         * @param pdfOrientation
         *            The PDF orientation of the first page to be proxy printed.
         */
        public void setPdfOrientation(PdfOrientationInfo pdfOrientation) {
            this.pdfOrientation = pdfOrientation;
        }

        public Map<String, String> getOptionValues() {
            return optionValues;
        }

        public void setOptionValues(Map<String, String> optionValues) {
            this.optionValues = optionValues;
        }

        /**
         * Deep copy of option values.
         *
         * @param optionValues
         */
        public void putOptionValues(final Map<String, String> optionValues) {
            if (this.optionValues == null) {
                this.optionValues = new HashMap<>();
            }
            this.optionValues.putAll(optionValues);
        }

        /**
         * Note: {@link LinkedHashMap} is insertion ordered.
         */
        public LinkedHashMap<String, Integer> getUuidPageCount() {
            return uuidPageCount;
        }

        /**
         * Note: {@link LinkedHashMap} is insertion ordered.
         */
        public void
                setUuidPageCount(LinkedHashMap<String, Integer> uuidPageCount) {
            this.uuidPageCount = uuidPageCount;
        }

        public OutboxAccountTrxInfoSet getAccountTransactions() {
            return accountTransactions;
        }

        public void setAccountTransactions(
                OutboxAccountTrxInfoSet accountTransactions) {
            this.accountTransactions = accountTransactions;
        }

        /**
         * Creates an {@link IppOptionMap} wrapping the {@link #optionValues}.
         *
         * @return The {@link IppOptionMap}.
         */
        @JsonIgnore
        public IppOptionMap createIppOptionMap() {
            return new IppOptionMap(this.getOptionValues());
        }

        /**
         *
         * @return {@code true} when this is a Copy Job Ticket.
         */
        @JsonIgnore
        public boolean isCopyJobTicket() {
            return this.uuidPageCount == null;
        }

        /**
         *
         * @return {@code true} when this is a Delegated Print Job Ticket.
         */
        @JsonIgnore
        public boolean isDelegatedPrint() {
            return this.accountTransactions != null;
        }
    }

    /**
     * {@link LocaleInfo#date} hold the earliest submitted date of the jobs.
     */
    private LocaleInfo localeInfo;

    /**
     * Note: {@link LinkedHashMap} is insertion ordered.
     */
    private LinkedHashMap<String, OutboxJobDto> jobs = new LinkedHashMap<>();

    /**
     * Note: {@link LinkedHashMap} is insertion ordered.
     */
    public LinkedHashMap<String, OutboxJobDto> getJobs() {
        return jobs;
    }

    /**
     * Note: {@link LinkedHashMap} is insertion ordered.
     */
    public void setJobs(LinkedHashMap<String, OutboxJobDto> jobs) {
        this.jobs = jobs;
    }

    public LocaleInfo getLocaleInfo() {
        return localeInfo;
    }

    public void setLocaleInfo(LocaleInfo localeInfo) {
        this.localeInfo = localeInfo;
    }

    /**
     * @return The number of jobs.
     */
    @JsonIgnore
    public int getJobCount() {
        return this.jobs.size();
    }

    /**
     * Adds a job.
     *
     * @param fileName
     *            The file name.
     * @param job
     *            The job to add.
     */
    @JsonIgnore
    public void addJob(final String fileName, final OutboxJobDto job) {
        this.jobs.put(fileName, job);
    }

    /**
     * Creates an instance from JSON string.
     *
     * @param json
     * @return
     * @throws Exception
     */
    public static OutboxInfoDto create(final String json) throws Exception {
        return getMapper().readValue(json, OutboxInfoDto.class);
    }

    /**
     *
     * @return
     * @throws IOException
     */
    public String prettyPrinted() throws IOException {
        final JsonFactory jsonFactory = new JsonFactory();
        final StringWriter sw = new StringWriter();
        final JsonGenerator jg = jsonFactory.createJsonGenerator(sw);
        jg.useDefaultPrettyPrinter();
        getMapper().writeValue(jg, this);
        return sw.toString();
    }

}
