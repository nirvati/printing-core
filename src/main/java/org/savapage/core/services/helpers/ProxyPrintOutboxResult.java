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
package org.savapage.core.services.helpers;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class ProxyPrintOutboxResult {

    /**
     * The number of released jobs.
     */
    private final int jobs;

    /**
     * The number of released sheets.
     */
    private final int sheets;

    /**
     * The number of released pages.
     */
    private final int pages;

    /**
     * Creates a zero-result.
     */
    public ProxyPrintOutboxResult() {
        this.jobs = 0;
        this.sheets = 0;
        this.pages = 0;
    }

    /**
     *
     * @param jobs
     *            The number of released jobs.
     * @param sheets
     *            The number of released sheets.
     * @param pages
     *            The number of released pages.
     */
    public ProxyPrintOutboxResult(final int jobs, final int sheets,
            final int pages) {
        this.jobs = jobs;
        this.sheets = sheets;
        this.pages = pages;
    }

    /**
     * @return The number of released jobs.
     */
    public int getJobs() {
        return jobs;
    }

    /**
     * @return The number of released sheets.
     */
    public int getSheets() {
        return sheets;
    }

    /**
     * @return The number of released pages.
     */
    public int getPages() {
        return pages;
    }

}
