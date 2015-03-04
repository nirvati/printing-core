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
package org.savapage.core.services;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import javax.xml.soap.SOAPException;

import org.savapage.core.ShutdownException;
import org.savapage.core.jpa.Account;
import org.savapage.core.jpa.IppQueue;
import org.savapage.core.jpa.User;
import org.savapage.core.jpa.Account.AccountTypeEnum;
import org.savapage.core.print.smartschool.SmartSchoolException;
import org.savapage.core.print.smartschool.SmartSchoolPrintStatusEnum;
import org.savapage.core.print.smartschool.xml.Document;
import org.savapage.core.print.smartschool.xml.Jobticket;
import org.savapage.core.services.helpers.AccountTrxInfoSet;
import org.savapage.core.services.helpers.SmartSchoolConnection;

/**
 *
 * @author Datraverse B.V.
 *
 */
public interface SmartSchoolService {

    /**
     * Creates a {@link SmartSchoolConnection} map of active SmartSchool
     * accounts (key is account).
     *
     * @return The connection list.
     * @throws SOAPException
     *             When SOAP connection error.
     */
    Map<String, SmartSchoolConnection> createConnections() throws SOAPException;

    /**
     *
     * @return
     */
    IppQueue getSmartSchoolQueue();

    /**
     * Gets the {@link Jobticket} from SmartSchool.
     *
     * @param connection
     *            The {@link SmartSchoolConnection}.
     * @return Tthe {@link Jobticket}.
     * @throws SmartSchoolException
     * @throws SOAPException
     *             When SOAP connection error.
     */
    Jobticket getJobticket(SmartSchoolConnection connection)
            throws SmartSchoolException, SOAPException;

    /**
     * Creates the {@link File} object to be used as document download target.
     *
     * @param documentName
     *            The SmartSchool document name.
     * @param uuid
     *            The assigned {@link UUID}.
     * @return the {@link File} object.
     */
    File getDownloadFile(String documentName, UUID uuid);

    /**
     * Downloads a document for printing.
     *
     * @param connection
     *            The {@link SmartSchoolConnection }.
     * @param user
     *            The requesting {@link User}.
     * @param document
     *            The SmartSchool {@link Document}.
     * @param uuid
     *            The assigned {@link UUID} of the PrintIn document.
     * @return The downloaded {@link File}.
     * @throws IOException
     *             When a a file IO error occurs.
     * @throws ShutdownException
     *             When download was interrupted because of a
     *             {@link SmartSchoolConnection#setShutdownRequested(boolean)}
     *             request.
     */
    File downloadDocument(SmartSchoolConnection connection, User user,
            Document document, UUID uuid) throws IOException, ShutdownException;

    /**
     * Reports the document status to SmartSchool.
     *
     * @param connection
     *            The {@link SmartSchoolConnection }.
     * @param documentId
     * @param status
     *            The {@link SmartSchoolPrintStatusEnum}.
     * @param comment
     * @throws SmartSchoolException
     * @throws SOAPException
     *             When SOAP connection error.
     */
    void
            reportDocumentStatus(SmartSchoolConnection connection,
                    String documentId, SmartSchoolPrintStatusEnum status,
                    String comment) throws SmartSchoolException, SOAPException;

    /**
     * Creates an account template for lazy creating new shared accounts.
     * <p>
     * Note: all data is set except the identifying attributes
     * {@link Account#setName(String)} and {@link Account#setNameLower(String)}.
     * </p>
     *
     * @param parent
     *            The parent {@link Account}.
     * @return The {@link Account} template
     */
    Account createSharedAccountTemplate(Account parent);

    /**
     * Gets the top-level parent {@link Account} for shared child "klas"
     * accounts.
     *
     * @return The parent {@link Account}.
     */
    Account getSharedParentAccount();

    /**
     * Gets the top-level parent account name.
     *
     * @return The parent account name.
     */
    String getSharedParentAccountName();

    /**
     * Gets the child account name for SmartSchool Jobs.
     *
     * @return The shared account name.
     */
    String getSharedJobsAccountName();

    /**
     * Composes a {@link AccountTypeEnum#SHARED} {@link Account} name for a
     * SmartSchool klas.
     *
     * @param connection
     *            The {@link SmartSchoolConnection}.
     * @param klas
     *            The SmartSchool klas.
     * @return The composed shared account name.
     */
    String composeSharedChildAccountNameForKlas(
            SmartSchoolConnection connection, String klas);

    /**
     * Gets the SmartSchool klas from the composed
     * {@link AccountTypeEnum#SHARED} {@link Account} name.
     *
     * @param accountName
     *            The account name.
     * @return {@code null} when not found.
     */
    String getKlasFromComposedAccountName(String accountName);

    /**
     * Creates the {@link AccountTrxInfoSet} for a SmartSchool print request.
     *
     * @param connection
     *            The {@link SmartSchoolConnection}.
     * @param parent
     *            The parent {@link Account}.
     * @param klasCopies
     *            The number of copies per klas.
     * @param userCopies
     *            The number of copies per user. Each user MUST exist as active
     *            {@link User}.
     * @return The {@link AccountTrxInfoSet}.
     */
    AccountTrxInfoSet createPrintInAccountTrxInfoSet(
            SmartSchoolConnection connection, Account parent,
            Map<String, Integer> klasCopies, Map<String, Integer> userCopies);
}
