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
package org.savapage.core.job;

import org.savapage.core.jpa.PrinterGroup;

/**
 *
 * @author Rijk Ravestein
 *
 */
public enum SpJobType {

    /**
     * .
     */
    ATOM_FEED,

    /**
     * CUPS push event subscription renewal.
     */
    CUPS_PUSH_EVENT_SUBS_RENEWAL,

    /**
     * CUPS job synchronization.
     */
    CUPS_SYNC_PRINT_JOBS,

    /**
     * Database backup.
     */
    DB_BACKUP,

    /**
     * Optimize the internal Derby Database.
     */
    DB_DERBY_OPTIMIZE,

    /**
     * Monitors the email outbox.
     */
    EMAIL_OUTBOX_MONITOR,

    /**
     * Monitors PaperCut Print jobs.
     */
    PAPERCUT_PRINT_MONITOR,

    /**
     * Monitors the runtime status.
     */
    SYSTEM_MONITOR,

    /**
     * Synchronize users.
     */
    SYNC_USERS,

    /**
     * Synchronize user groups.
     */
    SYNC_USER_GROUPS,

    /**
     * Check community membership.
     */
    CHECK_MEMBERSHIP_CARD,

    /**
     * Clean the Application Log.
     */
    APP_LOG_CLEAN,

    /**
     * Clean the Document Log.
     */
    DOC_LOG_CLEAN,

    /**
     * Clean the Document Store(s).
     */
    DOC_STORE_CLEAN,

    /**
     * Clean User Home directories.
     */
    USER_HOME_CLEAN,

    /**
     * Remove {@link PrinterGroup} instances that have no members.
     */
    PRINTER_GROUP_CLEAN,

    /**
     *
     */
    PRINTER_SNMP,

    /**
     * Mail Print.
     */
    IMAP_LISTENER_JOB,

}
