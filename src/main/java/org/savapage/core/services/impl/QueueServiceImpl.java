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
package org.savapage.core.services.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.savapage.core.SpException;
import org.savapage.core.concurrent.ReadWriteLockEnum;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.dao.enums.DocLogProtocolEnum;
import org.savapage.core.dao.enums.IppQueueAttrEnum;
import org.savapage.core.dao.enums.ReservedIppQueueEnum;
import org.savapage.core.doc.DocContent;
import org.savapage.core.doc.DocContentTypeEnum;
import org.savapage.core.fonts.InternalFontFamilyEnum;
import org.savapage.core.jpa.IppQueue;
import org.savapage.core.jpa.IppQueueAttr;
import org.savapage.core.jpa.User;
import org.savapage.core.json.JsonRollingTimeSeries;
import org.savapage.core.json.TimeSeriesInterval;
import org.savapage.core.print.server.DocContentPrintException;
import org.savapage.core.print.server.DocContentPrintProcessor;
import org.savapage.core.print.server.DocContentPrintReq;
import org.savapage.core.print.server.DocContentPrintRsp;
import org.savapage.core.print.server.PrintInResultEnum;
import org.savapage.core.print.server.UnsupportedPrintJobContent;
import org.savapage.core.services.QueueService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.util.InetUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Datraverse B.V.
 *
 */
public final class QueueServiceImpl extends AbstractService implements
        QueueService {

    /**
     * .
     */
    private static final Logger LOGGER = LoggerFactory
            .getLogger(QueueServiceImpl.class);

    @Override
    public boolean isRawPrintQueue(final IppQueue queue) {
        return queue.getUrlPath().equals(
                ReservedIppQueueEnum.RAW_PRINT.getUrlPath());
    }

    @Override
    public boolean isGcpPrintQueue(final IppQueue queue) {
        return queue.getUrlPath().equals(ReservedIppQueueEnum.GCP.getUrlPath());
    }

    @Override
    public boolean isMailPrintQueue(final IppQueue queue) {
        return queue.getUrlPath().equals(
                ReservedIppQueueEnum.MAILPRINT.getUrlPath());
    }

    @Override
    public boolean isSmartSchoolQueue(final IppQueue queue) {
        return queue.getUrlPath().equals(
                ReservedIppQueueEnum.SMARTSCHOOL.getUrlPath());
    }

    @Override
    public boolean isWebPrintQueue(final IppQueue queue) {
        return queue.getUrlPath().equals(
                ReservedIppQueueEnum.WEBPRINT.getUrlPath());
    }

    @Override
    public String getAttributeValue(final IppQueue queue, final String name) {

        for (final IppQueueAttr attr : queue.getAttributes()) {

            if (attr.getName().equals(name)) {
                return attr.getValue();
            }
        }
        return null;
    }

    @Override
    public void setLogicalDeleted(final IppQueue queue, final Date deletedDate,
            final String deletedBy) {
        queue.setDeleted(true);
        queue.setDeletedDate(deletedDate);
        queue.setModifiedBy(deletedBy);
        queue.setModifiedDate(deletedDate);
    }

    @Override
    public void undoLogicalDeleted(final IppQueue queue) {
        queue.setDeleted(false);
        queue.setDeletedDate(null);
    }

    @Override
    public void addJobTotals(final IppQueue queue, final Date jobDate,
            final int jobPages, final long jobBytes) {

        queue.setTotalJobs(queue.getTotalJobs().intValue() + 1);
        queue.setTotalPages(queue.getTotalPages().intValue() + jobPages);
        queue.setTotalBytes(queue.getTotalBytes().longValue() + jobBytes);

        queue.setLastUsageDate(jobDate);
    }

    @Override
    public void logPrintIn(final IppQueue queue, final Date observationTime,
            final Integer jobPages) {

        try {
            addTimeSeriesDataPoint(queue,
                    IppQueueAttrEnum.PRINT_IN_ROLLING_DAY_PAGES,
                    observationTime, jobPages);

        } catch (IOException e) {
            throw new SpException("logPrintIn failed", e);
        }

    }

    /**
     * Adds an observation to a time series.
     *
     * @param queue
     *            The {@link IppQueue} to add the observation to.
     * @param attrEnum
     *            The {@link IppQueueAttrEnum}.
     * @param observationTime
     *            The observation time.
     * @param observation
     *            The observation.
     * @throws IOException
     *             When JSON error.
     */
    private void addTimeSeriesDataPoint(final IppQueue queue,
            final IppQueueAttrEnum attrEnum, final Date observationTime,
            final Integer observation) throws IOException {

        final JsonRollingTimeSeries<Integer> statsPages =
                new JsonRollingTimeSeries<>(TimeSeriesInterval.DAY,
                        MAX_TIME_SERIES_INTERVALS_DAYS, 0);

        final IppQueueAttr attr =
                ippQueueAttrDAO().findByName(queue.getId(), attrEnum);

        String json = null;

        if (attr != null) {
            json = attr.getValue();
        }

        if (StringUtils.isNotBlank(json)) {
            statsPages.init(json);
        }

        statsPages.addDataPoint(observationTime, observation);

        setAttrValue(attr, queue, attrEnum, statsPages.stringify());
    }

    /**
     * Writes (create or update) the attribute value to the database.
     *
     * @param attr
     *            The {@link IppQueueAttr}. If {@code null} a new attribute is
     *            created.
     * @param queue
     *            The {@link IppQueue}.
     * @param attrName
     *            The {@link IppQueueAttrEnum}.
     * @param attrValue
     *            The {@link IppQueueAttr} value.
     */
    private void setAttrValue(final IppQueueAttr attr, final IppQueue queue,
            final IppQueueAttrEnum attrName, final String attrValue) {

        if (attr == null) {

            final IppQueueAttr attrNew = new IppQueueAttr();

            attrNew.setQueue(queue);

            attrNew.setName(attrName.getDbName());
            attrNew.setValue(attrValue);

            ippQueueAttrDAO().create(attrNew);

        } else {

            attr.setValue(attrValue);

            ippQueueAttrDAO().update(attr);
        }
    }

    @Override
    public IppQueue getOrCreateReservedQueue(
            final ReservedIppQueueEnum reservedQueue) {

        IppQueue queue = ippQueueDAO().find(reservedQueue);

        if (queue == null) {
            queue = createQueueDefault(reservedQueue.getUrlPath());
            ippQueueDAO().create(queue);

        } else if (reservedQueue == ReservedIppQueueEnum.AIRPRINT
                || reservedQueue == ReservedIppQueueEnum.IPP_PRINT_INTERNET) {

            // Force to untrusted.
            if (queue.getTrusted()) {
                queue.setTrusted(Boolean.FALSE);
                ippQueueDAO().update(queue);
            }
        }

        return queue;
    }

    /**
     * Creates a default {@link IppQueue}.
     *
     * @param urlPath
     *            The URL path.
     * @return The {@link IppQueue}.
     */
    private static IppQueue createQueueDefault(final String urlPath) {

        final IppQueue queue = new IppQueue();

        queue.setUrlPath(urlPath);
        queue.setTrusted(Boolean.FALSE);
        queue.setCreatedBy(ServiceContext.getActor());
        queue.setCreatedDate(ServiceContext.getTransactionDate());

        return queue;
    }

    @Override
    public void lazyCreateReservedQueues() {

        final boolean hasSmartSchoolModule =
                ConfigManager.isSmartSchoolPrintModuleActivated();

        final boolean hasFtpModule = ConfigManager.isFtpPrintActivated();

        for (final ReservedIppQueueEnum value : ReservedIppQueueEnum.values()) {

            if (value == ReservedIppQueueEnum.FTP && !hasFtpModule) {
                continue;
            }

            if (value == ReservedIppQueueEnum.SMARTSCHOOL
                    && !hasSmartSchoolModule) {
                continue;
            }

            this.getOrCreateReservedQueue(value);
        }
    }

    @Override
    public ReservedIppQueueEnum getReservedQueue(final String urlPath) {

        for (final ReservedIppQueueEnum value : ReservedIppQueueEnum.values()) {
            if (value.getUrlPath().equalsIgnoreCase(urlPath)) {
                return value;
            }
        }
        return null;
    }

    @Override
    public boolean isReservedQueue(final String urlPath) {
        return this.getReservedQueue(urlPath) != null;
    }

    @Override
    public DocContentPrintRsp printDocContent(final String queueName,
            final User user, final DocContentPrintReq printReq,
            final InputStream istrContent) throws DocContentPrintException {

        final DocLogProtocolEnum protocol = printReq.getProtocol();
        final String originatorEmail = printReq.getOriginatorEmail();
        final String originatorIp = printReq.getOriginatorIp();
        final String title = printReq.getTitle();
        final String fileName = printReq.getFileName();
        final DocContentTypeEnum contentType = printReq.getContentType();
        final InternalFontFamilyEnum preferredOutputFont =
                printReq.getPreferredOutputFont();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Printing [" + fileName + "] ...");
        }

        final DocContentPrintRsp printRsp = new DocContentPrintRsp();
        final IppQueue queue;

        DocContentPrintProcessor processor = null;

        boolean isAuthorized = false;

        ReadWriteLockEnum.DATABASE_READONLY.setReadLock(true);

        try {
            /*
             * Get the Queue object.
             */
            queue = ippQueueDAO().findByUrlPath(queueName);

            /*
             * Create the request.
             */
            processor =
                    new DocContentPrintProcessor(queue, originatorIp, title,
                            null);

            /*
             * If we tracked the user down by his email address, we know he
             * already exists in the database, so a lazy user insert is no issue
             * (same argument for a Web Print).
             */
            processor.processRequestingUser(user.getUserId());

            isAuthorized = processor.isAuthorized();

            if (isAuthorized) {

                if (contentType != null && DocContent.isSupported(contentType)) {

                    /*
                     * Process content stream, write DocLog and move PDF to
                     * inbox.
                     */
                    processor.process(istrContent, protocol, originatorEmail,
                            contentType, preferredOutputFont);

                    /*
                     * Fill response.
                     */
                    printRsp.setResult(PrintInResultEnum.OK);
                    printRsp.setDocumentUuid(processor.getUuidJob());

                } else {
                    throw new UnsupportedPrintJobContent("File type ["
                            + fileName + "] NOT supported.");
                }

            } else {
                printRsp.setResult(PrintInResultEnum.USER_NOT_AUTHORIZED);

                LOGGER.warn("User [" + user.getUserId() + "] NOT authorized.");
            }

        } catch (Exception e) {
            if (processor != null) {
                processor.setDeferredException(e);
            } else {
                throw new SpException(e.getMessage(), e);
            }
        } finally {
            ReadWriteLockEnum.DATABASE_READONLY.setReadLock(false);
        }

        /*
         * Get the deferred exception before evaluating the error state, because
         * deferred exceptions get nullified while being evaluated.
         */
        final DocContentPrintException printException;

        if (processor.hasDeferredException()) {
            final Exception e = processor.getDeferredException();
            printException = new DocContentPrintException(e.getMessage(), e);
        } else {
            if (processor.isDrmViolationDetected()) {
                printException =
                        new DocContentPrintException("Input is DRM restricted.");
            } else {
                printException = null;
            }
        }

        /*
         * Evaluate to trigger the message handling.
         */
        processor.evaluateErrorState(isAuthorized);

        /*
         * Throw deferred exception.
         */
        if (printException != null) {
            throw printException;
        }

        return printRsp;
    }

    @Override
    public boolean hasClientIpAccessToQueue(final IppQueue queue,
            final String printerNameForLogging, final String clientIpAddr) {

        /*
         * Assume remote host has NO access to printing.
         */
        boolean hasPrintAccessToQueue = false;
        boolean isTrustedQueue = false;

        /*
         * Is IppQueue present ... and can it be used?
         */
        if (queue == null || queue.getDeleted()) {
            throw new SpException(String.format("No queue found for [%s]",
                    printerNameForLogging));
        }

        if (queue.getDisabled()) {
            throw new SpException(String.format("queue [%s] is disabled.",
                    queue.getUrlPath()));
        }

        /*
         * Can the queue be trusted?
         */
        isTrustedQueue = queue.getTrusted();

        /*
         * Check if client IP address is inrange of the allowed IP CIDR.
         *
         * A single IPv4 CIDR for now...
         */
        final String ipAllowed = queue.getIpAllowed();

        hasPrintAccessToQueue =
                InetUtils.isIp4AddrInCidrRanges(ipAllowed, clientIpAddr);

        /*
         * Logging
         */
        if (LOGGER.isDebugEnabled()) {

            final StringBuilder msg = new StringBuilder(64);

            msg.append("Queue [").append(queue.getUrlPath()).append("] ");

            if (isTrustedQueue) {
                msg.append("TRUSTED");
            } else {
                msg.append("NOT Trusted");
            }
            msg.append(". [").append(clientIpAddr).append("] ");
            if (hasPrintAccessToQueue) {
                msg.append("ALLOWED");
            } else {
                msg.append("NOT Allowed");
            }

            LOGGER.debug(msg.toString());
        }

        return hasPrintAccessToQueue;
    }

    @Override
    public boolean isQueueEnabled(final ReservedIppQueueEnum queue) {
        return !ippQueueDAO().find(queue).getDisabled();
    }

}
