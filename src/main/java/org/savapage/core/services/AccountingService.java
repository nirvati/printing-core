/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2014 Datraverse B.V.
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
package org.savapage.core.services;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Locale;

import org.savapage.core.concurrent.ReadWriteLockEnum;
import org.savapage.core.dao.PrinterDao;
import org.savapage.core.dao.helpers.AccountTrxTypeEnum;
import org.savapage.core.dao.helpers.DaoBatchCommitter;
import org.savapage.core.dto.AccountDisplayInfoDto;
import org.savapage.core.dto.AccountVoucherRedeemDto;
import org.savapage.core.dto.FinancialDisplayInfoDto;
import org.savapage.core.dto.PosDepositDto;
import org.savapage.core.dto.PosDepositReceiptDto;
import org.savapage.core.dto.SharedAccountDisplayInfoDto;
import org.savapage.core.dto.UserAccountingDto;
import org.savapage.core.dto.UserCreditTransferDto;
import org.savapage.core.dto.UserPaymentGatewayDto;
import org.savapage.core.jpa.Account;
import org.savapage.core.jpa.Account.AccountTypeEnum;
import org.savapage.core.jpa.AccountTrx;
import org.savapage.core.jpa.DocLog;
import org.savapage.core.jpa.Printer;
import org.savapage.core.jpa.User;
import org.savapage.core.jpa.UserAccount;
import org.savapage.core.json.rpc.AbstractJsonRpcMethodResponse;
import org.savapage.core.json.rpc.JsonRpcMethodResult;
import org.savapage.core.json.rpc.JsonRpcResult;
import org.savapage.core.json.rpc.impl.ResultPosDeposit;
import org.savapage.core.print.proxy.ProxyPrintException;
import org.savapage.core.print.proxy.ProxyPrintJobChunk;
import org.savapage.core.print.proxy.ProxyPrintJobChunkInfo;
import org.savapage.core.services.helpers.AccountTrxInfoSet;
import org.savapage.core.services.helpers.ProxyPrintCostParms;

/**
 * Accounting services supporting the pay-per-print solution.
 *
 * @author Datraverse B.V.
 *
 */
public interface AccountingService {

    /**
     * Key of entry in message.xml file.
     * <p>
     * Used for {@link #redeemVoucher(AccountVoucherRedeemDto)} when INVARIANT
     * below is violated.
     * </p>
     * <p>
     * INVARIANT: cardNumber MUST exist, MUST NOT already be redeemed, and MUST
     * NOT be expired on the redeem date.
     * </p>
     */
    String MSG_KEY_VOUCHER_REDEEM_NUMBER_INVALID =
            "msg-voucher-redeem-number-invalid";

    /**
     * Key of entry in message.xml file.
     */
    String MSG_KEY_VOUCHER_REDEEM_USER_UNKNOWN =
            "msg-voucher-redeem-user-unknown";

    /**
     * Key of entry in message.xml file.
     */
    String MSG_KEY_DEPOSIT_FUNDS_USER_UNKNOWN =
            "msg-deposit-funds-user-unknown";

    /**
     * Key of entry in message.xml file.
     */
    String MSG_KEY_DEPOSIT_FUNDS_AMOUNT_ERROR =
            "msg-deposit-funds-amount-error";

    /**
     * Key of entry in message.xml file.
     */
    String MSG_KEY_DEPOSIT_FUNDS_AMOUNT_INVALID =
            "msg-deposit-funds-amount-invalid";

    /**
     * Gets the accounting parameters for a {@link User}. A {@link UserAccount}
     * is lazy created when needed.
     *
     * @param user
     *            The {@link User}.
     * @return The {@link UserAccountingDto}.
     */
    UserAccountingDto getUserAccounting(User user);

    /**
     * Gets the formatted balance of a {@link User}.
     * <p>
     * Note: {@link UserAccount} is NOT lazy created.
     * </p>
     *
     * @param user
     *            The non-null {@link User}.
     * @param locale
     *            The {@link Locale} for formatting.
     * @param currencySymbol
     *            The currency symbol (can be {@code null}).
     * @return Zero balance when {@link UserAccount} is NOT present.
     */
    String getFormattedUserBalance(User user, Locale locale,
            String currencySymbol);

    /**
     * Gets the formatted balance of an active (non-deleted) user.
     * <p>
     * Note: {@link UserAccount} is NOT lazy created.
     * </p>
     *
     * @param userId
     *            The unique user id.
     * @param locale
     *            The {@link Locale} for formatting.
     * @param currencySymbol
     *            The currency symbol (can be {@code null}).
     * @return Zero balance when {@link UserAccount} is NOT present (yet).
     */
    String getFormattedUserBalance(String userId, Locale locale,
            String currencySymbol);

    /**
     * Gets the {@link Account} information of a {@link User} meant for display.
     * A {@link UserAccount} is lazy created when needed.
     *
     * @param user
     *            The {@link User}.
     * @param locale
     *            The {@link Locale} used for formatting financial data.
     * @param currencySymbol
     *            {@code null} or empty when not applicable.
     * @return The {@link AccountDisplayInfoDto} object.
     */
    AccountDisplayInfoDto getAccountDisplayInfo(User user, Locale locale,
            String currencySymbol);

    /**
     * Gets the shared {@link Account} information of an {@link Account} meant
     * for display.
     *
     * @param account
     *            The shared {@link Account}.
     * @param locale
     *            The {@link Locale} used for formatting financial data.
     * @param currencySymbol
     *            {@code null} or empty when not applicable.
     * @return The {@link SharedAccountDisplayInfoDto} object.
     */
    SharedAccountDisplayInfoDto getSharedAccountDisplayInfo(Account account,
            Locale locale, String currencySymbol);

    /**
     * Updates or creates a shared {@link Account} from user input.
     *
     * @param dto
     *            {@link SharedAccountDisplayInfoDto} object
     * @return The JSON-RPC Return message (either a result or an error);
     */
    AbstractJsonRpcMethodResponse lazyUpdateSharedAccount(
            SharedAccountDisplayInfoDto dto);

    /**
     * Gets global financial information meant for display.
     *
     * @param locale
     *            The {@link Locale} used for formatting financial data.
     * @param currencySymbol
     *            {@code null} or empty when not applicable.
     * @return The {@link FinancialDisplayInfoDto} object.
     */
    FinancialDisplayInfoDto getFinancialDisplayInfo(Locale locale,
            String currencySymbol);

    /**
     * Sets the accounting parameters for a {@link User}. A {@link UserAccount}
     * is lazy created when needed.
     *
     * @param user
     *            The {@link User}.
     * @param dto
     *            The accounting parameters.
     */
    public AbstractJsonRpcMethodResponse setUserAccounting(User user,
            UserAccountingDto dto);

    /**
     * Gets the {@link PrinterDao.CostMediaAttr} of the default media.
     *
     * @return
     */
    PrinterDao.CostMediaAttr getCostMediaAttr();

    /**
     * Gets the {@link PrinterDao.CostMediaAttr} of the IPP media.
     *
     * @param ippMediaName
     *            The name of the IPP media.
     * @return
     */
    PrinterDao.CostMediaAttr getCostMediaAttr(String ippMediaName);

    /**
     * Gets the {@link PrinterDao.MediaSourceAttr} of the IPP media-source.
     *
     * @param ippMediaSourceName
     *            The name of the IPP media-source.
     * @return
     */
    PrinterDao.MediaSourceAttr getMediaSourceAttr(String ippMediaSourceName);

    /**
     * Calculates the cost for a proxy print request.
     *
     * @param printer
     *            The proxy {@link Printer}.
     * @param costParms
     *            The {@link ProxyPrintCostParms}.
     * @return The cost as {@link BigDecimal}.
     */
    BigDecimal
            calcProxyPrintCost(Printer printer, ProxyPrintCostParms costParms);

    /**
     * Calculates the cost for each individual {@link ProxyPrintJobChunk} in the
     * {@link ProxyPrintJobChunkInfo}.
     * <p>
     * This method checks if total cost exceeds user's credit limit: when
     * exceeded a {@link ProxyPrintException} is thrown.
     * </p>
     *
     * @param locale
     *            The {@link Locale} used for the message in a
     *            {@link ProxyPrintException},
     * @param currencySymbol
     *            The currency symbol.
     * @param user
     *            The {@link User}.
     * @param printer
     *            The {@link Printer}.
     * @param costParms
     *            The {@link ProxyPrintCostParms}.
     * @param jobChunkInfo
     *            The {@link ProxyPrintJobChunkInfo}.
     * @return The calculated cost total (sum of all chunks).
     * @throws ProxyPrintException
     *             When total cost exceeds user's credit limit.
     */
    BigDecimal calcProxyPrintCost(Locale locale, String currencySymbol,
            User user, Printer printer, ProxyPrintCostParms costParms,
            ProxyPrintJobChunkInfo jobChunkInfo) throws ProxyPrintException;

    /**
     * Checks if cost exceeds user's credit limit: when exceeded a
     * {@link ProxyPrintException} is thrown.
     *
     * @param user
     *            The {@link User}.
     * @param cost
     *            The cost.
     * @param locale
     *            The {@link Locale} used for the message in a
     *            {@link ProxyPrintException},
     * @param currencySymbol
     *            The currency symbol.
     * @throws ProxyPrintException
     *             When total cost exceeds user's credit limit.
     */
    void validateProxyPrintUserCost(User user, BigDecimal cost, Locale locale,
            String currencySymbol) throws ProxyPrintException;

    /**
     * Gets the {@link UserAccount} account of an active (non-deleted) user.
     *
     * @param userId
     *            The unique user id.
     * @param accountType
     *            The {@link Account.AccountTypeEnum}.
     * @return {@code null} when not found.
     */
    UserAccount getActiveUserAccount(String userId,
            Account.AccountTypeEnum accountType);

    /**
     * Gets the {@link UserAccount} account of a {@link User}. The
     * {@link UserAccount} and related {@link Account} are ad-hoc created when
     * they do not exist.
     *
     * @param user
     *            The {@link User}.
     * @param accountType
     *            The {@link Account.AccountTypeEnum}.
     * @return The {@link UserAccount}.
     */
    UserAccount lazyGetUserAccount(User user,
            Account.AccountTypeEnum accountType);

    /**
     * Gets an <i>active</i> shared {@link Account} by name. If it does not
     * exist it is created according to the offered template.
     * <p>
     * Note: the template MUST be of type {@link AccountTypeEnum#SHARED}.
     * </p>
     *
     * @param accountName
     *            The unique active account name.
     * @param accountTemplate
     *            The template used to created a new {@link Account}.
     * @return The {@link Account}.
     */
    Account lazyGetSharedAccount(String accountName, Account accountTemplate);

    /**
     * Creates an {@link AccountTrx} of {@link AccountTrx.AccountTrxTypeEnum}
     * and updates the {@link Account}.
     *
     * @param account
     *            The {@link Account} to update.
     * @param docLog
     *            The {@link DocLog} to be accounted for.
     * @param trxType
     *            The {@link AccountTrxTypeEnum} of the {@link AccountTrx}.
     */
    void createAccountTrx(Account account, DocLog docLog,
            AccountTrxTypeEnum trxType);

    /**
     * Uses the {@link AccountTrxInfoSet} to create {@link AccountTrx} objects
     * of {@link AccountTrx.AccountTrxTypeEnum} and update each corresponding
     * {@link Account} in the database.
     *
     * @param accountTrxInfoSet
     *            The {@link AccountTrxInfoSet}.
     * @param docLog
     *            The {@link DocLog} to be accounted for.
     * @param trxType
     *            The {@link AccountTrxTypeEnum} of the {@link AccountTrx}.
     */
    void createAccountTrxs(AccountTrxInfoSet accountTrxInfoSet, DocLog docLog,
            AccountTrxTypeEnum trxType);

    /**
     * Calculates the weighted amount in the context of the weight total.
     *
     * @param amount
     *            The amount to weigh.
     * @param weightTotal
     *            The weight total.
     * @param weight
     *            The mathematical weight of the amount in the context of the
     *            weightTotal.
     * @param scale
     *            The scale (precision) of the calculated result.
     * @return The weighted amount.
     */
    BigDecimal calcWeightedAmount(BigDecimal amount, int weightTotal,
            int weight, int scale);

    /**
     * Redeems a voucher.
     *
     * @param dto
     *            The {@link AccountVoucherRedeemDto}.
     * @return An error can be returned with key
     *         {@link #MSG_KEY_VOUCHER_REDEEM_NUMBER_INVALID} or
     *         {@link #MSG_KEY_VOUCHER_REDEEM_USER_UNKNOWN}
     */
    AbstractJsonRpcMethodResponse redeemVoucher(AccountVoucherRedeemDto dto);

    /**
     * Deposits funds as transacted at a point of sale.
     *
     * @param dto
     *            The {@link PosDepositDto}.
     * @return Use {@link AbstractJsonRpcMethodResponse#asResult()},
     *         {@link JsonRpcMethodResult#getResult()} and
     *         {@link JsonRpcResult#data(Class)} with Class
     *         {@link ResultPosDeposit} to get the result data.
     */
    AbstractJsonRpcMethodResponse depositFunds(PosDepositDto dto);

    /**
     * Accepts funds from a Payment Gateway: an {@link AccountTrx} is created
     * and the user {@link Account} is incremented.
     *
     * @param lockedUser
     *            The requesting {@link User} as locked by the caller (can be
     *            {@code null} when user is unknown).
     * @param dto
     *            The {@link UserPaymentGatewayDto}
     * @param orphanedPaymentAccount
     *            The {@link Account} to add funds on when the requesting
     *            {@link User} of the transaction is unknown.
     * @since 0.9.9
     */
    void acceptFundsFromGateway(final User lockedUser,
            UserPaymentGatewayDto dto, Account orphanedPaymentAccount);

    /**
     * Creates pending funds from a Payment Gateway: an {@link AccountTrx} is
     * created but the user {@link Account} is <i>not</i> incremented.
     *
     * @param lockedUser
     *            The {@link User} as locked by the caller.
     * @param dto
     *            The {@link UserPaymentGatewayDto}
     * @since 0.9.9
     */
    void createPendingFundsFromGateway(User lockedUser,
            UserPaymentGatewayDto dto);

    /**
     * Accepts pending funds from a Payment Gateway: the {@link AccountTrx} is
     * updated <i>and</i> the user {@link Account} is incremented.
     *
     * @param trx
     *            The {@link AccountTrx} to acknowledge.
     * @param dto
     *            The {@link UserPaymentGatewayDto}
     * @throws AccountingException
     *             When invariant is violated.
     * @since 0.9.9
     */
    void acceptPendingFundsFromGateway(final AccountTrx trx,
            UserPaymentGatewayDto dto) throws AccountingException;

    /**
     * Creates the DTO of a {@link AccountTrx.AccountTrxTypeEnum#DEPOSIT}
     * transaction.
     *
     * @param accountTrxId
     *            The id of the {@link AccountTrx}.
     * @return The {@link PosDepositReceiptDto}.
     */
    PosDepositReceiptDto createPosDepositReceiptDto(Long accountTrxId);

    /**
     * Checks if cost can be charged to account, according to account balance
     * and credit policy.
     *
     * @param account
     *            The {@link Account} to charge to.
     * @param cost
     *            The cost to charge.
     * @return {@code true} if cost can be charged.
     */
    boolean isBalanceSufficient(Account account, BigDecimal cost);

    /**
     * Changes the base application currency. This action creates financial
     * transactions to align each account to the new currency.
     * <p>
     * NOTE: Use {@link ReadWriteLockEnum#DATABASE_READONLY} and
     * {@link ReadWriteLockEnum#setWriteLock(boolean)} scope for this method.
     * </p>
     *
     * @param batchCommitter
     *            The {@link DaoBatchCommitter}.
     * @param currencyFrom
     *            The current base {@link Currency}.
     * @param currencyTo
     *            The new base {@link Currency}.
     * @param exchangeRate
     *            The exchange rate.
     * @return The JSON-RPC Return message (either a result or an error);
     */
    AbstractJsonRpcMethodResponse changeBaseCurrency(
            DaoBatchCommitter batchCommitter, Currency currencyFrom,
            Currency currencyTo, double exchangeRate);

    /**
     * Transfers credit from one user account to another.
     *
     * @param dto
     *            The {@link UserCreditTransferDto}.
     * @return The JSON-RPC Return message (either a result or an error);
     */
    AbstractJsonRpcMethodResponse transferUserCredit(UserCreditTransferDto dto);

    /**
     * Creates a new shared {@link Account} object with default values (from a
     * template).
     *
     * @param name
     *            The name of the account.
     * @param parent
     *            The shared {@link Account} parent.
     * @return
     */
    Account createSharedAccountTemplate(String name, Account parent);
}
