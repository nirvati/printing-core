/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2020 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: 2011-2020 Datraverse B.V. <info@datraverse.com>
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

import java.nio.file.Paths;
import java.util.Date;

import org.apache.commons.lang3.time.DateUtils;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.UnableToInterruptJobException;
import org.savapage.core.SpInfo;
import org.savapage.core.cometd.AdminPublisher;
import org.savapage.core.cometd.PubLevelEnum;
import org.savapage.core.cometd.PubTopicEnum;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.inbox.UserHomeVisitor;
import org.savapage.core.util.AppLogHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Clean-up user home directories.
 *
 * @author Rijk Ravestein
 *
 */
public final class UserHomeClean extends AbstractJob {

    /** */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(UserHomeClean.class);

    /** */
    private static final String PUB_MSGKEY_BASE = "UserHomeClean";

    /** */
    private UserHomeVisitor cleaner;

    @Override
    protected void onInterrupt() throws UnableToInterruptJobException {
        if (cleaner != null) {
            cleaner.terminate();
        }
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
    public void onExecute(final JobExecutionContext ctx)
            throws JobExecutionException {

        // If interrupt is issued just after onInit().
        if (this.isInterrupted()) {
            return;
        }

        final ConfigManager cm = ConfigManager.instance();

        final Date dateNow = new Date();

        //
        final int printInExpiryMinutes =
                cm.getConfigInt(Key.PRINT_IN_JOB_EXPIRY_MINS);

        final Date printInCleanDate;

        if (printInExpiryMinutes == 0) {
            printInCleanDate = null;
        } else {
            printInCleanDate = DateUtils.addMinutes(dateNow,
                    printInExpiryMinutes + cm.getConfigInt(
                            Key.PRINT_IN_JOB_EXPIRY_IGNORED_MINS));
        }

        final RunModeSwitch runMode;
        final String runModeTag;

        if (ConfigManager.isCleanUpUserHomeTest()) {
            runMode = RunModeSwitch.DRY;
            runModeTag = "Scan";
        } else {
            runMode = RunModeSwitch.REAL;
            runModeTag = "Cleanup";
        }

        //
        final Date holdJobCleanDate = DateUtils.addMinutes(dateNow,
                cm.getConfigInt(Key.PROXY_PRINT_HOLD_IGNORED_MINS));

        final AdminPublisher publisher = AdminPublisher.instance();

        publisher.publish(PubTopicEnum.USER, PubLevelEnum.INFO,
                this.localizeSysMsg(PUB_MSGKEY_BASE.concat(".started"),
                        runModeTag));

        try {
            cleaner = new UserHomeVisitor(
                    Paths.get(ConfigManager.getSafePagesHomeDir()),
                    printInCleanDate, holdJobCleanDate, runMode);

            final UserHomeVisitor.ExecStats stats = cleaner.execute();

            if (stats == null) {
                publisher.publish(PubTopicEnum.USER, PubLevelEnum.WARN,
                        this.localizeSysMsg(PUB_MSGKEY_BASE.concat(".busy"),
                                runModeTag));
            } else {
                publisher.publish(PubTopicEnum.USER, PubLevelEnum.INFO,
                        this.localizeSysMsg(PUB_MSGKEY_BASE.concat(".finished"),
                                runModeTag));

                SpInfo.instance().log(this.getClass().getSimpleName()
                        .concat("\n").concat(stats.summary()));

                stats.updateDb();
            }

        } catch (Exception e) {

            LOGGER.error(e.getMessage(), e);

            final String msg = AppLogHelper.logError(getClass(),
                    PUB_MSGKEY_BASE + ".error", runModeTag, String.format(
                            "[%s] %s", e.getClass().getName(), e.getMessage()));

            publisher.publish(PubTopicEnum.USER, PubLevelEnum.ERROR, msg);
        }
    }

}
