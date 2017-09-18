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

import org.savapage.core.dao.UserGroupMemberDao;
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
     * @param weight
     *            The transaction weight.
     * @param groupName
     *            The name of the group the user was selected by, or
     *            {@code null} when a single user.
     * @return The weight of the added {@link AccountTrxInfo} (same value as
     *         weight parameter).
     */
    private static int addUserAccountToTrxList(
            final List<AccountTrxInfo> targetList, final User user,
            final Integer weight, final String groupName) {

        final UserAccount userAccount = accountingService()
                .lazyGetUserAccount(user, AccountTypeEnum.USER);

        targetList.add(createAccountTrxInfo(userAccount.getAccount(), weight,
                groupName));

        return weight.intValue();
    }

    @Override
    public AccountTrxInfoSet
            createAccountTrxInfoSet(final JsonPrintDelegation source) {

        final List<AccountTrxInfo> targetList = new ArrayList<>();

        final Map<Long, Integer> sharedAccountWeights = new HashMap<>();

        int weightTotal = 0;

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

            weightTotal += addUserAccountToTrxList(targetList, user,
                    idUser.getValue(), null);
        }

        // Filter for users in group.
        final UserGroupMemberDao.GroupFilter userGroupFilter =
                new UserGroupMemberDao.GroupFilter();

        userGroupFilter.setDisabledPrintOut(Boolean.FALSE);

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

            userGroupFilter.setGroupId(idGroup.getKey());

            final int weightWlk = idGroup.getValue().intValue()
                    * (int) userGroupMemberDAO().getUserCount(userGroupFilter);

            if (weightWlk == 0) {
                continue;
            }

            final Account groupAccount =
                    accountingService().lazyGetUserGroupAccount(userGroup);

            targetList.add(createAccountTrxInfo(groupAccount, weightWlk, null));

            weightTotal += weightWlk;
        }

        /*
         * Groups: settle with USER accounts.
         */
        for (final Entry<Long, Integer> idGroup : source.getGroupsAccountUser()
                .entrySet()) {

            int weightGroup = 0;

            for (final UserGroupMember member : userGroupMemberDAO()
                    .getGroupMembers(idGroup.getKey())) {

                final User user = member.getUser();

                if (user.getDeleted()) {
                    continue;
                }

                if (user.getDisabledPrintOut()) {
                    continue;
                }

                final int weightWlk = addUserAccountToTrxList(targetList, user,
                        idGroup.getValue(), member.getGroup().getGroupName());

                weightTotal += weightWlk;
                weightGroup += weightWlk;
            }

            if (weightGroup > 0) {

                final UserGroup userGroup =
                        userGroupDAO().findById(idGroup.getKey());
                final Account groupAccount =
                        accountingService().lazyGetUserGroupAccount(userGroup);

                targetList.add(createAccountTrxInfo(groupAccount,
                        Integer.valueOf(weightGroup), null));
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

            userGroupFilter.setGroupId(idGroup);

            final int weightWlk = entry.getValue().getValue()
                    * (int) userGroupMemberDAO().getUserCount(userGroupFilter);

            if (weightWlk == 0) {
                continue;
            }

            final Long idAccount = entry.getValue().getKey();

            Integer sharedWeight = sharedAccountWeights.get(idAccount);

            if (sharedWeight == null) {
                sharedWeight = new Integer(0);
            }

            sharedWeight += weightWlk;
            sharedAccountWeights.put(idAccount, sharedWeight);
        }

        /*
         * Extra copies: SHARED accounts.
         */
        for (final Entry<Long, Integer> entry : source.getCopiesAccountShared()
                .entrySet()) {

            final int weightWlk = entry.getValue().intValue();

            if (weightWlk == 0) {
                continue;
            }

            final Long idAccount = entry.getKey();

            Integer sharedWeight = sharedAccountWeights.get(idAccount);

            if (sharedWeight == null) {
                sharedWeight = new Integer(0);
            }

            sharedWeight += weightWlk;
            sharedAccountWeights.put(idAccount, sharedWeight);
        }

        /*
         * Process shared account totals.
         */
        for (final Entry<Long, Integer> entry : sharedAccountWeights
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
