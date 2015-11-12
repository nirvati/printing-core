/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2014 Datraverse B.V.
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
package org.savapage.core.services.impl;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.FileSystems;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.savapage.core.LetterheadNotFoundException;
import org.savapage.core.PostScriptDrmException;
import org.savapage.core.SpException;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp;
import org.savapage.core.doc.DocContent;
import org.savapage.core.imaging.EcoPrintPdfTask;
import org.savapage.core.imaging.EcoPrintPdfTaskPendingException;
import org.savapage.core.inbox.InboxInfoDto;
import org.savapage.core.inbox.InboxInfoDto.InboxJobRange;
import org.savapage.core.jpa.DocLog;
import org.savapage.core.jpa.User;
import org.savapage.core.outbox.OutboxInfoDto;
import org.savapage.core.outbox.OutboxInfoDto.LocaleInfo;
import org.savapage.core.outbox.OutboxInfoDto.OutboxJob;
import org.savapage.core.pdf.PdfPrintCollector;
import org.savapage.core.pdf.PdfCreateRequest;
import org.savapage.core.print.proxy.AbstractProxyPrintReq.Status;
import org.savapage.core.print.proxy.ProxyPrintInboxReq;
import org.savapage.core.print.proxy.ProxyPrintJobChunk;
import org.savapage.core.services.OutboxService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.util.BigDecimalUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 *
 * @since 0.9.6
 * @author Datraverse B.V.
 */
public final class OutboxServiceImpl extends AbstractService implements
        OutboxService {

    /**
     * The logger.
     */
    private static final Logger LOGGER = LoggerFactory
            .getLogger(OutboxServiceImpl.class);

    private static final boolean APPLY_PDF_PROPS = true;
    private static final boolean APPLY_LETTERHEAD = true;
    private static final boolean PDF_FOR_PRINTING = true;

    /**
    *
    */
    private static final String OUTBOX_DESCRIPT_FILE_NAME = "outbox.json";

    /**
    *
    */
    private static final String USER_RELATIVE_OUTBOX_PATH = "outbox";

    @Override
    public File getUserOutboxDir(final String userId) {
        return FileSystems
                .getDefault()
                .getPath(ConfigManager.getUserHomeDir(userId),
                        USER_RELATIVE_OUTBOX_PATH).toFile();
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
        return FileSystems
                .getDefault()
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
    void storeOutboxInfo(final String userId, final OutboxInfoDto outboxInfo) {

        final File jsonFile = getOutboxInfoFilePath(userId);
        final ObjectMapper mapper = new ObjectMapper();

        try {
            mapper.writeValue(jsonFile, outboxInfo);
        } catch (IOException e) {
            throw new SpException("Error writing file ["
                    + jsonFile.getAbsolutePath() + "]", e);
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
            throw new SpException("Error reading file ["
                    + file.getAbsolutePath() + "]", e);
        }
        return outboxInfo;
    }

    @Override
    public void proxyPrintInbox(final User lockedUser,
            final ProxyPrintInboxReq request)
            throws EcoPrintPdfTaskPendingException {

        final Date submitDate = ServiceContext.getTransactionDate();
        final Date expiryDate =
                DateUtils.addMinutes(
                        submitDate,
                        ConfigManager.instance().getConfigInt(
                                IConfigProp.Key.PROXY_PRINT_HOLD_EXPIRY_MINS));

        final OutboxInfoDto outboxInfo =
                this.readOutboxInfo(lockedUser.getUserId());

        /*
         * When printing the chunks, the container request parameters are
         * replaced by chunk values. So, we save the original request parameters
         * here, and restore them afterwards.
         */
        final String orgJobName = request.getJobName();
        final int orgNumberOfPages = request.getNumberOfPages();
        final Boolean orgFitToPage = request.getFitToPage();
        final String orgMediaOption = request.getMediaOption();
        final String orgMediaSourceOption = request.getMediaSourceOption();
        final BigDecimal orgCost = request.getCost();

        try {

            if (request.getJobChunkInfo() == null) {

                final InboxInfoDto inboxInfo =
                        inboxService().readInboxInfo(lockedUser.getUserId());

                final InboxInfoDto filteredInboxInfo =
                        inboxService().filterInboxInfoPages(inboxInfo,
                                request.getPageRanges());

                proxyPrintInboxChunk(lockedUser, request, filteredInboxInfo,
                        outboxInfo, submitDate, expiryDate);

            } else {

                final InboxInfoDto inboxInfo =
                        request.getJobChunkInfo().getInboxInfo();

                for (final ProxyPrintJobChunk chunk : request.getJobChunkInfo()
                        .getChunks()) {

                    /*
                     * Replace the request parameters with the chunk parameters.
                     */
                    request.setNumberOfPages(chunk.getNumberOfPages());
                    request.setFitToPage(chunk.getFitToPage());
                    request.setMediaOption(chunk.getAssignedMedia()
                            .getIppKeyword());
                    request.setMediaSourceOption(chunk.getAssignedMediaSource()
                            .getSource());
                    request.setCost(chunk.getCost());

                    if (StringUtils.isBlank(orgJobName)) {
                        request.setJobName(chunk.getJobName());
                    }

                    /*
                     * Save the original pages.
                     */
                    final ArrayList<InboxJobRange> orgPages =
                            inboxService().replaceInboxInfoPages(inboxInfo,
                                    chunk.getRanges());

                    /*
                     * Proxy print the chunk.
                     */
                    proxyPrintInboxChunk(lockedUser, request, inboxInfo,
                            outboxInfo, submitDate, expiryDate);

                    /*
                     * Restore the original pages.
                     */
                    inboxInfo.setPages(orgPages);
                }
            }

            storeOutboxInfo(lockedUser.getUserId(), outboxInfo);

            request.setStatus(Status.WAITING_FOR_RELEASE);
            request.setUserMsgKey("msg-user-print-outbox");
            request.setUserMsg(localize("msg-user-print-outbox",
                    request.getPrinterName()));

        } finally {
            /*
             * Restore the original request parameters.
             */
            request.setJobName(orgJobName);
            request.setNumberOfPages(orgNumberOfPages);
            request.setFitToPage(orgFitToPage);
            request.setMediaOption(orgMediaOption);
            request.setMediaSourceOption(orgMediaSourceOption);
            request.setCost(orgCost);
        }
    }

    /**
     * Proxy prints a single inbox chunk to the outbox.
     *
     * @param lockedUser
     *            The requesting {@link User}, which should be locked.
     * @param request
     *            The {@link ProxyPrintInboxReq}.
     * @param inboxInfo
     *            The (filtered) {@link InboxInfoDto}.
     * @param outboxInfo
     *            The {@link OutboxInfoDto} to append the info about the print
     *            job on.
     * @param submitDate
     *            The data the proxy print was submitted.
     * @param expiryDate
     *            The data the proxy print expires.
     * @throws EcoPrintPdfTaskPendingException
     *             When {@link EcoPrintPdfTask} objects needed for this PDF are
     *             pending.
     */
    private void proxyPrintInboxChunk(final User lockedUser,
            final ProxyPrintInboxReq request, final InboxInfoDto inboxInfo,
            final OutboxInfoDto outboxInfo, final Date submitDate,
            final Date expiryDate) throws EcoPrintPdfTaskPendingException {

        final DocLog docLog = null;

        /*
         * Generate the PDF file.
         */
        File pdfFileToPrint = null;

        boolean fileCreated = false;

        try {

            final String pdfFileName =
                    createUuidFileName(lockedUser.getUserId())
                            .getAbsolutePath();

            final LinkedHashMap<String, Integer> uuidPageCount =
                    new LinkedHashMap<>();

            final PdfCreateRequest pdfRequest = new PdfCreateRequest();

            pdfRequest.setUserObj(lockedUser);
            pdfRequest.setPdfFile(pdfFileName);
            pdfRequest.setInboxInfo(inboxInfo);
            pdfRequest.setRemoveGraphics(request.isRemoveGraphics());

            pdfRequest.setEcoPdf(request.isEcoPrint());
            pdfRequest.setEcoPdfShadow(request.isEcoPrintShadow());

            pdfRequest.setGrayscale(request.isConvertToGrayscale());
            pdfRequest.setApplyPdfProps(!APPLY_PDF_PROPS);
            pdfRequest.setApplyLetterhead(APPLY_LETTERHEAD);
            pdfRequest.setForPrinting(PDF_FOR_PRINTING);

            pdfFileToPrint =
                    outputProducer().generatePdf(pdfRequest, uuidPageCount,
                            docLog);

            final OutboxJob job = new OutboxJob();
            //
            job.setFile(pdfFileToPrint.getName());
            job.setPrinterName(request.getPrinterName());
            job.setJobName(request.getJobName());
            job.setCopies(request.getNumberOfCopies());
            job.setPages(request.getNumberOfPages());
            job.setSheets(calNumberOfSheets(request));
            job.setRemoveGraphics(request.isRemoveGraphics());
            job.setEcoPrint(request.isEcoPrint() || request.isEcoPrintShadow());
            job.setCollate(request.isCollate());
            job.setCost(request.getCost());
            job.setSubmitTime(submitDate.getTime());
            job.setExpiryTime(expiryDate.getTime());
            job.setFitToPage(request.getFitToPage());

            job.setUuidPageCount(uuidPageCount);

            job.putOptionValues(request.getOptionValues());

            outboxInfo.addJob(job.getFile(), job);

            fileCreated = true;

        } catch (LetterheadNotFoundException | PostScriptDrmException e) {

            throw new SpException(e.getMessage());

        } finally {

            if (!fileCreated && pdfFileToPrint != null
                    && pdfFileToPrint.exists()) {
                if (pdfFileToPrint.delete()) {
                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace("deleted file [" + pdfFileToPrint + "]");
                    }
                } else {
                    LOGGER.error("delete of file [" + pdfFileToPrint
                            + "] FAILED");
                }
            }
        }
    }

    @Override
    public File getOutboxFile(final String userId, final String fileName) {

        try {
            return FileSystems
                    .getDefault()
                    .getPath(getUserOutboxDir(userId).getCanonicalPath(),
                            fileName).toFile();
        } catch (IOException e) {
            throw new SpException(e.getMessage());
        }
    }

    /**
     * Calculates the number of sheets as requested in the
     * {@link ProxyPrintInboxReq} request.
     *
     * @param request
     *            The request.
     * @return The number of sheets.
     */
    private static int calNumberOfSheets(final ProxyPrintInboxReq request) {
        return PdfPrintCollector.calcNumberOfPrintedSheets(request);
    }

    /**
     * Creates a unique outbox PDF file name.
     *
     * @param userId
     *            The unique id of the user.
     * @return the file.
     */
    private File createUuidFileName(final String userId) {

        return getOutboxFile(userId, java.util.UUID.randomUUID().toString()
                + "." + DocContent.FILENAME_EXT_PDF);
    }

    @Override
    public List<OutboxJob> getOutboxJobs(final String userId,
            final Set<String> printerNames, final Date expiryRef) {

        final OutboxInfoDto outboxInfo =
                pruneOutboxInfo(userId, readOutboxInfo(userId), expiryRef);

        final List<OutboxJob> jobs = new ArrayList<>();

        for (final Entry<String, OutboxJob> entry : outboxInfo.getJobs()
                .entrySet()) {

            final OutboxJob job = entry.getValue();

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
     * Prunes the {@link OutboxJob} instances in {@link OutboxInfoDto} for jobs
     * which are expired for Proxy Printing, or for which the outbox PDF file
     * has been deleted. Also, outbox PDF files which are not referenced by an
     * {@link OutboxJob} are deleted.
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

        for (final Entry<String, OutboxJob> entry : outboxInfo.getJobs()
                .entrySet()) {

            final OutboxJob job = entry.getValue();

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
    private LinkedHashMap<String, OutboxJob> pruneOutboxJobFiles(
            final String userId,
            final LinkedHashMap<String, OutboxJob> outboxJobs) {

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

        final LinkedHashMap<String, OutboxJob> prunedOutboxJobs =
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
    public int clearOutbox(final String userId) {
        final int jobCount = this.readOutboxInfo(userId).getJobCount();
        final OutboxInfoDto dto = new OutboxInfoDto();
        this.pruneOutboxJobFiles(userId, dto.getJobs());
        storeOutboxInfo(userId, dto);
        return jobCount;
    }

    @Override
    public boolean removeOutboxJob(final String userId, final String fileName) {

        final OutboxInfoDto outboxInfo = readOutboxInfo(userId);

        final boolean removedJob =
                outboxInfo.getJobs().remove(fileName) != null;

        if (removedJob) {
            this.storeOutboxInfo(userId, outboxInfo);
        }

        return removedJob;
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
        localeInfo.setRemainTime(DurationFormatUtils.formatDuration(
                expiryDate.getTime() - dateNow.getTime(), "H:mm"));

    }

    @Override
    public void applyLocaleInfo(final OutboxInfoDto outboxInfo,
            final Locale locale, final String currencySymbol) {

        final Date dateNow = new Date();

        final int nDecimals = 2;

        final DateFormat timeFormatter =
                DateFormat.getTimeInstance(DateFormat.SHORT, Locale.GERMAN);

        BigDecimal costTotal = BigDecimal.ZERO;
        Date firstSubmitDate = null;
        Date firstExpiryDate = null;

        try {

            for (final Entry<String, OutboxJob> entry : outboxInfo.getJobs()
                    .entrySet()) {

                final OutboxJob job = entry.getValue();

                costTotal = costTotal.add(job.getCost());

                final LocaleInfo localeInfo = new LocaleInfo();
                job.setLocaleInfo(localeInfo);

                localeInfo.setCost(BigDecimalUtil.localize(job.getCost(),
                        nDecimals, locale, currencySymbol, true));

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

        for (final Entry<String, OutboxJob> entry : outboxInfo.getJobs()
                .entrySet()) {

            final OutboxJob job = entry.getValue();

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

}
