/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2020 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: © 2020 Datraverse B.V. <info@datraverse.com>
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
package org.savapage.core.pdf;

import java.io.File;
import java.io.IOException;

import org.savapage.core.inbox.PdfOrientationInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.itextpdf.awt.geom.AffineTransform;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.io.RandomAccessSource;
import com.itextpdf.text.io.RandomAccessSourceFactory;
import com.itextpdf.text.pdf.PRTokeniser;
import com.itextpdf.text.pdf.PRTokeniser.TokenType;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.RandomAccessFileOrArray;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class PdfPageRotateHelper {

    /** */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(PdfPageRotateHelper.class);

    /** */
    public static final int ROTATION_0 = 0;
    /** */
    public static final int ROTATION_90 = 90;
    /** */
    public static final int ROTATION_180 = 180;
    /** */
    public static final int ROTATION_270 = 270;
    /** */
    public static final int ROTATION_360 = 360;

    /** */
    public static final Integer PDF_ROTATION_0 = Integer.valueOf(ROTATION_0);
    /** */
    public static final Integer PDF_ROTATION_90 = Integer.valueOf(ROTATION_90);
    /** */
    public static final Integer PDF_ROTATION_180 =
            Integer.valueOf(ROTATION_180);
    /** */
    public static final Integer PDF_ROTATION_270 =
            Integer.valueOf(ROTATION_270);
    /** */
    public static final Integer PDF_ROTATION_360 =
            Integer.valueOf(ROTATION_360);

    /** */
    public static final Integer CTM_ROTATION_0 = PDF_ROTATION_0;
    /** */
    public static final Integer CTM_ROTATION_90 = PDF_ROTATION_90;
    /** */
    public static final Integer CTM_ROTATION_180 = PDF_ROTATION_180;
    /** */
    public static final Integer CTM_ROTATION_270 = PDF_ROTATION_270;

    /**
     * Name of "Current Transformation Matrix" (CTM) token in PDF page content
     * stream.
     */
    private static final String PAGE_CONTENT_TOKEN_OTHER_CM = "cm";

    /**
     * The max number of numbers in a {@link #PAGE_CONTENT_TOKEN_OTHER_CM}.
     */
    private static final int PAGE_CONTENT_TOKEN_CM_MAX_NUMBERS = 6;

    /**
     * Array of array entries holding the first four "cm" page contant token
     * values for rotation 0, 90, 180 and 270.
     */
    private static final int[][] PAGE_CONTENT_CM_ROTATION_RULES = //
            new int[][] { //
                    { 1, 0, 0, 1, ROTATION_0 }, //
                    { 0, 1, -1, 0, ROTATION_90 }, //
                    { -1, 0, 0, -1, ROTATION_180 }, //
                    { 0, -1, 1, 0, ROTATION_270 } //
            };

    // ------------------------------------------------------------------
    //
    // ------------------------------------------------------------------

    /** */
    private static final Integer PORTRAIT = Integer.valueOf(0);
    /** */
    private static final Integer LANDSCAPE = Integer.valueOf(1);

    /** 0-based index. */
    private static final int I_PAGE_ORIENTATION = 0;
    /** 0-based index. */
    private static final int I_PAGE_ROTATION = 1;
    /** 0-based index. */
    private static final int I_CTM_ROTATION = 2;
    /** 0-based index. */
    private static final int I_VIEWED_ORIENTATION = 3;

    /**
     * Rules to determine the "viewed" orientation of a PDF page, depending on
     * page (rectangle) orientation, page rotation and page content orientation.
     */
    private static final Integer[][] RULES_VIEWED_ORIENTATION = { //
            //
            { LANDSCAPE, PDF_ROTATION_0, CTM_ROTATION_0, LANDSCAPE }, //
            { LANDSCAPE, PDF_ROTATION_180, CTM_ROTATION_0, LANDSCAPE }, //
            { LANDSCAPE, PDF_ROTATION_270, CTM_ROTATION_0, PORTRAIT }, //
            { LANDSCAPE, PDF_ROTATION_90, CTM_ROTATION_0, PORTRAIT }, //
            //
            { PORTRAIT, PDF_ROTATION_0, CTM_ROTATION_0, PORTRAIT }, //
            { PORTRAIT, PDF_ROTATION_180, CTM_ROTATION_0, PORTRAIT }, //
            { PORTRAIT, PDF_ROTATION_270, CTM_ROTATION_0, LANDSCAPE }, //
            { PORTRAIT, PDF_ROTATION_90, CTM_ROTATION_0, LANDSCAPE }, //
            //
            { LANDSCAPE, PDF_ROTATION_0, CTM_ROTATION_180, LANDSCAPE }, //
            { LANDSCAPE, PDF_ROTATION_180, CTM_ROTATION_180, LANDSCAPE }, //
            { LANDSCAPE, PDF_ROTATION_270, CTM_ROTATION_180, PORTRAIT }, //
            { LANDSCAPE, PDF_ROTATION_90, CTM_ROTATION_180, PORTRAIT }, //
            //
            { PORTRAIT, PDF_ROTATION_0, CTM_ROTATION_180, PORTRAIT }, //
            { PORTRAIT, PDF_ROTATION_180, CTM_ROTATION_180, PORTRAIT }, //
            { PORTRAIT, PDF_ROTATION_270, CTM_ROTATION_180, LANDSCAPE }, //
            { PORTRAIT, PDF_ROTATION_90, CTM_ROTATION_180, LANDSCAPE }, // {
            //
            { LANDSCAPE, PDF_ROTATION_0, CTM_ROTATION_270, LANDSCAPE }, //
            { LANDSCAPE, PDF_ROTATION_180, CTM_ROTATION_270, LANDSCAPE }, //
            { LANDSCAPE, PDF_ROTATION_270, CTM_ROTATION_270, PORTRAIT }, //
            { LANDSCAPE, PDF_ROTATION_90, CTM_ROTATION_270, PORTRAIT }, //
            //
            { PORTRAIT, PDF_ROTATION_0, CTM_ROTATION_270, PORTRAIT }, //
            { PORTRAIT, PDF_ROTATION_180, CTM_ROTATION_270, PORTRAIT }, //
            { PORTRAIT, PDF_ROTATION_270, CTM_ROTATION_270, LANDSCAPE }, //
            { PORTRAIT, PDF_ROTATION_90, CTM_ROTATION_270, LANDSCAPE }, // {
            //
            { LANDSCAPE, PDF_ROTATION_0, CTM_ROTATION_90, LANDSCAPE }, //
            { LANDSCAPE, PDF_ROTATION_180, CTM_ROTATION_90, LANDSCAPE }, //
            { LANDSCAPE, PDF_ROTATION_270, CTM_ROTATION_90, PORTRAIT }, //
            { LANDSCAPE, PDF_ROTATION_90, CTM_ROTATION_90, PORTRAIT }, //
            //
            { PORTRAIT, PDF_ROTATION_0, CTM_ROTATION_90, PORTRAIT }, //
            { PORTRAIT, PDF_ROTATION_180, CTM_ROTATION_90, PORTRAIT }, //
            { PORTRAIT, PDF_ROTATION_270, CTM_ROTATION_90, LANDSCAPE }, //
            { PORTRAIT, PDF_ROTATION_90, CTM_ROTATION_90, LANDSCAPE } //
    };

    // ------------------------------------------------------------------
    /**
     * {@link #RULES_ROTATE_TO_ORIENTATION} index for PDF page rotation.
     */
    private static final int IDX_R_PAGE = 0;

    /**
     * {@link #RULES_ROTATE_TO_ORIENTATION} index for PDF page content rotation.
     */
    private static final int IDX_R_CONTENT = 1;

    /**
     * {@link #RULES_ROTATE_TO_ORIENTATION} index for Portrait -> Landscape.
     */
    private static final int IDX_R_P_TO_L = 2;

    /**
     * {@link #RULES_ROTATE_TO_ORIENTATION} index for Landscape -> Portrait.
     */
    private static final int IDX_R_L_TO_P = 3;

    /**
     * Rules to determine page rotate to landscape and portrait.
     */
    private static final int[][] RULES_ROTATE_TO_ORIENTATION = { //

            // --------------------------------------
            // .. page . content . P->L .. L->P
            // --------------------------------------
            { /**/ 0, /* */ 0, /* */90, /**/270 }, // ?, OK
            { /**/ 0, /* */90, /* */90, /* */90 }, // OK, ?
            { /**/ 0, /**/180, /* */90, /**/270 }, // ?, ?
            { /**/ 0, /**/270, /**/270, /* */90 }, // ?, ?

            // --------------------------------------
            // . page .. content . P->L ... L->P
            // --------------------------------------
            { /**/180, /*  */0, /* */90, /* */90 }, // ?, ?
            { /**/180, /* */90, /**/270, /**/270 }, // ?, ?
            { /**/180, /**/180, /* */90, /* */90 }, // ?, ?
            { /**/180, /**/270, /**/270, /**/270 }, // ?, ?

            // --------------------------------------
            // . page .. content . P->L ... L->P
            // --------------------------------------
            { /**/ 90, /*  */0, /*  */0, /*  */0 }, // ?, ?
            { /**/ 90, /* */90, /**/180, /**/180 }, // OK, ?
            { /**/ 90, /**/180, /*  */0, /*  */0 }, // ?, ?
            { /**/ 90, /**/270, /*  */0, /*  */0 }, // ?, ?

            // --------------------------------------
            // . page .. content . P->L ... L->P
            // --------------------------------------
            { /**/270, /*  */0, /*  */0, /*  */0 }, // OK, ?
            { /**/270, /* */90, /*  */0, /*  */0 }, // ?, ?
            { /**/270, /**/180, /*  */0, /*  */0 }, // ?, ?
            { /**/270, /**/270, /*  */0, /*  */0 }, // ?, ?
    };

    // ------------------------------------------------------------------

    /**
     * Gets the PDF page <i>content</i> rotation.
     *
     * @param ctm
     *            Current Transformation Matrix of page. Value {@code null} is
     *            interpreted as {@link #PDF_ROTATION_0}.
     * @return The page content rotation.
     */
    public static Integer getPageContentRotation(final AffineTransform ctm) {

        if (ctm == null) {
            return PDF_ROTATION_0;
        }

        final double[] matrix =
                new double[PAGE_CONTENT_CM_ROTATION_RULES[0].length - 1];

        ctm.getMatrix(matrix);

        for (final int[] rule : PAGE_CONTENT_CM_ROTATION_RULES) {
            int match = 0;
            for (int i = 0; i < rule.length - 1; i++) {
                if (rule[i] == Double.valueOf(matrix[i]).intValue()) {
                    match++;
                }
            }
            if (match == rule.length - 1) {
                return Integer.valueOf(rule[rule.length - 1]);
            }
        }
        return PDF_ROTATION_0;
    }

    /**
    *
    */
    private PdfPageRotateHelper() {
    }

    /** */
    private static final class SingletonPageRotationHelper {
        /** */
        public static final PdfPageRotateHelper INSTANCE =
                new PdfPageRotateHelper();
    }

    /**
     *
     * @return The singleton instance.
     */
    public static PdfPageRotateHelper instance() {
        return SingletonPageRotationHelper.INSTANCE;
    }

    /**
     *
     * @param pdfPageSize
     *            The PDF page size.
     * @return {@code true} when the rectangle of PDF page indicates landscape
     *         orientation.
     */
    public static boolean isLandscapePage(final Rectangle pdfPageSize) {
        return pdfPageSize.getHeight() < pdfPageSize.getWidth();
    }

    /**
     * Gets the "Current Transformation Matrix" of PDF page content.
     *
     * @param reader
     *            The PDF reader.
     * @param nPage
     *            The 1-based page ordinal.
     * @return The CTM as {@link AffineTransform}.
     * @throws IOException
     *             When IO errors.
     */
    public static AffineTransform getPdfPageCTM(final PdfReader reader,
            final int nPage) throws IOException {

        final RandomAccessSourceFactory rasFactory =
                new RandomAccessSourceFactory();

        final RandomAccessSource ras =
                rasFactory.createSource(reader.getPageContent(nPage));

        final PRTokeniser tokeniser =
                new PRTokeniser(new RandomAccessFileOrArray(ras));

        final double[] cm = new double[PAGE_CONTENT_TOKEN_CM_MAX_NUMBERS];

        int iToken = 0;

        while (tokeniser.nextToken()) {

            if (iToken == PAGE_CONTENT_TOKEN_CM_MAX_NUMBERS
                    && (tokeniser.getTokenType() != TokenType.OTHER
                            || !tokeniser.getStringValue()
                                    .equals(PAGE_CONTENT_TOKEN_OTHER_CM))) {
                break;
            }

            if (iToken < PAGE_CONTENT_TOKEN_CM_MAX_NUMBERS) {

                if (tokeniser.getTokenType() != TokenType.NUMBER) {
                    break;
                }
                cm[iToken] = Double.parseDouble(tokeniser.getStringValue());
            }

            iToken++;

            if (iToken > PAGE_CONTENT_TOKEN_CM_MAX_NUMBERS) {
                break;
            }
        }

        if (iToken > PAGE_CONTENT_TOKEN_CM_MAX_NUMBERS) {
            return new AffineTransform(cm);
        }
        return null;
    }

    /**
     * Is PDF page seen in landscape orientation?
     *
     * @param info
     *            The PDF orientation info.
     * @return {@code true} when seen in landscape.
     * @throws IOException
     *             When IO errors.
     */
    public static boolean isSeenAsLandscape(final PdfOrientationInfo info) {

        final int contentRotation;
        final int pageRotation;

        if (info.getContentRotation() == null) {
            contentRotation = ROTATION_0;
        } else {
            contentRotation = info.getContentRotation().intValue();
        }

        if (info.getRotation() == null) {
            pageRotation = ROTATION_0;
        } else {
            pageRotation = info.getRotation().intValue();
        }

        return isSeenAsLandscape(contentRotation, pageRotation,
                info.getLandscape(), info.getRotate());
    }

    /**
     * Is PDF page seen in landscape orientation?
     *
     * @param ctm
     *            The CTM of the PDF page (can be {@code null}).
     * @param pageRotation
     *            The PDF page rotation.
     * @param landscape
     *            {@code true} when page rectangle has landscape orientation.
     * @param userRotate
     *            Rotation requested by user.
     * @return {@code true} when seen in landscape.
     * @throws IOException
     *             When IO errors.
     */
    public static boolean isSeenAsLandscape(final AffineTransform ctm,
            final int pageRotation, final boolean landscape,
            final Integer userRotate) {

        return isSeenAsLandscape(getPageContentRotation(ctm).intValue(),
                pageRotation, landscape, userRotate);
    }

    /**
     * Is PDF page seen in landscape orientation?
     *
     * @param contentRotation
     *            The PDF page content rotation.
     * @param pageRotation
     *            The PDF page rotation.
     * @param landscape
     *            {@code true} when page rectangle has landscape orientation.
     * @param userRotate
     *            Rotation requested by user.
     * @return {@code true} when seen in landscape.
     * @throws IOException
     *             When IO errors.
     */
    public static boolean isSeenAsLandscape(final int contentRotation,
            final int pageRotation, final boolean landscape,
            final Integer userRotate) {

        // Apply user rotate.
        final Integer pageRotationUser =
                Integer.valueOf(applyUserRotate(pageRotation, userRotate));

        final Integer pageOrientation;
        if (landscape) {
            pageOrientation = LANDSCAPE;
        } else {
            pageOrientation = PORTRAIT;
        }

        final Integer pageOrientationSeen;

        if (isSeenAsLandscape(landscape, pageRotationUser)) {
            pageOrientationSeen = LANDSCAPE;
        } else {
            pageOrientationSeen = PORTRAIT;
        }

        // Set default.
        Integer ruleOrientation = pageOrientationSeen;

        if (contentRotation != ROTATION_0) {

            for (final Integer[] rule : RULES_VIEWED_ORIENTATION) {

                if (rule[I_PAGE_ORIENTATION].equals(pageOrientation)
                        && rule[I_PAGE_ROTATION].equals(pageRotationUser)
                        && rule[I_CTM_ROTATION].equals(contentRotation)) {

                    ruleOrientation = rule[I_VIEWED_ORIENTATION];
                    break;
                }
            }
        }

        return ruleOrientation.equals(LANDSCAPE);
    }

    /**
     *
     * @param pageLandscape
     * @param pageRotation
     * @return {@code true} when seen as landscape on paper or in viewer.
     */
    public static boolean isSeenAsLandscape(final boolean pageLandscape,
            final int pageRotation) {

        if (pageLandscape) {
            return pageRotation == ROTATION_0 || pageRotation == ROTATION_180
                    || pageRotation == ROTATION_360;
        }
        return pageRotation == ROTATION_90 || pageRotation == ROTATION_270;
    }

    /**
     * Is PDF page seen in landscape orientation?
     *
     * @param pdfFile
     *            The PDF file.
     * @param page
     *            The 1-based page ordinal.
     * @param userRotate
     *            Rotation requested by user.
     * @return {@code true} when seen in landscape.
     * @throws IOException
     *             When IO errors.
     */
    public static boolean isSeenAsLandscape(final File pdfFile, final int page,
            final int userRotate) throws IOException {

        PdfReader readerWlk = null;

        try {
            readerWlk = new PdfReader(pdfFile.getAbsolutePath());

            readerWlk.selectPages(String.valueOf(page));

            final int firstPage = 1;

            final AffineTransform ctm = getPdfPageCTM(readerWlk, firstPage);
            final int page1Rotation = readerWlk.getPageRotation(firstPage);
            final boolean page1Landscape =
                    isLandscapePage(readerWlk.getPageSize(firstPage));

            return isSeenAsLandscape(ctm, page1Rotation, page1Landscape,
                    Integer.valueOf(userRotate));

        } finally {

            if (readerWlk != null) {
                readerWlk.close();
            }
        }
    }

    /**
     * Is PDF page seen in landscape orientation?
     *
     * @param reader
     *            The PDF file.
     * @param nPage
     *            The 1-based page ordinal.
     * @return {@code true} when seen in landscape.
     * @throws IOException
     *             When IO errors.
     */
    public static boolean isSeenAsLandscape(final PdfReader reader,
            final int nPage) throws IOException {

        final AffineTransform ctm = getPdfPageCTM(reader, nPage);
        final int page1Rotation = reader.getPageRotation(nPage);
        final boolean page1Landscape =
                isLandscapePage(reader.getPageSize(nPage));

        return isSeenAsLandscape(ctm, page1Rotation, page1Landscape,
                Integer.valueOf(0));
    }

    /**
     * Gets the new PDF page rotation to rotate to landscape or portait.
     *
     * @param toLandscape
     *            If {@code true}, rotate to landscape.
     * @param pageLandscape
     *            If {@code true}, PDF page orientation is landscape.
     * @param pageRotation
     *            The current PDF page rotation.
     * @param contentRotation
     *            The PDF page <i>content</i> rotation.
     * @return The new PDF page rotation.
     */
    public static int rotateToOrientationSeen(final boolean toLandscape,
            final boolean pageLandscape, final int pageRotation,
            final int contentRotation) {

        for (final int[] rule : RULES_ROTATE_TO_ORIENTATION) {

            if (rule[IDX_R_PAGE] == pageRotation
                    && rule[IDX_R_CONTENT] == contentRotation) {

                final int rotate;

                if (rule.length < IDX_R_L_TO_P) {
                    rotate = pageRotation;
                } else if (toLandscape) {
                    rotate = rule[IDX_R_P_TO_L];
                } else {
                    rotate = rule[IDX_R_L_TO_P];
                }

                LOGGER.debug(
                        "To landscape [{}] From landscape [{}] page [{}]"
                                + " content [{}] -> page [{}]",
                        toLandscape, pageLandscape, pageRotation,
                        contentRotation, rotate);

                return rotate;
            }
        }

        throw new IllegalArgumentException(String.format(
                "No rule found for [%d, %d]", pageRotation, contentRotation));
    }

    /**
     * Gets the page rotation for a PDF page, so its orientation will be the
     * same as the perceived orientation of the required alignment.
     *
     * @param reader
     *            The PDF reader.
     * @param alignToLandscape
     *            Required alignment. If {@code true}, page must be aligned to
     *            landscape.
     * @param nPage
     *            The 1-based page ordinal.
     * @return The rotation aligned to the standard.
     * @throws IOException
     *             When IO errors.
     */
    public static int getAlignedRotation(final PdfReader reader,
            final boolean alignToLandscape, final int nPage)
            throws IOException {

        final AffineTransform ctm = getPdfPageCTM(reader, nPage);

        final boolean pageLandscape =
                isLandscapePage(reader.getPageSize(nPage));

        final int pageRotation = reader.getPageRotation(nPage);

        final boolean seenLandscapeNxt = isSeenAsLandscape(ctm, pageRotation,
                pageLandscape, Integer.valueOf(0));

        if ((alignToLandscape && seenLandscapeNxt)
                || (!alignToLandscape && !seenLandscapeNxt)) {
            return pageRotation;
        }

        return rotateToOrientationSeen(alignToLandscape, pageLandscape,
                pageRotation, getPageContentRotation(ctm));
    }

    /**
     *
     * @param rotation
     *            The candidate rotation.
     * @return {code true} when valid.
     */
    public static boolean isPdfRotationValid(final Integer rotation) {
        return rotation.equals(PDF_ROTATION_0)
                || rotation.equals(PDF_ROTATION_90)
                || rotation.equals(PDF_ROTATION_180)
                || rotation.equals(PDF_ROTATION_270)
                || rotation.equals(PDF_ROTATION_360);
    }

    /**
     * Applies the user requested page rotation to the PDF page rotation, and
     * returns the resulting PDF page rotation.
     *
     * @param pageRotation
     *            The current rotation of the source PDF page.
     * @param userRotate
     *            Rotation requested by SavaPage user.
     * @return The page rotation to apply to the PDF page copy.
     */
    public static int applyUserRotate(final int pageRotation,
            final Integer userRotate) {

        if (userRotate.equals(PDF_ROTATION_0)) {
            return Integer.valueOf(pageRotation);
        }

        if (!userRotate.equals(PDF_ROTATION_90)) {
            throw new IllegalArgumentException(String.format(
                    "Rotation [%d] is not supported", userRotate.intValue()));
        }

        if (pageRotation == PDF_ROTATION_0.intValue()) {

            return userRotate;

        } else if (pageRotation == PDF_ROTATION_90.intValue()) {

            return PDF_ROTATION_180;

        } else if (pageRotation == PDF_ROTATION_180.intValue()) {

            return PDF_ROTATION_270;

        } else if (pageRotation == PDF_ROTATION_270.intValue()) {

            return PDF_ROTATION_0;
        }

        throw new IllegalArgumentException(String
                .format("PDF page rotation [%d] is invalid", pageRotation));
    }

    /**
     * Gets the {@link PdfOrientationInfo} by applying user rotate.
     *
     * @param ctm
     *            The CTM of the PDF page (can be {@code null}.
     * @param pageRotation
     *            The PDF page rotation.
     * @param landscape
     *            {@code true} when page has landscape orientation.
     * @param userRotate
     *            Rotation requested by user.
     * @return The PDF orientation info.
     * @throws IOException
     *             When IO errors.
     */
    public static PdfOrientationInfo getOrientationInfo(
            final AffineTransform ctm, final int pageRotation,
            final boolean landscape, final Integer userRotate)
            throws IOException {

        final PdfOrientationInfo pdfOrientation = new PdfOrientationInfo();

        pdfOrientation.setLandscape(landscape);
        pdfOrientation.setRotation(applyUserRotate(pageRotation, userRotate));
        pdfOrientation.setRotate(Integer.valueOf(0));
        pdfOrientation.setContentRotation(getPageContentRotation(ctm));

        return pdfOrientation;
    }

}
