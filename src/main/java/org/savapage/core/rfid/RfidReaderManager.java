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
package org.savapage.core.rfid;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * A singleton that manages Network Card Reader instances, identified by their
 * IP-address, and provides services to report and wait for Card Reader Events.
 * See {@link RfidEvent}.
 * <ul>
 * <li>Use {@link #reportEvent(String, RfidEvent)} to report an incoming
 * event.</li>
 * <li>Use {@link #waitForEvent(String, long, TimeUnit)} to blocking wait for an
 * event.</li>
 * </ul>
 *
 * @author Rijk Ravestein
 *
 */
public final class RfidReaderManager {

    /**
     * Lookup {@link RfIdReader} by IP address.
     * <p>
     * Note: {@link ConcurrentHashMap} is considered overkill (and wasting
     * resources) in our case. The map is lazy filled and static in a way that
     * no entries are removed from the map (yet).
     * </p>
     */
    private final Map<String, RfIdReader> readers =
            new HashMap<String, RfIdReader>();

    /**
     * Creates the singleton.
     */
    private RfidReaderManager() {
    }

    /**
     * The SingletonHolder is loaded on the first execution of
     * {@link RfidReaderManager#instance()} or the first access to
     * {@link SingletonHolder#INSTANCE}, not before.
     * <p>
     * <a href=
     * "http://en.wikipedia.org/wiki/Singleton_pattern#The_solution_of_Bill_Pugh"
     * >The Singleton solution of Bill Pugh</a>
     * </p>
     */
    private static class SingletonHolder {
        public static final RfidReaderManager INSTANCE =
                new RfidReaderManager();
    }

    /**
     * Gets the singleton instance.
     *
     * @return
     */
    public static RfidReaderManager instance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     *
     * @param readerIpAddress
     *            The IP-address of the reader.
     * @param event
     * @throws InterruptedException
     */
    private void onEvent(final String readerIpAddress, RfidEvent event)
            throws InterruptedException {
        getReader(readerIpAddress).put(event);
    }

    /**
     *
     * @param readerIpAddress
     *            The IP-address of the reader.
     * @return
     */
    private synchronized RfIdReader getReader(final String readerIpAddress) {
        if (!readers.containsKey(readerIpAddress)) {
            readers.put(readerIpAddress, new RfIdReader());
        }
        return readers.get(readerIpAddress);
    }

    /**
     * Reports an event for a Card Reader.
     *
     * @param readerIpAddress
     *            The IP-address of the reader.
     * @param event
     * @throws InterruptedException
     */
    public static void reportEvent(final String readerIpAddress,
            RfidEvent event) throws InterruptedException {
        instance().onEvent(readerIpAddress, event);
    }

    /**
     * Waits for an RfidEvent for max timeout time units.
     *
     * @param readerIpAddress
     *            The IP-address of the reader.
     * @param rfidNumberFormat
     *            The format of the RFID number.
     * @param timeout
     *            The timeout.
     * @param timeUnit
     *            The timeout time unit.
     * @return The event, or {@code null} when timeout occurred.
     * @throws InterruptedException
     *             When interrupted.
     */
    public static RfidEvent waitForEvent(final String readerIpAddress,
            final RfidNumberFormat rfidNumberFormat, final long timeout,
            final TimeUnit timeUnit) throws InterruptedException {

        final RfidEvent event =
                instance().getReader(readerIpAddress).take(timeout, timeUnit);

        if (event != null
                && event.getEvent() == RfidEvent.EventEnum.CARD_SWIPE) {
            event.setCardNumber(rfidNumberFormat
                    .getNormalizedNumber(event.getCardNumber()));
        }
        return event;
    }

}
