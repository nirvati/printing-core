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
package org.savapage.core.crypto;

import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

/**
 * A time-based user authentication token.
 *
 * @author Rijk Ravestein
 *
 */
public final class OneTimeAuthToken {

    private final static String WORD_SEPARATOR = "\n";
    private final static int NUMBER_OF_WORDS = 3;

    /**
     * .
     */
    private OneTimeAuthToken() {

    }

    /**
     * Creates a time-based authentication token for a user.
     *
     * @see {@link #isTimeBasedUserAuthTokenValid(String, String)}
     * @param userid
     *            The user id.
     * @return The authentication token.
     */
    public static String createToken(final String userid) {
        final StringBuilder msg = new StringBuilder();

        // Add extra check and uniqueness to get "random" encrypted result.
        msg.append(UUID.randomUUID().toString()).append(WORD_SEPARATOR);

        // The "real" data.
        msg.append(userid).append(WORD_SEPARATOR)
                .append(System.currentTimeMillis());

        return CryptoUser.encrypt(msg.toString());
    }

    /**
     * Checks if the user authentication token is valid.
     *
     * @param userid
     *            The user id.
     * @param token
     *            The time-based authentication token for a user.
     * @param msecExpiry
     *            The number of milliseconds after which the token expires.
     * @return {@code true} when valid (and not expired).
     */
    public static boolean isTokenValid(final String userid, final String token,
            final long msecExpiry) {

        try {
            final String decrypted = CryptoUser.decrypt(token);
            final String[] words = StringUtils.split(decrypted, WORD_SEPARATOR);

            if (words.length == NUMBER_OF_WORDS && words[1].equals(userid)) {

                // Check UUID syntax.
                UUID.fromString(words[0]);

                final long age =
                        System.currentTimeMillis() - Long.parseLong(words[2]);

                return age < msecExpiry;
            }
            return false;

        } catch (Exception e) {
            return false;
        }
    }

}
