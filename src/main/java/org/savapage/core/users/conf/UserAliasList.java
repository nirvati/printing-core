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
package org.savapage.core.users.conf;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class UserAliasList extends ConfFileReader {

    /**
     * The SingletonHolder is loaded on the first execution of
     * {@link UserAliasList#instance()} or the first access to
     * {@link SingletonHolder#INSTANCE}, not before.
     * <p>
     * <a href=
     * "http://en.wikipedia.org/wiki/Singleton_pattern#The_solution_of_Bill_Pugh"
     * >The Singleton solution of Bill Pugh</a>
     * </p>
     */
    private static class SingletonHolder {
        public static final UserAliasList INSTANCE = new UserAliasList();
    }

    /**
     * Key: alias name, Value: user name
     */
    private final Map<String, String> myAliasUser =
            new HashMap<String, String>();

    /**
     *
     */
    private UserAliasList() {

    }

    /**
     * Gets the singleton instance.
     *
     * @return The singleton.
     */
    public static UserAliasList instance() {
        return SingletonHolder.INSTANCE;
    }

    @Override
    protected void onItem(final String key, final String value) {
        myAliasUser.put(key, value);
    }

    /**
     *
     * @param file
     *            The {@link File}.
     * @return the number of aliases.
     * @throws IOException
     *             When error reading the file.
     */
    public int load(final File file) throws IOException {
        myAliasUser.clear();
        this.read(file);
        return myAliasUser.size();
    }

    /**
     * Checks if candidate user is an alias and returns the "real" user name.
     *
     * @param candidate
     *            The candidate user name.
     * @return The "real" user name if the candidate is an alias, or the
     *         candidate user name if not.
     */
    public String getUserName(final String candidate) {
        final String username = myAliasUser.get(candidate);
        if (username == null) {
            return candidate;
        }
        return username;
    }
}
