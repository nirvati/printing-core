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

import java.io.InputStream;
import java.io.OutputStream;

/**
 * This REQUIRED operation allows a client to cancel a Print Job from the time
 * the job is created up to the time it is completed, canceled, or aborted.
 * <p>
 * Since a Job might already be printing by the time a Cancel-Job is received,
 * some media sheet pages might be printed before the job is actually
 * terminated.
 * </p>
 * <p>
 * The IPP object MUST accept or reject the request based on the job's current
 * state and transition the job to the indicated new state as follows:
 * </p>
 * <p>
 * ...
 * </p>
 */
public class IppCancelJobOperation extends AbstractIppOperation {

    @Override
    protected final void process(final InputStream istr, final OutputStream ostr)
            throws Exception {
        // no code intended
    }

}
