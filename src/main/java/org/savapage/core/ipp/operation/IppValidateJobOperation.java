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

import org.savapage.core.cometd.AdminPublisher;
import org.savapage.core.cometd.PubLevelEnum;
import org.savapage.core.cometd.PubTopicEnum;
import org.savapage.core.concurrent.ReadWriteLockEnum;
import org.savapage.core.ipp.IppProcessingException;
import org.savapage.core.jpa.IppQueue;
import org.savapage.core.util.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class IppValidateJobOperation extends AbstractIppOperation {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(IppValidateJobOperation.class);

    private final IppValidateJobReq request;
    private final IppValidateJobRsp response;

    private final IppQueue queue;
    private final String requestedQueueUrlPath;
    private final boolean clientIpAccessToQueue;

    /** */
    private final String trustedIppClientUserId;

    /**
     * If {@code true}, the trustedIppClientUserId overrules the requesting
     * user.
     */
    private final boolean trustedUserAsRequester;

    /** */
    private final String remoteAddr;

    /**
     *
     * @param remoteAddr
     *            The client IP address.
     * @param queue
     *            The print queue. Can be {@code null} is no queue matches the
     *            URI.
     * @param requestedQueueUrlPath
     * @param clientIpAccessToQueue
     *            Indicates if client has access to printing. When {@code false}
     *            , printing is NOT allowed.
     * @param trustedIppClientUserId
     *            The user id of the trusted on the IPP client. If {@code null}
     *            there is NO trusted user.
     * @param trustedUserAsRequester
     *            If {@code true}, the trustedIppClientUserId overrules the
     *            requesting user.
     */
    public IppValidateJobOperation(final String remoteAddr,
            final IppQueue queue, final String requestedQueueUrlPath,
            final boolean clientIpAccessToQueue,
            final String trustedIppClientUserId,
            final boolean trustedUserAsRequester) {

        this.remoteAddr = remoteAddr;
        this.queue = queue;
        this.requestedQueueUrlPath = requestedQueueUrlPath;
        this.clientIpAccessToQueue = clientIpAccessToQueue;
        this.trustedIppClientUserId = trustedIppClientUserId;
        this.trustedUserAsRequester = trustedUserAsRequester;

        this.request = new IppValidateJobReq();
        this.response = new IppValidateJobRsp();
    }

    public IppQueue getQueue() {
        return queue;
    }

    public String getRemoteAddr() {
        return this.remoteAddr;
    }

    @Override
    protected void process(final InputStream istr, final OutputStream ostr)
            throws IppProcessingException {
        /*
         * IMPORTANT: we want to give a response in ALL cases. When an exception
         * occurs, the response will act in such a way that the client will not
         * try again (because we assume that the exception will re-occur when
         * re-tried, leading to an end-less chain of trials).
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
                LOGGER.error(e.getMessage(), e);
                request.setDeferredException(new IppProcessingException(
                        IppProcessingException.StateEnum.INTERNAL_ERROR,
                        e.getMessage()));
            }
            /*
             * Step 2.
             *
             * Since the request.process(istr) is empty, we can skip this step.
             */

            /*
             * Step 3.
             */
            try {
                response.process(this, request, ostr);
            } catch (IOException e) {
                LOGGER.error(e.getMessage(), e);
                request.setDeferredException(new IppProcessingException(
                        IppProcessingException.StateEnum.INTERNAL_ERROR,
                        e.getMessage()));
            }

        } catch (Exception e) {
            LOGGER.error(e.getMessage());
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
        if (request.hasDeferredException()) {

            String userid = request.getRequestingUserName();

            if (request.getUserDb() != null) {
                userid = request.getUserDb().getUserId();
            }

            PubLevelEnum pubLevel;
            String pubMessage;

            pubLevel = PubLevelEnum.ERROR;
            pubMessage = request.getDeferredException().getMessage();

            AdminPublisher.instance().publish(PubTopicEnum.USER, pubLevel,
                    localize("pub-user-print-in-denied", userid,
                            ("/" + requestedQueueUrlPath), remoteAddr,
                            pubMessage));

            throw request.getDeferredException();
        }
    }

    /**
     * Return a localized message string. IMPORTANT: The locale from the
     * application is used.
     *
     * @param key
     *            The key of the message.
     * @param args
     *            The placeholder arguments for the message template.
     *
     * @return The message text.
     */
    private String localize(final String key, final String... args) {
        return Messages.getMessage(getClass(), key, args);
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
     * Gets the trusted user id. This is either the Person currently
     * authenticated in User WebApp at same IP-address as the job was issued
     * from, or an authenticated user from Internet Print.
     *
     * @return {@code null} if no user is authenticated.
     */
    public String getTrustedIppClientUserId() {
        return trustedIppClientUserId;
    }

    /**
     * @return If {@code true}, the trustedIppClientUserId overrules the
     *         requesting user.
     */
    public boolean isTrustedUserAsRequester() {
        return trustedUserAsRequester;
    }

    /**
     * Is the Queue trusted?
     *
     * @return
     */
    public boolean isTrustedQueue() {
        return (queue == null) ? false : queue.getTrusted();
    }

    public boolean hasClientIpAccessToQueue() {
        return clientIpAccessToQueue;
    }

}
