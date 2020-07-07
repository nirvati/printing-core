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

import java.io.InputStream;

/**
 *
 * @author Rijk Ravestein
 *
 */
public class IppCancelJobReq extends AbstractIppRequest {

    /**
     * Group 1: Operation Attributes
     *
     *
     * Natural Language and Character Set:
     *
     * The "attributes-charset" and "attributes-natural-language" attributes as
     * described in section 3.1.4.1.
     *
     * Target:
     *
     * Either (1) the "printer-uri" (uri) plus "job-id" (integer(1:MAX))or (2)
     * the "job-uri" (uri) operation attribute(s) which define the target for
     * this operation as described in section 3.1.5.
     *
     *
     * Requesting User Name:
     *
     * The "requesting-user-name" (name(MAX)) attribute SHOULD be supplied by
     * the client as described in section 8.3.
     *
     *
     * "message" (text(127)):
     *
     * The client OPTIONALLY supplies this attribute. The Printer object
     * OPTIONALLY supports this attribute. It is a message to the operator. This
     * "message" attribute is not the same as the "job-message-from-operator"
     * attribute. That attribute is used to report a message from the operator
     * to the end user that queries that attribute. This "message" operation
     * attribute is used to send a message from the client to the operator along
     * with the operation request. It is an implementation decision of how or
     * where to display this message to the operator (if at all).
     *
     */

    @Override
    public void process(final AbstractIppOperation operation,
            final InputStream istr) {
        // no code intended
    }

}
