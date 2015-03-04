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

import org.savapage.core.ipp.attribute.IppAttrValue;
import org.savapage.core.jpa.User;
import org.savapage.core.print.server.DocContentPrintProcessor;

/**
 *
 * @author rijk
 *
 */
public class IppValidateJobReq extends AbstractIppRequest {

    // private Logger myLogger = LoggerFactory.getLogger(this.getClass());

    private DocContentPrintProcessor printInReqHandler = null;

    @Override
    void process(final InputStream istr) throws IOException {
        // no code intended
    }

    /**
     * Just the attributes, not the print job data.
     *
     * @param operation
     * @param istr
     * @throws Exception
     */
    public void processAttributes(final IppValidateJobOperation operation,
            final InputStream istr) throws Exception {

        /*
         * Create generic PrintIn handler. This should be a first action because
         * this handler holds the deferred exception.
         */
        printInReqHandler =
                new DocContentPrintProcessor(operation.getQueue(),
                        operation.getRemoteAddr(), null,
                        operation.getAuthWebAppUser());

        /*
         * Read the IPP attributes.
         */
        readAttributes(istr);

        /*
         * Check...
         */
        printInReqHandler.setJobName(getJobName());
        printInReqHandler.processRequestingUser(getRequestingUserName());

    }

    /**
     *
     * @return
     */
    public final String getJobName() {

        final IppAttrValue ippValue = getAttrValue("job-name");

        if (ippValue == null || ippValue.getValues().isEmpty()) {
            return "";
        }
        return ippValue.getValues().get(0);
    }

    /**
     * The user object from the database representing the user who printed this
     * job.
     *
     * @return {@code null} when unknown.
     */
    public User getUserDb() {
        return printInReqHandler.getUserDb();
    }

    /**
     * Is the trusted user present?
     *
     * @return
     */
    public boolean isTrustedUser() {
        return this.printInReqHandler.isTrustedUser();
    }

    public boolean hasDeferredException() {
        return printInReqHandler.getDeferredException() != null;
    }

    public Exception getDeferredException() {
        return printInReqHandler.getDeferredException();
    }

    public void setDeferredException(Exception e) {
        printInReqHandler.setDeferredException(e);
    }

}
