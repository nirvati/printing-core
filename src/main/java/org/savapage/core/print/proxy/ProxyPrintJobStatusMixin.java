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
package org.savapage.core.print.proxy;

import org.savapage.core.ipp.IppJobStateEnum;
import org.savapage.core.jpa.PrintOut;

/**
 *
 * @author Datraverse B.V.
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
    public Integer getCupsCreationTime() {
        return cupsCreationTime;
    }

    /**
     *
     * @param cupsCreationTime
     *            Unix epoch time (seconds).
     */
    public void setCupsCreationTime(Integer cupsCreationTime) {
        this.cupsCreationTime = cupsCreationTime;
    }

    public String getPrinterName() {
        return printerName;
    }

    public Integer getJobId() {
        return jobId;
    }

    public String getJobName() {
        return jobName;
    }

    public IppJobStateEnum getJobState() {
        return jobState;
    }

    /**
     *
     * @return Unix epoch time (seconds).
     */
    public Integer getCupsCompletedTime() {
        return cupsCompletedTime;
    }

    /**
     *
     * @param cupsCompletedTime
     *            Unix epoch time (seconds).
     */
    public void setCupsCompletedTime(Integer cupsCompletedTime) {
        this.cupsCompletedTime = cupsCompletedTime;
    }

    public boolean isCompleted() {
        return this.cupsCompletedTime != null
                && this.cupsCompletedTime.intValue() != 0;
    }

    public StatusSource getStatusSource() {
        return statusSource;
    }

}
