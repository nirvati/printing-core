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
package org.savapage.core.services.impl;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.doc.DocContent;
import org.savapage.core.doc.store.DocStoreBranchEnum;
import org.savapage.core.doc.store.DocStoreCleaner;
import org.savapage.core.doc.store.DocStoreConfig;
import org.savapage.core.doc.store.DocStoreException;
import org.savapage.core.doc.store.DocStoreTypeEnum;
import org.savapage.core.inbox.PrintInInfoDto;
import org.savapage.core.job.RunModeSwitch;
import org.savapage.core.jpa.DocLog;
import org.savapage.core.json.JsonAbstractBase;
import org.savapage.core.outbox.OutboxInfoDto.OutboxJobDto;
import org.savapage.core.pdf.PdfCreateInfo;
import org.savapage.core.print.proxy.AbstractProxyPrintReq;
import org.savapage.core.services.DocStoreService;
import org.savapage.core.services.helpers.DocContentPrintInInfo;
import org.savapage.core.util.JsonHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class DocStoreServiceImpl extends AbstractService
        implements DocStoreService {

    /** */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(DocStoreServiceImpl.class);

    /** */
    private static final String FILENAME_EXT_JSON = "json";

    /** */
    private Path homePathArchive;

    /** */
    private Path homePathJournal;

    /**
     * Creates UTC calendar instance from date.
     *
     * @param date
     *            The date.
     * @return The calendar.
     */
    private static Calendar createCalendarTime(final Date date) {
        final Calendar cal =
                Calendar.getInstance(TimeZone.getTimeZone(ZoneId.of("UTC")));
        cal.setTime(date);
        return cal;
    }

    /**
     * Gets the unique storage path for a document.
     *
     * @param store
     *            The store.
     * @param branch
     *            Branch in store.
     * @param docLog
     *            The document log.
     * @return The store path for this document.
     */
    private Path getStorePath(final DocStoreTypeEnum store,
            final DocStoreBranchEnum branch, final DocLog docLog) {
        return this.getStorePath(store, branch, docLog.getCreatedDate(),
                docLog.getUuid());
    }

    /**
     * Gets the unique storage path for a document.
     *
     * @param store
     *            The store.
     * @param branch
     *            Branch in store.
     * @param createDate
     *            Date of creation.
     * @param uuid
     *            {@link UUID} as string.
     * @return The store path for this document.
     */
    private Path getStorePath(final DocStoreTypeEnum store,
            final DocStoreBranchEnum branch, final Date createDate,
            final String uuid) {

        final Calendar cal = createCalendarTime(createDate);

        return Paths.get(this.getStoreBranch(store, branch).toString(),
                String.format("%04d%c%02d%c%02d%c%02d%c%s",
                        cal.get(Calendar.YEAR), File.separatorChar,
                        cal.get(Calendar.MONTH) + 1, File.separatorChar,
                        cal.get(Calendar.DAY_OF_MONTH), File.separatorChar,
                        cal.get(Calendar.HOUR_OF_DAY), File.separatorChar,
                        uuid));
    }

    /**
     * Gets the store path of a branch.
     *
     * @param store
     *            The store.
     * @param branch
     *            Branch in store.
     * @return The branch path.
     */
    private Path getStoreBranch(final DocStoreTypeEnum store,
            final DocStoreBranchEnum branch) {

        final Path path;

        switch (store) {
        case ARCHIVE:
            path = this.homePathArchive;
            break;
        case JOURNAL:
            path = this.homePathJournal;
            break;
        default:
            throw new UnknownError(store.toString());
        }
        return Paths.get(path.toString(), branch.getBranch().toString());
    }

    @Override
    public DocStoreTypeEnum getMainStore(final DocStoreBranchEnum branch) {
        DocStoreTypeEnum store = DocStoreTypeEnum.ARCHIVE;
        if (!this.isEnabled(store, branch)) {
            store = DocStoreTypeEnum.JOURNAL;
            if (!this.isEnabled(store, branch)) {
                store = null;
            }
        }
        return store;
    }

    @Override
    public boolean isEnabled(final DocStoreTypeEnum store,
            final DocStoreBranchEnum branch) {
        return getConfig(store, branch).isEnabled();
    }

    @Override
    public DocStoreConfig getConfig(final DocStoreTypeEnum store,
            final DocStoreBranchEnum branch) {

        final ConfigManager cm = ConfigManager.instance();

        Key key = null;
        final boolean enabled = cm.isConfigValue(Key.DOC_STORE_ENABLE);
        final boolean enabledStore;
        final boolean enabledBranch;

        switch (store) {
        case ARCHIVE:
            enabledStore =
                    enabled && cm.isConfigValue(Key.DOC_STORE_ARCHIVE_ENABLE);
            switch (branch) {
            case IN_PRINT:
                enabledBranch = enabledStore
                        && cm.isConfigValue(Key.DOC_STORE_ARCHIVE_IN_ENABLE);
                key = Key.DOC_STORE_ARCHIVE_IN_PRINT_DAYS_TO_KEEP;
                break;
            case OUT_PDF:
                enabledBranch = enabledStore
                        && cm.isConfigValue(Key.DOC_STORE_ARCHIVE_OUT_ENABLE)
                        && cm.isConfigValue(
                                Key.DOC_STORE_ARCHIVE_OUT_PDF_ENABLE);
                key = Key.DOC_STORE_ARCHIVE_OUT_PDF_DAYS_TO_KEEP;
                break;
            case OUT_PRINT:
                enabledBranch = enabledStore
                        && cm.isConfigValue(Key.DOC_STORE_ARCHIVE_OUT_ENABLE)
                        && cm.isConfigValue(
                                Key.DOC_STORE_ARCHIVE_OUT_PRINT_ENABLE);
                key = Key.DOC_STORE_ARCHIVE_OUT_PRINT_DAYS_TO_KEEP;
                break;
            default:
                enabledBranch = false;
                break;
            }
            break;

        case JOURNAL:
            enabledStore =
                    enabled && cm.isConfigValue(Key.DOC_STORE_JOURNAL_ENABLE);
            switch (branch) {
            case IN_PRINT:
                enabledBranch = enabledStore
                        && cm.isConfigValue(Key.DOC_STORE_JOURNAL_IN_ENABLE);
                key = Key.DOC_STORE_JOURNAL_IN_PRINT_DAYS_TO_KEEP;
                break;
            case OUT_PDF:
                enabledBranch = enabledStore
                        && cm.isConfigValue(Key.DOC_STORE_JOURNAL_OUT_ENABLE)
                        && cm.isConfigValue(
                                Key.DOC_STORE_JOURNAL_OUT_PDF_ENABLE);
                key = Key.DOC_STORE_JOURNAL_OUT_PDF_DAYS_TO_KEEP;
                break;
            case OUT_PRINT:
                enabledBranch = enabledStore
                        && cm.isConfigValue(Key.DOC_STORE_JOURNAL_OUT_ENABLE)
                        && cm.isConfigValue(
                                Key.DOC_STORE_JOURNAL_OUT_PRINT_ENABLE);
                key = Key.DOC_STORE_JOURNAL_OUT_PRINT_DAYS_TO_KEEP;
                break;
            default:
                enabledBranch = false;
                break;
            }
            break;

        default:
            enabledBranch = false;
            break;
        }

        if (key == null) {
            throw new UnknownError("Unhandled store/branch");
        }

        return new DocStoreConfig(store, branch, enabledBranch,
                ConfigManager.instance().getConfigInt(key));
    }

    @Override
    public boolean isDocPresent(final DocStoreTypeEnum store,
            final DocStoreBranchEnum branch, final DocLog docLog) {
        return this.getStorePath(store, branch, docLog).toFile().exists();
    }

    private DocStoreBranchEnum getStoreBranch(final DocLog docLog)
            throws DocStoreException {
        final DocStoreBranchEnum branch;

        if (docLog.getDocIn() != null) {
            if (docLog.getDocIn().getPrintIn() != null) {
                branch = DocStoreBranchEnum.IN_PRINT;
            } else {
                branch = null;
            }
        } else if (docLog.getDocOut() != null) {
            if (docLog.getDocOut().getPdfOut() != null) {
                branch = DocStoreBranchEnum.OUT_PDF;
            } else if (docLog.getDocOut().getPrintOut() != null) {
                branch = DocStoreBranchEnum.OUT_PRINT;
            } else {
                branch = null;
            }
        } else {
            branch = null;
        }
        if (branch == null) {
            throw new DocStoreException("No Store Branch found.");
        }
        return branch;
    }

    /**
     * Gets the path of stored PDF.
     *
     * @param dir
     *            Directory containing the PDF
     * @param uuid
     *            The UUID
     * @return The PDF file path.
     */
    private static Path getStoredPdf(final Path dir, final String uuid) {
        return Paths.get(dir.toString(),
                String.format("%s.%s", uuid, DocContent.FILENAME_EXT_PDF));
    }

    /**
     * Gets the path of stored JSON.
     *
     * @param dir
     *            Directory containing the JSON
     * @param uuid
     *            The UUID
     * @return The JSON file path.
     */
    private static Path getStoredJson(final Path dir, final String uuid) {
        return Paths.get(dir.toString(),
                String.format("%s.%s", uuid, FILENAME_EXT_JSON));
    }

    @Override
    public void start() {
        this.homePathArchive =
                ConfigManager.getDocStoreHome(DocStoreTypeEnum.ARCHIVE);
        this.homePathJournal =
                ConfigManager.getDocStoreHome(DocStoreTypeEnum.JOURNAL);
    }

    @Override
    public void shutdown() {
        // no code intended.
    }

    @Override
    public File retrievePdf(final DocStoreTypeEnum store, final DocLog docLog)
            throws DocStoreException {

        final Path dir =
                this.getStorePath(store, getStoreBranch(docLog), docLog);
        if (!dir.toFile().exists()) {
            throw new DocStoreException("No storage found.");
        }

        final Path file = getStoredPdf(dir, docLog.getUuid());
        if (!file.toFile().exists()) {
            throw new DocStoreException("No PDF found.");
        }

        return file.toFile();
    }

    @Override
    public OutboxJobDto retrieveJob(final DocStoreTypeEnum store,
            final DocLog docLog) throws DocStoreException, IOException {

        final Path dir =
                this.getStorePath(store, getStoreBranch(docLog), docLog);
        if (!dir.toFile().exists()) {
            throw new DocStoreException("No storage found.");
        }

        final Path file = getStoredJson(dir, docLog.getUuid());
        if (!file.toFile().exists()) {
            throw new DocStoreException("No JSON found.");
        }

        return JsonHelper.read(OutboxJobDto.class, file.toFile());
    }

    @Override
    public void store(final DocStoreTypeEnum store,
            final AbstractProxyPrintReq request, final DocLog docLog,
            final PdfCreateInfo createInfo) throws DocStoreException {

        final OutboxJobDto pojo = outboxService().createOutboxJob(request,
                docLog.getCreatedDate(), docLog.getCreatedDate(), createInfo);

        /*
         * Userid is not set in some cases. Therefore, explicitly set userid.
         */
        pojo.setUserId(request.getIdUser());

        if (pojo.isJobTicket()) {
            pojo.setPrinter(request.getTicketPrinterName());
            pojo.setPrinterRedirect(request.getPrinterName());
        }

        this.store(store, DocStoreBranchEnum.OUT_PRINT, pojo, docLog,
                createInfo);
    }

    @Override
    public void store(final DocStoreTypeEnum store, final OutboxJobDto job,
            final DocLog docLog, final File pdfFile) throws DocStoreException {

        final PdfCreateInfo createInfo;
        if (pdfFile == null) {
            createInfo = null;
        } else {
            createInfo = new PdfCreateInfo(pdfFile);
        }
        this.store(store, DocStoreBranchEnum.OUT_PRINT, job, docLog,
                createInfo);
    }

    @Override
    public void store(final DocStoreTypeEnum store,
            final DocContentPrintInInfo info, final File pdfFile)
            throws DocStoreException {

        final DocStoreBranchEnum branch = DocStoreBranchEnum.IN_PRINT;
        final String uuid = info.getUuidJob().toString();

        this.store(store, DocStoreBranchEnum.IN_PRINT,
                PrintInInfoDto.create(info), info.getPrintInDate(), uuid,
                new PdfCreateInfo(pdfFile));

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Stored {} [{}] in {}/{}", uuid, info.getJobName(),
                    store.toString(), branch.toString());
        }
    }

    /**
     * Stores a document.
     *
     * @param store
     *            The store.
     * @param branch
     *            Branch in store.
     * @param pojo
     *            POJO to store.
     * @param docLog
     *            The {@link DocLog} persisted in the database.
     * @param createInfo
     *            The {@link PdfCreateInfo} with the PDF file. Is {@code null}
     *            for Copy Job Ticket.
     * @throws DocStoreException
     *             When IO errors.
     */
    private void store(final DocStoreTypeEnum store,
            final DocStoreBranchEnum branch, final JsonAbstractBase pojo,
            final DocLog docLog, final PdfCreateInfo createInfo)
            throws DocStoreException {

        this.store(store, branch, pojo, docLog.getCreatedDate(),
                docLog.getUuid(), createInfo);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Stored {} [{}] in {}/{}", docLog.getUuid(),
                    docLog.getTitle(), store.toString(), branch.toString());
        }
    }

    /**
     * Stores a document.
     *
     * @param store
     *            The store.
     * @param branch
     *            Branch in store.
     * @param pojo
     *            POJO to store.
     * @param createDate
     *            Date of creation.
     * @param uuid
     *            {@link UUID} as string.
     * @param createInfo
     *            The {@link PdfCreateInfo} with the PDF file. Is {@code null}
     *            for Copy Job Ticket.
     * @throws DocStoreException
     *             When IO errors.
     */
    private void store(final DocStoreTypeEnum store,
            final DocStoreBranchEnum branch, final JsonAbstractBase pojo,
            final Date createDate, final String uuid,
            final PdfCreateInfo createInfo) throws DocStoreException {

        final Path dir = this.getStorePath(store, branch, createDate, uuid);

        try {
            FileUtils.forceMkdir(dir.toFile());

            if (createInfo != null) {
                FileUtils.copyFile(createInfo.getPdfFile(),
                        getStoredPdf(dir, uuid).toFile());
            }

        } catch (IOException e) {
            throw new DocStoreException(e.getMessage());
        }

        try (FileWriter writer =
                new FileWriter(getStoredJson(dir, uuid).toFile());) {

            JsonHelper.write(pojo, writer);

        } catch (IOException e) {
            throw new DocStoreException(e.getMessage());
        }
    }

    @Override
    public long clean(final DocStoreTypeEnum store,
            final DocStoreBranchEnum branch, final Date cleaningDate,
            final int keepDays, final RunModeSwitch runMode)
            throws IOException {

        final Date referenceDate = DateUtils.addDays(cleaningDate, -keepDays);

        return new DocStoreCleaner(this.getStoreBranch(store, branch),
                createCalendarTime(referenceDate), runMode).clean();
    }

}
