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

import org.savapage.core.concurrent.ReadWriteLockEnum;
import org.savapage.core.jpa.IppQueue;

/**
 *
 * @author Datraverse B.V.
 */
public class IppPrintJobOperation extends AbstractIppOperation {

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
    private final String authWebAppUser;

    /**
     *
     */
    private final String originatorIp;

    /**
     *
     * @param originatorIp
     *            The IP address of the requesting client.
     * @param queue
     *            The print queue. Can be {@code null} is no queue matches the
     *            URI.
     * @param clientIpAccessToQueue
     * @param authWebAppUser
     */
    public IppPrintJobOperation(final String originatorIp,
            final IppQueue queue, final boolean clientIpAccessToQueue,
            final String authWebAppUser) {

        this.originatorIp = originatorIp;
        this.queue = queue;
        this.clientIpAccessToQueue = clientIpAccessToQueue;
        this.authWebAppUser = authWebAppUser;
    }

    public IppQueue getQueue() {
        return queue;
    }

    /**
     * Gets the originator's IP address.
     *
     * @return
     */
    public String getOriginatorIp() {
        return this.originatorIp;
    }

    @Override
    protected final void
            process(final InputStream istr, final OutputStream ostr)
                    throws Exception {

        /*
         * IMPORTANT: we want to give a response in ALL cases. When an exception
         * occurs, the response will act in such a way that the client will not
         * try again (because we assume that the exception will re-occur when
         * re-tried, leading to an end-less chain of print trials).
         */

        ReadWriteLockEnum.DATABASE_READONLY.setReadLock(true);

        /*
         * Step 1.
         */
        try {
            request.processAttributes(this, istr);
        } catch (Exception e) {
            request.setDeferredException(e);
        }

        /*
         * Step 2.
         */
        try {
            if (isAuthorized()) {
                request.process(istr);
            }
        } catch (Exception e) {
            request.setDeferredException(e);
        }

        /*
         * Step 3.
         */
        try {
            response.process(this, request, ostr);
        } finally {
            ReadWriteLockEnum.DATABASE_READONLY.setReadLock(false);
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
                && (isTrustedQueue() || getAuthWebAppUser() != null)
                && request.isTrustedUser();
    }

    /**
     * Get the uid of the Person who is currently authenticated in User WebApp
     * at same IP-address as the job was issued from.
     *
     * @return {@code null} if no user is authenticated.
     */
    public String getAuthWebAppUser() {
        return authWebAppUser;
    }

    public boolean isTrustedQueue() {
        return (queue == null) ? false : queue.getTrusted();
    }

    public boolean hasClientIpAccessToQueue() {
        return clientIpAccessToQueue;
    }

}
