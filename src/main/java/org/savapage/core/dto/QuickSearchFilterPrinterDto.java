/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2019 Datraverse B.V.
 * Authors: Rijk Ravestein.
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
package org.savapage.core.dto;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class QuickSearchFilterPrinterDto extends QuickSearchFilterDto {

    /** */
    private Boolean jobTicket;

    /** */
    private Boolean searchCupsName;

    /**
     * @return If {@code null}, filter all printers. If {@link Boolean#TRUE},
     *         filter job ticket printers. If {@link Boolean#FALSE}, filter non
     *         job ticket printers.
     */
    public Boolean getJobTicket() {
        return jobTicket;
    }

    /**
     * @param jobTicket
     *            If {@code null}, filter all printers. If
     *            {@link Boolean#TRUE}, filter job ticket printers. If
     *            {@link Boolean#FALSE}, filter non job ticket printers.
     */
    public void setJobTicket(final Boolean jobTicket) {
        this.jobTicket = jobTicket;
    }

    /**
     * @return If {@link Boolean#TRUE} additionally filter on CUPS printer
     *         name.
     */
    public Boolean getSearchCupsName() {
        return searchCupsName;
    }

    /**
     * @param searchCupsName
     *            If {@link Boolean#TRUE} additionally filter on CUPS printer
     *            name.
     */
    public void setSearchCupsName(final Boolean searchCupsName) {
        this.searchCupsName = searchCupsName;
    }

}
