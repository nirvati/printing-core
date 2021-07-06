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
import java.io.OutputStream;
import java.util.UUID;

import javax.print.attribute.standard.MediaSizeName;

import org.apache.commons.io.FileUtils;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.system.SystemInfo;
import org.savapage.core.system.SystemInfo.Command;
import org.savapage.core.util.MediaUtils;

/**
 * Create a PDF file from HTML using {@link Command#WKHTMLTOPDF}.
 *
 * @author Rijk Ravestein
 *
 */
public final class WkHtmlToPdf extends AbstractDocFileConverter
        implements IDocStreamConverter, IStreamConverter {

    /** */
    private static final String HTML_PAGE_SIZE_A4 = "A4";
    /** */
    private static final String HTML_PAGE_SIZE_LETTER = "Letter";

    /**
     * @return {@code true} if this convertor is available and enabled.
     */
    public static boolean isAvailable() {
        return SystemInfo.isWkHtmlToPdfInstalled() && ConfigManager.instance()
                .isConfigValue(Key.SYS_CMD_WKHTMLTOPDF_ENABLE);
    }

    /** */
    public WkHtmlToPdf() {
        super(ExecMode.MULTI_THREADED);
    }

    @Override
    public boolean notifyStdOutMsg() {
        return this.hasStdout();
    }

    @Override
    protected ExecType getExecType() {
        return ExecType.ADVANCED;
    }

    @Override
    protected String getOsCommand(final DocContentTypeEnum contentType,
            final File fileIn, final File filePdf) {

        final MediaSizeName mediaSizeName = MediaUtils.getDefaultMediaSize();
        final String pageSize;
        if (mediaSizeName == MediaSizeName.NA_LETTER) {
            pageSize = HTML_PAGE_SIZE_LETTER;
        } else {
            pageSize = HTML_PAGE_SIZE_A4;
        }

        // margins 10 mm: -B 10 -L 10 -R 10 -T 10
        return Command.WKHTMLTOPDF.cmdLineExt("--quiet",
                "-B 10 -L 10 -R 10 -T 10", "--page-size ".concat(pageSize),
                fileIn.getAbsolutePath(), filePdf.getAbsolutePath());
    }

    @Override
    protected File getOutputFile(final File fileIn) {
        return getFileSibling(fileIn, DocContentTypeEnum.PDF);
    }

    @Override
    public long convert(final DocContentTypeEnum contentType,
            final DocInputStream istr, final File filePdf) throws Exception {

        final File fileHtmlTemp =
                getFileSibling(filePdf, DocContentTypeEnum.HTML);

        try {
            FileUtils.copyToFile(istr, fileHtmlTemp);
            this.convert(contentType, fileHtmlTemp);
        } finally {
            fileHtmlTemp.delete();
        }

        return istr.getBytesRead();
    }

    @Override
    public long convert(final DocContentTypeEnum contentType,
            final DocInputStream istr, final OutputStream ostr)
            throws Exception {

        final File filePdfTemp = File.createTempFile(
                "temp-".concat(UUID.randomUUID().toString()), ".pdf");
        try {
            this.convert(contentType, istr, filePdfTemp);
            FileUtils.copyFile(filePdfTemp, ostr);
        } finally {
            filePdfTemp.delete();
        }

        return istr.getBytesRead();
    }

}
