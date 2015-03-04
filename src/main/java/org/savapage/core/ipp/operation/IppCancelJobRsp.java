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
 */
public class IppCancelJobRsp extends AbstractIppResponse {

    /**
     * Group 1: Operation Attributes
     *
     *
     * Status Message:
     *
     * In addition to the REQUIRED status code returned in every response, the
     * response OPTIONALLY includes a "status-message" (text(255)) and/or a
     * "detailed-status-message" (text(MAX)) operation attribute as described in
     * sections 13 and 3.1.6.
     *
     *
     * Natural Language and Character Set:
     *
     * The "attributes-charset" and "attributes-natural-language" attributes as
     * described in section 3.1.4.2.
     */
    /**
     * Group 2: Unsupported Attributes
     *
     * See section 3.1.7 for details on returning Unsupported Attributes.
     *
     * Once a successful response has been sent, the implementation guarantees
     * that the Job will eventually end up in the 'canceled' state. Between the
     * time of the Cancel-Job operation is accepted and when the job enters the
     * 'canceled' job-state (see section 4.3.7), the "job-state-reasons"
     * attribute SHOULD contain the 'processing-to- stop-point' value which
     * indicates to later queries that although the Job might still be
     * 'processing', it will eventually end up in the 'canceled' state, not the
     * 'completed' state.
     */
}
