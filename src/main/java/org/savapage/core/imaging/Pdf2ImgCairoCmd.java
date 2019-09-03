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
package org.savapage.core.imaging;

import java.io.File;

import org.savapage.core.pdf.PdfPageRotateHelper;
import org.savapage.core.system.SystemInfo;

/**
 * Command using Cairo.
 *
 * @author Rijk Ravestein
 *
 */
public final class Pdf2ImgCairoCmd implements Pdf2ImgCommand {

    /**
     * Do not use lower value.
     */
    public static final int RESOLUTION_FOR_THUMNAIL = 24;
    /** */
    public static final int RESOLUTION_FOR_BROWSER = 72;

    /** */
    public enum ImgType {
        /** */
        PNG("-png"),
        /** */
        JPEG("-jpeg");

        private final String cmdOpt;

        public String getCmdOpt() {
            return this.cmdOpt;
        }

        ImgType(final String opt) {
            this.cmdOpt = opt;
        }
    }

    /**
     * .
     */
    private static final int STRINGBUILDER_CAPACITY = 256;

    /**
     * The image type.
     */
    private final ImgType imgType;

    /**
     * @param img
     *            The image type.
     */
    public Pdf2ImgCairoCmd(final ImgType img) {
        this.imgType = img;
    }

    @Override
    public String createCommand(final File pdfFile, final boolean landscape,
            final int rotation, final File imgFile, final int pageOrdinal,
            final int resolution, final int rotate) {

        final int pageOneBased = pageOrdinal + 1;

        final StringBuilder cmdBuffer =
                new StringBuilder(STRINGBUILDER_CAPACITY);

        cmdBuffer.append(SystemInfo.Command.PDFTOCAIRO.cmd()).append(" ")
                .append(this.imgType.getCmdOpt()).append(" -r ")
                .append(resolution).append(" -f ").append(pageOneBased)
                .append(" -l ").append(pageOneBased).append(" -singlefile");

        cmdBuffer.append(" \"").append(pdfFile.getAbsolutePath()).append("\"");

        /*
         * Apply user requested rotate.
         */
        final Integer rotate2Apply = Integer.valueOf(rotate);

        if (rotate2Apply.equals(PdfPageRotateHelper.PDF_ROTATION_0)) {
            cmdBuffer.append(" - > ");
        } else {
            cmdBuffer.append(" - | ").append(SystemInfo.Command.CONVERT.cmd())
                    .append(" -rotate ").append(rotate2Apply).append(" - ");
        }
        cmdBuffer.append("\"").append(imgFile.getAbsolutePath()).append("\"");

        final String command = cmdBuffer.toString();

        return command;
    }

}
