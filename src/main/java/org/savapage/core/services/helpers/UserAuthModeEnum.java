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
package org.savapage.core.services.helpers;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

/**
 * Authentication mode.
 *
 * @author Rijk Ravestein
 *
 */
public enum UserAuthModeEnum {

    /**
     * Username/password.
     */
    NAME("name"),
    /**
     * Email/password.
     */
    EMAIL("email"),
    /**
     * ID.
     */
    ID("id"),
    /**
     * Local NFC.
     */
    CARD_LOCAL("nfc-local"),
    /**
     * Network NFC.
     */
    CARD_IP("nfc-network"),
    /**
     * OAuth (any).
     */
    OAUTH("oauth"),
    /**
     * YubiKey.
     */
    YUBIKEY("yubikey");

    /**
     * Value as stored in database.
     * <p>
     * <b>NOTE</b>: Value is used as URL parameter at WebApp Login.
     * </p>
     */
    private String dbValue;

    /**
     *
     * @param value
     *            The database value.
     */
    UserAuthModeEnum(final String value) {
        this.dbValue = value;
    }

    /**
     * Gets the value as stored in database.
     * <p>
     * <b>NOTE</b>: Value is used as URL parameter at WebApp Login.
     * </p>
     *
     * @return String value.
     */
    public String toDbValue() {
        return dbValue;
    }

    /**
     * Gets {@link UserAuthModeEnum} of the database value.
     *
     * @param dbValue
     *            The database value.
     * @return {@code null} when not found.
     */
    public static UserAuthModeEnum fromDbValue(final String dbValue) {

        if (dbValue.equals(UserAuthModeEnum.NAME.dbValue)) {
            return UserAuthModeEnum.NAME;
        } else if (dbValue.equals(UserAuthModeEnum.EMAIL.dbValue)) {
            return UserAuthModeEnum.EMAIL;
        } else if (dbValue.equals(UserAuthModeEnum.ID.dbValue)) {
            return UserAuthModeEnum.ID;
        } else if (dbValue.equals(UserAuthModeEnum.CARD_IP.dbValue)) {
            return UserAuthModeEnum.CARD_IP;
        } else if (dbValue.equals(UserAuthModeEnum.CARD_LOCAL.dbValue)) {
            return UserAuthModeEnum.CARD_LOCAL;
        } else if (dbValue.equals(UserAuthModeEnum.OAUTH.dbValue)) {
            return UserAuthModeEnum.OAUTH;
        } else if (dbValue.equals(UserAuthModeEnum.YUBIKEY.dbValue)) {
            return UserAuthModeEnum.YUBIKEY;
        }
        return null;
    }

    /**
     *
     * @param list
     *            A " ,;:" separated string of dbValues.
     * @return The enum list.
     */
    public static List<UserAuthModeEnum> parseList(final String list) {

        final List<UserAuthModeEnum> enumList = new ArrayList<>();

        if (StringUtils.isNotBlank(list)) {
            for (final String dbValue : StringUtils.split(list, " ,;:")) {
                final UserAuthModeEnum enumWlk = fromDbValue(dbValue);
                if (enumWlk == null) {
                    throw new IllegalArgumentException(list);
                }
                enumList.add(enumWlk);
            }
        }
        return enumList;
    }

}
