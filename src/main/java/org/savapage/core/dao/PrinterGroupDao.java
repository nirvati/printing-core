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

import org.savapage.core.config.IConfigProp;
import org.savapage.core.jpa.Device;
import org.savapage.core.jpa.PrinterGroup;
import org.savapage.core.jpa.PrinterGroupMember;

/**
 *
 * @author Rijk Ravestein
 *
 */
public interface PrinterGroupDao extends GenericDao<PrinterGroup> {

    /** */
    class ListFilter {

        /** */
        private String containingNameText;

        public String getContainingNameText() {
            return containingNameText;
        }

        public void setContainingNameText(String containingNameText) {
            this.containingNameText = containingNameText;
        }
    }

    /**
     * Deletes {@link PrinterGroup} instances when:
     * <ul>
     * <li>No related {@link PrinterGroupMember} children present.</li>
     * <li>No referrer {@link Device} objects present.</li>
     * <li>No {@link IConfigProp} objects present with
     * {@link IConfigProp.Key#PROXY_PRINT_NON_SECURE} EQ true and
     * {@link IConfigProp.Key#PROXY_PRINT_NON_SECURE_PRINTER_GROUP} EQ not
     * blank.</li>
     * </ul>
     *
     * @return The number of groups deleted.
     */
    int prunePrinterGroups();

    /**
     * Finds (lazy adds) a {@link PrinterGroup}.
     * <p>
     * If the displayName found differs from the one offered, it is updated in
     * the database.
     * </p>
     *
     * @param groupName
     *            Unique case-insensitive name of the printer group.
     * @param displayName
     *            Display name of the printer group.
     * @param requestingUser
     *            The actor (when lazy adding).
     * @param requestDate
     *            The date (when lazy adding).
     * @return The {@link PrinterGroup}.
     */
    PrinterGroup readOrAdd(String groupName, String displayName,
            String requestingUser, Date requestDate);

    /**
     * Finds a {@link PrinterGroup} by name.
     *
     * @param groupName
     *            The unique case-insensitive name of the printer group.
     * @return The printer group object or {@code null} when not found.
     */
    PrinterGroup findByName(String groupName);

    /**
     * @param filter
     *            The filter.
     * @return Number of filtered rows.
     */
    long getListCount(ListFilter filter);

    /**
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
     * @param sortAscending
     *            {@code true} when sorted ascending.
     * @return The chunk.
     */
    List<PrinterGroup> getListChunk(ListFilter filter, Integer startPosition,
            Integer maxResults, boolean sortAscending);

}
