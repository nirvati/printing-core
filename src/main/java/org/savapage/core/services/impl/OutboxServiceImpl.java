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
package org.savapage.core.services.impl;

import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.file.FileSystems;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.savapage.core.SpException;
import org.savapage.core.concurrent.ReadWriteLockEnum;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.dao.DaoContext;
import org.savapage.core.dao.enums.ExternalSupplierEnum;
import org.savapage.core.dao.enums.ExternalSupplierStatusEnum;
import org.savapage.core.doc.DocContent;
import org.savapage.core.imaging.EcoPrintPdfTaskPendingException;
import org.savapage.core.jpa.Account;
import org.savapage.core.jpa.DocLog;
import org.savapage.core.jpa.User;
import org.savapage.core.outbox.OutboxInfoDto;
import org.savapage.core.outbox.OutboxInfoDto.LocaleInfo;
import org.savapage.core.outbox.OutboxInfoDto.OutboxAccountTrxInfo;
import org.savapage.core.outbox.OutboxInfoDto.OutboxAccountTrxInfoSet;
import org.savapage.core.outbox.OutboxInfoDto.OutboxJobDto;
import org.savapage.core.pdf.PdfCreateInfo;
import org.savapage.core.pdf.PdfPrintCollector;
import org.savapage.core.print.proxy.AbstractProxyPrintReq;
import org.savapage.core.print.proxy.AbstractProxyPrintReq.Status;
import org.savapage.core.print.proxy.ProxyPrintDocReq;
import org.savapage.core.print.proxy.ProxyPrintInboxReq;
import org.savapage.core.services.OutboxService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.helpers.AccountTrxInfo;
import org.savapage.core.services.helpers.AccountTrxInfoSet;
import org.savapage.core.services.helpers.DocContentPrintInInfo;
import org.savapage.core.services.helpers.ExternalSupplierInfo;
import org.savapage.core.services.helpers.ProxyPrintInboxPattern;
import org.savapage.core.util.BigDecimalUtil;
import org.savapage.core.util.JsonHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class OutboxServiceImpl extends AbstractService
        implements OutboxService {

    /**
     * The logger.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(OutboxServiceImpl.class);

    /**
    *
    */
    private static final String OUTBOX_DESCRIPT_FILE_NAME = "outbox.json";

    /**
     * .
     */
    private static final String USER_RELATIVE_OUTBOX_PATH = "outbox";

    /**
     * Implementation of execution pattern for proxy printing from the user
     * inbox to user outbox.
     */
    private static final class ProxyPrintInbox extends ProxyPrintInboxPattern {

        private final OutboxServiceImpl serviceImpl;

        /**
         * The date the proxy print was submitted.
         */
        private Date submitDate;

        /**
         * The date the proxy print expires.
         */
        private Date expiryDate;

        /**
         * The {@link OutboxInfoDto} to append the info about the print job on.
         */
        private OutboxInfoDto outboxInfo;

        /**
         *
         * @param service
         *            The parent service.
         */
        public ProxyPrintInbox(final OutboxServiceImpl service) {
            this.serviceImpl = service;
        }

        @Override
        protected void onInit(final User lockedUser,
                final ProxyPrintInboxReq request) {

            this.submitDate = ServiceContext.getTransactionDate();
            this.expiryDate = calcHoldExpiry(submitDate);

            this.outboxInfo =
                    this.serviceImpl.readOutboxInfo(lockedUser.getUserId());
        }

        @Override
        protected void onExit(final User lockedUser,
                final ProxyPrintInboxReq request) {

            serviceImpl.storeOutboxInfo(lockedUser.getUserId(), outboxInfo);

            final String msgKey = "msg-user-print-outbox";

            request.setStatus(Status.WAITING_FOR_RELEASE);
            request.setUserMsgKey(msgKey);
            request.setUserMsg(this.serviceImpl.localize(msgKey,
                    request.getPrinterName()));
        }

        @Override
        protected File onReservePdfToGenerate(final User lockedUser) {
            return this.serviceImpl.createUuidFileName(lockedUser.getUserId());
        }

        @Override
        protected void onPdfGenerated(final User lockedUser,
                final ProxyPrintInboxReq request,
                final LinkedHashMap<String, Integer> uuidPageCount,
                final PdfCreateInfo createInfo) {

            final OutboxJobDto job =
                    this.serviceImpl.createOutboxJob(request, this.submitDate,
                            this.expiryDate, createInfo, uuidPageCount);

            this.outboxInfo.addJob(job.getFile(), job);
        }

    } // end-of-class

    @Override
    public File getUserOutboxDir(final String userId) {
        return FileSystems.getDefault()
                .getPath(ConfigManager.getUserHomeDir(userId),
                        USER_RELATIVE_OUTBOX_PATH)
                .toFile();
    }

    @Override
    public boolean isOutboxPresent(final String userId) {
        return getOutboxInfoFilePath(userId).exists();
    }

    /**
     * Gets the full path of the outbox info file.
     *
     * @param userId
     *            The unique user id.
     * @return The filename.
     */
    private static File getOutboxInfoFilePath(final String userId) {
        return FileSystems.getDefault()
                .getPath(ConfigManager.getUserHomeDir(userId),
                        USER_RELATIVE_OUTBOX_PATH, OUTBOX_DESCRIPT_FILE_NAME)
                .toFile();
    }

    /**
     * Stores {@link OutboxInfoDto} as JSON file in user inbox directory.
     *
     * @param userId
     *            The unique user id.
     * @param outboxInfo
     *            The {@link OutboxInfoDto} object.
     */
    private void storeOutboxInfo(final String userId,
            final OutboxInfoDto outboxInfo) {

        final File jsonFile = getOutboxInfoFilePath(userId);

        Writer writer = null;
        try {
            writer = new FileWriter(jsonFile);
            JsonHelper.write(outboxInfo, writer);
            writer.close();
        } catch (IOException e) {
            throw new SpException(String.format("Error writing file [%s]",
                    jsonFile.getAbsolutePath()), e);
        } finally {
            IOUtils.closeQuietly(writer);
        }
    }

    /**
     * Reads {@link OutboxInfoDto} JSON file from user's outbox directory.
     * <p>
     * NOTE: The JSON file is created when it does not exist.
     * </p>
     *
     * @param userId
     *            The unique user id.
     * @return {@link OutboxInfoDto} object.
     */
    public OutboxInfoDto readOutboxInfo(final String userId) {

        final ObjectMapper mapper = new ObjectMapper();

        OutboxInfoDto outboxInfo = null;
        final File file = getOutboxInfoFilePath(userId);

        try {
            /*
             * First check if file exists, if not (first time use, or reset)
             * return an empty job info object.
             */
            if (file.exists()) {

                try {

                    outboxInfo = mapper.readValue(file, OutboxInfoDto.class);

                } catch (JsonMappingException e) {

                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Error mapping from file ["
                                + file.getAbsolutePath() + "]: create new.");
                    }

                    /*
                     * There has been a change in layout of the JSON file, so
                     * create a new default and store it.
                     */
                    outboxInfo = new OutboxInfoDto();
                    storeOutboxInfo(userId, outboxInfo);
                }
            }
            if (outboxInfo == null) {
                outboxInfo = new OutboxInfoDto();
            }
        } catch (IOException e) {
            throw new SpException(
                    "Error reading file [" + file.getAbsolutePath() + "]", e);
        }
        return outboxInfo;
    }

    /**
     * Calculates the expiration {@link Date} of a held proxy print job (using
     * the configuration setting).
     *
     * @param submitDate
     *            The {@link Date} the job is submitted.
     * @return The expiration {@link Date}.
     */
    private static Date calcHoldExpiry(final Date submitDate) {
        return DateUtils.addMinutes(submitDate, ConfigManager.instance()
                .getConfigInt(IConfigProp.Key.PROXY_PRINT_HOLD_EXPIRY_MINS));
    }

    @Override
    public void proxyPrintInbox(final User lockedUser,
            final ProxyPrintInboxReq request)
            throws EcoPrintPdfTaskPendingException {

        new ProxyPrintInbox(this).print(lockedUser, request);
    }

    @Override
    public OutboxJobDto createOutboxJob(final AbstractProxyPrintReq request,
            final Date submitDate, final Date expiryDate,
            final PdfCreateInfo createInfo,
            final LinkedHashMap<String, Integer> uuidPageCount) {

        final OutboxJobDto job = new OutboxJobDto();

        job.setFile(createInfo.getPdfFile().getName());
        job.setPrinterName(request.getPrinterName());
        job.setJobName(request.getJobName());
        job.setComment(request.getComment());
        job.setCopies(request.getNumberOfCopies());
        job.setPages(request.getNumberOfPages());
        job.setFillerPages(createInfo.getBlankFillerPages());
        job.setSheets(calNumberOfSheets(request, createInfo));
        job.setRemoveGraphics(request.isRemoveGraphics());
        job.setEcoPrint(request.isEcoPrintShadow());
        job.setCollate(request.isCollate());
        job.setCostResult(request.getCostResult());
        job.setSubmitTime(submitDate.getTime());
        job.setExpiryTime(expiryDate.getTime());
        job.setFitToPage(request.getFitToPage());
        job.setLandscape(request.getLandscape());
        job.setPdfOrientation(request.getPdfOrientation());
        job.setDrm(request.isDrm());
        job.putOptionValues(request.getOptionValues());
        job.setUuidPageCount(uuidPageCount);

        if (request.getSupplierInfo() != null) {
            job.setExternalSupplierInfo(request.getSupplierInfo());
        }

        this.importToOutbox(request.getAccountTrxInfoSet(), job);

        return job;
    }

    @Override
    public void proxyPrintPdf(final User lockedUser,
            final ProxyPrintDocReq request, final PdfCreateInfo createInfo,
            final DocContentPrintInInfo printInfo) throws IOException {

        final File pdfFile = createInfo.getPdfFile();
        final Date submitDate = ServiceContext.getTransactionDate();
        final Date expiryDate = calcHoldExpiry(submitDate);

        final OutboxInfoDto outboxInfo =
                this.readOutboxInfo(lockedUser.getUserId());

        final String pdfOutboxFileName =
                createUuidFileName(lockedUser.getUserId()).getAbsolutePath();

        final File pdfOutboxFile = new File(pdfOutboxFileName);

        boolean jobAdded = false;

        try {
            /*
             * Just in case, lazy create home directory.
             */
            userService().lazyUserHomeDir(lockedUser.getUserId());

            /*
             * Copy the input PDF file to the outbox.
             */
            FileUtils.copyFile(pdfFile, pdfOutboxFile);

            final LinkedHashMap<String, Integer> uuidPageCount =
                    new LinkedHashMap<>();

            uuidPageCount.put(printInfo.getUuidJob().toString(),
                    Integer.valueOf(request.getNumberOfPages()));

            final OutboxJobDto job = createOutboxJob(request, submitDate,
                    expiryDate, createInfo, uuidPageCount);

            outboxInfo.addJob(job.getFile(), job);

            jobAdded = true;

        } finally {

            if (!jobAdded && pdfOutboxFile != null && pdfOutboxFile.exists()) {
                if (pdfOutboxFile.delete()) {
                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace("deleted file [" + pdfOutboxFile + "]");
                    }
                } else {
                    LOGGER.error(
                            "delete of file [" + pdfOutboxFile + "] FAILED");
                }
            }
        }

        this.storeOutboxInfo(lockedUser.getUserId(), outboxInfo);

        request.setStatus(Status.WAITING_FOR_RELEASE);
        request.setUserMsgKey("msg-user-print-outbox");
        request.setUserMsg(
                localize("msg-user-print-outbox", request.getPrinterName()));
    }

    @Override
    public File getOutboxFile(final String userId, final String fileName) {

        try {
            return FileSystems.getDefault()
                    .getPath(getUserOutboxDir(userId).getCanonicalPath(),
                            fileName)
                    .toFile();
        } catch (IOException e) {
            throw new SpException(e.getMessage());
        }
    }

    /**
     * Calculates the number of sheets as requested in the
     * {@link AbstractProxyPrintReq} request.
     *
     * @param request
     *            The request.
     * @param createInfo
     *            The {@link PdfCreateInfo} with the PDF file to be printed by
     *            the Job Ticket.
     * @return The number of sheets.
     */
    private static int calNumberOfSheets(final AbstractProxyPrintReq request,
            final PdfCreateInfo createInfo) {
        return PdfPrintCollector.calcNumberOfPrintedSheets(request,
                createInfo.getBlankFillerPages());
    }

    /**
     * Creates a unique outbox PDF file path.
     *
     * @param userId
     *            The unique id of the user.
     * @return the file.
     */
    private File createUuidFileName(final String userId) {

        return getOutboxFile(userId,
                String.format("%s.%s", java.util.UUID.randomUUID().toString(),
                        DocContent.FILENAME_EXT_PDF));
    }

    @Override
    public int getOutboxJobCount(final String userId) {
        return this.readOutboxInfo(userId).getJobCount();
    }

    @Override
    public List<OutboxJobDto> getOutboxJobs(final String userId,
            final Set<String> printerNames, final Date expiryRef) {

        final OutboxInfoDto outboxInfo =
                pruneOutboxInfo(userId, readOutboxInfo(userId), expiryRef);

        final List<OutboxJobDto> jobs = new ArrayList<>();

        for (final Entry<String, OutboxJobDto> entry : outboxInfo.getJobs()
                .entrySet()) {

            final OutboxJobDto job = entry.getValue();

            if (printerNames.contains(job.getPrinterName())) {
                jobs.add(job);
            }
        }

        return jobs;
    }

    @Override
    public OutboxInfoDto pruneOutboxInfo(final String userId,
            final Date expiryRef) {

        final OutboxInfoDto dtoRead = readOutboxInfo(userId);

        final OutboxInfoDto dtoPruned =
                pruneOutboxInfo(userId, dtoRead, expiryRef);

        if (dtoPruned != dtoRead) {
            this.storeOutboxInfo(userId, dtoPruned);
        }

        return dtoPruned;
    }

    /**
     * Prunes the {@link OutboxJobDto} instances in {@link OutboxInfoDto} for
     * jobs which are expired for Proxy Printing, or for which the outbox PDF
     * file has been deleted. Also, outbox PDF files which are not referenced by
     * an {@link OutboxJobDto} are deleted.
     * <p>
     * IMPORTANT: when nothing is pruned the {@link OutboxInfoDto} <b>input</b>
     * object is returned.
     * </p>
     * <p>
     * NOTE: no user information is supplied, and therefore the pruned result is
     * NOT persisted.
     * </p>
     *
     * @since 0.9.6
     *
     * @param userId
     *            The unique user id.
     * @param outboxInfo
     *            The full {@link OutboxInfoDto}
     * @param expiryRef
     *            The reference date for calculating the expiration.
     * @return A new {@link OutboxInfoDto} object with a subset of valid Fast
     *         Proxy Printing jobs, or the {@link OutboxInfoDto} input object
     *         when nothing was pruned.
     */
    private OutboxInfoDto pruneOutboxInfo(final String userId,
            final OutboxInfoDto outboxInfo, final Date expiryRef) {

        /*
         * Return when jobs are absent.
         */
        if (outboxInfo.getJobs().isEmpty()) {
            return outboxInfo;
        }

        /*
         * Time parameters.
         */
        final long expiryRefTime = expiryRef.getTime();

        /*
         * Initialize the pruned OutboxInfoDto.
         */
        final OutboxInfoDto prunedInboxInfo = new OutboxInfoDto();

        /*
         * Traverse the jobs: add jobs which are not expired.
         */
        int nPruned = 0;

        for (final Entry<String, OutboxJobDto> entry : outboxInfo.getJobs()
                .entrySet()) {

            final OutboxJobDto job = entry.getValue();

            /*
             * Add job if not expired.
             */
            if (job.getExpiryTime() - expiryRefTime > 0) {
                prunedInboxInfo.addJob(job.getFile(), job);
            } else {
                nPruned++;
            }
        }

        final OutboxInfoDto returnInfo;

        if (nPruned == 0) {
            /*
             * Return the original input jobInfo if end-result is identical
             * (nothing was pruned).
             */
            returnInfo = outboxInfo;
        } else {
            returnInfo = prunedInboxInfo;
        }

        /*
         * Always check if PDF files are in sync with job descriptions.
         */
        returnInfo.setJobs(pruneOutboxJobFiles(userId, returnInfo.getJobs()));

        storeOutboxInfo(userId, returnInfo);

        return returnInfo;
    }

    /**
     * Prunes the outbox jobs.
     *
     * @param userId
     *            The unique user id.
     * @param outboxJobs
     *            the outbox jobs.
     * @return the pruned jobs.
     */
    private LinkedHashMap<String, OutboxJobDto> pruneOutboxJobFiles(
            final String userId,
            final LinkedHashMap<String, OutboxJobDto> outboxJobs) {

        final FileFilter filefilter = new FileFilter() {
            @Override
            public boolean accept(final File file) {
                return file.isFile()
                        && FilenameUtils.getExtension(file.getName())
                                .equalsIgnoreCase(DocContent.FILENAME_EXT_PDF);
            }
        };

        /*
         * Get all the PDF job files.
         */
        final File[] files = getUserOutboxDir(userId).listFiles(filefilter);

        final LinkedHashMap<String, OutboxJobDto> prunedOutboxJobs =
                new LinkedHashMap<>();

        if (files != null) {

            for (final File file : files) {

                final String fileKey = file.getName();

                if (outboxJobs.containsKey(fileKey)) {

                    prunedOutboxJobs.put(fileKey, outboxJobs.get(fileKey));

                } else {
                    /*
                     * No job description found for PDF file: delete PDF.
                     */
                    file.delete();
                }
            }
        }
        return prunedOutboxJobs;
    }

    @Override
    public int cancelOutboxJobs(final String userId) {

        final OutboxInfoDto outboxInfo = this.readOutboxInfo(userId);

        for (final Entry<String, OutboxJobDto> entry : outboxInfo.getJobs()
                .entrySet()) {
            this.onOutboxJobCanceled(entry.getValue());
        }

        final int jobCount = outboxInfo.getJobCount();
        final OutboxInfoDto dto = new OutboxInfoDto();
        this.pruneOutboxJobFiles(userId, dto.getJobs());
        storeOutboxInfo(userId, dto);
        return jobCount;
    }

    @Override
    public boolean cancelOutboxJob(final String userId, final String fileName) {

        final OutboxInfoDto outboxInfo = readOutboxInfo(userId);
        final OutboxJobDto removedJob = outboxInfo.getJobs().remove(fileName);

        if (removedJob == null) {
            return false;
        }

        this.onOutboxJobCanceled(removedJob);
        this.storeOutboxInfo(userId, outboxInfo);

        return true;
    }

    /**
     * Helper function to fill the {@link LocaleInfo }.
     *
     * @param localeInfo
     *            the {@link LocaleInfo }.
     * @param timeFormatter
     *            the time formatter.
     * @param dateNow
     *            now.
     * @param submitDate
     *            date submitted.
     * @param expiryDate
     *            dated expired.
     */
    private void applyLocaleInfo(final LocaleInfo localeInfo,
            final DateFormat timeFormatter, final Date dateNow,
            final Date submitDate, final Date expiryDate) {

        localeInfo.setSubmitTime(timeFormatter.format(submitDate));
        localeInfo.setExpiryTime(timeFormatter.format(expiryDate));

        final long remainMillis = expiryDate.getTime() - dateNow.getTime();

        final String remainTime;

        if (remainMillis < 0) {
            remainTime = String.format("-%s",
                    DurationFormatUtils.formatDuration(-remainMillis, "H:mm"));
        } else {
            remainTime =
                    DurationFormatUtils.formatDuration(remainMillis, "H:mm");
        }
        localeInfo.setRemainTime(remainTime);
    }

    @Override
    public void applyLocaleInfo(final OutboxInfoDto outboxInfo,
            final Locale locale, final String currencySymbol) {

        final Date dateNow = new Date();

        final int nDecimals = ConfigManager.instance()
                .getConfigInt(Key.FINANCIAL_USER_BALANCE_DECIMALS, 2);

        final DateFormat timeFormatter =
                new SimpleDateFormat("yyyy-MM-dd' 'HH:mm");

        BigDecimal costTotal = BigDecimal.ZERO;
        Date firstSubmitDate = null;
        Date firstExpiryDate = null;

        try {

            for (final Entry<String, OutboxJobDto> entry : outboxInfo.getJobs()
                    .entrySet()) {

                final OutboxJobDto job = entry.getValue();

                final BigDecimal jobCost = job.getCostTotal();

                costTotal = costTotal.add(jobCost);

                final LocaleInfo localeInfo = new LocaleInfo();
                job.setLocaleInfo(localeInfo);

                localeInfo.setCost(BigDecimalUtil.localize(jobCost, nDecimals,
                        locale, currencySymbol, true));

                final Date submitDate = new Date(job.getSubmitTime());
                final Date expiryDate = new Date(job.getExpiryTime());

                if (firstSubmitDate == null
                        || firstSubmitDate.getTime() > job.getSubmitTime()) {
                    firstSubmitDate = submitDate;
                }

                if (firstExpiryDate == null
                        || firstExpiryDate.getTime() > job.getExpiryTime()) {
                    firstExpiryDate = expiryDate;
                }

                applyLocaleInfo(localeInfo, timeFormatter, dateNow, submitDate,
                        new Date(job.getExpiryTime()));

            }

            final LocaleInfo localeInfo = new LocaleInfo();
            outboxInfo.setLocaleInfo(localeInfo);

            localeInfo.setCost(BigDecimalUtil.localize(costTotal, nDecimals,
                    locale, currencySymbol, true));

            if (firstSubmitDate != null) {
                applyLocaleInfo(localeInfo, timeFormatter, dateNow,
                        firstSubmitDate, firstExpiryDate);
            }

        } catch (ParseException e) {
            throw new SpException(e.getMessage());
        }
    }

    @Override
    public int extendOutboxExpiry(final String userId, final int minutes) {

        final long extendedTime =
                DateUtils.addMinutes(new Date(), minutes).getTime();

        final OutboxInfoDto outboxInfo = readOutboxInfo(userId);

        int nExtended = 0;

        for (final Entry<String, OutboxJobDto> entry : outboxInfo.getJobs()
                .entrySet()) {

            final OutboxJobDto job = entry.getValue();

            if (job.getExpiryTime() < extendedTime) {
                job.setExpiryTime(extendedTime);
                nExtended++;
            }
        }

        if (nExtended > 0) {
            this.storeOutboxInfo(userId, outboxInfo);
        }

        return nExtended;
    }

    @Override
    public AccountTrxInfoSet
            createAccountTrxInfoSet(final OutboxJobDto source) {

        final OutboxAccountTrxInfoSet sourceInfoSet =
                source.getAccountTransactions();

        if (sourceInfoSet == null) {
            return null;
        }

        final AccountTrxInfoSet targetInfoSet =
                new AccountTrxInfoSet(sourceInfoSet.getWeightTotal());

        final List<AccountTrxInfo> transactions = new ArrayList<>();
        targetInfoSet.setAccountTrxInfoList(transactions);

        for (final OutboxAccountTrxInfo sourceTrxInfo : sourceInfoSet
                .getTransactions()) {

            /*
             * INVARIANT: Account MUST be active?
             */
            final Account account =
                    accountDAO().findById(sourceTrxInfo.getAccountId());

            if (account.getDeleted()) {
                // TODO
            }

            //
            final AccountTrxInfo targetTrxInfo = new AccountTrxInfo();

            transactions.add(targetTrxInfo);
            //
            targetTrxInfo.setAccount(account);
            targetTrxInfo.setExtDetails(sourceTrxInfo.getExtDetails());
            targetTrxInfo.setWeight(Integer.valueOf(sourceTrxInfo.getWeight()));
        }

        //
        return targetInfoSet;
    }

    /**
     * Imports the source {@link AccountTrxInfoSet} into the target
     * {@link OutboxJobDto}.
     *
     * @see {@link #createAccountTrxInfoSet(OutboxJobDto)}.
     * @param source
     *            The {@link AccountTrxInfoSet}
     * @param target
     *            The {@link OutboxJobDto}.
     */
    private void importToOutbox(final AccountTrxInfoSet source,
            final OutboxJobDto target) {

        if (source == null) {
            target.setAccountTransactions(null);
            return;
        }

        //
        final OutboxAccountTrxInfoSet infoSet = new OutboxAccountTrxInfoSet();
        target.setAccountTransactions(infoSet);

        //
        infoSet.setWeightTotal(source.getWeightTotal());

        final List<OutboxAccountTrxInfo> transactions = new ArrayList<>();
        infoSet.setTransactions(transactions);

        //
        for (final AccountTrxInfo trxInfo : source.getAccountTrxInfoList()) {

            final OutboxAccountTrxInfo outboxTrxInfo =
                    new OutboxAccountTrxInfo();

            transactions.add(outboxTrxInfo);
            //
            outboxTrxInfo.setAccountId(trxInfo.getAccount().getId());
            outboxTrxInfo.setExtDetails(trxInfo.getExtDetails());
            outboxTrxInfo.setWeight(trxInfo.getWeight().intValue());
        }
    }

    @Override
    public OutboxInfoDto getOutboxJobTicketInfo(final User user,
            final Date expiryRef) {

        final OutboxInfoDto outboxInfo =
                pruneOutboxInfo(user.getUserId(), expiryRef);

        for (final OutboxJobDto dto : jobTicketService()
                .getTickets(user.getId())) {
            outboxInfo.addJob(dto.getFile(), dto);
        }

        return outboxInfo;
    }

    @Override
    public void onOutboxJobCanceled(final OutboxJobDto job) {
        this.onRemoveOutboxJob(job, true);
    }

    @Override
    public void onOutboxJobCompleted(final OutboxJobDto job) {
        this.onRemoveOutboxJob(job, false);
    }

    /**
     * Notifies removal of outbox job.
     * <p>
     * NOTE: When the outbox job was created from an
     * {@link ExternalSupplierEnum} other than
     * {@link ExternalSupplierEnum#SAVAPAGE} the print-in {@link DocLog} is
     * updated with either {@link ExternalSupplierStatusEnum#PENDING_CANCEL} or
     * {@link ExternalSupplierStatusEnum#PENDING_COMPLETE}.
     * </p>
     *
     * @param job
     *            The {@link OutboxJobDto}.
     * @param isCanceled
     *            {@code true} when removed due to cancellation.
     */
    private void onRemoveOutboxJob(final OutboxJobDto job,
            final boolean isCanceled) {

        final ExternalSupplierInfo supplierInfo = job.getExternalSupplierInfo();
        if (supplierInfo == null) {
            return;
        }

        final ExternalSupplierEnum supplier = supplierInfo.getSupplier();
        if (supplier == null) {
            return;
        }

        if (supplier == ExternalSupplierEnum.SAVAPAGE) {
            return;
        }

        if (supplier != ExternalSupplierEnum.SMARTSCHOOL) {
            throw new SpException(String.format("%s.%s is not handled.",
                    ExternalSupplierInfo.class.getSimpleName(),
                    supplier.toString()));
        }

        final String accountToFind = supplierInfo.getAccount();

        final String supplierId = supplierInfo.getId();

        final ExternalSupplierStatusEnum statusCurrent =
                ExternalSupplierStatusEnum.PENDING;

        final DocLog docLog = docLogService().getSuppliedDocLog(supplier,
                accountToFind, supplierId, statusCurrent);

        /*
         * Be forgiving if no DocLog found.
         */
        if (docLog == null) {
            LOGGER.error(String.format(
                    "DocLog from External Supplier [%s] Account [%s] "
                            + "ID [%s] Status [%s]: not found.",
                    supplier.toString(), accountToFind, supplierId,
                    statusCurrent.toString()));
            return;
        }

        final ExternalSupplierStatusEnum statusNew;
        if (isCanceled) {
            statusNew = ExternalSupplierStatusEnum.PENDING_CANCEL;
        } else {
            statusNew = ExternalSupplierStatusEnum.PENDING_COMPLETE;
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(String.format(
                    "DocLog from External Supplier [%s] Account [%s] "
                            + "ID [%s]: changed external status [%s] to [%s]",
                    supplier.toString(), accountToFind, supplierId,
                    statusCurrent.toString(), statusNew.toString()));
        }

        final DaoContext daoCtx = ServiceContext.getDaoContext();
        final boolean adhocTransaction = !daoCtx.isTransactionActive();

        if (adhocTransaction) {
            ReadWriteLockEnum.DATABASE_READONLY.setReadLock(true);
            daoCtx.beginTransaction();
        }
        try {
            docLog.setExternalStatus(statusNew.toString());
            docLogDAO().update(docLog);
            if (adhocTransaction) {
                daoCtx.commit();
            }

        } finally {
            if (adhocTransaction) {
                daoCtx.rollback();
                ReadWriteLockEnum.DATABASE_READONLY.setReadLock(false);
            }
        }
    }

    @Override
    public boolean isMonitorPaperCutPrintStatus(final OutboxJobDto job) {
        return job.getAccountTransactions() != null
                && job.getAccountTransactions().getTransactions() != null
                && paperCutService().isExtPaperCutPrint(job.getPrinterName());
    }

}
