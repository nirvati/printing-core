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
package org.savapage.core.doc.soffice;

/**
 * Configuration parameters.
 *
 * @author Rijk Ravestein
 *
 */
public class SOfficeConfig extends SOfficeSettings {

    /**
    *
    */
    private boolean enabled;

    /**
     * The default port numbers. We take 2 so a Dual Core (minimum requirement
     * for SavasPage) can be utilized.
     */
    private int[] portNumbers = new int[] { 2002, 2003 };

    /**
     *
     * @return {@code true} when SOffice service is enabled.
     */
    public final boolean isEnabled() {
        return enabled;
    }

    /**
     * @param enable
     *            {@code true} when enabled.
     */
    public final void setEnabled(final boolean enable) {
        this.enabled = enable;
    }

    /**
     * @return The port numbers.
     */
    public final int[] getPortNumbers() {
        return portNumbers;
    }

    /**
     * @param ports
     *            The port numbers.
     */
    public final void setPortNumbers(final int[] ports) {
        this.portNumbers = ports;
    }

    /**
     * @return The UNO urls.
     */
    public final SOfficeUnoUrl[] createUnoUrls() {

        final SOfficeUnoUrl[] unoUrls = new SOfficeUnoUrl[portNumbers.length];

        for (int i = 0; i < portNumbers.length; i++) {
            unoUrls[i] = SOfficeUnoUrl.socket(portNumbers[i]);
        }
        return unoUrls;
    }

}
