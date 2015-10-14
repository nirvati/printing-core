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
package org.savapage.core.services.impl;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.print.attribute.standard.MediaSizeName;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.savapage.core.LetterheadNotFoundException;
import org.savapage.core.PerformanceLogger;
import org.savapage.core.PostScriptDrmException;
import org.savapage.core.SpException;
import org.savapage.core.circuitbreaker.CircuitStateEnum;
import org.savapage.core.cometd.AdminPublisher;
import org.savapage.core.cometd.PubLevelEnum;
import org.savapage.core.cometd.PubTopicEnum;
import org.savapage.core.config.CircuitBreakerEnum;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.dao.PrinterDao;
import org.savapage.core.dao.helpers.DeviceTypeEnum;
import org.savapage.core.dao.helpers.DocLogProtocolEnum;
import org.savapage.core.dao.helpers.PrintModeEnum;
import org.savapage.core.dao.helpers.ProxyPrinterName;
import org.savapage.core.dto.IppMediaSourceCostDto;
import org.savapage.core.dto.IppMediaSourceMappingDto;
import org.savapage.core.dto.PrinterSnmpDto;
import org.savapage.core.imaging.EcoPrintPdfTask;
import org.savapage.core.imaging.EcoPrintPdfTaskPendingException;
import org.savapage.core.inbox.InboxInfoDto;
import org.savapage.core.inbox.InboxInfoDto.InboxJob;
import org.savapage.core.inbox.InboxInfoDto.InboxJobRange;
import org.savapage.core.inbox.OutputProducer;
import org.savapage.core.ipp.IppSyntaxException;
import org.savapage.core.ipp.attribute.IppDictJobTemplateAttr;
import org.savapage.core.ipp.attribute.syntax.IppKeyword;
import org.savapage.core.ipp.client.IppConnectException;
import org.savapage.core.ipp.client.IppNotificationRecipient;
import org.savapage.core.job.SpJobScheduler;
import org.savapage.core.job.SpJobType;
import org.savapage.core.jpa.Account;
import org.savapage.core.jpa.Account.AccountTypeEnum;
import org.savapage.core.jpa.Device;
import org.savapage.core.jpa.DocLog;
import org.savapage.core.jpa.DocOut;
import org.savapage.core.jpa.Entity;
import org.savapage.core.jpa.PrintOut;
import org.savapage.core.jpa.Printer;
import org.savapage.core.jpa.PrinterAttr;
import org.savapage.core.jpa.User;
import org.savapage.core.jpa.UserCard;
import org.savapage.core.json.JsonPrinter;
import org.savapage.core.json.JsonPrinterDetail;
import org.savapage.core.json.JsonPrinterList;
import org.savapage.core.json.rpc.AbstractJsonRpcMessage;
import org.savapage.core.json.rpc.JsonRpcError.Code;
import org.savapage.core.json.rpc.JsonRpcMethodError;
import org.savapage.core.json.rpc.JsonRpcMethodResult;
import org.savapage.core.json.rpc.impl.ParamsPrinterSnmp;
import org.savapage.core.json.rpc.impl.ResultAttribute;
import org.savapage.core.json.rpc.impl.ResultPrinterSnmp;
import org.savapage.core.outbox.OutboxInfoDto.OutboxJob;
import org.savapage.core.pdf.PdfCreateRequest;
import org.savapage.core.print.proxy.AbstractProxyPrintReq;
import org.savapage.core.print.proxy.JsonProxyPrintJob;
import org.savapage.core.print.proxy.JsonProxyPrinter;
import org.savapage.core.print.proxy.JsonProxyPrinterOpt;
import org.savapage.core.print.proxy.JsonProxyPrinterOptChoice;
import org.savapage.core.print.proxy.JsonProxyPrinterOptGroup;
import org.savapage.core.print.proxy.ProxyPrintDocReq;
import org.savapage.core.print.proxy.ProxyPrintException;
import org.savapage.core.print.proxy.ProxyPrintInboxReq;
import org.savapage.core.print.proxy.ProxyPrintJobChunk;
import org.savapage.core.services.PrinterService;
import org.savapage.core.services.ProxyPrintService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.helpers.ExternalSupplierInfo;
import org.savapage.core.services.helpers.PageScalingEnum;
import org.savapage.core.services.helpers.PrinterAttrLookup;
import org.savapage.core.services.helpers.PrinterSnmpReader;
import org.savapage.core.services.helpers.ProxyPrintCostParms;
import org.savapage.core.services.helpers.ProxyPrintInboxReqChunker;
import org.savapage.core.snmp.SnmpClientSession;
import org.savapage.core.snmp.SnmpConnectException;
import org.savapage.core.util.MediaUtils;
import org.savapage.core.util.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Datraverse B.V.
 *
 */
public abstract class AbstractProxyPrintService extends AbstractService
        implements ProxyPrintService {

    /**
     *
     */
    private static final Logger LOGGER = LoggerFactory
            .getLogger(AbstractProxyPrintService.class);

    /**
     * .
     */
    private static final PrinterService PRINTER_SERVICE = ServiceContext
            .getServiceFactory().getPrinterService();

    /**
     *
     */
    private final IppNotificationRecipient notificationRecipient;

    /**
     * True or False option.
     */
    // protected static final Integer UI_BOOLEAN = 0;
    /**
     * Pick one from a list.
     */
    protected static final Integer UI_PICKONE = 1;
    /**
     * Pick zero or more from a list.
     */
    // protected static final Integer UI_PICKMANY = 2;

    /**
     * Dictionary on printer name. NOTE: the key is in UPPER CASE.
     */
    private final ConcurrentMap<String, JsonProxyPrinter> cupsPrinterCache =
            new ConcurrentHashMap<>();

    /**
     * Is this the first time CUPS is contacted? This switch is used for lazy
     * starting the CUPS subscription.
     */
    private final AtomicBoolean isFirstTimeCupsContact =
            new AtomicBoolean(true);

    /**
     *
     */
    private ArrayList<JsonProxyPrinterOptGroup> commonPrinterOptGroups = null;

    /**
     * The {@link URL} of the default (local) CUPS server.
     */
    private URL urlDefaultServer = null;

    /**
     * .
     */
    protected AbstractProxyPrintService() {

        notificationRecipient = new IppNotificationRecipient(this);

        commonPrinterOptGroups = createCommonCupsOptions();

        try {
            urlDefaultServer = new URL(getDefaultCupsUrl());
        } catch (MalformedURLException e) {
            throw new SpException(e);
        }

    }

    protected final URL getUrlDefaultServer() {
        return this.urlDefaultServer;
    }

    /**
     * URL of the CUPS host, like http://localhost:631
     *
     * @return
     */
    private String getDefaultCupsUrl() {
        return "http://" + ConfigManager.getDefaultCupsHost() + ":"
                + ConfigManager.getCupsPort();
    }

    @Override
    public final boolean isConnectedToCups() {
        return ConfigManager.getCircuitBreaker(
                CircuitBreakerEnum.CUPS_LOCAL_IPP_CONNECTION).isCircuitClosed();
    }

    protected boolean hasCommonPrinterOptGroups() {
        return commonPrinterOptGroups != null;
    }

    protected ArrayList<JsonProxyPrinterOptGroup> getCommonPrinterOptGroups() {
        return commonPrinterOptGroups;
    }

    /**
     * Creates common option groups to be added to ALL printers.
     *
     * @return {@code null} when NO common groups are defined.
     */
    protected abstract ArrayList<JsonProxyPrinterOptGroup>
            createCommonCupsOptions();

    @Override
    public void init() {

        /*
         * We have never contacted CUPS at this point.
         */
        this.isFirstTimeCupsContact.set(true);

        /*
         * Make sure the circuit is closed, so a first attempt to use it is
         * honored.
         */
        ConfigManager.getCircuitBreaker(
                CircuitBreakerEnum.CUPS_LOCAL_IPP_CONNECTION).setCircuitState(
                CircuitStateEnum.CLOSED);

    }

    @Override
    public void exit() throws IppConnectException, IppSyntaxException {
        /*
         * Closes the CUPS services.
         *
         * The subscription to CUPS events is stopped. However, when this method
         * is called as a reaction to a <i>Linux OS shutdown</i>, CUPS probably
         * is stopped before SavaPage. In that case we encounter an exception
         * because the CUPS API fails in {@link #CUPS_BIN}. The exception is
         * catched and logged at INFO level.
         */
        try {
            stopSubscription(null);
        } catch (SpException e) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info(e.getMessage());
            }
        }
    }

    /**
     *
     */
    private String defaultPrinterName = null;

    protected boolean hasDefaultPrinterName() {
        return StringUtils.isNotBlank(defaultPrinterName);
    }

    protected String getDefaultPrinterName() {
        return defaultPrinterName;
    }

    protected void setDefaultPrinterName(String defaultPrinterName) {
        this.defaultPrinterName = defaultPrinterName;
    }

    @Override
    public final JsonPrinterDetail
            getPrinterDetailCopy(final String printerName) {
        return this.getPrinterDetailCopy(printerName, null, false);
    }

    @Override
    public final JsonPrinterDetail getPrinterDetailUserCopy(
            final Locale locale, final String printerName) {
        return this.getPrinterDetailCopy(printerName, locale, true);
    }

    /**
     * Gets a copy of the {@link JsonProxyPrinter} from the printer cache.
     *
     * @param printerName
     *            The printer name.
     * @return {@code null} when the printer is no longer part of the cache.
     */
    private JsonProxyPrinter getJsonProxyPrinterCopy(final String printerName) {

        final JsonProxyPrinter printerCopy;

        final JsonProxyPrinter cupsPrinter = getCachedCupsPrinter(printerName);

        if (cupsPrinter != null) {
            printerCopy = cupsPrinter.copy();
        } else {
            printerCopy = null;
        }

        return printerCopy;
    }

    /**
     * Gets a copy of the JsonPrinter from the printer cache. If this is a User
     * copy, the printer options are filtered according to user settings and
     * permissions.
     * <p>
     * <b>Note</b>: a copy is returned so the caller can do his private
     * {@link #localize(Locale, JsonPrinterDetail)}.
     * </p>
     *
     * @param printerName
     *            The printer name.
     * @param locale
     *            The user {@link Locale}.
     * @param isUserCopy
     *            {@code true} if this is a copy for a user.
     * @return {@code null} when the printer is no longer part of the cache.
     */
    private JsonPrinterDetail getPrinterDetailCopy(final String printerName,
            final Locale locale, final boolean isUserCopy) {

        final JsonPrinterDetail printerCopy;

        final JsonProxyPrinter cupsPrinter = getCachedCupsPrinter(printerName);

        if (cupsPrinter != null) {

            final JsonPrinterDetail printer = new JsonPrinterDetail();

            printer.setDbKey(cupsPrinter.getDbPrinter().getId());
            printer.setName(cupsPrinter.getName());
            printer.setLocation(cupsPrinter.getDbPrinter().getLocation());
            printer.setAlias(cupsPrinter.getDbPrinter().getDisplayName());
            printer.setGroups(cupsPrinter.getGroups());
            printer.setPrinterUri(cupsPrinter.getPrinterUri());

            /*
             * Create copy, localize and prune.
             */
            printerCopy = printer.copy();

            if (locale != null) {
                this.localize(locale, printerCopy);
            }

            if (isUserCopy) {
                this.setPrinterMediaSourcesForUser(cupsPrinter.getDbPrinter(),
                        printerCopy);
            }

        } else {
            printerCopy = null;
        }

        return printerCopy;
    }

    /**
     * Prunes printer media-source options according to user settings and
     * permissions and sets the
     * {@link JsonPrinterDetail#setMediaSources(ArrayList)} .
     *
     * @param printer
     *            The {@link Printer} from the cache.
     * @param printerDetail
     *            The {@link JsonPrinterDetail} to prune.
     */
    private void setPrinterMediaSourcesForUser(final Printer printer,
            final JsonPrinterDetail printerDetail) {

        final ArrayList<IppMediaSourceMappingDto> mediaSources =
                new ArrayList<>();

        printerDetail.setMediaSources(mediaSources);

        /*
         * Find the media-source option and choices.
         */
        JsonProxyPrinterOpt mediaSourceOption = null;

        List<JsonProxyPrinterOptChoice> mediaSourceChoices = null;

        for (final JsonProxyPrinterOptGroup optGroup : printerDetail
                .getGroups()) {

            for (final JsonProxyPrinterOpt option : optGroup.getOptions()) {

                if (option.getKeyword().equals(
                        IppDictJobTemplateAttr.ATTR_MEDIA_SOURCE)) {

                    mediaSourceOption = option;
                    mediaSourceChoices = option.getChoices();

                    break;
                }
            }

            if (mediaSourceChoices != null) {
                break;
            }
        }

        if (mediaSourceChoices == null) {
            return;
        }

        /*
         * We need a JPA "attached" printer instance to create the lookup.
         */

        final Printer dbPrinter = printerDAO().findById(printer.getId());

        final PrinterAttrLookup lookup = new PrinterAttrLookup(dbPrinter);

        final Iterator<JsonProxyPrinterOptChoice> iterMediaSourceChoice =
                mediaSourceChoices.iterator();

        while (iterMediaSourceChoice.hasNext()) {

            final JsonProxyPrinterOptChoice optChoice =
                    iterMediaSourceChoice.next();

            final PrinterDao.MediaSourceAttr mediaSourceAttr =
                    new PrinterDao.MediaSourceAttr(optChoice.getChoice());

            final String json = lookup.get(mediaSourceAttr.getKey());

            boolean removeMediaSourceChoice = true;

            if (json != null) {

                try {

                    final IppMediaSourceCostDto dto =
                            IppMediaSourceCostDto.create(json);

                    if (dto.getActive()) {

                        optChoice.setText(dto.getDisplay());

                        if (dto.getMedia() != null) {

                            final IppMediaSourceMappingDto mediaSource =
                                    new IppMediaSourceMappingDto();

                            mediaSource.setSource(dto.getSource());
                            mediaSource.setMedia(dto.getMedia().getMedia());

                            mediaSources.add(mediaSource);
                        }

                        removeMediaSourceChoice = false;
                    }

                } catch (IOException e) {
                    // be forgiving
                    LOGGER.error(e.getMessage());
                }
            }

            if (removeMediaSourceChoice) {
                iterMediaSourceChoice.remove();
            }
        }

        final JsonProxyPrinterOptChoice choiceAuto =
                new JsonProxyPrinterOptChoice();

        choiceAuto.setChoice(IppKeyword.MEDIA_SOURCE_AUTO);
        choiceAuto.setText("Automatic"); // TODO
        mediaSourceChoices.add(0, choiceAuto);

        mediaSourceOption.setDefchoice(IppKeyword.MEDIA_SOURCE_AUTO);
    }

    @Override
    public final JsonPrinterList getUserPrinterList(final Device terminal,
            final String userName) throws IppConnectException,
            IppSyntaxException {

        lazyInitPrinterCache();

        /*
         * The collected valid printers.
         */
        final ArrayList<JsonPrinter> collectedPrinters = new ArrayList<>();

        /*
         * Walker variables.
         */
        final MutableBoolean terminalSecuredWlk = new MutableBoolean();
        final MutableBoolean readerSecuredWlk = new MutableBoolean();

        final Map<String, Device> terminalDevicesWlk = new HashMap<>();
        final Map<String, Device> readerDevicesWlk = new HashMap<>();

        /*
         * Traverse the printer cache.
         */
        for (final JsonProxyPrinter printer : this.cupsPrinterCache.values()) {

            /*
             * Bring printer back into JPA session so @OneTo* relations can be
             * resolved.
             */
            final Printer dbPrinterWlk =
                    printerDAO().findById(printer.getDbPrinter().getId());

            /*
             * Skip printer that is not configured.
             */
            if (!this.isPrinterConfigured(printer, new PrinterAttrLookup(
                    dbPrinterWlk))) {
                continue;
            }

            if (isPrinterGrantedOnTerminal(terminal, userName, dbPrinterWlk,
                    terminalSecuredWlk, readerSecuredWlk, terminalDevicesWlk,
                    readerDevicesWlk)) {

                if (readerSecuredWlk.getValue()) {
                    /*
                     * Card Reader secured: one Printer entry for each reader.
                     */
                    for (final Entry<String, Device> entry : readerDevicesWlk
                            .entrySet()) {

                        final JsonPrinter basicPrinter = new JsonPrinter();

                        collectedPrinters.add(basicPrinter);

                        basicPrinter.setDbKey(dbPrinterWlk.getId());
                        basicPrinter.setName(printer.getName());
                        basicPrinter.setLocation(dbPrinterWlk.getLocation());
                        basicPrinter.setAlias(dbPrinterWlk.getDisplayName());
                        basicPrinter.setTerminalSecured(terminalSecuredWlk
                                .getValue());
                        basicPrinter.setReaderSecured(readerSecuredWlk
                                .getValue());

                        basicPrinter
                                .setAuthMode(deviceService()
                                        .getProxyPrintAuthMode(
                                                entry.getValue().getId()));

                        basicPrinter.setReaderName(entry.getKey());
                    }

                } else {
                    /*
                     * Just Terminal secured: one Printer entry.
                     */
                    final JsonPrinter basicPrinter = new JsonPrinter();

                    collectedPrinters.add(basicPrinter);

                    basicPrinter.setDbKey(dbPrinterWlk.getId());
                    basicPrinter.setName(printer.getName());
                    basicPrinter.setAlias(dbPrinterWlk.getDisplayName());
                    basicPrinter.setLocation(dbPrinterWlk.getLocation());
                    basicPrinter.setTerminalSecured(terminalSecuredWlk
                            .getValue());
                    basicPrinter.setReaderSecured(readerSecuredWlk.getValue());
                }

            }
        }

        final JsonPrinterList printerList = new JsonPrinterList();
        printerList.setList(collectedPrinters);

        if (hasDefaultPrinterName()) {
            printerList
                    .setDfault(getPrinterDetailCopy(getDefaultPrinterName()));
        }
        return printerList;
    }

    @Override
    public final IppNotificationRecipient notificationRecipient() {
        return notificationRecipient;
    }

    @Override
    public boolean isPrinterConfigured(final JsonProxyPrinter cupsPrinter,
            final PrinterAttrLookup lookup) {

        /*
         * Any media sources defined in CUPS printer?
         */
        List<JsonProxyPrinterOptChoice> mediaSourceChoices = null;

        for (final JsonProxyPrinterOptGroup optGroup : cupsPrinter.getGroups()) {

            for (final JsonProxyPrinterOpt option : optGroup.getOptions()) {

                if (option.getKeyword().equals(
                        IppDictJobTemplateAttr.ATTR_MEDIA_SOURCE)) {
                    mediaSourceChoices = option.getChoices();
                    break;
                }
            }

            if (mediaSourceChoices != null) {
                break;
            }
        }

        /*
         * There MUST be media source(s) defines in CUPS printer.
         */
        if (mediaSourceChoices == null) {
            return false;
        }

        /*
         * Count the number of configured media sources.
         */
        int nMediaSources = 0;

        for (final JsonProxyPrinterOptChoice optChoice : mediaSourceChoices) {

            final PrinterDao.MediaSourceAttr mediaSourceAttr =
                    new PrinterDao.MediaSourceAttr(optChoice.getChoice());

            final String json = lookup.get(mediaSourceAttr.getKey());

            if (json != null) {

                try {

                    final IppMediaSourceCostDto dto =
                            IppMediaSourceCostDto.create(json);

                    if (dto.getActive() && dto.getMedia() != null) {
                        nMediaSources++;
                    }

                } catch (IOException e) {
                    // Be forgiving when old JSON format.
                    LOGGER.debug(e.getMessage());
                }
            }
        }

        return nMediaSources > 0;
    }

    @Override
    public final boolean isColorPrinter(final String printerName) {
        return getCachedCupsPrinter(printerName).getColorDevice()
                .booleanValue();
    }

    @Override
    public final boolean isDuplexPrinter(final String printerName) {
        return getCachedCupsPrinter(printerName).getDuplexDevice()
                .booleanValue();
    }

    @Override
    public final boolean isCupsPrinterDetails(final String printerName) {
        return getCachedCupsPrinter(printerName) != null;
    }

    @Override
    public final boolean hasMediaSourceManual(final String printerName) {
        final Boolean manual =
                getCachedCupsPrinter(printerName).getManualMediaSource();
        if (manual == null) {
            return false;
        }
        return manual.booleanValue();
    }

    @Override
    public final boolean hasMediaSourceAuto(final String printerName) {
        final Boolean auto =
                getCachedCupsPrinter(printerName).getManualMediaSource();
        if (auto == null) {
            return false;
        }
        return auto.booleanValue();
    }

    /**
     * Convenience method to make sure the printer name is converted to format
     * used in database, i.e. UPPER CASE.
     *
     * @param printerName
     *            The unique printer name.
     * @return The {@link JsonProxyPrinter}.
     */
    private JsonProxyPrinter getCachedCupsPrinter(final String printerName) {
        return this.cupsPrinterCache.get(ProxyPrinterName
                .getDaoName(printerName));
    }

    @Override
    public final JsonProxyPrinter getCachedPrinter(final String printerName) {
        return getCachedCupsPrinter(printerName);
    }

    @Override
    public final void updateCachedPrinter(final Printer dbPrinter) {

        final JsonProxyPrinter proxyPrinter =
                this.cupsPrinterCache.get(dbPrinter.getPrinterName());

        if (proxyPrinter != null) {
            this.assignDbPrinter(proxyPrinter, dbPrinter);
        }
    }

    /**
     * Assigns the database {@link Printer} to the {@link JsonProxyPrinter}, and
     * overrules IPP option defaults specified as {@link PrinterAttr}.
     *
     * @param proxyPrinter
     *            The {@link JsonProxyPrinter}.
     * @param dbPrinter
     *            The database {@link Printer}.
     */
    private void assignDbPrinter(final JsonProxyPrinter proxyPrinter,
            final Printer dbPrinter) {

        proxyPrinter.setDbPrinter(dbPrinter);

        final String colorModeDefault =
                printerService().getPrintColorModeDefault(dbPrinter);

        if (colorModeDefault != null) {

            final Map<String, JsonProxyPrinterOpt> optionLookup =
                    proxyPrinter.getOptionsLookup();

            final JsonProxyPrinterOpt colorModeOpt =
                    optionLookup
                            .get(IppDictJobTemplateAttr.ATTR_PRINT_COLOR_MODE);

            if (colorModeOpt != null) {
                colorModeOpt.setDefchoice(colorModeDefault);
            }
        }
    }

    /**
     * Checks if a Printer is granted for a User on a Terminal and if its access
     * is Terminal Secured or Reader Secured (Print Authentication required via
     * Network Card Reader).
     *
     * @param terminal
     *            The Terminal Device (can be {@code null}).
     * @param userName
     *            The unique name of the requesting user.
     * @param printer
     *            The Printer.
     * @param terminalSecured
     *            Return value which holds {@code true} if Printer is secured
     *            via {@link Device.DeviceTypeEnum#TERMINAL}.
     * @param readerSecured
     *            Return value which holds {@code true} if Printer is secured
     *            via {@link Device.DeviceTypeEnum#CARD_READER}.
     * @param terminalDevices
     *            The Terminal Devices responsible for printer being secured.
     *            The map is cleared before collecting the members.
     * @param readerDevices
     *            The Reader Devices responsible for printer being secured. The
     *            map is cleared before collecting the members.
     * @return {@code true} is Printer is secured (either via Reader or
     *         Terminal).
     */
    private boolean isPrinterGrantedOnTerminal(final Device terminal,
            final String userName, final Printer printer,
            final MutableBoolean terminalSecured,
            final MutableBoolean readerSecured,
            final Map<String, Device> terminalDevices,
            final Map<String, Device> readerDevices) {

        /*
         * INVARIANT: we MUST be dealing with a terminal device.
         */
        if (terminal != null && !deviceDAO().isTerminal(terminal)) {
            throw new SpException("Device [" + terminal.getDisplayName()
                    + "] is not of type [" + DeviceTypeEnum.TERMINAL + "]");
        }

        /*
         * Reset return values.
         */
        terminalSecured.setValue(false);
        readerSecured.setValue(false);
        terminalDevices.clear();
        readerDevices.clear();

        /*
         * Evaluate availability.
         *
         * (1) disabled or deleted?
         */
        if (printer.getDisabled() || printer.getDeleted()) {
            return false;
        }

        /*
         * (2) Check dedicated printer(s) for device.
         */
        boolean isGlobalNonSecure = false;

        if (terminal != null && deviceDAO().hasPrinterRestriction(terminal)) {

            terminalSecured.setValue(printerService().checkDeviceSecurity(
                    printer, DeviceTypeEnum.TERMINAL, terminal));

        } else {

            if (!printerService().checkPrinterSecurity(printer,
                    terminalSecured, readerSecured, terminalDevices,
                    readerDevices)) {

                isGlobalNonSecure =
                        ConfigManager.instance().isNonSecureProxyPrinter(
                                printer);
            }
        }

        boolean isAvailable =
                isGlobalNonSecure || terminalSecured.getValue()
                        || readerSecured.getValue();

        /*
         * (3) user group access control?
         */
        if (isAvailable) {

            final User user = userDAO().findActiveUserByUserId(userName);

            isAvailable =
                    user != null
                            && printerService().isPrinterAccessGranted(printer,
                                    user);
        }
        return isAvailable;
    }

    /**
     * Retrieves printer details from CUPS.
     *
     * @return A list of {@link JsonProxyPrinter} objects.
     * @throws IppConnectException
     * @throws URISyntaxException
     * @throws MalformedURLException
     */
    protected abstract List<JsonProxyPrinter> retrieveCupsPrinters()
            throws IppConnectException, URISyntaxException,
            MalformedURLException;

    /**
     * Retrieves the printer details. Note that the details are a subset of all
     * the IPP printer options.
     *
     * @param printerName
     *            The CUPS printer name.
     * @param printerUri
     *            The URI of the printer.
     * @return The {@link JsonProxyPrinter} or {@code null} when not found.
     * @throws IppConnectException
     *             When IPP connection failed.
     */
    abstract JsonProxyPrinter retrieveCupsPrinterDetails(String printerName,
            URI printerUri) throws IppConnectException;

    /**
     * Retrieves data for a list of print jobs ids for a printer.
     *
     * @param printerName
     *            The identifying name of the printer.
     * @param jobIds
     *            The job ids.
     * @return A list of print job objects.
     * @throws IppConnectException
     *             When a connection error occurs.
     */
    protected abstract List<JsonProxyPrintJob> retrievePrintJobs(
            String printerName, List<Integer> jobIds)
            throws IppConnectException;

    @Override
    public final JsonProxyPrintJob retrievePrintJob(final String printerName,
            final Integer jobId) throws IppConnectException {

        JsonProxyPrintJob printJob = null;

        final List<Integer> jobIds = new ArrayList<>();
        jobIds.add(jobId);

        final List<JsonProxyPrintJob> printJobList =
                retrievePrintJobs(printerName, jobIds);

        if (!printJobList.isEmpty()) {
            printJob = printJobList.get(0);
        }
        return printJob;
    }

    @Override
    public final int[] syncPrintJobs() throws IppConnectException {

        final int[] stats = new int[3];

        for (int i = 0; i < stats.length; i++) {
            stats[i] = 0;
        }

        /*
         * constants
         */
        final int nChunkMax = 50;

        /*
         * Init batch.
         */
        final List<PrintOut> list = printOutDAO().findActiveCupsJobs();

        final Map<Integer, PrintOut> lookupPrintOut = new HashMap<>();

        for (final PrintOut printOut : list) {
            lookupPrintOut.put(printOut.getCupsJobId(), printOut);
        }

        stats[0] = list.size();

        int i = 0;
        int iChunk = 0;
        PrintOut printOut = null;
        String printer = null;
        String printerPrv = null;
        List<Integer> jobIds = null;

        /*
         * initial read
         */
        if (i < list.size()) {
            printOut = list.get(i);
            printer = printOut.getPrinter().getPrinterName();
        }

        /*
         * processing loop
         */
        while (printOut != null) {

            printerPrv = printer;

            if (iChunk == 0) {
                jobIds = new ArrayList<>();
            }

            jobIds.add(printOut.getCupsJobId());

            /*
             * read next
             */
            printOut = null;
            i++;
            iChunk++;

            if (i < list.size()) {
                printOut = list.get(i);
                printer = printOut.getPrinter().getPrinterName();
            }

            /*
             * EOF, new printer or chunk filled
             */
            if (printOut == null || !printer.equals(printerPrv)
                    || iChunk == nChunkMax) {

                final List<JsonProxyPrintJob> cupsJobs =
                        retrievePrintJobs(printerPrv, jobIds);

                stats[2] += (iChunk - cupsJobs.size());

                /**
                 *
                 */
                for (final JsonProxyPrintJob cupsJob : cupsJobs) {

                    /*
                     * Since the list of retrieved jobs does NOT contain jobs
                     * that were NOT found, we use the lookup map.
                     */
                    final PrintOut printOutWlk =
                            lookupPrintOut.get(cupsJob.getJobId());

                    /*
                     * It turns out that when using IPP (HTTP) there might be a
                     * difference, so we do NOT check on time differences.
                     */
                    boolean checkCreationTime = false;

                    if (checkCreationTime
                            && !printOutWlk.getCupsCreationTime().equals(
                                    cupsJob.getCreationTime())) {

                        if (LOGGER.isTraceEnabled()) {
                            LOGGER.trace("MISMATCH printer [" + printerPrv
                                    + "] job [" + cupsJob.getJobId()
                                    + "] state [" + cupsJob.getJobState()
                                    + "] created in CUPS ["
                                    + cupsJob.getCreationTime() + "] in log ["
                                    + printOutWlk.getCupsCreationTime() + "]");
                        }

                    } else if (!printOutWlk.getCupsJobState().equals(
                            cupsJob.getJobState())) {
                        /*
                         * State change.
                         */
                        printOutWlk.setCupsJobState(cupsJob.getIppJobState()
                                .asInt());
                        printOutWlk.setCupsCompletedTime(cupsJob
                                .getCompletedTime());

                        printOutDAO().update(printOutWlk);

                        if (LOGGER.isTraceEnabled()) {
                            LOGGER.trace("printer [" + printerPrv + "] job ["
                                    + cupsJob.getJobId() + "] state ["
                                    + cupsJob.getJobState() + "] completed ["
                                    + cupsJob.getCompletedTime() + "]");
                        }
                        stats[1]++;
                    }

                }

                iChunk = 0;

            }
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Syncing [" + stats[0] + "] active PrintOut jobs "
                    + "with CUPS : updated [" + stats[1] + "], not found ["
                    + stats[2] + "]");
        }
        return stats;
    }

    /**
     * Calculates the number of Environmental Sheets Units (ESU) from number of
     * printed sheets and media size.
     *
     * <ul>
     * <li>1 ESU == 1/100 of an A4 sheet.</li>
     * <li>1 Sheet Unit (SU) == 1 A4 sheet.</li>
     * </ul>
     *
     * <p>
     * NOTE: As environmental impact is concerned, {@link MediaSizeName#ISO_A4}
     * and {@link MediaSizeName#NA_LETTER} are equivalent, and are therefore
     * counted as 100 ESUs.
     * </p>
     *
     * @param numberOfSheets
     *            The number of physical sheets.
     * @param mediaWidth
     *            Media width in mm.
     * @param mediaHeight
     *            Media height in mm.
     * @return The number of ESU.
     */
    protected final long calcNumberOfEsu(final int numberOfSheets,
            final int mediaWidth, final int mediaHeight) {

        final int[] sizeA4 =
                MediaUtils.getMediaWidthHeight(MediaSizeName.ISO_A4);
        final int[] sizeLetter =
                MediaUtils.getMediaWidthHeight(MediaSizeName.NA_LETTER);

        for (int[] size : new int[][] { sizeA4, sizeLetter }) {
            if (size[0] == mediaWidth && size[1] == mediaHeight) {
                return numberOfSheets * 100;
            }
        }
        /*
         * The full double.
         */
        final double nSheets =
                (double) (numberOfSheets * mediaWidth * mediaHeight)
                        / (sizeA4[0] * sizeA4[1]);
        /*
         * Round on 2 decimals by multiplying by 100.
         */
        return Math.round(nSheets * 100);
    }

    /**
     * Gets the CUPS {@code notify-recipient-uri}.
     * <p>
     * Example: {@code savapage:localhost:8631}
     * </p>
     *
     * @return
     */
    private String getSubscrNotifyRecipientUri() {
        return ConfigManager.getCupsNotifier() + ":localhost:"
                + ConfigManager.getServerPort();
    }

    /**
     *
     * @return
     */
    private String getSubscrNotifyLeaseSeconds() {
        return ConfigManager.instance().getConfigValue(
                Key.CUPS_IPP_SUBSCR_NOTIFY_LEASE_DURATION);
    }

    /**
     *
     * @param requestingUser
     * @return
     */
    private String getSubscrRequestingUser(final String requestingUser) {
        if (requestingUser == null) {
            return ConfigManager.getProcessUserName();
        }
        return requestingUser;
    }

    @Override
    public final void startSubscription(final String requestingUser)
            throws IppConnectException, IppSyntaxException {
        startSubscription(getSubscrRequestingUser(requestingUser),
                getSubscrNotifyLeaseSeconds(), getSubscrNotifyRecipientUri());
    }

    @Override
    public final void stopSubscription(final String requestingUser)
            throws IppConnectException, IppSyntaxException {
        stopSubscription(getSubscrRequestingUser(requestingUser),
                getSubscrNotifyRecipientUri());
    }

    /**
     *
     * @param requestingUser
     * @param recipientUri
     * @throws IppConnectException
     * @throws IppSyntaxException
     */
    abstract protected void stopSubscription(final String requestingUser,
            String recipientUri) throws IppConnectException, IppSyntaxException;

    /**
     *
     * @param requestingUser
     * @param leaseSeconds
     * @param recipientUri
     * @throws IppConnectException
     * @throws IppSyntaxException
     */
    abstract protected void startSubscription(final String requestingUser,
            String leaseSeconds, String recipientUri)
            throws IppConnectException, IppSyntaxException;

    @Override
    public final void lazyInitPrinterCache() throws IppConnectException,
            IppSyntaxException {

        if (!this.isFirstTimeCupsContact.get()) {
            return;
        }

        try {
            updatePrinterCache();
        } catch (MalformedURLException | URISyntaxException e) {
            throw new IppConnectException(e);
        }
    }

    @Override
    public final void initPrinterCache() throws IppConnectException,
            IppSyntaxException {

        try {
            updatePrinterCache();
        } catch (MalformedURLException | URISyntaxException e) {
            throw new IppConnectException(e);
        }
    }

    /**
     * Updates (or initializes) the printer cache with retrieved printer
     * information from CUPS.
     * <p>
     * <i>When this is a first-time connection to CUPS, CUPS event subscription
     * and a one-shot CUPS job sync is started.</i>
     * </p>
     * <ul>
     * <li>New printers (printer name as key) are added to the cache AND to the
     * database.</li>
     * <li>Removed CUPS printers are deleted from the cache.</li>
     * <li>Printers with same name but changed signature (PPD name and version)
     * are update in the cache.</li>
     * <li>When a CUPS printer is identical to a logical deleted ProxyPrinter,
     * the logical delete mark will be removed so the ProxyPrinter will be
     * re-activated.</li>
     * </ul>
     *
     * @throws URISyntaxException
     * @throws MalformedURLException
     * @throws IppConnectException
     *             When a connection error occurs.
     * @throws IppSyntaxException
     *             When a syntax error.
     */
    protected synchronized final void updatePrinterCache()
            throws MalformedURLException, IppConnectException,
            URISyntaxException, IppSyntaxException {

        final boolean firstTimeCupsContact =
                this.isFirstTimeCupsContact.getAndSet(false);

        final boolean connectedToCupsPrv =
                !firstTimeCupsContact && isConnectedToCups();

        /*
         * If method below succeeds, the CUPS circuit breaker will be closed and
         * isConnectedToCups() will return true.
         */
        final List<JsonProxyPrinter> cupsPrinters = this.retrieveCupsPrinters();

        /*
         * We have a first-time connection to CUPS, so start the event
         * subscription and sync with CUPS jobs.
         */
        if (!connectedToCupsPrv && isConnectedToCups()) {

            startSubscription(null);

            LOGGER.trace("CUPS job synchronization started");

            SpJobScheduler.instance().scheduleOneShotJob(
                    SpJobType.CUPS_SYNC_PRINT_JOBS, 1L);
        }

        /*
         * Go on ....
         */
        String defaultPrinter = null;

        /*
         * Mark all currently cached printers as 'not present'.
         */
        final Map<String, Boolean> printersPresent = new HashMap<>();

        for (final String key : this.cupsPrinterCache.keySet()) {
            printersPresent.put(key, Boolean.FALSE);
        }

        /*
         * Traverse the CUPS printers.
         */
        final Date now = new Date();

        final boolean remoteCupsEnabled =
                ConfigManager.instance().isConfigValue(
                        Key.CUPS_IPP_REMOTE_ENABLED);

        for (final JsonProxyPrinter cupsPrinter : cupsPrinters) {

            /*
             * Access remote CUPS for remote printer?
             */
            if (!remoteCupsEnabled
                    && !isLocalPrinter(cupsPrinter.getPrinterUri())) {
                continue;
            }

            final String cupsPrinterKey = cupsPrinter.getName();

            /*
             * Mark as present.
             */
            printersPresent.put(cupsPrinterKey, Boolean.TRUE);

            /*
             * Get the cached replicate.
             */
            JsonProxyPrinter cachedCupsPrinter =
                    this.cupsPrinterCache.get(cupsPrinterKey);

            /*
             * Is this a new printer?
             */
            if (cachedCupsPrinter == null) {

                /*
                 * New cached object.
                 */
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("CUPS printer [" + cupsPrinter.getName()
                            + "] detected");
                }
                /*
                 * Add the extra groups.
                 */
                if (this.hasCommonPrinterOptGroups()) {
                    cupsPrinter.getGroups().addAll(0,
                            getCommonPrinterOptGroups());
                }
            }

            /*
             * Assign the replicated database printer + lazy create printer in
             * database.
             */
            this.assignDbPrinter(cupsPrinter,
                    printerDAO().findByNameInsert(cupsPrinter.getName()));

            /*
             * Undo the logical delete (if present).
             */
            final Printer dbPrinter = cupsPrinter.getDbPrinter();

            if (dbPrinter.getDeleted()) {

                printerService().undoLogicalDeleted(dbPrinter);

                dbPrinter.setModifiedBy(Entity.ACTOR_SYSTEM);
                dbPrinter.setModifiedDate(now);

                printerDAO().update(dbPrinter);
            }

            /*
             * Update the cache.
             */
            this.cupsPrinterCache.put(cupsPrinter.getName(), cupsPrinter);

            cachedCupsPrinter = cupsPrinter;

            if (printerService().canPrinterBeUsed(
                    cachedCupsPrinter.getDbPrinter())) {
                /*
                 * Native default printer found that can be used.
                 */
                if (cachedCupsPrinter.getDfault()) {
                    defaultPrinter = cachedCupsPrinter.getName();
                }
                /*
                 * If no native default printer found (yet), set the first
                 * printer that can be used as default.
                 */
                if (defaultPrinter == null) {
                    defaultPrinter = cachedCupsPrinter.getName();
                }
            }
        }

        /*
         * Remove printers from cache which are no longer present in CUPS.
         */
        for (Map.Entry<String, Boolean> entry : printersPresent.entrySet()) {

            if (!entry.getValue().booleanValue()) {

                final JsonProxyPrinter removed =
                        this.cupsPrinterCache.remove(entry.getKey());

                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("removed CUPS printer [" + removed.getName()
                            + "] detected");
                }
            }
        }

        setDefaultPrinterName(defaultPrinter);
    }

    @Override
    public final Map<String, String> getDefaultPrinterCostOptions(
            final String printerName) throws ProxyPrintException {

        try {
            lazyInitPrinterCache();
        } catch (Exception e) {
            throw new SpException(e.getMessage(), e);
        }

        final Map<String, String> printerOptionValues =
                new HashMap<String, String>();

        collectDefaultPrinterCostOptions(printerName, printerOptionValues);

        return printerOptionValues;
    }

    /**
     * Collects the "print-color-mode" and "sides" printer default options,
     * needed for cost calculation.
     *
     * @param printerName
     *            The printer name.
     * @param printerOptionValues
     *            The map to collect the default values on.
     * @throws ProxyPrintException
     *             When no printer details found.
     */
    private void collectDefaultPrinterCostOptions(final String printerName,
            final Map<String, String> printerOptionValues)
            throws ProxyPrintException {

        final JsonPrinterDetail printerDetail =
                getPrinterDetailCopy(printerName);

        if (printerDetail != null) {

            for (final JsonProxyPrinterOptGroup optGroup : printerDetail
                    .getGroups()) {

                for (final JsonProxyPrinterOpt option : optGroup.getOptions()) {

                    final String keyword = option.getKeyword();

                    if (keyword
                            .equals(IppDictJobTemplateAttr.ATTR_PRINT_COLOR_MODE)
                            || keyword
                                    .equals(IppDictJobTemplateAttr.ATTR_SIDES)) {
                        printerOptionValues.put(keyword, option.getDefchoice());
                    }
                }
            }

        } else {
            /*
             * INVARIANT: Printer details MUST be present.
             */
            if (printerDetail == null) {
                throw new ProxyPrintException("No details found for printer ["
                        + printerName + "].");
            }

        }
    }

    /**
     * Gets the {@link User} attached to the card number.
     *
     * @param cardNumber
     *            Tthe card number.
     * @return The {@link user}.
     * @throws ProxyPrintException
     *             When card is not associated with a user.
     */
    private User getValidateUserOfCard(final String cardNumber)
            throws ProxyPrintException {

        final UserCard userCard = userCardDAO().findByCardNumber(cardNumber);

        /*
         * INVARIANT: Card number MUST be associated with a User.
         */
        if (userCard == null) {
            throw new ProxyPrintException("Card number [" + cardNumber
                    + "] not associated with a user.");
        }
        return userCard.getUser();
    }

    /**
     * Gets the single {@link Printer} object from the reader {@link Device}
     * while validating {@link User} access.
     *
     * @param reader
     *            The reader {@link Device}.
     * @param user
     *            The {@link User}.
     * @return The {@link Printer}.
     * @throws ProxyPrintException
     *             When access is denied or no single proxy printer defined for
     *             card reader.
     */
    private Printer getValidateSingleProxyPrinterAccess(final Device reader,
            final User user) throws ProxyPrintException {

        final Printer printer = reader.getPrinter();

        /*
         * INVARIANT: printer MUST be available.
         */
        if (printer == null) {
            throw new ProxyPrintException(
                    "No proxy printer defined for card reader ["
                            + reader.getDeviceName() + "]");
        }

        this.getValidateProxyPrinterAccess(user, printer.getPrinterName(),
                ServiceContext.getTransactionDate());

        return printer;
    }

    @Override
    public final int proxyPrintOutbox(final Device reader,
            final String cardNumber) throws ProxyPrintException {

        final Date perfStartTime = PerformanceLogger.startTime();

        final User cardUser = getValidateUserOfCard(cardNumber);

        if (!outboxService().isOutboxPresent(cardUser.getUserId())) {
            return 0;
        }

        final Set<String> printerNames =
                deviceService().collectPrinterNames(reader);

        /*
         * Lock the user.
         */
        final User lockedUser = userDAO().lock(cardUser.getId());

        /*
         * Get the outbox job candidates.
         */
        final int nPagesTot;

        final List<OutboxJob> jobs =
                outboxService().getOutboxJobs(lockedUser.getUserId(),
                        printerNames, ServiceContext.getTransactionDate());

        /*
         * Make sure the CUPS printer is cached.
         */
        try {
            this.lazyInitPrinterCache();
        } catch (Exception e) {
            throw new ProxyPrintException(e);
        }

        /*
         * Check printer access and total costs first (all-or-none).
         */
        BigDecimal totCost = BigDecimal.ZERO;

        for (final OutboxJob job : jobs) {

            this.getValidateProxyPrinterAccess(cardUser, job.getPrinterName(),
                    ServiceContext.getTransactionDate());

            totCost = totCost.add(job.getCost());
        }

        accountingService().validateProxyPrintUserCost(lockedUser, totCost,
                ServiceContext.getLocale(),
                ServiceContext.getAppCurrencySymbol());

        int nTotWlk = 0;

        for (final OutboxJob job : jobs) {

            final ProxyPrintDocReq printReq =
                    new ProxyPrintDocReq(PrintModeEnum.HOLD);

            printReq.setDocumentUuid(FilenameUtils.getBaseName(job.getFile()));
            printReq.setJobName(job.getJobName());
            printReq.setNumberOfPages(job.getPages());
            printReq.setNumberOfCopies(job.getCopies());
            printReq.setPrinterName(job.getPrinterName());
            printReq.setRemoveGraphics(job.isRemoveGraphics());
            printReq.setEcoPrintShadow(job.isEcoPrint());
            printReq.setLocale(ServiceContext.getLocale());
            printReq.setIdUser(lockedUser.getId());
            printReq.putOptionValues(job.getOptionValues());
            printReq.setFitToPage(job.getFitToPage());
            printReq.setCost(job.getCost());

            /*
             * Create the DocLog container.
             */
            final DocLog docLog = this.createProxyPrintDocLog(printReq);

            final DocOut docOut = new DocOut();
            docLog.setDocOut(docOut);
            docOut.setDocLog(docLog);

            docLog.setTitle(printReq.getJobName());

            final File pdfFileToPrint =
                    outboxService().getOutboxFile(cardUser.getUserId(),
                            job.getFile());

            /*
             * Collect the DocOut data and proxy print.
             */
            try {

                docLogService().collectData4DocOut(lockedUser, docLog,
                        pdfFileToPrint, job.getUuidPageCount());

                proxyPrint(lockedUser, printReq, docLog, pdfFileToPrint);

                pdfFileToPrint.delete();

            } catch (IppConnectException | IOException e) {
                throw new SpException(e.getMessage());
            }

            nTotWlk += job.getPages() * job.getCopies();
        }

        nPagesTot = nTotWlk;

        PerformanceLogger.log(this.getClass(), "proxyPrintOutbox",
                perfStartTime, cardUser.getUserId());

        return nPagesTot;
    }

    @Override
    public final int proxyPrintInboxFast(final Device reader,
            final String cardNumber) throws ProxyPrintException {

        final Date perfStartTime = PerformanceLogger.startTime();

        final User cardUser = getValidateUserOfCard(cardNumber);

        final Printer printer =
                getValidateSingleProxyPrinterAccess(reader, cardUser);

        /*
         * Printer must be properly configured.
         */
        if (!this.isPrinterConfigured(
                this.getCachedCupsPrinter(printer.getPrinterName()),
                new PrinterAttrLookup(printer))) {

            throw new ProxyPrintException(String.format(
                    "Print for user \"%s\" denied: %s \"%s\" %s",
                    cardUser.getUserId(), "printer", printer.getPrinterName(),
                    "is not configured."));
        }

        //
        this.getValidateProxyPrinterAccess(cardUser, printer.getPrinterName(),
                ServiceContext.getTransactionDate());

        /*
         * Get Printer default options.
         */
        final Map<String, String> printerOptionValues =
                getDefaultPrinterCostOptions(printer.getPrinterName());

        final boolean isConvertToGrayscale =
                AbstractProxyPrintReq.isGrayscale(printerOptionValues)
                        && isColorPrinter(printer.getPrinterName())
                        && PRINTER_SERVICE.isClientSideMonochrome(printer);

        /*
         * Lock the user.
         */
        final User user = userDAO().lock(cardUser.getId());

        /*
         * Get the inbox.
         */
        final InboxInfoDto jobs;
        final int nPagesTot;

        if (inboxService().doesHomeDirExist(user.getUserId())) {

            inboxService().pruneOrphanJobs(
                    ConfigManager.getUserHomeDir(user.getUserId()), user);

            jobs =
                    inboxService().pruneForFastProxyPrint(
                            user.getUserId(),
                            ServiceContext.getTransactionDate(),
                            ConfigManager.instance().getConfigInt(
                                    Key.PROXY_PRINT_FAST_EXPIRY_MINS));

            nPagesTot = inboxService().calcNumberOfPagesInJobs(jobs);

        } else {

            jobs = null;
            nPagesTot = 0;
        }

        /*
         * INVARIANT: There MUST be at least one (1) inbox job.
         */
        if (nPagesTot == 0) {
            return 0;
        }

        /*
         * Create the request for each jobs, so we can check the credit limit
         * invariant.
         */
        final List<ProxyPrintInboxReq> printReqList = new ArrayList<>();

        final int nJobs = jobs.getJobs().size();

        if (nJobs > 1 && inboxService().isInboxVanilla(jobs)) {
            /*
             * Print each job separately.
             */
            int nJobPageBegin = 1;

            for (int iJob = 0; iJob < nJobs; iJob++) {

                final ProxyPrintInboxReq printReq = new ProxyPrintInboxReq();
                printReqList.add(printReq);

                final InboxJob job = jobs.getJobs().get(iJob);

                final int totJobPages = job.getPages().intValue();
                final int nJobPageEnd = nJobPageBegin + totJobPages - 1;
                final String pageRanges = nJobPageBegin + "-" + nJobPageEnd;

                /*
                 * Fixed values.
                 */
                printReq.setPrintMode(PrintModeEnum.FAST);
                printReq.setPrinterName(printer.getPrinterName());
                printReq.setNumberOfCopies(Integer.valueOf(1));
                printReq.setRemoveGraphics(false);
                printReq.setConvertToGrayscale(isConvertToGrayscale);
                printReq.setLocale(ServiceContext.getLocale());
                printReq.setIdUser(user.getId());
                printReq.putOptionValues(printerOptionValues);
                printReq.setMediaSourceOption(IppKeyword.MEDIA_SOURCE_AUTO);

                /*
                 * Variable values.
                 */
                printReq.setJobName(job.getTitle());
                printReq.setPageRanges(pageRanges);
                printReq.setNumberOfPages(totJobPages);

                /*
                 * If this is the last job, then clear all pages.
                 */
                printReq.setClearPages(iJob + 1 == nJobs);

                //
                nJobPageBegin += totJobPages;
            }

        } else {
            /*
             * Print as ONE job.
             */
            final ProxyPrintInboxReq printReq = new ProxyPrintInboxReq();
            printReqList.add(printReq);

            /*
             * Fixed values.
             */
            printReq.setPrintMode(PrintModeEnum.FAST);
            printReq.setPrinterName(printer.getPrinterName());
            printReq.setNumberOfCopies(Integer.valueOf(1));
            printReq.setRemoveGraphics(false);
            printReq.setConvertToGrayscale(isConvertToGrayscale);
            printReq.setLocale(ServiceContext.getLocale());
            printReq.setIdUser(user.getId());
            printReq.putOptionValues(printerOptionValues);
            printReq.setMediaSourceOption(IppKeyword.MEDIA_SOURCE_AUTO);

            /*
             * Variable values.
             */
            printReq.setJobName(jobs.getJobs().get(0).getTitle());
            printReq.setPageRanges(ProxyPrintInboxReq.PAGE_RANGES_ALL);
            printReq.setNumberOfPages(nPagesTot);
            printReq.setClearPages(true);

        }

        /*
         * INVARIANT: User MUST have enough balance.
         */
        final String currencySymbol = "";

        BigDecimal totalCost = BigDecimal.ZERO;

        for (final ProxyPrintInboxReq printReq : printReqList) {

            /*
             * Chunk!
             */
            this.chunkProxyPrintRequest(user, printReq, PageScalingEnum.CROP,
                    false, null);

            final ProxyPrintCostParms costParms = new ProxyPrintCostParms();

            /*
             * Set the common parameters for all print job chunks, and calculate
             * the cost.
             */
            costParms.setDuplex(printReq.isDuplex());
            costParms.setGrayscale(printReq.isGrayscale());
            costParms.setEcoPrint(printReq.isEcoPrintShadow()
                    || printReq.isEcoPrint());
            costParms.setNumberOfCopies(printReq.getNumberOfCopies());
            costParms.setPagesPerSide(printReq.getNup());

            printReq.setCost(accountingService().calcProxyPrintCost(
                    ServiceContext.getLocale(), currencySymbol, user, printer,
                    costParms, printReq.getJobChunkInfo()));

            totalCost = totalCost.add(printReq.getCost());
        }

        /*
         * Check the total, since each individual job may be within credit
         * limit, but the total may not.
         */
        final Account account =
                accountingService().lazyGetUserAccount(user,
                        AccountTypeEnum.USER).getAccount();

        if (!accountingService().isBalanceSufficient(account, totalCost)) {
            throw new ProxyPrintException("User [" + user.getUserId()
                    + "] has insufficient balance for proxy printing.");
        }

        /*
         * Direct Proxy Print.
         */
        for (final ProxyPrintInboxReq printReq : printReqList) {

            try {

                proxyPrintInbox(user, printReq);

            } catch (Exception e) {

                throw new SpException("Printing error for user ["
                        + user.getUserId() + "] on printer ["
                        + printer.getPrinterName() + "].", e);
            }

            if (printReq.getStatus() != ProxyPrintInboxReq.Status.PRINTED) {

                throw new ProxyPrintException("Proxy print error ["
                        + printReq.getStatus() + "] on ["
                        + printer.getPrinterName() + "] for user ["
                        + user.getUserId() + "].");
            }
        }

        PerformanceLogger.log(this.getClass(), "proxyPrintInboxFast",
                perfStartTime, user.getUserId());

        return nPagesTot;
    }

    /**
     * Creates a standard {@link DocLog} instance for proxy printing: just the
     * {@link AbstractProxyPrintReq#getCost()} is used, and no related objects
     * are created.
     *
     * @param request
     *            The {@link AbstractProxyPrintReq}.
     * @return The {@link DocLog} instance.
     */
    private DocLog createProxyPrintDocLog(final AbstractProxyPrintReq request) {

        final DocLog docLog = new DocLog();

        /*
         * Financial data.
         */
        docLog.setCost(request.getCost());
        docLog.setCostOriginal(request.getCost());
        docLog.setRefunded(false);
        docLog.setInvoiced(true);

        docLog.setLogComment(request.getComment());

        /*
         * External supplier.
         */
        final ExternalSupplierInfo supplierInfo = request.getSupplierInfo();

        if (supplierInfo != null) {

            docLog.setExternalId(supplierInfo.getId());
            docLog.setExternalStatus(supplierInfo.getStatus());
            docLog.setExternalSupplier(supplierInfo.getSupplier().toString());

            if (supplierInfo.getData() != null) {
                docLog.setExternalData(supplierInfo.getData().dataAsString());
            }
        }

        return docLog;
    }

    /**
     * Sends PDF file to the CUPS Printer, and updates {@link User},
     * {@link Printer} and global {@link IConfigProp} statistics.
     * <p>
     * Note: This is a straight proxy print. {@link InboxInfoDto} is not
     * consulted or updated, and invariants are NOT checked.
     * </p>
     *
     * @param lockedUser
     *            The requesting {@link User}, which should be locked.
     * @param request
     *            The {@link AbstractProxyPrintReq}.
     * @param docLog
     *            The {@link DocLog} to persist in the database.
     * @param pdfFileToPrint
     *            The PDF file to send to the printer.
     * @throws IppConnectException
     *             When CUPS connection is broken.
     */
    private void proxyPrint(final User lockedUser,
            final AbstractProxyPrintReq request, final DocLog docLog,
            final File pdfFileToPrint) throws IppConnectException {

        final String userid = lockedUser.getUserId();

        final String userMsgKey;
        final String userMsg;

        /*
         * Print the PDF file.
         */
        if (print(userid, request.getPrintMode(), request.getPrinterName(),
                pdfFileToPrint, request.getJobName(),
                request.getNumberOfCopies(), request.getFitToPage(),
                request.getOptionValues(), docLog)) {

            if (request.isClearPages()) {
                request.setClearedPages(inboxService().deleteAllPages(userid));
            } else {
                request.setClearedPages(0);
            }

            docLogService().logDocOut(lockedUser, docLog.getDocOut(),
                    request.getAccountTrxInfoSet());

            request.setStatus(ProxyPrintInboxReq.Status.PRINTED);

            if (request.getClearedPages() == 0) {
                userMsgKey = "msg-printed";
                userMsg = localize(request.getLocale(), userMsgKey);
            } else if (request.getClearedPages() == 1) {
                userMsgKey = "msg-printed-deleted-one";
                userMsg = localize(request.getLocale(), userMsgKey);
            } else {
                userMsgKey = "msg-printed-deleted-multiple";
                userMsg =
                        localize(request.getLocale(), userMsgKey,
                                String.valueOf(request.getClearedPages()));
            }

            final PrintOut printOut = docLog.getDocOut().getPrintOut();

            //
            final String pagesMsgKey;
            if (docLog.getNumberOfPages().intValue() == 1) {
                pagesMsgKey = "msg-printed-for-admin-single-page";
            } else {
                pagesMsgKey = "msg-printed-for-admin-multiple-pages";
            }

            //
            final String copiesMsgKey;
            if (printOut.getNumberOfCopies().intValue() == 1) {
                copiesMsgKey = "msg-printed-for-admin-single-copy";
            } else {
                copiesMsgKey = "msg-printed-for-admin-multiple-copies";
            }

            //
            final String sheetsMsgKey;
            if (printOut.getNumberOfSheets().intValue() == 1) {
                sheetsMsgKey = "msg-printed-for-admin-single-sheet";
            } else {
                sheetsMsgKey = "msg-printed-for-admin-multiple-sheets";
            }

            AdminPublisher.instance().publish(
                    PubTopicEnum.PROXY_PRINT,
                    PubLevelEnum.INFO,
                    localize(request.getLocale(),
                            "msg-printed-for-admin",
                            request.getPrintMode().getUiText(),
                            printOut.getPrinter().getDisplayName(),
                            lockedUser.getUserId(),
                            //
                            localize(request.getLocale(), pagesMsgKey, docLog
                                    .getNumberOfPages().toString()),
                            //
                            localize(request.getLocale(), copiesMsgKey,
                                    printOut.getNumberOfCopies().toString()),
                            //
                            localize(request.getLocale(), sheetsMsgKey,
                                    printOut.getNumberOfSheets().toString()))
            //
                    );

        } else {
            request.setStatus(ProxyPrintInboxReq.Status.ERROR_PRINTER_NOT_FOUND);
            userMsgKey = "msg-printer-not-found";
            userMsg =
                    localize(request.getLocale(), userMsgKey,
                            request.getPrinterName());
            LOGGER.error(userMsg);
        }

        request.setUserMsgKey(userMsgKey);
        request.setUserMsg(userMsg);
    }

    @Override
    public final void proxyPrintPdf(final User lockedUser,
            final ProxyPrintDocReq request, final File pdfFile)
            throws IppConnectException, ProxyPrintException {

        /*
         * Get access to the printer.
         */
        final String printerName = request.getPrinterName();

        final Printer printer =
                this.getValidateProxyPrinterAccess(lockedUser, printerName,
                        ServiceContext.getTransactionDate());
        /*
         * Calculate and validate cost.
         */
        final ProxyPrintCostParms costParms = new ProxyPrintCostParms();

        /*
         * Set the common parameters.
         */
        costParms.setDuplex(request.isDuplex());
        costParms.setGrayscale(request.isGrayscale());
        costParms.setEcoPrint(request.isEcoPrintShadow()
                || request.isEcoPrint());
        costParms.setNumberOfCopies(request.getNumberOfCopies());
        costParms.setPagesPerSide(request.getNup());

        /*
         * Set the parameters for this single PDF file.
         */
        costParms.setNumberOfPages(request.getNumberOfPages());
        costParms.setIppMediaOption(request.getMediaOption());

        final BigDecimal cost =
                accountingService().calcProxyPrintCost(
                        ServiceContext.getLocale(),
                        ServiceContext.getAppCurrencySymbol(), lockedUser,
                        printer, costParms, request.getJobChunkInfo());

        request.setCost(cost);

        /*
         * Create the DocLog container.
         */
        final DocLog docLog = this.createProxyPrintDocLog(request);

        final DocOut docOut = new DocOut();
        docLog.setDocOut(docOut);
        docOut.setDocLog(docLog);

        docLog.setTitle(request.getJobName());

        /*
         * Collect the DocOut data for just a single DocIn document.
         */
        final LinkedHashMap<String, Integer> uuidPageCount =
                new LinkedHashMap<>();

        uuidPageCount.put(request.getDocumentUuid(),
                Integer.valueOf(request.getNumberOfPages()));

        try {
            docLogService().collectData4DocOut(lockedUser, docLog, pdfFile,
                    uuidPageCount);
        } catch (IOException e) {
            throw new SpException(e.getMessage());
        }

        /*
         * Finally, proxy print.
         */
        proxyPrint(lockedUser, request, docLog, pdfFile);
    }

    @Override
    public final void chunkProxyPrintRequest(final User lockedUser,
            final ProxyPrintInboxReq request,
            final PageScalingEnum pageScaling, final boolean chunkVanillaJobs,
            final Integer iVanillaJob) throws ProxyPrintException {
        new ProxyPrintInboxReqChunker(lockedUser, request, pageScaling).chunk(
                chunkVanillaJobs, iVanillaJob, request.getPageRanges());
    }

    @Override
    public final void proxyPrintInbox(final User lockedUser,
            final ProxyPrintInboxReq request) throws IppConnectException,
            EcoPrintPdfTaskPendingException {

        /*
         * When printing the chunks, the container request parameters are
         * replaced by chunk values. So, we save the original request parameters
         * here, and restore them afterwards.
         */
        final String orgJobName = request.getJobName();
        final boolean orgClearPages = request.isClearPages();
        final Boolean orgFitToPage = request.getFitToPage();
        final String orgMediaOption = request.getMediaOption();
        final String orgMediaSourceOption = request.getMediaSourceOption();
        final BigDecimal orgCost = request.getCost();

        try {

            if (request.getJobChunkInfo() == null) {

                final InboxInfoDto inboxInfo =
                        inboxService().readInboxInfo(lockedUser.getUserId());

                final InboxInfoDto filteredInboxInfo =
                        inboxService().filterInboxInfoPages(inboxInfo,
                                request.getPageRanges());

                proxyPrintInboxChunk(lockedUser, request, filteredInboxInfo, 0);

            } else {

                final InboxInfoDto inboxInfo =
                        request.getJobChunkInfo().getInboxInfo();

                final int nChunkMax =
                        request.getJobChunkInfo().getChunks().size();

                int nChunk = 0;

                for (final ProxyPrintJobChunk chunk : request.getJobChunkInfo()
                        .getChunks()) {

                    nChunk++;

                    /*
                     * Replace the request parameters with the chunk parameters.
                     */
                    request.setClearPages(orgClearPages && nChunk == nChunkMax);

                    request.setFitToPage(chunk.getFitToPage());

                    request.setMediaOption(chunk.getAssignedMedia()
                            .getIppKeyword());

                    /*
                     * Take the media-source from the print request, unless it
                     * is assigned in the chunk.
                     */
                    if (chunk.getAssignedMediaSource() == null) {
                        request.setMediaSourceOption(orgMediaSourceOption);
                    } else {
                        request.setMediaSourceOption(chunk
                                .getAssignedMediaSource().getSource());
                    }

                    request.setCost(chunk.getCost());

                    if (StringUtils.isBlank(orgJobName)) {
                        request.setJobName(chunk.getJobName());
                    }

                    /*
                     * Save the original pages.
                     */
                    final ArrayList<InboxJobRange> orgPages =
                            inboxService().replaceInboxInfoPages(inboxInfo,
                                    chunk.getRanges());

                    /*
                     * Proxy print the chunk.
                     */
                    proxyPrintInboxChunk(lockedUser, request, inboxInfo, nChunk);

                    /*
                     * Restore the original pages.
                     */
                    inboxInfo.setPages(orgPages);
                }
            }

        } finally {
            /*
             * Restore the original request parameters.
             */
            request.setJobName(orgJobName);
            request.setClearPages(orgClearPages);
            request.setFitToPage(orgFitToPage);
            request.setMediaOption(orgMediaOption);
            request.setMediaSourceOption(orgMediaSourceOption);
            request.setCost(orgCost);
        }
    }

    /**
     * Proxy prints a single inbox chunk.
     *
     * @param lockedUser
     *            The requesting {@link User}, which should be locked.
     * @param request
     *            The {@link ProxyPrintInboxReq}.
     * @param inboxInfo
     *            The {@link InboxInfoDto}.
     * @param nChunk
     *            The chunk ordinal (used to compose a unique PDF filename).
     * @throws IppConnectException
     *             When CUPS connection is broken.
     * @throws EcoPrintPdfTaskPendingException
     *             When {@link EcoPrintPdfTask} objects needed for this PDF are
     *             pending.
     */
    private void proxyPrintInboxChunk(final User lockedUser,
            final ProxyPrintInboxReq request, final InboxInfoDto inboxInfo,
            final int nChunk) throws IppConnectException,
            EcoPrintPdfTaskPendingException {

        final DocLog docLog = this.createProxyPrintDocLog(request);

        /*
         * Generate the temporary PDF file.
         */
        File pdfFileToPrint = null;

        try {

            final String pdfFileName =
                    OutputProducer.createUniqueTempPdfName(lockedUser,
                            String.format("printjob-%d-", nChunk));

            final LinkedHashMap<String, Integer> uuidPageCount =
                    new LinkedHashMap<>();

            final PdfCreateRequest pdfRequest = new PdfCreateRequest();

            pdfRequest.setUserObj(lockedUser);
            pdfRequest.setPdfFile(pdfFileName);
            pdfRequest.setInboxInfo(inboxInfo);
            pdfRequest.setRemoveGraphics(request.isRemoveGraphics());

            pdfRequest.setEcoPdf(request.isEcoPrint());
            pdfRequest.setEcoPdfShadow(request.isEcoPrintShadow());

            pdfRequest.setGrayscale(request.isConvertToGrayscale());
            pdfRequest.setApplyPdfProps(false);
            pdfRequest.setApplyLetterhead(true);
            pdfRequest.setForPrinting(true);

            pdfFileToPrint =
                    outputProducer().generatePdf(pdfRequest, uuidPageCount,
                            docLog);

            docLogService().collectData4DocOut(lockedUser, docLog,
                    pdfFileToPrint, uuidPageCount);

            // Print
            proxyPrint(lockedUser, request, docLog, pdfFileToPrint);

        } catch (LetterheadNotFoundException | PostScriptDrmException
                | IOException e) {

            throw new SpException(e.getMessage());

        } finally {

            if (pdfFileToPrint != null && pdfFileToPrint.exists()) {

                if (pdfFileToPrint.delete()) {

                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace("deleted temp file [" + pdfFileToPrint
                                + "]");
                    }

                } else {
                    LOGGER.error("delete of temp file [" + pdfFileToPrint
                            + "] FAILED");
                }
            }
        }
    }

    /**
     * Gets the printerName from the printer cache, and prints the offered job
     * with parameters and options.
     * <p>
     * NOTE: page ranges are not relevant, since they already filtered into the
     * PDF document.
     * </p>
     *
     * @param user
     *            The user (owner of the print job).
     * @param printMode
     *            The {@link PrintModeEnum}.
     * @param printerName
     *            Name of the printer, used as key in the printer cache.
     * @param filePdf
     *            The PDF file to print.
     * @param jobName
     *            Name of the job.
     * @param copies
     *            Number of copies.
     * @param optionValues
     *            Options with values.
     * @param docLog
     *            The object to collect print data on.
     * @return {@code true} when printer was found, {@code false} when printer
     *         is no longer valid (because not found in cache, or when it is
     *         logically deleted or disabled).
     * @throws IppConnectException
     */
    protected boolean print(final String user, final PrintModeEnum printMode,
            final String printerName, final File filePdf, final String jobName,
            final int copies, final Boolean fitToPage,
            final Map<String, String> optionValues, final DocLog docLog)
            throws IppConnectException {

        final JsonProxyPrinter printer =
                this.getJsonProxyPrinterCopy(printerName);

        if (printer == null) {
            return false;
        }

        if (printer.getDbPrinter().getDisabled()
                || printer.getDbPrinter().getDeleted()) {
            return false;
        }

        printPdf(printMode, printer, user, filePdf, jobName, copies, fitToPage,
                optionValues, docLog);

        return true;
    }

    /**
     * Prints a file and logs the event.
     *
     * @param printMode
     * @param printer
     *            The printer object.
     * @param user
     *            The requesting user.
     * @param filePdf
     *            The file to print.
     * @param jobName
     *            The name of the job.
     * @param copies
     *            Number of copies.
     * @param optionValues
     *            The printer options.
     * @param docLog
     *            The documentation object to log the event.
     */
    abstract protected void printPdf(final PrintModeEnum printMode,
            final JsonProxyPrinter printer, final String user,
            final File filePdf, final String jobName, final int copies,
            final Boolean fitToPage, final Map<String, String> optionValues,
            final DocLog docLog) throws IppConnectException;

    /**
     * Collects data of the print event in the {@link DocLog} object.
     *
     * @param docLog
     *            The documentation object to log the event.
     * @param protocol
     *            {@link DocLog.DocLogProtocolEnum#LPR} or
     *            {@link DocLog.DocLogProtocolEnum#IPP}
     * @param printMode
     * @param printer
     *            The printer object.
     * @param printJob
     *            The job object.
     * @param jobName
     *            The name of the job.
     * @param copies
     *            Number of copies.
     * @param duplex
     *            ({@code true} if duplex.
     * @param grayscale
     *            ({@code true} if grayscale (not color).
     * @param nUp
     * @param cupsPageSet
     * @param oddOrEvenSheets
     * @param cupsJobSheets
     * @param coverPageBefore
     * @param coverPageAfter
     * @param ippMediaSize
     *            The media size of the print output.
     */
    protected void collectPrintOutData(final DocLog docLog,
            final DocLogProtocolEnum protocol, final PrintModeEnum printMode,
            final JsonProxyPrinter printer, final JsonProxyPrintJob printJob,
            final String jobName, int copies, final boolean duplex,
            final boolean grayscale, final int nUp, String cupsPageSet,
            final boolean oddOrEvenSheets, final String cupsJobSheets,
            final boolean coverPageBefore, final boolean coverPageAfter,
            final MediaSizeName ippMediaSize) {

        int numberOfSheets =
                calcNumberOfPrintedSheets(docLog.getNumberOfPages(), copies,
                        duplex, nUp, oddOrEvenSheets, coverPageBefore,
                        coverPageAfter);

        final DocOut docOut = docLog.getDocOut();

        docLog.setDeliveryProtocol(protocol.getDbName());
        docOut.setDestination(printer.getName());
        docLog.setTitle(jobName);

        final PrintOut printOut = new PrintOut();
        printOut.setDocOut(docOut);

        printOut.setPrintMode(printMode.toString());
        printOut.setCupsJobId(printJob.getJobId());
        printOut.setCupsJobState(printJob.getJobState());
        printOut.setCupsCreationTime(printJob.getCreationTime());

        printOut.setDuplex(duplex);

        printOut.setGrayscale(grayscale);

        printOut.setCupsJobSheets(cupsJobSheets);
        printOut.setCupsNumberUp(String.valueOf(nUp));
        printOut.setCupsPageSet(cupsPageSet);

        printOut.setNumberOfCopies(copies);
        printOut.setNumberOfSheets(numberOfSheets);

        printOut.setPaperSize(ippMediaSize.toString());

        int[] size = MediaUtils.getMediaWidthHeight(ippMediaSize);
        printOut.setPaperWidth(size[0]);
        printOut.setPaperHeight(size[1]);

        printOut.setNumberOfEsu(calcNumberOfEsu(numberOfSheets,
                printOut.getPaperWidth(), printOut.getPaperHeight()));

        printOut.setPrinter(printer.getDbPrinter());

        docOut.setPrintOut(printOut);

    }

    /**
     * Calculates the number of printed sheets.
     *
     * @param numberOfPages
     * @param copies
     * @param duplex
     * @param nUp
     * @param oddOrEvenSheets
     * @param coverPageBefore
     * @param coverPageAfter
     * @return
     */
    public static int calcNumberOfPrintedSheets(int numberOfPages, int copies,
            boolean duplex, int nUp, boolean oddOrEvenSheets,
            boolean coverPageBefore, boolean coverPageAfter) {

        int nPages = numberOfPages;

        // NOTE: the order of handling the print options is important.

        if (nPages <= nUp) {
            nPages = 1;
        } else if (nUp != 1) {
            nPages = (nPages / nUp) + (nPages % nUp);
        }

        /*
         * (2) Odd or even pages?
         */
        if (oddOrEvenSheets) {
            nPages /= 2;
        }

        /*
         * Sheets
         */
        int nSheets = nPages;

        /*
         * (3) Duplex
         */
        if (duplex) {
            nSheets = (nSheets / 2) + (nSheets % 2);
        }

        /*
         * (4) Copies
         */
        nSheets *= copies;

        /*
         * (5) Jobs Sheets
         */
        if (coverPageBefore) {
            // cover page (before)
            nSheets++;
        }
        if (coverPageAfter) {
            // cover page (after)
            nSheets++;
        }

        return nSheets;
    }

    /**
     * Return a localized string.
     *
     * @param key
     *            The key of the string.
     * @return
     */
    protected String localize(final String key) {
        return Messages.getMessage(getClass(), key, null);
    }

    /**
     * Return a localized string.
     *
     * @param locale
     *            The Locale
     * @param key
     *            The key of the string.
     * @return
     */
    protected String localize(final Locale locale, final String key) {
        return Messages.getMessage(getClass(), locale, key);
    }

    /**
     * Return a localized message string.
     *
     * @param locale
     *            The Locale
     * @param key
     *            The key of the message.
     * @param args
     *            The placeholder arguments for the message template.
     *
     * @return The message text.
     */
    protected String localize(final Locale locale, final String key,
            final String... args) {
        return Messages.getMessage(getClass(), locale, key, args);
    }

    /**
     *
     * @param key
     * @param dfault
     * @return
     */
    protected String localizeWithDefault(final Locale locale, final String key,
            final String dfault) {
        if (Messages.containsKey(getClass(), key, locale)) {
            return Messages.getMessage(getClass(), locale, key);
        }
        return dfault;
    }

    @Override
    public final Printer getValidateProxyPrinterAccess(final User user,
            final String printerName, final Date refDate)
            throws ProxyPrintException {

        /*
         * INVARIANT: MUST be connected to CUPS.
         */
        if (!this.isConnectedToCups()) {
            throw new ProxyPrintException(
                    localize("msg-printer-connection-broken"));
        }

        final Printer printer = printerDAO().findByName(printerName);

        /*
         * INVARIANT: printer MUST exist.
         */
        if (printer == null) {
            throw new SpException("Printer [" + printerName + "] not found");
        }

        /*
         * INVARIANT: printer MUST be enabled.
         */
        if (printer.getDisabled()) {
            throw new ProxyPrintException("Proxy printer ["
                    + printer.getPrinterName() + "] is disabled");
        }

        /*
         * INVARIANT: User MUST be enabled to print.
         */
        if (userService().isUserPrintOutDisabled(user, refDate)) {
            throw new ProxyPrintException(
                    localize("msg-user-print-out-disabled"));
        }

        /*
         * INVARIANT: User MUST be have access to printer.
         */
        if (!printerService().isPrinterAccessGranted(printer, user)) {
            throw new ProxyPrintException(
                    localize("msg-user-print-out-access-denied"));
        }

        return printer;
    }

    @Override
    public final boolean isLocalPrinter(final URI uriPrinter) {
        return uriPrinter.getHost().equals(urlDefaultServer.getHost());
    }

    @Override
    public final Boolean isLocalPrinter(final String cupsPrinterName) {

        final JsonProxyPrinter proxyPrinter =
                this.getCachedPrinter(cupsPrinterName);

        final Boolean isLocal;

        if (proxyPrinter == null) {
            isLocal = null;
        } else {
            isLocal = isLocalPrinter(proxyPrinter.getPrinterUri());
        }

        return isLocal;
    }

    @Override
    public final List<JsonProxyPrinterOptChoice> getMediaChoices(
            final String printerName) {

        final JsonProxyPrinter proxyPrinter = getCachedPrinter(printerName);

        if (proxyPrinter != null) {

            for (final JsonProxyPrinterOptGroup group : proxyPrinter
                    .getGroups()) {

                for (final JsonProxyPrinterOpt option : group.getOptions()) {
                    if (option.getKeyword().equals(
                            IppDictJobTemplateAttr.ATTR_MEDIA)) {
                        return option.getChoices();
                    }
                }
            }
        }

        return new ArrayList<JsonProxyPrinterOptChoice>();
    }

    @Override
    public final List<JsonProxyPrinterOptChoice> getMediaChoices(
            final String printerName, final Locale locale) {

        final List<JsonProxyPrinterOptChoice> list =
                this.getMediaChoices(printerName);
        this.localizePrinterOptChoices(locale,
                IppDictJobTemplateAttr.ATTR_MEDIA, list);
        return list;
    }

    @Override
    public final Map<String, JsonProxyPrinterOpt> getOptionsLookup(
            final String printerName) {

        final Map<String, JsonProxyPrinterOpt> lookup;

        final JsonProxyPrinter proxyPrinter = getCachedPrinter(printerName);

        if (proxyPrinter == null) {
            lookup = new HashMap<>();
        } else {
            lookup = proxyPrinter.getOptionsLookup();
        }

        return lookup;
    }

    @Override
    public final AbstractJsonRpcMessage
            readSnmp(final ParamsPrinterSnmp params)
                    throws SnmpConnectException {

        final String printerName = params.getPrinterName();

        String host = null;

        if (printerName != null) {

            final JsonProxyPrinter printer = this.getCachedPrinter(printerName);

            /*
             * INVARIANT: printer MUST present in cache.
             */
            if (printer == null) {
                return JsonRpcMethodError.createBasicError(
                        Code.INVALID_REQUEST, "Printer [" + printerName
                                + "] is unknown.", null);
            }

            final URI printerUri = printer.getPrinterUri();

            if (printerUri != null) {

                final String scheme = printerUri.getScheme();

                if (scheme != null
                        && (scheme.endsWith("socket") || scheme
                                .equalsIgnoreCase("ipp"))) {
                    host = printerUri.getHost();
                }

            }

        } else {
            host = params.getHost();
        }

        /*
         * INVARIANT: host MUST be present.
         */
        if (host == null) {
            return JsonRpcMethodError.createBasicError(Code.INVALID_REQUEST,
                    "No host name.", null);
        }

        //
        final int port;

        if (params.getPort() == null) {
            port = SnmpClientSession.DEFAULT_PORT_READ;
        } else {
            port = Integer.valueOf(params.getPort()).intValue();
        }

        //
        final String community;

        if (params.getCommunity() == null) {
            community = SnmpClientSession.DEFAULT_COMMUNITY;
        } else {
            community = params.getCommunity();
        }

        final ResultPrinterSnmp data = new ResultPrinterSnmp();
        PrinterSnmpDto dto = PrinterSnmpReader.read(host, port, community);
        data.setAttributes(dto.asAttributes());

        data.getAttributes()
                .add(0, new ResultAttribute("Community", community));
        data.getAttributes().add(0,
                new ResultAttribute("Port", String.valueOf(port)));
        data.getAttributes().add(0, new ResultAttribute("Host", host));

        for (final ResultAttribute attr : data.getAttributes()) {
            if (attr.getValue() == null) {
                attr.setValue("?");
            }
        }

        return JsonRpcMethodResult.createResult(data);
    }
}
