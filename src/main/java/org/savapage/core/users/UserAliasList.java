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
package org.savapage.core.users;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Datraverse B.V.
 *
 */
public class UserAliasList {

    /**
     * The logger.
     */
    private static final Logger LOGGER = LoggerFactory
            .getLogger(UserAliasList.class);

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
     * @return
     */
    public static UserAliasList instance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     *
     * @param file
     */
    public void load(File file) {

        myAliasUser.clear();

        if (!file.isFile()) {
            return;
        }

        BufferedReader br = null;

        try {
            br = new BufferedReader(new FileReader(file));
            String strLine;

            while ((strLine = br.readLine()) != null) {

                strLine = strLine.trim();

                if (strLine.isEmpty()) {
                    continue;
                }
                if (strLine.charAt(0) == '#') {
                    continue;
                }
                /*
                 * The aliasname and the username may be separated by '=', ':'
                 * or TAB (a tab-delimited file).
                 */
                int i = strLine.indexOf(':');
                if (i < 0) {
                    i = strLine.indexOf('=');
                }
                if (i < 0) {
                    i = strLine.indexOf('\t');
                }
                if (i < 1) {
                    // finding the separator on index zero makes no sense
                    continue;
                }
                if (strLine.length() == (i + 1)) {
                    continue;
                }
                myAliasUser.put(strLine.substring(0, i).trim(), strLine
                        .substring(i + 1).trim());
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }
        }
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
        String username = myAliasUser.get(candidate);
        if (username == null) {
            username = candidate;
        }
        return username;
    }

}
