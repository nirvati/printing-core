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
public final class JobTicketQueueInfo {

    public enum Scope {
        PRINT, COPY
    }

    /** */
    private final JobTicketStats statsPrintJobs;
    /** */
    private final JobTicketStats statsCopyJobs;

    /** */
    public JobTicketQueueInfo() {
        this.statsPrintJobs = new JobTicketStats();
        this.statsCopyJobs = new JobTicketStats();
    }

    /**
     *
     * @return
     */
    public JobTicketStats getStatsPrintJobs() {
        return statsPrintJobs;
    }

    /**
     *
     * @return
     */
    public JobTicketStats getStatsCopyJobs() {
        return statsCopyJobs;
    }

    /**
     *
     * @param scope
     *            The scope.
     * @param plus
     *            Stats to add.
     */
    public void increment(final Scope scope, final JobTicketStats plus) {
        if (scope == Scope.PRINT) {
            this.statsPrintJobs.increment(plus);
        } else {
            this.statsCopyJobs.increment(plus);
        }
    }

    /**
     *
     * @param scope
     *            The scope.
     * @param minus
     *            Stats to subtract.
     */
    public void decrement(final Scope scope, final JobTicketStats minus) {
        if (scope == Scope.PRINT) {
            this.statsPrintJobs.decrement(minus);
        } else {
            this.statsCopyJobs.decrement(minus);
        }
    }

    /**
     * @return A deep copy.
     */

    public JobTicketQueueInfo getCopy() {

        final JobTicketQueueInfo obj = new JobTicketQueueInfo();

        obj.increment(Scope.COPY, this.statsCopyJobs);
        obj.increment(Scope.PRINT, this.statsPrintJobs);

        return obj;
    }

}
