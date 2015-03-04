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
package org.savapage.core.cometd;

/**
 *
 * @author Datraverse B.V.
 *
 */
public class CometdClientMixin {

    /**
     * Shared authentication token for all Administrator CometD User clients.
     * <p>
     * Our {@linkplain BayeuxAuthenticator} uses this token to check if the
     * requester is an Administrator User.
     * </p>
     */
    public static final String SHARED_USER_ADMIN_TOKEN = "A-"
            + java.util.UUID.randomUUID().toString();

    /**
     * Shared authentication token for all Non-Administrator CometD User
     * clients.
     * <p>
     * Our {@linkplain BayeuxAuthenticator} uses this token to check if the
     * requester is a regular User.
     * </p>
     */
    public static final String SHARED_USER_TOKEN = "U-"
            + java.util.UUID.randomUUID().toString();

    /**
     * Shared authentication token for all CometD Device clients.
     * <p>
     * Our {@linkplain BayeuxAuthenticator} uses this token to check if the
     * requester is a Device.
     * </p>
     */
    public static final String SHARED_DEVICE_TOKEN = "D-"
            + java.util.UUID.randomUUID().toString();

    /**
    *
    */
   protected CometdClientMixin() {

   }

}
