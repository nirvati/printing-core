/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2016 Datraverse B.V.
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
package org.savapage.core.util;

/**
 *
 * @author Rijk Ravestein
 *
 */
public abstract class RetryExecutor {

    /**
     * Attempts a retry.
     *
     * @throws RetryException
     *             for an error condition that is temporary, i.e. that could be
     *             resolved by simply retrying the same operation after an
     *             interval.
     * @throws Exception
     *             for all other error conditions.
     */
    protected abstract void attempt() throws RetryException, Exception;

    /**
     * Executes the {@link #attempt()} immediately, every "interval" till it
     * succeeds or a timeout occurs.
     *
     * @param interval
     *            The attempt interval (milliseconds).
     * @param timeout
     *            The timeout (milliseconds).
     *
     * @throws RetryTimeoutException
     *             when attempts did not succeed within timeout.
     * @throws Exception
     *             for all other error conditions.
     */
    public final void execute(final long interval, final long timeout)
            throws RetryTimeoutException, Exception {
        this.execute(0L, interval, timeout);
    }

    /**
     * Executes the {@link #attempt()} after a delay, every "interval" till it
     * succeeds or a timeout occurs.
     *
     * @param delay
     *            The delay (milliseconds) before the first attempt is made.
     * @param interval
     *            The attempt interval (milliseconds).
     * @param timeout
     *            The timeout (milliseconds).
     *
     * @throws RetryTimeoutException
     *             when attempts did not succeed within timeout.
     * @throws Exception
     *             for all other error conditions.
     */
    public final void execute(final long delay, final long interval,
            final long timeout) throws RetryTimeoutException, Exception {

        final long start = System.currentTimeMillis();

        if (delay > 0L) {
            sleep(delay);
        }

        while (true) {
            try {
                attempt();
                return;
            } catch (RetryException e) {
                if (System.currentTimeMillis() - start < timeout) {
                    sleep(interval);
                } else {
                    throw new RetryTimeoutException(e.getCause());
                }
            }
        }
    }

    /**
     * Sleeps.
     *
     * @param millis
     *            number of milliseconds.
     */
    private void sleep(final long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException interruptedException) {
            // continue
        }
    }

}
