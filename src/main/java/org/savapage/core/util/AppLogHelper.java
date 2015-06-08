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
package org.savapage.core.util;

import org.savapage.core.dao.helpers.AppLogLevelEnum;
import org.savapage.core.services.AppLogService;
import org.savapage.core.services.ServiceContext;

/**
 * Helper for logging application messages.
 *
 * @author Datraverse B.V.
 *
 */
public final class AppLogHelper {

    /**
     *
     */
    private AppLogHelper() {

    }

    /**
     *
     */
    private static final AppLogService APPL_LOG_SERVICE = ServiceContext
            .getServiceFactory().getAppLogService();

    /**
     *
     * @param callingClass
     * @param messageKey
     *            The key of the message in the {@code messages_<locale>.xml}
     *            file.
     * @param args
     *            Variable string list used to fill the placeholders in the
     *            message template.
     * @return The message.
     */
    public static String logInfo(final Class<? extends Object> callingClass,
            final String messageKey, final String... args) {
        return APPL_LOG_SERVICE.logMessage(AppLogLevelEnum.INFO, callingClass,
                messageKey, args);
    }

    public static String logWarning(final Class<? extends Object> callingClass,
            final String messageKey, final String... args) {
        return APPL_LOG_SERVICE.logMessage(AppLogLevelEnum.WARN, callingClass,
                messageKey, args);
    }

    public static String logError(final Class<? extends Object> callingClass,
            final String messageKey, final String... args) {
        return APPL_LOG_SERVICE.logMessage(AppLogLevelEnum.ERROR, callingClass,
                messageKey, args);
    }

    public static String log(final Class<? extends Object> callingClass,
            final AppLogLevelEnum level, final String messageKey, final String... args) {
        return APPL_LOG_SERVICE.logMessage(level, callingClass, messageKey,
                args);
    }

    public static void log(final AppLogLevelEnum level,
            final String message) {
        APPL_LOG_SERVICE.logMessage(level, message);
    }

}
