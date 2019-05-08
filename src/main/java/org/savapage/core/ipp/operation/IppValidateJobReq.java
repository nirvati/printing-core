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
package org.savapage.core.ipp.operation;

import java.io.IOException;
import java.io.InputStream;

import org.savapage.core.ipp.IppProcessingException;
import org.savapage.core.ipp.attribute.IppAttrValue;
import org.savapage.core.jpa.User;
import org.savapage.core.print.server.DocContentPrintProcessor;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class IppValidateJobReq extends AbstractIppRequest {

    private DocContentPrintProcessor printInReqHandler = null;

    @Override
    protected void process(final InputStream istr) {
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
            final InputStream istr) throws IOException {

        final String authWebAppUser;

        if (operation.isTrustedUserAsRequester()) {
            authWebAppUser = null;
        } else {
            authWebAppUser = operation.getTrustedIppClientUserId();
        }

        /*
         * Create generic PrintIn handler. This should be a first action because
         * this handler holds the deferred exception.
         */
        printInReqHandler = new DocContentPrintProcessor(operation.getQueue(),
                operation.getRemoteAddr(), null, authWebAppUser);

        /*
         * Then, read the IPP attributes.
         */
        readAttributes(istr);

        /*
         * Then, get the IPP requesting user.
         */
        final String requestingUserId;

        if (operation.isTrustedUserAsRequester()) {
            requestingUserId = operation.getTrustedIppClientUserId();
        } else {
            requestingUserId = this.getRequestingUserName();
        }

        /*
         * Check...
         */
        printInReqHandler.setJobName(getJobName());
        printInReqHandler.processRequestingUser(requestingUserId);
    }

    /**
     *
     * @return
     */
    public String getJobName() {

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

    public IppProcessingException getDeferredException() {
        return (IppProcessingException) printInReqHandler
                .getDeferredException();
    }

    public void setDeferredException(IppProcessingException e) {
        printInReqHandler.setDeferredException(e);
    }

}
