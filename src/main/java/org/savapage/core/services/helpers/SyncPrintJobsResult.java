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
package org.savapage.core.services.helpers;

import org.savapage.core.jpa.PrintOut;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class SyncPrintJobsResult {

    /**
     * The number of active {@link PrintOut} jobs.
     */
    private final int jobsActive;

    /**
     * The number of {@link PrintOut} jobs that were updated with a new CUPS
     * state.
     */
    private final int jobsUpdated;

    /**
     * The number of jobs that were not found in CUPS: this could be due to an
     * off-line or disabled printer, or a printer that has been removed.
     */
    private final int jobsNotFound;

    /**
     *
     * @param active
     *            The number of active {@link PrintOut} jobs.
     * @param updated
     *            The number of {@link PrintOut} jobs that were updated with a
     *            new CUPS state.
     * @param notFound
     *            The number of jobs that were not found in CUPS.
     */
    public SyncPrintJobsResult(final int active, final int updated,
            final int notFound) {
        this.jobsActive = active;
        this.jobsUpdated = updated;
        this.jobsNotFound = notFound;
    }

    /**
     * @return The number of active {@link PrintOut} jobs.
     */
    public int getJobsActive() {
        return jobsActive;
    }

    /**
     * @return The number of {@link PrintOut} jobs that were updated with a new
     *         CUPS state.
     */
    public int getJobsUpdated() {
        return jobsUpdated;
    }

    /**
     * @return The number of jobs that were not found in CUPS.
     */
    public int getJobsNotFound() {
        return jobsNotFound;
    }

}
