/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2020 Datraverse B.V.
 * Author: Rijk Ravestein.
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
package org.savapage.core.doc;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FilenameUtils;
import org.savapage.core.SpException;
import org.savapage.core.system.SystemInfo;
import org.savapage.core.system.SystemInfo.ArgumentGS;

/**
 * Converts a PDF file to rasterized PDF using Ghostscript.
 *
 * @author Rijk Ravestein
 *
 */
public final class PdfToRasterPdf extends AbstractFileConverter
        implements IPdfConverter {

    public enum Raster {
        /** */
        GRAYSCALE("pdfimage8"),
        /** */
        RGB("pdfimage24"),
        /** */
        CMYK("pdfimage32");

        /** */
        private final String device;

        Raster(final String d) {
            this.device = d;
        }

        private String getDevice() {
            return this.device;
        }
    }

    public enum Resolution {
        /** */
        DPI_100("-r600 -dDownScaleFactor=5"),
        /** */
        DPI_150("-r600 -dDownScaleFactor=4"),
        /** */
        DPI_200("-r600 -dDownScaleFactor=3"),
        /** */
        DPI_300("-r600 -dDownScaleFactor=2"),
        /** */
        DPI_600("-r600 -dDownScaleFactor=1");

        /** */
        private final String parms;

        Resolution(final String p) {
            this.parms = p;
        }

        private String getParms() {
            return this.parms;
        }
    }

    /**
     * The directory location of the created file (can be {@code null}).
     */
    private final File createHome;

    /** */
    private final Raster raster;
    /** */
    private final Resolution resolution;

    /**
     * @param rst
     *            Raster.
     * @param res
     *            Resolution.
     */
    public PdfToRasterPdf(final Raster rst, final Resolution res) {
        super(ExecMode.MULTI_THREADED);
        this.createHome = null;
        this.raster = rst;
        this.resolution = res;
    }

    /**
     *
     * @param createDir
     *            The directory location of the created file.
     * @param img
     *            Image.
     * @param res
     *            Image resolution.
     */
    public PdfToRasterPdf(final File createDir, final Raster img,
            final Resolution res) {
        super(ExecMode.MULTI_THREADED);
        this.createHome = createDir;
        this.raster = img;
        this.resolution = res;
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
                .append("-imaged.")
                .append(DocContent.getFileExtension(DocContentTypeEnum.PDF));

        return new File(builder.toString());
    }

    @Override
    protected String getOsCommand(final DocContentTypeEnum contentType,
            final File fileIn, final File fileOut) {

        final StringBuilder cmd = new StringBuilder(128);

        try {
            cmd.append(SystemInfo.Command.GS.cmd()).append(" -sOutputFile=\"")
                    .append(fileOut.getCanonicalPath()).append("\" -sDEVICE=")
                    .append(this.raster.getDevice()).append(" ")
                    .append(resolution.getParms()).append(" -dNOPAUSE -dBATCH ")
                    .append(ArgumentGS.STDOUT_TO_DEV_NULL.getArg())
                    .append(" \"").append(fileIn.getCanonicalPath())
                    .append("\"");
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

    @Override
    protected void onStdout(final String stdout) {
        // no code intended.
    }
}
