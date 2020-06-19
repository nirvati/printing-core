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
package org.savapage.core.outbox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.savapage.core.outbox.OutboxInfoDto.OutboxAccountTrxInfo;
import org.savapage.core.outbox.OutboxInfoDto.OutboxAccountTrxInfoSet;

/**
 * Edits outbox printed copies and recalculate accounting.
 *
 * @author Rijk Ravestein
 *
 */
public final class OutboxCopiesEditor {

    /**
     * Account transactions: initial or result of last recalculation.
     */
    private OutboxAccountTrxInfoSet infoSetCurrent;

    /**
     * Group Account names (value) by ID (key).
     */
    private final Map<Long, String> groupById;

    /**
     * Group Account IDs (value) by name (key).
     */
    private final Map<String, Long> groupByName;

    /**
     * Account transactions by Group Account ID (used for recalculation).
     */
    private final Map<Long, List<OutboxAccountTrxInfo>> trxByGroupIdTmp =
            new HashMap<>();

    /**
     * Transactions ny Account ID not belonging to a Group Account (used for
     * recalculation).
     */
    private final Map<Long, OutboxAccountTrxInfo> trxOtherIdTmp =
            new HashMap<>();

    /**
     * Number of printed copies by Group Account ID (to be applied for
     * recalculation).
     */
    private final Map<Long, Integer> groupIdCopiesNewTmp = new HashMap<>();

    /**
     * Previous number of Group Account copies (used for recalculation).
     */
    private int groupCopiesPrvTmp;

    /**
     * @param infoSetInit
     *            Initial account transactions.
     * @param groups
     *            Group Account names by ID.
     */
    public OutboxCopiesEditor(final OutboxAccountTrxInfoSet infoSetInit,
            final Map<Long, String> groups) {

        this.infoSetCurrent = infoSetInit;

        this.groupById = groups;

        // Create reverse lookup.
        this.groupByName = new HashMap<>();
        for (final Entry<Long, String> entry : groups.entrySet()) {
            groupByName.put(entry.getValue(), entry.getKey());
        }

        this.init();
    }

    /**
     * Gets the number of printed copies of Group Account ID.
     *
     * @param accountID
     *            Group Account ID.
     * @return {@code null} if not found.
     */
    public Integer getGroupAccountCopies(final Long accountID) {
        return groupIdCopiesNewTmp.get(accountID);
    }

    /**
     * Evaluates Non-group Account copies.
     *
     * @param otherAccounts
     *            Non-group Account IDs (key) with number of copies (value).
     * @return {@code true} if at least one (1) account has a different number
     *         of copies than current state.
     */
    public boolean
            evaluateOtherAccountCopies(final Map<Long, Integer> otherAccounts) {

        for (final Entry<Long, Integer> entry : otherAccounts.entrySet()) {

            final Long accountID = entry.getKey();
            final OutboxAccountTrxInfo info = this.trxOtherIdTmp.get(accountID);

            if (info == null) {
                throw new IllegalStateException(String.format(
                        "Account [%s] not found.", accountID.toString()));
            }
            if (info.getWeight() != entry.getValue().intValue()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Prepares for calculation.
     */
    private void init() {

        this.trxByGroupIdTmp.clear();
        this.trxOtherIdTmp.clear();

        for (final OutboxAccountTrxInfo info : this.infoSetCurrent
                .getTransactions()) {

            final Long resultGroupKey;

            if (this.groupById.containsKey(info.getAccountId())) {
                resultGroupKey = info.getAccountId();
            } else {
                resultGroupKey = this.groupByName.get(info.getExtDetails());
            }

            if (resultGroupKey == null) {
                this.trxOtherIdTmp.put(info.getAccountId(), info);
            } else {
                final List<OutboxAccountTrxInfo> list;
                if (!this.trxByGroupIdTmp.containsKey(resultGroupKey)) {
                    list = new ArrayList<>();
                    this.trxByGroupIdTmp.put(resultGroupKey, list);
                } else {
                    list = this.trxByGroupIdTmp.get(resultGroupKey);
                }
                list.add(info);
            }
        }

        /*
         * Calculate number of copies for each Account Group.
         */
        this.groupIdCopiesNewTmp.clear();

        for (final Entry<Long, List<OutboxAccountTrxInfo>> entry : //
        this.trxByGroupIdTmp.entrySet()) {

            for (final OutboxAccountTrxInfo trx : entry.getValue()) {

                if (!StringUtils.isBlank(trx.getExtDetails())) {
                    continue;
                }

                if (trx.getWeight() % trx.getWeightUnit() > 0) {
                    throw new IllegalStateException(
                            "Group account weight % weightUnit > 0");
                }

                final int copies = trx.getWeight() / trx.getWeightUnit();
                this.groupIdCopiesNewTmp.put(entry.getKey(), copies);
                break;
            }
        }
    }

    /**
     * Recalculates weight and weightUnit of Group account and Group Member
     * accounts with a new number of printed group copies.
     *
     * @param groupAccountId
     *            Primary DB key of group account.
     * @param groupCopies
     *            Number of printed copies for group account.
     * @return Recalculated result.
     */
    public OutboxAccountTrxInfoSet recalcGroupCopies(final Long groupAccountId,
            final int groupCopies) {

        this.groupCopiesPrvTmp = 0;

        if (this.groupIdCopiesNewTmp.containsKey(groupAccountId)) {
            this.groupCopiesPrvTmp =
                    this.groupIdCopiesNewTmp.get(groupAccountId).intValue();
        }

        if (this.groupCopiesPrvTmp == 0) {
            throw new IllegalStateException("Group account not found.");
        }

        this.groupIdCopiesNewTmp.put(groupAccountId, groupCopies);

        final OutboxAccountTrxInfoSet trxNew = new OutboxAccountTrxInfoSet();

        final List<OutboxAccountTrxInfo> trxNewList = new ArrayList<>();

        final int weightTotalNew = this.infoSetCurrent.getWeightTotal()
                - this.groupCopiesPrvTmp + groupCopies;

        for (final Long accountId : this.groupById.keySet()) {
            this.recalcGroupCopies(trxNewList, accountId, weightTotalNew);
        }

        trxNewList.addAll(this.trxOtherIdTmp.values());

        trxNew.setWeightTotal(weightTotalNew);
        trxNew.setTransactions(trxNewList);

        this.infoSetCurrent = trxNew;
        this.init();

        return trxNew;
    }

    /**
     * Recalculates weight and weightUnit of other (non-group) accounts with a
     * new number of printed copies.
     *
     * @param otherAccounts
     *            Non-group Account IDs (key) with number of copies (value).
     * @return Recalculated result.
     */
    public OutboxAccountTrxInfoSet
            recalcOtherCopies(final Map<Long, Integer> otherAccounts) {

        // Validate
        for (final Long accountID : otherAccounts.keySet()) {
            if (!this.trxOtherIdTmp.containsKey(accountID)) {
                throw new IllegalStateException(String.format(
                        "Account [%s] not found.", accountID.toString()));
            }
        }

        // New set
        final OutboxAccountTrxInfoSet trxNew = new OutboxAccountTrxInfoSet();

        final List<OutboxAccountTrxInfo> trxNewList = new ArrayList<>();

        // Add Groups.
        for (final List<OutboxAccountTrxInfo> list : this.trxByGroupIdTmp
                .values()) {
            trxNewList.addAll(list);
        }

        int copiesDelta = 0;

        // Add other accounts.
        for (final Entry<Long, OutboxAccountTrxInfo> entry : this.trxOtherIdTmp
                .entrySet()) {

            final Long accountID = entry.getKey();
            final OutboxAccountTrxInfo info = entry.getValue();

            if (otherAccounts.containsKey(accountID)) {
                final int copiesNew = otherAccounts.get(accountID).intValue();
                if (info.getWeight() != copiesNew) {
                    copiesDelta += copiesNew - info.getWeight();
                    info.setWeight(copiesNew);
                }
            }
            trxNewList.add(info);
        }

        //
        trxNew.setWeightTotal(
                this.infoSetCurrent.getWeightTotal() + copiesDelta);
        trxNew.setTransactions(trxNewList);

        this.infoSetCurrent = trxNew;
        this.init();

        return trxNew;
    }

    /**
     * Recalculates weight and weightUnit for Group Account and User Account
     * members, and appends these recalculated transactions on list.
     *
     * @param trxNewList
     *            List to append transactions on.
     * @param groupAccountId
     *            Group Account ID.
     * @param weightTotalNew
     *            New weight total.
     * @throws IllegalStateException
     *             If more than one group account is present in current
     *             transaction set, or group members do not match group account.
     */
    private void recalcGroupCopies(final List<OutboxAccountTrxInfo> trxNewList,
            final long groupAccountId, final int weightTotalNew) {

        final int groupCopies = this.groupIdCopiesNewTmp.get(groupAccountId);
        final String groupName = this.groupById.get(groupAccountId);

        final List<OutboxAccountTrxInfo> trxPrvList =
                this.trxByGroupIdTmp.get(groupAccountId);

        final int nGroupMembers = trxPrvList.size() - 1;
        int nGroupMembersWlk = 0;

        for (final OutboxAccountTrxInfo infoPrv : trxPrvList) {

            final OutboxAccountTrxInfo infoNew = new OutboxAccountTrxInfo();
            infoNew.setAccountId(infoPrv.getAccountId());

            int weight;
            int weightUnit;

            if (StringUtils.isBlank(infoPrv.getExtDetails())) {

                if (infoPrv.getAccountId() != groupAccountId) {
                    throw new IllegalStateException("Group mismatch.");
                }

                weight = groupCopies * nGroupMembers;
                weightUnit = nGroupMembers;

            } else {

                if (!infoPrv.getExtDetails().equals(groupName)) {
                    throw new IllegalStateException("Group member mismatch.");
                }

                nGroupMembersWlk++;

                infoNew.setExtDetails(infoPrv.getExtDetails());

                weight = groupCopies;
                weightUnit = nGroupMembers;
            }

            if (weightUnit > 1 && weightUnit <= weight
                    && (weight % weightUnit == 0)) {
                weight = weight / weightUnit;
                weightUnit = 1;
            }

            infoNew.setWeight(weight);
            infoNew.setWeightUnit(weightUnit);

            trxNewList.add(infoNew);
        }

        if (nGroupMembers != nGroupMembersWlk) {
            throw new IllegalStateException("Group member count mismatch.");
        }
    }

}
