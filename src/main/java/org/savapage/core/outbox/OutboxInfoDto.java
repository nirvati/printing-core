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
package org.savapage.core.outbox;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.savapage.core.dto.AbstractDto;
import org.savapage.core.inbox.PdfOrientationInfo;
import org.savapage.core.ipp.IppJobStateEnum;
import org.savapage.core.ipp.attribute.IppDictJobTemplateAttr;
import org.savapage.core.ipp.attribute.syntax.IppKeyword;
import org.savapage.core.ipp.helpers.IppOptionMap;
import org.savapage.core.jpa.Account;
import org.savapage.core.jpa.DocLog;
import org.savapage.core.jpa.PrintOut;
import org.savapage.core.services.helpers.AccountTrxInfo;
import org.savapage.core.services.helpers.AccountTrxInfoSet;
import org.savapage.core.services.helpers.ExternalSupplierInfo;
import org.savapage.core.services.helpers.ProxyPrintCostDto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

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
         * The divider used on {@link #weight}, for calculating cost and copies.
         */
        private Integer weightUnit;

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

        /**
         * @return The divider used on {@link #weight}, for calculating cost and
         *         copies.
         */
        public Integer getWeightUnit() {
            return weightUnit;
        }

        /**
         * @param weightUnit
         *            The divider used on {@link #weight}, for calculating cost
         *            and copies.
         */
        public void setWeightUnit(Integer weightUnit) {
            this.weightUnit = weightUnit;
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
         * The weight total.
         * <p>
         * <b>Note</b>: This total need NOT be the same as the accumulated
         * weight of the individual Account transactions. For example: parts of
         * the printing costs may be charged to (personal and shared) multiple
         * accounts.
         * </p>
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
    public static class OutboxJobBaseDto extends AbstractDto {

        private String ticketNumber;
        private String jobName;
        private String comment;

        private boolean drm;
        private boolean ecoPrint;
        private boolean collate;

        private int copies;
        private int sheets;
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

        public int getSheets() {
            return sheets;
        }

        public void setSheets(int sheets) {
            this.sheets = sheets;
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
         * The primary database key of the {@link User}. Is {@code null} when
         * this job resides in User outbox.
         */
        private Long userId;

        /**
         * The primary database key of the {@link User} that owns
         * {@link DocLog}. If {@code null} the owner is the {@link #userId}.
         */
        private Long userIdDocLog;

        /**
         * The primary database key of the {@link PrintOut}. Is {@code null}
         * when in User outbox.
         */
        private Long printOutId;

        /** */
        private String jobTicketDomain;
        /** */
        private String jobTicketUse;
        /** */
        private String jobTicketTag;

        /** */
        private Boolean ticketReopen;

        /** */
        @JsonIgnore
        private IppJobStateEnum ippJobState;

        /**
         * Set with Printer Group IDs the (ticket) printer is member of.
         */
        @JsonIgnore
        private Set<Long> printerGroupIDs;
        /**
         * .
         */
        @JsonIgnore
        private ExternalSupplierInfo externalSupplierInfo;

        private String file;

        /**
         * The name of the main printer, which can be the printer for the Hold
         * Job or the Job Ticket printer.
         */
        private String printer;

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

        private boolean removeGraphics;
        private ProxyPrintCostDto costResult;
        private long submitTime;
        private long expiryTime;

        /**
         * 1-based index of chunkSize. All chunks have same {@link #submitTime}.
         */
        private Integer chunkIndex;

        /**
         * Total number of chunks. All chunks have same {@link #submitTime}.
         */
        private Integer chunkSize;

        /**
         * Note for developers: This attribute must be removed after reasonable
         * time, when old persisted JSON files are not present anymore.
         *
         * @deprecated Use {@link IppDictJobTemplateAttr#ATTR_PRINT_SCALING}
         *             instead.
         */
        @Deprecated
        private Boolean fitToPage;

        /** */
        private Boolean archive;

        /**
         * The RFC2911 IPP {@code media-source} keyword of the Job Sheet.
         */
        @JsonIgnore
        private String mediaSourceJobSheet;

        /**
         * IPP RFC2911 "media" name.
         */
        private String media;

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

        /**
         * @return {@code true} if this is a Monochrome job.
         */
        @JsonIgnore
        public boolean isMonochromeJob() {
            return this.optionValues == null || this.optionValues
                    .getOrDefault(IppDictJobTemplateAttr.ATTR_PRINT_COLOR_MODE,
                            IppKeyword.PRINT_COLOR_MODE_MONOCHROME)
                    .equals(IppKeyword.PRINT_COLOR_MODE_MONOCHROME);
        }

        /**
         * @return {@code true} if this is a Booklet job.
         */
        @JsonIgnore
        public boolean isBookletJob() {
            boolean booklet = false;
            if (this.optionValues != null) {
                final String value = this.optionValues.get(
                        IppDictJobTemplateAttr.ORG_SAVAPAGE_ATTR_FINISHINGS_BOOKLET);
                if (value != null) {
                    booklet = !value.equals(
                            IppKeyword.ORG_SAVAPAGE_ATTR_FINISHINGS_BOOKLET_NONE);
                }
            }
            return booklet;
        }

        /**
         *
         * @return The primary database key of the {@link User}. Is {@code null}
         *         when this job resides in User outbox.
         */
        public Long getUserId() {
            return userId;
        }

        /**
         * @param userId
         *            The primary database key of the {@link User}. Is
         *            {@code null} when this job resides in User outbox.
         */
        public void setUserId(Long userId) {
            this.userId = userId;
        }

        /**
         * @return The primary database key of the {@link User} that owns
         *         {@link DocLog}. If {@code null} the owner is the
         *         {@link #userId}.
         */
        public Long getUserIdDocLog() {
            return userIdDocLog;
        }

        /**
         * @param userIdDocLog
         *            The primary database key of the {@link User} that owns
         *            {@link DocLog}. If {@code null} the owner is the
         *            {@link #userId}.
         */
        public void setUserIdDocLog(Long userIdDocLog) {
            this.userIdDocLog = userIdDocLog;
        }

        public Long getPrintOutId() {
            return printOutId;
        }

        public void setPrintOutId(Long printOutId) {
            this.printOutId = printOutId;
        }

        /**
         * @return Job Ticket Domain or {@code null}.
         */
        public String getJobTicketDomain() {
            return jobTicketDomain;
        }

        /**
         *
         * @param jobTicketDomain
         *            Job Ticket Domain or {@code null}.
         */
        public void setJobTicketDomain(String jobTicketDomain) {
            this.jobTicketDomain = jobTicketDomain;
        }

        /**
         * @return Job Ticket Use or {@code null}.
         */
        public String getJobTicketUse() {
            return jobTicketUse;
        }

        /**
         *
         * @param jobTicketUse
         *            Job Ticket Use or {@code null}.
         */
        public void setJobTicketUse(String jobTicketUse) {
            this.jobTicketUse = jobTicketUse;
        }

        /**
         * @return Job Ticket Tag or {@code null}.
         */
        public String getJobTicketTag() {
            return jobTicketTag;
        }

        /**
         * @param jobTicketTag
         *            Job Ticket Tag or {@code null}.
         */
        public void setJobTicketTag(String jobTicketTag) {
            this.jobTicketTag = jobTicketTag;
        }

        @JsonIgnore
        public IppJobStateEnum getIppJobState() {
            return ippJobState;
        }

        @JsonIgnore
        public void setIppJobState(IppJobStateEnum ippJobState) {
            this.ippJobState = ippJobState;
        }

        @JsonIgnore
        public Set<Long> getPrinterGroupIDs() {
            return printerGroupIDs;
        }

        @JsonIgnore
        public void setPrinterGroupIDs(Set<Long> printerGroupIDs) {
            this.printerGroupIDs = printerGroupIDs;
        }

        @JsonIgnore
        public ExternalSupplierInfo getExternalSupplierInfo() {
            return externalSupplierInfo;
        }

        @JsonIgnore
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

        public String getPrinter() {
            return printer;
        }

        public void setPrinter(String printerName) {
            this.printer = printerName;
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

        /**
         * @return 1-based index of {@link #getChunkSize()}. All chunks have
         *         same {@link #submitTime}.
         */
        public Integer getChunkIndex() {
            return chunkIndex;
        }

        /**
         * @param chunkIndex
         *            1-based index of chunkSize. All chunks have same
         *            {@link #submitTime}.
         */
        public void setChunkIndex(Integer chunkIndex) {
            this.chunkIndex = chunkIndex;
        }

        /**
         * @return Total number of chunks. All chunks have same
         *         {@link #submitTime}.
         */
        public Integer getChunkSize() {
            return chunkSize;
        }

        /**
         * @param chunkSize
         *            Total number of chunks. All chunks have same
         *            {@link #submitTime}.
         */
        public void setChunkSize(Integer chunkSize) {
            this.chunkSize = chunkSize;
        }

        /**
         * Note for developers: This method must be removed after reasonable
         * time, when old persisted JSON files are not present anymore.
         *
         * @deprecated Use {@link IppDictJobTemplateAttr#ATTR_PRINT_SCALING}
         *             instead.
         * @return {@code true} if print must be fit-to-page.
         */
        @Deprecated
        public Boolean getFitToPage() {
            return fitToPage;
        }

        /**
         * Note for developers: This method must be removed after reasonable
         * time, when old persisted JSON files are not present anymore.
         *
         * @deprecated Use {@link IppDictJobTemplateAttr#ATTR_PRINT_SCALING}
         *             instead.
         * @param fitToPage
         *            {@code true} if print must be fit-to-page.
         */
        @Deprecated
        public void setFitToPage(Boolean fitToPage) {
            this.fitToPage = fitToPage;
        }

        public Boolean getArchive() {
            return archive;
        }

        public void setArchive(Boolean archive) {
            this.archive = archive;
        }

        public Boolean getTicketReopen() {
            return ticketReopen;
        }

        public void setTicketReopen(Boolean ticketReopen) {
            this.ticketReopen = ticketReopen;
        }

        /**
         * @return IPP RFC2911 "media" name.
         */
        public String getMedia() {
            return media;
        }

        /**
         * @param media
         *            IPP RFC2911 "media" name.
         */
        public void setMedia(String media) {
            this.media = media;
        }

        /**
         * @return {@code true} when first page is perceived as landscape
         *         orientation. {@code null} when unknown.
         */
        public Boolean getLandscape() {
            return landscape;
        }

        /**
         * @param landscape
         *            {@code true} when first page is perceived as landscape
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
         * @return {@code true} when this is a Copy Job Ticket.
         */
        @JsonIgnore
        public boolean isCopyJobTicket() {
            return isJobTicket() && this.uuidPageCount == null;
        }

        /**
         * @return {@code true} when this is a Job Ticket.
         */
        @JsonIgnore
        public boolean isJobTicket() {
            return StringUtils.isNotBlank(this.getTicketNumber());
        }

        /**
         * @return {@code true} when this is a Delegated Print Job Ticket.
         */
        @JsonIgnore
        public boolean isDelegatedPrint() {
            return this.accountTransactions != null;
        }

        /**
         * @return {@code true} if this job is charged to a single account.
         */
        @JsonIgnore
        public boolean isSingleAccountPrint() {
            if (this.accountTransactions == null) {
                return true;
            }
            if (this.accountTransactions.transactions.size() != 1) {
                return false;
            }
            final OutboxAccountTrxInfo trx =
                    this.accountTransactions.transactions.get(0);

            return trx.getWeight() == this.getCopies()
                    && trx.getWeightUnit() == 1 && this.accountTransactions
                            .getWeightTotal() == this.getCopies();
        }

        /**
         * @return {@code true} if this job is charged to a single User Group
         *         account <i>including</i> individual group members.
         */
        @JsonIgnore
        public boolean isSingleAccountUserGroupPrint() {
            return this.getSingleAccountUserGroupMap().size() == 1;
        }

        /**
         * @return A map of User Group Account key and name. If map is empty no
         *         single account is present.
         */
        @JsonIgnore
        public Map<Long, String> getSingleAccountUserGroupMap() {
            final Map<Long, String> map = new HashMap<>();

            if (this.accountTransactions == null) {
                return map;
            }

            String groupName = null;
            Long groupID = null;

            for (final OutboxAccountTrxInfo trx : this.accountTransactions.transactions) {

                if (StringUtils.isBlank(trx.getExtDetails())) {
                    if (groupID == null) {
                        groupID = trx.getAccountId();
                    } else if (trx.getAccountId() != groupID.longValue()) {
                        return map;
                    }
                } else {
                    if (groupName == null) {
                        groupName = trx.getExtDetails();
                    } else if (!trx.getExtDetails().equals(groupName)) {
                        return map;
                    }
                }
            }

            if (groupName != null && groupID != null) {
                map.put(groupID, groupName);
            }
            return map;
        }

        /**
         * Sets number of printed copies for a single account print.
         *
         * @param copies
         *            Number of printed copies.
         */
        @JsonIgnore
        public void setSingleAccountPrintCopies(final int copies) {
            if (!this.isSingleAccountPrint()) {
                throw new IllegalStateException("not a single account.");
            }
            this.setCopies(copies);
            if (this.isDelegatedPrint()) {
                this.accountTransactions.setWeightTotal(copies);
                this.accountTransactions.transactions.get(0).setWeight(copies);
            }
        }

        /**
         * Sets number of printed copies for a single User Group account
         * <i>and</i> including group members.
         *
         * @param singleAccount
         *            A map of User Group Account key and name.
         * @param copies
         *            Number of printed copies.
         */
        @JsonIgnore
        public void setSingleAccountUserGroupPrint(
                final Map<Long, String> singleAccount, final int copies) {

            this.setCopies(copies);

            final OutboxCopiesEditor editor = new OutboxCopiesEditor(
                    this.getAccountTransactions(), singleAccount);

            final Entry<Long, String> entry =
                    singleAccount.entrySet().iterator().next();

            this.setAccountTransactions(
                    editor.recalcGroupCopies(entry.getKey(), copies));
        }

        /**
         * @return The RFC2911 IPP {@code media-source} keyword of the Job
         *         Sheet.
         */
        @JsonIgnore
        public String getMediaSourceJobSheet() {
            return mediaSourceJobSheet;
        }

        /**
         * @param mediaSource
         *            The RFC2911 IPP {@code media-source} keyword of the Job
         *            Sheet.
         */
        @JsonIgnore
        public void setMediaSourceJobSheet(final String mediaSource) {
            this.mediaSourceJobSheet = mediaSource;
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
     *
     * @param fileName
     *            Job file name.
     * @return {@code true} if this info contain job file name..
     */
    @JsonIgnore
    public boolean containsJob(final String fileName) {
        return this.jobs.containsKey(fileName);
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

}
