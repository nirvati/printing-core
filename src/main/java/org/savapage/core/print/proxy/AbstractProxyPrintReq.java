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
package org.savapage.core.print.proxy;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.savapage.core.dao.enums.PrintModeEnum;
import org.savapage.core.ipp.attribute.IppDictJobTemplateAttr;
import org.savapage.core.ipp.attribute.syntax.IppKeyword;
import org.savapage.core.services.helpers.AccountTrxInfoSet;
import org.savapage.core.services.helpers.ExternalSupplierInfo;

/**
 * Proxy Print Request base on the SafePages inbox.
 *
 * @author Datraverse B.V.
 *
 */
public abstract class AbstractProxyPrintReq implements
        ProxyPrintSheetsCalcParms {

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
    private String jobName;
    private int numberOfCopies;
    private int numberOfPages;
    private boolean removeGraphics;

    private boolean ecoPrint;

    private boolean ecoPrintShadow;

    /**
     * Collate pages.
     */
    private boolean collate;

    private Locale locale = Locale.getDefault();

    private String userMsg;
    private String userMsgKey;
    private Long idUser;
    private BigDecimal cost;

    private Date submitDate;

    private Map<String, String> optionValues;

    private boolean clearPages;

    private Boolean fitToPage;

    private ProxyPrintJobChunkInfo jobChunkInfo;

    private AccountTrxInfoSet accountTrxInfoSet;

    private String comment;

    /**
     * .
     */
    private ExternalSupplierInfo supplierInfo;

    /**
     * The number of SafePages cleared at status {@link Status#PRINTED}.
     */
    private int clearedPages = 0;

    /**
     *
     * @param printMode
     */
    protected AbstractProxyPrintReq(final PrintModeEnum printMode) {
        this.printMode = printMode;
    }

    /**
     *
     * @return
     */
    public boolean isClearPages() {
        return clearPages;
    }

    public void setClearPages(boolean clearPages) {
        this.clearPages = clearPages;
    }

    /**
     * Gets the number of SafePages cleared at status {@link Status#PRINTED}.
     *
     * @return
     */
    public int getClearedPages() {
        return clearedPages;
    }

    /**
     * Sets the number of SafePages cleared at status {@link Status#PRINTED}.
     *
     * @param clearedPages
     */
    public void setClearedPages(int clearedPages) {
        this.clearedPages = clearedPages;
    }

    public boolean isRemoveGraphics() {
        return removeGraphics;
    }

    public void setRemoveGraphics(boolean removeGraphics) {
        this.removeGraphics = removeGraphics;
    }

    /**
     *
     * @return {@code true} if Eco PDF is to be created.
     */
    public boolean isEcoPrint() {
        return ecoPrint;
    }

    /**
     *
     * @param ecoPrint
     *            {@code true} if Eco PDF is to be created.
     */
    public void setEcoPrint(boolean ecoPrint) {
        this.ecoPrint = ecoPrint;
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

    public BigDecimal getCost() {
        return cost;
    }

    public void setCost(BigDecimal cost) {
        this.cost = cost;
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
     * Gets the value of the PWG5101.1 IPP "media" option.
     *
     * @return
     */
    public String getMediaOption() {
        return this.optionValues.get(IppDictJobTemplateAttr.ATTR_MEDIA);
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

    public void setGrayscale() {
        getOptionValues().put(IppDictJobTemplateAttr.ATTR_PRINT_COLOR_MODE,
                IppKeyword.PRINT_COLOR_MODE_MONOCHROME);
    }

    public void setColor() {
        getOptionValues().put(IppDictJobTemplateAttr.ATTR_PRINT_COLOR_MODE,
                IppKeyword.PRINT_COLOR_MODE_COLOR);
    }

    public Boolean getFitToPage() {
        return fitToPage;
    }

    public void setFitToPage(Boolean fitToPage) {
        this.fitToPage = fitToPage;
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

}
