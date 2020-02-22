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
package org.savapage.core.imaging;

import java.io.File;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.savapage.core.pdf.PdfPageRotateHelper;
import org.savapage.core.system.ICommandStrategy;
import org.savapage.core.system.SystemInfo;

/**
 * Create image from PDF using {@link SystemInfo.Command#PDFTOCAIRO}.
 *
 * @author Rijk Ravestein
 *
 */
public final class Pdf2ImgCairoCmd implements Pdf2ImgCommand, ICommandStrategy {

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

        /**
         * CLI option value.
         */
        private final String cmdOpt;

        /**
         * @param opt
         *            CLI option value.
         */
        ImgType(final String opt) {
            this.cmdOpt = opt;
        }

        /**
         * @return CLI option value.
         */
        public String getCmdOpt() {
            return this.cmdOpt;
        }

        /**
         * @return File extension, e.g. "png".
         */
        public String getFileExt() {
            return this.cmdOpt.substring(1);
        }
    }

    /**
     * Command strategy.
     */
    private enum Strategy {
        /**
         * Use stdout/stdin redirection.
         */
        STREAM,
        /**
         * Split in logical AND commands.
         */
        SPLIT;

        /** */
        static final String PROBE_V_0_26_5 = "version 0.26.5";

        /**
         * Determine strategy depending on command version.
         *
         * @return The strategy.
         */
        static Strategy getStrategy() {
            /*
             *
             */
            if (SystemInfo.getCommandVersionRegistry().getPdfToCairo()
                    .contains(PROBE_V_0_26_5)) {
                return Strategy.SPLIT;
            }
            return Strategy.STREAM;
        }
    }

    /** */
    private static final class StrategyHolder {
        /**
         * The singleton.
         */
        private static final Strategy INSTANCE = Strategy.getStrategy();
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
    public String createCommand(final CreateParms parms) {

        final int pageOneBased = parms.getPageOrdinal() + 1;

        final Integer rotate2Apply = Integer.valueOf(parms.getRotate());
        final boolean isRotateZero =
                rotate2Apply.equals(PdfPageRotateHelper.PDF_ROTATION_0);

        final StringBuilder cmdBuffer =
                new StringBuilder(STRINGBUILDER_CAPACITY);

        cmdBuffer.append(String.format("%s %s -r %d -f %d -l %d ",
                SystemInfo.Command.PDFTOCAIRO.cmd(), this.imgType.getCmdOpt(),
                parms.getResolution(), pageOneBased, pageOneBased));

        if (StrategyHolder.INSTANCE == Strategy.STREAM) {

            cmdBuffer.append(String.format("-singlefile \"%s\"",
                    parms.getPdfFile().getAbsolutePath()));

            if (isRotateZero) {
                cmdBuffer.append(String.format(" - >",
                        parms.getPdfFile().getAbsolutePath()));
            } else {
                cmdBuffer.append(String.format(" - | %s -rotate %d -",
                        SystemInfo.Command.CONVERT.cmd(),
                        rotate2Apply.intValue()));
            }

            cmdBuffer.append(" \"").append(parms.getImgFile().getAbsolutePath())
                    .append("\"");

        } else {

            final String imageFileOutTemplate =
                    String.format("%s%c_pdftocairo_split_%s",
                            StringUtils.defaultString(
                                    parms.getImgFile().getParent()),
                            File.separatorChar, UUID.randomUUID().toString());

            final String imageFileOutProduced =
                    String.format("%s-%s.%s", imageFileOutTemplate,
                            StringUtils.leftPad(String.valueOf(pageOneBased),
                                    String.valueOf(parms.getNumberOfPages())
                                            .length(),
                                    '0'),
                            this.imgType.getFileExt());

            cmdBuffer.append(String.format("\"%s\" \"%s\"",
                    parms.getPdfFile().getAbsolutePath(),
                    imageFileOutTemplate));

            if (isRotateZero) {
                cmdBuffer.append(String.format(" && mv \"%s\" \"%s\"",
                        imageFileOutProduced,
                        parms.getImgFile().getAbsolutePath()));
            } else {
                cmdBuffer.append(String.format(" && %s -rotate %d %s %s",
                        SystemInfo.Command.CONVERT.cmd(),
                        rotate2Apply.intValue(), imageFileOutProduced,
                        parms.getImgFile().getAbsolutePath()));
                cmdBuffer.append(
                        String.format(" && rm %s", imageFileOutProduced));
            }
        }

        final String command = cmdBuffer.toString();

        return command;
    }

}
