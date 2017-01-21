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
package org.savapage.core.pdf;

import java.util.HashMap;
import java.util.Map;

import org.savapage.core.SpException;

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
     * Map with key (the rotation of PDF page) and Map value with key the user
     * SafePage rotation and value, the rotation to apply (overwrite) to the PDF
     * page.
     */
    private final Map<Integer, Map<Integer, Integer>> rotationMapPortraitPrint;

    /**
     *
     */
    private final Map<Integer, Map<Integer, Integer>> rotationMapLandscapePrint;

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
     */
    private PdfPageRotateHelper() {
        this.rotationMapPortraitPrint = createRotationMapPortraitPrint();
        this.rotationMapLandscapePrint = createRotationMapLandscapePrint();
    }

    /**
     * Gets the PDF page rotation to be applied for printing.
     *
     * @param isLandscapePage
     *            If {@code true}, the Rectangle size of the source PDF page
     *            indicates landscape orientation.
     * @param pageRotation
     *            The current rotation of the source PDF page.
     * @param userRotation
     *            The rotation applied by the user on the visible SafePages. If
     *            {@code null}, no rotation is applied.
     * @return The page rotation to apply to the PDF page copy. If {@code null},
     *         no rotation has to be applied.
     */
    public Integer getPageRotationForPrinting(final boolean isLandscapePage,
            final Integer pageRotation, final Integer userRotation) {

        final Integer safePageRotation;

        if (userRotation == null) {
            safePageRotation = PDF_ROTATION_0;
        } else {
            safePageRotation = userRotation;
        }

        final Map<Integer, Map<Integer, Integer>> rotationMap;

        if (isLandscapePage) {
            rotationMap = this.rotationMapLandscapePrint;
        } else {
            rotationMap = this.rotationMapPortraitPrint;
        }

        final Integer rotate =
                rotationMap.get(pageRotation).get(safePageRotation);

        return rotate;
    }

    /**
     * Gets the PDF page rotation to be applied for export (download).
     *
     * @param isLandscapePage
     *            If {@code true}, the Rectangle size of the source PDF page
     *            indicates landscape orientation.
     * @param pageRotation
     *            The current rotation of the source PDF page.
     * @param userRotation
     *            The rotation applied by the user on the visible SafePages. If
     *            {@code null}, no rotation is applied.
     * @return The page rotation to apply to the PDF page copy. If {@code null},
     *         no rotation has to be applied.
     */
    public Integer getPageRotationForExport(final boolean isLandscapePage,
            final int pageRotation, final Integer userRotation) {

        if (userRotation != null && !userRotation.equals(PDF_ROTATION_0)) {
            return userRotation;
        }

        if (userRotation != null && userRotation.equals(PDF_ROTATION_0)) {
            return pageRotation;
        }

        if (!isLandscapePage) {
            return userRotation;
        }

        if (pageRotation == PDF_ROTATION_0.intValue()) {

            return PDF_ROTATION_0;

        } else if (pageRotation == PDF_ROTATION_90.intValue()) {

            return PDF_ROTATION_180;

        } else if (pageRotation == PDF_ROTATION_180.intValue()) {

            return PDF_ROTATION_90; // ??

        } else if (pageRotation == PDF_ROTATION_270.intValue()) {

            return PDF_ROTATION_360; // works, but why?

        } else {
            throw new SpException("Unsupported PDF page rotation.");
        }
    }

    /**
     * Creates a map from array of key/value pairs.
     *
     * @param keyValuePairs
     *            The key/value pairs.
     * @return the map.
     */
    private static Map<Integer, Integer>
            createMap(final Integer[][] keyValuePairs) {
        final Map<Integer, Integer> map = new HashMap<>();
        for (final Integer[] keyValue : keyValuePairs) {
            map.put(keyValue[0], keyValue[1]);
        }
        return map;
    }

    /**
     *
     * @return The rotations map for portrait PDF source page.
     */
    private static Map<Integer, Map<Integer, Integer>>
            createRotationMapPortraitPrint() {

        final Map<Integer, Map<Integer, Integer>> rotationMap = new HashMap<>();

        rotationMap.put(PDF_ROTATION_0,
                createMap(new Integer[][] {
                        // SafePage PORTRAIT
                        { PDF_ROTATION_0, PDF_ROTATION_0 },
                        // SafePage LANDSCAPE
                        { PDF_ROTATION_90, PDF_ROTATION_180 },
                        // SafePage PORTRAIT (reverse)
                        { PDF_ROTATION_180, PDF_ROTATION_180 },
                        // SafePage LANDSCAPE (reverse)
                        { PDF_ROTATION_270, PDF_ROTATION_0 },
                        // SafePage PORTRAIT
                        { PDF_ROTATION_360, PDF_ROTATION_0 } }));

        rotationMap.put(PDF_ROTATION_90,
                createMap(new Integer[][] {
                        // SafePage LANDSCAPE
                        { PDF_ROTATION_0, PDF_ROTATION_180 },
                        // SafePage PORTRAIT (reverse).
                        { PDF_ROTATION_90, PDF_ROTATION_180 },
                        // SafePage LANDSCAPE (reverse)
                        { PDF_ROTATION_180, PDF_ROTATION_0 },
                        // SafePage PORTRAIT
                        { PDF_ROTATION_270, PDF_ROTATION_0 },
                        // SafePage LANDSCAPE
                        { PDF_ROTATION_360, PDF_ROTATION_180 } }));

        rotationMap.put(PDF_ROTATION_180,
                createMap(new Integer[][] {
                        // SafePage PORTRAIT (reverse)
                        { PDF_ROTATION_0, PDF_ROTATION_180 },
                        // SafePage LANDSCAPE (reverse)
                        { PDF_ROTATION_90, PDF_ROTATION_0 },
                        // SafePage PORTRAIT
                        { PDF_ROTATION_180, PDF_ROTATION_0 },
                        // SafePage LANDSCAPE
                        { PDF_ROTATION_270, PDF_ROTATION_180 },
                        // SafePage PORTRAIT (reverse)
                        { PDF_ROTATION_360, PDF_ROTATION_180 } }));

        rotationMap.put(PDF_ROTATION_270,
                createMap(new Integer[][] {
                        // SafePage LANDSCAPE (reverse)
                        { PDF_ROTATION_0, PDF_ROTATION_0 },
                        // SafePage PORTRAIT
                        { PDF_ROTATION_90, PDF_ROTATION_0 },
                        // SafePage LANDSCAPE
                        { PDF_ROTATION_180, PDF_ROTATION_180 },
                        // SafePage LANDSCAPE
                        { PDF_ROTATION_270, PDF_ROTATION_180 },
                        // SafePage LANDSCAPE (reverse)
                        { PDF_ROTATION_360, PDF_ROTATION_0 } }));

        rotationMap.put(PDF_ROTATION_360, rotationMap.get(PDF_ROTATION_0));

        return rotationMap;
    }

    /**
     * @return The rotations map for landscape PDF source page.
     */
    private static Map<Integer, Map<Integer, Integer>>
            createRotationMapLandscapePrint() {

        final Map<Integer, Map<Integer, Integer>> rotationMap = new HashMap<>();

        rotationMap.put(PDF_ROTATION_0,
                createMap(new Integer[][] {
                        // SafePage LANDSCAPE
                        { PDF_ROTATION_0, PDF_ROTATION_90 },
                        // SafePage PORTRAIT
                        { PDF_ROTATION_90, PDF_ROTATION_90 },
                        // SafePage LANDSCAPE (reverse)
                        { PDF_ROTATION_180, PDF_ROTATION_270 },
                        // SafePage PORTRAIT (reverse)
                        { PDF_ROTATION_270, PDF_ROTATION_270 },
                        // SafePage LANDSCAPE
                        { PDF_ROTATION_360, PDF_ROTATION_90 } }));

        rotationMap.put(PDF_ROTATION_90,
                createMap(new Integer[][] {
                        // SafePage PORTRAIT
                        { PDF_ROTATION_0, PDF_ROTATION_90 },
                        // SafePage LANDSCAPE (reverse)
                        { PDF_ROTATION_90, PDF_ROTATION_270 },
                        // SafePage LANDSCAPE (reverse)
                        { PDF_ROTATION_180, PDF_ROTATION_270 },
                        // SafePage LANDSCAPE
                        { PDF_ROTATION_270, PDF_ROTATION_90 },
                        // SafePage PORTRAIT
                        { PDF_ROTATION_360, PDF_ROTATION_90 } }));

        rotationMap.put(PDF_ROTATION_180,
                createMap(new Integer[][] {
                        // SafePage LANDSCAPE (reverse)
                        { PDF_ROTATION_0, PDF_ROTATION_270 },
                        // SafePage LANDSCAPE (reverse)
                        { PDF_ROTATION_90, PDF_ROTATION_270 },
                        // SafePage LANDSCAPE
                        { PDF_ROTATION_180, PDF_ROTATION_90 },
                        // SafePage LANDSCAPE
                        { PDF_ROTATION_270, PDF_ROTATION_90 },
                        // SafePage LANDSCAPE (reverse)
                        { PDF_ROTATION_360, PDF_ROTATION_270 } }));

        rotationMap.put(PDF_ROTATION_270,
                createMap(new Integer[][] {
                        // SafePage LANDSCAPE (reverse)
                        { PDF_ROTATION_0, PDF_ROTATION_270 },
                        // SafePage LANDSCAPE
                        { PDF_ROTATION_90, PDF_ROTATION_90 },
                        // SafePage LANDSCAPE
                        { PDF_ROTATION_180, PDF_ROTATION_90 },
                        // SafePage LANDSCAPE
                        { PDF_ROTATION_270, PDF_ROTATION_270 },
                        // SafePage LANDSCAPE (reverse)
                        { PDF_ROTATION_360, PDF_ROTATION_270 } }));

        rotationMap.put(PDF_ROTATION_360, rotationMap.get(PDF_ROTATION_0));

        return rotationMap;
    }

}
