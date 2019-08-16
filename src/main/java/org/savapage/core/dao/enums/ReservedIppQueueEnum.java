/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2019 Datraverse B.V.
 * Authors: Rijk Ravestein.
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
package org.savapage.core.dao.enums;

import org.savapage.core.jpa.IppQueue;

/**
 * Reserved names for {@link IppQueue#getUrlPath()}.
 * <p>
 * Note for developers: do NOT change the name values, since it will invalidate
 * current database content.
 * </p>
 *
 * @author Rijk Ravestein
 *
 */
public enum ReservedIppQueueEnum {

    /**
     * The AirPrint queue.
     */
    AIRPRINT("airprint", "AirPrint", true),

    /**
     * The default IPP queue for driver printing.
     */
    IPP_PRINT("", "IPP Printer", true),

    /**
     * The default IPP queue for driver printing over public internet.
     */
    IPP_PRINT_INTERNET("internet", "Internet Printer", true),

    /**
     * The dedicated queue for FTP printing.
     */
    FTP("ftp", "FTP Printer", false),

    /**
     * The dedicated queue for driverless Google Cloud printing.
     */
    GCP("gcp", "Google Cloud Printer", false),

    /**
     * The dedicated queue for driverless IMAP mail printing.
     */
    MAILPRINT("mailprint", "Mail Printer", false),

    /**
     * The dedicated raw queue (JetDirect) for driver printing.
     */
    RAW_PRINT("raw", "Raw Printer", true),

    /**
     * The dedicated queue for driverless SmartSchool printing.
     */
    SMARTSCHOOL("smartschool", "SmartSchool Printer", false),

    /**
     * The dedicated queue for driverless Web printing.
     */
    WEBPRINT("webprint", "Web Printer", false),

    /**
     * The dedicated queue for driverless Web Service printing.
     */
    WEBSERVICE("webservice", "Web Service", false);

    /**
     * The URL path as used in the database.
     * <p>
     * Note for developers: do NOT change this value, since it will invalidate
     * current database content.
     * </p>
     */
    private final String urlPath;

    /**
     *
     */
    private final boolean driverPrint;

    /**
    *
    */
    private final String uiText;

    /**
     *
     * @param urlPath
     * @param driverPrint
     */
    private ReservedIppQueueEnum(final String urlPath, final String uiText,
            final boolean driverPrint) {
        this.urlPath = urlPath;
        this.uiText = uiText;
        this.driverPrint = driverPrint;
    }

    /**
     * Gets the unique URL path as used in {@link IppQueue#getUrlPath()} and
     * {@link IppQueue#setUrlPath(String)}.
     *
     * @return The URL path.
     */
    public String getUrlPath() {
        return this.urlPath;
    }

    /**
     * Gets the language-independent UI Text.
     *
     * @return The UI text.
     */
    public String getUiText() {
        return this.uiText;
    }

    /**
     * Checks if this URL path is used for driver printing.
     *
     * @return {@code true} if this queue is used for driver printing.
     */
    public boolean isDriverPrint() {
        return this.driverPrint;
    }

    /**
     *
     * @return {@code true} when this queue can NOT be set to trusted (always
     *         untrusted).
     */
    public boolean isNotTrusted() {
        return this == ReservedIppQueueEnum.AIRPRINT
                || this == ReservedIppQueueEnum.IPP_PRINT_INTERNET;
    }
}
