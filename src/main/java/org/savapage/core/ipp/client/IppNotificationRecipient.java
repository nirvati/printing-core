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
package org.savapage.core.ipp.client;

import java.io.IOException;
import java.util.Date;

import org.savapage.core.PerformanceLogger;
import org.savapage.core.community.CommunityDictEnum;
import org.savapage.core.dao.DaoContext;
import org.savapage.core.dao.PrintOutDao;
import org.savapage.core.ipp.IppJobStateEnum;
import org.savapage.core.jpa.PrintOut;
import org.savapage.core.msg.UserMsgIndicator;
import org.savapage.core.services.ProxyPrintService;
import org.savapage.core.services.ServiceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class IppNotificationRecipient {

    /**
     *
     */
    private static final Logger LOGGER = LoggerFactory
            .getLogger(IppNotificationRecipient.class);

    /**
     * .
     */
    private final ProxyPrintService proxyPrintService;

    /**
     *
     * @param proxyPrintService
     */
    public IppNotificationRecipient(final ProxyPrintService proxyPrintService) {
        this.proxyPrintService = proxyPrintService;
    }

    /**
     *
     * @param event
     * @param printer_name
     * @param printer_state
     */
    public void onPrinterEvent(final String event, final String printer_name,
            final Integer printer_state) {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("[" + event + "] printer [" + printer_name
                    + "] state [" + printer_state + "]");
        }
    }

    /**
     *
     * @param event
     */
    public void onServerEvent(final String event) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("[" + event + "]");
        }
    }

    /**
     * Handles the IPP job event. The {@link PrintOut} object belonging to this
     * event is updated with the end-state of the job.
     *
     * @param event
     * @param jobId
     * @param jobName
     * @param jobState
     * @param creation_time
     * @param completed_time
     * @param printer_name
     * @param printer_state
     * @throws Exception
     */
    public void onJobEvent(final String event, final Integer jobId,
            final String jobName, final Integer jobState,
            final Integer creation_time, final Integer completed_time,
            final String printer_name, final Integer printer_state)
            throws Exception {

        final Date perfStartTime = PerformanceLogger.startTime();

        final String perfMessage;

        if (PerformanceLogger.isEnabled()) {
            perfMessage =
                    String.format("Printer %s job [%s]", printer_name,
                            jobId.toString());
        } else {
            perfMessage = null;
        }

        if (LOGGER.isDebugEnabled()) {

            LOGGER.debug("[" + event + "] job [" + jobId + "] name [" + jobName
                    + "] state [" + jobState + "] creation_time ["
                    + creation_time + "] completed_time [" + completed_time
                    + "] printer [" + printer_name + "] state ["
                    + printer_state + "]");
        }
        /*
         * NOTE: when jobState is invalid an exception is thrown.
         */
        final IppJobStateEnum ippState = IppJobStateEnum.asEnum(jobState);

        /*
         * This is the initial state, no update needed.
         */
        if (ippState == IppJobStateEnum.IPP_JOB_PENDING) {
            return;
        }

        /*
         * For now, also skip the processing part, no update needed (we are
         * interested in end-state only).
         */
        if (ippState == IppJobStateEnum.IPP_JOB_PROCESSING) {
            return;
        }

        final PrintOutDao printOutDao =
                ServiceContext.getDaoContext().getPrintOutDao();

        /*
         * Find PrintOut CUPS job in the database.
         */
        PrintOut printOut =
                printOutDao.findCupsJob(printer_name, jobId);

        if (printOut != null) {

            final String userid =
                    updatePrintOutStatus(printOut, ippState, completed_time);

            writePrintOutUserMsg(userid, completed_time);

            if (PerformanceLogger.isEnabled()) {
                PerformanceLogger.log(this.getClass(), "onJobEvent",
                        perfStartTime, perfMessage);
            }

            return;
        }

        String userid = null;

        printOut = printOutDao.findCupsJob(printer_name, jobId);

        if (printOut == null) {

            if (LOGGER.isDebugEnabled()) {

                final String msg =
                        "[" + event + "] job [" + jobId + "] name [" + jobName
                                + "] state [" + jobState + "] creation_time ["
                                + creation_time + "] printer [" + printer_name
                                + "] not found (may be this is not a "
                                + CommunityDictEnum.SAVAPAGE.getWord()
                                + " job).";

                LOGGER.debug(msg);
            }

        } else {
            userid = updatePrintOutStatus(printOut, ippState, completed_time);
        }

        // Important: LAST action.
        writePrintOutUserMsg(userid, completed_time);

        if (PerformanceLogger.isEnabled()) {
            PerformanceLogger.log(this.getClass(), "onJobEvent (locked)",
                    perfStartTime, perfMessage);
        }

    }

    /**
     *
     * @param userid
     * @param completed_time
     * @throws IOException
     */
    private void writePrintOutUserMsg(final String userid,
            final Integer completed_time) throws IOException {

        if (userid != null && completed_time != null && completed_time != 0) {
            UserMsgIndicator.write(userid, new Date(completed_time * 1000L),
                    UserMsgIndicator.Msg.PRINT_OUT_COMPLETED, null);
        }
    }

    /**
     * Updates the {@link PrintOut} with CUPS status and completion time, and
     * signals the user via {@link UserMsgIndicator}.
     *
     * @param printOut
     * @param ippState
     * @param completed_time
     * @return The user id of the printOut.
     */
    private String updatePrintOutStatus(final PrintOut printOut,
            final IppJobStateEnum ippState, final Integer completed_time) {

        final DaoContext daoContext = ServiceContext.getDaoContext();

        boolean rollback = false;

        final String userid;

        try {

            userid = printOut.getDocOut().getDocLog().getUser().getUserId();

            daoContext.beginTransaction();

            rollback = true;

            printOut.setCupsJobState(ippState.asInteger());
            printOut.setCupsCompletedTime(completed_time);

            daoContext.getPrintOutDao().update(printOut);

            daoContext.commit();
            rollback = false;

        } finally {

            if (rollback) {
                daoContext.rollback();
            }

        }

        return userid;
    }

}
