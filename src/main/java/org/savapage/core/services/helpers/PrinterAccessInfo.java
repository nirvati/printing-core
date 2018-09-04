/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2018 Datraverse B.V.
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
package org.savapage.core.services.helpers;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class PrinterAccessInfo {

    /** */
    boolean jobTicketsOnly;

    /** */
    boolean jobTicketsPresent;

    /**
     * @return {@code true}, if only job ticket printers are available.
     */
    public boolean isJobTicketsOnly() {
        return jobTicketsOnly;
    }

    /**
     * @param jobTicketsOnly
     *            {@code true}, if only job ticket printers are available.
     */
    public void setJobTicketsOnly(boolean jobTicketsOnly) {
        this.jobTicketsOnly = jobTicketsOnly;
    }

    /**
     * @return {@code true}, if at least one job ticket printers is available.
     */
    public boolean isJobTicketsPresent() {
        return jobTicketsPresent;
    }

    /**
     * @param jobTicketsPresent
     *            {@code true}, if at least one job ticket printers is
     *            available.
     */
    public void setJobTicketsPresent(boolean jobTicketsPresent) {
        this.jobTicketsPresent = jobTicketsPresent;
    }

}
