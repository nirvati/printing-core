/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2014 Datraverse B.V.
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.core.dao;

import java.util.Date;
import java.util.List;

import org.savapage.core.dao.helpers.ReservedIppQueueEnum;
import org.savapage.core.jpa.DocLog;
import org.savapage.core.jpa.IppQueue;

/**
 *
 * @author Datraverse B.V.
 *
 */
public interface IppQueueDao extends GenericDao<IppQueue> {

    /**
     * Field identifiers used for select and sort.
     */
    enum Field {

        /**
         * URL path.
         */
        URL_PATH
    }

    /**
     *
     */
    class ListFilter {

        private String containingText;
        private Boolean trusted;
        private Boolean disabled;
        private Boolean deleted;

        public String getContainingText() {
            return containingText;
        }

        public void setContainingText(String containingText) {
            this.containingText = containingText;
        }

        public Boolean getTrusted() {
            return trusted;
        }

        public void setTrusted(Boolean trusted) {
            this.trusted = trusted;
        }

        public Boolean getDisabled() {
            return disabled;
        }

        public void setDisabled(Boolean disabled) {
            this.disabled = disabled;
        }

        public Boolean getDeleted() {
            return deleted;
        }

        public void setDeleted(Boolean deleted) {
            this.deleted = deleted;
        }

    }

    /**
     * Counts the number of queues according to filter.
     *
     * @param filter
     *            The filter.
     * @return The count.
     */
    long getListCount(final ListFilter filter);

    /**
     * Gets a chunk of queues.
     *
     * @param filter
     *            The filter.
     * @param startPosition
     *            The zero-based start position of the chunk related to the
     *            total number of rows. If {@code null} the chunk starts with
     *            the first row.
     * @param maxResults
     *            The maximum number of rows in the chunk. If {@code null}, then
     *            ALL (remaining rows) are returned.
     * @param orderBy
     *            The sort field.
     * @param sortAscending
     *            {@code true} when sorted ascending.
     * @return The chunk.
     */
    List<IppQueue> getListChunk(final ListFilter filter,
            final Integer startPosition, final Integer maxResults,
            final Field orderBy, final boolean sortAscending);

    /**
     * Finds a reserved {@link IppQueue}.
     *
     * @param reservedQueue
     *            The {@link ReservedIppQueueEnum}.
     * @return The {@link IppQueue}, or {@code null} when not found.
     */
    IppQueue find(ReservedIppQueueEnum reservedQueue);

    /**
     * Finds the queue by its URL path, when not found null is returned.
     *
     * @param urlPath
     *            The unique URL path name of the queue.
     * @return The queue instance, or {@code null} when not found.
     */
    IppQueue findByUrlPath(final String urlPath);

    /**
     * Resets the totals to zero for all {@link IppQueue} instances.
     *
     * @param resetDate
     *            The reset date.
     * @param resetBy
     *            The actor.
     */
    void resetTotals(Date resetDate, String resetBy);

    /**
     * Removes queues (cascade delete) that are <i>logically</i> deleted, and
     * who do <i>not</i> have any related {@link DocLog}.
     *
     * @return The number of removed queues.
     */
    int pruneQueues();

}
