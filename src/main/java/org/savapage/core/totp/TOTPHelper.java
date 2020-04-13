/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2020 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: 2011-2020 Datraverse B.V. <info@datraverse.com>
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
package org.savapage.core.totp;

import java.io.IOException;
import java.util.Date;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.savapage.core.crypto.CryptoUser;
import org.savapage.core.dao.enums.UserAttrEnum;
import org.savapage.core.jpa.User;
import org.savapage.core.services.UserService;
import org.savapage.core.util.JsonHelper;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class TOTPHelper {

    /** */
    private static final int RECOVERY_CODE_SIZE = 12;
    /** */
    private static final int MAX_TRIALS = 3;
    /** */
    private static final long MAX_AGE_MSEC = DateUtils.MILLIS_PER_HOUR;

    /** Utility class. */
    private TOTPHelper() {
    }

    /**
     * Generates a random recovery code.
     *
     * @return Backup code.
     */
    public static TOTPRecoveryDto generateRecoveryCode() {
        final TOTPRecoveryDto dto = new TOTPRecoveryDto();
        dto.setIssued(new Date());
        dto.setTrial(0);
        dto.setCode(RandomStringUtils.randomAlphanumeric(RECOVERY_CODE_SIZE));
        return dto;
    }

    /**
     * Verifies if code is a valid recovery code, and updates/deletes user
     * recovery code in database (within commit scope of caller).
     *
     * @param userService
     *            User service.
     * @param userDb
     *            User.
     * @param code
     *            Code to verify.
     * @return {@code true} if code is a valid recovery code.
     * @throws IOException
     *             If JSON error.
     */
    public static boolean verifyRecoveryCode(final UserService userService,
            final User userDb, final String code) throws IOException {

        if (code == null) {
            return false;
        }

        final String json = userService.getUserAttrValue(userDb,
                UserAttrEnum.TOTP_RECOVERY);

        if (json == null) {
            return false;
        }

        final TOTPRecoveryDto dto =
                JsonHelper.createOrNull(TOTPRecoveryDto.class, json);

        boolean remove = true;
        boolean valid = false;

        if (dto != null) {

            final long age = new Date().getTime() - dto.getIssued().getTime();

            if (dto.getTrial() < MAX_TRIALS && age < MAX_AGE_MSEC) {
                final int currentTrial = dto.getTrial() + 1;
                valid = code.equals(dto.getCode());
                remove = valid || currentTrial == MAX_TRIALS;
                if (!remove) {
                    dto.setTrial(currentTrial);
                    setRecoveryCodeDB(userService, userDb, dto);
                }
            }
        }

        if (remove) {
            userService.removeUserAttr(userDb, UserAttrEnum.TOTP_RECOVERY);
        }

        return valid;
    }

    /**
     * Updates/creates user recovery code in database (within commit scope of
     * caller).
     *
     * @param userService
     *            User service.
     * @param userDb
     *            User.
     * @param dto
     *            Recovery code.
     * @throws IOException
     *             If JSON error.
     */
    public static void setRecoveryCodeDB(final UserService userService,
            final User userDb, final TOTPRecoveryDto dto)
            throws IOException {

        userService.setUserAttrValue(userDb, UserAttrEnum.TOTP_RECOVERY,
                CryptoUser.encryptUserAttr(userDb.getId(),
                        JsonHelper.stringifyObject(dto)));
    }

}
