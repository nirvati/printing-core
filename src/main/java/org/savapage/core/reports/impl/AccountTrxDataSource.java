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
package org.savapage.core.reports.impl;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;

import org.apache.commons.lang3.StringUtils;
import org.savapage.core.SpException;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.dao.AccountTrxDao;
import org.savapage.core.dao.UserDao;
import org.savapage.core.dao.helpers.AccountTrxPagerReq;
import org.savapage.core.dao.helpers.AccountTrxTypeEnum;
import org.savapage.core.jpa.Account.AccountTypeEnum;
import org.savapage.core.jpa.AccountTrx;
import org.savapage.core.jpa.DocLog;
import org.savapage.core.jpa.PosPurchase;
import org.savapage.core.jpa.User;
import org.savapage.core.reports.AbstractJrDataSource;
import org.savapage.core.services.AccountingService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.util.BigDecimalUtil;

/**
 *
 * @author Datraverse B.V.
 *
 */
public final class AccountTrxDataSource extends AbstractJrDataSource implements
        JRDataSource {

    private static final int CHUNK_SIZE = 100;

    private List<AccountTrx> entryList = null;
    private Iterator<AccountTrx> iterator;

    private AccountTrx accountTrxWlk = null;

    private int counter = 1;
    private int chunkCounter = CHUNK_SIZE;

    private final UserDao userDao;

    private final AccountTrxDao.Field sortField;
    private final Boolean sortAscending;
    private final AccountTrxDao.ListFilter filter;

    private final AccountingService accountingService;

    private final int balanceDecimals = ConfigManager.getUserBalanceDecimals();

    private final SimpleDateFormat dfMediumDatetime;

    private final User user;

    /**
     *
     * @param req
     * @param locale
     */
    public AccountTrxDataSource(final AccountTrxPagerReq req,
            final Locale locale) {

        super(locale);

        this.dfMediumDatetime =
                new SimpleDateFormat("yyyy.MM.dd HH:mm:ss z", locale);

        this.accountingService =
                ServiceContext.getServiceFactory().getAccountingService();

        this.sortField = req.getSort().getField();
        this.sortAscending = req.getSort().getAscending();

        this.filter = new AccountTrxDao.ListFilter();

        //
        this.filter.setTrxType(req.getSelect().getTrxType());
        this.filter.setUserId(req.getSelect().getUserId());
        this.filter.setAccountType(AccountTypeEnum.USER);

        Long time = req.getSelect().getDateFrom();
        if (time != null) {
            this.filter.setDateFrom(new Date(time));
        }

        time = req.getSelect().getDateTo();
        if (time != null) {
            this.filter.setDateTo(new Date(time));
        }

        this.filter.setContainingCommentText(req.getSelect()
                .getContainingText());

        //
        this.counter = 0;
        this.chunkCounter = CHUNK_SIZE;

        //
        this.userDao = ServiceContext.getDaoContext().getUserDao();

        this.user = this.userDao.findById(req.getSelect().getUserId());
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

        if (user.getUserId() != null) {
            if (nSelect > 0) {
                where.append(", ");
            }
            nSelect++;
            where.append(localized("accounttrxlist-sel-userid",
                    user.getUserId()));
        }

        if (user.getFullName() != null) {
            if (nSelect > 0) {
                where.append(", ");
            }
            nSelect++;
            where.append(user.getFullName());
        }

        // Not yet...
        if (false && filter.getAccountType() != null) {
            if (nSelect > 0) {
                where.append(", ");
            }
            nSelect++;
            where.append(localized("accounttrxlist-sel-accounttype", filter
                    .getAccountType().toString()));
        }

        if (filter.getContainingCommentText() != null) {
            if (nSelect > 0) {
                where.append(", ");
            }
            nSelect++;
            where.append(localized("accounttrxlist-sel-comment",
                    filter.getContainingCommentText()));
        }

        if (filter.getDateFrom() != null) {
            if (nSelect > 0) {
                where.append(", ");
            }
            nSelect++;
            where.append(localized("accounttrxlist-sel-date-from",
                    dfMediumDate.format(filter.getDateFrom())));
        }

        if (filter.getDateTo() != null) {
            if (nSelect > 0) {
                where.append(", ");
            }
            nSelect++;
            where.append(localized("accounttrxlist-sel-date-to",
                    dfMediumDate.format(filter.getDateTo())));
        }

        return where.toString();
    }

    /**
     *
     * @param startPosition
     * @param maxResults
     */
    private void getNextChunk(final Integer startPosition,
            final Integer maxResults) {

        this.entryList =
                ServiceContext
                        .getDaoContext()
                        .getAccountTrxDao()
                        .getListChunk(this.filter, startPosition, maxResults,
                                this.sortField, this.sortAscending);

        this.chunkCounter = 0;
        this.iterator = this.entryList.iterator();

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

        final DocLog docLog = this.accountTrxWlk.getDocLog();
        final PosPurchase posPurchase = this.accountTrxWlk.getPosPurchase();

        final AccountTrxTypeEnum trxType =
                AccountTrxTypeEnum.valueOf(this.accountTrxWlk.getTrxType());

        final StringBuilder value = new StringBuilder(128);

        switch (jrField.getName()) {

        case "TRX_DATE":
            value.append(this.dfMediumDatetime.format(this.accountTrxWlk
                    .getTransactionDate()));
            break;

        case "TRX_TYPE":
            value.append(this.accountTrxWlk.getTrxType());
            break;

        case "CURRENCY":
            value.append(StringUtils.defaultString(accountTrxWlk
                    .getCurrencyCode()));
            break;

        case "AMOUNT":
            value.append(this.formattedCurrency(accountTrxWlk.getAmount()));
            break;

        case "BALANCE":
            value.append(this.formattedCurrency(accountTrxWlk.getBalance()));
            break;

        case "PAGE_TOTAL":
            if (docLog != null) {
                value.append(docLog.getNumberOfPages().toString());
            }
            break;

        case "RECEIPT":
            if (posPurchase != null) {
                value.append(posPurchase.getReceiptNumber());
            }
            if (trxType == AccountTrxTypeEnum.GATEWAY) {
                value.append(this.accountTrxWlk.getExtMethod());
            }
            break;

        case "DESCRIPTION":

            if (docLog == null) {
                value.append(StringUtils.defaultString(accountTrxWlk
                        .getComment()));
            } else {
                value.append(StringUtils.defaultString(docLog.getTitle()));
            }

            if (posPurchase != null) {
                value.append(" (")
                        .append(StringUtils.defaultString(posPurchase
                                .getPaymentType())).append(')');
            }

            if (trxType == AccountTrxTypeEnum.GATEWAY
                    && this.accountTrxWlk.getExtAmount() != null) {

                try {
                    value.append(" • ");
                    value.append(this.accountTrxWlk.getExtCurrencyCode())
                            .append(" ");
                    value.append(BigDecimalUtil.localize(
                            this.accountTrxWlk.getExtAmount(), balanceDecimals,
                            this.getLocale(), "", true));

                    if (this.accountTrxWlk.getExtFee() != null) {

                        value.append(" -/- ");

                        value.append(BigDecimalUtil.localize(
                                this.accountTrxWlk.getExtFee(),
                                balanceDecimals, this.getLocale(), "", true));
                    }

                } catch (ParseException e) {
                    throw new SpException(e);
                }

                if (StringUtils.isNotBlank(this.accountTrxWlk
                        .getExtMethodAddress())) {
                    value.append(" • ").append(
                            this.accountTrxWlk.getExtMethodAddress());
                }

                if (StringUtils.isNotBlank(this.accountTrxWlk.getExtDetails())) {
                    value.append(" • ").append(
                            this.accountTrxWlk.getExtDetails());
                }

            }

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

        this.accountTrxWlk = this.iterator.next();

        this.counter++;
        this.chunkCounter++;

        return true;
    }
}
