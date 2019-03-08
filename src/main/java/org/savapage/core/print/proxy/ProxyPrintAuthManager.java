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
package org.savapage.core.print.proxy;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import javax.persistence.EntityManager;

import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.jpa.Printer;
import org.savapage.core.jpa.User;
import org.savapage.core.rfid.RfidEvent;
import org.savapage.core.rfid.RfidNumberFormat;
import org.savapage.core.rfid.RfidReaderManager;
import org.savapage.core.services.ServiceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A singleton that manages Print Authentication for Proxy Printers. It provides
 * services to submit a Proxy Print Request, and to wait for Proxy Print
 * Authentication Events.
 * <ul>
 * <li>Use {@link #submitRequest(String, String, ProxyPrintInboxReq)} to submit
 * a Proxy Print Request.</li>
 * <li>Use {@link #waitForAuth(EntityManager, String, String, long, TimeUnit)}
 * to blocking wait for an authentication event.</li>
 * </ul>
 *
 * @author Rijk Ravestein
 *
 */
public final class ProxyPrintAuthManager {

    /**
     * The logger.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(ProxyPrintAuthManager.class);

    /**
     * Lookup for actual {@link ProxyPrintInboxReq} for Proxy Printer: for each
     * proxy printer there is a single request which is being handled.
     *
     * NOTE: {@link ConcurrentHashMap} is considered overkill (and wasting
     * resources) in our case. The map is lazy filled and static in a way that
     * no entries are removed from the map (yet).
     */
    private final Map<String, ProxyPrintInboxReq> requests = new HashMap<>();

    /**
     * Reader IP address lookup for Proxy Printer unique name.
     */
    private final Map<String, String> cardReaders = new HashMap<>();

    /**
     * Creates the singleton.
     */
    private ProxyPrintAuthManager() {
    }

    /**
     * The SingletonHolder is loaded on the first execution of
     * {@link ProxyPrintAuthManager#instance()} or the first access to
     * {@link SingletonHolder#INSTANCE}, not before.
     * <p>
     * <a href=
     * "http://en.wikipedia.org/wiki/Singleton_pattern#The_solution_of_Bill_Pugh"
     * >The Singleton solution of Bill Pugh</a>
     * </p>
     */
    private static class SingletonHolder {
        /**
         *
         */
        public static final ProxyPrintAuthManager INSTANCE =
                new ProxyPrintAuthManager();
    }

    /**
     * Gets the singleton instance.
     *
     * @return The singleton.
     */
    public static ProxyPrintAuthManager instance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     *
     * @return The number of seconds after which authentication window expires.
     */
    public static long getMaxRequestAgeSeconds() {
        return ConfigManager.instance()
                .getConfigLong(Key.PROXY_PRINT_DIRECT_EXPIRY_SECS);
    }

    /**
     * Checks if an authentication request is pending for a Proxy Printer.
     *
     * @param printerName
     *            Unique name of the proxy {@link Printer}.
     * @return {@code true} when an authentication is pending.
     */
    public synchronized boolean
            isAuthPendingForPrinter(final String printerName) {
        return isReqPendingForPrinter(printerName);
    }

    /**
     * Checks if an authentication request is pending for a User.
     *
     * @param userKey
     *            The database primary key of the {@link User}.
     * @return {@code true} when an authentication is pending.
     */
    public synchronized boolean isAuthPendingForUser(final Long userKey) {

        for (final Entry<String, ProxyPrintInboxReq> entry : requests
                .entrySet()) {

            final ProxyPrintInboxReq request = entry.getValue();

            if (request.getIdUser().equals(userKey)) {
                return !isExpired(request);
            }
        }
        return false;
    }

    /**
     * Checks if a Print Request for a Proxy Printer is pending.
     *
     * @param printerName
     *            Unique name of the proxy {@link Printer}.
     * @return {@code true} when a request is pending.
     */
    private boolean isReqPendingForPrinter(final String printerName) {

        final ProxyPrintInboxReq reqPending = requests.get(printerName);

        final boolean isPending;

        if (reqPending == null) {

            isPending = false;

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("NO request pending for [" + printerName + "]");
            }
        } else {

            isPending = !isExpired(reqPending);

            if (LOGGER.isTraceEnabled()) {
                if (isPending) {
                    LOGGER.trace("Request for [" + printerName + "] PENDING");
                } else {
                    LOGGER.trace("Pending request for [" + printerName
                            + "] EXPIRED");
                }
            }
        }

        return isPending;
    }

    /**
     * Submits a Print Request for a Proxy Printer. The submit is rejected if a
     * request is pending which is not expired.
     *
     * @param printerName
     *            Unique name of the {@link Printer}.
     * @param cardReaderIp
     *            IP address of the card reader to authenticate with.
     * @param request
     *            The print request.
     * @return {@code true} if the request was accepted, {@code false} if
     *         rejected because another request is pending for less then
     *         {@link #getMaxRequestAgeSeconds()}.
     */
    private synchronized boolean onSubmitRequest(final String printerName,
            final String cardReaderIp, final ProxyPrintInboxReq request) {

        if (isReqPendingForPrinter(printerName)) {
            return false;
        }

        request.setSubmitDate(new Date());

        cardReaders.put(printerName, cardReaderIp);
        requests.put(printerName, request);

        return true;
    }

    /**
     * Checks if request is expired.
     *
     * @param request
     *            The {@link ProxyPrintInboxReq}.
     * @return {@code true} when expired.
     */
    private static boolean isExpired(final ProxyPrintInboxReq request) {

        final Date now = new Date();
        final long ageSeconds =
                (now.getTime() - request.getSubmitDate().getTime()) / 1000;

        final boolean expired = ageSeconds > getMaxRequestAgeSeconds();

        if (LOGGER.isTraceEnabled()) {
            String msg = "Request for [" + request.getPrinterName() + "] aged ["
                    + ageSeconds + "] seconds";
            if (expired) {
                msg += " : EXPIRED";
            }

            LOGGER.trace(msg);
        }

        return expired;
    }

    /**
     * Handles an authentication event and return the matching
     * {@link ProxyPrintInboxReq}. The request status is updated to reflect the
     * new state.
     *
     * @param idUser
     *            Primary key of the requesting user.
     * @param printerName
     *            Unique name of the {@link Printer}.
     * @param event
     *            The {@link RfidEvent}.
     * @return The request of the user or {@code null} when no (matching)
     *         request is found.
     */
    private synchronized ProxyPrintInboxReq onAuthEvent(final Long idUser,
            final String printerName, final RfidEvent event) {

        final ProxyPrintInboxReq request = requests.get(printerName);

        /*
         * CHECK: no pending request.
         */
        if (request == null) {
            return null;
        }

        /*
         * INVARIANT: pending request MUST be of this user.
         */
        if (!idUser.equals(request.getIdUser())) {
            return null;
        }

        /*
         * CHECK: when a timeout occurs or the event is not a card swipe the
         * pending request of the user is returned unchanged.
         */
        if (event == null
                || event.getEvent() != RfidEvent.EventEnum.CARD_SWIPE) {
            return request;
        }

        /*
         * CHECK: when pending request is expired
         */
        if (isExpired(request)) {

            request.setStatus(ProxyPrintInboxReq.Status.EXPIRED);

        } else {
            /*
             * INVARIANT: the owner (User) of the Card Number MUST match the
             * owner (User) of the request.
             */
            final User userDb =
                    ServiceContext.getServiceFactory().getUserService()
                            .findUserByCardNumber(event.getCardNumber());

            if (userDb != null && userDb.getId().equals(request.getIdUser())) {
                request.setStatus(ProxyPrintInboxReq.Status.AUTHENTICATED);
            }
        }
        requests.remove(printerName);
        return request;
    }

    /**
     * Submits a Print Request for a Proxy Printer.
     *
     * @param printerName
     *            Unique name of the {@link Printer}.
     * @param cardReaderIp
     *            IP address of the card reader to authenticate with.
     * @param request
     *            The print request.
     * @return {@code true} if the request was accepted, {@code false} if
     *         rejected because another request is pending for less then
     *         {@link #getMaxRequestAgeSeconds()}.
     */
    public static boolean submitRequest(final String printerName,
            final String cardReaderIp, final ProxyPrintInboxReq request) {
        return instance().onSubmitRequest(printerName, cardReaderIp, request);
    }

    /**
     * Cancels a Print Request for a Proxy Printer AND reports the dummy
     * {@link RfidEvent.EventEnum#VOID} to the {@link RfidReaderManager} so any
     * blocking watch call falls through.
     *
     * @param idUser
     *            Primary key of the user.
     * @param printerName
     *            Unique name of the {@link Printer}.
     * @return {@code true} if the request was cancelled, {@code false} if no
     *         matching request was found.
     * @throws InterruptedException
     *
     */
    private synchronized boolean onCancelRequest(final Long idUser,
            final String printerName) throws InterruptedException {

        ProxyPrintInboxReq reqPending = requests.get(printerName);

        if (reqPending != null && !reqPending.getIdUser().equals(idUser)) {
            return false;
        }
        requests.remove(printerName);

        final String cardReaderIp = cardReaders.get(printerName);

        RfidReaderManager.reportEvent(cardReaderIp,
                new RfidEvent(RfidEvent.EventEnum.VOID));

        return true;
    }

    /**
     * Cancels a Print Request for a Proxy Printer.
     *
     * @param idUser
     *            Primary key of the user (owner of the request).
     * @param printerName
     *            Unique name of the {@link Printer}.
     * @return {@code true} if the request was cancelled, {@code false} if no
     *         matching request was found.
     * @throws InterruptedException
     */
    public static boolean cancelRequest(Long idUser, final String printerName)
            throws InterruptedException {
        return instance().onCancelRequest(idUser, printerName);
    }

    /**
     * Waits for a Print Request authentication by waiting for the RfidEvent of
     * associated Card Reader for max timeout time units.
     *
     * @param idUser
     *            Primary key of the user.
     * @param printerName
     *            Unique name of the {@link Printer}.
     * @param cardReaderIp
     *            The IP-address of the Reader Device.
     * @param rfidNumberFormat
     *            The format of the RFID number.
     * @param timeout
     *            The timeout.
     * @param timeUnit
     *            The timeout time unit.
     * @return The request of the user or {@code null} when no (matching)
     *         request is found.
     * @throws InterruptedException
     *             When interrupted.
     */
    public static ProxyPrintInboxReq waitForAuth(final Long idUser,
            final String printerName, final String cardReaderIp,
            final RfidNumberFormat rfidNumberFormat, final long timeout,
            final TimeUnit timeUnit) throws InterruptedException {

        final RfidEvent event = RfidReaderManager.waitForEvent(cardReaderIp,
                rfidNumberFormat, timeout, timeUnit);

        return instance().onAuthEvent(idUser, printerName, event);
    }

}
