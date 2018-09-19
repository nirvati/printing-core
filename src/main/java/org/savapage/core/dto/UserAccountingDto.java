/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2018 Datraverse B.V.
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.core.dto;

import java.util.Locale;

import org.savapage.core.jpa.Account;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * {@link Account} information for a {@link User}, formatted according to a
 * {@link Locale}.
 *
 * @author Rijk Ravestein
 *
 */
@JsonInclude(Include.NON_NULL)
public class UserAccountingDto extends AbstractDto {

    /**
     * The locale (languageTag) of the amount strings (e.g. {@code en-US}) See
     * {@link Locale#toLanguageTag()}.
     */
    @JsonProperty("locale")
    String locale;

    @JsonProperty("comment")
    private String comment;

    @JsonProperty("balance")
    private String balance;

    @JsonProperty("keepBalance")
    private Boolean keepBalance = false;

    /**
     * The type of credit limit.
     */
    @JsonProperty("creditLimit")
    private CreditLimitDtoEnum creditLimit;

    /**
     * Relevant when {@link #creditLimit} EQ
     * {@link CreditLimitDtoEnum#INDIVIDUAL}.
     */
    @JsonProperty("creditLimitAmount")
    private String creditLimitAmount;

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getBalance() {
        return balance;
    }

    public void setBalance(String balance) {
        this.balance = balance;
    }

    public Boolean getKeepBalance() {
        return keepBalance;
    }

    public void setKeepBalance(Boolean keepBalance) {
        this.keepBalance = keepBalance;
    }

    public CreditLimitDtoEnum getCreditLimit() {
        return creditLimit;
    }

    public void setCreditLimit(CreditLimitDtoEnum creditLimit) {
        this.creditLimit = creditLimit;
    }

    public String getCreditLimitAmount() {
        return creditLimitAmount;
    }

    public void setCreditLimitAmount(String creditLimitAmount) {
        this.creditLimitAmount = creditLimitAmount;
    }

}
