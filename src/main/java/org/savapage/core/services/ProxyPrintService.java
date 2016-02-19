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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.print.attribute.standard.MediaSizeName;

import org.savapage.core.config.IConfigProp;
import org.savapage.core.dto.IppMediaCostDto;
import org.savapage.core.dto.IppMediaSourceCostDto;
import org.savapage.core.dto.ProxyPrinterCostDto;
import org.savapage.core.dto.ProxyPrinterDto;
import org.savapage.core.dto.ProxyPrinterMediaSourcesDto;
import org.savapage.core.imaging.EcoPrintPdfTask;
import org.savapage.core.imaging.EcoPrintPdfTaskPendingException;
import org.savapage.core.inbox.InboxInfoDto;
import org.savapage.core.ipp.IppSyntaxException;
import org.savapage.core.ipp.attribute.IppAttrGroup;
import org.savapage.core.ipp.client.IppConnectException;
import org.savapage.core.ipp.client.IppNotificationRecipient;
import org.savapage.core.ipp.operation.IppStatusCode;
import org.savapage.core.jpa.Device;
import org.savapage.core.jpa.Printer;
import org.savapage.core.jpa.User;
import org.savapage.core.jpa.UserAccount;
import org.savapage.core.jpa.UserGroup;
import org.savapage.core.json.JsonPrinterDetail;
import org.savapage.core.json.JsonPrinterList;
import org.savapage.core.json.rpc.AbstractJsonRpcMessage;
import org.savapage.core.json.rpc.AbstractJsonRpcMethodResponse;
import org.savapage.core.json.rpc.JsonRpcMethodError;
import org.savapage.core.json.rpc.JsonRpcMethodResult;
import org.savapage.core.json.rpc.impl.ParamsPrinterSnmp;
import org.savapage.core.outbox.OutboxInfoDto.OutboxJobDto;
import org.savapage.core.print.proxy.JsonProxyPrintJob;
import org.savapage.core.print.proxy.JsonProxyPrinter;
import org.savapage.core.print.proxy.JsonProxyPrinterOpt;
import org.savapage.core.print.proxy.JsonProxyPrinterOptChoice;
import org.savapage.core.print.proxy.ProxyPrintDocReq;
import org.savapage.core.print.proxy.ProxyPrintException;
import org.savapage.core.print.proxy.ProxyPrintInboxReq;
import org.savapage.core.print.proxy.ProxyPrintJobChunk;
import org.savapage.core.services.helpers.PageScalingEnum;
import org.savapage.core.services.helpers.PrinterAttrLookup;
import org.savapage.core.services.helpers.SyncPrintJobsResult;
import org.savapage.core.services.helpers.ThirdPartyEnum;
import org.savapage.core.snmp.SnmpConnectException;

/**
 * Service for Proxy Printing.
 *
 * @author Rijk Ravestein
 *
 */
public interface ProxyPrintService {

    /**
     *
     * @param printer
     * @return
     */
    List<IppMediaSourceCostDto> getProxyPrinterCostMediaSource(Printer printer);

    /**
     *
     * @return
     */
    AbstractJsonRpcMethodResponse setProxyPrinterCostMediaSources(
            Printer printer, ProxyPrinterMediaSourcesDto dto);

    /**
     *
     * @param printer
     * @return
     */
    List<IppMediaCostDto> getProxyPrinterCostMedia(Printer printer);

    /**
     *
     * @return
     */
    AbstractJsonRpcMethodResponse setProxyPrinterCostMedia(Printer printer,
            ProxyPrinterCostDto dto);

    /**
     * Gives the localized mnemonic for a {@link MediaSizeName}.
     *
     * @param mediaSizeName
     *            The {@link MediaSizeName}.
     * @return The mnemonic.
     */
    String localizeMnemonic(MediaSizeName mediaSizeName);

    /**
     *
     * @return
     */
    ProxyPrinterDto getProxyPrinterDto(Printer printer);

    /**
     *
     * @return
     */
    void setProxyPrinterProps(Printer printer, ProxyPrinterDto dto);

    /**
     * Gets the CUPS runtime version.
     *
     * @return {@code null} when not found (unknown).
     */
    String getCupsVersion();

    /**
     * Gets the CUPS API version.
     *
     * @return {@code null} when not found (unknown).
     */
    String getCupsApiVersion();

    /**
     * Gets the CUPS URL for a printer.
     *
     * @param printerName
     *            The CUPS printer name.
     * @return The URL.
     */
    URL getCupsPrinterUrl(String printerName);

    /**
     * Gets the CUPS Administration URL.
     *
     * @return The URL.
     */
    URL getCupsAdminUrl();

    /**
     *
     * @return {@code true} When connected to CUPS.
     */
    boolean isConnectedToCups();

    /**
     *
     * @return The {@link IppNotificationRecipient}.
     */
    IppNotificationRecipient notificationRecipient();

    /**
     *
     * @param requestingUser
     * @param subscriptionId
     * @param response
     *            The output response.
     * @return The IPP status code after sending the request.
     * @throws IppConnectException
     *             When an connection error occurs.
     */
    IppStatusCode getNotifications(String requestingUser, String subscriptionId,
            List<IppAttrGroup> response) throws IppConnectException;

    /**
     *
     * @param printerName
     * @param jobId
     * @return {@code null} when NOT found.
     * @throws IppConnectException
     *             When a connection error occurs.
     */
    JsonProxyPrintJob retrievePrintJob(String printerName, Integer jobId)
            throws IppConnectException;

    /**
     * Gets the JsonCupsPrinter from the printer cache.
     * <p>
     * {@code null} is returned when the printer is not present in CUPS and
     * therefore is no longer part of the cache.
     * </p>
     *
     * @param printerName
     *            The printer name.
     * @return {@code null} when the printer is not present in CUPS and not part
     *         of the cache.
     */
    JsonProxyPrinter getCachedPrinter(String printerName);

    /**
     * Gets a copy of the JsonPrinter from the printer cache.
     * <p>
     * <b>Note</b>: a copy is returned so the caller can manipulate the
     * {@link JsonPrinterDetail} without changing the proxy printer cache.
     * </p>
     *
     * @param printerName
     *            The printer name.
     * @return {@code null} when the printer is no longer part of the cache.
     */
    JsonPrinterDetail getPrinterDetailCopy(String printerName);

    /**
     * Gets a user copy of the JsonPrinter from the printer cache: the printer
     * options are filtered according to user settings and permissions.
     * <p>
     * <b>Note</b>: a copy is returned so the caller can manipulate the
     * {@link JsonPrinterDetail} without changing the proxy printer cache.
     * </p>
     *
     * @param locale
     *            The user {@link Locale}.
     * @param printerName
     *            The printer name.
     * @return {@code null} when the printer is no longer part of the cache.
     */
    JsonPrinterDetail getPrinterDetailUserCopy(Locale locale,
            String printerName);

    /**
     * Gets the IPP attributes of a printer.
     *
     * @param printerName
     *            The printer name.
     * @param printerUri
     *            The {@link URI} of the IPP printer.
     * @return A list of {@link IppAttrGroup} instances.
     * @throws IppConnectException
     */
    List<IppAttrGroup> getIppPrinterAttr(String printerName, URI printerUri)
            throws IppConnectException;

    /**
     * Localizes the texts in all printer options.
     *
     * @param locale
     *            The {@link Locale}.
     * @param printerDetail
     *            The {@link JsonPrinterDetail}.
     */
    void localize(Locale locale, JsonPrinterDetail printerDetail);

    /**
     * Localizes the texts in a printer options.
     *
     * @param locale
     *            The {@link Locale}.
     * @param option
     *            The {@link JsonProxyPrinterOpt}.
     */
    void localizePrinterOption(final Locale locale,
            final JsonProxyPrinterOpt option);

    /**
     * Localizes the texts in printer option choices.
     *
     * @param locale
     *            The {@link Locale}.
     * @param attrKeyword
     *            The IPP option keyword.
     * @param choices
     *            The list with {@link JsonProxyPrinterOptChoice} objects.
     */
    void localizePrinterOptChoices(Locale locale, String attrKeyword,
            List<JsonProxyPrinterOptChoice> choices);

    /**
     * Gets the valid printers for a user on a terminal (sorted on alias).
     * <ul>
     * <li>Terminal {@link Device} restriction and {@link UserGroup} Access
     * Control are applied.</li>
     * <li>Disabled and deleted printers, as well as printers that are not
     * (fully) configured, are not included.</li>
     * </ul>
     * <p>
     * Note: CUPS is checked for printers changes.
     * </p>
     *
     * @param terminal
     *            The {@link Device.DeviceTypeEnum#TERMINAL} definition of the
     *            requesting client. Is {@code null} when NO definition is
     *            available.
     * @param userName
     *            The unique name of the requesting user.
     * @return The sorted {@link JsonPrinterList}.
     * @throws IppConnectException
     * @throws IppSyntaxException
     */
    JsonPrinterList getUserPrinterList(Device terminal, String userName)
            throws IppConnectException, IppSyntaxException;

    /**
     * Initializes the service.
     * <p>
     * IMPORTANT: CUPS must be up and running, otherwise any communication with
     * CUPS, like {@link #refreshPrinterCache()} and
     * {@link #startSubscription(String)} fails.
     * </p>
     * <p>
     * When the host machine is starting up and the savapage server process is
     * launched we have to make sure CUPS is up-and-running. We can enforce this
     * by setting the Required-Start: $cups ... in the BEGIN INIT INFO header of
     * the 'app-server' startup script.
     * </p>
     * <p>
     * HOWEVER, in practice we cannot depend CUPS is up-and-running, so we opt
     * for a defensive strategy and lazy init of the printer cache and start
     * CUPS event subscription in {@link #updatePrinterCache()}.
     * </p>
     *
     */
    void init();

    /**
     * Closes the service.
     *
     * @throws IppConnectException
     * @throws IppSyntaxException
     */
    void exit() throws IppConnectException, IppSyntaxException;

    /**
     * Updates the cached JsonCupsPrinter with Database Printer Object.
     * <p>
     * NOTE: When the dbPrinter is not part of the cache the update is silently
     * ignored.
     * </p>
     *
     * @param dbPrinter
     *            The Database Printer Object.
     */
    void updateCachedPrinter(Printer dbPrinter);

    /**
     * Initializes the CUPS printer cache when it does not exist.
     *
     * @throws IppConnectException
     *             When a connection error occurs.
     * @throws IppSyntaxException
     *             When a syntax error.
     */
    void lazyInitPrinterCache() throws IppConnectException, IppSyntaxException;

    /**
     * Initializes the CUPS printer cache (clearing any existing one).
     *
     * @throws IppConnectException
     *             When a connection error occurs.
     * @throws IppSyntaxException
     *             When a syntax error.
     */
    void initPrinterCache() throws IppConnectException, IppSyntaxException;

    /**
     * Synchronizes (updates) the PrintOut jobs with the CUPS job state (if the
     * state changed). A match is made between printer, job-id and
     * creation-time. If there is no match, i.e. when creation times differs, no
     * update is done.
     *
     * @return The {@link SyncPrintJobsResult}.
     * @throws IppConnectException
     *             When a connection error occurs.
     */
    SyncPrintJobsResult syncPrintJobs() throws IppConnectException;

    /**
     * Gets the {@link Printer} object while validating {@link User} access.
     *
     * @param user
     *            The {@link User}.
     * @param printerName
     *            The unique printer name.
     * @param refDate
     *            The reference {@link Date}.
     * @return The {@link Printer}.
     * @throws ProxyPrintException
     *             When access is denied.
     */
    Printer getValidateProxyPrinterAccess(User user, String printerName,
            final Date refDate) throws ProxyPrintException;

    /**
     * Checks the printer cache and collects the default printer options, needed
     * for cost calculation.
     * <p>
     * Note: This method might be called in a situation where user did not
     * select the target printer in the WebApp. Therefore, by checking the CUPS
     * printer cache, it gets lazy initialized upon first use.
     * </p>
     *
     * @param printerName
     *            The printer name.
     * @return A key-value {@link Map} with key (IPP attribute name).
     *
     * @throws ProxyPrintException
     *             When no printer details available.
     */
    Map<String, String> getDefaultPrinterCostOptions(String printerName)
            throws ProxyPrintException;

    /**
     * Sends a PDF file to the CUPS Printer, and updates {@link User},
     * {@link Printer} and global {@link IConfigProp} statistics.
     * <p>
     * Note: This is a straight proxy print: {@link InboxInfoDto} is not
     * consulted or updated. Invariants ARE checked.
     * </p>
     *
     * @param lockedUser
     *            The requesting {@link User}, which should be locked.
     * @param request
     *            The {@link ProxyPrintDocReq}.
     * @param pdfFile
     *            The PDF file to send to the printer.
     * @throws IppConnectException
     *             When CUPS connection is broken.
     * @throws ProxyPrintException
     *             When a invariant is violated.
     */
    void proxyPrintPdf(User lockedUser, ProxyPrintDocReq request, File pdfFile)
            throws IppConnectException, ProxyPrintException;

    /**
     * Prints one (1) copy of each the vanilla inbox job of the {@link User}
     * identified by card number, to the proxy printer associated with the card
     * reader. Printer defaults are used. The inbox is cleared after the print
     * job is successfully put on the print queue.
     * <p>
     * Note: All invariants for {@link Printer}, {@link User},
     * {@link UserAccount}, etc. are checked. When a invariant is violated a
     * {@link ProxyPrintException} is thrown.
     * </p>
     *
     * @param reader
     *            The card reader {@link Device}.
     * @param cardNumber
     *            The RFID card number identifying the user.
     * @return The number of printed pages. Zero ({@code 0} is returned when no
     *         inbox jobs were found for fast proxy printing.
     * @throws ProxyPrintException
     *             When a invariant is violated.
     */
    int proxyPrintInboxFast(Device reader, String cardNumber)
            throws ProxyPrintException;

    /**
     * Prints the outbox jobs of the {@link User} identified by card number, for
     * the proxy printer associated with the card reader. The outbox jobs are
     * cleared after the print job is successfully put on the print queue.
     * <p>
     * Note: All invariants for {@link Printer}, {@link User},
     * {@link UserAccount}, etc. are checked. When a invariant is violated a
     * {@link ProxyPrintException} is thrown.
     * </p>
     *
     * @param reader
     *            The card reader {@link Device}.
     * @param cardNumber
     *            The RFID card number identifying the user.
     * @return The number of printed pages.
     * @throws ProxyPrintException
     *             When a invariant is violated.
     */
    int proxyPrintOutbox(Device reader, String cardNumber)
            throws ProxyPrintException;

    /**
     * Print a Job Ticket.
     *
     * @param lockedUser
     *            The {@link User} who owns the Job Ticket, which should be
     *            locked.
     * @param job
     *            The {@link OutboxJobDto} Job Ticket.
     * @param pdfFileToPrint
     *            The PDF file to print.
     * @return The number of printed pages.
     * @throws IOException
     *             When IO error.
     * @throws IppConnectException
     *             When connection to CUPS fails.
     */
    int proxyPrintJobTicket(User lockedUser, OutboxJobDto job,
            File pdfFileToPrint) throws IOException, IppConnectException;

    /**
     * Sends Print Job to the CUPS Printer, and updates {@link User},
     * {@link Printer} and global {@link IConfigProp} statistics.
     * <p>
     * Note: This is a straight proxy print. {@link InboxInfoDto} is not
     * consulted or updated, and invariants are NOT checked.
     * </p>
     *
     * @param lockedUser
     *            The requesting {@link User}, which should be locked.
     * @param request
     *            The {@link ProxyPrintInboxReq}.
     * @throws IppConnectException
     *             When CUPS connection is broken.
     * @throws EcoPrintPdfTaskPendingException
     *             When {@link EcoPrintPdfTask} objects needed for this PDF are
     *             pending.
     */
    void proxyPrintInbox(User lockedUser, ProxyPrintInboxReq request)
            throws IppConnectException, EcoPrintPdfTaskPendingException;

    /**
     * Creates a CUPS event subscription. This is an idempotent operation: when
     * the subscription already exists it is renewed.
     *
     * @param requestingUserName
     *            The requesting user. If {@code null} the current CUPS user is
     *            used.
     *
     * @throws IppConnectException
     * @throws IppSyntaxException
     */
    void startSubscription(String requestingUserName)
            throws IppConnectException, IppSyntaxException;

    /**
     * Cancels a CUPS event subscription. This is an idempotent operation: when
     * the subscription does not exists, the cancel is not executed.
     *
     * @param requestingUserName
     *            The requesting user. If {@code null} the current CUPS user is
     *            used.
     * @throws IppConnectException
     * @throws IppSyntaxException
     */
    void stopSubscription(String requestingUserName)
            throws IppConnectException, IppSyntaxException;

    /**
     * Checks if the printer URI resides on local CUPS.
     *
     * @param uriPrinter
     *            The printer {@link URI}.
     * @return {@code true} if the printer URI points to local CUPS,
     *         {@code false} when printer resides on a remote CUPS.
     */
    boolean isLocalPrinter(URI uriPrinter);

    /**
     * Checks if the CUPS printer resides on local CUPS.
     *
     * @param cupsPrinterName
     *            The printer CUPS name.
     * @return {@code true} if the printer reside on local CUPS, {@code false}
     *         when printer resides on a remote CUPS, {@code null} when the
     *         printer is unknown.
     */
    Boolean isLocalPrinter(String cupsPrinterName);

    /**
     * Checks if the CUPS printer is managed by an external (third-party)
     * application.
     *
     * @param cupsPrinterName
     *            The printer CUPS name.
     * @return {@code null} when not managed by external party.
     */
    ThirdPartyEnum getExtPrinterManager(String cupsPrinterName);

    /**
     * Gets the media option choices for a printer.
     *
     * @param printerName
     *            The unique name of the printer.
     * @return A list with {@link JsonProxyPrinterOptChoice} objects.
     */
    List<JsonProxyPrinterOptChoice> getMediaChoices(String printerName);

    /**
     * Gets the media option choices for a printer with localized texts.
     *
     * @param printerName
     *            The unique name of the printer.
     * @param locale
     *            The {@link Locale}.
     * @return A list with {@link JsonProxyPrinterOptChoice} objects.
     */
    List<JsonProxyPrinterOptChoice> getMediaChoices(String printerName,
            Locale locale);

    /**
     * Checks if a {@link Printer} is fully configured to be used.
     *
     * @param cupsPrinter
     *            The {@link JsonProxyPrinter} CUPS definition.
     * @param lookup
     *            The corresponding {@link PrinterAttrLookup} with the Printer
     *            configuration.
     * @return {@code true} when printer can be used.
     */
    boolean isPrinterConfigured(JsonProxyPrinter cupsPrinter,
            PrinterAttrLookup lookup);

    /**
     * Checks if printer is a color printer.
     *
     * @param printerName
     *            The unique name of the printer.
     * @return {@code true} if color printer.
     */
    boolean isColorPrinter(String printerName);

    /**
     * Checks if printer is a duplex printer.
     *
     * @param printerName
     *            The unique name of the printer.
     * @return {@code true} if duplex printer.
     */
    boolean isDuplexPrinter(String printerName);

    /**
     * Checks if printer supports a 'manual' media-source.
     *
     * @param printerName
     *            The unique name of the printer.
     * @return {@code true} if manual media-source is supported.
     */
    boolean hasMediaSourceManual(final String printerName);

    /**
     * Checks if printer supports 'auto' media-source.
     *
     * @param printerName
     *            The unique name of the printer.
     * @return {@code true} if 'auto' media-source is supported.
     */
    boolean hasMediaSourceAuto(final String printerName);

    /**
     * Checks if the CUPS printer details have been successfully retrieved.
     * <p>
     * CUPS details are not retrieved for a remote printer when remote CUPS
     * cannot be accessed.
     * </p>
     *
     * @param printerName
     *            The unique name of the printer.
     * @return {@code true} if not retrieved.
     */
    boolean isCupsPrinterDetails(String printerName);

    /**
     * Flattens the printer options from groups and subgroups into one (1)
     * lookup.
     *
     * @param printerName
     *            The unique name of the printer.
     * @return The lookup {@link Map}.
     */
    Map<String, JsonProxyPrinterOpt> getOptionsLookup(String printerName);

    /**
     * Chunks the {@link ProxyPrintInboxReq} in separate print jobs per
     * media-source or per vanilla inbox job.
     * <p>
     * As a result the original request parameters "media", "media-source" and
     * "fit-to-page" are set or corrected, and
     * {@link ProxyPrintInboxReq#getJobChunkInfo()} will have at least one (1)
     * {@link ProxyPrintJobChunk}.
     * </p>
     *
     * @param lockedUser
     *            The requesting {@link User}, which should be locked.
     * @param request
     *            The {@link ProxyPrintInboxReq} to be chunked.
     * @param pageScaling
     *            The preferred {@link PageScalingEnum}.
     * @param chunkVanillaJobs
     *            When {@code true} a chunk is created for each job (of a
     *            vanilla inbox)
     * @param iVanillaJob
     *            The zero-based ordinal of the single vanilla job to print. If
     *            {@code null}, all vanilla jobs are printed.
     *
     * @throws ProxyPrintException
     *             When proxy printer is not fully configured to support this
     *             request, or when vanilla job chunking is requested and the
     *             inbox is not vanilla.
     */
    void chunkProxyPrintRequest(User lockedUser, ProxyPrintInboxReq request,
            PageScalingEnum pageScaling, boolean chunkVanillaJobs,
            Integer iVanillaJob) throws ProxyPrintException;

    /**
     * Reads SNMP printer info.
     *
     * @param params
     *            The {@link ParamsPrinterSnmp} name.
     * @return The JSON-RPC Return message: {@link JsonRpcMethodResult} or an
     *         {@link JsonRpcMethodError} instance.
     * @throws SnmpConnectException
     *             When SNMP communication fails.
     */
    AbstractJsonRpcMessage readSnmp(ParamsPrinterSnmp params)
            throws SnmpConnectException;

}
