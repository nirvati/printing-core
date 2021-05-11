/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2021 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: © 2021 Datraverse B.V. <info@datraverse.com>
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
package org.savapage.core.pdf.facade;

import org.savapage.core.SpException;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Image;

/**
 * {@link com.lowagie.text.Document}: MPL license.
 *
 * @author Rijk Ravestein
 *
 */
public final class PdfDocumentMPL implements PdfDocumentFacade {

    /** */
    private final Document document;

    /**
     * Constructor.
     *
     * @param doc
     *            {@link com.lowagie.text.Document}.
     */
    public PdfDocumentMPL(final Document doc) {
        this.document = doc;
    }

    @Override
    public boolean isOpen() {
        return this.document.isOpen();
    }

    @Override
    public void open() {
        this.document.open();
    }

    @Override
    public boolean newPage() {
        return this.document.newPage();
    }

    @Override
    public boolean rotatePage() {
        return this.document.setPageSize(this.document.getPageSize().rotate());
    }

    @Override
    public float getPageWidth() {
        return this.document.getPageSize().getWidth();
    }

    @Override
    public float getPageHeight() {
        return this.document.getPageSize().getHeight();
    }

    @Override
    public float leftMargin() {
        return this.document.leftMargin();
    }

    @Override
    public float rightMargin() {
        return this.document.rightMargin();
    }

    @Override
    public float topMargin() {
        return this.document.topMargin();
    }

    @Override
    public float bottomMargin() {
        return this.document.bottomMargin();
    }

    @Override
    public boolean add(final PdfImageFacade image) {
        if (image instanceof PdfImageMPL) {
            try {
                this.document.add(((PdfImageMPL) image).getImage());
            } catch (DocumentException e) {
                throw new SpException(e);
            }
            return true;
        }
        throw new SpException(
                PdfImageMPL.class.getSimpleName().concat(" expected."));
    }

    /**
     * Creates an image face.
     *
     * @param img
     *            {@link com.lowagie.text.Image}.
     * @return Image facade.
     */
    public PdfImageFacade create(final Image img) {
        return new PdfImageMPL(img);
    }

}
