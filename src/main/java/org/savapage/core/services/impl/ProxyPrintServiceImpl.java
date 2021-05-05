/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2020 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: © 2020 Datraverse B.V. <info@datraverse.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
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
package org.savapage.core.services.impl;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.Set;

import javax.print.attribute.standard.MediaSizeName;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.savapage.core.SpException;
import org.savapage.core.community.CommunityDictEnum;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.dao.IAttrDao;
import org.savapage.core.dao.PrinterDao;
import org.savapage.core.dao.enums.ACLOidEnum;
import org.savapage.core.dao.enums.PrinterAttrEnum;
import org.savapage.core.dao.helpers.ProxyPrinterName;
import org.savapage.core.doc.DocContent;
import org.savapage.core.doc.store.DocStoreBranchEnum;
import org.savapage.core.doc.store.DocStoreException;
import org.savapage.core.doc.store.DocStoreTypeEnum;
import org.savapage.core.dto.IppMediaCostDto;
import org.savapage.core.dto.IppMediaSourceCostDto;
import org.savapage.core.dto.MediaCostDto;
import org.savapage.core.dto.MediaPageCostDto;
import org.savapage.core.dto.ProxyPrinterCostDto;
import org.savapage.core.dto.ProxyPrinterDto;
import org.savapage.core.dto.ProxyPrinterMediaSourcesDto;
import org.savapage.core.i18n.PrintOutNounEnum;
import org.savapage.core.ipp.IppMediaSizeEnum;
import org.savapage.core.ipp.IppPrinterType;
import org.savapage.core.ipp.IppSyntaxException;
import org.savapage.core.ipp.attribute.AbstractIppDict;
import org.savapage.core.ipp.attribute.IppAttrGroup;
import org.savapage.core.ipp.attribute.IppAttrValue;
import org.savapage.core.ipp.attribute.IppDictJobDescAttr;
import org.savapage.core.ipp.attribute.IppDictJobTemplateAttr;
import org.savapage.core.ipp.attribute.IppDictJobTemplateAttr.ApplEnum;
import org.savapage.core.ipp.attribute.IppDictOperationAttr;
import org.savapage.core.ipp.attribute.IppDictPrinterDescAttr;
import org.savapage.core.ipp.attribute.IppDictSubscriptionAttr;
import org.savapage.core.ipp.attribute.syntax.IppBoolean;
import org.savapage.core.ipp.attribute.syntax.IppKeyword;
import org.savapage.core.ipp.client.IppClient;
import org.savapage.core.ipp.client.IppConnectException;
import org.savapage.core.ipp.client.IppReqCupsGetPpd;
import org.savapage.core.ipp.client.IppReqPrintJob;
import org.savapage.core.ipp.encoding.IppDelimiterTag;
import org.savapage.core.ipp.helpers.IppOptionMap;
import org.savapage.core.ipp.operation.IppGetPrinterAttrOperation;
import org.savapage.core.ipp.operation.IppOperationId;
import org.savapage.core.ipp.operation.IppStatusCode;
import org.savapage.core.jpa.DocLog;
import org.savapage.core.jpa.PrintOut;
import org.savapage.core.jpa.Printer;
import org.savapage.core.jpa.PrinterAttr;
import org.savapage.core.jpa.PrinterGroup;
import org.savapage.core.jpa.PrinterGroupMember;
import org.savapage.core.json.JsonAbstractBase;
import org.savapage.core.json.JsonPrinterDetail;
import org.savapage.core.json.rpc.AbstractJsonRpcMethodResponse;
import org.savapage.core.json.rpc.JsonRpcError.Code;
import org.savapage.core.json.rpc.JsonRpcMethodError;
import org.savapage.core.json.rpc.JsonRpcMethodResult;
import org.savapage.core.pdf.PdfCreateInfo;
import org.savapage.core.pdf.PdfPrintCollector;
import org.savapage.core.print.proxy.AbstractProxyPrintReq;
import org.savapage.core.print.proxy.JsonProxyPrintJob;
import org.savapage.core.print.proxy.JsonProxyPrinter;
import org.savapage.core.print.proxy.JsonProxyPrinterOpt;
import org.savapage.core.print.proxy.JsonProxyPrinterOptChoice;
import org.savapage.core.print.proxy.JsonProxyPrinterOptGroup;
import org.savapage.core.print.proxy.ProxyPrintLogger;
import org.savapage.core.print.proxy.ProxyPrinterOptGroupEnum;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.helpers.CupsPrinterClass;
import org.savapage.core.services.helpers.ThirdPartyEnum;
import org.savapage.core.util.BigDecimalUtil;
import org.savapage.core.util.DateUtil;
import org.savapage.core.util.InetUtils;
import org.savapage.core.util.JsonHelper;
import org.savapage.core.util.Messages;
import org.savapage.core.util.NumberUtil;
import org.savapage.ext.papercut.PaperCutHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class ProxyPrintServiceImpl extends AbstractProxyPrintService {

    /** */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(ProxyPrintServiceImpl.class);

    /** */
    private static final String CUSTOM_IPP_I18N_RESOURCE_NAME = "ipp-i18n";

    /** Key prefix of IPP option (choice) text. */
    private static final String LOCALIZE_IPP_ATTR_PREFIX = "ipp-attr-";

    /** Key prefix of IPP option choice(s) icon CSS class. */
    private static final String LOCALIZE_IPP_ICON_PREFIX = "ipp-icon-";

    /** */
    private static final String NOTIFY_PULL_METHOD = "ippget";

    /**
     * A unique ID to distinguish our subscription from other system
     * subscriptions.
     */
    private static final String NOTIFY_USER_DATA =
            "savapage:" + ConfigManager.getServerPort();

    /** */
    private static final String URL_PATH_CUPS_PRINTERS = "/printers";

    /** */
    private static final String URL_PATH_CUPS_ADMIN = "/admin";

    /** */
    private final IppClient ippClient = IppClient.instance();

    /**
     * {@code true} when custom IPP i18n files are present.
     */
    private final boolean hasCustomIppI18n;

    /**
     *
     * @throws MalformedURLException
     */
    public ProxyPrintServiceImpl() {
        super();
        this.hasCustomIppI18n = hasCustomIppI18n();
    }

    @Override
    public void init() {
        super.init();
        ippClient.init();
    }

    /**
     * Initializes the custom IPP i18n.
     *
     * @return {@code true} when custom CUPS i18n files are present.
     */
    private static boolean hasCustomIppI18n() {

        final File[] files =
                ConfigManager.getServerCustomCupsI18nHome().listFiles();

        if (files == null || files.length == 0) {
            return false;
        }

        for (final File file : files) {
            if (FilenameUtils.getExtension(file.getName())
                    .equals(DocContent.FILENAME_EXT_XML)
                    && file.getName()
                            .startsWith(CUSTOM_IPP_I18N_RESOURCE_NAME)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the localized IPP key string from custom resource.
     *
     * @param key
     *            The key.
     * @param locale
     *            The locale.
     * @return {@code null} if not found.
     */
    private String localizeCustomIpp(final String key, final Locale locale) {

        if (this.hasCustomIppI18n) {

            final ResourceBundle bundle = Messages.loadXmlResource(
                    ConfigManager.getServerCustomCupsI18nHome(),
                    CUSTOM_IPP_I18N_RESOURCE_NAME, locale);

            if (bundle.containsKey(key)) {
                return bundle.getString(key);
            }
        }
        return null;
    }

    @Override
    public void exit() throws IppConnectException, IppSyntaxException {
        super.exit();
        ippClient.shutdown();
    }

    @Override
    protected ArrayList<JsonProxyPrinterOptGroup> createCommonCupsOptions() {
        return null;
    }

    @Override
    protected List<JsonProxyPrinter> retrieveCupsPrinters()
            throws IppConnectException, URISyntaxException,
            MalformedURLException {

        // All printers, including CUPS printer classes.
        final List<JsonProxyPrinter> printers = new ArrayList<>();

        // A map of ALL printer by uppercase name.
        final Map<String, JsonProxyPrinter> printerMap = new HashMap<>();

        // The printers that are a CUPS printer class.
        final List<CupsPrinterClass> printerClasses = new ArrayList<>();

        //
        final boolean remoteCupsEnabled = ConfigManager.instance()
                .isConfigValue(Key.CUPS_IPP_REMOTE_ENABLED);

        /*
         * Get the list of CUPS printers.
         */
        final List<IppAttrGroup> response =
                ippClient.send(getUrlDefaultServer(),
                        IppOperationId.CUPS_GET_PRINTERS, reqCupsGetPrinters());

        /*
         * Traverse the response groups.
         */
        for (IppAttrGroup group : response) {

            /*
             * Handle PRINTER_ATTR groups only.
             */
            if (group.getDelimiterTag() != IppDelimiterTag.PRINTER_ATTR) {
                continue;
            }

            /*
             * Skip any SavaPage printer.
             */
            final String makeModel = group.getAttrSingleValue(
                    IppDictPrinterDescAttr.ATTR_PRINTER_MAKE_MODEL, "");

            if (makeModel.toLowerCase().startsWith("savapage")) {
                continue;
            }

            /*
             * Get the printer URI.
             */
            final String printerUriSupp = group.getAttrSingleValue(
                    IppDictPrinterDescAttr.ATTR_PRINTER_URI_SUPPORTED);

            final URI uriPrinter = new URI(printerUriSupp);

            /*
             * Skip remote printer when remoteCups is disabled.
             */
            if (!remoteCupsEnabled && !isLocalPrinter(uriPrinter)) {
                continue;
            }

            /*
             * Get the printer-type: printer class?
             */
            final Integer printerType =
                    Integer.parseInt(group.getAttrSingleValue(
                            IppDictPrinterDescAttr.ATTR_PRINTER_TYPE));

            if (IppPrinterType.hasProperty(printerType,
                    IppPrinterType.BitEnum.IMPLICIT_CLASS)) {
                continue;
            }

            final boolean isPpdPresent;

            if (IppPrinterType.hasProperty(printerType,
                    IppPrinterType.BitEnum.PRINTER_CLASS)) {

                isPpdPresent = false;

                final String printerName = group
                        .getAttrSingleValue(
                                IppDictPrinterDescAttr.ATTR_PRINTER_NAME)
                        .toUpperCase();

                final CupsPrinterClass printerClass = new CupsPrinterClass();

                printerClass.setPrinterUri(uriPrinter);
                printerClass.setName(printerName);

                for (final String member : group
                        .getAttrValue(IppDictPrinterDescAttr.ATTR_MEMBER_NAMES)
                        .getValues()) {
                    printerClass.addMemberName(member.toUpperCase());
                }

                printerClasses.add(printerClass);
            } else {
                isPpdPresent = isCupsPpdPresent(uriPrinter);
            }

            /*
             * Create JsonProxyPrinter object from PRINTER_ATTR group.
             */
            final JsonProxyPrinter proxyPrinterFromGroup =
                    createUserPrinter(group);

            if (proxyPrinterFromGroup == null) {
                continue;
            }

            /*
             * Retrieve printer details.
             */
            try {
                final JsonProxyPrinter proxyPrinterDetails =
                        retrieveCupsPrinterDetails(
                                proxyPrinterFromGroup.getName(),
                                proxyPrinterFromGroup.getPrinterUri());

                if (proxyPrinterDetails != null) {

                    proxyPrinterDetails.setPpdPresent(isPpdPresent);

                    printers.add(proxyPrinterDetails);
                    printerMap.put(proxyPrinterDetails.getName(),
                            proxyPrinterDetails);
                }

            } catch (IppConnectException e) {
                // noop
            }
        }

        //
        for (final CupsPrinterClass printerClass : printerClasses) {

            /*
             * INVARIANT: Printer Class MUST have members.
             */
            if (printerClass.getMemberNames().size() == 0) {
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn(String.format(
                            "No Proxy Printer Members of Printer "
                                    + "class [%s] found.",
                            printerClass.getName()));
                }
                continue;
            }

            /*
             * INVARIANT: Printer Class members MUST same Make and Model.
             */
            String commonMakeModel = null;

            for (final String member : printerClass.getMemberNames()) {

                final JsonProxyPrinter proxyPrinterMember =
                        printerMap.get(member);

                if (commonMakeModel == null) {
                    commonMakeModel = proxyPrinterMember.getModelName();
                } else if (!proxyPrinterMember.getModelName()
                        .equals(commonMakeModel)) {
                    commonMakeModel = null;
                    break;
                }
            }

            if (commonMakeModel == null) {
                LOGGER.error(String.format(
                        "Proxy Printer Members of Printer class [%s] "
                                + "do NOT have same Make/Model.",
                        printerClass.getName()));
                continue;
            }

            /*
             * Use the IPP options of the first member.
             */
            final String memberName = printerClass.getMemberNames().get(0);

            final JsonProxyPrinter proxyPrinterMember =
                    printerMap.get(memberName);

            /*
             * INVARIANT: Printer Class member MUST be present as Proxy Printer.
             */
            if (proxyPrinterMember == null) {
                LOGGER.error(String.format(
                        "Proxy Printer Member [%s] of Printer class [%s] "
                                + "not found.",
                        memberName, printerClass.getName()));
                continue;
            }

            final JsonProxyPrinter proxyPrinterClass =
                    printerMap.get(printerClass.getName());

            proxyPrinterClass
                    .setCupsClassMembers(printerClass.getMemberNames().size());

            initPrinterClassFromMember(proxyPrinterClass, proxyPrinterMember);

            if (LOGGER.isTraceEnabled()) {
                final String msg =
                        String.format("Printer Class [%s] URI [%s] [%s]",
                                printerClass.getName(),
                                printerClass.getPrinterUri(), commonMakeModel);
                LOGGER.trace(msg);
            }
        }
        return printers;
    }

    /**
     * Initializes a CUPS printer class from a class member.
     *
     * @param printerClass
     *            The CUPS printer class to initialize.
     * @param printerMember
     *            The CUPS printer class member to initialize from.
     */
    private static void initPrinterClassFromMember(
            final JsonProxyPrinter printerClass,
            final JsonProxyPrinter printerMember) {

        printerClass.setGroups(printerMember.getGroups());

        printerClass.setAutoMediaSource(printerMember.getAutoMediaSource());
        printerClass.setColorDevice(printerMember.getColorDevice());
        printerClass.setDuplexDevice(printerMember.getDuplexDevice());
        printerClass.setManualMediaSource(printerMember.getManualMediaSource());
        printerClass.setPpd(printerMember.getPpd());
        printerClass.setPpdVersion(printerMember.getPpdVersion());
        printerClass.setSheetCollated(printerMember.getSheetCollated());
        printerClass.setSheetUncollated(printerMember.getSheetUncollated());
    }

    /**
     * Gets the CUPS server {@link URL} of the printer {@link URI}. Example:
     * http://192.168.1.35:631
     *
     * @param uriPrinter
     *            The {@link URI} of the printer.
     * @return The CUPS server {@link URL}.
     * @throws MalformedURLException
     *             When the input is malformed.
     */
    private URL getCupsServerUrl(final URI uriPrinter)
            throws MalformedURLException {
        return new URL(InetUtils.URL_PROTOCOL_HTTP, uriPrinter.getHost(),
                uriPrinter.getPort(), "");
    }

    @Override
    public List<IppAttrGroup> getIppPrinterAttr(final String printerName,
            final URI printerUri) throws IppConnectException {

        final boolean isLocalCups = isLocalPrinter(printerUri);

        try {
            /*
             * If this is a REMOTE printer (e.g. printerUri:
             * ipp://192.168.1.36:631/printers/HL-2030-series) ...
             *
             * ... then we MUST use that URI to get the details (groups).
             */
            final URL urlCupsServer;

            if (isLocalCups) {
                urlCupsServer = getUrlDefaultServer();
            } else {
                urlCupsServer = getCupsServerUrl(printerUri);
            }
            return ippClient.send(urlCupsServer, isLocalCups,
                    IppOperationId.GET_PRINTER_ATTR,
                    reqGetPrinterAttr(printerUri.toString()));

        } catch (MalformedURLException e) {
            throw new SpException(e);
        }
    }

    @Override
    protected JsonProxyPrinter retrieveCupsPrinterDetails(
            final String printerName, final URI printerUri)
            throws IppConnectException {

        JsonProxyPrinter printer = null;

        final List<IppAttrGroup> response =
                getIppPrinterAttr(printerName, printerUri);

        if (response.size() > 1) {
            printer = createUserPrinter(response.get(1));
        }

        return printer;
    }

    @Override
    public ProxyPrinterOptGroupEnum getUiOptGroup(final String keywordIpp) {

        if (isIppKeywordPresent(IppDictJobTemplateAttr.ATTR_SET_UI_ADVANCED,
                keywordIpp)) {
            return ProxyPrinterOptGroupEnum.ADVANCED;
        }
        if (isIppKeywordPresent(IppDictJobTemplateAttr.ATTR_SET_UI_JOB,
                keywordIpp)) {
            return ProxyPrinterOptGroupEnum.JOB;
        }
        if (isIppKeywordPresent(IppDictJobTemplateAttr.ATTR_SET_UI_PAGE_SETUP,
                keywordIpp)) {
            return ProxyPrinterOptGroupEnum.PAGE_SETUP;
        }
        if (isIppKeywordPresent(IppDictJobTemplateAttr.ATTR_SET_REFERENCE_ONLY,
                keywordIpp)) {
            return ProxyPrinterOptGroupEnum.REFERENCE_ONLY;
        }
        return null;
    }

    /**
     * Checks if a keyword is present in array.
     *
     * @param keywords
     *            The keyword array.
     * @param keywordIpp
     *            The IPP keyword.
     * @return {@code null} when IPP keyword is not present in the array.
     */
    private boolean isIppKeywordPresent(final String[] keywords,
            final String keywordIpp) {
        for (final String keyword : keywords) {
            if (keyword.equals(keywordIpp)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Adds a {@link JsonProxyPrinterOptGroup} to {@link JsonProxyPrinter}.
     *
     * @param printer
     *            The printer to add the option group to.
     * @param ippAttrGroup
     *            All the (raw) IPP options of the printer.
     * @param groupId
     *            The ID of the option group.
     * @param attrKeywords
     *            The IPP keywords to add to the option group.
     */
    private void addUserPrinterOptGroup(final JsonProxyPrinter printer,
            final IppAttrGroup ippAttrGroup,
            final ProxyPrinterOptGroupEnum groupId,
            final String[] attrKeywords) {

        final ArrayList<JsonProxyPrinterOpt> printerOptions = new ArrayList<>();

        for (final String keyword : attrKeywords) {
            addOption(printer, ippAttrGroup, printerOptions, keyword);
        }

        if (!printerOptions.isEmpty()) {

            final JsonProxyPrinterOptGroup optGroup =
                    new JsonProxyPrinterOptGroup();

            optGroup.setOptions(printerOptions);
            optGroup.setGroupId(groupId);
            optGroup.setUiText(groupId.toString());

            printer.getGroups().add(optGroup);
        }
    }

    /**
     * Creates a {@link JsonProxyPrinter}, with a subset of IPP option
     * (attributes).
     * <p>
     * NOTE: Irrelevant (raw) IPP options from the {@link IppAttrGroup} input
     * parameter are not copied to the output {@link JsonProxyPrinter}.
     * </p>
     *
     * @param group
     *            The (raw) IPP printer options.
     * @return the {@link JsonProxyPrinter} or {@code null} when printer
     *         definition is not valid somehow.
     */
    private JsonProxyPrinter createUserPrinter(final IppAttrGroup group) {

        final JsonProxyPrinter printer = new JsonProxyPrinter();

        /*
         * Mantis #403: Cache CUPS printer-name internally as upper-case.
         */
        printer.setName(ProxyPrinterName.getDaoName(group
                .getAttrSingleValue(IppDictPrinterDescAttr.ATTR_PRINTER_NAME)));

        printer.setManufacturer(group.getAttrSingleValue(
                IppDictPrinterDescAttr.ATTR_PRINTER_MORE_INFO_MANUFACTURER));
        printer.setModelName(group.getAttrSingleValue(
                IppDictPrinterDescAttr.ATTR_PRINTER_MAKE_MODEL));

        // Device URI
        final URI deviceUri;

        final String deviceUriValue;

        try {
            deviceUriValue = group
                    .getAttrSingleValue(IppDictPrinterDescAttr.ATTR_DEVICE_URI);

            if (deviceUriValue == null) {
                deviceUri = null;
            } else {
                deviceUri = new URI(deviceUriValue);
            }
        } catch (URISyntaxException e) {
            throw new SpException(e.getMessage());
        }

        printer.setDeviceUri(deviceUri);

        // Printer URI
        final URI printerUri;

        try {
            final String uriValue = group.getAttrSingleValue(
                    IppDictPrinterDescAttr.ATTR_PRINTER_URI_SUPPORTED);
            if (uriValue == null) {
                // Mantis #585
                LOGGER.warn(String.format(
                        "Skipping printer [%s] model [%s] device URI [%s]"
                                + ": no printer URI",
                        printer.getName(),
                        StringUtils.defaultString(printer.getModelName(), ""),
                        StringUtils.defaultString(deviceUriValue, "")));
                return null;
            } else {
                printerUri = new URI(uriValue);
            }
        } catch (URISyntaxException e) {
            throw new SpException(e.getMessage());
        }

        printer.setPrinterUri(printerUri);

        //
        printer.setInfo(group
                .getAttrSingleValue(IppDictPrinterDescAttr.ATTR_PRINTER_INFO));

        printer.setAcceptingJobs(group.getAttrSingleValue(
                IppDictPrinterDescAttr.ATTR_PRINTER_IS_ACCEPTING_JOBS,
                IppBoolean.TRUE).equals(IppBoolean.TRUE));

        printer.setLocation(group.getAttrSingleValue(
                IppDictPrinterDescAttr.ATTR_PRINTER_LOCATION));
        printer.setState(group
                .getAttrSingleValue(IppDictPrinterDescAttr.ATTR_PRINTER_STATE));
        printer.setStateChangeTime(group.getAttrSingleValue(
                IppDictPrinterDescAttr.ATTR_PRINTER_STATE_CHANGE_TIME));
        printer.setStateReasons(group.getAttrSingleValue(
                IppDictPrinterDescAttr.ATTR_PRINTER_STATE_REASONS));

        printer.setColorDevice(group
                .getAttrSingleValue(IppDictPrinterDescAttr.ATTR_COLOR_SUPPORTED,
                        IppBoolean.FALSE)
                .equals(IppBoolean.TRUE));

        printer.setDuplexDevice(Boolean.FALSE);

        // ----------------
        ArrayList<JsonProxyPrinterOptGroup> printerOptGroups =
                new ArrayList<>();
        printer.setGroups(printerOptGroups);

        // ---------------------
        // Options
        // ---------------------
        addUserPrinterOptGroup(printer, group,
                ProxyPrinterOptGroupEnum.PAGE_SETUP,
                IppDictJobTemplateAttr.ATTR_SET_UI_PAGE_SETUP);

        addUserPrinterOptGroup(printer, group, ProxyPrinterOptGroupEnum.JOB,
                IppDictJobTemplateAttr.ATTR_SET_UI_JOB);

        addUserPrinterOptGroup(printer, group,
                ProxyPrinterOptGroupEnum.ADVANCED,
                IppDictJobTemplateAttr.ATTR_SET_UI_ADVANCED);

        addUserPrinterOptGroup(printer, group,
                ProxyPrinterOptGroupEnum.REFERENCE_ONLY,
                IppDictJobTemplateAttr.ATTR_SET_REFERENCE_ONLY);

        /*
         * TODO: The PPD values are used to see if a printer "changed", but this
         * becomes obsolete as soon as we utilize event subscription fully.
         *
         * For now make sure PPD values are NOT NULL !!
         *
         * "pcfilename":"HL1250.PPD", "FileVersion":"1.1"
         */
        printer.setPpd("");
        printer.setPpdVersion("");

        return printer;
    }

    @Override
    public void localizePrinterOptChoices(final Locale locale,
            final String attrKeyword,
            final List<JsonProxyPrinterOptChoice> choices) {

        final boolean isMedia = IppDictJobTemplateAttr.isMediaAttr(attrKeyword);
        for (final JsonProxyPrinterOptChoice optChoice : choices) {
            localizePrinterOptChoice(locale, attrKeyword, isMedia, optChoice);
        }
    }

    @Override
    public void localizePrinterOptChoice(final Locale locale,
            final String attrKeyword,
            final JsonProxyPrinterOptChoice optChoice) {

        localizePrinterOptChoice(locale, attrKeyword,
                IppDictJobTemplateAttr.isMediaAttr(attrKeyword), optChoice);
    }

    @Override
    public String localizePrinterOptValue(final Locale locale,
            final String attrKeyword, final String value) {
        return localizePrinterOptChoice(locale, attrKeyword,
                IppDictJobTemplateAttr.isMediaAttr(attrKeyword), value);
    }

    @Override
    public String localizePrinterOpt(final Locale locale,
            final String attrKeyword) {

        final String msgKey =
                String.format("%s%s", LOCALIZE_IPP_ATTR_PREFIX, attrKeyword);

        final String customOpt = localizeCustomIpp(msgKey, locale);

        if (customOpt == null) {
            return localizeWithDefault(locale, msgKey, attrKeyword);
        } else {
            return customOpt;
        }
    }

    @Override
    public void localizePrinterOpt(final Locale locale,
            final JsonProxyPrinterOpt option) {

        final String attrKeyword = option.getKeyword();

        option.setUiText(localizePrinterOpt(locale, attrKeyword));

        localizePrinterOptChoices(locale, attrKeyword, option.getChoices());
    }

    @Override
    public List<Pair<String, String>> getJobTicketOptionsExtUiText(
            final Locale locale, final Map<String, String> optionMap) {

        if (optionMap == null) {
            return null;
        }

        List<Pair<String, String>> list = new ArrayList<>();

        for (final Entry<String, String> entry : optionMap.entrySet()) {

            if (!IppDictJobTemplateAttr.isCustomExtAttr(entry.getKey())
                    || IppDictJobTemplateAttr
                            .isCustomExtAttrValueNone(entry.getValue())) {
                continue;
            }

            if (list == null) {
                list = new ArrayList<>();
            }

            list.add(new ImmutablePair<String, String>(
                    localizePrinterOpt(locale, entry.getKey()),
                    localizePrinterOptValue(locale, entry.getKey(),
                            entry.getValue())));
        }

        return list;
    }

    @Override
    public String getJobTicketOptionsExtHtml(final Locale locale,
            final Map<String, String> optionMap) {

        final List<Pair<String, String>> pairs =
                getJobTicketOptionsExtUiText(locale, optionMap);

        if (pairs == null || pairs.isEmpty()) {
            return null;
        }

        final StringBuilder extOpts = new StringBuilder();

        for (final Pair<String, String> pair : pairs) {

            if (extOpts.length() > 0) {
                extOpts.append(" • ");
            }
            extOpts.append(pair.getKey()).append(" ").append(pair.getValue());
        }
        return extOpts.toString();
    }

    @Override
    public String getJobTicketOptionsUiText(final Locale locale,
            final String[] ippOptionKeys, final IppOptionMap optionMap) {

        if (optionMap == null) {
            return null;
        }

        final StringBuilder uiText = new StringBuilder();

        for (final String optKey : ippOptionKeys) {

            final String optValue = optionMap.getOptionValue(optKey);

            if (optValue == null) {
                continue;
            }

            if (IppDictJobTemplateAttr.ORG_SAVAPAGE_ATTR_FINISHINGS_EXT
                    .equals(optKey)
                    && IppKeyword.ORG_SAVAPAGE_ATTR_FINISHINGS_EXTERNAL_NONE
                            .equals(optValue)) {
                continue;
            }
            if (IppDictJobTemplateAttr.ORG_SAVAPAGE_ATTR_COVER_TYPE
                    .equals(optKey)
                    && IppKeyword.ORG_SAVAPAGE_ATTR_COVER_TYPE_NO_COVER
                            .equals(optValue)) {
                continue;
            }
            if (IppDictJobTemplateAttr.ORG_SAVAPAGE_ATTR_COVER_TYPE_COLOR
                    .equals(optKey) && !optionMap.hasCoverType()) {
                continue;
            }

            final JsonProxyPrinterOptChoice choice =
                    new JsonProxyPrinterOptChoice();
            choice.setChoice(optValue);

            this.localizePrinterOptChoice(locale, optKey, choice);

            uiText.append(" ").append(choice.getUiText());

        }
        if (uiText.length() > 0) {
            return uiText.toString().trim();
        }

        return null;
    }

    /**
     *
     * Localizes the text in a printer option choice.
     *
     * @param locale
     *            The {@link Locale}.
     * @param attrKeyword
     *            The IPP option keyword.
     * @param isMedia
     *            {@code true] when this the "media" attribute. @param optChoice
     *            The {@link JsonProxyPrinterOptChoice} object.
     */
    private void localizePrinterOptChoice(final Locale locale,
            final String attrKeyword, final boolean isMedia,
            final JsonProxyPrinterOptChoice optChoice) {

        optChoice.setUiText(localizePrinterOptChoice(locale, attrKeyword,
                isMedia, optChoice.getChoice()));
    }

    /**
     *
     * Localizes the text in a printer option choice.
     *
     * @param locale
     *            The {@link Locale}.
     * @param attrKeyword
     *            The IPP option keyword.
     * @param isMedia
     *            {@code true] when this the "media" attribute. @param choice
     *            The {@link JsonProxyPrinterOptChoice} object. @return The
     *            localized choice text.
     */
    private String localizePrinterOptChoice(final Locale locale,
            final String attrKeyword, final boolean isMedia,
            final String choice) {

        String choiceTextDefault = choice;

        final String attrKeywordWrk;

        if (isMedia) {

            attrKeywordWrk = IppDictJobTemplateAttr.ATTR_MEDIA;

            final IppMediaSizeEnum ippMediaSize = IppMediaSizeEnum.find(choice);

            if (ippMediaSize == null) {
                return choiceTextDefault;
            }

            final MediaSizeName mediaSizeName = ippMediaSize.getMediaSizeName();

            choiceTextDefault = localizeWithDefault(locale,
                    String.format("%s%s-%s", LOCALIZE_IPP_ATTR_PREFIX,
                            attrKeywordWrk, mediaSizeName.toString()),
                    mediaSizeName.toString());
        } else {
            attrKeywordWrk = attrKeyword;
        }

        final String key = String.format("%s%s-%s", LOCALIZE_IPP_ATTR_PREFIX,
                attrKeywordWrk, choice);
        final String customChoice = localizeCustomIpp(key, locale);

        final String finalChoice;

        if (customChoice == null) {
            finalChoice = localizeWithDefault(locale, key, choiceTextDefault);
        } else {
            finalChoice = customChoice;
        }

        return finalChoice;
    }

    @Override
    public String localizeMnemonic(final MediaSizeName mediaSizeName) {
        return localizeWithDefault(ServiceContext.getLocale(),
                String.format("%s%s-%s", LOCALIZE_IPP_ATTR_PREFIX,
                        IppDictJobTemplateAttr.ATTR_MEDIA,
                        mediaSizeName.toString()),
                mediaSizeName.toString());
    }

    @Override
    public void localize(final Locale locale,
            final JsonPrinterDetail printerDetail) {

        for (final JsonProxyPrinterOptGroup optGroup : printerDetail
                .getGroups()) {

            if (optGroup.getGroupId() == ProxyPrinterOptGroupEnum.ADVANCED) {
                optGroup.setUiText(localize(locale, "ipp-cat-advanced"));
            } else if (optGroup.getGroupId() == ProxyPrinterOptGroupEnum.JOB) {
                optGroup.setUiText(localize(locale, "ipp-cat-job"));
            } else if (optGroup
                    .getGroupId() == ProxyPrinterOptGroupEnum.PAGE_SETUP) {
                optGroup.setUiText(localize(locale, "ipp-cat-page-setup"));
            }

            for (final JsonProxyPrinterOpt option : optGroup.getOptions()) {
                localizePrinterOpt(locale, option);
                attachCssIcons(locale, option);
            }
        }
    }

    /**
     * Attaches CSS icon class to each of the printer option choices.
     *
     * @param locale
     *            The locale.
     * @param option
     *            The printer option.
     */
    private void attachCssIcons(final Locale locale,
            final JsonProxyPrinterOpt option) {

        final String cssDefault = localizeCustomIpp(String.format("%s%s",
                LOCALIZE_IPP_ICON_PREFIX, option.getKeyword()), locale);

        for (final JsonProxyPrinterOptChoice choice : option.getChoices()) {

            final String cssChoice =
                    localizeCustomIpp(
                            String.format("%s%s-%s", LOCALIZE_IPP_ICON_PREFIX,
                                    option.getKeyword(), choice.getChoice()),
                            locale);

            if (cssChoice == null) {
                choice.setUiIconClass(cssDefault);
            } else {
                choice.setUiIconClass(cssChoice);
            }
        }
    }

    /**
     * Adds an {@code IppBoolean} option to the printerOptions parameter.
     *
     * @param printerOptions
     *            The list of {@link JsonProxyPrinterOpt} to add the option to.
     * @param attrKeyword
     *            The IPP attribute keyword.
     * @param defaultChoice
     *            The default choice.
     */
    private void addOptionBoolean(
            final ArrayList<JsonProxyPrinterOpt> printerOptions,
            final String attrKeyword, final boolean defaultChoice) {

        final JsonProxyPrinterOpt option = new JsonProxyPrinterOpt();

        option.setKeyword(attrKeyword);
        option.setUiText(attrKeyword);
        final ArrayList<JsonProxyPrinterOptChoice> choices = new ArrayList<>();

        JsonProxyPrinterOptChoice choice = new JsonProxyPrinterOptChoice();
        choice.setChoice(IppBoolean.FALSE);
        choice.setUiText(IppBoolean.FALSE);
        choices.add(choice);

        choice = new JsonProxyPrinterOptChoice();
        choice.setChoice(IppBoolean.TRUE);
        choice.setUiText(IppBoolean.TRUE);
        choices.add(choice);

        option.setChoices(choices);

        if (defaultChoice) {
            option.setDefchoice(IppBoolean.TRUE);
        } else {
            option.setDefchoice(IppBoolean.FALSE);
        }

        printerOptions.add(option);
    }

    /**
     * Adds an option to the printerOptions parameter.
     * <p>
     * See Mantis #185.
     * </p>
     *
     * @param printer
     *            The {@link JsonProxyPrinter}.
     * @param group
     *            The {@link IppAttrGroup} with all the (raw) IPP options of the
     *            printer.
     * @param printerOptions
     *            The list of {@link JsonProxyPrinterOpt} to add the option to.
     * @param attrKeyword
     *            The IPP attribute keyword.
     */
    private void addOption(final JsonProxyPrinter printer,
            final IppAttrGroup group,
            final ArrayList<JsonProxyPrinterOpt> printerOptions,
            final String attrKeyword) {

        /*
         * Skip exclusive PPDE options.
         */
        if (attrKeyword.equals(
                IppDictJobTemplateAttr.ORG_SAVAPAGE_ATTR_FINISHINGS_JOG_OFFSET)) {
            return;
        }

        /*
         * Handle internal attributes first.
         */
        if (attrKeyword.equals(
                IppDictJobTemplateAttr.ORG_SAVAPAGE_ATTR_INT_PAGE_ROTATE180)) {
            this.addOptionBoolean(printerOptions, attrKeyword, false);
            return;
        }

        /*
         * INVARIANT: More than one (1) choice.
         */
        final IppAttrValue attrChoice =
                group.getAttrValue(IppDictJobTemplateAttr.attrName(attrKeyword,
                        ApplEnum.SUPPORTED));

        if (attrChoice == null || attrChoice.getValues().size() < 2) {
            return;
        }

        /*
         * If no default found, use the first from the list of choices.
         */
        String defChoice = group.getAttrSingleValue(
                IppDictJobTemplateAttr.attrName(attrKeyword, ApplEnum.DEFAULT));

        if (defChoice == null) {
            defChoice = attrChoice.getValues().get(0);
        }

        final String txtKeyword = attrKeyword;

        final JsonProxyPrinterOpt option = new JsonProxyPrinterOpt();

        option.setKeyword(attrKeyword);
        option.setUiText(txtKeyword);

        String defChoiceFound = attrChoice.getValues().get(0);

        final boolean isMedia = IppDictJobTemplateAttr.isMediaAttr(attrKeyword);

        final boolean isMediaSource =
                attrKeyword.equals(IppDictJobTemplateAttr.ATTR_MEDIA_SOURCE);

        final boolean isNup =
                attrKeyword.equals(IppDictJobTemplateAttr.ATTR_NUMBER_UP);

        final boolean isSides =
                attrKeyword.equals(IppDictJobTemplateAttr.ATTR_SIDES);

        final boolean isSheetCollate =
                attrKeyword.equals(IppDictJobTemplateAttr.ATTR_SHEET_COLLATE);

        boolean sheetCollated = false;
        boolean sheetUncollated = false;
        boolean isDuplexPrinter = false;
        boolean hasManualMediaSource = false;
        boolean hasAutoMediaSource = false;

        for (final String choice : attrChoice.getValues()) {

            // Do not add "auto" media source, but set an indication (see
            // below).
            if (isMediaSource
                    && choice.equalsIgnoreCase(IppKeyword.MEDIA_SOURCE_AUTO)) {
                hasAutoMediaSource = true;
                continue;
            }

            /*
             * Mantis #185: Limit IPP n-up to max 9 (1 character).
             */
            if (isNup && choice.length() > 1) {
                continue;
            }

            // Skip media unknown in IppMediaSizeEnum.
            if (isMedia && IppMediaSizeEnum.find(choice) == null) {
                continue;
            }

            if (isSides
                    && !choice.equalsIgnoreCase(IppKeyword.SIDES_ONE_SIDED)) {
                isDuplexPrinter = true;
            } else if (isMediaSource && choice
                    .equalsIgnoreCase(IppKeyword.MEDIA_SOURCE_MANUAL)) {
                hasManualMediaSource = true;
            } else if (isSheetCollate) {
                if (choice
                        .equalsIgnoreCase(IppKeyword.SHEET_COLLATE_COLLATED)) {
                    sheetCollated = true;
                } else if (choice.equalsIgnoreCase(
                        IppKeyword.SHEET_COLLATE_UNCOLLATED)) {
                    sheetUncollated = true;
                }
            }

            if (choice.equals(defChoice)) {
                defChoiceFound = defChoice;
            }

            option.addChoice(choice, choice);
        }

        option.setDefchoice(defChoiceFound);
        option.setDefchoiceIpp(defChoiceFound);

        if (!option.getChoices().isEmpty()) {
            // A single media-source choice is added, but single choices of
            // other IPP attributes are not. See Mantis #1171.
            if (isMediaSource || option.getChoices().size() > 1) {
                printerOptions.add(option);
            }
        }

        if (isSides) {
            printer.setDuplexDevice(Boolean.valueOf(isDuplexPrinter));
        } else if (isMediaSource) {
            printer.setAutoMediaSource(Boolean.valueOf(hasAutoMediaSource));
            printer.setManualMediaSource(Boolean.valueOf(hasManualMediaSource));
        } else if (isSheetCollate) {
            printer.setSheetCollated(sheetCollated);
            printer.setSheetUncollated(sheetUncollated);
        }

    }

    @Override
    public List<JsonProxyPrintJob> retrievePrintJobs(final String printerName,
            final Set<Integer> jobIds) throws IppConnectException {

        final List<JsonProxyPrintJob> jobs = new ArrayList<>();

        final JsonProxyPrinter proxyPrinter =
                this.getCachedPrinter(printerName);

        if (proxyPrinter != null) {

            final String printerUri = proxyPrinter.getPrinterUri().toString();

            final URL urlCupsServer;

            try {
                urlCupsServer =
                        this.getCupsServerUrl(proxyPrinter.getPrinterUri());
            } catch (MalformedURLException e) {
                throw new IppConnectException(e);
            }

            for (final Integer jobId : jobIds) {
                final JsonProxyPrintJob job = retrievePrintJobUri(urlCupsServer,
                        printerUri, null, jobId);
                if (job != null) {
                    jobs.add(job);
                }
            }

        } else {
            /*
             * Remote printer might not be present when remote CUPS is down, or
             * when connection is refused.
             */
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("Proxy printer [" + printerName
                        + "] not found in cache: possibly due "
                        + "to remote CUPS connection problem.");
            }
        }

        return jobs;
    }

    /**
     * Gets the CUPS job-id from job-uri.
     *
     * @param jobUri
     *            The URI. For example: ipp://192.168.1.200:631/jobs/65
     * @return The job id.
     */
    private static String jobIdFromJobUri(final String jobUri) {
        return jobUri.substring(jobUri.lastIndexOf('/') + 1);
    }

    @Override
    public boolean isCupsPpdPresent(final URI printerURI)
            throws IppConnectException {

        final URL urlCupsServer;

        try {
            urlCupsServer = getCupsServerUrl(printerURI);
        } catch (MalformedURLException e) {
            throw new SpException(e.getMessage());
        }

        final List<IppAttrGroup> ippRequest =
                new IppReqCupsGetPpd(printerURI).build();

        final List<IppAttrGroup> response = new ArrayList<>();

        final IppStatusCode statusCode = ippClient.send(urlCupsServer,
                IppOperationId.CUPS_GET_PPD, ippRequest, response);

        if (statusCode == IppStatusCode.OK) {
            /*
             * The PPD file follows the end of the IPP response.
             */
            return true;
        } else if (statusCode == IppStatusCode.CLI_NOTFND) {
            // PPD file does not exist: e.g. Raw Printer
            return false;
        }

        return false;
    }

    @Override
    public URL getCupsPpdUrl(final String printerName) {

        try {
            return new URL(this.getUrlDefaultServer().toString()
                    .concat(URL_PATH_CUPS_PRINTERS).concat("/")
                    .concat(printerName).concat(".ppd"));
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    @Override
    public JsonProxyPrintJob sendPdfToPrinter(
            final AbstractProxyPrintReq request,
            final JsonProxyPrinter jsonPrinter, final String user,
            final PdfCreateInfo createInfo) throws IppConnectException {

        final File filePdf = createInfo.getPdfFile();

        final URL urlCupsServer;

        try {
            urlCupsServer = getCupsServerUrl(jsonPrinter.getPrinterUri());
        } catch (MalformedURLException e) {
            throw new SpException(e.getMessage());
        }

        final String jobNameWork;

        if (StringUtils.isBlank(request.getJobName())) {
            jobNameWork =
                    String.format("%s-%s", CommunityDictEnum.SAVAPAGE.getWord(),
                            DateUtil.formattedDateTime(
                                    ServiceContext.getTransactionDate()));
        } else {
            jobNameWork = request.getJobName();
        }

        /*
         * Client-side collate?
         */
        final boolean clientSideCollate;

        if (request.getNumberOfCopies() > 1) {

            if (request.isCollate()) {
                clientSideCollate =
                        BooleanUtils.isFalse(jsonPrinter.getSheetCollated());
            } else {
                clientSideCollate =
                        BooleanUtils.isFalse(jsonPrinter.getSheetUncollated());
            }

            if (!clientSideCollate) {

                final String collateKeyword;

                if (request.isCollate()) {
                    collateKeyword = IppKeyword.SHEET_COLLATE_COLLATED;
                } else {
                    collateKeyword = IppKeyword.SHEET_COLLATE_UNCOLLATED;
                }
                request.getOptionValues().put(
                        IppDictJobTemplateAttr.ATTR_SHEET_COLLATE,
                        collateKeyword);
            }

        } else {
            clientSideCollate = false;
        }

        // Save number of copies.
        final int numberOfCopiesSaved = request.getNumberOfCopies();

        /*
         * Send the IPP Print Job request.
         */
        File pdfFileCollected = null;
        final List<IppAttrGroup> response;

        try {

            final File pdfFileToPrint;

            if (clientSideCollate) {

                pdfFileCollected = new File(String.format("%s-collected",
                        filePdf.getCanonicalPath()));

                final int nTotCollectedPages =
                        PdfPrintCollector.collect(request, request.isCollate(),
                                filePdf, pdfFileCollected);

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(String.format(
                            "Collected PDF [%s] pages [%d], "
                                    + "copies [%d], collate [%s], "
                                    + "n-up [%d], duplex [%s] -> pages [%d]",
                            request.getJobName(), request.getNumberOfPages(),
                            request.getNumberOfCopies(),
                            Boolean.toString(request.isCollate()),
                            request.getNup(),
                            Boolean.toString(request.isDuplex()),
                            nTotCollectedPages));
                }

                pdfFileToPrint = pdfFileCollected;

                // Trick the request so IPP Print Job request sets the number of
                // copies to one (1).
                request.setNumberOfCopies(1);

            } else {
                pdfFileCollected = null;
                pdfFileToPrint = filePdf;
            }

            final IppOperationId ippOperation = IppOperationId.PRINT_JOB;

            final List<IppAttrGroup> ippRequest =
                    new IppReqPrintJob(request, pdfFileToPrint, jsonPrinter,
                            user, jobNameWork, jobNameWork, createInfo).build();

            response = ippClient.send(urlCupsServer, ippOperation, ippRequest,
                    pdfFileToPrint);

            ProxyPrintLogger.log(ippOperation, ippRequest, response);

        } catch (IOException e) {

            throw new SpException(e.getMessage(), e);

        } finally {

            if (pdfFileCollected != null) {
                pdfFileCollected.delete();
            }
        }

        // Restore number of copies.
        request.setNumberOfCopies(numberOfCopiesSaved);

        /*
         * Collect the PrintJob data.
         */
        final IppAttrGroup group = response.get(1);

        // The job-uri can be null.
        final String jobUri =
                group.getAttrSingleValue(IppDictOperationAttr.ATTR_JOB_URI);

        // The job-id can be null.
        String jobId =
                group.getAttrSingleValue(IppDictOperationAttr.ATTR_JOB_ID);

        if (jobId == null) {
            if (jobUri == null) {
                throw new SpException("job id could not be determined.");
            }
            jobId = jobIdFromJobUri(jobUri);
        }

        /*
         * Retrieve the JOB status from CUPS.
         *
         * NOTE: if the "media-source" is "manual", the printJob is returned
         * with status "processing".
         */
        final JsonProxyPrintJob printJob = retrievePrintJobUri(urlCupsServer,
                null, jobUri, Integer.valueOf(jobId, 10));

        // Add extra info to print job state.
        printJob.setUser(user);
        printJob.setTitle(request.getJobName());

        return printJob;
    }

    @Override
    protected void printPdf(final AbstractProxyPrintReq request,
            final JsonProxyPrinter jsonPrinter, final String user,
            final PdfCreateInfo createInfo, final DocLog docLog)
            throws IppConnectException, DocStoreException {

        final JsonProxyPrintJob printJob =
                this.sendPdfToPrinter(request, jsonPrinter, user, createInfo);

        collectPrintOutData(request, docLog, jsonPrinter, printJob, createInfo);

        final DocStoreTypeEnum docStoreType;

        if (request.isArchive()) {

            docStoreType = DocStoreTypeEnum.ARCHIVE;

        } else if (!request.isDisableJournal()
                && !jsonPrinter.isJournalDisabled()
                && docStoreService().isEnabled(DocStoreTypeEnum.JOURNAL,
                        DocStoreBranchEnum.OUT_PRINT)
                && accessControlService().hasAccess(docLog.getUser(),
                        ACLOidEnum.U_PRINT_JOURNAL)) {
            docStoreType = DocStoreTypeEnum.JOURNAL;

        } else {
            docStoreType = null;
        }

        if (docStoreType != null) {
            docStoreService().store(docStoreType, request, docLog, createInfo);
        }
    }

    /**
     * Gets the default {@link JsonProxyPrinter}.
     *
     * @return The default {@link JsonProxyPrinter} or {@code null} when not
     *         found.
     * @throws IppConnectException
     *             When IPP connection error.
     */
    private JsonProxyPrinter getCupsDefaultPrinter()
            throws IppConnectException {

        final List<IppAttrGroup> request = new ArrayList<>();
        request.add(createOperationGroup());

        final List<IppAttrGroup> response = new ArrayList<>();

        final IppStatusCode statusCode = ippClient.send(getUrlDefaultServer(),
                IppOperationId.CUPS_GET_DEFAULT, request, response);

        if (statusCode == IppStatusCode.CLI_NOTFND) {
            return null;
        }

        return createUserPrinter(response.get(1));
    }

    @Override
    public boolean cancelPrintJob(final PrintOut printOut)
            throws IppConnectException {

        final String printerName = printOut.getPrinter().getPrinterName();
        final String requestingUserName =
                printOut.getDocOut().getDocLog().getUser().getUserId();

        final JsonProxyPrinter proxyPrinter =
                this.getCachedPrinter(printerName);

        if (proxyPrinter == null) {
            throw new IllegalStateException(
                    String.format("Printer [%s] not found.", printerName));
        }

        final String printerUri = proxyPrinter.getPrinterUri().toString();
        final URL urlCupsServer;

        try {
            urlCupsServer = this.getCupsServerUrl(proxyPrinter.getPrinterUri());
        } catch (MalformedURLException e) {
            throw new SpException(e);
        }

        final List<IppAttrGroup> response = new ArrayList<>();

        final IppStatusCode statusCode = ippClient.send(urlCupsServer,
                IppOperationId.CANCEL_JOB, reqCancelJobAttr(printerUri,
                        printOut.getCupsJobId(), requestingUserName),
                response);

        return statusCode == IppStatusCode.OK;
    }

    /**
     * Retrieves the print job data using the URI of the printer or the job.
     *
     * @param urlCupsServer
     *            The {@link URL} of the CUPS server.
     * @param uriPrinter
     *            If {@code null} uriJob is used.
     * @param uriJob
     *            If {@code null} uriPrinter and jobId is used.
     * @param jobId
     *            CUPS job id.
     * @return {@code null} when print job is not found.
     * @throws IppConnectException
     *             When connection error.
     */
    private JsonProxyPrintJob retrievePrintJobUri(final URL urlCupsServer,
            final String uriPrinter, final String uriJob, final Integer jobId)
            throws IppConnectException {

        final List<IppAttrGroup> response = new ArrayList<>();

        final IppStatusCode statusCode;

        if (uriJob == null) {
            statusCode =
                    ippClient.send(urlCupsServer, IppOperationId.GET_JOB_ATTR,
                            reqGetJobAttr(uriPrinter, jobId), response);
        } else {
            statusCode =
                    ippClient.send(urlCupsServer, IppOperationId.GET_JOB_ATTR,
                            reqGetJobAttr(uriJob), response);
        }

        final JsonProxyPrintJob job;

        if (statusCode == IppStatusCode.OK && response.size() > 1) {

            job = new JsonProxyPrintJob();

            job.setJobId(jobId);

            final IppAttrGroup group = response.get(1);

            job.setDest(group.getAttrSingleValue(
                    IppDictJobDescAttr.ATTR_JOB_PRINTER_URI));
            job.setTitle(
                    group.getAttrSingleValue(IppDictJobDescAttr.ATTR_JOB_NAME));

            job.setJobState(Integer.parseInt(
                    group.getAttrSingleValue(IppDictJobDescAttr.ATTR_JOB_STATE),
                    NumberUtil.RADIX_10));
            job.setJobStateMessage(group.getAttrSingleValue(
                    IppDictJobDescAttr.ATTR_JOB_STATE_MESSAGE));
            job.setJobStateReasons(group
                    .getAttrValues(IppDictJobDescAttr.ATTR_JOB_STATE_REASONS));

            job.setCreationTime(Integer.valueOf(
                    group.getAttrSingleValue(
                            IppDictJobDescAttr.ATTR_TIME_AT_CREATION),
                    NumberUtil.RADIX_10));

            final String value = group.getAttrSingleValue(
                    IppDictJobDescAttr.ATTR_TIME_AT_COMPLETED, "");

            if (StringUtils.isNotBlank(value)) {
                job.setCompletedTime(
                        Integer.parseInt(value, NumberUtil.RADIX_10));
            }

        } else {
            job = null;
        }
        // ---------------
        return job;
    }

    /**
     * @return Value of IPP "requesting-user-name" keyword for CUPS event
     *         subscription.
     */
    private static String getCUPSEventSubscrRequestingUserName() {
        return ConfigManager.getProcessUserName();
    }

    /**
     * Gets the value of IPP "notify-recipient-uri" keyword for CUPS even push
     * notification.
     * <p>
     * Example: {@code savapage:localhost:8631}
     * </p>
     *
     * @return IPP keyword value.
     */
    private static String getCUPSPushSubscrNotifyRecipientUri() {
        return String.format("%s:localhost:%s", ConfigManager.getCupsNotifier(),
                ConfigManager.getServerPort());
    }

    @Override
    public boolean startCUPSPushEventSubscription()
            throws IppConnectException, IppSyntaxException {

        if (!ConfigManager.isCupsPushNotification()) {
            return false;
        }

        final String requestingUser = getCUPSEventSubscrRequestingUserName();
        final String recipientUri = getCUPSPushSubscrNotifyRecipientUri();
        final String leaseSeconds = ConfigManager.instance().getConfigValue(
                Key.CUPS_IPP_NOTIFICATION_PUSH_NOTIFY_LEASE_DURATION);

        /*
         * Step 1: Get the existing printer subscriptions for requestingUser.
         */
        List<IppAttrGroup> response = new ArrayList<>();

        IppStatusCode statusCode =
                ippClient.send(this.getUrlDefaultServer(),
                        IppOperationId.GET_SUBSCRIPTIONS,
                        this.reqGetPrinterSubscriptions(
                                SUBSCRIPTION_PRINTER_URI, requestingUser),
                        response);

        /*
         * NOTE: When this is a first-time subscription it is possible that
         * there are NO subscriptions for the user, this will result in status
         * IppStatusCode.CLI_NOTFND or IppStatusCode.CLI_NOTPOS.
         */
        if (statusCode != IppStatusCode.OK
                && statusCode != IppStatusCode.CLI_NOTFND
                && statusCode != IppStatusCode.CLI_NOTPOS) {
            throw new IppSyntaxException(statusCode.toString());
        }

        /*
         * Step 2: Renew only our OWN printer subscription.
         */
        boolean isRenewed = false;

        for (final IppAttrGroup group : response) {

            if (group.getDelimiterTag() != IppDelimiterTag.SUBSCRIPTION_ATTR) {
                continue;
            }

            /*
             * There might be other subscription that are NOT ours (like native
             * CUPS descriptions).
             */
            final String recipientUriFound = group.getAttrSingleValue(
                    IppDictSubscriptionAttr.ATTR_NOTIFY_RECIPIENT_URI);

            if (recipientUriFound == null
                    || !recipientUri.equals(recipientUriFound)) {
                continue;
            }

            final String subscriptionId = group.getAttrSingleValue(
                    IppDictSubscriptionAttr.ATTR_NOTIFY_SUBSCRIPTION_ID);

            ippClient.send(getUrlDefaultServer(),
                    IppOperationId.RENEW_SUBSCRIPTION,
                    reqRenewPrinterSubscription(requestingUser, subscriptionId,
                            leaseSeconds));

            isRenewed = true;
        }

        /*
         * ... or create when not renewed.
         */
        if (!isRenewed) {
            ippClient.send(getUrlDefaultServer(),
                    IppOperationId.CREATE_PRINTER_SUBSCRIPTIONS,
                    this.reqCreatePrinterPushSubscriptions(requestingUser,
                            recipientUri, leaseSeconds));
        }

        return true;
    }

    @Override
    public void stopCUPSEventSubscription()
            throws IppConnectException, IppSyntaxException {

        final String requestingUser = getCUPSEventSubscrRequestingUserName();
        final String recipientUri = getCUPSPushSubscrNotifyRecipientUri();
        /*
         * Step 1: Get the existing printer subscriptions for requestingUser.
         */
        final List<IppAttrGroup> response = new ArrayList<>();

        final IppStatusCode statusCode = ippClient.send(getUrlDefaultServer(),
                IppOperationId.GET_SUBSCRIPTIONS, reqGetPrinterSubscriptions(
                        SUBSCRIPTION_PRINTER_URI, requestingUser),
                response);
        /*
         * NOTE: it is possible that there are NO subscriptions for the user,
         * this will given an IPP_NOT_FOUND.
         */
        if (statusCode != IppStatusCode.OK
                && statusCode != IppStatusCode.CLI_NOTFND) {
            throw new IppSyntaxException(statusCode.toString());
        }

        /*
         * Step 2: Cancel only our OWN printer subscription.
         */
        for (final IppAttrGroup group : response) {

            if (group.getDelimiterTag() != IppDelimiterTag.SUBSCRIPTION_ATTR) {
                continue;
            }

            /*
             * There might be other subscription that are NOT ours (like native
             * CUPS descriptions).
             */
            final String recipientUriFound = group.getAttrSingleValue(
                    IppDictSubscriptionAttr.ATTR_NOTIFY_RECIPIENT_URI);

            if (recipientUriFound == null
                    || !recipientUri.equals(recipientUriFound)) {
                continue;
            }

            final String subscriptionId = group.getAttrSingleValue(
                    IppDictSubscriptionAttr.ATTR_NOTIFY_SUBSCRIPTION_ID);

            ippClient.send(getUrlDefaultServer(),
                    IppOperationId.CANCEL_SUBSCRIPTION,
                    reqCancelPrinterSubscription(requestingUser,
                            subscriptionId));
        }
    }

    /**
     *
     * @param requestingUser
     *            The requesting user.
     * @param subscriptionId
     *            The subscription id.
     *
     * @return The IPP request.
     */
    private List<IppAttrGroup> reqCancelPrinterSubscription(
            final String requestingUser, final String subscriptionId) {

        List<IppAttrGroup> attrGroups = new ArrayList<>();

        IppAttrGroup group = null;
        AbstractIppDict dict = null;

        /*
         * Group 1: Operation Attributes
         */
        group = createOperationGroup();
        attrGroups.add(group);

        dict = IppDictOperationAttr.instance();

        group.add(dict.getAttr(IppDictOperationAttr.ATTR_PRINTER_URI),
                SUBSCRIPTION_PRINTER_URI);
        group.add(dict.getAttr(IppDictOperationAttr.ATTR_REQUESTING_USER_NAME),
                requestingUser);
        group.add(
                dict.getAttr(IppDictOperationAttr.ATTR_NOTIFY_SUBSCRIPTION_ID),
                subscriptionId);
        // ---------
        return attrGroups;
    }

    /**
     * Creates an IPP request to renews the printer subscription.
     *
     * @param requestingUser
     *            The requesting user.
     * @param subscriptionId
     *            The subscription id.
     * @param leaseSeconds
     *            The lease seconds.
     *
     * @return The IPP request.
     */
    private List<IppAttrGroup> reqRenewPrinterSubscription(
            final String requestingUser, final String subscriptionId,
            final String leaseSeconds) {

        List<IppAttrGroup> attrGroups = new ArrayList<>();

        IppAttrGroup group = null;
        AbstractIppDict dict = null;

        /*
         * Group 1: Operation Attributes
         */
        group = createOperationGroup();
        attrGroups.add(group);

        dict = IppDictOperationAttr.instance();

        group.add(dict.getAttr(IppDictOperationAttr.ATTR_PRINTER_URI),
                SUBSCRIPTION_PRINTER_URI);
        group.add(dict.getAttr(IppDictOperationAttr.ATTR_REQUESTING_USER_NAME),
                requestingUser);
        group.add(
                dict.getAttr(IppDictOperationAttr.ATTR_NOTIFY_SUBSCRIPTION_ID),
                subscriptionId);

        /*
         * Group 2: Subscription Attributes
         */
        group = new IppAttrGroup(IppDelimiterTag.SUBSCRIPTION_ATTR);
        attrGroups.add(group);

        dict = IppDictSubscriptionAttr.instance();

        group.add(
                dict.getAttr(
                        IppDictSubscriptionAttr.ATTR_NOTIFY_LEASE_DURATION),
                leaseSeconds);

        // ---------
        return attrGroups;
    }

    /**
     * Creates an IPP request to create the printer subscription.
     * <p>
     * <b>Note</b>: either recipientUri or notifyPullMethod must be specified.
     * </p>
     *
     * @param requestingUser
     *            The requesting user.
     * @param recipientUri
     *            The recipient as as value of
     *            {@link IppDictSubscriptionAttr#ATTR_NOTIFY_RECIPIENT_URI}
     * @param leaseSeconds
     *            The lease seconds.
     *
     * @return The IPP request.
     */
    private List<IppAttrGroup> reqCreatePrinterPushSubscriptions(
            final String requestingUser, final String recipientUri,
            final String leaseSeconds) {

        List<IppAttrGroup> attrGroups = new ArrayList<>();

        IppAttrGroup group = null;
        AbstractIppDict dict = null;

        /*
         * Group 1: Operation Attributes
         */
        group = createOperationGroup();
        attrGroups.add(group);

        dict = IppDictOperationAttr.instance();

        group.add(dict.getAttr(IppDictOperationAttr.ATTR_PRINTER_URI),
                SUBSCRIPTION_PRINTER_URI);
        group.add(dict.getAttr(IppDictOperationAttr.ATTR_REQUESTING_USER_NAME),
                requestingUser);

        /*
         * Group 2: Subscription Attributes
         */
        group = new IppAttrGroup(IppDelimiterTag.SUBSCRIPTION_ATTR);
        attrGroups.add(group);

        dict = IppDictSubscriptionAttr.instance();

        if (recipientUri != null) {
            group.add(
                    dict.getAttr(
                            IppDictSubscriptionAttr.ATTR_NOTIFY_RECIPIENT_URI),
                    recipientUri);
        }

        group.add(dict.getAttr(IppDictSubscriptionAttr.ATTR_NOTIFY_USER_DATA),
                NOTIFY_USER_DATA);

        group.add(
                dict.getAttr(
                        IppDictSubscriptionAttr.ATTR_NOTIFY_LEASE_DURATION),
                leaseSeconds);

        /*
         * group.add(dict.getAttr(IppDictSubscriptionAttr.
         * ATTR_NOTIFY_TIME_INTERVAL ), "5");
         */

        /*
         * Printer and Jobs events to subscribe on ...
         */
        final String[] events = {
                /* */
                "printer-config-changed",
                /* */
                "printer-media-changed",
                /* */
                "printer-queue-order-changed",
                /* */
                "printer-restarted",
                /* */
                "printer-shutdown",
                /* */
                "printer-state-changed",
                /* */
                "printer-stopped",
                /* */
                "job-state-changed",
                /* */
                "job-created",
                /* */
                "job-completed",
                /* */
                "job-stopped",

                /* */
                /* "job-progress" */

                /* CUPS event: printer or class is added */
                "printer-added",
                /* CUPS event: printer or class is deleted */
                "printer-deleted",
                /* CUPS event: printer or class is modified */
                "printer-modified",
                /* CUPS event: security condition occurs */
                "server-audit",
                /* CUPS event: server is restarted */
                "server-restarted",
                /* CUPS event: server is started */
                "server-started",
                /* CUPS event: server is stopped */
                "server-stopped"
                //
        };

        IppAttrValue attrEvents = new IppAttrValue(
                dict.getAttr(IppDictSubscriptionAttr.ATTR_NOTIFY_EVENTS));

        for (String event : events) {
            attrEvents.addValue(event);
        }

        group.addAttribute(attrEvents);

        // ---------
        return attrGroups;
    }

    /**
     *
     * @param uriPrinter
     * @param requestingUser
     * @return The IPP request.
     */
    private List<IppAttrGroup> reqGetPrinterSubscriptions(
            final String uriPrinter, String requestingUser) {

        List<IppAttrGroup> attrGroups = new ArrayList<>();
        IppAttrGroup group = null;

        /*
         * Group 1: Operation Attributes
         */
        group = createOperationGroup();
        attrGroups.add(group);

        // ---------
        AbstractIppDict dict = IppDictOperationAttr.instance();
        group.add(dict.getAttr(IppDictOperationAttr.ATTR_PRINTER_URI),
                uriPrinter);
        group.add(dict.getAttr(IppDictOperationAttr.ATTR_MY_SUBSCRIPTIONS),
                IppBoolean.TRUE);
        group.add(dict.getAttr(IppDictOperationAttr.ATTR_REQUESTING_USER_NAME),
                requestingUser);

        // ---------
        return attrGroups;
    }

    private static final String SUBSCRIPTION_PRINTER_URI = "ipp://localhost/";

    /**
     *
     * @param uriJob
     * @param reqUser
     * @return
     */
    private List<IppAttrGroup> reqGetJobAttr(final String uriJob) {

        List<IppAttrGroup> attrGroups = new ArrayList<>();

        IppAttrGroup group = null;
        AbstractIppDict dict = null;

        /*
         * Group 1: Operation Attributes
         */
        group = createOperationGroup();
        attrGroups.add(group);

        dict = IppDictOperationAttr.instance();

        group.add(dict.getAttr(IppDictOperationAttr.ATTR_JOB_URI), uriJob);

        // ---------
        return attrGroups;
    }

    /**
     *
     * @param uriPrinter
     * @param jobId
     * @return
     */
    private List<IppAttrGroup> reqGetJobAttr(final String uriPrinter,
            final Integer jobId) {

        List<IppAttrGroup> attrGroups = new ArrayList<>();

        IppAttrGroup group = null;
        AbstractIppDict dict = null;

        /*
         * Group 1: Operation Attributes
         */
        group = createOperationGroup();
        attrGroups.add(group);

        dict = IppDictOperationAttr.instance();

        // ---------
        group.add(dict.getAttr(IppDictOperationAttr.ATTR_PRINTER_URI),
                uriPrinter);
        group.add(dict.getAttr(IppDictOperationAttr.ATTR_JOB_ID),
                jobId.toString());

        // ---------
        return attrGroups;
    }

    /**
     *
     * @param uriPrinter
     * @param jobId
     * @param requestingUserName
     * @return
     */
    private List<IppAttrGroup> reqCancelJobAttr(final String uriPrinter,
            final Integer jobId, final String requestingUserName) {

        final List<IppAttrGroup> attrGroups = new ArrayList<>();

        IppAttrGroup group = null;
        AbstractIppDict dict = null;

        /*
         * Group 1: Operation Attributes
         */
        group = createOperationGroup();
        attrGroups.add(group);

        dict = IppDictOperationAttr.instance();

        // ---------
        group.add(dict.getAttr(IppDictOperationAttr.ATTR_PRINTER_URI),
                uriPrinter);
        group.add(dict.getAttr(IppDictOperationAttr.ATTR_JOB_ID),
                jobId.toString());
        group.add(dict.getAttr(IppDictOperationAttr.ATTR_REQUESTING_USER_NAME),
                requestingUserName);

        // ---------
        return attrGroups;
    }

    /**
     *
     * @param uriPrinter
     * @return
     */
    private List<IppAttrGroup> reqGetPrinterAttr(final String uriPrinter) {

        final List<IppAttrGroup> attrGroups = new ArrayList<>();

        IppAttrGroup group = null;
        IppAttrValue value = null;
        AbstractIppDict dict = null;

        /*
         * Group 1: Operation Attributes
         */
        group = createOperationGroup();
        attrGroups.add(group);

        dict = IppDictOperationAttr.instance();

        // ---------
        group.add(dict.getAttr(IppDictOperationAttr.ATTR_PRINTER_URI),
                uriPrinter);

        // ---------
        value = new IppAttrValue(
                dict.getAttr(IppDictOperationAttr.ATTR_REQUESTED_ATTRIBUTES));

        /*
         * Commented code below shows how to retrieve attribute subsets.
         */
        // value.addValue(IppGetPrinterAttrOperation.ATTR_GRP_JOB_TPL);
        // value.addValue(IppGetPrinterAttrOperation.ATTR_GRP_PRINTER_DESC);
        // value.addValue(IppGetPrinterAttrOperation.ATTR_GRP_MEDIA_COL_DATABASE);

        /*
         * We want them all.
         */
        value.addValue(IppGetPrinterAttrOperation.ATTR_GRP_ALL);

        group.addAttribute(value);

        // ---------
        return attrGroups;
    }

    /**
     *
     * @return
     */
    private List<IppAttrGroup> reqCupsGetPrinters() {

        List<IppAttrGroup> attrGroups = new ArrayList<>();

        IppAttrGroup group = null;

        /*
         * Group 1: Operation Attributes
         */
        group = createOperationGroup();
        attrGroups.add(group);

        /*
         *
         */
        final IppAttrValue reqAttr =
                new IppAttrValue("requested-attributes", IppKeyword.instance());

        for (final String value : new String[] {
                IppDictPrinterDescAttr.ATTR_PRINTER_URI_SUPPORTED,
                IppDictPrinterDescAttr.ATTR_PRINTER_NAME,
                IppDictPrinterDescAttr.ATTR_PRINTER_TYPE,
                IppDictPrinterDescAttr.ATTR_MEMBER_NAMES,
                IppDictPrinterDescAttr.ATTR_PRINTER_INFO,
                IppDictPrinterDescAttr.ATTR_PRINTER_IS_ACCEPTING_JOBS,
                IppDictPrinterDescAttr.ATTR_PRINTER_LOCATION,
                IppDictPrinterDescAttr.ATTR_PRINTER_STATE,
                IppDictPrinterDescAttr.ATTR_PRINTER_STATE_CHANGE_TIME,
                IppDictPrinterDescAttr.ATTR_PRINTER_STATE_REASONS,
                IppDictPrinterDescAttr.ATTR_COLOR_SUPPORTED,
                IppDictPrinterDescAttr.ATTR_PRINTER_MORE_INFO_MANUFACTURER,
                IppDictPrinterDescAttr.ATTR_PRINTER_MAKE_MODEL }) {
            reqAttr.addValue(value);
        }

        group.addAttribute(reqAttr);

        // ---------
        return attrGroups;
    }

    /**
     * Creates the first Group with Operation Attributes.
     *
     * @return
     */
    private IppAttrGroup createOperationGroup() {
        IppAttrGroup group = null;

        /*
         * Group 1: Operation Attributes
         */
        group = new IppAttrGroup(IppDelimiterTag.OPERATION_ATTR);

        AbstractIppDict dict = IppDictOperationAttr.instance();

        // ------------------------------------------------------------------
        group.add(dict.getAttr(IppDictOperationAttr.ATTR_ATTRIBUTES_CHARSET),
                "utf-8");
        group.add(
                dict.getAttr(IppDictOperationAttr.ATTR_ATTRIBUTES_NATURAL_LANG),
                "en-us");

        return group;
    }

    @Override
    public String getCupsVersion() {

        List<IppAttrGroup> reqGroups = new ArrayList<>();

        IppAttrGroup group = null;

        /*
         * Group 1: Operation Attributes
         */
        group = createOperationGroup();
        reqGroups.add(group);

        AbstractIppDict dict = IppDictOperationAttr.instance();

        group.add(dict.getAttr(IppDictOperationAttr.ATTR_REQUESTED_ATTRIBUTES),
                IppDictPrinterDescAttr.ATTR_CUPS_VERSION);

        group.add(dict.getAttr(IppDictOperationAttr.ATTR_LIMIT), "1");

        // -----------
        List<IppAttrGroup> response = new ArrayList<>();

        String version = null;

        try {
            final IppStatusCode statusCode = ippClient.send(
                    getUrlDefaultServer(), IppOperationId.CUPS_GET_PRINTERS,
                    reqGroups, response);

            if (statusCode == IppStatusCode.OK) {
                version = response.get(1).getAttrSingleValue(
                        IppDictPrinterDescAttr.ATTR_CUPS_VERSION);
            }

        } catch (IppConnectException e) {
            // noop
        }

        return version;
    }

    @Override
    public String getCupsApiVersion() {
        return null;
    }

    @Override
    public int getCupsSystemTime() {
        return (int) (System.currentTimeMillis() / NumberUtil.INT_THOUSAND);
    }

    @Override
    public Date getCupsDate(final Integer cupsTime) {
        return new Date(cupsTime.longValue() * NumberUtil.INT_THOUSAND);
    }

    @Override
    public ProxyPrinterDto getProxyPrinterDto(final Printer printer) {

        final ProxyPrinterDto dto = new ProxyPrinterDto();

        dto.setId(printer.getId());
        dto.setPrinterName(printer.getPrinterName());
        dto.setDisplayName(printer.getDisplayName());
        dto.setLocation(printer.getLocation());
        dto.setDisabled(printer.getDisabled());
        dto.setDeleted(printer.getDeleted());

        dto.setPresent(this.getCachedPrinter(printer.getPrinterName()) != null);

        dto.setInternal(printerService().isInternalPrinter(printer.getId()));

        if (docStoreService().isEnabled(DocStoreTypeEnum.ARCHIVE,
                DocStoreBranchEnum.OUT_PRINT)) {
            dto.setArchiveDisabled(printerService().isDocStoreDisabled(
                    DocStoreTypeEnum.ARCHIVE, printer.getId()));
        }
        if (docStoreService().isEnabled(DocStoreTypeEnum.JOURNAL,
                DocStoreBranchEnum.OUT_PRINT)) {
            dto.setJournalDisabled(printerService().isDocStoreDisabled(
                    DocStoreTypeEnum.JOURNAL, printer.getId()));
        }

        dto.setPpdExtFile(printerService().getAttributeValue(printer,
                PrinterAttrEnum.CUSTOM_PPD_EXT_FILE));

        dto.setJobTicket(printerService().isJobTicketPrinter(printer.getId()));

        dto.setJobTicketGroup(printerService().getAttributeValue(printer,
                PrinterAttrEnum.JOBTICKET_PRINTER_GROUP));

        if (jobTicketService().isJobTicketLabelsEnabled()) {
            dto.setJobTicketLabelsEnabled(
                    printerService().isJobTicketLabelsEnabled(printer));
        }

        /*
         * Printer Groups.
         */
        String printerGroups = null;

        final List<PrinterGroupMember> members =
                printer.getPrinterGroupMembers();

        if (members != null) {
            for (PrinterGroupMember member : members) {
                if (printerGroups == null) {
                    printerGroups = "";
                } else {
                    printerGroups += ",";
                }
                printerGroups += member.getGroup().getDisplayName();
            }
        }

        dto.setPrinterGroups(printerGroups);
        return dto;
    }

    /**
     * Creates, updates or removes a printer boolean attribute in the database.
     *
     * @param printer
     *            The printer.
     * @param attribute
     *            The printer attribute.
     * @param attrValue
     *            The attribute value.
     */
    private void setPrinterAttr(final Printer printer,
            final PrinterAttrEnum attribute, final Boolean attrValue) {

        final boolean boolValue = BooleanUtils.isTrue(attrValue);

        final PrinterAttr printerAttr =
                printerService().getAttribute(printer, attribute);

        if (printerAttr == null) {

            if (boolValue) {

                final PrinterAttr attr = new PrinterAttr();

                attr.setPrinter(printer);
                attr.setName(attribute.getDbName());
                attr.setValue(printerAttrDAO().getDbBooleanValue(boolValue));

                printer.getAttributes().add(attr);

                printerAttrDAO().create(attr);
            }

        } else {

            final boolean currentValue =
                    printerAttrDAO().getBooleanValue(printerAttr);

            if (boolValue != currentValue) {

                if (boolValue) {
                    printerAttr.setValue(
                            printerAttrDAO().getDbBooleanValue(boolValue));
                    printerAttrDAO().update(printerAttr);
                } else {
                    printerService().removeAttribute(printer, attribute);
                    printerAttrDAO().delete(printerAttr);
                }
            }
        }
    }

    /**
     * Creates, updates or removes a printer string attribute in the database.
     *
     * @param printer
     *            The printer.
     * @param attribute
     *            The printer attribute.
     * @param attrValue
     *            The attribute value.
     */
    private void setPrinterAttr(final Printer printer,
            final PrinterAttrEnum attribute, final String attrValue) {

        final String strValue = StringUtils.defaultString(attrValue).trim();

        final PrinterAttr printerAttr =
                printerService().getAttribute(printer, attribute);

        if (printerAttr == null) {

            if (StringUtils.isNotBlank(strValue)) {

                final PrinterAttr attr = new PrinterAttr();

                attr.setPrinter(printer);
                attr.setName(attribute.getDbName());
                attr.setValue(strValue);

                printer.getAttributes().add(attr);

                printerAttrDAO().create(attr);
            }

        } else {

            final String currentValue =
                    StringUtils.defaultString(printerAttr.getValue());

            if (!strValue.equals(currentValue)) {

                if (StringUtils.isBlank(strValue)) {
                    printerService().removeAttribute(printer, attribute);
                    printerAttrDAO().delete(printerAttr);
                } else {
                    printerAttr.setValue(strValue);
                    printerAttrDAO().update(printerAttr);
                }
            }
        }
    }

    @Override
    public void setProxyPrinterProps(final Printer jpaPrinter,
            final ProxyPrinterDto dto) {

        final String requestingUser = ServiceContext.getActor();
        final Date now = ServiceContext.getTransactionDate();

        final boolean isJobTicket = BooleanUtils.isTrue(dto.getJobTicket());

        jpaPrinter.setModifiedBy(requestingUser);
        jpaPrinter.setModifiedDate(now);

        // Mantis #1105
        final String displayNameWrk;
        if (StringUtils.isBlank(dto.getDisplayName())) {
            displayNameWrk = dto.getPrinterName();
        } else {
            displayNameWrk = dto.getDisplayName();
        }
        jpaPrinter.setDisplayName(displayNameWrk);

        jpaPrinter.setDisabled(dto.getDisabled());

        // Deleted?
        final boolean isDeleted = dto.getDeleted();

        if (jpaPrinter.getDeleted() != isDeleted) {

            if (isDeleted) {
                printerService().setLogicalDeleted(jpaPrinter);
            } else {
                printerService().undoLogicalDeleted(jpaPrinter);
            }
        }

        // Location.
        jpaPrinter.setLocation(dto.getLocation());

        //
        setPrinterAttr(jpaPrinter, PrinterAttrEnum.ACCESS_INTERNAL,
                dto.getInternal());

        if (docStoreService().isEnabled(DocStoreTypeEnum.ARCHIVE,
                DocStoreBranchEnum.OUT_PRINT)) {
            setPrinterAttr(jpaPrinter, PrinterAttrEnum.ARCHIVE_DISABLE,
                    dto.getArchiveDisabled());
        }
        if (docStoreService().isEnabled(DocStoreTypeEnum.JOURNAL,
                DocStoreBranchEnum.OUT_PRINT)) {
            setPrinterAttr(jpaPrinter, PrinterAttrEnum.JOURNAL_DISABLE,
                    dto.getJournalDisabled());
        }

        setPrinterAttr(jpaPrinter, PrinterAttrEnum.CUSTOM_PPD_EXT_FILE,
                dto.getPpdExtFile());

        //
        setPrinterAttr(jpaPrinter, PrinterAttrEnum.JOBTICKET_ENABLE,
                dto.getJobTicket());
        setPrinterAttr(jpaPrinter, PrinterAttrEnum.JOBTICKET_PRINTER_GROUP,
                dto.getJobTicketGroup());

        if (jobTicketService().isJobTicketLabelsEnabled()) {
            setPrinterAttr(jpaPrinter, PrinterAttrEnum.JOBTICKET_LABELS_ENABLE,
                    Boolean.valueOf(BooleanUtils.isNotTrue(dto.getJobTicket())
                            && BooleanUtils
                                    .isTrue(dto.getJobTicketLabelsEnabled())));
        }

        /*
         * Printer Groups.
         *
         * (1) Put the entered PrinterGroups into a map for easy lookup.
         */
        final String printerGroups = dto.getPrinterGroups();

        final Map<String, String> printerGroupLookup = new HashMap<>();

        for (final String displayName : StringUtils.split(printerGroups,
                " ,;:")) {
            printerGroupLookup.put(displayName.trim().toLowerCase(),
                    displayName.trim());
        }

        /*
         * (1.1) "job sheet media sources configuration is offered for
         * non-job-ticket printers, that belong to at least one job ticket
         * printer group."
         *
         * NOTE: printer groups are not checked for being tied to a job ticket
         * printer, so clean-up might not be performed, even if it logically
         * should.
         */
        if (isJobTicket || printerGroupLookup.isEmpty()) {

            final PrinterAttr removedAttr = printerService().removeAttribute(
                    jpaPrinter, PrinterAttrEnum.JOB_SHEETS_MEDIA_SOURCES);
            if (removedAttr != null) {
                printerAttrDAO().delete(removedAttr);
            }
        }

        /*
         * (2) Remove PrinterGroupMembers which are not selected now, and remove
         * entries from the Map if member already exists.
         */
        boolean isGroupMemberChange = false;

        List<PrinterGroupMember> printerGroupMembers =
                jpaPrinter.getPrinterGroupMembers();

        if (printerGroupMembers == null) {
            printerGroupMembers = new ArrayList<>();
            jpaPrinter.setPrinterGroupMembers(printerGroupMembers);
        }

        final Iterator<PrinterGroupMember> iterMembers =
                printerGroupMembers.iterator();

        while (iterMembers.hasNext()) {

            final PrinterGroupMember member = iterMembers.next();

            final String groupName = member.getGroup().getGroupName();

            if (printerGroupLookup.containsKey(groupName)) {
                printerGroupLookup.remove(groupName);
            } else {
                printerGroupMemberDAO().delete(member);
                iterMembers.remove();
                isGroupMemberChange = true;
            }
        }

        /*
         * (3) Lazy add new Groups and GroupMember.
         */
        for (final Entry<String, String> entry : printerGroupLookup
                .entrySet()) {

            final PrinterGroup group = printerGroupDAO().readOrAdd(
                    entry.getKey(), entry.getValue(), requestingUser, now);

            final PrinterGroupMember member = new PrinterGroupMember();

            member.setGroup(group);
            member.setPrinter(jpaPrinter);
            member.setCreatedBy(requestingUser);
            member.setCreatedDate(now);

            printerGroupMembers.add(member);

            isGroupMemberChange = true;
        }

        //
        jpaPrinter.setModifiedDate(now);
        jpaPrinter.setModifiedBy(requestingUser);

        printerDAO().update(jpaPrinter);

        //
        updateCachedPrinter(jpaPrinter);

        //
        if (isJobTicket && isGroupMemberChange) {
            jobTicketService().updatePrinterGroupIDs(jpaPrinter);
        }
    }

    /**
     * Gets the media-source option choices for a printer from the printer
     * cache.
     *
     * @param printerName
     *            The unique name of the printer.
     * @return The media-source option choices.
     */
    private List<JsonProxyPrinterOptChoice>
            getMediaSourceChoices(final String printerName) {

        final JsonProxyPrinter proxyPrinter = getCachedPrinter(printerName);

        if (proxyPrinter != null) {
            for (JsonProxyPrinterOptGroup group : proxyPrinter.getGroups()) {
                for (JsonProxyPrinterOpt option : group.getOptions()) {
                    if (option.getKeyword()
                            .equals(IppDictJobTemplateAttr.ATTR_MEDIA_SOURCE)) {
                        return option.getChoices();
                    }
                }
            }
        }

        return new ArrayList<JsonProxyPrinterOptChoice>();
    }

    /**
     * Gets the media costs of a Printer from the database.
     *
     * @param printer
     *            The Printer.
     * @return the media costs.
     */
    private Map<String, IppMediaCostDto>
            getCostByIppMediaName(final Printer printer) {

        final Map<String, IppMediaCostDto> map = new HashMap<>();

        if (printer.getAttributes() != null) {

            Iterator<PrinterAttr> iterAttr = printer.getAttributes().iterator();

            while (iterAttr.hasNext()) {

                final PrinterAttr attr = iterAttr.next();

                final String key = attr.getName();

                if (!key.startsWith(
                        PrinterAttrEnum.PFX_COST_MEDIA.getDbName())) {
                    continue;
                }

                final PrinterDao.CostMediaAttr costMediaAttr =
                        PrinterDao.CostMediaAttr.createFromDbKey(key);

                /*
                 * Self-correcting action...
                 */
                if (costMediaAttr == null) {

                    // (1)
                    printerAttrDAO().delete(attr);
                    // (2)
                    iterAttr.remove();

                    //
                    if (LOGGER.isWarnEnabled()) {
                        LOGGER.warn("Auto-correct: removed invalid attribute ["
                                + key + "] " + "from printer ["
                                + printer.getPrinterName() + "]");
                    }
                    continue;
                }

                final String ippMediaName = costMediaAttr.getIppMediaName();

                if (ippMediaName != null) {

                    IppMediaCostDto dto = new IppMediaCostDto();
                    dto.setMedia(ippMediaName);
                    dto.setActive(Boolean.TRUE);
                    try {
                        dto.setPageCost(JsonAbstractBase
                                .create(MediaCostDto.class, attr.getValue()));
                        map.put(ippMediaName, dto);
                    } catch (SpException e) {
                        LOGGER.error(e.getMessage());
                    }

                } else {
                    LOGGER.error("Printer [" + printer.getPrinterName()
                            + "] : no IPP media name found in key [" + key
                            + "]");
                }
            }
        }

        return map;
    }

    /**
     * Gets the media costs of a Printer from the database.
     *
     * @param printer
     *            The Printer.
     * @return the media costs.
     */
    private Map<String, IppMediaSourceCostDto>
            getIppMediaSources(final Printer printer) {

        final Map<String, IppMediaSourceCostDto> map = new HashMap<>();

        if (printer.getAttributes() != null) {

            Iterator<PrinterAttr> iterAttr = printer.getAttributes().iterator();

            while (iterAttr.hasNext()) {

                final PrinterAttr attr = iterAttr.next();

                final String key = attr.getName();

                if (!key.startsWith(
                        PrinterAttrEnum.PFX_MEDIA_SOURCE.getDbName())) {
                    continue;
                }

                final PrinterDao.MediaSourceAttr mediaSourceAttr =
                        PrinterDao.MediaSourceAttr.createFromDbKey(key);

                /*
                 * Self-correcting action...
                 */
                if (mediaSourceAttr == null) {

                    // (1)
                    printerAttrDAO().delete(attr);
                    // (2)
                    iterAttr.remove();

                    //
                    if (LOGGER.isWarnEnabled()) {
                        LOGGER.warn("Auto-correct: removed invalid attribute ["
                                + key + "] " + "from printer ["
                                + printer.getPrinterName() + "]");
                    }
                    continue;
                }

                final String ippMediaSourceName =
                        mediaSourceAttr.getIppMediaSourceName();

                if (ippMediaSourceName != null) {

                    final IppMediaSourceCostDto dto;

                    try {
                        dto = JsonAbstractBase.create(
                                IppMediaSourceCostDto.class, attr.getValue());

                        map.put(dto.getSource(), dto);

                    } catch (SpException e) {
                        LOGGER.error(e.getMessage());
                    }

                } else {
                    LOGGER.error("Printer [" + printer.getPrinterName()
                            + "] : no IPP media source found in key [" + key
                            + "]");
                }
            }
        }

        return map;
    }

    @Override
    public List<IppMediaCostDto>
            getProxyPrinterCostMedia(final Printer printer) {

        final List<IppMediaCostDto> list = new ArrayList<>();

        /*
         *
         */
        final Map<String, IppMediaCostDto> databaseMediaCost =
                getCostByIppMediaName(printer);

        /*
         * Lazy create "default" media.
         */
        IppMediaCostDto dto =
                databaseMediaCost.get(IppMediaCostDto.DEFAULT_MEDIA);

        if (dto == null) {

            dto = new IppMediaCostDto();

            dto.setMedia(IppMediaCostDto.DEFAULT_MEDIA);
            dto.setActive(Boolean.TRUE);

            MediaCostDto pageCost = new MediaCostDto();
            dto.setPageCost(pageCost);

            MediaPageCostDto cost = new MediaPageCostDto();
            pageCost.setCostOneSided(cost);
            cost.setCostGrayscale("0.0");
            cost.setCostColor("0.0");

            cost = new MediaPageCostDto();
            pageCost.setCostTwoSided(cost);
            cost.setCostGrayscale("0.0");
            cost.setCostColor("0.0");

        }

        list.add(dto);

        /*
         * The "regular" media choices from the printer.
         */
        for (JsonProxyPrinterOptChoice media : getMediaChoices(
                printer.getPrinterName())) {

            dto = databaseMediaCost.get(media.getChoice());

            if (dto == null) {

                dto = new IppMediaCostDto();

                dto.setMedia(media.getChoice());
                dto.setActive(Boolean.FALSE);

                MediaCostDto pageCost = new MediaCostDto();
                dto.setPageCost(pageCost);

                MediaPageCostDto cost = new MediaPageCostDto();
                pageCost.setCostOneSided(cost);
                cost.setCostGrayscale("0.0");
                cost.setCostColor("0.0");

                cost = new MediaPageCostDto();
                pageCost.setCostTwoSided(cost);
                cost.setCostGrayscale("0.0");
                cost.setCostColor("0.0");
            }

            list.add(dto);
        }

        return list;
    }

    @Override
    public List<IppMediaSourceCostDto>
            getProxyPrinterCostMediaSource(final Printer printer) {

        final List<IppMediaSourceCostDto> list = new ArrayList<>();

        /*
         *
         */
        final Map<String, IppMediaSourceCostDto> databaseMediaSources =
                getIppMediaSources(printer);

        /*
         * The media-source choices from the printer.
         */
        for (final JsonProxyPrinterOptChoice mediaSource : getMediaSourceChoices(
                printer.getPrinterName())) {

            final String choice = mediaSource.getChoice();

            IppMediaSourceCostDto dto = databaseMediaSources.get(choice);

            if (dto == null) {

                dto = new IppMediaSourceCostDto();

                dto.setActive(Boolean.FALSE);
                dto.setDisplay(mediaSource.getUiText());
                dto.setSource(mediaSource.getChoice());

                if (!choice.equals(IppKeyword.MEDIA_SOURCE_AUTO)
                        && !choice.equals(IppKeyword.MEDIA_SOURCE_MANUAL)) {

                    IppMediaSourceCostDto dtoMedia =
                            new IppMediaSourceCostDto();
                    dto = dtoMedia;

                    dto.setActive(Boolean.FALSE);
                    dto.setSource(mediaSource.getChoice());

                    final IppMediaCostDto mediaCost = new IppMediaCostDto();
                    dtoMedia.setMedia(mediaCost);

                    mediaCost.setActive(Boolean.FALSE);
                    mediaCost.setMedia(""); // blank

                    final MediaCostDto pageCost = new MediaCostDto();
                    mediaCost.setPageCost(pageCost);

                    MediaPageCostDto cost;

                    cost = new MediaPageCostDto();
                    pageCost.setCostOneSided(cost);
                    cost.setCostGrayscale("0.0");
                    cost.setCostColor("0.0");

                    cost = new MediaPageCostDto();
                    pageCost.setCostTwoSided(cost);
                    cost.setCostGrayscale("0.0");
                    cost.setCostColor("0.0");
                }
            }

            list.add(dto);
        }

        return list;
    }

    /**
     *
     * @param printer
     * @param defaultCost
     * @return
     * @throws ParseException
     */
    private AbstractJsonRpcMethodResponse setPrinterSimpleCost(
            final Printer printer, final String defaultCost,
            final Locale locale) {

        try {
            printer.setDefaultCost(
                    BigDecimalUtil.parse(defaultCost, locale, false, false));

        } catch (ParseException e) {

            return JsonRpcMethodError.createBasicError(Code.INVALID_PARAMS, "",
                    localize(ServiceContext.getLocale(),
                            "msg-printer-cost-error", defaultCost));
        }

        printer.setChargeType(Printer.ChargeType.SIMPLE.toString());

        printer.setModifiedDate(ServiceContext.getTransactionDate());
        printer.setModifiedBy(ServiceContext.getActor());

        printerDAO().update(printer);

        updateCachedPrinter(printer);

        return JsonRpcMethodResult.createOkResult();
    }

    /**
     *
     * @param printer
     * @param dtoList
     * @param locale
     *            The Locale of the cost in the dtoList.
     * @return
     */
    private AbstractJsonRpcMethodResponse setPrinterMediaCost(
            final Printer printer, final List<IppMediaCostDto> dtoList,
            final Locale locale) {

        /*
         * Put into map for easy lookup of objects to handle. Validate along the
         * way.
         *
         * NOTE: processed entries are removed from the map later on.
         */
        final Map<String, IppMediaCostDto> mapCost = new HashMap<>();

        for (final IppMediaCostDto dto : dtoList) {

            final String mediaKey = dto.getMedia();

            if (!dto.isDefault() && IppMediaSizeEnum.find(mediaKey) == null) {

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Media [" + mediaKey + "] is invalid.");
                }
                continue;
            }

            /*
             * Validate active entries only.
             */

            if (dto.getActive()) {

                final MediaPageCostDto dtoCostOne =
                        dto.getPageCost().getCostOneSided();

                final MediaPageCostDto dtoCostTwo =
                        dto.getPageCost().getCostTwoSided();

                for (final String cost : new String[] {
                        dtoCostOne.getCostColor(),
                        dtoCostOne.getCostGrayscale(),
                        dtoCostTwo.getCostColor(),
                        dtoCostTwo.getCostGrayscale() }) {

                    if (!BigDecimalUtil.isValid(cost, locale, false)) {
                        return JsonRpcMethodError
                                .createBasicError(Code.INVALID_PARAMS, "",
                                        localize(ServiceContext.getLocale(),
                                                "msg-printer-cost-error",
                                                cost));
                    }
                }
            }

            mapCost.put(mediaKey, dto);
        }

        /*
         * Lazy create of attribute list.
         */
        if (printer.getAttributes() == null) {
            printer.setAttributes(new ArrayList<PrinterAttr>());
        }

        /*
         * Look for existing PrinterAttr objects to update or delete.
         */
        final Iterator<PrinterAttr> iterAttr =
                printer.getAttributes().iterator();

        while (iterAttr.hasNext()) {

            final PrinterAttr costAttr = iterAttr.next();

            final PrinterDao.CostMediaAttr costMediaAttr =
                    PrinterDao.CostMediaAttr
                            .createFromDbKey(costAttr.getName());

            if (costMediaAttr == null) {
                continue;
            }

            final String mediaKey = costMediaAttr.getIppMediaName();
            final IppMediaCostDto costDto = mapCost.get(mediaKey);

            if (costDto != null && costDto.getActive()) {
                /*
                 * Update active entry.
                 */
                String json;

                try {
                    json = costDto.getPageCost().stringify(locale);
                } catch (ParseException | IOException e) {
                    throw new SpException(e);
                }

                costAttr.setValue(json);

            } else {
                /*
                 * Remove non-active entry.
                 */
                // (1)
                printerAttrDAO().delete(costAttr);
                // (2)
                iterAttr.remove();
            }

            /*
             * Handled, so remove from the map.
             */
            mapCost.remove(mediaKey);
        }

        /*
         * Add the active entries which are left in the map.
         */
        for (final IppMediaCostDto costDto : mapCost.values()) {

            if (costDto.getActive()) {
                /*
                 * Add active entry.
                 */
                final PrinterAttr costAttr = new PrinterAttr();

                costAttr.setPrinter(printer);
                costAttr.setName(
                        new PrinterDao.CostMediaAttr(costDto.getMedia())
                                .getKey());

                String json;

                try {
                    json = costDto.getPageCost().stringify(locale);
                } catch (ParseException | IOException e) {
                    throw new SpException(e);
                }

                costAttr.setValue(json);

                // (1)
                printerAttrDAO().create(costAttr);
                // (2)
                printer.getAttributes().add(costAttr);

            } else {
                /*
                 * Non-active entry + no attribute found earlier on: no code
                 * intended.
                 */
            }

        }

        /*
         * Update the printer.
         */
        printer.setChargeType(Printer.ChargeType.MEDIA.toString());

        printer.setModifiedDate(ServiceContext.getTransactionDate());
        printer.setModifiedBy(ServiceContext.getActor());

        printerDAO().update(printer);

        updateCachedPrinter(printer);

        /*
         * We are ok.
         */
        return JsonRpcMethodResult.createOkResult();
    }

    @Override
    public AbstractJsonRpcMethodResponse setProxyPrinterCostMedia(
            final Printer printer, final ProxyPrinterCostDto dto) {

        final Locale locale =
                new Locale.Builder().setLanguageTag(dto.getLanguage())
                        .setRegion(dto.getCountry()).build();

        if (dto.getChargeType() == Printer.ChargeType.SIMPLE) {
            return setPrinterSimpleCost(printer, dto.getDefaultCost(), locale);
        } else {
            return setPrinterMediaCost(printer, dto.getMediaCost(), locale);
        }
    }

    @Override
    public AbstractJsonRpcMethodResponse setProxyPrinterCostMediaSources(
            final Printer printer,
            final ProxyPrinterMediaSourcesDto dtoMediaSources) {

        final Locale locale = new Locale.Builder()
                .setLanguageTag(dtoMediaSources.getLanguage())
                .setRegion(dtoMediaSources.getCountry()).build();

        /*
         * Put into map for easy lookup of objects to handle. Validate along the
         * way.
         *
         * NOTE: processed entries are removed from the map later on.
         */
        final Map<String, IppMediaSourceCostDto> mapMediaSources =
                new HashMap<>();

        for (final IppMediaSourceCostDto dto : dtoMediaSources.getSources()) {

            final String mediaSourceKey = dto.getSource();

            /*
             * VALIDATE active entries only.
             */
            if (dto.getActive()) {

                final IppMediaCostDto dtoMediaCost = dto.getMedia();

                if (IppMediaSizeEnum.find(dtoMediaCost.getMedia()) == null) {
                    return JsonRpcMethodError.createBasicError(
                            Code.INVALID_PARAMS, "",
                            localize(ServiceContext.getLocale(),
                                    "msg-printer-media-error",
                                    dtoMediaCost.getMedia(), dto.getDisplay()));
                }

                final MediaPageCostDto dtoCostOne =
                        dtoMediaCost.getPageCost().getCostOneSided();

                final MediaPageCostDto dtoCostTwo =
                        dtoMediaCost.getPageCost().getCostTwoSided();

                for (final String cost : new String[] {
                        dtoCostOne.getCostColor(),
                        dtoCostOne.getCostGrayscale(),
                        dtoCostTwo.getCostColor(),
                        dtoCostTwo.getCostGrayscale() }) {

                    if (!BigDecimalUtil.isValid(cost, locale, false)) {
                        return JsonRpcMethodError
                                .createBasicError(Code.INVALID_PARAMS, "",
                                        localize(ServiceContext.getLocale(),
                                                "msg-printer-cost-error",
                                                cost));
                    }
                }
            }

            mapMediaSources.put(mediaSourceKey, dto);
        }

        /*
         * Add Auto, Manual to the map
         */
        for (final IppMediaSourceCostDto miscSource : new IppMediaSourceCostDto[] {
                dtoMediaSources.getSourceAuto(),
                dtoMediaSources.getSourceManual() }) {

            if (miscSource != null) {
                mapMediaSources.put(miscSource.getSource(), miscSource);
            }
        }

        /*
         * Lazy create of attribute list.
         */
        if (printer.getAttributes() == null) {
            printer.setAttributes(new ArrayList<PrinterAttr>());
        }

        /*
         *
         */
        final Boolean isForceDefaultMonochrome =
                dtoMediaSources.getDefaultMonochrome();

        String attrPrintColorModeDefault = PrinterDao.IppKeywordAttr
                .getKey(IppDictJobTemplateAttr.ATTR_PRINT_COLOR_MODE_DFLT);

        /*
         * Look for existing PrinterAttr objects to update or delete.
         */
        final Iterator<PrinterAttr> iterAttr =
                printer.getAttributes().iterator();

        Boolean clientSideMonochrome =
                dtoMediaSources.getClientSideMonochrome();

        //
        final Set<String> jobSheetsMediaSources =
                dtoMediaSources.getJobSheetsMediaSources();

        String jsonJobSheetsMediaSources = null;

        if (jobSheetsMediaSources != null && !jobSheetsMediaSources.isEmpty()) {
            /*
             * INVARIANT: job-sheet media-sources must match active
             * media-source.
             */
            for (final String mediaSource : jobSheetsMediaSources) {
                final IppMediaSourceCostDto dtoWlk =
                        mapMediaSources.get(mediaSource);
                if (dtoWlk != null && BooleanUtils.isTrue(dtoWlk.getActive())) {
                    continue;
                }
                return JsonRpcMethodError.createBasicError(Code.INVALID_PARAMS,
                        "",
                        localize(ServiceContext.getLocale(),
                                "msg-printer-job-sheet-media-source-disabled",
                                PrintOutNounEnum.JOB_SHEET.uiText(
                                        ServiceContext.getLocale(), true),
                                mediaSource));
            }
            jsonJobSheetsMediaSources =
                    JsonHelper.stringifyStringSet(jobSheetsMediaSources);
        } else {
            jsonJobSheetsMediaSources = null;
        }

        //
        while (iterAttr.hasNext()) {

            final PrinterAttr printerAttr = iterAttr.next();

            /*
             * JobSheetsMediaSource?
             */
            if (printerAttr.getName().equalsIgnoreCase(
                    PrinterAttrEnum.JOB_SHEETS_MEDIA_SOURCES.getDbName())) {

                if (StringUtils.isNotBlank(jsonJobSheetsMediaSources)) {

                    printerAttr.setValue(jsonJobSheetsMediaSources);
                    jsonJobSheetsMediaSources = null;

                } else {
                    /*
                     * Remove non-active entry.
                     */
                    printerAttrDAO().delete(printerAttr);
                    iterAttr.remove();
                }
                continue;
            }

            /*
             * Client-side grayscale conversion?
             */
            if (printerAttr.getName().equalsIgnoreCase(
                    PrinterAttrEnum.CLIENT_SIDE_MONOCHROME.getDbName())) {

                if (clientSideMonochrome != null
                        && clientSideMonochrome.booleanValue()) {

                    printerAttr.setValue(IAttrDao.V_YES);
                    clientSideMonochrome = null;

                } else {
                    /*
                     * Remove non-active entry.
                     */
                    printerAttrDAO().delete(printerAttr);
                    iterAttr.remove();
                }
                continue;
            }

            /*
             * IppKeywordAttr: force monochrome default (update/delete).
             */
            if (attrPrintColorModeDefault != null && attrPrintColorModeDefault
                    .equalsIgnoreCase(printerAttr.getName())) {

                if (isForceDefaultMonochrome != null
                        && isForceDefaultMonochrome) {

                    printerAttr
                            .setValue(IppKeyword.PRINT_COLOR_MODE_MONOCHROME);

                } else {
                    /*
                     * Remove non-active entry.
                     */
                    printerAttrDAO().delete(printerAttr);
                    iterAttr.remove();
                }

                /*
                 * This is a one-shot setting: prevent handling again by setting
                 * to null.
                 */
                attrPrintColorModeDefault = null;
                continue;
            }

            /*
             * MediaSourceAttr
             */
            final PrinterDao.MediaSourceAttr mediaSourceAttr =
                    PrinterDao.MediaSourceAttr
                            .createFromDbKey(printerAttr.getName());

            if (mediaSourceAttr == null) {
                continue;
            }

            final String mediaSourceKey =
                    mediaSourceAttr.getIppMediaSourceName();

            final IppMediaSourceCostDto mediaSourceDto =
                    mapMediaSources.get(mediaSourceKey);

            if (mediaSourceDto != null && mediaSourceDto.getActive()) {
                /*
                 * Update active entry.
                 */
                String json;

                try {
                    mediaSourceDto.toDatabaseObject(locale);
                    json = mediaSourceDto.stringify();
                } catch (IOException e) {
                    throw new SpException(e);
                }

                printerAttr.setValue(json);

            } else {
                /*
                 * Remove non-active entry.
                 */
                printerAttrDAO().delete(printerAttr);
                iterAttr.remove();
            }

            /*
             * Handled, so remove from the map.
             */
            mapMediaSources.remove(mediaSourceKey);
        }

        /*
         * Add the active entries which are left in the map.
         */
        for (final IppMediaSourceCostDto mediaSourceDto : mapMediaSources
                .values()) {

            if (mediaSourceDto.getActive()) {
                /*
                 * Add active entry.
                 */
                mediaSourceDto.toDatabaseObject(locale);

                try {
                    createAddPrinterAttr(printer,
                            new PrinterDao.MediaSourceAttr(
                                    mediaSourceDto.getSource()).getKey(),
                            mediaSourceDto.stringify());
                } catch (IOException e) {
                    throw new SpException(e);
                }

            } else {
                /*
                 * Non-active entry + no attribute found earlier on: no code
                 * intended.
                 */
            }
        }

        /*
         * IppKeywordAttr: force monochrome default (add).
         */
        if (attrPrintColorModeDefault != null
                && isForceDefaultMonochrome != null
                && isForceDefaultMonochrome) {

            final String nameKey = new PrinterDao.IppKeywordAttr(
                    IppDictJobTemplateAttr.ATTR_PRINT_COLOR_MODE_DFLT).getKey();

            createAddPrinterAttr(printer, nameKey,
                    IppKeyword.PRINT_COLOR_MODE_MONOCHROME);
        }

        /*
         * Client-side grayscale conversion (add).
         */
        if (clientSideMonochrome != null
                && clientSideMonochrome.booleanValue()) {
            createAddPrinterAttr(printer,
                    PrinterAttrEnum.CLIENT_SIDE_MONOCHROME, IAttrDao.V_YES);
        }

        /*
         * Client-side grayscale conversion (add).
         */
        if (StringUtils.isNotBlank(jsonJobSheetsMediaSources)) {
            createAddPrinterAttr(printer,
                    PrinterAttrEnum.JOB_SHEETS_MEDIA_SOURCES,
                    jsonJobSheetsMediaSources);
        }

        /*
         * Update the printer.
         */
        printer.setModifiedDate(ServiceContext.getTransactionDate());
        printer.setModifiedBy(ServiceContext.getActor());

        printerDAO().update(printer);

        updateCachedPrinter(printer);

        /*
         * We are OK.
         */
        return JsonRpcMethodResult.createOkResult();
    }

    /**
     * Creates a {@link PrinterAttr} in database and adds it to printer
     * attribute list.
     *
     * @param printer
     *            The printer.
     * @param attrEnum
     *            The attribute enum.
     * @param value
     *            The attribute value.
     */
    private static void createAddPrinterAttr(final Printer printer,
            final PrinterAttrEnum attrEnum, final String value) {
        createAddPrinterAttr(printer, attrEnum.getDbName(), value);
    }

    /**
     * Creates a {@link PrinterAttr} in database and adds it to printer
     * attribute list.
     *
     * @param printer
     *            The printer.
     * @param name
     *            The attribute name (key).
     * @param value
     *            The attribute value.
     */
    private static void createAddPrinterAttr(final Printer printer,
            final String name, final String value) {

        final PrinterAttr printerAttr = new PrinterAttr();

        printerAttr.setPrinter(printer);
        printerAttr.setName(name);
        printerAttr.setValue(value);

        // (1)
        printerAttrDAO().create(printerAttr);
        // (2)
        printer.getAttributes().add(printerAttr);
    }

    /**
     * Gets the CUPS URL for a printer.
     *
     * @param path
     *            The path.
     * @return The URL.
     * @throws UnknownHostException
     *             If host unknown.
     */
    private URL getCupsUrl(final String path) throws UnknownHostException {
        return this.getCupsUrl(InetUtils.URL_PROTOCOL_HTTPS,
                InetUtils.getServerHostAddress(), path);
    }

    /**
     * Gets the CUPS URL for a printer.
     *
     * @param protocol
     *            The URL protocol.
     * @param host
     *            Host name or IP address.
     * @param path
     *            The path.
     * @return The URL.
     */
    private URL getCupsUrl(final String protocol, final String host,
            final String path) {

        final URL url;

        try {
            url = new URL(protocol, host,
                    Integer.parseInt(ConfigManager.getCupsPort()), path);

        } catch (MalformedURLException e) {
            throw new SpException(e.getMessage(), e);
        }

        return url;
    }

    @Override
    public URL getCupsPrinterUrl(final String printerName) {
        try {
            return this.getCupsPrinterUrl(InetUtils.URL_PROTOCOL_HTTPS,
                    InetUtils.getServerHostAddress(), printerName);
        } catch (UnknownHostException e) {
            throw new SpException(e.getMessage(), e);
        }
    }

    @Override
    public URI getCupsPrinterURI(final String printerName) {
        final JsonProxyPrinter proxyPrinter =
                this.getCachedPrinter(printerName);
        if (proxyPrinter == null) {
            return null;
        }
        return proxyPrinter.getDeviceUri();
    }

    /**
     *
     * @param protocol
     *            The URL protocol.
     * @param host
     *            Host name or IP address.
     * @param printerName
     *            CUPS Printer name.
     * @return The URL.
     * @throws UnknownHostException
     *             If host unknown.
     */
    private URL getCupsPrinterUrl(final String protocol, final String host,
            final String printerName) throws UnknownHostException {
        return getCupsUrl(protocol, host,
                URL_PATH_CUPS_PRINTERS.concat("/").concat(printerName));
    }

    @Override
    public URL getCupsAdminUrl() {
        try {
            return getCupsUrl(URL_PATH_CUPS_ADMIN);
        } catch (UnknownHostException e) {
            throw new SpException(e.getMessage());
        }
    }

    @Override
    public ThirdPartyEnum getExtPrinterManager(final String cupsPrinterName) {

        final JsonProxyPrinter cupsPrinter =
                this.getCachedPrinter(cupsPrinterName);

        if (cupsPrinter != null) {

            final URI deviceUri = cupsPrinter.getDeviceUri();

            if (deviceUri != null
                    && PaperCutHelper.isPaperCutPrinter(deviceUri)) {
                return ThirdPartyEnum.PAPERCUT;
            }
        }
        return null;
    }

}
