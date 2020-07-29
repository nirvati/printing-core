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
import org.savapage.core.ipp.attribute.syntax.IppOctetString;
import org.savapage.core.ipp.attribute.syntax.IppRangeOfInteger;
import org.savapage.core.ipp.attribute.syntax.IppResolution;
import org.savapage.core.ipp.attribute.syntax.IppText;
import org.savapage.core.ipp.attribute.syntax.IppUri;
import org.savapage.core.ipp.attribute.syntax.IppUriScheme;
import org.savapage.core.ipp.encoding.IppValueTag;

/**
 * A dictionary of "printer-description" attributes.
 *
 * @author Rijk Ravestein
 *
 */
public final class IppDictPrinterDescAttr extends AbstractIppDict {

    /**
     * The URF document format (MIME type) MUST be supported to print from Apple
     * iOS (iPhone, iPod, iPad).
     */
    public static final String DOCUMENT_FORMAT_URF = "image/urf";

    /**
     * A document format required by IPP everywhere.
     */
    public static final String DOCUMENT_FORMAT_JPEG = "image/jpeg";

    /**
     * A document format required by IPP everywhere.
     */
    public static final String DOCUMENT_FORMAT_PWG_RASTER = "image/pwg-raster";

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

    /**
     * text(1023) - PWG5107.2
     */
    public static final String ATTR_PRINTER_DEVICE_ID = "printer-device-id";

    public static final String ATTR_PRINTER_NAME = "printer-name";

    public static final String ATTR_PRINTER_LOCATION = "printer-location";
    public static final String ATTR_PRINTER_GEO_LOCATION =
            "printer-geo-location";

    public static final String ATTR_PRINTER_ORGANIZATION =
            "printer-organization";
    public static final String ATTR_PRINTER_ORGANIZATIONAL_UNIT =
            "printer-organizational-unit";

    public static final String ATTR_PRINTER_INFO = "printer-info";

    public static final String ATTR_PRINTER_DRIVER_INSTALLER =
            "printer-driver-installer";

    public static final String ATTR_PRINTER_MAKE_MODEL =
            "printer-make-and-model";

    /** 1setOf uri. */
    public static final String ATTR_PRINTER_ICONS = "printer-icons";

    /** */
    public static final String ATTR_PRINTER_STATE = "printer-state";

    /** type2 enum. */
    public static final String ATTR_PRINTER_TYPE = "printer-type";

    /**
     * member-names (1setof name(127))
     * <p>
     * Specifies each of the printer-name attributes of the member printers and
     * classes. Each name corresponds to the same element of the member-uris
     * attribute.
     * </p>
     */
    public static final String ATTR_MEMBER_NAMES = "member-names";

    /** */
    public static final String ATTR_COPIES_SUPPORTED = "copies-supported";

    /** */
    public static final String ATTR_PRINT_COLOR_MODE_DEFAULT =
            "print-color-mode-default";
    /** */
    public static final String ATTR_PRINT_COLOR_MODE_SUPPORTED =
            "print-color-mode-supported";

    /** */
    public static final String ATTR_PRINT_CONTENT_OPTIMIZE_DEFAULT =
            "print-content-optimize-default";
    /** */
    public static final String ATTR_PRINT_CONTENT_OPTIMIZE_SUPPORTED =
            "print-content-optimize-supported";

    /** */
    public static final String ATTR_JOB_PASSWORD_ENCRYPTION_SUPPORTED =
            "job-password-encryption-supported";

    /** Boolean. */
    public static final String ATTR_JOB_IDS_SUPPORTED = "job-ids-supported";

    /** Boolean. */
    public static final String ATTR_PAGE_RANGES_SUPPORTED =
            "page-ranges-supported";

    /** 1setOf type2 keyword. */
    public static final String ATTR_PWG_RASTER_DOCUMENT_TYPE_SUPPORTED =
            "pwg-raster-document-type-supported";

    /** 1setOf resolution */
    public static final String ATTR_PWG_RASTER_DOCUMENT_RESOLUTION_SUPPORTED =
            "pwg-raster-document-resolution-supported";

    /** 1setOf keyword */
    public static final String ATTR_OVERRIDES_SUPPORTED = "overrides-supported";

    /** type2 keyword. */
    public static final String ATTR_PRINT_RENDERING_INTENT_DEFAULT =
            "print-rendering-intent-default";

    /** 1setOf type2 keyword. */
    public static final String ATTR_PRINT_RENDERING_INTENT_SUPPORTED =
            "print-rendering-intent-supported";

    /** */
    public static final String ATTR_MULTIPLE_DOCUMENT_HANDLING_SUPPORTED =
            "multiple-document-handling-supported";

    /** dateTime. */
    public static final String ATTR_PRINTER_CONFIG_CHANGE_DATE_TIME =
            "printer-config-change-date-time";

    /** integer(1:MAX). */
    public static final String ATTR_PRINTER_CONFIG_CHANGE_TIME =
            "printer-config-change-time";

    /** 1setOf octetString(MAX). */
    public static final String ATTR_PRINTER_SUPPLY = "printer-supply";

    /** 1setOf text(MAX). */
    public static final String ATTR_PRINTER_SUPPLY_DESCRIPTION =
            "printer-supply-description";

    /** uri. */
    public static final String ATTR_PRINTER_SUPPLY_INFO_URI =
            "printer-supply-info-uri";

    /** 1setOf type2 keyword. */
    public static final String ATTR_WHICH_JOBS_SUPPORTED =
            "which-jobs-supported";

    /** */
    public static final String ATTR_PRINTER_STATE_CHANGE_TIME =
            "printer-state-change-time";

    /** */
    public static final String ATTR_PRINTER_STATE_REASONS =
            "printer-state-reasons";
    /** */
    public static final String ATTR_PRINTER_STATE_MESSAGE =
            "printer-state-message";

    /** 1setOf type2 keyword. */
    public static final String ATTR_IPP_VERSIONS_SUPP =
            "ipp-versions-supported";

    /** 1setOf type2 keyword. */
    public static final String ATTR_IPP_FEATURES_SUPP =
            "ipp-features-supported";
    /** */
    public static final String ATTR_OPERATIONS_SUPPORTED =
            "operations-supported";

    /** boolean */
    public static final String ATTR_MULTIPLE_DOCUMENT_JOBS_SUPPORTED =
            "multiple-document-jobs-supported";

    /** */
    public static final String ATTR_CHARSET_CONFIGURED = "charset-configured";

    /** */
    public static final String ATTR_CHARSET_SUPPORTED = "charset-supported";

    /** */
    public static final String ATTR_NATURAL_LANG_CONFIGURED =
            "natural-language-configured";

    /** */
    public static final String ATTR_GENERATED_NATURAL_LANG_SUPPORTED =
            "generated-natural-language-supported";

    /** */
    public static final String ATTR_DOC_FORMAT_DEFAULT =
            "document-format-default";

    /** */
    public static final String ATTR_DOC_FORMAT_SUPPORTED =
            "document-format-supported";

    /** */
    public static final String ATTR_PRINTER_IS_ACCEPTING_JOBS =
            "printer-is-accepting-jobs";

    /** */
    public static final String ATTR_QUEUES_JOB_COUNT = "queued-job-count";

    /** */
    public static final String ATTR_PRINTER_MSG_FROM_OPERATOR =
            "printer-message-from-operator";
    /** */
    public static final String ATTR_COLOR_SUPPORTED = "color-supported";

    /** */
    public static final String ATTR_REF_URI_SCHEMES_SUPPORTED =
            "reference-uri-schemes-supported";
    /** */
    public static final String ATTR_PDL_OVERRIDE_SUPPORTED =
            "pdl-override-supported";
    /** */
    public static final String ATTR_PRINTER_UP_TIME = "printer-up-time";
    /** */
    public static final String ATTR_PRINTER_CURRENT_TIME =
            "printer-current-time";

    /** integer(1:MAX). */
    public static final String ATTR_MULTIPLE_OPERATION_TIME_OUT =
            "multiple-operation-time-out";

    /** type2 keyword */
    public static final String ATTR_MULTIPLE_OPERATION_TIME_OUT_ACTION =
            "multiple-operation-time-out-action";
    /** */
    public static final String ATTR_COMPRESSION_SUPPORTED =
            "compression-supported";
    /** */
    public static final String ATTR_JOB_K_OCTETS_SUPPORTED =
            "job-k-octets-supported";
    /** */
    public static final String ATTR_JOB_IMPRESSIONS_SUPPORTED =
            "job-impressions-supported";
    /** */
    public static final String ATTR_JOB_MEDIA_SHEETS_SUPPORTED =
            "job-media-sheets-supported";
    /** */
    public static final String ATTR_PAGES_PER_MIN = "pages-per-minute";

    /** collection. */
    public static final String ATTR_MEDIA_COL_DEFAULT = "media-col-default";

    /** 1setOf collection. */
    public static final String ATTR_MEDIA_COL_READY = "media-col-ready";

    /**
     * Keyword names of the member attributes supported in the "media-col"
     * Job/Document Template attribute: 1setOf keyword.
     */
    public static final String ATTR_MEDIA_COL_SUPPORTED = "media-col-supported";

    /**
     * Keyword names of the member attributes supported in the "media-size"
     * Job/Document Template attribute: 1setOf collection.
     */
    public static final String ATTR_MEDIA_SIZE_SUPPORTED =
            "media-size-supported";

    /** 1setOf integer(0:MAX). */
    public static final String ATTR_MEDIA_TOP_MARGIN_SUPPORTED =
            "media-top-margin-supported";

    /** 1setOf integer(0:MAX). */
    public static final String ATTR_MEDIA_BOTTOM_MARGIN_SUPPORTED =
            "media-bottom-margin-supported";

    /** 1setOf integer(0:MAX). */
    public static final String ATTR_MEDIA_LEFT_MARGIN_SUPPORTED =
            "media-left-margin-supported";

    /** 1setOf integer(0:MAX). */
    public static final String ATTR_MEDIA_RIGHT_MARGIN_SUPPORTED =
            "media-right-margin-supported";

    /** */
    public static final String ATTR_PAGES_PER_MIN_COLOR =
            "pages-per-minute-color";
    /** */
    public static final String ATTR_MEDIA_READY = "media-ready";

    /**
     * This OPTIONAL extension enables an IPP client to query the printer for
     * the set of job attributes that can be set by the client during a
     * Create-Job, Print-Job, Validate-Job, or Print-URI operation.
     */
    public static final String ATTR_JOB_CREATION_ATTRIBUTES_SUPPORTED =
            "job-creation-attributes-supported";

    /** boolean. */
    public static final String ATTR_PREFERRED_ATTRIBUTES_SUPPORTED =
            "preferred-attributes-supported";

    /** 1setOf keyword. */
    public static final String ATTR_PRINTER_GET_ATTRIBUTES_SUPPORTED =
            "printer-get-attributes-supported";
    /** */
    public static final String ATTR_JOB_SETTABLE_ATTRIBUTES_SUPPORTED =
            "job-settable-attributes-supported";
    /** */
    public static final String ATTR_PRINTER_MORE_INFO_MANUFACTURER =
            "printer-more-info-manufacturer";

    /** */
    public static final String ATTR_PRINTER_MORE_INFO = "printer-more-info";

    /** */
    public static final String ATTR_PRINTER_UUID = "printer-uuid";

    /** */
    public static final String ATTR_DOC_PASSWORD_SUPPORTED =
            "document-password-supported";

    /** */
    public static final String ATTR_PRINTER_STATE_CHANGE_DATE_TIME =
            "printer-state-change-date-time";

    /** 1setOf type2 keyword. */
    public static final String ATTR_IDENTIFY_ACTIONS_DEFAULT =
            "identify-actions-default";

    /** 1setOf type2 keyword. */
    public static final String ATTR_IDENTIFY_ACTIONS_SUPPORTED =
            "identify-actions-supported";

    /** CUPS extension. */
    public static final String ATTR_CUPS_VERSION = "cups-version";

    /**
     * 1setOf type2 keyword: CUPS 1.3/macOS 10.5.
     *
     * The client OPTIONALLY supplies one or more authentication keywords that
     * are required to communicate with the printer/remote queue.
     *
     * <a href="https://www.cups.org/doc/spec-ipp.html">spec-ipp.html</a>
     */
    public static final String ATTR_AUTH_INFO_REQUIRED = "auth-info-required";

    /** */
    private final IppAttr[] attributes = {
            // 1setOf uri
            new IppAttr(ATTR_PRINTER_URI_SUPPORTED, IppUri.instance()),
            // 1setOf type2 keyword
            new IppAttr(ATTR_URI_AUTH_SUPPORTED, IppKeyword.instance()),
            // 1setOf type2 keyword
            new IppAttr(ATTR_URI_SECURITY_SUPPORTED, IppKeyword.instance()),

            new IppAttr(ATTR_PRINTER_NAME, new IppName(127)),
            new IppAttr(ATTR_PRINTER_DEVICE_ID, new IppText(1023)),
            new IppAttr(ATTR_PRINTER_LOCATION, new IppText(127)),
            new IppAttr(ATTR_PRINTER_GEO_LOCATION, IppUri.instance()),

            // (1setOf text(MAX))
            new IppAttr(ATTR_PRINTER_ORGANIZATION, new IppText()),
            // (1setOf text(MAX))
            new IppAttr(ATTR_PRINTER_ORGANIZATIONAL_UNIT, new IppText()),

            new IppAttr(ATTR_PRINTER_INFO, new IppText(127)),
            new IppAttr(ATTR_PRINTER_MORE_INFO, IppUri.instance()),
            new IppAttr(ATTR_PRINTER_DRIVER_INSTALLER, IppUri.instance()),
            new IppAttr(ATTR_PRINTER_MAKE_MODEL, new IppText(127)),
            new IppAttr(ATTR_PRINTER_MORE_INFO_MANUFACTURER, IppUri.instance()),

            new IppAttr(ATTR_PRINTER_STATE, IppEnum.instance()),
            new IppAttr(ATTR_PRINTER_STATE_CHANGE_TIME, IppInteger.instance()),

            // 1setOf type2 keyword
            new IppAttr(ATTR_PRINTER_STATE_REASONS, IppKeyword.instance()),
            new IppAttr(ATTR_PRINTER_STATE_MESSAGE, IppText.instance()),

            // 1setOf type2 keyword
            new IppAttr(ATTR_IPP_VERSIONS_SUPP, IppKeyword.instance()),
            // 1setOf type2 keyword
            new IppAttr(ATTR_IPP_FEATURES_SUPP, IppKeyword.instance()),

            new IppAttr(ATTR_IDENTIFY_ACTIONS_DEFAULT, IppKeyword.instance()),
            new IppAttr(ATTR_IDENTIFY_ACTIONS_SUPPORTED, IppKeyword.instance()),

            // 1setOf type2 enum
            new IppAttr(ATTR_OPERATIONS_SUPPORTED, IppEnum.instance()),

            new IppAttr(ATTR_CHARSET_CONFIGURED, IppCharset.instance()),

            // 1setOf charset
            new IppAttr(ATTR_CHARSET_SUPPORTED, IppCharset.instance()),

            new IppAttr(ATTR_NATURAL_LANG_CONFIGURED,
                    IppNaturalLanguage.instance()),
            // 1setOf naturalLanguage
            new IppAttr(ATTR_GENERATED_NATURAL_LANG_SUPPORTED,
                    IppNaturalLanguage.instance()),

            new IppAttr(ATTR_DOC_FORMAT_DEFAULT, IppMimeMediaType.instance()),

            // 1setOf mimeMediaType
            new IppAttr(ATTR_DOC_FORMAT_SUPPORTED, IppMimeMediaType.instance()),

            new IppAttr(ATTR_DOC_PASSWORD_SUPPORTED, new IppInteger(0)),
            new IppAttr(ATTR_PRINTER_IS_ACCEPTING_JOBS, IppBoolean.instance()),
            new IppAttr(ATTR_QUEUES_JOB_COUNT, new IppInteger(0)),
            new IppAttr(ATTR_PRINTER_MSG_FROM_OPERATOR, new IppText(127)),
            new IppAttr(ATTR_COLOR_SUPPORTED, IppBoolean.instance()),

            // 1setOf uriScheme
            new IppAttr(ATTR_REF_URI_SCHEMES_SUPPORTED,
                    IppUriScheme.instance()),

            new IppAttr(ATTR_PDL_OVERRIDE_SUPPORTED, IppKeyword.instance()),
            new IppAttr(ATTR_PRINTER_UP_TIME, new IppInteger(1)),

            new IppAttr(ATTR_PRINTER_CURRENT_TIME, IppDateTime.instance()),

            new IppAttr(ATTR_MULTIPLE_OPERATION_TIME_OUT,
                    IppInteger.instance()),
            new IppAttr(ATTR_MULTIPLE_OPERATION_TIME_OUT_ACTION,
                    IppKeyword.instance()),

            // 1setOf type3 keyword
            new IppAttr(ATTR_COMPRESSION_SUPPORTED, IppKeyword.instance()),

            new IppAttr(ATTR_JOB_K_OCTETS_SUPPORTED,
                    IppRangeOfInteger.instance()),

            new IppAttr(ATTR_JOB_IMPRESSIONS_SUPPORTED,
                    IppRangeOfInteger.instance()),
            new IppAttr(ATTR_JOB_MEDIA_SHEETS_SUPPORTED,
                    IppRangeOfInteger.instance()),

            new IppAttr(ATTR_JOB_CREATION_ATTRIBUTES_SUPPORTED,
                    IppKeyword.instance()),

            new IppAttr(ATTR_PREFERRED_ATTRIBUTES_SUPPORTED,
                    IppBoolean.instance()),

            new IppAttr(ATTR_JOB_SETTABLE_ATTRIBUTES_SUPPORTED,
                    IppKeyword.instance()),

            new IppAttr(ATTR_PRINTER_GET_ATTRIBUTES_SUPPORTED,
                    IppKeyword.instance()),

            new IppAttr(ATTR_MEDIA_READY, IppKeyword.instance()),
            new IppAttr(ATTR_WHICH_JOBS_SUPPORTED, IppKeyword.instance()),
            new IppAttr(ATTR_MEDIA_COL_SUPPORTED, IppKeyword.instance()),

            new IppAttr(ATTR_PAGES_PER_MIN, IppInteger.instance()),
            new IppAttr(ATTR_PAGES_PER_MIN_COLOR, IppInteger.instance()),

            // 1setOf integer(0:MAX)
            new IppAttr(ATTR_MEDIA_TOP_MARGIN_SUPPORTED, IppInteger.instance()),

            // 1setOf integer(0:MAX)
            new IppAttr(ATTR_MEDIA_BOTTOM_MARGIN_SUPPORTED,
                    IppInteger.instance()),

            // 1setOf integer(0:MAX)
            new IppAttr(ATTR_MEDIA_LEFT_MARGIN_SUPPORTED,
                    IppInteger.instance()),

            // 1setOf integer(0:MAX)
            new IppAttr(ATTR_MEDIA_RIGHT_MARGIN_SUPPORTED,
                    IppInteger.instance()),

            new IppAttr(ATTR_PRINTER_UUID, IppUri.instance()),

            new IppAttr(ATTR_PRINTER_SUPPLY, IppOctetString.instance()),
            new IppAttr(ATTR_PRINTER_SUPPLY_DESCRIPTION, IppText.instance()),
            new IppAttr(ATTR_PRINTER_SUPPLY_INFO_URI, IppUri.instance()),

            new IppAttr(ATTR_PRINT_COLOR_MODE_DEFAULT, IppKeyword.instance()),
            new IppAttr(ATTR_PRINT_COLOR_MODE_SUPPORTED, IppKeyword.instance()),

            new IppAttr(ATTR_PRINT_CONTENT_OPTIMIZE_DEFAULT,
                    IppKeyword.instance()),
            new IppAttr(ATTR_PRINT_CONTENT_OPTIMIZE_SUPPORTED,
                    IppKeyword.instance()),

            new IppAttr(ATTR_MULTIPLE_DOCUMENT_JOBS_SUPPORTED,
                    IppBoolean.instance()),

            new IppAttr(ATTR_JOB_IDS_SUPPORTED, IppBoolean.instance()),
            new IppAttr(ATTR_PAGE_RANGES_SUPPORTED, IppBoolean.instance()),

            new IppAttr(ATTR_PWG_RASTER_DOCUMENT_TYPE_SUPPORTED,
                    IppKeyword.instance()),
            new IppAttr(ATTR_PWG_RASTER_DOCUMENT_RESOLUTION_SUPPORTED,
                    IppResolution.instance()),

            new IppAttr(ATTR_OVERRIDES_SUPPORTED, IppKeyword.instance()),
            new IppAttr(ATTR_PRINT_RENDERING_INTENT_DEFAULT,
                    IppKeyword.instance()),
            new IppAttr(ATTR_PRINT_RENDERING_INTENT_SUPPORTED,
                    IppKeyword.instance()),

            new IppAttr(ATTR_PRINTER_STATE_CHANGE_DATE_TIME,
                    IppDateTime.instance()),
            new IppAttr(ATTR_PRINTER_CONFIG_CHANGE_DATE_TIME,
                    IppDateTime.instance()),
            new IppAttr(ATTR_PRINTER_CONFIG_CHANGE_TIME, IppInteger.instance()),

            // CUPS extension
            new IppAttr(ATTR_CUPS_VERSION, IppText.instance()),
            new IppAttr(ATTR_AUTH_INFO_REQUIRED, IppKeyword.instance()),

            new IppAttr(ATTR_PRINTER_TYPE, IppKeyword.instance()),

            new IppAttr(ATTR_JOB_PASSWORD_ENCRYPTION_SUPPORTED,
                    IppKeyword.instance()),
            new IppAttr(ATTR_MULTIPLE_DOCUMENT_HANDLING_SUPPORTED,
                    IppKeyword.instance()),
            new IppAttr(ATTR_PRINTER_ICONS, IppUri.instance())
            //
    };

    /** */
    private static class SingletonHolder {
        /** */
        public static final IppDictPrinterDescAttr INSTANCE =
                new IppDictPrinterDescAttr();
    }

    /**
     * @return The singleton instance.
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
