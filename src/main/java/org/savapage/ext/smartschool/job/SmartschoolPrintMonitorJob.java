/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2015 Datraverse B.V.
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
package org.savapage.ext.smartschool.job;

import java.math.BigDecimal;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.Locale;

import javax.mail.MessagingException;
import javax.xml.soap.SOAPException;

import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.UnableToInterruptJobException;
import org.savapage.core.ShutdownException;
import org.savapage.core.SpException;
import org.savapage.core.circuitbreaker.CircuitBreaker;
import org.savapage.core.circuitbreaker.CircuitBreakerException;
import org.savapage.core.circuitbreaker.CircuitBreakerOperation;
import org.savapage.core.circuitbreaker.CircuitDamagingException;
import org.savapage.core.circuitbreaker.CircuitNonTrippingException;
import org.savapage.core.circuitbreaker.CircuitTrippingException;
import org.savapage.core.cometd.AdminPublisher;
import org.savapage.core.cometd.PubLevelEnum;
import org.savapage.core.cometd.PubTopicEnum;
import org.savapage.core.community.CommunityDictEnum;
import org.savapage.core.community.MemberCard;
import org.savapage.core.config.CircuitBreakerEnum;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.job.AbstractJob;
import org.savapage.core.job.SpJobScheduler;
import org.savapage.core.util.AppLogHelper;
import org.savapage.core.util.BigDecimalUtil;
import org.savapage.core.util.DateUtil;
import org.savapage.ext.ExtSupplierConnectException;
import org.savapage.ext.ExtSupplierException;
import org.savapage.ext.papercut.PaperCutConnectException;
import org.savapage.ext.papercut.PaperCutDbProxy;
import org.savapage.ext.papercut.PaperCutServerProxy;
import org.savapage.ext.smartschool.SmartschoolPrintMonitor;
import org.savapage.ext.smartschool.SmartschoolPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Datraverse B.V.
 *
 */
public final class SmartschoolPrintMonitorJob extends AbstractJob {

    /**
     * The logger.
     */
    private static final Logger LOGGER = LoggerFactory
            .getLogger(SmartschoolPrintMonitorJob.class);

    /**
     * Number of seconds after restarting this job after an exception occurs.
     */
    private static final int RESTART_SECS_AFTER_EXCEPTION = 60;

    /**
     * .
     */
    public static final String ATTR_SIMULATION = "simulation";

    /**
     * {@code true} when running in simulation mode.
     */
    private boolean isSimulation = false;

    /**
     * The {@link CircuitBreaker}.
     */
    private CircuitBreaker breaker;

    /**
     * .
     */
    private SmartSchoolCircuitOperation circuitOperation = null;

    /**
     * Milliseconds to wait before starting this job again.
     */
    private long millisUntilNextInvocation;

    /**
     *
     * @author Datraverse B.V.
     *
     */
    private static class SmartSchoolCircuitOperation implements
            CircuitBreakerOperation {

        /**
         * .
         */
        private final SmartschoolPrintMonitorJob parentJob;

        /**
         * .
         */
        private SmartschoolPrintMonitor printMonitor = null;

        /**
         *
         * @param parent
         *            The parent {@link SmartschoolPrintMonitorJob}.
         */
        public SmartSchoolCircuitOperation(
                final SmartschoolPrintMonitorJob parent) {
            this.parentJob = parent;
        }

        @Override
        public Object execute(final CircuitBreaker circuitBreaker) {

            PaperCutServerProxy papercutServerProxy = null;
            PaperCutDbProxy papercutDbProxy = null;

            try {

                final ConfigManager cm = ConfigManager.instance();

                if (cm.isConfigValue(Key.SMARTSCHOOL_PAPERCUT_ENABLE)) {

                    papercutServerProxy = PaperCutServerProxy.create(cm, true);
                    papercutDbProxy = PaperCutDbProxy.create(cm, true);

                    papercutServerProxy.connect();
                    papercutDbProxy.connect();
                }

                //
                final int sessionDurationSecs =
                        cm.getConfigInt(IConfigProp.Key.SMARTSCHOOL_SOAP_PRINT_POLL_SESSION_DURATION_SECS);

                /*
                 *
                 */
                this.printMonitor =
                        new SmartschoolPrintMonitor(papercutServerProxy,
                                papercutDbProxy, this.parentJob.isSimulation);

                this.printMonitor.connect();

                final String msgKey;

                if (this.parentJob.isSimulation) {
                    msgKey = "SmartschoolMonitor.started.simulation";
                } else {
                    msgKey = "SmartschoolMonitor.started";
                }

                AdminPublisher.instance().publish(PubTopicEnum.SMARTSCHOOL,
                        PubLevelEnum.INFO,
                        this.parentJob.localizeSysMsg(msgKey));

                /*
                 * At this point we can inform the breaker we are up and
                 * running.
                 */
                circuitBreaker.closeCircuit();

                /*
                 * Blocking...
                 */
                this.printMonitor.monitor(sessionDurationSecs);

            } catch (ExtSupplierConnectException | SOAPException
                    | PaperCutConnectException e) {

                throw new CircuitTrippingException(e);

            } catch (InterruptedException | ShutdownException e) {

                throw new CircuitNonTrippingException(e);

            } catch (CircuitDamagingException e) {

                throw e;

            } catch (ExtSupplierException t) {

                throw new CircuitDamagingException(t);

            } catch (Exception t) {

                if (t instanceof UnknownHostException) {
                    throw new CircuitTrippingException(t);
                }

                throw new CircuitDamagingException(t);

            } finally {

                if (this.printMonitor != null) {
                    try {
                        this.printMonitor.disconnect();
                    } catch (Exception e) {
                        LOGGER.error(e.getMessage(), e);
                    }
                }

                if (papercutDbProxy != null) {
                    papercutDbProxy.disconnect();
                }

                if (papercutServerProxy != null) {
                    papercutServerProxy.disconnect();
                }

            }
            return null;
        }

        /**
         * @throws InterruptedException
         * @throws MessagingException
         *
         */
        public void onInterrupt() throws SOAPException, InterruptedException {
            if (this.printMonitor != null) {
                this.printMonitor.interrupt();
            }
        }
    }

    @Override
    protected void onInit(final JobExecutionContext ctx) {
        this.breaker =
                ConfigManager
                        .getCircuitBreaker(CircuitBreakerEnum.SMARTSCHOOL_CONNECTION);
    }

    @Override
    protected void onInterrupt() throws UnableToInterruptJobException {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Interrupted.");
        }

        if (this.circuitOperation != null) {
            try {
                this.circuitOperation.onInterrupt();
            } catch (Exception e) {
                LOGGER.error(e.getMessage());
            }
        }
    }

    @Override
    protected void onExecute(final JobExecutionContext ctx)
            throws JobExecutionException {

        SmartschoolPrinter.setBlocked(MemberCard.instance()
                .isMembershipDesirable());

        if (SmartschoolPrinter.isBlocked() || !SmartschoolPrinter.isOnline()) {
            return;
        }

        final JobDataMap map = ctx.getJobDetail().getJobDataMap();

        if (map.containsKey(ATTR_SIMULATION)) {
            isSimulation = map.getBoolean(ATTR_SIMULATION);
        } else {
            isSimulation = false;
        }

        try {
            this.circuitOperation = new SmartSchoolCircuitOperation(this);

            this.breaker.execute(this.circuitOperation);

            this.millisUntilNextInvocation = 1 * DateUtil.DURATION_MSEC_SECOND;

        } catch (CircuitBreakerException t) {

            this.millisUntilNextInvocation = this.breaker.getMillisUntilRetry();

        } catch (Exception t) {

            this.millisUntilNextInvocation =
                    RESTART_SECS_AFTER_EXCEPTION
                            * DateUtil.DURATION_MSEC_SECOND;

            AdminPublisher.instance().publish(PubTopicEnum.SMARTSCHOOL,
                    PubLevelEnum.ERROR,
                    localizeSysMsg("SmartschoolMonitor.error", t.getMessage()));

            LOGGER.error(t.getMessage(), t);
        }
    }

    @Override
    protected void onExit(final JobExecutionContext ctx) {

        if (SmartschoolPrinter.isBlocked()) {
            final String error =
                    AppLogHelper.logError(this.getClass(),
                            "SmartschoolMonitor.membership.error",
                            CommunityDictEnum.SAVAPAGE.getWord(),
                            CommunityDictEnum.MEMBERSHIP.getWord());
            AdminPublisher.instance().publish(PubTopicEnum.SMARTSCHOOL,
                    PubLevelEnum.ERROR, error);

            SmartschoolPrinter.setOnline(false);

            return;
        }

        final String msgKeyStopped;
        final String msgKeyRestart;

        if (this.isSimulation) {
            msgKeyStopped = "SmartschoolMonitor.stopped.simulation";
            msgKeyRestart = "SmartschoolMonitor.restart.simulation";
        } else {
            msgKeyStopped = "SmartschoolMonitor.stopped";
            msgKeyRestart = "SmartschoolMonitor.restart";
        }

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(localizeLogMsg(msgKeyStopped));
        }

        final AdminPublisher publisher = AdminPublisher.instance();

        if (this.isInterrupted() || !SmartschoolPrinter.isOnline()
                || !ConfigManager.isSmartSchoolPrintActiveAndEnabled()) {

            publisher.publish(PubTopicEnum.SMARTSCHOOL, PubLevelEnum.INFO,
                    localizeSysMsg(msgKeyStopped));

        } else if (this.breaker.isCircuitDamaged()) {

            publisher.publish(PubTopicEnum.SMARTSCHOOL, PubLevelEnum.ERROR,
                    localizeSysMsg(msgKeyStopped));

        } else {

            final PubLevelEnum pubLevel;
            final String pubMsg;

            if (this.breaker.isCircuitClosed()) {
                pubLevel = PubLevelEnum.INFO;
            } else {
                pubLevel = PubLevelEnum.WARN;
                this.millisUntilNextInvocation =
                        this.breaker.getMillisUntilRetry();
            }

            if (this.millisUntilNextInvocation > DateUtil.DURATION_MSEC_SECOND) {

                try {

                    final double seconds =
                            (double) this.millisUntilNextInvocation
                                    / DateUtil.DURATION_MSEC_SECOND;

                    pubMsg =
                            localizeSysMsg(
                                    msgKeyRestart,
                                    BigDecimalUtil.localize(
                                            BigDecimal.valueOf(seconds),
                                            Locale.getDefault(), false));
                } catch (ParseException e) {
                    throw new SpException(e.getMessage());
                }

            } else {
                pubMsg = localizeSysMsg(msgKeyStopped);
            }

            publisher.publish(PubTopicEnum.SMARTSCHOOL, pubLevel, pubMsg);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Starting again after ["
                        + this.millisUntilNextInvocation + "] milliseconds");
            }

            SpJobScheduler.instance().scheduleOneShotSmartSchoolPrintMonitor(
                    isSimulation, this.millisUntilNextInvocation);
        }
    }

}