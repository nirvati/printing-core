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

import java.util.List;

import org.savapage.core.jpa.PrinterGroup;
import org.savapage.core.jpa.PrinterGroupMember;

/**
 *
 * @author Rijk Ravestein
 *
 */
public interface PrinterGroupMemberDao extends GenericDao<PrinterGroupMember> {

    /**
     * Gets the list of Printer Groups where Job Ticket Printers are member of.
     *
     * @return {@link PrinterGroup} list.
     */
    List<PrinterGroup> getGroupsWithJobTicketMembers();

    /**
     * Gets the list of Printer Groups where printer is member of.
     *
     * @param printerName
     *            Printer name (case insensitive).
     * @return {@link PrinterGroup} list.
     */
    List<PrinterGroup> getGroups(String printerName);
}
