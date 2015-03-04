/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2014 Datraverse B.V.
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.core.ipp.attribute;

import org.savapage.core.ipp.attribute.syntax.IppBoolean;
import org.savapage.core.ipp.attribute.syntax.IppCharset;
import org.savapage.core.ipp.attribute.syntax.IppDateTime;
import org.savapage.core.ipp.attribute.syntax.IppEnum;
import org.savapage.core.ipp.attribute.syntax.IppInteger;
import org.savapage.core.ipp.attribute.syntax.IppKeyword;
import org.savapage.core.ipp.attribute.syntax.IppMimeMediaType;
import org.savapage.core.ipp.attribute.syntax.IppName;
import org.savapage.core.ipp.attribute.syntax.IppNaturalLanguage;
import org.savapage.core.ipp.attribute.syntax.IppRangeOfInteger;
import org.savapage.core.ipp.attribute.syntax.IppText;
import org.savapage.core.ipp.attribute.syntax.IppUri;
import org.savapage.core.ipp.attribute.syntax.IppUriScheme;
import org.savapage.core.ipp.encoding.IppValueTag;

/**
 * A dictionary of "printer-description" attributes.
 *
 * @author Datraverse B.V.
 *
 */
public final class IppDictPrinterDescAttr extends AbstractIppDict {

    /**
     * The URF document format (MIME type) MUST be supported to print from Apple
     * iOS (iPhone, iPod, iPad).
     */
    public static final String DOCUMENT_FORMAT_URF = "image/urf";

    /**
     * The document format is used by Linux.
     */
    public static final String DOCUMENT_FORMAT_PDF = "application/pdf";

    /**
     * The document format is used by Windows.
     */
    public static final String DOCUMENT_FORMAT_POSTSCRIPT =
            "application/postscript";

    /**
     * 'idle': Indicates that new jobs can start processing without waiting.
     */
    public static final String PRINTER_STATE_IDLE = "3";

    /**
     * 'processing': Indicates that jobs are processing; new jobs will wait
     * before processing.
     */
    public static final String PRINTER_STATE_PROCESSING = "4";

    /**
     * 'stopped': Indicates that no jobs can be processed and intervention is
     * required.
     */
    public static final String PRINTER_STATE_STOPPED = "5";

    /*
     * Attribute names
     */
    public static final String ATTR_DEVICE_URI = "device-uri";

    public static final String ATTR_PRINTER_URI_SUPPORTED =
            "printer-uri-supported";
    public static final String ATTR_URI_SECURITY_SUPPORTED =
            "uri-security-supported";
    public static final String ATTR_URI_AUTH_SUPPORTED =
            "uri-authentication-supported";
    public static final String ATTR_PRINTER_NAME = "printer-name";
    public static final String ATTR_PRINTER_LOCATION = "printer-location";
    public static final String ATTR_PRINTER_INFO = "printer-info";
    public static final String ATTR_PRINTER_MORE_INFO = "printer-more-info";
    public static final String ATTR_PRINTER_DRIVER_INSTALLER =
            "printer-driver-installer";
    public static final String ATTR_PRINTER_MAKE_MODEL =
            "printer-make-and-model";
    public static final String ATTR_PRINTER_MORE_INFO_MANUFACTURER =
            "printer-more-info-manufacturer";
    public static final String ATTR_PRINTER_STATE = "printer-state";
    public static final String ATTR_PRINTER_STATE_CHANGE_TIME =
            "printer-state-change-time";
    public static final String ATTR_PRINTER_STATE_REASONS =
            "printer-state-reasons";
    public static final String ATTR_PRINTER_STATE_MESSAGE =
            "printer-state-message";
    public static final String ATTR_IPP_VERSIONS_SUPP =
            "ipp-versions-supported";
    public static final String ATTR_OPERATIONS_SUPPORTED =
            "operations-supported";
    public static final String ATTR_MULTIPLE_DOC_JOBS_SUPPORTED =
            "multiple-document-jobs-supported";
    public static final String ATTR_CHARSET_CONFIGURED = "charset-configured";
    public static final String ATTR_CHARSET_SUPPORTED = "charset-supported";
    public static final String ATTR_NATURAL_LANG_CONFIGURED =
            "natural-language-configured";
    public static final String ATTR_GENERATED_NATURAL_LANG_SUPPORTED =
            "generated-natural-language-supported";
    public static final String ATTR_DOC_FORMAT_DEFAULT =
            "document-format-default";
    public static final String ATTR_DOC_FORMAT_SUPPORTED =
            "document-format-supported";
    public static final String ATTR_PRINTER_IS_ACCEPTING_JOBS =
            "printer-is-accepting-jobs";
    public static final String ATTR_QUEUES_JOB_COUNT = "queued-job-count";
    public static final String ATTR_PRINTER_MSG_FROM_OPERATOR =
            "printer-message-from-operator";
    public static final String ATTR_COLOR_SUPPORTED = "color-supported";
    public static final String ATTR_REF_URI_SCHEMES_SUPPORTED =
            "reference-uri-schemes-supported";
    public static final String ATTR_PDL_OVERRIDE_SUPPORTED =
            "pdl-override-supported";
    public static final String ATTR_PRINTER_UP_TIME = "printer-up-time";
    public static final String ATTR_PRINTER_CURRENT_TIME =
            "printer-current-time";
    public static final String ATTR_MULTIPLE_OPERATION_TIME_OUT =
            "multiple-operation-time-out";
    public static final String ATTR_COMPRESSION_SUPPORTED =
            "compression-supported";
    public static final String ATTR_JOB_K_OCTETS_SUPPORTED =
            "job-k-octets-supported";
    public static final String ATTR_JOB_IMPRESSIONS_SUPPORTED =
            "job-impressions-supported";
    public static final String ATTR_JOB_MEDIA_SHEETS_SUPPORTED =
            "job-media-sheets-supported";
    public static final String ATTR_PAGES_PER_MIN = "pages-per-minute";
    public static final String ATTR_PAGES_PER_MIN_COLOR =
            "pages-per-minute-color";

    /**
     * This OPTIONAL extension enables an IPP client to query the printer for
     * the set of job attributes that can be set by the client during a
     * Create-Job, Print-Job, Validate-Job, or Print-URI operation.
     *
     * <a href=
     * "ftp://ftp.pwg.org/pub/pwg/candidates/cs-ippjobprinterext10-20101030-5100.11.pdf"
     * >PWG-5100.11-2010 IPP: Job and Printer Extensions – Set 2 (JPS2) 30
     * October 2010</a>
     */
    public static final String ATTR_JOB_CREATION_ATTRIBUTES_SUPPORTED =
            "job-creation-attributes-supported";

    /**
     *
     */
    public static final String ATTR_JOB_SETTABLE_ATTRIBUTES_SUPPORTED =
            "job-settable-attributes-supported";

    /**
     * CUPS extension.
     */
    public static final String ATTR_CUPS_VERSION = "cups-version";

    /**
     * <pre>
     * +----------------------------+---------------------------+-----------+
     * |      Attribute             |     Syntax                | REQUIRED? |
     * +----------------------------+---------------------------+-----------+
     * | printer-uri-supported      | 1setOf uri                |  REQUIRED |
     * +----------------------------+---------------------------+-----------+
     * | uri-security-supported     | 1setOf type2 keyword      |  REQUIRED |
     * +----------------------------+---------------------------+-----------+
     * | uri-authentication-        | 1setOf type2 keyword      |  REQUIRED |
     * |     supported              |                           |           |
     * +----------------------------+---------------------------+-----------+
     * | printer-name               | name (127)                |  REQUIRED |
     * +----------------------------+---------------------------+-----------+
     * | printer-location           | text (127)                |           |
     * +----------------------------+---------------------------+-----------+
     * | printer-info               | text (127)                |           |
     * +----------------------------+---------------------------+-----------+
     * | printer-more-info          | uri                       |           |
     * +----------------------------+---------------------------+-----------+
     * | printer-driver-installer   | uri                       |           |
     * +----------------------------+---------------------------+-----------+
     * | printer-make-and-model     | text (127)                |           |
     * +----------------------------+---------------------------+-----------+
     * | printer-more-info-         | uri                       |           |
     * | manufacturer               |                           |           |
     * +----------------------------+---------------------------+-----------+
     * | printer-state              | type1 enum                |  REQUIRED |
     * +----------------------------+---------------------------+-----------+
     * | printer-state-reasons      | 1setOf type2 keyword      |  REQUIRED |
     * +----------------------------+---------------------------+-----------+
     * | printer-state-message      | text (MAX)                |           |
     * +----------------------------+---------------------------+-----------+
     * | ipp-versions-supported     | 1setOf type2 keyword      |  REQUIRED |
     * +----------------------------+---------------------------+-----------+
     * | operations-supported       | 1setOf type2 enum         |  REQUIRED |
     * +----------------------------+---------------------------+-----------+
     * | multiple-document-jobs-    | boolean                   |           |
     * |     supported              |                           |           |
     * +----------------------------+---------------------------+-----------+
     * | charset-configured         | charset                   |  REQUIRED |
     * +----------------------------+---------------------------+-----------+
     * | charset-supported          | 1setOf charset            |  REQUIRED |
     * +----------------------------+---------------------------+-----------+
     * | natural-language-configured| naturalLanguage           |  REQUIRED |
     * +----------------------------+---------------------------+-----------+
     * | generated-natural-language-| 1setOf naturalLanguage    |  REQUIRED |
     * | supported                  |                           |           |
     * +----------------------------+---------------------------+-----------+
     * | document-format-default    | mimeMediaType             |  REQUIRED |
     * +----------------------------+---------------------------+-----------+
     * | document-format-supported  | 1setOf mimeMediaType      |  REQUIRED |
     * +----------------------------+---------------------------+-----------+
     * | printer-is-accepting-jobs  | boolean                   |  REQUIRED |
     * +----------------------------+---------------------------+-----------+
     * | queued-job-count           | integer (0:MAX)           |  REQUIRED |
     * +----------------------------+---------------------------+-----------+
     * | printer-message-from-      | text (127)                |           |
     * | operator                   |                           |           |
     * +----------------------------+---------------------------+-----------+
     * | color-supported            | boolean                   |           |
     * +----------------------------+---------------------------+-----------+
     * | reference-uri-schemes-     | 1setOf uriScheme          |           |
     * |   supported                |                           |           |
     * +----------------------------+---------------------------+-----------+
     * | pdl-override-supported     | type2 keyword             |  REQUIRED |
     * +----------------------------+---------------------------+-----------+
     * | printer-up-time            | integer (1:MAX)           |  REQUIRED |
     * +----------------------------+---------------------------+-----------+
     * | printer-current-time       | dateTime                  |           |
     * +----------------------------+---------------------------+-----------+
     * | multiple-operation-time-out| integer (1:MAX)           |           |
     * +----------------------------+---------------------------+-----------+
     * | compression-supported      | 1setOf type3 keyword      |  REQUIRED |
     * +----------------------------+---------------------------+-----------+
     * | job-k-octets-supported     | rangeOfInteger (0:MAX)    |           |
     * +----------------------------+---------------------------+-----------+
     * | job-impressions-supported  | rangeOfInteger (0:MAX)    |           |
     * +----------------------------+---------------------------+-----------+
     * | job-media-sheets-supported | rangeOfInteger (0:MAX)    |           |
     * +----------------------------+---------------------------+-----------+
     * | pages-per-minute           | integer(0:MAX)            |           |
     * +----------------------------+---------------------------+-----------+
     * | pages-per-minute-color     | integer(0:MAX)            |           |
     * +----------------------------+---------------------------+-----------+
     * </pre>
     *
     */
    private final IppAttr[] attributes =
            {

                    /*
                     * 4.4.1 printer-uri-supported (1setOf uri)
                     */
                    new IppAttr(ATTR_PRINTER_URI_SUPPORTED, IppUri.instance()),

                    /*
                     * 4.4.2 uri-authentication-supported (1setOf type2 keyword)
                     */
                    new IppAttr(ATTR_URI_AUTH_SUPPORTED, IppKeyword.instance()),

                    /*
                     * 4.4.3 uri-security-supported (1setOf type2 keyword)
                     */
                    new IppAttr(ATTR_URI_SECURITY_SUPPORTED,
                            IppKeyword.instance()),

                    /*
                     * 4.4.4 printer-name (name(127))
                     */
                    new IppAttr(ATTR_PRINTER_NAME, new IppName(127)),

                    /*
                     * 4.4.5 printer-location (text(127))
                     */
                    new IppAttr(ATTR_PRINTER_LOCATION, new IppText(127)),

                    /*
                     * 4.4.6 printer-info (text(127))
                     */
                    new IppAttr(ATTR_PRINTER_INFO, new IppText(127)),

                    /*
                     * 4.4.7 printer-more-info (uri)
                     */
                    new IppAttr(ATTR_PRINTER_MORE_INFO, IppUri.instance()),

                    /*
                     * 4.4.8 printer-driver-installer (uri)
                     */
                    new IppAttr(ATTR_PRINTER_DRIVER_INSTALLER,
                            IppUri.instance()),

                    /*
                     * 4.4.9 printer-make-and-model (text(127))
                     */
                    new IppAttr(ATTR_PRINTER_MAKE_MODEL, new IppText(127)),

                    /*
                     * 4.4.10 printer-more-info-manufacturer (uri)
                     */
                    new IppAttr(ATTR_PRINTER_MORE_INFO_MANUFACTURER,
                            IppUri.instance()),

                    /*
                     * 4.4.11 printer-state (type1 enum)
                     *
                     * This REQUIRED Printer attribute identifies the current
                     * state of the device. The "printer-state reasons"
                     * attribute augments the "printer-state" attribute to give
                     * more detailed information about the Printer in the given
                     * printer state.
                     *
                     * A Printer object need only update this attribute before
                     * responding to an operation which requests the attribute;
                     * the Printer object NEED NOT update this attribute
                     * continually, since asynchronous event notification is not
                     * part of IPP/1.1. A Printer NEED NOT implement all values
                     * if they are not applicable to a given implementation.
                     *
                     * The following standard enum values are defined:
                     *
                     * Value Symbolic Name and Description
                     *
                     * '3' 'idle': Indicates that new jobs can start processing
                     * without waiting.
                     *
                     * '4' 'processing': Indicates that jobs are processing; new
                     * jobs will wait before processing.
                     *
                     * '5' 'stopped': Indicates that no jobs can be processed
                     * and intervention is required.
                     *
                     * Values of "printer-state-reasons", such as
                     * 'spool-area-full' and 'stopped-partly', MAY be used to
                     * provide further information.
                     */
                    new IppAttr(ATTR_PRINTER_STATE, IppEnum.instance()),

                    /*
                     *
                     */
                    new IppAttr(ATTR_PRINTER_STATE_CHANGE_TIME,
                            IppInteger.instance()),

                    /*
                     * 4.4.12 printer-state-reasons (1setOf type2 keyword)
                     *
                     * ...
                     */
                    new IppAttr(ATTR_PRINTER_STATE_REASONS,
                            IppKeyword.instance()),

                    /*
                     * 4.4.13 printer-state-message (text(MAX))
                     */
                    new IppAttr(ATTR_PRINTER_STATE_MESSAGE, IppText.instance()),

                    /*
                     * 4.4.14 ipp-versions-supported (1setOf type2 keyword)
                     */
                    new IppAttr(ATTR_IPP_VERSIONS_SUPP, IppKeyword.instance()),

                    /*
                     * 4.4.15 operations-supported (1setOf type2 enum)
                     */
                    new IppAttr(ATTR_OPERATIONS_SUPPORTED, IppEnum.instance()),

                    // 4.4.16 multiple-document-jobs-supported (boolean)

                    /*
                     * 4.4.17 charset-configured (charset)
                     */
                    new IppAttr(ATTR_CHARSET_CONFIGURED, IppCharset.instance()),

                    /*
                     * 4.4.18 charset-supported (1setOf charset)
                     */
                    new IppAttr(ATTR_CHARSET_SUPPORTED, IppCharset.instance()),

                    /*
                     * 4.4.19 natural-language-configured (naturalLanguage)
                     */
                    new IppAttr(ATTR_NATURAL_LANG_CONFIGURED,
                            IppNaturalLanguage.instance()),

                    /*
                     * 4.4.20 generated-natural-language-supported (1setOf
                     * naturalLanguage)
                     */
                    new IppAttr(ATTR_GENERATED_NATURAL_LANG_SUPPORTED,
                            IppNaturalLanguage.instance()),

                    /*
                     * 4.4.21 document-format-default (mimeMediaType)
                     */
                    new IppAttr(ATTR_DOC_FORMAT_DEFAULT,
                            IppMimeMediaType.instance()),

                    /*
                     * 4.4.22 document-format-supported (1setOf mimeMediaType)
                     */
                    new IppAttr(ATTR_DOC_FORMAT_SUPPORTED,
                            IppMimeMediaType.instance()),

                    /*
                     * 4.4.23 printer-is-accepting-jobs (boolean)
                     */
                    new IppAttr(ATTR_PRINTER_IS_ACCEPTING_JOBS,
                            IppBoolean.instance()),

                    /*
                     * 4.4.24 queued-job-count (integer(0:MAX))
                     */
                    new IppAttr(ATTR_QUEUES_JOB_COUNT, new IppInteger(0)),

                    /*
                     * 4.4.25 printer-message-from-operator (text(127))
                     */
                    new IppAttr(ATTR_PRINTER_MSG_FROM_OPERATOR,
                            new IppText(127)),

                    /*
                     * 4.4.26 color-supported (boolean)
                     */
                    new IppAttr(ATTR_COLOR_SUPPORTED, IppBoolean.instance()),

                    /*
                     * 4.4.27 reference-uri-schemes-supported (1setOf uriScheme)
                     */
                    new IppAttr(ATTR_REF_URI_SCHEMES_SUPPORTED,
                            IppUriScheme.instance()),

                    /*
                     * 4.4.28 pdl-override-supported (type2 keyword)
                     */
                    new IppAttr(ATTR_PDL_OVERRIDE_SUPPORTED,
                            IppKeyword.instance()),

                    /*
                     * 4.4.29 printer-up-time (integer(1:MAX))
                     */
                    new IppAttr(ATTR_PRINTER_UP_TIME, new IppInteger(1)),

                    /*
                     * 4.4.30 printer-current-time (dateTime)
                     */
                    new IppAttr(ATTR_PRINTER_CURRENT_TIME,
                            IppDateTime.instance()),

                    /*
                     * 4.4.31 multiple-operation-time-out (integer(1:MAX))
                     */
                    new IppAttr(ATTR_MULTIPLE_OPERATION_TIME_OUT,
                            new IppInteger(1)),

                    /*
                     * 4.4.32 compression-supported (1setOf type3 keyword)
                     */
                    new IppAttr(ATTR_COMPRESSION_SUPPORTED,
                            IppKeyword.instance()),

                    /*
                     * 4.4.33 job-k-octets-supported (rangeOfInteger(0:MAX))
                     */
                    new IppAttr(ATTR_JOB_K_OCTETS_SUPPORTED,
                            IppRangeOfInteger.instance()),

                    /*
                     * 4.4.34 job-impressions-supported (rangeOfInteger(0:MAX))
                     */
                    new IppAttr(ATTR_JOB_IMPRESSIONS_SUPPORTED,
                            IppRangeOfInteger.instance()),

                    /*
                     * 4.4.35 job-media-sheets-supported (rangeOfInteger(0:MAX))
                     */
                    new IppAttr(ATTR_JOB_MEDIA_SHEETS_SUPPORTED,
                            IppRangeOfInteger.instance()),

                    /*
                     *
                     */
                    new IppAttr(ATTR_JOB_CREATION_ATTRIBUTES_SUPPORTED,
                            IppKeyword.instance()),

                    new IppAttr(ATTR_JOB_SETTABLE_ATTRIBUTES_SUPPORTED,
                            IppKeyword.instance()),

                    /*
                     * 4.4.36 pages-per-minute (integer(0:MAX))
                     */
                    new IppAttr(ATTR_PAGES_PER_MIN, new IppInteger(0,
                            IppInteger.MAX)),
                    /*
                     * 4.4.37 pages-per-minute-color (integer(0:MAX))
                     */
                    new IppAttr(ATTR_PAGES_PER_MIN_COLOR, new IppInteger(0,
                            IppInteger.MAX)),

                    /*
                     * CUPS extenstion
                     */
                    new IppAttr(ATTR_CUPS_VERSION, IppText.instance())

            };

    /**
     * The SingletonHolder is loaded on the first execution of
     * {@link IppDictPrinterDescAttr#instance()} or the first access to
     * {@link SingletonHolder#INSTANCE}, not before.
     * <p>
     * <a href=
     * "http://en.wikipedia.org/wiki/Singleton_pattern#The_solution_of_Bill_Pugh"
     * >The Singleton solution of Bill Pugh</a>
     * </p>
     */
    private static class SingletonHolder {
        public static final IppDictPrinterDescAttr INSTANCE =
                new IppDictPrinterDescAttr();
    }

    /**
     * Gets the singleton instance.
     *
     * @return
     */
    public static IppDictPrinterDescAttr instance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     *
     */
    private IppDictPrinterDescAttr() {
        init(attributes);
    }

    @Override
    public IppAttr getAttr(String keyword, IppValueTag valueTag) {
        /*
         * Ignore the value tag.
         */
        return getAttr(keyword);
    }
}
