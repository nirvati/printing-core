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
package org.savapage.core.print.proxy;

import org.savapage.core.ipp.IppJobStateEnum;
import org.savapage.core.jpa.PrintOut;

/**
 *
 * @author Rijk Ravestein
 *
 */
public abstract class ProxyPrintJobStatusMixin {

    private final String printerName;
    private final Integer jobId;
    private final String jobName;
    private final IppJobStateEnum jobState;

    private Integer cupsCreationTime;
    private Integer cupsCompletedTime;

    /**
     * Update time (milliseconds).
     */
    private long updateTime;

    /**
     *
     */
    final private StatusSource statusSource;

    /**
     *
     * @author rijk
     *
     */
    public enum StatusSource {
        /**
         * The CUPS notifier.
         */
        CUPS,

        /**
         * The proxy print as committed {@link PrintOut} database object.
         */
        PRINT_OUT
    };

    protected ProxyPrintJobStatusMixin(final String printerName,
            final Integer jobId, final String jobName,
            final IppJobStateEnum jobState, final StatusSource statusSource) {

        this.printerName = printerName;
        this.jobId = jobId;
        this.jobName = jobName;
        this.jobState = jobState;
        this.statusSource = statusSource;
    }

    /**
     *
     * @return Unix epoch time (seconds).
     */
    public final Integer getCupsCreationTime() {
        return cupsCreationTime;
    }

    /**
     *
     * @param cupsCreationTime
     *            Unix epoch time (seconds).
     */
    public final void setCupsCreationTime(Integer cupsCreationTime) {
        this.cupsCreationTime = cupsCreationTime;
    }

    public final String getPrinterName() {
        return printerName;
    }

    public final Integer getJobId() {
        return jobId;
    }

    public final String getJobName() {
        return jobName;
    }

    public IppJobStateEnum getJobState() {
        return jobState;
    }

    /**
     *
     * @return Unix epoch time (seconds).
     */
    public final Integer getCupsCompletedTime() {
        return cupsCompletedTime;
    }

    /**
     *
     * @param completedTime
     *            Unix epoch time (seconds).
     */
    public final void setCupsCompletedTime(final Integer completedTime) {
        this.cupsCompletedTime = completedTime;
    }

    /**
     * @return {@code true} when the job state is finished. See
     *         {@link IppJobStateEnum#isFinished()}.
     */
    public final boolean isFinished() {
        return this.jobState.isFinished();
    }

    /**
     * @return Update time (milliseconds).
     */
    public long getUpdateTime() {
        return updateTime;
    }

    /**
     * @param updateTime
     *            Update time (milliseconds).
     */
    public void setUpdateTime(long updateTime) {
        this.updateTime = updateTime;
    }

    /**
     *
     * @return The source of the job status.
     */
    public final StatusSource getStatusSource() {
        return statusSource;
    }

}
