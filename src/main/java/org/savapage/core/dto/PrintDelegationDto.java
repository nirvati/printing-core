/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2016 Datraverse B.V.
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

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Print Delegation data.
 *
 * @author Rijk Ravestein
 *
 */
@JsonInclude(Include.NON_NULL)
public final class PrintDelegationDto extends AbstractDto {

    public enum DelegatorAccountEnum {
        GROUP, USER, SHARED
    }

    @JsonInclude(Include.NON_NULL)
    public static final class DelegatorAccount {

        @JsonProperty("type")
        private DelegatorAccountEnum accountType;

        @JsonProperty("id")
        private Long accountId;

        public DelegatorAccountEnum getAccountType() {
            return accountType;
        }

        public void setAccountType(DelegatorAccountEnum accountType) {
            this.accountType = accountType;
        }

        public Long getAccountId() {
            return accountId;
        }

        public void setAccountId(Long accountId) {
            this.accountId = accountId;
        }

    }

    private String name;

    private Map<Long, DelegatorAccount> groups;
    private Map<Long, DelegatorAccount> users;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<Long, DelegatorAccount> getGroups() {
        return groups;
    }

    public void setGroups(Map<Long, DelegatorAccount> groups) {
        this.groups = groups;
    }

    public Map<Long, DelegatorAccount> getUsers() {
        return users;
    }

    public void setUsers(Map<Long, DelegatorAccount> users) {
        this.users = users;
    }

}
