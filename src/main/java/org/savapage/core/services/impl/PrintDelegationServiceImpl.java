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
package org.savapage.core.services.impl;

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
public final class PrintDelegationServiceImpl extends AbstractService implements
        PrintDelegationService {

    /**
     * Creates an {@link AccountTrxInfo}.
     *
     * @param account
     * @param weight
     * @param groupName
     *            The name of the group the user was selected by, or
     *            {@code null} when selected as single user.
     * @return
     */
    private static AccountTrxInfo createAccountTrxInfo(final Account account,
            final Integer weight, final String groupName) {

        final AccountTrxInfo trx = new AccountTrxInfo();

        trx.setWeight(weight);
        trx.setAccount(account);

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
     * @param groupName
     *            The name of the group the user was selected by, or
     *            {@code null} when a single user.
     * @return The weight of the added {@link AccountTrxInfo}.
     */
    private static int addUserAccountToTrxList(
            final List<AccountTrxInfo> targetList, final User user,
            final String groupName) {

        final UserAccount userAccount =
                accountingService().lazyGetUserAccount(user,
                        AccountTypeEnum.USER);

        final int weightWlk = 1;

        targetList.add(createAccountTrxInfo(userAccount.getAccount(),
                Integer.valueOf(weightWlk), groupName));

        return weightWlk;
    }

    @Override
    public AccountTrxInfoSet createAccountTrxInfoSet(
            final JsonPrintDelegation source) {

        final List<AccountTrxInfo> targetList = new ArrayList<>();

        final Map<Long, Integer> sharedAccountWeights = new HashMap<>();

        int weightTotal = 0;

        /*
         * Users
         */
        for (final Long idUser : source.getUsers()) {

            final User user = userDAO().findActiveUserById(idUser);

            /*
             * INVARIANT: User must be present.
             */
            if (user == null) {
                continue;
            }

            weightTotal += addUserAccountToTrxList(targetList, user, null);
        }

        /*
         * Groups: GROUP accounts.
         */
        for (final Long idGroup : source.getGroupsAccountGroup()) {

            final UserGroup userGroup = userGroupDAO().findById(idGroup);

            // TODO: logically deleted?
            if (userGroup == null) {
                continue;
            }

            int weightWlk = (int) userGroupMemberDAO().getUserCount(idGroup);

            final Account groupAccount =
                    accountingService().lazyGetUserGroupAccount(userGroup);

            targetList.add(createAccountTrxInfo(groupAccount, weightWlk, null));

            weightTotal += weightWlk;
        }

        /*
         * Groups: USER accounts.
         */
        for (final Long idGroup : source.getGroupsAccountUser()) {

            // TODO: Add GROUP account?

            for (final UserGroupMember member : userGroupMemberDAO()
                    .getGroupMembers(idGroup)) {

                final User user = member.getUser();

                if (user.getDeleted()) {
                    continue;
                }

                weightTotal +=
                        addUserAccountToTrxList(targetList, user, member
                                .getGroup().getGroupName());
            }
        }

        /*
         * Groups: SHARED accounts.
         */
        for (final Entry<Long, Long> entry : source.getGroupsAccountShared()
                .entrySet()) {

            final Long idGroup = entry.getKey();
            final UserGroup userGroup = userGroupDAO().findById(idGroup);

            // TODO: logically deleted?
            if (userGroup == null) {
                continue;
            }

            final int weightWlk =
                    (int) userGroupMemberDAO().getUserCount(idGroup);

            if (weightWlk == 0) {
                continue;
            }

            final Long idAccount = entry.getValue();

            Integer sharedWeight = sharedAccountWeights.get(idAccount);

            if (sharedWeight == null) {
                sharedWeight = new Integer(0);
            }

            sharedWeight += weightWlk;
            sharedAccountWeights.put(idAccount, sharedWeight);
        }

        // Process shared account totals.
        for (final Entry<Long, Integer> entry : sharedAccountWeights.entrySet()) {

            final Long idAccount = entry.getKey();
            final Account account = accountDAO().findById(idAccount);

            /*
             * INVARIANT: Account must be available.
             */
            if (account == null || account.getDeleted()
                    || account.getDisabled()) {
                continue;
            }

            final Integer weight = entry.getValue();
            targetList.add(createAccountTrxInfo(account, weight, null));
            weightTotal += weight.intValue();
        }

        /*
         * Wrap-up
         */
        final AccountTrxInfoSet target = new AccountTrxInfoSet(weightTotal);
        target.setAccountTrxInfoList(targetList);
        return target;
    }
}
