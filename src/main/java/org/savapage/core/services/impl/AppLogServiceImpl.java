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
package org.savapage.core.services.impl;

import java.util.Date;

import javax.persistence.EntityManager;

import org.apache.commons.lang3.StringUtils;
import org.savapage.core.SpException;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.dao.AppLogDao;
import org.savapage.core.dao.enums.AppLogLevelEnum;
import org.savapage.core.jpa.AppLog;
import org.savapage.core.services.AppLogService;
import org.savapage.core.util.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rijk Ravestein
 *
 */
public class AppLogServiceImpl extends AbstractService
        implements AppLogService {

    /**
     * The logger.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(AppLogServiceImpl.class);

    @Override
    public final long countErrors(final Date dateAfter) {
        return filteredCount(AppLogLevelEnum.ERROR, dateAfter);
    }

    @Override
    public final long countWarnings(final Date dateAfter) {
        return filteredCount(AppLogLevelEnum.WARN, dateAfter);
    }

    /**
     * Counts the total number of entries for a
     * {@link AppLogDao.AppLogLevelEnum} after a certain date.
     *
     * @param level
     *            a {@link AppLogDao.AppLogLevelEnum}.
     * @param dateAfter
     *            The date/time after which the count starts. When {@code null},
     *            all level entries are counted.
     * @return the number of rows in this table after the date/time.
     */
    private long filteredCount(final AppLogLevelEnum level,
            final Date dateAfter) {

        final AppLogDao.ListFilter filter = new AppLogDao.ListFilter();
        filter.setDateFrom(dateAfter);

        filter.setLevel(level);
        return appLogDAO().getListCount(filter);
    }

    @Override
    public final void logMessage(final AppLogLevelEnum level,
            final String message) {

        final EntityManager em = ConfigManager.instance().createEntityManager();

        try {

            em.getTransaction().begin();
            em.persist(createAppLog(level, message));
            em.getTransaction().commit();

        } catch (SpException e) {

            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }

            LOGGER.error("Failed to write log to database");

        } finally {
            em.close();
        }
    }

    @Override
    public final String logMessage(final AppLogLevelEnum level,
            final Class<? extends Object> callingClass, final String messageKey,
            final String... args) {

        final String msgLog = Messages.getMessage(callingClass,
                ConfigManager.getDefaultLocale(), messageKey, args);

        if (level == AppLogLevelEnum.INFO) {
            LOGGER.debug(msgLog);
        } else if (level == AppLogLevelEnum.WARN) {
            LOGGER.warn(msgLog);
        } else if (level == AppLogLevelEnum.ERROR) {
            LOGGER.error(msgLog);
        }

        final String msg = Messages.getMessage(callingClass, messageKey, args);
        this.logMessage(level, msg);
        return msg;
    }

    /**
     * Creates an AppLog instance.
     *
     * @param level
     *            The log level.
     * @param message
     *            The message
     * @return A new AppLog instance.
     */
    private AppLog createAppLog(final AppLogLevelEnum level,
            final String message) {

        final AppLog log = new AppLog();

        // Mantis #1105
        log.setMessage(StringUtils.defaultIfBlank(message, "?"));

        log.setLogLevel(level.getDbName());
        log.setLogDate(new Date());

        return log;
    }

}
