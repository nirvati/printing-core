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
package org.savapage.core.jpa.schema;

import java.math.BigDecimal;
import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

/**
 *
 * @author Datraverse B.V.
 *
 */
@Entity
@Table(name = AccountTrxV01.TABLE_NAME, indexes = {
        @Index(name = "ix_account_trx_1", columnList = "account_id"),
        @Index(name = "ix_account_trx_2", columnList = "doc_id"),
        @Index(name = "ix_account_trx_3", columnList = "trx_date"),
        @Index(name = "ix_account_trx_4", columnList = "account_voucher_id") })
public class AccountTrxV01 implements SchemaEntityVersion {

    /**
     *
     */
    public static final String TABLE_NAME = "tbl_account_trx";

    @Id
    @Column(name = "account_trx_id")
    @TableGenerator(name = "accountTrxPropGen", table = SequenceV01.TABLE_NAME,
            //
            pkColumnName = "SEQUENCE_NAME",
            valueColumnName = "SEQUENCE_NEXT_VALUE",
            //
            pkColumnValue = TABLE_NAME, allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.TABLE,
            generator = "accountTrxPropGen")
    private Long id;

    /**
     * The related account (required).
     */
    @ManyToOne
    @JoinColumn(name = "account_id", nullable = false,
            foreignKey = @ForeignKey(name = "FK_ACCOUNT_TRX_TO_ACCOUNT"))
    private AccountV01 account;

    /**
     * The related document (can be null). Note that this is NOT @OneToOne,
     * since the cost of one (1) DocLog can be paid from more that one (1)
     * Account.
     */
    @ManyToOne
    @JoinColumn(name = "doc_id", nullable = true, foreignKey = @ForeignKey(
            name = "FK_ACCOUNT_TRX_TO_DOCLOG"))
    private DocLogV01 docLog;

    /**
     * The optional EAGER {@link AccountVoucherV01} association.
     */
    @OneToOne(cascade = { CascadeType.ALL }, fetch = FetchType.EAGER,
            optional = true)
    @JoinColumn(
            name = "account_voucher_id",
            nullable = true,
            foreignKey = @ForeignKey(name = "FK_ACCOUNT_TRX_TO_ACCOUNT_VOUCHER"))
    private AccountVoucherV01 accountVoucher;

    /**
     * The optional EAGER {@link PosPurchaseV01} association.
     */
    @OneToOne(cascade = { CascadeType.ALL }, fetch = FetchType.EAGER,
            optional = true)
    @JoinColumn(name = "pos_purchase_id", nullable = true,
            foreignKey = @ForeignKey(name = "FK_ACCOUNT_TRX_TO_POS_PURCHASE"))
    private PosPurchaseV01 posPurchase;

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
     * An optional comment. E.g: "Bulk credit adjustment" |
     * "from custom PHP application".
     */
    @Column(name = "trx_comment", length = 255, nullable = true)
    private String comment;

    /**
     *
     */
    @Column(name = "trx_type", length = 20, nullable = false)
    private String trxType;

    /**
     *
     * @return
     */
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AccountV01 getAccount() {
        return account;
    }

    public void setAccount(AccountV01 account) {
        this.account = account;
    }

    public DocLogV01 getDocLog() {
        return docLog;
    }

    public void setDocLog(DocLogV01 docLog) {
        this.docLog = docLog;
    }

    public AccountVoucherV01 getAccountVoucher() {
        return accountVoucher;
    }

    public void setAccountVoucher(AccountVoucherV01 accountVoucher) {
        this.accountVoucher = accountVoucher;
    }

    public PosPurchaseV01 getPosPurchase() {
        return posPurchase;
    }

    public void setPosPurchase(PosPurchaseV01 posPurchase) {
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

}
