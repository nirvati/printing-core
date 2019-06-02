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
package org.savapage.core.services;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.savapage.core.dao.enums.ExternalSupplierEnum;
import org.savapage.core.dao.enums.ExternalSupplierStatusEnum;
import org.savapage.core.imaging.EcoPrintPdfTask;
import org.savapage.core.imaging.EcoPrintPdfTaskPendingException;
import org.savapage.core.jpa.DocLog;
import org.savapage.core.jpa.User;
import org.savapage.core.outbox.OutboxInfoDto;
import org.savapage.core.outbox.OutboxInfoDto.OutboxJobDto;
import org.savapage.core.pdf.PdfCreateInfo;
import org.savapage.core.print.proxy.AbstractProxyPrintReq;
import org.savapage.core.print.proxy.ProxyPrintDocReq;
import org.savapage.core.print.proxy.ProxyPrintInboxReq;
import org.savapage.core.services.helpers.AccountTrxInfoSet;
import org.savapage.core.services.helpers.DocContentPrintInInfo;

/**
 *
 * @author Rijk Ravestein
 *
 */
public interface OutboxService {

    /**
     * Applies locale information to the {@link OutboxInfoDto}.
     *
     * @param outboxInfo
     *            the {@link OutboxInfoDto}.
     * @param locale
     *            The {@link Locale}.
     * @param currencySymbol
     *            The currency symbol.
     */
    void applyLocaleInfo(OutboxInfoDto outboxInfo, Locale locale,
            String currencySymbol);

    /**
     * Checks if the print status of the {@link OutboxJobDto} must be monitored
     * in PaperCut.
     *
     * @param job
     *            The {@link OutboxJobDto} to print.
     * @return {@code true} when print status of the {@link OutboxJobDto} must
     *         be monitored in PaperCut.
     */
    boolean isMonitorPaperCutPrintStatus(OutboxJobDto job);

    /**
     * Creates an {@link OutboxJobDto} from input parameters.
     *
     * @param request
     *            The {@link AbstractProxyPrintReq}.
     * @param submitDate
     *            The date the proxy print was submitted.
     * @param expiryDate
     *            The date the proxy print expires.
     * @param createInfo
     *            The {@link PdfCreateInfo} with the PDF file in the outbox. Is
     *            {@code null} when Copy Job Ticket.
     * @return The {@link OutboxJobDto}.
     */
    OutboxJobDto createOutboxJob(AbstractProxyPrintReq request, Date submitDate,
            Date expiryDate, PdfCreateInfo createInfo);

    /**
     * Cancels all jobs in the user's outbox.
     *
     * @param userId
     *            The unique user id.
     * @return The number of jobs canceled.
     */
    int cancelOutboxJobs(String userId);

    /**
     * Cancels a job in the user's outbox.
     *
     * @param userId
     *            The unique user id.
     * @param fileName
     *            The unique file name of the job to remove.
     * @return {@code false} if the job was not found.
     */
    boolean cancelOutboxJob(String userId, String fileName);

    /**
     * Notifies an outbox job is canceled.
     * <p>
     * NOTE: When the outbox job was created from an
     * {@link ExternalSupplierEnum} other than
     * {@link ExternalSupplierEnum#SAVAPAGE} the print-in {@link DocLog} is
     * updated with {@link ExternalSupplierStatusEnum#PENDING_CANCEL}.
     * </p>
     *
     * @param job
     *            The canceled {@link OutboxJobDto}.
     */
    void onOutboxJobCanceled(final OutboxJobDto job);

    /**
     * Notifies an outbox job is completed.
     * <p>
     * NOTE: When the outbox job was created from an
     * {@link ExternalSupplierEnum} other than
     * {@link ExternalSupplierEnum#SAVAPAGE} the print-in {@link DocLog} is
     * updated with {@link ExternalSupplierStatusEnum#PENDING_COMPLETE}.
     * </p>
     *
     * @param job
     *            The completed {@link OutboxJobDto}.
     */
    void onOutboxJobCompleted(final OutboxJobDto job);

    /**
     * Extends the job expiration time of jobs in the user's outbox, so that
     * each job will NOT expire within n minutes.
     *
     * @param userId
     *            The unique user id.
     * @param minutes
     *            The number of minutes to extend.
     * @return The number of jobs in the outbox whose expiration time was
     *         extended.
     */
    int extendOutboxExpiry(String userId, int minutes);

    /**
     * Reads and prunes the {@link OutboxInfoDto} JSON file from user's outbox
     * directory.
     * <p>
     * NOTE: The JSON file is created when it does not exist.
     * </p>
     *
     * @param userId
     *            The unique user id.
     * @param expiryRef
     *            The reference date for calculating the expiration.
     * @return the {@link OutboxInfoDto} object.
     */
    OutboxInfoDto pruneOutboxInfo(String userId, Date expiryRef);

    /**
     * Gets the full path {@link File} from an outbox file name.
     *
     * @param userId
     *            The unique user id.
     * @param fileName
     *            The file name (without the path).
     *
     * @return The full path {@link File}.
     */
    File getOutboxFile(String userId, String fileName);

    /**
     * Gets the outbox location of a user.
     *
     * @param userId
     *            The unique user id.
     * @return The location.
     */
    File getUserOutboxDir(String userId);

    /**
     * Checks if outbox of a user is present.
     *
     * @param userId
     *            The unique user id.
     * @return {@code true} is outbox exists.
     */
    boolean isOutboxPresent(String userId);

    /**
     * Sends Print Job to the OutBox.
     * <p>
     * Note: invariants are NOT checked.
     * </p>
     *
     * @param lockedUser
     *            The requesting {@link User}, which should be locked.
     * @param request
     *            The {@link ProxyPrintInboxReq}.
     * @throws EcoPrintPdfTaskPendingException
     *             When {@link EcoPrintPdfTask} objects needed for this PDF are
     *             pending.
     */
    void proxyPrintInbox(User lockedUser, ProxyPrintInboxReq request)
            throws EcoPrintPdfTaskPendingException;

    /**
     * Proxy prints a PDF file to the user's outbox.
     * <p>
     * NOTE: The PDF file location is arbitrary and NOT part in the user's
     * inbox.
     * </p>
     *
     * @param lockedUser
     *            The requesting {@link User}, which should be locked.
     * @param request
     *            The {@link ProxyPrintDocReq}.
     * @param createInfo
     *            The {@link PdfCreateInfo} with the arbitrary (non-inbox) PDF
     *            file to print.
     * @param printInfo
     *            The {@link DocContentPrintInInfo}.
     * @throws IOException
     *             When file IO error occurs.
     */
    void proxyPrintPdf(User lockedUser, ProxyPrintDocReq request,
            final PdfCreateInfo createInfo, DocContentPrintInInfo printInfo)
            throws IOException;

    /**
     *
     * @since 0.9.11
     *
     * @param userId
     *            The unique user id.
     * @return The number of outbox jobs.
     */
    int getOutboxJobCount(String userId);

    /**
     * Gets the {@link OutboxJobDto} candidate objects for proxy printing.
     * <p>
     * Note: prunes the {@link OutboxJobDto} instances in {@link OutboxInfoDto}
     * for jobs which are expired for Proxy Printing.
     * </p>
     *
     * @since 0.9.6
     *
     * @param userId
     *            The unique user id.
     * @param printerNames
     *            The unique printer names to get the jobs for.
     * @param expiryRef
     *            The reference date for calculating the expiration.
     * @return A list with {@link OutboxJobDto} candidate objects for proxy
     *         printing.
     */
    List<OutboxJobDto> getOutboxJobs(String userId, Set<String> printerNames,
            Date expiryRef);

    /**
     * Gets the {@link OutboxJobDto} by key (PDF base filename).
     *
     * @param userId
     *            The unique user id.
     * @param pdfFilename
     *            The unique PDF base filename.
     * @return The {@link OutboxJobDto} or {@code null} when not found.
     */
    OutboxJobDto getOutboxJob(String userId, String pdfFilename);

    /**
     * Creates the {@link AccountTrxInfoSet} from the {@link OutboxJobDto}
     * source.
     *
     * @since 0.9.11
     *
     * @param source
     *            The {@link OutboxJobDto}.
     * @return The {@link AccountTrxInfoSet} or {@code null} when the source
     *         does not have transactions.
     */
    AccountTrxInfoSet createAccountTrxInfoSet(OutboxJobDto source);

    /**
     * Gets the {@link OutboxInfoDto} from the user's outbox merged with the
     * user's Job Tickets.
     *
     * <p>
     * NOTE: The {@link OutboxInfoDto} JSON file from the user's outbox is read
     * and pruned, or void created when it does not exist. Job tickets of all
     * users are collected in one central place.
     * </p>
     *
     * @param user
     *            The {@link User}.
     * @param expiryRef
     *            The reference date for calculating the expiration.
     * @return The {@link OutboxInfoDto} object.
     */
    OutboxInfoDto getOutboxJobTicketInfo(User user, Date expiryRef);

}
