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
package org.savapage.core.dao.helpers;

import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
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
     * Map with key {@link UserGroup} ID assigned to {@link Account} of
     * {@link AccountTypeEnum#GROUP}. Value is number of copies per user.
     */
    @JsonProperty("gg")
    private Map<Long, Integer> groupsAccountGroup;

    /**
     * {@link UserGroup} ID assigned to {@link Account} of
     * {@link AccountTypeEnum#USER}.
     */
    @JsonProperty("gu")
    private Map<Long, Integer> groupsAccountUser;

    /**
     * Map with key {@link UserGroup} ID assigned to entry with {@link Account}
     * of {@link AccountTypeEnum#SHARED} and number of copies per user.
     */
    @JsonProperty("gs")
    private Map<Long, SimpleEntry<Long, Integer>> groupsAccountShared;

    /**
     * Map with key {@link User} ID assigned to {@link Account} of
     * {@link AccountTypeEnum#USER}. Value is number of copies.
     */
    @JsonProperty("u")
    private Map<Long, Integer> users;

    /**
     * Extra copies on {@link Account} of {@link AccountTypeEnum#SHARED}.
     */
    @JsonProperty("c")
    private Map<Long, Integer> copiesAccountShared;

    /**
     * @return Map with key {@link UserGroup} ID assigned to {@link Account} of
     *         {@link AccountTypeEnum#GROUP}. Value is number of copies per
     *         user.
     */
    public Map<Long, Integer> getGroupsAccountGroup() {
        return groupsAccountGroup;
    }

    /**
     * @param groupsAccountGroup
     *            Map with key {@link UserGroup} ID assigned to {@link Account}
     *            of {@link AccountTypeEnum#GROUP}. Value is number of copies
     *            per user.
     */
    public void setGroupsAccountGroup(Map<Long, Integer> groupsAccountGroup) {
        this.groupsAccountGroup = groupsAccountGroup;
    }

    /**
     * @return Map with key {@link UserGroup} ID assigned to {@link Account} of
     *         {@link AccountTypeEnum#USER}. Value is number of copies per user.
     */
    public Map<Long, Integer> getGroupsAccountUser() {
        return groupsAccountUser;
    }

    /**
     * @param groupsAccountUser
     *            Map with key {@link UserGroup} ID assigned to {@link Account}
     *            of {@link AccountTypeEnum#USER}. Value is number of copies per
     *            user.
     */
    public void setGroupsAccountUser(Map<Long, Integer> groupsAccountUser) {
        this.groupsAccountUser = groupsAccountUser;
    }

    /**
     * @return Map with key {@link UserGroup} ID assigned to entry with
     *         {@link Account} of {@link AccountTypeEnum#SHARED} and number of
     *         copies per user.
     */
    public Map<Long, SimpleEntry<Long, Integer>> getGroupsAccountShared() {
        return groupsAccountShared;
    }

    /**
     * @param groupsAccountShared
     *            Map with key {@link UserGroup} ID assigned to entry with
     *            {@link Account} of {@link AccountTypeEnum#SHARED} and number
     *            of copies per user.
     */
    public void setGroupsAccountShared(
            Map<Long, SimpleEntry<Long, Integer>> groupsAccountShared) {
        this.groupsAccountShared = groupsAccountShared;
    }

    /**
     * @return Map with key {@link User} ID assigned to {@link Account} of
     *         {@link AccountTypeEnum#USER}. Value is number of copies.
     */
    public Map<Long, Integer> getUsers() {
        return users;
    }

    /**
     * @param users
     *            Map with key {@link User} ID assigned to {@link Account} of
     *            {@link AccountTypeEnum#USER}. Value is number of copies.
     */
    public void setUsers(Map<Long, Integer> users) {
        this.users = users;
    }

    /**
     * @return Extra copies on {@link Account} of
     *         {@link AccountTypeEnum#SHARED}.
     */
    public Map<Long, Integer> getCopiesAccountShared() {
        return copiesAccountShared;
    }

    /**
     * @param copiesAccountShared
     *            Extra copies on {@link Account} of
     *            {@link AccountTypeEnum#SHARED}.
     */
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
        target.setGroupsAccountGroup(new HashMap<Long, Integer>());
        target.setGroupsAccountShared(
                new HashMap<Long, SimpleEntry<Long, Integer>>());
        target.setGroupsAccountUser(new HashMap<Long, Integer>());

        for (final Entry<Long, PrintDelegationDto.DelegatorAccount> entry : source
                .getGroups().entrySet()) {

            final PrintDelegationDto.DelegatorAccount sourceAccount =
                    entry.getValue();

            final Long id = entry.getKey();

            switch (sourceAccount.getAccountType()) {
            case GROUP:
                target.getGroupsAccountGroup().put(id,
                        sourceAccount.getUserCopies());
                break;

            case USER:
                target.getGroupsAccountUser().put(id,
                        sourceAccount.getUserCopies());
                break;

            case SHARED:
                target.getGroupsAccountShared().put(id,
                        new SimpleEntry<Long, Integer>(
                                sourceAccount.getAccountId(),
                                sourceAccount.getUserCopies()));
                break;

            default:
                throw new SpException(String.format("Unhandled %s.%s",
                        DelegatorAccountEnum.class.getSimpleName(),
                        sourceAccount.getAccountType().toString()));
            }
        }

        // Users
        target.setUsers(new HashMap<Long, Integer>());

        for (final Entry<Long, PrintDelegationDto.DelegatorAccount> entry : source
                .getUsers().entrySet()) {

            final PrintDelegationDto.DelegatorAccount sourceAccount =
                    entry.getValue();

            final Long id = entry.getKey();

            switch (sourceAccount.getAccountType()) {
            case USER:
                target.getUsers().put(id, entry.getValue().getUserCopies());
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

        return target;
    }

}
