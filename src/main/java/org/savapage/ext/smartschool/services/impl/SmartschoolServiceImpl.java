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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPBodyElement;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPMessage;

import org.apache.commons.codec.binary.Base64OutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.savapage.core.ShutdownException;
import org.savapage.core.ShutdownRequestedException;
import org.savapage.core.SpException;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.dao.enums.ReservedIppQueueEnum;
import org.savapage.core.jpa.Account;
import org.savapage.core.jpa.Account.AccountTypeEnum;
import org.savapage.core.jpa.IppQueue;
import org.savapage.core.jpa.User;
import org.savapage.core.jpa.UserAccount;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.helpers.AccountTrxInfo;
import org.savapage.core.services.helpers.AccountTrxInfoSet;
import org.savapage.core.services.impl.AbstractService;
import org.savapage.ext.smartschool.SmartschoolAccount;
import org.savapage.ext.smartschool.SmartschoolConnection;
import org.savapage.ext.smartschool.SmartschoolConstants;
import org.savapage.ext.smartschool.SmartschoolException;
import org.savapage.ext.smartschool.SmartschoolLogger;
import org.savapage.ext.smartschool.SmartschoolPrintStatusEnum;
import org.savapage.ext.smartschool.SmartschoolRequestEnum;
import org.savapage.ext.smartschool.SmartschoolTooManyRequestsException;
import org.savapage.ext.smartschool.services.SmartschoolService;
import org.savapage.ext.smartschool.xml.Document;
import org.savapage.ext.smartschool.xml.DocumentStatusIn;
import org.savapage.ext.smartschool.xml.Jobticket;
import org.savapage.ext.smartschool.xml.SmartschoolXmlObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class SmartschoolServiceImpl extends AbstractService implements
        SmartschoolService {

    /**
     *
     */
    private static final Logger LOGGER = LoggerFactory
            .getLogger(SmartschoolServiceImpl.class);

    /**
     * The user has sent too many requests in a given amount of time. Intended
     * for use with rate limiting schemes (RFC 6585).
     */
    private final static String HTTP_STATUS_TOO_MANY_REQUESTS = "429";

    /**
     * Format string to be used in {@link String#format(String, Object...)}. The
     * first {@code %s} is a placeholder for the SmartSchool account name. The
     * second {@code %s} is a placeholder for the klas name.
     */
    private static final String SHARED_ACCOUNT_FOR_KLAS_FORMAT = "%s.Klas.%s";

    /**
     * Creates the first account.
     *
     * @return The {@link SmartschoolAccount}.
     */
    private static SmartschoolAccount getAccount1() {

        final ConfigManager cm = ConfigManager.instance();

        final SmartschoolAccount acc = new SmartschoolAccount();

        acc.setEndpoint(cm
                .getConfigValue(Key.SMARTSCHOOL_1_SOAP_PRINT_ENDPOINT_URL));
        acc.setPassword(cm.getConfigValue(
                Key.SMARTSCHOOL_1_SOAP_PRINT_ENDPOINT_PASSWORD).toCharArray());

        final SmartschoolAccount.Config cfg = acc.getConfig();

        cfg.setChargeToStudents(cm
                .isConfigValue(Key.SMARTSCHOOL_1_SOAP_PRINT_CHARGE_TO_STUDENTS));

        cfg.setProxyPrinterName(cm
                .getConfigValue(Key.SMARTSCHOOL_1_SOAP_PRINT_PROXY_PRINTER));
        cfg.setProxyPrinterDuplexName(cm
                .getConfigValue(Key.SMARTSCHOOL_1_SOAP_PRINT_PROXY_PRINTER_DUPLEX));
        cfg.setProxyPrinterGrayscaleName(cm
                .getConfigValue(Key.SMARTSCHOOL_1_SOAP_PRINT_PROXY_PRINTER_GRAYSCALE));

        cfg.setProxyPrinterGrayscaleDuplexName(cm
                .getConfigValue(Key.SMARTSCHOOL_1_SOAP_PRINT_PROXY_PRINTER_GRAYSCALE_DUPLEX));

        if (cm.isConfigValue(Key.SMARTSCHOOL_1_SOAP_PRINT_NODE_ENABLE)) {
            final SmartschoolAccount.Node node = new SmartschoolAccount.Node();
            acc.setNode(node);
            node.setId(cm.getConfigValue(Key.SMARTSCHOOL_1_SOAP_PRINT_NODE_ID));
            node.setProxy(cm
                    .isConfigValue(Key.SMARTSCHOOL_1_SOAP_PRINT_NODE_PROXY_ENABLE));
            node.setProxyEndpoint(cm
                    .getConfigValue(Key.SMARTSCHOOL_1_SOAP_PRINT_NODE_PROXY_ENDPOINT_URL));
        }
        return acc;
    }

    /**
     * Creates the second account.
     *
     * @return The {@link SmartschoolAccount}.
     */
    private static SmartschoolAccount getAccount2() {

        final ConfigManager cm = ConfigManager.instance();

        final SmartschoolAccount acc = new SmartschoolAccount();

        acc.setEndpoint(cm
                .getConfigValue(Key.SMARTSCHOOL_2_SOAP_PRINT_ENDPOINT_URL));
        acc.setPassword(cm.getConfigValue(
                Key.SMARTSCHOOL_2_SOAP_PRINT_ENDPOINT_PASSWORD).toCharArray());

        final SmartschoolAccount.Config cfg = acc.getConfig();

        cfg.setChargeToStudents(cm
                .isConfigValue(Key.SMARTSCHOOL_2_SOAP_PRINT_CHARGE_TO_STUDENTS));

        cfg.setProxyPrinterName(cm
                .getConfigValue(Key.SMARTSCHOOL_2_SOAP_PRINT_PROXY_PRINTER));
        cfg.setProxyPrinterDuplexName(cm
                .getConfigValue(Key.SMARTSCHOOL_2_SOAP_PRINT_PROXY_PRINTER_DUPLEX));
        cfg.setProxyPrinterGrayscaleName(cm
                .getConfigValue(Key.SMARTSCHOOL_2_SOAP_PRINT_PROXY_PRINTER_GRAYSCALE));

        cfg.setProxyPrinterGrayscaleDuplexName(cm
                .getConfigValue(Key.SMARTSCHOOL_2_SOAP_PRINT_PROXY_PRINTER_GRAYSCALE_DUPLEX));

        if (cm.isConfigValue(Key.SMARTSCHOOL_2_SOAP_PRINT_NODE_ENABLE)) {
            final SmartschoolAccount.Node node = new SmartschoolAccount.Node();
            acc.setNode(node);
            node.setId(cm.getConfigValue(Key.SMARTSCHOOL_2_SOAP_PRINT_NODE_ID));
            node.setProxy(cm
                    .isConfigValue(Key.SMARTSCHOOL_2_SOAP_PRINT_NODE_PROXY_ENABLE));
            node.setProxyEndpoint(cm
                    .getConfigValue(Key.SMARTSCHOOL_2_SOAP_PRINT_NODE_PROXY_ENDPOINT_URL));
        }
        return acc;
    }

    @Override
    public Map<String, SmartschoolConnection> createConnections()
            throws SOAPException {

        final Map<String, SmartschoolConnection> connectionMap =
                new HashMap<>();

        final ConfigManager cm = ConfigManager.instance();

        SmartschoolConnection connection;

        if (cm.isConfigValue(Key.SMARTSCHOOL_1_ENABLE)) {
            connection = new SmartschoolConnection(getAccount1());
            connectionMap.put(connection.getAccountName(), connection);
        }

        if (cm.isConfigValue(Key.SMARTSCHOOL_2_ENABLE)) {
            connection = new SmartschoolConnection(getAccount2());
            connectionMap.put(connection.getAccountName(), connection);
        }

        return connectionMap;
    }

    @Override
    public Jobticket getJobticket(final SmartschoolConnection connection)
            throws SmartschoolException, SmartschoolTooManyRequestsException,
            SOAPException {

        final SOAPElement returnElement;

        try {

            final SmartschoolRequestEnum request =
                    SmartschoolRequestEnum.GET_PRINTJOBS;

            returnElement =
                    this.sendMessage(
                            connection,
                            request,
                            createPrintJobsRequest(request,
                                    connection.getPassword()));
        } catch (SOAPException e) {
            /*
             * This is a weak solution, but there is no other way to find out
             * the HTTP status. Is this a flaw in the
             * javax.xml.soap.SOAPConnection class or the Smartschool SOAP
             * interface design?
             */
            if (e.getMessage().contains(HTTP_STATUS_TOO_MANY_REQUESTS)) {
                throw new SmartschoolTooManyRequestsException(e);
            }
            throw e;
        }

        final Jobticket jobTicket;

        try {
            jobTicket =
                    SmartschoolXmlObject.create(Jobticket.class,
                            returnElement.getValue());
        } catch (JAXBException e) {
            throw new SpException(e);
        }
        return jobTicket;
    }

    @Override
    public void reportDocumentStatus(final SmartschoolConnection connection,
            final String documentId, final SmartschoolPrintStatusEnum status,
            final String comment) throws SmartschoolException, SOAPException {

        final SmartschoolRequestEnum request =
                SmartschoolRequestEnum.SET_DOCUMENTSTATUS;

        this.sendMessage(
                connection,
                request,
                createSetDocumentStatusRequest(connection, request, documentId,
                        status.getXmlText(), comment));
    }

    @Override
    public File getDownloadFile(final String documentName, final UUID uuid) {

        final StringBuffer downloadedFilePath = new StringBuffer(128);

        /*
         * Do NOT download in User inbox .temp directory, since that might not
         * exist (because this is the first user print). A lazy create requires
         * a User lock, which we do not want at this moment.
         */
        downloadedFilePath.append(ConfigManager.getAppTmpDir())
                .append(File.separatorChar).append(uuid.toString()).append("_")
                .append(documentName);

        return new File(downloadedFilePath.toString());
    }

    @Override
    public File downloadDocumentForProxy(
            final SmartschoolConnection connection, final Document document)
            throws IOException, ShutdownException {
        return downloadDocument(connection, document, UUID.randomUUID());
    }

    @Override
    public File downloadDocument(final SmartschoolConnection connection,
            final Document document, final UUID uuid) throws IOException,
            ShutdownException {

        final File downloadedFile =
                this.getDownloadFile(document.getName(), uuid);

        boolean exception = true;

        try {
            downloadDocument(connection, document, downloadedFile);
            exception = false;
        } finally {
            if (exception) {
                if (downloadedFile.exists()) {
                    downloadedFile.delete();
                }
            }
        }
        return downloadedFile;
    }

    /**
     *
     * @param connection
     * @param documentId
     * @param downloadedFile
     * @throws IOException
     * @throws ShutdownException
     */
    private static void downloadDocument(
            final SmartschoolConnection connection, final Document document,
            final File downloadedFile) throws IOException, ShutdownException {

        final SmartschoolRequestEnum request =
                SmartschoolRequestEnum.GET_DOCUMENT;

        final SOAPMessage soapMsg =
                createGetDocumentRequest(request, document.getId(),
                        connection.getPassword());

        final ContentType contentType = ContentType.create("application/xml");

        final HttpEntity entity =
                new StringEntity(getXmlFromSOAPMessage(soapMsg), contentType);

        final HttpPost httppost =
                new HttpPost(connection.getEndpointUri(request));

        httppost.setConfig(buildRequestConfig());

        /*
         * Our own signature :-)
         */
        httppost.setHeader(HttpHeaders.USER_AGENT,
                ConfigManager.getAppNameVersion());

        httppost.setEntity(entity);

        /*
         * Custom handler.
         */
        final ResponseHandler<File> streamingHandler =
                new ResponseHandler<File>() {

                    @Override
                    public File handleResponse(final HttpResponse response)
                            throws IOException {

                        final HttpEntity entity = response.getEntity();

                        if (entity == null) {
                            return null;
                        }

                        final OutputStream ostr =
                                new FileOutputStream(downloadedFile);

                        try {

                            saxParseDocumentData(connection,
                                    entity.getContent(), ostr);

                        } catch (IllegalStateException
                                | ParserConfigurationException | SAXException e) {
                            throw new SpException(e.getMessage());
                        }

                        return downloadedFile;
                    }
                };

        try {

            connection.getHttpClient().execute(httppost, streamingHandler);

        } catch (ShutdownRequestedException e) {

            throw new ShutdownException("Download of [" + document.getName()
                    + "] was interrupted.");

        } finally {
            /*
             * Mantis #487: release the connection.
             */
            httppost.reset();
        }
    }

    /**
     * Parses the XML {@link InputStream} and writes the Base64 decoded PDF
     * document to the {@link OutputStream}.
     *
     * @param connection
     *            The {@link SmartschoolConnection} (used to check if a shutdown
     *            is requested).
     * @param istr
     *            The XML {@link InputStream}.
     * @param ostr
     *            The {@link OutputStream} for the Base64 decoded PDF document.
     * @throws ParserConfigurationException
     * @throws SAXException
     *             When SAX error.
     * @throws IOException
     *             When IO error.
     * @throws ShutdownRequestedException
     *             When a shutdown was requested during processing.
     */
    private static void saxParseDocumentData(
            final SmartschoolConnection connection, final InputStream istr,
            final OutputStream ostr) throws ParserConfigurationException,
            SAXException, IOException, ShutdownRequestedException,
            ShutdownRequestedException {

        /*
         * ------------------------ IMPORTANT --------------------------------
         *
         * The download data is held as content of the "return" element. Beware
         * that the content is XML formatted (escaped) as string! This escaped
         * XML is NOT part of the XML tree, hence the SAXParser will NOT
         * encounter startElement() and endElement() callbacks for the escaped
         * elements "filename", "filesize", "md5sum", and "data"! That is why
         * string parsing is done in the characters() callback while the
         * "return" tag is in focus.
         * -------------------------------------------------------------------
         */
        final DefaultHandler saxHandler = new DefaultHandler() {

            /**
             * {@code true} when we are processing the "return" element.
             */
            private boolean processReturn;

            /**
             * {@code true} when we are processing the "data" element with the
             * Base64 encoded document.
             */
            private boolean processData;

            /**
             * The collected XML content of the "return" element.
             */
            private final StringBuilder initialReturnXml = new StringBuilder(
                    256);

            // private String fileName; // TODO
            // private String fileSize; // TODO
            // private String md5sum; // TODO

            /**
             * The {@link OutputStream} with the Base64 decode PDF.
             */
            private OutputStream ostrDoc = null;

            @Override
            public void startDocument() throws SAXException {

                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("startDocument");
                }

                processReturn = false;
                ostrDoc = new Base64OutputStream(ostr, false);
            }

            @Override
            public void startElement(final String namespaceURI,
                    final String localName, final String qName,
                    final org.xml.sax.Attributes atts) throws SAXException {

                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace(String.format("startElement [%s]", qName));
                }

                processReturn =
                        qName.equalsIgnoreCase(SmartschoolConstants.XML_ELM_RETURN);
                processData = false;

            }

            @Override
            public void characters(final char[] ch, final int start,
                    final int length) throws SAXException {

                if (connection.isShutdownRequested()) {
                    throw new ShutdownRequestedException();
                }

                if (!this.processReturn) {
                    return;
                }

                try {
                    if (!processData) {

                        initialReturnXml.append(String.valueOf(ch, start,
                                length));

                        int iWlk;
                        String searchWlk;

                        // </filename>
                        searchWlk = "</filename>";
                        iWlk = initialReturnXml.indexOf(searchWlk);
                        if (iWlk >= 0) {

                        }

                        // <data>
                        searchWlk =
                                "<" + SmartschoolConstants.XML_ELM_DATA + ">";
                        iWlk = initialReturnXml.indexOf(searchWlk);

                        if (iWlk >= 0) {

                            if (LOGGER.isTraceEnabled()) {
                                LOGGER.trace(String.format("Found [%s] ",
                                        searchWlk));
                            }

                            processData = true;

                            for (final int aChar : initialReturnXml.substring(
                                    iWlk + searchWlk.length()).toCharArray()) {
                                ostrDoc.write(aChar);
                            }

                        }

                    } else {

                        for (int i = start; i < length; i++) {

                            final char chWlk = ch[i];

                            /*
                             * The '<' character marks the end of the <data>
                             * content.
                             */
                            if (chWlk == '<') {
                                ostrDoc.close();
                                processReturn = false;
                                break;
                            } else {
                                ostrDoc.write(chWlk);
                            }

                        }
                    }

                } catch (IOException e) {
                    throw new SpException(e.getMessage());
                }

            }

            @Override
            public void endElement(final String uri, final String localName,
                    final String qName) throws SAXException {

                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace(String.format("endElement [%s]", qName));
                }
                /*
                 * Any endElement will end the document download processing.
                 */
                processReturn = false;
            }

            @Override
            public void endDocument() throws SAXException {
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("endDocument");
                }
                IOUtils.closeQuietly(ostrDoc);
            }
        };

        final SAXParserFactory spf = SAXParserFactory.newInstance();
        final SAXParser saxParser = spf.newSAXParser();
        final XMLReader xmlReader = saxParser.getXMLReader();

        xmlReader.setContentHandler(saxHandler);
        final InputSource input = new InputSource(istr);

        xmlReader.parse(input);

    }

    /**
     * Creates a {@link SOAPMessage} request to get the print jobs.
     *
     * @param request
     *            The {@link SmartschoolRequestEnum}.
     * @param password
     *            The password for the request
     * @return The {@link SOAPMessage}.
     */
    private static SOAPMessage createPrintJobsRequest(
            final SmartschoolRequestEnum request, final char[] password) {

        final SOAPMessage message;

        try {
            message = MessageFactory.newInstance().createMessage();
            final SOAPHeader header = message.getSOAPHeader();

            header.detachNode();

            final SOAPBody body = message.getSOAPBody();
            final QName bodyName = new QName(request.getSoapName());
            final SOAPBodyElement bodyElement = body.addBodyElement(bodyName);
            final SOAPElement elementPassword =
                    bodyElement
                            .addChildElement(SmartschoolConstants.XML_ELM_PWD);
            elementPassword.addTextNode(String.valueOf(password));

        } catch (SOAPException e) {
            throw new SpException(e.getMessage());
        }

        return message;
    }

    /**
     * Creates a {@link SOAPMessage} request to get the print job document.
     *
     * @param request
     *            The {@link SmartschoolRequestEnum}.
     * @param documentId
     *            The unique identification if the document.
     * @param password
     *            The password for the request
     * @return The {@link SOAPMessage}.
     */
    private static SOAPMessage createGetDocumentRequest(
            final SmartschoolRequestEnum request, final String documentId,
            final char[] password) {

        final SOAPMessage message;

        try {
            message = MessageFactory.newInstance().createMessage();
            final SOAPHeader header = message.getSOAPHeader();

            header.detachNode();

            final SOAPBody body = message.getSOAPBody();
            final QName bodyName = new QName(request.getSoapName());
            final SOAPBodyElement bodyElement = body.addBodyElement(bodyName);

            final SOAPElement elementPassword =
                    bodyElement
                            .addChildElement(SmartschoolConstants.XML_ELM_PWD);
            elementPassword.addTextNode(String.valueOf(password));

            final SOAPElement uid =
                    bodyElement
                            .addChildElement(SmartschoolConstants.XML_ELM_UID);
            uid.addTextNode(documentId);

        } catch (SOAPException e) {
            throw new SpException(e.getMessage());
        }

        return message;
    }

    /**
     *
     * @param documentId
     * @return
     * @throws SOAPException
     */
    private static SOAPMessage createSetDocumentStatusRequest(
            final SmartschoolConnection connection,
            final SmartschoolRequestEnum request, final String documentId,
            final String status, final String comment) throws SOAPException {

        final SOAPMessage message =
                MessageFactory.newInstance().createMessage();

        final SOAPHeader header = message.getSOAPHeader();

        header.detachNode();

        final SOAPBody body = message.getSOAPBody();

        final QName bodyName = new QName(request.getSoapName());
        final SOAPBodyElement bodyElement = body.addBodyElement(bodyName);

        //
        final SOAPElement password =
                bodyElement.addChildElement(SmartschoolConstants.XML_ELM_PWD);

        password.addTextNode(String.valueOf(connection.getPassword()));

        //
        final SOAPElement uid =
                bodyElement.addChildElement(SmartschoolConstants.XML_ELM_UID);

        uid.addTextNode(documentId);

        //
        final SOAPElement xmlElement =
                bodyElement.addChildElement(SmartschoolConstants.XML_ELM_XML);

        final DocumentStatusIn docStat = new DocumentStatusIn();
        docStat.setDocumentId(documentId);
        docStat.setComment(comment);
        docStat.setCode(status);

        try {
            xmlElement.addTextNode(docStat.asXmlString());
        } catch (JAXBException e) {
            throw new SpException(e.getMessage(), e);
        }

        return message;
    }

    /**
     * Sends a SOAP message.
     *
     * @param connection
     *            The {@link SmartschoolConnection}.
     * @param request
     *            The {@link SmartschoolRequestEnum}.
     * @param message
     *            The {@link SOAPMessage} to send.
     * @return The {@link SOAPElement} return element.
     * @throws SmartschoolException
     *             When SmartSchool returns a fault.
     * @throws SOAPException
     *             When SOAP (connection) error.
     */
    private SOAPElement sendMessage(final SmartschoolConnection connection,
            final SmartschoolRequestEnum request, final SOAPMessage message)
            throws SmartschoolException, SOAPException {

        final SOAPMessage response;

        try {
            response =
                    connection.getConnection().call(message,
                            connection.getEndpointUrl(request));

        } catch (SOAPException e) {
            if (SmartschoolLogger.isEnabled()) {
                SmartschoolLogger.logError(request, e);
            }
            throw e;
        }

        final SOAPBody responseBody = response.getSOAPBody();

        final SOAPBodyElement responseElement =
                (SOAPBodyElement) responseBody.getChildElements().next();

        final SOAPElement returnElement =
                (SOAPElement) responseElement.getChildElements().next();

        if (SmartschoolLogger.isEnabled() || LOGGER.isTraceEnabled()) {

            final String xml = getXmlFromSOAPMessage(response);

            SmartschoolLogger.logRequest(xml);

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(xml);
            }
        }

        if (responseBody.getFault() != null) {

            throw new SmartschoolException(returnElement.getValue() + " "
                    + responseBody.getFault().getFaultString());

        } else {

            if (SmartschoolLogger.isEnabled() || LOGGER.isTraceEnabled()) {

                SmartschoolLogger.logResponse(returnElement.getValue());

                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Return value: " + returnElement.getValue());
                }
            }
        }

        return returnElement;
    }

    /**
     * Gets a string representation of a {@link SOAPMessage} for debugging
     * purposes.
     *
     * @param msg
     *            The message
     * @return The XML string.
     */
    private static String getXmlFromSOAPMessage(final SOAPMessage msg) {
        final ByteArrayOutputStream byteArrayOS = new ByteArrayOutputStream();
        try {
            msg.writeTo(byteArrayOS);
        } catch (SOAPException | IOException e) {
            throw new SpException(e.getMessage());
        }
        return new String(byteArrayOS.toByteArray());
    }

    /**
     * @return The {@link RequestConfig}.
     */
    private static RequestConfig buildRequestConfig() {
        final ConfigManager cm = ConfigManager.instance();

        final Key connectTimeout;
        final Key socketTimeout;

        connectTimeout = Key.SMARTSCHOOL_SOAP_CONNECT_TIMEOUT_MILLIS;
        socketTimeout = Key.SMARTSCHOOL_SOAP_SOCKET_TIMEOUT_MILLIS;

        return RequestConfig.custom()
                .setConnectTimeout(cm.getConfigInt(connectTimeout))
                .setSocketTimeout(cm.getConfigInt(socketTimeout)).build();

    }

    @Override
    public IppQueue getSmartSchoolQueue() {
        return ippQueueDAO().find(ReservedIppQueueEnum.SMARTSCHOOL);
    }

    @Override
    public Account createSharedAccountTemplate(final Account parent) {

        final Account account = new org.savapage.core.jpa.Account();

        account.setParent(parent);

        account.setBalance(BigDecimal.ZERO);
        account.setOverdraft(BigDecimal.ZERO);
        account.setRestricted(false);
        account.setUseGlobalOverdraft(false);

        account.setAccountType(Account.AccountTypeEnum.SHARED.toString());
        account.setComments(Account.CommentsEnum.COMMENT_OPTIONAL.toString());
        account.setInvoicing(Account.InvoicingEnum.ALWAYS_ON.toString());
        account.setDeleted(false);
        account.setDisabled(false);

        account.setCreatedBy(ServiceContext.getActor());
        account.setCreatedDate(ServiceContext.getTransactionDate());

        return account;
    }

    @Override
    public String composeSharedChildAccountNameForKlas(
            final SmartschoolConnection connection, final String klas) {
        return String.format(SHARED_ACCOUNT_FOR_KLAS_FORMAT,
                connection.getAccountName(), klas);
    }

    @Override
    public String getKlasFromComposedAccountName(final String accountName) {

        final String[] parts = StringUtils.split(accountName, '.');

        if (parts.length < 3) {
            return null;
        }

        return parts[parts.length - 1];
    }

    /**
     * Creates an {@link AccountTrxInfo} object for an {@link Account}.
     *
     * @param account
     *            The {@link Account}.
     * @param copies
     *            The number of copies to print.
     * @param extDetails
     *            Free format details from external source.
     * @return The {@link AccountTrxInfo}.
     */
    private static AccountTrxInfo createAccountTrxInfo(final Account account,
            final Integer copies, final String extDetails) {

        final AccountTrxInfo accountTrxInfo = new AccountTrxInfo();

        accountTrxInfo.setWeight(copies);
        accountTrxInfo.setAccount(account);
        accountTrxInfo.setExtDetails(extDetails);

        return accountTrxInfo;
    }

    @Override
    public AccountTrxInfoSet createPrintInAccountTrxInfoSet(
            final SmartschoolConnection connection, final Account parent,
            final int nTotCopies, final Map<String, Integer> klasCopies,
            final Map<String, Integer> userCopies,
            final Map<String, String> userKlas) {

        final AccountTrxInfoSet infoSet = new AccountTrxInfoSet(nTotCopies);

        final List<AccountTrxInfo> accountTrxInfoList = new ArrayList<>();
        infoSet.setAccountTrxInfoList(accountTrxInfoList);

        final Account accountTemplate = createSharedAccountTemplate(parent);

        /*
         * Shared Accounts.
         */
        for (final Entry<String, Integer> entry : klasCopies.entrySet()) {

            final Account account =
                    accountingService().lazyGetSharedAccount(
                            this.composeSharedChildAccountNameForKlas(
                                    connection, entry.getKey()),
                            accountTemplate);

            accountTrxInfoList.add(createAccountTrxInfo(account,
                    entry.getValue(), null));
        }

        /*
         * User Accounts.
         */
        for (final Entry<String, Integer> entry : userCopies.entrySet()) {

            final String userId = entry.getKey();
            final User user = userDAO().findActiveUserByUserId(userId);

            final UserAccount userAccount =
                    accountingService().lazyGetUserAccount(user,
                            AccountTypeEnum.USER);

            accountTrxInfoList.add(createAccountTrxInfo(
                    userAccount.getAccount(), entry.getValue(),
                    userKlas.get(userId)));
        }

        return infoSet;
    }

    @Override
    public Account getSharedParentAccount() {
        return accountingService().lazyGetSharedAccount(
                this.getSharedParentAccountName(),
                createSharedAccountTemplate(null));
    }

    @Override
    public String getSharedParentAccountName() {
        return ConfigManager.instance().getConfigValue(
                Key.SMARTSCHOOL_PAPERCUT_ACCOUNT_SHARED_PARENT);
    }

    @Override
    public String getSharedJobsAccountName() {
        return ConfigManager.instance().getConfigValue(
                Key.SMARTSCHOOL_PAPERCUT_ACCOUNT_SHARED_CHILD_JOBS);
    }
}
