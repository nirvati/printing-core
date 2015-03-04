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
package org.savapage.core.ipp;

/**
 * Implements the server-side of the IPP/1.1 protocol. Using the protocol, end
 * users may query the attributes of the Printer object and submit print jobs to
 * the Printer object.
 *
 */
public class IppPrinter extends IppObject {

    /*
     * The capabilities and state of a Printer object are described by its
     * attributes. Printer attributes are divided into two groups:
     *
     * - "job-template" attributes: These attributes describe supported job
     * processing capabilities and defaults for the Printer object. (See section
     * 4.2)
     *
     * - "printer-description" attributes: These attributes describe the Printer
     * object’s identification, state, location, references to other sources of
     * information about the Printer object, etc. (see section 4.4)
     */

    /*
     * - Each Printer object is identified with one or more URIs. The Printer’s
     * "printer-uri-supported" attribute contains the URI(s).
     *
     * - The Printer object’s "uri-security-supported" attribute identifies the
     * communication channel security protocols that may or may not have been
     * configured for the various Printer object URIs (e.g., ’tls’ or ’none’).
     *
     * - The Printer object’s "uri-authentication-supported" attribute
     * identifies the authentication mechanisms that may or may not have been
     * configured for the various Printer object URIs (e.g., ’digest’ or
     * ’none’).
     *
     * - Each Printer object has a name (which is not necessarily unique). The
     * administrator chooses and sets this name through some mechanism outside
     * the scope of this IPP/1.1 document. The Printer object’s "printer-name"
     * attribute contains the name.
     */

    /**
     * Every operation request contains the following REQUIRED parameters:
     *
     * <pre>
     * - a "version-number",
     * - an "operation-id",
     * - a "request-id", and
     * - the attributes that are REQUIRED for that type of request.
     * </pre>
     *
     * Every operation response contains the following REQUIRED parameters:
     *
     * <pre>
     * - a "version-number",
     * - a "status-code",
     * - the "request-id" that was supplied in the corresponding request, and
     * - the attributes that are REQUIRED for that type of response.
     * </pre>
     */

    /**
     * 3.1.5 Operation Targets
     *
     * For Printer operations, the operation is always directed at a Printer
     * object using one of its URIs (i.e., one of the values in the Printer
     * object’s "printer-uri-supported" attribute). Even if the Printer object
     * supports more than one URI, the client supplies only one URI as the
     * target of the operation. The client identifies the target object by
     * supplying the correct URI in the "printer-uri (uri)" operation attribute.
     */

    /**
     * <b>REQUIRED</b> - The Print-Job Request: A client that wants to submit a
     * print job with only a single document uses the Print-Job operation. The
     * operation allows for the client to "push" the document data to the
     * Printer object by including the document data in the request itself.
     *
     */
    void reqPrintJob() {
        // TODO

        /**
         * Job submission time is the point in time when a client issues a
         * create request.
         *
         * NOTE (@RRA): A create request also includes our unsupported
         * 'Create-Job' 'Print-URI' Requests.
         *
         * The initial state of every Job object is the ’pending’,
         * ’pending-held’, or ’processing’ state (see section 4.3.7). When the
         * Printer object begins processing the print job, the Job object’s
         * state moves to ’processing’. This is known as job processing time.
         * There are validation checks that must be done at job submission time
         * and others that must be performed at job processing time.
         *
         * At job submission time and at the time a Validate-Job operation is
         * received, the Printer MUST do the following:
         *
         * 1. Process the client supplied attributes and either accept or reject
         * the request
         *
         * 2. Validate the syntax of and support for the scheme of any client
         * supplied URI
         *
         * At job submission time the Printer object MUST validate whether or
         * not the supplied attributes, attribute syntaxes, and values are
         * supported by matching them with the Printer object’s corresponding
         * "xxx-supported" attributes. See section 3.1.7 for details. [IPP- IIG]
         * presents suggested steps for an IPP object to either accept or reject
         * any request and additional steps for processing create requests.
         *
         * At job submission time the Printer object NEED NOT perform the
         * validation checks reserved for job processing time such as:
         *
         * 1. Validating the document data
         *
         * 2. Validating the actual contents of any client supplied URI (resolve
         * the reference and follow the link to the document data)
         *
         * At job submission time, these additional job processing time
         * validation checks are essentially useless, since they require
         * actually parsing and interpreting the document data, are not
         * guaranteed to be 100% accurate, and MUST be done, yet again, at job
         * processing time. Also, in the case of a URI, checking for
         * availability at job submission time does not guarantee availability
         * at job processing time. In addition, at job processing time, the
         * Printer object might discover any of the following conditions that
         * were not detectable at job submission time:
         *
         * - runtime errors in the document data,
         *
         * - nested document data that is in an unsupported format,
         *
         * - the URI reference is no longer valid (i.e., the server hosting the
         * document might be down), or
         *
         * - any other job processing error
         *
         * At job submission time, a Printer object, especially a non-spooling
         * Printer, MAY accept jobs that it does not have enough space for. In
         * such a situation, a Printer object MAY stop reading data from a
         * client for an indefinite period of time. A client MUST be prepared
         * for a write operation to block for an indefinite period of time (see
         * section 5.1 on client conformance).
         *
         * When a Printer object has too little space for starting a new job, it
         * MAY reject a new create request. In this case, a Printer object MUST
         * return a response (in reply to the rejected request) with a status-
         * code of ’server-error-busy’ (see section 14.1.5.8) and it MAY close
         * the connection before receiving all bytes of the operation. A Printer
         * SHOULD indicate that it is temporarily unable to accept jobs by
         * setting the ’spool-space-full’ value in its "printer-state-reasons"
         * attribute and removing the value when it can accept another job (see
         * section 4.4.12).
         *
         * When receiving a ’server-error-busy’ status-code in an operation
         * response, a client MUST be prepared for the Printer object to close
         * the connection before the client has sent all of the data (especially
         * for the Print-Job operation). A client MUST be prepared to keep
         * submitting a create request until the IPP Printer object accepts the
         * create request.
         *
         * At job processing time, since the Printer object has already
         * responded with a successful status code in the response to the create
         * request, if the Printer object detects an error, the Printer object
         * is unable to inform the end user of the error with an operation
         * status code.
         *
         * In this case, the Printer, depending on the error, can set the job
         * object’s "job-state", "job-state-reasons", or "job- state-message"
         * attributes to the appropriate value(s) so that later queries can
         * report the correct job status.
         */
    }

    /**
     * <b>OPTIONAL - UNSUPPORTED</b> - The Print-URI Request: A client that
     * wants to submit a print job with only a single document (where the
     * Printer object "pulls" the document data instead of the client "pushing"
     * the data to the Printer object) uses the Print-URI operation. In this
     * case, the client includes in the request only a URI reference to the
     * document data (not the document data itself).
     */
    private final void reqPrintUri() {
        throw new UnsupportedOperationException("Print-URI Request"
                + " is NOT implemented");
    }

    /**
     * <b>OPTIONAL - UNSUPPORTED</b> - The Create-Job Request: A client that
     * wants to submit a print job with multiple documents uses the Create-Job
     * operation.
     * <p>
     * This operation is followed by an arbitrary number (one or more) of
     * Send-Document and/or Send-URI operations (each creating another document
     * for the newly create Job object). The Send-Document operation includes
     * the document data in the request (the client "pushes" the document data
     * to the printer), and the Send-URI operation includes only a URI reference
     * to the document data in the request (the Printer "pulls" the document
     * data from the referenced location). The last Send-Document or Send-URI
     * request for a given Job object includes a "last-document" operation
     * attribute set to ’true’ indicating that this is the last request.
     * </p>
     */
    private final void reqCreateJob() {
        throw new UnsupportedOperationException("Create-Job Request"
                + " is NOT implemented");
    }

    /**
     * <b>REQUIRED</b>.
     */
    void reqValidateJob() {

    }
}
