/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2020 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: 2011-2020 Datraverse B.V. <info@datraverse.com>
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
package org.savapage.core.concurrent;

import java.util.Stack;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A read write lock that reports an error if a thread is locking more for more
 * than n milliseconds. It will also prevent a read lock from trying to obtain a
 * write lock. <i>The lock will not release the lock, just report the problem
 * using the logging system.</i>
 * <p>
 * See <a href=
 * "https://today.java.net/pub/a/today/2007/06/28/extending-reentrantreadwritelock.html"
 * >this link</a>.
 * </p>
 *
 * @author Ran Kornfeld (original author)
 * @author Rijk Ravestein (minor changes)
 *
 */
public final class TimedReadWriteLock {

    /**
     * Fair policy implies:
     * <ul>
     * <li>Read Lock acquire: blocks if either the write lock is held, or there
     * is a waiting writer thread.</li>
     * <li>Write Lock acquire: blocks unless both the read lock and write lock
     * are free.</li>
     * </ul>
     */
    private static final boolean LOCK_POLICY_FAIR = true;

    /**
     * .
     */
    private static final String STACK_TRACE_CLASSNAME_FILTER = "org.savapage.";

    /**
     * The logger.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(TimedReadWriteLock.class);

    /**
     *
     */
    private final ReadWriteLock rwLock =
            new ReentrantReadWriteLock(LOCK_POLICY_FAIR);

    /**
     * How long to wait to unlock.
     */
    private final long maxWait;

    /**
     * A name for the lock.
     */
    private final String name;

    /**
     * A static {@link Timer} so all the locks will use the same thread for the
     * wait timer.
     */
    private static Timer waitTimer = new Timer(true);

    /**
     * Timer task for reporting lock timeout error.
     */
    private class WaitTimerTask extends TimerTask {

        private final Thread locker;
        private final boolean readLock;
        private final String contextId;
        private final StackTraceElement[] stackElements;

        /**
         *
         * @param locker
         * @param readLock
         */
        WaitTimerTask(final Thread locker, final boolean readLock,
                final String contextId) {
            this.locker = locker;
            this.readLock = readLock;
            this.stackElements = locker.getStackTrace();
            this.contextId = contextId;
        }

        @Override
        public void run() {

            if (!LOGGER.isWarnEnabled()) {
                return;
            }

            final String lockType;

            if (this.readLock) {
                lockType = "read";
            } else {
                lockType = "write";
            }

            final String thisPackageName =
                    this.getClass().getPackage().getName();

            final StringBuilder msg = new StringBuilder();

            msg.append(this.locker).append(" holds [").append(lockType)
                    .append("] lock [").append(name).append("]");

            if (this.contextId != null) {
                msg.append(" [").append(this.contextId).append("]");
            }

            msg.append(" for more than ").append(maxWait)
                    .append(" ms. Stack trace snippet:\n");

            int nElement = 0;

            for (StackTraceElement element : this.stackElements) {
                if (element.getClassName().startsWith(thisPackageName)) {
                    continue;
                }
                if (element.getClassName()
                        .startsWith(STACK_TRACE_CLASSNAME_FILTER)) {
                    msg.append("\t").append(element).append("\n");
                    nElement++;
                } else if (nElement > 0) {
                    break;
                }
            }
            LOGGER.warn(msg.toString());
        }

        /**
         *
         * @return {@code true} when a read lock.
         */
        public boolean isReadLock() {
            return readLock;
        }

    }

    /**
     * .
     */
    private class LockTaskStack extends ThreadLocal<Stack<WaitTimerTask>> {

        @Override
        protected Stack<WaitTimerTask> initialValue() {
            return new Stack<WaitTimerTask>();
        }

    }

    /**
     *
     */
    private final LockTaskStack lockTaskStack = new LockTaskStack();

    /**
     * Constructor.
     *
     * @param name
     *            The unique name for the lock (used for reporting).
     * @param maxWait
     *            Max wait milliseconds after which an error is reported if a
     *            thread is still locked.
     */
    public TimedReadWriteLock(final String name, final long maxWait) {
        this.maxWait = maxWait;
        this.name = name;
    }

    /**
     * Acquires the read lock only if it is free at the time of invocation.
     *
     * Acquires the lock if it is available and returns immediately with the
     * value {@code true}. If the lock is not available then this method will
     * return immediately with the value {@code false}.
     *
     * @param contextId
     *            ID used for logging.
     *
     * @return {@code true} if the lock was acquired and {@code false}
     *         otherwise.
     */
    public boolean tryReadLock(final String contextId) {

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Try Read lock [" + Thread.currentThread().getName()
                    + "]: " + Thread.currentThread().getStackTrace()[3]);
        }
        if (!rwLock.readLock().tryLock()) {
            return false;
        }
        this.pushTimerTask(true, contextId);
        return true;
    }

    /**
     *
     * @param readLock
     * @param contextId
     *            ID used for logging.
     */
    private void pushTimerTask(final boolean readLock, final String contextId) {
        final WaitTimerTask job =
                new WaitTimerTask(Thread.currentThread(), readLock, contextId);
        lockTaskStack.get().push(job);
        waitTimer.schedule(job, maxWait);
    }

    /**
     * Locks or unlocks a read lock.
     *
     * @param lock
     *            true - lock for read, false - unlock for read.
     * @param contextId
     *            ID used for logging.
     */
    public void setReadLock(final boolean lock, final String contextId) {

        if (lock) {

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Read lock [" + Thread.currentThread().getName()
                        + "]: " + Thread.currentThread().getStackTrace()[3]);
            }

            rwLock.readLock().lock();
            this.pushTimerTask(true, contextId);

        } else {

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Read unlock [" + Thread.currentThread().getName()
                        + "]: " + Thread.currentThread().getStackTrace()[3]);
            }

            rwLock.readLock().unlock();

            final WaitTimerTask job = lockTaskStack.get().pop();

            job.cancel();
        }
    }

    /**
     * Locks or unlocks a write lock.
     *
     * @param lock
     *            {@code true} - lock for write, {@code false} - unlock for
     *            write.
     * @param contextId
     *            ID used for logging.
     */
    public void setWriteLock(final boolean lock, final String contextId) {

        if (lock) {
            /*
             * Check if the same thread is already holding a read lock If so,
             * the write lock will block forever.
             *
             * @see java.util.concurrent.ReentrantReadWriteLock javadocs for
             * details
             */
            final Stack<WaitTimerTask> taskStack = lockTaskStack.get();

            if (!taskStack.isEmpty()) {

                final WaitTimerTask job = taskStack.peek();

                if (job != null && job.isReadLock()) {

                    LOGGER.error("The same thread [" + Thread.currentThread()
                            + "] is already holding a read lock '" + name
                            + "'. Cannot lock for write!");

                    if (LOGGER.isDebugEnabled()) {

                        final StringBuilder msg = new StringBuilder();
                        msg.append(Thread.currentThread())
                                .append(" stack trace:\n");
                        for (StackTraceElement element : Thread.currentThread()
                                .getStackTrace()) {
                            msg.append("\t").append(element).append("\n");
                        }

                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug(msg.toString());
                        }

                    }
                    return;
                }
            }

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Write lock [" + Thread.currentThread().getName()
                        + "]: " + Thread.currentThread().getStackTrace()[3]);
            }

            rwLock.writeLock().lock();
            this.pushTimerTask(false, contextId);

        } else {

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Write unlock [" + Thread.currentThread().getName()
                        + "]: " + Thread.currentThread().getStackTrace()[3]);
            }

            rwLock.writeLock().unlock();

            final WaitTimerTask job = lockTaskStack.get().pop();
            job.cancel();
        }
    }

}
