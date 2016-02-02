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
package org.savapage.core.job;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.quartz.CronTrigger;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.simpl.PropertySettingJobFactory;
import org.savapage.core.SpException;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp;
import org.savapage.core.print.gcp.GcpPrinter;
import org.savapage.core.print.imap.ImapPrinter;
import org.savapage.core.util.DateUtil;
import org.savapage.ext.smartschool.SmartschoolPrinter;
import org.savapage.ext.smartschool.job.SmartschoolPrintMonitorJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Datraverse B.V.
 */
public class SpJobScheduler {

    public static final String JOB_GROUP_SCHEDULED = "DEFAULT";
    public static final String JOB_GROUP_ONESHOT = "ONESHOT";

    /**
     * Used for testing, not for production.
     */
    private static final String JOB_GROUP_TEST = "TEST";

    private final List<JobDetail> myHourlyJobs = new ArrayList<>();
    private final List<JobDetail> myDailyJobs = new ArrayList<>();
    private final List<JobDetail> myWeeklyJobs = new ArrayList<>();
    private final List<JobDetail> myMonthlyJobs = new ArrayList<>();
    private final List<JobDetail> myDailyMaintJobs = new ArrayList<>();

    /**
     * The SingletonHolder is loaded on the first execution of
     * {@link SpJobScheduler#instance()} or the first access to
     * {@link SingletonHolder#INSTANCE}, not before.
     * <p>
     * <a href=
     * "http://en.wikipedia.org/wiki/Singleton_pattern#The_solution_of_Bill_Pugh"
     * >The Singleton solution of Bill Pugh</a>
     * </p>
     */
    private static class SingletonHolder {
        public static final SpJobScheduler INSTANCE = new SpJobScheduler();
    }

    /**
     *
     */
    private static final Logger LOGGER = LoggerFactory
            .getLogger(SpJobScheduler.class);

    /**
     * My Quartz job scheduler.
     */
    private Scheduler myScheduler = null;

    /**
     *
     */
    private SpJobScheduler() {

    }

    /**
     * Gets the singleton instance.
     *
     * @return
     */
    public static SpJobScheduler instance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     * Initializes the instance, and schedules one-shot jobs at the start of the
     * application.
     * <p>
     * IMPORTANT: the one-shot {@link CupsSyncPrintJobs} job is NOT started,
     * because we are not sure if CUPS is fully initialized at this point.
     * </p>
     */
    public void init() {

        System.setProperty("org.terracotta.quartz.skipUpdateCheck", "true");

        try {
            PropertySettingJobFactory jobFactory =
                    new PropertySettingJobFactory();
            jobFactory.setWarnIfPropertyNotFound(false);

            myScheduler = StdSchedulerFactory.getDefaultScheduler();
            myScheduler.setJobFactory(jobFactory);

            myScheduler.start();

            initJobDetails();

            LOGGER.debug("initialized");

            startJobs();

            /*
             * Clean the App Log and Doc Log, and remove PrinterGroup that do
             * not have members.
             */
            scheduleOneShotJob(SpJobType.APP_LOG_CLEAN, 1L);
            scheduleOneShotJob(SpJobType.DOC_LOG_CLEAN, 1L);
            scheduleOneShotJob(SpJobType.PRINTER_GROUP_CLEAN, 1L);

            /*
             * Monitor email outbox unconditionally.
             */
            scheduleOneShotEmailOutboxMonitor(1L);

            /*
             * PaperCut Print Monitoring enabled?
             */
            if (ConfigManager.isPaperCutPrintEnabled()) {
                scheduleOneShotPaperCutPrintMonitor(1L);
            }

            /*
             * Mail Print enabled?
             */
            if (ConfigManager.isPrintImapEnabled()) {
                scheduleOneShotImapListener(1L);
            }

            /*
             * SmartSchool active and enabled?
             */
            if (ConfigManager.isSmartSchoolPrintActiveAndEnabled()) {
                scheduleOneShotSmartSchoolPrintMonitor(false, 1L);
            }

            /*
             * Google Cloud Print enabled AND configured?
             */
            if (ConfigManager.isGcpEnabled() && GcpPrinter.isConfigured()) {
                scheduleOneShotGcpListener(1L);
            }

        } catch (SchedulerException e) {
            throw new SpException(e.getMessage(), e);
        }
    }

    /**
     * Tells quartz to (re)schedule the jobs belonging to the configKey.
     *
     * @param configKey
     */
    public void scheduleJobs(final IConfigProp.Key configKey) {

        switch (configKey) {
        case SCHEDULE_HOURLY:
            scheduleJobs(myHourlyJobs, configKey);
            break;
        case SCHEDULE_DAILY:
            scheduleJobs(myDailyJobs, configKey);
            break;
        case SCHEDULE_WEEKLY:
            scheduleJobs(myWeeklyJobs, configKey);
            break;
        case SCHEDULE_MONTHLY:
            scheduleJobs(myMonthlyJobs, configKey);
            break;
        case SCHEDULE_DAILY_MAINT:
            scheduleJobs(myDailyMaintJobs, configKey);
            break;
        default:
            throw new SpException("no handler for configuration key ["
                    + configKey + "]");
        }
    }

    /**
     *
     */
    public void shutdown() {

        if (myScheduler != null) {

            LOGGER.debug("shutting down the scheduler...");

            try {

                /*
                 * Wait for EmailOutputMonitor to finish...
                 */
                interruptEmailOutputMonitor();

                /*
                 * Wait for ImapListener to finish...
                 */
                interruptImapListener();

                /*
                 * Wait for GcpListener to finish...
                 */
                interruptGcpListener();

                /*
                 * Wait for PaperCut Prit Monitor to finish...
                 */
                interruptPaperCutPrintMonitor();

                /*
                 * Wait for SmartSchoolListener to finish...
                 */
                interruptSmartSchoolPoller();

                /*
                 * The 'true' parameters makes the shutdown call block till all
                 * jobs are finished.
                 */
                myScheduler.shutdown(true);

            } catch (SchedulerException e) {
                LOGGER.error(e.getMessage());
            }

            myScheduler = null;
            LOGGER.debug("shutdown");
        }
    }

    /**
     * Creates a new Job instance.
     *
     * @param jobType
     *            The type of job.
     * @param group
     *            The name of the group.
     * @return
     */
    private JobDetail createJob(final SpJobType jobType, final String group) {

        String name = jobType.toString();

        Class<? extends Job> jobClass = null;
        JobDataMap data = null;

        switch (jobType) {

        case CUPS_SUBS_RENEW:
            jobClass = org.savapage.core.job.CupsSubsRenew.class;
            break;

        case CUPS_SYNC_PRINT_JOBS:
            jobClass = org.savapage.core.job.CupsSyncPrintJobs.class;
            break;

        case DB_BACKUP:
            jobClass = org.savapage.core.job.DbBackupJob.class;
            break;

        case DB_DERBY_OPTIMIZE:
            jobClass = org.savapage.core.job.DbDerbyOptimize.class;
            break;

        case EMAIL_OUTBOX_MONITOR:
            jobClass = org.savapage.core.job.EmailOutboxMonitor.class;
            break;

        case SYNC_USERS:
            data = new JobDataMap();
            data.put(SyncUsersJob.ATTR_IS_TEST, false);
            data.put(SyncUsersJob.ATTR_IS_DELETE_USERS, false);
            jobClass = org.savapage.core.job.SyncUsersJob.class;
            break;

        case SYNC_USER_GROUPS:
            data = new JobDataMap();
            data.put(SyncUsersJob.ATTR_IS_TEST, false);
            jobClass = org.savapage.core.job.SyncUserGroupsJob.class;
            break;

        case CHECK_MEMBERSHIP_CARD:
            jobClass = org.savapage.core.job.MemberCardCheckJob.class;
            break;

        case APP_LOG_CLEAN:
            jobClass = org.savapage.core.job.AppLogClean.class;
            break;

        case DOC_LOG_CLEAN:
            jobClass = org.savapage.core.job.DocLogClean.class;
            break;

        case IPP_GET_NOTIFICATIONS:
            jobClass = org.savapage.core.job.IppGetNotifications.class;
            break;

        case IMAP_LISTENER_JOB:
            jobClass = org.savapage.core.job.ImapListenerJob.class;
            break;

        case GCP_LISTENER_JOB:
            jobClass = org.savapage.core.job.GcpListenerJob.class;
            break;

        case GCP_POLL_FOR_AUTH_CODE:
            jobClass = org.savapage.core.job.GcpRegisterJob.class;
            break;

        case PAPERCUT_PRINT_MONITOR:
            jobClass =
                    org.savapage.ext.papercut.job.PaperCutPrintMonitorJob.class;
            break;

        case PRINTER_GROUP_CLEAN:
            jobClass = org.savapage.core.job.PrinterGroupClean.class;
            break;

        case SMARTSCHOOL_PRINT_MONITOR_JOB:
            jobClass =
                    org.savapage.ext.smartschool.job.SmartschoolPrintMonitorJob.class;
            break;

        default:
            return null;
        }

        JobBuilder builder = newJob(jobClass).withIdentity(name, group);

        if (data != null) {
            builder.usingJobData(data);
        }

        return builder.build();
    }

    /**
     *
     */
    private void initJobDetails() {
        //
        myHourlyJobs.add(createJob(SpJobType.CUPS_SUBS_RENEW,
                JOB_GROUP_SCHEDULED));

        //
        myWeeklyJobs.add(createJob(SpJobType.DB_BACKUP, JOB_GROUP_SCHEDULED));

        //
        myDailyJobs.add(createJob(SpJobType.SYNC_USERS, JOB_GROUP_SCHEDULED));
        myDailyJobs.add(createJob(SpJobType.CHECK_MEMBERSHIP_CARD,
                JOB_GROUP_SCHEDULED));
        myDailyJobs.add(createJob(SpJobType.PRINTER_GROUP_CLEAN,
                JOB_GROUP_SCHEDULED));
    }

    /**
     *
     */
    private void startJobs() {

        LOGGER.debug("Setting up scheduled jobs...");

        scheduleJobs(IConfigProp.Key.SCHEDULE_HOURLY);
        scheduleJobs(IConfigProp.Key.SCHEDULE_DAILY);
        scheduleJobs(IConfigProp.Key.SCHEDULE_WEEKLY);
        scheduleJobs(IConfigProp.Key.SCHEDULE_MONTHLY);
        scheduleJobs(IConfigProp.Key.SCHEDULE_DAILY_MAINT);
    }

    /**
     * Tells quartz to (re)schedule the jobs in the list with the cron
     * expression from configKey.
     *
     * @param jobs
     * @param configKey
     */
    private void scheduleJobs(final List<JobDetail> jobs,
            final IConfigProp.Key configKey) {

        if (jobs.isEmpty()) {
            LOGGER.debug(ConfigManager.instance().getConfigKey(configKey)
                    + " : " + jobs.size() + " jobs");
        }
        for (JobDetail job : jobs) {
            scheduleJob(job, configKey);
        }

    }

    /**
     *
     * @param isTest
     * @param deleteUser
     */
    public void scheduleOneShotUserSync(final boolean isTest,
            final boolean deleteUser) {

        JobDataMap data = new JobDataMap();
        data.put(SyncUsersJob.ATTR_IS_TEST, isTest);
        data.put(SyncUsersJob.ATTR_IS_DELETE_USERS, deleteUser);

        JobDetail job =
                newJob(org.savapage.core.job.SyncUsersJob.class)
                        .withIdentity(SpJobType.SYNC_USERS.toString(),
                                JOB_GROUP_ONESHOT).usingJobData(data).build();

        scheduleOneShotJob(job, DateUtil.DURATION_MSEC_SECOND);
    }

    /**
     *
     * @param isTest
     */
    public void scheduleOneShotUserGroupsSync(final boolean isTest) {

        JobDataMap data = new JobDataMap();
        data.put(SyncUsersJob.ATTR_IS_TEST, isTest);

        JobDetail job =
                newJob(org.savapage.core.job.SyncUserGroupsJob.class)
                        .withIdentity(SpJobType.SYNC_USER_GROUPS.toString(),
                                JOB_GROUP_ONESHOT).usingJobData(data).build();

        scheduleOneShotJob(job, DateUtil.DURATION_MSEC_SECOND);
    }

    /**
     *
     * @param requestingUser
     * @param subscriptionId
     * @param secondsFromNow
     */
    public void scheduleOneShotIppNotifications(String requestingUser,
            String subscriptionId, long secondsFromNow) {

        final JobDataMap data = new JobDataMap();
        data.put(IppGetNotifications.ATTR_REQUESTING_USER, requestingUser);
        data.put(IppGetNotifications.ATTR_SUBSCRIPTION_ID, subscriptionId);

        final JobDetail job =
                newJob(org.savapage.core.job.IppGetNotifications.class)
                        .withIdentity(
                                SpJobType.IPP_GET_NOTIFICATIONS.toString(),
                                JOB_GROUP_ONESHOT).usingJobData(data).build();

        rescheduleOneShotJob(job, secondsFromNow
                * DateUtil.DURATION_MSEC_SECOND);
    }

    /**
     *
     * @param milliSecondsFromNow
     */
    public void scheduleOneShotEmailOutboxMonitor(long milliSecondsFromNow) {

        final JobDataMap data = new JobDataMap();

        final JobDetail job =
                newJob(org.savapage.core.job.EmailOutboxMonitor.class)
                        .withIdentity(
                                SpJobType.EMAIL_OUTBOX_MONITOR.toString(),
                                JOB_GROUP_ONESHOT).usingJobData(data).build();

        rescheduleOneShotJob(job, milliSecondsFromNow);
    }

    /**
     *
     * @param milliSecondsFromNow
     */
    public void scheduleOneShotPaperCutPrintMonitor(long milliSecondsFromNow) {

        final JobDataMap data = new JobDataMap();

        final JobDetail job =
                newJob(
                        org.savapage.ext.papercut.job.PaperCutPrintMonitorJob.class)
                        .withIdentity(
                                SpJobType.PAPERCUT_PRINT_MONITOR.toString(),
                                JOB_GROUP_ONESHOT).usingJobData(data).build();

        rescheduleOneShotJob(job, milliSecondsFromNow);
    }

    /**
     *
     * @param milliSecondsFromNow
     */
    public void scheduleOneShotImapListener(long milliSecondsFromNow) {

        JobDataMap data = new JobDataMap();

        JobDetail job =
                newJob(org.savapage.core.job.ImapListenerJob.class)
                        .withIdentity(SpJobType.IMAP_LISTENER_JOB.toString(),
                                JOB_GROUP_ONESHOT).usingJobData(data).build();

        rescheduleOneShotJob(job, milliSecondsFromNow);

        ImapPrinter.setOnline(true);
    }

    /**
     *
     * @param milliSecondsFromNow
     */
    public void scheduleOneShotSmartSchoolPrintMonitor(final boolean simulate,
            long milliSecondsFromNow) {

        final JobDataMap data = new JobDataMap();

        data.put(SmartschoolPrintMonitorJob.ATTR_SIMULATION,
                Boolean.valueOf(simulate));

        final JobDetail job =
                newJob(
                        org.savapage.ext.smartschool.job.SmartschoolPrintMonitorJob.class)
                        .withIdentity(
                                SpJobType.SMARTSCHOOL_PRINT_MONITOR_JOB
                                        .toString(),
                                JOB_GROUP_ONESHOT).usingJobData(data).build();

        rescheduleOneShotJob(job, milliSecondsFromNow);

        SmartschoolPrinter.setOnline(true);
    }

    /**
     *
     * @param milliSecondsFromNow
     */
    public void scheduleOneShotGcpListener(long milliSecondsFromNow) {

        final JobDataMap data = new JobDataMap();

        final JobDetail job =
                newJob(org.savapage.core.job.GcpListenerJob.class)
                        .withIdentity(SpJobType.GCP_LISTENER_JOB.toString(),
                                JOB_GROUP_ONESHOT).usingJobData(data).build();

        rescheduleOneShotJob(job, milliSecondsFromNow);
    }

    /**
     *
     * @param milliSecondsFromNow
     */
    public void scheduleOneShotGcpPollForAuthCode(final String pollingUrl,
            final Integer tokenDuration, long milliSecondsFromNow) {

        final JobDataMap data = new JobDataMap();

        data.put(GcpRegisterJob.KEY_POLLING_URL, pollingUrl);
        data.put(GcpRegisterJob.KEY_TOKEN_DURATION, tokenDuration);

        final JobDetail job =
                newJob(org.savapage.core.job.GcpRegisterJob.class)
                        .withIdentity(
                                SpJobType.GCP_POLL_FOR_AUTH_CODE.toString(),
                                JOB_GROUP_ONESHOT).usingJobData(data).build();

        rescheduleOneShotJob(job, milliSecondsFromNow);
    }

    /**
     * .
     *
     * @return {@code true} if at least one instance of the identified job was
     *         found and interrupted.
     */
    public static boolean interruptEmailOutputMonitor() {
        return instance().interruptJob(SpJobType.EMAIL_OUTBOX_MONITOR,
                JOB_GROUP_ONESHOT);
    }

    /**
     * .
     *
     * @return {@code true} if at least one instance of the identified job was
     *         found and interrupted.
     */
    public static boolean interruptPaperCutPrintMonitor() {
        return instance().interruptJob(SpJobType.PAPERCUT_PRINT_MONITOR,
                JOB_GROUP_ONESHOT);
    }

    /**
     * @return {@code true} if at least one instance of the identified job was
     *         found and interrupted.
     */
    public static boolean interruptImapListener() {
        ImapPrinter.setOnline(false);
        return instance().interruptJob(SpJobType.IMAP_LISTENER_JOB,
                JOB_GROUP_ONESHOT);
    }

    /**
     * @return {@code true} if at least one instance of the identified job was
     *         found and interrupted.
     */
    public static boolean interruptGcpListener() {
        return instance().interruptJob(SpJobType.GCP_LISTENER_JOB,
                JOB_GROUP_ONESHOT);
    }

    /**
     * @return {@code true} if at least one instance of the identified job was
     *         found and interrupted.
     */
    public static boolean interruptSmartSchoolPoller() {
        SmartschoolPrinter.setOnline(false);
        return instance().interruptJob(SpJobType.SMARTSCHOOL_PRINT_MONITOR_JOB,
                JOB_GROUP_ONESHOT);
    }

    /**
     * Tells quartz to schedule a one shot job.
     *
     * @param typeOfJob
     *            The type of job.
     * @param secondsFromNow
     *            Number of seconds from now.
     */
    public void scheduleOneShotJob(final SpJobType typeOfJob,
            final long secondsFromNow) {

        scheduleOneShotJob(createJob(typeOfJob, JOB_GROUP_ONESHOT),
                secondsFromNow * DateUtil.DURATION_MSEC_SECOND);
    }

    /**
     * Tells quartz to resume the {@link SpJobType#CUPS_SUBS_RENEW}.
     */
    public static void resumeCupSubsRenew() {
        instance().resumeJob(SpJobType.CUPS_SUBS_RENEW, JOB_GROUP_SCHEDULED);
    }

    /**
     * Tells quartz to pause the {@link SpJobType#CUPS_SUBS_RENEW}.
     *
     * @return {@code true} if the Job was found and deleted.
     */
    public static void pauseCupSubsRenew() {
        instance().pauseJob(SpJobType.CUPS_SUBS_RENEW, JOB_GROUP_SCHEDULED);
    }

    /**
     *
     * @param typeOfJob
     * @param group
     */
    public void pauseJob(final SpJobType typeOfJob, final String group) {
        try {
            myScheduler.pauseJob(new JobKey(typeOfJob.toString(), group));
        } catch (SchedulerException e) {
            throw new SpException(e.getMessage(), e);
        }
    }

    /**
     *
     * @param typeOfJob
     * @param group
     */
    public void resumeJob(final SpJobType typeOfJob, final String group) {
        try {
            myScheduler.resumeJob(new JobKey(typeOfJob.toString(), group));
        } catch (SchedulerException e) {
            throw new SpException(e.getMessage(), e);
        }
    }

    /**
     * Interrupts running job type with a group.
     *
     * @param typeOfJob
     * @param group
     * @return {@code true} if at least one instance of the identified job was
     *         found and interrupted.
     */
    public boolean interruptJob(final SpJobType typeOfJob, final String group) {
        try {
            return myScheduler
                    .interrupt(new JobKey(typeOfJob.toString(), group));
        } catch (SchedulerException e) {
            throw new SpException(e.getMessage(), e);
        }
    }

    /**
     * Tells quartz to schedule a one shot job.
     *
     * @param job
     *            The job.
     * @param milliSecondsFromNow
     *            Number of milliseconds from now.
     */
    private void scheduleOneShotJob(final JobDetail job,
            final long milliSecondsFromNow) {
        scheduleOneShotJob(job, milliSecondsFromNow, false);
    }

    /**
     * Tells quartz to (re)schedule a one shot job.
     *
     * @param job
     *            The job.
     * @param milliSecondsFromNow
     *            Number of milliseconds from now.
     */
    private void rescheduleOneShotJob(final JobDetail job,
            final long milliSecondsFromNow) {
        scheduleOneShotJob(job, milliSecondsFromNow, true);
    }

    /**
     * Tells quartz to schedule a one shot job.
     *
     * @param job
     *            The job.
     * @param milliSecondsFromNow
     *            Number of milliseconds from now.
     * @param reschedule
     */
    private void scheduleOneShotJob(final JobDetail job,
            final long milliSecondsFromNow, boolean reschedule) {

        final String jobName = job.getKey().getName();
        final String jobGroup = job.getKey().getGroup();

        long startTime = System.currentTimeMillis() + milliSecondsFromNow;

        SimpleTrigger trigger =
                (SimpleTrigger) newTrigger()
                        .withIdentity("once." + jobName, jobGroup)
                        .startAt(new Date(startTime)).forJob(jobName, jobGroup)
                        .build();

        if (trigger != null) {

            try {

                boolean doReschedule = false;

                if (reschedule && myScheduler.checkExists(trigger.getKey())) {
                    doReschedule = true;
                }

                if (doReschedule) {
                    myScheduler.rescheduleJob(trigger.getKey(), trigger);
                } else {
                    myScheduler.scheduleJob(job, trigger);
                }

                LOGGER.debug("one-shot job : [" + jobName + "] [" + jobGroup
                        + "]");
            } catch (SchedulerException e) {
                /*
                 * Example: Unable to store Job : 'DEFAULT.DbBackup', because
                 * one already exists with this identification.
                 */
                final String msg =
                        "Error scheduling one-shot job [" + jobName + "]["
                                + jobGroup + "] : " + e.getMessage();
                throw new SpException(msg, e);
            }
        }
    }

    /**
     * Tells quartz to (re)schedule the job with the cron expression from
     * configKey.
     *
     * @param job
     * @param configKey
     */
    private void scheduleJob(JobDetail job, final IConfigProp.Key configKey) {

        final String jobName = job.getKey().getName();
        final String jobGroup = job.getKey().getGroup();

        CronTrigger trigger = createTrigger(jobName, jobGroup, configKey);

        if (trigger != null) {
            try {

                boolean isReschedule =
                        myScheduler.checkExists(trigger.getKey());

                if (isReschedule) {
                    myScheduler.rescheduleJob(trigger.getKey(), trigger);
                } else {
                    myScheduler.scheduleJob(job, trigger);
                }

                LOGGER.debug(ConfigManager.instance().getConfigKey(configKey)
                        + " : [" + jobName + "] [" + jobGroup + "]");

            } catch (SchedulerException e) {

                final String msg =
                        "Error scheduling job ["
                                + jobName
                                + "]["
                                + jobGroup
                                + "] for ["
                                + ConfigManager.instance().getConfigKey(
                                        configKey) + "] : " + e.getMessage();
                throw new SpException(msg, e);
            }
        }
    }

    /**
     * Creates a cron trigger for a job.
     *
     * @param jobName
     * @param jobGroup
     * @param configKey
     *            The string representation this value is used as key for the
     *            trigger.
     * @return
     */
    private CronTrigger createTrigger(String jobName, String jobGroup,
            final IConfigProp.Key configKey) {

        ConfigManager cm = ConfigManager.instance();

        final String cronExp = cm.getConfigValue(configKey);
        final String strKey = cm.getConfigKey(configKey);

        return createTrigger(jobName, jobGroup, cronExp, strKey);
    }

    /**
     * Creates a cron trigger for a job.
     *
     * @param jobName
     * @param jobGroup
     * @param cronExp
     *            The CRON expression
     * @param configKey
     * @return
     */
    private CronTrigger createTrigger(String jobName, String jobGroup,
            final String cronExp, final String configKey) {

        return newTrigger().withIdentity(configKey + "." + jobName, jobGroup)
                .startNow().withSchedule(cronSchedule(cronExp)).build();
    }

    /*
     * private void reportCronParseError(String expression, String configKey) {
     * getApplicationLogManager().log(getClass(), ApplicationLogLevelEnum.ERROR,
     * "TaskSchedulerImpl.unable-to-parse-cron-expression", new String[] {
     * expression, configKey }); }
     */
}
