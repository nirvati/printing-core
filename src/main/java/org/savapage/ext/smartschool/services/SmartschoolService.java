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
package org.savapage.ext.smartschool.services;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import javax.xml.soap.SOAPException;

import org.savapage.core.ShutdownException;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.jpa.Account;
import org.savapage.core.jpa.Account.AccountTypeEnum;
import org.savapage.core.jpa.IppQueue;
import org.savapage.core.jpa.User;
import org.savapage.core.services.helpers.AccountTrxInfoSet;
import org.savapage.ext.smartschool.SmartschoolConnection;
import org.savapage.ext.smartschool.SmartschoolException;
import org.savapage.ext.smartschool.SmartschoolPrintStatusEnum;
import org.savapage.ext.smartschool.SmartschoolTooManyRequestsException;
import org.savapage.ext.smartschool.xml.Document;
import org.savapage.ext.smartschool.xml.Jobticket;

/**
 *
 * @author Datraverse B.V.
 *
 */
public interface SmartschoolService {

    /**
     * Creates a {@link SmartschoolConnection} map of active SmartSchool
     * accounts (key is account).
     *
     * @return The connection list.
     * @throws SOAPException
     *             When SOAP connection error.
     */
    Map<String, SmartschoolConnection> createConnections() throws SOAPException;

    /**
     *
     * @return
     */
    IppQueue getSmartSchoolQueue();

    /**
     * Gets the {@link Jobticket} from SmartSchool.
     *
     * @param connection
     *            The {@link SmartschoolConnection}.
     * @return Tthe {@link Jobticket}.
     * @throws SmartschoolException
     * @throws SmartschoolTooManyRequestsException
     *             When HTTP status 429 "Too Many Requests" occurred.
     * @throws SOAPException
     *             When SOAP connection error.
     */
    Jobticket getJobticket(SmartschoolConnection connection)
            throws SmartschoolException, SmartschoolTooManyRequestsException,
            SOAPException;

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
     * Downloads a Smartschool {@link Document} for printing into the
     * application's temp directory. See {@link ConfigManager#getAppTmpDir()}.
     *
     * @param connection
     *            The {@link SmartschoolConnection }.
     * @param document
     *            The SmartSchool {@link Document}.
     * @param uuid
     *            The assigned {@link UUID} of the PrintIn document.
     * @return The downloaded {@link File}.
     * @throws IOException
     *             When a a file IO error occurs.
     * @throws ShutdownException
     *             When download was interrupted because of a
     *             {@link SmartschoolConnection#setShutdownRequested(boolean)}
     *             request.
     */
    File downloadDocument(SmartschoolConnection connection, Document document,
            UUID uuid) throws IOException, ShutdownException;

    /**
     * Downloads a Smartschool {@link Document} into the application's temp
     * directory. See {@link ConfigManager#getAppTmpDir()}.
     *
     * @param connection
     *            The {@link SmartschoolConnection }.
     * @param document
     *            The SmartSchool {@link Document}.
     * @return The downloaded {@link File}.
     * @throws IOException
     *             When a a file IO error occurs.
     * @throws ShutdownException
     *             When download was interrupted because of a
     *             {@link SmartschoolConnection#setShutdownRequested(boolean)}
     *             request.
     */
    File downloadDocumentForProxy(SmartschoolConnection connection,
            Document document) throws IOException, ShutdownException;

    /**
     * Reports the document status to SmartSchool.
     *
     * @param connection
     *            The {@link SmartschoolConnection }.
     * @param documentId
     *            The document ID.
     * @param status
     *            The {@link SmartschoolPrintStatusEnum}.
     * @param comment
     *            The comment.
     * @throws SmartschoolException
     * @throws SOAPException
     *             When SOAP connection error.
     */
    void
            reportDocumentStatus(SmartschoolConnection connection,
                    String documentId, SmartschoolPrintStatusEnum status,
                    String comment) throws SmartschoolException, SOAPException;

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
     *            The {@link SmartschoolConnection}.
     * @param klas
     *            The SmartSchool klas.
     * @return The composed shared account name.
     */
    String composeSharedChildAccountNameForKlas(
            SmartschoolConnection connection, String klas);

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
     *            The {@link SmartschoolConnection}.
     * @param parent
     *            The parent {@link Account}.
     * @param nTotCopies
     *            The total number of copies to be proxy printed.
     * @param klasCopies
     *            The number of copies per klas.
     * @param userCopies
     *            The number of copies per user. Each user MUST exist as active
     *            {@link User}.
     * @param userKlas
     *            Klas lookup for a user.
     * @return The {@link AccountTrxInfoSet}.
     */
    AccountTrxInfoSet createPrintInAccountTrxInfoSet(
            SmartschoolConnection connection, Account parent, int nTotCopies,
            Map<String, Integer> klasCopies, Map<String, Integer> userCopies,
            Map<String, String> userKlas);
}
