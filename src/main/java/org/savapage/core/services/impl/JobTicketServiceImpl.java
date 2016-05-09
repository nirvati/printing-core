/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2016 Datraverse B.V.
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

import static java.nio.file.FileVisitResult.CONTINUE;

import java.io.File;
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
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.savapage.core.SpException;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.doc.DocContent;
import org.savapage.core.dto.RedirectPrinterDto;
import org.savapage.core.imaging.EcoPrintPdfTaskPendingException;
import org.savapage.core.ipp.client.IppConnectException;
import org.savapage.core.jpa.Printer;
import org.savapage.core.jpa.PrinterGroup;
import org.savapage.core.jpa.PrinterGroupMember;
import org.savapage.core.jpa.User;
import org.savapage.core.outbox.OutboxInfoDto.OutboxJobDto;
import org.savapage.core.print.proxy.AbstractProxyPrintReq;
import org.savapage.core.print.proxy.AbstractProxyPrintReq.Status;
import org.savapage.core.print.proxy.JsonProxyPrinter;
import org.savapage.core.print.proxy.ProxyPrintDocReq;
import org.savapage.core.print.proxy.ProxyPrintInboxReq;
import org.savapage.core.services.JobTicketService;
import org.savapage.core.services.OutboxService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.helpers.DocContentPrintInInfo;
import org.savapage.core.services.helpers.ProxyPrintInboxPattern;
import org.savapage.core.services.helpers.ThirdPartyEnum;
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
         *
         * @param service
         *            The parent service.
         * @param deliveryDate
         *            The requested date of delivery.
         */
        ProxyPrintInbox(final JobTicketServiceImpl service,
                final Date deliveryDate) {
            this.serviceImpl = service;
            this.submitDate = ServiceContext.getTransactionDate();
            this.deliveryDate = deliveryDate;
        }

        @Override
        protected void onInit(final User lockedUser,
                final ProxyPrintInboxReq request) {
        }

        @Override
        protected void onExit(final User lockedUser,
                final ProxyPrintInboxReq request) {

            final String msgKey = "msg-user-print-jobticket";

            request.setStatus(Status.WAITING_FOR_RELEASE);
            request.setUserMsgKey(msgKey);
            request.setUserMsg(serviceImpl.localize(msgKey));
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
                final File pdfGenerated) {

            final UUID uuid = UUID.fromString(
                    FilenameUtils.getBaseName(pdfGenerated.getName()));

            try {
                this.serviceImpl.addJobticketToCache(lockedUser, pdfGenerated,
                        uuid, request, uuidPageCount, this.submitDate,
                        this.deliveryDate);
            } catch (IOException e) {
                throw new SpException(e.getMessage(), e);
            }

        }
    }

    @Override
    public void proxyPrintPdf(final User lockedUser,
            final ProxyPrintDocReq request, final File pdfFile,
            final DocContentPrintInInfo printInfo, final Date deliveryDate)
            throws IOException {

        final String msgKey = "msg-user-print-jobticket";

        request.setStatus(Status.WAITING_FOR_RELEASE);
        request.setUserMsgKey(msgKey);
        request.setUserMsg(localize(msgKey));

        final UUID uuid = UUID.fromString(request.getDocumentUuid());

        /*
         * Move PDF file to ticket Outbox.
         */
        final File pdfFileTicket = getJobTicketFile(uuid, FILENAME_EXT_PDF);
        FileUtils.moveFile(pdfFile, pdfFileTicket);

        /*
         * Add to cache.
         */
        final LinkedHashMap<String, Integer> uuidPageCount =
                new LinkedHashMap<>();
        uuidPageCount.put(uuid.toString(), request.getNumberOfPages());

        this.addJobticketToCache(lockedUser, pdfFileTicket, uuid, request,
                uuidPageCount, ServiceContext.getTransactionDate(),
                deliveryDate);
    }

    /**
     * Creates sibling JSON file with proxy print information and adds Job
     * Ticket to the cache.
     *
     * @param user
     *            The requesting {@link User}.
     * @param pdfFileTicket
     *            The PDF file to be printed by the Job Ticket.
     * @param uuid
     *            The Job Ticket {@link UUID}.
     * @param request
     *            The {@link AbstractProxyPrintReq}.
     * @param uuidPageCount
     *            Object filled with the number of selected pages per input file
     *            UUID.
     * @param submitDate
     *            The submit date.
     * @param deliveryDate
     *            The requested date of delivery.
     * @throws IOException
     *             When file IO error occurs.
     */
    private void addJobticketToCache(final User user, final File pdfFileTicket,
            final UUID uuid, final AbstractProxyPrintReq request,
            final LinkedHashMap<String, Integer> uuidPageCount,
            final Date submitDate, final Date deliveryDate) throws IOException {

        final OutboxJobDto dto = outboxService().createOutboxJob(request,
                submitDate, deliveryDate, pdfFileTicket, uuidPageCount);

        dto.setUserId(user.getId());

        final File jsonFileTicket = getJobTicketFile(uuid, FILENAME_EXT_JSON);

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

    @Override
    public void proxyPrintInbox(final User lockedUser,
            final ProxyPrintInboxReq request, final Date deliveryDate)
            throws EcoPrintPdfTaskPendingException {
        new ProxyPrintInbox(this, deliveryDate).print(lockedUser, request);
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
     * @param isCanceled
     *            {@code true} when ticket is canceled, {@code false} when
     *            ticket is completed.
     * @return The {@link OutboxJobDto}.
     */
    private OutboxJobDto removeTicket(final UUID uuid,
            final boolean isCanceled) {

        final OutboxJobDto job = this.removeTicket(uuid);

        if (job != null) {
            if (isCanceled) {
                outboxService().onOutboxJobCanceled(job);
            } else {
                outboxService().onOutboxJobCompleted(job);
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

        return this.removeTicket(uuid, true);
    }

    @Override
    public OutboxJobDto cancelTicket(final String fileName) {
        final UUID uuid = uuidFromFileName(fileName);
        return this.removeTicket(uuid, true);
    }

    @Override
    public OutboxJobDto printTicket(final Printer printer,
            final String fileName) throws IOException, IppConnectException {

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

        if (outboxService().isMonitorPaperCutPrintStatus(dto)) {
            extPrinterManager = ThirdPartyEnum.PAPERCUT;
        } else {
            extPrinterManager = null;
        }

        final User lockedUser = userDAO().lock(dto.getUserId());

        proxyPrintService().proxyPrintJobTicket(lockedUser, dto,
                getJobTicketFile(uuid, FILENAME_EXT_PDF), extPrinterManager);

        if (extPrinterManager == null) {
            return this.removeTicket(uuid, false);
        }

        /*
         * Just remove the ticket, do NOT notify. Notification is done in
         * PaperCut Monitoring.
         */
        return this.removeTicket(uuid);
    }

    @Override
    public List<RedirectPrinterDto> getRedirectPrinters(final String fileName) {

        final OutboxJobDto job = this.getTicket(fileName);

        if (job == null) {
            return null;
        }

        final List<RedirectPrinterDto> printerList = new ArrayList<>();

        final String groupName = ConfigManager.instance()
                .getConfigValue(Key.JOBTICKET_PROXY_PRINTER_GROUP);

        if (StringUtils.isBlank(groupName)) {
            return printerList;
        }

        final PrinterGroup printerGroup = ServiceContext.getDaoContext()
                .getPrinterGroupDao().findByName(groupName);

        if (printerGroup == null) {
            return printerList;
        }

        final boolean colorJob = job.isColorJob();
        final boolean duplexJob = job.isDuplexJob();

        int iPreferred = -1;
        int iPrinter = 0;

        for (final PrinterGroupMember member : printerGroup.getMembers()) {

            final Printer printer = member.getPrinter();

            final JsonProxyPrinter cupsPrinter = proxyPrintService()
                    .getCachedPrinter(printer.getPrinterName());

            if (cupsPrinter == null) {
                throw new IllegalStateException(
                        String.format("Printer [%s] not found in cache.",
                                printer.getPrinterName()));
            }

            /*
             * Check compatibility.
             */
            if (duplexJob && !cupsPrinter.getDuplexDevice()) {
                continue;
            }

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

            final RedirectPrinterDto redirectPrinter = new RedirectPrinterDto();
            printerList.add(redirectPrinter);

            redirectPrinter.setId(printer.getId());
            redirectPrinter.setName(printer.getDisplayName());

            iPrinter++;
        }

        if (!printerList.isEmpty() && iPreferred >= 0) {
            printerList.get(iPreferred).setPreferred(true);
        }

        return printerList;
    }

    @Override
    public RedirectPrinterDto getRedirectPrinter(final String fileName) {

        final List<RedirectPrinterDto> printers =
                this.getRedirectPrinters(fileName);

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

}
