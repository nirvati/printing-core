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
package org.savapage.ext.smartschool.services.impl;

import static java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE;
import static java.nio.file.attribute.PosixFilePermission.OWNER_READ;
import static java.nio.file.attribute.PosixFilePermission.OWNER_WRITE;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPBodyElement;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

import org.apache.commons.codec.binary.Base64InputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.util.FileSystemHelper;
import org.savapage.ext.smartschool.SmartschoolConnection;
import org.savapage.ext.smartschool.SmartschoolConstants;
import org.savapage.ext.smartschool.SmartschoolRequestEnum;
import org.savapage.ext.smartschool.services.SmartschoolProxyService;
import org.savapage.ext.smartschool.xml.Document;
import org.savapage.ext.smartschool.xml.Documents;
import org.savapage.ext.smartschool.xml.Jobticket;
import org.savapage.ext.smartschool.xml.Requestinfo;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class SmartschoolProxyServiceImpl implements
        SmartschoolProxyService {

    /**
     * The buffer size for reading.
     */
    private static final int BUFFER_SIZE = 1024;

    /**
     * .
     */
    private static final String CACHE_RELATIVE_HOME_PATH = "smartschool";

    /**
     * .
     */
    private static final String CACHE_RELATIVE_PROXY_PATH = "proxy";

    /**
     * .
     */
    private static final String FILE_EXT_XML = "xml";

    /**
     * .
     */
    private static final String NODE_ID_PREFIX = "@";

    /**
     * .
     */
    private static final String FILE_EXT_PDF = "pdf";

    /**
     * A map with Node ID as key and last notified heartbeat time as value.
     */
    private static ConcurrentHashMap<String, Long> proxyClientHeartbeats =
            new ConcurrentHashMap<>();

    /**
     * Formats a date to XML the Smartschool way.
     *
     * @param date
     *            The {@link Date}.
     * @return the formatted date.
     */
    public static String formatDate(final Date date) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return format.format(date);
    }

    @Override
    public void keepNodeAlive(final String nodeId) {
        proxyClientHeartbeats.put(nodeId,
                Long.valueOf(System.currentTimeMillis()));
    }

    @Override
    public boolean isNodeAlive(final String nodeId, final long expiryMsec) {
        final Long lastTime = proxyClientHeartbeats.get(nodeId);
        if (lastTime == null) {
            return false;
        }
        return System.currentTimeMillis() - lastTime.longValue() < expiryMsec;
    }

    /**
     *
     * @param keySmartschoolEnable
     * @param keySmartschoolEndPointUrl
     * @param keySmartschoolPwd
     * @param accountName
     * @param password
     * @return
     */
    private boolean checkPwd(final IConfigProp.Key keySmartschoolEnable,
            final IConfigProp.Key keySmartschoolNodeEnable,
            final IConfigProp.Key keySmartschoolNodeProxyEnable,
            final IConfigProp.Key keySmartschoolEndPointUrl,
            final IConfigProp.Key keySmartschoolPwd, final String accountName,
            final String password) {

        final ConfigManager cm = ConfigManager.instance();

        if (accountName != null && password != null
                && cm.isConfigValue(keySmartschoolEnable)) {

            try {
                final String name =
                        SmartschoolConnection.extractAccountName(new URL(cm
                                .getConfigValue(keySmartschoolEndPointUrl)));

                return name != null
                        && cm.isConfigValue(keySmartschoolNodeEnable)
                        && cm.isConfigValue(keySmartschoolNodeProxyEnable)
                        && name.equals(accountName)
                        && password
                                .equals(cm.getConfigValue(keySmartschoolPwd));
            } catch (MalformedURLException e) {
                // noop
            }

        }
        return false;
    }

    @Override
    public boolean checkPwd(final String accountName, final String password) {
        if (checkPwd(Key.SMARTSCHOOL_1_ENABLE,
                Key.SMARTSCHOOL_1_SOAP_PRINT_NODE_ENABLE,
                Key.SMARTSCHOOL_1_SOAP_PRINT_NODE_PROXY_ENABLE,
                Key.SMARTSCHOOL_1_SOAP_PRINT_ENDPOINT_URL,
                Key.SMARTSCHOOL_1_SOAP_PRINT_ENDPOINT_PASSWORD, accountName,
                password)) {
            return true;
        }
        return checkPwd(Key.SMARTSCHOOL_2_ENABLE,
                Key.SMARTSCHOOL_2_SOAP_PRINT_NODE_ENABLE,
                Key.SMARTSCHOOL_2_SOAP_PRINT_NODE_PROXY_ENABLE,
                Key.SMARTSCHOOL_2_SOAP_PRINT_ENDPOINT_URL,
                Key.SMARTSCHOOL_2_SOAP_PRINT_ENDPOINT_PASSWORD, accountName,
                password);
    }

    @Override
    public String getNodeId(final Document document) {

        final String comment = document.getComment();

        if (!StringUtils.isBlank(comment)) {

            final String[] words = StringUtils.split(comment.trim());

            if (words.length > 0) {
                final String word = words[0];
                if (word.length() > 1 && word.startsWith(NODE_ID_PREFIX)) {
                    return word.substring(NODE_ID_PREFIX.length());
                }
            }
        }
        return null;
    }

    /**
     * Creates An empty {@link Jobticket} without any document.
     *
     * @param accountName
     *            The Smartschool account name.
     * @return The {@link Jobticket}.
     */
    private static Jobticket createEmptyJobticket(final String accountName) {

        final Jobticket ticket = new Jobticket();
        ticket.setLocation(String.format("%s Proxy", accountName));
        ticket.setType(Jobticket.TYPE_PRINT);

        final Requestinfo reqInfo = new Requestinfo();
        ticket.setRequestinfo(reqInfo);

        reqInfo.setDate(formatDate(new Date()));

        final Documents docs = new Documents();
        ticket.setDocuments(docs);

        return ticket;
    }

    /**
     * Gets {@link Jobticket} for an account/node.
     *
     * @param accountName
     *            The Smartschool account name.
     * @param nodeId
     *            The Smartschool cluster Node ID.
     * @return The {@link Jobticket}.
     * @throws IOException
     *             When IO errors.
     */
    private static synchronized Jobticket getJobticket(
            final String accountName, final String nodeId) throws IOException {

        final Jobticket ticket = createEmptyJobticket(accountName);

        final Path directory = getCacheHomePath(accountName, nodeId);

        if (!Files.exists(directory)) {
            return ticket;
        }

        final File[] fileList = directory.toFile().listFiles();

        if (fileList == null) {
            throw new IOException(String.format("Error reading directory %s",
                    directory.toString()));
        }

        final List<Document> docList = ticket.getDocuments().getDocument();

        for (final File file : fileList) {

            if (!file.isFile() || !file.getName().endsWith(FILE_EXT_XML)) {
                continue;
            }

            final Reader reader = new FileReader(file);

            try {
                docList.add(Document.create(Document.class, reader));
            } catch (JAXBException e) {
                throw new IOException(e.getMessage(), e);
            } finally {
                IOUtils.closeQuietly(reader);
            }
        }

        return ticket;
    }

    @Override
    public SOAPMessage createPrintJobsRsp(final String accountName,
            final String nodeId) throws SOAPException, JAXBException,
            IOException {

        final Jobticket jobTicket = getJobticket(accountName, nodeId);

        final SOAPMessage message =
                MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL)
                        .createMessage();

        final SOAPBody body = message.getSOAPBody();
        final QName bodyName =
                new QName(
                        SmartschoolRequestEnum.GET_PRINTJOBS
                                .getSoapNameResponse());
        final SOAPBodyElement bodyElement = body.addBodyElement(bodyName);
        final SOAPElement elementReturn =
                bodyElement
                        .addChildElement(SmartschoolConstants.XML_ELM_RETURN);

        // NOTE: XML is added as text!!
        elementReturn.addTextNode(jobTicket.asXmlString());

        return message;
    }

    /**
     * Deletes the cache for a {@link Document}.
     *
     * @param pdfFile
     *            The document PDF file.
     * @param xmlFile
     *            The document XML file.
     */
    private static synchronized void deleteDocumentCache(final File pdfFile,
            final File xmlFile) {
        pdfFile.delete();
        xmlFile.delete();
    }

    @Override
    public void streamGetDocumentRsp(final String accountName,
            final String nodeId, final String documentId,
            final OutputStream ostr) throws IOException {

        final File file =
                createDocumentFile(accountName, nodeId, documentId,
                        FILE_EXT_PDF).toFile();

        if (!file.exists()) {
            // TODO
            throw new FileNotFoundException(String.format("File %s not found",
                    file.getAbsolutePath()));
        }

        streamGetDocumentRsp(documentId, file, ostr);

        deleteDocumentCache(
                file,
                createDocumentFile(accountName, nodeId, documentId,
                        FILE_EXT_XML).toFile());
    }

    /**
     * Streams a raw SOAP message with the document content.
     *
     * @param documentId
     *            The Smartschool document ID.
     * @param file
     *            The file to stream.
     * @param ostr
     *            The {@link OutputStream}.
     * @throws IOException
     *             When IO errors.
     */
    private static void streamGetDocumentRsp(final String documentId,
            final File file, final OutputStream ostr) throws IOException {

        /*
         * Regular XML.
         */
        final StringBuilder start = new StringBuilder();

        start.append("<env:Envelope "
                + "xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\">"
                + "<env:Header/><env:Body>");
        start.append("<")
                .append(SmartschoolRequestEnum.GET_DOCUMENT
                        .getSoapNameResponse()).append(">");
        start.append("<").append(SmartschoolConstants.XML_ELM_RETURN)
                .append(">");

        ostr.write(start.toString().getBytes());

        /*
         * Escaped XML.
         */
        final StringBuilder content = new StringBuilder();

        content.append("<document id=\"").append(documentId).append("\">");

        content.append("<filename>").append(file.getName())
                .append("</filename>");

        content.append("<filesize>").append(String.valueOf(file.length()))
                .append("</filesize>");

        content.append("<md5sum>").append("0").append("</md5sum>"); // TODO

        //
        content.append("<").append(SmartschoolConstants.XML_ELM_DATA)
                .append(">");

        ostr.write(StringEscapeUtils.escapeXml10(content.toString()).getBytes());

        /*
         * The Base64 encoded PDF file (no escape needed).
         */
        final InputStream istr = new FileInputStream(file);
        final InputStream istrB64 = new Base64InputStream(istr, true);

        final byte[] buffer = new byte[BUFFER_SIZE];

        try {
            int iBytes = istrB64.read(buffer);
            while (iBytes >= 0) {
                ostr.write(buffer, 0, iBytes);
                ostr.flush();
                iBytes = istrB64.read(buffer);
            }
        } finally {
            istrB64.close();
        }

        ostr.write(StringEscapeUtils.escapeXml10(
                "</" + SmartschoolConstants.XML_ELM_DATA + "></document>")
                .getBytes());

        /*
         * Regular XML.
         */
        final StringBuilder end = new StringBuilder();

        end.append("</return>");
        end.append("</")
                .append(SmartschoolRequestEnum.GET_DOCUMENT
                        .getSoapNameResponse()).append(">");
        end.append("</env:Body></env:Envelope>");

        ostr.write(end.toString().getBytes());
    }

    /**
     * @return The cache home directory path.
     */
    private static Path getCacheHomePath() {
        return Paths.get(ConfigManager.getServerExtHome().getAbsolutePath(),
                CACHE_RELATIVE_HOME_PATH, CACHE_RELATIVE_PROXY_PATH)
                .toAbsolutePath();
    }

    /**
     *
     * @param accountName
     *            The Smartschool account name.
     * @param nodeId
     *            The Smartschool cluster Node ID.
     * @return The cache home directory path.
     */
    private static Path getCacheHomePath(final String accountName,
            final String nodeId) {
        return Paths.get(getCacheHomePath().toString(),
                accountName.toLowerCase(), nodeId.toLowerCase())
                .toAbsolutePath();
    }

    /**
     * Creates the cache home directory for an account/node when it does not
     * exist.
     *
     * @param accountName
     *            The Smartschool account name.
     * @param nodeId
     *            The Smartschool cluster Node ID.
     * @throws IOException
     *             When IO error.
     */
    private static synchronized void lazyCreateCacheHome(
            final String accountName, final String nodeId) throws IOException {

        final Path home = getCacheHomePath(accountName, nodeId);

        if (!Files.exists(home)) {

            final Set<PosixFilePermission> permissions =
                    EnumSet.of(OWNER_READ, OWNER_WRITE, OWNER_EXECUTE);

            final FileAttribute<Set<PosixFilePermission>> fileAttributes =
                    PosixFilePermissions.asFileAttribute(permissions);

            Files.createDirectories(home, fileAttributes);
        }
    }

    @Override
    public void cacheDocument(final String accountName, final String nodeId,
            final Document document, final File pdfFile) throws IOException {

        lazyCreateCacheHome(accountName, nodeId);

        final File cachedXml =
                createDocumentFile(accountName, nodeId, document.getId(),
                        FILE_EXT_XML).toFile();

        final Writer xmlWriter = new FileWriter(cachedXml);

        try {
            document.writeXml(xmlWriter);
        } catch (JAXBException e) {
            throw new IOException(e.getMessage(), e);
        } finally {
            IOUtils.closeQuietly(xmlWriter);
        }

        final Path cachedPdf =
                createDocumentFile(accountName, nodeId, document.getId(),
                        FILE_EXT_PDF);

        boolean exception = true;
        try {
            FileSystemHelper.doAtomicFileMove(
                    Paths.get(pdfFile.getAbsolutePath()), cachedPdf);
            exception = false;
        } finally {
            if (exception) {
                cachedXml.delete();
            }
        }
    }

    /**
     * Creates a {@link Path} belonging to a {@link Document}.
     *
     * @param accountName
     *            The Smartschool account name.
     * @param nodeId
     *            The Smartschool cluster Node ID.
     * @param documentId
     *            The ID of the {@link Document}.
     * @param fileExt
     *            The file extension.
     * @return The {@link Path}.
     */
    private static Path createDocumentFile(final String accountName,
            final String nodeId, final String documentId, final String fileExt) {

        return Paths.get(getCacheHomePath(accountName, nodeId).toString(),
                String.format("%s.%s", documentId, fileExt));
    }

    /**
     * @deprecated
     * @return
     * @throws SOAPException
     * @throws JAXBException
     */
    @Deprecated
    private static SOAPMessage createSetDocumentStatusRsp()
            throws SOAPException, JAXBException {

        final SOAPMessage message =
                MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL)
                        .createMessage();

        final SOAPBody body = message.getSOAPBody();
        final QName bodyName =
                new QName(
                        SmartschoolRequestEnum.SET_DOCUMENTSTATUS
                                .getSoapNameResponse());
        final SOAPBodyElement bodyElement = body.addBodyElement(bodyName);
        final SOAPElement elementReturn = bodyElement.addChildElement("return");

        // NOTE: XML is added as text!!
        elementReturn.addTextNode("<status type=\"print\" direction=\"s2r\">"
                + "<code>ok</code></status>");

        return message;
    }

}
