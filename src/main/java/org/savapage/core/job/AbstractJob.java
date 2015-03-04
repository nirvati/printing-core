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
package org.savapage.core.job;

import org.quartz.InterruptableJob;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.UnableToInterruptJobException;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.ServiceEntryPoint;
import org.savapage.core.util.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Quotes from <a
 * href="http://www.quartz-scheduler.org/documentation/best-practices">Quartz
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
 * @author Datraverse B.V.
 *
 */
public abstract class AbstractJob implements InterruptableJob,
        ServiceEntryPoint {

    /**
    *
    */
    private static final Logger LOGGER = LoggerFactory
            .getLogger(AbstractJob.class);

    /**
     *
     */
    private boolean interrupted = false;

    protected boolean isInterrupted() {
        return this.interrupted;
    }

    @Override
    public void interrupt() throws UnableToInterruptJobException {

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

        try {
            ServiceContext.open();
            onInit(ctx);
            onExecute(ctx);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        } finally {
            try {
                onExit(ctx);
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            } finally {
                ServiceContext.close();
            }
        }
    }

    abstract protected void onInterrupt() throws UnableToInterruptJobException;

    abstract protected void onInit(final JobExecutionContext ctx);

    abstract protected void onExecute(final JobExecutionContext ctx)
            throws JobExecutionException;

    abstract protected void onExit(final JobExecutionContext ctx);

    protected String localizeMsg(String key, final String... args) {
        return Messages.getMessage(this.getClass(), key, args);
    }

}
