/*
 * This file is part of the SavaPage project <https://savapage.org>.
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

import java.util.UUID;

import org.savapage.core.dao.enums.PrintModeEnum;
import org.savapage.core.dto.IppMediaSourceCostDto;
import org.savapage.core.ipp.IppMediaSizeEnum;
import org.savapage.core.ipp.attribute.syntax.IppKeyword;
import org.savapage.core.jpa.DocIn;
import org.savapage.core.jpa.Printer;
import org.savapage.core.services.helpers.PrinterAttrLookup;

/**
 * Proxy Print Request based on a {@link DocIn} document identified by
 * {@link UUID}.
 *
 * @author Rijk Ravestein
 *
 */
public final class ProxyPrintDocReq extends AbstractProxyPrintReq {

    /** */
    private String documentUuid;

    /** */
    public String getDocumentUuid() {
        return documentUuid;
    }

    public void setDocumentUuid(String documentUuid) {
        this.documentUuid = documentUuid;
    }

    public ProxyPrintDocReq(final PrintModeEnum printMode) {
        super(printMode);
    }

    /**
     * Adds a "pro-forma" {@link ProxyPrintJobChunk} object.
     *
     * @param printer
     *            The {@link Printer}.
     * @param ippMediaSize
     *            The media size.
     * @param hasMediaSourceAuto
     *            {@code true} when printer has "auto"media source.
     * @param isManagedByPaperCut
     *            {@code true} when printer is managed by PaperCut.
     * @throws ProxyPrintException
     *             When printer has no media-source for media size.
     */
    public void addProxyPrintJobChunk(final Printer printer,
            final IppMediaSizeEnum ippMediaSize,
            final boolean hasMediaSourceAuto, final boolean isManagedByPaperCut)
            throws ProxyPrintException {

        final String printerName = this.getPrinterName();

        final PrinterAttrLookup printerAttrLookup =
                new PrinterAttrLookup(printer);

        /*
         * INVARIANT: If printer has media sources defined, a media-source MUST
         * be available that matches the media size of the document.
         */
        final IppMediaSourceCostDto assignedMediaSourceCost =
                printerAttrLookup.findAnyMediaSourceForMedia(ippMediaSize);

        if (assignedMediaSourceCost == null) {
            throw new ProxyPrintException(String.format(
                    "Printer %s has no media-source with media %s", printerName,
                    ippMediaSize.getIppKeyword()));
        }

        final ProxyPrintJobChunk jobChunk = new ProxyPrintJobChunk();

        jobChunk.setAssignedMedia(ippMediaSize);

        /*
         * If the printer is managed by PaperCut, set "media-source" to "auto"
         * in the Print Request if printer supports it, otherwise set the
         * assigned media-source in the Job Chunk.
         */
        if (isManagedByPaperCut && hasMediaSourceAuto) {
            this.setMediaSourceOption(IppKeyword.MEDIA_SOURCE_AUTO);
            jobChunk.setAssignedMediaSource(null);
            jobChunk.setIppMediaSource(IppKeyword.MEDIA_SOURCE_AUTO);
        } else {
            jobChunk.setAssignedMediaSource(assignedMediaSourceCost);
            jobChunk.setIppMediaSource(assignedMediaSourceCost.getSource());
        }

        /*
         * Chunk range begins at first page.
         */
        final ProxyPrintJobChunkRange chunkRange =
                new ProxyPrintJobChunkRange();

        chunkRange.pageBegin = 1;
        chunkRange.pageEnd = chunkRange.pageBegin + this.getNumberOfPages() - 1;

        jobChunk.getRanges().add(chunkRange);

        this.setJobChunkInfo(new ProxyPrintJobChunkInfo(jobChunk));
    }

}
