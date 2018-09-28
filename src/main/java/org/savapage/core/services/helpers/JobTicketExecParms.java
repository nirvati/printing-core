/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2018 Datraverse B.V.
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

import java.util.Locale;

import org.savapage.core.dao.enums.ACLRoleEnum;
import org.savapage.core.ipp.attribute.IppDictJobTemplateAttr;
import org.savapage.core.jpa.Printer;
import org.savapage.core.jpa.User;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class JobTicketExecParms {

    /**
     * The {@link User#getUserId()} with
     * {@link ACLRoleEnum#JOB_TICKET_OPERATOR}.
     */
    private String operator;

    /**
     * The (new redirect printer.
     */
    private Printer printer;

    /**
     * The {@link IppDictJobTemplateAttr#ATTR_MEDIA_SOURCE} value for the print
     * job.
     */
    private String ippMediaSource;

    /**
     * The {@link IppDictJobTemplateAttr#ATTR_MEDIA_SOURCE} value for the Job
     * Sheet print job.
     */
    private String ippMediaSourceJobSheet;

    /**
     * The {@link IppDictJobTemplateAttr#ATTR_OUTPUT_BIN} value for the print
     * job.
     */
    private String ippOutputBin;

    /**
     * The
     * {@link IppDictJobTemplateAttr#ORG_SAVAPAGE_ATTR_FINISHINGS_JOG_OFFSET}
     * value for the print job.
     */
    private String ippJogOffset;

    /**
     * The unique PDF file name of the job to print.
     */
    private String fileName;

    /**
     * The {@link Locale} for user messages.
     *
     */
    private Locale locale;

    /**
     * Constructor.
     */
    public JobTicketExecParms() {
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public Printer getPrinter() {
        return printer;
    }

    public void setPrinter(Printer printer) {
        this.printer = printer;
    }

    public String getIppMediaSource() {
        return ippMediaSource;
    }

    public void setIppMediaSource(String ippMediaSource) {
        this.ippMediaSource = ippMediaSource;
    }

    public String getIppMediaSourceJobSheet() {
        return ippMediaSourceJobSheet;
    }

    public void setIppMediaSourceJobSheet(String ippMediaSourceJobSheet) {
        this.ippMediaSourceJobSheet = ippMediaSourceJobSheet;
    }

    public String getIppOutputBin() {
        return ippOutputBin;
    }

    public void setIppOutputBin(String ippOutputBin) {
        this.ippOutputBin = ippOutputBin;
    }

    public String getIppJogOffset() {
        return ippJogOffset;
    }

    public void setIppJogOffset(String ippJogOffset) {
        this.ippJogOffset = ippJogOffset;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Locale getLocale() {
        return locale;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

}
