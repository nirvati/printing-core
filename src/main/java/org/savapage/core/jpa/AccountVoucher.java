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
package org.savapage.core.jpa;

import java.math.BigDecimal;
import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

/**
 * An Account Voucher is a (printed) card or file token that can be redeemed by
 * a {@link User}, so its value is added to an {@link Account} by an
 * {@link AccountTrx}.
 * <p>
 * The token <i>value</i> is created as the result of a financial transaction,
 * with the following parties and roles.
 * </p>
 * <p>
 * MERCHANT
 * <ul>
 * <li>The shop that sells the voucher.</li>
 * <li>This can be the internal SavaPage POS, or a Trusted Third Party (TTP).</li>
 * </ul>
 * </p>
 * <p>
 * CONSUMER
 * <ul>
 * <li>The Person who purchases a voucher from a MERCHANT.</li>
 * <li>This can be a different person than the one who redeems the voucher.</li>
 * </ul>
 * </p>
 * <p>
 * ACCEPTANT
 * <ul>
 * <li>Party who accepts the voucher, i.e. SavaPage Financial.</li>
 * </ul>
 * </p>
 * <p>
 * ACQUIRER (iDEAL, PayPal)
 * <ul>
 * <li>The bank holding the account of the ACCEPTANT.</li>
 * <li>The MERCHANT sells the transaction to the ACQUIRER, and is reimbursed the
 * amount of the sales ticket less a "discount fee".</li>
 * </ul>
 * </p>
 * <p>
 * ISSUER (not modelled)
 * <ul>
 * <li>The bank holding the account of the CONSUMER.</li>
 * <li>The ISSUER (issuing bank) pays the ACQUIRER, less an interchange fee
 * which partially reimburses the ISSUER for its expense.</li>
 * <li>The CONSUMER repays the ISSUER for the goods or services originally
 * purchased from the MERCHANT.</li>
 * </ul>
 * </p>
 *
 * @author Datraverse B.V.
 *
 */
@Entity
@Table(name = AccountVoucher.TABLE_NAME)
public class AccountVoucher extends org.savapage.core.jpa.Entity {

    public static final String TABLE_NAME = "tbl_account_voucher";

    @Id
    @Column(name = "account_voucher_id")
    @TableGenerator(name = "accountVoucherPropGen",
            table = Sequence.TABLE_NAME, pkColumnName = "SEQUENCE_NAME",
            valueColumnName = "SEQUENCE_NEXT_VALUE",
            pkColumnValue = TABLE_NAME, allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.TABLE,
            generator = "accountVoucherPropGen")
    private Long id;

    /**
     * The voucher's UUID as created by ACCEPTANT, i.e. SavaPage Financial. See
     * {@link java.util.UUID#randomUUID()};
     * <p>
     * NOTE (1): This UUID is passed to the Trusted-Third-Party (TTP) MERCHANT
     * to create the voucher.
     * </p>
     * <p>
     * NOTE (2): ACCEPTANT and MERCHANT can be one and the same party, as is the
     * case for Prepaid Cards.
     * </p>
     */
    @Column(name = "uuid", length = 64, nullable = false, updatable = false)
    private String uuid;

    /**
     * The value of this voucher.
     */
    @Column(name = "value_amount", nullable = false,
            precision = DECIMAL_PRECISION_6, scale = DECIMAL_SCALE_2)
    private BigDecimal valueAmount;

    /**
     * Currency Code: EUR, USD, etc.
     *
     * @since 0.9.9
     */
    @Column(name = "currency_code", length = 3, nullable = true)
    private String currencyCode;

    /**
     * CARD | TRX
     */
    @Column(name = "voucher_type", length = 16, nullable = false)
    private String voucherType;

    /**
     *
     */
    @Column(name = "card_number_batch", length = 64)
    private String cardNumberBatch;

    /**
     * Card Number
     */
    @Column(name = "card_number", length = 128, nullable = false)
    private String cardNumber;

    /**
     * The code of MERCHANT: [internal] | SAVAPAY | ...
     */
    @Column(name = "trx_merchant_code", length = 32)
    private String trxMerchantCode;

    /**
     * The name of ACQUIRER: [internal] | IDEAL | PAYPAL
     */
    @Column(name = "trx_acquirer_code", length = 32)
    private String trxAcquirerCode;

    /**
     * Time when transaction was requested (milliseconds since January 1, 1970,
     * 00:00:00 GMT) at ACCEPTANT. See {@link Date#getTime()}.
     */
    @Column(name = "trx_time_request")
    private Long trxTimeRequest;

    /**
     * Time when transaction was processed (milliseconds since January 1, 1970,
     * 00:00:00 GMT) at MERCHANT. See {@link Date#getTime()}.
     */
    @Column(name = "trx_time_merchant")
    private Long trxTimeMerchant;

    /**
     * Transaction Reference of the MERCHANT.
     */
    @Column(name = "trx_ref_merchant", length = 64)
    private String trxRefMerchant;

    /**
     * Transaction Reference of the ACQUIRER.
     * <p>
     * For example:
     * <ul>
     * <li>PayPal: 5Y3944021B523613T</li>
     * <li>iDEAL: 0020000696438834</li>
     * </p>
     */
    @Column(name = "trx_ref_acquirer", length = 64)
    private String trxRefAcquirer;

    /**
     * Amount paid in the transaction for this voucher.
     */
    @Column(name = "trx_amount", precision = DECIMAL_PRECISION_6,
            scale = DECIMAL_SCALE_2)
    private BigDecimal trxAmount;

    /**
     * The email address of the transaction CONSUMER.
     * <p>
     * This email address is theoretically unrelated to a {@link User} instance,
     * since anyone could buy a Voucher File, and give it to a known
     * {@link User} to redeem it.
     * </p>
     */
    @Column(name = "trx_consumer_email", length = 255)
    private String trxConsumerEmail;

    /**
     * The date this voucher token instance was CREATED.
     */
    @Column(name = "created_date", nullable = false, updatable = false)
    private Date createdDate;

    /**
     * The date this voucher EXPIRES.
     */
    @Column(name = "expiry_date")
    private Date expiryDate;

    /**
     * The date this voucher instance was ISSUED.
     */
    @Column(name = "issued_date")
    private Date issuedDate;

    /**
     * The date this voucher instance was REDEEMED.
     */
    @Column(name = "redeemed_date")
    private Date redeemedDate;

    /**
     * The optional LAZY {@link AccountTrx} association (is non-null when
     * voucher is redeemed).
     */
    @OneToOne(mappedBy = "accountVoucher", cascade = { CascadeType.ALL },
            fetch = FetchType.LAZY, optional = true)
    private AccountTrx accountTrx;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public BigDecimal getValueAmount() {
        return valueAmount;
    }

    public void setValueAmount(BigDecimal valueAmount) {
        this.valueAmount = valueAmount;
    }

    public String getVoucherType() {
        return voucherType;
    }

    public void setVoucherType(String voucherType) {
        this.voucherType = voucherType;
    }

    public String getCardNumberBatch() {
        return cardNumberBatch;
    }

    public void setCardNumberBatch(String cardNumberBatch) {
        this.cardNumberBatch = cardNumberBatch;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public String getTrxMerchantCode() {
        return trxMerchantCode;
    }

    public void setTrxMerchantCode(String trxMerchantCode) {
        this.trxMerchantCode = trxMerchantCode;
    }

    public String getTrxAcquirerCode() {
        return trxAcquirerCode;
    }

    public void setTrxAcquirerCode(String trxAcquirerCode) {
        this.trxAcquirerCode = trxAcquirerCode;
    }

    public Long getTrxTimeRequest() {
        return trxTimeRequest;
    }

    public void setTrxTimeRequest(Long trxTimeRequest) {
        this.trxTimeRequest = trxTimeRequest;
    }

    public Long getTrxTimeMerchant() {
        return trxTimeMerchant;
    }

    public void setTrxTimeMerchant(Long trxTimeMerchant) {
        this.trxTimeMerchant = trxTimeMerchant;
    }

    public String getTrxRefMerchant() {
        return trxRefMerchant;
    }

    public void setTrxRefMerchant(String trxRefMerchant) {
        this.trxRefMerchant = trxRefMerchant;
    }

    public String getTrxRefAcquirer() {
        return trxRefAcquirer;
    }

    public void setTrxRefAcquirer(String trxRefAcquirer) {
        this.trxRefAcquirer = trxRefAcquirer;
    }

    public BigDecimal getTrxAmount() {
        return trxAmount;
    }

    public void setTrxAmount(BigDecimal trxAmount) {
        this.trxAmount = trxAmount;
    }

    public String getTrxConsumerEmail() {
        return trxConsumerEmail;
    }

    public void setTrxConsumerEmail(String trxConsumerEmail) {
        this.trxConsumerEmail = trxConsumerEmail;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public Date getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(Date expiryDate) {
        this.expiryDate = expiryDate;
    }

    public Date getIssuedDate() {
        return issuedDate;
    }

    public void setIssuedDate(Date issuedDate) {
        this.issuedDate = issuedDate;
    }

    public Date getRedeemedDate() {
        return redeemedDate;
    }

    public void setRedeemedDate(Date redeemedDate) {
        this.redeemedDate = redeemedDate;
    }

    public AccountTrx getAccountTrx() {
        return accountTrx;
    }

    public void setAccountTrx(AccountTrx accountTrx) {
        this.accountTrx = accountTrx;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

}
