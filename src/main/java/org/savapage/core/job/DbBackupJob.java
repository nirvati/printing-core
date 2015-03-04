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

import java.io.File;
import java.util.Date;

import javax.persistence.EntityManager;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.UnableToInterruptJobException;
import org.savapage.core.cometd.AdminPublisher;
import org.savapage.core.cometd.PubLevelEnum;
import org.savapage.core.cometd.PubTopicEnum;
import org.savapage.core.concurrent.ReadWriteLockEnum;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.dao.DaoContext;
import org.savapage.core.dao.impl.DaoContextImpl;
import org.savapage.core.jpa.Entity;
import org.savapage.core.jpa.tools.DbTools;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.util.AppLogHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Datraverse B.V.
 */
public final class DbBackupJob extends AbstractJob {

    /**
     * The logger.
     */
    private static final Logger LOGGER = LoggerFactory
            .getLogger(DbBackupJob.class);

    @Override
    protected void onInterrupt() throws UnableToInterruptJobException {
        // noop
    }

    @Override
    protected void onInit(final JobExecutionContext ctx) {
        // noop
    }

    @Override
    protected void onExit(final JobExecutionContext ctx) {
        // noop
    }

    @Override
    protected void onExecute(final JobExecutionContext ctx)
            throws JobExecutionException {

        final ConfigManager cm = ConfigManager.instance();

        /*
         * Return if this is a scheduled (not a one-shot) job and auto-backup is
         * DISABLED.
         */
        if (ctx.getJobDetail().getKey().getGroup()
                .equals(SpJobScheduler.JOB_GROUP_SCHEDULED)
                && !cm.isConfigValue(Key.SYS_BACKUP_ENABLE_AUTOMATIC)) {
            return;
        }

        /*
         *
         */
        final DaoContext daoContext = ServiceContext.getDaoContext();

        final EntityManager em = DaoContextImpl.peekEntityManager();

        final AdminPublisher publisher = AdminPublisher.instance();

        /*
         *
         */
        String msg = null;
        PubLevelEnum level = PubLevelEnum.INFO;

        boolean unLockDatabase = false;

        try {

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Locking database...");
            }

            ReadWriteLockEnum.DATABASE_READONLY.setWriteLock(true);

            unLockDatabase = true;

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Database locked.");
            }

            //
            final File export = DbTools.exportDb(em);

            //
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Unlocking database...");
            }

            ReadWriteLockEnum.DATABASE_READONLY.setWriteLock(false);
            unLockDatabase = false;

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Database unlocked.");
            }

            msg =
                    AppLogHelper.logInfo(getClass(), "DbBackupJob.success",
                            export.getName());

            // Mantis #235
            daoContext.beginTransaction();

            cm.updateConfigKey(IConfigProp.Key.SYS_BACKUP_LAST_RUN_TIME,
                    new Date().getTime(), Entity.ACTOR_SYSTEM);

            daoContext.commit();

        } catch (Exception e) {

            daoContext.rollback();

            LoggerFactory.getLogger(this.getClass()).error(e.getMessage(), e);

            level = PubLevelEnum.ERROR;
            msg =
                    AppLogHelper.logError(getClass(), "DbBackupJob.error",
                            e.getMessage());

        } finally {

            if (unLockDatabase) {
                ReadWriteLockEnum.DATABASE_READONLY.setWriteLock(false);
            }
        }

        publisher.publish(PubTopicEnum.DB, level, msg);

        /*
         * If Scheduled AND Automatic Backup enabled...
         */
        if (ctx.getJobDetail().getKey().getGroup()
                .equals(SpJobScheduler.JOB_GROUP_SCHEDULED)
                && cm.isConfigValue(Key.SYS_BACKUP_ENABLE_AUTOMATIC)) {
            /*
             * Delete old back-ups
             */
            msg = null;
            level = PubLevelEnum.INFO;

            try {

                final int nFilesDeleted =
                        DbTools.cleanBackupDirectory(new Date(),
                                cm.getConfigInt(Key.SYS_BACKUP_DAYS_TO_KEEP));

                if (nFilesDeleted == 1) {
                    msg =
                            AppLogHelper.logInfo(getClass(),
                                    "CleanBackupDir.success.single");
                } else if (nFilesDeleted > 1) {
                    msg =
                            AppLogHelper.logInfo(getClass(),
                                    "CleanBackupDir.success.plural",
                                    String.valueOf(nFilesDeleted));
                }

            } catch (Exception e) {

                LoggerFactory.getLogger(this.getClass()).error(e.getMessage(),
                        e);

                level = PubLevelEnum.ERROR;

                msg =
                        AppLogHelper.logError(getClass(),
                                "CleanBackupDir.error", e.getMessage());

            }

            if (msg != null) {
                publisher.publish(PubTopicEnum.DB, level, msg);
            }

            /*
             * Launch on-shot-jobs to delete App and/or Doc Logs
             */
            try {

                if (cm.isConfigValue(Key.DELETE_APP_LOG)) {
                    SpJobScheduler.instance().scheduleOneShotJob(
                            SpJobType.APP_LOG_CLEAN, 1L);
                }
                if (cm.isConfigValue(Key.DELETE_DOC_LOG)) {
                    SpJobScheduler.instance().scheduleOneShotJob(
                            SpJobType.DOC_LOG_CLEAN, 1L);
                }

                if (ConfigManager.isDbInternal()) {
                    SpJobScheduler.instance().scheduleOneShotJob(
                            SpJobType.DB_DERBY_OPTIMIZE, 1L);
                }

            } catch (Exception e) {

                LoggerFactory.getLogger(this.getClass()).error(e.getMessage(),
                        e);

                msg =
                        AppLogHelper.logError(getClass(), "Scheduler.error",
                                e.getMessage());

                publisher.publish(PubTopicEnum.SCHEDULER, PubLevelEnum.ERROR,
                        msg);
            }
        }

    }

}