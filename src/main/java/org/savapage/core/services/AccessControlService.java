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
package org.savapage.core.services;

import org.savapage.core.dao.enums.ACLRoleEnum;
import org.savapage.core.jpa.User;
import org.savapage.core.jpa.UserGroup;
import org.savapage.core.jpa.UserGroupMember;

/**
 *
 * @author Rijk Ravestein
 *
 */
public interface AccessControlService {

    /**
     * Checks if {@link User} is authorized for a Role. Checks are done
     * bottom-up, starting at the {@link User} and moving up to the
     * {@link UserGroup} objects where user is {@link UserGroupMember} of. The
     * first encountered object with a defined {@link ACLRoleEnum} is used for
     * the check. When no reference object is found, the user is not authorized.
     *
     * @param user
     *            The {@link User}.
     * @param role
     *            The {@link ACLRoleEnum};
     * @return {@code true} when authorized.
     */
    boolean isAuthorized(User user, ACLRoleEnum role);

}
