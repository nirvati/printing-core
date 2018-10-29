/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2018 Datraverse B.V.
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

import java.io.IOException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;

import org.apache.commons.lang3.StringUtils;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.dao.UserGroupAttrDao;
import org.savapage.core.dao.UserGroupDao;
import org.savapage.core.dao.UserGroupDao.SchedulePeriodEnum;
import org.savapage.core.dao.UserGroupMemberDao;
import org.savapage.core.dao.enums.ACLRoleEnum;
import org.savapage.core.dao.enums.ReservedUserGroupEnum;
import org.savapage.core.dao.enums.UserGroupAttrEnum;
import org.savapage.core.dao.helpers.DaoBatchCommitter;
import org.savapage.core.dto.CreditLimitDtoEnum;
import org.savapage.core.dto.QuickSearchItemDto;
import org.savapage.core.dto.UserAccountingDto;
import org.savapage.core.dto.UserGroupPropertiesDto;
import org.savapage.core.jpa.User;
import org.savapage.core.jpa.UserGroup;
import org.savapage.core.jpa.UserGroupAttr;
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
import org.savapage.core.users.CommonUserGroup;
import org.savapage.core.users.IUserSource;
import org.savapage.core.users.conf.InternalGroupList;
import org.savapage.core.util.BigDecimalUtil;
import org.savapage.core.util.JsonHelper;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class UserGroupServiceImpl extends AbstractService
        implements UserGroupService {

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
    public UserGroup getAllUserGroup() {
        return userGroupDAO().find(ReservedUserGroupEnum.ALL);
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
    public AbstractJsonRpcMethodResponse
            listUserGroups(final Integer startIndex, final Integer itemsPerPage)
                    throws IOException {

        final UserGroupDao.ListFilter filter = new UserGroupDao.ListFilter();

        final List<UserGroup> list = userGroupDAO().getListChunk(filter,
                startIndex, itemsPerPage, UserGroupDao.Field.NAME, true);

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

        final List<UserGroup> list = userGroupMemberDAO().getGroupChunk(filter,
                startIndex, itemsPerPage,
                UserGroupMemberDao.GroupField.GROUP_NAME, true);

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

        final List<User> list = userGroupMemberDAO().getUserChunk(filter,
                startIndex, itemsPerPage,
                UserGroupMemberDao.UserField.USER_NAME, true);

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

    /**
     *
     * @param batchCommitter
     *            The {@link DaoBatchCommitter}.
     * @param groupName
     *            The name of the group to add.
     * @param groupFullName
     *            The full name of the group to add.
     * @param commonUsers
     *            The {@link CommonUser} set to add.
     * @return The number user members added.
     */
    private int addUserGroupMembers(final DaoBatchCommitter batchCommitter,
            final String groupName, final String groupFullName,
            final Set<CommonUser> commonUsers) {

        final UserGroup userGroup = new UserGroup();

        userGroup.setAllowAccumulation(Boolean.TRUE);
        userGroup.setCreatedBy(ServiceContext.getActor());
        userGroup.setCreatedDate(ServiceContext.getTransactionDate());
        userGroup.setGroupName(groupName);
        userGroup.setFullName(groupFullName);
        userGroup.setSchedulePeriod(SchedulePeriodEnum.NONE.toString());

        userGroupDAO().create(userGroup);

        int nMembersAdd = 0;

        for (final CommonUser commonUser : commonUsers) {

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
        }
        return nMembersAdd;
    }

    /**
     * Checks invariants when adding a user group. Throws an unchecked exception
     * when invariant is violated.
     *
     * @param groupName
     *            The {@link UserGroup} name.
     */
    private void checkAddGroupInvariants(final String groupName) {

        /*
         * INVARIANT: can NOT add reserved group names.
         */
        if (isReservedGroupName(groupName)) {
            throw new IllegalArgumentException(String
                    .format("Cannot add reserved groupname [%s]", groupName));
        }
    }

    @Override
    public int addInternalUserGroup(final DaoBatchCommitter batchCommitter,
            final String groupName) throws IOException {

        checkAddGroupInvariants(groupName);

        final SortedSet<CommonUser> members =
                InternalGroupList.getUsersInGroup(groupName);

        return this.addUserGroupMembers(batchCommitter, groupName, null,
                members);
    }

    @Override
    public AbstractJsonRpcMethodResponse addUserGroup(
            final DaoBatchCommitter batchCommitter, final String groupName)
            throws IOException {

        checkAddGroupInvariants(groupName);

        /*
         * Do NOT process request when group is already present.
         */
        if (userGroupDAO().findByName(groupName) != null) {
            return JsonRpcMethodResult.createOkResult(
                    "Group [" + groupName + "] is already present.");
        }

        /*
         * INVARIANT (extra): group MUST exist in user source.
         */
        final IUserSource userSource = ConfigManager.instance().getUserSource();

        final CommonUserGroup commonUserGroup = userSource.getGroup(groupName);

        if (commonUserGroup == null) {
            return JsonRpcMethodError.createBasicError(Code.INVALID_REQUEST,
                    "Group [" + groupName + "] does not exist in user source.",
                    null);
        }

        // Note: nested is 'true'
        final SortedSet<CommonUser> members =
                userSource.getUsersInGroup(groupName, true);

        final int nMembersTot = members.size();

        final int nMembersAdd = this.addUserGroupMembers(batchCommitter,
                groupName, commonUserGroup.getFullName(), members);

        return JsonRpcMethodResult.createOkResult(
                "Group [" + groupName + "] added: [" + nMembersAdd + "] of ["
                        + nMembersTot + "] users added as member.");
    }

    @Override
    public AbstractJsonRpcMethodResponse listUserSourceGroups()
            throws IOException {

        final IUserSource userSource = ConfigManager.instance().getUserSource();

        final List<String> items = new ArrayList<>();

        for (final CommonUserGroup group : userSource.getGroups()) {
            items.add(group.getGroupName());
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

        for (final String group : userSource.getGroupHierarchy(groupName,
                true)) {
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

        for (final CommonUser commonUser : userSource.getUsersInGroup(groupName,
                nested)) {
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
    public AbstractJsonRpcMethodResponse deleteUserGroup(final String groupName)
            throws IOException {

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
    private AbstractJsonRpcMethodResponse
            deleteUserGroup(final UserGroup userGroup) throws IOException {

        // Delete members.
        final int nMembers =
                userGroupMemberDAO().deleteGroup(userGroup.getId());

        // Delete attributes.
        userGroupAttrDAO().deleteGroup(userGroup.getId());

        // Delete group.
        userGroupDAO().delete(userGroup);

        return JsonRpcMethodResult.createOkResult(
                String.format("Group [%s] with [%d] members deleted.",
                        userGroup.getGroupName(), nMembers));
    }

    /**
     * Synchronizes a user group with a user source. Internal and (synchronized)
     * external users are added or removed as member.
     *
     * @param batchCommitter
     *            The {@link DaoBatchCommitter}.
     * @param userGroup
     *            The {@link UserGroup}.
     * @param source
     *            The sorted users from the source.
     * @param externalGroup
     *            If {@code true}, this is an external group sync, when
     *            {@code false} an internal sync.
     * @return The JSON-RPC Return message (either a result or an error);
     */
    private AbstractJsonRpcMethodResponse syncUserGroupMembers(
            final DaoBatchCommitter batchCommitter, final UserGroup userGroup,
            final SortedSet<CommonUser> source, final boolean externalGroup) {

        final List<UserGroupMember> destination =
                userGroupMemberDAO().getGroupMembers(userGroup.getId());

        final Iterator<CommonUser> iterSrc = source.iterator();
        final Iterator<UserGroupMember> iterDst = destination.iterator();

        /*
         * Initial read and counters.
         */
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
                final int compare = objSrc.getUserName()
                        .compareTo(objDst.getUser().getUserId());
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

        final StringBuilder msg = new StringBuilder();

        if (externalGroup) {
            msg.append("External");
        } else {
            msg.append("Internal");
        }
        msg.append(" Group [").append(userGroup.getGroupName())
                .append("] with [").append(source.size())
                .append("] members synced: added [").append(nCreated)
                .append("] removed [").append(nDeleted).append("].");

        return JsonRpcMethodResult.createOkResult(msg.toString());
    }

    /**
     * Checks invariants when syncing a user group. Throws an unchecked
     * exception when invariant is violated.
     *
     * @param groupName
     *            The {@link UserGroup} name.
     * @return The {@link UserGroup} or {@code null} when not found.
     */
    private UserGroup checkSyncGroupInvariants(final String groupName) {

        /*
         * INVARIANT: can NOT sync reserved group names.
         */
        if (isReservedGroupName(groupName)) {
            throw new IllegalArgumentException(String
                    .format("Cannot sync reserved groupname [%s]", groupName));
        }
        return userGroupDAO().findByName(groupName);
    }

    @Override
    public AbstractJsonRpcMethodResponse syncInternalUserGroup(
            final DaoBatchCommitter batchCommitter, final String groupName)
            throws IOException {

        final UserGroup userGroup = checkSyncGroupInvariants(groupName);
        final SortedSet<CommonUser> source =
                InternalGroupList.getUsersInGroup(groupName);
        return syncUserGroupMembers(batchCommitter, userGroup, source, false);
    }

    @Override
    public AbstractJsonRpcMethodResponse syncUserGroup(
            final DaoBatchCommitter batchCommitter, final String groupName)
            throws IOException {

        final UserGroup userGroup = checkSyncGroupInvariants(groupName);

        /*
         * INVARIANT: group MUST exist.
         */
        if (userGroup == null) {
            return JsonRpcMethodError.createBasicError(Code.INVALID_REQUEST,
                    "Group [" + groupName + "] does not exist.", null);
        }

        final IUserSource userSource = ConfigManager.instance().getUserSource();

        /*
         * Note: commonUserGroup can be null, when user groups are present from
         * previous user source type.
         */
        final CommonUserGroup commonUserGroup = userSource.getGroup(groupName);

        if (commonUserGroup != null && !StringUtils
                .defaultString(userGroup.getFullName()).equals(StringUtils
                        .defaultString(commonUserGroup.getFullName()))) {
            userGroup.setFullName(commonUserGroup.getFullName());
            userGroup.setModifiedBy(ServiceContext.getActor());
            userGroup.setModifiedDate(ServiceContext.getTransactionDate());
            userGroupDAO().update(userGroup);
        }

        final SortedSet<CommonUser> source =
                userSource.getUsersInGroup(groupName, true);

        return syncUserGroupMembers(batchCommitter, userGroup, source, true);
    }

    /**
     * Sets User Group accounting properties.
     *
     * @param jpaGroup
     *            The {@link UserGroup}.
     * @param accounting
     *            The accounting data.
     * @return {@code null} if no error.
     */
    private AbstractJsonRpcMethodResponse setUserGroupProperties(
            final UserGroup jpaGroup, final UserAccountingDto accounting) {
        boolean isUpdated = false;

        final Locale dtoLocale;

        if (accounting.getLocale() != null) {
            dtoLocale = Locale.forLanguageTag(accounting.getLocale());
        } else {
            dtoLocale = ServiceContext.getLocale();
        }

        final CreditLimitDtoEnum creditLimit = accounting.getCreditLimit();

        if (creditLimit != null) {

            //
            final boolean isRestricted = creditLimit != CreditLimitDtoEnum.NONE;

            if (jpaGroup.getInitiallyRestricted() != isRestricted) {
                jpaGroup.setInitiallyRestricted(isRestricted);
                isUpdated = true;
            }

            //
            final boolean useGlobalOverdraft =
                    creditLimit == CreditLimitDtoEnum.DEFAULT;

            if (jpaGroup.getInitialUseGlobalOverdraft() != useGlobalOverdraft) {
                jpaGroup.setInitialUseGlobalOverdraft(useGlobalOverdraft);
                isUpdated = true;
            }

            //
            if (creditLimit == CreditLimitDtoEnum.INDIVIDUAL) {

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
                jpaGroup.setInitialCredit(
                        BigDecimalUtil.parse(balance, dtoLocale, false, false));
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

        return null;
    }

    /**
     * Sets User Group ACLRole properties.
     *
     * @param userGroup
     *            The {@link UserGroup}.
     * @param roleUpdate
     *            The roles.
     * @throws IOException
     *             When JSON IO errors.
     */
    private void setUserGroupProperties(final UserGroup userGroup,
            final Map<ACLRoleEnum, Boolean> roleUpdate) throws IOException {

        final UserGroupAttrEnum attrEnum = UserGroupAttrEnum.ACL_ROLES;

        final UserGroupAttrDao daoAttr =
                ServiceContext.getDaoContext().getUserGroupAttrDao();

        final UserGroupAttr attr =
                userGroupAttrDAO().findByName(userGroup, attrEnum);

        final Map<ACLRoleEnum, Boolean> currentRoles;

        if (attr == null) {
            currentRoles = new HashMap<>();
        } else {
            currentRoles = JsonHelper.createEnumBooleanMap(ACLRoleEnum.class,
                    attr.getValue());
        }

        /*
         * Merge the update with the current roles.
         */
        for (final Entry<ACLRoleEnum, Boolean> entry : roleUpdate.entrySet()) {

            final ACLRoleEnum key = entry.getKey();
            final Boolean value = entry.getValue();

            if (value == null) {
                if (currentRoles.containsKey(key)) {
                    currentRoles.remove(key);
                }
            } else {
                currentRoles.put(key, value);
            }
        }

        /*
         * Process changes.
         */

        if (attr == null && currentRoles.isEmpty()) {
            return;
        }

        final String jsonRoles = JsonHelper.stringifyObject(currentRoles);

        if (attr == null) {

            final UserGroupAttr attrNew = new UserGroupAttr();

            attrNew.setUserGroup(userGroup);
            attrNew.setName(attrEnum.getName());
            attrNew.setValue(jsonRoles);

            daoAttr.create(attrNew);

        } else if (currentRoles.isEmpty()) {

            daoAttr.delete(attr);

        } else if (!attr.getValue().equals(jsonRoles)) {

            attr.setValue(jsonRoles);
            daoAttr.update(attr);
        }
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
         * INVARIANT: Accounting or Roles MUST be present.
         */
        final UserAccountingDto accounting = dto.getAccounting();
        final Map<ACLRoleEnum, Boolean> roleUpdate = dto.getRoleUpdate();

        if (accounting == null && roleUpdate == null) {
            return createError("msg-usergroup-data-is-empty");
        }

        final UserGroup jpaGroup = userGroupDAO().findByName(groupName);

        /*
         * INVARIANT: UserGroup MUST exist.
         */
        if (jpaGroup == null) {
            return createError("msg-usergroup-not-found", groupName);
        }

        /*
         * Accounting?
         */
        if (accounting != null) {
            final AbstractJsonRpcMethodResponse rsp =
                    this.setUserGroupProperties(jpaGroup, accounting);
            if (rsp != null) {
                return rsp;
            }
        }

        /*
         * Roles?
         */
        if (roleUpdate != null) {
            this.setUserGroupProperties(jpaGroup, roleUpdate);
        }

        return JsonRpcMethodResult.createOkResult();
    }

}
