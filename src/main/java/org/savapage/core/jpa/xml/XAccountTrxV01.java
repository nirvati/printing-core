/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2017 Datraverse B.V.
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
package org.savapage.core.jpa.xml;

import java.math.BigDecimal;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.savapage.core.jpa.schema.AccountTrxV01;
import org.savapage.core.jpa.schema.AccountVoucherV01;
import org.savapage.core.jpa.schema.CostChangeV01;
import org.savapage.core.jpa.schema.PosPurchaseV01;

/**
 *
 * @author Rijk Ravestein
 *
 */
@Entity
@Table(name = AccountTrxV01.TABLE_NAME)
public class XAccountTrxV01 extends XEntityVersion {

    @Id
    @Column(name = "account_trx_id")
    private Long id;

    /**
     * The related account (required).
     */
    @Column(name = "account_id", nullable = false)
    private Long account;

    /**
     * The related document (can be null). Note that this is NOT @OneToOne,
     * since the cost of one (1) DocLog can be paid from more that one (1)
     * Account.
     */
    @Column(name = "doc_id", nullable = true)
    private Long docLog;

    /**
     * The optional {@link AccountVoucherV01} association.
     */
    @Column(name = "account_voucher_id", nullable = true)
    private Long accountVoucher;

    /**
     * The optional {@link PosPurchaseV01} association.
     */
    @Column(name = "pos_purchase_id", nullable = true)
    private Long posPurchase;

    /**
     * The optional {@link CostChangeV01} association.
     */
    @Column(name = "cost_change_id", nullable = true)
    private Long costChange;

    /**
     * The amount of the transaction.
     */
    @Column(name = "amount", nullable = false, precision = 16, scale = 6)
    private BigDecimal amount;

    /**
     * The balance of the account AFTER this transaction.
     */
    @Column(name = "balance", nullable = false, precision = 16, scale = 6)
    private BigDecimal balance;

    /**
     * Indication if this is a credit transaction.
     */
    @Column(name = "is_credit", nullable = false)
    private Boolean isCredit;

    /**
     * Optional units responsible for the mathematical weight. For instance, the
     * number of users, so trx_weight / trx_weight_unit = copies per user.
     */
    @Column(name = "trx_weight_unit", nullable = false)
    private Integer transactionWeightUnit;

    /**
     * Mathematical weight of the transaction in the context of a transaction
     * set.
     */
    @Column(name = "trx_weight", nullable = false)
    private Integer transactionWeight;

    /**
     * Timestamp of the transaction.
     */
    @Column(name = "trx_date", nullable = false)
    private Date transactionDate;

    /**
     * Actor of the transaction. E.g. "admin" | "[system] (print)".
     */
    @Column(name = "trx_by", length = 50, nullable = false)
    private String transactedBy;

    /**
     * An optional comment. E.g: "Bulk credit adjustment" | "from custom PHP
     * application".
     */
    @Column(name = "trx_comment", length = 255, nullable = true)
    private String comment;

    /**
     * ADJUST | PRINT
     */
    @Column(name = "trx_type", length = 20, nullable = false)
    private String trxType;

    // -------------------------------------------------------------------------
    /**
     * Currency Code: EUR, USD, etc.
     *
     * @since 0.9.9
     */
    @Column(name = "currency_code", length = 3, nullable = true)
    private String currencyCode;

    /**
     * External source ID.
     *
     * @since 0.9.9
     */
    @Column(name = "ext_source", length = 32, nullable = true)
    private String extSource;

    /**
     * External transaction ID.
     *
     * @since 0.9.9
     */
    @Column(name = "ext_id", length = 128, nullable = true)
    private String extId;

    /**
     * PaymentMethodEnum:. "ideal", "bitcoin", etc.
     *
     * @since 0.9.9
     *
     */
    @Column(name = "ext_method", length = 20, nullable = true)
    private String extMethod;

    /**
     * The number of confirmations for the transaction from the external source.
     *
     * @since 0.9.9
     *
     */
    @Column(name = "ext_confirmations", nullable = true)
    private Integer extConfirmations;

    /**
     * When extMethod == OTHER
     *
     * @since 0.9.9
     */
    @Column(name = "ext_method_other", length = 32, nullable = true)
    private String extMethodOther;

    /**
     * @since 0.9.9
     */
    @Column(name = "ext_method_address", length = 128, nullable = true)
    private String extMethodAddress;

    /**
     * Currency Code: BTC, EUR, USD, etc.
     *
     * @since 0.9.9
     */
    @Column(name = "ext_currency_code", length = 3, nullable = true)
    private String extCurrencyCode;

    /**
     * Exchange rate.
     *
     * @since 0.9.9
     */
    @Column(name = "ext_exchange_rate", nullable = true,
            precision = DECIMAL_PRECISION_16, scale = DECIMAL_SCALE_8)
    private BigDecimal extExchangeRate;

    /**
     * The amount of the transaction in external currency.
     *
     * @since 0.9.9
     */
    @Column(name = "ext_amount", nullable = true,
            precision = DECIMAL_PRECISION_16, scale = DECIMAL_SCALE_8)
    private BigDecimal extAmount;

    /**
     * The fee of the transaction in external currency.
     *
     * @since 0.9.9
     */
    @Column(name = "ext_fee", nullable = true, precision = DECIMAL_PRECISION_16,
            scale = DECIMAL_SCALE_8)
    private BigDecimal extFee;

    /**
     * Free format details.
     *
     * @since 0.9.9
     */
    @Column(name = "ext_details", length = 2000, nullable = true)
    private String extDetails;

    // ------------------------------------------------------------------------

    @Override
    public final String xmlName() {
        return "AccountTrx";
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAccount() {
        return account;
    }

    public void setAccount(Long account) {
        this.account = account;
    }

    public Long getDocLog() {
        return docLog;
    }

    public void setDocLog(Long docLog) {
        this.docLog = docLog;
    }

    public Long getAccountVoucher() {
        return accountVoucher;
    }

    public void setAccountVoucher(Long accountVoucher) {
        this.accountVoucher = accountVoucher;
    }

    public Long getPosPurchase() {
        return posPurchase;
    }

    public void setPosPurchase(Long posPurchase) {
        this.posPurchase = posPurchase;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public Boolean getIsCredit() {
        return isCredit;
    }

    public void setIsCredit(Boolean isCredit) {
        this.isCredit = isCredit;
    }

    public Integer getTransactionWeight() {
        return transactionWeight;
    }

    public void setTransactionWeight(Integer transactionWeight) {
        this.transactionWeight = transactionWeight;
    }

    public Date getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(Date transactionDate) {
        this.transactionDate = transactionDate;
    }

    public String getTransactedBy() {
        return transactedBy;
    }

    public void setTransactedBy(String transactedBy) {
        this.transactedBy = transactedBy;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getTrxType() {
        return trxType;
    }

    public void setTrxType(String trxType) {
        this.trxType = trxType;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public String getExtSource() {
        return extSource;
    }

    public void setExtSource(String extSource) {
        this.extSource = extSource;
    }

    public String getExtId() {
        return extId;
    }

    public void setExtId(String extId) {
        this.extId = extId;
    }

    public String getExtMethod() {
        return extMethod;
    }

    public void setExtMethod(String extMethod) {
        this.extMethod = extMethod;
    }

    public String getExtMethodOther() {
        return extMethodOther;
    }

    public void setExtMethodOther(String extMethodOther) {
        this.extMethodOther = extMethodOther;
    }

    public String getExtMethodAddress() {
        return extMethodAddress;
    }

    public void setExtMethodAddress(String extMethodAddress) {
        this.extMethodAddress = extMethodAddress;
    }

    public String getExtCurrencyCode() {
        return extCurrencyCode;
    }

    public void setExtCurrencyCode(String extCurrencyCode) {
        this.extCurrencyCode = extCurrencyCode;
    }

    public BigDecimal getExtExchangeRate() {
        return extExchangeRate;
    }

    public void setExtExchangeRate(BigDecimal extExchangeRate) {
        this.extExchangeRate = extExchangeRate;
    }

    public BigDecimal getExtAmount() {
        return extAmount;
    }

    public void setExtAmount(BigDecimal extAmount) {
        this.extAmount = extAmount;
    }

    public String getExtDetails() {
        return extDetails;
    }

    public void setExtDetails(String extDetails) {
        this.extDetails = extDetails;
    }

    public Integer getExtConfirmations() {
        return extConfirmations;
    }

    public void setExtConfirmations(Integer extConfirmations) {
        this.extConfirmations = extConfirmations;
    }

    public BigDecimal getExtFee() {
        return extFee;
    }

    public void setExtFee(BigDecimal extFee) {
        this.extFee = extFee;
    }

    public Long getCostChange() {
        return costChange;
    }

    public void setCostChange(Long costChange) {
        this.costChange = costChange;
    }

    public Integer getTransactionWeightUnit() {
        return transactionWeightUnit;
    }

    public void setTransactionWeightUnit(Integer transactionWeightUnit) {
        this.transactionWeightUnit = transactionWeightUnit;
    }

}
