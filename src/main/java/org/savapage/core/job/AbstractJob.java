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
package org.savapage.core.job;

import java.util.Locale;

import org.quartz.InterruptableJob;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.UnableToInterruptJobException;
import org.savapage.core.SpInfo;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.ServiceEntryPoint;
import org.savapage.core.util.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Quotes from
 * <a href="http://www.quartz-scheduler.org/documentation/best-practices">Quartz
 * Best Practices<a>
 * <p>
 * <i> A Job's execute method should contain a try-catch block that handles all
 * possible exceptions.</i>
 * </p>
 * <p>
 * <i>If a job throws an exception, Quartz will typically immediately re-execute
 * it (and it will likely throw the same exception again). It's better if the
 * job catches all exception it may encounter, handle them, and reschedule
 * itself, or other jobs. to work around the issue.</i>
 * </p>
 *
 * @author Rijk Ravestein
 *
 */
public abstract class AbstractJob
        implements InterruptableJob, ServiceEntryPoint {

    /** */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(AbstractJob.class);

    /** */
    private boolean interrupted = false;

    /**
     * @return {@code true} when this job was interrupted.
     */
    protected final boolean isInterrupted() {
        return this.interrupted;
    }

    @Override
    public final void interrupt() throws UnableToInterruptJobException {

        SpInfo.instance().log(String.format("| %s interrupted",
                this.getClass().getSimpleName()));

        this.interrupted = true;

        try {
            onInterrupt();
        } finally {
            ServiceContext.close();
        }
    }

    @Override
    public final void execute(final JobExecutionContext ctx)
            throws JobExecutionException {

        final String logPfx =
                String.format("| %s", this.getClass().getSimpleName());

        LOGGER.info("{} starting...", logPfx);

        try {
            ServiceContext.open();

            LOGGER.info("{} ...initializing", logPfx);
            onInit(ctx);
            LOGGER.info("{} ...initialized", logPfx);

            LOGGER.info("{} ...executing", logPfx);
            onExecute(ctx);
            LOGGER.info("{} ...executed", logPfx);

        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        } finally {
            try {
                LOGGER.info("{} ...finalizing", logPfx);
                onExit(ctx);
                LOGGER.info("{} ...finalized", logPfx);
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            } finally {
                ServiceContext.close();
            }
        }
        LOGGER.info("{} finished", logPfx);
    }

    /**
     *
     * @throws UnableToInterruptJobException
     *             When unable to interrupt.
     */
    protected abstract void onInterrupt() throws UnableToInterruptJobException;

    /**
     *
     * @param ctx
     *            The {@link JobExecutionContext}.
     */
    protected abstract void onInit(JobExecutionContext ctx);

    /**
     *
     * @param ctx
     *            The {@link JobExecutionContext}.
     * @throws JobExecutionException
     *             When an error occurred while executing.
     */
    protected abstract void onExecute(JobExecutionContext ctx)
            throws JobExecutionException;

    /**
     *
     * @param ctx
     *            The {@link JobExecutionContext}.
     */
    protected abstract void onExit(JobExecutionContext ctx);

    /**
     * Localizes a system message.
     *
     * @param key
     *            The message key.
     * @param args
     *            The message arguments.
     * @return The localized message.
     */
    protected final String localizeSysMsg(final String key,
            final String... args) {
        return Messages.getMessage(this.getClass(), key, args);
    }

    /**
     * Localizes a message to English for logging.
     *
     * @param key
     *            The message key.
     * @param args
     *            The message arguments.
     * @return The English message.
     */
    protected final String localizeLogMsg(final String key,
            final String... args) {
        return Messages.getMessage(this.getClass(), Locale.ENGLISH, key, args);
    }

}
