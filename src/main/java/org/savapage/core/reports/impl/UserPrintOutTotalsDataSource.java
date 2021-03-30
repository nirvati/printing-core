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
package org.savapage.core.reports.impl;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.savapage.core.config.ConfigManager;
import org.savapage.core.dao.helpers.UserPrintOutTotalsReq;
import org.savapage.core.dto.UserPrintOutTotalDto;
import org.savapage.core.i18n.NounEnum;
import org.savapage.core.i18n.PrepositionEnum;
import org.savapage.core.reports.AbstractJrDataSource;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.util.BigDecimalUtil;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class UserPrintOutTotalsDataSource extends AbstractJrDataSource
        implements JRDataSource {

    private static final int CHUNK_SIZE = 200;

    private Iterator<UserPrintOutTotalDto> iterator;

    private UserPrintOutTotalDto chunkDtoWlk = null;

    private int counter = 1;
    private int chunkCounter = CHUNK_SIZE;

    private final int balanceDecimals = ConfigManager.getUserBalanceDecimals();

    private final SimpleDateFormat dfMediumDatetime;

    private final UserPrintOutTotalsReq request;

    /**
     *
     * @param req
     * @param locale
     */
    public UserPrintOutTotalsDataSource(final UserPrintOutTotalsReq req,
            final Locale locale) {

        super(locale);

        this.request = req;

        this.dfMediumDatetime =
                new SimpleDateFormat("yyyy.MM.dd HH:mm:ss z", locale);

        this.counter = 0;
        this.chunkCounter = CHUNK_SIZE;
    }

    /**
     *
     * @return The {@link String} with the formatted selection parameters.
     */
    public String getSelectionInfo() {

        final DateFormat dfMediumDate =
                DateFormat.getDateInstance(DateFormat.MEDIUM, this.getLocale());

        final StringBuilder where = new StringBuilder();

        int nSelect = 0;

        final String aspectTxt = this.request.getAspect().uiText(getLocale());

        // Pages (sent|printed)
        where.append(
                UserPrintOutTotalsReq.Aspect.PAGES.uiText(this.getLocale()))
                .append(" (").append(this.request.getPages()
                        .uiText(this.getLocale()).toLowerCase())
                .append(")");

        // Aspect
        if (this.request.getAspect() != UserPrintOutTotalsReq.Aspect.PAGES) {
            where.append(", ")
                    .append(NounEnum.OPTION.uiText(this.getLocale(), true))
                    .append(" (").append(aspectTxt.toLowerCase()).append(")");
        }

        nSelect++;

        if (this.request.getTimeFrom() != null) {
            if (nSelect > 0) {
                where.append(", ");
            }
            nSelect++;
            where.append(PrepositionEnum.FROM_TIME.uiText(getLocale()))
                    .append(" ")
                    .append(dfMediumDate.format(this.request.getTimeFrom()));
        }

        if (this.request.getTimeTo() != null) {
            if (nSelect > 0) {
                where.append(", ");
            }
            nSelect++;
            where.append(PrepositionEnum.TO_TIME.uiText(getLocale()))
                    .append(" ")
                    .append(dfMediumDate.format(this.request.getTimeTo()));
        }

        if (this.request.getUserGroups() != null
                && !this.request.getUserGroups().isEmpty()) {
            if (nSelect > 0) {
                where.append(". ");
            }
            where.append(NounEnum.GROUP.uiText(getLocale(),
                    this.request.getUserGroups().size() > 1)).append(": ");

            nSelect = 0;

            for (final String group : this.request.getUserGroups()) {
                if (nSelect > 0) {
                    where.append(", ");
                }
                nSelect++;
                where.append(group);
            }
        }

        return where.toString();
    }

    /**
     * @param startPosition
     *            The zero-based start position of the chunk related to the
     *            total number of rows. If {@code null} the chunk starts with
     *            the first row.
     * @param maxResults
     *            The maximum number of rows in the chunk. If {@code null}, then
     *            ALL (remaining rows) are returned.
     */
    private void getNextChunk(final Integer startPosition,
            final Integer maxResults) {

        final List<UserPrintOutTotalDto> entryList = ServiceContext
                .getDaoContext().getAccountTrxDao().getUserPrintOutTotalsChunk(
                        this.request, startPosition, maxResults);

        this.chunkCounter = 0;
        this.iterator = entryList.iterator();
    }

    /**
     *
     * @param decimal
     *            The {@link BigDecimal}
     * @return The formatted {@link BigDecimal}.
     */
    private String formattedCurrency(final BigDecimal decimal) {
        try {
            return BigDecimalUtil.localize(decimal, this.balanceDecimals,
                    this.getLocale(), "", true);
        } catch (ParseException e) {
            return "?";
        }
    }

    @Override
    public Object getFieldValue(final JRField jrField) throws JRException {

        final UserPrintOutTotalDto.Detail detailDto;

        switch (this.request.getAspect()) {
        case COPIES:
            detailDto = this.chunkDtoWlk.getTotalCopies();
            break;
        case JOBS:
            detailDto = this.chunkDtoWlk.getTotalJobs();
            break;
        case PAGES:
            if (this.request
                    .getPages() == UserPrintOutTotalsReq.Pages.PRINTED) {
                detailDto = this.chunkDtoWlk.getTotalPagesPrinted();
            } else {
                detailDto = this.chunkDtoWlk.getTotalPagesSent();
            }
            break;
        case SHEETS:
            detailDto = this.chunkDtoWlk.getTotalSheets();
            break;
        default:
            throw new RuntimeException(String.format("%s not handled.",
                    this.request.getAspect().toString()));
        }

        final StringBuilder value = new StringBuilder(256);

        switch (jrField.getName()) {

        case "USER_ID":
            value.append(this.chunkDtoWlk.getUserId());
            break;

        case "USER_NAME":
            value.append(this.chunkDtoWlk.getUserName());
            break;

        case "KLAS":
            value.append(this.chunkDtoWlk.getUserGroup());
            break;

        case "AMOUNT":
            value.append(this.formattedCurrency(this.chunkDtoWlk.getAmount()));
            break;

        case "JOBS":
            value.append(this.chunkDtoWlk.getTotalJobs().getTotal());
            break;

        case "COPIES":
            value.append(this.chunkDtoWlk.getTotalCopies().getTotal());
            break;

        case "PAGES":
            if (this.request
                    .getPages() == UserPrintOutTotalsReq.Pages.PRINTED) {
                value.append(
                        this.chunkDtoWlk.getTotalPagesPrinted().getTotal());
            } else {
                value.append(this.chunkDtoWlk.getTotalPagesSent().getTotal());
            }
            break;

        case "PAGES_A4":
            value.append(detailDto.getA4());
            break;

        case "PAGES_A3":
            value.append(detailDto.getA3());
            break;

        case "PAGES_1_SIDED":
            value.append(detailDto.getSimplex());
            break;

        case "PAGES_2_SIDED":
            value.append(detailDto.getDuplex());
            break;

        case "PAGES_BW":
            value.append(detailDto.getGrayscale());
            break;

        case "PAGES_COLOR":
            value.append(detailDto.getColor());
            break;

        case "DATE_FROM":
            value.append(this.dfMediumDatetime
                    .format(this.chunkDtoWlk.getDateFrom()));
            break;

        case "DATE_TO":
            value.append(
                    this.dfMediumDatetime.format(this.chunkDtoWlk.getDateTo()));
            break;

        default:
            // nop
            break;
        }

        return value.toString();
    }

    @Override
    public boolean next() throws JRException {

        if (this.chunkCounter == CHUNK_SIZE) {
            getNextChunk(this.counter, CHUNK_SIZE);
        }

        if (!this.iterator.hasNext()) {
            return false;
        }

        this.chunkDtoWlk = this.iterator.next();

        this.counter++;
        this.chunkCounter++;

        return true;
    }

}
