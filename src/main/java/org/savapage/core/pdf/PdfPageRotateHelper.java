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

import java.io.IOException;

import org.savapage.core.inbox.PdfOrientationInfo;
import org.savapage.core.ipp.rules.IppRuleNumberUp;

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
    private static final int I_EFF_ORIENTATION = 3;

    /**
     * Rules to determine the key values for the {@link #RULES} array.
     */
    private static final Integer[][] RULES_RULE_KEY = { //
            { LANDSCAPE, PDF_ROTATION_270, CTM_ROTATION_270, PORTRAIT } //
    };

    // ------------------------------------------------------------------

    /**
     * Gets the PDF page <i>content</i> rotation.
     *
     * @param ctm
     *            Current Transformation Matrix of page.
     * @return The page content rotation.
     */
    public static Integer getPageContentRotation(final AffineTransform ctm) {

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
     * @param ctm
     *            The CTM of the PDF page (can be {@code null}.
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

        final Integer contentRotation;

        if (ctm == null) {
            contentRotation = PDF_ROTATION_0;
        } else {
            contentRotation = getPageContentRotation(ctm);
        }

        // Apply user rotate.
        final Integer pageRotationUser =
                Integer.valueOf(applyUserRotate(pageRotation, userRotate));

        final Integer pageOrientation;

        if (isSeenAsLandscape(landscape, pageRotationUser)) {
            pageOrientation = LANDSCAPE;
        } else {
            pageOrientation = PORTRAIT;
        }

        // Set default.
        Integer ruleOrientation = pageOrientation;

        if (!contentRotation.equals(PDF_ROTATION_0)) {

            for (final Integer[] rule : RULES_RULE_KEY) {

                if (rule[I_PAGE_ORIENTATION].equals(pageOrientation)
                        && rule[I_PAGE_ROTATION].equals(pageRotationUser)
                        && rule[I_CTM_ROTATION].equals(contentRotation)) {

                    ruleOrientation = rule[I_EFF_ORIENTATION];
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
     * Gets the page rotation for a PDF page, so its orientation will be the
     * same as the perceived orientation of the standard.
     *
     * @param reader
     *            The PDF reader.
     * @param alignToLandscape
     *            If {@code true}, page must be aligned to landscape.
     * @param nPage
     *            The 1-based page ordinal.
     * @return The rotation aligned to the standard.
     * @throws IOException
     *             When IO errors.
     */
    public static int getAlignedRotation(final PdfReader reader,
            final boolean alignToLandscape, final int nPage)
            throws IOException {

        final boolean pageLandscape =
                isLandscapePage(reader.getPageSize(nPage));
        final int pageRotation = reader.getPageRotation(nPage);

        final boolean seenLandscapeNxt =
                isSeenAsLandscape(pageLandscape, pageRotation);

        final int alignedRotation;

        if (alignToLandscape) {

            if (seenLandscapeNxt) {
                alignedRotation = pageRotation;
            } else {
                // portrait -> landscape
                if (pageLandscape) {
                    alignedRotation = ROTATION_0;
                } else {
                    alignedRotation = ROTATION_90;
                }
            }

        } else {

            if (seenLandscapeNxt) {
                // landscape -> portrait
                if (pageLandscape) {
                    alignedRotation = ROTATION_270;
                } else {
                    alignedRotation = ROTATION_0;
                }

            } else {
                alignedRotation = pageRotation;
            }

        }

        return alignedRotation;
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
     * Gets the {@link PdfOrientationInfo} to find the proper
     * {@link IppRuleNumberUp}.
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

        final Integer contentRotation;

        if (ctm == null) {
            contentRotation = PDF_ROTATION_0;
        } else {
            contentRotation = getPageContentRotation(ctm);
        }

        final PdfOrientationInfo pdfOrientation = new PdfOrientationInfo();

        pdfOrientation.setLandscape(landscape);
        pdfOrientation.setRotation(pageRotation);
        pdfOrientation.setRotate(userRotate);
        pdfOrientation.setContentRotation(contentRotation);

        return pdfOrientation;
    }

}
