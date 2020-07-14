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

import org.savapage.core.ipp.routing.IppRoutingListener;
import org.savapage.core.jpa.IppQueue;

/**
 *
 * @author Rijk Ravestein
 *
 */
public abstract class AbstractIppJobOperation extends AbstractIppOperation {

    /**
     *
     */
    private final IppQueue queue;

    /**
     *
     */
    private final boolean clientIpAccessToQueue;

    /**
     *
     */
    private final String trustedIppClientUserId;

    /**
     * If {@code true}, the trustedIppClientUserId overrules the requesting
     * user.
     */
    private final boolean trustedUserAsRequester;

    /** */
    private final String originatorIp;

    /**
     * Can be {@code null}.
     */
    private final IppRoutingListener ippRoutingListener;

    /** */
    private final AbstractIppPrintJobReq request;

    /** */
    private final AbstractIppPrintJobRsp response;

    /**
     *
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
     * @param req
     *            IPP Request.
     * @param rsp
     *            IPP Response.
     */
    protected AbstractIppJobOperation(final IppQueue queue,
            final boolean clientIpAccessToQueue,
            final String trustedIppClientUserId,
            final boolean trustedUserAsRequester, final IppOperationContext ctx,
            final AbstractIppPrintJobReq req,
            final AbstractIppPrintJobRsp rsp) {

        this.originatorIp = ctx.getRemoteAddr();
        this.queue = queue;
        this.clientIpAccessToQueue = clientIpAccessToQueue;
        this.trustedIppClientUserId = trustedIppClientUserId;
        this.trustedUserAsRequester = trustedUserAsRequester;
        this.request = req;
        this.response = rsp;
        this.ippRoutingListener = ctx.getIppRoutingListener();

    }

    /**
     * @return {@link IppRoutingListener}, or {@code null} when not present.
     */
    public IppRoutingListener getIppRoutingListener() {
        return ippRoutingListener;
    }

    /**
     * @return {@link IppQueue}.
     */
    public IppQueue getQueue() {
        return this.queue;
    }

    /**
     * @return The originator's IP address.
     */
    public String getOriginatorIp() {
        return this.originatorIp;
    }

    /**
     * Is the remoteAddr (client) and requesting user allowed to print?
     *
     * @return {@code true} if remoteAddr (client) and requesting user are
     *         allowed to print to this queue.
     */
    public boolean isAuthorized() {
        return this.hasClientIpAccessToQueue()
                && (this.isTrustedQueue()
                        || this.getTrustedIppClientUserId() != null)
                && this.isRequestingUserTrusted();
    }

    /**
     * Get the user id of the Person who is currently trusted on the IPP client.
     *
     * @return {@code null} if there is NO trusted user.
     */
    public String getTrustedIppClientUserId() {
        return this.trustedIppClientUserId;
    }

    /**
     * @return {@code true} if IPP "requesting-user" is trusted.
     */
    protected abstract boolean isRequestingUserTrusted();

    /**
     * @return {@code true} if the trustedIppClientUserId overrules the
     *         requesting user.
     */
    public boolean isTrustedUserAsRequester() {
        return this.trustedUserAsRequester;
    }

    /**
     *
     * @return {@code true} if printed to trusted queue.
     */
    public boolean isTrustedQueue() {
        return this.queue != null && this.queue.getTrusted();
    }

    /**
     *
     * @return {@code true} if remote client IP address has access to queue.
     */
    public boolean hasClientIpAccessToQueue() {
        return this.clientIpAccessToQueue;
    }

    /**
     * @return IPP request.
     */
    public AbstractIppPrintJobReq getRequest() {
        return this.request;
    }

    /**
     * @return IPP response.
     */
    public AbstractIppPrintJobRsp getResponse() {
        return response;
    }

}
