/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2019 Datraverse B.V.
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
package org.savapage.core.doc;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FilenameUtils;
import org.savapage.core.SpException;
import org.savapage.core.system.SystemInfo;

/**
 * Decrypts a PDF file.
 *
 * @author Rijk Ravestein
 *
 */
public final class PdfToDecrypted extends AbstractFileConverter
        implements IPdfConverter {

    /**
     * The directory location of the created file (can be {@code null}).
     */
    private final File createHome;

    /**
     * @return {@code true} if this convertor is available.
     */
    public static boolean isAvailable() {
        return SystemInfo.isQPdfInstalled();
    }

    /**
     *
     */
    public PdfToDecrypted() {
        super(ExecMode.MULTI_THREADED);
        this.createHome = null;
    }

    /**
     *
     * @param createDir
     *            The directory location of the created file.
     */
    public PdfToDecrypted(final File createDir) {
        super(ExecMode.MULTI_THREADED);
        this.createHome = createDir;
    }

    @Override
    protected File getOutputFile(final File fileIn) {

        final StringBuilder builder = new StringBuilder(128);

        if (this.createHome == null) {
            builder.append(fileIn.getParent());
        } else {
            builder.append(this.createHome.getAbsolutePath());
        }

        builder.append(File.separator)
                .append(FilenameUtils.getBaseName(fileIn.getAbsolutePath()))
                .append("-decrypted.")
                .append(DocContent.getFileExtension(DocContentTypeEnum.PDF));

        return new File(builder.toString());
    }

    @Override
    protected String getOsCommand(final DocContentTypeEnum contentType,
            final File fileIn, final File fileOut) {

        final StringBuilder cmd = new StringBuilder(128);

        try {
            /*
             * See #598
             */
            cmd.append("qpdf --decrypt \"").append(fileIn.getCanonicalPath())
                    .append("\" \"").append(fileOut.getCanonicalPath()) //
                    .append("\" < /dev/null");
        } catch (IOException e) {
            throw new SpException(e.getMessage(), e);
        }

        return cmd.toString();
    }

    @Override
    public File convert(final File fileIn) throws IOException {
        final File filePdf = getOutputFile(fileIn);
        try {
            return convertWithOsCommand(DocContentTypeEnum.PDF, fileIn, filePdf,
                    getOsCommand(DocContentTypeEnum.PDF, fileIn, filePdf));
        } catch (DocContentToPdfException e) {
            throw new IOException(e.getMessage());
        }
    }

}
