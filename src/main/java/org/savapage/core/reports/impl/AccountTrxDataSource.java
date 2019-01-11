/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
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
package org.savapage.core.reports.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.savapage.core.SpException;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.dao.AccountDao;
import org.savapage.core.dao.AccountTrxDao;
import org.savapage.core.dao.UserDao;
import org.savapage.core.dao.enums.AccountTrxTypeEnum;
import org.savapage.core.dao.enums.PrintModeEnum;
import org.savapage.core.dao.helpers.AccountTrxPagerReq;
import org.savapage.core.i18n.JobTicketNounEnum;
import org.savapage.core.i18n.PrintOutNounEnum;
import org.savapage.core.i18n.PrintOutVerbEnum;
import org.savapage.core.ipp.attribute.IppDictJobTemplateAttr;
import org.savapage.core.ipp.helpers.IppOptionMap;
import org.savapage.core.jpa.Account;
import org.savapage.core.jpa.Account.AccountTypeEnum;
import org.savapage.core.jpa.AccountTrx;
import org.savapage.core.jpa.DocLog;
import org.savapage.core.jpa.PosPurchase;
import org.savapage.core.jpa.PrintOut;
import org.savapage.core.jpa.User;
import org.savapage.core.reports.AbstractJrDataSource;
import org.savapage.core.services.AccountingService;
import org.savapage.core.services.ProxyPrintService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.util.BigDecimalUtil;
import org.savapage.core.util.BitcoinUtil;
import org.savapage.core.util.CurrencyUtil;
import org.savapage.core.util.JsonHelper;
import org.savapage.core.util.LocaleHelper;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class AccountTrxDataSource extends AbstractJrDataSource
        implements JRDataSource {

    private static final String BULL_SEP = " • ";

    private static final int CHUNK_SIZE = 100;

    /** */
    private static final ProxyPrintService PROXYPRINT_SERVICE =
            ServiceContext.getServiceFactory().getProxyPrintService();

    /** */
    private static final AccountingService ACCOUNTING_SERVICE =
            ServiceContext.getServiceFactory().getAccountingService();

    /** */
    private List<AccountTrx> entryList = null;
    private Iterator<AccountTrx> iterator;

    private AccountTrx accountTrxWlk = null;

    private int counter = 1;
    private int chunkCounter = CHUNK_SIZE;

    private final AccountTrxDao.Field sortField;
    private final Boolean sortAscending;
    private final AccountTrxDao.ListFilter filter;

    private final int balanceDecimals = ConfigManager.getUserBalanceDecimals();

    private final SimpleDateFormat dfMediumDatetime;

    private final User user;
    private final Account account;

    final boolean showDocLogTitle;

    final LocaleHelper localeHelper;

    /**
     *
     * @param req
     * @param locale
     */
    public AccountTrxDataSource(final AccountTrxPagerReq req,
            final Locale locale) {

        super(locale);

        this.localeHelper = new LocaleHelper(locale);

        this.dfMediumDatetime =
                new SimpleDateFormat("yyyy.MM.dd HH:mm:ss z", locale);

        this.sortField = req.getSort().getField();
        this.sortAscending = req.getSort().getAscending();

        this.filter = new AccountTrxDao.ListFilter();

        //
        this.filter.setTrxType(req.getSelect().getTrxType());

        this.filter.setUserId(req.getSelect().getUserId());
        this.filter.setAccountId(req.getSelect().getAccountId());

        Long time = req.getSelect().getDateFrom();
        if (time != null) {
            this.filter.setDateFrom(new Date(time));
        }

        time = req.getSelect().getDateTo();
        if (time != null) {
            this.filter.setDateTo(new Date(time));
        }

        this.filter
                .setContainingCommentText(req.getSelect().getContainingText());

        //
        this.counter = 0;
        this.chunkCounter = CHUNK_SIZE;

        //
        this.showDocLogTitle = ConfigManager.instance()
                .isConfigValue(Key.WEBAPP_DOCLOG_SHOW_DOC_TITLE);
        //
        if (req.getSelect().getUserId() != null) {

            this.filter.setAccountType(AccountTypeEnum.USER);

            final UserDao dao = ServiceContext.getDaoContext().getUserDao();
            this.user = dao.findById(req.getSelect().getUserId());
            this.account = null;
        } else if (req.getSelect().getAccountId() != null) {
            this.user = null;
            final AccountDao dao =
                    ServiceContext.getDaoContext().getAccountDao();
            this.account = dao.findById(req.getSelect().getAccountId());
        } else {
            this.user = null;
            this.account = null;
        }

    }

    /**
     *
     * @return The {@link String} with the formatted selection parameters.
     */
    @SuppressWarnings("unused")
    public String getSelectionInfo() {

        final DateFormat dfMediumDate =
                DateFormat.getDateInstance(DateFormat.MEDIUM, this.getLocale());

        final StringBuilder where = new StringBuilder();

        int nSelect = 0;

        // User
        if (this.user != null && this.user.getUserId() != null) {
            if (nSelect > 0) {
                where.append(", ");
            }
            nSelect++;
            where.append(localized("accounttrxlist-sel-userid",
                    this.user.getUserId()));
        }

        if (this.user != null && this.user.getFullName() != null) {
            if (nSelect > 0) {
                where.append(", ");
            }
            nSelect++;
            where.append(this.user.getFullName());
        }

        // Account
        if (this.account != null && this.account.getName() != null) {
            if (nSelect > 0) {
                where.append(", ");
            }
            nSelect++;

            final Account parent = this.account.getParent();
            final String accountDisplay;
            if (parent != null) {
                accountDisplay = String.format("%s / %s", parent.getName(),
                        this.account.getName());
            } else {
                accountDisplay = this.account.getName();
            }
            where.append(
                    localized("accounttrxlist-sel-account", accountDisplay));
        }

        // Not yet...
        if (false && filter.getAccountType() != null) {
            if (nSelect > 0) {
                where.append(", ");
            }
            nSelect++;
            where.append(localized("accounttrxlist-sel-accounttype",
                    filter.getAccountType().toString()));
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

        this.entryList = ServiceContext.getDaoContext().getAccountTrxDao()
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

        final StringBuilder value = new StringBuilder(256);

        switch (jrField.getName()) {

        case "TRX_DATE":
            value.append(this.dfMediumDatetime
                    .format(this.accountTrxWlk.getTransactionDate()));
            break;

        case "TRX_TYPE":
            value.append(this.accountTrxWlk.getTrxType());
            break;

        case "CURRENCY":
            value.append(
                    StringUtils.defaultString(accountTrxWlk.getCurrencyCode()));
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
                value.append(
                        StringUtils.defaultString(accountTrxWlk.getComment()));
            } else if (this.showDocLogTitle) {
                value.append(StringUtils.defaultString(docLog.getTitle()));
            }

            final PrintOut printOut;

            if (docLog == null || docLog.getDocOut() == null) {
                printOut = null;
            } else {
                printOut = docLog.getDocOut().getPrintOut();
            }

            if (printOut != null) {
                this.appendDescription(value, docLog, printOut,
                        this.accountTrxWlk);
            }

            if (posPurchase != null) {
                value.append(" (")
                        .append(StringUtils
                                .defaultString(posPurchase.getPaymentType()))
                        .append(')');
            }

            if (trxType == AccountTrxTypeEnum.GATEWAY
                    && this.accountTrxWlk.getExtAmount() != null) {

                final boolean isExtBitcoin =
                        this.accountTrxWlk.getExtCurrencyCode()
                                .equals(CurrencyUtil.CURRENCY_CODE_BITCOIN);

                final int decimalsWrk;

                if (isExtBitcoin) {
                    decimalsWrk = BitcoinUtil.BTC_DECIMALS;
                } else {
                    decimalsWrk = balanceDecimals;
                }

                if (value.length() > 0) {
                    value.append(BULL_SEP);
                }

                value.append(this.accountTrxWlk.getExtCurrencyCode())
                        .append(" ");

                try {
                    value.append(BigDecimalUtil.localize(
                            this.accountTrxWlk.getExtAmount(), decimalsWrk,
                            this.getLocale(), "", true));

                    if (this.accountTrxWlk.getExtFee() != null
                            && this.accountTrxWlk.getExtFee()
                                    .compareTo(BigDecimal.ZERO) != 0) {

                        value.append("-/-");

                        value.append(BigDecimalUtil.localize(
                                this.accountTrxWlk.getExtFee(), decimalsWrk,
                                this.getLocale(), "", true));
                    }

                } catch (ParseException e) {
                    throw new SpException(e);
                }

                if (StringUtils
                        .isNotBlank(this.accountTrxWlk.getExtMethodAddress())) {
                    value.append(BULL_SEP)
                            .append(this.accountTrxWlk.getExtMethodAddress());
                }

                if (StringUtils
                        .isNotBlank(this.accountTrxWlk.getExtDetails())) {
                    value.append(BULL_SEP)
                            .append(this.accountTrxWlk.getExtDetails());
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

    /**
     * Appends PrintOut description.
     *
     * @param desc
     *            The {@link StringBuilder} to append to.
     * @param docLog
     *            The {@link DocLog} container.
     * @param printOut
     *            The {@link PrintOut}.
     * @param trx
     *            The {@link AccountTrx}.
     */
    private void appendDescription(final StringBuilder desc,
            final DocLog docLog, final PrintOut printOut,
            final AccountTrx trx) {

        final Locale locale = getLocale();

        //
        final BigDecimal costPerCopy =
                ACCOUNTING_SERVICE.calcCostPerPrintedCopy(
                        docLog.getCostOriginal(), printOut.getNumberOfCopies());

        final BigDecimal printedCopies;

        if (costPerCopy.compareTo(BigDecimal.ZERO) == 0) {
            printedCopies = BigDecimal.ZERO;
        } else {
            printedCopies = ACCOUNTING_SERVICE
                    .calcPrintedCopies(trx.getAmount(), costPerCopy, 2).abs();
        }

        final int nCopies =
                printedCopies.setScale(0, RoundingMode.HALF_UP).intValue();

        desc.append(BULL_SEP)
                .append(this.localeHelper.asExactIntegerOrScaled(printedCopies))
                .append(" ")
                .append(PrintOutNounEnum.COPY.uiText(locale, nCopies > 1));

        //
        final int nSheets = nCopies * printOut.getNumberOfSheets().intValue()
                / printOut.getNumberOfCopies().intValue();

        desc.append(BULL_SEP).append(nSheets).append(" ")
                .append(PrintOutNounEnum.SHEET.uiText(locale, nSheets > 1));

        //
        desc.append(BULL_SEP).append(printOut.getPaperSize().toUpperCase());

        if (BooleanUtils.isTrue(printOut.getDuplex())) {
            desc.append(BULL_SEP)
                    .append(PrintOutNounEnum.DUPLEX.uiText(locale));
        } else {
            desc.append(BULL_SEP)
                    .append(PrintOutNounEnum.SIMPLEX.uiText(locale));
        }

        if (BooleanUtils.isTrue(printOut.getGrayscale())) {
            desc.append(BULL_SEP)
                    .append(PrintOutNounEnum.GRAYSCALE.uiText(locale));
        } else {
            desc.append(BULL_SEP).append(PrintOutNounEnum.COLOR.uiText(locale));
        }

        //
        final Map<String, String> ippOptions =
                JsonHelper.createStringMapOrNull(printOut.getIppOptions());

        if (ippOptions == null) {
            return;
        }

        final IppOptionMap optionMap = new IppOptionMap(ippOptions);

        if (optionMap.hasFinishingPunch()) {
            desc.append(BULL_SEP).append(PrintOutVerbEnum.PUNCH.uiText(locale));
        }
        if (optionMap.hasFinishingStaple()) {
            desc.append(BULL_SEP)
                    .append(PrintOutVerbEnum.STAPLE.uiText(locale));
        }
        if (optionMap.hasFinishingFold()) {
            desc.append(BULL_SEP).append(PrintOutVerbEnum.FOLD.uiText(locale));
        }
        if (optionMap.hasFinishingBooklet()) {
            desc.append(BULL_SEP)
                    .append(PrintOutNounEnum.BOOKLET.uiText(locale));
        }

        //
        final String[][] ippAttrArrays =
                { IppDictJobTemplateAttr.JOBTICKET_ATTR_MEDIA,
                        IppDictJobTemplateAttr.JOBTICKET_ATTR_COPY,
                        IppDictJobTemplateAttr.JOBTICKET_ATTR_FINISHINGS_EXT };

        for (final String[] array : ippAttrArrays) {
            String valueWrk = PROXYPRINT_SERVICE
                    .getJobTicketOptionsUiText(locale, array, optionMap);
            if (valueWrk != null) {
                desc.append(BULL_SEP).append(valueWrk);
            }
        }

        //
        final List<Pair<String, String>> extOpts = PROXYPRINT_SERVICE
                .getJobTicketOptionsExtUiText(locale, ippOptions);

        if (extOpts != null && !extOpts.isEmpty()) {
            for (final Pair<String, String> pair : extOpts) {
                desc.append(BULL_SEP).append(pair.getKey()).append(" ")
                        .append(pair.getValue());
            }
        }

        //
        if (StringUtils.isNotBlank(docLog.getExternalId())) {

            desc.append(BULL_SEP);

            final PrintModeEnum printOutMode = EnumUtils
                    .getEnum(PrintModeEnum.class, printOut.getPrintMode());

            if (printOutMode == PrintModeEnum.TICKET
                    || printOutMode == PrintModeEnum.TICKET_C
                    || printOutMode == PrintModeEnum.TICKET_E) {
                desc.append(printOutMode.uiText(locale));
            } else {
                desc.append(JobTicketNounEnum.TAG.uiText(locale));
            }
            desc.append(" ")
                    .append(StringUtils.defaultString(docLog.getExternalId()));
        }

    }
}
