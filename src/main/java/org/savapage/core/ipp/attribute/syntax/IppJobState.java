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
package org.savapage.core.ipp.attribute.syntax;

import org.savapage.core.jpa.PrintOut;

/**
 * Note: see {@link org.savapage.core.ipp.IppJobStateEnum}, which is the Java
 * enum used to interpret the {@link PrintOut#getCupsJobState()}.
 *
 * @author Rijk Ravestein
 *
 */
public final class IppJobState extends IppEnum {

    /** */
    private static class SingletonHolder {
        /** */
        public static final IppJobState INSTANCE = new IppJobState();
    }

    /** */
    public static final String STATE_PENDING = "3";
    /** */
    public static final String STATE_PENDING_HELD = "4";
    /** */
    public static final String STATE_PROCESSING = "5";
    /** */
    public static final String STATE_PROCESSING_STOPPED = "6";
    /** */
    public static final String STATE_CANCELED = "7";
    /** */
    public static final String STATE_ABORTED = "8";
    /** */
    public static final String STATE_COMPLETED = "9";

    /**
     * @return The singleton instance.
     */
    public static IppJobState instance() {
        return SingletonHolder.INSTANCE;
    }

}
