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
package org.savapage.core.ipp.operation;

/**
 *
 * @author rijk
 *
 */
public class IppMessageMixin {

    public static final String CONTENT_TYPE_IPP = "application/ipp";

    /**
     *
     */

    /**
     * Each IPP operation request includes an identifying "operation-id" value.
     */
    // private IppOperationId operationId;

    /**
     * For each request, the client chooses the "request-id" which MUST be an
     * integer (possibly unique depending on client requirements) in the range
     * from 1 to 2**31 - 1 (inclusive).
     *
     * This "request-id" allows clients to manage multiple outstanding requests.
     *
     * The receiving IPP object copies all 32-bits of the client- supplied
     * "request-id" attribute into the response so that the client can match the
     * response with the correct outstanding request, even if the "request-id"
     * is out of range.
     *
     * If the request is terminated before the complete "request-id" is
     * received, the IPP object rejects the request and returns a response with
     * a "request-id" of 0.
     */
    // private int requestId;

    /**
     * 3.1.4 Character Set and Natural Language Operation Attributes
     *
     * Some Job and Printer attributes have values that are text strings and
     * names intended for human understanding rather than machine understanding
     * (see the ’text’ and ’name’ attribute syntax descriptions in section 4.1).
     *
     * The following sections describe two special Operation Attributes called
     * "attributes-charset" and "attributes-natural-language".
     *
     * These attributes are always part of the Operation Attributes group. For
     * most attribute groups, the order of the attributes within the group is
     * not important.
     *
     * However, for these two attributes within the Operation Attributes group,
     * the order is critical.
     *
     * The "attributes-charset" attribute MUST be the first attribute in the
     * group and the "attributes-natural-language" attribute MUST be the second
     * attribute in the group.
     *
     * In other words, these attributes MUST be supplied in every IPP request
     * and response, they MUST come first in the group, and MUST come in the
     * specified order.
     *
     * For job creation operations, the IPP Printer implementation saves these
     * two attributes with the new Job object as Job Description attributes. For
     * the sake of brevity in this document, these operation attribute
     * descriptions are not repeated with every operation request and response,
     * but have a reference back to this section instead.
     *
     * 3.1.4.1 Request Operation Attributes
     *
     * ...
     *
     * 3.1.4.2 Response Operation Attributes
     *
     * ...
     *
     */

    protected IppMessageMixin() {

    }
}
