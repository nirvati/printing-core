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
package org.savapage.core.inbox;

import org.savapage.core.json.JsonAbstractBase;
import org.savapage.core.services.helpers.DocContentPrintInInfo;
import org.savapage.core.services.helpers.PdfRepairEnum;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 *
 * @author Rijk Ravestein
 *
 */
@JsonInclude(Include.NON_NULL)
public final class PrintInInfoDto extends JsonAbstractBase {

    private Boolean drmRestricted;

    private PdfRepairEnum pdfRepair;

    private String mimetype;

    private String jobName;

    private String comment;

    private String originatorIp;

    private String originatorEmail;

    public Boolean getDrmRestricted() {
        return drmRestricted;
    }

    public void setDrmRestricted(Boolean drmRestricted) {
        this.drmRestricted = drmRestricted;
    }

    public PdfRepairEnum getPdfRepair() {
        return pdfRepair;
    }

    public void setPdfRepair(PdfRepairEnum pdfRepair) {
        this.pdfRepair = pdfRepair;
    }

    public String getMimetype() {
        return mimetype;
    }

    public void setMimetype(String mimetype) {
        this.mimetype = mimetype;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getOriginatorIp() {
        return originatorIp;
    }

    public void setOriginatorIp(String originatorIp) {
        this.originatorIp = originatorIp;
    }

    public String getOriginatorEmail() {
        return originatorEmail;
    }

    public void setOriginatorEmail(String originatorEmail) {
        this.originatorEmail = originatorEmail;
    }

    /**
     * Creates an instance from {@link DocContentPrintInInfo}.
     *
     * @param info
     *            {@link DocContentPrintInInfo}.
     * @return Object.
     */
    public static PrintInInfoDto create(final DocContentPrintInInfo info) {

        final PrintInInfoDto pojo = new PrintInInfoDto();

        pojo.setComment(info.getLogComment());
        if (info.isDrmRestricted()) {
            pojo.setDrmRestricted(Boolean.TRUE);
        }
        pojo.setJobName(info.getJobName());
        pojo.setMimetype(info.getMimetype());
        pojo.setOriginatorEmail(info.getOriginatorEmail());
        pojo.setOriginatorIp(info.getOriginatorIp());
        pojo.setPdfRepair(info.getPdfRepair());

        return pojo;
    }

    /**
     * Creates an instance from JSON string.
     *
     * @param json
     *            JSON string
     * @return Object.
     * @throws Exception
     *             If syntax error.
     */
    public static PrintInInfoDto create(final String json) throws Exception {
        return getMapper().readValue(json, PrintInInfoDto.class);
    }
}
