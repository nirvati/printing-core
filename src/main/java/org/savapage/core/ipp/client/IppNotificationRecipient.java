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
package org.savapage.core.ipp.client;

import org.savapage.core.services.ProxyPrintService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class IppNotificationRecipient {

    /** */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(IppNotificationRecipient.class);

    /**
     * .
     */
    @SuppressWarnings("unused")
    private final ProxyPrintService proxyPrintService;

    /**
     * @param svc
     *            {@link ProxyPrintService}.
     */
    public IppNotificationRecipient(final ProxyPrintService svc) {
        this.proxyPrintService = svc;
    }

    /**
     *
     * @param event
     * @param printerName
     * @param printerState
     */
    public void onPrinterEvent(final String event, final String printerName,
            final Integer printerState) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("[{}] printer [{}] state [{}]", event, printerName,
                    printerState);
        }
    }

    /**
     * @param event
     */
    public void onServerEvent(final String event) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Event [{}]", event);
        }
    }

}
