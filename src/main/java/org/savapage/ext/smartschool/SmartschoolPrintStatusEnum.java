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
package org.savapage.ext.smartschool;

import org.savapage.core.dao.enums.ExternalSupplierStatusEnum;

/**
 * Implementation of {@link ExternalSupplierStatusEnum} with SmartSchool print
 * status with XML string values used in SmartSchool SOAP communication.
 *
 * <p>
 * NOTE: Do NOT changes enum text since they must mirror
 * {@link ExternalSupplierStatusEnum}.
 * </p>
 *
 * @author Rijk Ravestein
 *
 */
public enum SmartschoolPrintStatusEnum {

    /**
     * Cancelled by user. E.g. by PaperCut user RELEASE_STATION_CANCELLED.
     */
    CANCELLED(SmartschoolPrintStatusEnum.XML_CANCELED,
            ExternalSupplierStatusEnum.CANCELLED),

    /**
     * .
     */
    COMPLETED(SmartschoolPrintStatusEnum.XML_COMPLETED,
            ExternalSupplierStatusEnum.COMPLETED),

    /**
     * .
     */
    ERROR(SmartschoolPrintStatusEnum.XML_ERROR,
            ExternalSupplierStatusEnum.ERROR),

    /**
     * A PENDING print expired. E.g. by a PaperCut RELEASE_STATION_TIMEOUT.
     * NOTE: the XML value is 'canceled', since SmartSchool does NOT have a
     * separate EXPIRED status.
     */
    EXPIRED(SmartschoolPrintStatusEnum.XML_CANCELED,
            ExternalSupplierStatusEnum.EXPIRED),

    /**
     * SmartSchool Print request is pending in SavaPage.
     */
    PENDING(SmartschoolPrintStatusEnum.XML_IN_PROGRESS,
            ExternalSupplierStatusEnum.PENDING),

    /**
     * SmartSchool Print request is pending in an external system like PaperCut.
     */
    PENDING_EXT(SmartschoolPrintStatusEnum.XML_IN_PROGRESS,
            ExternalSupplierStatusEnum.PENDING_EXT);

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
     * The {@link ExternalSupplierStatusEnum} equivalent.
     */
    private final ExternalSupplierStatusEnum externalSupplierStatus;

    /**
     *
     * @param text
     *            The text used by SmartSchool.
     */
    private SmartschoolPrintStatusEnum(final String text,
            final ExternalSupplierStatusEnum externalStatus) {
        this.xmlText = text;
        this.externalSupplierStatus = externalStatus;
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
        return this.externalSupplierStatus;
    }

    /**
     *
     * @param status
     * @return
     */
    public static SmartschoolPrintStatusEnum fromGenericStatus(
            final ExternalSupplierStatusEnum status) {

        switch (status) {

        case CANCELLED:
            return SmartschoolPrintStatusEnum.CANCELLED;

        case COMPLETED:
            return SmartschoolPrintStatusEnum.COMPLETED;

        case EXPIRED:
            return SmartschoolPrintStatusEnum.EXPIRED;

        case ERROR:
            return SmartschoolPrintStatusEnum.ERROR;

        case PENDING:
            return SmartschoolPrintStatusEnum.PENDING;

        case PENDING_EXT:
            return SmartschoolPrintStatusEnum.PENDING_EXT;

        default:
            throw new IllegalArgumentException(String.format(
                    "%s.%s cannot be mapped to %s", status.getClass()
                            .getSimpleName(), status.toString(),
                    SmartschoolPrintStatusEnum.class.getSimpleName()));
        }
    }

}
