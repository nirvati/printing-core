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
package org.savapage.core.dto;

import java.math.BigDecimal;

/**
 * Information for an user payment using a Payment Gateway.
 *
 * @author Datraverse B.V.
 *
 */
public class UserPaymentGatewayDto extends AbstractDto {

    /**
     *
     */
    private String gatewayId;

    /**
     *
     */
    private String transactionId;

    /**
     * The payment method according to the Payment Gateway Enum.
     */
    private String paymentMethod;

    /**
     * Details of the other payment method.
     */
    private String paymentMethodOther;

    /**
     * The user who requested the payment.
     */
    private String userId;

    /**
     * The acknowledged amount.
     */
    private BigDecimal amountAcknowledged;

    /**
     * The amount.
     */
    private BigDecimal amount;

    /**
     * .
     */
    private Integer confirmations;

    /**
     *
     */
    private String comment;

    /**
     * The ISO currency code of the amount.
     */
    private String currencyCode;

    /**
     *
     */
    private String paymentMethodAddress;

    /**
     * Currency Code of the payment method: BTC, EUR, USD, etc.
     */
    private String paymentMethodCurrency;

    /**
     * Exchange rate.
     */
    private BigDecimal exchangeRate;

    /**
     * The amount of the transaction in external currency.
     */
    private BigDecimal paymentMethodAmount;

    /**
     * The fee.
     */
    private BigDecimal paymentMethodFee;

    /**
     * Free format details.
     */
    private String paymentMethodDetails;

    public String getGatewayId() {
        return gatewayId;
    }

    public void setGatewayId(String gatewayId) {
        this.gatewayId = gatewayId;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getPaymentMethodOther() {
        return paymentMethodOther;
    }

    public void setPaymentMethodOther(String paymentMethodOther) {
        this.paymentMethodOther = paymentMethodOther;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    /**
     *
     * @return The acknowledged amount.
     */
    public BigDecimal getAmountAcknowledged() {
        return amountAcknowledged;
    }

    /**
     *
     * @param amount
     *            The acknowledged amount.
     */
    public void setAmountAcknowledged(BigDecimal amount) {
        this.amountAcknowledged = amount;
    }

    /**
     *
     * @return The amount.
     */
    public BigDecimal getAmount() {
        return amount;
    }

    /**
     *
     * @param amount
     *            The amount.
     */
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public String getPaymentMethodAddress() {
        return paymentMethodAddress;
    }

    public void setPaymentMethodAddress(String paymentMethodAddress) {
        this.paymentMethodAddress = paymentMethodAddress;
    }

    public String getPaymentMethodCurrency() {
        return paymentMethodCurrency;
    }

    public void setPaymentMethodCurrency(String paymentMethodCurrency) {
        this.paymentMethodCurrency = paymentMethodCurrency;
    }

    public BigDecimal getExchangeRate() {
        return exchangeRate;
    }

    public void setExchangeRate(BigDecimal exchangeRate) {
        this.exchangeRate = exchangeRate;
    }

    public BigDecimal getPaymentMethodAmount() {
        return paymentMethodAmount;
    }

    public void setPaymentMethodAmount(BigDecimal paymentMethodAmount) {
        this.paymentMethodAmount = paymentMethodAmount;
    }

    public BigDecimal getPaymentMethodFee() {
        return paymentMethodFee;
    }

    public void setPaymentMethodFee(BigDecimal paymentMethodFee) {
        this.paymentMethodFee = paymentMethodFee;
    }

    public String getPaymentMethodDetails() {
        return paymentMethodDetails;
    }

    public void setPaymentMethodDetails(String paymentMethodDetails) {
        this.paymentMethodDetails = paymentMethodDetails;
    }

    public Integer getConfirmations() {
        return confirmations;
    }

    public void setConfirmations(Integer confirmations) {
        this.confirmations = confirmations;
    }

}
