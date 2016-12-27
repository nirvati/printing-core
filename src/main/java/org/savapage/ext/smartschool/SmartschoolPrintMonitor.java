/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.ext.smartschool;

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
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.time.DateUtils;
import org.savapage.core.ShutdownException;
import org.savapage.core.SpException;
import org.savapage.core.UnavailableException;
import org.savapage.core.cometd.AdminPublisher;
import org.savapage.core.cometd.PubLevelEnum;
import org.savapage.core.cometd.PubTopicEnum;
import org.savapage.core.community.CommunityDictEnum;
import org.savapage.core.concurrent.ReadWriteLockEnum;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.dao.DaoContext;
import org.savapage.core.dao.DocLogDao;
import org.savapage.core.dao.PrinterDao;
import org.savapage.core.dao.UserDao;
import org.savapage.core.dao.enums.DocLogProtocolEnum;
import org.savapage.core.dao.enums.ExternalSupplierEnum;
import org.savapage.core.dao.enums.ExternalSupplierStatusEnum;
import org.savapage.core.dao.enums.PrintModeEnum;
import org.savapage.core.doc.DocContent;
import org.savapage.core.doc.DocContentToPdfException;
import org.savapage.core.doc.DocContentTypeEnum;
import org.savapage.core.doc.IFileConverter;
import org.savapage.core.doc.PdfToGrayscale;
import org.savapage.core.dto.IppMediaSourceCostDto;
import org.savapage.core.ipp.IppMediaSizeEnum;
import org.savapage.core.ipp.attribute.syntax.IppKeyword;
import org.savapage.core.ipp.client.IppConnectException;
import org.savapage.core.job.AbstractJob;
import org.savapage.core.jpa.DocLog;
import org.savapage.core.jpa.IppQueue;
import org.savapage.core.jpa.Printer;
import org.savapage.core.jpa.User;
import org.savapage.core.msg.UserMsgIndicator;
import org.savapage.core.pdf.PdfCreateInfo;
import org.savapage.core.pdf.PdfSecurityException;
import org.savapage.core.pdf.PdfValidityException;
import org.savapage.core.pdf.SpPdfPageProps;
import org.savapage.core.print.proxy.ProxyPrintDocReq;
import org.savapage.core.print.proxy.ProxyPrintException;
import org.savapage.core.print.proxy.ProxyPrintJobChunk;
import org.savapage.core.print.proxy.ProxyPrintJobChunkInfo;
import org.savapage.core.print.proxy.ProxyPrintJobChunkRange;
import org.savapage.core.print.server.DocContentPrintException;
import org.savapage.core.services.AccountingService;
import org.savapage.core.services.DocLogService;
import org.savapage.core.services.JobTicketService;
import org.savapage.core.services.OutboxService;
import org.savapage.core.services.PrinterService;
import org.savapage.core.services.ProxyPrintService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.UserService;
import org.savapage.core.services.helpers.AccountTrxInfoSet;
import org.savapage.core.services.helpers.DocContentPrintInInfo;
import org.savapage.core.services.helpers.ExternalSupplierInfo;
import org.savapage.core.services.helpers.InboxSelectScopeEnum;
import org.savapage.core.services.helpers.PrinterAttrLookup;
import org.savapage.core.services.helpers.ThirdPartyEnum;
import org.savapage.core.users.IUserSource;
import org.savapage.core.util.AppLogHelper;
import org.savapage.core.util.DateUtil;
import org.savapage.core.util.FileSystemHelper;
import org.savapage.core.util.Messages;
import org.savapage.ext.ExtSupplierConnectException;
import org.savapage.ext.ExtSupplierException;
import org.savapage.ext.papercut.PaperCutDbProxy;
import org.savapage.ext.papercut.PaperCutException;
import org.savapage.ext.papercut.PaperCutHelper;
import org.savapage.ext.papercut.PaperCutPrintJobListener;
import org.savapage.ext.papercut.PaperCutPrintMonitorPattern;
import org.savapage.ext.papercut.PaperCutPrinterUsageLog;
import org.savapage.ext.papercut.PaperCutServerProxy;
import org.savapage.ext.papercut.PaperCutUser;
import org.savapage.ext.papercut.services.PaperCutService;
import org.savapage.ext.smartschool.job.SmartschoolPrintMonitorJob;
import org.savapage.ext.smartschool.services.SmartschoolProxyService;
import org.savapage.ext.smartschool.services.SmartschoolService;
import org.savapage.ext.smartschool.xml.Account;
import org.savapage.ext.smartschool.xml.Accounts;
import org.savapage.ext.smartschool.xml.Billinginfo;
import org.savapage.ext.smartschool.xml.Document;
import org.savapage.ext.smartschool.xml.Documents;
import org.savapage.ext.smartschool.xml.Jobticket;
import org.savapage.ext.smartschool.xml.Processinfo;
import org.savapage.ext.smartschool.xml.Requester;
import org.savapage.ext.smartschool.xml.Requestinfo;
import org.savapage.ext.smartschool.xml.SmartschoolRoleEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class SmartschoolPrintMonitor implements PaperCutPrintJobListener {

    /**
     *
     */
    private static int getJobTickerCounter = 0;

    /**
     * Fixed values for simulation "klassen".
     */
    private static final String SIMULATION_CLASS_1 = "simulatie.1A1";
    private static final String SIMULATION_CLASS_2 = "simulatie.1A2";

    /**
     * The logger.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(SmartschoolPrintMonitor.class);

    private static final String MSG_COMMENT_PRINT_PENDING_PAPERCUT =
            "Afdrukopdracht staat klaar in PaperCut.";

    private static final String MSG_COMMENT_PRINT_PENDING_SAVAPAGE =
            "Afdrukopdracht staat klaar in "
                    + CommunityDictEnum.SAVAPAGE.getWord() + ".";

    private static final String MSG_COMMENT_PRINT_SAFEPAGES =
            "Document is niet afgedrukt, maar is zichtbaar in "
                    + CommunityDictEnum.SAVAPAGE.getWord()
                    + ". Afdrukinformatie is niet bewaard. "
                    + "Ga naar de WebApp voor verdere acties.";

    public static final String MSG_COMMENT_PRINT_COMPLETED =
            "Document is met succes afgedrukt.";

    private static final String MSG_COMMENT_PRINT_QUEUED =
            "Document is met succes in de wachtrij geplaatst.";

    private static final String MSG_COMMENT_PRINT_CACHED_IN_PROXY =
            "Afdrukopdracht wordt verwerkt...";

    public static final String MSG_COMMENT_PRINT_CANCELLED =
            "Afdrukopdracht is geannuleerd.";

    public static final String MSG_COMMENT_PRINT_EXPIRED =
            "Afdrukopdracht is verlopen.";

    public static final String MSG_COMMENT_PRINT_DOCUMENT_TOO_LARGE =
            "De omvang van de afdrukopdracht is te groot "
                    + "om in één keer verwerkt te worden: "
                    + "splits uw verzoek op in kleinere opdrachten.";

    private static final String MSG_COMMENT_PRINT_CANCELLED_PFX =
            "Afdrukopdracht is geannuleerd: ";

    private static final String MSG_COMMENT_PRINT_CANCELLED_DOC_TYPE =
            MSG_COMMENT_PRINT_CANCELLED_PFX
                    + "document kan niet worden afgedrukt.";

    private static final String MSG_COMMENT_PRINT_CANCELLED_PDF_INVALID =
            MSG_COMMENT_PRINT_CANCELLED_PFX + "PDF document is ongeldig.";

    private static final String MSG_COMMENT_PRINT_CANCELLED_PDF_ENCRYPTED =
            MSG_COMMENT_PRINT_CANCELLED_PFX + "PDF document is versleuteld.";

    private static final String MSG_COMMENT_PRINT_ERROR_NO_COPIES =
            "Geen kopieen gespecificeerd.";

    private static final String MSG_COMMENT_PRINT_ERROR_NO_NODE_ID =
            "Geen Node ID gespecificeerd.";

    private static final String MSG_COMMENT_PRINT_ERROR_UNKNOWN_USER_BOTH =
            "Aanvrager \"%s\" is onbekend in "
                    + CommunityDictEnum.SAVAPAGE.getWord() + " en PaperCut.";

    private static final String MSG_COMMENT_PRINT_ERROR_UNKNOWN_USER_SAVAPAGE =
            "Aanvrager \"%s\" is onbekend in "
                    + CommunityDictEnum.SAVAPAGE.getWord() + ".";

    private static final String MSG_COMMENT_PRINT_ERROR_UNKNOWN_USER_PAPERCUT =
            "Aanvrager \"%s\" is onbekend in PaperCut.";

    /**
     * The map with Smartschool account connections with key is SmartSchool
     * account.
     */
    private final Map<String, SmartschoolConnection> connectionMap;

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
     * Indicating if shutdown is requested.
     */
    private volatile boolean isShutdownRequested = false;

    /**
     * The connection that is processing.
     */
    private SmartschoolConnection processingConnection = null;

    /**
     * .
     */
    private static final AccountingService ACCOUNTING_SERVICE =
            ServiceContext.getServiceFactory().getAccountingService();

    /**
     * .
     */
    private static final DocLogService DOCLOG_SERVICE =
            ServiceContext.getServiceFactory().getDocLogService();

    /**
     * .
     */
    private static final JobTicketService JOBTICKET_SERVICE =
            ServiceContext.getServiceFactory().getJobTicketService();

    /**
     * .
     */
    private static final OutboxService OUTBOX_SERVICE =
            ServiceContext.getServiceFactory().getOutboxService();

    /**
     * .
     */
    private static final ProxyPrintService PROXY_PRINT_SERVICE =
            ServiceContext.getServiceFactory().getProxyPrintService();

    /**
     * .
     */
    private static final PrinterService PRINTER_SERVICE =
            ServiceContext.getServiceFactory().getPrinterService();

    /**
     * .
     */
    private static final SmartschoolService SMARTSCHOOL_SERVICE =
            ServiceContext.getServiceFactory().getSmartSchoolService();

    /**
     * .
     */
    private static final SmartschoolProxyService SMARTSCHOOL_PROXY_SERVICE =
            ServiceContext.getServiceFactory().getSmartSchoolProxyService();

    /**
     * .
     */
    private static final PaperCutService PAPERCUT_SERVICE =
            ServiceContext.getServiceFactory().getPaperCutService();

    /**
    *
    */
    private static final UserService USER_SERVICE =
            ServiceContext.getServiceFactory().getUserService();

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
     * {@code true} if Smartschool job is non-secure printed to a PaperCut
     * managed printer.
     */
    private final boolean isPaperCutPrintNonSecure;

    /**
     * {@code true} if one of the proxy printers is a Job Ticket printer, by
     * which the Smartschool job can be released to a (PaperCut managed) proxy
     * printer.
     */
    private final boolean isPrintByJobTicket;

    /**
     * {@code true} if one of the proxy printers is a Hold/Release printer.
     */
    private final boolean isPrintByHoldRelease;

    /**
     * Number of seconds of a monitoring heartbeat.
     */
    private final int monitorHeartbeatSecs;

    /**
     * The number of monitoring heartbeats after which Smartschool is contacted
     * to get the {@link Jobticket}.
     */
    private final int monitorHeartbeatsForJobticket;

    /**
     * @param connections
     *            A map of {@link SmartschoolConnection} object by Smartschool
     *            account key.
     * @param papercutPrintNonSecure
     *            {@code true} if non-secure print to a PaperCut managed
     *            printer.
     * @param printByJobTicket
     *            {@code true} if one of the proxy printers is a Job Ticket
     *            printer, by which the Smartschool job can be released to a
     *            (PaperCut managed) proxy printer.
     * @param papercutProxy
     *            The {@link PaperCutServerProxy}.
     * @param papercutProxyDb
     *            The {@link PaperCutDbProxy}.
     * @param simulation
     *            {@code true} when in simulation mode
     * @throws SOAPException
     *             When the {@link SOAPConnection} cannot be established.
     */
    public SmartschoolPrintMonitor(
            final Map<String, SmartschoolConnection> connections,
            final boolean papercutPrintNonSecure,
            final boolean printByJobTicket,
            final PaperCutServerProxy papercutProxy,
            final PaperCutDbProxy papercutProxyDb, final boolean simulation)
            throws SOAPException {

        this.simulationMode = simulation;

        this.isConnected = true;

        this.connectionMap = connections;

        this.isPaperCutPrintNonSecure = papercutPrintNonSecure;

        this.isPrintByJobTicket = printByJobTicket;

        this.isPrintByHoldRelease = SMARTSCHOOL_SERVICE
                .hasHoldReleaseProxyPrinter(connections.values());

        this.smartSchoolQueue = SMARTSCHOOL_SERVICE.getSmartSchoolQueue();
        this.papercutServerProxy = papercutProxy;
        this.papercutDbProxy = papercutProxyDb;

        //
        final ConfigManager cm = ConfigManager.instance();

        this.monitorHeartbeatSecs = cm.getConfigInt(
                IConfigProp.Key.SMARTSCHOOL_SOAP_PRINT_POLL_HEARTBEAT_SECS);

        //
        final IConfigProp.Key sessionHeartbeatSecsKey;

        if (simulation) {
            sessionHeartbeatSecsKey =
                    IConfigProp.Key.SMARTSCHOOL_SIMULATION_SOAP_PRINT_POLL_HEARTBEATS;
        } else {
            sessionHeartbeatSecsKey =
                    IConfigProp.Key.SMARTSCHOOL_SOAP_PRINT_POLL_HEARTBEATS;
        }

        this.monitorHeartbeatsForJobticket =
                cm.getConfigInt(sessionHeartbeatSecsKey);
    }

    /**
     *
     * @return
     */
    public boolean isSimulationMode() {
        return this.simulationMode;
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

        for (final SmartschoolConnection connection : this.connectionMap
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
     * {@link SmartschoolPrintMonitorJob}.
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

        this.isShutdownRequested = true;

        /*
         * Wait for processing to finish.
         */
        waitForProcessing(this, DateUtil.DURATION_MSEC_SECOND);

        int nActions = 0;

        if (this.isConnected) {

            for (final SmartschoolConnection connection : this.connectionMap
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
     * @return {@code true} when Smartschool PaperCut integration is active
     *         either directly through non-secure proxy printing, or indirectly
     *         through Job Ticket release.
     */
    private boolean isIntegratedWithPaperCut() {
        return this.papercutServerProxy != null;
    }

    /**
     * @return The number of msecs since last heartbeat after which node is
     *         considered dead.
     */
    private long getNodeExpiryMsec() {
        final int extraFactor = 2;
        return this.monitorHeartbeatSecs * this.monitorHeartbeatsForJobticket
                * DateUtil.DURATION_MSEC_SECOND * extraFactor;
    }

    /**
     * Monitors incoming Smartschool print jobs and print status.
     * <p>
     * <b>Note</b>: This is a blocking call that returns when the maximum
     * duration is reached (or a {@link #disconnect()} is called before
     * returning).
     * </p>
     *
     * @param monitorDurationSecs
     *            The duration after which this method returns.
     * @throws InterruptedException
     *             When we are interrupted.
     * @throws ShutdownException
     *             When we application shutdown is in progress.
     * @throws PaperCutException
     * @throws ExtSupplierConnectException
     *             When a SOAP communication error occurs.
     * @throws ExtSupplierException
     *             When Smartschool returns a fault.
     */
    public void monitor(final int monitorDurationSecs)
            throws InterruptedException, ShutdownException, PaperCutException,
            DocContentToPdfException, ExtSupplierException,
            ExtSupplierConnectException {

        final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

        int heartbeatPollCounter = 0;

        try {

            final long sessionEndTime = System.currentTimeMillis()
                    + DateUtil.DURATION_MSEC_SECOND * monitorDurationSecs;

            final Date sessionEndDate = new Date(sessionEndTime);

            /*
             * Do an initial poll when in simulation mode. When in production
             * mode the minimal polling frequency enforced by Smartschool must
             * be respected.
             */
            if (this.simulationMode && this.isConnected) {
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
                if (System.currentTimeMillis() > sessionEndTime) {
                    break;
                }

                /*
                 * Smartschool still enabled?
                 */
                if (!ConfigManager.isSmartSchoolPrintEnabled()) {
                    LOGGER.trace("Smartschool disabled by administrator.");
                    break;
                }

                /*
                 * Stop if PaperCut integration changed.
                 */
                final boolean isPaperCutEnabled = ConfigManager.instance()
                        .isConfigValue(Key.SMARTSCHOOL_PAPERCUT_ENABLE);

                if (isPaperCutEnabled && !this.isPaperCutPrintNonSecure) {
                    break;
                }

                if (!isPaperCutEnabled && this.isPaperCutPrintNonSecure) {
                    break;
                }

                heartbeatPollCounter++;

                if (heartbeatPollCounter < this.monitorHeartbeatsForJobticket) {

                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace("Waiting... next ["
                                + dateFormat.format(DateUtils.addSeconds(
                                        new Date(), this.monitorHeartbeatSecs))
                                + "] till [" + dateFormat.format(sessionEndDate)
                                + "] ...");
                    }
                    Thread.sleep(this.monitorHeartbeatSecs
                            * DateUtil.DURATION_MSEC_SECOND);

                } else if (this.isConnected) {

                    heartbeatPollCounter = 0;

                    processJobs(this);
                }
            }

        } catch (SmartschoolException e) {

            throw new ExtSupplierException(e.getMessage(), e);

        } catch (SOAPException e) {

            throw new ExtSupplierConnectException(e.getMessage(), e);

        } finally {

            disconnect();
        }
    }

    /**
     * @param connection
     *            The {@link SmartschoolConnection}.
     * @param simulationMode
     *            {@code true} when in simulation mode.
     * @return The job ticket.
     * @throws SmartschoolException
     *             When Smartschool reported an error.
     * @throws SmartschoolTooManyRequestsException
     *             When HTTP status 429 "Too Many Requests" occurred.
     * @throws SOAPException
     *             When SOAP error.
     */
    private static Jobticket getJobticket(
            final SmartschoolConnection connection,
            final boolean simulationMode) throws SmartschoolException,
            SmartschoolTooManyRequestsException, SOAPException {

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
     *            The {@link SmartschoolConnection}.
     * @param documentId
     *            The SmartSchooldocument id.
     * @param status
     *            The {@link SmartschoolPrintStatusEnum}.
     * @param comment
     *            The Smartschool feedback comment.
     * @param simulationMode
     *            {@code true} when in simulation mode.
     * @throws SmartschoolException
     *             When Smartschool reported an error.
     * @throws SOAPException
     *             When SOAP error.
     */
    private static void reportDocumentStatus(
            final SmartschoolConnection connection, final String documentId,
            final SmartschoolPrintStatusEnum status, final String comment,
            final boolean simulationMode)
            throws SmartschoolException, SOAPException {

        if (simulationMode) {

            if (LOGGER.isInfoEnabled()) {
                LOGGER.info(String.format(
                        "Simulated reportDocumentStatus [%s] [%s]",
                        status.toString(), comment));
            }

        } else {
            SMARTSCHOOL_SERVICE.reportDocumentStatus(connection, documentId,
                    status, comment);
        }
    }

    /**
     * Downloads a document for printing into the application's temp directory.
     * See {@link ConfigManager#getAppTmpDir()}.
     *
     * @param connection
     * @param document
     * @param uuid
     * @return The downloaded {@link File}.
     * @throws IOException
     * @throws ShutdownException
     *             When download was interrupted because of a
     *             {@link SmartschoolConnection#setShutdownRequested(boolean)}
     *             request.
     */
    private static File downloadDocument(final SmartschoolConnection connection,
            final Document document, final UUID uuid,
            final boolean simulationMode)
            throws IOException, ShutdownException {

        if (simulationMode) {

            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Simulated document download.");
            }

            return downloadDocumentSimulation(document, uuid);
        }

        return SMARTSCHOOL_SERVICE.downloadDocument(connection, document, uuid);
    }

    /**
     * Simulates the download of a one-page document.
     *
     * @param document
     * @param uuid
     * @return The simulated download {@link File}.
     */
    private static File downloadDocumentSimulation(final Document document,
            final UUID uuid) {

        final File downloadFile =
                SMARTSCHOOL_SERVICE.getDownloadFile(document.getName(), uuid);

        final com.itextpdf.text.Document pdfDoc =
                new com.itextpdf.text.Document();

        final Font font = FontFactory.getFont("Courier", 42f);
        Paragraph para;

        try {
            final String text = "\n\n\n\nSmartschool Simulation";

            PdfWriter.getInstance(pdfDoc, new FileOutputStream(downloadFile));
            pdfDoc.open();

            int nPage = 0;

            // Page #1
            font.setColor(BaseColor.GREEN);
            para = new Paragraph(String.format("%s\n\nPage %d", text, ++nPage),
                    font);
            para.setAlignment(Element.ALIGN_CENTER);
            pdfDoc.add(para);

            // Page #2
            pdfDoc.newPage();

            font.setColor(BaseColor.BLACK);
            para = new Paragraph(String.format("%s\n\nPage %d", text, ++nPage),
                    font);
            para.setAlignment(Element.ALIGN_CENTER);
            pdfDoc.add(para);

            // Page #3
            pdfDoc.newPage();

            font.setColor(BaseColor.BLACK);
            para = new Paragraph(String.format("%s\n\nPage %d", text, ++nPage),
                    font);
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

        final String simulationUser = ConfigManager.instance()
                .getConfigValue(Key.SMARTSCHOOL_SIMULATION_USER);

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
        document.setComment("Simulátiön Document • 'simulation'");
        document.setName("simulatë•'doc' & 'tést.pdf");

        //
        final Requester requester = new Requester();
        document.setRequester(requester);

        requester.setUsername(simulationUser);

        requester.setRole(SmartschoolRoleEnum.TEACHER.getXmlValue());

        //
        final Processinfo processinfo = new Processinfo();
        document.setProcessinfo(processinfo);

        processinfo.setPapersize("a4");

        processinfo.setDuplex("off");
        // processinfo.setDuplex("on");

        processinfo.setRendermode(SmartschoolConstants.RENDER_MODE_GRAYSCALE);
        // processinfo.setRendermode("color");

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
        account.setUsername(ConfigManager.instance()
                .getConfigValue(Key.SMARTSCHOOL_SIMULATION_STUDENT_1));

        account.setRole(SmartschoolRoleEnum.STUDENT.getXmlValue());

        // Account #2
        account = new Account();
        accountList.add(account);

        account.setClazz(SIMULATION_CLASS_2);
        account.setCopies(2);
        account.setExtra(2);
        account.setUsername(ConfigManager.instance()
                .getConfigValue(Key.SMARTSCHOOL_SIMULATION_STUDENT_2));
        account.setRole(SmartschoolRoleEnum.STUDENT.getXmlValue());

        // Account #3
        account = new Account();
        accountList.add(account);

        account.setClazz("");
        account.setCopies(1);
        account.setExtra(0);

        account.setUsername(simulationUser);
        account.setRole(SmartschoolRoleEnum.TEACHER.getXmlValue());

        //
        return ticket;
    }

    /**
     * Processes the Smartschool print queue(s) and monitors PaperCut print
     * status.
     *
     * @param monitor
     *            The {@link SmartschoolPrintMonitor} instance.
     * @throws SOAPException
     *             When connectivity problems.
     * @throws SmartschoolException
     *             When Smartschool reported an error.
     * @throws ShutdownException
     *             When interrupted by a shutdown request.
     * @throws PaperCutException
     * @throws DocContentToPdfException
     * @throws ExtSupplierConnectException
     * @throws ExtSupplierException
     */
    private static void processJobs(final SmartschoolPrintMonitor monitor)
            throws ShutdownException, PaperCutException,
            DocContentToPdfException, ExtSupplierException,
            ExtSupplierConnectException, SmartschoolException, SOAPException {

        /*
         * Any progress on the PaperCut Print front?
         */
        if (monitor.isPaperCutPrintNonSecure || monitor.isPrintByJobTicket) {
            monitorPaperCut(monitor);
        }

        /*
         * Any progress on the SavaPage HOLD jobs and Job Tickets?
         */
        if (monitor.isPrintByHoldRelease || monitor.isPrintByJobTicket) {
            monitorSavaPage(monitor);
        }

        /*
         * Any Smartschool Job Tickets waiting?
         */
        for (final SmartschoolConnection connection : monitor.connectionMap
                .values()) {

            monitor.processingConnection = connection;

            /*
             * Any jobs to print?
             */
            final Jobticket jobTicket;

            try {

                jobTicket = getJobticket(connection, monitor.simulationMode);

            } catch (SmartschoolTooManyRequestsException e) {
                final String msg = "Smartschool reports \"too many request\".";
                publishAdminMsg(PubLevelEnum.WARN, msg);
                LOGGER.warn(msg);
                continue;
            }

            /*
             * Logging.
             */
            if (LOGGER.isDebugEnabled()) {

                final int nDocs = jobTicket.getDocuments().getDocument().size();

                if (nDocs == 0) {
                    LOGGER.debug("no documents to print.");
                } else {
                    LOGGER.debug(
                            String.format("[%d] document(s) to print.", nDocs));
                }
            }

            /*
             * Process print jobs one-by-one.
             */
            for (final Document doc : jobTicket.getDocuments().getDocument()) {

                final MutableBoolean doProcess = new MutableBoolean();
                final MutableBoolean doProxy = new MutableBoolean();
                final StringBuilder proxyClientNodeId = new StringBuilder();

                evaluateJobProcessing(monitor, doc, doProcess, doProxy,
                        proxyClientNodeId);

                if (doProcess.isTrue()) {

                    if (doProxy.isTrue()) {
                        storeJobInProxyCache(monitor, doc,
                                proxyClientNodeId.toString());
                    } else {
                        processJob(monitor, doc);
                    }
                }
            }
        }
    }

    /**
     * Checks if this job (document) can be processed. If a required Node ID tag
     * is missing from the document comment the
     * {@link #processJobWithoutNodeId(SmartschoolConnection, boolean, Document)}
     * method is executed, so we don't encounter the document at the next poll.
     *
     * @param monitor
     *            The {@link SmartschoolPrintMonitor}.
     * @param doc
     *            The Smartschool {@link Document}.
     * @param doProcess
     *            Set to {@code true} if job can be processed.
     * @param doProxy
     *            Set to {@code true} if job must be processed as proxy.
     * @param proxyClientNodeId
     *            The cluster client node this job is aimed at.
     * @throws SmartschoolException
     * @throws SOAPException
     * @throws SpException
     *             When cluster configuration error.
     */
    private static void evaluateJobProcessing(
            final SmartschoolPrintMonitor monitor, final Document doc,
            final MutableBoolean doProcess, final MutableBoolean doProxy,
            final StringBuilder proxyClientNodeId)
            throws SmartschoolException, SOAPException {

        doProcess.setFalse();
        doProxy.setFalse();

        final SmartschoolConnection connection = monitor.processingConnection;

        /*
         * Process if connection is NOT part of a cluster.
         */
        if (!connection.isPartOfCluster()) {
            doProcess.setTrue();
            return;
        }

        /*
         * INVARIANT: This connection MUST has a Node ID since it is part of
         * cluster.
         */
        final String nodeId = connection.getNodeId();

        if (StringUtils.isBlank(nodeId)) {

            final StringBuilder msg = new StringBuilder();
            msg.append("Connection [")
                    .append(StringUtils
                            .defaultString(connection.getAccountName()))
                    .append("] is part of a cluster, but Node ID is missing.");

            throw new SpException(msg.toString());
        }

        /*
         * INVARIANT: Document comment MUST be present if connection is part of
         * cluster.
         */
        final String docComment = doc.getComment();
        if (StringUtils.isBlank(docComment)) {
            doProcess.setFalse();
            processJobWithoutNodeId(connection, monitor.simulationMode, doc);
            return;
        }

        final String targetNodeId = SMARTSCHOOL_PROXY_SERVICE.getNodeId(doc);

        /*
         * Process if connection is part of a cluster and the document comment
         * contains the tagged Node ID.
         */
        if (targetNodeId != null && targetNodeId.equalsIgnoreCase(nodeId)) {
            doProcess.setTrue();
            return;
        }

        /*
         * If this connection is a proxy, we will proxy if the target Node ID is
         * alive.
         */
        if (connection.isProxy() && SMARTSCHOOL_PROXY_SERVICE
                .isNodeAlive(targetNodeId, monitor.getNodeExpiryMsec())) {
            doProcess.setTrue();
            doProxy.setTrue();
            proxyClientNodeId.append(targetNodeId);
            return;
        }
    }

    /**
     * Monitors the print job status in PaperCut for
     * {@link SmartschoolPrintStatusEnum#PENDING_EXT} Smartschool jobs (i.e jobs
     * that were proxy printed to a PaperCut managed printer).
     *
     * @param smartschoolMonitor
     *            The {@link SmartschoolPrintMonitor}.
     * @throws PaperCutException
     * @throws ExtSupplierException
     * @throws ExtSupplierConnectException
     */
    private static void
            monitorPaperCut(final SmartschoolPrintMonitor smartschoolMonitor)
                    throws PaperCutException, ExtSupplierException,
                    ExtSupplierConnectException {

        final PaperCutPrintMonitorPattern papercutMonitor =
                new SmartschoolPaperCutMonitor(
                        smartschoolMonitor.papercutServerProxy,
                        smartschoolMonitor.papercutDbProxy, smartschoolMonitor);

        papercutMonitor.process();
    }

    /**
     * Monitors the print job status in SavaPage for
     * {@link ExternalSupplierStatusEnum#PENDING_CANCEL} and
     * {@link ExternalSupplierStatusEnum#PENDING_COMPLETE} Smartschool jobs (i.e
     * jobs that were proxy printed to a SavaPage managed Hold/Release printer
     * or Job Ticket).
     *
     * @param smartschoolMonitor
     *            The {@link SmartschoolPrintMonitor}.
     * @throws ExtSupplierConnectException
     * @throws ExtSupplierException
     */
    private static void
            monitorSavaPage(final SmartschoolPrintMonitor smartschoolMonitor)
                    throws ExtSupplierException, ExtSupplierConnectException {

        final DaoContext daoCtx = ServiceContext.getDaoContext();
        final DocLogDao doclogDAO = daoCtx.getDocLogDao();

        final DocLogDao.ListFilter filter = new DocLogDao.ListFilter();
        filter.setExternalSupplier(ExternalSupplierEnum.SMARTSCHOOL);

        //
        filter.setExternalStatus(ExternalSupplierStatusEnum.PENDING_CANCEL);
        final List<DocLog> listCancel = doclogDAO.getListChunk(filter);

        //
        filter.setExternalStatus(ExternalSupplierStatusEnum.PENDING_COMPLETE);
        final List<DocLog> listComplete = doclogDAO.getListChunk(filter);

        //
        if (listCancel.isEmpty() && listComplete.isEmpty()) {
            return;
        }

        ReadWriteLockEnum.DATABASE_READONLY.setReadLock(true);

        try {
            if (!listCancel.isEmpty()) {
                monitorSavaPageUpdate(smartschoolMonitor, daoCtx, doclogDAO,
                        listCancel, ExternalSupplierStatusEnum.CANCELLED);
            }
            if (!listComplete.isEmpty()) {
                monitorSavaPageUpdate(smartschoolMonitor, daoCtx, doclogDAO,
                        listComplete, ExternalSupplierStatusEnum.COMPLETED);
            }

        } finally {
            daoCtx.rollback();
            ReadWriteLockEnum.DATABASE_READONLY.setReadLock(false);
        }
    }

    /**
     *
     * @param smartschoolMonitor
     * @param daoCtx
     * @param doclogDAO
     * @param list
     * @param statusNew
     * @throws ExtSupplierException
     * @throws ExtSupplierConnectException
     */
    private static void monitorSavaPageUpdate(
            final SmartschoolPrintMonitor smartschoolMonitor,
            final DaoContext daoCtx, final DocLogDao doclogDAO,
            final List<DocLog> list, final ExternalSupplierStatusEnum statusNew)
            throws ExtSupplierException, ExtSupplierConnectException {

        for (final DocLog docLog : list) {

            final String warning;

            /*
             * Smartschool update.
             */
            if (docLog.getExternalData() == null) {

                warning = "Smartschool external data not specified.";

            } else {

                final SmartschoolPrintInData data = SmartschoolPrintInData
                        .createFromData(docLog.getExternalData());

                if (data.getAccount() == null) {
                    warning = "Smartschool account not specified.";
                } else {

                    final SmartschoolConnection connection =
                            smartschoolMonitor.connectionMap
                                    .get(data.getAccount());

                    if (connection == null) {

                        warning = String.format(
                                "Smartschool account [%s] not configured.",
                                data.getAccount());

                    } else {
                        warning = null;
                        smartschoolMonitor.onPrintJobProcessed(null, connection,
                                docLog.getExternalId(), docLog.getTitle(),
                                statusNew, false, null);
                    }
                }
            }

            /*
             * Be forgiving when data missing, just log the warning.
             */
            if (warning != null) {
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn(String.format(
                            "DocLog from External Supplier [%s] "
                                    + "ID [%s] Status [%s] Title [%s]: %s",
                            docLog.getExternalSupplier(),
                            docLog.getExternalId(), docLog.getExternalStatus(),
                            docLog.getTitle(), warning));
                }
                publishAdminMsg(PubLevelEnum.WARN, warning);
            }

            /*
             * SavaPage update.
             */
            daoCtx.beginTransaction();
            docLog.setExternalStatus(statusNew.toString());
            doclogDAO.update(docLog);
            daoCtx.commit();
        }
    }

    /**
     * Creates {@link AccountTrxInfoSet} for a Smartschool klas and personal
     * copies.
     *
     * <p>
     * NOTE: This method has its own database transaction scope, because shared
     * user accounts might get lazy created.
     * </p>
     *
     * @param connection
     *            The {@link SmartschoolConnection}.
     * @param nTotCopies
     *            The total number of copies to be proxy printed.
     * @param klasCopies
     *            The collected "klas" copies.
     * @param userCopies
     *            The collected user copies.
     * @param userKlas
     *            Klas lookup for a user.
     * @return The {@link AccountTrxInfoSet}.
     */
    private static AccountTrxInfoSet createAccountTrxInfoSet(
            final SmartschoolConnection connection, final int nTotCopies,
            final Map<String, Integer> klasCopies,
            final Map<String, Integer> userCopies,
            final Map<String, String> userKlas) {

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

            infoSet = SMARTSCHOOL_SERVICE.createPrintInAccountTrxInfoSet(
                    connection, sharedParentAccount, nTotCopies, klasCopies,
                    userCopies, userKlas);

            daoContext.commit();

        } finally {

            daoContext.rollback();

            ReadWriteLockEnum.DATABASE_READONLY.setReadLock(false);

        }

        return infoSet;
    }

    /**
     * Creates {@link ExternalSupplierInfo} with {@link SmartschoolPrintInData}
     * supplier data.
     *
     * @param connection
     *            The {@link SmartschoolConnection}.
     * @param document
     *            The {@link Document}.
     * @param nTotCopies
     *            The number of copies to be proxy printed.
     * @return The {@link ExternalSupplierInfo}.
     */
    private static ExternalSupplierInfo createExternalSupplierInfo(
            final SmartschoolConnection connection, final Document document,
            final int nTotCopies) {

        final ExternalSupplierInfo supplierInfo = new ExternalSupplierInfo();

        supplierInfo.setSupplier(ExternalSupplierEnum.SMARTSCHOOL);

        final SmartschoolPrintInData supplierData =
                new SmartschoolPrintInData();

        supplierData.setAccount(connection.getAccountName());

        supplierData.setCopies(nTotCopies);

        // Translate Smartschool processInfo to SavaPage speak.
        final Processinfo processInfo = document.getProcessinfo();

        // media
        if (processInfo.getPapersize()
                .equalsIgnoreCase(SmartschoolConstants.PAPERSIZE_A3)) {
            supplierData.setMediaSize(IppMediaSizeEnum.ISO_A3);
        } else {
            supplierData.setMediaSize(IppMediaSizeEnum.ISO_A4);
        }

        // duplex
        supplierData.setDuplex(asBoolean(processInfo.getDuplex()));

        // grayscale
        supplierData.setColor(!processInfo.getRendermode()
                .equalsIgnoreCase(SmartschoolConstants.RENDER_MODE_GRAYSCALE));

        // staple (TODO)

        // punch (TODO)

        // frontcover (TODO)

        // backcover (TODO)

        // confidential (TODO)

        // Do NOT the status.

        //
        supplierInfo.setAccount(supplierData.getAccount());
        supplierInfo.setId(document.getId());

        supplierInfo.setData(supplierData);

        return supplierInfo;
    }

    /**
     * Creates {@link DocContentPrintInInfo} for a downloaded SmartSchool
     * document.
     *
     * @param connection
     *            The {@link SmartschoolConnection}.
     * @param document
     *            The Smartschool {@link Document} to be printed.
     * @param downloadedFile
     *            The downloaded {@link File}.
     * @param nTotCopies
     *            The total number of copies to be proxy printed.
     * @param uuid
     *            The SavaPage assigned {@link UUID} for the downloaded
     *            document.
     * @return The {@link DocContentPrintInInfo}.
     * @throws PdfSecurityException
     *             When the PDF file has security restrictions.
     * @throws PdfValidityException
     *             When the document isn't a valid PDF document.
     */
    private static DocContentPrintInInfo createPrintInInfo(
            final SmartschoolConnection connection, final Document document,
            final File downloadedFile, final int nTotCopies, final UUID uuid)
            throws PdfSecurityException, PdfValidityException {

        /*
         * Get the PDF properties to check security issues.
         */
        final SpPdfPageProps pdfProps;

        try {
            pdfProps = SpPdfPageProps.create(downloadedFile.getCanonicalPath());
        } catch (PdfSecurityException | PdfValidityException e) {
            throw e;
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
        printInInfo.setOriginatorIp(connection
                .getEndpointIpAddress(SmartschoolRequestEnum.GET_DOCUMENT));
        printInInfo.setPageProps(pdfProps);
        printInInfo.setUuidJob(uuid);

        return printInInfo;
    }

    /**
     * Finds the active (non-deleted) SavaPage user (optionally lazy insert).
     * <p>
     * NOTE: this method has its own database transaction scope.
     * </p>
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

            user = USER_SERVICE.lazyInsertExternalUser(userSource, userId,
                    userSourceGroup);

            if (user == null) {
                ServiceContext.getDaoContext().rollback();
            } else {
                ServiceContext.getDaoContext().commit();
            }
        }

        return user;
    }

    /**
     * Stores a Smartschool print job in the proxy cache and reports status and
     * comment back to Smartschool.
     *
     * @param monitor
     *            The {@link SmartschoolPrintMonitor} instance.
     * @param document
     *            The Smartschool document.
     * @param proxyClientNodeId
     *            The cluster Node ID the document is meant for.
     * @throws ShutdownException
     *             When interrupted by a shutdown request.
     * @throws SOAPException
     * @throws SmartschoolException
     */
    private static void storeJobInProxyCache(
            final SmartschoolPrintMonitor monitor, final Document document,
            final String proxyClientNodeId)
            throws ShutdownException, SmartschoolException, SOAPException {

        boolean exception = true;

        File pdfFile = null;

        try {
            monitor.isProcessing = true;

            pdfFile = SMARTSCHOOL_SERVICE.downloadDocumentForProxy(
                    monitor.processingConnection, document);

            final String accountName =
                    monitor.processingConnection.getAccountName();

            SMARTSCHOOL_PROXY_SERVICE.cacheDocument(accountName,
                    proxyClientNodeId, document, pdfFile);

            exception = false;

        } catch (IOException e) {

            throw new SpException(e.getMessage());

        } finally {

            if (exception && pdfFile != null) {
                pdfFile.delete();
            }
            monitor.isProcessing = false;
        }

        reportDocumentStatus(monitor.processingConnection, document.getId(),
                SmartschoolPrintStatusEnum.PENDING,
                MSG_COMMENT_PRINT_CACHED_IN_PROXY, monitor.simulationMode);
    }

    /**
     * Processes a Smartschool print job.
     * <p>
     * The job is canceled when the requesting user does NOT exist in SavaPage
     * (and PaperCut, when integration is enabled).
     * </p>
     *
     * @param monitor
     *            The {@link SmartschoolPrintMonitor} instance.
     * @param document
     *            The Smartschool document.
     * @throws SOAPException
     *             When connectivity problems.
     * @throws SmartschoolException
     *             When Smartschool reported an error.
     * @throws ShutdownException
     *             When interrupted by a shutdown request.
     * @throws DocContentToPdfException
     */
    private static void processJob(SmartschoolPrintMonitor monitor,
            final Document document) throws SmartschoolException, SOAPException,
            ShutdownException, DocContentToPdfException {

        ServiceContext.resetTransactionDate();

        final ConfigManager cm = ConfigManager.instance();

        final boolean lazyInsertUser =
                cm.isConfigValue(Key.SMARTSCHOOL_USER_INSERT_LAZY_PRINT)
                        && cm.isUserInsertLazyPrint();

        final IUserSource userSource = cm.getUserSource();

        final String userSourceGroup =
                cm.getConfigValue(Key.USER_SOURCE_GROUP).trim();

        /*
         * Does requesting user exist in SavaPage?
         */
        final String userId = document.getRequester().getUsername();

        final User requestingUser = findActiveUser(userId, lazyInsertUser,
                userSource, userSourceGroup);

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
            papercutUser = PAPERCUT_SERVICE
                    .findUser(monitor.papercutServerProxy, userId);
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
                final Map<String, String> userKlas = new HashMap<>();

                final int nTotCopies = collectCopyInfo(monitor, document,
                        klasCopies, userCopies, userKlas, lazyInsertUser,
                        userSource, userSourceGroup);

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
                                    document, uuid, monitor.simulationMode);

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
                                    monitor.processingConnection, nTotCopies,
                                    klasCopies, userCopies, userKlas);
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

            if (downloadedFile == null) {
                /*
                 * Notify and give it another chance at next polling cycle.
                 */
                final String error = AppLogHelper.logError(
                        SmartschoolPrintMonitor.class, "download-error",
                        document.getName(), e.getMessage());

                publishAdminMsg(PubLevelEnum.ERROR, error);

            } else {
                throw new SpException(e.getMessage(), e);
            }

        } catch (DocContentPrintException e) {

            publishAdminMsg(PubLevelEnum.WARN, localizedMsg("print-cancelled",
                    document.getName(), e.getMessage()));

            reportDocumentStatus(monitor.processingConnection, document.getId(),
                    SmartschoolPrintStatusEnum.CANCELLED,
                    MSG_COMMENT_PRINT_CANCELLED_DOC_TYPE,
                    monitor.simulationMode);

        } catch (PdfSecurityException e) {

            publishAdminMsg(PubLevelEnum.WARN, localizedMsg("print-cancelled",
                    document.getName(), e.getMessage()));

            reportDocumentStatus(monitor.processingConnection, document.getId(),
                    SmartschoolPrintStatusEnum.CANCELLED,
                    MSG_COMMENT_PRINT_CANCELLED_PDF_ENCRYPTED,
                    monitor.simulationMode);

        } catch (PdfValidityException e) {

            publishAdminMsg(PubLevelEnum.WARN, localizedMsg("print-cancelled",
                    document.getName(), e.getMessage()));

            reportDocumentStatus(monitor.processingConnection, document.getId(),
                    SmartschoolPrintStatusEnum.CANCELLED,
                    MSG_COMMENT_PRINT_CANCELLED_PDF_INVALID,
                    monitor.simulationMode);

        } catch (ProxyPrintException e) {

            publishAdminMsg(PubLevelEnum.WARN, localizedMsg("print-error",
                    document.getName(), e.getMessage()));

            reportDocumentStatus(monitor.processingConnection, document.getId(),
                    SmartschoolPrintStatusEnum.ERROR, e.getMessage(),
                    monitor.simulationMode);

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
        return Messages.getMessage(SmartschoolPrintMonitor.class,
                ServiceContext.getLocale(), msgKey, strings);
    }

    /**
     * Processes a job request without any copies.
     *
     * @param connection
     *            The {@link SmartschoolConnection}.
     * @param simulationMode
     *            {@code true} when in simulation mode.
     * @param document
     *            The document.
     * @throws SOAPException
     *             When connectivity problems.
     * @throws SmartschoolException
     *             When Smartschool reported an error.
     */
    private static void processJobNoCopies(
            final SmartschoolConnection connection,
            final boolean simulationMode, final Document document)
            throws SmartschoolException, SOAPException {

        if (LOGGER.isWarnEnabled()) {
            LOGGER.warn(String.format(
                    "Document [%s] [%s] skipped: no copies identified.",
                    document.getId(), document.getName()));
        }

        publishAdminMsg(PubLevelEnum.WARN,
                String.format("Smartschool document [%s]: %s",
                        document.getName(), MSG_COMMENT_PRINT_ERROR_NO_COPIES));

        reportDocumentStatus(connection, document.getId(),
                SmartschoolPrintStatusEnum.ERROR,
                MSG_COMMENT_PRINT_ERROR_NO_COPIES, simulationMode);
    }

    /**
     * Processes a job request without a Node ID tag.
     *
     * @param connection
     *            The {@link SmartschoolConnection}.
     * @param simulationMode
     *            {@code true} when in simulation mode.
     * @param document
     *            The document.
     * @throws SOAPException
     *             When connectivity problems.
     * @throws SmartschoolException
     *             When Smartschool reported an error.
     */
    private static void processJobWithoutNodeId(
            final SmartschoolConnection connection,
            final boolean simulationMode, final Document document)
            throws SmartschoolException, SOAPException {

        if (LOGGER.isWarnEnabled()) {
            LOGGER.warn(String.format(
                    "Document [%s] [%s] skipped: Node ID is missing.",
                    document.getId(), document.getName()));
        }

        publishAdminMsg(PubLevelEnum.WARN,
                String.format("Smartschool document [%s]: %s",
                        document.getName(),
                        MSG_COMMENT_PRINT_ERROR_NO_NODE_ID));

        reportDocumentStatus(connection, document.getId(),
                SmartschoolPrintStatusEnum.ERROR,
                MSG_COMMENT_PRINT_ERROR_NO_NODE_ID, simulationMode);
    }

    /**
     * Processes a job request with an unknown requester.
     *
     * @param connection
     *            The {@link SmartschoolConnection}.
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
     * @throws SmartschoolException
     *             When Smartschool reported an error.
     */
    private static void processJobRequesterNotFound(
            final SmartschoolConnection connection,
            final boolean simulationMode, final Document document,
            final String userId, final boolean userExistSavaPage,
            final Boolean userExistPaperCut)
            throws SmartschoolException, SOAPException {

        final String msg;

        if (!userExistSavaPage && userExistPaperCut != null
                && !userExistPaperCut) {

            msg = String.format(MSG_COMMENT_PRINT_ERROR_UNKNOWN_USER_BOTH,
                    userId);

        } else if (!userExistSavaPage) {
            msg = String.format(MSG_COMMENT_PRINT_ERROR_UNKNOWN_USER_SAVAPAGE,
                    userId);

        } else if (userExistPaperCut != null && !userExistPaperCut) {
            msg = String.format(MSG_COMMENT_PRINT_ERROR_UNKNOWN_USER_PAPERCUT,
                    userId);
        } else {
            throw new SpException(
                    "no msg needed (SavaPage and PaperCut users found");
        }

        publishAdminMsg(PubLevelEnum.WARN,
                "Smartschool document [" + document.getName() + "]: " + msg);

        reportDocumentStatus(connection, document.getId(),
                SmartschoolPrintStatusEnum.ERROR, msg, simulationMode);
    }

    /**
     * Checks if user account exists in SavaPage and (optionally) PaperCut.
     *
     * @param monitor
     *            The {@link SmartschoolPrintMonitor} instance.
     * @param userName
     *            The unique user id.
     * @param lazyInsertUser
     *            {@code true} if user may be lazy inserted.
     * @param userSource
     *            The {@link IUserSource}.
     * @param userSourceGroup
     *            The user group of the The {@link IUserSource}.
     *
     * @return {@code true} if checked OK.
     */
    private static boolean collectCopyInfoUserCheck(
            final SmartschoolPrintMonitor monitor, final String userName,
            final boolean lazyInsertUser, final IUserSource userSource,
            final String userSourceGroup) {

        final PaperCutUser papercutUser;
        final Boolean userExistPaperCut;

        if (monitor.isIntegratedWithPaperCut()) {
            papercutUser = monitor.papercutServerProxy.getUser(userName);
            userExistPaperCut = papercutUser != null;
        } else {
            userExistPaperCut = null;
        }

        final User user = findActiveUser(userName, lazyInsertUser, userSource,
                userSourceGroup);

        final boolean userExistSavaPage = user != null;

        final String msg;

        if (!userExistSavaPage && userExistPaperCut != null
                && !userExistPaperCut) {
            msg = String.format(
                    "Account [%s] skipped: onbekend " + "in %s en PaperCut.",
                    userName, CommunityDictEnum.SAVAPAGE.getWord());
        } else if (!userExistSavaPage) {
            msg = String.format("Account [%s] skipped: onbekend in %s.",
                    userName, CommunityDictEnum.SAVAPAGE.getWord());
        } else if (userExistPaperCut != null && !userExistPaperCut) {
            msg = String.format(
                    "Account [%s] skipped: " + "onbekend in PaperCut.",
                    userName);
        } else {
            msg = null;
        }

        if (msg != null) {
            LOGGER.warn(msg);
        }

        return msg == null;
    }

    /**
     * Collects info about the number of copies to be printed.
     * <p>
     * If copies are charged to personal accounts these accounts (users) MUST
     * exist in SavaPage and (optionally) PaperCut: if these accounts do not
     * exist copies for these accounts are NOT counted.
     * </p>
     *
     * @param monitor
     *            The {@link SmartschoolPrintMonitor} instance.
     * @param document
     *            The Smartschool {@link Document} to print.
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
     *            The user group of the {@link IUserSource}.
     * @return The total number of copies of the document to be printed.
     */
    private static int collectCopyInfo(final SmartschoolPrintMonitor monitor,
            final Document document, final Map<String, Integer> klasCopies,
            final Map<String, Integer> userCopies,
            final Map<String, String> userKlas, final boolean lazyInsertUser,
            final IUserSource userSource, final String userSourceGroup) {

        final boolean chargeToStudents = monitor.processingConnection
                .getAccountConfig().isChargeToStudents();

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
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn(String.format(
                            "Account [%s] skipped: no role specified.",
                            userName));
                }
                continue;
            }

            final SmartschoolRoleEnum roleEnum =
                    SmartschoolRoleEnum.fromXmlValue(role);

            /*
             * INVARIANT: Role MUST be known.
             */
            if (roleEnum == null) {
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn(String.format(
                            "Account [%s] skipped: unknown role [%s].",
                            userName, role));
                }
                continue;
            }

            // Clazz
            final String clazz = account.getClazz();

            final boolean teacherAccount = StringUtils.isBlank(clazz);

            /*
             * INVARIANT: Student MUST have a class.
             */
            if (teacherAccount && roleEnum == SmartschoolRoleEnum.STUDENT) {
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn(String.format(
                            "Account [%s] skipped: no class specified.",
                            userName));
                }
                continue;
            }

            final boolean chargeCopiesToUser =
                    teacherAccount || chargeToStudents;

            /*
             * INVARIANT: If copies are charged to an individual user, this user
             * MUST exist in SavaPage and (optionally) PaperCut.
             */
            if (chargeCopiesToUser && !collectCopyInfoUserCheck(monitor,
                    userName, lazyInsertUser, userSource, userSourceGroup)) {
                continue;
            }

            /*
             * Calculate number of copies + extra.
             */
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
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn(String.format(
                            "Account [%s] skipped: no (extra) copies.",
                            userName));
                }
                continue;
            }

            nTotCopies += nCopies;

            /*
             * Collect number of copies.
             */
            Integer collectedSum;
            Integer collectedSumNew;

            // User copies
            if (chargeCopiesToUser) {

                collectedSum = userCopies.get(userName);

                if (collectedSum == null) {
                    collectedSumNew = Integer.valueOf(nCopies);
                } else {
                    collectedSumNew =
                            Integer.valueOf(collectedSum.intValue() + nCopies);
                }

                userCopies.put(userName, collectedSumNew);

                if (roleEnum == SmartschoolRoleEnum.STUDENT) {
                    userKlas.put(userName, clazz);
                }
            }

            // Class copies.
            if (!teacherAccount) {

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
     * Processes the downloaded Smartschool print Job file.
     *
     * @param monitor
     *            The {@link SmartschoolPrintMonitor} instance.
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
     * @throws SmartschoolException
     * @throws SOAPException
     * @throws DocContentToPdfException
     */
    private static void processJobFile(final SmartschoolPrintMonitor monitor,
            final Document document, final User user, final File downloadedFile,
            final DocContentPrintInInfo printInInfo,
            final AccountTrxInfoSet accountTrxInfoSet,
            final ExternalSupplierInfo externalSupplierInfo)
            throws IOException, DocContentPrintException, ShutdownException,
            ProxyPrintException, IppConnectException, SmartschoolException,
            SOAPException, DocContentToPdfException {

        /*
         * Proxy print if printer name is configured.
         */
        final boolean isProxyPrint =
                StringUtils.isNotBlank(monitor.processingConnection
                        .getAccountConfig().getProxyPrinterName());

        /*
         * Determine (initial) print status.
         */
        final SmartschoolPrintStatusEnum smartschoolPrintStatus;

        if (isProxyPrint) {

            if (monitor.isPaperCutPrintNonSecure) {
                smartschoolPrintStatus = SmartschoolPrintStatusEnum.PENDING_EXT;
            } else {
                // TODO: When NOT printing to a HOLD or JOBTICKET printer
                // PENDING is not right.
                smartschoolPrintStatus = SmartschoolPrintStatusEnum.PENDING;
            }

        } else {
            smartschoolPrintStatus = SmartschoolPrintStatusEnum.COMPLETED;
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
         * printing to the inbox this is crucial, since the document MUST be
         * present in the database BEFORE moving the PDF file.
         */
        printInInfo.setSupplierInfo(externalSupplierInfo);

        if (isProxyPrint) {

            if (monitor.isIntegratedWithPaperCut()) {

                if (monitor.isPaperCutPrintNonSecure) {

                    feedbackComment = MSG_COMMENT_PRINT_PENDING_PAPERCUT;

                    /*
                     * Set the AccountTrx's in the DocLog source
                     * (DocIn/PrintIn).
                     *
                     * Later on, when we monitor PaperCut and find out that the
                     * print succeeded, we will move/update the AccountTrx's to
                     * the DocLog (DocOut/PrintOut) target.
                     */
                    printInInfo.setAccountTrxInfoSet(accountTrxInfoSet);

                } else {
                    feedbackComment = MSG_COMMENT_PRINT_PENDING_SAVAPAGE;

                    /*
                     * Account Transactions are persisted in Job Ticket or HOLD
                     * Print, so we don't want them in the DocIn/PrintIn.
                     */
                    printInInfo.setAccountTrxInfoSet(null);
                }
            } else {
                feedbackComment = MSG_COMMENT_PRINT_QUEUED;
                printInInfo.setAccountTrxInfoSet(null);
            }

            /*
             * STEP 1: Logging the PrintIn as first action.
             */
            DOCLOG_SERVICE.logPrintIn(user, monitor.smartSchoolQueue,
                    DocLogProtocolEnum.SMARTSCHOOL, printInInfo);

            /*
             * STEP 2: Direct Proxy Print.
             */
            processJobProxyPrint(monitor, user, document, downloadedFile,
                    printInInfo, accountTrxInfoSet, externalSupplierInfo);

        } else {

            feedbackComment = MSG_COMMENT_PRINT_SAFEPAGES;

            /*
             * Ignore the AccountTrxInfoSet.
             */
            printInInfo.setAccountTrxInfoSet(null);

            /*
             * STEP 1: Logging the PrintIn as first action.
             */
            DOCLOG_SERVICE.logPrintIn(user, monitor.smartSchoolQueue,
                    DocLogProtocolEnum.SMARTSCHOOL, printInInfo);

            /*
             * STEP 2: Move PDF document to the SavaPage inbox.
             */
            final String homeDir =
                    USER_SERVICE.lazyUserHomeDir(user).getCanonicalPath();

            final Path source = FileSystems.getDefault()
                    .getPath(downloadedFile.getCanonicalPath());

            final Path target = FileSystems.getDefault().getPath(homeDir,
                    printInInfo.getUuidJob().toString() + "."
                            + DocContent.FILENAME_EXT_PDF);

            FileSystemHelper.doAtomicFileMove(source, target);
        }

        /*
         * Report status and comment back to SmartSchool.
         */
        reportDocumentStatus(monitor.processingConnection, document.getId(),
                smartschoolPrintStatus, feedbackComment,
                monitor.simulationMode);
    }

    /**
     * Translates a The Smartschool boolean value to a boolean type.
     *
     * @param value
     *            The Smartschool boolean value.
     * @return {@code true} when value represents {@link Boolean#TRUE}.
     */
    private static boolean asBoolean(final String value) {
        return value.equalsIgnoreCase("on");
    }

    /**
     * Adds a "pro-forma" {@link ProxyPrintJobChunk} object to the
     * {@link ProxyPrintDocReq}.
     *
     * @param printer
     *            The {@link Printer}.
     * @param printReq
     *            The {@link ProxyPrintDocReq}.
     * @param ippMediaSize
     *            The media size.
     * @param hasMediaSourceAuto
     *            {@code true} when printer has "auto"media source.
     * @param isManagedByPaperCut
     *            {@code true} when printer is managed by PaperCut.
     * @throws ProxyPrintException
     *             When printer has no media-source for media size.
     */
    private static void addProxyPrintJobChunk(final Printer printer,
            final ProxyPrintDocReq printReq,
            final IppMediaSizeEnum ippMediaSize,
            final boolean hasMediaSourceAuto, final boolean isManagedByPaperCut)
            throws ProxyPrintException {

        final String printerName = printReq.getPrinterName();

        final PrinterAttrLookup printerAttrLookup =
                new PrinterAttrLookup(printer);

        /*
         * INVARIANT: If printer has media sources defined, a media-source MUST
         * be available that matches the media size of the document.
         */
        final IppMediaSourceCostDto assignedMediaSource =
                printerAttrLookup.findAnyMediaSourceForMedia(ippMediaSize);

        if (assignedMediaSource == null) {
            throw new ProxyPrintException(localizedMsg("printer-media-not-foud",
                    printerName, ippMediaSize.getIppKeyword()));
        }

        final ProxyPrintJobChunk jobChunk = new ProxyPrintJobChunk();

        jobChunk.setAssignedMedia(ippMediaSize);

        /*
         * If the printer is managed by PaperCut, set "media-source" to "auto"
         * in the Print Request if printer supports it, otherwise set the
         * assigned media-source in the Job Chunk.
         */
        if (isManagedByPaperCut && hasMediaSourceAuto) {
            printReq.setMediaSourceOption(IppKeyword.MEDIA_SOURCE_AUTO);
            jobChunk.setAssignedMediaSource(null);
        } else {
            jobChunk.setAssignedMediaSource(assignedMediaSource);
        }

        /*
         * Chunk range begins at first page.
         */
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
     * <p>
     * Note: {@link #unicodeToAscii(String)} is applied to the document name,
     * since PaperCut converts the job name to 7-bit ascii. So we better do the
     * convert ourselves.
     * </p>
     *
     * @param connection
     *            The {@link SmartschoolConnection}.
     * @param document
     *            The {@link Document}
     * @return The encoded name.
     */
    private static String encodeProxyPrintJobName(
            final SmartschoolConnection connection, final Document document) {

        return PaperCutHelper.encodeProxyPrintJobName(
                connection.getAccountName(), document.getId(),
                document.getName());
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
     * Selects the proxy printer for the supplied data.
     *
     * @param connection
     *            The {@link SmartschoolConnection} instance.
     * @param supplierData
     *            The {@link SmartschoolPrintInData}.
     * @return The selected proxy printer name.
     */
    private static String selectProxyPrinter(
            final SmartschoolConnection connection,
            final SmartschoolPrintInData supplierData) {

        // The unique printer name for all jobs.
        final String printerName =
                connection.getAccountConfig().getProxyPrinterName();

        // The unique printer name for grayscale jobs.
        final String printerGrayscaleName =
                connection.getAccountConfig().getProxyPrinterGrayscaleName();

        //
        final String printerNameSelected;

        if (supplierData.getColor()
                || StringUtils.isBlank(printerGrayscaleName)) {

            final String printerDuplexName =
                    connection.getAccountConfig().getProxyPrinterDuplexName();

            if (supplierData.getDuplex()
                    && StringUtils.isNotBlank(printerDuplexName)) {
                printerNameSelected = printerDuplexName;
            } else {
                printerNameSelected = printerName;
            }

        } else {

            final String printerDuplexName = connection.getAccountConfig()
                    .getProxyPrinterGrayscaleDuplexName();

            if (supplierData.getDuplex()
                    && StringUtils.isNotBlank(printerDuplexName)) {
                printerNameSelected = printerDuplexName;
            } else {
                printerNameSelected = printerGrayscaleName;
            }
        }

        return printerNameSelected;
    }

    /**
     * Proxy Prints the Smartschool document.
     *
     * @param monitor
     *            The {@link SmartschoolPrintMonitor} instance.
     * @param user
     *            The locked user
     * @param document
     *            The {@link Document}.
     * @param downloadedFile
     *            The downloaded {@link File}.
     * @param printInInfo
     *            The {@link DocContentPrintInInfo}.
     * @param accountTrxInfoSet
     *            The {@link AccountTrxInfoSet}}.
     * @param externalSupplierInfo
     *            The {@link ExternalSupplierInfo}.
     * @throws ProxyPrintException
     *             When logical proxy print errors.
     * @throws IppConnectException
     *             When connection to CUPS fails.
     * @throws DocContentToPdfException
     *             When monochrome conversion failed.
     * @throws IOException
     *             When IO errors.
     */
    private static void processJobProxyPrint(
            final SmartschoolPrintMonitor monitor, final User user,
            final Document document, final File downloadedFile,
            final DocContentPrintInInfo printInInfo,
            final AccountTrxInfoSet accountTrxInfoSet,
            final ExternalSupplierInfo externalSupplierInfo)
            throws ProxyPrintException, IppConnectException,
            DocContentToPdfException, IOException {

        /*
         * Get Smartschool process info from the supplier data.
         */
        final SmartschoolPrintInData supplierData =
                (SmartschoolPrintInData) externalSupplierInfo.getData();

        /*
         * Select the proxy printer.
         */
        final String printerNameSelected =
                selectProxyPrinter(monitor.processingConnection, supplierData);

        /*
         * Read the selected Printer.
         */
        final PrinterDao printerDao =
                ServiceContext.getDaoContext().getPrinterDao();

        final Printer printer = printerDao.findByName(printerNameSelected);

        final boolean isJobTicketPrinter =
                PRINTER_SERVICE.isJobTicketPrinter(printer);

        /*
         * Determine Print Mode.
         */
        final PrintModeEnum printMode;

        if (monitor.isPaperCutPrintNonSecure) {
            printMode = PrintModeEnum.AUTO;
        } else if (isJobTicketPrinter
                || PRINTER_SERVICE.isHoldReleasePrinter(printer)) {
            printMode = PrintModeEnum.HOLD;
        } else {
            printMode = PrintModeEnum.AUTO;
        }

        /*
         * When this is an AUTO print and we are NOT integrated with PaperCut,
         * the AccountTrxInfoSet is RESET.
         */
        if (monitor.isIntegratedWithPaperCut()
                && printMode == PrintModeEnum.AUTO) {
            /*
             * Use an EMPTY AccountTrxInfoSet, so NO (user or shared) account
             * transactions are created at the DocOut/PrintOut target NOW. This
             * will be done when PaperCut reports that the document was
             * successfully printed (by moving the PrintIn account transactions
             * created in an earlier to the DocOut/PrintOut target).
             */
            printInInfo.setAccountTrxInfoSet(new AccountTrxInfoSet(0));
        } else {
            /*
             * Set the AccountTrx's in the DocOut/PrintOut target.
             */
            printInInfo.setAccountTrxInfoSet(accountTrxInfoSet);
        }

        /*
         * Create print request.
         */
        final ProxyPrintDocReq printReq = new ProxyPrintDocReq(printMode);

        printReq.setDocumentUuid(printInInfo.getUuidJob().toString());

        printReq.setAccountTrxInfoSet(printInInfo.getAccountTrxInfoSet());
        printReq.setComment(document.getComment());

        printReq.setNumberOfPages(
                printInInfo.getPageProps().getNumberOfPages());
        printReq.setPrinterName(printerNameSelected);

        printReq.setLocale(ServiceContext.getLocale());
        printReq.setIdUser(user.getId());

        printReq.setCollate(true);

        printReq.setRemoveGraphics(false);
        printReq.setClearScope(InboxSelectScopeEnum.NONE);

        final Map<String, String> ippOptions = PROXY_PRINT_SERVICE
                .getDefaultPrinterCostOptions(printerNameSelected);

        printReq.setOptionValues(ippOptions);

        // copies
        printReq.setNumberOfCopies(supplierData.getCopies());

        // media
        printReq.setMediaOption(supplierData.getMediaSize().getIppKeyword());

        final boolean isDuplexPrinter =
                PROXY_PRINT_SERVICE.isDuplexPrinter(printerNameSelected);

        // duplex
        if (isDuplexPrinter) {
            if (supplierData.getDuplex()) {
                printReq.setDuplexLongEdge();
            } else {
                printReq.setSinglex();
            }
        }

        // color
        final boolean isColorPrinter =
                PROXY_PRINT_SERVICE.isColorPrinter(printerNameSelected);

        if (isColorPrinter) {
            if (supplierData.getColor()) {
                printReq.setColor();
            } else {
                printReq.setGrayscale();
            }
        }

        /*
         * Pro-forma chunk.
         */
        final boolean isPrinterManagedByPaperCut =
                printMode == PrintModeEnum.AUTO
                        && monitor.isIntegratedWithPaperCut();

        addProxyPrintJobChunk(printer, printReq, supplierData.getMediaSize(),
                PROXY_PRINT_SERVICE.hasMediaSourceAuto(printerNameSelected),
                isPrinterManagedByPaperCut);

        /*
         * At this point we do NOT need the external data anymore and can
         * nullify it.
         *
         * Actually, we MUST nullify it, since persisted as JSON, the
         * ExternalSupplierData (which is an interface) can only be read with
         * extra effort (which we do not choose to make).
         *
         * Since we don't want to use it in the print request (i.e. store it in
         * the database) we nullify the external data.
         *
         * We still hold the Smartschool account, document id and status though
         * to persist in the database.
         */
        externalSupplierInfo.setData(null);

        if (monitor.isPaperCutPrintNonSecure) {
            printReq.setJobName(encodeProxyPrintJobName(
                    monitor.processingConnection, document));
        } else {
            printReq.setJobName(document.getName());
        }

        printReq.setSupplierInfo(externalSupplierInfo);

        if (LOGGER.isDebugEnabled()) {

            final StringBuilder msg = new StringBuilder();

            msg.append(printReq.getPrintMode()).append(" ProxyPrint [")
                    .append(printReq.getJobName()).append("]: ")
                    .append(printReq.getJobChunkInfo().getChunks().size())
                    .append(" chunk(s), ").append(printReq.getNumberOfPages())
                    .append(" page(s), ").append(printReq.getNumberOfCopies())
                    .append(" copies on ").append(printerNameSelected)
                    .append(" by [").append(user.getUserId()).append("]");

            msg.append(" Color printer [").append(isColorPrinter)
                    .append("] color [").append(supplierData.getColor())
                    .append("]");

            msg.append(" Duplex printer [").append(isDuplexPrinter)
                    .append("] duplex [").append(supplierData.getDuplex())
                    .append("]");

            LOGGER.debug(msg.toString());
        }

        /*
         * Client-side monochrome filtering?
         */
        final File fileToPrint;

        File downloadedFileConverted = null;

        if (printMode == PrintModeEnum.AUTO && isColorPrinter
                && printReq.isGrayscale()
                && PRINTER_SERVICE.isClientSideMonochrome(printer)) {

            final IFileConverter converter = new PdfToGrayscale();

            try {
                downloadedFileConverted = converter
                        .convert(DocContentTypeEnum.PDF, downloadedFile);
            } catch (UnavailableException e) {
                /*
                 * INVARIANT: Service MUST be available.
                 */
                throw new DocContentToPdfException(
                        "Monochrome conversion failed "
                                + "because service is unavailable.");
            }

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("[%s] converted to grayscale.",
                        printReq.getJobName()));
            }

            fileToPrint = downloadedFileConverted;

        } else {
            fileToPrint = downloadedFile;
        }

        /*
         * Proxy Print Transaction.
         */
        ReadWriteLockEnum.DATABASE_READONLY.setReadLock(true);

        final DaoContext daoContext = ServiceContext.getDaoContext();

        try {
            final UserDao userDao = ServiceContext.getDaoContext().getUserDao();

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("Start %s printing of [%s] ...",
                        printReq.getPrintMode().toString(),
                        printReq.getJobName()));
            }

            daoContext.beginTransaction();

            final User lockedUser = userDao.lock(user.getId());

            if (printReq.getPrintMode() == PrintModeEnum.HOLD) {

                final BigDecimal cost = ACCOUNTING_SERVICE.calcProxyPrintCost(
                        ServiceContext.getLocale(),
                        ServiceContext.getAppCurrencySymbol(), lockedUser,
                        printer, printReq.createProxyPrintCostParms(null),
                        printReq.getJobChunkInfo());

                printReq.setCost(cost);

                final PdfCreateInfo createInfo = new PdfCreateInfo(fileToPrint);

                if (isJobTicketPrinter) {
                    // TODO: 4 hours?
                    final int hours = 4;

                    JOBTICKET_SERVICE.proxyPrintPdf(lockedUser, printReq,
                            createInfo, printInInfo,
                            DateUtils.addHours(
                                    ServiceContext.getTransactionDate(),
                                    hours));
                } else {
                    OUTBOX_SERVICE.proxyPrintPdf(lockedUser, printReq,
                            createInfo, printInInfo);
                }
                /*
                 * This will refresh the User Web App with new status
                 * information.
                 */
                UserMsgIndicator.write(lockedUser.getUserId(), new Date(),
                        UserMsgIndicator.Msg.PRINT_OUT_HOLD, null);

            } else {

                PROXY_PRINT_SERVICE.proxyPrintPdf(lockedUser, printReq,
                        new PdfCreateInfo(fileToPrint));
            }

            daoContext.commit();

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("Finished %s printing of [%s]",
                        printReq.getPrintMode().toString(),
                        printReq.getJobName()));
            }

        } finally {

            daoContext.rollback();
            ReadWriteLockEnum.DATABASE_READONLY.setReadLock(false);

            if (downloadedFileConverted != null
                    && downloadedFileConverted.exists()) {
                downloadedFileConverted.delete();
            }
        }
    }

    /**
     * Waits for processing to finish.
     *
     * @param monitor
     *            The {@link SmartschoolPrintMonitor} instance.
     * @param millisInterval
     *            The sleep interval applied while {@link #isProcessing}.
     *
     * @throws InterruptedException
     *             When thread has been interrupted.
     */
    private static void waitForProcessing(final SmartschoolPrintMonitor monitor,
            final long millisInterval) throws InterruptedException {

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
     * Tests the Smartschool print connections and returns the total number of
     * jobs.
     *
     * @param nMessagesInbox
     *            Number of Inbox messages.
     * @throws SOAPException
     *             When SOAP connection fails.
     * @throws SmartschoolException
     *             When Smartschool returns errors.
     * @throws SmartschoolTooManyRequestsException
     *             When HTTP status 429 "Too Many Requests" occurred.
     */
    public static void testConnection(final MutableInt nMessagesInbox)
            throws SOAPException, SmartschoolException,
            SmartschoolTooManyRequestsException {

        final Map<String, SmartschoolConnection> connections =
                SMARTSCHOOL_SERVICE.createConnections();

        if (connections.isEmpty()) {
            throw new SmartschoolException(localizedMsg("no-connections"));
        }

        for (final SmartschoolConnection connection : connections.values()) {

            final Jobticket jobTicket =
                    SMARTSCHOOL_SERVICE.getJobticket(connection);

            nMessagesInbox.setValue(nMessagesInbox.getValue().intValue()
                    + jobTicket.getDocuments().getDocument().size());
        }
    }

    @Override
    public boolean onPaperCutPrintJobProcessingStep() {
        return !this.isShutdownRequested;
    }

    /**
     * Notifies that a print job has been processed and changes were committed
     * to the SavaPage database.
     *
     * @param printJobManager
     *            The {@link ThirdPartyEnum} that managed the print job. If
     *            {@code null}, SavaPage managed the print.
     * @param connection
     *            The {@link SmartschoolConnection}.
     * @param documentId
     *            The Smartschool document ID.
     * @param documentName
     *            The Smartschool document name.
     * @param printStatus
     *            The {@link ExternalSupplierStatusEnum}.
     * @param isDocumentTooLarge
     *            {@code true} when document was too large to be processed.
     * @param deniedReason
     *            The reason the job was denied (can be {@code null}.
     * @throws ExtSupplierException
     *             Error returned by external supplier.
     * @throws ExtSupplierConnectException
     *             Error connecting to external supplier.
     */
    private void onPrintJobProcessed(final ThirdPartyEnum printJobManager,
            final SmartschoolConnection connection, final String documentId,
            final String documentName,
            final ExternalSupplierStatusEnum printStatus,
            final boolean isDocumentTooLarge, final String deniedReason)
            throws ExtSupplierException, ExtSupplierConnectException {

        try {
            final String comment;

            switch (printStatus) {

            case CANCELLED:
                if (isDocumentTooLarge) {
                    comment = MSG_COMMENT_PRINT_DOCUMENT_TOO_LARGE;
                } else {
                    comment = MSG_COMMENT_PRINT_CANCELLED;
                }
                break;

            case COMPLETED:
                comment = MSG_COMMENT_PRINT_COMPLETED;
                break;

            case EXPIRED:
                comment = MSG_COMMENT_PRINT_EXPIRED;
                break;

            default:
                comment = null;
                break;
            }

            reportDocumentStatus(connection, documentId,
                    SmartschoolPrintStatusEnum.fromGenericStatus(printStatus),
                    comment, this.simulationMode);

        } catch (SmartschoolException e) {
            throw new ExtSupplierException(e.getMessage(), e);
        } catch (SOAPException e) {
            throw new ExtSupplierConnectException(e.getMessage(), e);
        }

        /*
         * Log and publish.
         */
        final PubLevelEnum pubLevel;
        final StringBuilder msg = new StringBuilder();

        if (printJobManager != null) {
            msg.append(printJobManager.getUiText()).append(" ");
        }
        msg.append("Print of Smartschool document [").append(documentName)
                .append("] ").append(printStatus.toString());

        if (printStatus == ExternalSupplierStatusEnum.COMPLETED) {
            pubLevel = PubLevelEnum.CLEAR;
        } else {
            pubLevel = PubLevelEnum.WARN;
            if (StringUtils.isNotBlank(deniedReason)) {
                msg.append(" because \"").append(deniedReason).append("\"");
            }
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(msg.toString());
        }

        publishAdminMsg(pubLevel, msg.toString());
    }

    @Override
    public void onPaperCutPrintJobProcessed(final DocLog docLog,
            final PaperCutPrinterUsageLog papercutLog,
            final ExternalSupplierStatusEnum printStatus,
            final boolean isDocumentTooLarge)
            throws ExtSupplierException, ExtSupplierConnectException {

        /*
         * Get Smartschool account name from the document name.
         */
        final StringBuilder msg = new StringBuilder();

        final String account =
                PaperCutHelper.getAccountFromEncodedProxyPrintJobName(
                        papercutLog.getDocumentName());

        /*
         * Be forgiving when account is not found or unknown.
         */
        if (account == null) {

            msg.append("No account found in DocLog supplier data for [")
                    .append(papercutLog.getDocumentName()).append("].");

            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn(msg.toString());
            }

            if (SmartschoolLogger.getLogger().isDebugEnabled()) {
                SmartschoolLogger.logDebug(msg.toString());
            }
            return;
        }

        final SmartschoolConnection connection = connectionMap.get(account);

        if (connection == null) {

            msg.append("No connection found for account [").append(account)
                    .append("] of [").append(papercutLog.getDocumentName())
                    .append("].");

            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn(msg.toString());
            }

            if (SmartschoolLogger.getLogger().isDebugEnabled()) {
                SmartschoolLogger.logDebug(msg.toString());
            }
            return;
        }

        onPrintJobProcessed(ThirdPartyEnum.PAPERCUT, connection,
                docLog.getExternalId(), papercutLog.getDocumentName(),
                printStatus, isDocumentTooLarge, papercutLog.getDeniedReason());
    }

    @Override
    public void onPaperCutPrintJobNotFound(final String docName,
            final long docAge) {

        final StringBuilder msg = new StringBuilder();

        msg.append("PaperCut print log of ")
                .append(DateUtil.formatDuration(docAge))
                .append(" old Smartschool document [").append(docName)
                .append("] not found.");

        LOGGER.error(msg.toString());

        publishAdminMsg(PubLevelEnum.ERROR, msg.toString());
    }

}
