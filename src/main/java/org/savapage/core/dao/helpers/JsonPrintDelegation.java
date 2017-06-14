/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2016 Datraverse B.V.
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
package org.savapage.core.dao.helpers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.savapage.core.SpException;
import org.savapage.core.dto.PrintDelegationDto;
import org.savapage.core.dto.PrintDelegationDto.DelegatorAccountEnum;
import org.savapage.core.jpa.Account.AccountTypeEnum;
import org.savapage.core.jpa.UserGroup;
import org.savapage.core.json.JsonAbstractBase;

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
public final class JsonPrintDelegation extends JsonAbstractBase {

    /**
     * {@link UserGroup} IDs assigned to {@link Account} of
     * {@link AccountTypeEnum#GROUP}.
     */
    @JsonProperty("gg")
    private List<Long> groupsAccountGroup;

    /**
     * {@link UserGroup} IDs assigned to {@link Account} of
     * {@link AccountTypeEnum#USER}.
     */
    @JsonProperty("gu")
    private List<Long> groupsAccountUser;

    /**
     * Assignments to {@link Account} of {@link AccountTypeEnum#SHARED}.
     */
    @JsonProperty("gs")
    private Map<Long, Long> groupsAccountShared;

    /**
     * {@link User} IDs assigned to {@link Account} of
     * {@link AccountTypeEnum#USER}.
     */
    @JsonProperty("u")
    private List<Long> users;

    /**
     * Extra copies on {@link Account} of {@link AccountTypeEnum#SHARED}.
     */
    @JsonProperty("c")
    private Map<Long, Integer> copiesAccountShared;

    //
    public List<Long> getGroupsAccountGroup() {
        return groupsAccountGroup;
    }

    public void setGroupsAccountGroup(List<Long> groupsAccountGroup) {
        this.groupsAccountGroup = groupsAccountGroup;
    }

    public List<Long> getGroupsAccountUser() {
        return groupsAccountUser;
    }

    public void setGroupsAccountUser(List<Long> groupsAccountUser) {
        this.groupsAccountUser = groupsAccountUser;
    }

    public Map<Long, Long> getGroupsAccountShared() {
        return groupsAccountShared;
    }

    public void setGroupsAccountShared(Map<Long, Long> groupsAccountShared) {
        this.groupsAccountShared = groupsAccountShared;
    }

    public List<Long> getUsers() {
        return users;
    }

    public void setUsers(List<Long> users) {
        this.users = users;
    }

    public Map<Long, Integer> getCopiesAccountShared() {
        return copiesAccountShared;
    }

    public void setCopiesAccountShared(Map<Long, Integer> copiesAccountShared) {
        this.copiesAccountShared = copiesAccountShared;
    }

    /**
     * Creates
     *
     * @param source
     *            The {@link PrintDelegationDto}
     * @return The {@link JsonPrintDelegation}.
     */
    public static JsonPrintDelegation create(final PrintDelegationDto source) {

        final JsonPrintDelegation target = new JsonPrintDelegation();

        // Groups
        target.setGroupsAccountGroup(new ArrayList<Long>());
        target.setGroupsAccountShared(new HashMap<Long, Long>());
        target.setGroupsAccountUser(new ArrayList<Long>());

        for (final Entry<Long, PrintDelegationDto.DelegatorAccount> entry : source
                .getGroups().entrySet()) {

            final PrintDelegationDto.DelegatorAccount sourceAccount =
                    entry.getValue();

            final Long id = entry.getKey();

            switch (sourceAccount.getAccountType()) {
            case GROUP:
                target.getGroupsAccountGroup().add(id);
                break;

            case USER:
                target.getGroupsAccountUser().add(id);
                break;

            case SHARED:
                target.getGroupsAccountShared().put(id,
                        sourceAccount.getAccountId());
                break;

            default:
                throw new SpException(String.format("Unhandled %s.%s",
                        DelegatorAccountEnum.class.getSimpleName(),
                        sourceAccount.getAccountType().toString()));
            }
        }

        // Users
        target.setUsers(new ArrayList<Long>());

        for (final Entry<Long, PrintDelegationDto.DelegatorAccount> entry : source
                .getUsers().entrySet()) {

            final PrintDelegationDto.DelegatorAccount sourceAccount =
                    entry.getValue();

            final Long id = entry.getKey();

            switch (sourceAccount.getAccountType()) {
            case USER:
                target.getUsers().add(id);
                break;

            case SHARED:
                // no break intended
            case GROUP:
                throw new IllegalStateException(String.format("%s.%s",
                        DelegatorAccountEnum.class.getSimpleName(),
                        sourceAccount.getAccountType().toString()));

            default:
                throw new SpException(String.format("Unhandled %s.%s",
                        DelegatorAccountEnum.class.getSimpleName(),
                        sourceAccount.getAccountType().toString()));
            }
        }

        // Extra copies
        target.setCopiesAccountShared(new HashMap<Long, Integer>());

        for (final Entry<Long, PrintDelegationDto.DelegatorAccount> entry : source
                .getCopies().entrySet()) {

            final PrintDelegationDto.DelegatorAccount sourceAccount =
                    entry.getValue();

            switch (sourceAccount.getAccountType()) {
            case SHARED:
                target.getCopiesAccountShared().put(entry.getKey(),
                        sourceAccount.getUserCount());
                break;

            case USER:
                // no break intended
            case GROUP:
                throw new IllegalStateException(String.format("%s.%s",
                        DelegatorAccountEnum.class.getSimpleName(),
                        sourceAccount.getAccountType().toString()));

            default:
                throw new SpException(String.format("Unhandled %s.%s",
                        DelegatorAccountEnum.class.getSimpleName(),
                        sourceAccount.getAccountType().toString()));
            }
        }

        //
        return target;
    }

}
