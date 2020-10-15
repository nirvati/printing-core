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
package org.savapage.core.services.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.savapage.core.imaging.EcoPrintPdfTask;
import org.savapage.core.imaging.EcoPrintPdfTaskInfo;

/**
 * Inspired by <a href=
 * "https://docs.oracle.com/javase/7/docs/api/java/util/concurrent/ThreadPoolExecutor.html"
 * >this source</a>.
 *
 * @author Rijk Ravestein
 *
 */
public final class EcoPrintPdfTaskThreadPoolExecutor
        extends ThreadPoolExecutor {

    /**
     * .
     */
    private boolean isPaused;

    /**
     * .
     */
    private final ReentrantLock pauseLock = new ReentrantLock();

    /**
     *
     */
    private final Condition unpaused = pauseLock.newCondition();

    /**
     * List with currently running objects.
     */
    private final List<Runnable> running =
            Collections.synchronizedList(new ArrayList<Runnable>());

    /**
     * Creates a new {@code ThreadPoolExecutor} with the given initial
     * parameters.
     *
     * @param corePoolSize
     *            the number of threads to keep in the pool, even if they are
     *            idle, unless {@code allowCoreThreadTimeOut} is set
     * @param maximumPoolSize
     *            the maximum number of threads to allow in the pool
     * @param keepAliveTime
     *            when the number of threads is greater than the core, this is
     *            the maximum time that excess idle threads will wait for new
     *            tasks before terminating.
     * @param unit
     *            the time unit for the {@code keepAliveTime} argument
     * @param workQueue
     *            the queue to use for holding tasks before they are executed.
     *            This queue will hold only the {@code Runnable} tasks submitted
     *            by the {@code execute} method.
     * @param threadFactory
     *            the factory to use when the executor creates a new thread
     * @param handler
     *            the handler to use when execution is blocked because the
     *            thread bounds and queue capacities are reached
     * @throws IllegalArgumentException
     *             if one of the following holds:<br>
     *             {@code corePoolSize < 0}<br>
     *             {@code keepAliveTime < 0}<br>
     *             {@code maximumPoolSize <= 0}<br>
     *             {@code maximumPoolSize < corePoolSize}
     * @throws NullPointerException
     *             if {@code workQueue} or {@code threadFactory} or
     *             {@code handler} is null
     */
    public EcoPrintPdfTaskThreadPoolExecutor(int corePoolSize,
            int maximumPoolSize, long keepAliveTime, TimeUnit unit,
            BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory,
            RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
                threadFactory, handler);
    }

    @Override
    protected void beforeExecute(Thread t, Runnable r) {

        super.beforeExecute(t, r);

        running.add(r);

        pauseLock.lock();
        try {
            while (isPaused) {
                unpaused.await();
            }
        } catch (InterruptedException ie) {
            t.interrupt();
        } finally {
            pauseLock.unlock();
        }
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);
        running.remove(r);
    }

    /**
     * Pauses execution.
     */
    public void pause() {
        pauseLock.lock();
        try {
            isPaused = true;
        } finally {
            pauseLock.unlock();
        }
    }

    /**
     * Resumes execution after a {@link #pause()}.
     */
    public void resume() {
        pauseLock.lock();
        try {
            isPaused = false;
            unpaused.signalAll();
        } finally {
            pauseLock.unlock();
        }
    }

    /**
     * Stops a running task.
     *
     * @param id
     *            The id of the task.
     * @return {@code true} if the task was found and stopped.
     */
    public boolean stopTask(final EcoPrintPdfTaskInfo taskInfo) {

        boolean isStopped = false;

        final EcoPrintPdfTask task = new EcoPrintPdfTask(taskInfo, this);

        synchronized (this.running) {

            final int idx = this.running.indexOf(task);
            if (idx >= 0) {
                final EcoPrintPdfTask found =
                        (EcoPrintPdfTask) this.running.get(idx);
                found.stop();
                isStopped = true;
            }
        }

        return isStopped;
    }

    /**
     * Checks if a task is running.
     *
     * @param id
     *            The id of the task.
     * @return {@code true} if the task is running.
     */
    public boolean isTaskRunning(final EcoPrintPdfTaskInfo taskInfo) {

        final EcoPrintPdfTask task = new EcoPrintPdfTask(taskInfo, this);

        boolean isRunning = false;

        synchronized (this.running) {
            isRunning = this.running.indexOf(task) >= 0;
        }

        return isRunning;
    }

}
