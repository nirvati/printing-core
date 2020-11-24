/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2020 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: © 2020 Datraverse B.V. <info@datraverse.com>
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
package org.savapage.core.print.server;

import org.savapage.core.dao.enums.DocLogProtocolEnum;
import org.savapage.core.doc.DocContentTypeEnum;
import org.savapage.core.fonts.InternalFontFamilyEnum;

/**
 *
 * @author Rijk Ravestein
 *
 */
public class DocContentPrintReq {

    /**
     *
     */
    private DocLogProtocolEnum protocol;

    /**
     * MUST be present for {@link DocLogProtocolEnum#IMAP}. For all other
     * protocols {@code null}.
     */
    private String originatorEmail;

    /**
     * The originator client IP address.
     */
    private String originatorIp;

    /**
     * The title of the document.
     */
    private String title;

    /**
     * The (file) name of the document, used for logging purposes only.
     */
    private String fileName;

    /**
     *
     */
    private DocContentTypeEnum contentType;

    /**
     * The preferred font for the PDF output. This parameter is {@code null}
     * when (user) preference is unknown or irrelevant.
     */
    private InternalFontFamilyEnum preferredOutputFont;

    public DocLogProtocolEnum getProtocol() {
        return protocol;
    }

    public void setProtocol(DocLogProtocolEnum protocol) {
        this.protocol = protocol;
    }

    public String getOriginatorEmail() {
        return originatorEmail;
    }

    public void setOriginatorEmail(String originatorEmail) {
        this.originatorEmail = originatorEmail;
    }

    public String getOriginatorIp() {
        return originatorIp;
    }

    public void setOriginatorIp(String originatorIp) {
        this.originatorIp = originatorIp;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public DocContentTypeEnum getContentType() {
        return contentType;
    }

    public void setContentType(DocContentTypeEnum contentType) {
        this.contentType = contentType;
    }

    /**
     * Gets the preferred font for the PDF output.
     *
     * @return {@code null} when (user) preference is unknown or irrelevant.
     */
    public InternalFontFamilyEnum getPreferredOutputFont() {
        return preferredOutputFont;
    }

    /**
     * Sets the preferred font for the PDF output.
     *
     * @param preferredOutputFont
     *            {@code null} when (user) preference is unknown or irrelevant.
     */
    public void
            setPreferredOutputFont(InternalFontFamilyEnum preferredOutputFont) {
        this.preferredOutputFont = preferredOutputFont;
    }

}
