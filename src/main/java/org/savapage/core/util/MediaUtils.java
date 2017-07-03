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
package org.savapage.core.util;

import java.awt.print.PageFormat;
import java.awt.print.PrinterJob;
import java.util.HashMap;
import java.util.Map;

import javax.print.attribute.EnumSyntax;
import javax.print.attribute.Size2DSyntax;
import javax.print.attribute.standard.MediaSize;
import javax.print.attribute.standard.MediaSizeName;

import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang3.StringUtils;
import org.savapage.core.SpException;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp;
import org.savapage.core.config.IConfigProp.Key;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class MediaUtils {

    /**
     * The SingletonHolder is loaded on the first execution of
     * {@link MediaUtils#instance()} or the first access to
     * {@link SingletonHolder#INSTANCE}, not before.
     */
    private static class SingletonHolder {
        /** */
        public static final MediaUtils INSTANCE = new MediaUtils();
    }

    /**
     * This is a hack...
     */
    private class MyMediaSizeName extends MediaSizeName {

        /** */
        private static final long serialVersionUID = 1L;

        /** */
        MyMediaSizeName() {
            super(0);
        }

        /**
         *
         * @param media
         * @return
         */
        public MediaSizeName convert(final String media) {

            if (!media.startsWith("custom")) {
                EnumSyntax[] syntax = getEnumValueTable();
                int i = 0;
                for (String val : this.getStringTable()) {
                    if (StringUtils.replace(media, "_", "-").startsWith(val)) {
                        return (MediaSizeName) syntax[i];
                    }
                    i++;
                }
            }
            return null;
        }
    }

    /** */
    private final MyMediaSizeName mediaConverter = new MyMediaSizeName();

    /*
     * See:
     * http://www.docjar.org/html/api/gnu/javax/print/CupsMediaMapping.java.html
     */

    /**
     * Currently known mapping of MediaSize attribute values of the CUPS
     * printing system to the Java JPS API enum values.
     *
     * See
     * <a href="http://tools.ietf.org/html/rfc2911">http://tools.ietf.org/html
     * /rfc2911</a>
     */
    private final Map<String, MediaSizeName> cups2MediaSize = new HashMap<>();

    /**
     * Vice versa of {@link #cups2MediaSize}.
     */
    private final Map<MediaSizeName, String> mediaSize2Cups = new HashMap<>();

    /**
     *
     */
    // private final Map<String, MediaSizeName> ipp2MediaSize = new HashMap<>();

    /**
     * Vice versa of {@link #ipp2MediaSize}.
     */
    // private final Map<MediaSizeName, String> mediaSize2Ipp = new HashMap<>();

    /**
     * Initialize currently known mappings.
     */
    private MediaUtils() {

        cups2MediaSize.put("Postcard", MediaSizeName.JAPANESE_POSTCARD);
        cups2MediaSize.put("Statement", MediaSizeName.INVOICE);

        cups2MediaSize.put("Letter", MediaSizeName.NA_LETTER);
        cups2MediaSize.put("Executive", MediaSizeName.EXECUTIVE);
        cups2MediaSize.put("Legal", MediaSizeName.NA_LEGAL);

        cups2MediaSize.put("A0", MediaSizeName.ISO_A0);
        cups2MediaSize.put("A1", MediaSizeName.ISO_A1);
        cups2MediaSize.put("A2", MediaSizeName.ISO_A2);
        cups2MediaSize.put("A3", MediaSizeName.ISO_A3);
        cups2MediaSize.put("A4", MediaSizeName.ISO_A4);
        cups2MediaSize.put("A5", MediaSizeName.ISO_A5);
        cups2MediaSize.put("A6", MediaSizeName.ISO_A6);
        cups2MediaSize.put("A7", MediaSizeName.ISO_A7);
        cups2MediaSize.put("A8", MediaSizeName.ISO_A8);
        cups2MediaSize.put("A9", MediaSizeName.ISO_A9);
        cups2MediaSize.put("A10", MediaSizeName.ISO_A10);

        cups2MediaSize.put("B0", MediaSizeName.JIS_B0);
        cups2MediaSize.put("B1", MediaSizeName.JIS_B1);
        cups2MediaSize.put("B2", MediaSizeName.JIS_B2);
        cups2MediaSize.put("B3", MediaSizeName.JIS_B3);
        cups2MediaSize.put("B4", MediaSizeName.JIS_B4);
        cups2MediaSize.put("B5", MediaSizeName.JIS_B5);
        cups2MediaSize.put("B6", MediaSizeName.JIS_B6);
        cups2MediaSize.put("B7", MediaSizeName.JIS_B7);
        cups2MediaSize.put("B8", MediaSizeName.JIS_B8);
        cups2MediaSize.put("B9", MediaSizeName.JIS_B9);
        cups2MediaSize.put("B10", MediaSizeName.JIS_B10);

        cups2MediaSize.put("ISOB0", MediaSizeName.ISO_B0);
        cups2MediaSize.put("ISOB1", MediaSizeName.ISO_B1);
        cups2MediaSize.put("ISOB2", MediaSizeName.ISO_B2);
        cups2MediaSize.put("ISOB3", MediaSizeName.ISO_B3);
        cups2MediaSize.put("ISOB4", MediaSizeName.ISO_B4);
        cups2MediaSize.put("ISOB5", MediaSizeName.ISO_B5);
        cups2MediaSize.put("ISOB6", MediaSizeName.ISO_B6);
        cups2MediaSize.put("ISOB7", MediaSizeName.ISO_B7);
        cups2MediaSize.put("ISOB8", MediaSizeName.ISO_B8);
        cups2MediaSize.put("ISOB9", MediaSizeName.ISO_B9);
        cups2MediaSize.put("ISOB10", MediaSizeName.ISO_B10);
        cups2MediaSize.put("EnvISOB0", MediaSizeName.ISO_B0);
        cups2MediaSize.put("EnvISOB1", MediaSizeName.ISO_B1);
        cups2MediaSize.put("EnvISOB2", MediaSizeName.ISO_B2);
        cups2MediaSize.put("EnvISOB3", MediaSizeName.ISO_B3);
        cups2MediaSize.put("EnvISOB4", MediaSizeName.ISO_B4);
        cups2MediaSize.put("EnvISOB5", MediaSizeName.ISO_B5);
        cups2MediaSize.put("EnvISOB6", MediaSizeName.ISO_B6);
        cups2MediaSize.put("EnvISOB7", MediaSizeName.ISO_B7);
        cups2MediaSize.put("EnvISOB8", MediaSizeName.ISO_B8);
        cups2MediaSize.put("EnvISOB9", MediaSizeName.ISO_B9);
        cups2MediaSize.put("EnvISOB10", MediaSizeName.ISO_B10);

        cups2MediaSize.put("C0", MediaSizeName.ISO_C0);
        cups2MediaSize.put("C1", MediaSizeName.ISO_C1);
        cups2MediaSize.put("C2", MediaSizeName.ISO_C2);
        cups2MediaSize.put("C3", MediaSizeName.ISO_C3);
        cups2MediaSize.put("C4", MediaSizeName.ISO_C4);
        cups2MediaSize.put("C5", MediaSizeName.ISO_C5);
        cups2MediaSize.put("C6", MediaSizeName.ISO_C6);

        cups2MediaSize.put("EnvPersonal", MediaSizeName.PERSONAL_ENVELOPE);
        cups2MediaSize.put("EnvMonarch", MediaSizeName.MONARCH_ENVELOPE);
        cups2MediaSize.put("Monarch", MediaSizeName.MONARCH_ENVELOPE);
        cups2MediaSize.put("Env9", MediaSizeName.NA_NUMBER_9_ENVELOPE);
        cups2MediaSize.put("Env10", MediaSizeName.NA_NUMBER_10_ENVELOPE);
        cups2MediaSize.put("Env11", MediaSizeName.NA_NUMBER_11_ENVELOPE);
        cups2MediaSize.put("Env12", MediaSizeName.NA_NUMBER_12_ENVELOPE);
        cups2MediaSize.put("Env14", MediaSizeName.NA_NUMBER_14_ENVELOPE);
        cups2MediaSize.put("c8x10", MediaSizeName.NA_8X10);

        cups2MediaSize.put("EnvDL", MediaSizeName.ISO_DESIGNATED_LONG);
        cups2MediaSize.put("DL", MediaSizeName.ISO_DESIGNATED_LONG);
        cups2MediaSize.put("EnvC0", MediaSizeName.ISO_C0);
        cups2MediaSize.put("EnvC1", MediaSizeName.ISO_C1);
        cups2MediaSize.put("EnvC2", MediaSizeName.ISO_C2);
        cups2MediaSize.put("EnvC3", MediaSizeName.ISO_C3);
        cups2MediaSize.put("EnvC4", MediaSizeName.ISO_C4);
        cups2MediaSize.put("EnvC5", MediaSizeName.ISO_C5);
        cups2MediaSize.put("EnvC6", MediaSizeName.ISO_C6);

        for (Map.Entry<String, MediaSizeName> entry : cups2MediaSize
                .entrySet()) {
            mediaSize2Cups.put(entry.getValue(), entry.getKey());
        }
    }

    /**
     * Gets the singleton instance.
     *
     * @return
     */
    private static MediaUtils getInstance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     * Gets the default paper size as configured in this application.
     *
     * @return
     */
    public static MediaSizeName getDefaultMediaSize() {

        MediaSizeName mediaSizeName;

        final String papersize = ConfigManager.instance()
                .getConfigValue(Key.SYS_DEFAULT_PAPER_SIZE);

        if (papersize.equals(IConfigProp.PAPERSIZE_V_SYSTEM)) {
            mediaSizeName = MediaUtils.getHostDefaultMediaSize();
        } else if (papersize.equals(IConfigProp.PAPERSIZE_V_LETTER)) {
            mediaSizeName = MediaSizeName.NA_LETTER;
        } else if (papersize.equals(IConfigProp.PAPERSIZE_V_A4)) {
            mediaSizeName = MediaSizeName.ISO_A4;
        } else {
            throw new SpException(Key.SYS_DEFAULT_PAPER_SIZE.toString() + " ["
                    + papersize + "] not handled.");
        }

        return mediaSizeName;
    }

    /**
     * Gets the default paper size from the host OS. When it is not found or not
     * supported {@link MediaSizeName#NA_LETTER} is returned.
     * <p>
     * TODO: there must be an easier way ?
     * </p>
     *
     * @return
     */
    public static MediaSizeName getHostDefaultMediaSize() {
        PrinterJob printerJob = PrinterJob.getPrinterJob();
        PageFormat pageformat = printerJob.defaultPage();
        MediaSizeName sizeName = getMediaSize(pageformat);
        if (sizeName == null) {
            sizeName = MediaSizeName.NA_LETTER;
        }
        return sizeName;
    }

    /**
     * Return the media size in millimeters.
     *
     * @param name
     *            The media size name.
     * @return A two-dimensional array with the WIDTH at index 0, and the HEIGHT
     *         at index 1.
     */
    public static int[] getMediaWidthHeight(final MediaSizeName name) {
        MediaSize ms = MediaSize.getMediaSizeForName(name);
        float[] flSize = ms.getSize(Size2DSyntax.MM);
        int[] size = new int[2];
        for (int i = 0; i < 2; i++) {
            size[i] = (int) Math.floor(flSize[i] + 0.5d);
        }
        return size;
    }

    /**
     * Return the media size in millimeters and <i>portrait</i> orientation.
     *
     * @param pageformat
     *            The page format.
     *
     * @return A two-dimensional array with the WIDTH at index 0, and the HEIGHT
     *         at index 1.
     */
    public static int[] getMediaWidthHeight(final PageFormat pageformat) {

        int[] size;

        MediaSizeName sizeName = getMediaSize(pageformat);

        if (sizeName != null) {

            size = getMediaWidthHeight(sizeName);

        } else {

            size = new int[2];

            double inchWidth = pageformat.getWidth();
            double inchHeight = pageformat.getHeight();

            inchWidth /= 72;
            inchHeight /= 72;

            int mmWidth = (int) Math.floor(25.4 * inchWidth + 0.5d);
            int mmHeight = (int) Math.floor(25.4 * inchHeight + 0.5d);

            int iSizeWidth = 0;
            int iSizeHeight = 1;

            /*
             * Correct for landscape orientation.
             */
            if (mmWidth > mmHeight) {
                iSizeWidth = 1;
                iSizeHeight = 0;
            }

            size[iSizeWidth] = mmWidth;
            size[iSizeHeight] = mmHeight;
        }

        return size;
    }

    /**
     * Return the IPP RFC2911 media name for the page format, using the
     * {@link MediaSizeName} if found, otherwise a non-IPP name is composed with
     * format 'width'x'height'.
     *
     * @param pageformat
     * @return
     */
    public static String getMediaSizeName(final PageFormat pageformat) {

        final MediaSizeName sizeName = getMediaSize(pageformat);

        if (sizeName != null) {
            return sizeName.toString();
        } else {
            int[] size = getMediaWidthHeight(pageformat);
            return String.format("%dx%dmm", size[0], size[1]);
        }
    }

    /**
     *
     * @param name
     * @return
     */
    public static String getUserFriendlyMediaName(MediaSizeName name) {
        return getInstance().mediaSize2Cups.get(name);
    }

    /**
     * Returns the IPP MediaSizeName for a PageFormat.
     * <p>
     * <b>Note</b>: The page format CAN be in portrait or landscape orientation.
     * <p>
     *
     * @param pageformat
     *            The page format.
     *
     * @return The IPP MediaSizeName, or {@code null} when not found.
     */
    public static MediaSizeName getMediaSize(final PageFormat pageformat) {

        double inchWidth = 0;
        double inchHeight = 0;

        inchWidth = pageformat.getWidth();
        inchHeight = pageformat.getHeight();

        inchWidth /= 72;
        inchHeight /= 72;

        int mmWidth = (int) Math.floor(25.4 * inchWidth + 0.5d);
        int mmHeight = (int) Math.floor(25.4 * inchHeight + 0.5d);

        /*
         * The size array with width and height.
         */
        float[] size;

        int iSizeWidth = 0;
        int iSizeHeight = 1;

        /*
         * Correct for landscape orientation.
         */
        if (mmWidth > mmHeight) {
            iSizeWidth = 1;
            iSizeHeight = 0;
        }

        /*
         * Since units of PageFormat are 1/72 of an inch, rounding errors will
         * occur.
         *
         * E.g. Format "Letter" is defined in inches, so the conversion to
         * millimeter will have rounding error. Likewise, A4 will have rounding
         * errors for inches.
         *
         * So, we will look for an exact match for both inches and millimeters.
         */
        for (MediaSizeName name : getInstance().cups2MediaSize.values()) {

            MediaSize ms = MediaSize.getMediaSizeForName(name);

            if (ms == null) {
                continue;
            }

            // Check inches
            size = ms.getSize(Size2DSyntax.INCH);

            if (NumberUtils.compare(inchWidth, size[iSizeWidth]) == 0
                    && NumberUtils.compare(inchHeight,
                            size[iSizeHeight]) == 0) {
                return name;
            }

            // Check millimeters
            size = ms.getSize(Size2DSyntax.MM);
            if (mmWidth == size[iSizeWidth] && mmHeight == size[iSizeHeight]) {
                return name;
            }

        }
        return null;
    }

    /**
     * Returns the IPP MediaSizeName value for an RFC2911 IPP media keyword.
     *
     * @param keyword
     *            The RFC2911 IPP media keyword.
     * @return The IPP MediaSizeName if a mapping is known, <code>null</code>
     *         when not.
     */
    public static MediaSizeName
            getMediaSizeFromInboxMedia(final String keyword) {
        return getInstance().mediaConverter.convert(keyword);
    }

    /**
     * Compares width or height of topw media sizes.
     *
     * @param mediaSizeNameA
     *            {@link MediaSizeName} A.
     * @param mediaSizeNameB
     *            {@link MediaSizeName} B.
     * @return the value {@code 0} if width or height of A is EQ to B; a value
     *         LT {@code 0} if width or height of A is LT width or height of B;
     *         and a value greater than {@code 0} width or height of A is GT
     *         width or height of B.
     */
    public static int compareMediaSize(final MediaSizeName mediaSizeNameA,
            final MediaSizeName mediaSizeNameB) {

        final int[] dimA = MediaUtils.getMediaWidthHeight(mediaSizeNameA);
        final int[] dimB = MediaUtils.getMediaWidthHeight(mediaSizeNameB);

        final int compare;

        if (dimA[0] == dimB[0] && dimA[1] == dimB[1]) {
            compare = 0;
        } else if (dimA[0] < dimB[0] || dimA[1] < dimB[1]) {
            compare = -1;
        } else {
            compare = 1;
        }

        return compare;
    }

}
