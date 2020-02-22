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

import org.savapage.core.services.helpers.InboxPageImageInfo;

/**
 *
 * @author Rijk Ravestein
 *
 */
public interface Pdf2ImgCommand {

    /** */
    class CreateParms {

        /**
         * The PDF source {@link File}.
         */
        private File pdfFile;

        /**
         * Number of pages of PDF file.
         */
        private int numberOfPages;

        /**
         * The zero-based ordinal page number in the PDF document.
         */
        private int pageOrdinal;

        /**
         * {@code true} if the PDF orientation of the PDF inbox document is
         * landscape.
         */
        private boolean landscape;

        /**
         * The PDF rotation of the PDF inbox document.
         */
        private int rotation;

        /**
         * The rotation on the PDF document set by the User.
         */
        private int rotate;

        /**
         * The image target {@link File}.
         */
        private File imgFile;

        /**
         * The resolution (density) in DPI (e.g. 72, 150, 300, 600).
         */
        private int resolution;

        /**
         * @return The PDF source {@link File}.
         */
        public File getPdfFile() {
            return pdfFile;
        }

        /**
         * @param file
         *            The PDF source {@link File}.
         */
        public void setPdfFile(File file) {
            this.pdfFile = file;
        }

        /**
         * @return Number of pages of PDF file.
         */
        public int getNumberOfPages() {
            return numberOfPages;
        }

        /**
         * @param numberOfPages
         *            Number of pages of PDF file.
         */
        public void setNumberOfPages(int numberOfPages) {
            this.numberOfPages = numberOfPages;
        }

        /**
         * @return The zero-based ordinal page number in the PDF document.
         */
        public int getPageOrdinal() {
            return pageOrdinal;
        }

        /**
         * @param pageOrdinal
         *            The zero-based ordinal page number in the PDF document.
         */
        public void setPageOrdinal(int pageOrdinal) {
            this.pageOrdinal = pageOrdinal;
        }

        /**
         *
         * @return {@code true} if the PDF orientation of the PDF inbox document
         *         is landscape.
         */
        public boolean isLandscape() {
            return landscape;
        }

        /**
         *
         * @param landscape
         *            {@code true} if the PDF orientation of the PDF inbox
         *            document is landscape.
         */
        public void setLandscape(boolean landscape) {
            this.landscape = landscape;
        }

        /**
         * @return The PDF rotation of the PDF inbox document.
         */
        public int getRotation() {
            return rotation;
        }

        /**
         * @param rotation
         *            The PDF rotation of the PDF inbox document.
         */
        public void setRotation(int rotation) {
            this.rotation = rotation;
        }

        /**
         * @return The rotation on the PDF document set by the User.
         */
        public int getRotate() {
            return rotate;
        }

        /**
         * @param rotate
         *            The rotation on the PDF document set by the User.
         */
        public void setRotate(int rotate) {
            this.rotate = rotate;
        }

        /**
         * @return The image target {@link File}.
         */
        public File getImgFile() {
            return imgFile;
        }

        /**
         * @param imgFile
         *            The image target {@link File}.
         */
        public void setImgFile(File imgFile) {
            this.imgFile = imgFile;
        }

        /**
         * @return The resolution (density) in DPI (e.g. 72, 150, 300, 600).
         */
        public int getResolution() {
            return resolution;
        }

        /**
         * @param resolution
         *            The resolution (density) in DPI (e.g. 72, 150, 300, 600).
         */
        public void setResolution(int resolution) {
            this.resolution = resolution;
        }

        /**
         *
         * @param srcFile
         * @param imgFile
         * @param pageImageInfo
         * @param pageOrdinal
         * @param resolution
         * @return
         */
        public static CreateParms create(final File srcFile, final File imgFile,
                final InboxPageImageInfo pageImageInfo, final int pageOrdinal,
                final int resolution) {

            final CreateParms parms = new CreateParms();

            parms.setImgFile(imgFile);
            parms.setLandscape(pageImageInfo.isLandscape());
            parms.setNumberOfPages(pageImageInfo.getNumberOfPages());
            parms.setPageOrdinal(pageOrdinal);
            parms.setPdfFile(srcFile);
            parms.setResolution(resolution);
            parms.setRotate(pageImageInfo.getRotate());
            parms.setRotation(pageImageInfo.getRotation());

            return parms;
        }
    }

    /**
     * Creates an OS command for creating an (graphic) image of a page in a PDF
     * document.
     *
     * @param parms
     *            Parameter object.
     * @return The OS command string.
     */
    String createCommand(CreateParms parms);

}
