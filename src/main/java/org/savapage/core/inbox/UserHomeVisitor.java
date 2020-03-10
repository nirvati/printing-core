/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2020 Datraverse B.V.
 * Authors: Rijk Ravestein.
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
package org.savapage.core.inbox;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp;
import org.savapage.core.config.UserHomePathEnum;
import org.savapage.core.dao.DaoContext;
import org.savapage.core.doc.DocContent;
import org.savapage.core.doc.IDocVisitor;
import org.savapage.core.dto.UserHomeStatsDto;
import org.savapage.core.i18n.AdverbEnum;
import org.savapage.core.i18n.NounEnum;
import org.savapage.core.job.RunModeSwitch;
import org.savapage.core.outbox.OutboxInfoDto;
import org.savapage.core.services.InboxService;
import org.savapage.core.services.OutboxService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.util.DateUtil;
import org.savapage.core.util.LocaleHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Visitor of user homes.
 *
 * @author Rijk Ravestein
 *
 */
public final class UserHomeVisitor extends SimpleFileVisitor<Path>
        implements IDocVisitor {

    /**
     * File statistics.
     */
    public static final class FileStats {

        /** */
        private long scanned;

        /** */
        private long cleanup;

        /** */
        private BigInteger bytes;

        /** */
        private BigInteger bytesCleanup;

        /** */
        private Date cleanDate;

        /** */
        private FileStats() {
            this.init();
        }

        public void init() {
            this.scanned = 0;
            this.bytes = BigInteger.ZERO;
            this.bytesCleanup = BigInteger.ZERO;
            this.cleanup = 0;
        }

        public long getScanned() {
            return scanned;
        }

        public Date getCleanDate() {
            return cleanDate;
        }

        public long getCleanup() {
            return cleanup;
        }

        public BigInteger getBytes() {
            return bytes;
        }

        public BigInteger getBytesCleanup() {
            return bytesCleanup;
        }

    }

    /**
     * Execution statistics.
     */
    public static final class ExecStats {

        /**
         * Execution start.
         */
        private Date start;

        /**
         * Execution duration.
         */
        private Duration duration;

        /**
         * If {@code true} execution is terminated prematurely.
         */
        private boolean terminated;

        /**
         * Number of user homes scanned.
         */
        private long userHomeScanned;

        /**
         * Number of user homes (to be) cleaned up.
         */
        private long userHomeCleanup;

        /**
         *
         */
        private final FileStats pdfInbox;

        /**
         *
         */
        private final FileStats pdfOutbox;

        /** */
        private final RunModeSwitch mode;

        /**
         * Number of conflicts. For instance, concurrent file delete due to
         * concurrent user home access after User Web App login or Hold/Fast
         * Print release.
         */
        private long conflicts;

        /**
         * @param run
         *            Run mode.
         */
        private ExecStats(final RunModeSwitch run) {
            this.mode = run;
            this.pdfInbox = new FileStats();
            this.pdfOutbox = new FileStats();
        }

        /**
         * Initialize statistics.
         */
        private void init() {
            this.userHomeScanned = 0;
            this.userHomeCleanup = 0;
            this.terminated = false;
            this.conflicts = 0;
            this.pdfInbox.init();
            this.pdfOutbox.init();
        }

        /**
         * @return Run mode.
         */
        public RunModeSwitch getMode() {
            return mode;
        }

        /**
         * @return Number of user homes scanned.
         */
        public long getUserHomeScanned() {
            return userHomeScanned;
        }

        /**
         * @return Number of user homes (to be) cleaned.
         */
        public long getUserHomeCleanup() {
            return userHomeCleanup;
        }

        /**
         * @return Number of IO errors.
         */
        public long getConflicts() {
            return conflicts;
        }

        /**
         * @return PDF inbox statistics.
         */
        public FileStats getPdfInbox() {
            return this.pdfInbox;
        }

        /**
         * @return PDF outbox statistics.
         */
        public FileStats getPdfOutbox() {
            return this.pdfOutbox;
        }

        /**
         * @return Execution duration.
         */
        public Duration getDuration() {
            return this.duration;
        }

        /**
         *
         * @param size
         *            Size.
         * @return display string.
         */
        private static String byteCountToDisplaySize(final BigInteger size) {
            return FileUtils.byteCountToDisplaySize(size).replace("bytes", "")
                    .trim();
        }

        /**
         * @return {@link UserHomeStatsDto}.
         */
        public UserHomeStatsDto createDto() {

            final UserHomeStatsDto dto = new UserHomeStatsDto();

            dto.setDate(
                    new Date(this.start.getTime() + this.duration.toMillis()));

            dto.setCleaned(this.mode.isReal());

            UserHomeStatsDto.Stats statsWlk;
            UserHomeStatsDto.Scope scopeWlk;

            FileStats fileStatsWlk;

            // -----------------------
            // Current
            // -----------------------
            statsWlk = new UserHomeStatsDto.Stats();
            dto.setCurrent(statsWlk);

            // --- Homes
            scopeWlk = new UserHomeStatsDto.Scope();
            statsWlk.setUsers(scopeWlk);

            scopeWlk.setCount(this.userHomeScanned);

            // --- Inbox
            scopeWlk = new UserHomeStatsDto.Scope();
            statsWlk.setInbox(scopeWlk);

            fileStatsWlk = this.pdfInbox;

            scopeWlk.setCount(fileStatsWlk.getScanned());
            scopeWlk.setSize(fileStatsWlk.getBytes());

            if (this.mode.isReal()) {
                scopeWlk.setCount(scopeWlk.getCount() - fileStatsWlk.cleanup);
                scopeWlk.setSize(scopeWlk.getSize()
                        .subtract(fileStatsWlk.getBytesCleanup()));
            }

            // --- Outbox
            scopeWlk = new UserHomeStatsDto.Scope();
            statsWlk.setOutbox(scopeWlk);

            fileStatsWlk = this.pdfOutbox;

            scopeWlk.setCount(fileStatsWlk.getScanned());
            scopeWlk.setSize(fileStatsWlk.getBytes());

            if (this.mode.isReal()) {
                scopeWlk.setCount(scopeWlk.getCount() - fileStatsWlk.cleanup);
                scopeWlk.setSize(scopeWlk.getSize()
                        .subtract(fileStatsWlk.getBytesCleanup()));
            }

            // -----------------------
            // Cleanup
            // -----------------------
            statsWlk = new UserHomeStatsDto.Stats();
            dto.setCleanup(statsWlk);

            // --- Homes
            scopeWlk = new UserHomeStatsDto.Scope();
            statsWlk.setUsers(scopeWlk);

            scopeWlk.setCount(this.userHomeCleanup);

            // --- Inbox
            scopeWlk = new UserHomeStatsDto.Scope();
            statsWlk.setInbox(scopeWlk);

            fileStatsWlk = this.pdfInbox;
            scopeWlk.setCount(fileStatsWlk.cleanup);
            scopeWlk.setSize(fileStatsWlk.getBytesCleanup());

            // --- Outbox
            scopeWlk = new UserHomeStatsDto.Scope();
            statsWlk.setOutbox(scopeWlk);

            fileStatsWlk = this.pdfOutbox;
            scopeWlk.setCount(fileStatsWlk.cleanup);
            scopeWlk.setSize(fileStatsWlk.getBytesCleanup());

            //
            return dto;
        }

        /**
         * @param locale
         *            Locale.
         * @return A one-line info message.
         */
        public String infoMessage(final Locale locale) {

            final LocaleHelper localeHelper = new LocaleHelper(locale);

            final StringBuilder msg = new StringBuilder();
            if (this.mode.isReal()) {
                msg.append(AdverbEnum.CLEANED_UP.uiText(locale));
            } else {
                msg.append(AdverbEnum.CLEANABLE.uiText(locale));
            }
            msg.append(": ");

            final long nUsers = this.userHomeCleanup;

            msg.append(localeHelper.getNumber(nUsers)).append(" ")
                    .append(NounEnum.USER.uiText(locale, nUsers != 1));

            if (nUsers > 0) {

                final long nFiles = pdfInbox.cleanup + pdfOutbox.cleanup;

                msg.append(", ").append(localeHelper.getNumber(nFiles))
                        .append(" ")
                        .append(NounEnum.DOCUMENT.uiText(locale, nFiles != 1))
                        .append(", ")
                        .append(FileUtils
                                .byteCountToDisplaySize(pdfInbox.bytesCleanup
                                        .add(pdfOutbox.bytesCleanup)));
            }
            msg.append(".");
            return msg.toString();
        }

        /**
         * @return Summary table.
         */
        public String summary() {

            final String headerMain;
            final String headerClean;
            if (this.mode.isReal()) {
                headerMain = "User Home Clean";
                headerClean = "Cleaned";
            } else {
                headerMain = "User Home Scan";
                headerClean = "Cleanable";
            }

            final StringBuilder msg = new StringBuilder();
            msg.append(
                    "+==============================+===========+=========+");
            msg.append(String.format("\n" + "| %-28s | Conflicts | %7d |",
                    headerMain, this.conflicts));

            msg.append("\n"
                    + "+====================+=========+===========+=========+");
            msg.append(String.format(
                    "\n" + "| Scope              |  Before | %9s |   After |",
                    headerClean));
            msg.append("\n"
                    + "+------------+--- ---+---------+-----------+---------+");
            msg.append(String.format(
                    "\n" + "| Home       | users | %7d |   %7d | %7s |",
                    this.userHomeScanned, this.userHomeCleanup, ""));
            msg.append(String.format(
                    "\n" + "| Print-In   | jobs  | %7d |   %7d | %7d |",
                    this.pdfInbox.scanned, this.pdfInbox.cleanup,
                    this.pdfInbox.scanned - this.pdfInbox.cleanup));
            msg.append(String.format(
                    "\n" + "|            | size  | %7s |   %7s | %7s |",
                    byteCountToDisplaySize(this.pdfInbox.bytes),
                    byteCountToDisplaySize(this.pdfInbox.bytesCleanup),
                    byteCountToDisplaySize(this.pdfInbox.bytes
                            .subtract(this.pdfInbox.bytesCleanup))));

            msg.append(String.format(
                    "\n" + "| Print-Hold | jobs  | %7d |   %7d | %7d |",
                    this.pdfOutbox.scanned, this.pdfOutbox.cleanup,
                    this.pdfOutbox.scanned - this.pdfOutbox.cleanup));
            msg.append(String.format(
                    "\n" + "|            | size  | %7s |   %7s | %7s |",
                    byteCountToDisplaySize(this.pdfOutbox.bytes),
                    byteCountToDisplaySize(this.pdfOutbox.bytesCleanup),
                    byteCountToDisplaySize(this.pdfOutbox.bytes
                            .subtract(this.pdfOutbox.bytesCleanup))));

            msg.append("\n"
                    + "+============+=======+=========+===========+=========+");

            final StringBuilder last = new StringBuilder();
            if (this.terminated) {
                last.append("Terminated");
            } else {
                last.append("Completed");
            }
            last.append(" after ");
            if (this.duration.toMillis() < DateUtil.DURATION_MSEC_SECOND) {
                last.append(this.duration.toMillis()).append(" msec.");
            } else {
                last.append(DurationFormatUtils.formatDurationWords(
                        this.duration.toMillis(), true, true));
            }
            msg.append(String.format("\n| %-50s |", last.toString()));
            msg.append("\n"
                    + "+====================================================+");

            return msg.toString();
        }

        /**
         * Updates {@link IConfigProp.Key#STATS_USERHOME} in database.
         *
         * @throws IOException
         *             If JSON error.
         */
        public void updateDb() throws IOException {

            final DaoContext daoCtx = ServiceContext.getDaoContext();
            final boolean innerTrx = !daoCtx.isTransactionActive();

            final String json = this.createDto().stringify();

            try {

                if (innerTrx) {
                    daoCtx.beginTransaction();
                }
                ConfigManager.instance().updateConfigKey(
                        IConfigProp.Key.STATS_USERHOME, json,
                        ServiceContext.getActor());
                daoCtx.commit();

            } finally {
                if (innerTrx) {
                    daoCtx.rollback();
                }
            }
        }

    }

    /** */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(UserHomeVisitor.class);

    /** */
    private static final OutboxService OUTBOX_SERVICE =
            ServiceContext.getServiceFactory().getOutboxService();

    /**
     * Indicated if execution is in progress.
     */
    private static AtomicBoolean executing = new AtomicBoolean();

    /** */
    private final Path userHomeRootPath;

    /** */
    private final RunModeSwitch runMode;

    /** */
    private final ExecStats stats;

    /** */
    private String wlkUserId;

    /** */
    private UserHomePathEnum wlkUserHomePath;

    /** */
    private int wlkDepth;

    /** */
    private FileStats wlkPdfStats;

    /** */
    private boolean wlkUserHomeCleaned;

    /**
     * Lookup by User Outbox PDF filename.
     */
    private final Map<String, Path> wlkUserOutboxJobsMap;

    private final List<Path> wlkUserInboxEcoFiles;

    /**
     * @param inboxHome
     *            SafePages home directory.
     * @param dateCleanInbox
     *            Inbox PDF documents with creation date before this date are
     *            cleaned. If {@code null} no cleaning is done.
     * @param dateCleanOutbox
     *            Outbox PDF documents with expiration date before this date are
     *            cleaned. If {@code null} no cleaning is done.
     * @param mode
     *            The run mode. If {@code RunModeSwitch#DRY}, processing is done
     *            without cleaning.
     */
    public UserHomeVisitor(final Path inboxHome, final Date dateCleanInbox,
            final Date dateCleanOutbox, final RunModeSwitch mode) {

        this.userHomeRootPath = inboxHome;

        this.runMode = mode;

        this.stats = new ExecStats(mode);
        this.stats.pdfInbox.cleanDate = dateCleanInbox;
        this.stats.pdfOutbox.cleanDate = dateCleanOutbox;

        this.wlkUserOutboxJobsMap = new HashMap<>();
        this.wlkUserInboxEcoFiles = new ArrayList<>();
    }

    /**
     * Initialize at scan start.
     */
    public void onInit() {
        this.stats.init();
        this.wlkUserHomePath = UserHomePathEnum.BASE;
        this.wlkUserHomeCleaned = false;
        this.wlkPdfStats = this.stats.pdfInbox;
        this.wlkDepth = 0;
        this.wlkUserOutboxJobsMap.clear();
        this.wlkUserInboxEcoFiles.clear();
    }

    /**
     * @return {@code true} when execution is in progress.
     */
    public static boolean isExecuting() {
        return executing.get();
    }

    /**
     * Visits all levels of the user home root file tree.
     *
     * @return Scan statistics or {@code null} when execution is already in
     *         progress.
     * @throws IOException
     *             If IO error.
     */
    public ExecStats execute() throws IOException {

        if (!executing.compareAndSet(false, true)) {
            return null;
        }

        try {
            final long execStart = System.currentTimeMillis();

            this.stats.start = new Date(execStart);

            this.onInit();

            /*
             * The file tree traversal is **depth-first** with this FileVisitor
             * invoked for each file encountered. File tree traversal completes
             * when all accessible files in the tree have been visited, or a
             * visit method returns a FileVisitResult#TERMINATE.
             *
             * Where a visit method terminates due an IOException, an uncaught
             * error, or runtime exception, then the traversal is terminated and
             * the error or exception is propagated to the caller of this
             * method.
             */
            Files.walkFileTree(this.userHomeRootPath, this);

            this.stats.duration =
                    Duration.ofMillis(System.currentTimeMillis() - execStart);
        } finally {
            executing.compareAndSet(true, false);
        }

        return this.stats;
    }

    /**
     * Terminates execution.
     */
    public void terminate() {
        this.stats.terminated = true;
    }

    /**
     * Checks if valid UUID string.
     *
     * @param uuid
     *            UUID string.
     * @return {@code true} if valid.
     */
    private static boolean isUUID(final String uuid) {
        try {
            UUID.fromString(uuid);
            return true;
        } catch (Exception e) {
            // noop
        }
        return false;
    }

    /**
     * @param dir
     *            User home directory path.
     * @return {@link UserHomePathEnum}.
     */
    private static UserHomePathEnum getUserHomePathEnum(final Path dir) {

        if (dir.endsWith(UserHomePathEnum.LETTERHEADS.getPath())) {
            return UserHomePathEnum.LETTERHEADS;
        } else if (dir.endsWith(UserHomePathEnum.OUTBOX.getPath())) {
            return UserHomePathEnum.OUTBOX;
        } else if (dir.endsWith(UserHomePathEnum.PGP_PUBRING.getPath())) {
            return UserHomePathEnum.PGP_PUBRING;
        }
        return UserHomePathEnum.BASE;
    }

    @Override
    public FileVisitResult preVisitDirectory(final Path dir,
            final BasicFileAttributes attrs) throws IOException {

        if (this.stats.terminated) {
            return FileVisitResult.TERMINATE;
        }

        Objects.requireNonNull(dir);
        Objects.requireNonNull(attrs);

        if (dir.equals(this.userHomeRootPath)) {
            return FileVisitResult.CONTINUE;
        }

        this.wlkDepth++;

        if (this.wlkDepth < ConfigManager.getUserHomeDepthFromRoot()) {

            this.wlkUserHomePath = null;
            this.wlkPdfStats = null;

        } else if (this.wlkDepth == ConfigManager.getUserHomeDepthFromRoot()) {

            this.wlkUserHomePath = UserHomePathEnum.BASE;
            this.wlkUserId = dir.getFileName().toString();
            this.wlkPdfStats = this.stats.pdfInbox;
            this.stats.userHomeScanned++;

            LOGGER.debug("Home [{}]", this.wlkUserId);

        } else {

            this.wlkUserHomePath = getUserHomePathEnum(dir);

            if (this.wlkUserHomePath == UserHomePathEnum.OUTBOX) {
                this.wlkPdfStats = this.stats.pdfOutbox;
                this.wlkUserOutboxJobsMap.clear();
            } else {
                this.wlkPdfStats = null;
            }
        }

        return FileVisitResult.CONTINUE;
    }

    /**
     *
     * @param path
     *            File path.
     * @return File size
     * @throws IOException
     *             When file does not exist.
     */
    private static BigInteger getFileSize(final Path path) throws IOException {
        try {
            return FileUtils.sizeOfAsBigInteger(path.toFile());
        } catch (Exception e) {
            throw new NoSuchFileException(path.toFile().getAbsolutePath());
        }
    }

    @Override
    public FileVisitResult visitFile(final Path file,
            final BasicFileAttributes attrs) throws IOException {

        if (this.stats.terminated) {
            return FileVisitResult.TERMINATE;
        }

        Objects.requireNonNull(file);
        Objects.requireNonNull(attrs);

        if (this.wlkDepth < ConfigManager.getUserHomeDepthFromRoot()) {
            LOGGER.warn("{} : out of place", file.toString());
            return FileVisitResult.CONTINUE;
        }

        final Path fileName = file.getFileName();
        if (fileName == null) {
            return FileVisitResult.CONTINUE;
        }

        // Anything to scan?
        if (this.wlkPdfStats == null) {
            return FileVisitResult.CONTINUE;
        }

        final String ext = FilenameUtils.getExtension(fileName.toString());

        try {
            // Postpone .eco file cleaning.
            if (this.wlkUserHomePath == UserHomePathEnum.BASE
                    && ext.equalsIgnoreCase(InboxService.FILENAME_EXT_ECO)) {
                this.wlkUserInboxEcoFiles.add(file);
            }

            //
            if (!ext.equalsIgnoreCase(DocContent.FILENAME_EXT_PDF)) {
                // TODO: stats for non-PDF files.
                return FileVisitResult.CONTINUE;
            }

            final String baseName =
                    FilenameUtils.getBaseName(fileName.toString());

            if (!isUUID(baseName)) {
                // TODO: stats for invalid files.
                LOGGER.warn("{} : invalid UUID [{}]", file.toString(),
                        baseName);
            }

            final BigInteger fileSize = getFileSize(file);

            this.wlkPdfStats.scanned++;
            this.wlkPdfStats.bytes = this.wlkPdfStats.bytes.add(fileSize);

            if (this.wlkUserHomePath == UserHomePathEnum.BASE) {
                if (this.wlkPdfStats.cleanDate != null && FileUtils.isFileOlder(
                        file.toFile(), this.wlkPdfStats.cleanDate)) {

                    if (this.runMode.isReal()) {
                        Files.delete(file);
                    }

                    this.wlkPdfStats.cleanup++;
                    this.wlkPdfStats.bytesCleanup =
                            this.wlkPdfStats.bytesCleanup.add(fileSize);

                    this.wlkUserHomeCleaned = true;
                }
            } else if (this.wlkUserHomePath == UserHomePathEnum.OUTBOX) {
                this.wlkUserOutboxJobsMap.put(fileName.toString(), file);
            }

        } catch (IOException e) {
            this.stats.conflicts++;
            LOGGER.error("{} {} ", e.getClass().getSimpleName(),
                    e.getMessage());
        }

        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(final Path file,
            final IOException exc) throws IOException {

        if (this.stats.terminated) {
            return FileVisitResult.TERMINATE;
        }

        Objects.requireNonNull(file);

        LOGGER.warn("{} {} {}", file.getFileName(),
                exc.getClass().getSimpleName(), exc.getMessage());

        this.stats.conflicts++;

        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(final Path dir,
            final IOException exc) throws IOException {

        if (this.stats.terminated) {
            return FileVisitResult.TERMINATE;
        }

        Objects.requireNonNull(dir);

        if (exc != null) {
            throw exc;
        }

        final FileVisitResult visitResult = FileVisitResult.CONTINUE;

        if (dir.equals(this.userHomeRootPath)) {
            return visitResult;
        }

        if (this.wlkUserHomePath == UserHomePathEnum.OUTBOX) {
            this.onPostVisitOutbox();
        }

        this.wlkDepth--;

        if (this.wlkDepth == ConfigManager.getUserHomeDepthFromRoot()) {
            this.wlkUserHomePath = UserHomePathEnum.BASE;
            this.wlkPdfStats = this.stats.pdfInbox;
        } else if (this.wlkDepth < ConfigManager.getUserHomeDepthFromRoot()) {
            this.onPostVisitBase();
        }

        return visitResult;
    }

    /**
     * Invoked for a {@link UserHomePathEnum#OUTBOX} directory after entries in
     * this directory, and all of their descendants, have been visited.
     */
    private void onPostVisitOutbox() {

        if (this.wlkUserOutboxJobsMap.isEmpty()
                || this.wlkPdfStats.cleanDate == null) {
            return;
        }

        final OutboxInfoDto dto = OUTBOX_SERVICE.pruneOutboxInfo(this.wlkUserId,
                this.wlkPdfStats.cleanDate, this.runMode);

        if (dto.getJobCount() == this.wlkUserOutboxJobsMap.size()) {
            return;
        }

        for (final Entry<String, Path> entry : this.wlkUserOutboxJobsMap
                .entrySet()) {

            if (!dto.containsJob(entry.getKey())) {

                final Path path = entry.getValue();

                try {

                    final BigInteger fileSize = getFileSize(path);

                    if (this.runMode.isReal()) {
                        Files.delete(path);
                    }

                    this.wlkPdfStats.cleanup++;
                    this.wlkPdfStats.bytesCleanup =
                            this.wlkPdfStats.bytesCleanup.add(fileSize);
                    this.wlkUserHomeCleaned = true;

                } catch (IOException e) {
                    this.stats.conflicts++;
                    LOGGER.error("{} {} ", e.getClass().getSimpleName(),
                            e.getMessage());
                }
            }
        }
    }

    /**
     * Invoked for a {@link UserHomePathEnum#BASE} directory after entries in
     * this directory, and all of their descendants, have been visited.
     *
     * @throws IOException
     *             When file delete error.
     */
    private void onPostVisitBase() {

        for (final Path path : this.wlkUserInboxEcoFiles) {
            final File file =
                    new File(FilenameUtils.removeExtension(path.toString()));
            try {
                if (!file.exists()) {
                    final BigInteger size = getFileSize(path);

                    if (this.runMode.isReal()) {
                        Files.delete(path);
                    }

                    this.wlkPdfStats.bytes = this.wlkPdfStats.bytes.add(size);
                    this.wlkPdfStats.bytesCleanup =
                            this.wlkPdfStats.bytesCleanup.add(size);
                    this.wlkUserHomeCleaned = true;
                }
            } catch (IOException e) {
                LOGGER.warn("{} {}", e.getClass().getSimpleName(),
                        e.getMessage());
            }
        }

        this.wlkUserInboxEcoFiles.clear();

        if (this.wlkUserHomeCleaned) {
            this.stats.userHomeCleanup++;
        }
        this.wlkUserHomeCleaned = false;
    }
}
