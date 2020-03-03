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
package org.savapage.core.config;

import org.savapage.core.SpInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class CoreShutdownHook extends Thread {

    /**
     *
     */
    private static final Logger LOGGER = LoggerFactory
            .getLogger(CoreShutdownHook.class);

    /**
     *
     */
    private final ConfigManager myManager;

    /**
     *
     * @param manager
     *            The {@link ConfigManager}.
     */
    public CoreShutdownHook(final ConfigManager manager) {
        super("CoreShutdownHook");
        myManager = manager;
    }

    @Override
    public void run() {

        SpInfo.instance().log("Shutting down Application ...");

        try {
            myManager.exit();
            SpInfo.instance().log("... Application shutdown completed.");
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }
}
