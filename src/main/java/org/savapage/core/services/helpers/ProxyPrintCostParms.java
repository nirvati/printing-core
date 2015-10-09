/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2014 Datraverse B.V.
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
package org.savapage.core.services.helpers;

import org.savapage.core.dto.IppMediaSourceCostDto;

/**
 *
 * @author Datraverse B.V.
 */
public class ProxyPrintCostParms {

    private int numberOfCopies;
    private int numberOfPages;
    private int pagesPerSide;

    private IppMediaSourceCostDto mediaSourceCost;

    private String ippMediaOption;
    private boolean grayscale;
    private boolean duplex;
    private boolean ecoPrint;

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

}
