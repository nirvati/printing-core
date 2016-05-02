/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2016 Datraverse B.V.
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
package org.savapage.core.services;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import org.savapage.core.dto.RedirectPrinterDto;
import org.savapage.core.imaging.EcoPrintPdfTask;
import org.savapage.core.imaging.EcoPrintPdfTaskPendingException;
import org.savapage.core.ipp.client.IppConnectException;
import org.savapage.core.jpa.Printer;
import org.savapage.core.jpa.User;
import org.savapage.core.outbox.OutboxInfoDto.OutboxJobDto;
import org.savapage.core.print.proxy.ProxyPrintDocReq;
import org.savapage.core.print.proxy.ProxyPrintInboxReq;
import org.savapage.core.services.helpers.DocContentPrintInInfo;

/**
 *
 * @author Rijk Ravestein
 *
 */
public interface JobTicketService extends StatefulService {

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
     * @param deliveryDate
     *            The requested date of delivery.
     * @throws EcoPrintPdfTaskPendingException
     *             When {@link EcoPrintPdfTask} objects needed for this PDF are
     *             pending.
     */
    void proxyPrintInbox(User lockedUser, ProxyPrintInboxReq request,
            Date deliveryDate) throws EcoPrintPdfTaskPendingException;

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
     * @param pdfFile
     *            The arbitrary (non-inbox) PDF file to print.
     * @param printInfo
     *            The {@link DocContentPrintInInfo}.
     * @param deliveryDate
     *            The requested date of delivery.
     * @throws IOException
     *             When file IO error occurs.
     */
    void proxyPrintPdf(User lockedUser, ProxyPrintDocReq request, File pdfFile,
            DocContentPrintInInfo printInfo, Date deliveryDate)
            throws IOException;

    /**
     * Gets the pending Job Tickets.
     *
     * @return The Job Tickets.
     */
    List<OutboxJobDto> getTickets();

    /**
     * Gets the pending Job Ticket.
     *
     * @param fileName
     *            The unique PDF file name of the job (no path).
     * @return The Job Ticket or {@code null} when not found.
     */
    OutboxJobDto getTicket(String fileName);

    /**
     * Gets the pending Job Tickets of a {@link User}.
     *
     * @param userId
     *            The {@link User} database key.
     * @return The Job Tickets.
     */
    List<OutboxJobDto> getTickets(Long userId);

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
     * Cancels the pending Job Tickets of a {@link User}.
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
     * Prints a Job Ticket.
     *
     * @param printer
     *            The redirect printer.
     * @param fileName
     *            The unique PDF file name of the job to print.
     * @return The printed ticket or {@code null} when ticket was not found.
     * @throws IOException
     *             When IO error.
     * @throws IppConnectException
     *             When connection to CUPS fails.
     */
    OutboxJobDto printTicket(Printer printer, String fileName)
            throws IOException, IppConnectException;

    /**
     * Gets the list of {@link RedirectPrinterDto} compatible printers for a Job
     * Ticket.
     *
     * @param fileName
     *            The unique PDF file name of the job ticket.
     * @return The list of redirect printers (can be empty) or {@code null} when
     *         job ticket is not found.
     */
    List<RedirectPrinterDto> getRedirectPrinters(String fileName);

    /**
     * Gets a {@link RedirectPrinterDto} compatible printer for a Job Ticket.
     *
     * @param fileName
     *            The unique PDF file name of the job ticket.
     * @return The redirect printer or {@code null} when no job ticket or
     *         printer is found.
     */
    RedirectPrinterDto getRedirectPrinter(String fileName);
}
