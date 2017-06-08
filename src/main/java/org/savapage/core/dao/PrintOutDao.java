/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2017 Datraverse B.V.
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
package org.savapage.core.dao;

import java.util.List;
import java.util.Map;

import org.savapage.core.ipp.IppJobStateEnum;
import org.savapage.core.jpa.PrintOut;
import org.savapage.core.jpa.Printer;
import org.savapage.core.print.proxy.JsonProxyPrintJob;

/**
 *
 * @author Rijk Ravestein
 *
 */
public interface PrintOutDao extends GenericDao<PrintOut> {

    /**
     * Finds the CUPS print job with the identifying attributes equal to the
     * parameters passed.
     *
     * @param jobPrinterName
     *            The name of the printer.
     * @param jobId
     *            The job ID.
     * @return The PrintOut object or {@code null} when not found.
     */
    PrintOut findCupsJob(String jobPrinterName, final Integer jobId);

    /**
     * Finds the CUPS jobs which are NOT registered as completed.
     *
     * @return The list of jobs ordered by CUPS printer name and job id.
     */
    List<PrintOut> findActiveCupsJobs();

    /**
     * Updates a {@link PrintOut} instance with new CUPS job state data.
     *
     * <p>
     * NOTE: Use this method instead of {@link #update(PrintOut)}, to make sure
     * updated data are available to other resident threads. Updates committed
     * with {@link #update(PrintOut)}, i.e merge(), will <b>not</b> show in
     * other resident threads (this is a Hibernate "feature").
     * </p>
     *
     * @param printOutId
     *            The database primary key of the {@link PrintOut} instance.
     * @param ippState
     *            The {@code IppJobStateEnum}.
     * @param cupsCompletedTime
     *            The CUPS completed time (can be {@code null}).
     * @return {@code true} when instance is updated, {@code false} when not
     *         found.
     */
    boolean updateCupsJob(Long printOutId, IppJobStateEnum ippState,
            Integer cupsCompletedTime);

    /**
     * Updates a {@link PrintOut} instance with a new Printer and new CUPS job
     * data.
     *
     * <p>
     * NOTE: Use this method instead of {@link #update(PrintOut)}, to make sure
     * updated data are available to other resident threads. Updates committed
     * with {@link #update(PrintOut)}, i.e merge(), will <b>not</b> show in
     * other resident threads (this is a Hibernate "feature").
     * </p>
     *
     * @param printOutId
     *            The database primary key of the {@link PrintOut} instance.
     * @param printer
     *            The {@link Printer} the job was printed to.
     * @param printJob
     *            The print job data.
     * @param ippOptions
     *            The IPP options.
     * @return {@code true} when instance is updated, {@code false} when not
     *         found.
     */
    boolean updateCupsJobPrinter(Long printOutId, Printer printer,
            JsonProxyPrintJob printJob, Map<String, String> ippOptions);

    /**
     * Gets the {@link IppJobStateEnum} value of a {@link PrintOut} job.
     *
     * @param printOut
     *            The {@link PrintOut} job.
     * @return The {@link IppJobStateEnum}, or {@code null} when no job state is
     *         present.
     */
    IppJobStateEnum getIppJobState(PrintOut printOut);
}
