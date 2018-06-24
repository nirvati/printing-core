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
package org.savapage.core.services.impl;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.savapage.core.dao.helpers.JsonPrintDelegation;
import org.savapage.core.jpa.Account;
import org.savapage.core.jpa.Account.AccountTypeEnum;
import org.savapage.core.jpa.User;
import org.savapage.core.jpa.UserAccount;
import org.savapage.core.jpa.UserGroup;
import org.savapage.core.jpa.UserGroupMember;
import org.savapage.core.services.PrintDelegationService;
import org.savapage.core.services.helpers.AccountTrxInfo;
import org.savapage.core.services.helpers.AccountTrxInfoSet;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class PrintDelegationServiceImpl extends AbstractService
        implements PrintDelegationService {

    /**
     * Creates an {@link AccountTrxInfo}.
     *
     * @param account
     *            The account.
     * @param weight
     *            The transaction weight.
     * @param weightUnit
     *            The transaction weight unit.
     * @param groupName
     *            The name of the group the user was selected by, or
     *            {@code null} when selected as single user.
     * @return The account info.
     */
    private static AccountTrxInfo createAccountTrxInfo(final Account account,
            final Integer weight, final int weightUnit,
            final String groupName) {

        final AccountTrxInfo trx = new AccountTrxInfo();

        trx.setAccount(account);

        trx.setWeight(weight);
        trx.setWeightUnit(Integer.valueOf(weightUnit));

        /*
         * Use free format external details to set the group name.
         */
        trx.setExtDetails(groupName);

        return trx;
    }

    /**
     * Adds an {@link AccountTrxInfo} for a user account to target list.
     *
     * @param targetList
     *            The target list.
     * @param user
     *            The {@link User}.
     * @param weight
     *            The transaction weight.
     * @param weightUnit
     *            The transaction weight unit.
     * @param groupName
     *            The name of the group the user was selected by, or
     *            {@code null} when a single user.
     * @return The weight of the added {@link AccountTrxInfo} (same value as
     *         weight parameter).
     */
    private static int addUserAccountToTrxList(
            final List<AccountTrxInfo> targetList, final User user,
            final Integer weight, final int weightUnit,
            final String groupName) {

        final UserAccount userAccount = accountingService()
                .lazyGetUserAccount(user, AccountTypeEnum.USER);

        targetList.add(createAccountTrxInfo(userAccount.getAccount(), weight,
                weightUnit, groupName));

        return weight.intValue();
    }

    @Override
    public AccountTrxInfoSet
            createAccountTrxInfoSet(final JsonPrintDelegation source) {

        final List<AccountTrxInfo> targetList = new ArrayList<>();

        final Map<Long, Integer> sharedAccountCopies = new HashMap<>();

        int weightTotal = 0;
        int copiesTotal = 0;

        /*
         * Settle with Users.
         */
        for (final Entry<Long, Integer> idUser : source.getUsers().entrySet()) {

            final User user = userDAO().findActiveUserById(idUser.getKey());

            /*
             * INVARIANT: User must be present.
             */
            if (user == null) {
                continue;
            }

            final int copiesWlk = addUserAccountToTrxList(targetList, user,
                    idUser.getValue(), 1, null);

            weightTotal += copiesWlk;
            copiesTotal += copiesWlk;
        }

        /*
         * Groups: settle with GROUP account.
         */
        for (final Entry<Long, Integer> idGroup : source.getGroupsAccountGroup()
                .entrySet()) {

            final UserGroup userGroup =
                    userGroupDAO().findById(idGroup.getKey());

            // TODO: logically deleted?
            if (userGroup == null) {
                continue;
            }

            final int copiesWlk = idGroup.getValue().intValue();

            if (copiesWlk == 0) {
                continue;
            }

            final Account groupAccount =
                    accountingService().lazyGetUserGroupAccount(userGroup);

            targetList.add(
                    createAccountTrxInfo(groupAccount, copiesWlk, 1, null));

            weightTotal += copiesWlk;
            copiesTotal += copiesWlk;
        }

        /*
         * Groups: settle with USER accounts.
         */
        for (final Entry<Long, Integer> idGroup : source.getGroupsAccountUser()
                .entrySet()) {

            final int copiesGroupWlk = idGroup.getValue().intValue();

            weightTotal += copiesGroupWlk;
            copiesTotal += copiesGroupWlk;

            final List<UserGroupMember> groupMembers =
                    userGroupMemberDAO().getGroupMembers(idGroup.getKey());

            int weightMemberTotal = 0;

            final int weightMember;
            final int weightMemberUnit;

            if (copiesGroupWlk % groupMembers.size() == 0) {
                weightMember = copiesGroupWlk / groupMembers.size();
                weightMemberUnit = 1;
            } else {
                weightMember = copiesGroupWlk;
                weightMemberUnit = groupMembers.size();
            }

            for (final UserGroupMember member : groupMembers) {

                final User user = member.getUser();

                if (user.getDeleted()) {
                    continue;
                }

                if (user.getDisabledPrintOut()) {
                    continue;
                }

                addUserAccountToTrxList(targetList, user, weightMember,
                        weightMemberUnit, member.getGroup().getGroupName());

                weightMemberTotal += weightMember;
            }

            // Extra trx for group.
            if (weightMemberTotal > 0) {
                final UserGroup userGroup =
                        userGroupDAO().findById(idGroup.getKey());
                final Account groupAccount =
                        accountingService().lazyGetUserGroupAccount(userGroup);
                targetList.add(createAccountTrxInfo(groupAccount,
                        Integer.valueOf(weightMemberTotal), weightMemberUnit,
                        null));
            }
        }

        /*
         * Groups: settle with SHARED accounts.
         */
        for (final Entry<Long, SimpleEntry<Long, Integer>> entry : source
                .getGroupsAccountShared().entrySet()) {

            final Long idGroup = entry.getKey();
            final UserGroup userGroup = userGroupDAO().findById(idGroup);

            // TODO: logically deleted?
            if (userGroup == null) {
                continue;
            }

            final int copiesWlk = entry.getValue().getValue();

            if (copiesWlk == 0) {
                continue;
            }

            final Long idAccount = entry.getValue().getKey();

            Integer sharedCopies = sharedAccountCopies.get(idAccount);

            if (sharedCopies == null) {
                sharedCopies = new Integer(0);
            }

            sharedCopies += copiesWlk;
            sharedAccountCopies.put(idAccount, sharedCopies);
        }

        /*
         * Extra copies: SHARED accounts.
         */
        for (final Entry<Long, Integer> entry : source.getCopiesAccountShared()
                .entrySet()) {

            final int copiesWlk = entry.getValue().intValue();

            if (copiesWlk == 0) {
                continue;
            }

            final Long idAccount = entry.getKey();

            Integer copiesShared = sharedAccountCopies.get(idAccount);

            if (copiesShared == null) {
                copiesShared = new Integer(0);
            }

            copiesShared += copiesWlk;
            sharedAccountCopies.put(idAccount, copiesShared);
        }

        /*
         * Process shared account totals.
         */
        for (final Entry<Long, Integer> entry : sharedAccountCopies
                .entrySet()) {

            final Long idAccount = entry.getKey();
            final Account account = accountDAO().findById(idAccount);

            /*
             * INVARIANT: Account must be available.
             */
            if (account == null || account.getDeleted()
                    || account.getDisabled()) {
                continue;
            }

            final Integer sharedCopies = entry.getValue();

            targetList
                    .add(createAccountTrxInfo(account, sharedCopies, 1, null));

            weightTotal += sharedCopies.intValue();
            copiesTotal += sharedCopies.intValue();
        }

        /*
         * Wrap-up
         */
        final AccountTrxInfoSet target =
                new AccountTrxInfoSet(weightTotal, copiesTotal);
        target.setAccountTrxInfoList(targetList);
        return target;
    }
}
