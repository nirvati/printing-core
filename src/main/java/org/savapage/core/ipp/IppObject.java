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
 * This object type models relevant aspects of a real-world entity such as a
 * real printer or real print job. Each object type is defined as a set of
 * possible attributes that may be supported by instances of that object type.
 * For each object (instance), the actual set of supported attributes and values
 * describe a specific implementation. The object’s attributes and values
 * describe its state, capabilities, realizable features, job processing
 * functions, and default behaviors and characteristics (RFC2911 - page 12).
 *
 */
public class IppObject {

    /*
     * All Printer and Job objects are identified by a Uniform Resource
     * Identifier (URI) [RFC2396] so that they can be persistently and
     * unambiguously referenced.
     */

    /*
     * Each attribute included in the set of attributes defining an object type
     * is labeled as:
     *
     * - "REQUIRED": each object MUST support the attribute.
     *
     * - "RECOMMENDED": each object SHOULD support the attribute.
     *
     * - "OPTIONAL": each object MAY support the attribute.
     */

    /*
     * IPP objects support operations. An operation consists of a request and a
     * response. When a client communicates with an IPP object, the client
     * issues an operation request to the URI for that object.
     *
     * Operation requests and responses have parameters that identify the
     * operation.
     *
     * Operations also have attributes that affect the run-time characteristics
     * of the operation (the intended target, localization information, etc.).
     * These operation-specific attributes are called operation attributes (as
     * compared to object attributes such as Printer object attributes or Job
     * object attributes).
     *
     * Each request carries along with it any operation attributes, object
     * attributes, and/or document data required to perform the operation.
     *
     * Each request requires a response from the object. Each response indicates
     * success or failure of the operation with a status code as a response
     * parameter. The response contains any operation attributes, object
     * attributes, and/or status messages generated during the execution of the
     * operation request.
     */

}
