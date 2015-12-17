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
package org.savapage.core.print.smartschool;

import org.savapage.core.SpException;
import org.savapage.core.services.helpers.ExternalSupplierStatusEnum;

/**
 * SmartSchool print status with XML string values used in SmartSchool SOAP
 * communication.
 *
 * <p>
 * NOTE: Do NOT changes enum values since they are stored in the database.
 * </p>
 *
 * @author Datraverse B.V.
 *
 */
public enum SmartSchoolPrintStatusEnum {

    /**
     * Cancelled by user. E.g. by PaperCut user RELEASE_STATION_CANCELLED.
     */
    CANCELLED(SmartSchoolPrintStatusEnum.XML_CANCELED),

    /**
     * .
     */
    COMPLETED(SmartSchoolPrintStatusEnum.XML_COMPLETED),

    /**
     * .
     */
    ERROR(SmartSchoolPrintStatusEnum.XML_ERROR),

    /**
     * A PENDING print expired. E.g. by a PaperCut RELEASE_STATION_TIMEOUT.
     * NOTE: the XML value is 'canceled', since SmartSchool does NOT have a
     * separate EXPIRED status.
     */
    EXPIRED(SmartSchoolPrintStatusEnum.XML_CANCELED),

    /**
     * SmartSchool Print request is pending in SavaPage.
     */
    PENDING(SmartSchoolPrintStatusEnum.XML_IN_PROGRESS),

    /**
     * SmartSchool Print request is pending in an external system like PaperCut.
     */
    PENDING_EXT(SmartSchoolPrintStatusEnum.XML_IN_PROGRESS);

    /**
     * .
     */
    private static final String XML_CANCELED = "canceled";

    /**
     * .
     */
    private static final String XML_COMPLETED = "completed";

    /**
     * .
     */
    private static final String XML_ERROR = "error";

    /**
     * .
     */
    private static final String XML_IN_PROGRESS = "in progress";

    /**
     * The text used by SmartSchool.
     */
    private final String xmlText;

    /**
     *
     * @param text
     *            The text used by SmartSchool.
     */
    private SmartSchoolPrintStatusEnum(final String text) {
        this.xmlText = text;
    }

    /**
     * @return The text used by SmartSchool.
     */
    public String getXmlText() {
        return xmlText;
    }

    /**
     * Maps to the generic {@link ExternalSupplierStatusEnum}.
     *
     * @return The {@link ExternalSupplierStatusEnum}.
     */
    public ExternalSupplierStatusEnum asGenericStatus() {
        switch (this) {
        case CANCELLED:
            return ExternalSupplierStatusEnum.CANCELLED;
        case COMPLETED:
            return ExternalSupplierStatusEnum.COMPLETED;
        case ERROR:
            return ExternalSupplierStatusEnum.ERROR;
        case EXPIRED:
            return ExternalSupplierStatusEnum.EXPIRED;
        case PENDING:
            return ExternalSupplierStatusEnum.PENDING;
        case PENDING_EXT:
            return ExternalSupplierStatusEnum.PENDING_EXT;
        default:
            throw new SpException(String.format("%s.%s cannot be mapped to %s",
                    this.getClass().getSimpleName(), this.toString(),
                    ExternalSupplierStatusEnum.class.getSimpleName()));
        }
    }
}
