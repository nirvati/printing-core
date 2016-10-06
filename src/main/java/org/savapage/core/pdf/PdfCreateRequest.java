/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2016 Datraverse B.V.
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
package org.savapage.core.pdf;

import org.savapage.core.inbox.InboxInfoDto;
import org.savapage.core.jpa.User;

/**
 * A request to create a PDF file.
 *
 * @author Rijk Ravestein
 *
 */
public final class PdfCreateRequest {

    /**
     * The {@link User} to create the PDF for.
     */
    private User userObj;

    /**
     * pdfFile The name of the PDF file to create.
     */
    private String pdfFile;

    /**
     * The {@link InboxInfoDto} (with the filtered jobs).
     */
    private InboxInfoDto inboxInfo;

    /**
     * If {@code true} graphics are to be removed (minified to one-pixel).
     */
    private boolean removeGraphics;

    /**
     * If {@code true} the stored PDF properties for 'user' should be applied.
     */
    private boolean applyPdfProps;

    /**
     * {@code true} When letterhead should be applied.
     */
    private boolean applyLetterhead;

    /**
     * {@code true} if this is a PDF created for printing.
     */
    private boolean forPrinting;

    /**
     * {@code true} when duplex (printing only).
     */
    private boolean printDuplex = false;

    /**
     * Number of pages per side (printing only).
     */
    private int printNup = 1;

    /**
     * {@code true} if Eco PDF shadow is to be used.
     */
    private boolean ecoPdfShadow;

    /**
     * {@code true} if grayscale PDF is to be created.
     */
    private boolean grayscale;

    public User getUserObj() {
        return userObj;
    }

    public void setUserObj(User userObj) {
        this.userObj = userObj;
    }

    public String getPdfFile() {
        return pdfFile;
    }

    public void setPdfFile(String pdfFile) {
        this.pdfFile = pdfFile;
    }

    public InboxInfoDto getInboxInfo() {
        return inboxInfo;
    }

    public void setInboxInfo(InboxInfoDto inboxInfo) {
        this.inboxInfo = inboxInfo;
    }

    public boolean isRemoveGraphics() {
        return removeGraphics;
    }

    public void setRemoveGraphics(boolean removeGraphics) {
        this.removeGraphics = removeGraphics;
    }

    public boolean isApplyPdfProps() {
        return applyPdfProps;
    }

    public void setApplyPdfProps(boolean applyPdfProps) {
        this.applyPdfProps = applyPdfProps;
    }

    public boolean isApplyLetterhead() {
        return applyLetterhead;
    }

    public void setApplyLetterhead(boolean applyLetterhead) {
        this.applyLetterhead = applyLetterhead;
    }

    public boolean isForPrinting() {
        return forPrinting;
    }

    public void setForPrinting(boolean forPrinting) {
        this.forPrinting = forPrinting;
    }

    /**
     * @return {@code true} if Eco PDF shadow is to be used.
     */
    public boolean isEcoPdfShadow() {
        return ecoPdfShadow;
    }

    /**
     *
     * @param ecoPdfShadow
     *            {@code true} if Eco PDF shadow is to be used.
     */
    public void setEcoPdfShadow(boolean ecoPdfShadow) {
        this.ecoPdfShadow = ecoPdfShadow;
    }

    /**
     *
     * @return {@code true} if grayscale PDF is to be created.
     */
    public boolean isGrayscale() {
        return grayscale;
    }

    /**
     *
     * @param grayscale
     *            {@code true} if grayscale PDF is to be created.
     */
    public void setGrayscale(boolean grayscale) {
        this.grayscale = grayscale;
    }

    /**
     *
     * @return {@code true} when duplex (printing only).
     */
    public boolean isPrintDuplex() {
        return printDuplex;
    }

    /**
     *
     * @param printDuplex
     *            {@code true} when duplex (printing only).
     */
    public void setPrintDuplex(boolean printDuplex) {
        this.printDuplex = printDuplex;
    }

    /**
     *
     * @return Number of pages per side (printing only).
     */
    public int getPrintNup() {
        return printNup;
    }

    /**
     *
     * @param printNup
     *            Number of pages per side (printing only).
     */
    public void setPrintNup(int printNup) {
        this.printNup = printNup;
    }

}
