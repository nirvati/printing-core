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

import java.util.ArrayList;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 *
 * @author Datraverse B.V.
 *
 */
@JsonPropertyOrder({ "userName", "password", "fullName", "email", "emailOther",
        "card", "cardFormat", "cardFirstByte", "id;", "pin", "admin", "person",
        "disabled", "keepEmailOther", "keepCard", "keepPassword", "keepPin",
        "accounting" })
@JsonInclude(Include.NON_NULL)
public class UserDto extends AbstractDto {

    @JsonProperty("dbId")
    private Long databaseId;

    @JsonProperty("userName")
    private String userName;

    @JsonProperty("password")
    private String password;

    @JsonProperty("fullName")
    private String fullName;

    @JsonProperty("email")
    private String email;

    @JsonProperty("emailOther")
    private ArrayList<UserEmailDto> emailOther;

    @JsonProperty("card")
    private String card;

    @JsonProperty("cardFormat")
    private String cardFormat;

    @JsonProperty("cardFirstByte")
    private String cardFirstByte;

    @JsonProperty("id")
    private String id;

    @JsonProperty("pin")
    private String pin;

    @JsonProperty("internal")
    private Boolean internal = Boolean.FALSE;

    @JsonProperty("admin")
    private Boolean admin = Boolean.FALSE;

    @JsonProperty("person")
    private Boolean person = Boolean.TRUE;

    @JsonProperty("disabled")
    private Boolean disabled = Boolean.FALSE;

    @JsonProperty("keepEmailOther")
    private Boolean keepEmailOther = false;

    @JsonProperty("keepCard")
    private Boolean keepCard = false;

    @JsonProperty("keepPassword")
    private Boolean keepPassword = false;

    @JsonProperty("keepPin")
    private Boolean keepPin = false;

    @JsonProperty("accounting")
    private UserAccountingDto accounting;

    /**
     *
     */
    public UserDto() {

    }

    /**
     *
     * @param dto
     */
    public UserDto(UserDto dto) {
        //
        databaseId = dto.databaseId;
        card = dto.card;
        cardFirstByte = dto.cardFirstByte;
        cardFormat = dto.cardFormat;
        email = dto.email;
        emailOther = dto.emailOther;
        id = dto.id;
        password = dto.password;
        pin = dto.pin;
        userName = dto.userName;
        //
        internal = dto.internal;
        person = dto.person;
        admin = dto.admin;
        disabled = dto.disabled;
        //
        keepCard = dto.keepCard;
        keepEmailOther = dto.keepEmailOther;
        keepPassword = dto.keepPassword;
        keepPin = dto.keepPin;
        //
        accounting = dto.accounting;
    }

    public Long getDatabaseId() {
        return databaseId;
    }

    public void setDatabaseId(Long databaseId) {
        this.databaseId = databaseId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public ArrayList<UserEmailDto> getEmailOther() {
        return emailOther;
    }

    public void setEmailOther(ArrayList<UserEmailDto> emailOther) {
        this.emailOther = emailOther;
    }

    public String getCard() {
        return card;
    }

    public void setCard(String card) {
        this.card = card;
    }

    public String getCardFormat() {
        return cardFormat;
    }

    public void setCardFormat(String cardFormat) {
        this.cardFormat = cardFormat;
    }

    public String getCardFirstByte() {
        return cardFirstByte;
    }

    public void setCardFirstByte(String cardFirstByte) {
        this.cardFirstByte = cardFirstByte;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }

    public Boolean getInternal() {
        return internal;
    }

    public void setInternal(Boolean internal) {
        this.internal = internal;
    }

    public Boolean getAdmin() {
        return admin;
    }

    public void setAdmin(Boolean admin) {
        this.admin = admin;
    }

    public Boolean getPerson() {
        return person;
    }

    public void setPerson(Boolean person) {
        this.person = person;
    }

    public Boolean getDisabled() {
        return disabled;
    }

    public void setDisabled(Boolean disabled) {
        this.disabled = disabled;
    }

    public Boolean getKeepEmailOther() {
        return keepEmailOther;
    }

    public void setKeepEmailOther(Boolean keepEmailOther) {
        this.keepEmailOther = keepEmailOther;
    }

    public Boolean getKeepCard() {
        return keepCard;
    }

    public void setKeepCard(Boolean keepCard) {
        this.keepCard = keepCard;
    }

    public Boolean getKeepPassword() {
        return keepPassword;
    }

    public void setKeepPassword(Boolean keepPassword) {
        this.keepPassword = keepPassword;
    }

    public Boolean getKeepPin() {
        return keepPin;
    }

    public void setKeepPin(Boolean keepPin) {
        this.keepPin = keepPin;
    }

    public UserAccountingDto getAccounting() {
        return accounting;
    }

    public void setAccounting(UserAccountingDto accounting) {
        this.accounting = accounting;
    }

    /**
     * Imports a string with concatenatedEmails, separated by one of the
     * characters {@code ";, \r\n"}.
     * <p>
     * NOTE: The email addresses are NOT validated.
     * </p>
     *
     * @param concatenatedEmails
     *            If {@code null} or {@code empty} the list of other emails will
     *            be empty.
     */
    public void importEmailOther(final String concatenatedEmails) {

        this.emailOther = new ArrayList<>();

        if (StringUtils.isNotBlank(concatenatedEmails)) {

            for (String address : StringUtils.split(concatenatedEmails,
                    ";, \r\n")) {
                if (StringUtils.isNotBlank(address)) {
                    UserEmailDto dto = new UserEmailDto();
                    dto.setAddress(address);
                    emailOther.add(dto);
                }
            }
        }
    }

}
