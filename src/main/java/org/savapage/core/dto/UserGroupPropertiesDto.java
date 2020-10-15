/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2020 Datraverse B.V.
 * Author: Rijk Ravestein.
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
package org.savapage.core.dto;

import java.util.Map;

import org.savapage.core.dao.enums.ACLRoleEnum;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 *
 * @author Rijk Ravestein
 *
 */
@JsonPropertyOrder({ "accounting", "roleUpdate" })
@JsonInclude(Include.NON_NULL)
public class UserGroupPropertiesDto extends UserGroupDto {

    /**
     *
     */
    @JsonProperty("accounting")
    private UserAccountingDto accounting;

    /**
     * The {@link ACLRoleEnum} instances that need to be updated.
     */
    private Map<ACLRoleEnum, Boolean> roleUpdate;

    /**
     *
     */
    public UserGroupPropertiesDto() {
    }

    /**
     *
     * @param dto
     */
    public UserGroupPropertiesDto(UserGroupDto dto) {
    }

    public UserAccountingDto getAccounting() {
        return accounting;
    }

    public void setAccounting(UserAccountingDto accounting) {
        this.accounting = accounting;
    }

    /**
     * @return The {@link ACLRoleEnum} instances that need to be updated.
     */
    public Map<ACLRoleEnum, Boolean> getRoleUpdate() {
        return roleUpdate;
    }

    /**
     * @param roleUpdate
     *            The {@link ACLRoleEnum} instances that need to be updated.
     */
    public void setRoleUpdate(Map<ACLRoleEnum, Boolean> roleUpdate) {
        this.roleUpdate = roleUpdate;
    }

}
