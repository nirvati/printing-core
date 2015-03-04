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
package org.savapage.core.print.proxy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import javax.print.attribute.standard.MediaSizeName;

import org.savapage.core.dto.IppMediaSourceCostDto;
import org.savapage.core.inbox.RangeAtom;
import org.savapage.core.ipp.IppMediaSizeEnum;

/**
 *
 * @author Datraverse B.V.
 *
 */
public final class ProxyPrintJobChunk {

    private final List<ProxyPrintJobChunkRange> ranges = new ArrayList<>();

    private MediaSizeName mediaSizeName;

    private IppMediaSourceCostDto assignedMediaSource;
    private IppMediaSizeEnum assignedMedia;
    private Boolean fitToPage;

    private BigDecimal cost;

    /**
     *
     * @return
     */
    public List<ProxyPrintJobChunkRange> getRanges() {
        return ranges;
    }

    public MediaSizeName getMediaSizeName() {
        return mediaSizeName;
    }

    public void setMediaSizeName(MediaSizeName mediaSizeName) {
        this.mediaSizeName = mediaSizeName;
    }

    public int getNumberOfPages() {

        int numberOfPages = 0;

        for (final RangeAtom rangeAtom : this.ranges) {
            numberOfPages +=
                    rangeAtom.calcPageTo() - rangeAtom.calcPageFrom() + 1;
        }

        return numberOfPages;
    }

    public IppMediaSourceCostDto getAssignedMediaSource() {
        return assignedMediaSource;
    }

    public void setAssignedMediaSource(IppMediaSourceCostDto assignedMediaSource) {
        this.assignedMediaSource = assignedMediaSource;
    }

    public IppMediaSizeEnum getAssignedMedia() {
        return assignedMedia;
    }

    public void setAssignedMedia(IppMediaSizeEnum assignedMedia) {
        this.assignedMedia = assignedMedia;
    }

    public Boolean getFitToPage() {
        return fitToPage;
    }

    public void setFitToPage(Boolean fitToPage) {
        this.fitToPage = fitToPage;
    }

    public BigDecimal getCost() {
        return cost;
    }

    public void setCost(BigDecimal cost) {
        this.cost = cost;
    }

}
