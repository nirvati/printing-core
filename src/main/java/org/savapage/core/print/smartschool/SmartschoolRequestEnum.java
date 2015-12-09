/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2015 Datraverse B.V.
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

/**
 *
 * @author Rijk Ravestein
 *
 */
public enum SmartschoolRequestEnum {

    /**
     * .
     */
    GET_DOCUMENT("getDocument", "getDocumentResponse"),

    /**
     * .
     */
    GET_PRINTJOBS("getPrintJobs", "getPrintJobsResponse"),

    /**
     * .
     */
    SET_DOCUMENTSTATUS("setDocumentStatus", "setDocumentStatusResponse");

    /**
     * The identifying name in the SOAP request.
     */
    private final String soapName;

    /**
     * The identifying name in the SOAP response.
     */
    private final String soapNameResponse;

    /**
     * @param req
     *            The identifying name in the SOAP Request.
     * @param rsp
     *            The identifying name in the SOAP Response.
     */
    private SmartschoolRequestEnum(final String req, final String rsp) {
        this.soapName = req;
        this.soapNameResponse = rsp;
    }

    /**
     * @return The identifying name in the SOAP Request.
     */
    public String getSoapName() {
        return this.soapName;
    }

    /**
     * @return The identifying name in the SOAP Response.
     */
    public String getSoapNameResponse() {
        return this.soapNameResponse;
    }

    /**
     * Gets the {@link SmartschoolRequestEnum} from the SOAP Request name.
     *
     * @param soapName
     *            Tthe SOAP Request name.
     * @return {@code null} when not found.
     */
    public static SmartschoolRequestEnum fromSoapName(final String soapName) {

        if (soapName.equalsIgnoreCase(SmartschoolRequestEnum.GET_PRINTJOBS
                .getSoapName())) {
            return SmartschoolRequestEnum.GET_PRINTJOBS;
        }
        if (soapName.equalsIgnoreCase(SmartschoolRequestEnum.GET_DOCUMENT
                .getSoapName())) {
            return SmartschoolRequestEnum.GET_DOCUMENT;
        }
        if (soapName.equalsIgnoreCase(SmartschoolRequestEnum.SET_DOCUMENTSTATUS
                .getSoapName())) {
            return SmartschoolRequestEnum.SET_DOCUMENTSTATUS;
        }
        return null;
    }
}
