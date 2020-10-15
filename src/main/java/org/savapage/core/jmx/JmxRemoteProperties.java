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
package org.savapage.core.jmx;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.Properties;

import org.savapage.core.SpException;
import org.savapage.core.community.CommunityDictEnum;
import org.savapage.core.config.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rijk Ravestein
 *
 */
public class JmxRemoteProperties {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(JmxRemoteProperties.class);

    private static final String KEY_ADMIN = "admin";

    private static final String BASE_KEY = "com.sun.management.jmxremote";

    private static final String KEY_PORT = BASE_KEY + ".port";

    // private static final String KEY_SSL = BASE_KEY + ".ssl";
    // private static final String KEY_PASSWORD = BASE_KEY + ".password";
    // private static final String KEY_PASSWORD_FILE = BASE_KEY +
    // ".password.file";
    // private static final String KEY_ACCESS_FILE = BASE_KEY + ".access.file";

    /**
     * Basename of the properties file .
     */
    private static final String MANAGEMENT_FILENAME = "jmxremote.properties";

    /**
     * Basename of the password file.
     * <p>
     * IMPORTANT: This Password file's read access MUST be restricted, otherwise
     * Java throws an exception and the application won't start.
     * </p>
     *
     * <pre>
     * $ chmod 600 jmxremote.password
     * </pre>
     */
    private static final String PASSWORD_FILENAME = "jmxremote.password";

    private static Properties theProps;

    /**
     *
     * @return
     */
    public static String getAdminUsername() {
        return KEY_ADMIN;
    }

    /**
     *
     * @return
     */
    public static String getPort() {
        return getProperty(KEY_PORT);
    }

    synchronized private static String getProperty(String key) {
        if (theProps == null) {
            read();
        }
        return theProps.getProperty(key);
    }

    /**
     *
     * @return
     */
    public static void setAdminPassword(final String password) {

        Properties props = new Properties();

        File fileProp = new File(getPasswordFilePath());

        InputStream istr = null;
        Writer writer = null;

        try {
            istr = new java.io.FileInputStream(fileProp);
            props.load(istr);
            istr.close();
            istr = null;

            props.put(KEY_ADMIN, password);

            writer = new FileWriter(fileProp);
            props.store(writer, getPasswordFileComments());

            writer.close();
            writer = null;

        } catch (IOException e) {

            throw new SpException(e);

        } finally {

            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException e) {
                LOGGER.error(e.getMessage(), e);
            }

            try {
                if (istr != null) {
                    istr.close();
                }
            } catch (IOException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }

    }

    /**
     * Creates a comment header text for the stored {@link #PASSWORD_FILENAME}.
     *
     * @return
     */
    private static String getPasswordFileComments() {

        final String line = "---------------------------"
                + "-------------------------------";

        return line + "\n " + CommunityDictEnum.SAVAPAGE.getWord()
                + " JMX Agent"
                + "\n Keep the content of this file at a secure place.\n"
                + line;
    }

    /**
     *
     * @return
     */
    private static String getManagementFilePath() {
        return ConfigManager.getServerHome() + "/" + MANAGEMENT_FILENAME;
    }

    /**
     *
     * @return
     */
    private static String getPasswordFilePath() {
        return ConfigManager.getServerHome() + "/" + PASSWORD_FILENAME;
    }

    /**
     *
     * @return
     */
    private static void read() {

        theProps = new Properties();

        File fileProp = new File(getManagementFilePath());

        InputStream istr = null;

        try {
            if (fileProp.exists()) {
                istr = new java.io.FileInputStream(fileProp);
                theProps.load(istr);
            }

        } catch (IOException e) {

            throw new SpException(e);

        } finally {
            try {
                if (istr != null) {
                    istr.close();
                }
            } catch (IOException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
    }

}
