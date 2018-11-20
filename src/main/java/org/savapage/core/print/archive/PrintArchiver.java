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
package org.savapage.core.print.archive;

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
import java.util.EnumSet;
import java.util.Set;
import java.util.TimeZone;

import org.savapage.core.config.ConfigManager;
import org.savapage.core.doc.DocContent;
import org.savapage.core.jpa.DocLog;
import org.savapage.core.pdf.PdfCreateInfo;
import org.savapage.core.print.proxy.AbstractProxyPrintReq;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class PrintArchiver {

    /** */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(PrintArchiver.class);

    /** */
    private static class SingletonHolder {
        /** */
        public static final PrintArchiver INSTANCE =
                new PrintArchiver();
    }

    /** */
    private final Path archiveHome;

    /** */
    private final FileAttribute<Set<PosixFilePermission>> fileAttributes;

    /**
     * Singleton.
     */
    private PrintArchiver() {

        this.archiveHome = ConfigManager.getArchiveHome();

        final Set<PosixFilePermission> permissions =
                EnumSet.of(OWNER_READ, OWNER_WRITE, OWNER_EXECUTE);

        this.fileAttributes = PosixFilePermissions.asFileAttribute(permissions);
    }

    /**
     * @return The singleton instance.
     */
    public static PrintArchiver instance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     *
     * @param docLog
     *            The document log.
     * @return The archive path for this document.
     */
    private Path getArchivePath(final DocLog docLog) {

        final Calendar cal =
                Calendar.getInstance(TimeZone.getTimeZone(ZoneId.of("UTC")));

        cal.setTime(docLog.getCreatedDate());

        return Paths.get(this.archiveHome.toString(),
                String.format("%04d%c%02d%c%02d%c%02d%c%s",
                        cal.get(Calendar.YEAR), File.separatorChar,
                        cal.get(Calendar.MONTH) + 1, File.separatorChar,
                        cal.get(Calendar.DAY_OF_MONTH), File.separatorChar,
                        cal.get(Calendar.HOUR_OF_DAY), File.separatorChar,
                        docLog.getUuid()));
    }

    /**
     * Checks if archive is present for a document log.
     *
     * @param docLog
     *            The document log.
     * @return {@code true} if archive is present for this document.
     */
    public boolean isArchivePresent(final DocLog docLog) {
        return this.getArchivePath(docLog).toFile().exists();
    }

    /**
     *
     * @param request
     *            The {@link AbstractProxyPrintReq}.
     * @param docLog
     *            The {@link DocLog} persisted in the database.
     * @param createInfo
     *            The {@link PdfCreateInfo} with the PDF file sent to the
     *            printer.
     * @throws PrintArchiveException
     *             When IO errors.
     */
    public void archive(final AbstractProxyPrintReq request,
            final DocLog docLog, final PdfCreateInfo createInfo)
            throws PrintArchiveException {

        final Path dir = getArchivePath(docLog);

        try {
            Files.createDirectories(dir, this.fileAttributes);

            java.nio.file.Files.copy(
                    Paths.get(createInfo.getPdfFile().getPath()),
                    Paths.get(dir.toString(), String.format("%s.%s",
                            docLog.getUuid(), DocContent.FILENAME_EXT_PDF)));
        } catch (IOException e) {
            throw new PrintArchiveException(e.getMessage());
        }
    }

}
