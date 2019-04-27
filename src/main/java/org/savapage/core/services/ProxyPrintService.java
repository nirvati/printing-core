/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2019 Datraverse B.V.
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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.print.attribute.standard.MediaSizeName;

import org.apache.commons.lang3.tuple.Pair;
import org.savapage.core.config.IConfigProp;
import org.savapage.core.dao.enums.ACLRoleEnum;
import org.savapage.core.dao.enums.PrintModeEnum;
import org.savapage.core.doc.store.DocStoreException;
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
import org.savapage.core.ipp.attribute.IppDictJobTemplateAttr;
import org.savapage.core.ipp.client.IppConnectException;
import org.savapage.core.ipp.client.IppNotificationRecipient;
import org.savapage.core.ipp.helpers.IppOptionMap;
import org.savapage.core.ipp.operation.IppStatusCode;
import org.savapage.core.jpa.CostChange;
import org.savapage.core.jpa.Device;
import org.savapage.core.jpa.DocLog;
import org.savapage.core.jpa.IppQueue;
import org.savapage.core.jpa.PrintIn;
import org.savapage.core.jpa.PrintOut;
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
import org.savapage.core.pdf.PdfCreateInfo;
import org.savapage.core.print.proxy.AbstractProxyPrintReq;
import org.savapage.core.print.proxy.JsonProxyPrintJob;
import org.savapage.core.print.proxy.JsonProxyPrinter;
import org.savapage.core.print.proxy.JsonProxyPrinterOpt;
import org.savapage.core.print.proxy.JsonProxyPrinterOptChoice;
import org.savapage.core.print.proxy.ProxyPrintDocReq;
import org.savapage.core.print.proxy.ProxyPrintException;
import org.savapage.core.print.proxy.ProxyPrintInboxReq;
import org.savapage.core.print.proxy.ProxyPrintJobChunk;
import org.savapage.core.print.proxy.ProxyPrinterOptGroupEnum;
import org.savapage.core.services.helpers.DocContentPrintInInfo;
import org.savapage.core.services.helpers.InboxSelectScopeEnum;
import org.savapage.core.services.helpers.PrinterAccessInfo;
import org.savapage.core.services.helpers.PrinterAttrLookup;
import org.savapage.core.services.helpers.ProxyPrintOutboxResult;
import org.savapage.core.services.helpers.SnmpPrinterQueryDto;
import org.savapage.core.services.helpers.ThirdPartyEnum;
import org.savapage.core.snmp.SnmpConnectException;
import org.savapage.ext.papercut.PaperCutException;

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
     * Gets the CUPS Web Interface URL for a printer.
     *
     * @param printerName
     *            The CUPS printer name.
     * @return The URL.
     */
    URL getCupsPrinterUrl(String printerName);

    /**
     * Gets the CUPS Web Interface Administration URL.
     *
     * @return The URL.
     */
    URL getCupsAdminUrl();

    /**
     * @return The CUPS time (seconds from epoch) of "now" (this very moment).
     */
    int getCupsSystemTime();

    /**
     *
     * @param cupsTime
     *            The CUPS time (seconds from epoch).
     * @return The CUPS date.
     */
    Date getCupsDate(Integer cupsTime);

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
     * Checks if PPD is present in CUPS for printer.
     *
     * @param printerURI
     *            Printer URI.
     * @return {@code true} when PPD is present.
     * @throws IppConnectException
     *             When an connection error occurs.
     */
    boolean isCupsPpdPresent(URI printerURI) throws IppConnectException;

    /**
     * Gets the CUPS printer PPD URL for download.
     *
     * @param printerName
     *            CUPS printer name.
     * @return The URL.
     */
    URL getCupsPpdUrl(String printerName);

    /**
     * Retrieves data for a list of print jobs ids for a printer.
     *
     * @param printerName
     *            The identifying name of the printer.
     * @param jobIds
     *            Job id set.
     * @return A list of print job objects.
     * @throws IppConnectException
     *             When a connection error occurs.
     */
    List<JsonProxyPrintJob> retrievePrintJobs(String printerName,
            Set<Integer> jobIds) throws IppConnectException;

    /**
     *
     * @param printerName
     *            The printer name.
     * @param jobId
     *            The job id.
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
     * Gets the host part of the CUPS printer device URI.
     *
     * @param printerName
     *            The printer name.
     * @return The host part of the URI, or {@code null} when inapplicable.
     */
    String getCachedPrinterHost(String printerName);

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
     * @param isExtended
     *            {@code true} if this is an extended copy.
     * @return {@code null} when the printer is no longer part of the cache.
     */
    JsonPrinterDetail getPrinterDetailUserCopy(Locale locale,
            String printerName, boolean isExtended);

    /**
     * Gets a user copy of IPP option for a printer from the printer cache.
     *
     * @param printerName
     *            The unique name of the printer.
     * @param ippKeyword
     *            The IPP keyword. E.g.
     *            {@link IppDictJobTemplateAttr#ATTR_OUTPUT_BIN}.
     * @param locale
     *            The {@link Locale} for the UI texts.
     * @return The option choices, or {@code null} when no choices found.
     */
    JsonProxyPrinterOpt getPrinterOptUserCopy(String printerName,
            String ippKeyword, Locale locale);

    /**
     * Gets the IPP attributes of a printer.
     *
     * @param printerName
     *            The printer name.
     * @param printerUri
     *            The {@link URI} of the IPP printer.
     * @return A list of {@link IppAttrGroup} instances.
     * @throws IppConnectException
     *             When a connection error occurs.
     */
    List<IppAttrGroup> getIppPrinterAttr(String printerName, URI printerUri)
            throws IppConnectException;

    /**
     * Gets the printer option UI group of an IPP keyword.
     *
     * @param keywordIpp
     *            The IPP keyword.
     * @return {@code null} when printer option is not shown in UI.
     */
    ProxyPrinterOptGroupEnum getUiOptGroup(String keywordIpp);

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
    void localizePrinterOpt(Locale locale, JsonProxyPrinterOpt option);

    /**
     * Localizes an IPP option keyword.
     *
     * @param locale
     *            The {@link Locale}.
     * @param attrKeyword
     *            The IPP option keyword.
     * @return the localized UI keyword.
     */
    String localizePrinterOpt(final Locale locale, final String attrKeyword);

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
     * Localizes the text in a printer option choice.
     *
     * @param locale
     *            The {@link Locale}.
     * @param attrKeyword
     *            The IPP option keyword.
     * @param choice
     *            The {@link JsonProxyPrinterOptChoice} object.
     */
    void localizePrinterOptChoice(Locale locale, String attrKeyword,
            JsonProxyPrinterOptChoice choice);

    /**
     * Localizes the text of a printer option value.
     *
     * @param locale
     *            The {@link Locale}.
     * @param attrKeyword
     *            The IPP option keyword.
     * @param value
     *            The keyword value.
     * @return The localized value.
     */
    String localizePrinterOptValue(Locale locale, String attrKeyword,
            String value);

    /**
     * Composes a localized Job Ticket UI text from a selected combination of
     * IPP options values.
     *
     * @param locale
     *            The {@link Locale}.
     * @param ippOptionKeys
     *            The IPP options keys to the option map
     * @param optionMap
     *            The {@link IppOptionMap} with IPP key/values.
     * @return {@code null} when no options keys found in the map.
     */
    String getJobTicketOptionsUiText(Locale locale, String[] ippOptionKeys,
            IppOptionMap optionMap);

    /**
     * Composes a list with localized Job Ticket Custom Extension Option
     * key/value UI texts from a map of IPP options values.
     *
     * @param locale
     *            The {@link Locale}.
     * @param optionMap
     *            The {@link Map} with IPP key/values.
     * @return {@code null} when no options keys found in the map.
     */
    List<Pair<String, String>> getJobTicketOptionsExtUiText(Locale locale,
            Map<String, String> optionMap);

    /**
     * Same as {@link #getJobTicketOptionsExtUiText(Locale, Map)}, but returning
     * HTML representation.
     *
     * @param locale
     *            The {@link Locale}.
     * @param optionMap
     *            The {@link Map} with IPP key/values.
     * @return {@code null} when no options keys found in the map.
     */
    String getJobTicketOptionsExtHtml(Locale locale,
            Map<String, String> optionMap);

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
     *             When a connection error occurs.
     * @throws IppSyntaxException
     *             When a syntax error.
     */
    JsonPrinterList getUserPrinterList(Device terminal, String userName)
            throws IppConnectException, IppSyntaxException;

    /**
     * Gets printer access info for a user at a terminal.
     *
     * @param terminal
     *            The {@link Device.DeviceTypeEnum#TERMINAL} definition of the
     *            requesting client. Is {@code null} when NO definition is
     *            available.
     * @param userName
     *            The unique name of the requesting user.
     * @return The {@link PrinterAccessInfo}.
     * @throws IppConnectException
     *             When a connection error occurs.
     * @throws IppSyntaxException
     *             When a syntax error.
     */
    PrinterAccessInfo getUserPrinterAccessInfo(Device terminal, String userName)
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
     * for a defensive strategy and lazy init the printer cache and lazy start
     * the CUPS event subscription.
     * </p>
     *
     */
    void init();

    /**
     * Closes the service.
     *
     * @throws IppConnectException
     *             When a connection error occurs.
     * @throws IppSyntaxException
     *             When a syntax error.
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
     * <p>
     * <b>Important</b>: This method performs a commit, and re-opens any
     * transaction this was pending at the start of this method.
     * </p>
     *
     * @throws IppConnectException
     *             When a connection error occurs.
     * @throws IppSyntaxException
     *             When a syntax error.
     */
    void lazyInitPrinterCache() throws IppConnectException, IppSyntaxException;

    /**
     * Initializes the CUPS printer cache (clearing any existing one).
     * <p>
     * <b>Important</b>: This method performs a commit, and re-opens any
     * transaction this was pending at the start of this method.
     * </p>
     *
     * @throws IppConnectException
     *             When a connection error occurs.
     * @throws IppSyntaxException
     *             When a syntax error.
     */
    void initPrinterCache() throws IppConnectException, IppSyntaxException;

    /**
     * Peeks if the CUPS printer cache is available.
     * <p>
     * Note: The cache is lazy initialized upon first use.
     * </p>
     *
     * @return {@code true} when available.
     */
    boolean isPrinterCacheAvailable();

    /**
     * @return {@code true} when at least one (1) Job Ticket Printer is present.
     */
    boolean isJobTicketPrinterPresent();

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
            Date refDate) throws ProxyPrintException;

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
     * Sends a PDF file to the CUPS Printer, updates {@link User},
     * {@link Printer} and global {@link IConfigProp} statistics, and optionally
     * archives/journals the PDF file and print request.
     * <p>
     * Note: This is a straight proxy print: {@link InboxInfoDto} is not
     * consulted or updated. Invariants ARE checked.
     * </p>
     *
     * @param lockedUser
     *            The requesting {@link User}, which should be locked.
     * @param request
     *            The {@link ProxyPrintDocReq}.
     * @param createInfo
     *            The {@link PdfCreateInfo} with the PDF file to send to the
     *            printer.
     * @throws IppConnectException
     *             When CUPS connection is broken.
     * @throws ProxyPrintException
     *             When a invariant is violated.
     * @throws DocStoreException
     *             When print archiving errors.
     */
    void proxyPrintPdf(User lockedUser, ProxyPrintDocReq request,
            PdfCreateInfo createInfo)
            throws IppConnectException, ProxyPrintException, DocStoreException;

    /**
     * Sends a PDF file to a CUPS printer, and that is it. No database or
     * document store action is executed.
     *
     * @param request
     *            The {@link AbstractProxyPrintReq}.
     * @param jsonPrinter
     *            The printer object.
     * @param user
     *            The requesting user.
     * @param createInfo
     *            The {@link PdfCreateInfo} with the file to print.
     * @return The print job data.
     * @throws IppConnectException
     *             When IPP connection error.
     */
    JsonProxyPrintJob sendPdfToPrinter(AbstractProxyPrintReq request,
            JsonProxyPrinter jsonPrinter, String user, PdfCreateInfo createInfo)
            throws IppConnectException;

    /**
     * Prints one (1) copy of each job in a vanilla inbox job of the
     * {@link User} identified by card number, to the proxy printer associated
     * with the card reader. Printer defaults are used.
     *
     * <p>
     * <i>When the inbox is not vanilla, because user edited the SafePages, the
     * inbox is cleared, and zero (0) is returned.</i>
     * </p>
     *
     * <ul>
     * <li>Print-in jobs that are expired for Fast Proxy Printing are deleted
     * first.</li>
     * <li>The inbox is cleared after the print jobs are successfully put on the
     * print queue.</li>
     * </ul>
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
     * Routes a {@link PrintIn} to a proxy printer.
     *
     * @param user
     *            The requesting user.
     * @param queue
     *            The print-in queue.
     * @param printer
     *            The target {@link Printer}.
     * @param printInInfo
     *            {@link PrintIn} information.
     * @param pdfFile
     *            The PDF to print.
     * @throws ProxyPrintException
     *             If printing error.
     */
    void proxyPrintIppRouting(User user, IppQueue queue, Printer printer,
            DocContentPrintInInfo printInInfo, File pdfFile)
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
     * @return The number {@link ProxyPrintOutboxResult}.
     * @throws ProxyPrintException
     *             When a invariant is violated.
     */
    ProxyPrintOutboxResult proxyPrintOutbox(Device reader, String cardNumber)
            throws ProxyPrintException;

    /**
     * Prints a outbox job of a {@link User}.
     *
     * @param userDbId
     *            The primary database key of the {@link User}.
     * @param job
     *            The job.
     * @return The number {@link ProxyPrintOutboxResult}.
     * @throws ProxyPrintException
     *             When a invariant is violated.
     */
    ProxyPrintOutboxResult proxyPrintOutbox(Long userDbId, OutboxJobDto job)
            throws ProxyPrintException;

    /**
     * Prints a Job Ticket.
     *
     * @param operator
     *            The {@link User#getUserId()} with
     *            {@link ACLRoleEnum#JOB_TICKET_OPERATOR}.
     * @param lockedUser
     *            The {@link User} who owns the Job Ticket, which should be
     *            locked.
     * @param job
     *            The {@link OutboxJobDto} Job Ticket.
     * @param pdfFileToPrint
     *            The PDF file to print.
     * @param extPrinterManager
     *            The {@link ThirdPartyEnum} external print manager:
     *            {@code null} when native SavaPage.
     * @return The committed {@link DocLog} instance related to the
     *         {@link PrintOut}.
     * @throws IOException
     *             When IO error.
     * @throws IppConnectException
     *             When connection to CUPS fails.
     */
    DocLog proxyPrintJobTicket(String operator, User lockedUser,
            OutboxJobDto job, File pdfFileToPrint,
            ThirdPartyEnum extPrinterManager)
            throws IOException, IppConnectException;

    /**
     * Re-sends PDF job file (and Job Sheet PDF) to CUPS printer, and that is
     * it. No database action is executed.
     *
     * @param request
     *            The {@link AbstractProxyPrintReq}.
     * @param job
     *            The {@link OutboxJobDto}.
     * @param jsonPrinter
     *            The printer object.
     * @param user
     *            The requesting user.
     * @param createInfo
     *            The {@link PdfCreateInfo} with the file to print.
     * @return The print job data.
     * @throws IppConnectException
     *             When IPP connection error.
     */
    JsonProxyPrintJob proxyPrintJobTicketResend(AbstractProxyPrintReq request,
            OutboxJobDto job, JsonProxyPrinter jsonPrinter, String user,
            PdfCreateInfo createInfo) throws IppConnectException;

    /**
     * Settles a Job Ticket without printing it.
     *
     * @param operator
     *            The {@link User#getUserId()} with
     *            {@link ACLRoleEnum#JOB_TICKET_OPERATOR}.
     * @param lockedUser
     *            The {@link User} who owns the Job Ticket, which should be
     *            locked.
     * @param job
     *            The {@link OutboxJobDto} Job Ticket.
     * @param pdfFileNotToPrint
     *            The PDF file <b>not</b> to print.
     * @param extPrinterManager
     *            The {@link ThirdPartyEnum} external print manager:
     *            {@code null} when native SavaPage.
     * @return The number of printed pages.
     * @throws IOException
     *             When IO error.
     */
    int settleJobTicket(String operator, User lockedUser, OutboxJobDto job,
            File pdfFileNotToPrint, ThirdPartyEnum extPrinterManager)
            throws IOException;

    /**
     * Refunds a proxy print in {@link ThirdPartyEnum#PAPERCUT}.
     *
     * @param costChange
     *            The {@link CostChange} of the refund.
     * @throws PaperCutException
     *             When logical PaperCut error.
     */
    void refundProxyPrintPaperCut(CostChange costChange)
            throws PaperCutException;

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
     *             When a connection error occurs.
     * @throws IppSyntaxException
     *             When a syntax error.
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
     *             When a connection error occurs.
     * @throws IppSyntaxException
     *             When a syntax error.
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
    boolean hasMediaSourceManual(String printerName);

    /**
     * Checks if printer supports 'auto' media-source.
     *
     * @param printerName
     *            The unique name of the printer.
     * @return {@code true} if 'auto' media-source is supported.
     */
    boolean hasMediaSourceAuto(String printerName);

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
            boolean chunkVanillaJobs, Integer iVanillaJob)
            throws ProxyPrintException;

    /**
     * Clears the user's inbox depending on the print request and the
     * {@link InboxSelectScopeEnum} in it.
     *
     * @param lockedUser
     *            The requesting {@link User}, which should be locked.
     * @param request
     *            The {@link ProxyPrintInboxReq}.
     * @return The number of cleared object (pages or documents, depending on
     *         {@link ProxyPrintInboxReq#getClearScope()}).
     */
    int clearInbox(User lockedUser, ProxyPrintInboxReq request);

    /**
     * Gets list of SNMP printer queries.
     *
     * @return The list of queries (can be empty).
     */
    List<SnmpPrinterQueryDto> getSnmpQueries();

    /**
     * Gets SNMP query for a particular printer.
     *
     * @param printerID
     *            The primary database key of a {@link Printer}.
     * @return The query, or {@code null} when not found or applicable.
     */
    SnmpPrinterQueryDto getSnmpQuery(Long printerID);

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

    /**
     * Cancels a print job.
     *
     * @param printOut
     *            The {@link PrintOut} object.
     * @return {@code true} when successfully cancelled.
     * @throws IppConnectException
     *             When an connection error occurs.
     */
    boolean cancelPrintJob(PrintOut printOut) throws IppConnectException;

    /**
     * Creates a {@link ProxyPrintDocReq} for an {@link OutboxJobDto}.
     *
     * @param user
     *            The {@link User}.
     * @param job
     *            The {@link OutboxJobDto}.
     * @param printMode
     *            The {@link PrintModeEnum}.
     * @return The {@link ProxyPrintDocReq}.
     */
    ProxyPrintDocReq createProxyPrintDocReq(User user, OutboxJobDto job,
            PrintModeEnum printMode);

    /**
     * Validates IPP choices according to generic and proxy printer specific
     * constraints.
     *
     * @param proxyPrinter
     *            The proxy printer holding the custom rules.
     * @param ippOptions
     *            The IPP attribute key/choice pairs.
     * @return The {@link Set} with conflicting IPP option keywords. When set is
     *         empty all choices are valid.
     */
    Set<String> validateContraints(JsonProxyPrinter proxyPrinter,
            Map<String, String> ippOptions);

    /**
     * Validates IPP choices according to generic and proxy printer specific
     * constraints. When valid, {@code null} is returned. Otherwise, a localized
     * message is returned.
     *
     * @param proxyPrinter
     *            The proxy printer holding the custom rules.
     * @param ippOptions
     *            The IPP attribute key/choice pairs.
     * @param locale
     *            The locale for the UI message.
     * @return The message string, or {@code null} when choices are valid.
     */
    String validateContraintsMsg(JsonProxyPrinter proxyPrinter,
            Map<String, String> ippOptions, Locale locale);

    /**
     * Validates IPP choices according to the custom cost rules of the proxy
     * printer. When valid, {@code null} is returned. Otherwise, a localized
     * message is returned. When proxy printer does not have custom rules,
     * {@code null} is returned.
     *
     * @param proxyPrinter
     *            The proxy printer holding the custom rules.
     * @param ippOptions
     *            The IPP attribute key/choice pairs.
     * @param locale
     *            The locale for the UI message.
     * @return The message string, or {@code null} when choices are valid.
     */
    String validateCustomCostRules(JsonProxyPrinter proxyPrinter,
            Map<String, String> ippOptions, Locale locale);
}
