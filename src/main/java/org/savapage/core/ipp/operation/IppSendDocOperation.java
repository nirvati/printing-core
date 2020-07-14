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

import org.savapage.core.jpa.IppQueue;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class IppSendDocOperation extends IppPrintJobOperation {

    /**
     * @param queue
     *            The print queue. Can be {@code null} is no queue matches the
     *            URI.
     * @param clientIpAccessToQueue
     *            Indicates if client has access to printing. When {@code false}
     *            , printing is NOT allowed.
     * @param trustedIppClientUserId
     *            The user id of the trusted on the IPP client. If {@code null}
     *            there is NO trusted user.
     * @param trustedUserAsRequester
     *            If {@code true}, the trustedIppClientUserId overrules the
     *            requesting user.
     * @param ctx
     *            The operation context.
     */
    public IppSendDocOperation(final IppQueue queue,
            final boolean clientIpAccessToQueue,
            final String trustedIppClientUserId,
            final boolean trustedUserAsRequester,
            final IppOperationContext ctx) {

        super(queue, clientIpAccessToQueue, trustedIppClientUserId,
                trustedUserAsRequester, ctx, new IppSendDocReq(),
                new IppSendDocRsp());
    }

}
