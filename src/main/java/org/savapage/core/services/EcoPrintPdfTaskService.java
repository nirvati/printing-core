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
package org.savapage.core.services;

import org.savapage.core.imaging.EcoPrintPdfTaskInfo;

/**
 * A service for asynchronous (background) conversion of PDF files to their Eco
 * Print variant.
 *
 * @author Rijk Ravestein
 *
 */
public interface EcoPrintPdfTaskService {

    /**
     * Submits a task.
     *
     * @param info
     *            The {@link EcoPrintPdfTaskInfo}.
     */
    void submitTask(final EcoPrintPdfTaskInfo info);

    /**
     * Pauses the service.
     */
    void pause();

    /**
     * Resumes the service.
     */
    void resume();

    /**
     * Stops a task, either by removing it from the queue or by aborting it when
     * running.
     *
     * @param id
     *            The id of the task, see {@link EcoPrintPdfTaskInfo#getId()} .
     * @return {@code true} when task was found and stopped, {@code false} when
     *         task was not found.
     */
    boolean stopTask(final String id);

    /**
     * Shuts the service down and blocks till is has terminated. Current running
     * tasks are aborted.
     */
    void shutdown();
}
