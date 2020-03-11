/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2020 Datraverse B.V.
 * Author: Rijk Ravestein.
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
package org.savapage.core.dao;

import java.util.Date;
import java.util.List;

import org.savapage.core.dao.enums.AppLogLevelEnum;
import org.savapage.core.jpa.AppLog;

/**
 *
 * @author Datraverse B.V.
 *
 */
public interface AppLogDao extends GenericDao<AppLog> {

    /**
     * Field identifiers used for select and sort.
     */
    enum Field {
        DATE, LEVEL
    }

    /**
     *
     */
    class ListFilter {

        private AppLogLevelEnum level;
        private Date dateFrom;
        private Date dateTo;
        private String containingText;

        public AppLogLevelEnum getLevel() {
            return level;
        }

        public void setLevel(AppLogLevelEnum level) {
            this.level = level;
        }

        public Date getDateFrom() {
            return dateFrom;
        }

        public void setDateFrom(Date dateFrom) {
            this.dateFrom = dateFrom;
        }

        public Date getDateTo() {
            return dateTo;
        }

        public void setDateTo(Date dateTo) {
            this.dateTo = dateTo;
        }

        public String getContainingText() {
            return containingText;
        }

        public void setContainingText(String containingText) {
            this.containingText = containingText;
        }

    }

    /**
     *
     * @param filter
     * @return
     */
    long getListCount(final ListFilter filter);

    /**
     *
     * @param filter
     * @param startPosition
     * @param maxResults
     * @param orderBy
     * @param sortAscending
     * @return
     */
    List<AppLog> getListChunk(final ListFilter filter,
            final Integer startPosition, final Integer maxResults,
            final Field orderBy, final boolean sortAscending);

    /**
     * Deletes all rows from daysBackInTime and older.
     *
     * @param daysBackInTime
     *            Number of days back in current time (now).
     * @return The number of rows deleted.
     */
    int clean(final int daysBackInTime);

}
