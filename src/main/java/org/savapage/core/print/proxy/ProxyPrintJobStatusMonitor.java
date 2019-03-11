/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2019 Datraverse B.V.
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
package org.savapage.core.print.proxy;

import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.lang3.StringUtils;
import org.savapage.core.SpException;
import org.savapage.core.SpInfo;
import org.savapage.core.cometd.AdminPublisher;
import org.savapage.core.cometd.PubLevelEnum;
import org.savapage.core.cometd.PubTopicEnum;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp;
import org.savapage.core.dao.DaoContext;
import org.savapage.core.dao.PrintOutDao;
import org.savapage.core.ipp.IppJobStateEnum;
import org.savapage.core.ipp.client.IppConnectException;
import org.savapage.core.jpa.PrintOut;
import org.savapage.core.msg.UserMsgIndicator;
import org.savapage.core.print.proxy.ProxyPrintJobStatusMixin.StatusSource;
import org.savapage.core.services.ProxyPrintService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.util.DateUtil;
import org.savapage.ext.papercut.PaperCutPrintMonitorPattern;
import org.savapage.ext.papercut.services.PaperCutService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Processes {@link PrintJobStatus} events in a separate thread.
 *
 * @author Rijk Ravestein
 *
 */
public final class ProxyPrintJobStatusMonitor extends Thread {

    /** */
    private static final String OBJECT_NAME_FOR_LOG =
            "Print Job Status monitor";

    /** */
    private static final ProxyPrintService PROXY_PRINT_SERVICE =
            ServiceContext.getServiceFactory().getProxyPrintService();

    /** */
    private static final PaperCutService PAPERCUT_SERVICE =
            ServiceContext.getServiceFactory().getPaperCutService();

    /**
     *
     */
    private static class SingletonHolder {

        /**
         *
         */
        public static final ProxyPrintJobStatusMonitor INSTANCE =
                new ProxyPrintJobStatusMonitor().execute();

        /**
         *
         * @return The singleton instance.
         */
        public static ProxyPrintJobStatusMonitor init() {
            return INSTANCE;
        }
    }

    /**
     * .
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(ProxyPrintJobStatusMonitor.class);

    /**
     * .
     */
    private boolean keepProcessing = true;

    /**
     * .
     */
    private boolean isProcessing = false;

    /**
     * Look-up of {@link PrintJobStatus} by CUPS Job ID.
     */
    private final ConcurrentMap<Integer, PrintJobStatus> jobStatusMap =
            new ConcurrentHashMap<>();

    /**
     * Polling time of the job status map.
     */
    private static final long POLLING_MSEC = 2 * DateUtil.DURATION_MSEC_SECOND;

    /**
     * Waiting time till processing finished.
     */
    private static final long WAIT_TO_FINISH_MSEC =
            1 * DateUtil.DURATION_MSEC_SECOND;

    /**
     * Max wait for a CUPS/PRINT_OUT match (30 seconds).
     */
    private static final long TIMEOUT_CUPS_PRINTOUT_MATCH_MSEC =
            30 * DateUtil.DURATION_MSEC_SECOND;

    /**
     *
     */
    private final class PrintJobStatus {

        private final String printerName;
        private final Integer jobId;
        private final String jobName;

        /**
         * The current status.
         */
        private IppJobStateEnum jobStateCups;

        /**
         * The status update.
         */
        private IppJobStateEnum jobStateCupsUpdate;

        /**
         *
         */
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
         * Update time (milliseconds).
         */
        private long updateTime;

        /**
         *
         * @param mixin
         */
        public PrintJobStatus(final ProxyPrintJobStatusMixin mixin) {

            this.printerName = mixin.getPrinterName();
            this.jobId = mixin.getJobId();
            this.jobName = mixin.getJobName();

            if (mixin
                    .getStatusSource() == ProxyPrintJobStatusMixin.StatusSource.CUPS) {
                this.jobStateCups = mixin.getJobState();
            } else {
                this.jobStatePrintOut = mixin.getJobState();
            }

            this.cupsCreationTime = mixin.getCupsCreationTime();
            this.cupsCompletedTime = mixin.getCupsCompletedTime();
            this.updateTime = mixin.getUpdateTime();

        }

        public IppJobStateEnum getJobStateCups() {
            return jobStateCups;
        }

        public void setJobStateCups(IppJobStateEnum jobStateCups) {
            this.jobStateCups = jobStateCups;
        }

        public IppJobStateEnum getJobStateCupsUpdate() {
            return jobStateCupsUpdate;
        }

        public void setJobStateCupsUpdate(IppJobStateEnum jobStateCupsUpdate) {
            this.jobStateCupsUpdate = jobStateCupsUpdate;
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
         * @return {@code true} when the job state is finished. See
         *         {@link IppJobStateEnum#isFinished()}.
         */
        public boolean isFinished() {
            // Mantis #858
            return this.jobStateCups != null && this.jobStateCups.isFinished();
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
     * Notifies job status from {@link StatusSource#CUPS}.
     *
     * @param jobStatus
     *            The {@link ProxyPrintJobStatusCups}.
     */
    public static void notify(final ProxyPrintJobStatusCups jobStatus) {
        SingletonHolder.INSTANCE.onNotify(jobStatus);
    }

    /**
     * Notifies job status from {@link StatusSource#PRINT_OUT}.
     * <p>
     * Note: a notification for a remote printer is ignored.
     * </p>
     *
     * @param printerName
     *            The CUPS printer name.
     * @param printJob
     *            The CUPS job data.
     */
    public static void notifyPrintOut(final String printerName,
            final JsonProxyPrintJob printJob) {

        final Boolean isLocalPrinter =
                PROXY_PRINT_SERVICE.isLocalPrinter(printerName);

        if (isLocalPrinter == null || !isLocalPrinter) {
            return;
        }

        final IppJobStateEnum jobState =
                IppJobStateEnum.asEnum(printJob.getJobState().intValue());

        final ProxyPrintJobStatusPrintOut jobStatus =
                new ProxyPrintJobStatusPrintOut(printerName,
                        printJob.getJobId(), printJob.getTitle(), jobState);

        jobStatus.setCupsCreationTime(printJob.getCreationTime());
        jobStatus.setCupsCompletedTime(printJob.getCompletedTime());
        jobStatus.setUpdateTime(System.currentTimeMillis());

        SingletonHolder.INSTANCE.onNotify(jobStatus);
    }

    /**
     * Notifies a job state from a {@link StatusSource}.
     *
     * @param jobUpdate
     *            The status update.
     */
    private void onNotify(final ProxyPrintJobStatusMixin jobUpdate) {

        if (jobUpdate.getCupsCreationTime() == null) {
            /*
             * The CUPS creation time is set when the job is added by either
             * CUPS or PRINT_OUT (whoever is first).
             */
            LOGGER.warn(String.format(
                    "CUPS Job [%d] REFUSED because is has"
                            + " no creation time.",
                    jobUpdate.getJobId().intValue()));
            return;
        }

        final PrintJobStatus jobCurrent =
                this.jobStatusMap.get(jobUpdate.getJobId());

        /*
         * First status event for job id?
         */
        if (jobCurrent == null) {

            this.jobStatusMap.put(jobUpdate.getJobId(),
                    new PrintJobStatus(jobUpdate));

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("Add job [%s] [%d] [%s] [%s] [%s]",
                        jobUpdate.getPrinterName(), jobUpdate.getJobId(),
                        StringUtils.defaultString(jobUpdate.getJobName()),
                        jobUpdate.getJobState().toString(),
                        jobUpdate.getStatusSource()));
            }

            return;
        }

        jobCurrent.setUpdateTime(jobUpdate.getUpdateTime());

        /*
         * Mantis #734: Correct missing CUPS job completion time. Mantis #834:
         * Handle missing CUPS job completion time.
         *
         * For printers that got their jobs delivered from a Printer Class,
         * completion time is set to zero (0) by our SavaPage CUPS notifier.
         */
        if (jobUpdate.isFinished()) {

            // CUPS creation and completed time is in seconds.
            final int timeNowSecs = PROXY_PRINT_SERVICE.getCupsSystemTime();

            final int timeCompletedSecs =
                    jobUpdate.getCupsCompletedTime().intValue();
            /*
             * Just to be sure, we also check illogical time deviations.
             * Comparison is valid for local CUPS. For (future) notifications
             * from remote CUPS, this host and the remote host must be NTP
             * sync-ed.
             */
            if (timeCompletedSecs == 0 || timeNowSecs < timeCompletedSecs
                    || timeCompletedSecs < jobCurrent.getCupsCreationTime()
                            .intValue()) {

                if (timeCompletedSecs != 0 && LOGGER.isWarnEnabled()) {
                    LOGGER.warn(String.format(
                            "Completed time for Printer [%s] Job [%d] %s"
                                    + " corrected from [%d] to [%d]",
                            jobUpdate.getPrinterName(),
                            jobUpdate.getJobId().intValue(),
                            jobUpdate.getJobState().asLogText(),
                            timeCompletedSecs, timeNowSecs));
                }

                jobUpdate.setCupsCompletedTime(Integer.valueOf(timeNowSecs));
            }
        }

        /*
         * A status event of job id already present: update the completed time
         * and the status.
         */
        if (jobUpdate.isFinished() && !jobCurrent.isFinished()) {
            jobCurrent.setCupsCompletedTime(jobUpdate.getCupsCompletedTime());
        }

        if (LOGGER.isDebugEnabled()) {

            final StringBuilder msg = new StringBuilder();

            msg.append("Update job [").append(jobUpdate.getPrinterName())
                    .append("] [").append(jobUpdate.getJobId()).append("] [")
                    .append(StringUtils.defaultString(jobUpdate.getJobName()))
                    .append("] : current [");

            if (jobCurrent.getJobStateCups() != null) {

                msg.append(jobCurrent.getJobStateCups()).append("][")
                        .append(ProxyPrintJobStatusMixin.StatusSource.CUPS);

            } else if (jobCurrent.getJobStatePrintOut() != null) {

                msg.append(jobCurrent.getJobStatePrintOut()).append("][")
                        .append(ProxyPrintJobStatusMixin.StatusSource.PRINT_OUT);
            }

            msg.append("] update [").append(jobUpdate.getJobState().toString())
                    .append("][").append(jobUpdate.getStatusSource())
                    .append("]");

            if (jobCurrent.isFinished()) {
                msg.append(" Finished on: ")
                        .append(new Date(jobCurrent.getCupsCompletedTime()
                                * DateUtil.DURATION_MSEC_SECOND).toString());
            }

            LOGGER.debug(msg.toString());
        }

        switch (jobUpdate.getStatusSource()) {

        case CUPS:
            jobCurrent.setJobStateCupsUpdate(jobUpdate.getJobState());
            break;

        case PRINT_OUT:
            jobCurrent.setJobStatePrintOut(jobUpdate.getJobState());
            break;

        default:
            throw new SpException(
                    "[" + jobUpdate.getStatusSource() + "] is not supported");
        }
    }

    /**
     * Processes a print job status entry.
     *
     * @param printOut
     *            {@link PrintOut} object.
     * @param jobStatus
     *            The {@link PrintJobStatus}.
     * @return {@code true} when job status reached steady end-state.
     */
    private boolean processJobStatusEntry(final PrintOut printOut,
            final PrintJobStatus jobStatus) {

        if (LOGGER.isDebugEnabled()) {

            final StringBuilder log = new StringBuilder();

            log.append("PrintOut on printer [")
                    .append(jobStatus.getPrinterName()).append("] job [")
                    .append(jobStatus.getJobId()).append("] status [");

            if (jobStatus.getJobStatePrintOut() != null) {
                log.append(jobStatus.getJobStatePrintOut().toString());
            }
            log.append("], CupsState [");
            if (jobStatus.getJobStateCups() != null) {
                log.append(jobStatus.getJobStateCups().toString());
            }
            log.append("], CupsUpdate [");
            if (jobStatus.getJobStateCupsUpdate() != null) {
                log.append(jobStatus.getJobStateCupsUpdate().toString());
            }
            log.append("]");

            log.append(" PrintOut [").append(IppJobStateEnum
                    .asEnum(printOut.getCupsJobState().intValue()).toString())
                    .append("]");

            LOGGER.debug(log.toString());
        }

        final IppJobStateEnum jobStateCups;

        if (jobStatus.getJobStateCupsUpdate() == null
                && jobStatus.getJobStateCups() == null) {

            jobStateCups = jobStatus.getJobStatePrintOut();
            jobStatus.setJobStateCupsUpdate(jobStateCups);

        } else if (jobStatus.getJobStateCupsUpdate() == null) {

            jobStateCups = jobStatus.getJobStateCups();
            jobStatus.setJobStateCupsUpdate(jobStateCups);

        } else if (jobStatus.getJobStateCups() == null) {

            jobStateCups = jobStatus.getJobStateCupsUpdate();

        } else if (jobStatus
                .getJobStateCupsUpdate() == IppJobStateEnum.IPP_JOB_UNKNOWN) {

            jobStateCups = jobStatus.getJobStateCupsUpdate();

        } else {
            /*
             * Change of status?
             */
            if (jobStatus.getJobStateCupsUpdate() == jobStatus
                    .getJobStateCups()) {
                return false;
            }

            jobStateCups = jobStatus.getJobStateCupsUpdate();
        }
        //
        final PubLevelEnum pubLevel;
        switch (jobStateCups) {
        case IPP_JOB_HELD:
            pubLevel = PubLevelEnum.WARN;
            break;
        case IPP_JOB_ABORTED:
        case IPP_JOB_CANCELED:
            pubLevel = PubLevelEnum.WARN;
            break;
        case IPP_JOB_UNKNOWN:
        case IPP_JOB_STOPPED:
            pubLevel = PubLevelEnum.ERROR;
            break;
        default:
            pubLevel = PubLevelEnum.INFO;
            break;
        }

        final StringBuilder msg = new StringBuilder();

        msg.append("CUPS job #").append(jobStatus.getJobId()).append(" \"")
                .append(StringUtils.defaultString(jobStatus.getJobName()))
                .append("\" on printer ").append(jobStatus.getPrinterName())
                .append(" is ").append(jobStateCups.asLogText()).append(".");

        AdminPublisher.instance().publish(PubTopicEnum.CUPS, pubLevel,
                msg.toString());

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(msg.toString());
        }

        jobStatus.setJobStateCups(jobStateCups);

        final String userid =
                printOut.getDocOut().getDocLog().getUser().getUserId();

        this.updatePrintOutStatus(printOut.getId(), jobStateCups,
                jobStatus.getCupsCompletedTime());

        this.evaluatePrintOutUserMsg(userid, jobStatus.getCupsCompletedTime());

        return !jobStatus.getJobStateCups().isPresentOnQueue();
    }

    /**
     * Processes print job status instances from {@link #jobStatusMap}.
     */
    private void processJobStatusMap() {

        final long timeNow = System.currentTimeMillis();

        final long pullHeatbeat = ConfigManager.instance().getConfigLong(
                IConfigProp.Key.CUPS_NOTIFIER_JOB_STATUS_PULL_HEARTBEAT_MSEC);

        final Iterator<Integer> iter = this.jobStatusMap.keySet().iterator();

        while (iter.hasNext() && this.keepProcessing) {

            final PrintJobStatus jobIter = this.jobStatusMap.get(iter.next());

            final boolean removeJobIter;

            if (jobIter.getCupsCreationTime() == null) {
                /*
                 * INVARIANT: CUPS creation time MUST be present. It must be set
                 * when the job was added by either CUPS or PRINT_OUT (whoever
                 * is first).
                 */
                LOGGER.error(String.format(
                        "Removed CUPS Job [%d]. Reason: no creation time.",
                        jobIter.jobId.intValue()));

                removeJobIter = true;

            } else if (jobIter.jobStatePrintOut == null) {
                /*
                 * INVARIANT: A PRINT_OUT event MUST be received within
                 * reasonable time.
                 */
                final long msecAge = timeNow - jobIter.getCupsCreationTime()
                        * DateUtil.DURATION_MSEC_SECOND;

                final boolean orphanedPrint =
                        msecAge > TIMEOUT_CUPS_PRINTOUT_MATCH_MSEC;

                if (!orphanedPrint) {
                    // Let it stay.
                    continue;
                }
                /*
                 * Wait for PRINT_OUT message has expired: this is probably an
                 * external print action (from outside SavaPage).
                 */
                final StringBuilder msg = new StringBuilder();

                msg.append("External CUPS job #").append(jobIter.getJobId())
                        .append(" \"")
                        .append(StringUtils.defaultString(jobIter.getJobName()))
                        .append("\" on printer ")
                        .append(jobIter.getPrinterName()).append(" is ");

                final IppJobStateEnum state;

                if (jobIter.getJobStateCupsUpdate() != null) {
                    state = jobIter.getJobStateCupsUpdate();
                } else {
                    state = jobIter.getJobStateCups();
                }

                msg.append(state.asLogText()).append(".");

                AdminPublisher.instance().publish(PubTopicEnum.CUPS,
                        PubLevelEnum.WARN, msg.toString());

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(msg.toString());
                }

                removeJobIter = true;

            } else {
                /*
                 * INVARIANT: Active PrintOut CUPS job MUST be present in
                 * database.
                 */
                final PrintOut printOut = this.getCupsJobActive(jobIter);

                if (printOut == null) {
                    /*
                     * When CUPS push notification fails, and PaperCut Print
                     * integration is enabled, the job might already have
                     * received an end-state status from PaperCut, and therefore
                     * will not be found as active.
                     *
                     * So, find job that is set end-of-state by PaperCut
                     * monitor.
                     */
                    final PrintOut printOutPaperCut =
                            this.getCupsJobEndOfStatePaperCut(jobIter);

                    if (printOutPaperCut == null) {

                        final StringBuilder msg = new StringBuilder();

                        msg.append("Active CUPS job #")
                                .append(jobIter.getJobId()).append(" \"")
                                .append(StringUtils
                                        .defaultString(jobIter.getJobName()))
                                .append("\" on printer \"")
                                .append(jobIter.getPrinterName())
                                .append("\" not found in Log.");

                        AdminPublisher.instance().publish(PubTopicEnum.CUPS,
                                PubLevelEnum.ERROR, msg.toString());
                        LOGGER.error(msg.toString());

                    } else {
                        LOGGER.warn("CUPS Job #{} {} by PaperCut.",
                                jobIter.getJobId(),
                                IppJobStateEnum
                                        .asEnum(printOutPaperCut
                                                .getCupsJobState())
                                        .asLogText());

                        final String userid = printOutPaperCut.getDocOut()
                                .getDocLog().getUser().getUserId();

                        this.evaluatePrintOutUserMsg(userid,
                                printOutPaperCut.getCupsCompletedTime());
                    }

                    removeJobIter = true;

                } else {
                    // TEST for getEndOfStatePaperCutCupsJob()
                    // removeJobIter = false;
                    removeJobIter =
                            this.processJobStatusEntry(printOut, jobIter);
                }
            }

            /*
             * Remove job from the map?
             */
            if (removeJobIter) {
                iter.remove();
            } else {
                evaluateJobStatusPull(jobIter, timeNow, pullHeatbeat);
            }

        } // end-while iter.
    }

    /**
     * Checks if last status update is too long ago. If {@code true}, then pull
     * job status from CUPS.
     *
     * @param jobStatus
     *            {@link PrintJobStatus}.
     * @param timeNow
     *            Current time in milliseconds.
     * @param pullHeatbeat
     *            Max wait for a CUPS job id notification before <b>pulling</b>
     *            its status from CUPS.
     */
    private void evaluateJobStatusPull(final PrintJobStatus jobStatus,
            final long timeNow, final long pullHeatbeat) {

        if (timeNow - jobStatus.getUpdateTime() < pullHeatbeat) {
            return;
        }

        try {
            final JsonProxyPrintJob cupsJob =
                    PROXY_PRINT_SERVICE.retrievePrintJob(
                            jobStatus.getPrinterName(), jobStatus.getJobId());

            final IppJobStateEnum ippState;
            if (cupsJob == null) {
                ippState = IppJobStateEnum.IPP_JOB_UNKNOWN;
                LOGGER.warn(
                        "Pulling job #{} status from CUPS failed. "
                                + "Using status [{}]",
                        jobStatus.getJobId(),
                        ippState.uiText(Locale.ENGLISH).toUpperCase());
            } else {
                ippState = cupsJob.getIppJobState();
                LOGGER.warn("Pulled job #{} [{}] from CUPS.",
                        jobStatus.getJobId(),
                        ippState.uiText(Locale.ENGLISH).toUpperCase());
            }

            jobStatus.setJobStateCupsUpdate(ippState);
            jobStatus.setUpdateTime(timeNow);

            if (ippState.isFinished()) {
                jobStatus.setCupsCompletedTime(
                        PROXY_PRINT_SERVICE.getCupsSystemTime());
            }

        } catch (IppConnectException e) {
            LOGGER.warn("CUPS job #{} : {}", jobStatus.getJobId(),
                    e.getMessage());
        }
    }

    @Override
    public void run() {

        this.isProcessing = true;

        while (this.keepProcessing) {

            try {
                processJobStatusMap();
            } catch (Exception e) {
                LOGGER.error(e.getMessage());
            }

            try {
                if (this.keepProcessing) {
                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace("wait ...");
                    }
                    Thread.sleep(POLLING_MSEC);

                    ServiceContext.reopen(); // !!!
                }
            } catch (InterruptedException e) {
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info(e.getMessage());
                }
            }

        } // end-while endless loop.

        this.isProcessing = false;
    }

    /**
     * Finds the {@link PrintOut} that is end-of-state, belongs to a print job
     * status notification and PaperCut managed printer.
     *
     * @param printJobStatus
     *            The CUPS {@link PrintJobStatus} notification.
     * @return {@code null} when not found.
     */
    private PrintOut
            getCupsJobEndOfStatePaperCut(final PrintJobStatus printJobStatus) {

        if (PAPERCUT_SERVICE
                .isExtPaperCutPrint(printJobStatus.getPrinterName())) {

            ServiceContext.reopen();

            final PrintOutDao printOutDao =
                    ServiceContext.getDaoContext().getPrintOutDao();

            final PrintOut printOut = printOutDao.findEndOfStateCupsJob(
                    printJobStatus.getPrinterName(), printJobStatus.getJobId());

            /*
             * NOTE: CUPS job id is not unique, so evaluate completed time.
             */
            if (printOut != null && printJobStatus.getCupsCreationTime() != null
                    && printOut.getCupsCompletedTime() != null
                    && printJobStatus.getCupsCreationTime()
                            .compareTo(printOut.getCupsCompletedTime()) <= 0) {
                return printOut;
            }
        }

        return null;
    }

    /**
     * Finds the {@link PrintOut} that is NOT end-of-state, and belongs to a
     * print job status notification.
     * <p>
     * The PrintOut is expected to be present, so when not found, we might have
     * a synchronization problem. I.e. the CUPS notification arrives, before the
     * database commit of the PrintOut is visible from this thread. Therefore,
     * we {@link ServiceContext#reopen()} before doing max. 3 trials (with 2
     * seconds in between).
     * </p>
     * <p>
     * NOTE: When CUPS push notification fails, and a
     * {@link PaperCutPrintMonitorPattern} is active, the {@link PrintOut} might
     * already have received an end-state status from PaperCut, and therefore
     * will not be found as active.
     * </p>
     *
     * @param printJobStatus
     *            The CUPS {@link PrintJobStatus} notification.
     * @return {@code null} when not found.
     */
    private PrintOut getCupsJobActive(final PrintJobStatus printJobStatus) {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Reading Active Job #{} PrintOut from database",
                    printJobStatus.getJobId());
        }

        final int nMaxTrials = 3; // retry 2 times.

        int iTrial = 0;

        while (iTrial < nMaxTrials) {

            ServiceContext.reopen();

            final PrintOutDao printOutDao =
                    ServiceContext.getDaoContext().getPrintOutDao();

            final PrintOut printOut = printOutDao.findActiveCupsJob(
                    printJobStatus.getPrinterName(), printJobStatus.getJobId());

            if (printOut != null) {
                return printOut;
            }

            iTrial++;

            if (iTrial < nMaxTrials) {
                try {
                    Thread.sleep(2 * DateUtil.DURATION_MSEC_SECOND);
                } catch (InterruptedException e) {
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info(e.getMessage());
                    }
                }
            }
        }

        if (LOGGER.isWarnEnabled()) {

            final StringBuilder msg = new StringBuilder();

            msg.append("Active CUPS job #").append(printJobStatus.getJobId());

            msg.append(" [");
            if (printJobStatus.getJobStateCups() == null) {
                msg.append("-");
            } else {
                msg.append(printJobStatus.getJobStateCups().asLogText());
            }
            msg.append("] not found as PrintOut after [").append(iTrial)
                    .append("] trials.");

            LOGGER.warn(msg.toString());
        }

        return null;
    }

    /**
     * Checks if CUPS completed time NEQ {@code null} and NEQ zero (0). If
     * {@code true}, then user PrintOut message via {@link UserMsgIndicator} is
     * written.
     *
     * @param userid
     *            The user id.
     * @param cupsCompletedTime
     *            The CUPS complete time in seconds (can be {@code null} or zero
     *            (0)).
     */
    private void evaluatePrintOutUserMsg(final String userid,
            final Integer cupsCompletedTime) {

        if (userid != null && cupsCompletedTime != null
                && cupsCompletedTime.intValue() != 0) {

            final Date completedDate =
                    new Date(cupsCompletedTime * DateUtil.DURATION_MSEC_SECOND);

            try {
                UserMsgIndicator.write(userid, completedDate,
                        UserMsgIndicator.Msg.PRINT_OUT_COMPLETED, null);
            } catch (IOException e) {
                AdminPublisher.instance().publish(PubTopicEnum.CUPS,
                        PubLevelEnum.ERROR, e.getMessage());
            }
        }
    }

    /**
     * Updates the {@link PrintOut} with CUPS status and completion time.
     *
     * @param printOut
     *            The {@link PrintOut}.
     * @param ippState
     *            The {@link IppJobStateEnum}.
     * @param cupsCompletedTime
     *            The CUPS complete time in seconds (can be {@code null}).
     * @return The user id of the printOut.
     */
    private void updatePrintOutStatus(final Long printOutId,
            final IppJobStateEnum ippState, final Integer cupsCompletedTime) {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("PrintOut ID [%d] update: state [%s]",
                    printOutId, ippState.asLogText()));
        }

        final DaoContext daoContext = ServiceContext.getDaoContext();

        boolean rollback = false;

        try {

            daoContext.beginTransaction();

            rollback = true;

            daoContext.getPrintOutDao().updateCupsJob(printOutId, ippState,
                    cupsCompletedTime);

            daoContext.commit();
            rollback = false;

        } finally {
            if (rollback) {
                daoContext.rollback();
            }
        }
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

        SpInfo.instance().log(String.format("... %s shutdown completed.",
                OBJECT_NAME_FOR_LOG));

    }

}
