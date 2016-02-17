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
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.savapage.core.SpException;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.doc.DocContent;
import org.savapage.core.imaging.EcoPrintPdfTaskPendingException;
import org.savapage.core.jpa.User;
import org.savapage.core.outbox.OutboxInfoDto.OutboxJobDto;
import org.savapage.core.print.proxy.AbstractProxyPrintReq.Status;
import org.savapage.core.print.proxy.ProxyPrintInboxReq;
import org.savapage.core.services.JobTicketService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.helpers.ProxyPrintInboxPattern;
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
        private Date submitDate;

        /**
         *
         * @param service
         *            The parent service.
         */
        public ProxyPrintInbox(final JobTicketServiceImpl service) {
            this.serviceImpl = service;
        }

        @Override
        protected void onInit(final User lockedUser,
                final ProxyPrintInboxReq request) {
            this.submitDate = ServiceContext.getTransactionDate();
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

            // TODO
            final Date expiryDate = DateUtils.addHours(new Date(), 1);

            /*
             * Create sibling json file with proxy print information.
             */
            final UUID uuid = UUID.fromString(
                    FilenameUtils.getBaseName(pdfGenerated.getName()));

            final File jsonFile = getJobTicketFile(uuid, FILENAME_EXT_JSON);

            final OutboxJobDto dto = outboxService().createOutboxJob(request,
                    this.submitDate, expiryDate, pdfGenerated, uuidPageCount);

            dto.setUserId(lockedUser.getId());

            Writer writer = null;
            try {
                writer = new FileWriter(jsonFile);
                JsonHelper.write(dto, writer);
                writer.close();
            } catch (IOException e) {
                throw new SpException(e.getMessage(), e);
            } finally {
                IOUtils.closeQuietly(writer);
            }

            /*
             * Add to cache.
             */
            this.serviceImpl.jobTicketCache.put(uuid, dto);
        }
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
            final ProxyPrintInboxReq request)
                    throws EcoPrintPdfTaskPendingException {
        new ProxyPrintInbox(this).print(lockedUser, request);
    }

    @Override
    public List<OutboxJobDto> getTickets() {
        return filterTickets(null);
    }

    @Override
    public List<OutboxJobDto> getTickets(final Long userId) {
        return filterTickets(userId);
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

    @Override
    public boolean removeTicket(final Long userId, final String fileName) {

        final UUID uuid = uuidFromFileName(fileName);
        final OutboxJobDto dto = this.jobTicketCache.get(uuid);

        if (dto == null) {
            return false;
        }

        if (!dto.getUserId().equals(userId)) {
            throw new SpException(
                    String.format("Job ticket [%s] is not owned by user [%s]",
                            uuid.toString(), userId.toString()));
        }

        getJobTicketFile(uuid, FILENAME_EXT_JSON).delete();
        getJobTicketFile(uuid, FILENAME_EXT_PDF).delete();

        return this.jobTicketCache.remove(uuid) != null;
    }

}
