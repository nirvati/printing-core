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
package org.savapage.core;

import java.util.Calendar;

import org.savapage.common.SystemPropertyEnum;
import org.savapage.core.community.CommunityDictEnum;
import org.savapage.core.community.MemberCard;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.jpa.tools.DbVersionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generic logger.
 *
 * @author Rijk Ravestein
 *
 */
public final class SpInfo {

    /** */
    private static final Logger LOGGER = LoggerFactory.getLogger(SpInfo.class);

    /** */
    private static final String DELIM = "+----------------------------"
            + "--------------------------------------------+";

    /** */
    private SpInfo() {
    }

    /**
     * Logs application signature.
     *
     * @param dbInfo
     *            Database info.
     */
    public void logSignature(final DbVersionInfo dbInfo) {
        LOGGER.info("\n" + DELIM
        //
                + "\n| " + ConfigManager.getAppNameVersionBuild()
                //
                + "\n| Copyright (c) 2011-"
                + Calendar.getInstance().get(Calendar.YEAR) + " by "
                + CommunityDictEnum.DATRAVERSE_BV.getWord()
                //
                + "\n| " + VersionInfo.LICENSE_NAME
                //
                + "\n| " + SystemPropertyEnum.JAVA_VM_NAME.getValue() + " ("
                + SystemPropertyEnum.JAVA_VERSION.getValue() + ")"
                //
                + "\n| " + dbInfo.getProdName() + " " + dbInfo.getProdVersion()
                //
                + "\n| Running as user [" + ConfigManager.getProcessUserName()
                + "]"
                //
                + "\n" + DELIM);
    }

    /**
     * .
     */
    public void logCommunityNotice() {
        try {
            LOGGER.info(String.format("\n%s\n%s\n%s", DELIM,
                    MemberCard.instance().getCommunityNotice("| "), DELIM));
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
    }

    /**
     * The SingletonHolder is loaded on the first execution of
     * {@link SpInfo#instance()} or the first access to
     * {@link SingletonHolder#INSTANCE}, not before.
     */
    private static class SingletonHolder {
        /** */
        public static final SpInfo INSTANCE = new SpInfo();
    }

    /**
     * @return The singleton instance.
     */
    public static SpInfo instance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     *
     * @param msg
     *            Message to log.
     */
    public void log(final String msg) {
        LOGGER.info(msg);
    }

    /**
     * Reserved for logging the end (closing down) of the application.
     *
     * @param msg
     *            Message to log.
     */
    public void logDestroy(final String msg) {
        LOGGER.info(msg + "\n" + DELIM);
    }

}
