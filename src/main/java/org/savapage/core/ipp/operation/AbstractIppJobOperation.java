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

import org.apache.commons.lang3.BooleanUtils;
import org.savapage.core.ipp.attribute.IppDictOperationAttr;
import org.savapage.core.ipp.routing.IppRoutingListener;
import org.savapage.core.jpa.IppQueue;

/**
 *
 * @author Rijk Ravestein
 *
 */
public abstract class AbstractIppJobOperation extends AbstractIppOperation {

    /** */
    private final IppQueue queue;

    /**
     * Authenticated user in Web App. If {@code null}, no authenticated user is
     * present.
     */
    private final String authenticatedUser;

    /**
     * If {@code true}, the {@link #authenticatedUser} overrules the IPP
     * {@link IppDictOperationAttr#ATTR_REQUESTING_USER_NAME}.
     */
    private final boolean isAuthUserIppRequester;

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
     * @param ippQueue
     *            The print queue.
     * @param authUser
     *            The authenticated user id associated with the IPP client. If
     *            {@code null} there is NO authenticated user.
     * @param isAuthUserIppReq
     *            If {@code true}, the authUser overrules the IPP requesting
     *            user.
     * @param ctx
     *            The operation context.
     * @param req
     *            IPP Request.
     * @param rsp
     *            IPP Response.
     */
    protected AbstractIppJobOperation(final IppQueue ippQueue,
            final String authUser, final boolean isAuthUserIppReq,
            final IppOperationContext ctx, final AbstractIppPrintJobReq req,
            final AbstractIppPrintJobRsp rsp) {

        this.originatorIp = ctx.getRemoteAddr();
        this.queue = ippQueue;
        this.authenticatedUser = authUser;
        this.isAuthUserIppRequester = isAuthUserIppReq;
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
     * @return {@code true} if user is allowed/authorized to print on queue.
     */
    public boolean isAuthorized() {
        if (BooleanUtils.isTrue(queue.getDisabled())) {
            return false;
        }
        if (this.isAuthUserIppRequester) {
            return this.authenticatedUser != null;
        } else {
            return this.queue.getTrusted();
        }
    }

    /**
     * Get the user id authenticated in User Web App or otherwise.
     *
     * @return {@code null} if there is NO authenticated user.
     */
    public String getAuthenticatedUser() {
        return this.authenticatedUser;
    }

    /**
     * @return {@code true} if
     *         {@link AbstractIppJobOperation#getAuthenticatedUser()} overrules
     *         the requesting user.
     */
    public boolean isAuthUserIppRequester() {
        return this.isAuthUserIppRequester;
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
