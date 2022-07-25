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
package org.savapage.core.util;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Detector for threads that are deadlocked waiting for object monitors or
 * ownable synchronizers.
 *
 * @author Rijk Ravestein
 *
 */
public final class DeadlockedThreadsDetector {

    /** */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(DeadlockedThreadsDetector.class);

    /** */
    private static volatile int deadlockCount = 0;

    /**
     * Class shared between the two test threads.
     */
    private static class SharedTest {

        /** */
        private static final long MSEC_SLEEP = 2000;

        /**
         * @param s
         *            Another {@link SharedTest} object.
         */
        synchronized void methodA(final SharedTest s) {

            final Thread t = Thread.currentThread();

            LOGGER.debug("{} is executing synchronized methodA...",
                    t.getName());

            try {
                Thread.sleep(MSEC_SLEEP);
            } catch (InterruptedException e) {
                LOGGER.error(e.getMessage());
            }

            LOGGER.debug("{} is calling synchronized methodB...", t.getName());

            s.methodB(this);

            LOGGER.debug("{} finished executing synchronized methodA...",
                    t.getName());
        }

        /**
         * @param s
         *            Another {@link SharedTest} object.
         */
        synchronized void methodB(final SharedTest s) {

            final Thread t = Thread.currentThread();

            LOGGER.debug("{} is executing synchronized methodB...",
                    t.getName());

            try {
                Thread.sleep(MSEC_SLEEP);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            LOGGER.debug("{} is calling synchronized methodA...", t.getName());

            s.methodA(this);

            LOGGER.debug("{} finished executing synchronized methodB...",
                    t.getName());
        }
    }

    /** */
    private static class SingletonHolder {
        /** */
        public static final DeadlockedThreadsDetector INSTANCE =
                new DeadlockedThreadsDetector();
    }

    /** */
    private final SharedTest s1;
    /** */
    private final SharedTest s2;

    /**
     * First Test Thread.
     */
    private final Thread testThreadA;

    /**
     * Second Test Thread.
     */
    private final Thread testThreadB;

    /** */
    public DeadlockedThreadsDetector() {

        this.s1 = new SharedTest();
        this.s2 = new SharedTest();

        this.testThreadA = new Thread("DeadlockTest-A") {
            @Override
            public void run() {
                s1.methodA(s2);
            }
        };

        this.testThreadB = new Thread("DeadlockTest-B") {
            @Override
            public void run() {
                s2.methodB(s1);
            }
        };
    }

    /**
     * Stops deadlocked threads.
     */
    private void startDeadlockThreads() {
        this.testThreadA.start();
        this.testThreadB.start();
    }

    /**
     * Stops deadlocked threads. Just kidding :-)
     * <p>
     * <i>There is no way to stop deadlocked threads.</i>
     * </p>
     */
    @SuppressWarnings("unused")
    private void stopDeadlockThreads() {
        // noop
    }

    /**
     * Creates two deadlocked threads for testing.
     *
     * @return {@code false} when already started.
     */
    public static boolean createDeadlockTest() {
        try {
            SingletonHolder.INSTANCE.startDeadlockThreads();
            return true;
        } catch (IllegalThreadStateException e) {
            // noop
        }
        return false;
    }

    /**
     * Wraps {@link ThreadMXBean#findDeadlockedThreads()}.
     *
     * @return {@code null} when no deadlocked threads are found.
     */
    public static ThreadInfo[] findDeadlockedThreads() {
        final ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        final long[] ids = bean.findDeadlockedThreads();
        if (ids == null) {
            deadlockCount = 0;
            return null;
        }
        deadlockCount = ids.length;
        return bean.getThreadInfo(ids, true, true);
    }

    /**
     * @return The number deadlocked threads.
     */
    public static int getDeadlockedThreadsCount() {
        return deadlockCount;
    }

    /**
     * Formats thread info to string.
     *
     * @param ti
     *            ThreadInfo
     * @return Formatted info.
     */
    public static String toString(final ThreadInfo ti) {

        final StringBuilder msg = new StringBuilder();

        msg.append("Thread [").append(ti.getThreadName()).append("] Id [")
                .append(ti.getThreadId()).append("] ")
                .append(ti.getThreadState()).append(" on [")
                .append(ti.getLockName()).append("] owned by [")
                .append(ti.getLockOwnerName()).append("] Id [")
                .append(ti.getLockOwnerId()).append("]" + " Blocked [")
                .append(DateUtil.formatDuration(ti.getBlockedTime()))
                .append("] Waited [")
                .append(DateUtil.formatDuration(ti.getWaitedTime()))
                .append("]");
        return msg.toString();
    }

}
