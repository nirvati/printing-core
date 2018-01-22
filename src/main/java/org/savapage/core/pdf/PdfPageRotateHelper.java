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

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class PdfPageRotateHelper {

    public static final Integer PDF_ROTATION_0 = Integer.valueOf(0);
    public static final Integer PDF_ROTATION_90 = Integer.valueOf(90);
    public static final Integer PDF_ROTATION_180 = Integer.valueOf(180);
    public static final Integer PDF_ROTATION_270 = Integer.valueOf(270);
    public static final Integer PDF_ROTATION_360 = Integer.valueOf(360);

    /**
    *
    */
    private static final class SingletonPageRotationHelper {
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
     *
     */
    private PdfPageRotateHelper() {
    }

    /**
     * Applies the requested page rotation by user to the source PDF page
     * rotation, and returns the PDF page rotation to be applied for export
     * (download).
     *
     * @param isLandscapePage
     *            If {@code true}, the mediabox rectangle size of the source PDF
     *            page indicates landscape orientation.
     * @param pdfPageRotation
     *            The current rotation of the source PDF page.
     * @param userRotation
     *            The rotation applied by the user on the visible SafePages.
     * @return The page rotation to apply to the PDF page copy.
     */
    public Integer getPageRotationForExport(final boolean isLandscapePage,
            final int pdfPageRotation, final Integer userRotation) {

        if (userRotation.equals(PDF_ROTATION_0)) {
            return Integer.valueOf(pdfPageRotation);
        }

        if (!userRotation.equals(PDF_ROTATION_90)) {
            throw new IllegalArgumentException(String.format(
                    "Rotation [%d] is not supported", userRotation.intValue()));
        }

        final Integer rotationForExport;

        if (pdfPageRotation == PDF_ROTATION_0.intValue()) {

            rotationForExport = userRotation;

        } else if (pdfPageRotation == PDF_ROTATION_90.intValue()) {

            rotationForExport = PDF_ROTATION_180;

        } else if (pdfPageRotation == PDF_ROTATION_180.intValue()) {

            rotationForExport = PDF_ROTATION_270;

        } else if (pdfPageRotation == PDF_ROTATION_270.intValue()) {

            rotationForExport = PDF_ROTATION_360;

        } else {
            throw new IllegalArgumentException(String.format(
                    "PDF page rotation [%d] is invalid", pdfPageRotation));
        }

        return rotationForExport;
    }

}
