/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2016 Datraverse B.V.
 * Authors: Rijk Ravestein.
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
package org.savapage.core.dto;

import org.savapage.core.dao.enums.ACLRoleEnum;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class QuickSearchFilterUserGroupDto extends QuickSearchFilterDto {

    private ACLRoleEnum aclRole;

    public ACLRoleEnum getAclRole() {
        return aclRole;
    }

    public void setAclRole(ACLRoleEnum aclRole) {
        this.aclRole = aclRole;
    }

}
