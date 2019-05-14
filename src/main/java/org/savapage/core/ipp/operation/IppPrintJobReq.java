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
import java.net.URI;
import java.net.URISyntaxException;

import org.savapage.core.dao.enums.DocLogProtocolEnum;
import org.savapage.core.ipp.IppProcessingException;
import org.savapage.core.ipp.attribute.IppAttrValue;
import org.savapage.core.jpa.User;
import org.savapage.core.print.server.DocContentPrintProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 3.2.1.1 Print-Job Request.
 *
 * @author Rijk Ravestein
 *
 */
public final class IppPrintJobReq extends AbstractIppRequest {

    /*
     * Group 1: Operation Attributes
     *
     * Group 2: Job Template Attributes
     *
     * Group 3: Document Content
     */

    private static final Logger LOGGER =
            LoggerFactory.getLogger(IppPrintJobReq.class);

    private DocContentPrintProcessor printInProcessor = null;

    /**
     * Just the attributes, not the print job data.
     *
     * @param operation
     * @param istr
     * @throws IOException
     */
    public void processAttributes(final IppPrintJobOperation operation,
            final InputStream istr) throws IOException {

        final String authWebAppUser;

        if (operation.isTrustedUserAsRequester()) {
            authWebAppUser = null;
        } else {
            authWebAppUser = operation.getTrustedIppClientUserId();
        }

        /*
         * Create generic PrintIn handler. This should be a fist action because
         * this handler holds the deferred exception.
         */
        this.printInProcessor =
                new DocContentPrintProcessor(operation.getQueue(),
                        operation.getOriginatorIp(), null, authWebAppUser);

        this.printInProcessor
                .setIppRoutinglistener(operation.getIppRoutingListener());

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
        printInProcessor.setJobName(getJobName());
        printInProcessor.processRequestingUser(requestingUserId);
    }

    /**
     * Is the trusted user present?
     *
     * @return
     */
    public boolean isTrustedUser() {
        return this.printInProcessor.isTrustedUser();
    }

    @Override
    void process(final InputStream istr) throws IOException {
        printInProcessor.process(istr, DocLogProtocolEnum.IPP, null, null,
                null);
    }

    /**
     *
     * @return The job name.
     */
    public String getJobName() {

        final IppAttrValue ippValue = getAttrValue("job-name");

        if (ippValue == null || ippValue.getValues().isEmpty()) {
            return "";
        }
        return ippValue.getValues().get(0);
    }

    /**
     * Is this an URL used by the client ?!
     *
     * @param printerUri
     * @param jobId
     * @return
     * @throws URISyntaxException
     */
    public static String getJobUri(final String printerUri, final String jobId)
            throws URISyntaxException {

        // TODO: check on max 1023 octets

        /*
         * The Printer object assigns the new Job object a URI which is stored
         * in the "job-uri" Job attribute. This URI is then used by clients as
         * the target for subsequent Job operations. The Printer object
         * generates a Job URI based on its configured security policy and the
         * URI used by the client in the create request.
         */
        final URI uri = new URI(printerUri);

        final StringBuilder jobUri = new StringBuilder(64);

        jobUri.append(uri.getScheme()).append("://").append(uri.getHost());

        if (uri.getPort() != -1) {
            jobUri.append(":").append(uri.getPort());
        }

        final String path = uri.getPath();

        if (path != null && path.length() > 1) {
            jobUri.append(path);
        }

        jobUri.append("/jobs/").append(jobId);

        return jobUri.toString();
    }

    /**
     *
     * @return
     */
    public String getJobUri(final int jobId) {

        try {
            return getJobUri(getAttrValue("printer-uri").getValues().get(0),
                    String.valueOf(jobId));
        } catch (URISyntaxException e) {
            LOGGER.error(e.getMessage());
            return null; // TODO
        }

        /*
         * The UUID as a URN
         */
        // return "urn:uuid:" + myUuidJob.toString();
    }

    /**
     * The user object from the database representing the user who printed this
     * job.
     *
     * @return {@code null} when unknown.
     */
    public User getUserDb() {
        return printInProcessor.getUserDb();
    }

    public boolean hasDeferredException() {
        return printInProcessor.getDeferredException() != null;
    }

    public IppProcessingException getDeferredException() {
        final Exception ex = printInProcessor.getDeferredException();
        if (ex == null) {
            return null;
        }
        if (ex instanceof IppProcessingException) {
            return (IppProcessingException) ex;
        }
        return new IppProcessingException(
                IppProcessingException.StateEnum.INTERNAL_ERROR,
                ex.getMessage(), ex);
    }

    public void setDeferredException(IppProcessingException e) {
        printInProcessor.setDeferredException(e);
    }

    public boolean isDrmViolationDetected() {
        return printInProcessor.isDrmViolationDetected();
    }

    /**
     * Wraps the {@link DocContentPrintProcessor#evaluateErrorState(boolean)}
     * method.
     *
     * @param isAuthorized
     * @throws IppProcessingException
     */
    public void evaluateErrorState(final boolean isAuthorized)
            throws IppProcessingException {

        printInProcessor.evaluateErrorState(isAuthorized);

        if (hasDeferredException()) {
            throw getDeferredException();
        } else if (!isAuthorized) {
            throw new IppProcessingException(
                    IppProcessingException.StateEnum.UNAUTHORIZED,
                    "not authorized");
        }
    }

}
