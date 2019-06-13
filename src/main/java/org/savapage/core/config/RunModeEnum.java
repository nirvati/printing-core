/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2019 Datraverse B.V.
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.core.config;

/**
 * The Application run-mode.
 *
 * @author Rijk Ravestein
 *
 */
public enum RunModeEnum {

    /**
     * Fully initialized as a runnable core library in a Web Server context.
     * <p>
     * The job scheduler is NOT initialized, this should be done by the (server)
     * application.
     * </p>
     */
    SERVER,
    /**
     * Initialized as a runnable core library (with a fully functional
     * database), so basic operations can be performed. Community Membership
     * operations are not allowed.
     */
    LIB,
    /**
     * Initialized to a core library, without a fully functional database, so
     * basic operations can be performed. Community Membership are not allowed.
     */
    CORE

}
