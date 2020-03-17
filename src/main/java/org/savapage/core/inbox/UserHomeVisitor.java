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
import java.nio.file.Paths;
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
import org.savapage.core.i18n.PrintOutNounEnum;
import org.savapage.core.job.RunModeSwitch;
import org.savapage.core.outbox.OutboxInfoDto;
import org.savapage.core.services.InboxService;
import org.savapage.core.services.OutboxService;
import org.savapage.core.services.PGPPublicKeyService;
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
     * File cleanup statistics.
     */
    public static class FileStats {

        /** */
        private long scanned;

        /** */
        private BigInteger bytes;

        public void init() {
            this.scanned = 0;
            this.bytes = BigInteger.ZERO;
        }

        public final long getScanned() {
            return scanned;
        }

        public final BigInteger getBytes() {
            return bytes;
        }

        public final void incrementScanned() {
            this.scanned++;
        }

        public final void addBytes(final BigInteger fileSize) {
            this.bytes = this.bytes.add(fileSize);
        }
    }

    /**
     * File cleanup statistics.
     */
    public static final class FileCleanupStats extends FileStats {

        /** */
        private long cleanup;

        /** */
        private BigInteger bytesCleanup;

        /** */
        private Date cleanDate;

        /** */
        private FileCleanupStats() {
            this.init();
        }

        @Override
        public void init() {
            super.init();
            this.bytesCleanup = BigInteger.ZERO;
            this.cleanup = 0;
        }

        public Date getCleanDate() {
            return cleanDate;
        }

        public long getCleanup() {
            return cleanup;
        }

        public BigInteger getBytesCleanup() {
            return bytesCleanup;
        }

        public final void incrementCleanup() {
            this.cleanup++;
        }

        public void addBytesCleanup(final BigInteger fileSize) {
            this.bytesCleanup = this.bytesCleanup.add(fileSize);
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
         * User home letterhead files.
         */
        private final FileStats filesHomeLetterheads;

        /**
         * User home PGP pubring public key files.
         */
        private final FileStats filesHomePgpPubRing;

        /**
         * Unknown (unidentified) user home files.
         */
        private final FileStats filesUnknown;

        /**
         * Number of user homes (to be) cleaned up.
         */
        private long userHomeCleanup;

        /**
         *
         */
        private final FileCleanupStats pdfInbox;

        /**
         *
         */
        private final FileCleanupStats pdfOutbox;

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
            this.filesUnknown = new FileStats();
            this.filesHomeLetterheads = new FileStats();
            this.filesHomePgpPubRing = new FileStats();
            this.pdfInbox = new FileCleanupStats();
            this.pdfOutbox = new FileCleanupStats();
        }

        /**
         * Initialize statistics.
         */
        private void init() {
            this.userHomeScanned = 0;
            this.userHomeCleanup = 0;
            this.terminated = false;
            this.conflicts = 0;
            this.filesUnknown.init();
            this.filesHomeLetterheads.init();
            this.filesHomePgpPubRing.init();
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
         * @return User home letterheads files.
         */
        public FileStats getFilesHomeLetterheads() {
            return this.filesHomeLetterheads;
        }

        /**
         * @return User home PGP pubring public key files.
         */
        public FileStats getFilesHomePgpPubRing() {
            return this.filesHomePgpPubRing;
        }

        /**
         * @return Unknown (unidentified) user home files.
         */
        public FileStats getFilesUnknown() {
            return this.filesUnknown;
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
        public FileCleanupStats getPdfInbox() {
            return this.pdfInbox;
        }

        /**
         * @return PDF outbox statistics.
         */
        public FileCleanupStats getPdfOutbox() {
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

            if (this.terminated) {
                dto.setReturnCode(1);
            } else {
                dto.setReturnCode(0);
            }

            dto.setCleaned(this.mode.isReal());

            UserHomeStatsDto.Stats statsWlk;
            UserHomeStatsDto.Scope scopeWlk;

            FileCleanupStats fileStatsWlk;

            // -----------------------
            // Current
            // -----------------------
            statsWlk = new UserHomeStatsDto.Stats();
            dto.setCurrent(statsWlk);

            // --- Homes
            scopeWlk = new UserHomeStatsDto.Scope();
            statsWlk.setUsers(scopeWlk);

            scopeWlk.setCount(this.userHomeScanned);

            // --- Letterheads
            if (this.filesHomeLetterheads.getScanned() > 0) {
                scopeWlk = new UserHomeStatsDto.Scope();
                scopeWlk.setCount(this.filesHomeLetterheads.getScanned());
                scopeWlk.setSize(this.filesHomeLetterheads.getBytes());
                statsWlk.setLetterheads(scopeWlk);
            }

            // --- PGP
            if (this.filesHomePgpPubRing.getScanned() > 0) {
                scopeWlk = new UserHomeStatsDto.Scope();
                scopeWlk.setCount(this.filesHomePgpPubRing.getScanned());
                scopeWlk.setSize(this.filesHomePgpPubRing.getBytes());
                statsWlk.setPgpPubRing(scopeWlk);
            }

            // --- Unknown
            if (this.filesUnknown.getScanned() > 0) {
                scopeWlk = new UserHomeStatsDto.Scope();
                scopeWlk.setCount(this.filesUnknown.getScanned());
                scopeWlk.setSize(this.filesUnknown.getBytes());
                statsWlk.setUnkown(scopeWlk);
            }

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
                msg.append(AdverbEnum.CLEANED.uiText(locale));
            } else {
                msg.append(AdverbEnum.CLEANABLE.uiText(locale));
            }
            msg.append(": ");

            final long nUsers = this.userHomeCleanup;

            msg.append(localeHelper.getNumber(nUsers)).append(" ")
                    .append(NounEnum.USER.uiText(locale, nUsers != 1))
                    .append(".");

            if (nUsers > 0) {

                FileCleanupStats stats = pdfInbox;
                long nCount = stats.cleanup;
                if (nCount > 0) {
                    msg.append(" ").append(localeHelper.getNumber(nCount));
                    msg.append(" ").append(
                            NounEnum.DOCUMENT.uiText(locale, nCount != 1));
                    msg.append(": ")
                            .append(FileUtils
                                    .byteCountToDisplaySize(stats.bytesCleanup))
                            .append(".");
                }

                stats = pdfOutbox;
                nCount = stats.cleanup;
                if (nCount > 0) {
                    msg.append(" ").append(localeHelper.getNumber(nCount));
                    msg.append(" ").append(
                            PrintOutNounEnum.JOB.uiText(locale, nCount != 1));
                    msg.append(": ")
                            .append(FileUtils
                                    .byteCountToDisplaySize(stats.bytesCleanup))
                            .append(".");
                }
            }
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

            if (this.filesHomeLetterheads.getScanned() > 0) {
                msg.append(String.format(
                        "\n" + "|            | lhead | %7d |   %7s | %7s |",
                        this.filesHomeLetterheads.getScanned(), "", ""));
            }
            if (this.filesHomePgpPubRing.getScanned() > 0) {
                msg.append(String.format(
                        "\n" + "|            | pgp   | %7d |   %7s | %7s |",
                        this.filesHomePgpPubRing.getScanned(), "", ""));
            }
            if (this.filesUnknown.getScanned() > 0) {
                msg.append(String.format(
                        "\n" + "|            | ???   | %7d |   %7s | %7s |",
                        this.filesUnknown.getScanned(), "", ""));
            }

            msg.append(String.format(
                    "\n" + "| Print-In   | jobs  | %7d |   %7d | %7d |",
                    this.pdfInbox.getScanned(), this.pdfInbox.cleanup,
                    this.pdfInbox.getScanned() - this.pdfInbox.cleanup));
            msg.append(String.format(
                    "\n" + "|            | size  | %7s |   %7s | %7s |",
                    byteCountToDisplaySize(this.pdfInbox.getBytes()),
                    byteCountToDisplaySize(this.pdfInbox.getBytesCleanup()),
                    byteCountToDisplaySize(this.pdfInbox.getBytes()
                            .subtract(this.pdfInbox.getBytesCleanup()))));

            msg.append(String.format(
                    "\n" + "| Print-Hold | jobs  | %7d |   %7d | %7d |",
                    this.pdfOutbox.getScanned(), this.pdfOutbox.cleanup,
                    this.pdfOutbox.getScanned() - this.pdfOutbox.cleanup));
            msg.append(String.format(
                    "\n" + "|            | size  | %7s |   %7s | %7s |",
                    byteCountToDisplaySize(this.pdfOutbox.getBytes()),
                    byteCountToDisplaySize(this.pdfOutbox.getBytesCleanup()),
                    byteCountToDisplaySize(this.pdfOutbox.getBytes()
                            .subtract(this.pdfOutbox.getBytesCleanup()))));

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
    private static final InboxService INBOX_SERVICE =
            ServiceContext.getServiceFactory().getInboxService();

    /** */
    private static final OutboxService OUTBOX_SERVICE =
            ServiceContext.getServiceFactory().getOutboxService();

    /** */
    private static final PGPPublicKeyService PGP_PUBLICKEY_SERVICE =
            ServiceContext.getServiceFactory().getPGPPublicKeyService();
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
    private Path wlkUserOutboxDir;

    /** */
    private int wlkDepth;

    /** */
    private FileCleanupStats wlkPdfStats;

    /** */
    private boolean wlkUserHomeCleaned;

    /**
     * Lookup by User Outbox PDF filename.
     */
    private final Map<String, BigInteger> wlkUserOutboxJobsMap;

    /** */
    private final List<Path> wlkUserInboxEcoFiles;

    /**
     * @param inboxHome
     *            SafePages home directory.
     * @param dateCleanInbox
     *            Inbox PDF documents with creation date <i>before</i> this date
     *            are cleaned. If {@code null}, no cleaning is done.
     * @param dateCleanOutbox
     *            Outbox jobs with expiration date <i>before</i> this date are
     *            cleaned. If {@code null}, no cleaning is done.
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
        this.wlkUserOutboxDir = null;
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
                this.wlkUserOutboxDir = dir;
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

        final String ext = FilenameUtils.getExtension(fileName.toString());

        final boolean isValidFileName;

        if (this.wlkUserHomePath == UserHomePathEnum.BASE) {
            if (ext.equalsIgnoreCase(InboxService.FILENAME_EXT_ECO)) {
                // Postpone .eco file cleaning.
                this.wlkUserInboxEcoFiles.add(file);
            }
            isValidFileName =
                    INBOX_SERVICE.isValidInboxFileName(fileName.toString());
        } else if (this.wlkUserHomePath == UserHomePathEnum.OUTBOX) {
            isValidFileName =
                    OUTBOX_SERVICE.isValidOutboxFileName(fileName.toString());
        } else if (this.wlkUserHomePath == UserHomePathEnum.LETTERHEADS) {
            isValidFileName = INBOX_SERVICE
                    .isValidInboxLetterheadFileName(fileName.toString());
        } else if (this.wlkUserHomePath == UserHomePathEnum.PGP_PUBRING) {
            isValidFileName = PGP_PUBLICKEY_SERVICE
                    .isValidRingEntryFileName(fileName.toString());
        } else {
            isValidFileName = false;
        }

        try {

            final BigInteger fileSize = getFileSize(file);

            // Homes
            // this.stats.

            // --- Unknown
            if (!isValidFileName) {
                this.stats.filesUnknown.incrementScanned();
                this.stats.filesUnknown.addBytes(fileSize);
                LOGGER.warn("Unknown filename syntax [{}]", file.toString());
            }

            final boolean isPdf =
                    ext.equalsIgnoreCase(DocContent.FILENAME_EXT_PDF);

            switch (this.wlkUserHomePath) {

            case PGP_PUBRING:
                this.stats.filesHomePgpPubRing.incrementScanned();
                this.stats.filesHomePgpPubRing.addBytes(fileSize);
                break;

            case LETTERHEADS:
                if (isPdf) {
                    this.stats.filesHomeLetterheads.incrementScanned();
                    this.stats.filesHomeLetterheads.addBytes(fileSize);
                }
                break;

            case OUTBOX:
                if (isPdf) {
                    this.wlkUserOutboxJobsMap.put(fileName.toString(),
                            fileSize);
                }
                break;

            case BASE:
                if (isPdf && this.wlkPdfStats.cleanDate != null
                        && FileUtils.isFileOlder(file.toFile(),
                                this.wlkPdfStats.cleanDate)) {

                    if (this.runMode.isReal()) {
                        Files.delete(file);
                    }

                    this.wlkPdfStats.incrementCleanup();
                    this.wlkPdfStats.addBytesCleanup(fileSize);

                    this.wlkUserHomeCleaned = true;
                }
                break;

            default:
                throw new java.lang.UnsupportedOperationException(
                        this.wlkUserHomePath.toString());
            }

            if (isPdf && this.wlkPdfStats != null) {
                this.wlkPdfStats.incrementScanned();
                this.wlkPdfStats.addBytes(fileSize);
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
            this.wlkUserOutboxDir = null;
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

        final OutboxInfoDto outboxInfo = OUTBOX_SERVICE.pruneOutboxInfo(
                this.wlkUserId, this.wlkPdfStats.cleanDate, this.runMode);

        if (outboxInfo.getJobCount() == this.wlkUserOutboxJobsMap.size()) {
            return;
        }

        for (final Entry<String, BigInteger> entry : this.wlkUserOutboxJobsMap
                .entrySet()) {

            if (outboxInfo.containsJob(entry.getKey())) {
                continue;
            }

            /*
             * Observed PDF is not part of outbox job info: add to cleanup.
             */
            this.wlkPdfStats.incrementCleanup();
            this.wlkPdfStats.addBytesCleanup(entry.getValue());
            this.wlkUserHomeCleaned = true;

            if (this.runMode.isReal()) {
                /*
                 * If PDF exists (was not pruned), it is orphaned (due to some
                 * error) and not an active job. So, we prune prune it here.
                 */
                final Path pdfPath = Paths.get(this.wlkUserOutboxDir.toString(),
                        entry.getKey());

                try {
                    if (Files.exists(pdfPath)) {
                        Files.delete(pdfPath);
                    }
                } catch (IOException e) {
                    LOGGER.warn("{} {}", e.getClass().getSimpleName(),
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

                    this.wlkPdfStats.addBytes(size);
                    this.wlkPdfStats.addBytesCleanup(size);
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
