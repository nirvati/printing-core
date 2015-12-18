/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2014 Datraverse B.V.
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

import java.io.IOException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.SortedSet;

import org.apache.commons.lang3.StringUtils;
import org.savapage.core.SpException;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.dao.UserGroupDao;
import org.savapage.core.dao.UserGroupDao.SchedulePeriodEnum;
import org.savapage.core.dao.UserGroupMemberDao;
import org.savapage.core.dao.enums.ReservedUserGroupEnum;
import org.savapage.core.dao.helpers.DaoBatchCommitter;
import org.savapage.core.dto.QuickSearchItemDto;
import org.savapage.core.dto.UserAccountingDto;
import org.savapage.core.dto.UserGroupPropertiesDto;
import org.savapage.core.jpa.User;
import org.savapage.core.jpa.UserGroup;
import org.savapage.core.jpa.UserGroupMember;
import org.savapage.core.json.rpc.AbstractJsonRpcMethodResponse;
import org.savapage.core.json.rpc.JsonRpcError.Code;
import org.savapage.core.json.rpc.JsonRpcMethodError;
import org.savapage.core.json.rpc.JsonRpcMethodResult;
import org.savapage.core.json.rpc.impl.ResultListQuickSearchItem;
import org.savapage.core.json.rpc.impl.ResultListStrings;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.UserGroupService;
import org.savapage.core.users.CommonUser;
import org.savapage.core.users.IUserSource;
import org.savapage.core.util.BigDecimalUtil;

/**
 *
 * @author Datraverse B.V.
 *
 */
public final class UserGroupServiceImpl extends AbstractService implements
        UserGroupService {

    /**
     * Creates a default {@link UserGroup}.
     *
     * @param groupName
     *            The group name.
     * @return The {@link UserGroup}.
     */
    private static UserGroup createGroupDefault(final String groupName) {

        final UserGroup group = new UserGroup();

        group.setGroupName(groupName);

        group.setInitialSettingsEnabled(Boolean.TRUE);

        group.setInitiallyRestricted(Boolean.FALSE);
        group.setInitialCredit(BigDecimal.ZERO);

        group.setAllowAccumulation(Boolean.TRUE);
        group.setMaxAccumulationBalance(BigDecimal.ZERO);
        group.setScheduleAmount(BigDecimal.ZERO);
        group.setSchedulePeriod(SchedulePeriodEnum.NONE.toString());

        group.setResetStatistics(Boolean.FALSE);

        group.setCreatedBy(ServiceContext.getActor());
        group.setCreatedDate(ServiceContext.getTransactionDate());

        return group;
    }

    /**
     * Gets the reserved {@link UserGroup}. When it does not exist it is
     * created.
     *
     * @param reservedGroup
     *            The group name.
     * @return The {@link UserGroup}.
     */
    private UserGroup getOrCreateReservedGroup(
            final ReservedUserGroupEnum reservedGroup) {

        UserGroup group = userGroupDAO().find(reservedGroup);

        if (group == null) {
            group = createGroupDefault(reservedGroup.getGroupName());
            userGroupDAO().create(group);
        }
        return group;
    }

    @Override
    public UserGroup getExternalUserGroup() {
        return userGroupDAO().find(ReservedUserGroupEnum.EXTERNAL);
    }

    @Override
    public UserGroup getInternalUserGroup() {
        return userGroupDAO().find(ReservedUserGroupEnum.INTERNAL);
    }

    @Override
    public void lazyCreateReservedGroups() {
        for (ReservedUserGroupEnum value : ReservedUserGroupEnum.values()) {
            this.getOrCreateReservedGroup(value);
        }
    }

    @Override
    public AbstractJsonRpcMethodResponse listUserGroups(
            final Integer startIndex, final Integer itemsPerPage)
            throws IOException {

        final UserGroupDao.ListFilter filter = new UserGroupDao.ListFilter();

        final List<UserGroup> list =
                userGroupDAO().getListChunk(filter, startIndex, itemsPerPage,
                        UserGroupDao.Field.NAME, true);

        final List<QuickSearchItemDto> items = new ArrayList<>();

        for (final UserGroup group : list) {

            final QuickSearchItemDto dto = new QuickSearchItemDto();

            dto.setKey(group.getId());
            dto.setText(group.getGroupName());

            items.add(dto);
        }

        final ResultListQuickSearchItem data = new ResultListQuickSearchItem();
        data.setItems(items);

        return JsonRpcMethodResult.createResult(data);
    }

    @Override
    public AbstractJsonRpcMethodResponse listUserGroupMemberships(
            final String userName, final Integer startIndex,
            final Integer itemsPerPage) throws IOException {

        final User user = userDAO().findActiveUserByUserId(userName);

        /*
         * INVARIANT: user MUST exist.
         */
        if (user == null) {
            return JsonRpcMethodError.createBasicError(Code.INVALID_REQUEST,
                    "User [" + userName + "] does not exist.", null);
        }

        final UserGroupMemberDao.UserFilter filter =
                new UserGroupMemberDao.UserFilter();

        filter.setUserId(user.getId());

        final List<UserGroup> list =
                userGroupMemberDAO().getGroupChunk(filter, startIndex,
                        itemsPerPage, UserGroupMemberDao.GroupField.GROUP_NAME,
                        true);

        final List<QuickSearchItemDto> items = new ArrayList<>();

        for (final UserGroup group : list) {

            final QuickSearchItemDto dto = new QuickSearchItemDto();

            dto.setKey(group.getId());
            dto.setText(group.getGroupName());

            items.add(dto);
        }

        final ResultListQuickSearchItem data = new ResultListQuickSearchItem();
        data.setItems(items);

        return JsonRpcMethodResult.createResult(data);
    }

    @Override
    public AbstractJsonRpcMethodResponse listUserGroupMembers(
            final String groupName, final Integer startIndex,
            final Integer itemsPerPage) throws IOException {

        final UserGroup userGroup = userGroupDAO().findByName(groupName);

        /*
         * INVARIANT: group MUST exist.
         */
        if (userGroup == null) {
            return JsonRpcMethodError.createBasicError(Code.INVALID_REQUEST,
                    "Group [" + groupName + "] does not exist.", null);
        }

        final UserGroupMemberDao.GroupFilter filter =
                new UserGroupMemberDao.GroupFilter();

        filter.setGroupId(userGroup.getId());

        final List<User> list =
                userGroupMemberDAO().getUserChunk(filter, startIndex,
                        itemsPerPage, UserGroupMemberDao.UserField.USER_NAME,
                        true);

        final List<QuickSearchItemDto> items = new ArrayList<>();

        for (final User user : list) {

            final QuickSearchItemDto dto = new QuickSearchItemDto();

            dto.setKey(user.getId());
            dto.setText(user.getUserId());

            items.add(dto);
        }

        final ResultListQuickSearchItem data = new ResultListQuickSearchItem();
        data.setItems(items);

        return JsonRpcMethodResult.createResult(data);
    }

    @Override
    public boolean isReservedGroupName(final String groupName) {
        return ReservedUserGroupEnum.fromDbName(groupName) != null;
    }

    @Override
    public AbstractJsonRpcMethodResponse addUserGroup(
            final DaoBatchCommitter batchCommitter, final String groupName)
            throws IOException {

        /*
         * INVARIANT: can NOT add reserved group names.
         */
        if (isReservedGroupName(groupName)) {
            throw new SpException("Cannot add reserved groupname [" + groupName
                    + "].");
        }

        /*
         * Do NOT process request when group is already present.
         */
        if (userGroupDAO().findByName(groupName) != null) {
            return JsonRpcMethodResult.createOkResult("Group [" + groupName
                    + "] is already present.");
        }

        /*
         * INVARIANT: group MUST exist in user source.
         */

        final IUserSource userSource = ConfigManager.instance().getUserSource();

        if (!userSource.isGroupPresent(groupName)) {
            return JsonRpcMethodError.createBasicError(Code.INVALID_REQUEST,
                    "Group [" + groupName + "] does not exist in user source.",
                    null);
        }

        /*
         *
         */
        final UserGroup userGroup = new UserGroup();

        userGroup.setAllowAccumulation(Boolean.TRUE);
        userGroup.setCreatedBy(ServiceContext.getActor());
        userGroup.setCreatedDate(ServiceContext.getTransactionDate());
        userGroup.setGroupName(groupName);
        userGroup.setSchedulePeriod(SchedulePeriodEnum.NONE.toString());

        userGroupDAO().create(userGroup);

        /*
         *
         */
        int nMembersTot = 0;
        int nMembersAdd = 0;

        for (final CommonUser commonUser : userSource
                .getUsersInGroup(groupName)) {

            final User userMember =
                    userDAO().findActiveUserByUserId(commonUser.getUserName());

            if (userMember != null) {

                final UserGroupMember member = new UserGroupMember();

                member.setGroup(userGroup);
                member.setUser(userMember);

                member.setCreatedBy(ServiceContext.getActor());
                member.setCreatedDate(ServiceContext.getTransactionDate());

                userGroupMemberDAO().create(member);
                batchCommitter.increment();

                nMembersAdd++;
            }

            nMembersTot++;
        }

        return JsonRpcMethodResult.createOkResult("Group [" + groupName
                + "] added: [" + nMembersAdd + "] of [" + nMembersTot
                + "] users added as member.");
    }

    @Override
    public AbstractJsonRpcMethodResponse listUserSourceGroups()
            throws IOException {

        final IUserSource userSource = ConfigManager.instance().getUserSource();

        final List<String> items = new ArrayList<>();

        for (final String group : userSource.getGroups()) {
            items.add(group);
        }

        final ResultListStrings data = new ResultListStrings();
        data.setItems(items);

        return JsonRpcMethodResult.createResult(data);
    }

    @Override
    public AbstractJsonRpcMethodResponse listUserSourceGroupNesting(
            final String groupName) throws IOException {

        final IUserSource userSource = ConfigManager.instance().getUserSource();

        final List<String> items = new ArrayList<>();

        for (final String group : userSource.getGroupHierarchy(groupName, true)) {
            items.add(group);
        }

        final ResultListStrings data = new ResultListStrings();
        data.setItems(items);

        return JsonRpcMethodResult.createResult(data);
    }

    @Override
    public AbstractJsonRpcMethodResponse listUserSourceGroupMembers(
            final String groupName, final boolean nested) throws IOException {

        final IUserSource userSource = ConfigManager.instance().getUserSource();

        final List<String> items = new ArrayList<>();

        for (final CommonUser commonUser : userSource.getUsersInGroup(
                groupName, nested)) {
            items.add(commonUser.getUserName());
        }

        final ResultListStrings data = new ResultListStrings();
        data.setItems(items);

        return JsonRpcMethodResult.createResult(data);
    }

    @Override
    public AbstractJsonRpcMethodResponse deleteUserGroup(final Long groupId)
            throws IOException {

        final UserGroup userGroup = userGroupDAO().findById(groupId);

        /*
         * INVARIANT: group MUST exist.
         */
        if (userGroup == null) {
            return JsonRpcMethodError.createBasicError(Code.INVALID_REQUEST,
                    "Group [" + groupId + "] does not exist.", null);
        }

        return deleteUserGroup(userGroup);
    }

    @Override
    public AbstractJsonRpcMethodResponse
            deleteUserGroup(final String groupName) throws IOException {

        final UserGroup userGroup = userGroupDAO().findByName(groupName);

        /*
         * INVARIANT: group MUST exist.
         */
        if (userGroup == null) {
            return JsonRpcMethodError.createBasicError(Code.INVALID_REQUEST,
                    "Group [" + groupName + "] does not exist.", null);
        }

        return deleteUserGroup(userGroup);
    }

    /**
     *
     * Deletes a user group.
     *
     * @param userGroup
     *            The {@link UserGroup} to delete.
     *
     * @return The JSON-RPC Return message (either a result or an error);
     *
     * @throws IOException
     *             When something goes wrong.
     */
    private AbstractJsonRpcMethodResponse deleteUserGroup(
            final UserGroup userGroup) throws IOException {

        // Delete members.
        final int nMembers =
                userGroupMemberDAO().deleteGroup(userGroup.getId());

        // Delete group.
        userGroupDAO().delete(userGroup);

        return JsonRpcMethodResult.createOkResult(String.format(
                "Group [%s] with [%d] members deleted.",
                userGroup.getGroupName(), nMembers));
    }

    @Override
    public AbstractJsonRpcMethodResponse syncUserGroup(
            final DaoBatchCommitter batchCommitter, final String groupName)
            throws IOException {

        /*
         * INVARIANT: can NOT add reserved group names.
         */
        if (isReservedGroupName(groupName)) {
            throw new SpException("Cannot sync reserved groupname ["
                    + groupName + "].");
        }

        final UserGroup userGroup = userGroupDAO().findByName(groupName);

        /*
         * INVARIANT: group MUST exist.
         */
        if (userGroup == null) {
            return JsonRpcMethodError.createBasicError(Code.INVALID_REQUEST,
                    "Group [" + groupName + "] does not exist.", null);
        }

        /*
         * Initial read and counters.
         */
        final IUserSource userSource = ConfigManager.instance().getUserSource();

        final List<UserGroupMember> destination =
                userGroupMemberDAO().getGroupMembers(userGroup.getId());

        final SortedSet<CommonUser> source =
                userSource.getUsersInGroup(groupName);

        final Iterator<CommonUser> iterSrc = source.iterator();
        final Iterator<UserGroupMember> iterDst = destination.iterator();

        CommonUser objSrc = null;
        UserGroupMember objDst = null;

        if (iterSrc.hasNext()) {
            objSrc = iterSrc.next();
        }
        if (iterDst.hasNext()) {
            objDst = iterDst.next();
        }

        boolean readNextSrc;
        boolean readNextDst;
        boolean createDst;
        boolean deleteDst;
        int nCreated = 0;
        int nDeleted = 0;

        /*
         * Balanced line between User Source and User Database (since the
         * results sets are sorted by user name).
         */
        while (objSrc != null || objDst != null) {

            readNextSrc = false;
            readNextDst = false;

            createDst = false;
            deleteDst = false;

            /*
             * Compare.
             */
            if (objSrc == null) {
                // No entries (left) in source: REMOVE destination.
                deleteDst = true;
                readNextDst = true;

            } else if (objDst == null) {
                // No entries (left) in destination: ADD destination.
                createDst = true;
                readNextSrc = true;

            } else {
                final int compare =
                        objSrc.getUserName().compareTo(
                                objDst.getUser().getUserId());
                if (compare < 0) {
                    // Source < Destination: ADD destination.
                    createDst = true;
                    readNextSrc = true;
                } else if (compare > 0) {
                    // Source > Destination: REMOVE destination.
                    deleteDst = true;
                    readNextDst = true;
                } else {
                    // Source == Destination: noop.
                    readNextSrc = true;
                    readNextDst = true;
                }
            }

            /*
             * DAO persistence actions.
             */
            if (deleteDst) {

                userGroupMemberDAO().delete(objDst);
                batchCommitter.increment();

                nDeleted++;

            } else if (createDst) {
                /*
                 * INVARIANT: User MUST be present (synchronized) in destination
                 * (database).
                 */
                final User syncedUser =
                        userDAO().findActiveUserByUserId(objSrc.getUserName());

                if (syncedUser != null) {

                    final UserGroupMember member = new UserGroupMember();

                    member.setUser(syncedUser);
                    member.setGroup(userGroup);

                    member.setCreatedBy(ServiceContext.getActor());
                    member.setCreatedDate(ServiceContext.getTransactionDate());

                    userGroupMemberDAO().create(member);
                    batchCommitter.increment();

                    nCreated++;
                }
            }
            /*
             * Read next.
             */
            if (readNextSrc) {
                objSrc = null;
                if (iterSrc.hasNext()) {
                    objSrc = iterSrc.next();
                }
            }
            if (readNextDst) {
                objDst = null;
                if (iterDst.hasNext()) {
                    objDst = iterDst.next();
                }
            }
        } // end-while

        return JsonRpcMethodResult.createOkResult("Group [" + groupName
                + "] with [" + source.size() + "] members synced: added ["
                + nCreated + "] removed [" + nDeleted + "].");
    }

    @Override
    public AbstractJsonRpcMethodResponse setUserGroupProperties(
            final UserGroupPropertiesDto dto) throws IOException {

        final String groupName = dto.getGroupName();

        /*
         * INVARIANT: groupName MUST be present.
         */
        if (StringUtils.isBlank(groupName)) {
            return createError("msg-usergroup-name-is-empty");
        }

        /*
         * INVARIANT: Accounting MUST be present.
         */
        final UserAccountingDto accounting = dto.getAccounting();

        if (accounting == null) {
            return createError("msg-usergroup-accounting-is-empty");
        }

        final UserGroup jpaGroup = userGroupDAO().findByName(groupName);

        /*
         * INVARIANT: UserGroup MUST exist.
         */
        if (jpaGroup == null) {
            return createError("msg-usergroup-not-found", groupName);
        }

        boolean isUpdated = false;

        final Locale dtoLocale;

        if (accounting.getLocale() != null) {
            dtoLocale = Locale.forLanguageTag(accounting.getLocale());
        } else {
            dtoLocale = ServiceContext.getLocale();
        }

        final UserAccountingDto.CreditLimitEnum creditLimit =
                accounting.getCreditLimit();

        if (creditLimit != null) {

            //
            final boolean isRestricted =
                    creditLimit != UserAccountingDto.CreditLimitEnum.NONE;

            if (jpaGroup.getInitiallyRestricted() != isRestricted) {
                jpaGroup.setInitiallyRestricted(isRestricted);
                isUpdated = true;
            }

            //
            final boolean useGlobalOverdraft =
                    creditLimit == UserAccountingDto.CreditLimitEnum.DEFAULT;

            if (jpaGroup.getInitialUseGlobalOverdraft() != useGlobalOverdraft) {
                jpaGroup.setInitialUseGlobalOverdraft(useGlobalOverdraft);
                isUpdated = true;
            }

            //
            if (creditLimit == UserAccountingDto.CreditLimitEnum.INDIVIDUAL) {

                final String amount = accounting.getCreditLimitAmount();

                try {
                    jpaGroup.setInitialOverdraft(BigDecimalUtil.parse(amount,
                            dtoLocale, false, false));
                } catch (ParseException e) {
                    return createError("msg-amount-error", amount);
                }
                isUpdated = true;
            }
        }

        //
        final String balance = accounting.getBalance();

        if (balance != null) {
            try {
                jpaGroup.setInitialCredit(BigDecimalUtil.parse(balance,
                        dtoLocale, false, false));
            } catch (ParseException e) {
                return createError("msg-amount-error", balance);
            }
            isUpdated = true;
        }

        //
        if (isUpdated) {
            jpaGroup.setModifiedBy(ServiceContext.getActor());
            jpaGroup.setModifiedDate(ServiceContext.getTransactionDate());
            userGroupDAO().update(jpaGroup);
        }

        return JsonRpcMethodResult.createOkResult();
    }

}
