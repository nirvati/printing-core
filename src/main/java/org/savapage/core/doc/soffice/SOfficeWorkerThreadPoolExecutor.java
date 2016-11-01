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
package org.savapage.core.doc.soffice;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class SOfficeWorkerThreadPoolExecutor extends ThreadPoolExecutor {

    /**
     *
     */
    private boolean available = false;

    /**
     *
     */
    private final ReentrantLock suspendLock = new ReentrantLock();

    /**
     *
     */
    private final Condition availableCondition = suspendLock.newCondition();

    /**
     * Constructor.
     *
     * @param threadFactory
     *            The {@link ThreadFactory}.
     */
    public SOfficeWorkerThreadPoolExecutor(final ThreadFactory threadFactory) {

        super(1, 1, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(), threadFactory);
    }

    @Override
    protected void beforeExecute(final Thread thread, final Runnable task) {

        super.beforeExecute(thread, task);

        this.suspendLock.lock();

        try {
            while (!this.available) {
                this.availableCondition.await();
            }
        } catch (InterruptedException interruptedException) {
            thread.interrupt();
        } finally {
            this.suspendLock.unlock();
        }
    }

    /**
     *
     * @param isAvailable
     *            {@code true} when available.
     */
    public void setAvailable(final boolean isAvailable) {

        this.suspendLock.lock();
        this.available = isAvailable;

        try {
            if (this.available) {
                this.availableCondition.signalAll();
            }
        } finally {
            this.suspendLock.unlock();
        }
    }

}
