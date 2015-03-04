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
package org.savapage.core.ipp.attribute.syntax;

import org.savapage.core.jpa.PrintOut;

/**
 * Note: see {@link org.savapage.core.ipp.IppJobStateEnum}, which is the Java
 * enum used to interpret the {@link PrintOut#getCupsJobState()}.
 *
 * @author Datraverse B.V.
 */
public class IppJobState extends IppEnum {

    /**
     * The SingletonHolder is loaded on the first execution of
     * {@link IppJobState#instance()} or the first access to
     * {@link SingletonHolder#INSTANCE}, not before.
     * <p>
     * <a href=
     * "http://en.wikipedia.org/wiki/Singleton_pattern#The_solution_of_Bill_Pugh"
     * >The Singleton solution of Bill Pugh</a>
     * </p>
     */
    private static class SingletonHolder {
        public static final IppJobState INSTANCE = new IppJobState();
    }

    public static final String STATE_PENDING = "3";
    public static final String STATE_PENDING_HELD = "4";
    public static final String STATE_PROCESSING = "5";
    public static final String STATE_PROCESSING_STOPPED = "6";
    public static final String STATE_CANCELED = "7";

    /**
     * The job has been aborted by the system, usually while the job was in the
     * 'processing' or 'processing- stopped' state and the Printer has completed
     * aborting the job and all job status attributes have reached their final
     * values for the job.
     *
     * While the Printer object is aborting the job, the job remains in its
     * current state, but the job's "job-state-reasons" attribute SHOULD contain
     * the 'processing-to-stop-point' and 'aborted-by- system' values. When the
     * job moves to the 'aborted' state, the 'processing-to-stop-point' value,
     * if present, MUST be removed, but the 'aborted-by-system' value, if
     * present, MUST remain.
     */
    public static final String STATE_ABORTED = "8";

    /**
     * The job has completed successfully or with warnings or errors after
     * processing and all of the job media sheets have been successfully stacked
     * in the appropriate output bin(s) and all job status attributes have
     * reached their final values for the job.
     *
     * The job's "job-state-reasons" attribute SHOULD contain one of:
     * 'completed-successfully', 'completed-with-warnings', or
     * 'completed-with-errors' values.
     */
    public static final String STATE_COMPLETED = "9";

    /**
     * This REQUIRED attribute identifies the current state of the job. Even
     * though the IPP protocol defines seven values for job states (plus the
     * out-of-band 'unknown' value - see Section 4.1), implementations only need
     * to support those states which are appropriate for the particular
     * implementation. In other words, a Printer supports only those job states
     * implemented by the output device and available to the Printer object
     * implementation.
     *
     * Standard enum values are:
     *
     * Values Symbolic Name and Description
     *
     * '3' 'pending': The job is a candidate to start processing, but is not yet
     * processing.
     *
     * '4' 'pending-held': The job is not a candidate for processing for any
     * number of reasons but will return to the 'pending' state as soon as the
     * reasons are no longer present. The job's "job-state-reason" attribute
     * MUST indicate why the job is no longer a candidate for processing.
     *
     * '5' 'processing': One or more of:
     *
     * 1. the job is using, or is attempting to use, one or more purely software
     * processes that are analyzing, creating, or interpreting a PDL, etc., 2.
     * the job is using, or is attempting to use, one or more hardware devices
     * that are interpreting a PDL, making marks on a medium, and/or performing
     * finishing, such as stapling, etc., 3. the Printer object has made the job
     * ready for printing, but the output device is not yet printing it, either
     * because the job hasn't reached the output device or because the job is
     * queued in the output device or some other spooler, awaiting the output
     * device to print it.
     *
     * When the job is in the 'processing' state, the entire job state includes
     * the detailed status represented in the Printer object's "printer-state",
     * "printer-state- reasons", and "printer-state-message" attributes.
     *
     * Implementations MAY, though they NEED NOT, include additional values in
     * the job's "job-state-reasons" attribute to indicate the progress of the
     * job, such as adding the 'job-printing' value to indicate when the output
     * device is actually making marks on paper and/or the
     * 'processing-to-stop-point' value to indicate that the IPP object is in
     * the process of canceling or aborting the job. Most implementations won't
     * bother with this nuance.
     *
     * '6' 'processing-stopped': The job has stopped while processing for any
     * number of reasons and will return to the 'processing' state as soon as
     * the reasons are no longer present.
     *
     * The job's "job-state-reason" attribute MAY indicate why the job has
     * stopped processing. For example, if the output device is stopped, the
     * 'printer-stopped' value MAY be included in the job's "job-state-reasons"
     * attribute.
     *
     * Note: When an output device is stopped, the device usually indicates its
     * condition in human readable form locally at the device. A client can
     * obtain more complete device status remotely by querying the Printer
     * object's "printer-state", "printer-state-reasons" and "printer-
     * state-message" attributes.
     *
     * '7' 'canceled': The job has been canceled by a Cancel-Job operation and
     * the Printer object has completed canceling the job and all job status
     * attributes have reached their final values for the job. While the Printer
     * object is canceling the job, the job remains in its current state, but
     * the job's "job-state-reasons" attribute SHOULD contain the
     * 'processing-to-stop-point' value and one of the 'canceled-by-user',
     * 'canceled-by-operator', or 'canceled-at-device' value. When the job moves
     * to the 'canceled' state, the 'processing-to-stop-point' value, if
     * present, MUST be removed, but the 'canceled-by-xxx', if present, MUST
     * remain.
     *
     * '8' 'aborted': The job has been aborted by the system, usually while the
     * job was in the 'processing' or 'processing- stopped' state and the
     * Printer has completed aborting the job and all job status attributes have
     * reached their final values for the job. While the Printer object is
     * aborting the job, the job remains in its current state, but the job's
     * "job-state-reasons" attribute SHOULD contain the
     * 'processing-to-stop-point' and 'aborted-by- system' values. When the job
     * moves to the 'aborted' state, the 'processing-to-stop-point' value, if
     * present, MUST be removed, but the 'aborted-by-system' value, if present,
     * MUST remain.
     *
     * '9' 'completed': The job has completed successfully or with warnings or
     * errors after processing and all of the job media sheets have been
     * successfully stacked in the appropriate output bin(s) and all job status
     * attributes have reached their final values for the job. The job's
     * "job-state-reasons" attribute SHOULD contain one of:
     * 'completed-successfully', 'completed-with-warnings', or
     * 'completed-with-errors' values.
     *
     * The final value for this attribute MUST be one of: 'completed',
     * 'canceled', or 'aborted' before the Printer removes the job altogether.
     * The length of time that jobs remain in the 'canceled', 'aborted', and
     * 'completed' states depends on implementation. See section 4.3.7.2.
     *
     * The following figure shows the normal job state transitions.
     *
     * <pre>
     *                                                       +----> canceled
     *                                                      /
     *        +----> pending --------> processing ---------+------> completed
     *        |         ^                   ^               \
     *    --->+         |                   |                +----> aborted
     *        |         v                   v               /
     *        +----> pending-held    processing-stopped ---+
     * </pre>
     *
     * Normally a job progresses from left to right. Other state transitions are
     * unlikely, but are not forbidden. Not shown are the transitions to the
     * 'canceled' state from the 'pending', 'pending- held', and
     * 'processing-stopped' states.
     *
     * Jobs reach one of the three terminal states: 'completed', 'canceled', or
     * 'aborted', after the jobs have completed all activity, including stacking
     * output media, after the jobs have completed all activity, and all job
     * status attributes have reached their final values for the job.
     *
     */

    /**
     * Gets the singleton instance.
     *
     * @return The singleton.
     */
    public static IppJobState instance() {
        return SingletonHolder.INSTANCE;
    }

}
