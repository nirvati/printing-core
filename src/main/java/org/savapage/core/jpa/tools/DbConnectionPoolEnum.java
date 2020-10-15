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
package org.savapage.core.jpa.tools;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.hibernate.cfg.AvailableSettings;

/**
 * Database connection pool parameters.
 * <p>
 * <a href="https://www.mchange.com/projects/c3p0/">c3p0 - JDBC3 Connection and
 * Statement Pooling</a>
 * </p>
 *
 * @author Rijk Ravestein
 *
 */
public enum DbConnectionPoolEnum {
    /**
     * Minimum number of JDBC connections in the pool.
     */
    MIN_SIZE("database.connection.pool.min", AvailableSettings.C3P0_MIN_SIZE,
            "5"),
    /**
     * Maximum number of JDBC connections in the pool.
     */
    MAX_SIZE("database.connection.pool.max", AvailableSettings.C3P0_MAX_SIZE,
            "200"),

    /**
     * When an idle connection is removed from the pool (in second). Hibernate
     * default: 0, never expire.
     */
    TIMEOUT_SECS("database.connection.idle-timeout-secs",
            AvailableSettings.C3P0_TIMEOUT, "600"),

    /**
     * Idle time in seconds before a connection is automatically validated.
     * (Hibernate default: 0).
     *
     * IMPORTANT: this value must be LESS than {@link #TIMEOUT_SECS}. If not,
     * the connections closed by the database will not be properly detected.
     */
    TIMEOUT_TEST_SECS("database.connection.idle-timeout-test-secs",
            AvailableSettings.C3P0_IDLE_TEST_PERIOD, "120"),

    /**
     * Number of prepared statements that will be cached. Increase performance.
     * Hibernate default: 0 , caching is disable.
     */
    STATEMENTS_CACHE("database.connection.statement-cache",
            AvailableSettings.C3P0_MAX_STATEMENTS, "50");

    /**
     * SavaPage config property key.
     */
    private final String configKey;

    /**
     * c3p0 config key.
     */
    private final String c3p0Key;

    /**
     * SavaPage default value.
     */
    private final String defaultValue;

    /**
     *
     * @param cfgKey
     *            The SavaPage config key.
     * @param key
     *            The c3p0 hibernate key.
     * @param value
     *            The default value.
     */
    DbConnectionPoolEnum(final String cfgKey, final String key,
            final String value) {
        this.configKey = cfgKey;
        this.c3p0Key = key;
        this.defaultValue = value;
    }

    /**
     * @return SavaPage config property key.
     */
    public String getConfigKey() {
        return configKey;
    }

    /**
     * @return c3p0 config key.
     */
    public String getC3p0Key() {
        return c3p0Key;
    }

    /**
     * @return SavaPage default value.
     */
    public String getDefaultValue() {
        return defaultValue;
    }

    /**
     * Creates key value map from server configuration.
     *
     * @param serverConfig
     *            The server configuration.
     * @return Key value map.
     */
    public static Map<DbConnectionPoolEnum, String>
            createFromConfig(final Properties serverConfig) {

        final Map<DbConnectionPoolEnum, String> map = new HashMap<>();

        for (final DbConnectionPoolEnum enumVal : DbConnectionPoolEnum
                .values()) {

            map.put(enumVal, serverConfig.getProperty(enumVal.getConfigKey(),
                    enumVal.getDefaultValue()));
        }
        return map;
    }
}
