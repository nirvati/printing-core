/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2015 Datraverse B.V.
 * Authors: Rijk Ravestein.
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
package org.savapage.core.services.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Currency;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.savapage.core.SpException;
import org.savapage.core.cometd.AdminPublisher;
import org.savapage.core.cometd.PubLevelEnum;
import org.savapage.core.cometd.PubTopicEnum;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.dao.AccountDao;
import org.savapage.core.dao.PosPurchaseDao.ReceiptNumberPrefixEnum;
import org.savapage.core.dao.PrinterDao;
import org.savapage.core.dao.UserDao;
import org.savapage.core.dao.helpers.AccountTrxTypeEnum;
import org.savapage.core.dao.helpers.AggregateResult;
import org.savapage.core.dao.helpers.DaoBatchCommitter;
import org.savapage.core.dto.AccountDisplayInfoDto;
import org.savapage.core.dto.AccountVoucherRedeemDto;
import org.savapage.core.dto.FinancialDisplayInfoDto;
import org.savapage.core.dto.IppMediaCostDto;
import org.savapage.core.dto.IppMediaSourceCostDto;
import org.savapage.core.dto.MediaCostDto;
import org.savapage.core.dto.MediaPageCostDto;
import org.savapage.core.dto.PosDepositDto;
import org.savapage.core.dto.PosDepositReceiptDto;
import org.savapage.core.dto.SharedAccountDisplayInfoDto;
import org.savapage.core.dto.UserAccountingDto;
import org.savapage.core.dto.UserCreditTransferDto;
import org.savapage.core.dto.UserPaymentGatewayDto;
import org.savapage.core.jpa.Account;
import org.savapage.core.jpa.Account.AccountTypeEnum;
import org.savapage.core.jpa.AccountTrx;
import org.savapage.core.jpa.AccountVoucher;
import org.savapage.core.jpa.DocLog;
import org.savapage.core.jpa.PosPurchase;
import org.savapage.core.jpa.Printer;
import org.savapage.core.jpa.User;
import org.savapage.core.jpa.UserAccount;
import org.savapage.core.jpa.UserGroup;
import org.savapage.core.json.rpc.AbstractJsonRpcMethodResponse;
import org.savapage.core.json.rpc.JsonRpcError.Code;
import org.savapage.core.json.rpc.JsonRpcMethodError;
import org.savapage.core.json.rpc.JsonRpcMethodResult;
import org.savapage.core.json.rpc.impl.ResultPosDeposit;
import org.savapage.core.msg.UserMsgIndicator;
import org.savapage.core.print.proxy.ProxyPrintException;
import org.savapage.core.print.proxy.ProxyPrintJobChunk;
import org.savapage.core.print.proxy.ProxyPrintJobChunkInfo;
import org.savapage.core.services.AccountingException;
import org.savapage.core.services.AccountingService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.helpers.AccountTrxInfo;
import org.savapage.core.services.helpers.AccountTrxInfoSet;
import org.savapage.core.services.helpers.ProxyPrintCostParms;
import org.savapage.core.util.BigDecimalUtil;
import org.savapage.core.util.Messages;

/**
 *
 * @author Datraverse B.V.
 *
 */
public final class AccountingServiceImpl extends AbstractService implements
        AccountingService {

    @Override
    public PrinterDao.CostMediaAttr getCostMediaAttr() {
        return printerDAO().getCostMediaAttr();
    }

    @Override
    public PrinterDao.CostMediaAttr getCostMediaAttr(final String ippMediaName) {
        return printerDAO().getCostMediaAttr(ippMediaName);
    }

    @Override
    public PrinterDao.MediaSourceAttr getMediaSourceAttr(
            final String ippMediaSourceName) {

        return printerDAO().getMediaSourceAttr(ippMediaSourceName);
    }

    @Override
    public UserAccount getActiveUserAccount(final String userId,
            final Account.AccountTypeEnum accountType) {

        return userAccountDAO().findByActiveUserId(userId, accountType);
    }

    /**
     * Checks if account is a shared account, if not an {@link SpException} is
     * thrown.
     *
     * @param account
     *            The account to check.
     */
    private void checkSharedAccountType(final Account account) {

        final AccountTypeEnum accountType =
                AccountTypeEnum.valueOf(account.getAccountType());

        if (accountType != AccountTypeEnum.SHARED) {
            throw new SpException(String.format(
                    "AccountType [%s] expected: found [%s]",
                    AccountTypeEnum.SHARED.toString(), accountType.toString()));
        }
    }

    @Override
    public Account lazyGetSharedAccount(final String accountName,
            final Account accountTemplate) {

        checkSharedAccountType(accountTemplate);

        final Account parent = accountTemplate.getParent();
        Account account;

        if (parent == null) {
            account = accountDAO().findActiveSharedAccountByName(accountName);
        } else {
            account =
                    accountDAO().findActiveSharedChildAccountByName(
                            parent.getId(), accountName);
        }

        if (account == null) {
            account =
                    accountDAO().createFromTemplate(accountName,
                            accountTemplate);
        }
        return account;
    }

    @Override
    public UserAccount lazyGetUserAccount(final User user,
            final Account.AccountTypeEnum accountType) {

        UserAccount userAccount =
                this.getActiveUserAccount(user.getUserId(), accountType);

        if (userAccount == null) {

            final UserGroup userGroup;

            if (accountType == AccountTypeEnum.USER) {
                if (user.getInternal()) {
                    userGroup = userGroupService().getInternalUserGroup();
                } else {
                    userGroup = userGroupService().getExternalUserGroup();
                }
            } else {
                userGroup = null;
            }

            userAccount = createUserAccount(user, userGroup);
        }
        return userAccount;
    }

    @Override
    public UserAccountingDto getUserAccounting(final User user) {

        UserAccountingDto dto = new UserAccountingDto();

        dto.setLocale(ServiceContext.getLocale().toLanguageTag());

        Account account =
                lazyGetUserAccount(user, AccountTypeEnum.USER).getAccount();

        try {
            dto.setBalance(BigDecimalUtil.localize(account.getBalance(),
                    ConfigManager.getUserBalanceDecimals(),
                    ServiceContext.getLocale(), true));

            UserAccountingDto.CreditLimitEnum creditLimit;

            if (account.getRestricted()) {
                if (account.getUseGlobalOverdraft()) {
                    creditLimit = UserAccountingDto.CreditLimitEnum.DEFAULT;
                } else {
                    creditLimit = UserAccountingDto.CreditLimitEnum.INDIVIDUAL;
                }
            } else {
                creditLimit = UserAccountingDto.CreditLimitEnum.NONE;
            }

            dto.setCreditLimit(creditLimit);
            dto.setCreditLimitAmount(BigDecimalUtil.localize(
                    account.getOverdraft(),
                    ConfigManager.getUserBalanceDecimals(),
                    ServiceContext.getLocale(), true));

        } catch (ParseException e) {
            throw new SpException(e);
        }
        return dto;
    }

    @Override
    public AbstractJsonRpcMethodResponse setUserAccounting(final User user,
            final UserAccountingDto dto) {

        final Locale dtoLocale;

        if (dto.getLocale() != null) {
            dtoLocale = Locale.forLanguageTag(dto.getLocale());
        } else {
            dtoLocale = ServiceContext.getLocale();
        }

        final AccountTypeEnum accountType = AccountTypeEnum.USER;

        final UserAccount userAccount =
                this.getActiveUserAccount(user.getUserId(), accountType);
        final boolean isNewAccount = userAccount == null;

        final Account account;

        if (isNewAccount) {
            account = lazyGetUserAccount(user, accountType).getAccount();
        } else {
            account = userAccount.getAccount();
        }

        /*
         * Change balance?
         */
        if ((dto.getBalance() != null)
                && (isNewAccount || !dto.getKeepBalance().booleanValue())) {

            final String amount = dto.getBalance();

            try {

                final BigDecimal balanceNew =
                        BigDecimalUtil.parse(amount, dtoLocale, false, false);

                final BigDecimal balanceDiff =
                        balanceNew.subtract(account.getBalance());

                if (balanceDiff.compareTo(BigDecimal.ZERO) != 0) {

                    String comment = dto.getComment();
                    if (StringUtils.isBlank(comment)) {
                        comment = "";
                    }

                    final AccountTrx trx =
                            createAccountTrx(account,
                                    AccountTrxTypeEnum.ADJUST, balanceDiff,
                                    balanceNew, comment);

                    accountTrxDAO().create(trx);

                    account.setBalance(balanceNew);
                }

            } catch (ParseException e) {
                return createError("msg-amount-error", amount);
            }
        }

        final UserAccountingDto.CreditLimitEnum creditLimit =
                dto.getCreditLimit();

        if (creditLimit != null) {

            account.setRestricted(creditLimit != UserAccountingDto.CreditLimitEnum.NONE);
            account.setUseGlobalOverdraft(creditLimit == UserAccountingDto.CreditLimitEnum.DEFAULT);

            if (creditLimit == UserAccountingDto.CreditLimitEnum.INDIVIDUAL) {
                final String amount = dto.getCreditLimitAmount();
                try {
                    account.setOverdraft(BigDecimalUtil.parse(amount,
                            dtoLocale, false, false));
                } catch (ParseException e) {
                    return createError("msg-amount-error", amount);
                }
            }
        }

        account.setModifiedBy(ServiceContext.getActor());
        account.setModifiedDate(ServiceContext.getTransactionDate());

        accountDAO().update(account);

        return JsonRpcMethodResult.createOkResult();
    }

    /**
     * Creates an {@link Account} of type {@link Account.AccountTypeEnum#USER}
     * for a {@link User} (including the related {@link UserAccount}).
     *
     * @param user
     *            The {@link User} as owner of the account.
     * @param userGroupTemplate
     *            The {@link UserGroup} to be used as template. Is {@code null}
     *            when NO template is available.
     * @return The {@link UserAccount} created.
     */
    private UserAccount createUserAccount(final User user,
            final UserGroup userGroupTemplate) {

        final String actor = ServiceContext.getActor();
        final Date trxDate = ServiceContext.getTransactionDate();

        //
        final Account account = new Account();

        if (userGroupTemplate != null
                && userGroupTemplate.getInitialSettingsEnabled()) {

            account.setBalance(userGroupTemplate.getInitialCredit());
            account.setOverdraft(userGroupTemplate.getInitialOverdraft());
            account.setRestricted(userGroupTemplate.getInitiallyRestricted());
            account.setUseGlobalOverdraft(userGroupTemplate
                    .getInitialUseGlobalOverdraft());

        } else {
            account.setBalance(BigDecimal.ZERO);
            account.setOverdraft(BigDecimal.ZERO);
            account.setRestricted(true);
            account.setUseGlobalOverdraft(false);
        }

        account.setAccountType(Account.AccountTypeEnum.USER.toString());
        account.setComments(Account.CommentsEnum.COMMENT_OPTIONAL.toString());
        account.setInvoicing(Account.InvoicingEnum.USER_CHOICE_ON.toString());
        account.setDeleted(false);
        account.setDisabled(false);
        account.setName(user.getUserId());
        account.setNameLower(user.getUserId().toLowerCase());

        account.setCreatedBy(actor);
        account.setCreatedDate(trxDate);

        accountDAO().create(account);

        //
        final UserAccount userAccount = new UserAccount();

        userAccount.setAccount(account);
        userAccount.setUser(user);

        userAccount.setCreatedBy(actor);
        userAccount.setCreatedDate(trxDate);

        userAccountDAO().create(userAccount);

        //
        return userAccount;
    }

    /**
     * Creates a {@link AccountTrx} object with base Application Currency.
     * <p>
     * Transaction Actor and Date are retrieved from the {@link ServiceContext}.
     * </p>
     *
     * @param account
     *            The {@link Account} the transaction is executed on.
     * @param trxType
     *            The {@link AccountTrxTypeEnum}.
     * @param amount
     *            The transaction amount.
     * @param accountBalance
     *            The balance of the account AFTER this transaction.
     * @param comment
     *            The transaction comment.
     * @return The created {@link AccountTrx} object.
     */
    private AccountTrx createAccountTrx(final Account account,
            final AccountTrxTypeEnum trxType, final BigDecimal amount,
            final BigDecimal accountBalance, final String comment) {

        return createAccountTrx(account, trxType, ConfigManager
                .getAppCurrency().getCurrencyCode(), amount, accountBalance,
                comment);
    }

    /**
     * Creates a {@link AccountTrx} object.
     * <p>
     * Transaction Actor and Date are retrieved from the {@link ServiceContext}.
     * </p>
     *
     * @param account
     *            The {@link Account} the transaction is executed on.
     * @param trxType
     *            The {@link AccountTrxTypeEnum}.
     * @param currencyCode
     *            The ISO currency code.
     * @param amount
     *            The transaction amount.
     * @param accountBalance
     *            The balance of the account AFTER this transaction.
     * @param comment
     *            The transaction comment.
     * @return The created {@link AccountTrx} object.
     */
    private AccountTrx createAccountTrx(final Account account,
            final AccountTrxTypeEnum trxType, final String currencyCode,
            final BigDecimal amount, final BigDecimal accountBalance,
            final String comment) {

        final AccountTrx trx = new AccountTrx();

        trx.setAccount(account);

        trx.setCurrencyCode(currencyCode);
        trx.setAmount(amount);
        trx.setBalance(accountBalance);
        trx.setComment(comment);
        trx.setIsCredit(amount.compareTo(BigDecimal.ZERO) > 0);

        trx.setTrxType(trxType.toString());

        trx.setTransactionWeight(Integer.valueOf(1));

        trx.setTransactedBy(ServiceContext.getActor());
        trx.setTransactionDate(ServiceContext.getTransactionDate());

        return trx;
    }

    @Override
    public void createAccountTrx(final Account account, final DocLog docLog,
            final AccountTrxTypeEnum trxType) {
        createAccountTrx(account, docLog, trxType, 1, 1, null);
    }

    @Override
    public void createAccountTrxs(final AccountTrxInfoSet accountTrxInfoSet,
            final DocLog docLog, final AccountTrxTypeEnum trxType) {

        final int nTotalWeight = accountTrxInfoSet.calcTotalWeight();

        for (final AccountTrxInfo trxInfo : accountTrxInfoSet
                .getAccountTrxInfoList()) {

            createAccountTrx(trxInfo.getAccount(), docLog, trxType,
                    nTotalWeight, trxInfo.getWeight(), trxInfo.getExtDetails());
        }
    }

    /**
     * Calculates the weighted amount.
     *
     * @param amount
     *            The amount to weigh.
     * @param weightTotal
     *            The total of all weights.
     * @param weight
     *            The mathematical weight of the transaction in the context of a
     *            transaction set.
     * @param scale
     *            The scale (precision).
     * @return The weighted amount.
     */
    @Override
    public BigDecimal calcWeightedAmount(final BigDecimal amount,
            final int weightTotal, final int weight, final int scale) {
        return amount.multiply(BigDecimal.valueOf(weight)).divide(
                BigDecimal.valueOf(weightTotal), scale, RoundingMode.HALF_EVEN);
    }

    /**
     * Creates an {@link AccountTrx} of {@link AccountTrx.AccountTrxTypeEnum},
     * updates the {@link Account} and adds the {@link AccountTrx} to the
     * {@link DocLog}.
     *
     * @param account
     *            The {@link Account} to update.
     * @param docLog
     *            The {@link DocLog} to be accounted for.
     * @param trxType
     *            The {@link AccountTrxTypeEnum} of the {@link AccountTrx}.
     * @param weightTotal
     *            The total of all weights.
     * @param weight
     *            The mathematical weight of the transaction in the context of a
     *            transaction set.
     * @param extDetails
     *            Free format details from external source.
     */
    private void createAccountTrx(final Account account, final DocLog docLog,
            final AccountTrxTypeEnum trxType, final int weightTotal,
            final int weight, final String extDetails) {

        final String actor = ServiceContext.getActor();
        final Date trxDate = ServiceContext.getTransactionDate();

        /*
         * Weighted amount.
         */
        BigDecimal trxAmount = docLog.getCostOriginal().negate();

        if (weight != weightTotal) {
            trxAmount =
                    calcWeightedAmount(trxAmount, weightTotal, weight,
                            ConfigManager.getFinancialDecimalsInDatabase());
        }

        /*
         * Update account.
         */
        account.setBalance(account.getBalance().add(trxAmount));

        account.setModifiedBy(actor);
        account.setModifiedDate(trxDate);

        accountDAO().update(account);

        /*
         * Create transaction
         */
        final AccountTrx trx = new AccountTrx();

        trx.setAccount(account);
        trx.setDocLog(docLog);

        trx.setCurrencyCode(ConfigManager.getAppCurrency().getCurrencyCode());
        trx.setAmount(trxAmount);
        trx.setBalance(account.getBalance());
        trx.setComment("");
        trx.setIsCredit(false);

        trx.setTrxType(trxType.toString());

        trx.setTransactionWeight(weight);

        trx.setTransactedBy(ServiceContext.getActor());
        trx.setTransactionDate(ServiceContext.getTransactionDate());

        trx.setExtDetails(extDetails);

        accountTrxDAO().create(trx);

        /*
         * Add transaction to the DocLog transaction list.
         */
        if (docLog.getTransactions() == null) {
            docLog.setTransactions(new ArrayList<AccountTrx>());
        }
        docLog.getTransactions().add(trx);
    }

    /**
     * Calculates the cost of a priont job.
     * <p>
     * The pageCostTwoSided is applied to pages that are print on both sides of
     * a sheet. If a job has an odd number of pages, the pageCostTwoSided is not
     * applied to the last page. For example, if a 3 page document is printed as
     * duplex, the pageCostTwoSided is applied to the first 2 pages: the last
     * page has pageCostOneSided.
     * </p>
     *
     * @param nPages
     *            The number of pages.
     * @param nPagesPerSide
     *            the number of pages per side (n-up).
     * @param nCopies
     *            the number of copies.
     * @param duplex
     *            {@code true} if a duplex print job.
     * @param pageCostOneSided
     *            Cost per page when single-sided.
     * @param pageCostTwoSided
     *            Cost per page when double-sided.
     * @param discountPerc
     *            The discount percentage. 10% is passed as 0.10
     * @return The {@link BigDecimal}.
     */
    public static BigDecimal calcPrintJobCost(final int nPages,
            final int nPagesPerSide, final int nCopies, final boolean duplex,
            final BigDecimal pageCostOneSided,
            final BigDecimal pageCostTwoSided, final BigDecimal discountPerc) {

        final BigDecimal copies = BigDecimal.valueOf(nCopies);

        BigDecimal pagesOneSided = BigDecimal.ZERO;
        BigDecimal pagesTwoSided = BigDecimal.ZERO;

        int nSides = nPages / nPagesPerSide;

        if (nPages % nPagesPerSide > 0) {
            nSides++;
        }

        if (duplex) {
            pagesTwoSided = new BigDecimal((nSides / 2) * 2);
            pagesOneSided = new BigDecimal(nSides % 2);
        } else {
            pagesOneSided = new BigDecimal(nSides);
        }

        return pageCostOneSided.multiply(pagesOneSided).multiply(copies)
                .add(pageCostTwoSided.multiply(pagesTwoSided).multiply(copies))
                .multiply(BigDecimal.ONE.subtract(discountPerc));
    }

    /**
     *
     * @param cost
     * @param grayscale
     * @return The {@link BigDecimal}.
     */
    private BigDecimal getCost(final MediaPageCostDto cost,
            final boolean grayscale) {

        String strCost = null;

        if (grayscale) {
            strCost = cost.getCostGrayscale();
        } else {
            strCost = cost.getCostColor();
        }

        return new BigDecimal(strCost);
    }

    @Override
    public boolean isBalanceSufficient(final Account account,
            final BigDecimal cost) {

        boolean isChargeable = true;

        if (account.getRestricted()) {

            final BigDecimal creditLimit;

            if (account.getUseGlobalOverdraft()) {
                creditLimit =
                        ConfigManager.instance().getConfigBigDecimal(
                                Key.FINANCIAL_GLOBAL_CREDIT_LIMIT);
            } else {
                creditLimit = account.getOverdraft();
            }

            final BigDecimal balanceAfter = account.getBalance().subtract(cost);

            isChargeable = balanceAfter.compareTo(creditLimit.negate()) >= 0;
        }
        return isChargeable;
    }

    @Override
    public BigDecimal calcProxyPrintCost(final Printer printer,
            final ProxyPrintCostParms costParms) {

        BigDecimal pageCostOneSided = BigDecimal.ZERO;
        BigDecimal pageCostTwoSided = BigDecimal.ZERO;

        final IppMediaSourceCostDto mediaSourceCost =
                costParms.getMediaSourceCost();

        if (mediaSourceCost != null && !mediaSourceCost.isManualSource()) {

            final MediaCostDto pageCost =
                    mediaSourceCost.getMedia().getPageCost();

            pageCostOneSided =
                    this.getCost(pageCost.getCostOneSided(),
                            costParms.isGrayscale());

            pageCostTwoSided =
                    this.getCost(pageCost.getCostTwoSided(),
                            costParms.isGrayscale());

        } else {

            switch (printerDAO().getChargeType(printer.getChargeType())) {

            case SIMPLE:

                pageCostOneSided = printer.getDefaultCost();
                pageCostTwoSided = pageCostOneSided;
                break;

            case MEDIA:

                final IppMediaCostDto costDto =
                        printerDAO().getMediaCost(printer,
                                costParms.getIppMediaOption());

                if (costDto != null) {

                    final MediaCostDto pageCost = costDto.getPageCost();

                    pageCostOneSided =
                            this.getCost(pageCost.getCostOneSided(),
                                    costParms.isGrayscale());

                    pageCostTwoSided =
                            this.getCost(pageCost.getCostTwoSided(),
                                    costParms.isGrayscale());
                }
                break;

            default:
                throw new SpException("Charge type [" + printer.getChargeType()
                        + "] not supported");
            }
        }

        final BigDecimal discountPerc;

        if (costParms.isEcoPrint()) {
            discountPerc =
                    BigDecimal.valueOf(
                            ConfigManager.instance().getConfigLong(
                                    Key.ECO_PRINT_DISCOUNT_PERC, 0L)).divide(
                            BigDecimal.valueOf(100L));
        } else {
            discountPerc = BigDecimal.ZERO;
        }

        return calcPrintJobCost(costParms.getNumberOfPages(),
                costParms.getPagesPerSide(), costParms.getNumberOfCopies(),
                costParms.isDuplex(), pageCostOneSided, pageCostTwoSided,
                discountPerc);
    }

    /**
     * Gets the localized string for a BigDecimal.
     *
     * @param decimal
     *            The {@link BigDecimal}.
     * @param locale
     *            The {@link Locale}.
     * @param currencySymbol
     *            {@code null} when not available.
     * @return The localized string.
     * @throws ParseException
     */
    private String localizedPrinterCost(final BigDecimal decimal,
            final Locale locale, final String currencySymbol) {

        BigDecimal value = decimal;

        if (value == null) {
            value = BigDecimal.ZERO;
        }

        String cost = null;

        try {
            cost =
                    BigDecimalUtil.localize(value,
                            ConfigManager.getPrinterCostDecimals(), locale,
                            true);
        } catch (ParseException e) {
            throw new SpException(e.getMessage());
        }

        if (StringUtils.isBlank(currencySymbol)) {
            return cost;
        }
        return currencySymbol + cost;
    }

    @Override
    public void validateProxyPrintUserCost(final User user,
            final BigDecimal cost, final Locale locale,
            final String currencySymbol) throws ProxyPrintException {
        /*
         * INVARIANT: User is NOT allowed to print when resulting balance is
         * below credit limit.
         */
        final Account account =
                this.lazyGetUserAccount(user, AccountTypeEnum.USER)
                        .getAccount();

        final BigDecimal balanceBefore = account.getBalance();
        final BigDecimal balanceAfter = balanceBefore.subtract(cost);

        if (account.getRestricted()) {

            final BigDecimal creditLimit;

            if (account.getUseGlobalOverdraft()) {
                creditLimit =
                        ConfigManager.instance().getConfigBigDecimal(
                                Key.FINANCIAL_GLOBAL_CREDIT_LIMIT);
            } else {
                creditLimit = account.getOverdraft();
            }

            if (balanceAfter.compareTo(creditLimit.negate()) < 0) {

                if (creditLimit.compareTo(BigDecimal.ZERO) == 0) {

                    throw new ProxyPrintException(localize(
                            "msg-print-denied-no-balance",
                            localizedPrinterCost(balanceBefore, locale,
                                    currencySymbol),
                            localizedPrinterCost(cost, locale, currencySymbol)));

                } else {
                    throw new ProxyPrintException(localize(
                            "msg-print-denied-no-credit",
                            localizedPrinterCost(creditLimit, locale,
                                    currencySymbol),
                            localizedPrinterCost(cost, locale, currencySymbol),
                            localizedPrinterCost(balanceBefore, locale,
                                    currencySymbol)));
                }
            }
        }
    }

    @Override
    public BigDecimal calcProxyPrintCost(final Locale locale,
            final String currencySymbol, final User user,
            final Printer printer, final ProxyPrintCostParms costParms,
            final ProxyPrintJobChunkInfo jobChunkInfo)
            throws ProxyPrintException {

        BigDecimal totalCost = BigDecimal.ZERO;

        /*
         * Traverse the chunks and calculate.
         */
        for (final ProxyPrintJobChunk chunk : jobChunkInfo.getChunks()) {

            costParms.setNumberOfPages(chunk.getNumberOfPages());
            costParms.setIppMediaOption(chunk.getAssignedMedia()
                    .getIppKeyword());
            costParms.setMediaSourceCost(chunk.getAssignedMediaSource());

            final BigDecimal chunkCost =
                    this.calcProxyPrintCost(printer, costParms);

            chunk.setCost(chunkCost);

            totalCost = totalCost.add(chunkCost);
        }

        validateProxyPrintUserCost(user, totalCost, locale, currencySymbol);

        return totalCost;
    }

    @Override
    public String getFormattedUserBalance(final User user, final Locale locale,
            final String currencySymbol) {

        final String balance;

        if (user.getDeleted()) {

            balance =
                    formatUserBalance(
                            userAccountDAO().findByUserId(user.getId(),
                                    AccountTypeEnum.USER), locale,
                            currencySymbol);
        } else {
            balance =
                    getFormattedUserBalance(user.getUserId(), locale,
                            currencySymbol);
        }
        return balance;
    }

    @Override
    public String getFormattedUserBalance(final String userId,
            final Locale locale, final String currencySymbol) {

        return formatUserBalance(
                getActiveUserAccount(userId, AccountTypeEnum.USER), locale,
                currencySymbol);

    }

    /**
     * Formats the {@link UserAccount} balance.
     *
     * @param userAccount
     *            The {@link UserAccount}.
     * @param locale
     * @param currencySymbol
     * @return
     */
    private String formatUserBalance(final UserAccount userAccount,
            final Locale locale, final String currencySymbol) {

        final BigDecimal userBalance;

        if (userAccount != null) {
            userBalance = userAccount.getAccount().getBalance();
        } else {
            userBalance = BigDecimal.ZERO;
        }

        final String currencySymbolWrk;

        if (currencySymbol == null) {
            currencySymbolWrk = "";
        } else {
            currencySymbolWrk = currencySymbol;
        }

        try {

            return BigDecimalUtil.localize(userBalance,
                    ConfigManager.getUserBalanceDecimals(), locale,
                    currencySymbolWrk, true);

        } catch (ParseException e) {
            throw new SpException(e);
        }
    }

    /**
     * Creates display statistics from aggregate result.
     *
     * @param aggr
     *            The {@link AggregateResult}.
     * @param locale
     *            The {@link Locale}.
     * @param currencySymbol
     *            The currency symbol.
     * @param multiplicand
     *            Either 1 or -1.
     * @return The {@link FinancialDisplayInfoDto.Stats}.
     */
    private FinancialDisplayInfoDto.Stats createFinancialDisplayInfoStats(
            final AggregateResult<BigDecimal> aggr, final Locale locale,
            final String currencySymbol, final BigDecimal multiplicand) {

        final int balanceDecimals = ConfigManager.getUserBalanceDecimals();
        final NumberFormat fmNumber = NumberFormat.getInstance(locale);

        final FinancialDisplayInfoDto.Stats stats =
                new FinancialDisplayInfoDto.Stats();

        final String valueEmpty = "";

        String valueWlk;

        try {

            //
            valueWlk = valueEmpty;

            if (aggr.getCount() != 0) {
                valueWlk = fmNumber.format(aggr.getCount());
            }
            stats.setCount(valueWlk);

            //
            valueWlk = valueEmpty;

            if (aggr.getAvg() != null) {
                valueWlk =
                        BigDecimalUtil.localize(
                                aggr.getAvg().multiply(multiplicand),
                                balanceDecimals, locale, currencySymbol, true);
            }
            stats.setAvg(valueWlk);

            //
            valueWlk = valueEmpty;
            if (aggr.getMax() != null) {
                valueWlk =
                        BigDecimalUtil.localize(
                                aggr.getMax().multiply(multiplicand),
                                balanceDecimals, locale, currencySymbol, true);
            }
            stats.setMax(valueWlk);

            //
            valueWlk = valueEmpty;
            if (aggr.getMin() != null) {
                valueWlk =
                        BigDecimalUtil.localize(
                                aggr.getMin().multiply(multiplicand),
                                balanceDecimals, locale, currencySymbol, true);
            }
            stats.setMin(valueWlk);

            //
            valueWlk = valueEmpty;
            if (aggr.getSum() != null) {
                valueWlk =
                        BigDecimalUtil.localize(
                                aggr.getSum().multiply(multiplicand),
                                balanceDecimals, locale, currencySymbol, true);
            }
            stats.setSum(valueWlk);

        } catch (ParseException e) {
            throw new SpException(e);
        }
        return stats;
    }

    @Override
    public FinancialDisplayInfoDto getFinancialDisplayInfo(final Locale locale,
            final String currencySymbol) {

        final String currencySymbolWrk =
                StringUtils.defaultString(currencySymbol);

        final FinancialDisplayInfoDto dto = new FinancialDisplayInfoDto();

        dto.setLocale(locale.getDisplayLanguage());

        dto.setUserCredit(createFinancialDisplayInfoStats(accountDAO()
                .getBalanceStats(true, false), locale, currencySymbolWrk,
                BigDecimal.ONE.negate()));

        dto.setUserDebit(createFinancialDisplayInfoStats(accountDAO()
                .getBalanceStats(true, true), locale, currencySymbolWrk,
                BigDecimal.ONE));

        return dto;
    }

    /**
     * Throws an {@link IllegalArgumentException} when account is not of type
     * SHARED.
     *
     * @param account
     *            The account.
     */
    private static void checkAccountShared(final Account account) {

        final String accountTypeShared = AccountTypeEnum.SHARED.toString();

        if (!account.getAccountType().equals(accountTypeShared)) {
            throw new IllegalArgumentException(String.format(
                    "Account [%s] must be %s.", account.getName(),
                    accountTypeShared));
        }
    }

    @Override
    public SharedAccountDisplayInfoDto getSharedAccountDisplayInfo(
            final Account account, final Locale locale,
            final String currencySymbol) {

        /*
         * INVARIANT: Account MUST be of type SHARED.
         */
        checkAccountShared(account);

        final SharedAccountDisplayInfoDto dto =
                new SharedAccountDisplayInfoDto();

        fillAccountDisplayInfo(account, locale, currencySymbol, dto);

        dto.setId(account.getId());
        dto.setName(account.getName());

        if (account.getParent() != null) {
            dto.setParentId(account.getParent().getId());
            dto.setParentName(account.getParent().getName());
        }

        dto.setNotes(account.getNotes());
        dto.setDeleted(account.getDeleted());

        return dto;
    }

    @Override
    public AbstractJsonRpcMethodResponse lazyUpdateSharedAccount(
            final SharedAccountDisplayInfoDto dto) {

        /*
         * INVARIANT: Account name MUST be present.
         */
        if (StringUtils.isBlank(dto.getName())) {
            return createErrorMsg("msg-shared-account-name-needed");
        }

        final Account parent;

        if (StringUtils.isBlank(dto.getParentName())) {

            parent = null;

        } else {
            /*
             * INVARIANT: Account can NOT be a sub-account of oneself.
             */
            if (dto.getParentName().equalsIgnoreCase(dto.getName())) {
                return createErrorMsg("msg-shared-account-parent-must-differ");
            }
            /*
             * INVARIANT: Parent account MUST exist.
             */
            parent =
                    accountDAO().findActiveSharedAccountByName(
                            dto.getParentName());

            if (parent == null) {
                return createErrorMsg("msg-shared-account-parent-unknown");
            }
        }

        /*
         * INVARIANT: Top Account name MUST be case insensitive unique among top
         * accounts.
         *
         * INVARIANT: Account name must be case insensitive unique among sibling
         * sub accounts.
         */
        final Account accountDuplicate;

        if (parent == null) {
            accountDuplicate =
                    accountDAO().findActiveSharedAccountByName(dto.getName());
        } else {
            accountDuplicate =
                    accountDAO().findActiveSharedChildAccountByName(
                            parent.getId(), dto.getName());
        }

        if (accountDuplicate != null
                && !accountDuplicate.getId().equals(dto.getId())) {
            return createErrorMsg("msg-shared-account-name-in-use");
        }

        //
        final Account account;
        final boolean newAccount = dto.getId() == null;

        if (newAccount) {
            account = this.createSharedAccountTemplate(dto.getName(), parent);
        } else {
            account = accountDAO().findById(dto.getId());
        }

        /*
         * INVARIANT: Account MUST exist.
         */
        if (account == null) {
            throw new IllegalArgumentException(String.format(
                    "Account [%s] can not be found.", dto.getName()));
        }

        /*
         * INVARIANT: Account MUST be of type SHARED.
         */
        checkAccountShared(account);

        /*
         * INVARIANT: A deleted Account can NOT be updated.
         */
        if (account.getDeleted()) {
            throw new IllegalArgumentException(String.format(
                    "Deleted Account [%s] can not be updated.",
                    account.getName()));
        }

        /*
         * Is this a top account?
         */
        final boolean topAccount = account.getParent() == null;

        /*
         * Logical delete.
         */
        if (dto.getDeleted() != null && dto.getDeleted()) {

            accountDAO().setLogicalDelete(account,
                    ServiceContext.getTransactionDate(),
                    ServiceContext.getActor());

            int nUpdated = 1;

            if (topAccount) {
                nUpdated +=
                        accountDAO().logicalDeleteSubAccounts(account.getId(),
                                ServiceContext.getTransactionDate(),
                                ServiceContext.getActor());
            }

            accountDAO().update(account);
            return createOkResult("msg-shared-accounts-deleted",
                    String.valueOf(nUpdated));
        }

        /*
         * Update.
         */

        /*
         * INVARIANT: An account that has sub accounts can NOT have a parent
         * account.
         */
        if (!newAccount && StringUtils.isNotBlank(dto.getParentName())
                && accountDAO().countSubAccounts(account.getId()) > 0) {
            return createErrorMsg("msg-shared-account-cannot-have-parent",
                    account.getName());
        }

        //
        account.setName(dto.getName().trim());
        account.setNameLower(dto.getName().trim().toLowerCase());
        account.setParent(parent);
        account.setNotes(dto.getNotes());

        //
        if (newAccount) {
            accountDAO().create(account);
        } else {
            accountDAO().update(account);
        }

        return JsonRpcMethodResult.createOkResult();
    }

    @Override
    public AccountDisplayInfoDto getAccountDisplayInfo(final User user,
            final Locale locale, final String currencySymbol) {

        final AccountDisplayInfoDto dto = new AccountDisplayInfoDto();

        final Account account =
                this.lazyGetUserAccount(user, AccountTypeEnum.USER)
                        .getAccount();

        fillAccountDisplayInfo(account, locale, currencySymbol, dto);

        return dto;
    }

    /**
     * Gets the {@link Account} information meant for display.
     *
     * @param account
     *            The {@link Account}.
     * @param locale
     *            The {@link Locale} used for formatting financial data.
     * @param currencySymbol
     *            {@code null} or empty when not applicable.
     * @param dto
     *            The {@link AccountDisplayInfoDto} object
     */
    private static void fillAccountDisplayInfo(final Account account,
            final Locale locale, final String currencySymbol,
            final AccountDisplayInfoDto dto) {

        final String currencySymbolWrk =
                StringUtils.defaultString(currencySymbol);

        final int balanceDecimals = ConfigManager.getUserBalanceDecimals();

        final String formattedCreditLimit;
        final AccountDisplayInfoDto.Status status;

        if (account.getRestricted()) {

            BigDecimal creditLimit;

            if (account.getUseGlobalOverdraft()) {
                creditLimit =
                        ConfigManager.instance().getConfigBigDecimal(
                                Key.FINANCIAL_GLOBAL_CREDIT_LIMIT);
            } else {
                creditLimit = account.getOverdraft();
            }

            try {
                formattedCreditLimit =
                        BigDecimalUtil.localize(creditLimit, balanceDecimals,
                                locale, currencySymbolWrk, true);
            } catch (ParseException e) {
                throw new SpException(e);
            }

            if (account.getBalance().compareTo(creditLimit.negate()) < 0) {
                status = AccountDisplayInfoDto.Status.OVERDRAFT;
            } else if (account.getBalance().compareTo(BigDecimal.ZERO) > 0) {
                status = AccountDisplayInfoDto.Status.DEBIT;
            } else {
                status = AccountDisplayInfoDto.Status.CREDIT;
            }

        } else {

            formattedCreditLimit = null;

            if (account.getBalance().compareTo(BigDecimal.ZERO) > 0) {
                status = AccountDisplayInfoDto.Status.DEBIT;
            } else {
                status = AccountDisplayInfoDto.Status.CREDIT;
            }
        }

        try {
            dto.setBalance(BigDecimalUtil.localize(account.getBalance(),
                    balanceDecimals, locale, currencySymbolWrk, true));
        } catch (ParseException e) {
            throw new SpException(e);
        }

        dto.setLocale(locale.getDisplayLanguage());
        dto.setCreditLimit(formattedCreditLimit);
        dto.setStatus(status);
    }

    @Override
    public AbstractJsonRpcMethodResponse redeemVoucher(
            final AccountVoucherRedeemDto dto) {

        final AccountVoucher voucher =
                accountVoucherDAO().findByCardNumber(dto.getCardNumber());

        final Date redeemDate = new Date(dto.getRedeemDate().longValue());

        /*
         * INVARIANT: cardNumber MUST exist, MUST NOT already be redeemed, and
         * MUST NOT be expired on the redeem date.
         */
        if (voucher == null
                || voucher.getRedeemedDate() != null
                || accountVoucherService()
                        .isVoucherExpired(voucher, redeemDate)) {
            return createErrorMsg(MSG_KEY_VOUCHER_REDEEM_NUMBER_INVALID);
        }

        /*
         * INVARIANT: User MUST exist.
         */
        final User user = userDAO().findActiveUserByUserId(dto.getUserId());

        if (user == null) {
            return createErrorMsg(MSG_KEY_VOUCHER_REDEEM_USER_UNKNOWN,
                    dto.getUserId());
        }

        /*
         * Update account.
         */
        final Account account =
                this.lazyGetUserAccount(user, AccountTypeEnum.USER)
                        .getAccount();

        account.setBalance(account.getBalance().add(voucher.getValueAmount()));
        account.setModifiedBy(ServiceContext.getActor());
        account.setModifiedDate(ServiceContext.getTransactionDate());

        accountDAO().update(account);

        /*
         * Create transaction.
         */
        final String comment =
                localize("msg-voucher-redeem-trx-comment", dto.getCardNumber());

        final AccountTrx accountTrx =
                this.createAccountTrx(account, AccountTrxTypeEnum.VOUCHER,
                        voucher.getValueAmount(), account.getBalance(), comment);

        accountTrx.setAccountVoucher(voucher);

        accountTrxDAO().create(accountTrx);

        /*
         * Update voucher
         */
        voucher.setRedeemedDate(redeemDate);
        voucher.setAccountTrx(accountTrx);

        accountVoucherDAO().update(voucher);

        //
        return JsonRpcMethodResult.createOkResult();
    }

    /**
     * Fills an account transaction from dto.
     *
     * @param trx
     *            The {@link AccountTrx}.
     * @param dto
     *            The {@link {@link UserPaymentGatewayDto}.
     */
    private static void fillTrxFromDto(final AccountTrx trx,
            final UserPaymentGatewayDto dto) {

        trx.setCurrencyCode(dto.getCurrencyCode());

        trx.setExtCurrencyCode(dto.getPaymentMethodCurrency());
        trx.setExtAmount(dto.getPaymentMethodAmount());
        trx.setExtFee(dto.getPaymentMethodFee());
        trx.setExtExchangeRate(dto.getExchangeRate());

        trx.setExtConfirmations(dto.getConfirmations());

        trx.setExtSource(dto.getGatewayId());
        trx.setExtId(dto.getTransactionId());
        trx.setExtMethod(dto.getPaymentMethod());
        trx.setExtMethodOther(dto.getPaymentMethodOther());
        trx.setExtMethodAddress(dto.getPaymentMethodAddress());
        trx.setExtDetails(dto.getPaymentMethodDetails());
    }

    @Override
    public void createPendingFundsFromGateway(final User user,
            final UserPaymentGatewayDto dto) {
        /*
         * Find the account to add the amount on.
         */
        final Account account =
                this.lazyGetUserAccount(user, AccountTypeEnum.USER)
                        .getAccount();

        /*
         * Create and fill transaction.
         */
        final AccountTrx trx =
                this.createAccountTrx(account, AccountTrxTypeEnum.GATEWAY,
                        BigDecimal.ZERO, BigDecimal.ZERO, null);

        fillTrxFromDto(trx, dto);
        accountTrxDAO().create(trx);
    }

    @Override
    public void acceptPendingFundsFromGateway(final AccountTrx trx,
            final UserPaymentGatewayDto dto) throws AccountingException {

        /*
         * INVARIANT: transaction must not be accepted.
         */
        if (trx.getAmount().compareTo(BigDecimal.ZERO) != 0) {
            throw new AccountingException(String.format(
                    "Transaction %s is already accepted.",
                    dto.getTransactionId()));
        }

        final Account account = trx.getAccount();

        account.setBalance(account.getBalance()
                .add(dto.getAmountAcknowledged()));
        account.setModifiedBy(ServiceContext.getActor());
        account.setModifiedDate(ServiceContext.getTransactionDate());

        fillTrxFromDto(trx, dto);

        trx.setAmount(dto.getAmountAcknowledged());
        trx.setBalance(account.getBalance());
        trx.setComment(dto.getComment());

        accountDAO().update(account);
        accountTrxDAO().update(trx);
    }

    @Override
    public void acceptFundsFromGateway(final User user,
            final UserPaymentGatewayDto dto,
            final Account orphanedPaymentAccount) {

        /*
         * Find the account to add the amount on.
         */
        final Account account;

        if (user == null) {
            account = orphanedPaymentAccount;
        } else {
            account =
                    this.lazyGetUserAccount(user, AccountTypeEnum.USER)
                            .getAccount();
        }

        account.setBalance(account.getBalance()
                .add(dto.getAmountAcknowledged()));
        account.setModifiedBy(ServiceContext.getActor());
        account.setModifiedDate(ServiceContext.getTransactionDate());

        /*
         * Create and fill transaction.
         */
        final AccountTrx trx =
                this.createAccountTrx(account, AccountTrxTypeEnum.GATEWAY,
                        dto.getAmountAcknowledged(), account.getBalance(),
                        dto.getComment());

        fillTrxFromDto(trx, dto);

        /*
         * Database update/persist.
         */
        accountDAO().update(account);
        accountTrxDAO().create(trx);
    }

    /**
     * Deposits funds to an {@link Account}.
     *
     * @param account
     *            The {@link Account}.
     * @param accountTrxType
     *            The {@link AccountTrxTypeEnum}.
     * @param paymentType
     *            The payment type.
     * @param receiptNumber
     *            The receipt number.
     * @param amount
     *            The funds amount.
     * @param comment
     *            The comment set in {@link PosPurchase} and {@link AccountTrx}.
     * @return The {@link AbstractJsonRpcMethodResponse}.
     */
    private AbstractJsonRpcMethodResponse depositFundsToAccount(
            final Account account, final AccountTrxTypeEnum accountTrxType,
            final String paymentType, final String receiptNumber,
            final BigDecimal amount, final String comment) {

        account.setBalance(account.getBalance().add(amount));
        account.setModifiedBy(ServiceContext.getActor());
        account.setModifiedDate(ServiceContext.getTransactionDate());

        /*
         * Create PosPurchase.
         */
        final PosPurchase purchase = new PosPurchase();

        purchase.setComment(comment);
        purchase.setPaymentType(paymentType);
        purchase.setTotalCost(amount);
        purchase.setReceiptNumber(receiptNumber);

        /*
         * Create transaction.
         */
        final AccountTrx accountTrx =
                this.createAccountTrx(account, accountTrxType, amount,
                        account.getBalance(), comment);

        // Set references.
        accountTrx.setPosPurchase(purchase);
        purchase.setAccountTrx(accountTrx);

        /*
         * Database update/persist.
         */
        accountDAO().update(account);
        accountTrxDAO().create(accountTrx);
        purchaseDAO().create(purchase);

        //
        final JsonRpcMethodResult methodResult =
                JsonRpcMethodResult.createOkResult();

        final ResultPosDeposit resultData = new ResultPosDeposit();
        resultData.setAccountTrxDbId(accountTrx.getId());

        methodResult.getResult().setData(resultData);

        return methodResult;

    }

    @Override
    public AbstractJsonRpcMethodResponse depositFunds(final PosDepositDto dto) {

        /*
         * INVARIANT: User MUST exist.
         */
        final User user = userDAO().findActiveUserByUserId(dto.getUserId());

        if (user == null) {
            return createErrorMsg(MSG_KEY_DEPOSIT_FUNDS_USER_UNKNOWN,
                    dto.getUserId());
        }

        /*
         * INVARIANT: Amount MUST be valid.
         */
        final String plainAmount =
                dto.getAmountMain() + "." + dto.getAmountCents();

        if (!BigDecimalUtil.isValid(plainAmount)) {
            return createErrorMsg(MSG_KEY_DEPOSIT_FUNDS_AMOUNT_ERROR);
        }

        final BigDecimal depositAmount = BigDecimalUtil.valueOf(plainAmount);

        /*
         * INVARIANT: Amount MUST be GT zero.
         */
        if (depositAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return createErrorMsg(MSG_KEY_DEPOSIT_FUNDS_AMOUNT_INVALID);
        }

        /*
         * Deposit amount into Account.
         */
        final Account account =
                this.lazyGetUserAccount(user, AccountTypeEnum.USER)
                        .getAccount();

        final String receiptNumber =
                purchaseDAO().getNextReceiptNumber(
                        ReceiptNumberPrefixEnum.DEPOSIT);

        return depositFundsToAccount(account, AccountTrxTypeEnum.DEPOSIT,
                dto.getPaymentType(), receiptNumber, depositAmount,
                dto.getComment());

    }

    @Override
    public PosDepositReceiptDto createPosDepositReceiptDto(
            final Long accountTrxId) {

        final AccountTrx accountTrx = accountTrxDAO().findById(accountTrxId);

        if (accountTrx == null) {
            throw new SpException("Transaction not found.");
        }

        if (!accountTrx.getTrxType().equals(
                AccountTrxTypeEnum.DEPOSIT.toString())) {
            throw new SpException("This is not a DEPOSIT transaction.");
        }

        final PosPurchase purchase = accountTrx.getPosPurchase();

        final User user =
                userAccountDAO().findByAccountId(
                        accountTrx.getAccount().getId()).getUser();

        //
        final PosDepositReceiptDto receipt = new PosDepositReceiptDto();

        receipt.setAccountTrx(accountTrx);

        receipt.setComment(purchase.getComment());
        receipt.setPlainAmount(BigDecimalUtil.toPlainString(accountTrx
                .getAmount()));
        receipt.setPaymentType(purchase.getPaymentType());
        receipt.setReceiptNumber(purchase.getReceiptNumber());
        receipt.setTransactedBy(accountTrx.getTransactedBy());
        receipt.setTransactionDate(accountTrx.getTransactionDate().getTime());
        receipt.setUserId(user.getUserId());
        receipt.setUserFullName(user.getFullName());

        return receipt;
    }

    @Override
    public AbstractJsonRpcMethodResponse changeBaseCurrency(
            final DaoBatchCommitter batchCommitter,
            final Currency currencyFrom, final Currency currencyTo,
            final double exchangeRate) {

        int nAccounts = 0;
        int nTrx = 0;

        /*
         * INVARIANT: currencyFrom must match current currency.
         */
        if (!ConfigManager.getAppCurrencyCode().equals(
                currencyFrom.getCurrencyCode())) {

            final String err =
                    String.format("Currency %s does not match current "
                            + "base currency %s.",
                            currencyFrom.getCurrencyCode(),
                            ConfigManager.getAppCurrencyCode());

            return JsonRpcMethodError.createBasicError(Code.INVALID_REQUEST,
                    err, null);
        }

        /*
         * INVARIANT: currencyFrom and currencyTo must differ.
         */
        if (currencyFrom.getCurrencyCode().equals(currencyTo.getCurrencyCode())) {
            return JsonRpcMethodError.createBasicError(Code.INVALID_REQUEST,
                    "Currency codes must be different.", null);
        }

        /*
         * INVARIANT: exchange rate must be GT zero.
         */
        if (exchangeRate <= 0.0) {
            return JsonRpcMethodError.createBasicError(Code.INVALID_REQUEST,
                    "Exchange rate must be greater than zero.", null);
        }

        /*
         * Batch process.
         */
        final AccountDao.ListFilter filter = new AccountDao.ListFilter();

        final Integer maxResults =
                Integer.valueOf(batchCommitter.getCommitThreshold());

        int startPosition = 0;

        final List<Account> list =
                accountDAO().getListChunk(filter,
                        Integer.valueOf(startPosition), maxResults,
                        AccountDao.Field.ACCOUNT_TYPE, true);

        final BigDecimal exchangeDecimal = BigDecimal.valueOf(exchangeRate);

        for (final Account account : list) {

            int nChanges = 0;

            // Overdraft
            if (account.getOverdraft().compareTo(BigDecimal.ZERO) != 0) {
                account.setOverdraft(account.getOverdraft().multiply(
                        exchangeDecimal));
                nChanges++;
            }

            final BigDecimal balance = account.getBalance();

            // Balance
            if (balance.compareTo(BigDecimal.ZERO) != 0) {

                // Reverse current balance currency.
                final AccountTrx trxReversal =
                        this.createAccountTrx(account,
                                AccountTrxTypeEnum.ADJUST, balance.negate(),
                                BigDecimal.ZERO, null);

                trxReversal.setCurrencyCode(currencyFrom.getCurrencyCode());

                accountTrxDAO().create(trxReversal);

                nChanges++;
                nTrx++;

                // Initialize with new balance currency.
                final StringBuilder comment = new StringBuilder();

                comment.append(currencyFrom.getCurrencyCode()).append(" ")
                        .append(balance.toPlainString()).append(" * ")
                        .append(exchangeDecimal.toPlainString());

                final BigDecimal balanceInit =
                        balance.multiply(exchangeDecimal);

                final AccountTrx trxInit =
                        this.createAccountTrx(account,
                                AccountTrxTypeEnum.ADJUST, balanceInit,
                                balanceInit, comment.toString());

                trxInit.setCurrencyCode(currencyTo.getCurrencyCode());

                accountTrxDAO().create(trxInit);

                nChanges++;
                nTrx++;

                //
                account.setBalance(balanceInit);
            }

            if (nChanges > 0) {

                accountDAO().update(account);

                batchCommitter.increment();
                nAccounts++;
            }
        }

        //
        ConfigManager.instance().updateConfigKey(
                Key.FINANCIAL_GLOBAL_CURRENCY_CODE,
                currencyTo.getCurrencyCode(), ServiceContext.getActor());

        batchCommitter.increment();
        batchCommitter.commit();

        /*
         * Return result.
         */
        final StringBuilder result = new StringBuilder();

        if (batchCommitter.isTest()) {
            result.append("[TEST] ");
        }

        result.append(localize("msg-changed-base-currency", currencyFrom
                .getCurrencyCode(), currencyTo.getCurrencyCode(), Double
                .valueOf(exchangeRate).toString(), Integer.valueOf(nTrx)
                .toString(), Integer.valueOf(nAccounts).toString()));

        return JsonRpcMethodResult.createOkResult(result.toString());
    }

    /**
     * Adds an {@link AccountTrx} and updates the {@link Account}.
     *
     * @param user
     *            The {@link User}.
     * @param accountType
     *            The {@link AccountTypeEnum}.
     * @param trxType
     *            The {@link AccountTrxTypeEnum}.
     * @param currencyCode
     *            The ISO currency code.
     * @param amount
     *            The amount (can be negative).
     * @param trxComment
     *            The transaction comment.
     */
    private void addAccountTrx(final User user,
            final AccountTypeEnum accountType,
            final AccountTrxTypeEnum trxType, final String currencyCode,
            final BigDecimal amount, final String trxComment) {

        final Account account =
                this.lazyGetUserAccount(user, accountType).getAccount();

        account.setBalance(account.getBalance().add(amount));
        account.setModifiedBy(ServiceContext.getActor());
        account.setModifiedDate(ServiceContext.getTransactionDate());

        final AccountTrx trx =
                this.createAccountTrx(account, trxType, currencyCode, amount,
                        account.getBalance(), trxComment);

        accountDAO().update(account);
        accountTrxDAO().create(trx);
    }

    /**
     * Checks if balance is sufficient to make a payment. We allow for a
     * non-significant overdraft beyond the balance precision.
     *
     * @param balance
     *            The balance amount.
     * @param payment
     *            The payment (a positive value).
     * @param balancePrecision
     *            The balance precision (number of decimals).
     * @return {@code true} if payment is within balance.
     */
    private static boolean isPaymentWithinBalance(final BigDecimal balance,
            final BigDecimal payment, final int balancePrecision) {

        final BigDecimal balanceMin =
                BigDecimal.valueOf(-5 / Math.pow(10, balancePrecision + 1));

        final BigDecimal balanceAfter = balance.subtract(payment);

        return balanceAfter.compareTo(balanceMin) >= 0;
    }

    @Override
    public AbstractJsonRpcMethodResponse transferUserCredit(
            final UserCreditTransferDto dto) {

        /*
         * INVARIANT: Amount MUST be valid.
         */
        final String plainAmount =
                dto.getAmountMain() + "." + dto.getAmountCents();

        if (!BigDecimalUtil.isValid(plainAmount)) {

            return JsonRpcMethodError.createBasicError(Code.INVALID_PARAMS,
                    localize("msg-amount-error", plainAmount));
        }

        final BigDecimal transferAmount = BigDecimalUtil.valueOf(plainAmount);

        /*
         * INVARIANT: Amount MUST be GT zero.
         */
        if (transferAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return JsonRpcMethodError.createBasicError(Code.INVALID_PARAMS,
                    localize("msg-amount-must-be-positive"));
        }

        /*
         * INVARIANT: MUST transfer to another user.
         */
        if (dto.getUserIdFrom().equalsIgnoreCase(dto.getUserIdTo())) {
            return JsonRpcMethodError.createBasicError(Code.INVALID_PARAMS,
                    localize("msg-user-credit-transfer-err-user-same"));
        }

        /*
         * INVARIANT: Source and target user MUST exist.
         */
        final UserDao userDao = ServiceContext.getDaoContext().getUserDao();

        final User lockedUserTo = userDao.lockByUserId(dto.getUserIdTo());
        final User lockedUserFrom = userDao.lockByUserId(dto.getUserIdFrom());

        if (lockedUserFrom == null || lockedUserTo == null) {

            final String msg;

            if (lockedUserFrom != null) {
                msg =
                        localize("msg-user-credit-transfer-err-unknown-user",
                                dto.getUserIdTo());
            } else if (lockedUserTo != null) {
                msg =
                        localize("msg-user-credit-transfer-err-unknown-user",
                                dto.getUserIdFrom());
            } else {
                msg =
                        localize("msg-user-credit-transfer-err-unknown-users",
                                dto.getUserIdFrom(), dto.getUserIdTo());
            }

            return JsonRpcMethodError
                    .createBasicError(Code.INVALID_PARAMS, msg);
        }

        /*
         * INVARIANT: User balance MUST be sufficient.
         */
        final int userBalanceDecimals = ConfigManager.getUserBalanceDecimals();

        final AccountTypeEnum accountTypeTransfer = AccountTypeEnum.USER;

        final Account accountFrom =
                this.lazyGetUserAccount(lockedUserFrom, accountTypeTransfer)
                        .getAccount();

        if (!isPaymentWithinBalance(accountFrom.getBalance(), transferAmount,
                userBalanceDecimals)) {
            return JsonRpcMethodError.createBasicError(Code.INVALID_PARAMS,
                    localize("msg-user-credit-transfer-err-amount-greater"));
        }

        /*
         * INVARIANT: transfer amount MUST be GT/EQ to minimum and LT/EQ
         * maximum.
         */
        try {
            final BigDecimal minimalTransfer =
                    ConfigManager.instance().getConfigBigDecimal(
                            Key.FINANCIAL_USER_TRANSFER_AMOUNT_MIN);

            if (minimalTransfer != null
                    && transferAmount.compareTo(minimalTransfer) < 0) {

                return JsonRpcMethodError.createBasicError(
                        Code.INVALID_PARAMS,
                        localize("msg-amount-must-be-gt-eq", BigDecimalUtil
                                .localize(minimalTransfer, userBalanceDecimals,
                                        ServiceContext.getLocale(),
                                        ServiceContext.getAppCurrencySymbol(),
                                        true)));
            }

            final BigDecimal maximalTransfer =
                    ConfigManager.instance().getConfigBigDecimal(
                            Key.FINANCIAL_USER_TRANSFER_AMOUNT_MAX);

            if (maximalTransfer != null
                    && transferAmount.compareTo(maximalTransfer) > 0) {

                return JsonRpcMethodError.createBasicError(
                        Code.INVALID_PARAMS,
                        localize("msg-amount-must-be-lt-eq", BigDecimalUtil
                                .localize(maximalTransfer, userBalanceDecimals,
                                        ServiceContext.getLocale(),
                                        ServiceContext.getAppCurrencySymbol(),
                                        true)));
            }
        } catch (ParseException e) {
            throw new SpException(e.getMessage(), e);
        }

        /*
         * Transfer funds.
         */
        final String currencyCode = ConfigManager.getAppCurrencyCode();

        final StringBuilder trxComment =
                new StringBuilder().append(Messages.getSystemMessage(
                        getClass(), "msg-user-credit-transfer-comment",
                        dto.getUserIdFrom(), dto.getUserIdTo()));

        if (StringUtils.isNotBlank(dto.getComment())) {
            trxComment.append(" - ").append(dto.getComment());
        }

        addAccountTrx(lockedUserFrom, accountTypeTransfer,
                AccountTrxTypeEnum.TRANSFER, currencyCode,
                transferAmount.negate(), trxComment.toString());

        addAccountTrx(lockedUserTo, accountTypeTransfer,
                AccountTrxTypeEnum.TRANSFER, currencyCode, transferAmount,
                trxComment.toString());

        /*
         * Notifications.
         */
        final String userNotification;

        try {
            userNotification =
                    localize(
                            "msg-user-credit-transfer-for-user",
                            BigDecimalUtil.localize(transferAmount,
                                    userBalanceDecimals,
                                    ServiceContext.getLocale(),
                                    ServiceContext.getAppCurrencySymbol(), true),
                            dto.getUserIdTo());

            AdminPublisher.instance().publish(
                    PubTopicEnum.USER,
                    PubLevelEnum.INFO,
                    Messages.getSystemMessage(getClass(),
                            "msg-user-credit-transfer-for-system", dto
                                    .getUserIdFrom(), BigDecimalUtil.localize(
                                    transferAmount, userBalanceDecimals,
                                    ConfigManager.getDefaultLocale(),
                                    ConfigManager.getAppCurrencyCode(), true),
                            dto.getUserIdTo()));

            UserMsgIndicator.notifyAccountInfoEvent(dto.getUserIdFrom());
            UserMsgIndicator.notifyAccountInfoEvent(dto.getUserIdTo());

        } catch (ParseException e) {
            throw new SpException(e.getMessage(), e);
        }

        return JsonRpcMethodResult.createOkResult(userNotification);
    }

    @Override
    public Account createSharedAccountTemplate(final String name,
            final Account parent) {

        final Account account = new Account();

        account.setName(name);
        account.setNameLower(name.toLowerCase());

        account.setParent(parent);

        account.setBalance(BigDecimal.ZERO);
        account.setOverdraft(BigDecimal.ZERO);
        account.setRestricted(false);
        account.setUseGlobalOverdraft(false);

        account.setAccountType(Account.AccountTypeEnum.SHARED.toString());
        account.setComments(Account.CommentsEnum.COMMENT_OPTIONAL.toString());
        account.setInvoicing(Account.InvoicingEnum.ALWAYS_ON.toString());
        account.setDeleted(false);
        account.setDisabled(false);

        account.setCreatedBy(ServiceContext.getActor());
        account.setCreatedDate(ServiceContext.getTransactionDate());

        return account;
    }

}
