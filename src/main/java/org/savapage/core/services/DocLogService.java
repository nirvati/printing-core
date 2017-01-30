/*
 * This file is part of the SavaPage project <https://savapage.org>.
 * Copyright (c) 2011-2017 Datraverse B.V.
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

import java.io.IOException;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import org.savapage.core.cometd.AdminPublisher;
import org.savapage.core.dao.enums.DocLogProtocolEnum;
import org.savapage.core.dao.enums.ExternalSupplierEnum;
import org.savapage.core.dao.enums.ExternalSupplierStatusEnum;
import org.savapage.core.jpa.AccountTrx;
import org.savapage.core.jpa.ConfigProperty;
import org.savapage.core.jpa.DocIn;
import org.savapage.core.jpa.DocLog;
import org.savapage.core.jpa.DocOut;
import org.savapage.core.jpa.IppQueue;
import org.savapage.core.jpa.PrintIn;
import org.savapage.core.jpa.PrintOut;
import org.savapage.core.jpa.Printer;
import org.savapage.core.jpa.PrinterAttr;
import org.savapage.core.jpa.User;
import org.savapage.core.jpa.UserAttr;
import org.savapage.core.pdf.PdfCreateInfo;
import org.savapage.core.print.proxy.ProxyPrintJobStatusMonitor;
import org.savapage.core.services.helpers.AccountTrxInfoSet;
import org.savapage.core.services.helpers.DocContentPrintInInfo;

/**
 *
 * @author Rijk Ravestein
 *
 */
public interface DocLogService {

    /**
     * Generates a document signature for a {@link DocLog} instance. The HMAC
     * signature is based on this message:
     * <p>
     * {@code date time || userid || document name || document uuid}
     * </p>
     *
     * @param docLog
     *            The {@link DocLog} instance.
     * @return the HMAC signature.
     */
    String generateSignature(DocLog docLog);

    /**
     * Applies the creation date to the {@link DocLog} instance.
     *
     * @param docLog
     *            The {@link DocLog} instance.
     * @param date
     *            The creation date.
     */
    void applyCreationDate(DocLog docLog, Date date);

    /**
     * Logs the {@link DocOut} object WITH accounting info as created after
     * document <i>output</i> (like proxy print, download, send).
     * <p>
     * <b>IMPORTANT</b>: This method has it <u>own transaction scopes</u>. Any
     * open transaction is used: the end-result is a closed transaction.
     * <ul>
     * <li>The {@link DocLog} container is persisted in the database.</li>
     * <li>Document statistics are updated in the database for
     * {@link ConfigProperty} (global system), {@link User}, {@link UserAttr},
     * {@link Printer} and {@link PrinterAttr}.</li>
     * <li>{@link AccountTrx} objects are created when costs are GT zero.</li>
     * <li>If the {@link DocOut} contains a {@link PrintOut} the
     * {@link ProxyPrintJobStatusMonitor} is notified of the event.</li>
     * </ul>
     * </p>
     *
     * @param lockedUser
     *            The {@link User} instance, which could be locked by the
     *            caller. If not, the User wil be locked ad-hoc.
     * @param docOut
     *            The {@link DocOut} instance.
     * @param accountTrxInfoSet
     *            Information about the account transactions to be created.
     */
    void logDocOut(User lockedUser, DocOut docOut,
            AccountTrxInfoSet accountTrxInfoSet);

    /**
     * Logs the {@link DocOut} container with the {@link PrintOut} object, WITH
     * accounting info.
     * <p>
     * <b>IMPORTANT</b>: This method has it <u>own transaction scopes</u>. Any
     * open transaction is used: the end-result is a closed transaction.
     * <ul>
     * <li>The {@link DocLog} container is persisted in the database.</li>
     * <li>Document statistics are updated in the database for
     * {@link ConfigProperty} (global system), {@link User}, {@link UserAttr},
     * {@link Printer} and {@link PrinterAttr}.</li>
     * <li>{@link AccountTrx} objects are created when costs are GT zero.</li>
     * </ul>
     * </p>
     *
     * @param lockedUser
     *            The {@link User} instance, which could be locked by the
     *            caller. If not, the User wil be locked ad-hoc.
     * @param printOut
     *            The {@link PrintOut} instance with the {@link DocOut} object.
     * @param accountTrxInfoSet
     *            Information about the account transactions to be created.
     */
    void settlePrintOut(User lockedUser, PrintOut printOut,
            AccountTrxInfoSet accountTrxInfoSet);

    /**
     * Logs the {@link DocOut} object WITHOUT accounting info as created after
     * document <i>output</i> (like proxy print, download, send).
     * <p>
     * <b>IMPORTANT</b>: This method has it <u>own transaction scope</u>. Any
     * open transaction is used: the end-result is a closed transaction.
     * <ul>
     * <li>The {@link DocLog} container is persisted in the database.</li>
     * <li>Document statistics are updated in the database for
     * {@link ConfigProperty} (global system), {@link User}, {@link UserAttr},
     * {@link Printer} and {@link PrinterAttr}.</li>
     * </ul>
     * </p>
     *
     * @param lockedUser
     *            The {@link User} instance, which could be locked by the
     *            caller. If not, the User wil be locked ad-hoc.
     * @param docOut
     *            The {@link DocOut} instance.
     */
    void logDocOut(User lockedUser, DocOut docOut);

    /**
     * Logs a {@link PrintIn} job in the database using the
     * {@link DocContentPrintInInfo}.
     * <p>
     * <b>IMPORTANT</b>: This method manages its <u>own transaction scopes</u>.
     *
     * <ul>
     * <li>A {@link DocLog} container is persisted in the database and document
     * statistics are updated in the database for {@link User} and
     * {@link UserAttr}.</li>
     *
     * <li>Document statistics for {@link IppQueue} and {@link IppQueueAttrAttr}
     * are updated in the database in a separate transaction.</li>
     *
     * <li>Global document statistics, i.e. {@link ConfigProperty} objects, are
     * updated in the database in a separate transaction.</li>
     *
     * <li>Notifications are send to {@link AdminPublisher} and {@link User}.
     * </li>
     * </ul>
     * </p>
     *
     * @param userDb
     *            The {@link User}.
     * @param queue
     *            The {@link IppQueue}.
     * @param protocol
     *            The protocol with which the printIn was acquired.
     * @param printInInfo
     *            The {@link DocContentPrintInInfo}.
     */
    void logPrintIn(final User userDb, final IppQueue queue,
            final DocLogProtocolEnum protocol,
            final DocContentPrintInInfo printInInfo);

    /**
     * Gets the input {@link DocLog} from an External Supplier with a specific
     * {@link ExternalSupplierStatusEnum} and ID.
     *
     * @param supplier
     *            The supplier.
     * @param supplierAccount
     *            The supplier account.
     * @param suppliedId
     *            The supplied id.
     * @param status
     *            The status.
     * @return {@code null} when not found.
     */
    DocLog getSuppliedDocLog(final ExternalSupplierEnum supplier,
            final String supplierAccount, final String suppliedId,
            final ExternalSupplierStatusEnum status);

    /**
     * Collects data for the DocOut object using the merged PDF out file and the
     * {@link DocLog} UUID keys of {@link DocIn} documents (with number of
     * pages) that were used for the merge.
     * <p>
     * Note: {@link DocOut#setDeliveryProtocol(String)} and
     * {@link DocOut#setDestination(String)} should be performed on the
     * {@link DocLog} by the client using the PDF file.
     * </p>
     *
     * @param user
     *            The {@link User} to collect the data for.
     * @param docLogCollect
     *            Collects data for the DocOut object using the generated PDF
     *            and the uuid page counts.
     * @param createInfo
     *            The {@link PdfCreateInfo}.
     * @param uuidPageCount
     *            A {@link Map} with {@link DocLog} UUID keys of {@link DocIn}
     *            documents, with number of pages as value. Note:
     *            {@link LinkedHashMap} is insertion ordered.
     * @throws IOException
     *             When error reading the pdfFile (file size).
     */
    void collectData4DocOut(User user, DocLog docLogCollect,
            PdfCreateInfo createInfo,
            LinkedHashMap<String, Integer> uuidPageCount) throws IOException;

    /**
     * Collects data for the DocOut object of a Copy Job Ticket.
     *
     * @param user
     *            The {@link User} to collect the data for.
     * @param docLogCollect
     *            Collects data for the DocOut object.
     * @param numberOfPages
     *            The number of pages of the orginal document.
     */
    void collectData4DocOutCopyJob(final User user, final DocLog docLogCollect,
            final int numberOfPages);

    /**
     *
     * @param resetBy
     * @param resetDashboard
     * @param resetQueues
     * @param resetPrinters
     * @param resetUsers
     */
    void resetPagometers(String resetBy, boolean resetDashboard,
            boolean resetQueues, boolean resetPrinters, boolean resetUsers);

}
