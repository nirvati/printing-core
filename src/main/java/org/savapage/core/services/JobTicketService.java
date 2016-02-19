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

import java.io.IOException;
import java.util.List;

import org.savapage.core.imaging.EcoPrintPdfTask;
import org.savapage.core.imaging.EcoPrintPdfTaskPendingException;
import org.savapage.core.ipp.client.IppConnectException;
import org.savapage.core.jpa.User;
import org.savapage.core.outbox.OutboxInfoDto.OutboxJobDto;
import org.savapage.core.print.proxy.ProxyPrintInboxReq;

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
     * @throws EcoPrintPdfTaskPendingException
     *             When {@link EcoPrintPdfTask} objects needed for this PDF are
     *             pending.
     */
    void proxyPrintInbox(User lockedUser, ProxyPrintInboxReq request)
            throws EcoPrintPdfTaskPendingException;

    /**
     * Gets the pending Job Tickets.
     *
     * @return The Job Tickets.
     */
    List<OutboxJobDto> getTickets();

    /**
     * Gets the pending Job Tickets of a {@link User}.
     *
     * @param userId
     *            The {@link User} database key.
     * @return The Job Tickets.
     */
    List<OutboxJobDto> getTickets(Long userId);

    /**
     * Removes a Job Ticket with an extra user check.
     *
     * @param userId
     *            The {@link User} database key of the ticket owner.
     * @param fileName
     *            The unique PDF file name of the job to remove.
     * @return The removed ticket of {@code null} when ticket was not found.
     * @throws IllegalArgumentException
     *             When Job Ticket is not owned by user.
     */
    OutboxJobDto removeTicket(Long userId, String fileName);

    /**
     * Removes a Job Ticket.
     *
     * @param fileName
     *            The unique PDF file name of the job to remove.
     * @return The removed ticket or {@code null} when ticket was not found.
     */
    OutboxJobDto removeTicket(String fileName);

    /**
     * Prints a Job Ticket.
     *
     * @param fileName
     *            The unique PDF file name of the job to remove.
     * @return The printed ticket or {@code null} when ticket was not found.
     * @throws IOException
     *             When IO error.
     * @throws IppConnectException
     *             When connection to CUPS fails.
     */
    OutboxJobDto printTicket(String fileName)
            throws IOException, IppConnectException;

}
