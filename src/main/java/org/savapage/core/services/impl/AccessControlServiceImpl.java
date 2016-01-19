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

import java.io.IOException;
import java.util.List;

import org.savapage.core.dao.UserGroupMemberDao;
import org.savapage.core.dao.enums.ACLRoleEnum;
import org.savapage.core.dao.enums.UserAttrEnum;
import org.savapage.core.dao.enums.UserGroupAttrEnum;
import org.savapage.core.jpa.User;
import org.savapage.core.jpa.UserAttr;
import org.savapage.core.jpa.UserGroup;
import org.savapage.core.jpa.UserGroupAttr;
import org.savapage.core.services.AccessControlService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.util.JsonHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class AccessControlServiceImpl extends AbstractService implements
        AccessControlService {

    /**
     * The logger.
     */
    private static final Logger LOGGER = LoggerFactory
            .getLogger(AccessControlServiceImpl.class);

    @Override
    public boolean isAuthorized(final User user, final ACLRoleEnum role) {

        // Check User
        final UserAttr userAttr =
                userAttrDAO().findByName(user, UserAttrEnum.ACL_ROLES);

        if (userAttr != null) {

            try {
                return JsonHelper.deserializeEnumSet(ACLRoleEnum.class,
                        userAttr.getValue()).contains(role);

            } catch (IOException e) {
                // Try to remove the culprit.
                if (ServiceContext.getDaoContext().isTransactionActive()) {
                    userAttrDAO().delete(userAttr);
                    LOGGER.warn(String.format(
                            "%s [%s] has invalid value: %s (the object "
                                    + "is deleted from the database)",
                            UserAttr.class.getSimpleName(), userAttr.getName(),
                            userAttr.getValue()));
                }
            }
        }

        // Check Groups
        final UserGroupMemberDao.UserFilter filter =
                new UserGroupMemberDao.UserFilter();

        filter.setUserId(user.getId());

        final List<UserGroup> groupList =
                userGroupMemberDAO().getGroupChunk(filter, null, null,
                        UserGroupMemberDao.GroupField.GROUP_NAME, true);

        for (final UserGroup group : groupList) {

            final UserGroupAttr groupAttr =
                    userGroupAttrDAO().findByName(group,
                            UserGroupAttrEnum.ACL_ROLES);

            if (groupAttr == null) {
                continue;
            }

            try {
                if (JsonHelper.deserializeEnumSet(ACLRoleEnum.class,
                        groupAttr.getValue()).contains(role)) {
                    return true;
                }
            } catch (IOException e) {
                // Try to remove the culprit.
                if (ServiceContext.getDaoContext().isTransactionActive()) {
                    userGroupAttrDAO().delete(groupAttr);
                    LOGGER.warn(String.format(
                            "%s [%s] has invalid value: %s (the object "
                                    + "is deleted from the database)",
                            UserGroupAttr.class.getSimpleName(),
                            groupAttr.getName(), groupAttr.getValue()));
                }
            }

        }
        return false;
    }
}
