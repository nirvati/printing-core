/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2015 Datraverse B.V.
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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;

import javax.xml.bind.JAXBException;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

import org.savapage.core.print.smartschool.xml.Document;

/**
 *
 * @author Rijk Ravestein
 *
 */
public interface SmartSchoolProxyService {

    /**
     * Keeps the cluster node alive (heartbeat).
     *
     * @param nodeId
     *            The cluster Node ID.
     */
    void keepNodeAlive(String nodeId);

    /**
     * Checks node heartbeat to find out if it is alive.
     *
     * @param nodeId
     *            The cluster Node ID.
     * @param expiryMsec
     *            The number of msecs since last heartbeat after which node is
     *            considered dead.
     * @return {@code true} if node is alive.
     */
    boolean isNodeAlive(String nodeId, long expiryMsec);

    /**
     * Checks if password is valid for Smartschool account.
     *
     * @param accountName
     *            The Smartschool account name.
     * @param password
     *            The password.
     * @return {@code true} when password is valid for account.
     */
    boolean checkPwd(String accountName, String password);

    /**
     * Creates a {@link SOAPMessage} response for an account/node.
     *
     * @param accountName
     *            The Smartschool account name.
     * @param nodeId
     *            The Smartschool cluster Node ID.
     * @return The {@link SOAPMessage} response.
     * @throws SOAPException
     *             When SOAP error.
     * @throws JAXBException
     *             When XML serialization goes wrong.
     * @throws IOException
     *             When IO errors.
     */
    SOAPMessage createPrintJobsRsp(String accountName, String nodeId)
            throws SOAPException, JAXBException, IOException;

    /**
     * Streams a raw SOAP message with the document content.
     *
     * @param accountName
     *            The Smartschool account name.
     * @param nodeId
     *            The Smartschool cluster Node ID.
     * @param documentId
     *            The Smartschool document ID.
     * @param ostr
     *            The {@link OutputStream}.
     * @throws IOException
     *             When IO errors.
     * @throws FileNotFoundException
     *             When the document PDF file can not be found.
     */
    void streamGetDocumentRsp(String accountName, String nodeId,
            String documentId, OutputStream ostr) throws IOException,
            FileNotFoundException;

    /**
     * Caches the {@link Document}.
     *
     * @param accountName
     *            The Smartschool account name.
     * @param nodeId
     *            The Smartschool cluster Node ID.
     * @param document
     *            The {@link Document}
     * @param pdfFile
     *            The PDF file.
     * @throws IOException
     *             When IO error writing the file.
     */
    void cacheDocument(String accountName, String nodeId, Document document,
            File pdfFile) throws IOException;

    /**
     * Gets the Node ID as tagged in the {@link Document} (comment).
     *
     * @param document
     *            The {@link Document}.
     * @return The Node ID, or {@code null} when not found.
     */
    String getNodeId(Document document);

}
