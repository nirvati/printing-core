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
package org.savapage.core.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 *
 * @author Datraverse B.V.
 *
 */
@JsonPropertyOrder({ "removeEmail", "removeEmailOther", "removeCard",
        "removeId", "removePassword", "removePin", "removeUuid" })
@JsonInclude(Include.NON_NULL)
public class UserPropertiesDto extends UserDto {

    @JsonProperty("removeEmail")
    private Boolean removeEmail = false;

    @JsonProperty("removeEmailOther")
    private Boolean removeEmailOther = false;

    @JsonProperty("removeCard")
    private Boolean removeCard = false;

    @JsonProperty("removeId")
    private Boolean removeId = false;

    @JsonProperty("removePassword")
    private Boolean removePassword = false;

    @JsonProperty("removePin")
    private Boolean removePin = false;

    @JsonProperty("removeUuid")
    private Boolean removeUuid = false;

    /**
     *
     */
    public UserPropertiesDto() {
    }

    /**
     *
     * @param dto
     */
    public UserPropertiesDto(UserDto dto) {
        super(dto);
    }

    public Boolean getRemoveEmail() {
        return removeEmail;
    }

    public void setRemoveEmail(Boolean removeEmail) {
        this.removeEmail = removeEmail;
    }

    public Boolean getRemoveEmailOther() {
        return removeEmailOther;
    }

    public void setRemoveEmailOther(Boolean removeEmailOther) {
        this.removeEmailOther = removeEmailOther;
    }

    public Boolean getRemoveCard() {
        return removeCard;
    }

    public void setRemoveCard(Boolean removeCard) {
        this.removeCard = removeCard;
    }

    public Boolean getRemoveId() {
        return removeId;
    }

    public void setRemoveId(Boolean removeId) {
        this.removeId = removeId;
    }

    public Boolean getRemovePassword() {
        return removePassword;
    }

    public void setRemovePassword(Boolean removePassword) {
        this.removePassword = removePassword;
    }

    public Boolean getRemovePin() {
        return removePin;
    }

    public void setRemovePin(Boolean removePin) {
        this.removePin = removePin;
    }

    public Boolean getRemoveUuid() {
        return removeUuid;
    }

    public void setRemoveUuid(Boolean removeUuid) {
        this.removeUuid = removeUuid;
    }

}
