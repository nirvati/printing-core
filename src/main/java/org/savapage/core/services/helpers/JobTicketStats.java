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

import org.savapage.core.outbox.OutboxInfoDto.OutboxJobDto;

/**
 * Job Ticket Statistics.
 *
 * @author Rijk Ravestein
 *
 */
public final class JobTicketStats {

    private int jobs;
    private int copies;
    private int sheets;
    private int pages;

    public int getJobs() {
        return jobs;
    }

    public void setJobs(int jobs) {
        this.jobs = jobs;
    }

    public int getCopies() {
        return copies;
    }

    public void setCopies(int copies) {
        this.copies = copies;
    }

    public int getSheets() {
        return sheets;
    }

    public void setSheets(int sheets) {
        this.sheets = sheets;
    }

    public int getPages() {
        return pages;
    }

    public void setPages(int pages) {
        this.pages = pages;
    }

    public JobTicketStats increment(final JobTicketStats in) {
        jobs += in.jobs;
        copies += in.copies;
        sheets += in.sheets;
        pages += in.pages;
        return this;
    }

    public JobTicketStats decrement(final JobTicketStats out) {
        jobs -= out.jobs;
        copies -= out.copies;
        sheets -= out.sheets;
        pages -= out.pages;
        return this;
    }

    /**
     * @param dto
     *            The ticket.
     * @return The statistics.
     */
    public static JobTicketStats create(final OutboxJobDto dto) {

        final JobTicketStats stats = new JobTicketStats();

        stats.setJobs(1);
        stats.setCopies(dto.getCopies());
        stats.setSheets(dto.getSheets());
        stats.setPages(dto.getPages());

        return stats;
    }

}
