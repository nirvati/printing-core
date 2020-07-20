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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.savapage.core.concurrent.ReadWriteLockEnum;
import org.savapage.core.ipp.IppProcessingException;
import org.savapage.core.ipp.attribute.syntax.IppJobState;
import org.savapage.core.jpa.IppQueue;

/**
 *
 * @author Rijk Ravestein
 *
 */
public class IppPrintJobOperation extends AbstractIppJobOperation {

    /**
     * @param queue
     *            The print queue.
     * @param authUser
     *            The authenticated user id associated with the IPP client. If
     *            {@code null} there is NO authenticated user.
     * @param isAuthUserIppRequester
     *            If {@code true}, the authUser overrules the IPP requesting
     *            user.
     * @param ctx
     *            The operation context.
     */
    public IppPrintJobOperation(final IppQueue queue, final String authUser,
            final boolean isAuthUserIppRequester,
            final IppOperationContext ctx) {

        super(queue, authUser, isAuthUserIppRequester, ctx,
                new IppPrintJobReq(), new IppPrintJobRsp());
    }

    /**
     * @param queue
     *            The print queue. Can be {@code null} is no queue matches the
     *            URI.
     * @param authUser
     *            The authenticated user id associated with the IPP client. If
     *            {@code null} there is NO authenticated user.
     * @param isAuthUserIppRequester
     *            If {@code true}, the authUser overrules the IPP requesting
     *            user.
     * @param ctx
     *            The operation context.
     * @param req
     *            IPP Request.
     * @param rsp
     *            IPP Response.
     */
    protected IppPrintJobOperation(final IppQueue queue, final String authUser,
            final boolean isAuthUserIppRequester, final IppOperationContext ctx,
            final AbstractIppPrintJobReq req,
            final AbstractIppPrintJobRsp rsp) {

        super(queue, authUser, isAuthUserIppRequester, ctx, req, rsp);
    }

    @Override
    protected final void process(final InputStream istr,
            final OutputStream ostr)
            throws IOException, IppProcessingException {

        /*
         * IMPORTANT: we want to give a response in ALL cases. When an exception
         * occurs, the response will act in such a way that the client will not
         * try again (because we assume that the exception will re-occur when
         * re-tried, leading to an end-less chain of print trials).
         */
        boolean isDbReadLock = false;

        /*
         * Step 0.
         */
        try {
            ReadWriteLockEnum.DATABASE_READONLY.tryReadLock();
            isDbReadLock = true;
        } catch (Exception e) {
            throw new IppProcessingException(
                    IppProcessingException.StateEnum.UNAVAILABLE,
                    e.getMessage());
        }

        try {

            /*
             * Step 1.
             */
            try {
                this.getRequest().processAttributes(this, istr);
            } catch (IOException e) {
                this.getRequest()
                        .setDeferredException(new IppProcessingException(
                                IppProcessingException.StateEnum.INTERNAL_ERROR,
                                e.getMessage()));
            }

            /*
             * Step 2.
             */
            try {
                if (this.isAuthorized()) {
                    this.getRequest().process(this, istr);
                }
            } catch (IOException e) {
                this.getRequest()
                        .setDeferredException(new IppProcessingException(
                                IppProcessingException.StateEnum.INTERNAL_ERROR,
                                e.getMessage()));
            }

            /*
             * Step 3.
             */
            try {
                this.getResponse().process(this, getRequest(), ostr,
                        IppJobState.STATE_COMPLETED);
            } catch (IOException e) {
                this.getRequest()
                        .setDeferredException(new IppProcessingException(
                                IppProcessingException.StateEnum.INTERNAL_ERROR,
                                e.getMessage()));
            }

        } catch (Exception e) {
            this.getRequest()
                    .setDeferredException(new IppProcessingException(
                            IppProcessingException.StateEnum.INTERNAL_ERROR,
                            e.getMessage()));
        } finally {
            if (isDbReadLock) {
                ReadWriteLockEnum.DATABASE_READONLY.setReadLock(false);
            }
        }

        /*
         * Step 4: deferred exception? or not allowed to print?
         */
        this.getRequest().evaluateErrorState(this);
    }

}
