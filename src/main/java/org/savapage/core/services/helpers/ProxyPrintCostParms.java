/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2017 Datraverse B.V.
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
package org.savapage.core.services.helpers;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.savapage.core.dto.IppMediaSourceCostDto;
import org.savapage.core.ipp.attribute.IppDictJobTemplateAttr;
import org.savapage.core.ipp.attribute.syntax.IppKeyword;
import org.savapage.core.print.proxy.JsonProxyPrinter;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class ProxyPrintCostParms {

    /**
     *
     */
    private int numberOfCopies;

    /**
     * The total number of pages. <b>Note</b>: Blank filler pages are <i>not</i>
     * included in the count.
     */
    private int numberOfPages;

    /**
     * The number of pages of logical sub-jobs. <b>Note</b>: Blank filler pages
     * are <i>not</i> included in the count. When {@code null}, no logical
     * sub-jobs are defined, and {@link #numberOfPages} must be used to
     * calculate the cost.
     */
    private List<Integer> logicalNumberOfPages;

    /**
     * Number of pages per sheet-side.
     */
    private int pagesPerSide;

    /**
     * .
     */
    private IppMediaSourceCostDto mediaSourceCost;

    private String ippMediaOption;
    private boolean grayscale;
    private boolean duplex;
    private boolean ecoPrint;

    /**
     *
     */
    private final JsonProxyPrinter proxyPrinter;

    /**
     * Work area for calculating custom cost.
     */
    private Map<String, String> ippOptionValues;

    /**
     * Custom cost per single-sided media side. When not {@code null} this value
     * is leading.
     */
    private BigDecimal customCostMediaSide;

    /**
     * Custom cost per duplex media side. When not {@code null} this value is
     * leading.
     */
    private BigDecimal customCostMediaSideDuplex;

    /**
     * Additional custom cost for one (1) copy.
     */
    private BigDecimal customCostCopy;

    /**
     * Cost for {@link IppDictJobTemplateAttr#ORG_SAVAPAGE_ATTR_COVER_TYPE}.
     * When {@code null} a Cover is not applicable.
     */
    private BigDecimal customCostCoverPrint;

    /**
     * Number of media pages for
     * {@link IppDictJobTemplateAttr#ORG_SAVAPAGE_ATTR_COVER_TYPE}. Value can be
     * {@code 1} for
     * {@link IppKeyword#ORG_SAVAPAGE_ATTR_COVER_TYPE_PRINTFRONT_EXT_PFX}, or
     * {@code 2} for
     * {@link IppKeyword#ORG_SAVAPAGE_ATTR_COVER_TYPE_PRINTBOTH_EXT_PFX}. If
     * {@code 0} a Cover is not applicable.
     */
    private int customCoverPrintPages;

    /**
     * Constructor.
     *
     * @param printer
     *            The {@link JsonProxyPrinter} used to calculate custom
     *            media/copy costs. Can be {@code null}, in which case no custom
     *            costs are calculated/applied.
     */
    public ProxyPrintCostParms(final JsonProxyPrinter printer) {
        this.proxyPrinter = printer;
    }

    /**
     *
     * @return The number of copies.
     */
    public int getNumberOfCopies() {
        return numberOfCopies;
    }

    public void setNumberOfCopies(int numberOfCopies) {
        this.numberOfCopies = numberOfCopies;
    }

    public int getNumberOfPages() {
        return numberOfPages;
    }

    public void setNumberOfPages(int numberOfPages) {
        this.numberOfPages = numberOfPages;
    }

    public int getPagesPerSide() {
        return pagesPerSide;
    }

    public void setPagesPerSide(int pagesPerSide) {
        this.pagesPerSide = pagesPerSide;
    }

    public String getIppMediaOption() {
        return ippMediaOption;
    }

    public void setIppMediaOption(String ippMediaOption) {
        this.ippMediaOption = ippMediaOption;
    }

    public boolean isGrayscale() {
        return grayscale;
    }

    public void setGrayscale(boolean grayscale) {
        this.grayscale = grayscale;
    }

    public boolean isDuplex() {
        return duplex;
    }

    public void setDuplex(boolean duplex) {
        this.duplex = duplex;
    }

    public boolean isEcoPrint() {
        return ecoPrint;
    }

    public void setEcoPrint(boolean ecoPrint) {
        this.ecoPrint = ecoPrint;
    }

    public IppMediaSourceCostDto getMediaSourceCost() {
        return mediaSourceCost;
    }

    public void setMediaSourceCost(IppMediaSourceCostDto mediaSourceCost) {
        this.mediaSourceCost = mediaSourceCost;
    }

    /**
     * @return The number of pages of logical sub-jobs. <b>Note</b>: Blank
     *         filler pages are <i>not</i> included in the count. When
     *         {@code null}, no logical sub-jobs are defined, and
     *         {@link #numberOfPages} must be used to calculate the cost.
     */
    public List<Integer> getLogicalNumberOfPages() {
        return logicalNumberOfPages;
    }

    /**
     * @param logicalNumberOfPages
     *            The number of pages of logical sub-jobs. <b>Note</b>: Blank
     *            filler pages are <i>not</i> included in the count. When
     *            {@code null}, no logical sub-jobs are defined, and
     *            {@link #numberOfPages} must be used to calculate the cost.
     */
    public void setLogicalNumberOfPages(List<Integer> logicalNumberOfPages) {
        this.logicalNumberOfPages = logicalNumberOfPages;
    }

    /**
     * Imports IPP option values. All values are put in a new {@link Map}, since
     * we use it as work area to calculate custom costs.
     *
     * @param optionValues
     *            The IPP option values.
     */
    public void importIppOptionValues(Map<String, String> optionValues) {
        this.ippOptionValues = new HashMap<>();
        this.ippOptionValues.putAll(optionValues);
    }

    /**
     *
     * @return Custom cost per single-sided media side. When not {@code null}
     *         this value is leading.
     */
    public BigDecimal getCustomCostMediaSide() {
        return customCostMediaSide;
    }

    /**
     * @return Custom cost per duplex media side. When not {@code null} this
     *         value is leading.
     */
    public BigDecimal getCustomCostMediaSideDuplex() {
        return customCostMediaSideDuplex;
    }

    /**
     *
     * @return Additional custom cost for one (1) copy.
     */
    public BigDecimal getCustomCostCopy() {
        return customCostCopy;
    }

    /**
     * @return Cost for
     *         {@link IppDictJobTemplateAttr#ORG_SAVAPAGE_ATTR_COVER_TYPE}. When
     *         {@code null} a Cover is not applicable.
     */
    public BigDecimal getCustomCostCoverPrint() {
        return customCostCoverPrint;
    }

    /**
     * @return Number of media pages for
     *         {@link IppDictJobTemplateAttr#ORG_SAVAPAGE_ATTR_COVER_TYPE}.
     *         Value can be {@code 1} for
     *         {@link IppKeyword#ORG_SAVAPAGE_ATTR_COVER_TYPE_PRINTFRONT_EXT_PFX}
     *         , or {@code 2} for
     *         {@link IppKeyword#ORG_SAVAPAGE_ATTR_COVER_TYPE_PRINTBOTH_EXT_PFX}
     *         . If {@code 0} a Cover is not applicable.
     */
    public int getCustomCoverPrintPages() {
        return customCoverPrintPages;
    }

    /**
     * (Re)calculates the custom media/copy costs.
     * <p>
     * NOTE: Use this method <i>after</i> {@link #setIppMediaOption(String)} and
     * {@link #importIppOptionValues(Map)}
     * </p>
     */
    public void calcCustomCost() {

        if (this.proxyPrinter == null || this.ippOptionValues == null) {
            this.customCostCopy = null;
            this.customCostMediaSide = null;
            this.customCostCoverPrint = null;
            this.customCoverPrintPages = 0;
            return;
        }

        /*
         * Cover cost and pages
         */
        final String ippCoverChoice = this.ippOptionValues
                .get(IppDictJobTemplateAttr.ORG_SAVAPAGE_ATTR_COVER_TYPE);

        if (ippCoverChoice == null) {

            this.customCostCoverPrint = null;
            this.customCoverPrintPages = 0;

        } else {

            this.customCostCoverPrint =
                    this.proxyPrinter.getCustomCostCover(ippCoverChoice);

            if (this.customCostCoverPrint == null) {
                this.customCoverPrintPages = 0;
            } else if (ippCoverChoice.startsWith(
                    IppKeyword.ORG_SAVAPAGE_ATTR_COVER_TYPE_PRINTBOTH_EXT_PFX)) {
                this.customCoverPrintPages = 2;
            } else {
                this.customCoverPrintPages = 1;
            }

        }
        /*
         * Media dependent cost.
         */
        this.ippOptionValues.put(IppDictJobTemplateAttr.ATTR_MEDIA,
                this.ippMediaOption);

        this.customCostCopy =
                this.proxyPrinter.calcCustomCostCopy(this.ippOptionValues);

        //
        final BigDecimal costWrk =
                this.proxyPrinter.calcCustomCostMedia(this.ippOptionValues);

        if (isDuplex()) {

            this.customCostMediaSideDuplex = costWrk;

            // Save sides option.
            final String sidesSaved =
                    this.ippOptionValues.get(IppDictJobTemplateAttr.ATTR_SIDES);

            this.ippOptionValues.put(IppDictJobTemplateAttr.ATTR_SIDES,
                    IppKeyword.SIDES_ONE_SIDED);

            this.customCostMediaSide =
                    this.proxyPrinter.calcCustomCostMedia(this.ippOptionValues);

            // Restore sides option.
            this.ippOptionValues.put(IppDictJobTemplateAttr.ATTR_SIDES,
                    sidesSaved);

        } else {
            this.customCostMediaSide = costWrk;
            this.customCostMediaSideDuplex = costWrk;
        }

    }

}
