/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2019 Datraverse B.V.
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
package org.savapage.core.dto;

import org.apache.commons.lang3.BooleanUtils;
import org.savapage.core.jpa.User;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class UserIdDto {

    private Long dbKey;
    private String userId;
    private String fullName;
    private boolean admin;
    private boolean internalUser;

    public Long getDbKey() {
        return dbKey;
    }

    public void setDbKey(Long dbKey) {
        this.dbKey = dbKey;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public boolean isAdmin() {
        return admin;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
    }

    public boolean isInternalUser() {
        return internalUser;
    }

    public void setInternalUser(boolean internalUser) {
        this.internalUser = internalUser;
    }

    /**
     *
     * @param user
     *            User.
     * @return UserInfoDto.
     */
    public static UserIdDto create(final User user) {

        final UserIdDto dto = new UserIdDto();

        dto.setDbKey(user.getId());
        dto.setUserId(user.getUserId());
        dto.setFullName(user.getFullName());
        dto.setAdmin(BooleanUtils.isTrue(user.getAdmin()));
        dto.setInternalUser(BooleanUtils.isTrue(user.getInternal()));

        return dto;
    }
}
