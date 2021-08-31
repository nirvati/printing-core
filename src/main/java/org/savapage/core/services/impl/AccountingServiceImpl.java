/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2020 Datraverse B.V.
 * Authors: Rijk Ravestein.
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
package org.savapage.core.services.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Currency;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.savapage.core.SpException;
import org.savapage.core.cometd.AdminPublisher;
import org.savapage.core.cometd.PubLevelEnum;
import org.savapage.core.cometd.PubTopicEnum;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.dao.AccountDao;
import org.savapage.core.dao.PosPurchaseDao.ReceiptNumberPrefixEnum;
import org.savapage.core.dao.PrinterDao;
import org.savapage.core.dao.UserGroupMemberDao;
import org.savapage.core.dao.enums.AccountTrxTypeEnum;
import org.savapage.core.dao.helpers.AggregateResult;
import org.savapage.core.dao.helpers.DaoBatchCommitter;
import org.savapage.core.dto.AccountDisplayInfoDto;
import org.savapage.core.dto.AccountVoucherRedeemDto;
import org.savapage.core.dto.CreditLimitDtoEnum;
import org.savapage.core.dto.FinancialDisplayInfoDto;
import org.savapage.core.dto.IppMediaCostDto;
import org.savapage.core.dto.IppMediaSourceCostDto;
import org.savapage.core.dto.MediaCostDto;
import org.savapage.core.dto.MediaPageCostDto;
import org.savapage.core.dto.PosDepositDto;
import org.savapage.core.dto.PosDepositReceiptDto;
import org.savapage.core.dto.PosSalesDto;
import org.savapage.core.dto.PosTransactionDto;
import org.savapage.core.dto.SharedAccountDisplayInfoDto;
import org.savapage.core.dto.UserAccountingDto;
import org.savapage.core.dto.UserCreditTransferDto;
import org.savapage.core.dto.UserGroupDto;
import org.savapage.core.dto.UserPaymentGatewayDto;
import org.savapage.core.jpa.Account;
import org.savapage.core.jpa.Account.AccountTypeEnum;
import org.savapage.core.jpa.AccountTrx;
import org.savapage.core.jpa.AccountVoucher;
import org.savapage.core.jpa.DocLog;
import org.savapage.core.jpa.PosPurchase;
import org.savapage.core.jpa.PrintOut;
import org.savapage.core.jpa.Printer;
import org.savapage.core.jpa.User;
import org.savapage.core.jpa.UserAccount;
import org.savapage.core.jpa.UserGroup;
import org.savapage.core.jpa.UserGroupAccount;
import org.savapage.core.json.rpc.AbstractJsonRpcMethodResponse;
import org.savapage.core.json.rpc.JsonRpcError.Code;
import org.savapage.core.json.rpc.JsonRpcMethodError;
import org.savapage.core.json.rpc.JsonRpcMethodResult;
import org.savapage.core.json.rpc.impl.ResultPosDeposit;
import org.savapage.core.msg.UserMsgIndicator;
import org.savapage.core.outbox.OutboxInfoDto.OutboxAccountTrxInfo;
import org.savapage.core.outbox.OutboxInfoDto.OutboxJobDto;
import org.savapage.core.pdf.PdfPrintCollector;
import org.savapage.core.print.proxy.ProxyPrintException;
import org.savapage.core.print.proxy.ProxyPrintJobChunk;
import org.savapage.core.print.proxy.ProxyPrintJobChunkInfo;
import org.savapage.core.services.AccountingService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.helpers.AccountTrxInfo;
import org.savapage.core.services.helpers.AccountTrxInfoSet;
import org.savapage.core.services.helpers.AccountingException;
import org.savapage.core.services.helpers.ProxyPrintCostDto;
import org.savapage.core.services.helpers.ProxyPrintCostParms;
import org.savapage.core.util.BigDecimalUtil;
import org.savapage.core.util.Messages;
import org.savapage.ext.papercut.PaperCutException;
import org.savapage.ext.papercut.PaperCutHelper;
import org.savapage.ext.papercut.PaperCutServerProxy;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class AccountingServiceImpl extends AbstractService
        implements AccountingService {

    /**
     * Use grouping for integer part of localized currency amount.
     */
    private static final boolean CURRENCY_AMOUNT_GROUPING_USED = true;

    @Override
    public PrinterDao.CostMediaAttr getCostMediaAttr() {
        return printerDAO().getCostMediaAttr();
    }

    @Override
    public PrinterDao.CostMediaAttr
            getCostMediaAttr(final String ippMediaName) {
        return printerDAO().getCostMediaAttr(ippMediaName);
    }

    @Override
    public PrinterDao.MediaSourceAttr
            getMediaSourceAttr(final String ippMediaSourceName) {

        return printerDAO().getMediaSourceAttr(ippMediaSourceName);
    }

    @Override
    public UserAccount getActiveUserAccount(final String userId,
            final Account.AccountTypeEnum accountType) {

        return userAccountDAO().findByActiveUserId(userId, accountType);
    }

    @Override
    public Account getActiveUserGroupAccount(final String groupName) {
        return accountDAO().findActiveAccountByName(groupName,
                AccountTypeEnum.GROUP);
    }

    /**
     * Checks if account is a shared account, if not an {@link SpException} is
     * thrown.
     *
     * @param account
     *            The account to check.
     */
    private static void checkSharedAccountType(final Account account) {

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
            account = accountDAO().findActiveSharedChildAccountByName(
                    parent.getId(), accountName);
        }

        if (account == null) {
            account = accountDAO().createFromTemplate(accountName,
                    accountTemplate);
        }
        return account;
    }

    @Override
    public UserAccount lazyGetUserAccount(final User user,
            final Account.AccountTypeEnum accountType) {

        final UserAccount userAccount =
                this.getActiveUserAccount(user.getUserId(), accountType);

        if (userAccount != null) {
            return userAccount;
        }

        if (accountType != AccountTypeEnum.USER) {
            return null;
        }

        /*
         * Check Group Memberships (explicit).
         */
        final UserGroupMemberDao.UserFilter filter =
                new UserGroupMemberDao.UserFilter();

        filter.setUserId(user.getId());

        final List<UserGroup> groupList =
                userGroupMemberDAO().getGroupChunk(filter, null, null,
                        UserGroupMemberDao.GroupField.GROUP_NAME, true);

        BigDecimal accumulatedBalance = BigDecimal.ZERO;
        BigDecimal overdraft = BigDecimal.ZERO;
        Boolean useGlobalOverdraft = Boolean.FALSE;
        Boolean initiallyRestricted = Boolean.TRUE;

        int groupCount = 0;

        for (final UserGroup group : groupList) {

            if (!group.getInitialSettingsEnabled()) {
                continue;
            }

            if (!group.getInitiallyRestricted()) {
                initiallyRestricted = Boolean.FALSE;
            }

            accumulatedBalance =
                    accumulatedBalance.add(group.getInitialCredit());
            overdraft = group.getInitialOverdraft();
            useGlobalOverdraft = group.getInitialUseGlobalOverdraft();

            groupCount++;
        }

        final UserAccount userAccountNew;
        final Account account;

        if (groupCount == 0) {

            final UserGroup userGroup;

            if (user.getInternal()) {
                userGroup = userGroupService().getInternalUserGroup();
            } else {
                userGroup = userGroupService().getExternalUserGroup();
            }

            userAccountNew = createUserAccount(user, userGroup);
            account = userAccountNew.getAccount();

        } else {

            userAccountNew = createUserAccount(user, null);
            account = userAccountNew.getAccount();

            account.setBalance(accumulatedBalance);
            account.setRestricted(initiallyRestricted);
            account.setOverdraft(overdraft);
            account.setUseGlobalOverdraft(useGlobalOverdraft);
        }

        if (account.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            accountTrxDAO().create(
                    createAccountTrx(account, AccountTrxTypeEnum.INITIAL,
                            account.getBalance(), account.getBalance(), ""));
        }

        return userAccountNew;
    }

    @Override
    public Account lazyGetUserGroupAccount(final UserGroup userGroup) {

        final Account account =
                this.getActiveUserGroupAccount(userGroup.getGroupName());

        if (account != null) {
            return account;
        }
        return this.createUserGroupAccount(userGroup);
    }

    /**
     * Localizes a {@link BigDecimal} with a minimal precision.
     *
     * @param decimal
     *            The {@link BigDecimal} to localize.
     * @param fractionDigitsMinimum
     *            The minimum number of fraction digits in the result.
     * @param locale
     *            The {@link Locale}.
     * @return The localized decimal as string.
     * @throws ParseException
     *             When parsing fails.
     */
    private String localizeMinimalPrecision(final BigDecimal decimal,
            final int fractionDigitsMinimum, final Locale locale)
            throws ParseException {
        return BigDecimalUtil.localizeMinimalPrecision(decimal,
                fractionDigitsMinimum, locale, CURRENCY_AMOUNT_GROUPING_USED);
    }

    /**
     * Localizes a {@link BigDecimal}.
     *
     * @param decimal
     *            The {@link BigDecimal} to localize.
     * @param fractionDigits
     *            The number of fraction digits in the result.
     * @param locale
     *            The {@link Locale}.
     * @param currencySymbol
     *            The currency symbol to prepend to the formatted decimal.
     * @return The localized string.
     * @throws ParseException
     *             When parsing fails.
     */
    private String localize(final BigDecimal decimal, final int fractionDigits,
            final Locale locale, final String currencySymbol)
            throws ParseException {
        return BigDecimalUtil.localize(decimal, fractionDigits, locale,
                currencySymbol, CURRENCY_AMOUNT_GROUPING_USED);
    }

    /**
     * Localizes a {@link BigDecimal}.
     *
     * @param decimal
     *            The {@link BigDecimal} to localize.
     * @param fractionDigits
     *            The number of fraction digits in the result.
     * @param locale
     *            The {@link Locale}.
     * @return The localized string.
     * @throws ParseException
     *             When parsing fails.
     */
    private String localize(final BigDecimal decimal, final int fractionDigits,
            final Locale locale) throws ParseException {
        return BigDecimalUtil.localize(decimal, fractionDigits, locale,
                CURRENCY_AMOUNT_GROUPING_USED);
    }

    @Override
    public UserAccountingDto createUserAccounting(final BigDecimal balance,
            final Boolean restricted, final Boolean useGlobalOverdraft,
            final BigDecimal overdraft) {

        final UserAccountingDto dto = new UserAccountingDto();
        final int minimalDecimals = ConfigManager.getUserBalanceDecimals();

        dto.setLocale(ServiceContext.getLocale().toLanguageTag());

        try {
            dto.setBalance(this.localizeMinimalPrecision(balance,
                    minimalDecimals, ServiceContext.getLocale()));

            CreditLimitDtoEnum creditLimit;

            if (restricted) {
                if (useGlobalOverdraft) {
                    creditLimit = CreditLimitDtoEnum.DEFAULT;
                } else {
                    creditLimit = CreditLimitDtoEnum.INDIVIDUAL;
                }
            } else {
                creditLimit = CreditLimitDtoEnum.NONE;
            }

            dto.setCreditLimit(creditLimit);
            dto.setCreditLimitAmount(this.localizeMinimalPrecision(overdraft,
                    minimalDecimals, ServiceContext.getLocale()));

        } catch (ParseException e) {
            throw new SpException(e);
        }
        return dto;

    }

    @Override
    public UserAccountingDto getUserAccounting(final User user) {

        final Account account =
                lazyGetUserAccount(user, AccountTypeEnum.USER).getAccount();

        return this.createUserAccounting(account.getBalance(),
                account.getRestricted(), account.getUseGlobalOverdraft(),
                account.getOverdraft());
    }

    @Override
    public UserAccountingDto getInitialUserAccounting(final UserGroup group) {
        return this.createUserAccounting(group.getInitialCredit(),
                group.getInitiallyRestricted(),
                group.getInitialUseGlobalOverdraft(),
                group.getInitialOverdraft());
    }

    @Override
    public void setInitialUserAccounting(final UserGroup group,
            final UserAccountingDto dto) throws ParseException {

        final Locale dtoLocale;

        if (dto.getLocale() != null) {
            dtoLocale = Locale.forLanguageTag(dto.getLocale());
        } else {
            dtoLocale = ServiceContext.getLocale();
        }

        if (dto.getBalance() != null) {

            final String amount = dto.getBalance();
            group.setInitialCredit(BigDecimalUtil.parse(amount, dtoLocale,
                    false, CURRENCY_AMOUNT_GROUPING_USED));
        }

        final CreditLimitDtoEnum creditLimit = dto.getCreditLimit();

        if (creditLimit != null) {

            group.setInitiallyRestricted(
                    creditLimit != CreditLimitDtoEnum.NONE);

            group.setInitialUseGlobalOverdraft(
                    creditLimit == CreditLimitDtoEnum.DEFAULT);

            if (creditLimit == CreditLimitDtoEnum.INDIVIDUAL) {
                final String amount = dto.getCreditLimitAmount();
                group.setInitialOverdraft(BigDecimalUtil.parse(amount,
                        dtoLocale, false, CURRENCY_AMOUNT_GROUPING_USED));
            }
        }
    }

    /**
     * Creates an {@link AccountTrx} for an {@link Account} when relevant
     * difference in balance is determined.
     * <p>
     * Note: A difference in user balance in decimal range beyond
     * {@link IConfigProp.Key#FINANCIAL_USER_BALANCE_DECIMALS} is considered
     * irrelevant.
     * </p>
     *
     * @param account
     *            The {@link Account} to set the balance in
     * @param balanceNew
     *            The new balance amount.
     * @param comment
     *            The comment for the {@link AccountTrx}.
     */
    private void checkCreateAccountTrx(final Account account,
            final BigDecimal balanceNew, final String comment) {

        final int userBalanceDecimals = ConfigManager.instance()
                .getConfigInt(Key.FINANCIAL_USER_BALANCE_DECIMALS);

        final BigDecimal balanceDiff = balanceNew.subtract(account.getBalance())
                .setScale(userBalanceDecimals, BigDecimal.ROUND_DOWN);

        if (balanceDiff.compareTo(BigDecimal.ZERO) != 0) {

            final AccountTrx trx = createAccountTrx(account,
                    AccountTrxTypeEnum.ADJUST, balanceDiff, balanceNew,
                    StringUtils.defaultString(comment));

            accountTrxDAO().create(trx);

            account.setBalance(balanceNew);
        }
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
                final BigDecimal balanceNew = BigDecimalUtil.parse(amount,
                        dtoLocale, false, CURRENCY_AMOUNT_GROUPING_USED);
                checkCreateAccountTrx(account, balanceNew, dto.getComment());
            } catch (ParseException e) {
                return createError("msg-amount-error", amount);
            }
        }

        final CreditLimitDtoEnum creditLimit = dto.getCreditLimit();

        if (creditLimit != null) {

            account.setRestricted(creditLimit != CreditLimitDtoEnum.NONE);
            account.setUseGlobalOverdraft(
                    creditLimit == CreditLimitDtoEnum.DEFAULT);

            if (creditLimit == CreditLimitDtoEnum.INDIVIDUAL) {
                final String amount = dto.getCreditLimitAmount();
                try {
                    account.setOverdraft(BigDecimalUtil.parse(amount, dtoLocale,
                            false, CURRENCY_AMOUNT_GROUPING_USED));
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
            account.setUseGlobalOverdraft(
                    userGroupTemplate.getInitialUseGlobalOverdraft());

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

        return userAccount;
    }

    /**
     * Creates an {@link Account} of type {@link Account.AccountTypeEnum#GROUP}
     * for a {@link UserGroup}.
     *
     * @param userGroup
     *            The {@link UserGroup}.
     * @return The {@link Account} created.
     */
    private Account createUserGroupAccount(final UserGroup userGroup) {

        final Account account = new Account();

        account.setAccountType(Account.AccountTypeEnum.GROUP.toString());

        account.setBalance(BigDecimal.ZERO);
        account.setOverdraft(BigDecimal.ZERO);
        account.setRestricted(Boolean.FALSE);
        account.setUseGlobalOverdraft(Boolean.FALSE);

        account.setComments(Account.CommentsEnum.COMMENT_OPTIONAL.toString());
        account.setInvoicing(Account.InvoicingEnum.USER_CHOICE_ON.toString());
        account.setDeleted(Boolean.FALSE);
        account.setDisabled(Boolean.FALSE);
        account.setName(userGroup.getGroupName());
        account.setNameLower(userGroup.getGroupName().toLowerCase());

        account.setCreatedBy(ServiceContext.getActor());
        account.setCreatedDate(ServiceContext.getTransactionDate());

        accountDAO().create(account);

        return account;
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

        return createAccountTrx(account, trxType,
                ConfigManager.getAppCurrency().getCurrencyCode(), amount,
                accountBalance, comment);
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

        final Integer weight = Integer.valueOf(1);
        trx.setTransactionWeight(weight);
        trx.setTransactionWeightUnit(Integer.valueOf(1));

        trx.setTransactedBy(ServiceContext.getActor());
        trx.setTransactionDate(ServiceContext.getTransactionDate());

        return trx;
    }

    @Override
    public void createAccountTrx(final Account account,
            final PrintOut printOut) {

        final DocLog docLog = printOut.getDocOut().getDocLog();
        final int weight = printOut.getNumberOfCopies().intValue();

        createAccountTrx(account, docLog, AccountTrxTypeEnum.PRINT_OUT, weight,
                weight, 1, null);
    }

    @Override
    public void chargeAccountTrxAmount(final AccountTrx trx,
            final BigDecimal trxAmount, final DocLog trxDocLog) {

        final Account account = trx.getAccount();

        /*
         * Update Account.
         */
        account.setBalance(account.getBalance().subtract(trxAmount));
        accountDAO().update(account);

        /*
         * Update AccountTrx.
         */
        trx.setAmount(trxAmount.negate());
        trx.setBalance(account.getBalance());

        trx.setTransactedBy(ServiceContext.getActor());
        trx.setTransactionDate(ServiceContext.getTransactionDate());

        if (trxDocLog != null) {
            trx.setDocLog(trxDocLog);
        }

        accountTrxDAO().update(trx);
    }

    @Override
    public void createAccountTrxs(final AccountTrxInfoSet accountTrxInfoSet,
            final DocLog docLog, final AccountTrxTypeEnum trxType) {

        final int nTotalWeight = accountTrxInfoSet.getWeightTotal();

        for (final AccountTrxInfo trxInfo : accountTrxInfoSet
                .getAccountTrxInfoList()) {

            createAccountTrx(trxInfo.getAccount(), docLog, trxType,
                    nTotalWeight, trxInfo.getWeight(),
                    trxInfo.getWeightUnit().intValue(),
                    trxInfo.getExtDetails());
        }
    }

    @Override
    public List<AccountTrx> createAccountTrxsUI(final OutboxJobDto outboxJob) {

        final List<AccountTrx> trxList = new ArrayList<>();

        final AccountDao dao = ServiceContext.getDaoContext().getAccountDao();
        final int scale = ConfigManager.getFinancialDecimalsInDatabase();

        if (outboxJob.getAccountTransactions() == null) {

            final AccountTrx trx = new AccountTrx();

            final Account account = new Account();
            account.setAccountType(AccountTypeEnum.USER.toString());
            trx.setAccount(account);

            final User user = ServiceContext.getDaoContext().getUserDao()
                    .findById(outboxJob.getUserId());

            if (user == null || user.getUserId() == null) {
                account.setName("");
            } else {
                account.setName(user.getUserId());
            }

            trx.setTransactionWeight(Integer.valueOf(outboxJob.getCopies()));
            trx.setTransactionWeightUnit(Integer.valueOf(1));

            trx.setCurrencyCode(ConfigManager.getAppCurrencyCode());
            trx.setAmount(outboxJob.getCostTotal().negate());

            trxList.add(trx);

        } else {

            final int weightTotal =
                    outboxJob.getAccountTransactions().getWeightTotal();

            final BigDecimal costTotal = outboxJob.getCostTotal();

            for (final OutboxAccountTrxInfo trxInfo : outboxJob
                    .getAccountTransactions().getTransactions()) {

                final AccountTrx trx = new AccountTrx();

                trx.setAccount(dao.findById(trxInfo.getAccountId()));
                trx.setExtDetails(trxInfo.getExtDetails());

                final Integer weight = Integer.valueOf(trxInfo.getWeight());
                final Integer weightUnit;

                if (trxInfo.getWeightUnit() == null) {
                    weightUnit = Integer.valueOf(1);
                } else {
                    weightUnit = trxInfo.getWeightUnit();
                }

                trx.setTransactionWeight(weight);
                trx.setTransactionWeightUnit(weightUnit);

                trx.setCurrencyCode(ConfigManager.getAppCurrencyCode());

                if (weightTotal == 0) {
                    trx.setAmount(BigDecimal.ZERO);
                } else {
                    trx.setAmount(this.calcWeightedAmount(costTotal,
                            weightTotal, trxInfo.getWeight(),
                            weightUnit.intValue(), scale).negate());
                }
                trxList.add(trx);
            }
        }
        return trxList;
    }

    @Override
    public BigDecimal calcWeightedAmount(final BigDecimal amount,
            final int weightTotal, final int weight, final int weightUnit,
            final int scale) {
        return amount.multiply(BigDecimal.valueOf(weight)).divide(
                BigDecimal.valueOf(weightTotal * weightUnit), scale,
                RoundingMode.HALF_EVEN);
    }

    @Override
    public BigDecimal calcCostPerPrintedCopy(final BigDecimal totalCost,
            final int copies) {

        if (totalCost.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return totalCost.divide(new BigDecimal(copies),
                ConfigManager.getFinancialDecimalsInDatabase(),
                RoundingMode.HALF_EVEN);
    }

    @Override
    public BigDecimal calcPrintedCopies(final BigDecimal cost,
            final BigDecimal costPerCopy, final int scale) {
        return cost.divide(costPerCopy, scale, RoundingMode.HALF_UP);
    }

    /**
     * Creates an {@link AccountTrx} of {@link AccountTrx.AccountTrxTypeEnum},
     * updates the {@link Account} and adds the {@link AccountTrx} to the
     * {@link DocLog}.
     * <p>
     * Note: when {@link DocLog#getCostOriginal()} equals
     * {@link BigDecimal#ZERO}, this is a so-called "shadow" transaction, whose
     * amount is set after the transaction is fulfilled (e.g. when proxy
     * printing is reported as completed).
     * </p>
     *
     * @param account
     *            The {@link Account} to update.
     * @param docLog
     *            The {@link DocLog} to be accounted for.
     * @param trxType
     *            The {@link AccountTrxTypeEnum} of the {@link AccountTrx}.
     * @param weightTotal
     *            The total of all weights in the transaction set.
     * @param weight
     *            The mathematical weight of the to be created transaction in
     *            the context of a transaction set.
     * @param weightUnit
     *            The weight unit.
     * @param extDetails
     *            Free format details from external source.
     */
    private void createAccountTrx(final Account account, final DocLog docLog,
            final AccountTrxTypeEnum trxType, final int weightTotal,
            final int weight, final int weightUnit, final String extDetails) {

        final String actor = ServiceContext.getActor();
        final Date trxDate = ServiceContext.getTransactionDate();

        /*
         * The total transaction amount.
         */
        BigDecimal trxAmount = docLog.getCostOriginal().negate();

        /*
         * Amount is zero for a so-called "shadow" transaction. The zero amount
         * is updated later on with the "real" value after the transaction is
         * fulfilled (e.g. when proxy printing is completed).
         */
        final boolean isZeroAmount = trxAmount.equals(BigDecimal.ZERO);

        if (!isZeroAmount) {

            trxAmount = calcWeightedAmount(trxAmount, weightTotal, weight,
                    weightUnit, ConfigManager.getFinancialDecimalsInDatabase());

            account.setBalance(account.getBalance().add(trxAmount));

            account.setModifiedBy(actor);
            account.setModifiedDate(trxDate);

            accountDAO().update(account);
        }

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
        trx.setTransactionWeightUnit(weightUnit);

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
     * Calculates the media cost of a print job.
     * <p>
     * The pageCostTwoSided is applied to pages that are printed on both sides
     * of a sheet. If a job has an odd number of pages, the pageCostTwoSided is
     * not applied to the last page. For example, if a 3 page document is
     * printed as duplex, the pageCostTwoSided is applied to the first 2 pages:
     * the last page has pageCostOneSided.
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
    public static BigDecimal calcProxyPrintCostMedia(final int nPages,
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

        BigDecimal cost =
                pageCostOneSided.multiply(pagesOneSided).multiply(copies);

        if (pageCostTwoSided != null) {
            cost = cost.add(
                    pageCostTwoSided.multiply(pagesTwoSided).multiply(copies));
        }

        return cost.multiply(BigDecimal.ONE.subtract(discountPerc));
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
                creditLimit = ConfigManager.instance()
                        .getConfigBigDecimal(Key.FINANCIAL_GLOBAL_CREDIT_LIMIT);
            } else {
                creditLimit = account.getOverdraft();
            }

            final BigDecimal balanceAfter = account.getBalance().subtract(cost);

            isChargeable = balanceAfter.compareTo(creditLimit.negate()) >= 0;
        }
        return isChargeable;
    }

    @Override
    public ProxyPrintCostDto calcProxyPrintCost(final Printer printer,
            final ProxyPrintCostParms costParms) {

        BigDecimal pageCostOneSided = BigDecimal.ZERO;
        BigDecimal pageCostTwoSided = BigDecimal.ZERO;

        final IppMediaSourceCostDto mediaSourceCost =
                costParms.getMediaSourceCost();

        if (costParms.getCustomCostMediaSide() != null) {

            pageCostOneSided = costParms.getCustomCostMediaSide();
            pageCostTwoSided = costParms.getCustomCostMediaSideDuplex();

        } else if (mediaSourceCost != null
                && !mediaSourceCost.isManualSource()) {

            final MediaCostDto pageCost =
                    mediaSourceCost.getMedia().getPageCost();

            pageCostOneSided = this.getCost(pageCost.getCostOneSided(),
                    costParms.isGrayscale());

            pageCostTwoSided = this.getCost(pageCost.getCostTwoSided(),
                    costParms.isGrayscale());

        } else {

            switch (printerDAO().getChargeType(printer.getChargeType())) {

            case SIMPLE:

                pageCostOneSided = printer.getDefaultCost();
                pageCostTwoSided = pageCostOneSided;
                break;

            case MEDIA:

                final IppMediaCostDto costDto = printerDAO()
                        .getMediaCost(printer, costParms.getIppMediaOption());

                if (costDto != null) {

                    final MediaCostDto pageCost = costDto.getPageCost();

                    pageCostOneSided = this.getCost(pageCost.getCostOneSided(),
                            costParms.isGrayscale());

                    pageCostTwoSided = this.getCost(pageCost.getCostTwoSided(),
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
            discountPerc = BigDecimal
                    .valueOf(ConfigManager.instance()
                            .getConfigLong(Key.ECO_PRINT_DISCOUNT_PERC, 0L))
                    .divide(BigDecimal.valueOf(100L));
        } else {
            discountPerc = BigDecimal.ZERO;
        }

        //
        final List<Integer> logicalNumberOfPages;

        if (costParms.getLogicalNumberOfPages() == null) {
            logicalNumberOfPages = new ArrayList<>();
            logicalNumberOfPages
                    .add(Integer.valueOf(costParms.getNumberOfPages()));
        } else {
            logicalNumberOfPages = costParms.getLogicalNumberOfPages();
        }

        final ProxyPrintCostDto costResult = new ProxyPrintCostDto();

        /*
         * Media cost.
         */
        BigDecimal costMedia = BigDecimal.ZERO;

        // The number of cover pages, that are not charged as media.
        int coverPagesCounter = costParms.getCustomCoverPrintPages();

        for (final Integer numberOfPages : logicalNumberOfPages) {

            final Integer numberOfPagesCost;

            if (coverPagesCounter > 0) {

                int pagesForCost = numberOfPages.intValue();

                if (pagesForCost < coverPagesCounter) {
                    coverPagesCounter -= pagesForCost;
                    numberOfPagesCost = null;
                } else {
                    pagesForCost -= coverPagesCounter;
                    numberOfPagesCost = Integer.valueOf(pagesForCost);
                    coverPagesCounter = 0;
                }

            } else {
                numberOfPagesCost = numberOfPages;
            }

            if (numberOfPagesCost != null) {
                costMedia = costMedia.add(calcProxyPrintCostMedia(
                        numberOfPagesCost, costParms.getPagesPerSide(),
                        costParms.getNumberOfCopies(), costParms.isDuplex(),
                        pageCostOneSided, pageCostTwoSided, discountPerc));
            }
        }

        costResult.setCostMedia(costMedia);

        /*
         * Set cost.
         */
        if (costParms.getCustomCostSet() != null) {
            costResult.setCostSet(costParms.getCustomCostSet());
        }

        /*
         * Copy cost.
         */
        if (costParms.getCustomCostCopy() != null) {
            costResult.setCostCopy(costParms.getCustomCostCopy().multiply(
                    BigDecimal.valueOf(costParms.getNumberOfCopies())));
        }

        /*
         * Sheet cost.
         */
        if (costParms.getCustomCostSheet() != null) {
            costResult.setCostSheet(costParms.getCustomCostSheet().multiply(
                    BigDecimal.valueOf(costParms.getNumberOfSheets())));
        }

        /*
         * Cover cost.
         */
        final BigDecimal costCover = costParms.getCustomCostCoverPrint();

        if (costCover != null) {

            BigDecimal costCopy = costResult.getCostCopy();

            if (costCopy == null) {
                costCopy = costCover;
            } else {
                costCopy = costCopy.add(costCover);
            }

            costResult.setCostCopy(costCopy);
        }

        return costResult;
    }

    @Override
    public ProxyPrintCostDto calcProxyPrintCost(final Locale locale,
            final String currencySymbol, final User user, final Printer printer,
            final ProxyPrintCostParms costParms,
            final ProxyPrintJobChunkInfo jobChunkInfo)
            throws ProxyPrintException {

        final BigDecimal totalCostSet;

        if (costParms.getCustomCostSet() == null) {
            totalCostSet = BigDecimal.ZERO;
        } else {
            totalCostSet = costParms.getCustomCostSet();
        }

        BigDecimal totalCostMedia = BigDecimal.ZERO;
        BigDecimal totalCostSheet = BigDecimal.ZERO;
        BigDecimal totalCostCopy = BigDecimal.ZERO;

        /*
         * Traverse the chunks and calculate.
         */
        for (final ProxyPrintJobChunk chunk : jobChunkInfo.getChunks()) {

            // Number of sheets
            costParms.setNumberOfSheets(PdfPrintCollector
                    .calcNumberOfPrintedSheets(chunk.getNumberOfPages(),
                            costParms.getNumberOfCopies(), costParms.isDuplex(),
                            costParms.getPagesPerSide(), false, false, false));

            costParms.setNumberOfPages(chunk.getNumberOfPages());
            costParms.setLogicalNumberOfPages(chunk.getLogicalJobPages());
            costParms.setMediaSourceCost(chunk.getAssignedMediaSource());

            costParms.setIppMediaOption(
                    chunk.getAssignedMedia().getIppKeyword());

            costParms.calcCustomCost();

            final ProxyPrintCostDto chunkCostResult =
                    this.calcProxyPrintCost(printer, costParms);

            chunk.setCostResult(chunkCostResult);

            totalCostCopy = totalCostCopy.add(chunkCostResult.getCostCopy());
            totalCostSheet = totalCostSheet.add(chunkCostResult.getCostSheet());
            totalCostMedia = totalCostMedia.add(chunkCostResult.getCostMedia());
        }

        final ProxyPrintCostDto costResult = new ProxyPrintCostDto();

        costResult.setCostSet(totalCostSet);
        costResult.setCostCopy(totalCostCopy);
        costResult.setCostMedia(totalCostMedia);

        validateProxyPrintUserCost(user, costResult.getCostTotal(), locale,
                currencySymbol);

        return costResult;
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
            cost = this.localize(value, ConfigManager.getPrinterCostDecimals(),
                    locale);
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
        final Account account = this
                .lazyGetUserAccount(user, AccountTypeEnum.USER).getAccount();

        final BigDecimal balanceBefore = account.getBalance();
        final BigDecimal balanceAfter = balanceBefore.subtract(cost);

        if (account.getRestricted()) {

            final BigDecimal creditLimit;

            if (account.getUseGlobalOverdraft()) {
                creditLimit = ConfigManager.instance()
                        .getConfigBigDecimal(Key.FINANCIAL_GLOBAL_CREDIT_LIMIT);
            } else {
                creditLimit = account.getOverdraft();
            }

            if (balanceAfter.compareTo(creditLimit.negate()) < 0) {

                if (creditLimit.compareTo(BigDecimal.ZERO) == 0) {

                    throw new ProxyPrintException(
                            this.localize("msg-print-denied-no-balance",
                                    localizedPrinterCost(balanceBefore, locale,
                                            currencySymbol),
                                    localizedPrinterCost(cost, locale,
                                            currencySymbol)));

                } else {
                    throw new ProxyPrintException(this.localize(
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
    public String getFormattedUserBalance(final User user, final Locale locale,
            final String currencySymbol) {

        final String balance;

        if (user.getDeleted()) {

            balance =
                    formatUserBalance(
                            userAccountDAO().findByUserId(user.getId(),
                                    AccountTypeEnum.USER),
                            locale, currencySymbol);
        } else {
            balance = getFormattedUserBalance(user.getUserId(), locale,
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
     *            The {@link Locale}.
     * @param currencySymbol
     *            The currency symbol.
     * @return The formatted balance.
     */
    private String formatUserBalance(final UserAccount userAccount,
            final Locale locale, final String currencySymbol) {

        final BigDecimal balance;

        if (userAccount != null) {
            balance = userAccount.getAccount().getBalance();
        } else {
            balance = BigDecimal.ZERO;
        }

        return formatUserBalance(balance, locale, currencySymbol);
    }

    @Override
    public String formatUserBalance(final BigDecimal balance,
            final Locale locale, final String currencySymbol) {

        final String currencySymbolWrk;

        if (currencySymbol == null) {
            currencySymbolWrk = "";
        } else {
            currencySymbolWrk = currencySymbol;
        }

        try {
            return this.localize(balance,
                    ConfigManager.getUserBalanceDecimals(), locale,
                    currencySymbolWrk);
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
                valueWlk = this.localize(aggr.getAvg().multiply(multiplicand),
                        balanceDecimals, locale, currencySymbol);
            }
            stats.setAvg(valueWlk);

            //
            valueWlk = valueEmpty;
            if (aggr.getMax() != null) {
                valueWlk = this.localize(aggr.getMax().multiply(multiplicand),
                        balanceDecimals, locale, currencySymbol);
            }
            stats.setMax(valueWlk);

            //
            valueWlk = valueEmpty;
            if (aggr.getMin() != null) {
                valueWlk = this.localize(aggr.getMin().multiply(multiplicand),
                        balanceDecimals, locale, currencySymbol);
            }
            stats.setMin(valueWlk);

            //
            valueWlk = valueEmpty;
            if (aggr.getSum() != null) {
                valueWlk = this.localize(aggr.getSum().multiply(multiplicand),
                        balanceDecimals, locale, currencySymbol);
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

        dto.setUserCredit(createFinancialDisplayInfoStats(
                accountDAO().getBalanceStats(true, false), locale,
                currencySymbolWrk, BigDecimal.ONE.negate()));

        dto.setUserDebit(createFinancialDisplayInfoStats(
                accountDAO().getBalanceStats(true, true), locale,
                currencySymbolWrk, BigDecimal.ONE));

        return dto;
    }

    /**
     * Throws an {@link IllegalArgumentException} when account is not of type
     * {@link AccountTypeEnum#SHARED} or {@link AccountTypeEnum#GROUP}.
     *
     * @param account
     *            The account.
     * @return The {@link AccountTypeEnum}.
     */
    private static AccountTypeEnum
            checkAccountSharedOrGroup(final Account account) {

        final AccountTypeEnum accountType = EnumUtils
                .getEnum(AccountTypeEnum.class, account.getAccountType());

        if (accountType != AccountTypeEnum.SHARED
                && accountType != AccountTypeEnum.GROUP) {

            throw new IllegalArgumentException(
                    String.format("Account [%s] : type %s is not allowed.",
                            account.getName(), accountType.toString()));
        }
        return accountType;
    }

    @Override
    public SharedAccountDisplayInfoDto getSharedAccountDisplayInfo(
            final Account account, final Locale locale,
            final String currencySymbol) {

        /*
         * INVARIANT: Account MUST be of type SHARED.
         */
        final AccountTypeEnum accountType = checkAccountSharedOrGroup(account);

        final SharedAccountDisplayInfoDto dto =
                new SharedAccountDisplayInfoDto();

        this.fillAccountDisplayInfo(account, locale, currencySymbol, dto);

        dto.setId(account.getId());
        dto.setName(account.getName());
        dto.setAccountType(accountType);

        if (account.getParent() != null) {
            dto.setParentId(account.getParent().getId());
            dto.setParentName(account.getParent().getName());
        }

        dto.setNotes(account.getNotes());
        dto.setDeleted(account.getDeleted());
        dto.setDisabled(account.getDisabled());

        /*
         * User Group Access
         */
        final ArrayList<UserGroupDto> userGroupsDto = new ArrayList<>();
        dto.setUserGroupAccess(userGroupsDto);

        if (account.getMemberGroups() != null) {

            for (final UserGroupAccount group : account.getMemberGroups()) {

                final UserGroupDto groupDto = new UserGroupDto();
                groupDto.setGroupName(group.getUserGroup().getGroupName());
                userGroupsDto.add(groupDto);
            }
        }

        return dto;
    }

    @Override
    public AbstractJsonRpcMethodResponse
            deleteUserGroupAccount(final String groupName) {

        final Account account = accountDAO().findActiveAccountByName(groupName,
                AccountTypeEnum.GROUP);
        /*
         * INVARIANT: account MUST exist.
         */
        if (account == null) {
            return JsonRpcMethodError.createBasicError(Code.INVALID_REQUEST,
                    String.format("No active User Group Account [%s] present.",
                            groupName),
                    null);
        }

        accountDAO().setLogicalDelete(account,
                ServiceContext.getTransactionDate(), ServiceContext.getActor());

        accountDAO().update(account);

        return JsonRpcMethodResult.createOkResult(String.format(
                "User Group Account [%s] logically deleted.", groupName));
    }

    @Override
    public AbstractJsonRpcMethodResponse
            lazyUpdateSharedAccount(final SharedAccountDisplayInfoDto dto) {

        /*
         * INVARIANT: Account name MUST be present.
         */
        if (StringUtils.isBlank(dto.getName())) {
            return createErrorMsg("msg-shared-account-name-needed");
        }

        /*
         * INVARIANT: Account name syntax MUST be valid.
         */
        final CharSequence reservedNameChars =
                String.valueOf(PaperCutHelper.COMPOSED_ACCOUNT_NAME_SEPARATOR);

        if (StringUtils.containsAny(reservedNameChars, dto.getName())) {
            return createErrorMsg("msg-shared-account-name-syntax-error",
                    reservedNameChars.toString());
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
            parent = accountDAO()
                    .findActiveSharedAccountByName(dto.getParentName());

            if (parent == null) {
                return createErrorMsg("msg-shared-account-parent-unknown");
            }
        }

        //
        final Account account;
        final boolean newAccount = dto.getId() == null;

        final String prevAccountName;

        if (newAccount) {
            prevAccountName = dto.getName();
            account = this.createSharedAccountTemplate(dto.getName(), parent);
        } else {
            account = accountDAO().findById(dto.getId());
            prevAccountName = account.getName();
        }

        /*
         * INVARIANT: Account MUST exist.
         */
        if (account == null) {
            throw new IllegalArgumentException(String
                    .format("Account [%s] can not be found.", dto.getName()));
        }

        /*
         * INVARIANT: Account MUST be of type SHARED or GROUP.
         */
        final AccountTypeEnum accountType = checkAccountSharedOrGroup(account);

        /*
         * Is this a top account?
         */
        final boolean topAccount = account.getParent() == null;

        /*
         * INVARIANT: Group Account MUST be a top account.
         */
        if (accountType == AccountTypeEnum.GROUP && !topAccount) {
            throw new IllegalArgumentException(
                    String.format("No parent account allowed for account [%s]",
                            dto.getName()));
        }
        /*
         * INVARIANT: Name of Group Account can NOT be changed (must be the same
         * initial name).
         */
        if (accountType == AccountTypeEnum.GROUP
                && !prevAccountName.equals(dto.getName())) {
            throw new IllegalArgumentException(String.format(
                    "Name of account [%s] can not be changed.", dto.getName()));
        }

        final Account accountDuplicate;

        if (parent == null) {
            /*
             * INVARIANT: Top Account name MUST be case insensitive unique among
             * top accounts.
             */
            accountDuplicate = accountDAO()
                    .findActiveAccountByName(dto.getName(), accountType);
        } else {
            /*
             * INVARIANT: Account name must be case insensitive unique among
             * sibling sub accounts.
             */
            accountDuplicate = accountDAO().findActiveSharedChildAccountByName(
                    parent.getId(), dto.getName());
        }

        if (accountDuplicate != null
                && !accountDuplicate.getId().equals(dto.getId())) {
            return createErrorMsg(
                    this.localize("msg-shared-account-name-in-use"));
        }

        /*
         * Logical delete.
         */
        if (dto.getDeleted() != null && dto.getDeleted()) {

            accountDAO().setLogicalDelete(account,
                    ServiceContext.getTransactionDate(),
                    ServiceContext.getActor());

            int nUpdated = 1;

            if (topAccount) {
                nUpdated += accountDAO().logicalDeleteSubAccounts(
                        account.getId(), ServiceContext.getTransactionDate(),
                        ServiceContext.getActor());
            }

            accountDAO().update(account);
            return createOkResult("msg-shared-accounts-deleted",
                    String.valueOf(nUpdated));
        }

        /*
         * INVARIANT: An account that has sub accounts can NOT have a parent
         * account.
         */
        if (!newAccount && StringUtils.isNotBlank(dto.getParentName())
                && accountDAO().countSubAccounts(account.getId()) > 0) {
            return createErrorMsg("msg-shared-account-cannot-have-parent",
                    account.getName());
        }

        account.setName(dto.getName().trim());
        account.setNameLower(dto.getName().trim().toLowerCase());
        account.setParent(parent);
        account.setNotes(dto.getNotes());
        account.setDeleted(dto.getDeleted());
        account.setDisabled(BooleanUtils.isTrue(dto.getDisabled()));

        final Locale dtoLocale;

        if (dto.getLocaleLanguage() != null && dto.getLocaleCountry() != null) {
            dtoLocale =
                    new Locale(dto.getLocaleLanguage(), dto.getLocaleCountry());
        } else {
            dtoLocale = ServiceContext.getLocale();
        }

        final BigDecimal balanceNew;
        final String balance = dto.getBalance();
        try {
            if (!BigDecimalUtil.isValid(balance, dtoLocale, false)) {
                throw new ParseException(balance, 0);
            }
            balanceNew = BigDecimalUtil.parse(balance, dtoLocale, false,
                    CURRENCY_AMOUNT_GROUPING_USED);
        } catch (ParseException e) {
            return createErrorMsg("msg-amount-error", balance);
        }

        //
        if (newAccount) {
            account.setBalance(balanceNew);
            accountDAO().create(account);
        } else {
            checkCreateAccountTrx(account, balanceNew, null);
            accountDAO().update(account);
        }

        /*
         * UserGroup access.
         */
        final List<UserGroupDto> userGroupList = dto.getUserGroupAccess();

        if (userGroupList != null) {

            JsonRpcMethodError error =
                    setUserGroupAccess(account, userGroupList);

            if (error != null) {
                return error;
            }
        }

        return JsonRpcMethodResult.createOkResult();
    }

    /**
     * Sets {@link UserGroup} access at an Account (creates new
     * {@link UserGroupAccount} objects and removes obsolete ones).
     *
     * @param account
     *            The account.
     * @param userGroupList
     *            The access list to set.
     * @return {@code null} when no error.
     */
    private JsonRpcMethodError setUserGroupAccess(final Account account,
            final List<UserGroupDto> userGroupList) {
        /*
         * Create sorted lists for balanced line.
         */
        final SortedMap<String, UserGroupDto> sortedMemberGroupsDto =
                new TreeMap<>();

        for (final UserGroupDto dto : userGroupList) {
            sortedMemberGroupsDto.put(dto.getGroupName().trim(), dto);
        }

        // Get (lazy initialize) the current member groups list.
        List<UserGroupAccount> memberGroups = account.getMemberGroups();

        if (memberGroups == null) {
            memberGroups = new ArrayList<>();
            account.setMemberGroups(memberGroups);
        }

        // Sorted map of current UserGroup members.
        final SortedMap<String, UserGroupAccount> sortedMemberGroups =
                new TreeMap<>();

        for (final UserGroupAccount userGroupAccount : memberGroups) {
            sortedMemberGroups.put(
                    userGroupAccount.getUserGroup().getGroupName(),
                    userGroupAccount);
        }
        /*
         * Balanced line.
         */
        final Iterator<Entry<String, UserGroupAccount>> iterMemberGroups =
                sortedMemberGroups.entrySet().iterator();

        final Iterator<Entry<String, UserGroupDto>> iterMemberGroupsDto =
                sortedMemberGroupsDto.entrySet().iterator();

        boolean isUpdated = false;

        UserGroupDto userGroupDtoWlk = null;
        UserGroupAccount userGroupAccountWlk = null;

        if (iterMemberGroups.hasNext()) {
            userGroupAccountWlk = iterMemberGroups.next().getValue();
        }

        if (iterMemberGroupsDto.hasNext()) {
            userGroupDtoWlk = iterMemberGroupsDto.next().getValue();
        }

        // Balanced line: process.
        while (userGroupAccountWlk != null || userGroupDtoWlk != null) {

            boolean readNextUserGroupAccount = false;
            boolean readNextUserGroupDto = false;

            String groupNameToAdd = null;

            if (userGroupDtoWlk != null && userGroupAccountWlk != null) {

                final String keyUserGroup =
                        userGroupAccountWlk.getUserGroup().getGroupName();

                final String keyDto = userGroupDtoWlk.getGroupName();

                final int compare = keyUserGroup.compareTo(keyDto);

                if (compare < 0) {
                    // keyUserGroup < keyDto : Remove UserGroupAccount.
                    userGroupAccountDAO().delete(userGroupAccountWlk);
                    memberGroups.remove(userGroupAccountWlk);
                    isUpdated = true;
                    readNextUserGroupAccount = true;

                } else if (compare > 0) {
                    // keyUserGroup > keyDto : Add UserGroup from dto
                    groupNameToAdd = keyDto;
                    readNextUserGroupDto = true;

                } else {
                    // keyUserGroup == keyDto : no update.
                    readNextUserGroupDto = true;
                    readNextUserGroupAccount = true;
                }

            } else if (userGroupDtoWlk != null) {
                // Add UserGroupAccount.
                groupNameToAdd = userGroupDtoWlk.getGroupName();
                readNextUserGroupDto = true;

            } else {
                // Remove UserGroupAccount.
                userGroupAccountDAO().delete(userGroupAccountWlk);
                memberGroups.remove(userGroupAccountWlk);
                isUpdated = true;

                readNextUserGroupAccount = true;
            }
            //
            if (groupNameToAdd != null) {
                final UserGroupAccount userGroupAccount =
                        new UserGroupAccount();

                final UserGroup userGroup =
                        userGroupDAO().findByName(groupNameToAdd);

                if (userGroup == null) {
                    return createError("msg-user-group-name-unknown",
                            groupNameToAdd);
                }

                userGroupAccount.setAccount(account);
                userGroupAccount.setUserGroup(userGroup);

                userGroupAccount.setCreatedBy(ServiceContext.getActor());
                userGroupAccount
                        .setCreatedDate(ServiceContext.getTransactionDate());

                userGroupAccountDAO().create(userGroupAccount);
                memberGroups.add(userGroupAccount);

                isUpdated = true;
            }
            /*
             * Next read(s).
             */
            if (readNextUserGroupAccount) {
                userGroupAccountWlk = null;
                if (iterMemberGroups.hasNext()) {
                    userGroupAccountWlk = iterMemberGroups.next().getValue();
                }
            }
            if (readNextUserGroupDto) {
                userGroupDtoWlk = null;
                if (iterMemberGroupsDto.hasNext()) {
                    userGroupDtoWlk = iterMemberGroupsDto.next().getValue();
                }
            }
        } // end-while

        if (isUpdated) {
            accountDAO().update(account);
        }
        return null;
    }

    @Override
    public AccountDisplayInfoDto getAccountDisplayInfo(final User user,
            final Locale locale, final String currencySymbol) {

        final AccountDisplayInfoDto dto = new AccountDisplayInfoDto();

        final Account account = this
                .lazyGetUserAccount(user, AccountTypeEnum.USER).getAccount();

        this.fillAccountDisplayInfo(account, locale, currencySymbol, dto);

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
    private void fillAccountDisplayInfo(final Account account,
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
                creditLimit = ConfigManager.instance()
                        .getConfigBigDecimal(Key.FINANCIAL_GLOBAL_CREDIT_LIMIT);
            } else {
                creditLimit = account.getOverdraft();
            }

            try {
                formattedCreditLimit = this.localize(creditLimit,
                        balanceDecimals, locale, currencySymbolWrk);
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
            dto.setBalance(this.localize(account.getBalance(), balanceDecimals,
                    locale, currencySymbolWrk));
        } catch (ParseException e) {
            throw new SpException(e);
        }

        dto.setLocaleLanguage(locale.getLanguage());
        dto.setLocaleCountry(locale.getCountry());

        dto.setCreditLimit(formattedCreditLimit);
        dto.setStatus(status);
    }

    @Override
    public AbstractJsonRpcMethodResponse
            redeemVoucher(final AccountVoucherRedeemDto dto) {

        final AccountVoucher voucher =
                accountVoucherDAO().findByCardNumber(dto.getCardNumber());

        final Date redeemDate = new Date(dto.getRedeemDate().longValue());

        /*
         * INVARIANT: cardNumber MUST exist, MUST NOT already be redeemed, and
         * MUST NOT be expired on the redeem date.
         */
        if (voucher == null || voucher.getRedeemedDate() != null
                || accountVoucherService().isVoucherExpired(voucher,
                        redeemDate)) {
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
        final Account account = this
                .lazyGetUserAccount(user, AccountTypeEnum.USER).getAccount();

        account.setBalance(account.getBalance().add(voucher.getValueAmount()));
        account.setModifiedBy(ServiceContext.getActor());
        account.setModifiedDate(ServiceContext.getTransactionDate());

        accountDAO().update(account);

        /*
         * Create transaction.
         */
        final String comment = this.localize("msg-voucher-redeem-trx-comment",
                dto.getCardNumber());

        final AccountTrx accountTrx = this.createAccountTrx(account,
                AccountTrxTypeEnum.VOUCHER, voucher.getValueAmount(),
                account.getBalance(), comment);

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
        final Account account = this
                .lazyGetUserAccount(user, AccountTypeEnum.USER).getAccount();

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
            throw new AccountingException(
                    String.format("Transaction %s is already accepted.",
                            dto.getTransactionId()));
        }

        final Account account = trx.getAccount();

        account.setBalance(
                account.getBalance().add(dto.getAmountAcknowledged()));
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
            account = this.lazyGetUserAccount(user, AccountTypeEnum.USER)
                    .getAccount();
        }

        account.setBalance(
                account.getBalance().add(dto.getAmountAcknowledged()));
        account.setModifiedBy(ServiceContext.getActor());
        account.setModifiedDate(ServiceContext.getTransactionDate());

        /*
         * Create and fill transaction.
         */
        final AccountTrx trx = this.createAccountTrx(account,
                AccountTrxTypeEnum.GATEWAY, dto.getAmountAcknowledged(),
                account.getBalance(), dto.getComment());

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
        final AccountTrx accountTrx = this.createAccountTrx(account,
                accountTrxType, amount, account.getBalance(), comment);

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

    /**
     * Validates POS funds.
     *
     * @param user
     *            User.
     * @param userId
     *            User ID.
     * @param formattedAmount
     *            Decimal point amount.
     * @return {@code null} if valid.
     */
    private JsonRpcMethodError validatePosFunds(final User user,
            final String userId, final String formattedAmount) {
        /*
         * INVARIANT: User MUST exist.
         */
        if (user == null) {
            return createErrorMsg(MSG_KEY_POS_USER_UNKNOWN, userId);
        }
        return validatePosFunds(userId, formattedAmount);
    }

    /**
     * Validates POS funds.
     *
     * @param userId
     *            User ID.
     * @param formattedAmount
     *            Decimal point amount.
     * @return {@code null} if valid.
     */
    private JsonRpcMethodError validatePosFunds(final String userId,
            final String formattedAmount) {
        /*
         * INVARIANT: Amount MUST be valid.
         */
        if (!BigDecimalUtil.isValid(formattedAmount)) {
            return createError(MSG_KEY_POS_AMOUNT_ERROR);
        }
        /*
         * INVARIANT: Amount MUST be GT zero.
         */
        if (BigDecimalUtil.valueOf(formattedAmount)
                .compareTo(BigDecimal.ZERO) <= 0) {
            return createError(MSG_KEY_POS_AMOUNT_INVALID);
        }
        return null;
    }

    @Override
    public AbstractJsonRpcMethodResponse depositFunds(final PosDepositDto dto) {
        return handlePosTransaction(dto, ReceiptNumberPrefixEnum.DEPOSIT,
                AccountTrxTypeEnum.DEPOSIT, false, dto.getPaymentType());
    }

    @Override
    public AbstractJsonRpcMethodResponse chargePosSales(final PosSalesDto dto) {
        return handlePosTransaction(dto, ReceiptNumberPrefixEnum.PURCHASE,
                AccountTrxTypeEnum.PURCHASE, true, null);
    }

    @Override
    public AbstractJsonRpcMethodResponse chargePosSales(
            final PaperCutServerProxy proxy, final PosSalesDto dto) {

        final String formattedAmount = dto.formatAmount();

        final JsonRpcMethodError error =
                this.validatePosFunds(dto.getUserId(), formattedAmount);
        if (error != null) {
            return error;
        }

        final BigDecimal adjustment = new BigDecimal(formattedAmount).negate();

        try {
            if (!paperCutService().adjustUserAccountBalanceIfAvailable(proxy,
                    dto.getUserId(), null, adjustment, dto.getComment())) {
                return createErrorMsg(MSG_KEY_POS_CREDIT_INSUFFICIENT,
                        dto.getUserId());
            }
        } catch (PaperCutException e) {
            return createErrorMsg(e.getMessage());
        }
        return JsonRpcMethodResult.createOkResult();
    }

    /**
     * Handles a POS transaction.
     *
     * @param dto
     *            Transaction.
     * @param receiptNumberPrefix
     * @param accountTrxType
     * @param withdrawal
     * @param paymentType
     * @return response.
     */
    private AbstractJsonRpcMethodResponse handlePosTransaction(
            final PosTransactionDto dto,
            final ReceiptNumberPrefixEnum receiptNumberPrefix,
            final AccountTrxTypeEnum accountTrxType, final boolean withdrawal,
            final String paymentType) {

        final User user = userDAO().findActiveUserByUserId(dto.getUserId());
        final String formattedAmount = dto.formatAmount();

        final JsonRpcMethodError error =
                this.validatePosFunds(user, dto.getUserId(), formattedAmount);
        if (error != null) {
            return error;
        }

        final BigDecimal amount = BigDecimalUtil.valueOf(formattedAmount);
        final BigDecimal amountDeposit;
        if (withdrawal) {
            amountDeposit = amount.negate();
        } else {
            amountDeposit = amount;
        }
        final Account account = this
                .lazyGetUserAccount(user, AccountTypeEnum.USER).getAccount();

        final String receiptNumber =
                purchaseDAO().getNextReceiptNumber(receiptNumberPrefix);

        return depositFundsToAccount(account, accountTrxType, null,
                receiptNumber, amountDeposit, dto.getComment());
    }

    @Override
    public PosDepositReceiptDto
            createPosDepositReceiptDto(final Long accountTrxId) {

        final AccountTrx accountTrx = accountTrxDAO().findById(accountTrxId);

        if (accountTrx == null) {
            throw new SpException("Transaction not found.");
        }

        if (!accountTrx.getTrxType()
                .equals(AccountTrxTypeEnum.DEPOSIT.toString())) {
            throw new SpException("This is not a DEPOSIT transaction.");
        }

        final PosPurchase purchase = accountTrx.getPosPurchase();

        final User user = userAccountDAO()
                .findByAccountId(accountTrx.getAccount().getId()).getUser();

        //
        final PosDepositReceiptDto receipt = new PosDepositReceiptDto();

        receipt.setAccountTrx(accountTrx);

        receipt.setComment(purchase.getComment());
        receipt.setPlainAmount(
                BigDecimalUtil.toPlainString(accountTrx.getAmount()));
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
            final DaoBatchCommitter batchCommitter, final Currency currencyFrom,
            final Currency currencyTo, final double exchangeRate) {

        int nAccounts = 0;
        int nTrx = 0;

        /*
         * INVARIANT: currencyFrom must match current currency.
         */
        if (!ConfigManager.getAppCurrencyCode()
                .equals(currencyFrom.getCurrencyCode())) {

            final String err = String.format(
                    "Currency %s does not match current " + "base currency %s.",
                    currencyFrom.getCurrencyCode(),
                    ConfigManager.getAppCurrencyCode());

            return JsonRpcMethodError.createBasicError(Code.INVALID_REQUEST,
                    err, null);
        }

        /*
         * INVARIANT: currencyFrom and currencyTo must differ.
         */
        if (currencyFrom.getCurrencyCode()
                .equals(currencyTo.getCurrencyCode())) {
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

        final List<Account> list = accountDAO().getListChunk(filter,
                Integer.valueOf(startPosition), maxResults,
                AccountDao.Field.ACCOUNT_TYPE, true);

        final BigDecimal exchangeDecimal = BigDecimal.valueOf(exchangeRate);

        for (final Account account : list) {

            int nChanges = 0;

            // Overdraft
            if (account.getOverdraft().compareTo(BigDecimal.ZERO) != 0) {
                account.setOverdraft(
                        account.getOverdraft().multiply(exchangeDecimal));
                nChanges++;
            }

            final BigDecimal balance = account.getBalance();

            // Balance
            if (balance.compareTo(BigDecimal.ZERO) != 0) {

                // Reverse current balance currency.
                final AccountTrx trxReversal = this.createAccountTrx(account,
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

                final AccountTrx trxInit = this.createAccountTrx(account,
                        AccountTrxTypeEnum.ADJUST, balanceInit, balanceInit,
                        comment.toString());

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

        result.append(this.localize("msg-changed-base-currency",
                currencyFrom.getCurrencyCode(), currencyTo.getCurrencyCode(),
                Double.valueOf(exchangeRate).toString(),
                Integer.valueOf(nTrx).toString(),
                Integer.valueOf(nAccounts).toString()));

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
            final AccountTypeEnum accountType, final AccountTrxTypeEnum trxType,
            final String currencyCode, final BigDecimal amount,
            final String trxComment) {

        final Account account =
                this.lazyGetUserAccount(user, accountType).getAccount();

        account.setBalance(account.getBalance().add(amount));
        account.setModifiedBy(ServiceContext.getActor());
        account.setModifiedDate(ServiceContext.getTransactionDate());

        final AccountTrx trx = this.createAccountTrx(account, trxType,
                currencyCode, amount, account.getBalance(), trxComment);

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
    public AbstractJsonRpcMethodResponse
            transferUserCredit(final UserCreditTransferDto dto) {

        /*
         * INVARIANT: Amount MUST be valid.
         */
        final String plainAmount =
                dto.getAmountMain() + "." + dto.getAmountCents();

        if (!BigDecimalUtil.isValid(plainAmount)) {

            return JsonRpcMethodError.createBasicError(Code.INVALID_PARAMS,
                    this.localize("msg-amount-error", plainAmount));
        }

        final BigDecimal transferAmount = BigDecimalUtil.valueOf(plainAmount);

        /*
         * INVARIANT: Amount MUST be GT zero.
         */
        if (transferAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return JsonRpcMethodError.createBasicError(Code.INVALID_PARAMS,
                    this.localize("msg-amount-must-be-positive"));
        }

        /*
         * INVARIANT: MUST transfer to another user.
         */
        if (dto.getUserIdFrom().equalsIgnoreCase(dto.getUserIdTo())) {
            return JsonRpcMethodError.createBasicError(Code.INVALID_PARAMS,
                    this.localize("msg-user-credit-transfer-err-user-same"));
        }

        /*
         * INVARIANT: Source and target user MUST exist.
         */
        final User lockedUserTo = userService().lockByUserId(dto.getUserIdTo());
        final User lockedUserFrom =
                userService().lockByUserId(dto.getUserIdFrom());

        if (lockedUserFrom == null || lockedUserTo == null) {

            final String msg;

            if (lockedUserFrom != null) {
                msg = this.localize("msg-user-credit-transfer-err-unknown-user",
                        dto.getUserIdTo());
            } else if (lockedUserTo != null) {
                msg = this.localize("msg-user-credit-transfer-err-unknown-user",
                        dto.getUserIdFrom());
            } else {
                msg = this.localize(
                        "msg-user-credit-transfer-err-unknown-users",
                        dto.getUserIdFrom(), dto.getUserIdTo());
            }

            return JsonRpcMethodError.createBasicError(Code.INVALID_PARAMS,
                    msg);
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
            return JsonRpcMethodError.createBasicError(Code.INVALID_PARAMS, this
                    .localize("msg-user-credit-transfer-err-amount-greater"));
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

                return JsonRpcMethodError.createBasicError(Code.INVALID_PARAMS,
                        this.localize("msg-amount-must-be-gt-eq", this.localize(
                                minimalTransfer, userBalanceDecimals,
                                ServiceContext.getLocale(),
                                ServiceContext.getAppCurrencySymbol())));
            }

            final BigDecimal maximalTransfer =
                    ConfigManager.instance().getConfigBigDecimal(
                            Key.FINANCIAL_USER_TRANSFER_AMOUNT_MAX);

            if (maximalTransfer != null
                    && transferAmount.compareTo(maximalTransfer) > 0) {

                return JsonRpcMethodError.createBasicError(Code.INVALID_PARAMS,
                        this.localize("msg-amount-must-be-lt-eq", this.localize(
                                maximalTransfer, userBalanceDecimals,
                                ServiceContext.getLocale(),
                                ServiceContext.getAppCurrencySymbol())));
            }
        } catch (ParseException e) {
            throw new SpException(e.getMessage(), e);
        }

        /*
         * Transfer funds.
         */
        final String currencyCode = ConfigManager.getAppCurrencyCode();

        final StringBuilder trxComment =
                new StringBuilder().append(Messages.getSystemMessage(getClass(),
                        "msg-user-credit-transfer-comment", dto.getUserIdFrom(),
                        dto.getUserIdTo()));

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
                    this.localize("msg-user-credit-transfer-for-user",
                            this.localize(transferAmount, userBalanceDecimals,
                                    ServiceContext.getLocale(),
                                    ServiceContext.getAppCurrencySymbol()),
                            dto.getUserIdTo());

            AdminPublisher.instance().publish(PubTopicEnum.USER,
                    PubLevelEnum.INFO,
                    Messages.getSystemMessage(getClass(),
                            "msg-user-credit-transfer-for-system",
                            dto.getUserIdFrom(),
                            this.localize(transferAmount, userBalanceDecimals,
                                    ConfigManager.getDefaultLocale(),
                                    ConfigManager.getAppCurrencyCode()),
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
