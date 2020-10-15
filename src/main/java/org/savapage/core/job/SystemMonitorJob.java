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
package org.savapage.core.job;

import java.lang.management.ThreadInfo;
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.Locale;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.UnableToInterruptJobException;
import org.savapage.core.SpException;
import org.savapage.core.cometd.AdminPublisher;
import org.savapage.core.cometd.PubLevelEnum;
import org.savapage.core.cometd.PubTopicEnum;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp;
import org.savapage.core.util.BigDecimalUtil;
import org.savapage.core.util.DateUtil;
import org.savapage.core.util.DeadlockedThreadsDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class SystemMonitorJob extends AbstractJob {

    /**
     * The logger.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(SystemMonitorJob.class);

    /**
     * Number of seconds after restarting this job after an exception occurs.
     */
    private static final int RESTART_SECS_AFTER_EXCEPTION = 60;

    /** */
    private static final long MAX_MONITOR_MSEC = DateUtil.DURATION_MSEC_HOUR;

    /** */
    private static final long MONITOR_HEARTBEAT_MSEC =
            5 * DateUtil.DURATION_MSEC_SECOND;

    /**
     * Milliseconds to wait before starting this job again.
     */
    private long millisUntilNextInvocation;

    @Override
    protected void onInterrupt() throws UnableToInterruptJobException {
        LOGGER.debug("Interrupted.");
    }

    @Override
    protected void onInit(final JobExecutionContext ctx) {

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(localizeLogMsg("SystemMonitor.started"));
        }

        AdminPublisher.instance().publish(PubTopicEnum.SYSTEM,
                PubLevelEnum.INFO, localizeSysMsg("SystemMonitor.started"));
    }

    @Override
    protected void onExecute(final JobExecutionContext ctx)
            throws JobExecutionException {

        try {

            this.pollErrors();

            this.millisUntilNextInvocation = 1 * DateUtil.DURATION_MSEC_SECOND;

        } catch (Exception t) {

            this.millisUntilNextInvocation = RESTART_SECS_AFTER_EXCEPTION
                    * DateUtil.DURATION_MSEC_SECOND;

            AdminPublisher.instance().publish(PubTopicEnum.SYSTEM,
                    PubLevelEnum.ERROR,
                    localizeSysMsg("SystemMonitor.error", t.getMessage()));

            LOGGER.error(t.getMessage(), t);
        }
    }

    @Override
    protected void onExit(final JobExecutionContext ctx) {

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(localizeLogMsg("SystemMonitor.stopped"));
        }

        final AdminPublisher publisher = AdminPublisher.instance();

        if (this.isInterrupted()) {

            publisher.publish(PubTopicEnum.SYSTEM, PubLevelEnum.INFO,
                    localizeSysMsg("SystemMonitor.stopped"));

        } else {

            final PubLevelEnum pubLevel = PubLevelEnum.INFO;
            final String pubMsg;

            if (this.millisUntilNextInvocation > DateUtil.DURATION_MSEC_SECOND) {

                try {

                    final double seconds =
                            (double) this.millisUntilNextInvocation
                                    / DateUtil.DURATION_MSEC_SECOND;

                    pubMsg = localizeSysMsg("SystemMonitor.restart",
                            BigDecimalUtil.localize(BigDecimal.valueOf(seconds),
                                    Locale.getDefault(), false));

                } catch (ParseException e) {
                    throw new SpException(e.getMessage());
                }

            } else {
                pubMsg = localizeSysMsg("SystemMonitor.stopped");
            }

            publisher.publish(PubTopicEnum.SYSTEM, pubLevel, pubMsg);

            LOGGER.debug("Starting again after [{}] milliseconds",
                    this.millisUntilNextInvocation);

            SpJobScheduler.instance().scheduleOneShotSystemMonitor(
                    this.millisUntilNextInvocation);
        }
    }

    /**
     * Poll runtime state for errors.
     */
    private void pollErrors() {

        final long msecStart = System.currentTimeMillis();

        final long msecTrigger = ConfigManager.instance()
                .getConfigInt(IConfigProp.Key.SYS_MONITOR_HEARTBEAT_SEC)
                * DateUtil.DURATION_MSEC_SECOND;

        /*
         * Use a small heartbeat to be responsive to interrupts.
         */
        long msecHeartbeat = MONITOR_HEARTBEAT_MSEC;

        if (msecTrigger < msecHeartbeat) {
            msecHeartbeat = msecTrigger;
        }

        final long iTrigger = msecTrigger / msecHeartbeat;

        int i = 0;

        while (!this.isInterrupted()) {

            try {
                Thread.sleep(msecHeartbeat);
            } catch (InterruptedException e) {
                break;
            }

            if (this.isInterrupted()) {
                break;
            }

            if (++i < iTrigger) {
                continue;
            }

            i = 0;

            findDeadlockedThreads();

            // STOP if the max monitor time has elapsed.
            final long timeElapsed =
                    System.currentTimeMillis() + msecHeartbeat - msecStart;

            if (timeElapsed >= MAX_MONITOR_MSEC) {
                break;
            }

        } // end-while
    }

    /**
     * Monitors and reports deadlocked threads.
     */
    private void findDeadlockedThreads() {

        final ThreadInfo[] infos =
                DeadlockedThreadsDetector.findDeadlockedThreads();

        if (infos == null) {
            return;
        }

        final int nThreads = infos.length;
        final String msg =
                String.format("Deadlocked threads detected (%d)", nThreads);

        LOGGER.warn(msg);

        AdminPublisher.instance().publish(PubTopicEnum.SYSTEM,
                PubLevelEnum.ERROR, msg);

        int i = 0;

        for (final ThreadInfo ti : infos) {
            LOGGER.warn("Deadlocked Thread {}/{} {}", ++i, nThreads,
                    DeadlockedThreadsDetector.toString(ti));
        }
    }

}
