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

import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.savapage.core.SpException;
import org.savapage.core.SpInfo;
import org.savapage.core.cometd.AdminPublisher;
import org.savapage.core.cometd.PubLevelEnum;
import org.savapage.core.cometd.PubTopicEnum;
import org.savapage.core.dao.DaoContext;
import org.savapage.core.dao.PrintOutDao;
import org.savapage.core.ipp.IppJobStateEnum;
import org.savapage.core.jpa.PrintOut;
import org.savapage.core.msg.UserMsgIndicator;
import org.savapage.core.services.ProxyPrintService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.util.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Datraverse B.V.
 *
 */
public class ProxyPrintJobStatusMonitor extends Thread {

    /**
     *
     */
    private static final String OBJECT_NAME_FOR_LOG =
            "Print Job Status monitor";

    /**
     * .
     */
    private static final ProxyPrintService PROXY_PRINT_SERVICE = ServiceContext
            .getServiceFactory().getProxyPrintService();;

    /**
     *
     */
    private static class SingletonHolder {

        public static final ProxyPrintJobStatusMonitor INSTANCE =
                new ProxyPrintJobStatusMonitor().execute();

        public static ProxyPrintJobStatusMonitor init() {
            return INSTANCE;
        }
    }

    /**
     * .
     */
    private static final Logger LOGGER = LoggerFactory
            .getLogger(ProxyPrintJobStatusMonitor.class);

    /**
     * .
     */
    private boolean keepProcessing = true;

    /**
     * .
     */
    private boolean isProcessing = false;

    /**
     * .
     */
    private final ConcurrentMap<Integer, PrintJobStatus> jobStatusMap =
            new ConcurrentHashMap<>();

    /**
     * Polling time of the job status map.
     */
    protected static final long POLLING_MSEC =
            2 * DateUtil.DURATION_MSEC_SECOND;

    /**
     * Waiting time till processing finished.
     */
    protected static final long WAIT_TO_FINISH_MSEC =
            1 * DateUtil.DURATION_MSEC_SECOND;

    /**
     *
     */
    private final class PrintJobStatus {

        private final String printerName;
        private final Integer jobId;
        private final String jobName;

        private IppJobStateEnum jobStateCups;
        private IppJobStateEnum jobStatePrintOut;

        /**
         * Unix epoch time (seconds).
         */
        private final Integer cupsCreationTime;

        /**
         * Unix epoch time (seconds).
         */
        private Integer cupsCompletedTime;

        /**
         *
         * @param mixin
         */
        public PrintJobStatus(final ProxyPrintJobStatusMixin mixin) {

            this.printerName = mixin.getPrinterName();
            this.jobId = mixin.getJobId();
            this.jobName = mixin.getJobName();

            if (mixin.getStatusSource() == ProxyPrintJobStatusMixin.StatusSource.CUPS) {
                this.jobStateCups = mixin.getJobState();
            } else {
                this.jobStatePrintOut = mixin.getJobState();
            }

            this.cupsCreationTime = mixin.getCupsCreationTime();
            this.cupsCompletedTime = mixin.getCupsCompletedTime();

        }

        public IppJobStateEnum getJobStateCups() {
            return jobStateCups;
        }

        public void setJobStateCups(IppJobStateEnum jobStateCups) {
            this.jobStateCups = jobStateCups;
        }

        public IppJobStateEnum getJobStatePrintOut() {
            return jobStatePrintOut;
        }

        public void setJobStatePrintOut(IppJobStateEnum jobStatePrintOut) {
            this.jobStatePrintOut = jobStatePrintOut;
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

        /**
         *
         * @return Unix epoch time (seconds).
         */
        public Integer getCupsCreationTime() {
            return cupsCreationTime;
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
            return this.cupsCompletedTime != null;
        }

    }

    /**
     * Prevent public instantiation.
     */
    private ProxyPrintJobStatusMonitor() {
    }

    /**
     * Wrapper for {@link Thread#start()}.
     *
     * @return this instance.
     */
    private ProxyPrintJobStatusMonitor execute() {
        this.start();
        return this;
    }

    /**
     * Wrapper to get the monitoring going.
     */
    public static void init() {
        SingletonHolder.init();

        SpInfo.instance()
                .log(String.format("%s started.", OBJECT_NAME_FOR_LOG));
    }

    /**
     *
     * @return The number of pending proxy print jobs.
     */
    public static int getPendingJobs() {
        return SingletonHolder.INSTANCE.jobStatusMap.size();
    }

    /**
     *
     */
    public static void exit() {
        SingletonHolder.INSTANCE.shutdown();
    }

    /**
     *
     * @param jobStatus
     */
    public static void notify(final ProxyPrintJobStatusCups jobStatus) {
        SingletonHolder.INSTANCE.onNotify(jobStatus);
    }

    /**
     * Note: a notification for a remote printer is ignored.
     *
     * @param jobStatus
     */
    public static void notify(final ProxyPrintJobStatusPrintOut jobStatus) {

        final Boolean isLocalPrinter =
                PROXY_PRINT_SERVICE.isLocalPrinter(jobStatus.getPrinterName());

        if (isLocalPrinter != null && isLocalPrinter) {
            SingletonHolder.INSTANCE.onNotify(jobStatus);
        }
    }

    /**
     *
     * @param jobUpdate
     */
    private void onNotify(final ProxyPrintJobStatusMixin jobUpdate) {

        final PrintJobStatus jobCurrent =
                this.jobStatusMap.get(jobUpdate.getJobId());

        /*
         * This is the first status event for job id.
         */
        if (jobCurrent == null) {

            this.jobStatusMap.put(jobUpdate.getJobId(), new PrintJobStatus(
                    jobUpdate));

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("Add job [%s] [%d] [%s] [%s] [%s]",
                        jobUpdate.getPrinterName(), jobUpdate.getJobId(),
                        jobUpdate.getJobName(), jobUpdate.getJobState()
                                .toString(), jobUpdate.getStatusSource()));
            }

            return;
        }

        /*
         * A status event of job id already present: update the completed time
         * and the status.
         */

        if (LOGGER.isDebugEnabled()) {

            final StringBuilder msg = new StringBuilder();

            msg.append("Update job [").append(jobUpdate.getPrinterName())
                    .append("] [").append(jobUpdate.getJobId()).append("] [")
                    .append(jobUpdate.getJobName()).append("] : current [");

            if (jobCurrent.getJobStateCups() != null) {

                msg.append(jobCurrent.getJobStateCups()).append("] [")
                        .append(ProxyPrintJobStatusMixin.StatusSource.CUPS);

            } else if (jobCurrent.getJobStatePrintOut() != null) {

                msg.append(jobCurrent.getJobStatePrintOut())
                        .append("] [")
                        .append(ProxyPrintJobStatusMixin.StatusSource.PRINT_OUT);
            }

            msg.append("] update [").append(jobUpdate.getJobState().toString())
                    .append("] [").append(jobUpdate.getStatusSource())
                    .append("]");

            LOGGER.debug(msg.toString());
        }

        /*
         * Update the completed time and the status.
         */
        if (jobUpdate.isCompleted() && !jobCurrent.isCompleted()) {
            jobCurrent.setCupsCompletedTime(jobUpdate.getCupsCompletedTime());
        }

        switch (jobUpdate.getStatusSource()) {

        case CUPS:
            jobCurrent.setJobStateCups(jobUpdate.getJobState());
            break;

        case PRINT_OUT:
            jobCurrent.setJobStatePrintOut(jobUpdate.getJobState());
            break;

        default:
            throw new SpException("[" + jobUpdate.getStatusSource()
                    + "] is not supported");
        }

    }

    /**
     * Max 5 minutes wait for a CUPS/PRINT_OUT match.
     */
    private static final long JOB_STATUS_MAX_AGE_MSEC =
            1 * DateUtil.DURATION_MSEC_MINUTE;

    @Override
    public void run() {

        this.isProcessing = true;

        final PrintOutDao printOutDao =
                ServiceContext.getDaoContext().getPrintOutDao();

        while (this.keepProcessing) {

            final long timeNow = new Date().getTime();

            final Iterator<Integer> iter =
                    this.jobStatusMap.keySet().iterator();

            while (iter.hasNext()) {

                final PrintJobStatus job = this.jobStatusMap.get(iter.next());

                if (job.getCupsCreationTime() == null
                        || job.getCupsCompletedTime() == null) {
                    continue;
                }

                /*
                 * The CUPS creation time was set when the job was added by
                 * either CUPS or PRINT_OUT (whoever is first).
                 */
                if (job.jobStatePrintOut == null) {

                    /*
                     * No corresponding PRINT_OUT received (yet). How long are
                     * we waiting?
                     */
                    final long msecAge =
                            timeNow - job.getCupsCreationTime()
                                    * DateUtil.DURATION_MSEC_SECOND;

                    final boolean orphanedPrint =
                            msecAge > JOB_STATUS_MAX_AGE_MSEC;

                    if (!orphanedPrint) {
                        // Let it stay.
                        continue;
                    }

                    /*
                     * Wait for PRINT_OUT message expired: this is probably an
                     * external print action (from outside SavaPage).
                     */
                    final StringBuilder msg = new StringBuilder();

                    msg.append("External CUPS job #").append(job.getJobId())
                            .append(" \"").append(job.getJobName())
                            .append("\" on printer ")
                            .append(job.getPrinterName()).append(" completed.");

                    AdminPublisher.instance().publish(PubTopicEnum.CUPS,
                            PubLevelEnum.WARN, msg.toString());

                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug(msg.toString());
                    }

                } else {

                    /*
                     * Find PrintOut CUPS job in the database.
                     */
                    final PrintOut printOut =
                            printOutDao.findCupsJob(job.getPrinterName(),
                                    job.getJobId(), job.getCupsCreationTime());

                    if (printOut == null) {

                        final StringBuilder msg = new StringBuilder();

                        msg.append("Print log of CUPS job #")
                                .append(job.getJobId()).append(" \"")
                                .append(job.getJobName())
                                .append("\" on printer ")
                                .append(job.getPrinterName())
                                .append(" not found.");

                        AdminPublisher.instance().publish(PubTopicEnum.CUPS,
                                PubLevelEnum.ERROR, msg.toString());

                        if (LOGGER.isErrorEnabled()) {
                            LOGGER.error(msg.toString());
                        }

                    } else {

                        final StringBuilder msg = new StringBuilder();

                        msg.append("CUPS job #").append(job.getJobId())
                                .append(" \"").append(job.getJobName())
                                .append("\" on printer ")
                                .append(job.getPrinterName())
                                .append(" completed.");

                        AdminPublisher.instance().publish(PubTopicEnum.CUPS,
                                PubLevelEnum.INFO, msg.toString());

                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug(msg.toString());
                        }

                        final String userid =
                                updatePrintOutStatus(printOut,
                                        job.getJobStateCups(),
                                        job.getCupsCompletedTime());

                        try {

                            writePrintOutUserMsg(userid,
                                    job.getCupsCompletedTime());

                        } catch (IOException e) {
                            AdminPublisher.instance().publish(
                                    PubTopicEnum.CUPS, PubLevelEnum.ERROR,
                                    e.getMessage());
                        }

                    }
                }

                /*
                 * Remove job from the map.
                 */
                iter.remove();
            }

            try {
                if (this.keepProcessing) {
                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace("wait ...");
                    }
                    Thread.sleep(POLLING_MSEC);
                }
            } catch (InterruptedException e) {
                LOGGER.info(e.getMessage());
            }
        }

        this.isProcessing = false;
    }

    /**
     *
     * @param userid
     * @param cupsCompletedTime
     * @throws IOException
     */
    private void writePrintOutUserMsg(final String userid,
            final Integer cupsCompletedTime) throws IOException {

        if (userid != null && cupsCompletedTime != null
                && cupsCompletedTime != 0) {
            UserMsgIndicator.write(userid, new Date(cupsCompletedTime * 1000L),
                    UserMsgIndicator.Msg.PRINT_OUT_COMPLETED, null);
        }
    }

    /**
     * Updates the {@link PrintOut} with CUPS status and completion time, and
     * signals the user via {@link UserMsgIndicator}.
     *
     * @param printOut
     * @param ippState
     * @param cupsCompletedTime
     * @return The user id of the printOut.
     */
    private String updatePrintOutStatus(final PrintOut printOut,
            final IppJobStateEnum ippState, final Integer cupsCompletedTime) {

        final DaoContext daoContext = ServiceContext.getDaoContext();

        boolean rollback = false;

        final String userid;

        try {

            userid = printOut.getDocOut().getDocLog().getUser().getUserId();

            daoContext.beginTransaction();

            rollback = true;

            printOut.setCupsJobState(ippState.asInt());
            printOut.setCupsCompletedTime(cupsCompletedTime);

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

    /**
     * .
     */
    public void shutdown() {

        SpInfo.instance().log(
                String.format("Shutting down %s ...", OBJECT_NAME_FOR_LOG));

        this.keepProcessing = false;

        /*
         * Waiting for active requests to finish.
         */
        while (this.isProcessing) {
            try {
                Thread.sleep(WAIT_TO_FINISH_MSEC);
            } catch (InterruptedException ex) {
                LOGGER.error(ex.getMessage(), ex);
                break;
            }
        }

        SpInfo.instance()
                .log(String.format("... %s shutdown completed.",
                        OBJECT_NAME_FOR_LOG));

    }

}
