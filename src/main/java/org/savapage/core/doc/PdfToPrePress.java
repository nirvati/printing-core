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
 * Converts a PDF file to PrePress PDF, automatically repairing corrupted PDF
 * along the way.
 *
 * @deprecated Use {@link PdfRepair} for repair actions.
 * @see <a href="https://issues.savapage.org/view.php?id=1011">Mantis #1011</a>
 *
 * @author Rijk Ravestein
 *
 */
@Deprecated
public final class PdfToPrePress extends AbstractFileConverter
        implements IPdfRepair, IPdfConverter {

    /**
     * The directory location of the created file (can be {@code null}).
     */
    private final File createHome;

    /**
     *
     */
    public PdfToPrePress() {
        super(ExecMode.MULTI_THREADED);
        this.createHome = null;
    }

    /**
     *
     * @param createDir
     *            The directory location of the created file.
     */
    public PdfToPrePress(final File createDir) {
        super(ExecMode.MULTI_THREADED);
        this.createHome = createDir;
    }

    @Override
    protected ExecType getExecType() {
        return ExecType.ADVANCED;
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
                .append("-prepress.")
                .append(DocContent.getFileExtension(DocContentTypeEnum.PDF));

        return new File(builder.toString());
    }

    @Override
    protected String getOsCommand(final DocContentTypeEnum contentType,
            final File fileIn, final File fileOut) {

        final StringBuilder cmd = new StringBuilder(128);

        try {
            cmd.append(SystemInfo.Command.GS.cmd()).append(" -sOutputFile=\"")
                    .append(fileOut.getCanonicalPath())
                    .append("\" -sDEVICE=pdfwrite -q -dNOPAUSE -dBATCH") //
                    .append(" -dPDFSETTINGS=/prepress") //
                    .append(" \"").append(fileIn.getCanonicalPath()) //
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
