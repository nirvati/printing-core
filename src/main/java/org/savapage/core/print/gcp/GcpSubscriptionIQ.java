/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2019 Datraverse B.V.
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
package org.savapage.core.print.gcp;

import org.jivesoftware.smack.packet.IQ;

/**
 * @deprecated See Mantis #1094.
 *
 * @author Rijk Ravestein
 *
 */
@Deprecated
class GcpSubscriptionIQ extends IQ {

    /**
     *
     */
    private static final String CHILD_ELEMENT_XML = "<subscribe "
            + "xmlns=\"google:push\">" + "<item channel=\"cloudprint.google.com"
            // + "/proxy/"
            // + GoogleCloudPrintTest.PRINTER_PROXY_ID
            + "\" from=\"cloudprint.google.com\"/>" + "</subscribe>";

    private final String bareId;

    public GcpSubscriptionIQ(String bareId) {
        this.bareId = bareId;
    }

    @Override
    public String getTo() {
        return bareId;
    }

    @Override
    public Type getType() {
        return Type.SET;
    }

    /**
     *
     */
    @Override
    public String getChildElementXML() {
        return CHILD_ELEMENT_XML;
    }

}
