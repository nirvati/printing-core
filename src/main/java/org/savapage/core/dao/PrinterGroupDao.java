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
package org.savapage.core.dao;

import java.util.Date;

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

}
