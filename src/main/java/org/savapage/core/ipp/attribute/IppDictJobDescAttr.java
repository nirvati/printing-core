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

import org.savapage.core.ipp.attribute.syntax.IppInteger;
import org.savapage.core.ipp.attribute.syntax.IppJobState;
import org.savapage.core.ipp.attribute.syntax.IppKeyword;
import org.savapage.core.ipp.attribute.syntax.IppName;
import org.savapage.core.ipp.attribute.syntax.IppUri;
import org.savapage.core.ipp.encoding.IppEncoder;
import org.savapage.core.ipp.encoding.IppValueTag;

/**
 * Job Description attribute dictionary on attribute name. See: section 4.3 of
 * <a href="http://tools.ietf.org/html/rfc2911">RFC2911</a>.
 *
 * <p>
 * NOTE: <a href="http://tools.ietf.org/html/rfc3382">RFC3382</a> is parsed in
 * {@link IppEncoder.Util#readValueTagValue(IppValueTag, java.io.InputStream, int, java.nio.charset.Charset)}
 * but <b>not</b> interpreted.
 * </p>
 *
 * @author Datraverse B.V.
 */
public final class IppDictJobDescAttr extends AbstractIppDict {

    public static final String ATTR_JOB_NAME = "job-name";
    public static final String ATTR_JOB_STATE = "job-state";
    public static final String ATTR_JOB_URI = "job-uri";
    public static final String ATTR_JOB_ID = "job-id";
    public static final String ATTR_JOB_MORE_INFO = "job-more-info";
    public static final String ATTR_JOB_STATE_REASONS = "job-state-reasons";
    public static final String ATTR_TIME_AT_CREATION = "time-at-creation";
    public static final String ATTR_TIME_AT_COMPLETED = "time-at-completed";
    public static final String ATTR_TIME_AT_PROCESSING = "time-at-processing";
    public static final String ATTR_JOB_PRINTER_URI = "job-printer-uri";
    public static final String ATTR_JOB_ORIGINATING_USER_NAME =
            "job-originating-user-name";
    /**
     *
     */
    private final IppAttr[] attributes = {
            //
            /*
             * 4.3.1 job-uri (uri)
             *
             * The Printer object MUST return the Job object's URI by returning
             * the contents of the REQUIRED "job-uri" Job object attribute.
             *
             * The client uses the Job object's URI when directing operations at
             * the Job object.
             *
             * The Printer object always uses its configured security policy
             * when creating the new URI.
             *
             * However, if the Printer object supports more than one URI, the
             * Printer object also uses information about which URI was used in
             * the Print-Job Request to generated the new URI so that the new
             * URI references the correct access channel.
             *
             * In other words, if the Print-Job Request comes in over a secure
             * channel, the Printer object MUST generate a Job URI that uses the
             * secure channel as well.
             */
            new IppAttr(ATTR_JOB_URI, IppUri.instance()), // REQUIRED

            /*
             * 4.3.2 job-id (integer(1:MAX))
             *
             * The Printer object MUST return the Job object's Job ID by
             * returning the REQUIRED "job-id" Job object attribute.
             *
             * The client uses this "job-id" attribute in conjunction with the
             * "printer-uri" attribute used in the Print-Job Request when
             * directing Job operations at the Printer object.
             */
            new IppAttr(ATTR_JOB_ID, new IppInteger(1)), // REQUIRED

            // 4.3.3 job-printer-uri (uri)
            new IppAttr(ATTR_JOB_PRINTER_URI, IppUri.instance()), // REQUIRED

            // 4.3.4 job-more-info (uri)
            new IppAttr(ATTR_JOB_MORE_INFO, IppUri.instance()),

            // 4.3.5 job-name (name(MAX))
            new IppAttr(ATTR_JOB_NAME, IppName.instance()), // REQUIRED

            // 4.3.6 job-originating-user-name (name(MAX))
            new IppAttr(ATTR_JOB_ORIGINATING_USER_NAME, IppName.instance()), // REQUIRED

            /*
             * 4.3.7 job-state (type1 enum)
             *
             * The Printer object MUST return the Job object's REQUIRED "job-
             * state" attribute.
             *
             * The value of this attribute (along with the value of the next
             * attribute: "job-state-reasons") is taken from a "snapshot" of the
             * new Job object at some meaningful point in time (implementation
             * defined) between when the Printer object receives the Print-Job
             * Request and when the Printer object returns the response.
             */
            // 4.3.7.1 Forwarding Servers
            // 4.3.7.2 Partitioning of Job States
            new IppAttr(ATTR_JOB_STATE, IppJobState.instance()), // REQUIRED

            // 4.3.8 job-state-reasons (1setOf type2 keyword)
            new IppAttr(ATTR_JOB_STATE_REASONS, IppKeyword.instance()), // REQUIRED

            // new IppAttr("job-state-message",),
            // new IppAttr("job-detailed-status-messages",),
            // new IppAttr("job-document-access-errors",),
            // new IppAttr("number-of-documents",),
            // new IppAttr("output-device-assigned",),
            // new IppAttr("job-printer-up-time",),
            // new IppAttr("date-time-at-creation",),
            // new IppAttr("date-time-at-processing",),
            // new IppAttr("date-time-at-completed",),
            // new IppAttr("number-of-intervening-jobs",),
            // new IppAttr("job-message-from-operator",),
            // new IppAttr("job-k-octets",),
            // new IppAttr("job-impressions",),
            // new IppAttr("job-media-sheets",),
            // new IppAttr("job-k-octets-processed",),
            // new IppAttr("job-impressions-completed",),
            // new IppAttr("job-media-sheets-completed",),
            // new IppAttr("attributes-charset",),
            // new IppAttr("attributes-natural-language",),

            //

            // 4.3.9 job-state-message (text(MAX))
            // 4.3.10 job-detailed-status-messages (1setOf text(MAX))
            // 4.3.11 job-document-access-errors (1setOf text(MAX))
            // 4.3.12 number-of-documents (integer(0:MAX))
            // 4.3.13 output-device-assigned (name(127))

            // 4.3.14 Event Time Job Description Attributes
            new IppAttr(ATTR_TIME_AT_PROCESSING, IppInteger.instance()),

            /*
             * 4.3.14.1 time-at-creation (integer(MIN:MAX))
             */
            new IppAttr(ATTR_TIME_AT_CREATION, IppInteger.instance()),

            /*
             * 4.3.14.2 time-at-processing (integer(MIN:MAX))
             */
            new IppAttr(ATTR_TIME_AT_PROCESSING, IppInteger.instance()),

            /*
             * 4.3.14.3 time-at-completed (integer(MIN:MAX))
             */
            new IppAttr(ATTR_TIME_AT_COMPLETED, IppInteger.instance()),

    // 4.3.14.4 job-printer-up-time (integer(1:MAX))
    // 4.3.14.5 date-time-at-creation (dateTime)
    // 4.3.14.6 date-time-at-processing (dateTime)
    // 4.3.14.7 date-time-at-completed (dateTime)

            // 4.3.15 number-of-intervening-jobs (integer(0:MAX))
            // 4.3.16 job-message-from-operator (text(127))
            // 4.3.17 Job Size Attributes
            // 4.3.17.1 job-k-octets (integer(0:MAX))
            // 4.3.17.2 job-impressions (integer(0:MAX))
            // 4.3.17.3 job-media-sheets (integer(0:MAX))
            // 4.3.18 Job Progress Attributes
            // 4.3.18.1 job-k-octets-processed (integer(0:MAX))
            // 4.3.18.2 job-impressions-completed (integer(0:MAX))
            // 4.3.18.3 job-media-sheets-completed (integer(0:MAX))
            // 4.3.19 attributes-charset (charset)
            // 4.3.20 attributes-natural-language (naturalLanguage)
            };

    /**
     * The SingletonHolder is loaded on the first execution of
     * {@link IppDictJobDescAttr#instance()} or the first access to
     * {@link SingletonHolder#INSTANCE}, not before.
     * <p>
     * <a href=
     * "http://en.wikipedia.org/wiki/Singleton_pattern#The_solution_of_Bill_Pugh"
     * >The Singleton solution of Bill Pugh</a>
     * </p>
     */
    private static class SingletonHolder {
        public static final IppDictJobDescAttr INSTANCE =
                new IppDictJobDescAttr();
    }

    /**
     * Gets the singleton instance.
     *
     * @return
     */
    public static IppDictJobDescAttr instance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     *
     */
    private IppDictJobDescAttr() {
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
