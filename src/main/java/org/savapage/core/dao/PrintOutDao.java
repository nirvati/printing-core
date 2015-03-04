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
package org.savapage.core.dao;

import java.util.List;

import org.savapage.core.ipp.IppJobStateEnum;
import org.savapage.core.jpa.PrintOut;

/**
 *
 * @author Datraverse B.V.
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
     * @param jobCreationTime
     *            The time created.
     * @return The PrintOut object or {@code null} when not found.
     */
    PrintOut findCupsJob(String jobPrinterName, final Integer jobId,
            final Integer jobCreationTime);

    /**
     * Finds the CUPS jobs which are NOT registered as completed.
     *
     * @return The list of jobs ordered by CUPS printer name and job id.
     */
    List<PrintOut> findActiveCupsJobs();

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
