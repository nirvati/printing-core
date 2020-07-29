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
package org.savapage.core.ipp.operation;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.savapage.core.community.CommunityDictEnum;
import org.savapage.core.community.MemberCard;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.ipp.IppMediaSizeEnum;
import org.savapage.core.ipp.IppPrinterType;
import org.savapage.core.ipp.IppVersionEnum;
import org.savapage.core.ipp.attribute.IppAttr;
import org.savapage.core.ipp.attribute.IppAttrCollection;
import org.savapage.core.ipp.attribute.IppAttrGroup;
import org.savapage.core.ipp.attribute.IppAttrValue;
import org.savapage.core.ipp.attribute.IppDictJobTemplateAttr;
import org.savapage.core.ipp.attribute.IppDictJobTemplateAttr.ApplEnum;
import org.savapage.core.ipp.attribute.IppDictOperationAttr;
import org.savapage.core.ipp.attribute.IppDictPrinterDescAttr;
import org.savapage.core.ipp.attribute.syntax.IppBoolean;
import org.savapage.core.ipp.attribute.syntax.IppDateTime;
import org.savapage.core.ipp.attribute.syntax.IppInteger;
import org.savapage.core.ipp.attribute.syntax.IppKeyword;
import org.savapage.core.ipp.attribute.syntax.IppRangeOfInteger;
import org.savapage.core.ipp.attribute.syntax.IppResolution;
import org.savapage.core.ipp.attribute.syntax.IppUri;
import org.savapage.core.ipp.encoding.IppDelimiterTag;
import org.savapage.core.ipp.helpers.IppMediaSizeHelper;
import org.savapage.core.jpa.IppQueue;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.system.SystemInfo;
import org.savapage.core.util.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * Get-Printer-Attributes Response.
 *
 * @author Rijk Ravestein
 *
 */
public class IppGetPrinterAttrRsp extends AbstractIppResponse {

    /** */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(IppGetPrinterAttrRsp.class);

    /**
     * CUPS version singleton holder.
     */
    private static class CUPSVersionHolder {
        /** CUPS version singleton. */
        public static final String VERSION = ServiceContext.getServiceFactory()
                .getProxyPrintService().getCupsVersion();
    }

    /** */
    public static final IppMediaSizeEnum IPP_MEDIA_DEFAULT =
            IppMediaSizeEnum.ISO_A4;

    /** */
    private static final IppMediaSizeEnum[] IPP_MEDIA_SUPPORTED = {
            //
            IPP_MEDIA_DEFAULT, IppMediaSizeEnum.NA_LETTER,
            IppMediaSizeEnum.ISO_A0, IppMediaSizeEnum.ISO_A1,
            IppMediaSizeEnum.ISO_A2, IppMediaSizeEnum.ISO_A3
            //
    };

    /**
     * Same as {@link #IPP_MEDIA_SUPPORTED}, but with different order, i.e.
     * media default as last. Test shows that iOS uses last value as default
     * media.
     */
    private static final IppMediaSizeEnum[] IPP_MEDIA_READY = {
            //
            IppMediaSizeEnum.ISO_A0, IppMediaSizeEnum.ISO_A1,
            IppMediaSizeEnum.ISO_A2, IppMediaSizeEnum.ISO_A3,
            IppMediaSizeEnum.NA_LETTER, IPP_MEDIA_DEFAULT
            //
    };

    /**
     *
     */
    private static final String[] PRINTER_ICONS_PNG = {
            //
            "48x48.png", "128x128.png", "512x512.png" };

    /**
     * Job Template attributes supported by SavaPage Printer IPP/1.1.
     */
    private static final String[] SUPPORTED_ATTR_JOB_TPL_V1 = {
            //
            IppDictJobTemplateAttr.ATTR_COPIES,
            IppDictJobTemplateAttr.ATTR_MEDIA,
            IppDictJobTemplateAttr.ATTR_ORIENTATION_REQUESTED,
            IppDictJobTemplateAttr.ATTR_NUMBER_UP,
            IppDictJobTemplateAttr.ATTR_JOB_SHEETS,
            IppDictJobTemplateAttr.ATTR_OUTPUT_BIN,
            IppDictJobTemplateAttr.ATTR_SIDES
            //
    };

    /**
     * Job Template attributes supported by SavaPage Printer IPP/2.0.
     */
    private static final String[] SUPPORTED_ATTR_JOB_TPL_V2 = {
            //
            IppDictJobTemplateAttr.ATTR_PRINT_QUALITY,
            IppDictJobTemplateAttr.ATTR_PRINTER_RESOLUTION
            //
    };

    /**
     * Printer attributes supported by SavaPage Printer IPP/1.1.
     */
    private static final String[] SUPPORTED_ATTR_PRINTER_DESC_V1 = {
            /* REQUIRED */
            IppDictPrinterDescAttr.ATTR_PRINTER_URI_SUPPORTED,
            /* REQUIRED */
            IppDictPrinterDescAttr.ATTR_URI_AUTH_SUPPORTED,
            /* REQUIRED */
            IppDictPrinterDescAttr.ATTR_URI_SECURITY_SUPPORTED,
            /* REQUIRED */
            IppDictPrinterDescAttr.ATTR_PRINTER_NAME,
            /* REQUIRED */
            IppDictPrinterDescAttr.ATTR_PRINTER_STATE,
            /* REQUIRED */
            IppDictPrinterDescAttr.ATTR_PRINTER_STATE_REASONS,
            /* REQUIRED */
            IppDictPrinterDescAttr.ATTR_IPP_VERSIONS_SUPP,
            /* REQUIRED */
            IppDictPrinterDescAttr.ATTR_OPERATIONS_SUPPORTED,
            /* REQUIRED */
            IppDictPrinterDescAttr.ATTR_CHARSET_CONFIGURED,
            /* REQUIRED */
            IppDictPrinterDescAttr.ATTR_CHARSET_SUPPORTED,
            /* REQUIRED */
            IppDictPrinterDescAttr.ATTR_NATURAL_LANG_CONFIGURED,
            /* REQUIRED */
            IppDictPrinterDescAttr.ATTR_GENERATED_NATURAL_LANG_SUPPORTED,
            /* REQUIRED */
            IppDictPrinterDescAttr.ATTR_DOC_FORMAT_DEFAULT,
            /* REQUIRED */
            IppDictPrinterDescAttr.ATTR_DOC_FORMAT_SUPPORTED,
            /* REQUIRED */
            IppDictPrinterDescAttr.ATTR_PRINTER_IS_ACCEPTING_JOBS,
            /* REQUIRED */
            IppDictPrinterDescAttr.ATTR_QUEUES_JOB_COUNT,
            /* REQUIRED */
            IppDictPrinterDescAttr.ATTR_PDL_OVERRIDE_SUPPORTED,
            /* REQUIRED */
            IppDictPrinterDescAttr.ATTR_PRINTER_UP_TIME,
            /* REQUIRED */
            IppDictPrinterDescAttr.ATTR_COMPRESSION_SUPPORTED,

            /* OPTIONAL */
            IppDictPrinterDescAttr.ATTR_PAGES_PER_MIN,
            /* OPTIONAL */
            IppDictPrinterDescAttr.ATTR_PAGES_PER_MIN_COLOR,
            /* OPTIONAL */
            IppDictPrinterDescAttr.ATTR_COLOR_SUPPORTED,
            //

            // REQUIRED by Bonjour.
            IppDictPrinterDescAttr.ATTR_PRINTER_UUID,
            // REQUIRED by Bonjour.
            IppDictPrinterDescAttr.ATTR_PRINTER_MORE_INFO

    };
    /**
     * Printer attributes supported by SavaPage Printer IPP/2.x.
     */
    private static final String[] SUPPORTED_ATTR_PRINTER_DESC_V2 = {
            IppDictPrinterDescAttr.ATTR_IPP_FEATURES_SUPP,
            IppDictPrinterDescAttr.ATTR_PRINTER_DEVICE_ID,
            IppDictPrinterDescAttr.ATTR_DOC_PASSWORD_SUPPORTED,
            IppDictPrinterDescAttr.ATTR_PRINTER_STATE_MESSAGE,
            IppDictPrinterDescAttr.ATTR_PRINTER_STATE_CHANGE_TIME,
            IppDictPrinterDescAttr.ATTR_PRINTER_MAKE_MODEL,
            IppDictPrinterDescAttr.ATTR_PRINTER_INFO,
            IppDictPrinterDescAttr.ATTR_PRINTER_LOCATION,
            IppDictPrinterDescAttr.ATTR_PRINTER_GEO_LOCATION,
            IppDictPrinterDescAttr.ATTR_PRINTER_ORGANIZATION,
            IppDictPrinterDescAttr.ATTR_PRINTER_ORGANIZATIONAL_UNIT,
            IppDictPrinterDescAttr.ATTR_PRINTER_CURRENT_TIME,
            IppDictPrinterDescAttr.ATTR_PRINTER_SUPPLY,
            IppDictPrinterDescAttr.ATTR_PRINTER_SUPPLY_DESCRIPTION,
            IppDictPrinterDescAttr.ATTR_PRINTER_SUPPLY_INFO_URI,
            IppDictPrinterDescAttr.ATTR_IDENTIFY_ACTIONS_DEFAULT,
            IppDictPrinterDescAttr.ATTR_IDENTIFY_ACTIONS_SUPPORTED,
            IppDictPrinterDescAttr.ATTR_MEDIA_READY,
            IppDictPrinterDescAttr.ATTR_WHICH_JOBS_SUPPORTED,
            IppDictPrinterDescAttr.ATTR_PRINT_COLOR_MODE_DEFAULT,
            IppDictPrinterDescAttr.ATTR_PRINT_COLOR_MODE_SUPPORTED,
            IppDictPrinterDescAttr.ATTR_PRINT_CONTENT_OPTIMIZE_DEFAULT,
            IppDictPrinterDescAttr.ATTR_PRINT_CONTENT_OPTIMIZE_SUPPORTED,
            IppDictPrinterDescAttr.ATTR_JOB_IDS_SUPPORTED,
            IppDictPrinterDescAttr.ATTR_PAGE_RANGES_SUPPORTED,
            IppDictPrinterDescAttr.ATTR_PWG_RASTER_DOCUMENT_TYPE_SUPPORTED,
            IppDictPrinterDescAttr.ATTR_PWG_RASTER_DOCUMENT_RESOLUTION_SUPPORTED,
            IppDictPrinterDescAttr.ATTR_PRINT_RENDERING_INTENT_DEFAULT,
            IppDictPrinterDescAttr.ATTR_PRINT_RENDERING_INTENT_SUPPORTED,
            IppDictPrinterDescAttr.ATTR_PRINTER_STATE_CHANGE_DATE_TIME,
            IppDictPrinterDescAttr.ATTR_PRINTER_CONFIG_CHANGE_DATE_TIME,
            IppDictPrinterDescAttr.ATTR_PRINTER_CONFIG_CHANGE_TIME,
            IppDictPrinterDescAttr.ATTR_JOB_CREATION_ATTRIBUTES_SUPPORTED,
            IppDictPrinterDescAttr.ATTR_PREFERRED_ATTRIBUTES_SUPPORTED,
            IppDictPrinterDescAttr.ATTR_PRINTER_GET_ATTRIBUTES_SUPPORTED,
            IppDictPrinterDescAttr.ATTR_MEDIA_TOP_MARGIN_SUPPORTED,
            IppDictPrinterDescAttr.ATTR_MEDIA_BOTTOM_MARGIN_SUPPORTED,
            IppDictPrinterDescAttr.ATTR_MEDIA_LEFT_MARGIN_SUPPORTED,
            IppDictPrinterDescAttr.ATTR_MEDIA_RIGHT_MARGIN_SUPPORTED,
            IppDictPrinterDescAttr.ATTR_MULTIPLE_DOCUMENT_JOBS_SUPPORTED,
            IppDictPrinterDescAttr.ATTR_MULTIPLE_OPERATION_TIME_OUT,
            IppDictPrinterDescAttr.ATTR_MULTIPLE_OPERATION_TIME_OUT_ACTION,
            IppDictPrinterDescAttr.ATTR_MEDIA_COL_DEFAULT,
            IppDictPrinterDescAttr.ATTR_MEDIA_COL_READY,
            IppDictPrinterDescAttr.ATTR_MEDIA_COL_SUPPORTED,
            IppDictPrinterDescAttr.ATTR_MEDIA_SIZE_SUPPORTED,
            IppDictPrinterDescAttr.ATTR_OVERRIDES_SUPPORTED,
            IppDictPrinterDescAttr.ATTR_PRINTER_ICONS
            //
    };
    /** */
    private IppStatusCode ippStatusCode;

    /**
     * {@code true} if IPP/2.x.
     */
    private boolean ippVersion2;

    /**
     * The requested printer queue.
     */
    private final IppQueue printerQueue;

    /**
     *
     * @param queue
     *            The requested printer queue (can be {@code null});
     */
    public IppGetPrinterAttrRsp(final IppQueue queue) {
        this.printerQueue = queue;
    }

    /**
     * @return {@code true} if IPP/2.x.
     */
    private boolean isIPPversion2() {
        return this.ippVersion2;
    }

    /**
     *
     * @param operation
     *            IPP operation.
     * @param request
     *            IPP request.
     * @param ostr
     *            IPP output stream.
     * @throws IOException
     *             If error.
     */
    public void process(final IppGetPrinterAttrOperation operation,
            final IppGetPrinterAttrReq request, final OutputStream ostr)
            throws IOException {

        this.ippVersion2 = operation.isIPPversion2();
        this.ippStatusCode = this.determineStatusCode(operation, request);

        if (this.ippStatusCode == IppStatusCode.OK
                && StringUtils.isBlank(request.getPrinterURI())) {
            this.ippStatusCode = IppStatusCode.CLI_BADREQ;
        }

        final List<IppAttrGroup> attrGroups = new ArrayList<>();

        /*
         * Group 1: Operation Attributes
         */
        attrGroups.add(this.createOperationGroup());

        /*
         * In addition to the REQUIRED status code returned in every response,
         * the response OPTIONALLY includes a "status-message" (text(255))
         * and/or a "detailed-status-message" (text(MAX)) operation attribute as
         * described in sections 13 and 3.1.6.
         */

        if (this.ippStatusCode == IppStatusCode.OK) {

            /*
             * Group 2: Unsupported Attributes
             */
            final IppAttrGroup groupAttrUnsupp =
                    new IppAttrGroup(IppDelimiterTag.UNSUPP_ATTR);

            /*
             * Group 3: Printer Object Attributes
             */
            final IppAttrGroup groupAttrSupp =
                    new IppAttrGroup(IppDelimiterTag.PRINTER_ATTR);

            final IppAttrValue printerUriAttr =
                    request.getAttrValue("printer-uri");

            final URI printerUri;

            if (printerUriAttr == null
                    || printerUriAttr.getValues().isEmpty()) {
                printerUri = null;
            } else {
                try {
                    printerUri = new URI(printerUriAttr.getValues().get(0));
                } catch (URISyntaxException e) {
                    throw new IllegalStateException(e.getMessage());
                }
            }

            if (operation.getRequestedAttributes() == null) {
                /*
                 * The client OPTIONALLY supplies a set of attribute names
                 * and/or attribute group names in whose values the requester is
                 * interested. The Printer object MUST support this attribute.
                 *
                 * If the client omits this attribute, the Printer MUST respond
                 * as if this attribute had been supplied with a value of 'all'.
                 */
                this.handleRequestedAttr(printerUri, groupAttrSupp,
                        groupAttrUnsupp,
                        IppGetPrinterAttrOperation.ATTR_GRP_ALL);

            } else {

                if (LOGGER.isDebugEnabled()) {
                    final StringBuilder log = new StringBuilder();
                    log.append("requested attributes:");
                    for (final String keyword : operation
                            .getRequestedAttributes().getValues()) {
                        log.append(" ").append(keyword);
                    }
                    LOGGER.debug(log.toString());
                }

                for (final String keyword : operation.getRequestedAttributes()
                        .getValues()) {
                    this.handleRequestedAttr(printerUri, groupAttrSupp,
                            groupAttrUnsupp, keyword);
                }
            }

            /*
             * If the Printer object is not returning any Unsupported Attributes
             * in the response, the Printer object SHOULD omit Group 2 rather
             * than sending an empty group. However, a client MUST be able to
             * accept an empty group.
             */
            if (!groupAttrUnsupp.getAttributes().isEmpty()) {
                attrGroups.add(groupAttrUnsupp);
            }

            attrGroups.add(groupAttrSupp);
        }

        this.writeHeaderAndAttributes(operation, this.ippStatusCode, attrGroups,
                ostr, request.getAttributesCharset());
    }

    /**
     * Adds Job template attribute values to IPP attribute group.
     *
     * @param attrGroup
     *            IPP attribute group.
     * @param attrNames
     *            Array of IPP attribute names.
     */
    private void addJobTemplateAttrs(final IppAttrGroup attrGroup,
            final String[] attrNames) {

        for (String nameWlk : attrNames) {

            attrGroup.addAttribute(this.getAttrValueJobTemplate(nameWlk,
                    IppDictJobTemplateAttr.ApplEnum.SUPPORTED));

            attrGroup.addAttribute(this.getAttrValueJobTemplate(nameWlk,
                    IppDictJobTemplateAttr.ApplEnum.DEFAULT));
        }
    }

    /**
     * Adds Printer Description attribute values to IPP attribute group.
     *
     * @param printerUri
     *            Printer URI.
     * @param attrGroup
     *            IPP attribute group.
     * @param attrNames
     *            Array of IPP attribute names.
     */
    private void addPrinterDescriptionAttrs(final URI printerUri,
            final IppAttrGroup attrGroup, final String[] attrNames) {

        for (String nameWlk : attrNames) {

            switch (nameWlk) {

            case IppDictPrinterDescAttr.ATTR_MEDIA_SIZE_SUPPORTED:
                attrGroup.addCollection(
                        IppMediaSizeHelper.createMediaCollectionSet(nameWlk,
                                IPP_MEDIA_SUPPORTED));
                break;

            case IppDictPrinterDescAttr.ATTR_MEDIA_COL_READY:
                final IppAttrCollection colDatabase = IppMediaSizeHelper
                        .createMediaCollection(nameWlk, IPP_MEDIA_READY);
                attrGroup.addCollection(colDatabase);
                break;

            case IppDictPrinterDescAttr.ATTR_MEDIA_COL_DEFAULT:
                final IppAttrCollection collection =
                        new IppAttrCollection(nameWlk);
                collection.addCollection(
                        IppMediaSizeHelper.createMediaSizeCollection(
                                IPP_MEDIA_DEFAULT.getIppKeyword()));
                attrGroup.addCollection(collection);
                break;

            default:
                attrGroup.addAttribute(
                        this.getAttrValuePrinterDesc(nameWlk, printerUri));
                break;
            }
        }
    }

    /**
     *
     * @param printerUri
     *            Printer URI.
     * @param grpSupp
     *            IPP group of supported attributes.
     * @param grpUnSupp
     *            IPP group of unsupported attributes.
     * @param name
     *            IPP keyword.
     */
    private void handleRequestedAttr(final URI printerUri,
            final IppAttrGroup grpSupp, final IppAttrGroup grpUnSupp,
            final String name) {

        IppAttrValue value = null;
        IppAttr attr = null;

        switch (name) {

        case IppGetPrinterAttrOperation.ATTR_GRP_NONE:
            // noop
            break;

        case IppGetPrinterAttrOperation.ATTR_GRP_ALL:
            // Recurse.
            this.handleRequestedAttr(printerUri, grpSupp, grpUnSupp,
                    IppGetPrinterAttrOperation.ATTR_GRP_PRINTER_DESC);
            // Recurse.
            this.handleRequestedAttr(printerUri, grpSupp, grpUnSupp,
                    IppGetPrinterAttrOperation.ATTR_GRP_JOB_TPL);
            break;

        case IppGetPrinterAttrOperation.ATTR_GRP_JOB_TPL:
            this.addJobTemplateAttrs(grpSupp, SUPPORTED_ATTR_JOB_TPL_V1);
            if (this.isIPPversion2()) {
                this.addJobTemplateAttrs(grpSupp, SUPPORTED_ATTR_JOB_TPL_V2);
            }
            break;

        case IppGetPrinterAttrOperation.ATTR_GRP_PRINTER_DESC:
            this.addPrinterDescriptionAttrs(printerUri, grpSupp,
                    SUPPORTED_ATTR_PRINTER_DESC_V1);
            if (this.isIPPversion2()) {
                this.addPrinterDescriptionAttrs(printerUri, grpSupp,
                        SUPPORTED_ATTR_PRINTER_DESC_V2);
            }
            break;

        case IppGetPrinterAttrOperation.ATTR_GRP_MEDIA_COL_DATABASE:
            final IppAttrCollection colDatabase = IppMediaSizeHelper
                    .createMediaCollection(name, IPP_MEDIA_SUPPORTED);
            colDatabase.addAttribute(createValueMediaSourceAuto());
            grpSupp.addCollection(colDatabase);
            break;

        case IppDictPrinterDescAttr.ATTR_COPIES_SUPPORTED:

            attr = new IppAttr(name, new IppRangeOfInteger());
            value = new IppAttrValue(attr);
            value.addValue("1:2");
            grpSupp.addAttribute(value);

            break;

        default:
            value = this.getAttrValuePrinterDesc(name, printerUri);
            if (value != null) {
                grpSupp.addAttribute(value);
                break;
            } else {
                if (this.handleRequestedAttrJobTemplate(grpSupp, name,
                        printerUri)) {
                    break;
                }
            }
            this.ippStatusCode = IppStatusCode.OK_ATTRIGN;
            break;
        }
    }

    /**
     * @param grpSupp
     * @param name
     *            IPP attribute name.
     * @param printerUri
     *            Printer URI.
     */
    private boolean handleRequestedAttrJobTemplate(final IppAttrGroup grpSupp,
            final String name, final URI printerUri) {

        if (name.equals(IppDictPrinterDescAttr.ATTR_MEDIA_SIZE_SUPPORTED)) {
            grpSupp.addCollection(IppMediaSizeHelper
                    .createMediaCollectionSet(name, IPP_MEDIA_SUPPORTED));
            return true;
        }

        if (name.equals(IppDictPrinterDescAttr.ATTR_MEDIA_COL_DEFAULT)) {
            final IppAttrCollection collection = new IppAttrCollection(name);
            collection
                    .addCollection(IppMediaSizeHelper.createMediaSizeCollection(
                            IPP_MEDIA_DEFAULT.getIppKeyword()));
            grpSupp.addCollection(collection);
            return true;
        }

        if (name.equals(IppDictPrinterDescAttr.ATTR_MEDIA_COL_READY)) {
            grpSupp.addCollection(IppMediaSizeHelper.createMediaCollection(name,
                    IPP_MEDIA_READY));
            return true;
        }

        //
        final String ippBaseName;
        final ApplEnum attrAppl;

        if (name.endsWith(IppDictJobTemplateAttr._DFLT)) {
            ippBaseName =
                    StringUtils.removeEnd(name, IppDictJobTemplateAttr._DFLT);
            attrAppl = ApplEnum.DEFAULT;
        } else if (name.endsWith(IppDictJobTemplateAttr._SUPP)) {
            ippBaseName =
                    StringUtils.removeEnd(name, IppDictJobTemplateAttr._SUPP);
            attrAppl = ApplEnum.SUPPORTED;
        } else {
            ippBaseName = null;
            attrAppl = null;
        }

        final IppAttrValue ippValue;
        if (ippBaseName == null) {
            ippValue = this.getAttrValuePrinterDesc(name, printerUri);
        } else {
            ippValue = this.getAttrValueJobTemplate(ippBaseName, attrAppl);
        }

        if (ippValue != null) {
            grpSupp.addAttribute(ippValue);
            return true;
        }

        return false;
    }

    /**
     * Get "-default" and "-supported" value of IPP attribute base name.
     *
     * @param ippBaseName
     *            IPP attribute base name.
     * @param attrAppl
     *            {@link ApplEnum}.
     * @return {@code NULL} if the NOT supported
     */
    private IppAttrValue getAttrValueJobTemplate(final String ippBaseName,
            final ApplEnum attrAppl) {

        IppAttr attr = IppDictJobTemplateAttr.instance().getAttr(ippBaseName);

        if (attr == null) {
            return null;
        }

        if (attrAppl == ApplEnum.DEFAULT) {

            attr = attr.copy(ippBaseName + IppDictJobTemplateAttr._DFLT);

        } else {

            final String nameSupported =
                    ippBaseName + IppDictJobTemplateAttr._SUPP;

            /*
             * Some "supported" variants have a different syntax.
             */
            if (ippBaseName.equals(IppDictJobTemplateAttr.ATTR_COPIES)) {
                attr = new IppAttr(nameSupported, new IppRangeOfInteger());
            } else {
                attr = attr.copy(nameSupported);
            }
        }

        /*
         * Create value wrapper
         */
        IppAttrValue value = new IppAttrValue(attr);

        switch (ippBaseName) {

        case IppDictJobTemplateAttr.ATTR_COPIES:
            if (attrAppl == ApplEnum.DEFAULT) {
                value.addValue("1");
            } else {
                value.addValue("1:1");
            }
            break;

        case IppDictJobTemplateAttr.ATTR_MEDIA:
            // Same as IppDictPrinterDescAttr.ATTR_MEDIA_READY:
            if (attrAppl == ApplEnum.DEFAULT) {
                value.addValue(IPP_MEDIA_DEFAULT.getIppKeyword());
            } else {
                for (final IppMediaSizeEnum mediaSize : IPP_MEDIA_SUPPORTED) {
                    value.addValue(mediaSize.getIppKeyword());
                }
            }
            break;

        case IppDictJobTemplateAttr.ATTR_PRINTER_RESOLUTION:
            value.addValue(IppResolution.DPI_600X600);
            break;

        case IppDictJobTemplateAttr.ATTR_ORIENTATION_REQUESTED:
            value.addValue("3"); // portrait
            if (attrAppl == ApplEnum.SUPPORTED) {
                value.addValue("4"); // landscape
                value.addValue("5"); // reverse-landscape
                value.addValue("6"); // reverse-portrait
            }
            break;

        case IppDictJobTemplateAttr.ATTR_NUMBER_UP:
            value.addValue("1");
            break;

        case IppDictJobTemplateAttr.ATTR_JOB_SHEETS:
            value.addValue("none");
            break;

        case IppDictJobTemplateAttr.ATTR_OUTPUT_BIN:
            value.addValue(IppKeyword.OUTPUT_BIN_AUTO);
            break;
        case IppDictJobTemplateAttr.ATTR_SIDES:
            value.addValue(IppKeyword.SIDES_ONE_SIDED);
            break;
        case IppDictJobTemplateAttr.ATTR_PRINT_QUALITY:
            value.addValue(IppKeyword.PRINT_QUALITY_HIGH);
            break;
        case IppDictJobTemplateAttr.ATTR_PRINT_SCALING:
            value.addValue("auto");
            break;
        default:
            // unsupported
            value = null;
            break;
        }
        return value;
    }

    /**
     * @return Composed printer name with {@link CommunityDictEnum#SAVAPAGE} and
     *         optional printer queue name suffix.
     */
    private String composePrinterName() {
        final StringBuilder printerName = new StringBuilder();
        printerName.append(CommunityDictEnum.SAVAPAGE.getWord());

        if (this.printerQueue != null
                && StringUtils.isNotBlank(this.printerQueue.getUrlPath())) {
            printerName.append("-").append(this.printerQueue.getUrlPath());
        }
        return printerName.toString();
    }

    /**
     * Gets the supported value for a REQUIRED or OPTIONAL attribute of the
     * SavaPage Printer.
     *
     * @param name
     *            IPP attribute name.
     * @param printerUri
     *            URI of printer.
     * @return {@code NULL} if the NOT supported
     */
    private IppAttrValue getAttrValuePrinterDesc(final String name,
            final URI printerUri) {

        IppAttr attr = IppDictPrinterDescAttr.instance().getAttr(name);
        if (attr == null) {
            return null;
        }

        IppAttrValue value = new IppAttrValue(attr);

        switch (name) {

        case IppDictPrinterDescAttr.ATTR_PRINTER_URI_SUPPORTED:

            if (printerUri != null) {
                if (!this.isIPPversion2()) {
                    value.addValue(getPrinterUriSupported(printerUri, "ipp",
                            ConfigManager.getServerPort()));
                    value.addValue(getPrinterUriSupported(printerUri, "http",
                            ConfigManager.getServerPort()));
                    value.addValue(getPrinterUriSupported(printerUri, "https",
                            ConfigManager.getServerSslPort()));
                }
                value.addValue(getPrinterUriSupported(printerUri, "ipps",
                        ConfigManager.getServerSslPort()));
            }
            break;

        case IppDictPrinterDescAttr.ATTR_URI_AUTH_SUPPORTED:
            /*
             * This REQUIRED Printer attribute MUST have the same cardinality
             * (contain the same number of values) as the
             * "printer-uri-supported" attribute.
             *
             * Possible values: 'none', 'requesting-user-name', 'basic',
             * 'digest', 'certificate'.
             */
            if (!this.isIPPversion2()) {
                value.addValue(IppDictOperationAttr.ATTR_REQUESTING_USER_NAME);
                value.addValue(IppDictOperationAttr.ATTR_REQUESTING_USER_NAME);
                value.addValue(IppDictOperationAttr.ATTR_REQUESTING_USER_NAME);
            }
            value.addValue(IppDictOperationAttr.ATTR_REQUESTING_USER_NAME);
            break;

        case IppDictPrinterDescAttr.ATTR_URI_SECURITY_SUPPORTED:
            /*
             * This REQUIRED Printer attribute MUST have the same cardinality
             * (contain the same number of values) as the
             * "printer-uri-supported" attribute.
             */
            if (!this.isIPPversion2()) {
                value.addValue("none");
                value.addValue("none");
                value.addValue("tls");
            }
            value.addValue("tls");
            break;

        case IppDictPrinterDescAttr.ATTR_AUTH_INFO_REQUIRED:
            value.addValue("none");
            break;

        case IppDictPrinterDescAttr.ATTR_PRINTER_NAME:
            value.addValue(this.composePrinterName());
            break;

        case IppDictPrinterDescAttr.ATTR_PRINTER_TYPE:
            value.addValue(String.valueOf(
                    IppPrinterType.BitEnum.CAN_PRINT_IN_COLOR.asInt()));
            break;

        case IppDictPrinterDescAttr.ATTR_PRINTER_STATE:
            value.addValue(IppDictPrinterDescAttr.PRINTER_STATE_IDLE);
            break;

        case IppDictPrinterDescAttr.ATTR_PRINTER_STATE_REASONS:
            if (this.printerQueue == null
                    || BooleanUtils.isTrue(this.printerQueue.getDisabled())) {
                // Satisfy IPP-everywhere self certification test.
                value.addValue("media-needed");
            } else {
                value.addValue("none");
            }
            break;

        case IppDictPrinterDescAttr.ATTR_IPP_VERSIONS_SUPP:
            for (final IppVersionEnum version : IppVersionEnum.values()) {
                if (version.isSupported()) {
                    value.addValue(version.getVersionKeyword());
                }
            }
            break;

        case IppDictPrinterDescAttr.ATTR_IPP_FEATURES_SUPP:
            if (this.isIPPversion2()) {
                // PWG 5100.14 – IPP Everywhere
                value.addValue("ipp-everywhere");
            } else {
                value.addValue("none");
            }
            break;

        case IppDictPrinterDescAttr.ATTR_OPERATIONS_SUPPORTED:
            for (final IppOperationId id : IppOperationId.supportedV1()) {
                value.addValue(String.valueOf(id.asInt()));
            }
            if (this.isIPPversion2()) {
                for (final IppOperationId id : IppOperationId.supportedV2()) {
                    value.addValue(String.valueOf(id.asInt()));
                }
            }
            break;

        case IppDictPrinterDescAttr.ATTR_CHARSET_CONFIGURED:
            value.addValue("utf-8");
            break;

        case IppDictPrinterDescAttr.ATTR_CHARSET_SUPPORTED:
            value.addValue("utf-8");
            break;

        case IppDictPrinterDescAttr.ATTR_NATURAL_LANG_CONFIGURED:
            value.addValue("en-us");
            break;

        case IppDictPrinterDescAttr.ATTR_GENERATED_NATURAL_LANG_SUPPORTED:
            value.addValue("en-us");
            break;

        case IppDictPrinterDescAttr.ATTR_IDENTIFY_ACTIONS_DEFAULT:
            // no break intended.
        case IppDictPrinterDescAttr.ATTR_IDENTIFY_ACTIONS_SUPPORTED:
            value.addValue("display");
            break;

        case IppDictPrinterDescAttr.ATTR_DOC_FORMAT_DEFAULT:
            value.addValue(IppDictPrinterDescAttr.DOCUMENT_FORMAT_PDF);
            break;

        case IppDictPrinterDescAttr.ATTR_DOC_FORMAT_SUPPORTED:

            value.addValue(IppDictPrinterDescAttr.DOCUMENT_FORMAT_PDF);
            value.addValue(IppDictPrinterDescAttr.DOCUMENT_FORMAT_POSTSCRIPT);

            if (this.isIPPversion2()) {
                value.addValue(IppDictPrinterDescAttr.DOCUMENT_FORMAT_JPEG);
                value.addValue(
                        IppDictPrinterDescAttr.DOCUMENT_FORMAT_PWG_RASTER);
            }
            /*
             * IMPORTANT: image/urf MUST be present for iOS printing !!!!
             */
            value.addValue(IppDictPrinterDescAttr.DOCUMENT_FORMAT_URF);

            break;

        case IppDictPrinterDescAttr.ATTR_PRINTER_IS_ACCEPTING_JOBS:
            value.addValue(IppBoolean.TRUE);
            break;

        case IppDictPrinterDescAttr.ATTR_QUEUES_JOB_COUNT:
            value.addValue("0");
            break;

        case IppDictPrinterDescAttr.ATTR_PDL_OVERRIDE_SUPPORTED:
            value.addValue("not-attempted");
            break;

        case IppDictPrinterDescAttr.ATTR_PRINTER_UP_TIME:
            value.addValue(String.valueOf(IppInteger.getPrinterUpTime()));
            break;

        case IppDictPrinterDescAttr.ATTR_COMPRESSION_SUPPORTED:
            value.addValue("none");
            break;

        case IppDictPrinterDescAttr.ATTR_PRINTER_STATE_MESSAGE:
            value.addValue("SavaPage is ready!");
            break;

        case IppDictPrinterDescAttr.ATTR_PRINTER_STATE_CHANGE_TIME:
            value.addValue("0"); // TODO
            break;

        case IppDictPrinterDescAttr.ATTR_PRINTER_CURRENT_TIME:
            value.addValue(IppDateTime.formatDate(new Date()));
            break;

        case IppDictPrinterDescAttr.ATTR_PRINTER_MAKE_MODEL:
            value.addValue(ConfigManager.getAppNameVersionBuild());
            break;

        case IppDictPrinterDescAttr.ATTR_PRINTER_LOCATION:
            value.addValue("SavaPage Print Server");
            break;

        case IppDictPrinterDescAttr.ATTR_PRINTER_GEO_LOCATION:
            // North pole for now :-)
            value.addValue("geo:90.000000,0.000000");
            break;

        case IppDictPrinterDescAttr.ATTR_PRINTER_INFO:
            value.addValue("SavaPage Virtual Printer");
            break;

        case IppDictPrinterDescAttr.ATTR_PRINTER_MORE_INFO:
            /*
             * Value must match
             * <txt-record>adminurl=https://host:port/user</txt-record> in Avahi
             * service file.
             */
            value.addValue(ConfigManager.getWebAppUserSslUrl().toString());
            break;

        case IppDictPrinterDescAttr.ATTR_PRINTER_ORGANIZATION:
            value.addValue(MemberCard.instance().getMemberOrganisation());
            break;

        case IppDictPrinterDescAttr.ATTR_PRINTER_ORGANIZATIONAL_UNIT:
            value.addValue(
                    MemberCard.instance().getStatusUserText(Locale.ENGLISH));
            break;

        case IppDictPrinterDescAttr.ATTR_PRINTER_SUPPLY:
            final String format =
                    "type=toner;maxcapacity=100;level=100;index=%s;"
                            + "markerindex=1;"
                            + "class=supplyThatIsConsumed;unit=percent;"
                            + "colorantindex=4;colorantrole=process;"
                            + "colorantname=%s;coloranttonality=128;";
            value.addValue(String.format(format, "1", "cyan"));
            value.addValue(String.format(format, "2", "magenta"));
            value.addValue(String.format(format, "3", "yellow"));
            value.addValue(String.format(format, "4", "black"));
            break;

        case IppDictPrinterDescAttr.ATTR_PRINTER_SUPPLY_DESCRIPTION:
            /// Same cardinality as ATTR_PRINTER_SUPPLY
            final String formatDesc = "%s Virtual Toner Cartridge";
            value.addValue(String.format(formatDesc, "Cyan"));
            value.addValue(String.format(formatDesc, "Magenta"));
            value.addValue(String.format(formatDesc, "Yellow"));
            value.addValue(String.format(formatDesc, "Black"));
            break;

        case IppDictPrinterDescAttr.ATTR_PRINTER_SUPPLY_INFO_URI:
            value.addValue(String.format("http://%s:%s", printerUri.getHost(),
                    ConfigManager.getServerPort()));
            break;

        case IppDictPrinterDescAttr.ATTR_PRINTER_ICONS:
            if (printerUri != null) {
                for (final String icon : PRINTER_ICONS_PNG) {
                    value.addValue(String.format("http://%s:%s%s/%s",
                            printerUri.getHost(), ConfigManager.getServerPort(),
                            ConfigManager.getIppPrinterIconsUrlPath(), icon));
                }
            }
            break;
        case IppDictPrinterDescAttr.ATTR_PRINTER_UUID:
            /*
             * Value must match <txt-record>UUID=xxxx</txt-record> in Avahi
             * service file.
             */
            final String printerUUID = ConfigManager.getIppPrinterUuid();
            if (StringUtils.isNotBlank(printerUUID)) {
                value.addValue(IppUri.getUrnUuid(printerUUID));
            }
            break;

        case IppDictPrinterDescAttr.ATTR_PRINTER_DEVICE_ID:
            // IEEE 1284 Device ID strings
            // MODEL == MDL
            // MANUFACTURER == MFG
            // COMMAND SET == CMD
            value.addValue(
                    "MFG:SavaPage;" + "MDL:SavaPage;" + "DESCRIPTION:SavaPage;"
            // + "CMD:PS,PDF;" // ??
            );
            break;

        case IppDictPrinterDescAttr.ATTR_DOC_PASSWORD_SUPPORTED:
            value.addValue("0");
            break;

        case IppDictPrinterDescAttr.ATTR_PRINTER_MORE_INFO_MANUFACTURER:
            value.addValue(
                    CommunityDictEnum.SAVAPAGE_WWW_DOT_ORG_URL.getWord());
            break;

        case IppDictPrinterDescAttr.ATTR_PAGES_PER_MIN:
        case IppDictPrinterDescAttr.ATTR_PAGES_PER_MIN_COLOR:
            value.addValue("120");
            break;

        case IppDictPrinterDescAttr.ATTR_COLOR_SUPPORTED:
            value.addValue(IppBoolean.TRUE);
            break;

        case IppDictPrinterDescAttr.ATTR_CUPS_VERSION:
            value.addValue(CUPSVersionHolder.VERSION);
            break;

        case IppDictPrinterDescAttr.ATTR_PRINT_CONTENT_OPTIMIZE_DEFAULT:
            value.addValue("auto");
            break;

        case IppDictPrinterDescAttr.ATTR_PRINT_CONTENT_OPTIMIZE_SUPPORTED:
            value.addValue("auto");
            break;

        case IppDictPrinterDescAttr.ATTR_JOB_IDS_SUPPORTED:
            value.addValue(IppBoolean.TRUE);
            break;

        case IppDictPrinterDescAttr.ATTR_PAGE_RANGES_SUPPORTED:
            value.addValue(IppBoolean.TRUE);
            break;

        case IppDictPrinterDescAttr.ATTR_MULTIPLE_DOCUMENT_JOBS_SUPPORTED:
            value.addValue(IppBoolean.FALSE);
            break;

        case IppDictPrinterDescAttr.ATTR_MULTIPLE_OPERATION_TIME_OUT:
            /*
             * RFC 8011: Printers SHOULD use a value between '60' and '240'
             * (seconds).
             */
            value.addValue("60");
            break;

        case IppDictPrinterDescAttr.ATTR_MULTIPLE_OPERATION_TIME_OUT_ACTION:
            value.addValue("abort-job");
            break;

        case IppDictPrinterDescAttr.ATTR_PWG_RASTER_DOCUMENT_TYPE_SUPPORTED:
            // PWG 5102.4-2012 – PWG Raster Format
            value.addValue("adobe-rgb_16");
            value.addValue("adobe-rgb_8");
            value.addValue("cmyk_16");
            value.addValue("cmyk_8");
            value.addValue("rgb_16");
            value.addValue("rgb_8");
            value.addValue("srgb_16");
            value.addValue("srgb_8");
            break;

        case IppDictPrinterDescAttr.ATTR_PWG_RASTER_DOCUMENT_RESOLUTION_SUPPORTED:
            // PWG 5102.4-2012 – PWG Raster Format
            value.addValue(IppResolution.format(600, 600, IppResolution.DPI));
            value.addValue(IppResolution.format(1200, 1200, IppResolution.DPI));
            break;

        case IppDictPrinterDescAttr.ATTR_OVERRIDES_SUPPORTED:
            // PWG 5100.6-2003 Standard for IPP: Page Overrides
            value.addValue("pages");
            value.addValue("document-numbers");
            break;

        case IppDictPrinterDescAttr.ATTR_PRINT_RENDERING_INTENT_DEFAULT:
            // PWG 5100.13 – IPP: Job and Printer Extensions – Set 3
            value.addValue("auto");
            break;

        case IppDictPrinterDescAttr.ATTR_PRINT_RENDERING_INTENT_SUPPORTED:
            // PWG 5100.13 – IPP: Job and Printer Extensions – Set 3
            // REQUIRED.
            value.addValue("auto");
            value.addValue("relative");
            value.addValue("relative-bpc");
            break;

        case IppDictPrinterDescAttr.ATTR_PRINTER_STATE_CHANGE_DATE_TIME:
            // TODO: for now no break intended.
        case IppDictPrinterDescAttr.ATTR_PRINTER_CONFIG_CHANGE_DATE_TIME:
            value.addValue(IppDateTime
                    .formatDate(new Date(SystemInfo.getStarttime())));
            break;

        case IppDictPrinterDescAttr.ATTR_PRINTER_GET_ATTRIBUTES_SUPPORTED:
            value.addValue("document-format");
            break;

        case IppDictPrinterDescAttr.ATTR_JOB_CREATION_ATTRIBUTES_SUPPORTED:
            for (final String val : SUPPORTED_ATTR_JOB_TPL_V1) {
                value.addValue(val);
            }
            if (this.isIPPversion2()) {
                for (final String val : SUPPORTED_ATTR_JOB_TPL_V2) {
                    value.addValue(val);
                }
            }
            break;

        case IppDictPrinterDescAttr.ATTR_PREFERRED_ATTRIBUTES_SUPPORTED:
            value.addValue(IppBoolean.FALSE);
            break;

        case IppDictPrinterDescAttr.ATTR_PRINTER_CONFIG_CHANGE_TIME:
            // integer(1:MAX)
            int uptime = (int) (SystemInfo.getUptime()
                    / DateUtil.DURATION_MSEC_SECOND);
            if (uptime == 0) {
                uptime++;
            } else if (uptime > IppInteger.MAX) {
                uptime = IppInteger.MAX;
            }
            value.addValue(String.valueOf(uptime));
            break;

        case IppDictPrinterDescAttr.ATTR_PRINT_COLOR_MODE_DEFAULT:
            value.addValue(IppKeyword.PRINT_COLOR_MODE_AUTO);
            break;

        case IppDictPrinterDescAttr.ATTR_PRINT_COLOR_MODE_SUPPORTED:
            value.addValue(IppKeyword.PRINT_COLOR_MODE_AUTO);
            break;

        case IppDictPrinterDescAttr.ATTR_JOB_PASSWORD_ENCRYPTION_SUPPORTED:
            value.addValue(IppKeyword.JOB_PASSWORD_ENCRYPTION_MD5);
            break;

        case IppDictPrinterDescAttr.ATTR_MULTIPLE_DOCUMENT_HANDLING_SUPPORTED:
            value.addValue(IppKeyword.MULTIPLE_DOCUMENT_HANDLING_SINGLE);
            break;

        case IppDictPrinterDescAttr.ATTR_MEDIA_TOP_MARGIN_SUPPORTED:
        case IppDictPrinterDescAttr.ATTR_MEDIA_BOTTOM_MARGIN_SUPPORTED:
        case IppDictPrinterDescAttr.ATTR_MEDIA_LEFT_MARGIN_SUPPORTED:
        case IppDictPrinterDescAttr.ATTR_MEDIA_RIGHT_MARGIN_SUPPORTED:
            value.addValue("0");
            break;

        case IppDictPrinterDescAttr.ATTR_MEDIA_COL_SUPPORTED:
            value.addValue(IppDictJobTemplateAttr.ATTR_MEDIA_SIZE);
            break;

        case IppDictPrinterDescAttr.ATTR_MEDIA_READY:
            // As in IppDictJobTemplateAttr.ATTR_MEDIA
            for (final IppMediaSizeEnum mediaSize : IPP_MEDIA_READY) {
                value.addValue(mediaSize.getIppKeyword());
            }
            break;

        case IppDictPrinterDescAttr.ATTR_WHICH_JOBS_SUPPORTED:
            // Required by IPP everywhere.
            value.addValue(IppGetJobsOperation.WHICH_JOB_COMPLETED);
            // Required by IPP everywhere.
            value.addValue(IppGetJobsOperation.WHICH_JOB_NOT_COMPLETED);
            break;

        default:
            // unsupported
            break;
        }
        return value;
    }

    /**
     *
     * @param uri
     * @param uriScheme
     * @param port
     * @return Printer URI.
     */
    private static String getPrinterUriSupported(final URI uri,
            final String uriScheme, final String port) {

        final StringBuilder jobUri = new StringBuilder().append(uriScheme)
                .append("://").append(uri.getHost()).append(":").append(port);
        final String path = uri.getPath();
        if (path != null) {
            jobUri.append(path);
        }
        return jobUri.toString();
    }

    /**
     *
     * @return {@link IppAttrValue} with keyword 'media-source' and value
     *         'auto'.
     */
    private static IppAttrValue createValueMediaSourceAuto() {
        final IppAttr attr = IppDictJobTemplateAttr.instance()
                .getAttr(IppDictJobTemplateAttr.ATTR_MEDIA_SOURCE);
        final IppAttrValue attrValue = new IppAttrValue(attr);
        attrValue.addValue(IppKeyword.MEDIA_SOURCE_AUTO);
        return attrValue;
    }
}
