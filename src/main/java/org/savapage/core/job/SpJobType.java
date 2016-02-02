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
package org.savapage.core.job;

import org.savapage.core.jpa.PrinterGroup;

/**
 *
 * @author Datraverse B.V.
 */
public enum SpJobType {

    /**
     * CUPS event subscription renewal.
     */
    CUPS_SUBS_RENEW,

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
     * Remove {@link PrinterGroup} instances that have no members.
     */
    PRINTER_GROUP_CLEAN,

    /**
     * SmartSchool.
     */
    SMARTSCHOOL_PRINT_MONITOR_JOB,

    /**
     *
     */
    IPP_GET_NOTIFICATIONS,

    /**
     * Mail Print.
     */
    IMAP_LISTENER_JOB,

    /**
     * Google Cloud Print: listen to incoming jobs.
     */
    GCP_LISTENER_JOB,

    /**
     * Google Cloud Print: poll for authentication_code after printer
     * registration (the poll will be a success when the user submits the
     * registration_token of the registered printer within x seconds).
     */
    GCP_POLL_FOR_AUTH_CODE

}
