/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2017 Datraverse B.V.
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

/**
 * Command using Poppler.
 * <p>
 * IMPORTANT: older versions of {@code pdftoppm}, like 0.12.4 (c) 2009, do NOT
 * have the {@code -jpeg} switch.
 * </p>
 *
 * @author Rijk Ravestein
 *
 */
public final class Pdf2PngPopplerCmd implements Pdf2ImgCommandExt {

    /**
     * Advised resolution for display on screen.
     */
    public static final int RESOLUTION_FOR_SCREEN = 72;

    /**
     * .
     */
    private static final int STRINGBUILDER_CAPACITY = 256;

    /**
     * Enable or disable font anti-aliasing.
     */
    private final boolean antiAliasingFont;

    /**
     * Enable or disable vector anti-aliasing.
     */
    private final boolean antiAliasingVector;

    /**
     *
     */
    public Pdf2PngPopplerCmd() {
        this.antiAliasingFont = true;
        this.antiAliasingVector = true;
    }

    /**
     *
     */
    public Pdf2PngPopplerCmd(final boolean antiAliasing) {
        this.antiAliasingFont = antiAliasing;
        this.antiAliasingVector = true;
    }

    @Override
    public String createCommand(final File pdfFile, final boolean landscape,
            final int rotation, final File imgFile, final int pageOrdinal,
            final int resolution, final int rotate, final Integer imgWidth) {

        final int pageOneBased = pageOrdinal + 1;

        final StringBuilder cmdBuffer =
                new StringBuilder(STRINGBUILDER_CAPACITY);

        cmdBuffer.append("pdftoppm -png -r ").append(resolution).append(" -f ")
                .append(pageOneBased).append(" -l ").append(pageOneBased);

        if (imgWidth != null) {
            cmdBuffer.append(" -scale-to ").append(imgWidth);
        }

        if (!this.antiAliasingFont) {
            cmdBuffer.append(" -aa no ");
        }

        if (!this.antiAliasingVector) {
            cmdBuffer.append(" -aaVector no ");
        }

        cmdBuffer.append(" \"").append(pdfFile.getAbsolutePath()).append("\"");

        /*
         * Apply user requested rotate.
         */
        final Integer rotate2Apply = Integer.valueOf(rotate);

        if (rotate2Apply.equals(PdfPageRotateHelper.PDF_ROTATION_0)) {
            cmdBuffer.append(" > ");
        } else {
            cmdBuffer.append(" | convert -rotate ").append(rotate2Apply)
                    .append(" - ");
        }
        cmdBuffer.append("\"").append(imgFile.getAbsolutePath()).append("\"");

        final String command = cmdBuffer.toString();

        return command;
    }

    @Override
    public String createCommand(final File pdfFile, final boolean landscape,
            final int rotation, final File imgFile, final int pageOrdinal,
            final int resolution, final int rotate) {
        return this.createCommand(pdfFile, landscape, rotation, imgFile,
                pageOrdinal, resolution, rotate, null);
    }

}
