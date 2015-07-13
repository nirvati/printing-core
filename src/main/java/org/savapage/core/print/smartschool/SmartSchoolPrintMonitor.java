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
package org.savapage.core.print.smartschool;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.time.DateUtils;
import org.savapage.core.OutputProducer;
import org.savapage.core.ShutdownException;
import org.savapage.core.SpException;
import org.savapage.core.cometd.AdminPublisher;
import org.savapage.core.cometd.PubLevelEnum;
import org.savapage.core.cometd.PubTopicEnum;
import org.savapage.core.community.CommunityDictEnum;
import org.savapage.core.concurrent.ReadWriteLockEnum;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.dao.AccountDao;
import org.savapage.core.dao.AccountTrxDao;
import org.savapage.core.dao.DaoContext;
import org.savapage.core.dao.DocInOutDao;
import org.savapage.core.dao.DocLogDao;
import org.savapage.core.dao.PrinterDao;
import org.savapage.core.dao.UserAccountDao;
import org.savapage.core.dao.UserDao;
import org.savapage.core.dao.helpers.DocLogProtocolEnum;
import org.savapage.core.dao.helpers.PrintModeEnum;
import org.savapage.core.doc.DocContent;
import org.savapage.core.dto.IppMediaSourceCostDto;
import org.savapage.core.ipp.IppMediaSizeEnum;
import org.savapage.core.job.AbstractJob;
import org.savapage.core.job.SmartSchoolPrintMonitorJob;
import org.savapage.core.jpa.Account.AccountTypeEnum;
import org.savapage.core.jpa.AccountTrx;
import org.savapage.core.jpa.DocLog;
import org.savapage.core.jpa.DocOut;
import org.savapage.core.jpa.IppQueue;
import org.savapage.core.jpa.PrintOut;
import org.savapage.core.jpa.Printer;
import org.savapage.core.jpa.User;
import org.savapage.core.papercut.PaperCutDbProxy;
import org.savapage.core.papercut.PaperCutException;
import org.savapage.core.papercut.PaperCutPrinterUsageLog;
import org.savapage.core.papercut.PaperCutServerProxy;
import org.savapage.core.papercut.PaperCutUser;
import org.savapage.core.pdf.PdfSecurityException;
import org.savapage.core.pdf.SpPdfPageProps;
import org.savapage.core.print.proxy.IppConnectException;
import org.savapage.core.print.proxy.ProxyPrintDocReq;
import org.savapage.core.print.proxy.ProxyPrintException;
import org.savapage.core.print.proxy.ProxyPrintJobChunk;
import org.savapage.core.print.proxy.ProxyPrintJobChunkInfo;
import org.savapage.core.print.proxy.ProxyPrintJobChunkRange;
import org.savapage.core.print.server.DocContentPrintException;
import org.savapage.core.print.smartschool.xml.Account;
import org.savapage.core.print.smartschool.xml.Accounts;
import org.savapage.core.print.smartschool.xml.Billinginfo;
import org.savapage.core.print.smartschool.xml.Document;
import org.savapage.core.print.smartschool.xml.Documents;
import org.savapage.core.print.smartschool.xml.Jobticket;
import org.savapage.core.print.smartschool.xml.Processinfo;
import org.savapage.core.print.smartschool.xml.Requester;
import org.savapage.core.print.smartschool.xml.Requestinfo;
import org.savapage.core.print.smartschool.xml.SmartSchoolRoleEnum;
import org.savapage.core.services.AccountingService;
import org.savapage.core.services.DocLogService;
import org.savapage.core.services.PaperCutService;
import org.savapage.core.services.ProxyPrintService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.SmartSchoolService;
import org.savapage.core.services.UserService;
import org.savapage.core.services.helpers.AccountTrxInfoSet;
import org.savapage.core.services.helpers.DocContentPrintInInfo;
import org.savapage.core.services.helpers.ExternalSupplierEnum;
import org.savapage.core.services.helpers.ExternalSupplierInfo;
import org.savapage.core.services.helpers.PrinterAttrLookup;
import org.savapage.core.services.helpers.SmartSchoolConnection;
import org.savapage.core.users.IUserSource;
import org.savapage.core.util.DateUtil;
import org.savapage.core.util.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;

/**
 *
 * @author Datraverse B.V.
 *
 */
public final class SmartSchoolPrintMonitor {

    /**
     *
     */
    private static int getJobTickerCounter = 0;

    /**
     * .
     */
    private static final String JOB_NAME_INFO_SEPARATOR = ".";

    /**
     * .
     */
    private static final String JOBS_COMMENT_FIELD_SEPARATOR = " | ";
    private static final String JOBS_COMMENT_FIELD_SEPARATOR_FIRST = "";
    private static final String JOBS_COMMENT_FIELD_SEPARATOR_LAST = "";

    /**
     * .
     */
    private static final char JOBS_COMMENT_USER_CLASS_SEPARATOR = '@';

    /**
     * Fixed values for simulation "klassen".
     */
    private static final String SIMULATION_CLASS_1 = "simulatie.1A1";
    private static final String SIMULATION_CLASS_2 = "simulatie.1A2";

    /**
     * The logger.
     */
    private static final Logger LOGGER = LoggerFactory
            .getLogger(SmartSchoolPrintMonitor.class);

    private static final String MSG_COMMENT_PRINT_PENDING_PAPERCUT =
            "Afdrukopdracht staat klaar in PaperCut.";

    private static final String MSG_COMMENT_PRINT_PENDING_SAVAPAGE =
            "Afdrukopdracht staat klaar in "
                    + CommunityDictEnum.SAVAPAGE.getWord()
                    + ". Ga naar de WebApp voor verdere acties.";

    private static final String MSG_COMMENT_PRINT_COMPLETED =
            "Document is met succes afgedrukt.";

    private static final String MSG_COMMENT_PRINT_QUEUED =
            "Document is met succes in de wachtrij geplaatst.";

    private static final String MSG_COMMENT_PRINT_CANCELLED =
            "Afdrukopdracht is geannuleerd.";

    private static final String MSG_COMMENT_PRINT_EXPIRED =
            "Afdrukopdracht is verlopen.";

    private static final String MSG_COMMENT_PRINT_CANCELLED_DOC_TYPE =
            "Afdrukopdracht is geannuleerd: "
                    + "document type kan niet worden afgedrukt.";

    private static final String MSG_COMMENT_PRINT_ERROR_NO_COPIES =
            "Geen kopieen gespecificeerd.";

    private static final String MSG_COMMENT_PRINT_ERROR_UNKNOWN_USER_BOTH =
            "Aanvrager \"%s\" is onbekend in "
                    + CommunityDictEnum.SAVAPAGE.getWord() + " en PaperCut.";

    private static final String MSG_COMMENT_PRINT_ERROR_UNKNOWN_USER_SAVAPAGE =
            "Aanvrager \"%s\" is onbekend in "
                    + CommunityDictEnum.SAVAPAGE.getWord() + ".";

    private static final String MSG_COMMENT_PRINT_ERROR_UNKNOWN_USER_PAPERCUT =
            "Aanvrager \"%s\" is onbekend in PaperCut.";

    /**
     * The map with SmartSchool account connections with key is SmartSchool
     * account.
     */
    private final Map<String, SmartSchoolConnection> connectionMap;

    /**
     * .
     */
    private final boolean simulationMode;

    /**
     * Indicating if monitor is connected.
     */
    private volatile boolean isConnected = true;

    /**
     * Indicating if monitor is busy with processing.
     */
    private volatile boolean isProcessing = false;

    /**
     * The connection that is processing.
     */
    private SmartSchoolConnection processingConnection = null;

    /**
    *
    */
    private static final AccountingService ACCOUNTING_SERVICE = ServiceContext
            .getServiceFactory().getAccountingService();

    /**
     * .
     */
    private static final DocLogService DOCLOG_SERVICE = ServiceContext
            .getServiceFactory().getDocLogService();

    /**
     * .
     */
    private static final ProxyPrintService PROXY_PRINT_SERVICE = ServiceContext
            .getServiceFactory().getProxyPrintService();

    /**
     * .
     */
    private static final SmartSchoolService SMARTSCHOOL_SERVICE =
            ServiceContext.getServiceFactory().getSmartSchoolService();

    /**
     * .
     */
    private static final PaperCutService PAPERCUT_SERVICE = ServiceContext
            .getServiceFactory().getPaperCutService();

    /**
    *
    */
    private static final UserService USER_SERVICE = ServiceContext
            .getServiceFactory().getUserService();

    /**
     *
     */
    private final IppQueue smartSchoolQueue;

    /**
     * .
     */
    private final PaperCutServerProxy papercutServerProxy;

    /**
     * .
     */
    private final PaperCutDbProxy papercutDbProxy;

    /**
     * {@link DocLogDao.ListFilter} for retrieving
     * {@link SmartSchoolPrintStatusEnum#PENDING_EXT} SmartSchool jobs.
     */
    private final DocLogDao.ListFilter listFilterPendingExt =
            new DocLogDao.ListFilter();

    /**
     *
     * @param paperCutProxy
     *            The {@link PaperCutServerProxy}.
     * @param paperDbProxy
     *            The {@link PaperCutDbProxy}.
     * @param simulationMode
     *            {@code true} when in simulation mode
     * @throws SOAPException
     *             When the {@link SOAPConnection} cannot be established.
     */
    public SmartSchoolPrintMonitor(final PaperCutServerProxy paperCutProxy,
            final PaperCutDbProxy papercutDbProxy, final boolean simulationMode)
            throws SOAPException {

        this.simulationMode = simulationMode;

        this.isConnected = true;

        this.connectionMap = SMARTSCHOOL_SERVICE.createConnections();

        this.smartSchoolQueue = SMARTSCHOOL_SERVICE.getSmartSchoolQueue();
        this.papercutServerProxy = paperCutProxy;
        this.papercutDbProxy = papercutDbProxy;

        listFilterPendingExt
                .setExternalSupplier(ExternalSupplierEnum.SMARTSCHOOL);

        listFilterPendingExt
                .setExternalStatus(SmartSchoolPrintStatusEnum.PENDING_EXT
                        .toString());

        listFilterPendingExt.setProtocol(DocLogProtocolEnum.IPP);
    }

    /**
     *
     */
    public static void resetJobTickerCounter() {
        getJobTickerCounter = 0;
    }

    /**
     *
     */
    public void connect() {
        // no code intended
    }

    /**
     * Gracefully stops the polling, but any document download is interrupted.
     *
     * @throws InterruptedException
     *             When interrupted while handling the interrupt.
     */
    public void interrupt() throws InterruptedException {

        for (final SmartSchoolConnection connection : this.connectionMap
                .values()) {
            connection.setShutdownRequested(true);
        }

        disconnect();
    }

    /**
     * Gracefully stops the polling.
     * <p>
     * <b>Important</b>: this method is synchronized since it can be called from
     * multiple threads. E.g. as result of an exception (in the finally block)
     * or as a result from a Quartz scheduler job interrupt. See the
     * {@link AbstractJob#interrupt()} implementation, and its handling in
     * {@link SmartSchoolPrintMonitorJob}.
     * </p>
     * <p>
     * Note: this method is idempotent, i.e. it can be called more than once
     * resulting in the same end-state.
     * </p>
     *
     * @throws InterruptedException
     *             When interrupted while handling the disconnect.
     */
    public synchronized void disconnect() throws InterruptedException {

        /*
         * Wait for processing to finish.
         */
        waitForProcessing(this, 1000L);

        int nActions = 0;

        if (this.isConnected) {

            for (final SmartSchoolConnection connection : this.connectionMap
                    .values()) {
                connection.close();
            }

            this.isConnected = false;

            nActions++;
        }

        if (nActions > 0) {
            LOGGER.debug("Disconnected.");
        }

    }

    /**
     * @return {@code true} when PaperCut integration is active.
     */
    private boolean isIntegratedWithPaperCut() {
        return this.papercutServerProxy != null;
    }

    /**
     * Monitors incoming SmartSchool print jobs and print status.
     * <p>
     * <b>Note</b>: This is a blocking call that returns when the maximum
     * duration is reached (or a {@link #disconnect()} is called before
     * returning).
     * </p>
     *
     * @param heartbeatSecs
     *            Number of seconds of a heartbeat.
     * @param pollingHeartbeats
     *            The polling interval in seconds.
     * @param sessionDurationSecs
     *            The duration after which this method returns.
     * @throws InterruptedException
     *             When we are interrupted.
     * @throws SOAPException
     *             When a SOAP communication error occurs.
     * @throws ShutdownException
     *             When we application shutdown is in progress.
     * @throws SmartSchoolException
     *             When SmartSchool return a fault.
     * @throws PaperCutException
     *
     */
    public void monitor(final int heartbeatSecs, final int pollingHeartbeats,
            final int sessionDurationSecs) throws InterruptedException,
            SOAPException, ShutdownException, SmartSchoolException,
            PaperCutException {

        final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

        int heartbeatPollCounter = 0;

        try {

            final long sessionEndTime =
                    System.currentTimeMillis() + DateUtil.DURATION_MSEC_SECOND
                            * sessionDurationSecs;

            final Date sessionEndDate = new Date(sessionEndTime);

            /*
             * Initial poll.
             */
            if (this.isConnected) {
                processJobs(this);
            }

            while (!Thread.interrupted()) {

                /*
                 * Disconnected?
                 */
                if (!this.isConnected) {
                    break;
                }

                /*
                 * Session ended?
                 */
                final Date now = new Date();

                if (now.getTime() > sessionEndTime) {
                    break;
                }

                /*
                 * SmartSchool still enabled?
                 */
                if (!ConfigManager.isSmartSchoolPrintEnabled()) {
                    LOGGER.trace("SmartSchool disabled by administrator.");
                    break;
                }

                /*
                 * Stop if PaperCut integration changed.
                 */
                final boolean isPaperCutEnabled =
                        ConfigManager.instance().isConfigValue(
                                Key.SMARTSCHOOL_PAPERCUT_ENABLE);

                if (isPaperCutEnabled && this.papercutServerProxy == null) {
                    break;
                }

                if (!isPaperCutEnabled && this.papercutServerProxy != null) {
                    break;
                }

                /*
                 *
                 */
                heartbeatPollCounter++;

                if (heartbeatPollCounter < pollingHeartbeats) {

                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace("Waiting... next ["
                                + dateFormat.format(DateUtils.addSeconds(now,
                                        heartbeatSecs)) + "] till ["
                                + dateFormat.format(sessionEndDate) + "] ...");
                    }
                    Thread.sleep(heartbeatSecs * 1000);

                } else if (this.isConnected) {

                    heartbeatPollCounter = 0;

                    processJobs(this);
                }
            }

        } finally {

            disconnect();
        }
    }

    /**
     * @param connection
     *            The {@link SmartSchoolConnection}.
     *
     * @return The job ticket.
     * @throws SmartSchoolException
     *             When SmartSchool reported an error.
     * @throws ShutdownException
     *             When interrupted by a shutdown request.
     */
    private static Jobticket
            getJobticket(final SmartSchoolConnection connection,
                    final boolean simulationMode) throws SmartSchoolException,
                    SOAPException {

        if (simulationMode) {

            getJobTickerCounter++;

            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Simulated getJobTicket().");
            }

            if (getJobTickerCounter > 1) {
                return getJobticketSimulationEmpty();
            } else {
                return getJobticketSimulation();
            }
        }

        return SMARTSCHOOL_SERVICE.getJobticket(connection);
    }

    /**
     * Reports the document status to SmartSchool.
     *
     * @param connection
     *            The {@link SmartSchoolConnection}.
     * @param documentId
     *            The SmartSchooldocument id.
     * @param status
     *            The {@link SmartSchoolPrintStatusEnum}.
     * @param comment
     *            The SmartSchool feedback comment.
     * @throws SmartSchoolException
     * @throws SOAPException
     */
    private static void reportDocumentStatus(
            final SmartSchoolConnection connection, String documentId,
            SmartSchoolPrintStatusEnum status, String comment,
            final boolean simulationMode) throws SmartSchoolException,
            SOAPException {

        if (simulationMode) {

            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Simulated reportDocumentStatus [" + status + "] ["
                        + comment + "].");
            }

        } else {
            SMARTSCHOOL_SERVICE.reportDocumentStatus(connection, documentId,
                    status, comment);
        }
    }

    /**
     * Downloads a document for printing.
     *
     * @param connection
     * @param user
     * @param document
     * @param uuid
     * @return The downloaded {@link File}.
     * @throws IOException
     * @throws ShutdownException
     *             When download was interrupted because of a
     *             {@link SmartSchoolConnection#setShutdownRequested(boolean)}
     *             request.
     */
    private static File downloadDocument(SmartSchoolConnection connection,
            User user, Document document, UUID uuid,
            final boolean simulationMode) throws IOException, ShutdownException {

        if (simulationMode) {

            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Simulated document download.");
            }

            return downloadDocumentSimulation(document, uuid);
        }

        return SMARTSCHOOL_SERVICE.downloadDocument(connection, user, document,
                uuid);
    }

    /**
     *
     * @param document
     * @param uuid
     * @return
     */
    private static File
            downloadDocumentSimulation(Document document, UUID uuid) {

        final File downloadFile =
                SMARTSCHOOL_SERVICE.getDownloadFile(document.getName(), uuid);

        final com.itextpdf.text.Document pdfDoc =
                new com.itextpdf.text.Document();

        try {

            PdfWriter.getInstance(pdfDoc, new FileOutputStream(downloadFile));
            pdfDoc.open();

            final Paragraph para =
                    new Paragraph("\n\n\n\nSmartSchool Simulation",
                            FontFactory.getFont("Courier", 42f));

            para.setAlignment(Element.ALIGN_CENTER);

            pdfDoc.add(para);

        } catch (DocumentException de) {
            System.err.println(de.getMessage());
        } catch (IOException ioe) {
            System.err.println(ioe.getMessage());
        }
        pdfDoc.close();

        return downloadFile;
    }

    /**
     *
     * @return An empty job ticket.
     */
    private static Jobticket getJobticketSimulationEmpty() {

        final Jobticket ticket = new Jobticket();

        ticket.setType("print");
        ticket.setLocation("SIMULATION");

        //
        final Requestinfo reqInfo = new Requestinfo();
        ticket.setRequestinfo(reqInfo);

        reqInfo.setDate("2001-01-01 01:01:01");

        //
        final Documents documents = new Documents();
        ticket.setDocuments(documents);

        //
        return ticket;
    }

    /**
     *
     * @return A simulated job ticket.
     */
    private static Jobticket getJobticketSimulation() {

        final String simulationUser =
                ConfigManager.instance().getConfigValue(
                        Key.SMARTSCHOOL_SIMULATION_USER);

        final Jobticket ticket = new Jobticket();

        ticket.setType("print");
        ticket.setLocation("SIMULATION");

        //
        final Requestinfo reqInfo = new Requestinfo();
        ticket.setRequestinfo(reqInfo);

        reqInfo.setDate("2001-01-01 01:01:01");

        //
        final Documents documents = new Documents();
        ticket.setDocuments(documents);

        final List<Document> documentList = new ArrayList<>();
        documents.setDocument(documentList);

        //
        final Document document = new Document();
        documentList.add(document);

        document.setId(Long.toString(System.currentTimeMillis()));
        document.setComment("Simulation Document (simulation).");
        document.setName("simulate & test.pdf");

        //
        final Requester requester = new Requester();
        document.setRequester(requester);

        requester.setUsername(simulationUser);

        requester.setRole(SmartSchoolRoleEnum.TEACHER.getXmlValue());

        //
        final Processinfo processinfo = new Processinfo();
        document.setProcessinfo(processinfo);

        processinfo.setPapersize("a4");
        processinfo.setDuplex("off");
        processinfo.setRendermode("grayscale");

        //
        final Billinginfo billinginfo = new Billinginfo();
        document.setBillinginfo(billinginfo);

        final Accounts accounts = new Accounts();
        billinginfo.setAccounts(accounts);

        final List<Account> accountList = new ArrayList<>();
        accounts.setAccount(accountList);

        Account account;

        // Account #1
        account = new Account();
        accountList.add(account);

        account.setClazz(SIMULATION_CLASS_1);
        account.setCopies(1);
        account.setExtra(2);
        account.setUsername(ConfigManager.instance().getConfigValue(
                Key.SMARTSCHOOL_SIMULATION_STUDENT_1));
        account.setRole(SmartSchoolRoleEnum.STUDENT.getXmlValue());

        // Account #2
        account = new Account();
        accountList.add(account);

        account.setClazz(SIMULATION_CLASS_2);
        account.setCopies(2);
        account.setExtra(2);
        account.setUsername(ConfigManager.instance().getConfigValue(
                Key.SMARTSCHOOL_SIMULATION_STUDENT_2));
        account.setRole(SmartSchoolRoleEnum.STUDENT.getXmlValue());

        // Account #3
        account = new Account();
        accountList.add(account);

        account.setClazz("");
        account.setCopies(1);
        account.setExtra(0);

        account.setUsername(simulationUser);
        account.setRole(SmartSchoolRoleEnum.TEACHER.getXmlValue());

        //
        return ticket;
    }

    /**
     * Processes the SmartSchool print queue(s) and monitors PaperCut print
     * status.
     *
     * @param monitor
     *            The {@link SmartSchoolPrintMonitor} instance.
     * @throws SOAPException
     *             When connectivity problems.
     * @throws SmartSchoolException
     *             When SmartSchool reported an error.
     * @throws ShutdownException
     *             When interrupted by a shutdown request.
     * @throws PaperCutException
     */
    private static void processJobs(final SmartSchoolPrintMonitor monitor)
            throws SOAPException, SmartSchoolException, ShutdownException,
            PaperCutException {

        /*
         * Any progress on the PaperCut front?
         */
        if (monitor.isIntegratedWithPaperCut()) {

            monitorPaperCut(monitor.connectionMap, monitor.simulationMode,
                    monitor.papercutServerProxy, monitor.papercutDbProxy,
                    monitor.listFilterPendingExt);
        }

        for (final SmartSchoolConnection connection : monitor.connectionMap
                .values()) {

            monitor.processingConnection = connection;

            /*
             * Any jobs to print?
             */
            final Jobticket jobTicket =
                    getJobticket(connection, monitor.simulationMode);

            /*
             * Logging.
             */
            if (LOGGER.isDebugEnabled()) {

                final int nDocs = jobTicket.getDocuments().getDocument().size();

                if (nDocs == 0) {
                    LOGGER.debug("no documents to print.");
                } else {
                    LOGGER.debug("[" + nDocs + "] document(s) to print.");
                }
            }

            /*
             * Process print jobs one-by-one.
             */
            for (final Document doc : jobTicket.getDocuments().getDocument()) {
                processJob(monitor, doc);
            }

        }
    }

    /**
     * Monitors the print job status in PaperCut for
     * {@link SmartSchoolPrintStatusEnum#PENDING_EXT} SmartSchool jobs (i.e jobs
     * that were proxy printed to a PaperCut managed printer).
     *
     * @param connectionMap
     *            The {@link SmartSchoolConnection} map.
     * @param papercutDbProxy
     *            The {@link PaperCutDbProxy}.
     * @throws PaperCutException
     * @throws SOAPException
     * @throws SmartSchoolException
     *
     */
    private static void monitorPaperCut(
            final Map<String, SmartSchoolConnection> connectionMap,
            final boolean simulationMode,
            final PaperCutServerProxy papercutServerProxy,
            final PaperCutDbProxy papercutDbProxy,
            final DocLogDao.ListFilter listFilterPendingExt)
            throws PaperCutException, SmartSchoolException, SOAPException {

        final DocLogDao doclogDao =
                ServiceContext.getDaoContext().getDocLogDao();

        final Map<String, DocLog> uniqueDocNames = new HashMap<>();

        for (final DocLog docLog : doclogDao.getListChunk(listFilterPendingExt)) {
            uniqueDocNames.put(docLog.getTitle(), docLog);
        }

        if (uniqueDocNames.isEmpty()) {
            return;
        }

        for (final PaperCutPrinterUsageLog papercutLog : PAPERCUT_SERVICE
                .getPrinterUsageLog(papercutDbProxy, uniqueDocNames.keySet())) {

            debugLog(papercutLog);

            final DocLog docLog =
                    uniqueDocNames.get(papercutLog.getDocumentName());

            final SmartSchoolPrintStatusEnum printStatus;
            final String comment;

            /*
             * Database transaction.
             */
            ReadWriteLockEnum.DATABASE_READONLY.setReadLock(true);

            final DaoContext daoContext = ServiceContext.getDaoContext();

            try {

                if (!daoContext.isTransactionActive()) {
                    daoContext.beginTransaction();
                }

                if (papercutLog.isPrinted()) {

                    printStatus = SmartSchoolPrintStatusEnum.COMPLETED;
                    comment = MSG_COMMENT_PRINT_COMPLETED;

                    processPaperCutCompleted(papercutServerProxy, docLog,
                            papercutLog, printStatus);

                } else {

                    if (papercutLog.getDeniedReason().contains("TIMEOUT")) {

                        printStatus = SmartSchoolPrintStatusEnum.EXPIRED;
                        comment = MSG_COMMENT_PRINT_EXPIRED;

                    } else {

                        printStatus = SmartSchoolPrintStatusEnum.CANCELLED;
                        comment = MSG_COMMENT_PRINT_CANCELLED;
                    }

                    processPaperCutCancelled(docLog, papercutLog, printStatus);
                }

                daoContext.commit();

            } finally {

                daoContext.rollback();

                ReadWriteLockEnum.DATABASE_READONLY.setReadLock(false);

            }

            /*
             * Get SmartSchool account name from the document name.
             */
            final StringBuilder msg = new StringBuilder();

            final String account =
                    getAccountFromProxyPrintJobName(papercutLog
                            .getDocumentName());

            if (account == null) {

                msg.append("No account found in DocLog supplier data for [")
                        .append(papercutLog.getDocumentName()).append("].");

                LOGGER.warn(msg.toString());

                if (SmartSchoolLogger.getLogger().isDebugEnabled()) {
                    SmartSchoolLogger.logDebug(msg.toString());
                }
                return;
            }

            final SmartSchoolConnection connection = connectionMap.get(account);

            if (connection == null) {

                msg.append("No connection found for account [").append(account)
                        .append("] of [").append(papercutLog.getDocumentName())
                        .append("].");

                LOGGER.warn(msg.toString());

                if (SmartSchoolLogger.getLogger().isDebugEnabled()) {
                    SmartSchoolLogger.logDebug(msg.toString());
                }
                return;
            }

            /*
             * NOTE: reporting the status to SmartSchool MUST be the LAST
             * action.
             */
            reportDocumentStatus(connection, docLog.getExternalId(),
                    printStatus, comment, simulationMode);
        }
    }

    /**
     *
     * Processes a cancelled PaperCut SmartSchool print job.
     * <ul>
     * <li></li>
     * </ul>
     *
     * @param docLog
     *            The SavaPage log.
     * @param papercutLog
     *            The PaperCut log.
     * @param printStatus
     *            The {@link SmartSchoolPrintStatusEnum}.
     */
    private static void processPaperCutCancelled(final DocLog docLog,
            final PaperCutPrinterUsageLog papercutLog,
            final SmartSchoolPrintStatusEnum printStatus) {

        final DocLogDao doclogDao =
                ServiceContext.getDaoContext().getDocLogDao();

        publishAdminMsg(
                PubLevelEnum.WARN,
                "PaperCut print of SmartSchool document ["
                        + papercutLog.getDocumentName() + "] " + printStatus);

        docLog.setExternalStatus(printStatus.toString());
        doclogDao.update(docLog);
    }

    /**
     * Processes a completed PaperCut SmartSchool print job.
     * <ul>
     * <li>The Personal and Shared SmartSchool\Klas accounts are lazy adjusted
     * in PaperCut and SavaPage.</li>
     * <li>The {@link AccountTrx} objects are moved from the {@link DocLog}
     * source to the {@link DocLog} target.</li>
     * <li>The {@link DocLog} target is updated with the
     * {@link SmartSchoolPrintStatusEnum}.</li>
     * </ul>
     *
     * @param papercutServerProxy
     *            The {@link PaperCutServerProxy}.
     * @param docLogOut
     *            The SavaPage {@link DocLog} target holding the {@link DocOut}
     *            with the {@link PrintOut}.
     * @param papercutLog
     *            The {@link PaperCutPrinterUsageLog}.
     * @param printStatus
     *            The {@link SmartSchoolPrintStatusEnum}.
     * @throws PaperCutException
     *             When a PaperCut error occurs.
     */
    private static void processPaperCutCompleted(
            final PaperCutServerProxy papercutServerProxy,
            final DocLog docLogOut, final PaperCutPrinterUsageLog papercutLog,
            final SmartSchoolPrintStatusEnum printStatus)
            throws PaperCutException {

        //
        final AccountDao accountDao =
                ServiceContext.getDaoContext().getAccountDao();

        final AccountTrxDao accountTrxDao =
                ServiceContext.getDaoContext().getAccountTrxDao();

        final DocInOutDao docInOutDao =
                ServiceContext.getDaoContext().getDocInOutDao();

        final DocLogDao doclogDao =
                ServiceContext.getDaoContext().getDocLogDao();

        /*
         * Get the DocLog source.
         */
        final DocLog docLogIn =
                docInOutDao.findDocOutSource(docLogOut.getDocOut().getId());

        /*
         * Any transactions?
         */
        final List<AccountTrx> trxList = docLogIn.getTransactions();

        if (trxList == null || trxList.isEmpty()) {

            if (SmartSchoolLogger.getLogger().isDebugEnabled()) {
                SmartSchoolLogger.logDebug("No DocLog transactions found for ["
                        + docLogOut.getTitle() + "] [" + docLogOut.getId()
                        + "]");
            }
            return;
        }

        /*
         * Accumulate the total AccountTrx weight.
         */
        int weightTotal = 0;

        for (final AccountTrx trx : trxList) {

            final org.savapage.core.jpa.Account account = trx.getAccount();

            final AccountTypeEnum accountType =
                    AccountTypeEnum.valueOf(account.getAccountType());

            /*
             * Weights of personal and shared accounts are NOT mutually
             * exclusive: add user account totals only.
             */
            if (accountType != AccountTypeEnum.SHARED) {
                weightTotal += trx.getTransactionWeight().intValue();
            }
        }

        /*
         * Total printing cost reported by PaperCut.
         */
        final BigDecimal papercutUsageCost =
                BigDecimal.valueOf(papercutLog.getUsageCost());

        /*
         * Number of decimals for decimal scaling.
         */
        final int scale = ConfigManager.getFinancialDecimalsInDatabase();

        /*
         * Comment with job data
         */
        final String requestingUserId = docLogIn.getUser().getUserId();

        final StringBuilder classCopiesComment = new StringBuilder();

        classCopiesComment.append(JOBS_COMMENT_FIELD_SEPARATOR_FIRST)
                .append(requestingUserId).append(JOBS_COMMENT_FIELD_SEPARATOR)
                .append(weightTotal);

        /*
         * Adjust the Personal and Shared Accounts in PaperCut and update the
         * SavaPage AccountTrx's.
         */
        for (final AccountTrx trx : trxList) {

            final int weight = trx.getTransactionWeight().intValue();

            final BigDecimal weightedCost =
                    ACCOUNTING_SERVICE.calcWeightedAmount(papercutUsageCost,
                            weightTotal, weight, scale);

            final org.savapage.core.jpa.Account account = trx.getAccount();

            /*
             * PaperCut account adjustment.
             */
            final BigDecimal papercutAdjustment = weightedCost.negate();

            final AccountTypeEnum accountType =
                    AccountTypeEnum.valueOf(account.getAccountType());

            final String msgKeyCopies;

            if (weight == 1) {
                msgKeyCopies = "copies-single";
            } else {
                msgKeyCopies = "copies-multiple";
            }

            final String comment =
                    localizedMsg("account-trx-comment",
                            docLogOut.getLogComment(), docLogOut.getUser()
                                    .getUserId(),
                            localizedMsg(msgKeyCopies, String.valueOf(weight)));

            if (accountType == AccountTypeEnum.SHARED) {

                /*
                 * Adjust Shared SmartSchool/klas Account.
                 */
                final String topAccountName = account.getParent().getName();

                final String subAccountName = account.getName();

                if (SmartSchoolLogger.getLogger().isDebugEnabled()) {

                    SmartSchoolLogger.logDebug("PaperCut shared account ["
                            + papercutServerProxy.composeSharedAccountName(
                                    topAccountName, subAccountName)
                            + "] adjustment ["
                            + papercutAdjustment.toPlainString()
                            + "] comment: " + comment);
                }

                PAPERCUT_SERVICE.lazyAdjustSharedAccount(papercutServerProxy,
                        topAccountName, subAccountName, papercutAdjustment,
                        comment);

                classCopiesComment
                        .append(JOBS_COMMENT_FIELD_SEPARATOR)
                        .append(requestingUserId)
                        .append(JOBS_COMMENT_USER_CLASS_SEPARATOR)
                        .append(SMARTSCHOOL_SERVICE
                                .getKlasFromComposedAccountName(subAccountName))
                        .append(JOBS_COMMENT_FIELD_SEPARATOR).append(weight);

            } else {

                final UserAccountDao userAccountDao =
                        ServiceContext.getDaoContext().getUserAccountDao();

                /*
                 * Get the user of the transaction.
                 */
                final User user =
                        userAccountDao
                                .findByAccountId(trx.getAccount().getId())
                                .getUser();
                /*
                 * Adjust Personal Account.
                 */
                if (SmartSchoolLogger.getLogger().isDebugEnabled()) {

                    SmartSchoolLogger.logDebug("PaperCut personal account ["
                            + user.getUserId() + "] adjustment ["
                            + papercutAdjustment.toPlainString()
                            + "] comment [" + comment + "]");
                }

                PAPERCUT_SERVICE.adjustUserAccountBalance(papercutServerProxy,
                        user.getUserId(), papercutAdjustment, comment);
            }

            /*
             * Update Account.
             */
            account.setBalance(account.getBalance().subtract(weightedCost));
            accountDao.update(account);

            /*
             * Update AccountTrx.
             */
            trx.setAmount(papercutAdjustment);
            trx.setBalance(account.getBalance());

            trx.setTransactedBy(ServiceContext.getActor());
            trx.setTransactionDate(ServiceContext.getTransactionDate());

            // Move from DocLog source to target.
            trx.setDocLog(docLogOut);

            accountTrxDao.update(trx);
        }

        /*
         * Create a transaction in the shared Jobs account with a comment of
         * formatted job data.
         */
        classCopiesComment.append(JOBS_COMMENT_FIELD_SEPARATOR)
                .append(docLogIn.getTitle())
                .append(JOBS_COMMENT_FIELD_SEPARATOR)
                .append(docLogIn.getLogComment())
                .append(JOBS_COMMENT_FIELD_SEPARATOR_LAST);

        PAPERCUT_SERVICE.lazyAdjustSharedAccount(papercutServerProxy,
                SMARTSCHOOL_SERVICE.getSharedParentAccountName(),
                SMARTSCHOOL_SERVICE.getSharedJobsAccountName(),
                papercutUsageCost.negate(), StringUtils.abbreviate(
                        classCopiesComment.toString(),
                        PaperCutDbProxy.COL_LEN_TXN_COMMENT));

        /*
         * Move the AccountTrx list from DocLog source to target and Update
         * source and target.
         */
        docLogOut.setExternalStatus(printStatus.toString());
        docLogOut.setTransactions(trxList);
        doclogDao.update(docLogOut);

        docLogIn.setTransactions(null);
        doclogDao.update(docLogIn);

        /*
         * Publish admin message.
         */
        publishAdminMsg(
                PubLevelEnum.INFO,
                "PaperCut print of SmartSchool document ["
                        + papercutLog.getDocumentName() + "] " + printStatus);

    }

    /**
     * Logs content of a {@link PaperCutPrinterUsageLog} object.
     *
     * @param usageLog
     *            The object to log.
     */
    private static void debugLog(final PaperCutPrinterUsageLog usageLog) {

        if (LOGGER.isDebugEnabled()) {

            LOGGER.debug(usageLog.getDocumentName() + " | printed ["
                    + usageLog.isPrinted() + "] cancelled ["
                    + usageLog.isCancelled() + "] deniedReason ["
                    + usageLog.getDeniedReason() + "] usageCost["
                    + usageLog.getUsageCost() + "]");
        }
    }

    /**
     * Creates {@link AccountTrxInfoSet} for a SmartSchool klas and personal
     * copies.
     *
     * <p>
     * NOTE: This method has its own database transaction scope, because shared
     * user accounts might get lazy created.
     * </p>
     *
     * @param connection
     *            The {@link SmartSchoolConnection}.
     * @param klasCopies
     *            The collected "klas" copies.
     * @param userCopies
     *            The collected user copies.
     * @return The {@link AccountTrxInfoSet}.
     */
    private static AccountTrxInfoSet createAccountTrxInfoSet(
            final SmartSchoolConnection connection,
            final Map<String, Integer> klasCopies,
            final Map<String, Integer> userCopies) {

        final AccountTrxInfoSet infoSet;

        ReadWriteLockEnum.DATABASE_READONLY.setReadLock(true);

        final DaoContext daoContext = ServiceContext.getDaoContext();

        try {

            daoContext.beginTransaction();

            /**
             * The parent {@link Account} for all lazy created shared klas
             * accounts.
             */
            final org.savapage.core.jpa.Account sharedParentAccount =
                    SMARTSCHOOL_SERVICE.getSharedParentAccount();

            infoSet =
                    SMARTSCHOOL_SERVICE.createPrintInAccountTrxInfoSet(
                            connection, sharedParentAccount, klasCopies,
                            userCopies);

            daoContext.commit();

        } finally {

            daoContext.rollback();

            ReadWriteLockEnum.DATABASE_READONLY.setReadLock(false);

        }

        return infoSet;
    }

    /**
     * Creates {@link ExternalSupplierInfo}.
     *
     * @param document
     *            The {@link Document}.
     * @param nTotCopies
     *            The number of copies to be proxy printed.
     * @return The {@link ExternalSupplierInfo}.
     */
    private static ExternalSupplierInfo createExternalSupplierInfo(
            final SmartSchoolConnection connection, final Document document,
            final int nTotCopies) {

        final ExternalSupplierInfo supplierInfo = new ExternalSupplierInfo();

        supplierInfo.setSupplier(ExternalSupplierEnum.SMARTSCHOOL);

        final SmartSchoolPrintInData supplierData =
                new SmartSchoolPrintInData();

        supplierData.setAccount(connection.getAccountName());

        supplierData.setCopies(nTotCopies);

        // Translate SmartSchool processInfo to SavaPage speak.
        final Processinfo processInfo = document.getProcessinfo();

        // media
        if (processInfo.getPapersize().equalsIgnoreCase("a3")) {
            supplierData.setMediaSize(IppMediaSizeEnum.ISO_A3);
        } else {
            supplierData.setMediaSize(IppMediaSizeEnum.ISO_A4);
        }

        // duplex
        supplierData.setDuplex(asBoolean(processInfo.getDuplex()));

        // grayscale
        supplierData.setColor(!processInfo.getRendermode().equalsIgnoreCase(
                "grayscale"));

        // staple (TODO)

        // punch (TODO)

        // frontcover (TODO)

        // backcover (TODO)

        // confidential (TODO)

        // Do NOT the status.

        //
        supplierInfo.setId(document.getId());

        supplierInfo.setData(supplierData);

        return supplierInfo;
    }

    /**
     * Creates {@link DocContentPrintInInfo} for a downloaded SmartSchool
     * document.
     *
     * @param connection
     *            The {@link SmartSchoolConnection}.
     * @param document
     *            The SmartSchool {@link Document} to be printed.
     * @param downloadedFile
     *            The downloaded {@link File}.
     * @param nTotCopies
     *            The total number of copies to be proxy printed.
     * @param uuid
     *            The SavaPage assigned {@link UUID} for the downloaded
     *            document.
     * @return The {@link DocContentPrintInInfo}.
     * @throws DocContentPrintException
     *             When the PDF file has security restrictions.
     */
    private static DocContentPrintInInfo createPrintInInfo(
            final SmartSchoolConnection connection, final Document document,
            final File downloadedFile, final int nTotCopies, final UUID uuid)
            throws DocContentPrintException {

        /*
         * Get the PDF properties to check security issues.
         */
        final SpPdfPageProps pdfProps;

        try {
            pdfProps = SpPdfPageProps.create(downloadedFile.getCanonicalPath());
        } catch (PdfSecurityException e) {
            throw new DocContentPrintException(e.getMessage(), e);
        } catch (IOException e) {
            throw new SpException(e.getMessage());
        }

        final DocContentPrintInInfo printInInfo = new DocContentPrintInInfo();

        printInInfo.setDrmRestricted(false);
        printInInfo.setJobBytes(downloadedFile.length());
        printInInfo.setJobName(document.getName());
        printInInfo.setLogComment(document.getComment());
        printInInfo.setMimetype(DocContent.MIMETYPE_PDF);
        printInInfo.setOriginatorEmail(null);
        printInInfo.setOriginatorIp(connection.getEndpointIpAddress());
        printInInfo.setPageProps(pdfProps);
        printInInfo.setUuidJob(uuid);

        return printInInfo;
    }

    /**
     * Finds the active (non-deleted) SavaPage user (optionally lazy insert).
     *
     * @param userId
     *            The unique user id.
     * @param lazyInsertUser
     *            {@code true} if user may be lazy inserted.
     * @param userSource
     *            The {@link IUserSource}.
     * @param userSourceGroup
     *            The user group of the The {@link IUserSource}.
     * @return {@code null} when not found (or lazy inserted).
     */
    private static User findActiveUser(final String userId,
            final boolean lazyInsertUser, final IUserSource userSource,
            final String userSourceGroup) {

        final UserDao userDao = ServiceContext.getDaoContext().getUserDao();

        User user = userDao.findActiveUserByUserId(userId);

        if (user == null && lazyInsertUser) {

            ServiceContext.getDaoContext().beginTransaction();

            user =
                    USER_SERVICE.lazyInsertExternalUser(userSource, userId,
                            userSourceGroup);

            ServiceContext.getDaoContext().commit();
        }

        return user;
    }

    /**
     * Processes a SmartSchool print job.
     * <p>
     * The job is canceled when the requesting user does NOT exist in SavaPage
     * (and PaperCut, when integration is enabled).
     * </p>
     *
     * @param monitor
     *            The {@link SmartSchoolPrintMonitor} instance.
     * @param document
     *            The SmartSchool document.
     * @throws SOAPException
     *             When connectivity problems.
     * @throws SmartSchoolException
     *             When SmartSchool reported an error.
     * @throws ShutdownException
     *             When interrupted by a shutdown request.
     */
    private static void processJob(SmartSchoolPrintMonitor monitor,
            final Document document) throws SmartSchoolException,
            SOAPException, ShutdownException {

        ServiceContext.resetTransactionDate();

        final ConfigManager cm = ConfigManager.instance();

        final boolean lazyInsertUser =
                cm.isConfigValue(Key.SMARTSCHOOL_USER_INSERT_LAZY_PRINT)
                        && cm.isUserInsertLazyPrint();

        final IUserSource userSource = cm.getUserSource();

        final String userSourceGroup =
                cm.getConfigValue(Key.USER_SOURCE_GROUP).trim();

        final String userId = document.getRequester().getUsername();

        final User requestingUser =
                findActiveUser(userId, lazyInsertUser, userSource,
                        userSourceGroup);

        final boolean userExistSavaPage = requestingUser != null;

        if (userExistSavaPage) {
            try {
                USER_SERVICE.lazyUserHomeDir(requestingUser);
            } catch (IOException e) {
                throw new SpException(e.getMessage());
            }
        }

        /*
         * Find the PaperCut requesting user.
         */
        final PaperCutUser papercutUser;
        final Boolean userExistPaperCut;

        if (monitor.isIntegratedWithPaperCut()) {
            papercutUser =
                    PAPERCUT_SERVICE.findUser(monitor.papercutServerProxy,
                            userId);
            userExistPaperCut = papercutUser != null;
        } else {
            papercutUser = null;
            userExistPaperCut = null;
        }

        File downloadedFile = null;

        try {

            monitor.isProcessing = true;

            /*
             * INVARIANT: A User matching the requester MUST exist in SavaPage
             * and (optionally) in PaperCut.
             */
            if (!userExistSavaPage
                    || (userExistPaperCut != null && !userExistPaperCut)) {

                processJobRequesterNotFound(monitor.processingConnection,
                        monitor.simulationMode, document, userId,
                        userExistSavaPage, userExistPaperCut);

            } else {

                final Map<String, Integer> klasCopies = new HashMap<>();
                final Map<String, Integer> userCopies = new HashMap<>();

                final int nTotCopies =
                        collectCopyInfo(monitor, document, klasCopies,
                                userCopies, lazyInsertUser, userSource,
                                userSourceGroup);

                if (nTotCopies == 0) {

                    processJobNoCopies(monitor.processingConnection,
                            monitor.simulationMode, document);

                } else {
                    /*
                     * Download document.
                     */
                    final UUID uuid = UUID.randomUUID();

                    downloadedFile =
                            downloadDocument(monitor.processingConnection,
                                    requestingUser, document, uuid,
                                    monitor.simulationMode);

                    /*
                     * Create ExternalSupplierInfo info.
                     */
                    final ExternalSupplierInfo externalSupplierInfo =
                            createExternalSupplierInfo(
                                    monitor.processingConnection, document,
                                    nTotCopies);

                    /*
                     * Create Account info.
                     */
                    final AccountTrxInfoSet accountTrxInfoSet =
                            createAccountTrxInfoSet(
                                    monitor.processingConnection, klasCopies,
                                    userCopies);
                    /*
                     * Create PrintIn info.
                     */
                    final DocContentPrintInInfo printInInfo =
                            createPrintInInfo(monitor.processingConnection,
                                    document, downloadedFile, nTotCopies, uuid);

                    /*
                     * Process the downloaded file.
                     */
                    processJobFile(monitor, document, requestingUser,
                            downloadedFile, printInInfo, accountTrxInfoSet,
                            externalSupplierInfo);
                }
            }

        } catch (IOException e) {

            throw new SpException(e.getMessage());

        } catch (DocContentPrintException e) {

            publishAdminMsg(
                    PubLevelEnum.WARN,
                    localizedMsg("print-cancelled", document.getName(),
                            e.getMessage()));

            reportDocumentStatus(monitor.processingConnection,
                    document.getId(), SmartSchoolPrintStatusEnum.CANCELLED,
                    MSG_COMMENT_PRINT_CANCELLED_DOC_TYPE,
                    monitor.simulationMode);

        } catch (ProxyPrintException e) {

            publishAdminMsg(
                    PubLevelEnum.WARN,
                    localizedMsg("print-error", document.getName(),
                            e.getMessage()));

            reportDocumentStatus(monitor.processingConnection,
                    document.getId(), SmartSchoolPrintStatusEnum.ERROR,
                    e.getMessage(), monitor.simulationMode);

        } catch (IppConnectException e) {

            // TODO
            LOGGER.error(e.getMessage());

        } finally {

            if (downloadedFile != null && downloadedFile.exists()) {
                downloadedFile.delete();
            }

            monitor.isProcessing = false;
        }
    }

    /**
     * Localizes and formats a message string with placeholder arguments.
     *
     * @param msgKey
     *            The message key in the XML resource file.
     * @param strings
     *            The values to fill the placeholders.
     * @return The localized string.
     */
    private static String localizedMsg(final String msgKey,
            final String... strings) {
        return Messages.getMessage(SmartSchoolPrintMonitor.class,
                ServiceContext.getLocale(), msgKey, strings);
    }

    /**
     * Processes a job request without any copies.
     *
     * @param connection
     *            The {@link SmartSchoolConnection}.
     * @param document
     *            The document.
     * @throws SOAPException
     *             When connectivity problems.
     * @throws SmartSchoolException
     *             When SmartSchool reported an error.
     */
    private static void processJobNoCopies(
            final SmartSchoolConnection connection,
            final boolean simulationMode, final Document document)
            throws SmartSchoolException, SOAPException {

        LOGGER.warn("Document [" + document.getId() + "] ["
                + document.getName() + "] skipped: no copies identified.");

        publishAdminMsg(PubLevelEnum.WARN,
                "SmartSchool document [" + document.getName() + "]: "
                        + MSG_COMMENT_PRINT_ERROR_NO_COPIES);

        reportDocumentStatus(connection, document.getId(),
                SmartSchoolPrintStatusEnum.ERROR,
                MSG_COMMENT_PRINT_ERROR_NO_COPIES, simulationMode);
    }

    /**
     * Processes a job request with an unknown requester.
     *
     * @param connection
     *            The {@link SmartSchoolConnection}.
     * @param document
     *            The document.
     * @param userId
     *            The unique name of the requesting user.
     * @param userExistSavaPage
     *            {@code true} when user exists in SavaPage.
     * @param userExistPaperCut
     *            {@code true} when user exists in PaperCut.
     * @throws SOAPException
     *             When connectivity problems.
     * @throws SmartSchoolException
     *             When SmartSchool reported an error.
     */
    private static void processJobRequesterNotFound(
            final SmartSchoolConnection connection,
            final boolean simulationMode, final Document document,
            final String userId, final boolean userExistSavaPage,
            final Boolean userExistPaperCut) throws SmartSchoolException,
            SOAPException {

        final String msg;

        if (!userExistSavaPage && userExistPaperCut != null
                && !userExistPaperCut) {

            msg =
                    String.format(MSG_COMMENT_PRINT_ERROR_UNKNOWN_USER_BOTH,
                            userId);

        } else if (!userExistSavaPage) {
            msg =
                    String.format(
                            MSG_COMMENT_PRINT_ERROR_UNKNOWN_USER_SAVAPAGE,
                            userId);

        } else if (userExistPaperCut != null && !userExistPaperCut) {
            msg =
                    String.format(
                            MSG_COMMENT_PRINT_ERROR_UNKNOWN_USER_PAPERCUT,
                            userId);
        } else {
            throw new SpException(
                    "no msg needed (SavaPage and PaperCut users found");
        }

        publishAdminMsg(PubLevelEnum.WARN,
                "SmartSchool document [" + document.getName() + "]: " + msg);

        reportDocumentStatus(connection, document.getId(),
                SmartSchoolPrintStatusEnum.ERROR, msg, simulationMode);
    }

    /**
     * Collects info about the number of copies to be printed.
     * <p>
     * Copies are counted for accounts that exist in SavaPage and (optionally)
     * PaperCut.
     * </p>
     *
     * @param monitor
     *            The {@link SmartSchoolPrintMonitor} instance.
     * @param document
     *            The SmartSchool {@link Document} to print.
     * @param klasCopies
     *            The map to collect the number of copies to per Klas (students
     *            only).
     * @param userCopies
     *            The map to collect the number of individual copies on (both
     *            students and non-students).
     * @param lazyInsertUser
     *            {@code true} if user may be lazy inserted.
     * @param userSource
     *            The {@link IUserSource}.
     * @param userSourceGroup
     *            The user group of the The {@link IUserSource}.
     * @return The total number of copies of the document to be printed.
     */
    private static int collectCopyInfo(final SmartSchoolPrintMonitor monitor,
            final Document document, final Map<String, Integer> klasCopies,
            final Map<String, Integer> userCopies,
            final boolean lazyInsertUser, final IUserSource userSource,
            final String userSourceGroup) {

        int nTotCopies = 0;

        klasCopies.clear();
        userCopies.clear();

        final Billinginfo billing = document.getBillinginfo();

        List<Account> accountList = null;

        if (billing != null) {

            final Accounts accounts = billing.getAccounts();

            if (accounts != null) {
                accountList = accounts.getAccount();
            }
        }

        if (accountList == null) {
            return 0;
        }

        /*
         * Traverse accounts.
         */
        for (final Account account : accountList) {

            // Name
            final String userName = account.getUsername();

            /*
             * INVARIANT: User name MUST be specified.
             */
            if (StringUtils.isBlank(userName)) {
                LOGGER.warn("Account skipped: no username specified.");
                continue;
            }

            // Role
            final String role = account.getRole();

            /*
             * INVARIANT: Role MUST be specified.
             */
            if (StringUtils.isBlank(role)) {
                LOGGER.warn("Account [" + userName
                        + "] skipped: no role specified.");
                continue;
            }

            final SmartSchoolRoleEnum roleEnum =
                    SmartSchoolRoleEnum.fromXmlValue(role);

            /*
             * INVARIANT: Role MUST be known.
             */
            if (roleEnum == null) {
                LOGGER.warn("Account [" + userName
                        + "] skipped: unknown role [" + role + "].");
                continue;
            }

            // Clazz
            final String clazz = account.getClazz();

            final boolean personalAccount = StringUtils.isBlank(clazz);

            /*
             * INVARIANT: Student MUST have a class.
             */
            if (personalAccount && roleEnum == SmartSchoolRoleEnum.STUDENT) {
                LOGGER.warn("Account [" + userName
                        + "] skipped: no class specified.");
                continue;
            }

            /*
             * INVARIANT: Account MUST exist in SavaPage and (optionally)
             * PaperCut.
             */
            final PaperCutUser papercutUser;
            final Boolean userExistPaperCut;

            if (monitor.isIntegratedWithPaperCut()) {
                papercutUser = monitor.papercutServerProxy.getUser(userName);
                userExistPaperCut = papercutUser != null;
            } else {
                userExistPaperCut = null;
            }

            final User user =
                    findActiveUser(userName, lazyInsertUser, userSource,
                            userSourceGroup);

            final boolean userExistSavaPage = user != null;

            final String msg;

            if (!userExistSavaPage && userExistPaperCut != null
                    && !userExistPaperCut) {
                msg =
                        String.format("Account [%s] skipped: onbekend "
                                + "in %s en PaperCut.", userName,
                                CommunityDictEnum.SAVAPAGE.getWord());
            } else if (!userExistSavaPage) {
                msg =
                        String.format("Account [%s] skipped: onbekend in %s.",
                                userName, CommunityDictEnum.SAVAPAGE.getWord());
            } else if (userExistPaperCut != null && !userExistPaperCut) {
                msg =
                        String.format("Account [%s] skipped: "
                                + "onbekend in PaperCut.", userName);
            } else {
                msg = null;
            }

            if (msg != null) {
                LOGGER.warn(msg);
                continue;
            }

            // Copies + extra
            final Integer copies = account.getCopies();
            final Integer extra = account.getExtra();

            int nCopies = 0;

            if (copies != null) {
                nCopies += copies.intValue();
            }
            if (extra != null) {
                nCopies += extra.intValue();
            }

            /*
             * INVARIANT: Copies MUST be more zero.
             */
            if (nCopies == 0) {
                LOGGER.warn("Account [" + userName
                        + "] skipped: no (extra) copies.");
                continue;
            }

            nTotCopies += nCopies;

            /*
             * Collect number of copies.
             */
            Integer collectedSum;
            Integer collectedSumNew;

            // User copies
            collectedSum = userCopies.get(userName);

            if (collectedSum == null) {
                collectedSumNew = Integer.valueOf(nCopies);
            } else {
                collectedSumNew =
                        Integer.valueOf(collectedSum.intValue() + nCopies);
            }
            userCopies.put(userName, collectedSumNew);

            // Class copies.
            if (!personalAccount) {

                collectedSum = klasCopies.get(clazz);

                if (collectedSum == null) {
                    collectedSumNew = Integer.valueOf(nCopies);
                } else {
                    collectedSumNew =
                            Integer.valueOf(collectedSum.intValue() + nCopies);
                }

                klasCopies.put(clazz, collectedSumNew);
            }

        }
        return nTotCopies;
    }

    /**
     * Processes the downloaded SmartSchool print Job file.
     *
     * @param monitor
     *            The {@link SmartSchoolPrintMonitor} instance.
     * @param document
     *            The {@link Document}.
     * @param user
     *            The requesting user.
     * @param downloadedFile
     *            The downloaded {@link File}.
     * @param printInInfo
     *            The {@link DocContentPrintInInfo}.
     * @param accountTrxInfoSet
     *            The {@link AccountTrxInfoSet}.
     * @param externalSupplierInfo
     *            The {@link ExternalSupplierInfo}.
     *
     * @throws IOException
     * @throws DocContentPrintException
     * @throws ShutdownException
     * @throws ProxyPrintException
     * @throws IppConnectException
     * @throws SmartSchoolException
     * @throws SOAPException
     */
    private static void processJobFile(final SmartSchoolPrintMonitor monitor,
            final Document document, final User user,
            final File downloadedFile, final DocContentPrintInInfo printInInfo,
            final AccountTrxInfoSet accountTrxInfoSet,
            final ExternalSupplierInfo externalSupplierInfo)
            throws IOException, DocContentPrintException, ShutdownException,
            ProxyPrintException, IppConnectException, SmartSchoolException,
            SOAPException {

        final boolean isIntegratedWithPaperCut =
                monitor.isIntegratedWithPaperCut();

        /*
         * Find the configured printer name for Direct Printing.
         */
        final String proxyPrinterName =
                monitor.processingConnection.getProxyPrinterName();

        final boolean isDirectProxyPrint =
                StringUtils.isNotBlank(proxyPrinterName);

        /*
         * Determine (initial) print status.
         */
        final SmartSchoolPrintStatusEnum smartschoolPrintStatus;

        if (isDirectProxyPrint) {

            if (isIntegratedWithPaperCut) {
                smartschoolPrintStatus = SmartSchoolPrintStatusEnum.PENDING_EXT;
            } else {
                smartschoolPrintStatus = SmartSchoolPrintStatusEnum.COMPLETED;
            }

        } else {
            // TODO
            smartschoolPrintStatus = SmartSchoolPrintStatusEnum.COMPLETED;
            // smartschoolPrintStatus = SmartSchoolPrintStatusEnum.PENDING;
        }

        /*
         * Set the status FIRST.
         */
        externalSupplierInfo.setStatus(smartschoolPrintStatus.toString());

        /*
         * The comment reported back to SmartSchool.
         */
        final String feedbackComment;

        /*
         * IMPORTANT: Logging the PrintIn event MUST be the first action. When
         * printing to the inbox this is crucial, since it is the document MUST
         * be present in the database BEFORE moving the PDF file.
         */
        printInInfo.setSupplierInfo(externalSupplierInfo);

        if (isDirectProxyPrint) {

            if (isIntegratedWithPaperCut) {

                feedbackComment = MSG_COMMENT_PRINT_PENDING_PAPERCUT;

                /*
                 * Set the AccountTrx's in the DocLog source (DocIn/PrintIn).
                 *
                 * Later on, when we monitor PaperCut and find out that the
                 * print succeeded, we will move/update the AccountTrx's to the
                 * DocLog (DocOut/PrintOut) target.
                 */
                printInInfo.setAccountTrxInfoSet(accountTrxInfoSet);

            } else {

                feedbackComment = MSG_COMMENT_PRINT_QUEUED;

                /*
                 * Do NOT set the AccountTrx's here.
                 */
                printInInfo.setAccountTrxInfoSet(null);
            }

            /*
             * Logging as first action.
             */
            DOCLOG_SERVICE.logPrintIn(user, monitor.smartSchoolQueue,
                    DocLogProtocolEnum.SMARTSCHOOL, printInInfo);

            /*
             * Direct Proxy Print.
             */

            if (isIntegratedWithPaperCut) {
                /*
                 * Use an empty transaction set, so NO (user or shared) account
                 * transactions are created.
                 */
                printInInfo.setAccountTrxInfoSet(new AccountTrxInfoSet());
            } else {
                /*
                 * Set the AccountTrx's in the DocLog target (DocOut/PrintOut).
                 */
                printInInfo.setAccountTrxInfoSet(accountTrxInfoSet);
            }

            processJobProxyPrint(monitor, user, proxyPrinterName, document,
                    downloadedFile, printInInfo, externalSupplierInfo);

        } else {

            feedbackComment = MSG_COMMENT_PRINT_PENDING_SAVAPAGE;

            /*
             * Set the AccountTrx's in the DocLog source (DocIn/PrintIn).
             *
             * TODO
             *
             * Later on, when we .... (hold?/fast?) print, we will move/update
             * the AccountTrx's to the DocLog (DocOut/PrintOut) target.
             */
            printInInfo.setAccountTrxInfoSet(accountTrxInfoSet);

            /*
             * Logging as first action.
             */
            DOCLOG_SERVICE.logPrintIn(user, monitor.smartSchoolQueue,
                    DocLogProtocolEnum.SMARTSCHOOL, printInInfo);

            /*
             * Move PDF document to the SavaPage inbox.
             */
            final String homeDir =
                    USER_SERVICE.lazyUserHomeDir(user).getCanonicalPath();

            final Path source =
                    FileSystems.getDefault().getPath(
                            downloadedFile.getCanonicalPath());

            final Path target =
                    FileSystems.getDefault().getPath(
                            homeDir,
                            printInInfo.getUuidJob().toString() + "."
                                    + DocContent.FILENAME_EXT_PDF);

            OutputProducer.doAtomicFileMove(source, target);
        }

        /*
         * Report status and comment back to SmartSchool.
         */
        reportDocumentStatus(monitor.processingConnection, document.getId(),
                smartschoolPrintStatus, feedbackComment, monitor.simulationMode);
    }

    /**
     * Translates a The SmartSchool boolean value to a boolean type.
     *
     * @param value
     *            The SmartSchool boolean value.
     * @return {@code true} when value represents {@link Boolean#TRUE}.
     */
    private static boolean asBoolean(final String value) {
        return value.equalsIgnoreCase("on");
    }

    /**
     * Adds a "pro-forma" {@link ProxyPrintJobChunk} object to the
     * {@link ProxyPrintDocReq}.
     *
     * @param printReq
     *            The {@link ProxyPrintDocReq}.
     * @param ippMediaSize
     *            The media size.
     * @throws ProxyPrintException
     *             When printer has no media-source for media size.
     */
    private static void addProxyPrintJobChunk(final ProxyPrintDocReq printReq,
            final IppMediaSizeEnum ippMediaSize) throws ProxyPrintException {

        final PrinterDao printerDao =
                ServiceContext.getDaoContext().getPrinterDao();

        final String printerName = printReq.getPrinterName();

        final Printer printer = printerDao.findByName(printerName);

        final PrinterAttrLookup printerAttrLookup =
                new PrinterAttrLookup(printer);

        /*
         * INVARIANT: If printer has media sources defined, a media-source MUST
         * be available that matches the media size of the document.
         */
        final IppMediaSourceCostDto assignedMediaSource =
                printerAttrLookup.findAnyMediaSourceForMedia(ippMediaSize);

        if (assignedMediaSource == null) {
            throw new ProxyPrintException(localizedMsg(
                    "printer-media-not-foud", printerName,
                    ippMediaSize.getIppKeyword()));
        }

        final ProxyPrintJobChunk jobChunk = new ProxyPrintJobChunk();

        jobChunk.setAssignedMedia(ippMediaSize);
        jobChunk.setAssignedMediaSource(assignedMediaSource);

        final ProxyPrintJobChunkRange chunkRange =
                new ProxyPrintJobChunkRange();

        chunkRange.pageBegin = 1;
        chunkRange.pageEnd =
                chunkRange.pageBegin + printReq.getNumberOfPages() - 1;

        jobChunk.getRanges().add(chunkRange);

        printReq.setJobChunkInfo(new ProxyPrintJobChunkInfo(jobChunk));

    }

    /**
     * Encodes the job name of the proxy printed {@link Document} to a unique
     * name that can be used to query the PaperCut's tbl_printer_usage_log table
     * about the print status.
     *
     * @param connection
     *            The {@link SmartSchoolConnection}.
     * @param document
     *            The {@link Document}
     * @return The encoded name.
     */
    private static String encodeProxyPrintJobName(
            final SmartSchoolConnection connection, final Document document) {

        final String suffix =
                String.format("%s%s%s%s", JOB_NAME_INFO_SEPARATOR,
                        connection.getAccountName(), JOB_NAME_INFO_SEPARATOR,
                        document.getId());

        if (suffix.length() > PaperCutDbProxy.COL_LEN_DOCUMENT_NAME) {
            throw new RuntimeException("length exceeded");
        }

        return String.format("%s%s", StringUtils.abbreviate(document.getName(),
                PaperCutDbProxy.COL_LEN_DOCUMENT_NAME - suffix.length()),
                suffix);
    }

    /**
     *
     * @param jobName
     *            The job name.
     * @return {@code null} when not found.
     */
    private static String getAccountFromProxyPrintJobName(final String jobName) {

        final String[] parts =
                StringUtils.split(jobName, JOB_NAME_INFO_SEPARATOR);

        if (parts.length < 3) {
            return null;
        }

        return parts[parts.length - 2];
    }

    /**
     * Uses {@link AdminPublisher} to publish a message.
     *
     * @param level
     *            The {@link PubLevelEnum}.
     * @param msg
     *            The message.
     */
    private static void publishAdminMsg(final PubLevelEnum level,
            final String msg) {
        AdminPublisher.instance().publish(PubTopicEnum.SMARTSCHOOL, level, msg);
    }

    /**
     * Proxy Prints the SmartSchool document.
     *
     * @param monitor
     *            The {@link SmartSchoolPrintMonitor} instance.
     * @param user
     *            The locked user
     * @param printerName
     *            The unique printer name.
     * @param document
     *            The {@link Document}.
     * @param downloadedFile
     *            The downloaded {@link File}.
     * @param printInInfo
     *            The {@link DocContentPrintInInfo}.
     * @param externalSupplierInfo
     *            The {@link ExternalSupplierInfo}.
     * @throws ProxyPrintException
     *             When logical proxy print errors.
     * @throws IppConnectException
     *             When connection to CUPS fails.
     */
    private static void processJobProxyPrint(
            final SmartSchoolPrintMonitor monitor, final User user,
            final String printerName, final Document document,
            final File downloadedFile, final DocContentPrintInInfo printInInfo,
            final ExternalSupplierInfo externalSupplierInfo)
            throws ProxyPrintException, IppConnectException {

        final ProxyPrintDocReq printReq =
                new ProxyPrintDocReq(PrintModeEnum.AUTO);

        printReq.setDocumentUuid(printInInfo.getUuidJob().toString());

        printReq.setAccountTrxInfoSet(printInInfo.getAccountTrxInfoSet());
        printReq.setComment(document.getComment());

        printReq.setNumberOfPages(printInInfo.getPageProps().getNumberOfPages());
        printReq.setPrinterName(printerName);
        printReq.setRemoveGraphics(false);
        printReq.setLocale(ServiceContext.getLocale());
        printReq.setIdUser(user.getId());
        printReq.setClearPages(false);

        final Map<String, String> ippOptions =
                PROXY_PRINT_SERVICE.getDefaultPrinterCostOptions(printerName);

        printReq.setOptionValues(ippOptions);

        /*
         * Get SmartSchool process info from the supplier data.
         */
        SmartSchoolPrintInData supplierData =
                (SmartSchoolPrintInData) externalSupplierInfo.getData();

        printReq.setNumberOfCopies(supplierData.getCopies());

        // media
        printReq.setMediaOption(supplierData.getMediaSize().getIppKeyword());

        // duplex
        if (supplierData.getDuplex() && ProxyPrintDocReq.hasDuplex(ippOptions)) {
            printReq.setDuplexLongEdge();
        }

        // grayscale
        if (!supplierData.getColor()
                && !ProxyPrintDocReq.isGrayscale(ippOptions)) {
            printReq.setGrayscale();
        }

        addProxyPrintJobChunk(printReq, supplierData.getMediaSize());

        /*
         * At this point we do NOT need the external data anymore.
         *
         * Since we don't want to use it in the print request (i.e. store it in
         * the database) we nullify the external data.
         *
         * We still hold the document id and status though to persist in the
         * database.
         */
        externalSupplierInfo.setData(null);

        if (monitor.isIntegratedWithPaperCut()) {
            printReq.setJobName(encodeProxyPrintJobName(
                    monitor.processingConnection, document));
        } else {
            printReq.setJobName(document.getName());
        }

        printReq.setSupplierInfo(externalSupplierInfo);

        /*
         *
         */
        if (LOGGER.isDebugEnabled()) {

            final StringBuilder msg = new StringBuilder();

            msg.append("ProxyPrint [").append(printReq.getJobName())
                    .append("]: ")
                    .append(printReq.getJobChunkInfo().getChunks().size())
                    .append(" chunk(s), ").append(printReq.getNumberOfPages())
                    .append(" page(s), ").append(printReq.getNumberOfCopies())
                    .append(" copies on ").append(printerName).append(" by [")
                    .append(user.getUserId()).append("]");
            LOGGER.debug(msg.toString());
        }

        /*
         * Proxy Print Transaction.
         */
        ReadWriteLockEnum.DATABASE_READONLY.setReadLock(true);

        final DaoContext daoContext = ServiceContext.getDaoContext();

        try {

            final UserDao userDao = ServiceContext.getDaoContext().getUserDao();

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("start printing [%s] ...",
                        printReq.getJobName()));
            }

            daoContext.beginTransaction();

            final User lockedUser = userDao.lock(user.getId());

            PROXY_PRINT_SERVICE.proxyPrintPdf(lockedUser, printReq,
                    downloadedFile);

            daoContext.commit();

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("finished printing [%s]",
                        printReq.getJobName()));
            }

        } finally {
            daoContext.rollback();
            ReadWriteLockEnum.DATABASE_READONLY.setReadLock(false);
        }
    }

    /**
     * Waits for processing to finish.
     *
     * @param monitor
     *            The {@link SmartSchoolPrintMonitor} instance.
     * @param millisInterval
     *            The sleep interval applied while {@link #isProcessing}.
     *
     * @throws InterruptedException
     *             When thread has been interrupted.
     */
    private static void waitForProcessing(
            final SmartSchoolPrintMonitor monitor, final long millisInterval)
            throws InterruptedException {

        boolean waiting = monitor.isProcessing;

        if (waiting) {
            LOGGER.trace("waiting for processing to finish ...");
        }

        while (monitor.isProcessing) {
            Thread.sleep(millisInterval);
            LOGGER.trace("processing ...");
        }

        if (waiting) {
            LOGGER.trace("processing finished.");
        }
    }

    /**
     * Tests the SmartSchool print connections and returns the total number of
     * jobs.
     *
     * @param nMessagesInbox
     *            Number of Inbox messages.
     * @throws SOAPException
     *             When SOAP connection fails.
     * @throws SmartSchoolException
     *             When SmartSchool returns errors.
     */
    public static void testConnection(final MutableInt nMessagesInbox)
            throws SOAPException, SmartSchoolException {

        final Map<String, SmartSchoolConnection> connections =
                SMARTSCHOOL_SERVICE.createConnections();

        if (connections.isEmpty()) {
            throw new SmartSchoolException(localizedMsg("no-connections"));
        }

        for (final SmartSchoolConnection connection : connections.values()) {

            final Jobticket jobTicket =
                    SMARTSCHOOL_SERVICE.getJobticket(connection);

            nMessagesInbox.setValue(nMessagesInbox.getValue().intValue()
                    + jobTicket.getDocuments().getDocument().size());
        }

    }

}
