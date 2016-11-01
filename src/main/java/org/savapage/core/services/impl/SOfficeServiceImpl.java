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
package org.savapage.core.services.impl;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.savapage.core.SpInfo;
import org.savapage.core.doc.soffice.SOfficeConfig;
import org.savapage.core.doc.soffice.SOfficeException;
import org.savapage.core.doc.soffice.SOfficeTask;
import org.savapage.core.doc.soffice.SOfficeUnoUrl;
import org.savapage.core.doc.soffice.SOfficeWorker;
import org.savapage.core.doc.soffice.SOfficeWorkerSettings;
import org.savapage.core.services.SOfficeService;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class SOfficeServiceImpl implements SOfficeService {

    /**
     * The {@link BlockingQueue} with workers.
     */
    private BlockingQueue<SOfficeWorker> workerPool;

    /**
     *
     */
    private SOfficeWorker[] pooledWorkers;

    /**
     * Wait time (milliseconds) for an {@link SOfficeWorker} to become available
     * for task execution.
     */
    private long taskQueueTimeout;

    /**
     * {@code true} when service is running..
     */
    private volatile boolean running = false;

    /**
     * {@code true} when service is enabled.
     */
    private volatile boolean enabled = false;

    /**
     * Constructor.
     *
     * @param config
     *            The configuration.
     */
    public SOfficeServiceImpl(final SOfficeConfig config) {
        this.init(config);
    }

    /**
     *
     * @param config
     *            The configuration.
     */
    private void init(final SOfficeConfig config) {

        this.enabled = config.isEnabled();

        if (!this.enabled) {
            return;
        }

        config.validate();

        final SOfficeUnoUrl[] unoUrls = config.createUnoUrls();

        this.taskQueueTimeout = config.getTaskQueueTimeout();

        this.workerPool = new ArrayBlockingQueue<SOfficeWorker>(unoUrls.length);

        this.pooledWorkers = new SOfficeWorker[unoUrls.length];

        for (int i = 0; i < unoUrls.length; i++) {

            final SOfficeWorkerSettings settings =
                    new SOfficeWorkerSettings(unoUrls[i], config);

            this.pooledWorkers[i] = new SOfficeWorker(settings);
        }

    }

    @Override
    public synchronized void restart(final SOfficeConfig config)
            throws SOfficeException {
        shutdown();
        init(config);
        start();
    }

    @Override
    public synchronized void start() throws SOfficeException {

        if (this.running || !this.enabled) {
            return;
        }

        for (final SOfficeWorker worker : this.pooledWorkers) {
            worker.start();
            releaseWorker(worker);
        }

        this.running = true;

        final int nWorkers = this.pooledWorkers.length;

        final String plural;
        if (nWorkers == 1) {
            plural = "";
        } else {
            plural = "s";
        }

        SpInfo.instance().log(String.format(
                "SOffice Service started (%d worker%s).", nWorkers, plural));
    }

    @Override
    public synchronized void shutdown() {

        if (!this.running) {
            return;
        }

        SpInfo.instance().log("Shutting down SOffice Service...");

        this.running = false;

        this.workerPool.clear();

        for (final SOfficeWorker worker : this.pooledWorkers) {
            worker.shutdown();
        }

        SpInfo.instance().log("... SOffice Service shutdown completed.");
    }

    @Override
    public void execute(final SOfficeTask task)
            throws IllegalStateException, SOfficeException {

        if (!this.enabled) {
            throw new IllegalStateException(
                    "Cannot execute: service is disabled.");
        }

        if (!this.running) {
            throw new IllegalStateException(
                    "Cannot execute: service is stopped.");
        }

        SOfficeWorker worker = null;

        try {

            worker = acquireWorker();

            if (worker == null) {
                throw new SOfficeException("No worker available.");
            }

            worker.execute(task);

        } catch (InterruptedException e) {
            throw new SOfficeException(e.getMessage());
        } finally {
            if (worker != null) {
                releaseWorker(worker);
            }
        }
    }

    /**
     * Retrieves a worker from the pool, waiting up to the specified wait time
     * if necessary for one to become available.
     *
     * @return The {@link SOfficeWorker}, or {@code null} if the specified
     *         waiting time elapses before an element is available.
     *
     * @throws InterruptedException
     *             if interrupted while waiting.
     */
    private SOfficeWorker acquireWorker() throws InterruptedException {
        /*
         * Retrieves and removes the head of the pool queue, waiting up to the
         * specified wait time if necessary for an element to become available.
         */
        return this.workerPool.poll(taskQueueTimeout, TimeUnit.MILLISECONDS);
    }

    /**
     * Releases the {@link SOfficeWorker} by putting it in the pool again.
     *
     * @param worker
     *            The {@link SOfficeWorker}.
     */
    private void releaseWorker(final SOfficeWorker worker) {
        try {
            this.workerPool.put(worker);
        } catch (InterruptedException interruptedException) {
            throw new SOfficeException("interrupted", interruptedException);
        }
    }

    @Override
    public boolean isRunning() {
        return this.running;
    }

}
