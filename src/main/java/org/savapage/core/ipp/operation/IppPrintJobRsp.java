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
package org.savapage.core.ipp.operation;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.savapage.core.ipp.attribute.IppAttr;
import org.savapage.core.ipp.attribute.IppAttrGroup;
import org.savapage.core.ipp.attribute.IppAttrValue;
import org.savapage.core.ipp.attribute.IppDictJobDescAttr;
import org.savapage.core.ipp.attribute.syntax.IppCharset;
import org.savapage.core.ipp.attribute.syntax.IppJobState;
import org.savapage.core.ipp.attribute.syntax.IppNaturalLanguage;
import org.savapage.core.ipp.attribute.syntax.IppText;
import org.savapage.core.ipp.encoding.IppDelimiterTag;

/**
 *
 * 3.2.1.2 Print-Job Response.
 *
 * @author Datraverse B.V.
 */
public class IppPrintJobRsp extends AbstractIppResponse {

    /**
     *
     */
    private static volatile int theJobId = 0;

    /*
     * The Printer object MUST return to the client the following sets of
     * attributes as part of the Print-Job Response:
     */

    /*
     * Group 1: Operation Attributes
     *
     * Status Message: In addition to the REQUIRED status code returned in every
     * response, the response OPTIONALLY includes a "status-message" (text(255))
     * and/or a "detailed-status-message" (text(MAX)) operation attribute as
     * described in sections 13 and 3.1.6.
     *
     * If the client supplies unsupported or conflicting Job Template attributes
     * or values, the Printer object MUST reject or accept the Print-Job request
     * depending on the whether the client supplied a 'true' or 'false' value
     * for the "ipp-attribute- fidelity" operation attribute. See the
     * Implementer's Guide [IPP-IIG] for a complete description of the suggested
     * steps for processing a create request.
     *
     * Natural Language and Character Set: The "attributes-charset" and
     * "attributes-natural-language" attributes as described in section 3.1.4.2.
     */

    /*
     * Group 2: Unsupported Attributes
     *
     * See section 3.1.7 for details on returning Unsupported Attributes.
     *
     * The value of the "ipp-attribute-fidelity" supplied by the client does not
     * affect what attributes the Printer object returns in this group. The
     * value of "ipp-attribute-fidelity" only affects whether the Print-Job
     * operation is accepted or rejected. If the job is accepted, the client may
     * query the job using the Get-Job- Attributes operation requesting the
     * unsupported attributes that were returned in the create response to see
     * which attributes were ignored (not stored on the Job object) and which
     * attributes were stored with other (substituted) values.
     */

    /*
     * Group 3: Job Object Attributes
     *
     *
     * "job-uri" (uri):
     *
     * The Printer object MUST return the Job object's URI by returning the
     * contents of the REQUIRED "job-uri" Job object attribute. The client uses
     * the Job object's URI when directing operations at the Job object. The
     * Printer object always uses its configured security policy when creating
     * the new URI. However, if the Printer object supports more than one URI,
     * the Printer object also uses information about which URI was used in the
     * Print-Job Request to generated the new URI so that the new URI references
     * the correct access channel. In other words, if the Print-Job Request
     * comes in over a secure channel, the Printer object MUST generate a Job
     * URI that uses the secure channel as well.
     *
     *
     * "job-id" (integer(1:MAX)):
     *
     * The Printer object MUST return the Job object's Job ID by returning the
     * REQUIRED "job-id" Job object attribute. The client uses this "job-id"
     * attribute in conjunction with the "printer-uri" attribute used in the
     * Print-Job Request when directing Job operations at the Printer object.
     *
     *
     * "job-state" (type1 enum):
     *
     * The Printer object MUST return the Job object's REQUIRED "job-state"
     * attribute. The value of this attribute (along with the value of the next
     * attribute: "job-state-reasons") is taken from a "snapshot" of the new Job
     * object at some meaningful point in time (implementation defined) between
     * when the Printer object receives the Print-Job Request and when the
     * Printer object returns the response.
     *
     *
     * "job-state-reasons" (1setOf type2 keyword):
     *
     * The Printer object MUST return the Job object's REQUIRED "job-
     * state-reasons" attribute.
     *
     *
     * "job-state-message" (text(MAX)):
     *
     * The Printer object OPTIONALLY returns the Job object's OPTIONAL
     * "job-state-message" attribute. If the Printer object supports this
     * attribute then it MUST be returned in the response. If this attribute is
     * not returned in the response, the client can assume that the
     * "job-state-message" attribute is not supported and will not be returned
     * in a subsequent Job object query.
     *
     *
     * "number-of-intervening-jobs" (integer(0:MAX)):
     *
     * The Printer object OPTIONALLY returns the Job object's OPTIONAL
     * "number-of-intervening-jobs" attribute. If the Printer object supports
     * this attribute then it MUST be returned in the response. If this
     * attribute is not returned in the response, the client can assume that the
     * "number-of-intervening-jobs" attribute is not supported and will not be
     * returned in a subsequent Job object query.
     *
     * Note: Since any printer state information which affects a job's state is
     * reflected in the "job-state" and "job-state-reasons" attributes, it is
     * sufficient to return only these attributes and no specific printer status
     * attributes.
     */

    /*
     * Note: In addition to the MANDATORY parameters required for every
     * operation response, the simplest response consists of the just the
     * "attributes-charset" and "attributes-natural-language" operation
     * attributes and the "job-uri", "job-id", and "job-state" Job Object
     * Attributes. In this simplest case, the status code is 'successful- ok'
     * and there is no "status-message" or "detailed-status-message" operation
     * attribute.
     */

    // private boolean isPrintingAllowed() {
    // return printAccessToQueue
    // && (trustedQueue || (authenticatedWebAppUser != null));
    // }

    /**
     *
     * @param operation
     * @param ostr
     * @throws IOException
     */
    public final void process(final IppPrintJobOperation operation,
            final IppPrintJobReq request, final OutputStream ostr)
            throws IOException {

        List<IppAttrGroup> attrGroups = new ArrayList<>();

        IppAttrGroup group = null;
        IppAttrValue value = null;
        IppAttr attr = null;
        IppStatusCode requestStatus = null;
        String jobState = null;
        String jobStateReasons = null;

        /*
         * First test on an exception, and DRM violation since this overrules
         * everything.
         */
        if (request.hasDeferredException()
                || request.isDrmViolationDetected()) {

            /*
             * EFFECT IN CLIENT OS
             *
             * Windows XP : The job will NOT print, and is NOT held in the local
             * queue.
             *
             * Ubuntu 11.10 : The job will NOT print, and is NOT held in the
             * local queue. Ubuntu will show a message the "Document printed".
             *
             * http://localhost:631/jobs?which_jobs=completed shows the job as
             * "completed" WITHOUT "status-message" (see below).
             *
             * EFFECT IN CLIENT WEBAPP
             *
             * Pop-up that printing failed.
             */
            requestStatus = IppStatusCode.OK;

            jobState = IppJobState.STATE_ABORTED;
            jobStateReasons = "aborted-by-system";

        } else if (operation.isAuthorized()) {
            /*
             *
             * EFFECT IN CLIENT WEBAPP
             *
             * Pop-up that printing was successful.
             */
            requestStatus = IppStatusCode.OK;

            jobState = IppJobState.STATE_COMPLETED;
            jobStateReasons = "job-completed-successfully";

        } else {
            /*
             * requestStatus = IppStatusCode.CLI_FORBID
             *
             * ----------------------------------------------------------------
             * EFFECT IN CLIENT OS
             *
             * ----- Windows XP ----------------------------------
             *
             * Job will show status "error" in local queue. After login to
             * SavaPage locally, you can restart the job, and it will be
             * printed.
             *
             * ----- Ubuntu 11.10 --------------------------------
             *
             * Job will show status "processing" for a while in local queue.
             * Then, Ubuntu pops up an Authentication dialog (why?). After login
             * to SavaPage locally, you can restart the job, and it will be
             * printed.
             *
             * http://localhost:631/jobs?which_jobs=completed shows the job as
             * "canceled" with "status-message" (see below).
             *
             * ----- iOS (iPad) ----------------------------------
             *
             * A pop-up message "You do not have permission to use this printer"
             * with 'Cancel' and 'Retry' buttons.
             *
             * IMPORTANT: the request status of the the ValidationJob request
             * should be OK for this to happen!
             *
             * ----------------------------------------------------------------
             * EFFECT IN CLIENT WEBAPP
             *
             * n/a (because WebApp is not open, otherwise we would not be at
             * this point)
             */
            final boolean askForAuthentication = false; // work in progress...

            if (askForAuthentication) {

                /*
                 * Work in progress...
                 */
                requestStatus = IppStatusCode.CLI_NOAUTH;
                // jobState = IppJobState.STATE_CANCELED;
                // jobStateReasons = "aborted-by-system";

            } else {
                requestStatus = IppStatusCode.CLI_FORBID;
                jobState = IppJobState.STATE_ABORTED;
                jobStateReasons = "aborted-by-system";
            }

        }

        /**
         * Group 1: Operation Attributes
         */
        group = new IppAttrGroup(IppDelimiterTag.OPERATION_ATTR);
        attrGroups.add(group);

        /*
         * Natural Language and Character Set:
         *
         * The "attributes-charset" and "attributes-natural-language" attributes
         * as described in section 3.1.4.2.
         */
        attr = new IppAttr("attributes-charset", new IppCharset());
        value = new IppAttrValue(attr);
        value.addValue("utf-8");
        group.addAttribute(value);

        attr = new IppAttr("attributes-natural-language",
                new IppNaturalLanguage());
        value = new IppAttrValue(attr);
        value.addValue("en-us");
        group.addAttribute(value);

        /*
         * (detailed) messages
         */
        if (!operation.isAuthorized()) {
            /*
             *
             */
            attr = new IppAttr("status-message", new IppText());
            value = new IppAttrValue(attr);
            value.addValue(
                    "before printing login to the SavaPage WebApp first");
            group.addAttribute(value);

            /*
             *
             */
            attr = new IppAttr("detailed-status-message", new IppText());
            value = new IppAttrValue(attr);
            value.addValue("You are printing to an untrusted SavaPage Queue. "
                    + "Make sure you are logged into the SavaPage WebApp, "
                    + "and inspect your local printer queue for held jobs.");
            group.addAttribute(value);

        } else if (request.hasDeferredException()) {

            /*
             *
             */
            attr = new IppAttr("status-message", new IppText());
            value = new IppAttrValue(attr);
            value.addValue("Internal Savapage Error");
            group.addAttribute(value);

            /*
             *
             */
            String msg = request.getDeferredException().getMessage();
            if (StringUtils.isNotBlank(msg)) {
                attr = new IppAttr("detailed-status-message", new IppText());
                value = new IppAttrValue(attr);
                value.addValue(msg);
                group.addAttribute(value);
            }

        } else if (request.isDrmViolationDetected()) {

            /*
             *
             */
            attr = new IppAttr("status-message", new IppText());
            value = new IppAttrValue(attr);
            value.addValue("PostScript Re-Distill not allowed");
            group.addAttribute(value);

            /*
             *
             */
            attr = new IppAttr("detailed-status-message", new IppText());
            value = new IppAttrValue(attr);
            value.addValue("SavaPage is set to disallow printing Encrypted "
                    + "PDF documents.");
            group.addAttribute(value);
        }

        /**
         * Group 2: Unsupported Attributes
         *
         */
        // group = new IppAttrGroup(IppDelimiterTag.UNSUPP_ATTR);
        // attrGroups.add(group);

        /**
         * Group 3: Job Object Attributes
         *
         */
        /*
         * IMPORTANT: hold in separate variable (because of concurrent use of
         * static instance member)!!
         */
        final int jobId = ++theJobId;

        group = new IppAttrGroup(IppDelimiterTag.JOB_ATTR);
        attrGroups.add(group);

        final IppDictJobDescAttr dict = IppDictJobDescAttr.instance();

        attr = dict.getAttr(IppDictJobDescAttr.ATTR_JOB_URI);
        value = new IppAttrValue(attr);
        value.addValue(request.getJobUri(jobId));
        group.addAttribute(value);

        attr = dict.getAttr(IppDictJobDescAttr.ATTR_JOB_ID);
        value = new IppAttrValue(attr);
        value.addValue(String.valueOf(jobId));
        group.addAttribute(value);

        attr = dict.getAttr(IppDictJobDescAttr.ATTR_JOB_STATE);
        value = new IppAttrValue(attr);
        value.addValue(jobState);
        group.addAttribute(value);

        attr = dict.getAttr(IppDictJobDescAttr.ATTR_JOB_STATE_REASONS);
        value = new IppAttrValue(attr);
        value.addValue(jobStateReasons);
        group.addAttribute(value);

        attr = dict.getAttr(IppDictJobDescAttr.ATTR_JOB_STATE_MESSAGE);
        value = new IppAttrValue(attr);
        if (requestStatus == IppStatusCode.OK) {
            value.addValue("Open SavaPage Web App to see the print.");
        } else {
            if (operation.isAuthorized()) {
                value.addValue("Something went wrong");
            } else {
                value.addValue("Login to the SavaPage WebApp before printing!");
            }
        }
        group.addAttribute(value);

        //
        write(operation, requestStatus, attrGroups, ostr,
                request.getAttributesCharset());
    }
}
