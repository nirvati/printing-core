/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2015 Datraverse B.V.
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
package org.savapage.core.services.impl;

import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.savapage.core.SpInfo;
import org.savapage.core.imaging.EcoPrintPdfTask;
import org.savapage.core.imaging.EcoPrintPdfTaskInfo;
import org.savapage.core.services.EcoPrintPdfTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link EcoPrintPdfTaskService} with
 * {@link EcoPrintPdfTaskThreadPoolExecutor} and An unbounded
 * {@link PriorityBlockingQueue}.
 *
 * @author Rijk Ravestein
 *
 */
public final class EcoPrintPdfTaskServiceImpl implements EcoPrintPdfTaskService {

    /**
     * The logger.
     */
    private static final Logger LOGGER = LoggerFactory
            .getLogger(EcoPrintPdfTaskServiceImpl.class);

    /**
     * When the number of threads is greater than the
     * {@link EcoPrintPdfTaskServiceImpl#THREADPOOL_CORE_SIZE}, this is the
     * maximum time that excess idle threads will wait for new tasks before
     * terminating.
     */
    private static final int THREADPOOL_KEEP_ALIVE_SECONDS = 10;

    /**
     * .
     */
    private final EcoPrintPdfTaskThreadPoolExecutor executorPool;

    /**
     * .
     */
    private final RejectedExecutionHandler rejectionHandler =
            new RejectedExecutionHandler() {

                @Override
                public void rejectedExecution(final Runnable r,
                        final ThreadPoolExecutor executor) {

                    final EcoPrintPdfTaskInfo info =
                            ((EcoPrintPdfTask) r).getTaskInfo();

                    LOGGER.error(String.format("[%s] %s is REJECTED", info
                            .getUuid().toString(), info.getPdfIn().getName()));
                }
            };

    /**
     * An unbounded {@link PriorityBlockingQueue} that uses the same ordering
     * rules as class {@link PriorityQueue}.
     */
    private final BlockingQueue<Runnable> workQueue =
            new PriorityBlockingQueue<>();

    /**
     *
     */
    public EcoPrintPdfTaskServiceImpl() {

        /*
         * The optimum size of a thread pool depends on the number of processors
         * available and the nature of the tasks on the work queue. On an
         * N-processor system for a work queue that will hold entirely
         * compute-bound tasks, you will generally achieve maximum CPU
         * utilization with a thread pool of N or N+1 threads.
         *
         * http://www.ibm.com/developerworks/library/j-jtp0730/index.html
         */

        /*
         * The maximum number of threads to allow in the pool.
         */
        final int maximumPoolSize = Runtime.getRuntime().availableProcessors();

        /*
         * The number of threads to keep in the pool, even if they are idle,
         * unless allowCoreThreadTimeOut is set.
         */
        final int corePoolSize = maximumPoolSize;

        this.executorPool =
                new EcoPrintPdfTaskThreadPoolExecutor(corePoolSize,
                        maximumPoolSize, THREADPOOL_KEEP_ALIVE_SECONDS,
                        TimeUnit.SECONDS, this.workQueue,
                        Executors.defaultThreadFactory(), this.rejectionHandler);
    }

    @Override
    public void submitTask(final EcoPrintPdfTaskInfo info) {
        this.executorPool.execute(new EcoPrintPdfTask(info, executorPool));
    }

    @Override
    public void pause() {
        this.executorPool.pause();
    }

    @Override
    public void resume() {
        this.executorPool.resume();
    }

    @Override
    public boolean cancelTask(final UUID uuid) {

        boolean isCancelled = false;

        final Iterator<Runnable> iter = this.executorPool.getQueue().iterator();

        while (iter.hasNext()) {

            final EcoPrintPdfTask task = (EcoPrintPdfTask) iter.next();

            if (task.getTaskInfo().getUuid().equals(uuid)) {
                task.stop();
                isCancelled = true;
                break;
            }

        }

        if (!isCancelled) {
            isCancelled =
                    this.executorPool.stopTask(new EcoPrintPdfTaskInfo(uuid));
        }

        return isCancelled;
    }

    @Override
    public boolean hasTask(final UUID uuid) {

        boolean isPresent = false;

        final Iterator<Runnable> iter = this.executorPool.getQueue().iterator();

        while (iter.hasNext()) {

            final EcoPrintPdfTask task = (EcoPrintPdfTask) iter.next();

            if (task.getTaskInfo().getUuid().equals(uuid)) {
                isPresent = true;
                break;
            }
        }

        if (!isPresent) {
            isPresent =
                    this.executorPool.isTaskRunning(new EcoPrintPdfTaskInfo(
                            uuid));
        }

        return isPresent;
    }

    @Override
    public void start() {
        // noop
    }

    @Override
    public void shutdown() {

        if (this.executorPool.isShutdown()) {
            return;
        }

        this.executorPool.shutdown();
        SpInfo.instance().log("Shutting down Eco Print Service...");

        try {
            while (!this.executorPool.awaitTermination(1, TimeUnit.SECONDS))
                ;
            SpInfo.instance().log("... Eco Print Service shutdown completed.");
        } catch (InterruptedException e) {
            SpInfo.instance().log("... Eco Print Service interrupted.");
        }

    }

}
