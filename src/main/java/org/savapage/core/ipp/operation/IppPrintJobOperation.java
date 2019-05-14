/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2019 Datraverse B.V.
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
import org.savapage.core.ipp.routing.IppRoutingListener;
import org.savapage.core.jpa.IppQueue;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class IppPrintJobOperation extends AbstractIppOperation {

    /**
     *
     */
    private final IppPrintJobReq request = new IppPrintJobReq();

    /**
     *
     */
    private final IppPrintJobRsp response = new IppPrintJobRsp();

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

    /** */
    private final IppRoutingListener ippRoutingListener;

    /**
     *
     * @param originatorIp
     *            The IP address of the requesting client.
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
    public IppPrintJobOperation(final IppQueue queue,
            final boolean clientIpAccessToQueue,
            final String trustedIppClientUserId,
            final boolean trustedUserAsRequester,
            final IppOperationContext ctx) {

        this.originatorIp = ctx.getRemoteAddr();
        this.queue = queue;
        this.clientIpAccessToQueue = clientIpAccessToQueue;
        this.trustedIppClientUserId = trustedIppClientUserId;
        this.trustedUserAsRequester = trustedUserAsRequester;
        this.ippRoutingListener = ctx.getIppRoutingListener();
    }

    public IppRoutingListener getIppRoutingListener() {
        return ippRoutingListener;
    }

    public IppQueue getQueue() {
        return queue;
    }

    /**
     * @return The originator's IP address.
     */
    public String getOriginatorIp() {
        return this.originatorIp;
    }

    @Override
    protected void process(final InputStream istr, final OutputStream ostr)
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
                request.processAttributes(this, istr);
            } catch (IOException e) {
                request.setDeferredException(new IppProcessingException(
                        IppProcessingException.StateEnum.INTERNAL_ERROR,
                        e.getMessage()));
            }

            /*
             * Step 2.
             */
            try {
                if (isAuthorized()) {
                    request.process(istr);
                }
            } catch (IOException e) {
                request.setDeferredException(new IppProcessingException(
                        IppProcessingException.StateEnum.INTERNAL_ERROR,
                        e.getMessage()));
            }

            /*
             * Step 3.
             */
            try {
                response.process(this, request, ostr);
            } catch (IOException e) {
                request.setDeferredException(new IppProcessingException(
                        IppProcessingException.StateEnum.INTERNAL_ERROR,
                        e.getMessage()));
            }

        } catch (Exception e) {
            request.setDeferredException(new IppProcessingException(
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
        request.evaluateErrorState(isAuthorized());
    }

    /**
     * Is the remoteAddr (client) and requesting user allowed to print?
     *
     * @return {@code true} if remoteAddr (client) and requesting user are
     *         allowed to print to this queue.
     */
    public boolean isAuthorized() {
        return hasClientIpAccessToQueue()
                && (isTrustedQueue() || getTrustedIppClientUserId() != null)
                && request.isTrustedUser();
    }

    /**
     * Get the user id of the Person who is currently trusted on the IPP client.
     *
     * @return {@code null} if there is NO trusted user.
     */
    public String getTrustedIppClientUserId() {
        return trustedIppClientUserId;
    }

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
        return queue != null && queue.getTrusted();
    }

    /**
     *
     * @return {@code true} if remote client IP address has access to queue.
     */
    public boolean hasClientIpAccessToQueue() {
        return clientIpAccessToQueue;
    }

}
