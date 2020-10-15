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
package org.savapage.core.services;

import java.util.Date;
import java.util.Locale;

import javax.persistence.EntityManager;

import org.savapage.core.dao.enums.AppLogLevelEnum;
import org.savapage.core.jpa.AppLog;

/**
 *
 * @author Rijk Ravestein
 *
 */
public interface AppLogService {

    /**
     * Counts the total number of ERROR entries.
     *
     * @param dateAfter
     *            The date/time after which the count starts. When {@code null},
     *            all level entries are counted.
     * @return the number of errors after the date/time.
     */
    long countErrors(Date dateAfter);

    /**
     * Counts the total number of WARNING entries.
     *
     * @param dateAfter
     *            The date/time after which the count starts. When {@code null},
     *            all level entries are counted.
     * @return The number of warnings after the date/time.
     */
    long countWarnings(Date dateAfter);

    /**
     * Inserts an {@link AppLog} row in the database.
     *
     * @param level
     *            the log level.
     * @param message
     *            The message.
     */
    void logMessage(AppLogLevelEnum level, String message);

    /**
     * Logs a application message and inserts an {@link AppLog} row in the
     * database.
     * <p>
     * <b>NOTE</b>: this method MUST have its own {@link EntityManager} and
     * transaction scope, because it must be possible to log a message of a
     * failed database action.
     * </p>
     * <p>
     * The message written to the log file is in {@link Locale.US}. The AppLog
     * database message is in the locale as set with
     * {@link Key#SYS_DEFAULT_LOCALE}.
     * </p>
     *
     * @param level
     *            the log level.
     * @param callingClass
     *            The calling class.
     * @param messageKey
     *            The message key
     * @param args
     *            The message arguments.
     * @return The message written to the AppLog.
     */
    String logMessage(AppLogLevelEnum level,
            Class<? extends Object> callingClass, String messageKey,
            String... args);

}
