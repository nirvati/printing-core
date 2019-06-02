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
package org.savapage.core.print.proxy;

import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.savapage.core.dao.enums.PrintModeEnum;
import org.savapage.core.inbox.PdfOrientationInfo;
import org.savapage.core.ipp.attribute.IppDictJobTemplateAttr;
import org.savapage.core.ipp.attribute.syntax.IppKeyword;
import org.savapage.core.ipp.helpers.IppOptionMap;
import org.savapage.core.services.helpers.AccountTrxInfoSet;
import org.savapage.core.services.helpers.ExternalSupplierInfo;
import org.savapage.core.services.helpers.InboxSelectScopeEnum;
import org.savapage.core.services.helpers.PrintScalingEnum;
import org.savapage.core.services.helpers.ProxyPrintCostDto;
import org.savapage.core.services.helpers.ProxyPrintCostParms;

/**
 * Proxy Print Request base on the SafePages inbox.
 *
 * @author Rijk Ravestein
 *
 */
public abstract class AbstractProxyPrintReq
        implements ProxyPrintSheetsCalcParms {

    /**
     * Proxy Print Request Status.
     */
    public enum Status {

        /**
         * Proxy Print requested (this is the first status in the print flow).
         */
        REQUESTED,

        /**
         * Proxy Print Request needs User Authentication.
         */
        NEEDS_AUTH,

        /**
         * Proxy Print Request successfully authenticated.
         */
        AUTHENTICATED,

        /**
         * The requested Proxy Printer is not found (delete, disabled, not
         * present, etc.).
         */
        ERROR_PRINTER_NOT_FOUND,

        /**
         * Proxy Print Request expired.
         */
        EXPIRED,

        /**
         * Proxy Print Job is successfully offered for proxy printing.
         */
        PRINTED,

        /**
         * Waiting to be released.
         */
        WAITING_FOR_RELEASE
    }

    private Status status = Status.REQUESTED;

    private PrintModeEnum printMode;

    private String printerName;
    private String ticketPrinterName;
    private String jobName;
    private int numberOfCopies;
    private int numberOfPages;
    private boolean removeGraphics;
    private boolean drm;

    private boolean ecoPrintShadow;

    /**
     * Collate pages.
     */
    private boolean collate;

    /**
     * Archive print job.
     */
    private boolean archive;

    /**
     * Disable print job journal.
     */
    private boolean disableJournal;

    /**
     * {@code true} if PDF must to be converted to grayscale before proxy
     * printing.
     */
    private boolean convertToGrayscale = false;

    /**
     * {@code true} if booklet page ordering is performed client-side (locally).
     */
    private boolean localBooklet = false;

    /** */
    private String jobTicketNumber;
    /** */
    private String jobTicketDomain;
    /** */
    private String jobTicketUse;
    /** */
    private String jobTicketTag;

    //
    private Locale locale = Locale.getDefault();

    private String userMsg;
    private String userMsgKey;
    private Long idUser;

    private ProxyPrintCostDto costResult;

    private Date submitDate;

    private Map<String, String> optionValues;

    private InboxSelectScopeEnum clearScope;

    /**
     * {@code true} when one of the job pages has landscape orientation.
     * {@code null} when unknown.
     */
    private Boolean landscape;

    /**
     * The PDF inbox document orientation of the first page to be proxy printed.
     */
    private PdfOrientationInfo pdfOrientation;

    private ProxyPrintJobChunkInfo jobChunkInfo;

    private AccountTrxInfoSet accountTrxInfoSet;

    private String comment;

    /**
     * .
     */
    private ExternalSupplierInfo supplierInfo;

    /**
     * The number of cleared object (pages or documents, depending on
     * {@link #clearScope}) at status {@link Status#PRINTED}.
     */
    private int clearedObjects = 0;

    /**
     *
     * @param printMode
     */
    protected AbstractProxyPrintReq(final PrintModeEnum printMode) {
        this.printMode = printMode;
    }

    public InboxSelectScopeEnum getClearScope() {
        return clearScope;
    }

    public void setClearScope(InboxSelectScopeEnum clearScope) {
        this.clearScope = clearScope;
    }

    /**
     * Gets the number of cleared object (pages or documents, depending on
     * {@link #clearScope}) at status {@link Status#PRINTED}.
     *
     * @return
     */
    public int getClearedObjects() {
        return clearedObjects;
    }

    /**
     * Sets the number of cleared object (pages or documents, depending on
     * {@link #clearScope}) at status {@link Status#PRINTED}.
     *
     * @param clearedObjects
     */
    public void setClearedObjects(int clearedObjects) {
        this.clearedObjects = clearedObjects;
    }

    public boolean isRemoveGraphics() {
        return removeGraphics;
    }

    public void setRemoveGraphics(boolean removeGraphics) {
        this.removeGraphics = removeGraphics;
    }

    public boolean isDrm() {
        return drm;
    }

    public void setDrm(boolean drm) {
        this.drm = drm;
    }

    /**
     *
     * @return {@code true} if Eco PDF shadow is to be used.
     */
    public boolean isEcoPrintShadow() {
        return ecoPrintShadow;
    }

    /**
     *
     * @param ecoPrintShadow
     *            {@code true} if Eco PDF shadow is to be used.
     */
    public void setEcoPrintShadow(boolean ecoPrintShadow) {
        this.ecoPrintShadow = ecoPrintShadow;
    }

    public boolean isCollate() {
        return collate;
    }

    public void setCollate(boolean collate) {
        this.collate = collate;
    }

    public boolean isArchive() {
        return archive;
    }

    public void setArchive(boolean archive) {
        this.archive = archive;
    }

    public boolean isDisableJournal() {
        return disableJournal;
    }

    public void setDisableJournal(boolean disableJournal) {
        this.disableJournal = disableJournal;
    }

    @Override
    public int getNumberOfCopies() {
        return numberOfCopies;
    }

    public void setNumberOfCopies(int numberOfCopies) {
        this.numberOfCopies = numberOfCopies;
    }

    @Override
    public int getNumberOfPages() {
        return numberOfPages;
    }

    public void setNumberOfPages(int numberOfPages) {
        this.numberOfPages = numberOfPages;
    }

    public PrintModeEnum getPrintMode() {
        return printMode;
    }

    public void setPrintMode(PrintModeEnum printMode) {
        this.printMode = printMode;
    }

    public String getPrinterName() {
        return printerName;
    }

    public void setPrinterName(String printerName) {
        this.printerName = printerName;
    }

    public String getTicketPrinterName() {
        return ticketPrinterName;
    }

    public void setTicketPrinterName(String ticketPrinterName) {
        this.ticketPrinterName = ticketPrinterName;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public Map<String, String> getOptionValues() {
        return optionValues;
    }

    public void setOptionValues(Map<String, String> optionValues) {
        this.optionValues = optionValues;
    }

    public void putOptionValues(final Map<String, String> optionValues) {
        if (this.optionValues == null) {
            this.optionValues = new HashMap<>();
        }
        this.optionValues.putAll(optionValues);
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Locale getLocale() {
        return locale;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    public String getUserMsg() {
        return userMsg;
    }

    public void setUserMsg(String userMsg) {
        this.userMsg = userMsg;
    }

    public String getUserMsgKey() {
        return userMsgKey;
    }

    public void setUserMsgKey(String userMsgKey) {
        this.userMsgKey = userMsgKey;
    }

    public Long getIdUser() {
        return idUser;
    }

    public void setIdUser(Long idUser) {
        this.idUser = idUser;
    }

    public ProxyPrintCostDto getCostResult() {
        return costResult;
    }

    public void setCostResult(ProxyPrintCostDto costResult) {
        this.costResult = costResult;
    }

    public boolean isAuthenticated() {
        return status == Status.AUTHENTICATED;
    }

    public boolean isExpired() {
        return status == Status.EXPIRED;
    }

    public Date getSubmitDate() {
        return submitDate;
    }

    public void setSubmitDate(Date submitDate) {
        this.submitDate = submitDate;
    }

    public boolean isColor() {
        return !isGrayscale();
    }

    public boolean isGrayscale() {
        return isGrayscale(getOptionValues());
    }

    /**
     *
     * @return {@code true} if Booklet option is present.
     */
    public boolean isBooklet() {
        return isBooklet(getOptionValues());
    }

    /**
     * @return {@code true} if <u>printer</u> is capable of duplex printing.
     */
    public boolean hasDuplex() {
        return hasDuplex(getOptionValues());
    }

    @Override
    public boolean isDuplex() {
        return isDuplex(getOptionValues());
    }

    @Override
    public int getNup() {
        return getNup(getOptionValues());
    }

    /**
     * @return The value of the PWG5101.1 IPP "media" option.
     */
    public String getMediaOption() {
        return getMediaOption(this.optionValues);
    }

    /**
     *
     * @param optionValues
     * @return The value of the PWG5101.1 IPP "media" option.
     */
    public static String getMediaOption(Map<String, String> optionValues) {
        return optionValues.get(IppDictJobTemplateAttr.ATTR_MEDIA);
    }

    /**
     * Gets the value of the PWG5101.1 IPP "media-source" option.
     *
     * @return
     */
    public String getMediaSourceOption() {
        return this.optionValues.get(IppDictJobTemplateAttr.ATTR_MEDIA_SOURCE);
    }

    /**
     * Sets the value of the PWG5101.1 IPP "media" option.
     *
     * @param media
     *            The {@link IppDictJobTemplateAttr#ATTR_MEDIA} value.
     */
    public void setMediaOption(final String media) {
        this.optionValues.put(IppDictJobTemplateAttr.ATTR_MEDIA, media);
    }

    /**
     * Sets the value of the PWG5101.1 IPP "media-source" option.
     *
     * @param mediaSource
     *            The {@link IppDictJobTemplateAttr#ATTR_MEDIA_SOURCE} value.
     */
    public void setMediaSourceOption(final String mediaSource) {
        this.optionValues.put(IppDictJobTemplateAttr.ATTR_MEDIA_SOURCE,
                mediaSource);
    }

    /**
     * Sets the value of the "print-scaling" option.
     *
     * @param printScaling
     *            The {@link PrintScalingEnum}.
     */
    public void setPrintScalingOption(final PrintScalingEnum printScaling) {
        this.optionValues.put(PrintScalingEnum.IPP_NAME,
                printScaling.getIppValue());
    }

    /**
     * Gets the value of the "print-scaling" option.
     *
     * @return The {@link PrintScalingEnum}.
     */
    public PrintScalingEnum getPrintScalingOption() {
        return PrintScalingEnum
                .fromIppValue(this.optionValues.get(PrintScalingEnum.IPP_NAME));
    }

    @Override
    public boolean isOddOrEvenSheets() {
        return isOddOrEvenSheets(getOptionValues());
    }

    @Override
    public boolean isCoverPageBefore() {
        return isCoverPageBefore(getOptionValues());
    }

    @Override
    public boolean isCoverPageAfter() {
        return isCoverPageAfter(getOptionValues());
    }

    /**
     * @param optionValues
     * @return {@code true} if <u>printer</u> is capable of duplex printing.
     */
    public static boolean hasDuplex(Map<String, String> optionValues) {
        return optionValues.get(IppDictJobTemplateAttr.ATTR_SIDES) != null;
    }

    /**
     *
     * @param optionValues
     */
    public static void setDuplexLongEdge(Map<String, String> optionValues) {
        optionValues.put(IppDictJobTemplateAttr.ATTR_SIDES,
                IppKeyword.SIDES_TWO_SIDED_LONG_EDGE);
    }

    /**
     *
     * @param optionValues
     */
    public static void setDuplexShortEdge(Map<String, String> optionValues) {
        optionValues.put(IppDictJobTemplateAttr.ATTR_SIDES,
                IppKeyword.SIDES_TWO_SIDED_SHORT_EDGE);
    }

    /**
     * Set to one-sided printing.
     */
    public void setSinglex() {
        this.getOptionValues().put(IppDictJobTemplateAttr.ATTR_SIDES,
                IppKeyword.SIDES_ONE_SIDED);
    }

    /**
    *
    */
    public void setDuplexLongEdge() {
        setDuplexLongEdge(this.getOptionValues());
    }

    /**
    *
    */
    public void setDuplexShortEdge() {
        setDuplexShortEdge(this.getOptionValues());
    }

    /**
     *
     * @param optionValues
     * @return {@code true} if <u>this job</u> is printed in duplex.
     */
    public static boolean isDuplex(Map<String, String> optionValues) {
        boolean duplex = false;
        final String value =
                optionValues.get(IppDictJobTemplateAttr.ATTR_SIDES);
        if (value != null) {
            duplex = !value.equals(IppKeyword.SIDES_ONE_SIDED);
        }
        return duplex;
    }

    public static boolean isGrayscale(Map<String, String> optionValues) {
        boolean grayscale = true;
        final String value =
                optionValues.get(IppDictJobTemplateAttr.ATTR_PRINT_COLOR_MODE);
        if (value != null) {
            grayscale = value.equals(IppKeyword.PRINT_COLOR_MODE_MONOCHROME);
        }
        return grayscale;
    }

    public static boolean isBooklet(Map<String, String> optionValues) {
        boolean booklet = false;
        final String value = optionValues.get(
                IppDictJobTemplateAttr.ORG_SAVAPAGE_ATTR_FINISHINGS_BOOKLET);
        if (value != null) {
            booklet = !value.equals(
                    IppKeyword.ORG_SAVAPAGE_ATTR_FINISHINGS_BOOKLET_NONE);
        }
        return booklet;
    }

    public void setGrayscale() {
        getOptionValues().put(IppDictJobTemplateAttr.ATTR_PRINT_COLOR_MODE,
                IppKeyword.PRINT_COLOR_MODE_MONOCHROME);
    }

    public void setColor() {
        getOptionValues().put(IppDictJobTemplateAttr.ATTR_PRINT_COLOR_MODE,
                IppKeyword.PRINT_COLOR_MODE_COLOR);
    }

    /**
     * @return The {@link IppOptionMap}.
     */
    public IppOptionMap createIppOptionMap() {
        return new IppOptionMap(this.getOptionValues());
    }

    /**
     * @return {@code true} when one of the job pages has landscape orientation.
     *         {@code null} when unknown.
     */
    public Boolean getLandscape() {
        return landscape;
    }

    /**
     * @param landscape
     *            {@code true} when one of the job pages has landscape
     *            orientation. {@code null} when unknown.
     */
    public void setLandscape(Boolean landscape) {
        this.landscape = landscape;
    }

    /**
     * @return The PDF inbox document orientation of the first page to be proxy
     *         printed.
     */
    public PdfOrientationInfo getPdfOrientation() {
        return pdfOrientation;
    }

    /**
     * @param pdfOrientation
     *            The PDF inbox document orientation of the first page to be
     *            proxy printed.
     */
    public void setPdfOrientation(PdfOrientationInfo pdfOrientation) {
        this.pdfOrientation = pdfOrientation;
    }

    public static int getNup(Map<String, String> optionValues) {
        int nUp = 1;
        final String value =
                optionValues.get(IppDictJobTemplateAttr.ATTR_NUMBER_UP);
        if (value != null) {
            nUp = Integer.parseInt(value, 10);
        }
        return nUp;
    }

    /**
     * Not supported yet: always returns false.
     *
     * @return {@code false}.
     */
    public static boolean isOddOrEvenSheets(Map<String, String> optionValues) {
        return false;
    }

    /**
     * Not supported yet: always returns false.
     *
     * @return {@code false}.
     */
    public static boolean isCoverPageBefore(Map<String, String> optionValues) {
        return false;
    }

    /**
     * Not supported yet: always returns false.
     *
     * @return {@code false}.
     */
    public static boolean isCoverPageAfter(Map<String, String> optionValues) {
        return false;
    }

    public final ProxyPrintJobChunkInfo getJobChunkInfo() {
        return jobChunkInfo;
    }

    public final void
            setJobChunkInfo(final ProxyPrintJobChunkInfo jobChunkInfo) {
        this.jobChunkInfo = jobChunkInfo;
    }

    public AccountTrxInfoSet getAccountTrxInfoSet() {
        return accountTrxInfoSet;
    }

    public void setAccountTrxInfoSet(AccountTrxInfoSet accountTrxInfoSet) {
        this.accountTrxInfoSet = accountTrxInfoSet;
    }

    public ExternalSupplierInfo getSupplierInfo() {
        return supplierInfo;
    }

    public void setSupplierInfo(ExternalSupplierInfo supplierInfo) {
        this.supplierInfo = supplierInfo;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    /**
     * Creates {@link ProxyPrintCostParms}.
     *
     * @param proxyPrinter
     *            The target proxy printer (can be {@code null}, in which case
     *            no custom media/copy costs are applicable).
     * @return The {@link ProxyPrintCostParms}.
     */
    public final ProxyPrintCostParms
            createProxyPrintCostParms(final JsonProxyPrinter proxyPrinter) {

        final ProxyPrintCostParms costParms =
                new ProxyPrintCostParms(proxyPrinter);

        costParms.setDuplex(this.isDuplex());
        costParms.setEcoPrint(this.isEcoPrintShadow());
        costParms.setGrayscale(this.isGrayscale());
        costParms.setNumberOfCopies(this.getNumberOfCopies());
        costParms.setPagesPerSide(this.getNup());
        costParms.setIppMediaOption(this.getMediaOption());

        costParms.importIppOptionValues(this.getOptionValues());

        costParms.calcCustomCost();

        return costParms;
    }

    /**
     *
     * @return {@code true} if PDF must to be converted to grayscale before
     *         proxy printing.
     */
    public final boolean isConvertToGrayscale() {
        return convertToGrayscale;
    }

    /**
     *
     * @param convertToGrayscale
     *            {@code true} if PDF must to be converted to grayscale before
     *            proxy printing.
     */
    public final void setConvertToGrayscale(boolean convertToGrayscale) {
        this.convertToGrayscale = convertToGrayscale;
    }

    /**
     * @return {@code true} if booklet page ordering is performed client-side
     *         (locally).
     */
    public boolean isLocalBooklet() {
        return localBooklet;
    }

    /**
     *
     * @param localBooklet
     *            {@code true} if booklet page ordering is performed client-side
     *            (locally).
     */
    public void setLocalBooklet(boolean localBooklet) {
        this.localBooklet = localBooklet;
    }

    public String getJobTicketNumber() {
        return jobTicketNumber;
    }

    public void setJobTicketNumber(String jobTicketNumber) {
        this.jobTicketNumber = jobTicketNumber;
    }

    /**
     * @return Job Ticket Domain or {@code null}.
     */
    public String getJobTicketDomain() {
        return jobTicketDomain;
    }

    /**
     * @param jobTicketDomain
     *            Job Ticket Domain or {@code null}.
     */
    public void setJobTicketDomain(String jobTicketDomain) {
        this.jobTicketDomain = jobTicketDomain;
    }

    /**
     * @return Job Ticket Use or {@code null}.
     */
    public String getJobTicketUse() {
        return jobTicketUse;
    }

    /**
     * @param jobTicketUse
     *            Job Ticket Use or {@code null}.
     */
    public void setJobTicketUse(String jobTicketUse) {
        this.jobTicketUse = jobTicketUse;
    }

    /**
     * @return Job Ticket Tag or {@code null}.
     */
    public String getJobTicketTag() {
        return jobTicketTag;
    }

    /**
     * @param jobTicketTag
     *            Job Ticket Tag or {@code null}.
     */
    public void setJobTicketTag(String jobTicketTag) {
        this.jobTicketTag = jobTicketTag;
    }

}
