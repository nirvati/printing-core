/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2018 Datraverse B.V.
 * Authors: Rijk Ravestein.
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
package org.savapage.core.doc.store;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Calendar;
import java.util.Objects;
import java.util.StringTokenizer;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableLong;
import org.savapage.core.job.RunModeSwitch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class DocStoreCleaner extends SimpleFileVisitor<Path> {

    /** */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(DocStoreCleaner.class);

    /** */
    private int visitYear;
    /** */
    private int visitMonth;
    /** */
    private int visitDay;
    /** */
    private int visitHour;
    /** */
    private String visitUuid;

    /** */
    private final Path store;

    /** */
    private final RunModeSwitch runMode;

    /** */
    private final int refYear;
    /** */
    private final int refMonth;
    /** */
    private final int refDay;
    /** */
    private final int refHour;

    /** */
    private final MutableLong totCleanedDoc;
    /** */
    private final MutableLong totCleanedContainers;

    /** */
    private static final int VISIT_PART_VOID = -1;

    /** */
    private static final int IDX_VISIT_CCYY = 0;
    /** */
    private static final int IDX_VISIT_MM = IDX_VISIT_CCYY + 1;
    /** */
    private static final int IDX_VISIT_DD = IDX_VISIT_MM + 1;
    /** */
    private static final int IDX_VISIT_HH = IDX_VISIT_DD + 1;

    /**
     *
     * @param pathStore
     *            Home directory of the store.
     * @param calRef
     *            The reference calendar date before which stored documents are
     *            cleaned.
     * @param mode
     *            The run mode. If {@code RunModeSwitch#DRY}, processing is done
     *            without cleaning.
     */
    public DocStoreCleaner(final Path pathStore, final Calendar calRef,
            final RunModeSwitch mode) {

        this.store = pathStore;

        this.refYear = calRef.get(Calendar.YEAR);
        this.refMonth = calRef.get(Calendar.MONTH) + 1;
        this.refDay = calRef.get(Calendar.DAY_OF_MONTH);
        this.refHour = calRef.get(Calendar.HOUR_OF_DAY);

        this.totCleanedDoc = new MutableLong();
        this.totCleanedContainers = new MutableLong();

        this.runMode = mode;
    }

    /**
     * Cleans the store.
     *
     * @return Number of documents cleaned.
     * @throws IOException
     *             If IO error.
     */
    public long clean() throws IOException {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Clean: {}", this.store);
            LOGGER.debug(String.format("Reference: %04d %02d %02d %02d",
                    this.refYear, this.refMonth, this.refDay, this.refHour));
        }

        final long nCleaned;

        if (this.store.toFile().exists()) {
            Files.walkFileTree(this.store, this);
            nCleaned = this.totCleanedDoc.longValue();
        } else {
            nCleaned = 0;
        }
        return nCleaned;
    }

    /**
     * Strips the store home path from full path, so the relative date path
     * remains.
     *
     * @param fullPath
     *            The full path.
     * @return The relative date path.
     */
    private String toDatePath(final Path fullPath) {
        return StringUtils.removeStart(fullPath.toString(),
                this.store.toString());
    }

    /**
     * Parses the visited directory path and sets the visit date and uuid
     * fields.
     *
     * @param visitDir
     *            The visited directory.
     */
    private void parseVisitedDir(final Path visitDir) {

        final StringTokenizer st =
                new StringTokenizer(this.toDatePath(visitDir), File.separator);

        this.visitYear = VISIT_PART_VOID;
        this.visitMonth = VISIT_PART_VOID;
        this.visitDay = VISIT_PART_VOID;
        this.visitHour = VISIT_PART_VOID;
        this.visitUuid = null;

        int i = 0;
        for (; st.hasMoreTokens() && i <= IDX_VISIT_HH; i++) {

            final int part = Integer.parseInt(st.nextToken());

            switch (i) {
            case IDX_VISIT_CCYY:
                this.visitYear = part;
                break;
            case IDX_VISIT_MM:
                this.visitMonth = part;
                break;
            case IDX_VISIT_DD:
                this.visitDay = part;
                break;
            case IDX_VISIT_HH:
                this.visitHour = part;
                break;
            default:
                break;
            }
        }

        if (st.hasMoreTokens() && this.visitHour != VISIT_PART_VOID) {
            this.visitUuid = st.nextToken();
        }
    }

    @Override
    public FileVisitResult preVisitDirectory(final Path dir,
            final BasicFileAttributes attrs) throws IOException {

        Objects.requireNonNull(dir);

        if (dir.equals(this.store)) {
            return FileVisitResult.CONTINUE;
        }

        this.parseVisitedDir(dir);

        // Year
        if (this.refYear < this.visitYear) {
            return FileVisitResult.SKIP_SUBTREE;
        }

        // Month
        if (this.visitMonth == VISIT_PART_VOID) {
            return FileVisitResult.CONTINUE;
        }
        if (this.refYear == this.visitYear && this.refMonth < this.visitMonth) {
            return FileVisitResult.SKIP_SUBTREE;
        }

        // Day
        if (this.visitDay == VISIT_PART_VOID) {
            return FileVisitResult.CONTINUE;
        }
        if (this.refYear == this.visitYear && this.refMonth == this.visitMonth
                && this.refDay < this.visitDay) {
            return FileVisitResult.SKIP_SUBTREE;
        }

        // Hour
        if (this.visitHour == VISIT_PART_VOID) {
            return FileVisitResult.CONTINUE;
        }
        if (this.refYear == this.visitYear && this.refMonth == this.visitMonth
                && this.refDay == this.visitDay
                && this.refHour <= this.visitHour) {
            return FileVisitResult.SKIP_SUBTREE;
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(final Path file,
            final BasicFileAttributes attrs) throws IOException {

        Objects.requireNonNull(file);

        if (this.runMode.isReal()) {
            Files.delete(file);
        }
        return FileVisitResult.CONTINUE;
    }

    /**
     *
     * @param dir
     *            The directory path.
     * @return {@code true} if directory must be deleted.
     */
    private boolean isDeleteDir(final Path dir) {

        if (dir.equals(this.store)) {
            return false;
        }

        this.parseVisitedDir(dir);

        // Year
        if (this.refYear > this.visitYear) {
            return true;
        }

        // Month
        if (this.visitMonth == VISIT_PART_VOID) {
            return false;
        }
        if (this.refYear == this.visitYear && refMonth > this.visitMonth) {
            return true;
        }

        // Day
        if (this.visitDay == VISIT_PART_VOID) {
            return false;
        }
        if (this.refYear == this.visitYear && refMonth == this.visitMonth
                && this.refDay > this.visitDay) {
            return true;
        }

        // Hour
        if (this.visitHour == VISIT_PART_VOID) {
            return false;
        }
        if (this.refYear == this.visitYear && this.refMonth == this.visitMonth
                && this.refDay == this.visitDay
                && this.refHour > this.visitHour) {
            return true;
        }

        return false;
    }

    @Override
    public FileVisitResult postVisitDirectory(final Path dir,
            final IOException exc) throws IOException {

        Objects.requireNonNull(dir);

        if (exc != null) {
            throw exc;
        }

        if (isDeleteDir(dir)) {

            if (this.runMode.isReal()) {
                Files.delete(dir);
            }

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Delete: {}", toDatePath(dir));
            }

            if (this.visitUuid == null) {
                this.totCleanedContainers.increment();
            } else {
                this.totCleanedDoc.increment();
            }

        } else {
            if (LOGGER.isDebugEnabled() && !dir.equals(this.store)) {
                LOGGER.debug("Remain: {}", toDatePath(dir));
            }
        }
        return FileVisitResult.CONTINUE;
    }

    /**
     * @return Number of cleaned documents.
     */
    public long getDocsCleaned() {
        return this.totCleanedDoc.longValue();
    }
}
