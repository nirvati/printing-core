/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2018 Datraverse B.V.
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
package org.savapage.core.ipp.client;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.print.attribute.standard.MediaSizeName;

import org.apache.commons.lang3.tuple.Pair;
import org.savapage.core.inbox.PdfOrientationInfo;
import org.savapage.core.ipp.IppMediaSizeEnum;
import org.savapage.core.ipp.attribute.AbstractIppDict;
import org.savapage.core.ipp.attribute.IppAttr;
import org.savapage.core.ipp.attribute.IppAttrCollection;
import org.savapage.core.ipp.attribute.IppAttrGroup;
import org.savapage.core.ipp.attribute.IppAttrValue;
import org.savapage.core.ipp.attribute.IppDictJobTemplateAttr;
import org.savapage.core.ipp.attribute.IppDictOperationAttr;
import org.savapage.core.ipp.attribute.syntax.IppBoolean;
import org.savapage.core.ipp.attribute.syntax.IppInteger;
import org.savapage.core.ipp.attribute.syntax.IppKeyword;
import org.savapage.core.ipp.attribute.syntax.IppMimeMediaType;
import org.savapage.core.ipp.encoding.IppDelimiterTag;
import org.savapage.core.ipp.helpers.IppNumberUpHelper;
import org.savapage.core.ipp.rules.IppRuleExtra;
import org.savapage.core.ipp.rules.IppRuleNumberUp;
import org.savapage.core.ipp.rules.IppRuleSubst;
import org.savapage.core.pdf.PdfCreateInfo;
import org.savapage.core.print.proxy.AbstractProxyPrintReq;
import org.savapage.core.print.proxy.JsonProxyPrinter;
import org.savapage.core.print.proxy.JsonProxyPrinterOpt;
import org.savapage.core.print.proxy.JsonProxyPrinterOptChoice;
import org.savapage.core.services.ProxyPrintService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.util.MediaUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class IppReqPrintJob extends IppReqCommon {

    /** */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(IppReqPrintJob.class);

    /** */
    private static final ProxyPrintService PROXYPRINT_SERVICE =
            ServiceContext.getServiceFactory().getProxyPrintService();

    private final AbstractProxyPrintReq request;
    private final File fileToPrint;
    private final JsonProxyPrinter jsonPrinter;
    private final String reqUser;
    private final String docName;
    private final String jobName;
    private final PdfCreateInfo pdfCreateInfo;

    /*
     * NOTE: PWG5100.13 states that "A Client specifies that is has borderless
     * or "full-bleed" content by setting all of the margins to 0."
     *
     * HOWEVER, this does NOT seem to be true. We get an "Unsupported margins"
     * error when " media-*-margin-supported" are NEQ zero. Why?
     *
     * Therefore, we use CUPS specific IPP Job template attributes. See:
     * http://www.cups.org/documentation.php/spec-ipp.html
     *
     * Tested OK with: CUPS 1.5.3
     */

    // TODO: dependent on CUPS version. When was it implemented?
    private static boolean usePWG5100_13 = false;
    // No full bleed for now.
    private static boolean isFullBleed = false;

    /**
     * IPP request for a print job.
     *
     * @param printRequest
     *            The print request.
     * @param file
     *            The PDF to print.
     * @param printer
     *            The printer.
     * @param user
     *            The requesting user.
     * @param documentName
     *            The document name.
     * @param printJobName
     *            The print job name.
     * @param createInfo
     *            The PDF creation info.
     */
    public IppReqPrintJob(final AbstractProxyPrintReq printRequest,
            final File file, final JsonProxyPrinter printer, final String user,
            final String documentName, final String printJobName,
            final PdfCreateInfo createInfo) {

        this.request = printRequest;
        this.fileToPrint = file;
        this.jsonPrinter = printer;
        this.reqUser = user;
        this.docName = documentName;
        this.jobName = printJobName;
        this.pdfCreateInfo = createInfo;
    }

    /**
     * Corrects a Print Job request for n-up.
     *
     * @param jsonPrinter
     *            The printer.
     * @param optionValues
     *            The IPP job option values.
     * @param group
     *            The IPP attribute group to append on.
     * @param pdfOrientation
     *            The {@link PdfOrientationInfo} of the PDF inbox document
     *            belonging to the first page to be proxy printed.
     * @param numberUp
     *            The n-up value.
     */
    private static void correctForNup(final JsonProxyPrinter jsonPrinter,
            final Map<String, String> optionValues, final IppAttrGroup group,
            final PdfOrientationInfo pdfOrientation, final String numberUp) {

        final IppRuleNumberUp templateRule = new IppRuleNumberUp("template");

        templateRule.setLandscape(pdfOrientation.getLandscape());
        templateRule.setPdfRotation(pdfOrientation.getRotation().intValue());
        templateRule.setUserRotate(pdfOrientation.getRotate().intValue());
        templateRule.setNumberUp(numberUp);

        //
        IppRuleNumberUp numberUpRule = jsonPrinter.findCustomRule(templateRule);

        if (numberUpRule == null) {
            numberUpRule =
                    IppNumberUpHelper.instance().findCustomRule(templateRule);
        }

        if (numberUpRule == null) {
            LOGGER.debug("No correction for n-up.");
            return;
        }

        final String cupsOrientationRequested =
                numberUpRule.getOrientationRequested();
        final String cupsNupLayout = numberUpRule.getNumberUpLayout();

        final AbstractIppDict dict = IppDictJobTemplateAttr.instance();

        if (cupsOrientationRequested != null) {
            group.add(
                    dict.createPpdOptionAttr(
                            IppDictJobTemplateAttr.CUPS_ATTR_ORIENTATION_REQUESTED),
                    cupsOrientationRequested);
        }

        if (cupsNupLayout != null) {
            group.add(
                    dict.createPpdOptionAttr(
                            IppDictJobTemplateAttr.CUPS_ATTR_NUMBER_UP_LAYOUT),
                    cupsNupLayout);
        }

        if (numberUpRule.isLandscapePrint()) {
            // Set ad-hoc landscape indication in original request.
            optionValues.put(IppDictJobTemplateAttr.ORG_SAVAPAGE_ATTR_LANDSCAPE,
                    "");
        }

        if (LOGGER.isDebugEnabled()) {
            final StringBuilder msg = new StringBuilder();

            msg.append("PDF source: ");

            if (pdfOrientation.getLandscape()) {
                msg.append("landscape");
            } else {
                msg.append("portrait");
            }
            msg.append(" rotation [").append(pdfOrientation.getRotation())
                    .append("] user rotate [")
                    .append(pdfOrientation.getRotate()).append("]")
                    .append(" | CUPS ").append(numberUp).append("-up");

            if (cupsNupLayout != null) {
                msg.append(" ").append(cupsNupLayout.toUpperCase());
            }
            if (cupsOrientationRequested != null) {
                msg.append(" orientation [").append(cupsOrientationRequested)
                        .append("]");
            }

            msg.append(" | PDF out [");
            if (numberUpRule.isLandscapePrint()) {
                msg.append("landscape");
            } else {
                msg.append("portrait");
            }
            msg.append("]");

            LOGGER.debug(msg.toString());
        }
    }

    /**
     * Corrects for missing {@link IppDictJobTemplateAttr#ATTR_SHEET_COLLATE}
     * option when an {@link IppRuleSubst} instance is applicable after all.
     *
     * @param jsonPrinter
     *            The printer.
     * @param printerOptionsLookup
     *            The printer options look-up.
     * @param optionValuesOrg
     *            The original IPP job option values.
     * @param group
     *            The IPP attribute group to append on.
     */
    private static void correctForRulesSubst(final JsonProxyPrinter jsonPrinter,
            final Map<String, JsonProxyPrinterOpt> printerOptionsLookup,
            final Map<String, String> optionValuesOrg,
            final IppAttrGroup group) {

        // Are rules present?
        if (jsonPrinter.getCustomRulesSubst().isEmpty()) {
            return;
        }

        // Is sheet-collate already substituted?
        if (optionValuesOrg
                .containsKey(IppDictJobTemplateAttr.ATTR_SHEET_COLLATE)) {
            return;
        }

        final JsonProxyPrinterOpt proxyPrinterOpt = printerOptionsLookup
                .get(IppDictJobTemplateAttr.ATTR_SHEET_COLLATE);

        // Is sheet-collate present as printer option?
        if (proxyPrinterOpt == null
                || proxyPrinterOpt.getKeywordPpd() == null) {
            return;
        }

        final Map<String, String> optionValuesWork = new HashMap<>();
        optionValuesWork.putAll(optionValuesOrg);

        optionValuesWork.put(IppDictJobTemplateAttr.ATTR_SHEET_COLLATE,
                IppKeyword.SHEET_COLLATE_COLLATED);

        final Map<String, IppRuleSubst> ruleMapAdhoc =
                jsonPrinter.findCustomRulesSubst(optionValuesWork);

        // Is sheet-collate rule present?
        if (!ruleMapAdhoc
                .containsKey(IppDictJobTemplateAttr.ATTR_SHEET_COLLATE)) {
            return;
        }

        final String ppdValue = ruleMapAdhoc
                .get(IppDictJobTemplateAttr.ATTR_SHEET_COLLATE).getPpdValue();

        final AbstractIppDict dict = IppDictJobTemplateAttr.instance();

        group.add(dict.createPpdOptionAttr(proxyPrinterOpt.getKeywordPpd()),
                ppdValue);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("Added: %s/%s",
                    proxyPrinterOpt.getKeywordPpd(), ppdValue));
        }
    }

    /**
     * Corrects a Print Job request with Extra PPD options.
     * <p>
     * Note: Existing PPD options are <i>replaced</i> by the Extra options.
     * </p>
     *
     * @param jsonPrinter
     *            The printer.
     * @param optionValues
     *            The IPP job option values.
     * @param group
     *            The IPP attribute group to append on.
     */
    private static void correctForRulesExtra(final JsonProxyPrinter jsonPrinter,
            final Map<String, String> optionValues, final IppAttrGroup group) {

        final AbstractIppDict dict = IppDictJobTemplateAttr.instance();

        // Rules
        for (final IppRuleExtra rule : jsonPrinter
                .findCustomRulesExtra(optionValues)) {

            // PPD options
            for (final Pair<String, String> pair : rule.getExtraPPD()) {

                final String ppdOptionOrg = pair.getKey();
                final String ppdValueNew = pair.getValue();

                final IppAttrCollection mediaSizeCollection;

                if (ppdOptionOrg.equals(IppDictJobTemplateAttr.ATTR_MEDIA)) {
                    mediaSizeCollection =
                            createMediaSizeCollection(ppdValueNew);
                } else {
                    mediaSizeCollection = null;
                }

                boolean addExtra = true;

                /*
                 * Find attribute to replace it.
                 */
                if (mediaSizeCollection == null) {

                    for (final IppAttrValue attrVal : group.getAttributes()) {

                        if (!attrVal.getAttribute().getKeyword()
                                .equals(ppdOptionOrg)) {
                            continue;
                        }

                        final List<String> values = attrVal.getValues();
                        values.clear();
                        values.add(ppdValueNew);

                        LOGGER.warn("Replaced: {}/{}", ppdOptionOrg,
                                ppdValueNew);

                        addExtra = false;
                        break;
                    }

                } else {

                    for (final IppAttrCollection collection : group
                            .getCollections()) {

                        if (!collection.getKeyword().equals(
                                IppDictJobTemplateAttr.ATTR_MEDIA_COL)) {
                            continue;
                        }

                        // Remove existing
                        final Iterator<IppAttrCollection> iter =
                                collection.getCollections().iterator();

                        boolean isReplaced = false;
                        while (iter.hasNext()) {
                            final IppAttrCollection collectionNested =
                                    iter.next();
                            if (collectionNested.getKeyword().equals(
                                    IppDictJobTemplateAttr.ATTR_MEDIA_SIZE)) {
                                iter.remove();
                                isReplaced = true;
                                break;
                            }
                        }

                        // Add new.
                        collection.addCollection(mediaSizeCollection);

                        if (isReplaced) {
                            LOGGER.warn("Replaced {} with {}",
                                    IppDictJobTemplateAttr.ATTR_MEDIA_SIZE,
                                    ppdValueNew);
                        } else {
                            LOGGER.warn("Set {} to {}",
                                    IppDictJobTemplateAttr.ATTR_MEDIA_SIZE,
                                    ppdValueNew);
                        }

                        addExtra = false;

                        break;
                    }
                }

                if (addExtra) {

                    if (mediaSizeCollection == null) {
                        group.add(dict.createPpdOptionAttr(ppdOptionOrg),
                                ppdValueNew);
                    } else {

                        final IppAttrCollection collection =
                                new IppAttrCollection(
                                        IppDictJobTemplateAttr.ATTR_MEDIA_COL);
                        collection.addCollection(mediaSizeCollection);
                        group.addCollection(collection);
                    }

                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug(String.format("Added: %s/%s", ppdOptionOrg,
                                ppdValueNew));
                    }
                }
            }
        }

    }

    /**
     * Creates Operation Attributes.
     *
     * @return The group.
     */
    private IppAttrGroup createOperationAttributes() {

        final IppAttrGroup group = createOperationGroup();

        final AbstractIppDict dict = IppDictOperationAttr.instance();

        group.add(dict.getAttr(IppDictOperationAttr.ATTR_PRINTER_URI),
                this.jsonPrinter.getPrinterUri().toString());

        group.add(dict.getAttr(IppDictOperationAttr.ATTR_JOB_NAME),
                this.jobName);

        group.add(dict.getAttr(IppDictOperationAttr.ATTR_REQUESTING_USER_NAME),
                this.reqUser);

        group.add(dict.getAttr(IppDictOperationAttr.ATTR_JOB_K_OCTETS),
                String.valueOf(this.fileToPrint.length()));

        group.add(
                dict.getAttr(IppDictOperationAttr.ATTR_IPP_ATTRIBUTE_FIDELITY),
                IppBoolean.TRUE);

        group.add(dict.getAttr(IppDictOperationAttr.ATTR_DOCUMENT_NAME),
                this.docName);

        group.add(dict.getAttr(IppDictOperationAttr.ATTR_COMPRESSION),
                IppKeyword.COMPRESSION_NONE);

        group.add(dict.getAttr(IppDictOperationAttr.ATTR_DOCUMENT_FORMAT),
                IppMimeMediaType.PDF);

        // "document-natural-language" (naturalLanguage):

        return group;
    }

    @Override
    public List<IppAttrGroup> build() {

        final int copies = this.request.getNumberOfCopies();
        final Boolean fitToPage = this.request.getFitToPage();
        final Map<String, String> optionValues = this.request.getOptionValues();

        final List<IppAttrGroup> attrGroups = new ArrayList<>();

        /*
         * Group 1: Operation Attributes
         */
        attrGroups.add(this.createOperationAttributes());

        /*
         * Group 2: Job Template Attributes
         */
        final IppAttrGroup group = new IppAttrGroup(IppDelimiterTag.JOB_ATTR);
        attrGroups.add(group);

        /*
         * We need easy lookup of options when they are injected with an
         * SavaPage PPD Extension.
         */
        final Map<String, JsonProxyPrinterOpt> printerOptionsLookup;

        if (this.jsonPrinter.isInjectPpdExt()) {
            printerOptionsLookup = this.jsonPrinter.getOptionsLookup();
        } else {
            printerOptionsLookup = null;
        }

        /*
         * Mantis #738.
         */
        final StringBuilder numberUpRequested =
                new StringBuilder(IppKeyword.NUMBER_UP_1);

        this.addJobOptions(group, printerOptionsLookup, numberUpRequested);

        addCupsPageAttrs(group);

        if (fitToPage != null) {
            addFitToPage(group, printerOptionsLookup, fitToPage.booleanValue());
        }

        /*
         * Mantis #259: add number of copies when GT 1.
         */
        if (copies > 1) {
            group.add(IppDictJobTemplateAttr.ATTR_COPIES, IppInteger.instance(),
                    String.valueOf(copies));
        }

        /*
         * Mantis #738: Apply correct n-up layout in landscape proxy print.
         */
        final PdfOrientationInfo pdfOrientation;

        if (this.pdfCreateInfo.getPdfOrientationInfo() != null) {
            pdfOrientation = this.pdfCreateInfo.getPdfOrientationInfo();
        } else if (this.request.getJobChunkInfo() == null) {
            pdfOrientation = this.request.getPdfOrientation();
        } else {
            pdfOrientation = this.request.getJobChunkInfo().getPdfOrientation();
        }

        if (pdfOrientation != null) {
            correctForNup(
                    PROXYPRINT_SERVICE.getCachedPrinter(
                            this.jsonPrinter.getName()),
                    optionValues, group, pdfOrientation,
                    numberUpRequested.toString());
        }

        //
        if (this.jsonPrinter.isInjectPpdExt()) {
            correctForRulesSubst(this.jsonPrinter, printerOptionsLookup,
                    optionValues, group);
            correctForRulesExtra(this.jsonPrinter, optionValues, group);
        }

        if (LOGGER.isDebugEnabled()) {
            for (final Entry<String, String> entry : optionValues.entrySet()) {
                LOGGER.debug(String.format("IPP: %s/%s", entry.getKey(),
                        entry.getValue()));
            }
            for (final IppAttrValue val : group.getAttributes()) {
                LOGGER.debug(String.format("PPD %s/%s",
                        val.getAttribute().getKeyword(), val.getSingleValue()));
            }
            for (final IppAttrCollection col : group.getCollections()) {
                LOGGER.debug(
                        String.format("PPD Collection: %s", col.getKeyword()));
                for (final IppAttrValue val : col.getAttributes()) {
                    LOGGER.debug(String.format("\t%s/%s",
                            val.getAttribute().getKeyword(),
                            val.getSingleValue()));
                }
            }
        }

        return attrGroups;
    }

    /**
     *
     * @param group
     *            The group to add job attributes to.
     * @param printerOptionsLookup
     *            The printer options look-up.
     * @param numberUpRequested
     *            The requested number-up to be filled by this method.
     */
    private void addJobOptions(final IppAttrGroup group,
            final Map<String, JsonProxyPrinterOpt> printerOptionsLookup,
            final StringBuilder numberUpRequested) {

        final Map<String, String> optionValues = this.request.getOptionValues();

        final Map<String, IppRuleSubst> mapIppRuleSubst =
                this.jsonPrinter.findCustomRulesSubst(optionValues);

        final AbstractIppDict dict = IppDictJobTemplateAttr.instance();

        IppAttrCollection collectionMediaSize = null;
        IppAttrValue attrValueMediaSource = null;

        /*
         * Traverse the job options.
         */
        for (final Entry<String, String> entry : optionValues.entrySet()) {

            // The IPP option keyword.
            final String optionKeywordIpp = entry.getKey();

            // The mapped PPD option keyword.
            final String optionKeywordPpd;

            // IPP option from proxy printer definition.
            final JsonProxyPrinterOpt proxyPrinterOpt;

            if (this.jsonPrinter.isInjectPpdExt()) {

                proxyPrinterOpt = printerOptionsLookup.get(optionKeywordIpp);

                if (proxyPrinterOpt != null
                        && proxyPrinterOpt.getKeywordPpd() != null) {
                    optionKeywordPpd = proxyPrinterOpt.getKeywordPpd();
                } else {
                    optionKeywordPpd = null;
                }

            } else {
                proxyPrinterOpt = null;
                optionKeywordPpd = null;
            }

            // IPP option for JobTicket specification.
            if (proxyPrinterOpt != null && proxyPrinterOpt.isJobTicket()) {
                continue;
            }

            // The actual attribute and options to send.
            final IppAttr attr;
            final String optionKeyword;
            final String optionValue;

            if (optionKeywordPpd == null) {

                attr = dict.getAttr(optionKeywordIpp);
                optionKeyword = optionKeywordIpp;
                optionValue = entry.getValue();

            } else {

                final String optionValueIpp = entry.getValue();
                String optionValuePpd = null;
                for (final JsonProxyPrinterOptChoice choice : proxyPrinterOpt
                        .getChoices()) {
                    if (choice.getChoice().equals(optionValueIpp)) {
                        optionValuePpd = choice.getChoicePpd();
                        break;
                    }
                }
                attr = dict.createPpdOptionAttr(optionKeywordPpd);
                optionKeyword = optionKeywordPpd;

                if (mapIppRuleSubst.containsKey(optionKeywordIpp)) {

                    optionValue =
                            mapIppRuleSubst.get(optionKeywordIpp).getPpdValue();

                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug(String.format("%s/%s -> %s/%s",
                                optionKeywordIpp, optionValueIpp, optionKeyword,
                                optionValue));
                    }
                } else {
                    optionValue = optionValuePpd;
                }
            }

            // Skip attributes exclusively used for Job Ticket.
            if (IppDictJobTemplateAttr.isJobTicketAttr(optionKeyword)) {
                continue;
            }

            if (attr == null) {

                if (optionKeyword.equals(
                        IppDictJobTemplateAttr.ORG_SAVAPAGE_ATTR_LANDSCAPE)) {
                    continue;
                }
                /*
                 * Finishing options are not found when they are NOT mapped in
                 * the PPDE. They can be skipped if they have a "none" value.
                 * Any other value is signaled first.
                 */
                if (!IppDictJobTemplateAttr.isNoneValueFinishing(optionKeyword,
                        optionValue)) {
                    final StringBuilder msg = new StringBuilder();
                    msg.append("IPP Attribute [").append(optionKeyword)
                            .append("] with value [").append(optionValue)
                            .append("] is unknown (attribute is skipped).");
                    LOGGER.error(msg.toString());
                }
                continue;
            }

            if (optionValue == null) {
                final StringBuilder msg = new StringBuilder();
                msg.append("IPP Attribute [").append(optionKeywordIpp)
                        .append("]");
                if (optionKeywordPpd != null) {
                    msg.append(" with mapped PPD option [")
                            .append(optionKeywordPpd).append("]");
                }
                msg.append(": mapped PPD value of [").append(entry.getValue())
                        .append("] unknown (attribute is skipped).");
                LOGGER.error(msg.toString());
                continue;
            }

            /*
             * Mantis #738.
             */
            if (optionKeyword.equals(IppDictJobTemplateAttr.ATTR_NUMBER_UP)) {
                numberUpRequested.setLength(0); // clear ...
                numberUpRequested.append(optionValue); // ... and set.
            }

            if (optionKeyword.equals(IppDictJobTemplateAttr.ATTR_MEDIA)) {
                // Create here, apply later.
                collectionMediaSize = createMediaSizeCollection(optionValue);

            } else if (optionKeyword
                    .equals(IppDictJobTemplateAttr.ATTR_MEDIA_SOURCE)) {
                // Create here, apply later.
                attrValueMediaSource = new IppAttrValue(attr);
                attrValueMediaSource.addValue(optionValue);

            } else {
                group.add(attr, optionValue);
            }
        } // job options

        /*
         * Mantis #409: add media-source or media-size.
         */
        if (collectionMediaSize != null || attrValueMediaSource != null) {

            final IppAttrCollection collection = new IppAttrCollection(
                    IppDictJobTemplateAttr.ATTR_MEDIA_COL);

            group.addCollection(collection);

            if (collectionMediaSize != null) {
                collection.addCollection(collectionMediaSize);
            }

            if (attrValueMediaSource != null) {
                collection.addAttribute(attrValueMediaSource);
            }

            addMediaMargins(collection);
        }
    }

    /**
     * Add print-scaling.
     *
     * @param group
     *            The group to add job attributes to.
     * @param printerOptionsLookup
     *            The printer options look-up.
     * @param fitToPage
     *            {@code true} when fit to page.
     */
    private static void addFitToPage(final IppAttrGroup group,
            final Map<String, JsonProxyPrinterOpt> printerOptionsLookup,
            final boolean fitToPage) {

        final String optionKeywordIpp =
                IppDictJobTemplateAttr.ATTR_PRINT_SCALING;

        final JsonProxyPrinterOpt printScaling;

        if (printerOptionsLookup == null) {
            printScaling = null;
        } else {
            printScaling = printerOptionsLookup.get(optionKeywordIpp);
        }

        if (printScaling == null) {
            /*
             * Mantis #205: add fit to page attribute.
             */
            final String fitToPageIppBoolean;

            if (fitToPage) {
                fitToPageIppBoolean = IppBoolean.TRUE;
            } else {
                fitToPageIppBoolean = IppBoolean.FALSE;
            }

            group.add(IppDictJobTemplateAttr.CUPS_ATTR_FIT_TO_PAGE,
                    IppBoolean.instance(), fitToPageIppBoolean);

        } else {

            final AbstractIppDict dict = IppDictJobTemplateAttr.instance();

            /*
             * Mantis #719: Create PPD mapping for print-scaling.
             */

            // The mapped PPD option keyword.
            final String optionKeywordPpd = printScaling.getKeywordPpd();

            // The IPP value.
            final String optionValueIpp;
            if (fitToPage) {
                optionValueIpp = IppKeyword.PRINT_SCALING_FIT;
            } else {
                optionValueIpp = IppKeyword.PRINT_SCALING_NONE;
            }

            // The actual attribute and value to send.
            final IppAttr attr;
            final String optionValue;

            if (optionKeywordPpd == null) {

                attr = dict.getAttr(optionKeywordIpp);
                optionValue = optionValueIpp;

            } else {

                String optionValuePpd = null;

                for (final JsonProxyPrinterOptChoice choice : printScaling
                        .getChoices()) {
                    if (choice.getChoice().equals(optionValueIpp)) {
                        optionValuePpd = choice.getChoicePpd();
                        break;
                    }
                }
                attr = dict.createPpdOptionAttr(optionKeywordPpd);
                optionValue = optionValuePpd;
            }

            group.add(attr, optionValue);
        }
    }

    /**
     * Adds CUPS page-* attributes to the group.
     *
     * @param group
     *            The group.
     */
    private static void addCupsPageAttrs(final IppAttrGroup group) {

        if (!usePWG5100_13 && isFullBleed) {

            final IppInteger syntax = new IppInteger(0);

            for (final String keyword : new String[] {
                    IppDictJobTemplateAttr.CUPS_ATTR_PAGE_BOTTOM,
                    IppDictJobTemplateAttr.CUPS_ATTR_PAGE_LEFT,
                    IppDictJobTemplateAttr.CUPS_ATTR_PAGE_RIGHT,
                    IppDictJobTemplateAttr.CUPS_ATTR_PAGE_TOP }) {

                group.add(keyword, syntax, "0");
            }
        }
    }

    /**
     * Adds media margins to the media collection.
     *
     * @param collection
     *            The media collection.
     * @param usePWG5100_13
     * @param isFullBleed
     */
    private static void addMediaMargins(final IppAttrCollection collection) {

        if (usePWG5100_13 && isFullBleed) {
            /*
             * PWG5100.13: A Client specifies that is has borderless or
             * "full-bleed" content by setting all of the margins to 0."
             *
             * HOWEVER, this does NOT seem to be true. We get an
             * "Unsupported margins" error when " media-*-margin-supported" are
             * NEQ zero. Why?
             */
            final IppInteger syntax = new IppInteger(0);

            for (final String keyword : new String[] {
                    IppDictJobTemplateAttr.ATTR_MEDIA_BOTTOM_MARGIN,
                    IppDictJobTemplateAttr.ATTR_MEDIA_LEFT_MARGIN,
                    IppDictJobTemplateAttr.ATTR_MEDIA_RIGHT_MARGIN,
                    IppDictJobTemplateAttr.ATTR_MEDIA_TOP_MARGIN }) {

                collection.add(keyword, syntax, "0");
            }
        }
    }

    /**
     * Creates a media-size collection.
     *
     * @param mediaOptionValue
     *            The media option value.
     * @return The {@link IppAttrCollection}.
     */
    private static IppAttrCollection
            createMediaSizeCollection(final String mediaOptionValue) {

        final IppAttrCollection collectionMediaSize =
                new IppAttrCollection(IppDictJobTemplateAttr.ATTR_MEDIA_SIZE);

        final MediaSizeName sizeName =
                IppMediaSizeEnum.findMediaSizeName(mediaOptionValue);

        final int[] array = MediaUtils.getMediaWidthHeight(sizeName);

        // Number of hundredth in a mm.
        final int hundredthMM = 100;

        collectionMediaSize.add("x-dimension", new IppInteger(0),
                String.valueOf(array[0] * hundredthMM));

        collectionMediaSize.add("y-dimension", new IppInteger(0),
                String.valueOf(array[1] * hundredthMM));

        return collectionMediaSize;
    }
}
