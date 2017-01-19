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
package org.savapage.core.ipp.attribute;

import java.util.HashMap;
import java.util.Map;

import org.savapage.core.ipp.attribute.syntax.IppBoolean;
import org.savapage.core.ipp.attribute.syntax.IppEnum;
import org.savapage.core.ipp.attribute.syntax.IppInteger;
import org.savapage.core.ipp.attribute.syntax.IppKeyword;
import org.savapage.core.ipp.attribute.syntax.IppName;
import org.savapage.core.ipp.attribute.syntax.IppRangeOfInteger;
import org.savapage.core.ipp.attribute.syntax.IppResolution;
import org.savapage.core.ipp.encoding.IppValueTag;

/**
 * A dictionary of "job-template" attributes.
 *
 * @author Rijk Ravestein
 *
 */
public final class IppDictJobTemplateAttr extends AbstractIppDict {

    /**
     * Application of the template attribute.
     *
     */
    public static enum ApplEnum {
        /**
         *
         */
        DEFAULT,
        /**
         *
         */
        SUPPORTED
    }

    public static final String ATTR_JOB_PRIORITY = "job-priority";

    public static final String ATTR_JOB_HOLD_UNTIL = "job-hold-until";

    /**
     * job-sheets (1setof type3 keyword | name(MAX))
     * <p>
     * <b>Note</b>: according to <a href=
     * "http://www.opensource.apple.com/source/cups/cups-30/doc/ipp.shtml?txt" >
     * this source</a>. Standard IPP only allows specification of a single
     * job-sheets attribute value.
     * </p>
     * <p>
     * (CUPS 1.1 and higher)
     * </p>
     * <p>
     * The job-sheets attribute specifies one or two banner files that are
     * printed before and after a job. The reserved value of "none" disables
     * banner printing. The default value is stored in the job-sheets-default
     * attribute.
     * </p>
     * <p>
     * If only one value is supplied, the banner file is printed before the job.
     * If two values are supplied, the first value is used as the starting
     * banner file and the second as the ending banner file.
     * </p>
     */
    public static final String ATTR_JOB_SHEETS = "job-sheets";

    public static final String ATTR_MULTIPLE_DOC_HANDLING =
            "multiple-document-handling";

    public static final String ATTR_COPIES = "copies";

    public static final String ATTR_FINISHINGS = "finishings";

    public static final String ATTR_PAGES_RANGES = "page-ranges";

    public static final String ATTR_SIDES = "sides";

    public static final String ATTR_NUMBER_UP = "number-up";

    public static final String ATTR_ORIENTATION_REQUESTED =
            "orientation-requested";

    /**
     * ???
     */
    public static final String ATTR_PRINT_COLOR_MODE = "print-color-mode";

    /**
     * media (type3 keyword | name(MAX))
     * <p>
     * This attribute identifies the medium that the Printer uses for all
     * impressions of the Job.
     * </p>
     */
    public static final String ATTR_MEDIA = "media";

    /**
     * Media collection.
     */
    public static final String ATTR_MEDIA_COL = "media-col";

    /**
     * media-size collection: { "x-dimension" = INTEGER(0:MAX); "y-dimension" =
     * INTEGER(0:MAX) }. Dimensions are in hundredths of a millimeter.
     */
    public static final String ATTR_MEDIA_SIZE = "media-size";

    /**
     * .
     */
    public static final String ATTR_MEDIA_COLOR = "media-color";

    /**
     * .
     */
    public static final String ATTR_MEDIA_TYPE = "media-type";

    /**
     *
     */
    public static final String ATTR_MEDIA_SOURCE = "media-source";

    public static final String ATTR_PRINTER_RESOLUTION = "printer-resolution";

    public static final String ATTR_PRINT_QUALITY = "print-quality";

    public static final String ATTR_PRINT_SCALING = "print-scaling";

    /**
     * "This attribute specifies whether or not the media sheets of each copy of
     * each printed document in a job are to be in sequence, when multiple
     * copies of the document are specified by the 'copies' attribute."
     * <p>
     * <a href="https://tools.ietf.org/html/rfc3381">https://tools.ietf.org/html
     * /rfc3381</a>
     * </p>
     *
     * @since 0.9.11
     */
    public static final String ATTR_SHEET_COLLATE = "sheet-collate";

    /**
     * CUPS attribute (since CUPS 1.4/OS X 10.6): specifies whether to scale
     * documents to fit on the selected media (fit-to-page=true) or use the
     * physical size specified in the document (fit-to-page=false). The default
     * value is false.
     * <p>
     * <a href="http://www.cups.org/documentation.php/spec-ipp.html">http://www.
     * cups.org/documentation.php/spec-ipp.html</a>
     * </p>
     *
     * @since 0.9.6
     */
    public static final String CUPS_ATTR_FIT_TO_PAGE = "fit-to-page";

    /**
     * CUPS attribute (deprecated since CUPS 1.6): The page-bottom attribute
     * specifies the bottom margin in points (72 points equals 1 inch). The
     * default value is the device physical margin.
     * <p>
     * <a href="http://www.cups.org/documentation.php/spec-ipp.html">http://www.
     * cups.org/documentation.php/spec-ipp.html</a>
     * </p>
     */
    public static final String CUPS_ATTR_PAGE_BOTTOM = "page-bottom";

    /**
     * CUPS attribute (deprecated since CUPS 1.6): The page-left attribute
     * specifies the left margin in points (72 points equals 1 inch). The
     * default value is the device physical margin.
     * <p>
     * <a href="http://www.cups.org/documentation.php/spec-ipp.html">http://www.
     * cups.org/documentation.php/spec-ipp.html</a>
     * </p>
     */
    public static final String CUPS_ATTR_PAGE_LEFT = "page-left";

    /**
     * CUPS attribute (deprecated since CUPS 1.6): The page-right attribute
     * specifies the right margin in points (72 points equals 1 inch). The
     * default value is the device physical margin.
     * <p>
     * <a href="http://www.cups.org/documentation.php/spec-ipp.html">http://www.
     * cups.org/documentation.php/spec-ipp.html</a>
     * </p>
     */
    public static final String CUPS_ATTR_PAGE_RIGHT = "page-right";

    /**
     * CUPS attribute (deprecated since CUPS 1.6): The page-top attribute
     * specifies the top margin in points (72 points equals 1 inch). The default
     * value is the device physical margin.
     * <p>
     * <a href="http://www.cups.org/documentation.php/spec-ipp.html">http://www.
     * cups.org/documentation.php/spec-ipp.html</a>
     * </p>
     */
    public static final String CUPS_ATTR_PAGE_TOP = "page-top";

    /**
     *
     */
    public static final String CUPS_ATTR_PAGE_SET = "page-set";

    /**
     *
     */
    public static final String CUPS_ATTR_LANDSCAPE = "landscape";

    /**
     * Rotates the page.
     */
    public static final String CUPS_ATTR_ORIENTATION_REQUESTED =
            "orientation-requested";

    /**
     * N-Up printing places multiple document pages on a single printed page.
     * The "number-up-layout" option chooses the layout of the pages on each
     * output page.
     */
    public static final String CUPS_ATTR_NUMBER_UP_LAYOUT = "number-up-layout";

    /**
     * PWG5100.13: The RECOMMENDED "media-bottom-margin" member attribute
     * defines the Printer's physical bottom margin in hundredths of millimeters
     * from the bottom edge, without respect to the value of the
     * “orientation-requested” Job Template attribute.
     */
    public static final String ATTR_MEDIA_BOTTOM_MARGIN = "media-bottom-margin";

    /**
     * PWG5100.13: The RECOMMENDED "media-left-margin" member attribute defines
     * the Printer's physical left margin in hundredths of millimeters from the
     * left edge, without respect to the value of the “orientation-requested”
     * Job Template attribute.
     */
    public static final String ATTR_MEDIA_LEFT_MARGIN = "media-left-margin";

    /**
     * PWG5100.13: The RECOMMENDED "media-right-margin" member attribute defines
     * the Printer's physical right margin in hundredths of millimeters from the
     * right edge, without respect to the value of the “orientation-requested”
     * Job Template attribute.
     */
    public static final String ATTR_MEDIA_RIGHT_MARGIN = "media-right-margin";

    /**
     * PWG5100.13: The RECOMMENDED "media-top-margin" member attribute defines
     * the Printer's physical top margin in hundredths of millimeters from the
     * top edge, without respect to the value of the “orientation-requested” Job
     * Template attribute.
     */
    public static final String ATTR_MEDIA_TOP_MARGIN = "media-top-margin";

    /**
     * ftp://ftp.pwg.org/pub/pwg/ipp/new_ATT/pwg5100.2.pdf
     * <p>
     * Internet Printing Protocol (IPP): “output-bin” attribute extension
     * IEEE-ISTO Printer Working Group Standard 5100.2-2001 February 7, 2001
     * </p>
     */
    public static final String ATTR_OUTPUT_BIN = "output-bin";

    /**
     * .
     */
    public static final String ORG_SAVAPAGE_ATTR_MEDIA_WEIGHT_METRIC =
            ORG_SAVAPAGE_ATTR_PFX + "media-weight-metric";

    /**
     * .
     */
    public static final String ORG_SAVAPAGE_ATTR_JOB_COVER =
            ORG_SAVAPAGE_ATTR_PFX + "job-cover";

    /**
     * The prefix for Custom SavaPage IPP Job template finishings attributes.
     */
    public static final String ORG_SAVAPAGE_ATTR_PFX_FINISHINGS =
            ORG_SAVAPAGE_ATTR_PFX + "finishings-";

    /**
     * Custom SavaPage IPP Job template finishing attribute for external
     * operator action.
     */
    public static final String ORG_SAVAPAGE_ATTR_FINISHINGS_EXT =
            ORG_SAVAPAGE_ATTR_PFX_FINISHINGS + "ext";

    /**
     * The Job Ticket media attributes related to {@link #ATTR_MEDIA}.
     */
    public static final String[] JOBTICKET_ATTR_MEDIA =
            new String[] { ATTR_MEDIA_COLOR, ATTR_MEDIA_TYPE,
                    ORG_SAVAPAGE_ATTR_MEDIA_WEIGHT_METRIC };

    /**
     * The Job Ticket media attributes related to
     * {@link IppKeyword#MEDIA_TYPE_PAPER}.
     */
    public static final String[] JOBTICKET_ATTR_MEDIA_TYPE_PAPER =
            new String[] { ATTR_MEDIA_COLOR,
                    ORG_SAVAPAGE_ATTR_MEDIA_WEIGHT_METRIC };

    /**
     * .
     */
    public static final String[] JOBTICKET_ATTR_COPY =
            new String[] { ORG_SAVAPAGE_ATTR_JOB_COVER };

    /**
     * .
     */
    public static final String[] JOBTICKET_ATTR_FINISHINGS_EXT =
            new String[] { ORG_SAVAPAGE_ATTR_FINISHINGS_EXT };

    /**
     * All Job Tickets attributes.
     */
    private static final String[][] JOBTICKET_ATTR_ARRAYS =
            { JOBTICKET_ATTR_MEDIA, JOBTICKET_ATTR_COPY,
                    JOBTICKET_ATTR_FINISHINGS_EXT };

    /**
     * Array of 2-element array elements, one for each Job Ticket copy
     * attribute: the first element is the IPP option key, and the second
     * element its NONE value.
     */
    public static final String[][] JOBTICKET_ATTR_COPY_V_NONE = {
            { IppDictJobTemplateAttr.ORG_SAVAPAGE_ATTR_JOB_COVER,
                    IppKeyword.ORG_SAVAPAGE_ATTR_JOB_COVER_NONE },
            { IppDictJobTemplateAttr.ORG_SAVAPAGE_ATTR_FINISHINGS_EXT,
                    IppKeyword.ORG_SAVAPAGE_ATTR_FINISHINGS_EXTERNAL_NONE }
            //
    };

    /**
     * Custom SavaPage IPP Job template finishing attribute for Stapling.
     */
    public static final String ORG_SAVAPAGE_ATTR_FINISHINGS_STAPLE =
            ORG_SAVAPAGE_ATTR_PFX_FINISHINGS + "staple";

    /**
     * Custom SavaPage IPP Job template finishing attribute for Punching.
     */
    public static final String ORG_SAVAPAGE_ATTR_FINISHINGS_PUNCH =
            ORG_SAVAPAGE_ATTR_PFX_FINISHINGS + "punch";

    /**
     * Custom SavaPage IPP Job template finishing attribute for Folding.
     */
    public static final String ORG_SAVAPAGE_ATTR_FINISHINGS_FOLD =
            ORG_SAVAPAGE_ATTR_PFX_FINISHINGS + "fold";

    /**
     * Custom SavaPage IPP Job template finishing attribute for Booklet.
     */
    public static final String ORG_SAVAPAGE_ATTR_FINISHINGS_BOOKLET =
            ORG_SAVAPAGE_ATTR_PFX_FINISHINGS + "booklet";

    /**
     * Array of 2-element array elements, one for each finishings: the first
     * element is the IPP option key, and the second element its NONE value.
     */
    public static final String[][] ORG_SAVAPAGE_ATTR_FINISHINGS_V_NONE = {
            { IppDictJobTemplateAttr.ORG_SAVAPAGE_ATTR_FINISHINGS_BOOKLET,
                    IppKeyword.ORG_SAVAPAGE_ATTR_FINISHINGS_BOOKLET_NONE },
            { IppDictJobTemplateAttr.ORG_SAVAPAGE_ATTR_FINISHINGS_FOLD,
                    IppKeyword.ORG_SAVAPAGE_ATTR_FINISHINGS_FOLD_NONE },
            { IppDictJobTemplateAttr.ORG_SAVAPAGE_ATTR_FINISHINGS_PUNCH,
                    IppKeyword.ORG_SAVAPAGE_ATTR_FINISHINGS_PUNCH_NONE },
            { IppDictJobTemplateAttr.ORG_SAVAPAGE_ATTR_FINISHINGS_STAPLE,
                    IppKeyword.ORG_SAVAPAGE_ATTR_FINISHINGS_STAPLE_NONE }
            //
    };

    /*
     * Defaults
     */
    private static final String _DFLT = "-default";

    public static final String ATTR_JOB_PRIORITY_DFLT =
            ATTR_JOB_PRIORITY + _DFLT;

    public static final String ATTR_JOB_HOLD_UNTIL_DFLT =
            ATTR_JOB_HOLD_UNTIL + _DFLT;

    public static final String ATTR_JOB_SHEETS_DFLT = ATTR_JOB_SHEETS + _DFLT;

    public static final String ATTR_MULTIPLE_DOC_HANDLING_DFLT =
            ATTR_MULTIPLE_DOC_HANDLING + _DFLT;

    public static final String ATTR_COPIES_DFLT = ATTR_COPIES + _DFLT;

    public static final String ATTR_FINISHINGS_DFLT = ATTR_FINISHINGS + _DFLT;

    public static final String ATTR_SIDES_DFLT = ATTR_SIDES + _DFLT;

    public static final String ATTR_NUMBER_UP_DFLT = ATTR_NUMBER_UP + _DFLT;

    public static final String ATTR_ORIENTATION_REQUESTED_DFLT =
            ATTR_ORIENTATION_REQUESTED + _DFLT;

    public static final String ATTR_MEDIA_DFLT = ATTR_MEDIA + _DFLT;

    public static final String ATTR_MEDIA_SOURCE_DFLT =
            ATTR_MEDIA_SOURCE + _DFLT;

    public static final String ATTR_PRINTER_RESOLUTION_DFLT =
            ATTR_PRINTER_RESOLUTION + _DFLT;

    public static final String ATTR_PRINT_QUALITY_DFLT =
            ATTR_PRINT_QUALITY + _DFLT;

    public static final String ATTR_SHEET_COLLATE_DFLT =
            ATTR_SHEET_COLLATE + _DFLT;

    public static final String ATTR_PRINT_SCALING_DFLT =
            ATTR_PRINT_SCALING + _DFLT;

    public static final String ATTR_PRINT_COLOR_MODE_DFLT =
            ATTR_PRINT_COLOR_MODE + _DFLT;

    public static final String ATTR_OUTPUT_BIN_DFLT = ATTR_OUTPUT_BIN + _DFLT;

    /*
     * Supported
     */
    private static final String _SUPP = "-supported";

    public static final String ATTR_JOB_PRIORITY_SUPP =
            ATTR_JOB_PRIORITY + _SUPP;

    public static final String ATTR_JOB_HOLD_UNTIL_SUPP =
            ATTR_JOB_HOLD_UNTIL + _SUPP;

    public static final String ATTR_JOB_SHEETS_SUPP = ATTR_JOB_SHEETS + _SUPP;

    public static final String ATTR_MULTIPLE_DOC_HANDLING_SUPP =
            ATTR_MULTIPLE_DOC_HANDLING + _SUPP;

    public static final String ATTR_COPIES_SUPP = ATTR_COPIES + _SUPP;

    public static final String ATTR_FINISHINGS_SUPP = ATTR_FINISHINGS + _SUPP;

    public static final String ATTR_PAGES_RANGES_SUPP =
            ATTR_PAGES_RANGES + _SUPP;

    public static final String ATTR_SIDES_SUPP = ATTR_SIDES + _SUPP;

    public static final String ATTR_NUMBER_UP_SUPP = ATTR_NUMBER_UP + _SUPP;

    public static final String ATTR_ORIENTATION_REQUESTED_SUPP =
            ATTR_ORIENTATION_REQUESTED + _SUPP;

    public static final String ATTR_MEDIA_SUPP = ATTR_MEDIA + _SUPP;

    public static final String ATTR_MEDIA_SOURCE_SUPP =
            ATTR_MEDIA_SOURCE + _SUPP;

    public static final String ATTR_PRINTER_RESOLUTION_SUPP =
            ATTR_PRINTER_RESOLUTION + _SUPP;

    public static final String ATTR_PRINT_QUALITY_SUPP =
            ATTR_PRINT_QUALITY + _SUPP;

    public static final String ATTR_SHEET_COLLATE_SUPP =
            ATTR_SHEET_COLLATE + _SUPP;

    public static final String ATTR_PRINT_SCALING_SUPP =
            ATTR_PRINT_SCALING + _SUPP;

    public static final String ATTR_PRINT_COLOR_MODE_SUPP =
            ATTR_PRINT_COLOR_MODE + _SUPP;

    public static final String ATTR_OUTPUT_BIN_SUPP = ATTR_OUTPUT_BIN + _SUPP;

    /*
     * Extra ...
     */
    public static final String ATTR_MEDIA_READY = ATTR_MEDIA + "-ready";

    /**
     * <pre>
     * +===================+======================+======================+
     * | Job Attribute     |Printer: Default Value|  Printer: Supported  |
     * |                   |   Attribute          |   Values Attribute   |
     * +===================+======================+======================+
     * | job-priority      | job-priority-default |job-priority-supported|
     * | (integer 1:100)   | (integer 1:100)      |(integer 1:100)       |
     * +-------------------+----------------------+----------------------+
     * | job-hold-until    | job-hold-until-      |job-hold-until-       |
     * | (type3 keyword |  |  default             | supported            |
     * |    name)          |  (type3 keyword |    |(1setOf (             |
     * |                   |    name)             |type3 keyword | name))|
     * +-------------------+----------------------+----------------------+
     * | job-sheets        | job-sheets-default   |job-sheets-supported  |
     * | (type3 keyword |  | (type3 keyword |     |(1setOf (             |
     * |    name)          |    name)             |type3 keyword | name))|
     * +-------------------+----------------------+----------------------+
     * |multiple-document- |multiple-document-    |multiple-document-    |
     * | handling          | handling-default     |handling-supported    |
     * | (type2 keyword)   | (type2 keyword)      |(1setOf type2 keyword)|
     * +-------------------+----------------------+----------------------+
     * | copies            | copies-default       | copies-supported     |
     * | (integer (1:MAX)) | (integer (1:MAX))    | (rangeOfInteger      |
     * |                   |                      |       (1:MAX))       |
     * +-------------------+----------------------+----------------------+
     * | finishings        | finishings-default   | finishings-supported |
     * |(1setOf type2 enum)|(1setOf type2 enum)   |(1setOf type2 enum)   |
     * +-------------------+----------------------+----------------------+
     * | page-ranges       | No                   | page-ranges-         |
     * | (1setOf           |                      | supported (boolean)  |
     * |   rangeOfInteger  |                      |                      |
     * |        (1:MAX))   |                      |                      |
     * +-------------------+----------------------+----------------------+
     * | sides             | sides-default        | sides-supported      |
     * | (type2 keyword)   | (type2 keyword)      |(1setOf type2 keyword)|
     * +-------------------+----------------------+----------------------+
     * | number-up         | number-up-default    | number-up-supported  |
     * | (integer (1:MAX)) | (integer (1:MAX))    |(1setOf (integer      |
     * |                   |                      | (1:MAX) |            |
     * |                   |                      |  rangeOfInteger      |
     * |                   |                      |   (1:MAX)))          |
     * +-------------------+----------------------+----------------------+
     * | orientation-      |orientation-requested-|orientation-requested-|
     * |  requested        |  default             |  supported           |
     * |   (type2 enum)    |  (type2 enum)        |  (1setOf type2 enum) |
     * +-------------------+----------------------+----------------------+
     * | media             | media-default        | media-supported      |
     * | (type3 keyword |  | (type3 keyword |     |(1setOf (             |
     * |    name)          |    name)             |type3 keyword | name))|
     * |                   |                      |                      |
     * |                   |                      | media-ready          |
     * |                   |                      |(1setOf (             |
     * |                   |                      |type3 keyword | name))|
     * +-------------------+----------------------+----------------------+
     * | printer-resolution| printer-resolution-  | printer-resolution-  |
     * | (resolution)      |  default             | supported            |
     * |                   | (resolution)         |(1setOf resolution)   |
     * +-------------------+----------------------+----------------------+
     * | print-quality     | print-quality-default| print-quality-       |
     * | (type2 enum)      | (type2 enum)         | supported            |
     * |                   |                      |(1setOf type2 enum)   |
     * +-------------------+----------------------+----------------------+
     * </pre>
     */
    private final IppAttr[] attributes = {
            //

            /**
             * <pre>
             * +-------------------+----------------------+----------------------+
             * | job-priority      | job-priority-default |job-priority-supported|
             * | (integer 1:100)   | (integer 1:100)      |(integer 1:100)       |
             * +-------------------+----------------------+----------------------+
             * </pre>
             */
            new IppAttr(ATTR_JOB_PRIORITY, new IppInteger(1, 100)),
            new IppAttr(ATTR_JOB_PRIORITY_DFLT, new IppInteger(1, 100)),
            new IppAttr(ATTR_JOB_PRIORITY_SUPP, new IppInteger(1, 100)),

            /**
             * <pre>
             * +-------------------+----------------------+----------------------+
             * | job-hold-until    | job-hold-until-      |job-hold-until-       |
             * | (type3 keyword |  |  default             | supported            |
             * |    name)          |  (type3 keyword |    |(1setOf (             |
             * |                   |    name)             |type3 keyword | name))|
             * +-------------------+----------------------+----------------------+
             * </pre>
             */
            // DEFAULT
            new IppAttr(ATTR_JOB_HOLD_UNTIL, IppKeyword.instance()),
            new IppAttr(ATTR_JOB_HOLD_UNTIL_DFLT, IppKeyword.instance()),
            new IppAttr(ATTR_JOB_HOLD_UNTIL_SUPP, IppKeyword.instance()),
            // ALTERNATIVE: see attributesAlt

            /**
             * <pre>
             * +-------------------+----------------------+----------------------+
             * | job-sheets        | job-sheets-default   |job-sheets-supported  |
             * | (type3 keyword |  | (type3 keyword |     |(1setOf (             |
             * |    name)          |    name)             |type3 keyword | name))|
             * +-------------------+----------------------+----------------------+
             * </pre>
             */
            // DEFAULT
            new IppAttr(ATTR_JOB_SHEETS, IppKeyword.instance()),
            new IppAttr(ATTR_JOB_SHEETS_DFLT, IppKeyword.instance()),
            new IppAttr(ATTR_JOB_SHEETS_SUPP, IppKeyword.instance()),
            // ALTERNATIVE: see attributesAlt

            /**
             * <pre>
             * +-------------------+----------------------+----------------------+
             * |multiple-document- |multiple-document-    |multiple-document-    |
             * | handling          | handling-default     |handling-supported    |
             * | (type2 keyword)   | (type2 keyword)      |(1setOf type2 keyword)|
             * +-------------------+----------------------+----------------------+
             * </pre>
             */
            new IppAttr(ATTR_MULTIPLE_DOC_HANDLING, IppKeyword.instance()),
            new IppAttr(ATTR_MULTIPLE_DOC_HANDLING_DFLT, IppKeyword.instance()),
            new IppAttr(ATTR_MULTIPLE_DOC_HANDLING_SUPP, IppKeyword.instance()),

            /**
             * <pre>
             * +-------------------+----------------------+----------------------+
             * | copies            | copies-default       | copies-supported     |
             * | (integer (1:MAX)) | (integer (1:MAX))    | (rangeOfInteger      |
             * |                   |                      |       (1:MAX))       |
             * +-------------------+----------------------+----------------------+
             * </pre>
             */
            //
            new IppAttr(ATTR_COPIES, new IppInteger(1)),
            new IppAttr(ATTR_COPIES_DFLT, new IppInteger(1)),
            new IppAttr(ATTR_COPIES_SUPP, IppRangeOfInteger.instance()),

            /**
             * <pre>
             * +-------------------+----------------------+----------------------+
             * | finishings        | finishings-default   | finishings-supported |
             * |(1setOf type2 enum)|(1setOf type2 enum)   |(1setOf type2 enum)   |
             * +-------------------+----------------------+----------------------+
             * </pre>
             */
            new IppAttr(ATTR_FINISHINGS, IppEnum.instance()),
            new IppAttr(ATTR_FINISHINGS_DFLT, IppEnum.instance()),
            new IppAttr(ATTR_FINISHINGS_SUPP, IppEnum.instance()),

            /**
             * <pre>
             * +-------------------+----------------------+----------------------+
             * | page-ranges       | No                   | page-ranges-         |
             * | (1setOf           |                      | supported (boolean)  |
             * |   rangeOfInteger  |                      |                      |
             * |        (1:MAX))   |                      |                      |
             * +-------------------+----------------------+----------------------+
             * </pre>
             */
            new IppAttr(ATTR_PAGES_RANGES, IppRangeOfInteger.instance()),
            new IppAttr(ATTR_PAGES_RANGES_SUPP, IppBoolean.instance()),

            /**
             * <pre>
             * +-------------------+----------------------+----------------------+
             * | sides             | sides-default        | sides-supported      |
             * | (type2 keyword)   | (type2 keyword)      |(1setOf type2 keyword)|
             * +-------------------+----------------------+----------------------+
             * </pre>
             */
            new IppAttr(ATTR_SIDES, IppKeyword.instance()),
            new IppAttr(ATTR_SIDES_DFLT, IppKeyword.instance()),
            new IppAttr(ATTR_SIDES_SUPP, IppKeyword.instance()),

            /**
             * <pre>
             * +-------------------+----------------------+----------------------+
             * | number-up         | number-up-default    | number-up-supported  |
             * | (integer (1:MAX)) | (integer (1:MAX))    |(1setOf (integer      |
             * |                   |                      | (1:MAX) |            |
             * |                   |                      |  rangeOfInteger      |
             * |                   |                      |   (1:MAX)))          |
             * +-------------------+----------------------+----------------------+
             * </pre>
             */
            new IppAttr(ATTR_NUMBER_UP, new IppInteger(1)),
            new IppAttr(ATTR_NUMBER_UP_DFLT, new IppInteger(1)),
            // DEFAULT
            new IppAttr(ATTR_NUMBER_UP_SUPP, new IppInteger(1)),
            // ALTERNATIVE: see attributesAlt

            /**
             * <pre>
             * +-------------------+----------------------+----------------------+
             * | orientation-      |orientation-requested-|orientation-requested-|
             * |  requested        |  default             |  supported           |
             * |   (type2 enum)    |  (type2 enum)        |  (1setOf type2 enum) |
             * </pre>
             */
            new IppAttr(ATTR_ORIENTATION_REQUESTED, IppEnum.instance()),
            new IppAttr(ATTR_ORIENTATION_REQUESTED_DFLT, IppEnum.instance()),
            new IppAttr(ATTR_ORIENTATION_REQUESTED_SUPP, IppEnum.instance()),

            /**
             * <pre>
             * +-------------------+----------------------+----------------------+
             * | media             | media-default        | media-supported      |
             * | (type3 keyword |  | (type3 keyword |     |(1setOf (             |
             * |    name)          |    name)             |type3 keyword | name))|
             * |                   |                      |                      |
             * |                   |                      | media-ready          |
             * |                   |                      |(1setOf (             |
             * |                   |                      |type3 keyword | name))|
             * +-------------------+----------------------+----------------------+
             * </pre>
             */
            // DEFAULT
            new IppAttr(ATTR_MEDIA, IppKeyword.instance()),
            new IppAttr(ATTR_MEDIA_DFLT, IppKeyword.instance()),
            new IppAttr(ATTR_MEDIA_SUPP, IppKeyword.instance()),
            new IppAttr(ATTR_MEDIA_READY, IppKeyword.instance()),
            // ALTERNATIVE: see attributesAlt

            /**
             * media-source type3 keyword | name(MAX)
             */
            // DEFAULT
            new IppAttr(ATTR_MEDIA_SOURCE, IppKeyword.instance()),
            new IppAttr(ATTR_MEDIA_SOURCE_DFLT, IppKeyword.instance()),
            new IppAttr(ATTR_MEDIA_SOURCE_SUPP, IppKeyword.instance()),
            // ALTERNATIVE: see attributesAlt

            /**
             * <pre>
             * +-------------------+----------------------+----------------------+
             * | printer-resolution| printer-resolution-  | printer-resolution-  |
             * | (resolution)      |  default             | supported            |
             * |                   | (resolution)         |(1setOf resolution)   |
             * +-------------------+----------------------+----------------------+
             * </pre>
             */
            new IppAttr(ATTR_PRINTER_RESOLUTION, IppResolution.instance()),
            new IppAttr(ATTR_PRINTER_RESOLUTION_DFLT, IppResolution.instance()),
            new IppAttr(ATTR_PRINTER_RESOLUTION_SUPP, IppResolution.instance()),

            /**
             * <pre>
             * +-------------------+----------------------+----------------------+
             * | print-quality     | print-quality-default| print-quality-       |
             * | (type2 enum)      | (type2 enum)         | supported            |
             * |                   |                      |(1setOf type2 enum)   |
             * +-------------------+----------------------+----------------------+
             * </pre>
             */
            new IppAttr(ATTR_PRINT_QUALITY, IppEnum.instance()),
            new IppAttr(ATTR_PRINT_QUALITY_DFLT, IppEnum.instance()),
            new IppAttr(ATTR_PRINT_QUALITY_SUPP, IppEnum.instance()),

            /**
             * <pre>
             * +===================+======================+=====================+
             * | Job Attribute     |Printer: Default Value|  Printer: Supported |
             * |                   |   Attribute          |   Values Attribute  |
             * +===================+======================+=====================+
             * | sheet-collate     | sheet-collate-default| sheet-collate-      |
             * | (type2 keyword)   | (type2 keyword)      |  supported (1setOf  |
             * |                   |                      |      type2 keyword) |
             * +-------------------+----------------------+---------------------+
             * </pre>
             */
            new IppAttr(ATTR_SHEET_COLLATE, IppKeyword.instance()),
            new IppAttr(ATTR_SHEET_COLLATE_DFLT, IppKeyword.instance()),
            new IppAttr(ATTR_SHEET_COLLATE_SUPP, IppKeyword.instance()),

            new IppAttr(ATTR_PRINT_SCALING, IppKeyword.instance()),
            new IppAttr(ATTR_PRINT_SCALING_DFLT, IppKeyword.instance()),
            new IppAttr(ATTR_PRINT_SCALING_SUPP, IppKeyword.instance()),

            /**
             * Where from ???
             */
            new IppAttr(ATTR_PRINT_COLOR_MODE, IppKeyword.instance()),
            new IppAttr(ATTR_PRINT_COLOR_MODE_DFLT, IppKeyword.instance()),
            new IppAttr(ATTR_PRINT_COLOR_MODE_SUPP, IppKeyword.instance()),

            /**
             * output-bin type3 keyword | name(MAX)
             */
            // DEFAULT
            new IppAttr(ATTR_OUTPUT_BIN, IppKeyword.instance()),
            new IppAttr(ATTR_OUTPUT_BIN_DFLT, IppKeyword.instance()),
            new IppAttr(ATTR_OUTPUT_BIN_SUPP, IppKeyword.instance()),
            // ALTERNATIVE: see attributesAlt

    };

    /**
     * Dictionary on attribute keyword.
     */
    private final Map<String, IppAttr> dictionaryAlt = new HashMap<>();

    private final IppAttr[] attributesAlt = {
            /**
             * <pre>
             * +-------------------+----------------------+----------------------+
             * | job-hold-until    | job-hold-until-      |job-hold-until-       |
             * | (type3 keyword |  |  default             | supported            |
             * |    name)          |  (type3 keyword |    |(1setOf (             |
             * |                   |    name)             |type3 keyword | name))|
             * +-------------------+----------------------+----------------------+
             * </pre>
             */
            // ALTERNATIVE
            new IppAttr(ATTR_JOB_HOLD_UNTIL, IppName.instance()),
            new IppAttr(ATTR_JOB_HOLD_UNTIL_DFLT, IppName.instance()),
            new IppAttr(ATTR_JOB_HOLD_UNTIL_SUPP, IppName.instance()),
            /**
             * <pre>
             * +-------------------+----------------------+----------------------+
             * | job-sheets        | job-sheets-default   |job-sheets-supported  |
             * | (type3 keyword |  | (type3 keyword |     |(1setOf (             |
             * |    name)          |    name)             |type3 keyword | name))|
             * +-------------------+----------------------+----------------------+
             * </pre>
             */
            // ALTERNATIVE
            new IppAttr(ATTR_JOB_SHEETS, IppName.instance()),
            new IppAttr(ATTR_JOB_SHEETS_DFLT, IppName.instance()),
            new IppAttr(ATTR_JOB_SHEETS_SUPP, IppName.instance()),

            /**
             * <pre>
             * +-------------------+----------------------+----------------------+
             * | number-up         | number-up-default    | number-up-supported  |
             * | (integer (1:MAX)) | (integer (1:MAX))    |(1setOf (integer      |
             * |                   |                      | (1:MAX) |            |
             * |                   |                      |  rangeOfInteger      |
             * |                   |                      |   (1:MAX)))          |
             * +-------------------+----------------------+----------------------+
             * </pre>
             */
            // ALTERNATIVE
            new IppAttr(ATTR_NUMBER_UP_SUPP, IppRangeOfInteger.instance()),

            /**
             * <pre>
             * +-------------------+----------------------+----------------------+
             * | media             | media-default        | media-supported      |
             * | (type3 keyword |  | (type3 keyword |     |(1setOf (             |
             * |    name)          |    name)             |type3 keyword | name))|
             * |                   |                      |                      |
             * |                   |                      | media-ready          |
             * |                   |                      |(1setOf (             |
             * |                   |                      |type3 keyword | name))|
             * +-------------------+----------------------+----------------------+
             * </pre>
             */
            // ALTERNATIVE
            new IppAttr(ATTR_MEDIA, IppName.instance()),
            new IppAttr(ATTR_MEDIA_DFLT, IppName.instance()),
            new IppAttr(ATTR_MEDIA_SUPP, IppName.instance()),
            new IppAttr(ATTR_MEDIA_READY, IppName.instance()),

            /**
             * media-source type3 keyword | name(MAX)
             */
            // ALTERNATIVE
            new IppAttr(ATTR_MEDIA_SOURCE, IppName.instance()),
            new IppAttr(ATTR_MEDIA_SOURCE_DFLT, IppName.instance()),
            new IppAttr(ATTR_MEDIA_SOURCE_SUPP, IppName.instance()),

            /**
             * output-bin type3 keyword | name(MAX)
             */
            // ALTERNATIVE
            new IppAttr(ATTR_OUTPUT_BIN, IppName.instance()),
            new IppAttr(ATTR_OUTPUT_BIN_DFLT, IppName.instance()),
            new IppAttr(ATTR_OUTPUT_BIN_SUPP, IppName.instance()),

            //
    };

    /**
     * The SingletonHolder is loaded on the first execution of
     * {@link IppDictJobTemplateAttr#instance()} or the first access to
     * {@link SingletonHolder#INSTANCE}, not before.
     * <p>
     * <a href=
     * "http://en.wikipedia.org/wiki/Singleton_pattern#The_solution_of_Bill_Pugh"
     * >The Singleton solution of Bill Pugh</a>
     * </p>
     */
    private static class SingletonHolder {
        public static final IppDictJobTemplateAttr INSTANCE =
                new IppDictJobTemplateAttr();
    }

    /**
     * Gets the singleton instance.
     *
     * @return
     */
    public static IppDictJobTemplateAttr instance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     *
     */
    private IppDictJobTemplateAttr() {

        init(attributes);

        for (IppAttr attribute : attributesAlt) {
            dictionaryAlt.put(attribute.getKeyword(), attribute);
        }

    }

    @Override
    public IppAttr getAttr(final String keyword, final IppValueTag valueTag) {

        if (keyword.startsWith(ATTR_JOB_HOLD_UNTIL)
                && (valueTag != IppValueTag.KEYWORD)) {
            return dictionaryAlt.get(keyword);
        }

        if (keyword.startsWith(ATTR_JOB_SHEETS)
                && (valueTag != IppValueTag.KEYWORD)) {
            return dictionaryAlt.get(keyword);
        }

        if (keyword.equals(ATTR_NUMBER_UP_SUPP)
                && (valueTag == IppValueTag.INTRANGE)) {
            return dictionaryAlt.get(keyword);
        }

        if (keyword.startsWith(ATTR_MEDIA)
                && (valueTag != IppValueTag.KEYWORD)) {
            return dictionaryAlt.get(keyword);
        }

        if (keyword.startsWith(ATTR_MEDIA_SOURCE)
                && (valueTag != IppValueTag.KEYWORD)) {
            return dictionaryAlt.get(keyword);
        }

        if (keyword.startsWith(ATTR_OUTPUT_BIN)
                && (valueTag != IppValueTag.KEYWORD)) {
            return dictionaryAlt.get(keyword);
        }

        /*
         * Use the default.
         */
        return getAttr(keyword);
    }

    /**
     * Checks if an IPP option is exclusively used in Job Ticket context.
     *
     * @param keyword
     *            The IPP option keyword.
     * @return {@code true} if IPP option is exclusively used for Job Ticket.
     */
    public static boolean isJobTicketAttr(final String keyword) {

        for (final String[] attrs : JOBTICKET_ATTR_ARRAYS) {
            for (final String attr : attrs) {
                if (attr.equals(keyword)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Checks if an IPP option/value is a finishing with unspecified "none"
     * value.
     *
     * @param keyword
     *            The IPP option keyword.
     * @param value
     *            The IPP option value.
     * @return {@code true} if IPP option/value is finishing with unspecified
     *         "none" value.
     */
    public static boolean isNoneValueFinishing(final String keyword,
            final String value) {

        for (final String[] finishing : ORG_SAVAPAGE_ATTR_FINISHINGS_V_NONE) {
            if (finishing[0].equals(keyword)) {
                return finishing[1].equals(value);
            }
        }
        return false;
    }

    /**
     * Composes an attribute name from a base keyword and an application.
     *
     * @param keyword
     *            The base keyword (name).
     * @param appl
     *            The {@link ApplEnum}.
     * @return The composed attribute name.
     */
    public static String attrName(final String keyword, final ApplEnum appl) {

        final String suffix;

        if (appl == ApplEnum.DEFAULT) {
            suffix = _DFLT;
        } else {
            suffix = _SUPP;
        }
        return String.format("%s%s", keyword, suffix);
    }
}
