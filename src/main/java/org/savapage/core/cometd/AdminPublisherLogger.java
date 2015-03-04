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
package org.savapage.core.cometd;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A dedicated logger for {@link AdminPublisher} messages.
 *
 * @author Datraverse B.V.
 */
public class AdminPublisherLogger {

    /**
     * .
     */
    private static final Logger LOGGER = LoggerFactory
            .getLogger(AdminPublisherLogger.class);

    private static final String DATEFORMAT_PATTERN = "yyyy-MM-dd'\t'HH:mm:ss.S";

    private AdminPublisherLogger() {

    }

    /**
     *
     * @param topic
     * @param level
     * @param msg
     * @param now
     */
    public static void logInfo(final PubTopicEnum topic,
            final PubLevelEnum level, final String msg, final Date now) {

        if (LOGGER.isInfoEnabled()) {

            final SimpleDateFormat dateFormat =
                    new SimpleDateFormat(DATEFORMAT_PATTERN);

            LOGGER.info(String.format("%s\t%s\t%s\t%s\t%s",
                    dateFormat.format(now), topic.toString(), level.toString(),
                    msg, Thread.currentThread().getName()));
        }
    }

    /**
     *
     * @return
     */
    public static boolean isEnabled() {
        return LOGGER.isInfoEnabled();
    }

}
