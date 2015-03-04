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
 * Models a print job. A Job object contains documents.
 *
 * @author Datraverse B.V.
 */
public class IppJob extends IppObject {

    /**
     * The characteristics and state of a Job object are described by its
     * attributes. Job attributes are grouped into two groups as follows:
     *
     * - "job-template" attributes: These attributes can be supplied by the
     * client or end user and include job processing instructions which are
     * intended to override any Printer object defaults and/or instructions
     * embedded within the document data. (See section 4.2)
     *
     * - "job-description" attributes: These attributes describe the Job
     * object’s identification, state, size, etc. The client supplies some of
     * these attributes, and the Printer object generates others. (See section
     * 4.3)
     */

    /**
     * An implementation MUST support at least one document per Job object. An
     * implementation MAY support multiple documents per Job object. A document
     * is either:
     *
     * - a stream of document data in a format supported by the Printer object
     * (typically a Page Description Language - PDL), or
     *
     * - a reference to such a stream of document data
     *
     * In IPP/1.1, a document is not modeled as an IPP object, therefore it has
     * no object identifier or associated attributes. All job processing
     * instructions are modeled as Job object attributes. These attributes are
     * called Job Template attributes and they apply equally to all documents
     * within a Job object.
     */

    /**
     * - Each Job object is identified with a Job URI. The Job’s "job-uri"
     * attribute contains the URI.
     *
     * - Each Job object is also identified with Job ID which is a 32- bit,
     * positive integer. The Job’s "job-id" attribute contains the Job ID. The
     * Job ID is only unique within the context of the Printer object which
     * created the Job object.
     *
     * - Each Job object has a "job-printer-uri" attribute which contains the
     * URI of the Printer object that was used to create the Job object. This
     * attribute is used to determine the Printer object that created a Job
     * object when given only the URI for the Job object. This linkage is
     * necessary to determine the languages, charsets, and operations which are
     * supported on that Job (the basis for such support comes from the creating
     * Printer object).
     *
     * - Each Job object has a name (which is not necessarily unique). The
     * client optionally supplies this name in the create request. If the client
     * does not supply this name, the Printer object generates a name for the
     * Job object. The Job object’s "job-name" attribute contains the name.
     */

    /**
     * 3.1.5 Operation Targets
     *
     * For Job operations, the operation is directed at either:
     *
     * - The Job object itself using the Job object’s URI. In this case, the
     * client identifies the target object by supplying the correct URI in the
     * "job-uri (uri)" operation attribute.
     *
     * - The Printer object that created the Job object using both the Printer
     * objects URI and the Job object’s Job ID. Since the Printer object that
     * created the Job object generated the Job ID, it MUST be able to correctly
     * associate the client supplied Job ID with the correct Job object. The
     * client supplies the Printer object’s URI in the "printer-uri (uri)"
     * operation attribute and the Job object’s Job ID in the
     * "job-id (integer(1:MAX))" operation attribute.
     *
     * If the operation is directed at the Job object directly using the Job
     * object’s URI, the client MUST NOT include the redundant "job-id"
     * operation attribute.
     */
}
