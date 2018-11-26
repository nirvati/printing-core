/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2018 Datraverse B.V.
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

import static java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE;
import static java.nio.file.attribute.PosixFilePermission.OWNER_READ;
import static java.nio.file.attribute.PosixFilePermission.OWNER_WRITE;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;
import java.util.Set;
import java.util.TimeZone;

import org.apache.commons.lang3.time.DateUtils;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.doc.DocContent;
import org.savapage.core.doc.store.DocStoreBranchEnum;
import org.savapage.core.doc.store.DocStoreCleaner;
import org.savapage.core.doc.store.DocStoreException;
import org.savapage.core.doc.store.DocStoreTypeEnum;
import org.savapage.core.job.RunModeSwitch;
import org.savapage.core.jpa.DocLog;
import org.savapage.core.pdf.PdfCreateInfo;
import org.savapage.core.print.proxy.AbstractProxyPrintReq;
import org.savapage.core.services.DocStoreService;
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
    private static final Path ARCHIVE_HOME =
            ConfigManager.getDocStoreHome(DocStoreTypeEnum.ARCHIVE);

    /** */
    private static final Path JOURNAL_HOME =
            ConfigManager.getDocStoreHome(DocStoreTypeEnum.JOURNAL);

    /** */
    private static final Set<PosixFilePermission> FILE_PERMISSIONS =
            EnumSet.of(OWNER_READ, OWNER_WRITE, OWNER_EXECUTE);
    /** */
    private static final FileAttribute<Set<PosixFilePermission>> FILE_ATTRS =
            PosixFilePermissions.asFileAttribute(FILE_PERMISSIONS);;

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
    private static Path getStorePath(final DocStoreTypeEnum store,
            final DocStoreBranchEnum branch, final DocLog docLog) {

        final Calendar cal = createCalendarTime(docLog.getCreatedDate());

        return Paths.get(getStoreBranch(store, branch).toString(),
                String.format("%04d%c%02d%c%02d%c%02d%c%s",
                        cal.get(Calendar.YEAR), File.separatorChar,
                        cal.get(Calendar.MONTH) + 1, File.separatorChar,
                        cal.get(Calendar.DAY_OF_MONTH), File.separatorChar,
                        cal.get(Calendar.HOUR_OF_DAY), File.separatorChar,
                        docLog.getUuid()));
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
    private static Path getStoreBranch(final DocStoreTypeEnum store,
            final DocStoreBranchEnum branch) {

        final Path path;

        switch (store) {
        case ARCHIVE:
            path = ARCHIVE_HOME;
            break;
        case JOURNAL:
            path = JOURNAL_HOME;
            break;
        default:
            throw new UnknownError(store.toString());
        }
        return Paths.get(path.toString(), branch.getBranch().toString());
    }

    @Override
    public boolean isDocPresent(final DocStoreTypeEnum store,
            final DocStoreBranchEnum branch, final DocLog docLog) {
        return getStorePath(store, branch, docLog).toFile().exists();
    }

    @Override
    public void store(final DocStoreTypeEnum store,
            final AbstractProxyPrintReq request, final DocLog docLog,
            final PdfCreateInfo createInfo) throws DocStoreException {

        final DocStoreBranchEnum branch = DocStoreBranchEnum.OUT_PRINT;

        final Path dir = getStorePath(store, branch, docLog);

        try {
            Files.createDirectories(dir, FILE_ATTRS);

            java.nio.file.Files.copy(
                    Paths.get(createInfo.getPdfFile().getPath()),
                    Paths.get(dir.toString(), String.format("%s.%s",
                            docLog.getUuid(), DocContent.FILENAME_EXT_PDF)));

        } catch (IOException e) {
            throw new DocStoreException(e.getMessage());
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Stored {} [{}] in archive", docLog.getTitle(),
                    docLog.getTitle());
        }
    }

    @Override
    public long clean(final DocStoreTypeEnum store,
            final DocStoreBranchEnum branch, final Date cleaningDate,
            final int keepDays, final RunModeSwitch runMode)
            throws IOException {

        final Date referenceDate = DateUtils.addDays(cleaningDate, -keepDays);

        return new DocStoreCleaner(getStoreBranch(store, branch),
                createCalendarTime(referenceDate), runMode).clean();
    }

}