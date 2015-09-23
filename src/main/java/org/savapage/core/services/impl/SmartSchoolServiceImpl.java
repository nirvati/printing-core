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
package org.savapage.core.services.impl;

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
import org.savapage.core.dao.helpers.ReservedIppQueueEnum;
import org.savapage.core.jpa.Account;
import org.savapage.core.jpa.Account.AccountTypeEnum;
import org.savapage.core.jpa.IppQueue;
import org.savapage.core.jpa.User;
import org.savapage.core.jpa.UserAccount;
import org.savapage.core.print.smartschool.SmartSchoolException;
import org.savapage.core.print.smartschool.SmartSchoolLogger;
import org.savapage.core.print.smartschool.SmartSchoolPrintStatusEnum;
import org.savapage.core.print.smartschool.xml.Document;
import org.savapage.core.print.smartschool.xml.Jobticket;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.SmartSchoolService;
import org.savapage.core.services.helpers.AccountTrxInfo;
import org.savapage.core.services.helpers.AccountTrxInfoSet;
import org.savapage.core.services.helpers.SmartSchoolConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 * @author Datraverse B.V.
 *
 */
public final class SmartSchoolServiceImpl extends AbstractService implements
        SmartSchoolService {

    /**
     *
     */
    private static final Logger LOGGER = LoggerFactory
            .getLogger(SmartSchoolServiceImpl.class);

    /**
     * The name of the parent {@link Account} for all child "klas" accounts.
     */
    private static final String SHARED_PARENT_ACCOUNT_NAME = "Smartschool";

    /**
     * The name of the child {@link Account} for all SmartScholl Jobs.
     */
    private static final String SHARED_ACCOUNT_JOBS = "Jobs";

    /**
     * Format string to be used in {@link String#format(String, Object...)}. The
     * first {@code %s} is a placeholder for the SmartSchool account name. The
     * second {@code %s} is a placeholder for the klas name.
     */
    private static final String SHARED_ACCOUNT_FOR_KLAS_FORMAT = "%s.Klas.%s";

    @Override
    public Map<String, SmartSchoolConnection> createConnections()
            throws SOAPException {

        final Map<String, SmartSchoolConnection> connectionMap =
                new HashMap<>();

        final ConfigManager cm = ConfigManager.instance();

        SmartSchoolConnection connection;

        if (cm.isConfigValue(Key.SMARTSCHOOL_1_ENABLE)) {

            connection =
                    new SmartSchoolConnection(
                            cm.getConfigValue(Key.SMARTSCHOOL_1_SOAP_PRINT_ENDPOINT_URL),
                            cm.getConfigValue(
                                    Key.SMARTSCHOOL_1_SOAP_PRINT_ENDPOINT_PASSWORD)
                                    .toCharArray(),
                            cm.getConfigValue(Key.SMARTSCHOOL_1_SOAP_PRINT_PROXY_PRINTER),
                            cm.getConfigValue(Key.SMARTSCHOOL_1_SOAP_PRINT_PROXY_PRINTER_GRAYSCALE),
                            cm.isConfigValue(Key.SMARTSCHOOL_1_SOAP_PRINT_CHARGE_TO_STUDENTS));

            connectionMap.put(connection.getAccountName(), connection);
        }

        if (cm.isConfigValue(Key.SMARTSCHOOL_2_ENABLE)) {

            connection =
                    new SmartSchoolConnection(
                            cm.getConfigValue(Key.SMARTSCHOOL_2_SOAP_PRINT_ENDPOINT_URL),
                            cm.getConfigValue(
                                    Key.SMARTSCHOOL_2_SOAP_PRINT_ENDPOINT_PASSWORD)
                                    .toCharArray(),
                            cm.getConfigValue(Key.SMARTSCHOOL_2_SOAP_PRINT_PROXY_PRINTER),
                            cm.getConfigValue(Key.SMARTSCHOOL_2_SOAP_PRINT_PROXY_PRINTER_GRAYSCALE),
                            cm.isConfigValue(Key.SMARTSCHOOL_2_SOAP_PRINT_CHARGE_TO_STUDENTS));
            connectionMap.put(connection.getAccountName(), connection);
        }

        return connectionMap;
    }

    @Override
    public Jobticket getJobticket(final SmartSchoolConnection connection)
            throws SmartSchoolException, SOAPException {

        final SOAPElement returnElement =
                sendMessage(connection,
                        createPrintJobsRequest(connection.getPassword()));

        final Jobticket jobTicket;

        try {
            jobTicket = Jobticket.createFromXml(returnElement.getValue());
        } catch (JAXBException e) {
            throw new SpException(e);
        }
        return jobTicket;
    }

    @Override
    public void reportDocumentStatus(final SmartSchoolConnection connection,
            final String documentId, final SmartSchoolPrintStatusEnum status,
            final String comment) throws SmartSchoolException, SOAPException {

        sendMessage(
                connection,
                createSetDocumentStatusRequest(connection, documentId,
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
    public File downloadDocument(final SmartSchoolConnection connection,
            final User user, final Document document, final UUID uuid)
            throws IOException, ShutdownException {

        final StringBuffer downloadedFilePath = new StringBuffer(128);

        /*
         * Do NOT download in user safepages .temp directory, since that might
         * not exist (because this is the first user print). A lazy create
         * requires a User lock, which we do not want at this moment.
         */
        downloadedFilePath.append(ConfigManager.getAppTmpDir())
                .append(File.separatorChar).append(uuid.toString()).append("_")
                .append(document.getName());

        final File downloadedFile =
                this.getDownloadFile(document.getName(), uuid);

        boolean exception = true;

        try {
            downloadDocument(connection, user, document, downloadedFile);
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
     * @param user
     * @param documentId
     * @param downloadedFile
     * @throws IOException
     * @throws ShutdownException
     */
    private static void downloadDocument(
            final SmartSchoolConnection connection, final User user,
            final Document document, final File downloadedFile)
            throws IOException, ShutdownException {

        final SOAPMessage soapMsg =
                createGetDocumentRequest(document.getId(),
                        connection.getPassword());

        final ContentType contentType = ContentType.create("application/xml");

        final HttpEntity entity =
                new StringEntity(getXmlFromSOAPMessage(soapMsg), contentType);

        final HttpPost httppost = new HttpPost(connection.getEndpointUri());

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
     *
     * @param istr
     * @param ostr
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     * @throws ShutdownRequestedException
     */
    private static void saxParseDocumentData(
            final SmartSchoolConnection connection, final InputStream istr,
            final OutputStream ostr) throws ParserConfigurationException,
            SAXException, IOException, ShutdownRequestedException,
            ShutdownRequestedException {

        final DefaultHandler saxHandler = new DefaultHandler() {

            private boolean processReturn;
            private boolean processData;
            private final StringBuilder initialReturnXml = new StringBuilder(
                    256);

            private String fileName;
            private String fileSize;
            private String md5sum;

            private OutputStream ostrDoc = null;

            @Override
            public void startDocument() throws SAXException {

                processReturn = false;
                ostrDoc = new Base64OutputStream(ostr, false);
            }

            @Override
            public void startElement(String namespaceURI, String localName,
                    String qName, org.xml.sax.Attributes atts)
                    throws SAXException {

                processReturn = qName.equalsIgnoreCase("return");
                processData = false;

            }

            @Override
            public void characters(final char[] ch, final int start,
                    final int length) throws SAXException {

                if (connection.isShutdownRequested()) {
                    throw new ShutdownRequestedException();
                }

                if (this.processReturn) {

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
                            searchWlk = "<data>";
                            iWlk = initialReturnXml.indexOf(searchWlk);

                            if (iWlk >= 0) {

                                processData = true;

                                for (final int aChar : initialReturnXml
                                        .substring(iWlk + searchWlk.length())
                                        .toCharArray()) {
                                    ostrDoc.write(aChar);
                                }

                            }

                        } else {

                            for (int i = start; i < length; i++) {

                                final char chWlk = ch[i];

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
            }

            @Override
            public void endElement(String uri, String localName, String qName)
                    throws SAXException {
                processReturn = false;
            }

            @Override
            public void endDocument() throws SAXException {
                if (ostrDoc != null) {
                    try {
                        ostrDoc.close();
                    } catch (IOException e) {
                        throw new SpException(e.getMessage());
                    }
                }
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
     * @param password
     *            The password for the request
     * @return The {@link SOAPMessage}.
     */
    private static SOAPMessage createPrintJobsRequest(final char[] password) {

        final SOAPMessage message;

        try {
            message = MessageFactory.newInstance().createMessage();
            final SOAPHeader header = message.getSOAPHeader();

            header.detachNode();

            final SOAPBody body = message.getSOAPBody();
            final QName bodyName = new QName("getPrintJobs");
            final SOAPBodyElement bodyElement = body.addBodyElement(bodyName);
            final SOAPElement elementPassword =
                    bodyElement.addChildElement("pwd");
            elementPassword.addTextNode(String.valueOf(password));

        } catch (SOAPException e) {
            throw new SpException(e.getMessage());
        }

        return message;
    }

    /**
     * Creates a {@link SOAPMessage} request to get the print job document.
     *
     * @param documentId
     *            The unique identification if the document.
     * @param password
     *            The password for the request
     * @return The {@link SOAPMessage}.
     */
    private static SOAPMessage createGetDocumentRequest(
            final String documentId, final char[] password) {

        final SOAPMessage message;

        try {
            message = MessageFactory.newInstance().createMessage();
            final SOAPHeader header = message.getSOAPHeader();

            header.detachNode();

            final SOAPBody body = message.getSOAPBody();
            final QName bodyName = new QName("getDocument");
            final SOAPBodyElement bodyElement = body.addBodyElement(bodyName);

            final SOAPElement elementPassword =
                    bodyElement.addChildElement("pwd");
            elementPassword.addTextNode(String.valueOf(password));

            final SOAPElement uid = bodyElement.addChildElement("uid");
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
            final SmartSchoolConnection connection, final String documentId,
            final String status, final String comment) throws SOAPException {

        final SOAPMessage message =
                MessageFactory.newInstance().createMessage();

        final SOAPHeader header = message.getSOAPHeader();

        header.detachNode();

        final SOAPBody body = message.getSOAPBody();

        final QName bodyName = new QName("setDocumentStatus");
        final SOAPBodyElement bodyElement = body.addBodyElement(bodyName);

        /*
        *
        */
        final SOAPElement password = bodyElement.addChildElement("pwd");
        password.addTextNode(String.valueOf(connection.getPassword()));

        /*
        *
        */
        final SOAPElement uid = bodyElement.addChildElement("uid");
        uid.addTextNode(documentId);

        /*
        *
        */
        final SOAPElement xmlElement = bodyElement.addChildElement("xml");
        xmlElement.addTextNode("<status type=\"print\"><id>" + documentId
                + "</id><code>" + status + "</code><comment>" + comment
                + "</comment></status>");

        return message;
    }

    /**
     * Sends a SOAP message.
     *
     * @param connection
     *            The {@link SmartSchoolConnection}.
     * @param message
     *            The {@link SOAPMessage} to send.
     * @return The {@link SOAPElement} return element.
     * @throws SmartSchoolException
     *             When SmartSchool returns a fault.
     * @throws SOAPException
     *             When SOAP (connection) error.
     */
    private SOAPElement sendMessage(final SmartSchoolConnection connection,
            final SOAPMessage message) throws SmartSchoolException,
            SOAPException {

        final SOAPMessage response =
                connection.getConnection().call(message,
                        connection.getEndpointUrl());

        final SOAPBody responseBody = response.getSOAPBody();

        final SOAPBodyElement responseElement =
                (SOAPBodyElement) responseBody.getChildElements().next();

        final SOAPElement returnElement =
                (SOAPElement) responseElement.getChildElements().next();

        if (SmartSchoolLogger.isEnabled() || LOGGER.isTraceEnabled()) {

            final String xml = getXmlFromSOAPMessage(response);

            SmartSchoolLogger.logRequest(xml);

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(xml);
            }

        }

        if (responseBody.getFault() != null) {

            throw new SmartSchoolException(returnElement.getValue() + " "
                    + responseBody.getFault().getFaultString());

        } else {

            if (SmartSchoolLogger.isEnabled() || LOGGER.isTraceEnabled()) {

                SmartSchoolLogger.logResponse(returnElement.getValue());

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
            final SmartSchoolConnection connection, final String klas) {
        return String.format(SHARED_ACCOUNT_FOR_KLAS_FORMAT,
                connection.getAccountName(), klas);
    }

    /**
     *
     * @param accountName
     *            The account name.
     * @return {@code null} when not found.
     */
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
     * @return The {@link AccountTrxInfo}.
     */
    private static AccountTrxInfo createAccountTrxInfo(final Account account,
            final Integer copies) {

        final AccountTrxInfo accountTrxInfo = new AccountTrxInfo();

        accountTrxInfo.setWeight(copies);
        accountTrxInfo.setAccount(account);

        return accountTrxInfo;
    }

    @Override
    public AccountTrxInfoSet createPrintInAccountTrxInfoSet(
            SmartSchoolConnection connection, final Account parent,
            final Map<String, Integer> klasCopies,
            final Map<String, Integer> userCopies) {

        final AccountTrxInfoSet infoSet = new AccountTrxInfoSet();

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
                    entry.getValue()));
        }

        /*
         * User Accounts.
         */
        for (final Entry<String, Integer> entry : userCopies.entrySet()) {

            final User user = userDAO().findActiveUserByUserId(entry.getKey());

            final UserAccount userAccount =
                    accountingService().lazyGetUserAccount(user,
                            AccountTypeEnum.USER);

            accountTrxInfoList.add(createAccountTrxInfo(
                    userAccount.getAccount(), entry.getValue()));
        }

        return infoSet;
    }

    @Override
    public Account getSharedParentAccount() {
        return accountingService().lazyGetSharedAccount(
                SHARED_PARENT_ACCOUNT_NAME, createSharedAccountTemplate(null));
    }

    @Override
    public String getSharedParentAccountName() {
        return SHARED_PARENT_ACCOUNT_NAME;
    }

    @Override
    public String getSharedJobsAccountName() {
        return SHARED_ACCOUNT_JOBS;
    }
}
