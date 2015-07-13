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
package org.savapage.core.rfid;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents an RFID Card Reader.
 * <p>
 * Note: Adapted from <a href=
 * "http://docs.oracle.com/javase/7/docs/api/java/util/concurrent/locks/Condition.html"
 * >this</a> sample from the Java 7 SE documentation.
 * </p>
 *
 * @author Datraverse B.V.
 *
 */
public class RfIdReader {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(RfIdReader.class);

    /**
     *
     */
    final Lock lock = new ReentrantLock();

    /*
     * We keep waiting 'put' and 'take' threads in separate wait-sets so that we
     * can use the optimization of only notifying a single thread at a time when
     * an incoming RfIdEvent can be stored and becomes available. This can be
     * achieved with these two Condition instances.
     */
    final Condition notFull = lock.newCondition();
    final Condition notEmpty = lock.newCondition();

    /**
     * The most recent event is all we need.
     */
    RfidEvent lastEvent = null;

    /**
     *
     * @param event
     * @throws InterruptedException
     */
    public void put(RfidEvent event) throws InterruptedException {

        this.lock.lock();

        try {

            /*
             * We ALWAYS replace the current event with the new one.
             */
            if (this.lastEvent != null) {
                this.lastEvent = null;
            }

            /*
             * Wait for event to be consumed.
             */
            while (this.lastEvent != null) {
                /*
                 * Wait for the notFull.signal(): during the await() the
                 * acquired lock is (temporary) released.
                 */
                this.notFull.await();
            }

            /*
             * Produce the event.
             */
            this.lastEvent = event;

            /*
             * Tell the world about produced the event!
             */
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Producer: signalled "
                        + event.getEvent().toString() + " ["
                        + event.getCardNumber() + "]");
            }

            this.notEmpty.signal();

        } finally {
            this.lock.unlock();
        }
    }

    /**
     * Waits for an incoming RfidEvent for max timeout time units.
     *
     * @param timeout
     *            The timeout.
     * @param timeUnit
     *            The timeout time unit.
     * @return {@code null} when timeout occurred.
     * @throws InterruptedException
     */
    public RfidEvent take(long timeout, TimeUnit timeUnit)
            throws InterruptedException {

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Consumer: waiting for event ... (max " + timeout
                    + " " + timeUnit.toString() + ")");
        }

        this.lock.lock();

        RfidEvent event = null;

        try {

            /*
             * SKIP event that is too old.
             */
            if (this.lastEvent != null) {

                final long expiryMsec =
                        ConfigManager.instance().getConfigLong(
                                Key.AUTH_MODE_CARD_IP_EXPIRY_MSECS);

                final boolean eventExpired =
                        (System.currentTimeMillis() - this.lastEvent.getDate()
                                .getTime()) > expiryMsec;

                if (eventExpired) {

                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace("Consumer: skipped expired "
                                + lastEvent.getEvent().toString() + " ["
                                + lastEvent.getCardNumber() + "]");
                    }

                    this.lastEvent = null;
                    this.notFull.signal();
                }
            }

            /*
             * Get or wait for event...
             */
            if (this.lastEvent == null) {
                /*
                 * Wait for the notEmpty.signal() : during the await() the
                 * acquired lock is (temporary) released.
                 */
                if (this.notEmpty.await(timeout, timeUnit)) {
                    /*
                     * Consume the event.
                     */
                    event = this.lastEvent;
                }
            } else {
                event = this.lastEvent;
            }

            if (event != null) {

                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Consumer: consumed "
                            + event.getEvent().toString() + " ["
                            + event.getCardNumber() + "]");
                }
                /*
                 * Tell the world the next event can be produced!
                 */
                this.lastEvent = null;
                this.notFull.signal();
            } else {
                LOGGER.trace("Consumer: no event");
            }

            return event;

        } finally {
            this.lock.unlock();
        }
    }
}
