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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.core.users;

import org.savapage.core.config.ConfigManager;
import org.savapage.core.dao.enums.UserAttrEnum;
import org.savapage.core.jpa.User;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.UserService;

/**
 * Authenticator for internal users only.
 *
 * @author Rijk Ravestein
 *
 */
public final class InternalUserAuthenticator {

    /** */
    private static final UserService USER_SERVICE =
            ServiceContext.getServiceFactory().getUserService();

    /**
     * Utility class.
     */
    private InternalUserAuthenticator() {
    }

    /**
     *
     * @param em
     * @param user
     * @param password
     * @return
     */
    public static boolean authenticate(final User user, final String password) {

        final String checkSum = USER_SERVICE.findUserAttrValue(user.getId(),
                UserAttrEnum.INTERNAL_PASSWORD);

        return ConfigManager.instance().isUserPasswordValid(checkSum,
                user.getUserId(), password);
    }

}
