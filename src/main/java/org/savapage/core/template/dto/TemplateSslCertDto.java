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
package org.savapage.core.template.dto;

import java.util.Date;

import org.savapage.core.config.SslCertInfo;

/**
 *
 * @author Rijk Ravestein
 *
 */
public class TemplateSslCertDto implements TemplateDto {

    /** */
    private String issuerCN;
    /** */
    private Date creationDate;
    /** */
    private Date notAfter;
    /** */
    private boolean notAfterWarning;
    /** */
    private boolean notAfterError;
    /** */
    private boolean selfSigned;

    public String getIssuerCN() {
        return issuerCN;
    }

    public void setIssuerCN(String issuerCN) {
        this.issuerCN = issuerCN;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public Date getNotAfter() {
        return notAfter;
    }

    public void setNotAfter(Date notAfter) {
        this.notAfter = notAfter;
    }

    public boolean isSelfSigned() {
        return selfSigned;
    }

    public void setSelfSigned(boolean selfSigned) {
        this.selfSigned = selfSigned;
    }

    public boolean isNotAfterError() {
        return notAfterError;
    }

    public void setNotAfterError(boolean notAfterError) {
        this.notAfterError = notAfterError;
    }

    public boolean isNotAfterWarning() {
        return notAfterWarning;
    }

    public void setNotAfterWarning(boolean notAfterWarning) {
        this.notAfterWarning = notAfterWarning;
    }

    /**
     * Creates template from info.
     *
     * @param info
     *            {@link SslCertInfo}.
     * @return
     */
    public static TemplateSslCertDto create(final SslCertInfo info) {
        final TemplateSslCertDto dto = new TemplateSslCertDto();
        dto.creationDate = info.getCreationDate();
        dto.issuerCN = info.getIssuerCN();
        dto.notAfter = info.getNotAfter();
        dto.selfSigned = info.isSelfSigned();
        return dto;
    }
}
