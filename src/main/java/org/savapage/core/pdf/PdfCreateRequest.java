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
package org.savapage.core.pdf;

import org.savapage.core.inbox.InboxInfoDto;
import org.savapage.core.jpa.User;

/**
 * Page Properties of a PDF file.
 *
 * @author Datraverse B.V.
 */
public class PdfCreateRequest {

    /**
     * The user object.
     */
    private User userObj;

    /**
     * pdfFile The name of the PDF file to generate.
     */
    private String pdfFile;

    /**
     * The {@link InboxInfoDto} (with the filtered jobs).
     */
    private InboxInfoDto inboxInfo;

    /**
     * If <code>true</code> graphics are removed (minified to
     * one-pixel).
     */
    private boolean removeGraphics;

    /**
     * If <code>true</code> the stored PDF properties for 'user'
     * should be applied.
     */
    private boolean applyPdfProps;

    /**
     *
     */
    private boolean applyLetterhead;

    /**
     * <code>true</code> if this is a PDF generated for
     * printing.
     */
    private boolean forPrinting;

    /**
     * <code>true</code> if Eco PDF is to be generated.
     */
    private boolean ecoPdf;

    /**
     * <code>true</code> if grayscale PDF is to be generated.
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

    public boolean isEcoPdf() {
        return ecoPdf;
    }

    public void setEcoPdf(boolean ecoPdf) {
        this.ecoPdf = ecoPdf;
    }

    public boolean isGrayscale() {
        return grayscale;
    }

    public void setGrayscale(boolean grayscale) {
        this.grayscale = grayscale;
    }

}
