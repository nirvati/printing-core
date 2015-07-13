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
public class TimedReadWriteLock {

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
     * The logger.
     */
    private static final Logger LOGGER = LoggerFactory
            .getLogger(TimedReadWriteLock.class);

    /**
     *
     */
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock(
            LOCK_POLICY_FAIR);

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

        private final StackTraceElement[] stackElements;

        /**
         *
         * @param locker
         * @param readLock
         */
        WaitTimerTask(final Thread locker, final boolean readLock) {
            this.locker = locker;
            this.readLock = readLock;

            stackElements = locker.getStackTrace();
        }

        @Override
        public void run() {

            final String lockType = readLock ? "read" : "write";

            final StringBuilder msg =
                    new StringBuilder(locker + " is holding the [" + lockType
                            + "] lock '" + name + "' for more than " + maxWait
                            + " ms.\nLocker stack trace:\n");

            for (StackTraceElement element : stackElements) {
                msg.append("\t").append(element).append("\n");
            }

            LOGGER.error(msg.toString());

        }

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
     * Locks or unlocks a read lock.
     *
     * @param lock
     *            true - lock for read, false - unlock for read.
     */
    public void setReadLock(final boolean lock) {

        if (lock) {

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Read lock [" + Thread.currentThread().getName()
                        + "]: " + Thread.currentThread().getStackTrace()[3]);
            }

            rwLock.readLock().lock();

            final WaitTimerTask job =
                    new WaitTimerTask(Thread.currentThread(), true);

            lockTaskStack.get().push(job);

            waitTimer.schedule(job, maxWait);

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
     *            true - lock for write, false - unlock for write.
     */
    public void setWriteLock(final boolean lock) {

        if (lock) {
            /*
             * Check if the same thread is already holding a read lock If so,
             * the write lock will block forever.
             *
             * @see java.util.concurrent.ReentrantReadWriteLock javadocs for
             * details
             */
            Stack<WaitTimerTask> taskStack = lockTaskStack.get();

            if (!taskStack.isEmpty()) {

                final WaitTimerTask job = taskStack.peek();

                if (job != null && job.isReadLock()) {

                    LOGGER.error("The same thread [" + Thread.currentThread()
                            + "] is already holding a read lock '" + name
                            + "'. Cannot lock for write!");

                    if (LOGGER.isDebugEnabled()) {

                        final StringBuilder msg =
                                new StringBuilder(Thread.currentThread()
                                        + " stack trace:\n");

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

            final WaitTimerTask job =
                    new WaitTimerTask(Thread.currentThread(), false);

            lockTaskStack.get().push(job);

            waitTimer.schedule(job, maxWait);

        } else {

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Write unlock ["
                        + Thread.currentThread().getName() + "]: "
                        + Thread.currentThread().getStackTrace()[3]);
            }

            rwLock.writeLock().unlock();

            final WaitTimerTask job = lockTaskStack.get().pop();
            job.cancel();
        }
    }

}
