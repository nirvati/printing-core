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
import java.lang.management.ManagementFactory;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.savapage.core.community.CommunityDictEnum;
import org.savapage.core.community.MemberCard;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.ipp.IppMediaSizeEnum;
import org.savapage.core.ipp.IppVersionEnum;
import org.savapage.core.ipp.attribute.AbstractIppDict;
import org.savapage.core.ipp.attribute.IppAttr;
import org.savapage.core.ipp.attribute.IppAttrCollection;
import org.savapage.core.ipp.attribute.IppAttrGroup;
import org.savapage.core.ipp.attribute.IppAttrValue;
import org.savapage.core.ipp.attribute.IppDictJobTemplateAttr;
import org.savapage.core.ipp.attribute.IppDictJobTemplateAttr.ApplEnum;
import org.savapage.core.ipp.attribute.IppDictPrinterDescAttr;
import org.savapage.core.ipp.attribute.syntax.IppBoolean;
import org.savapage.core.ipp.attribute.syntax.IppDateTime;
import org.savapage.core.ipp.attribute.syntax.IppKeyword;
import org.savapage.core.ipp.attribute.syntax.IppRangeOfInteger;
import org.savapage.core.ipp.attribute.syntax.IppResolution;
import org.savapage.core.ipp.encoding.IppDelimiterTag;
import org.savapage.core.ipp.helpers.IppMediaSizeHelper;
import org.savapage.core.jpa.IppQueue;
import org.savapage.core.services.ServiceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 *
 * 3.2.5.2 Get-Printer-Attributes Response.
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

    /**
     * Job Template attributes supported by SavaPage Printer.
     */
    private static final String[] SUPPORTED_ATTR_JOB_TPL = {
            //
            IppDictJobTemplateAttr.ATTR_COPIES,
            IppDictJobTemplateAttr.ATTR_MEDIA,
            IppDictJobTemplateAttr.ATTR_ORIENTATION_REQUESTED,

            /* PWG 5100.12 - IPP/2.0 Standard. */
            IppDictJobTemplateAttr.ATTR_OUTPUT_BIN,
            IppDictJobTemplateAttr.ATTR_SIDES,
            IppDictJobTemplateAttr.ATTR_PRINT_QUALITY,
            IppDictJobTemplateAttr.ATTR_PRINTER_RESOLUTION
            //
    };

    /**
     * Printer attributes supported by SavaPage Printer.
     */
    private static final String[] SUPPORTED_ATTR_PRINTER_DESC = {
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

            /* OPTIONAL: but needed for IPP Everywhere */
            IppDictPrinterDescAttr.ATTR_IPP_FEATURES_SUPP,
            IppDictPrinterDescAttr.ATTR_PRINTER_DEVICE_ID,
            IppDictPrinterDescAttr.ATTR_PRINTER_UUID,
            IppDictPrinterDescAttr.ATTR_DOC_PASSWORD_SUPPORTED,
            IppDictPrinterDescAttr.ATTR_PRINTER_STATE_MESSAGE,
            IppDictPrinterDescAttr.ATTR_PRINTER_STATE_CHANGE_TIME,
            IppDictPrinterDescAttr.ATTR_PRINTER_MAKE_MODEL,
            IppDictPrinterDescAttr.ATTR_PRINTER_INFO,
            IppDictPrinterDescAttr.ATTR_PRINTER_MORE_INFO,
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
            IppDictPrinterDescAttr.ATTR_PRINT_CONTENT_OPTIMIZE_SUPPORTED
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

        /**
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
             *
             * See section 3.1.7 for details on returning Unsupported
             * Attributes.
             *
             * The response NEED NOT contain the "requested-attributes"
             * operation attribute with any supplied values (attribute keywords)
             * that were requested by the client but are not supported by the
             * IPP object.
             *
             * If the Printer object does return unsupported attributes
             * referenced in the "requested-attributes" operation attribute and
             * that attribute included group names, such as 'all', the
             * unsupported attributes MUST NOT include attributes described in
             * the standard but not supported by the implementation.
             */
            final IppAttrGroup groupAttrUnsupp =
                    new IppAttrGroup(IppDelimiterTag.UNSUPP_ATTR);

            /*
             * Group 3: Printer Object Attributes
             *
             * This is the set of requested attributes and their current values.
             *
             * The Printer object ignores (does not respond with) any requested
             * attribute which is not supported.
             *
             * The Printer object MAY respond with a subset of the supported
             * attributes and values, depending on the security policy in force.
             *
             * However, the Printer object MUST respond with the 'unknown' value
             * for any supported attribute (including all REQUIRED attributes)
             * for which the Printer object does not know the value.
             *
             * Also the Printer object MUST respond with the 'no-value' for any
             * supported attribute (including all REQUIRED attributes) for which
             * the system administrator has not configured a value. See the
             * description of the "out-of-band" values in the beginning of
             * Section 4.1.
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
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Client requested NO attributes: using ["
                            + IppGetPrinterAttrOperation.ATTR_GRP_ALL
                            + "] attributes");
                }

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
     *
     * @param grpSupp
     * @param grpUnsupp
     * @param name
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

            handleRequestedAttr(printerUri, grpSupp, grpUnSupp,
                    IppGetPrinterAttrOperation.ATTR_GRP_PRINTER_DESC);

            handleRequestedAttr(printerUri, grpSupp, grpUnSupp,
                    IppGetPrinterAttrOperation.ATTR_GRP_JOB_TPL);

            break;

        case IppGetPrinterAttrOperation.ATTR_GRP_JOB_TPL:

            for (String nameWlk : SUPPORTED_ATTR_JOB_TPL) {

                grpSupp.addAttribute(getAttrValueJobTemplate(nameWlk,
                        IppDictJobTemplateAttr.ApplEnum.SUPPORTED));

                grpSupp.addAttribute(getAttrValueJobTemplate(nameWlk,
                        IppDictJobTemplateAttr.ApplEnum.DEFAULT));
            }
            break;

        case IppGetPrinterAttrOperation.ATTR_GRP_PRINTER_DESC:

            for (String nameWlk : SUPPORTED_ATTR_PRINTER_DESC) {
                grpSupp.addAttribute(
                        getAttrValuePrinterDesc(nameWlk, printerUri));
            }
            break;

        case IppGetPrinterAttrOperation.ATTR_GRP_MEDIA_COL_DATABASE:

            final IppAttrCollection colDatabase = new IppAttrCollection(
                    IppGetPrinterAttrOperation.ATTR_GRP_MEDIA_COL_DATABASE);

            grpSupp.addCollection(colDatabase);

            final IppAttrCollection collection = new IppAttrCollection(
                    IppDictJobTemplateAttr.ATTR_MEDIA_COL);
            colDatabase.addCollection(collection);

            collection.addCollection(
                    IppMediaSizeHelper.createMediaSizeCollection());
            collection.addAttribute(createValueMediaSource());

            break;

        /*
         * 4.2 Job Template Attributes
         *
         * Job Template attributes describe job processing behavior.
         *
         * Support for Job Template attributes by a Printer object is OPTIONAL
         * (see section 12.2.3 for a description of support for OPTIONAL
         * attributes).
         *
         * Also, clients OPTIONALLY supply Job Template attributes in create
         * requests.
         *
         * Job Template attributes conform to the following rules. For each Job
         * Template attribute called "xxx":
         *
         * 1. If the Printer object supports "xxx" then it MUST support both a
         * "xxx-default" attribute (unless there is a "No" in the table below)
         * and a "xxx-supported" attribute. If the Printer object doesn't
         * support "xxx", then it MUST support neither an "xxx-
         * default" attribute nor an "xxx-supported" attribute, and it MUST
         * treat an attribute "xxx" supplied by a client as unsupported. An
         * attribute "xxx" may be supported for some document formats and not
         * supported for other document formats. For example, it is expected
         * that a Printer object would only support "orientation-requested" for
         * some document formats (such as 'text/plain' or 'text/html') but not
         * others (such as 'application/postscript').
         *
         * 2. "xxx" is OPTIONALLY supplied by the client in a create request. If
         * "xxx" is supplied, the client is indicating a desired job processing
         * behavior for this Job. When "xxx" is not supplied, the client is
         * indicating that the Printer object apply its default job processing
         * behavior at job processing time if the document content does not
         * contain an embedded instruction indicating an xxx-related behavior.
         *
         * Since an administrator MAY change the default value attribute after a
         * Job object has been submitted but before it has been processed, the
         * default value used by the Printer object at job processing time may
         * be different that the default value in effect at job submission time.
         *
         * 3. The "xxx-supported" attribute is a Printer object attribute that
         * describes which job processing behaviors are supported by that
         * Printer object. A client can query the Printer object to find out
         * what xxx-related behaviors are supported by inspecting the returned
         * values of the "xxx-supported" attribute.
         *
         * Note: The "xxx" in each "xxx-supported" attribute name is singular,
         * even though an "xxx-supported" attribute usually has more than one
         * value, such as "job-sheet-supported", unless the "xxx" Job Template
         * attribute is plural, such as "finishings" or "sides". In such cases
         * the "xxx-supported" attribute names are: "finishings- supported" and
         * "sides-supported".
         *
         * 4. The "xxx-default" default value attribute describes what will be
         * done at job processing time when no other job processing information
         * is supplied by the client (either explicitly as an IPP attribute in
         * the create request or implicitly as an embedded instruction within
         * the document data).
         */
        case IppDictPrinterDescAttr.ATTR_COPIES_SUPPORTED:

            attr = new IppAttr(name, new IppRangeOfInteger());
            value = new IppAttrValue(attr);
            value.addValue("1:2");
            grpSupp.addAttribute(value);

            break;

        default:

            value = getAttrValuePrinterDesc(name, printerUri);

            if (value != null) {
                grpSupp.addAttribute(value);
                break;
            }

            this.ippStatusCode = IppStatusCode.OK_ATTRIGN;

            /*
             * 'unknown': The attribute is supported by the IPP object, but the
             * value is unknown to the IPP object for some reason.
             *
             * 'unsupported': The attribute is unsupported by the IPP object.
             * This value MUST be returned only as the value of an attribute in
             * the Unsupported Attributes Group.
             *
             * 'no-value': The attribute is supported by the Printer object, but
             * the administrator has not yet configured a value.
             */

            /*
             * Unsupported attributes fall into three categories:
             *
             * 1. The Printer object does not support the supplied attribute (no
             * matter what the attribute syntax or value).
             */

            /*
             * 2. The Printer object does support the attribute, but does not
             * support some or all of the particular attribute syntaxes or
             * values supplied by the client (i.e., the Printer object does not
             * have those attribute syntaxes or values in its corresponding
             * "xxx-supported" attribute).
             *
             * In the case of a supported attribute with one or more unsupported
             * attribute syntaxes or values, the Printer object simply returns
             * the client-supplied attribute with the unsupported attribute
             * syntaxes or values as supplied by the client. This indicates
             * support for the attribute, but no support for that particular
             * attribute syntax or value. If the client supplies a multi-valued
             * attribute with more than one value and the Printer object
             * supports the attribute but only supports a subset of the
             * client-supplied attribute syntaxes or values, the Printer object
             *
             * MUST return only those attribute syntaxes or values that are
             * unsupported.
             */

            /*
             * 3. The Printer object does support the attributes and values
             * supplied, but the particular values are in conflict with one
             * another, because they violate a constraint, such as not being
             * able to staple transparencies
             */

            break;
        }
    }

    /**
     *
     * @param name
     * @return {@code NULL} if the NOT supported
     */
    private IppAttrValue getAttrValueJobTemplate(final String name,
            ApplEnum attrAppl) {

        IppAttr attr = IppDictJobTemplateAttr.instance().getAttr(name);

        if (attr == null) {
            return null;
        }

        if (attrAppl == ApplEnum.DEFAULT) {

            attr = attr.copy(name + "-default");

        } else {

            final String nameSupported = name + "-supported";

            /*
             * Some "supported" variants have a different syntax.
             */
            if (name.equals(IppDictJobTemplateAttr.ATTR_COPIES)) {
                attr = new IppAttr(nameSupported, new IppRangeOfInteger());
            } else {
                attr = attr.copy(nameSupported);
            }
        }

        /*
         * Create value wrapper
         */
        IppAttrValue value = new IppAttrValue(attr);

        switch (name) {

        case IppDictJobTemplateAttr.ATTR_COPIES:
            if (attrAppl == ApplEnum.DEFAULT) {
                value.addValue("1");
            } else {
                value.addValue("1:1");
            }
            break;

        case IppDictJobTemplateAttr.ATTR_MEDIA:
            // Same as IppDictPrinterDescAttr.ATTR_MEDIA_READY:
            value.addValue(IppMediaSizeEnum.ISO_A4.getIppKeyword());
            if (attrAppl == ApplEnum.SUPPORTED) {
                value.addValue(IppMediaSizeEnum.NA_LETTER.getIppKeyword());
                value.addValue(IppMediaSizeEnum.ISO_A0.getIppKeyword());
                value.addValue(IppMediaSizeEnum.ISO_A1.getIppKeyword());
                value.addValue(IppMediaSizeEnum.ISO_A2.getIppKeyword());
                value.addValue(IppMediaSizeEnum.ISO_A3.getIppKeyword());
            }
            break;
        case IppDictJobTemplateAttr.ATTR_PRINTER_RESOLUTION:
            value.addValue(IppResolution.format(600, 600, IppResolution.DPI));
            if (attrAppl == ApplEnum.SUPPORTED) {
                value.addValue(
                        IppResolution.format(1200, 1200, IppResolution.DPI));
            }
            break;
        case IppDictJobTemplateAttr.ATTR_ORIENTATION_REQUESTED:
            value.addValue("3"); // portrait
            if (attrAppl == ApplEnum.SUPPORTED) {
                value.addValue("4"); // landscape
                value.addValue("5"); // reverse-landscape
                value.addValue("6"); // reverse-portrait
            }
            break;
        case IppDictJobTemplateAttr.ATTR_OUTPUT_BIN:
            value.addValue(IppKeyword.OUTPUT_BIN_AUTO);
            break;
        case IppDictJobTemplateAttr.ATTR_SIDES:
            value.addValue(IppKeyword.SIDES_ONE_SIDED);
            break;
        case IppDictJobTemplateAttr.ATTR_PRINT_QUALITY:
            value.addValue("5"); // high
            if (attrAppl == ApplEnum.SUPPORTED) {
                value.addValue("4"); // normal
            }
            break;
        default:
            /**
             * UNSUPPORTED (for this moment)
             *
             * <pre>
             * ...
             * </pre>
             */
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
                if (!this.ippVersion2) {
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
             * "printer-uri-supported" attribute. This attribute identifies the
             * Client Authentication mechanism associated with each URI listed
             * in the "printer-uri- supported" attribute.
             *
             * 'none': There is no authentication mechanism associated with the
             * URI. The Printer object assumes that the authenticated user is
             * "anonymous".
             *
             * 'requesting-user-name': When a client performs an operation whose
             * target is the associated URI, the Printer object assumes that the
             * authenticated user is specified by the "requesting-user- name"
             * Operation attribute (see section 8.3). If the
             * "requesting-user-name" attribute is absent in a request, the
             * Printer object assumes that the authenticated user is
             * "anonymous".
             *
             * 'basic': When a client performs an operation whose target is the
             * associated URI, the Printer object challenges the client with
             * HTTP basic authentication [RFC2617]. The Printer object assumes
             * that the authenticated user is the name received via the basic
             * authentication mechanism.
             *
             * 'digest': When a client performs an operation whose target is the
             * associated URI, the Printer object challenges the client with
             * HTTP digest authentication [RFC2617]. The Printer object assumes
             * that the authenticated user is the name received via the digest
             * authentication mechanism.
             *
             * 'certificate': When a client performs an operation whose target
             * is the associated URI, the Printer object expects the client to
             * provide a certificate. The Printer object assumes that the
             * authenticated user is the textual name contained within the
             * certificate.
             */
            if (!this.ippVersion2) {
                value.addValue("requesting-user-name");
                value.addValue("requesting-user-name");
                value.addValue("requesting-user-name");
            }
            value.addValue("requesting-user-name");
            break;

        case IppDictPrinterDescAttr.ATTR_URI_SECURITY_SUPPORTED:
            /*
             * This REQUIRED Printer attribute MUST have the same cardinality
             * (contain the same number of values) as the
             * "printer-uri-supported" attribute.
             */
            if (!this.ippVersion2) {
                value.addValue("none");
                value.addValue("none");
                value.addValue("tls");
            }
            value.addValue("tls");
            break;

        case IppDictPrinterDescAttr.ATTR_PRINTER_NAME:
            value.addValue(this.composePrinterName());
            break;

        case IppDictPrinterDescAttr.ATTR_PRINTER_STATE:
            value.addValue(IppDictPrinterDescAttr.PRINTER_STATE_IDLE);
            break;

        case IppDictPrinterDescAttr.ATTR_PRINTER_STATE_REASONS:
            value.addValue("none");
            break;

        case IppDictPrinterDescAttr.ATTR_IPP_VERSIONS_SUPP:
            for (final IppVersionEnum version : IppVersionEnum.values()) {
                if (version.isSupported()) {
                    value.addValue(version.getVersionKeyword());
                }
            }
            break;

        case IppDictPrinterDescAttr.ATTR_IPP_FEATURES_SUPP:
            if (this.ippVersion2) {
                // PWG 5100.14 – IPP Everywhere
                value.addValue("ipp-everywhere");
            }
            break;

        case IppDictPrinterDescAttr.ATTR_OPERATIONS_SUPPORTED:
            for (IppOperationId id : IppOperationId.supported()) {
                value.addValue(String.valueOf(id.asInt()));
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
            value.addValue("application/pdf");
            break;

        case IppDictPrinterDescAttr.ATTR_DOC_FORMAT_SUPPORTED:

            value.addValue(IppDictPrinterDescAttr.DOCUMENT_FORMAT_PDF);
            value.addValue(IppDictPrinterDescAttr.DOCUMENT_FORMAT_POSTSCRIPT);

            if (this.ippVersion2) {
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
            value.addValue(String.valueOf(
                    (int) ManagementFactory.getRuntimeMXBean().getUptime()
                            / 1000));
            break;

        case IppDictPrinterDescAttr.ATTR_COMPRESSION_SUPPORTED:
            /*
             * 'none': no compression is used.
             */
            value.addValue("none");

            /*
             * ZIP public domain inflate/deflate
             *
             * value.addValue("deflate");
             */

            /*
             * GNU zip compression technology described in RFC 1952
             *
             * value.addValue("gzip");
             */

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
            value.addValue(ConfigManager.getWebAppUserSslUrl().toString());
            break;

        case IppDictPrinterDescAttr.ATTR_PRINTER_UUID:
            // TODO: linked to UUID in Avahi service file?
            final String printerUUID = ConfigManager.getIppPrinterUuid();
            if (StringUtils.isNotBlank(printerUUID)) {
                value.addValue(String.format("urn:uuid:%s", printerUUID));
            }
            break;

        case IppDictPrinterDescAttr.ATTR_PRINTER_DEVICE_ID:
            // IEEE 1284 Device ID strings
            value.addValue("MANUFACTURER:SavaPage;" + "MODEL:SavaPage;"
                    + "DESCRIPTION:SavaPage;"
            // + "COMMAND SET:PS,PDF;" // ??
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
            /*
             * This value should match with the "Throughput" option in
             * savapage.drv
             */
            value.addValue("120");
            break;

        case IppDictPrinterDescAttr.ATTR_COLOR_SUPPORTED:
            /*
             * This value should match with the "ColorDevice" option in
             * savapage.drv
             */
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

        case IppDictPrinterDescAttr.ATTR_PRINT_COLOR_MODE_DEFAULT:
            value.addValue(IppKeyword.PRINT_COLOR_MODE_AUTO);
            break;

        case IppDictPrinterDescAttr.ATTR_PRINT_COLOR_MODE_SUPPORTED:
            value.addValue(IppKeyword.PRINT_COLOR_MODE_AUTO);
            value.addValue(IppKeyword.PRINT_COLOR_MODE_COLOR);
            value.addValue(IppKeyword.PRINT_COLOR_MODE_MONOCHROME);
            break;

        case IppDictPrinterDescAttr.ATTR_JOB_PASSWORD_ENCRYPTION_SUPPORTED:
            value.addValue(IppKeyword.JOB_PASSWORD_ENCRYPTION_MD5);
            break;

        case IppDictPrinterDescAttr.ATTR_MULTIPLE_DOCUMENT_HANDLING_SUPPORTED:
            value.addValue(IppKeyword.MULTIPLE_DOCUMENT_HANDLING_SINGLE);
            break;

        case IppDictPrinterDescAttr.ATTR_MEDIA_READY:
            // Same as IppDictJobTemplateAttr.ATTR_MEDIA
            value.addValue(IppMediaSizeEnum.ISO_A4.getIppKeyword());
            value.addValue(IppMediaSizeEnum.NA_LETTER.getIppKeyword());
            value.addValue(IppMediaSizeEnum.ISO_A0.getIppKeyword());
            value.addValue(IppMediaSizeEnum.ISO_A1.getIppKeyword());
            value.addValue(IppMediaSizeEnum.ISO_A2.getIppKeyword());
            value.addValue(IppMediaSizeEnum.ISO_A3.getIppKeyword());
            break;

        case IppDictPrinterDescAttr.ATTR_WHICH_JOBS_SUPPORTED:
            /*
             * aborted, all, canceled, completed, fetchable, not-completed,
             * pending, pending-held, processing, processing-stopped,
             * proof-print, saved
             */
            value.addValue("completed");
            value.addValue("not-completed");
            break;

        default:
            /**
             * UNSUPPORTED (for this moment)
             *
             * <pre>
             * printer-driver-installer
             * printer-message-from-operator
             * reference-uri-schemes-supported
             * printer-current-time
             * multiple-operation-time-out
             * job-k-octets-supported
             * job-impressions-supported
             * job-media-sheets-supported
             * </pre>
             */
            break;
        }
        return value;
    }

    /**
     *
     * @param uri
     * @param uriScheme
     * @param port
     * @return
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
     * @return
     */
    private static IppAttrValue createValueMediaSource() {
        final AbstractIppDict dict = IppDictJobTemplateAttr.instance();
        final IppAttr attr =
                dict.getAttr(IppDictJobTemplateAttr.ATTR_MEDIA_SOURCE);
        final IppAttrValue attrValueMediaSource = new IppAttrValue(attr);
        attrValueMediaSource.addValue(IppKeyword.MEDIA_SOURCE_AUTO);
        return attrValueMediaSource;
    }
}
