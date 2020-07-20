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

import java.net.URI;
import java.net.URISyntaxException;

import org.savapage.core.ipp.attribute.syntax.IppDateTime;
import org.savapage.core.ipp.attribute.syntax.IppEnum;
import org.savapage.core.ipp.attribute.syntax.IppInteger;
import org.savapage.core.ipp.attribute.syntax.IppJobState;
import org.savapage.core.ipp.attribute.syntax.IppKeyword;
import org.savapage.core.ipp.attribute.syntax.IppMimeMediaType;
import org.savapage.core.ipp.attribute.syntax.IppName;
import org.savapage.core.ipp.attribute.syntax.IppResolution;
import org.savapage.core.ipp.attribute.syntax.IppText;
import org.savapage.core.ipp.attribute.syntax.IppUri;
import org.savapage.core.ipp.encoding.IppValueTag;

/**
 * Job Description attribute dictionary on attribute name.
 *
 * @author Rijk Ravestein
 *
 */
public final class IppDictJobDescAttr extends AbstractIppDict {

    public static final String ATTR_JOB_NAME = "job-name";
    public static final String ATTR_JOB_STATE = "job-state";

    public static final String ATTR_JOB_STATE_MESSAGE = "job-state-message";
    public static final String ATTR_JOB_STATE_REASONS = "job-state-reasons";
    public static final String ATTR_JOB_URI = "job-uri";
    public static final String ATTR_JOB_ID = "job-id";
    public static final String ATTR_JOB_UUID = "job-uuid";
    public static final String ATTR_JOB_MORE_INFO = "job-more-info";

    public static final String ATTR_JOB_PRINTER_URI = "job-printer-uri";
    public static final String ATTR_JOB_ORIGINATING_USER_NAME =
            "job-originating-user-name";

    public static final String ATTR_JOB_PRINTER_UP_TIME = "job-printer-up-time";

    /** */
    public static final String ATTR_JOB_IMPRESSIONS = "job-impressions";
    /** */
    public static final String ATTR_JOB_IMPRESSIONS_COMPLETED =
            "job-impressions-completed";

    /** */
    public static final String ATTR_DOCUMENT_NAME_SUPPLIED =
            "document-name-supplied";

    public static final String ATTR_DATE_TIME_AT_CREATION =
            "date-time-at-creation";
    public static final String ATTR_DATE_TIME_AT_COMPLETED =
            "date-time-at-completed";
    public static final String ATTR_DATE_TIME_AT_PROCESSING =
            "date-time-at-processing";

    public static final String ATTR_TIME_AT_CREATION = "time-at-creation";
    public static final String ATTR_TIME_AT_COMPLETED = "time-at-completed";
    public static final String ATTR_TIME_AT_PROCESSING = "time-at-processing";

    public static final String ATTR_COMPRESSION_SUPPLIED =
            "compression-supplied";

    public static final String ATTR_SIDES = "sides";
    public static final String ATTR_PRINT_COLOR_MODE = "print-color-mode";
    public static final String ATTR_PRINT_QUALITY = "print-quality";
    public static final String ATTR_PRINTER_RESOLUTION = "printer-resolution";
    public static final String ATTR_MEDIA = "media";

    /** */
    public static final String ATTR_PRINT_CONTENT_OPTIMIZE =
            "print-content-optimize";

    /** type2 keyword. */
    public static final String ATTR_PRINT_RENDERING_INTENT =
            "print-rendering-intent";

    /** */
    public static final String ATTR_DOC_FORMAT_SUPPLIED =
            "document-format-supplied";

    /**
     *
     */
    private final IppAttr[] attributes = {
            // REQUIRED
            new IppAttr(ATTR_JOB_URI, IppUri.instance()),
            new IppAttr(ATTR_JOB_ID, new IppInteger(1)),
            new IppAttr(ATTR_JOB_PRINTER_URI, IppUri.instance()),
            new IppAttr(ATTR_JOB_MORE_INFO, IppUri.instance()),
            new IppAttr(ATTR_JOB_NAME, IppName.instance()),
            new IppAttr(ATTR_JOB_ORIGINATING_USER_NAME, IppName.instance()),
            new IppAttr(ATTR_JOB_STATE, IppJobState.instance()),
            // 1setOf type2 keyword
            new IppAttr(ATTR_JOB_STATE_REASONS, IppKeyword.instance()),
            //
            new IppAttr(ATTR_JOB_STATE_MESSAGE, IppText.instance()),
            // integer(MIN:MAX)
            new IppAttr(ATTR_TIME_AT_CREATION, IppInteger.instance()),
            // integer(MIN:MAX)
            new IppAttr(ATTR_TIME_AT_PROCESSING, IppInteger.instance()),
            // integer(MIN:MAX)
            new IppAttr(ATTR_TIME_AT_COMPLETED, IppInteger.instance()),

            new IppAttr(ATTR_DATE_TIME_AT_CREATION, IppDateTime.instance()),
            new IppAttr(ATTR_DATE_TIME_AT_PROCESSING, IppDateTime.instance()),
            new IppAttr(ATTR_DATE_TIME_AT_COMPLETED, IppDateTime.instance()),
            new IppAttr(ATTR_COMPRESSION_SUPPLIED, IppKeyword.instance()),
            new IppAttr(ATTR_JOB_UUID, IppUri.instance()),
            new IppAttr(ATTR_JOB_PRINTER_UP_TIME, IppInteger.instance()),

            /* integer(0:MAX) */
            new IppAttr(ATTR_JOB_IMPRESSIONS, IppInteger.instance()),
            /* integer(0:MAX) */
            new IppAttr(ATTR_JOB_IMPRESSIONS_COMPLETED, IppInteger.instance()),
            /* name(MAX) */
            new IppAttr(ATTR_DOCUMENT_NAME_SUPPLIED, IppName.instance()),
            new IppAttr(ATTR_SIDES, IppKeyword.instance()),
            new IppAttr(ATTR_PRINT_COLOR_MODE, IppKeyword.instance()),
            new IppAttr(ATTR_PRINT_QUALITY, IppEnum.instance()),
            new IppAttr(ATTR_PRINTER_RESOLUTION, IppResolution.instance()),
            new IppAttr(ATTR_MEDIA, IppKeyword.instance()),
            new IppAttr(ATTR_PRINT_CONTENT_OPTIMIZE, IppKeyword.instance()),
            new IppAttr(ATTR_PRINT_RENDERING_INTENT, IppKeyword.instance()),
            new IppAttr(ATTR_DOC_FORMAT_SUPPLIED, IppMimeMediaType.instance())
            //
    };

    /**
     * The SingletonHolder is loaded on the first execution of
     * {@link IppDictJobDescAttr#instance()} or the first access to
     * {@link SingletonHolder#INSTANCE}, not before.
     */
    private static class SingletonHolder {
        /** */
        public static final IppDictJobDescAttr INSTANCE =
                new IppDictJobDescAttr();
    }

    /**
     * @return the singleton instance.
     */
    public static IppDictJobDescAttr instance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     * Creates a "job-uri" attribute value.
     * <p>
     * "The Printer object assigns the new Job object a URI which is stored in
     * the "job-uri" Job attribute. This URI is then used by clients as the
     * target for subsequent Job operations. The Printer object generates a Job
     * URI based on its configured security policy and the URI used by the
     * client in the create request."
     * </p>
     *
     * @param printerUri
     *            URI of printer.
     * @param jobId
     *            Job ID.
     * @return 'job-uri' value.
     */
    public static String createJobUri(final String printerUri,
            final String jobId) {

        try {
            final URI uri = new URI(printerUri);

            final StringBuilder jobUri = new StringBuilder(64);

            jobUri.append(uri.getScheme()).append("://").append(uri.getHost());

            if (uri.getPort() != -1) {
                jobUri.append(":").append(uri.getPort());
            }

            final String path = uri.getPath();

            if (path != null && path.length() > 1) {
                jobUri.append(path);
            }

            jobUri.append("/jobs/").append(jobId);

            if (jobUri.length() > IppUri.MAX_OCTETS) {
                throw new IllegalArgumentException(
                        String.format("URI [] exceeds %d octets.",
                                jobUri.toString(), IppUri.MAX_OCTETS));
            }

            return jobUri.toString();

        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    /**
     *
     */
    private IppDictJobDescAttr() {
        init(this.attributes);
    }

    @Override
    public IppAttr getAttr(final String keyword, final IppValueTag valueTag) {
        /*
         * Ignore the value tag.
         */
        return getAttr(keyword);
    }

}
