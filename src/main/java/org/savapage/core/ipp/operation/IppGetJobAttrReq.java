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
package org.savapage.core.ipp.operation;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class IppGetJobAttrReq extends AbstractIppRequest {

    /**
     * The logger.
     */
    private static final Logger LOGGER = LoggerFactory
            .getLogger(IppGetJobAttrReq.class);

    /**
     * Group 1: Operation Attributes
     *
     *
     * Natural Language and Character Set:
     *
     * The "attributes-charset" and "attributes-natural-language" attributes as
     * described in section 3.1.4.1.
     *
     *
     * Target:
     *
     * Either (1) the "printer-uri" (uri) plus "job-id" (integer(1:MAX)) or (2)
     * the "job-uri" (uri) operation attribute(s) which define the target for
     * this operation as described in section 3.1.5.
     *
     *
     * Requesting User Name:
     *
     * The "requesting-user-name" (name(MAX)) attribute SHOULD be supplied by
     * the client as described in section 8.3.
     *
     *
     * "requested-attributes" (1setOf keyword):
     *
     * The client OPTIONALLY supplies this attribute. The IPP object MUST
     * support this attribute. It is a set of attribute names and/or attribute
     * group names in whose values the requester is interested. If the client
     * omits this attribute, the IPP object MUST respond as if this attribute
     * had been supplied with a value of 'all'.
     */

    @Override
    void process(final InputStream istr) throws IOException {

        readAttributes(istr);

    }

    /**
     *
     * @return
     */
    public String getJobId() {
        return getAttrValue("job-id").getValues().get(0);
    }

    /**
     *
     * @return
     */
    public String getJobUri() {

        try {
            return IppPrintJobReq.getJobUri(getAttrValue("printer-uri")
                    .getValues().get(0), getJobId());
        } catch (URISyntaxException e) {
            LOGGER.error(e.getMessage());
            return null; // TODO
        }

        /*
         * The UUID as a URN
         */
        // return "urn:uuid:" + myUuidJob.toString();
    }
}
