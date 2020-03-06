/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2020 Datraverse B.V.
 * Authors: Rijk Ravestein.
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

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class RunModeSwitch {

    /** */
    private final boolean modeReal;

    /** */
    public static final RunModeSwitch DRY = new RunModeSwitch(false);
    /** */
    public static final RunModeSwitch REAL = new RunModeSwitch(true);

    /**
     *
     * @param isModeReal
     *            {@code true} if Real Run.
     */
    private RunModeSwitch(final boolean isModeReal) {
        this.modeReal = isModeReal;
    }

    /**
     * @return {@code true} if Dry Run.
     */
    public boolean isDry() {
        return !this.modeReal;
    }

    /**
     * @return {@code true} if Real Run.
     */
    public boolean isReal() {
        return this.modeReal;
    }

}
