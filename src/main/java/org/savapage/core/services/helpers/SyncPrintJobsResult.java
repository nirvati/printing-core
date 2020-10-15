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
package org.savapage.core.services.helpers;

import org.savapage.core.ipp.IppJobStateEnum;
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
    private final int jobsStateChange;

    /**
     * The number of CUPS jobs that were forced to cancel because they were
     * {@link IppJobStateEnum#IPP_JOB_STOPPED}.
     */
    private final int jobsForcedCancel;

    /**
     * The number of jobs that were not found in CUPS: this could be due to an
     * off-line or disabled printer, or a printer that has been removed.
     */
    private final int jobsNotFound;

    /**
     * The last CUPS job id handled.
     */
    private final int jobIdLast;

    /**
     *
     * @param active
     *            The number of active {@link PrintOut} jobs.
     * @param stateChange
     *            The number of {@link PrintOut} jobs that were updated with a
     *            new CUPS state.
     * @param forcedCancel
     *            The number of CUPS jobs that were forced to cancel because
     *            they were {@link IppJobStateEnum#IPP_JOB_STOPPED}.
     * @param notFound
     *            The number of jobs that were not found in CUPS.
     * @param lastJobId
     *            The last CUPS job id handled.
     */
    public SyncPrintJobsResult(final int active, final int stateChange,
            final int forcedCancel, final int notFound, final int lastJobId) {
        this.jobsActive = active;
        this.jobsStateChange = stateChange;
        this.jobsForcedCancel = forcedCancel;
        this.jobsNotFound = notFound;
        this.jobIdLast = lastJobId;
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
    public int getJobsStateChange() {
        return jobsStateChange;
    }

    /**
     * @return The number of CUPS jobs that were forced to cancel because they
     *         were {@link IppJobStateEnum#IPP_JOB_STOPPED}.
     */
    public int getJobsForcedCancel() {
        return jobsForcedCancel;
    }

    /**
     * @return
     */
    public int getJobsIdentical() {
        return jobsActive - jobsStateChange - jobsNotFound;
    }

    /**
     * @return The number of jobs that were not found in CUPS.
     */
    public int getJobsNotFound() {
        return jobsNotFound;
    }

    /**
     * @return The last CUPS job id handled.
     */
    public int getJobIdLast() {
        return jobIdLast;
    }

}
