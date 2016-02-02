/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2016 Datraverse B.V.
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
package org.savapage.ext.papercut.job;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.Locale;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.UnableToInterruptJobException;
import org.savapage.core.SpException;
import org.savapage.core.circuitbreaker.CircuitBreaker;
import org.savapage.core.cometd.AdminPublisher;
import org.savapage.core.cometd.PubLevelEnum;
import org.savapage.core.cometd.PubTopicEnum;
import org.savapage.core.config.CircuitBreakerEnum;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.dao.enums.ExternalSupplierEnum;
import org.savapage.core.dao.enums.ExternalSupplierStatusEnum;
import org.savapage.core.job.AbstractJob;
import org.savapage.core.job.SpJobScheduler;
import org.savapage.core.jpa.Account;
import org.savapage.core.jpa.DocLog;
import org.savapage.core.util.BigDecimalUtil;
import org.savapage.core.util.DateUtil;
import org.savapage.ext.ExtSupplierConnectException;
import org.savapage.ext.ExtSupplierException;
import org.savapage.ext.papercut.PaperCutDbProxy;
import org.savapage.ext.papercut.PaperCutException;
import org.savapage.ext.papercut.PaperCutPrintJobListener;
import org.savapage.ext.papercut.PaperCutPrintMonitorPattern;
import org.savapage.ext.papercut.PaperCutPrinterUsageLog;
import org.savapage.ext.papercut.PaperCutServerProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class PaperCutPrintMonitorJob extends AbstractJob implements
        PaperCutPrintJobListener {

    /**
     * The logger.
     */
    private static final Logger LOGGER = LoggerFactory
            .getLogger(PaperCutPrintMonitorJob.class);

    /**
     * Number of seconds after restarting this job after an exception occurs.
     */
    private static final int RESTART_SECS_AFTER_EXCEPTION = 60;

    /**
     * .
     */
    private static final long MAX_MONITOR_MSEC = DateUtil.DURATION_MSEC_HOUR;

    /**
     * .
     */
    private static final long MSECS_WAIT_BETWEEN_POLLS = 30000;

    /**
     * Milliseconds to wait before starting this job again.
     */
    private long millisUntilNextInvocation;

    /**
     * The {@link CircuitBreaker}.
     */
    private CircuitBreaker breaker;

    /**
     * .
     */
    private final class PaperCutPrintMonitor extends
            PaperCutPrintMonitorPattern {

        /**
         * INVARIANT: PaperCut is configured with Multiple Personal Accounts,
         * and this is the account name to use.
         */
        private static final String PAPERCUT_USER_ACCOUNT_NAME = "SavaPage";

        /**
         * The name of the parent {@link Account} for all child "group"
         * accounts.
         */
        private static final String SHARED_PARENT_ACCOUNT_NAME = "SavaPage";

        /**
         * The name of the child {@link Account} for all SavaPage Jobs.
         */
        private static final String SHARED_ACCOUNT_JOBS = "Jobs";

        /**
         *
         * @param papercutServerProxy
         *            The {@link PaperCutServerProxy}.
         * @param papercutDbProxy
         *            The {@link PaperCutDbProxy}.
         * @param statusListener
         *            The {@link PaperCutPrintJobListener}.
         */
        protected PaperCutPrintMonitor(
                final PaperCutServerProxy papercutServerProxy,
                final PaperCutDbProxy papercutDbProxy,
                final PaperCutPrintJobListener statusListener) {

            super(ExternalSupplierEnum.SAVAPAGE, papercutServerProxy,
                    papercutDbProxy, statusListener);
        }

        @Override
        protected String getUserAccountName() {
            return PAPERCUT_USER_ACCOUNT_NAME;
        }

        @Override
        protected String getSharedParentAccountName() {
            return SHARED_PARENT_ACCOUNT_NAME;
        }

        @Override
        protected String getSharedJobsAccountName() {
            return SHARED_ACCOUNT_JOBS;
        }

        @Override
        protected int getAccountTrxWeightTotal(final DocLog docLogOut,
                final DocLog docLogIn) {
            return docLogOut.getDocOut().getPrintOut().getNumberOfCopies();
        }

        @Override
        protected String getKlasFromAccountName(final String accountName) {
            /*
             * The account name is NOT composed, but is the plain (sub) account.
             */
            return accountName;
        }

        @Override
        protected Logger getLogger() {
            return LOGGER;
        }

        @Override
        protected boolean isDocInAccountTrx() {
            return false;
        }
    }

    @Override
    protected void onInterrupt() throws UnableToInterruptJobException {
        LOGGER.debug("Interrupted.");
    }

    @Override
    protected void onInit(final JobExecutionContext ctx) {

        this.breaker =
                ConfigManager
                        .getCircuitBreaker(CircuitBreakerEnum.PAPERCUT_CONNECTION);

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(localizeLogMsg("PaperCutPrintMonitor.started"));
        }

        AdminPublisher.instance().publish(PubTopicEnum.PAPERCUT,
                PubLevelEnum.INFO,
                localizeSysMsg("PaperCutPrintMonitor.started"));
    }

    @Override
    protected void onExecute(final JobExecutionContext ctx)
            throws JobExecutionException {

        PaperCutServerProxy papercutServerProxy = null;
        PaperCutDbProxy papercutDbProxy = null;

        try {

            final ConfigManager cm = ConfigManager.instance();

            papercutServerProxy = PaperCutServerProxy.create(cm, true);
            papercutDbProxy = PaperCutDbProxy.create(cm, true);

            papercutServerProxy.connect();
            papercutDbProxy.connect();

            final PaperCutPrintMonitorPattern monitor =
                    new PaperCutPrintMonitor(papercutServerProxy,
                            papercutDbProxy, this);

            //
            this.monitorPaperCut(monitor);
            //

            this.millisUntilNextInvocation = 1 * DateUtil.DURATION_MSEC_SECOND;

        } catch (ExtSupplierConnectException e) {
            /*
             * Assuming this exception was caused by PaperCut connection
             * circuit.
             */
            this.millisUntilNextInvocation = this.breaker.getMillisUntilRetry();

        } catch (Exception t) {

            this.millisUntilNextInvocation =
                    RESTART_SECS_AFTER_EXCEPTION
                            * DateUtil.DURATION_MSEC_SECOND;

            AdminPublisher.instance()
                    .publish(
                            PubTopicEnum.PAPERCUT,
                            PubLevelEnum.ERROR,
                            localizeSysMsg("PaperCutPrintMonitor.error",
                                    t.getMessage()));

            LOGGER.error(t.getMessage(), t);

        } finally {

            if (papercutDbProxy != null) {
                papercutDbProxy.disconnect();
            }

            if (papercutServerProxy != null) {
                papercutServerProxy.disconnect();
            }
        }
    }

    @Override
    protected void onExit(final JobExecutionContext ctx) {

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(localizeLogMsg("PaperCutPrintMonitor.stopped"));
        }

        final AdminPublisher publisher = AdminPublisher.instance();

        if (this.isInterrupted() || !ConfigManager.isPaperCutPrintEnabled()) {

            publisher.publish(PubTopicEnum.PAPERCUT, PubLevelEnum.INFO,
                    localizeSysMsg("PaperCutPrintMonitor.stopped"));
            return;
        }

        if (this.breaker.isCircuitDamaged()) {

            publisher.publish(PubTopicEnum.PAPERCUT, PubLevelEnum.ERROR,
                    localizeSysMsg("PaperCutPrintMonitor.stopped"));
            return;
        }

        final PubLevelEnum pubLevel;
        final String pubMsg;

        if (this.breaker.isCircuitClosed()) {
            pubLevel = PubLevelEnum.INFO;
        } else {
            pubLevel = PubLevelEnum.WARN;
            this.millisUntilNextInvocation = this.breaker.getMillisUntilRetry();
        }

        if (this.millisUntilNextInvocation > DateUtil.DURATION_MSEC_SECOND) {

            try {

                final double seconds =
                        (double) this.millisUntilNextInvocation
                                / DateUtil.DURATION_MSEC_SECOND;

                pubMsg =
                        localizeSysMsg(
                                "PaperCutPrintMonitor.restart",
                                BigDecimalUtil.localize(
                                        BigDecimal.valueOf(seconds),
                                        Locale.getDefault(), false));
            } catch (ParseException e) {
                throw new SpException(e.getMessage());
            }

        } else {
            pubMsg = localizeSysMsg("PaperCutPrintMonitor.stopped");
        }

        publisher.publish(PubTopicEnum.PAPERCUT, pubLevel, pubMsg);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Starting again after ["
                    + this.millisUntilNextInvocation + "] milliseconds");
        }

        SpJobScheduler.instance().scheduleOneShotPaperCutPrintMonitor(
                this.millisUntilNextInvocation);

    }

    /**
     * Monitors PaperCut for print job status.
     *
     * @param monitor
     *            The {@link PaperCutPrintMonitorPattern}.
     * @throws PaperCutException
     *             When PaperCut returns an error.
     * @throws ExtSupplierException
     *             When external supplier returns an error.
     * @throws ExtSupplierConnectException
     *             When error connecting to external supplier.
     */
    private void monitorPaperCut(final PaperCutPrintMonitorPattern monitor)
            throws PaperCutException, ExtSupplierException,
            ExtSupplierConnectException {

        final long msecStart = System.currentTimeMillis();

        int i = 0;

        while (!this.isInterrupted()) {

            try {
                Thread.sleep(MSECS_WAIT_BETWEEN_POLLS);
            } catch (InterruptedException e) {
                break;
            }

            if (this.isInterrupted()) {
                break;
            }

            i++;

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(String.format("PaperCut Print Job poll [%d]", i));
            }

            /*
             * Process.
             */
            monitor.process();

            /*
             * STOP if the max monitor time has elapsed.
             */
            final long timeElapsed =
                    System.currentTimeMillis() + MSECS_WAIT_BETWEEN_POLLS
                            - msecStart;

            if (timeElapsed >= MAX_MONITOR_MSEC) {

                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("PaperCut Print Job poll: time elapsed.");
                }

                break;
            }

        } // end-for
    }

    @Override
    public void onPaperCutPrintJobProcessed(final DocLog docLogOut,
            final PaperCutPrinterUsageLog papercutLog,
            final ExternalSupplierStatusEnum printStatus,
            final boolean isDocumentTooLarge) throws ExtSupplierException,
            ExtSupplierConnectException {

        final PubLevelEnum pubLevel;
        final StringBuilder msg = new StringBuilder();

        msg.append("PaperCut print of SavaPage document [")
                .append(papercutLog.getDocumentName()).append("] ")
                .append(printStatus.toString());

        if (printStatus == ExternalSupplierStatusEnum.COMPLETED) {
            pubLevel = PubLevelEnum.CLEAR;
        } else {
            pubLevel = PubLevelEnum.WARN;
            msg.append(" because \"").append(papercutLog.getDeniedReason())
                    .append("\"");
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(msg.toString());
        }

        AdminPublisher.instance().publish(PubTopicEnum.PAPERCUT, pubLevel,
                msg.toString());
    }

    @Override
    public void onPaperCutPrintJobNotFound(final String docName,
            final long docAge) {

        final StringBuilder msg = new StringBuilder();

        msg.append("PaperCut print log of ")
                .append(DateUtil.formatDuration(docAge))
                .append(" old SavaPage document [").append(docName)
                .append("] not found.");

        LOGGER.error(msg.toString());

        AdminPublisher.instance().publish(PubTopicEnum.PAPERCUT,
                PubLevelEnum.ERROR, msg.toString());
    }
}
