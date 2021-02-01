/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2020 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: © 2020 Datraverse B.V. <info@datraverse.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.SortedSet;

import org.savapage.core.dao.enums.ACLRoleEnum;
import org.savapage.core.doc.store.DocStoreException;
import org.savapage.core.dto.JobTicketDomainDto;
import org.savapage.core.dto.JobTicketLabelDto;
import org.savapage.core.dto.JobTicketTagDto;
import org.savapage.core.dto.JobTicketUseDto;
import org.savapage.core.dto.RedirectPrinterDto;
import org.savapage.core.imaging.EcoPrintPdfTask;
import org.savapage.core.imaging.EcoPrintPdfTaskPendingException;
import org.savapage.core.ipp.client.IppConnectException;
import org.savapage.core.ipp.helpers.IppOptionMap;
import org.savapage.core.ipp.rules.IppRuleValidationException;
import org.savapage.core.jpa.DocLog;
import org.savapage.core.jpa.PrintOut;
import org.savapage.core.jpa.Printer;
import org.savapage.core.jpa.User;
import org.savapage.core.outbox.OutboxInfoDto.OutboxJobBaseDto;
import org.savapage.core.outbox.OutboxInfoDto.OutboxJobDto;
import org.savapage.core.pdf.PdfCreateInfo;
import org.savapage.core.print.proxy.ProxyPrintDocReq;
import org.savapage.core.print.proxy.ProxyPrintInboxReq;
import org.savapage.core.print.proxy.TicketJobSheetDto;
import org.savapage.core.services.helpers.DocContentPrintInInfo;
import org.savapage.core.services.helpers.JobTicketExecParms;
import org.savapage.core.services.helpers.JobTicketQueueInfo;
import org.savapage.core.services.helpers.JobTicketWrapperDto;

/**
 *
 * @author Rijk Ravestein
 *
 */
public interface JobTicketService extends StatefulService {

    /** */
    class JobTicketFilter {

        /**
         * The {@link User} database key.
         */
        private Long userId;

        /**
         * Part of a ticket id as case-insensitive search argument.
         */
        private String searchTicketId;

        /**
         * Primary DB key of job ticket printer group.
         */
        private Long printerGroupID;

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public String getSearchTicketId() {
            return searchTicketId;
        }

        public void setSearchTicketId(String searchTicketId) {
            this.searchTicketId = searchTicketId;
        }

        public Long getPrinterGroupID() {
            return printerGroupID;
        }

        public void setPrinterGroupID(Long printerGroupID) {
            this.printerGroupID = printerGroupID;
        }

    }

    /**
     * Sends Copy Job to the OutBox.
     * <p>
     * Note: invariants are NOT checked.
     * </p>
     *
     * @param user
     *            The requesting {@link User}.
     * @param request
     *            The {@link ProxyPrintInboxReq}.
     * @param deliveryDate
     *            The requested date of delivery.
     * @param label
     *            The label(to be pre-pended to the generated ticket number).
     *            Can be {@code null} or empty.
     * @return The job ticket created.
     */
    OutboxJobDto createCopyJob(User user, ProxyPrintInboxReq request,
            Date deliveryDate, JobTicketLabelDto label);

    /**
     * Reopens a Job Ticket for extra copies.
     *
     * @param docLog
     *            The original {@link DocLog}.
     * @return The job ticket created.
     * @throws IOException
     *             If IO error.
     * @throws DocStoreException
     *             If original ticket does not exist.
     */
    OutboxJobDto reopenTicketForExtraCopies(DocLog docLog)
            throws IOException, DocStoreException;

    /**
     * Sends Job Ticket(s) to the OutBox.
     * <p>
     * Note: invariants are NOT checked.
     * </p>
     *
     * @param lockedUser
     *            The requesting {@link User}, which should be locked.
     * @param request
     *            The {@link ProxyPrintInboxReq}.
     * @param deliveryDate
     *            The requested date of delivery.
     * @param label
     *            The label (to be pre-pended to the generated ticket number).
     *            Can be {@code null} or empty.
     * @return The job tickets created.
     *
     * @throws EcoPrintPdfTaskPendingException
     *             When {@link EcoPrintPdfTask} objects needed for this PDF are
     *             pending.
     */
    List<OutboxJobDto> proxyPrintInbox(User lockedUser,
            ProxyPrintInboxReq request, Date deliveryDate,
            JobTicketLabelDto label) throws EcoPrintPdfTaskPendingException;

    /**
     * Sends Print Job to the OutBox.
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
     * @param deliveryDate
     *            The requested date of delivery.
     * @param label
     *            The label (to be pre-pended to the generated ticket number).
     *            Can be {@code null} or empty.
     * @throws IOException
     *             When file IO error occurs.
     */
    void proxyPrintPdf(User lockedUser, ProxyPrintDocReq request,
            PdfCreateInfo createInfo, DocContentPrintInInfo printInfo,
            Date deliveryDate, JobTicketLabelDto label) throws IOException;

    /**
     * Creates and formats a unique ticket number.
     *
     * @return The formatted ticket number.
     */
    String createTicketNumber();

    /**
     *
     * @return Ticket Queue information.
     */
    JobTicketQueueInfo getTicketQueueInfo();

    /**
     * Gets the pending Job Tickets.
     *
     * @param filter
     *            The filter.
     * @return The Job Tickets.
     */
    List<OutboxJobDto> getTickets(JobTicketFilter filter);

    /**
     * Gets the pending Job Tickets numbers.
     *
     * @param filter
     *            The filter.
     * @param maxItems
     *            Max items to return.
     * @return The Job Tickets numbers.
     */
    List<String> getTicketNumbers(JobTicketFilter filter, int maxItems);

    /**
     * Gets the pending Job Ticket.
     *
     * @param fileName
     *            The unique PDF file name of the job (no path).
     * @return The Job Ticket or {@code null} when not found.
     */
    OutboxJobDto getTicket(String fileName);

    /**
     * Gets the pending Job Ticket belonging to a {@link User} job file.
     *
     * @param userId
     *            The {@link User} database key.
     * @param fileName
     *            The unique PDF file name of the job (no path).
     * @return The Job Ticket or {@code null} when not found, or not owned by
     *         this user.
     */
    OutboxJobDto getTicket(Long userId, String fileName);

    /**
     * Cancels the pending Job Tickets of a {@link User} that are waiting to be
     * printed (no redirect printer is assigned yet).
     *
     * @param userId
     *            The {@link User} database key.
     * @return The number of Job Tickets removed.
     */
    int cancelTickets(Long userId);

    /**
     * Cancels a Job Ticket with an extra user check.
     *
     * @param userId
     *            The {@link User} database key of the ticket owner.
     * @param fileName
     *            The unique PDF file name of the job to remove.
     * @return The removed ticket of {@code null} when ticket was not found.
     * @throws IllegalArgumentException
     *             When Job Ticket is not owned by user.
     */
    OutboxJobDto cancelTicket(Long userId, String fileName);

    /**
     * Cancels a Job Ticket.
     *
     * @param fileName
     *            The unique PDF file name of the job to remove.
     * @return The removed ticket or {@code null} when ticket was not found.
     */
    OutboxJobDto cancelTicket(String fileName);

    /**
     * Cancels a Job Ticket proxy print.
     *
     * @param fileName
     *            The unique PDF file name of the job ticket.
     * @return {@link Boolean#TRUE} when cancelled, {@link Boolean#FALSE} when
     *         cancellation failed, or {@code null} when ticket was not found.
     */
    Boolean cancelTicketPrint(String fileName);

    /**
     * Gets the {@link PrintOut} of a job ticket.
     *
     * @param fileName
     *            The unique PDF file name of the job ticket.
     * @return The {@link PrintOut} or {@code null} when not present.
     * @throws FileNotFoundException
     *             When ticket is not found.
     */
    PrintOut getTicketPrintOut(String fileName) throws FileNotFoundException;

    /**
     * Closes a Job Ticket after proxy print.
     *
     * @param fileName
     *            The unique PDF file name of the job to remove.
     * @return The closed ticket or {@code null} when ticket was not found.
     */
    OutboxJobDto closeTicketPrint(String fileName);

    /**
     * Updates Job Ticket Printer Groups in ticket cache.
     */
    void updatePrinterGroupIDs();

    /**
     * Updates Job Ticket Printer Groups of printer in ticket cache.
     *
     * @param printer
     *            Printer.
     */
    void updatePrinterGroupIDs(Printer printer);

    /**
     * Updates a Job Ticket.
     *
     * @param dto
     *            The ticket.
     * @return {@code true} when found and updated, {@code false} when not
     *         found.
     * @throws IOException
     *             When file IO error occurs.
     */
    boolean updateTicket(OutboxJobDto dto) throws IOException;

    /**
     * Updates a Job Ticket.
     *
     * @param wrapper
     *            The ticket wrapper.
     * @return {@code true} when found and updated, {@code false} when not
     *         found.
     * @throws IOException
     *             When file IO error occurs.
     */
    boolean updateTicket(JobTicketWrapperDto wrapper) throws IOException;

    /**
     * Notifies Job Ticket owner (by email) that ticket is completed.
     *
     * @param dto
     *            The {@link OutboxJobBaseDto}.
     * @param operator
     *            The user id of the Job Ticket Operator.
     * @param user
     *            The Job Ticket owner.
     * @param locale
     *            The locale for the email text.
     * @return The email address, or {@code null} when not send.
     */
    String notifyTicketCompletedByEmail(OutboxJobBaseDto dto, String operator,
            User user, Locale locale);

    /**
     * Notifies Job Ticket owner (by email) that ticket is canceled.
     *
     * @param dto
     *            The {@link OutboxJobBaseDto}.
     * @param operator
     *            The user id of the Job Ticket Operator.
     * @param user
     *            The Job Ticket owner.
     * @param reason
     *            Reason for cancellation.
     * @param locale
     *            The locale for the email text.
     *
     * @return The email address, or {@code null} when not send.
     */
    String notifyTicketCanceledByEmail(OutboxJobBaseDto dto, String operator,
            User user, String reason, Locale locale);

    /**
     * Prints and settles a Job Ticket.
     *
     * @param parms
     *            The parameters.
     *
     * @return The printed ticket or {@code null} when ticket was not found.
     * @throws IOException
     *             When IO error.
     * @throws IppConnectException
     *             When connection to CUPS fails.
     * @throws IppRuleValidationException
     *             When IPP constraint violations.
     */
    OutboxJobDto printTicket(JobTicketExecParms parms)
            throws IOException, IppConnectException, IppRuleValidationException;

    /**
     * Retries a Job Ticket Print (typically after a job is cancelled, due to
     * printer failure). Note: this method does <i>not</i> settle the ticket,
     * since it is assumed this is already done at the first print trial.
     *
     * @param parms
     *            The parameters.
     *
     * @return The printed ticket or {@code null} when ticket was not found.
     * @throws IOException
     *             When IO error.
     * @throws IppConnectException
     *             When connection to CUPS fails.
     * @throws IppRuleValidationException
     *             When IPP constraint violations.
     */
    OutboxJobDto retryTicketPrint(JobTicketExecParms parms)
            throws IOException, IppConnectException, IppRuleValidationException;

    /**
     * Settles a Job Ticket without printing it.
     *
     * @param operator
     *            The {@link User#getUserId()} with
     *            {@link ACLRoleEnum#JOB_TICKET_OPERATOR}.
     * @param printer
     *            The redirect printer.
     * @param fileName
     *            The unique PDF file name of the job to print.
     * @return The printed ticket or {@code null} when ticket was not found.
     * @throws IOException
     *             When IO error.
     */
    OutboxJobDto settleTicket(String operator, Printer printer, String fileName)
            throws IOException;

    /**
     * @param number
     *            Ticket number.
     * @return {@code true} if present.
     */
    boolean isTicketNumberPresent(String number);

    /**
     * Checks if job represents a reopened Job Ticket.
     *
     * @param job
     *            The Job Ticket.
     * @return {@code true} when this is a Reopened Job Ticket.
     */
    boolean isReopenedTicket(OutboxJobDto job);

    /**
     * Checks if job ticket number represents a reopened Job Ticket.
     *
     * @param ticketNumber
     *            The Job Ticket number (can be {@code null}).
     * @return {@code true} when ticket number represents a reopened Job Ticket.
     */
    boolean isReopenedTicketNumber(String ticketNumber);

    /**
     * Checks if job ticket number is active as reopened Job Ticket.
     *
     * @param ticketNumber
     *            The Job Ticket number.
     * @return {@code true} when ticket number is active as reopened Job Ticket.
     */
    boolean isTicketReopened(String ticketNumber);

    /**
     * Gets the list of {@link RedirectPrinterDto} compatible printers for a Job
     * Ticket.
     *
     * @param job
     *            The Job Ticket.
     * @param optionFilter
     *            An additional filter, apart from the Job Ticket specification,
     *            of IPP option values that must be present in the redirect
     *            printers.
     * @param locale
     *            The {@link Locale} for UI texts.
     * @return The list of redirect printers (can be empty) or {@code null} when
     *         job ticket is not found.
     */
    List<RedirectPrinterDto> getRedirectPrinters(OutboxJobDto job,
            IppOptionMap optionFilter, Locale locale);

    /**
     * Gets a {@link RedirectPrinterDto} compatible printer for a Job Ticket.
     *
     * @param fileName
     *            The unique PDF file name of the job ticket.
     * @param optionFilter
     *            An additional filter, apart from the Job Ticket specification,
     *            of IPP option values that must be present in the redirect
     *            printer.
     * @param locale
     *            The {@link Locale} for UI texts.
     * @return The redirect printer or {@code null} when no job ticket or
     *         printer is found.
     */
    RedirectPrinterDto getRedirectPrinter(String fileName,
            IppOptionMap optionFilter, Locale locale);

    /**
     * Creates a single page PDF Job Sheet file.
     *
     * @param user
     *            The unique user id.
     * @param jobDto
     *            The {@link OutboxJobDto} job ticket.
     * @param jobSheetDto
     *            Job Sheet information.
     * @return The PDF file.
     */
    File createTicketJobSheet(String user, OutboxJobDto jobDto,
            TicketJobSheetDto jobSheetDto);

    /**
     *
     * @return The number of tickets in queue.
     */
    int getJobTicketQueueSize();

    /**
     *
     * @param options
     *            The Ticket options.
     * @return Job Sheet info for Job Ticket.
     */
    TicketJobSheetDto getTicketJobSheet(IppOptionMap options);

    /**
     * Get the ticket domains from cache, sorted by domain name.
     *
     * @return The sorted domains, or empty when no domains are defined or
     *         domains are disabled.
     */
    Collection<JobTicketDomainDto> getTicketDomainsByName();

    /**
     * Get the ticket uses from cache, sorted by use name.
     *
     * @return The sorted uses, or empty when no uses are defined or uses are
     *         disabled.
     */
    Collection<JobTicketUseDto> getTicketUsesByName();

    /**
     * Get the ticket tags from cache, sorted by tag word.
     *
     * @return The sorted tags, or empty when no tags are defined or tags are
     *         disabled.
     */
    Collection<JobTicketTagDto> getTicketTagsByName();

    /**
     * Gets the prefix label from a ticket number.
     *
     * @param ticketNumber
     *            The ticket number.
     * @return {@code null} when not present.
     */
    String getTicketNumberLabel(String ticketNumber);

    /**
     * Creates a job ticket label from constituents.
     *
     * @param label
     *            The label.
     * @return The aggregated label (can be empty)
     */
    String createTicketLabel(JobTicketLabelDto label);

    /**
     *
     * @return Valid delivery weekdays as set of ordinals, sorted ascending
     *         (Sunday is zero).
     */
    SortedSet<Integer> getDeliveryDaysOfWeek();

    /**
     * Calculates the next Job Ticket delivery date.
     *
     * @param offsetDate
     *            Date offset.
     * @return Next Job Ticket delivery date.
     */
    Date getDeliveryDateNext(Date offsetDate);

}
