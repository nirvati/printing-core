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
package org.savapage.core.pdf;

import java.io.File;
import java.util.List;

import org.savapage.core.inbox.PdfOrientationInfo;
import org.savapage.core.ipp.rules.IppRuleNumberUp;

/**
 * Information about a created PDF file.
 *
 * @author Rijk Ravestein
 *
 */
public final class PdfCreateInfo {

    /**
     * The created PDF file.
     */
    private File pdfFile;

    /**
     * The total number of blank filler pages appended between logical sub-jobs
     * (proxy print only).
     */
    private int blankFillerPages;

    /**
     * The number of pages of logical sub-jobs. <b>Note</b>: Blank filler pages
     * are <i>not</i> included in the count. When {@code null}, no logical
     * sub-jobs are defined.
     */
    private List<Integer> logicalJobPages;

    /**
     * The {@link PdfOrientationInfo} of the first page, used to find the
     * {@link IppRuleNumberUp}.
     */
    private PdfOrientationInfo pdfOrientationInfo;

    /**
     *
     * @param file
     *            The created PDF file.
     */
    public PdfCreateInfo(final File file) {
        this.pdfFile = file;
        this.blankFillerPages = 0;
    }

    /**
     *
     * @return The created PDF file.
     */
    public File getPdfFile() {
        return pdfFile;
    }

    /**
     *
     * @param pdfFile
     *            The created PDF file.
     */
    public void setPdfFile(File pdfFile) {
        this.pdfFile = pdfFile;
    }

    /**
     *
     * @return The total number of blank filler pages appended between logical
     *         jobs (proxy print only).
     */
    public int getBlankFillerPages() {
        return blankFillerPages;
    }

    /**
     *
     * @param blankFillerPages
     *            The total number of blank filler pages appended between
     *            logical jobs (proxy print only).
     */
    public void setBlankFillerPages(int blankFillerPages) {
        this.blankFillerPages = blankFillerPages;
    }

    /**
     *
     * @return The number of pages of logical sub-jobs. <b>Note</b>: Blank
     *         filler pages are <i>not</i> included in the count. When
     *         {@code null}, no logical sub-jobs are defined.
     */
    public List<Integer> getLogicalJobPages() {
        return logicalJobPages;
    }

    /**
     *
     * @param logicalJobPages
     *            The number of pages of logical sub-jobs. <b>Note</b>: Blank
     *            filler pages are <i>not</i> included in the count. When
     *            {@code null}, no logical sub-jobs are defined.
     */
    public void setLogicalJobPages(List<Integer> logicalJobPages) {
        this.logicalJobPages = logicalJobPages;
    }

    /**
     * @return The {@link PdfOrientationInfo} of the first page, used to find
     *         the {@link IppRuleNumberUp} (can be {@code null}).
     */
    public PdfOrientationInfo getPdfOrientationInfo() {
        return pdfOrientationInfo;
    }

    /**
     * @param orientationInfo
     *            The {@link PdfOrientationInfo} of the first page, used to find
     *            the {@link IppRuleNumberUp}.
     */
    public void
            setPdfOrientationInfo(final PdfOrientationInfo orientationInfo) {
        this.pdfOrientationInfo = orientationInfo;
    }

}
