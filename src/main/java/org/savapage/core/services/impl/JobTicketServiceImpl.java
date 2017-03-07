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

import static java.nio.file.FileVisitResult.CONTINUE;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.mail.MessagingException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.savapage.core.SpException;
import org.savapage.core.circuitbreaker.CircuitBreakerException;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.dao.enums.ACLRoleEnum;
import org.savapage.core.dao.enums.PrinterAttrEnum;
import org.savapage.core.doc.DocContent;
import org.savapage.core.dto.RedirectPrinterDto;
import org.savapage.core.imaging.EcoPrintPdfTaskPendingException;
import org.savapage.core.ipp.IppJobStateEnum;
import org.savapage.core.ipp.attribute.IppDictJobTemplateAttr;
import org.savapage.core.ipp.client.IppConnectException;
import org.savapage.core.ipp.helpers.IppOptionMap;
import org.savapage.core.jpa.PrintOut;
import org.savapage.core.jpa.Printer;
import org.savapage.core.jpa.PrinterGroup;
import org.savapage.core.jpa.PrinterGroupMember;
import org.savapage.core.jpa.User;
import org.savapage.core.outbox.OutboxInfoDto.OutboxJobBaseDto;
import org.savapage.core.outbox.OutboxInfoDto.OutboxJobDto;
import org.savapage.core.pdf.PdfCreateInfo;
import org.savapage.core.print.proxy.AbstractProxyPrintReq;
import org.savapage.core.print.proxy.AbstractProxyPrintReq.Status;
import org.savapage.core.print.proxy.JsonProxyPrinter;
import org.savapage.core.print.proxy.JsonProxyPrinterOpt;
import org.savapage.core.print.proxy.JsonProxyPrinterOptChoice;
import org.savapage.core.print.proxy.ProxyPrintDocReq;
import org.savapage.core.print.proxy.ProxyPrintInboxReq;
import org.savapage.core.services.JobTicketService;
import org.savapage.core.services.OutboxService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.helpers.DocContentPrintInInfo;
import org.savapage.core.services.helpers.PrinterAttrLookup;
import org.savapage.core.services.helpers.ProxyPrintInboxPattern;
import org.savapage.core.services.helpers.ThirdPartyEnum;
import org.savapage.core.services.helpers.email.EmailMsgParms;
import org.savapage.core.util.DateUtil;
import org.savapage.core.util.JsonHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonMappingException;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class JobTicketServiceImpl extends AbstractService
        implements JobTicketService {

    /**
     * .
     */
    public static final String FILENAME_EXT_JSON = "json";

    /**
     * .
     */
    private static final String FILENAME_EXT_PDF = DocContent.FILENAME_EXT_PDF;

    /**
     * The logger.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(JobTicketServiceImpl.class);

    /**
     *
     */
    private static final int TICKER_NUMBER_CHUNK_WIDTH = 4;

    /**
     *
     */
    private static final String TICKER_NUMBER_CHUNK_SEPARATOR = "-";

    /**
     *
     */
    private ConcurrentHashMap<UUID, OutboxJobDto> jobTicketCache;

    /**
     * Implementation of execution pattern for proxy printing from the user
     * inbox to job ticket.
     */
    private static final class ProxyPrintInbox extends ProxyPrintInboxPattern {

        private final JobTicketServiceImpl serviceImpl;

        /**
         * The date the proxy print was submitted.
         */
        private final Date submitDate;

        /**
         * The requested delivery date.
         */
        private final Date deliveryDate;

        /**
         * The job tickets created.
         */
        private final List<OutboxJobDto> ticketsCreated;

        /**
         *
         * @param service
         *            The parent service.
         * @param deliveryDate
         *            The requested date of delivery (can be {@code null}).
         */
        ProxyPrintInbox(final JobTicketServiceImpl service,
                final Date deliveryDate) {
            this.serviceImpl = service;
            this.submitDate = ServiceContext.getTransactionDate();
            this.deliveryDate = this.serviceImpl
                    .getTicketDeliveryDate(submitDate, deliveryDate);
            this.ticketsCreated = new ArrayList<>();
        }

        @Override
        protected void onInit(final User lockedUser,
                final ProxyPrintInboxReq request) {
        }

        @Override
        protected void onExit(final User lockedUser,
                final ProxyPrintInboxReq request) {
            request.setStatus(Status.WAITING_FOR_RELEASE);
        }

        @Override
        protected File onReservePdfToGenerate(final User lockedUser) {
            return getJobTicketFile(java.util.UUID.randomUUID(),
                    FILENAME_EXT_PDF);
        }

        @Override
        protected void onPdfGenerated(final User lockedUser,
                final ProxyPrintInboxReq request,
                final LinkedHashMap<String, Integer> uuidPageCount,
                final PdfCreateInfo createInfo) {

            final UUID uuid = UUID.fromString(FilenameUtils
                    .getBaseName(createInfo.getPdfFile().getName()));

            try {
                final OutboxJobDto dto = this.serviceImpl.addJobticketToCache(
                        lockedUser, createInfo, uuid, request, uuidPageCount,
                        this.submitDate, this.deliveryDate);
                ticketsCreated.add(dto);
            } catch (IOException e) {
                throw new SpException(e.getMessage(), e);
            }
        }

        /**
         * @return The job tickets created.
         */
        public List<OutboxJobDto> getTicketsCreated() {
            return ticketsCreated;
        }

    }

    @Override
    public void proxyPrintPdf(final User lockedUser,
            final ProxyPrintDocReq request, final PdfCreateInfo createInfo,
            final DocContentPrintInInfo printInfo, final Date deliveryDate)
            throws IOException {

        final UUID uuid = UUID.fromString(request.getDocumentUuid());

        /*
         * Move PDF file to ticket Outbox.
         */
        final File pdfFileTicket = getJobTicketFile(uuid, FILENAME_EXT_PDF);
        FileUtils.moveFile(createInfo.getPdfFile(), pdfFileTicket);

        createInfo.setPdfFile(pdfFileTicket);

        /*
         * Add to cache.
         */
        final LinkedHashMap<String, Integer> uuidPageCount =
                new LinkedHashMap<>();
        uuidPageCount.put(uuid.toString(), request.getNumberOfPages());

        final OutboxJobDto dto = this.addJobticketToCache(lockedUser,
                createInfo, uuid, request, uuidPageCount,
                ServiceContext.getTransactionDate(), deliveryDate);

        final String msgKey = "msg-user-print-jobticket-print";

        request.setStatus(Status.WAITING_FOR_RELEASE);
        request.setUserMsgKey(msgKey);
        request.setUserMsg(localize(msgKey, dto.getTicketNumber()));
    }

    /**
     * Creates sibling JSON file with proxy print information and adds Job
     * Ticket to the cache.
     *
     * @param user
     *            The requesting {@link User}.
     * @param createInfo
     *            The {@link PdfCreateInfo} with the PDF file to be printed by
     *            the Job Ticket. Is {@code null} when Copy Job Ticket.
     * @param uuid
     *            The Job Ticket {@link UUID}.
     * @param request
     *            The {@link AbstractProxyPrintReq}.
     * @param uuidPageCount
     *            Object filled with the number of selected pages per input file
     *            UUID. Is {@code null} when Copy Job Ticket.
     * @param submitDate
     *            The submit date.
     * @param deliveryDate
     *            The requested date of delivery.
     * @throws IOException
     *             When file IO error occurs.
     */
    private OutboxJobDto addJobticketToCache(final User user,
            final PdfCreateInfo createInfo, final UUID uuid,
            final AbstractProxyPrintReq request,
            final LinkedHashMap<String, Integer> uuidPageCount,
            final Date submitDate, final Date deliveryDate) throws IOException {

        final OutboxJobDto dto = outboxService().createOutboxJob(request,
                submitDate, deliveryDate, createInfo, uuidPageCount);

        dto.setUserId(user.getId());
        dto.setTicketNumber(this.createTicketNumber());

        final File jsonFileTicket = getJobTicketFile(uuid, FILENAME_EXT_JSON);

        if (createInfo == null) {
            dto.setFile(jsonFileTicket.getName());
        }

        Writer writer = null;
        try {
            writer = new FileWriter(jsonFileTicket);
            JsonHelper.write(dto, writer);
            writer.close();
        } finally {
            IOUtils.closeQuietly(writer);
        }

        /*
         * Add to cache.
         */
        this.jobTicketCache.put(uuid, dto);

        return dto;
    }

    /**
     * Gets the full file path of a Job Ticket file.
     *
     * @param uuid
     *            The {@link UUID}
     * @param fileExt
     *            The JSON or PDF file extension.
     * @return The job ticket JSON or PDF file.
     */
    private static File getJobTicketFile(final UUID uuid,
            final String fileExt) {
        return Paths
                .get(ConfigManager.getJobTicketsHome().toString(),
                        String.format("%s.%s", uuid.toString(), fileExt))
                .toFile();
    }

    /**
     * Creates a job ticket cache.
     *
     * @return The cache.
     * @throws IOException
     *             When IO error.
     */
    private static ConcurrentHashMap<UUID, OutboxJobDto> createTicketCache()
            throws IOException {

        final ConcurrentHashMap<UUID, OutboxJobDto> jobTicketMap =
                new ConcurrentHashMap<>();

        final Set<UUID> uuidToDelete = new HashSet<>();

        final SimpleFileVisitor<Path> visitor = new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(final Path file,
                    final BasicFileAttributes attrs) throws IOException {

                final String filePath = file.toString();

                if (!FilenameUtils.getExtension(filePath)
                        .equalsIgnoreCase(FILENAME_EXT_JSON)) {
                    return CONTINUE;
                }

                final UUID uuid =
                        UUID.fromString(FilenameUtils.getBaseName(filePath));

                try {
                    final OutboxJobDto dto =
                            JsonHelper.read(OutboxJobDto.class, file.toFile());
                    jobTicketMap.put(uuid, dto);
                } catch (JsonMappingException e) {

                    /*
                     * There has been a change in layout of the JSON file...
                     */
                    uuidToDelete.add(uuid);
                }
                return CONTINUE;
            }
        };

        Files.walkFileTree(ConfigManager.getJobTicketsHome(), visitor);

        /*
         * Clean up misfits?
         */
        if (!uuidToDelete.isEmpty()) {
            final Iterator<UUID> iter = uuidToDelete.iterator();
            while (iter.hasNext()) {
                final UUID uuid = iter.next();
                getJobTicketFile(uuid, FILENAME_EXT_PDF).delete();
                getJobTicketFile(uuid, FILENAME_EXT_JSON).delete();
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn(
                            String.format("Jobticket JSON error: deleted [%s]",
                                    uuid.toString()));
                }
            }
        }

        return jobTicketMap;
    }

    @Override
    public void start() {
        try {
            this.jobTicketCache = createTicketCache();
        } catch (IOException e) {
            throw new SpException(e.getMessage(), e);
        }
    }

    @Override
    public void shutdown() {
        // noop
    }

    /**
     * Determines the ticket delivery date.
     *
     * @param submitDate
     *            The submit date
     * @param deliveryDateCandidate
     *            The delivery date candidate. Can be {@code null}.
     * @return The determined delivery date.
     */
    private Date getTicketDeliveryDate(final Date submitDate,
            final Date deliveryDateCandidate) {
        if (deliveryDateCandidate == null) {
            return submitDate;
        } else {
            return deliveryDateCandidate;
        }
    }

    @Override
    public OutboxJobDto createCopyJob(final User user,
            final ProxyPrintInboxReq request, final Date deliveryDate) {

        final UUID uuid = UUID.randomUUID();
        final Date submitDate = ServiceContext.getTransactionDate();

        final OutboxJobDto dto;

        try {
            dto = this.addJobticketToCache(user, null, uuid, request, null,
                    submitDate,
                    this.getTicketDeliveryDate(submitDate, deliveryDate));

            final String msgKey = "msg-user-print-jobticket-copy";

            request.setStatus(Status.WAITING_FOR_RELEASE);
            request.setUserMsgKey(msgKey);
            request.setUserMsg(localize(msgKey, dto.getTicketNumber()));

        } catch (IOException e) {
            throw new SpException(e.getMessage(), e);
        }
        return dto;
    }

    @Override
    public List<OutboxJobDto> proxyPrintInbox(final User lockedUser,
            final ProxyPrintInboxReq request, final Date deliveryDate)
            throws EcoPrintPdfTaskPendingException {

        final ProxyPrintInbox executor =
                new ProxyPrintInbox(this, deliveryDate);
        executor.print(lockedUser, request);

        final List<OutboxJobDto> tickets = executor.getTicketsCreated();

        if (tickets.isEmpty()) {
            throw new IllegalStateException("no job tickets created");
        }

        final String msgKey;
        final String msgTxt;

        if (tickets.size() == 1) {
            msgKey = "msg-user-print-jobticket-print";
            msgTxt = localize(msgKey, tickets.get(0).getTicketNumber());
        } else {
            final StringBuilder msg = new StringBuilder();
            for (final OutboxJobDto dto : tickets) {
                if (msg.length() > 0) {
                    msg.append(", ");
                }
                // Replace soft-hyphen by hard-hyphen, so ticket is displayed as
                // one.
                msg.append(dto.getTicketNumber().replace('-', '\u2011'));
            }
            msgKey = "msg-user-print-jobticket-print-multiple";
            msgTxt = localize(msgKey, msg.toString());
        }

        request.setUserMsgKey(msgKey);
        request.setUserMsg(msgTxt);

        return tickets;
    }

    @Override
    public List<OutboxJobDto> getTickets() {
        return filterTickets(null);
    }

    @Override
    public List<OutboxJobDto> getTickets(final Long userId) {
        return filterTickets(userId);
    }

    @Override
    public OutboxJobDto getTicket(final String fileName) {
        final UUID uuid = uuidFromFileName(fileName);
        return this.jobTicketCache.get(uuid);
    }

    @Override
    public OutboxJobDto getTicket(final Long userId, final String fileName) {
        final OutboxJobDto dto = this.getTicket(fileName);
        if (dto != null && dto.getUserId().equals(userId)) {
            return dto;
        }
        return null;
    }

    @Override
    public int cancelTickets(final Long userId) {
        int nRemoved = 0;
        for (final OutboxJobDto dto : filterTickets(userId)) {
            if (cancelTicket(dto.getFile()) != null) {
                nRemoved++;
            }
        }
        return nRemoved;
    }

    /**
     * Filters the pending Job Tickets.
     *
     * @param userId
     *            The {@link User} database key as filter, when {@code null}
     *            tickets from all users are selected.
     * @return The Job Tickets.
     */
    private List<OutboxJobDto> filterTickets(final Long userId) {
        final List<OutboxJobDto> tickets = new ArrayList<>();

        for (final Entry<UUID, OutboxJobDto> entry : this.jobTicketCache
                .entrySet()) {

            if (userId != null
                    && !entry.getValue().getUserId().equals(userId)) {
                continue;
            }

            /*
             * Create a new localized copy.
             */
            try {
                final OutboxJobDto dto = JsonHelper.read(OutboxJobDto.class,
                        getJobTicketFile(entry.getKey(), FILENAME_EXT_JSON));

                tickets.add(dto);

            } catch (IOException e) {

                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn(String.format("Job Ticket [%s] cannot be read.",
                            entry.getKey().toString()));
                }
            }
        }
        return tickets;
    }

    /**
     * Gets the UUID from one of the a job ticket files.
     *
     * @param fileName
     *            One of the files from the Job Ticket file pair.
     * @return The UUID.
     */
    private static UUID uuidFromFileName(final String fileName) {
        return UUID.fromString(FilenameUtils.getBaseName(fileName));
    }

    /**
     * Removes a Job Ticket.
     *
     * @param uuid
     *            The {@link UUID}.
     * @return The removed {@link OutboxJobDto}.
     */
    private OutboxJobDto removeTicket(final UUID uuid) {

        getJobTicketFile(uuid, FILENAME_EXT_JSON).delete();
        getJobTicketFile(uuid, FILENAME_EXT_PDF).delete();

        return this.jobTicketCache.remove(uuid);
    }

    /**
     * Removes a Job Ticket and notifies the {@link OutboxService} that job was
     * completed or canceled.
     *
     * @param uuid
     *            The {@link UUID}.
     * @param isCompleted
     *            {@code true} when ticket is completed, {@code false} when
     *            ticket is canceled.
     * @return The {@link OutboxJobDto}.
     */
    private OutboxJobDto removeTicket(final UUID uuid,
            final boolean isCompleted) {

        final OutboxJobDto job = this.removeTicket(uuid);

        if (job != null) {
            if (isCompleted) {
                outboxService().onOutboxJobCompleted(job);
            } else {
                outboxService().onOutboxJobCanceled(job);
            }
        }
        return job;
    }

    @Override
    public OutboxJobDto cancelTicket(final Long userId, final String fileName) {

        final UUID uuid = uuidFromFileName(fileName);
        final OutboxJobDto dto = this.jobTicketCache.get(uuid);

        if (dto == null) {
            return dto;
        }

        if (!dto.getUserId().equals(userId)) {
            throw new IllegalArgumentException(
                    String.format("Job ticket [%s] is not owned by user [%s]",
                            uuid.toString(), userId.toString()));
        }
        return this.removeTicket(uuid, false);
    }

    @Override
    public OutboxJobDto cancelTicket(final String fileName) {
        final UUID uuid = uuidFromFileName(fileName);
        return this.removeTicket(uuid, false);
    }

    @Override
    public PrintOut getTicketPrintOut(final String fileName)
            throws FileNotFoundException {

        final UUID uuid = uuidFromFileName(fileName);
        final OutboxJobDto dto = this.jobTicketCache.get(uuid);

        if (dto == null) {
            throw new FileNotFoundException(
                    String.format("Ticket %s not found.", fileName));
        }

        final Long printOutId = dto.getPrintOutId();

        if (printOutId == null) {
            return null;
        }

        final PrintOut printOut = printOutDAO().findById(printOutId);

        if (printOut == null) {
            throw new IllegalStateException(String.format(
                    "Ticket %s: PrintOut not found.", dto.getTicketNumber()));
        }

        return printOut;
    }

    @Override
    public Boolean cancelTicketPrint(final String fileName) {

        final PrintOut printOut;

        try {
            printOut = this.getTicketPrintOut(fileName);
        } catch (FileNotFoundException e1) {
            return null;
        }

        if (printOut == null) {
            throw new IllegalStateException(String
                    .format("Ticket %s: PrintOut ID not found.", fileName));
        }

        try {
            return Boolean
                    .valueOf(proxyPrintService().cancelPrintJob(printOut));
        } catch (IppConnectException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public OutboxJobDto closeTicketPrint(final String fileName) {

        final PrintOut printOut;

        try {
            printOut = this.getTicketPrintOut(fileName);
        } catch (FileNotFoundException e) {
            return null;
        }

        final UUID uuid = uuidFromFileName(fileName);

        final OutboxJobDto dto = this.removeTicket(uuid, true);

        if (dto == null) {
            return null;
        }

        dto.setIppJobState(
                IppJobStateEnum.asEnum(printOut.getCupsJobState().intValue()));

        return dto;
    }

    @Override
    public boolean updateTicket(final OutboxJobDto dto) throws IOException {

        final UUID uuid = uuidFromFileName(dto.getFile());

        if (!this.jobTicketCache.containsKey(uuid)) {
            return false;
        }

        this.jobTicketCache.put(uuid, dto);

        final File jsonFileTicket = getJobTicketFile(uuid, FILENAME_EXT_JSON);

        Writer writer = null;
        try {
            writer = new FileWriter(jsonFileTicket);
            JsonHelper.write(dto, writer);
            writer.close();
        } finally {
            IOUtils.closeQuietly(writer);
        }
        return true;
    }

    @Override
    public String notifyTicketCompletedByEmail(final OutboxJobBaseDto dto,
            final String operator, final User user, final Locale locale) {

        final EmailMsgParms emailParms = new EmailMsgParms();

        final String emailAddr = userService().getPrimaryEmailAddress(user);

        if (emailAddr == null) {
            LOGGER.warn(String.format(
                    "No primary email address found for user [%s]",
                    user.getUserId()));
            return null;
        }

        emailParms.setToAddress(emailAddr);
        emailParms.setSubject(
                localize(locale, "email-user-jobticket-completed-subject",
                        dto.getTicketNumber()));
        emailParms
                .setBody(localize(locale, "email-user-jobticket-completed-body",
                        user.getFullName(), operator));
        try {
            emailService().sendEmail(emailParms);
        } catch (InterruptedException | CircuitBreakerException
                | MessagingException | IOException e) {
            throw new SpException(e.getMessage());
        }

        return emailAddr;
    }

    /**
     * Executes a Job Ticket request.
     * <p>
     * </p>
     *
     * @param operator
     *            The {@link User#getUserId()} with
     *            {@link ACLRoleEnum#JOB_TICKET_OPERATOR}.
     * @param printer
     *            The redirect printer.
     * @param ippMediaSource
     *            The {@link IppDictJobTemplateAttr#ATTR_MEDIA_SOURCE} value for
     *            the print job. Is irrelevant ({@code null}) when settleOnly.
     * @param fileName
     *            The unique PDF file name of the job to print.
     * @param settleOnly
     *            {@code true} when ticket must be settled only, {@code false}
     *            when ticket job must be proxy printed.
     * @return The printed ticket or {@code null} when ticket was not found.
     * @throws IOException
     *             When IO error.
     * @throws IppConnectException
     *             When connection to CUPS fails.
     * @throws IllegalStateException
     *             For a print job (no settlement) with ippMediaSource is
     *             {@code null}: when "media" option is not specified in Job
     *             Ticket, or printer has no "auto" choice for "media-source".
     */
    private OutboxJobDto execTicket(final String operator,
            final Printer printer, final String ippMediaSource,
            final String fileName, final boolean settleOnly)
            throws IOException, IppConnectException {

        final UUID uuid = uuidFromFileName(fileName);
        final OutboxJobDto dto = this.jobTicketCache.get(uuid);

        if (dto == null) {
            return dto;
        }

        /*
         * Replace with redirect printer.
         */
        dto.setPrinterName(printer.getPrinterName());

        final ThirdPartyEnum extPrinterManager;

        if (paperCutService().isExtPaperCutPrint(dto.getPrinterName())) {
            extPrinterManager = ThirdPartyEnum.PAPERCUT;
        } else {
            extPrinterManager = null;
        }

        final User lockedUser = userDAO().lock(dto.getUserId());

        final OutboxJobDto dtoReturn;

        if (settleOnly) {

            proxyPrintService().settleJobTicket(operator, lockedUser, dto,
                    getJobTicketFile(uuid, FILENAME_EXT_JSON),
                    extPrinterManager);

            dtoReturn = this.removeTicket(uuid, true);

            if (ConfigManager.instance().isConfigValue(
                    Key.JOBTICKET_NOTIFY_EMAIL_COMPLETED_ENABLE)) {
                notifyTicketCompletedByEmail(dto, operator, lockedUser,
                        ConfigManager.getDefaultLocale());
            }

        } else {

            if (ippMediaSource == null) {
                throw new IllegalStateException(String.format(
                        "Job Ticket %s to Printer %s: %s missing.",
                        dto.getTicketNumber(), printer.getPrinterName(),
                        IppDictJobTemplateAttr.ATTR_MEDIA_SOURCE));
            }

            /*
             * Set (overwrite) media-source.
             */
            dto.getOptionValues().put(IppDictJobTemplateAttr.ATTR_MEDIA_SOURCE,
                    ippMediaSource);

            final PrintOut printOut =
                    proxyPrintService()
                            .proxyPrintJobTicket(operator, lockedUser, dto,
                                    getJobTicketFile(uuid, FILENAME_EXT_PDF),
                                    extPrinterManager)
                            .getDocOut().getPrintOut();

            dto.setPrintOutId(printOut.getId());
            this.updateTicket(dto);
            dtoReturn = dto;
        }

        return dtoReturn;
    }

    @Override
    public OutboxJobDto printTicket(final String operator,
            final Printer printer, final String ippMediaSource,
            final String fileName) throws IOException, IppConnectException {
        return execTicket(operator, printer, ippMediaSource, fileName, false);
    }

    @Override
    public OutboxJobDto settleTicket(final String operator,
            final Printer printer, final String fileName) throws IOException {

        try {
            return execTicket(operator, printer, null, fileName, true);
        } catch (IppConnectException e) {
            // This is not supposed to happen, because no proxy print is done.
            throw new IllegalStateException(e.getMessage());
        }
    }

    /**
     * Checks if capability of printer matches the IPP option value requested.
     *
     * @param optionsRequested
     *            The requested {@link IppOptionMap}.
     * @param ippOptionKey
     *            The IPP option key.
     * @param ippOptionValueNone
     *            The IPP option value representing a "none" choice.
     * @param printerOptionLookup
     *            The lookup of printer IPP options.
     * @return {@code true} if job does not request the IPP option, or when
     *         printer has capability to fulfill job IPP option value.
     */
    private static boolean isPrinterCapabilityMatch(
            final IppOptionMap optionsRequested, final String ippOptionKey,
            final String ippOptionValueNone,
            final Map<String, JsonProxyPrinterOpt> printerOptionLookup) {

        final String jobOptionValue =
                optionsRequested.getOptionValue(ippOptionKey);

        if (jobOptionValue == null) {
            // Option not requested: match.
            return true;
        }

        if (optionsRequested.isOptionPresent(ippOptionKey,
                ippOptionValueNone)) {
            // non-choice option: match.
            return true;
        }

        final JsonProxyPrinterOpt printerOptionValue =
                printerOptionLookup.get(ippOptionKey);

        if (printerOptionValue == null) {
            // Printer does not have option: no match.
            return false;
        }

        for (final JsonProxyPrinterOptChoice choice : printerOptionValue
                .getChoices()) {
            if (choice.getChoice().equals(jobOptionValue)) {
                // Printer has option value: match.
                return true;
            }
        }
        // Printer does not have option value: no match.
        return false;
    }

    @Override
    public List<RedirectPrinterDto> getRedirectPrinters(final String fileName,
            final IppOptionMap ippOptionFilter) {

        final OutboxJobDto job = this.getTicket(fileName);

        if (job == null) {
            return null;
        }

        final Printer jobTicketPrinter =
                printerDAO().findByName(job.getPrinterName());

        final List<RedirectPrinterDto> printerList = new ArrayList<>();

        final String groupName = printerService().getAttributeValue(
                jobTicketPrinter, PrinterAttrEnum.JOBTICKET_PRINTER_GROUP);

        if (StringUtils.isBlank(groupName)) {
            return printerList;
        }

        final PrinterGroup printerGroup = ServiceContext.getDaoContext()
                .getPrinterGroupDao().findByName(groupName);

        if (printerGroup == null) {
            return printerList;
        }

        final IppOptionMap optionMap = job.createIppOptionMap();

        final boolean colorJob = optionMap.isColorJob();
        final boolean duplexJob = optionMap.isDuplexJob();

        int iPreferred = -1;
        int iPrinter = 0;

        // Check compatibility.
        final String requestedMediaForJob =
                job.getOptionValues().get(IppDictJobTemplateAttr.ATTR_MEDIA);

        for (final PrinterGroupMember member : printerGroup.getMembers()) {

            final Printer printer = member.getPrinter();

            if (printer.getDisabled().booleanValue()) {
                continue;
            }

            final JsonProxyPrinter cupsPrinter = proxyPrintService()
                    .getCachedPrinter(printer.getPrinterName());

            if (cupsPrinter == null) {
                throw new IllegalStateException(
                        String.format("Printer [%s] not found in cache.",
                                printer.getPrinterName()));
            }

            // Duplex
            if (duplexJob && !cupsPrinter.getDuplexDevice()) {
                continue;
            }

            // Color
            final boolean colorPrinter = cupsPrinter.getColorDevice();

            if (colorJob && !colorPrinter) {
                continue;
            }

            if (iPreferred < 0) {
                if (colorJob) {
                    if (colorPrinter) {
                        iPreferred = iPrinter;
                    }
                } else if (!colorPrinter) {
                    iPreferred = iPrinter;
                }
            }

            final Map<String, JsonProxyPrinterOpt> printerOptionlookup =
                    cupsPrinter.getOptionsLookup();

            // Finishings
            final String[][] finishingOptionsToCheck =
                    IppDictJobTemplateAttr.ORG_SAVAPAGE_ATTR_FINISHINGS_V_NONE;

            final IppOptionMap optionsRequested = job.createIppOptionMap();

            boolean isMatch = true;

            for (final String[] ippOptionCheck : finishingOptionsToCheck) {

                if (!isPrinterCapabilityMatch(optionsRequested,
                        ippOptionCheck[0], ippOptionCheck[1],
                        printerOptionlookup)) {
                    isMatch = false;
                    break;
                }
            }

            if (!isMatch) {
                continue;
            }

            // Extra filter
            if (!ippOptionFilter.areOptionValuesValid(printerOptionlookup)) {
                continue;
            }

            // Find the media-source
            final JsonProxyPrinterOpt mediaSource = printerOptionlookup
                    .get(IppDictJobTemplateAttr.ATTR_MEDIA_SOURCE);

            if (mediaSource == null) {
                LOGGER.warn(String.format("[%s] not found for printer [%s]",
                        IppDictJobTemplateAttr.ATTR_MEDIA_SOURCE,
                        printer.getPrinterName()));
                continue;
            }

            //
            final RedirectPrinterDto redirectPrinter = new RedirectPrinterDto();

            redirectPrinter.setId(printer.getId());
            redirectPrinter.setName(printer.getDisplayName());
            redirectPrinter.setDeviceUri(cupsPrinter.getDeviceUri().toString());
            redirectPrinter.setMediaSourceOpt(mediaSource);

            final PrinterAttrLookup printerAttrLookup =
                    new PrinterAttrLookup(printer);

            redirectPrinter.setMediaSourceOptChoice(
                    printerService().findMediaSourceForMedia(printerAttrLookup,
                            mediaSource, requestedMediaForJob));

            printerList.add(redirectPrinter);
            iPrinter++;
        }

        if (!printerList.isEmpty() && iPreferred >= 0) {
            printerList.get(iPreferred).setPreferred(true);
        }

        return printerList;
    }

    @Override
    public RedirectPrinterDto getRedirectPrinter(final String fileName,
            final IppOptionMap optionFilter) {

        final List<RedirectPrinterDto> printers =
                this.getRedirectPrinters(fileName, optionFilter);

        if (printers == null || printers.isEmpty()) {
            return null;
        }

        final int index;

        if (printers.size() == 1) {
            index = 0;
        } else {
            index = new Random().nextInt(printers.size());
        }
        return printers.get(index);
    }

    @Override
    public String createTicketNumber() {
        return chunkFormatted(
                shuffle(Long.toHexString(DateUtil.uniqueCurrentTime())),
                TICKER_NUMBER_CHUNK_WIDTH, TICKER_NUMBER_CHUNK_SEPARATOR)
                        .toUpperCase();
    }

    /**
     * Shuffles characters in a string.
     *
     * @param input
     *            The string to shuffle.
     * @return The shuffled {@link StringBuilder} result.
     */
    private static StringBuilder shuffle(final String input) {

        final List<Character> characters = new ArrayList<Character>();

        for (final char c : input.toCharArray()) {
            characters.add(c);
        }

        final StringBuilder shuffled = new StringBuilder(input.length());

        while (characters.size() != 0) {
            final int randPicker = (int) (Math.random() * characters.size());
            shuffled.append(characters.remove(randPicker));
        }
        return shuffled;
    }

    /**
     * Formats a string by inserting a separator between substring chunks.
     *
     * @param str
     *            The {@link StringBuilder} to format.
     * @param chunkWidth
     *            The character width of the substring chunks.
     * @param chunkSeparator
     *            The separator between chunks.
     * @return The formatted string.
     */
    private static String chunkFormatted(final StringBuilder str,
            final int chunkWidth, final String chunkSeparator) {

        int idx = str.length() - chunkWidth;

        while (idx > 0) {
            str.insert(idx, chunkSeparator);
            idx = idx - chunkWidth;
        }
        return str.toString();
    }

}
