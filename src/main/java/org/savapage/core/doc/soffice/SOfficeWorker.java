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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class SOfficeWorker {

    /**
     *
     */
    private final SOfficeWorkerSettings settings;

    /**
     *
     */
    private final SOfficeProcessManager processManager;

    /**
     *
     */
    private final SOfficeWorkerThreadPoolExecutor taskExecutor;

    /**
     *
     */
    private volatile boolean stopping = false;

    /**
     *
     */
    private int taskCount;

    /**
     * The active task.
     */
    private Future<?> activeTask;

    /**
     * The logger.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(SOfficeWorker.class);

    /**
     *
     */
    private final SOfficeConnectionListener connectEventListener =
            new SOfficeConnectionListener() {
                @Override
                public void onConnected(final SOfficeConnectEvent event) {
                    taskCount = 0;
                    taskExecutor.setAvailable(true);
                }

                @Override
                public void onDisconnected(final SOfficeConnectEvent event) {
                    taskExecutor.setAvailable(false);
                    if (stopping) {
                        // expected
                        stopping = false;
                    } else {
                        LOGGER.warn("Connection unexpectedly lost: "
                                + "attempting restart...");

                        if (activeTask != null) {
                            activeTask.cancel(true);
                        }
                        processManager.restartDueToLostConnection();
                    }
                }
            };

    /**
     * Constructor.
     *
     * @param workerSettings
     *            The settings.
     */
    public SOfficeWorker(final SOfficeWorkerSettings workerSettings) {

        this.settings = workerSettings;

        this.processManager = new SOfficeProcessManager(workerSettings);

        this.processManager.getConnection()
                .addConnectionEventListener(connectEventListener);

        this.taskExecutor = new SOfficeWorkerThreadPoolExecutor(
                new SOfficeThreadFactory("SOfficeWorkerThread"));
    }

    /**
     * Executes a task.
     *
     * @param task
     *            The task.
     * @throws SOfficeTaskTimeoutException
     *             When task did not complete within time.
     */
    public void execute(final SOfficeTask task)
            throws SOfficeTaskTimeoutException {

        final Future<?> futureTask = this.taskExecutor.submit(new Runnable() {

            @Override
            public void run() {

                if (settings.getTasksCountForProcessRestart() > 0) {

                    if (taskCount == settings
                            .getTasksCountForProcessRestart()) {

                        if (LOGGER.isInfoEnabled()) {
                            LOGGER.info(String.format(
                                    "Restarting after %d tasks...",
                                    settings.getTasksCountForProcessRestart()));
                        }

                        taskExecutor.setAvailable(false);
                        stopping = true;
                        processManager.restartAndWait();

                        taskCount = 0;
                    }

                    taskCount++;

                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace("Task count: " + taskCount);
                    }

                }

                task.execute(processManager.getConnection());

                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace(String.format("PID [%s]",
                            processManager.getProcess().getPid()));
                }
            }
        });

        this.activeTask = futureTask;

        try {

            futureTask.get(settings.getTaskExecutionTimeout(),
                    TimeUnit.MILLISECONDS);

        } catch (TimeoutException e) {

            this.processManager.restartDueToTaskTimeout();

            throw new SOfficeTaskTimeoutException(
                    "Task did not complete within time.", e);

        } catch (ExecutionException e) {

            if (e.getCause() instanceof SOfficeException) {
                throw (SOfficeException) e.getCause();
            } else {
                throw new SOfficeException("Task failed", e.getCause());
            }

        } catch (Exception exception) {
            throw new SOfficeException("Task failed", exception);
        }
    }

    /**
     * Starts.
     */
    public void start() {
        this.processManager.startAndWait();
    }

    /**
     * Shuts down.
     */
    public void shutdown() {
        this.taskExecutor.setAvailable(false);
        this.stopping = true;
        this.taskExecutor.shutdownNow();
        this.processManager.stopAndWait();
    }

    /**
     *
     * @return {@code true} when running.
     */
    public boolean isRunning() {
        return this.processManager.isConnected();
    }

}
