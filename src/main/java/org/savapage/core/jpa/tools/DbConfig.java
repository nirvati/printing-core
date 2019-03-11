/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2018 Datraverse B.V.
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
package org.savapage.core.jpa.tools;

import java.util.Map;

import javax.persistence.EntityManagerFactory;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.HibernatePersistenceProvider;

/**
 * Encapsulates database configuration.
 *
 * @author Rijk Ravestein
 *
 */
public final class DbConfig {

    /**
     * Constants only.
     */
    private DbConfig() {
    }

    /**
     * "The configuration for entity managers both inside an application server
     * and in a standalone application reside in a persistence archive. A
     * persistence archive is a JAR file which must define a persistence.xml
     * file that resides in the META-INF folder."
     * <p>
     * Use persistence.xml configuration. The map is a set of overrides that
     * will take precedence over any properties defined in your persistence.xml
     * file.
     * </p>
     * See <persistence-unit name="savapage" ...> in
     * resources/META-INF/persistence.xml
     */
    private static final String PERSISTENCE_UNIT_NAME = "savapage";

    /**
     * The name of a JDBC driver to use to connect to the database. See
     * {@link AvailableSettings#JPA_JDBC_DRIVER}.
     */
    public static final String JPA_JDBC_DRIVER =
            AvailableSettings.JPA_JDBC_DRIVER;

    /**
     * The JDBC connection user name. See
     * {@link AvailableSettings#JPA_JDBC_USER}.
     */
    private static final String JPA_JDBC_USER = AvailableSettings.JPA_JDBC_USER;

    /**
     * The JDBC connection password. See
     * {@link AvailableSettings#JPA_JDBC_PASSWORD}.
     */
    private static final String JPA_JDBC_PASSWORD =
            AvailableSettings.JPA_JDBC_PASSWORD;

    /**
     * The JDBC connection url to use to connect to the database. See
     * {@link AvailableSettings#JPA_JDBC_URL}.
     */
    public static final String JPA_JDBC_URL = AvailableSettings.JPA_JDBC_URL;

    /**
     * Names the Hibernate {@literal SQL} {@link org.hibernate.dialect.Dialect}
     * class. See {@link AvailableSettings#DIALECT}.
     */
    public static final String HIBERNATE_DIALECT = AvailableSettings.DIALECT;

    /**
     * Names the {@literal JDBC} driver class class. See
     * {@link AvailableSettings#DRIVER}.
     */
    public static final String HIBERNATE_DRIVER = AvailableSettings.DRIVER;

    /**
     * Names the
     * {@link org.hibernate.engine.jdbc.connections.spi.ConnectionProvider} to
     * use for obtaining JDBC connections. See
     * {@link AvailableSettings#CONNECTION_PROVIDER}.
     */
    public static final String HIBERNATE_CONNECTION_PROVIDER =
            AvailableSettings.CONNECTION_PROVIDER;

    /**
     * Configures the Hibernate Connection Pool.
     *
     * @param config
     *            The configuration map.
     */
    public static void configHibernateC3p0(final Map<String, Object> config) {
        /*
         * Minimum number of JDBC connections in the pool.
         */
        config.put(AvailableSettings.C3P0_MIN_SIZE, "5");
        /*
         * Maximum number of JDBC connections in the pool.
         */
        config.put(AvailableSettings.C3P0_MAX_SIZE, "400");

        /*
         * When an idle connection is removed from the pool (in second).
         * Hibernate default: 0, never expire.
         */
        config.put(AvailableSettings.C3P0_TIMEOUT, "600");

        /*
         * Number of prepared statements will be cached. Increase performance.
         * Hibernate default: 0 , caching is disable.
         */
        config.put(AvailableSettings.C3P0_MAX_STATEMENTS, "50");

        /*
         * Idle time in seconds before a connection is automatically validated.
         * Hibernate default: 0.
         *
         * IMPORTANT: this value must be LESS than C3P0_TIMEOUT. If not, the
         * connections closed by the database will not be properly detected.
         */
        config.put(AvailableSettings.C3P0_IDLE_TEST_PERIOD, "120");
    }

    /**
     * Sets the Hibernate properties for PostgrSQL.
     *
     * @param config
     *            The configuration map.
     */
    public static void
            configHibernatePostgreSQL(final Map<String, Object> config) {
        configHibernatePostgreSQL(config, null, null, null, null);
    }

    /**
     * Sets the Hibernate properties for PostgrSQL.
     *
     * @param config
     *            The configuration map.
     * @param jdbcUser
     *            User.
     * @param jdbcPassword
     *            Password.
     * @param jdbcUrl
     *            URL.
     * @param jdbcDriver
     *            Driver.
     */
    public static void configHibernatePostgreSQL(
            final Map<String, Object> config, final String jdbcUser,
            final String jdbcPassword, final String jdbcUrl,
            final String jdbcDriver) {

        config.put(HIBERNATE_DIALECT,
                "org.hibernate.dialect.PostgreSQLDialect");

        final String jdbcDriverDefault = "org.postgresql.Driver";

        if (jdbcUser != null) {
            config.put(JPA_JDBC_USER, jdbcUser);
            config.put(JPA_JDBC_PASSWORD, jdbcPassword);
            config.put(JPA_JDBC_URL, jdbcUrl);
        }
        if (jdbcDriver == null) {
            config.put(JPA_JDBC_DRIVER, jdbcDriverDefault);
        } else {
            config.put(JPA_JDBC_DRIVER, jdbcDriver);
        }
    }

    /**
     * Creates the {@link EntityManagerFactory}.
     *
     * @param config
     *            The configuration map.
     * @return The {@link EntityManagerFactory}.
     */
    public static EntityManagerFactory
            createEntityManagerFactory(final Map<String, Object> config) {
        /*
         * Since Mantis #348
         */
        final HibernatePersistenceProvider persistenceProvider =
                new HibernatePersistenceProvider();

        /*
         * "An entity manager factory is typically create at application
         * initialization time and closed at application end. It's creation is
         * an expensive process. For those who are familiar with Hibernate, an
         * entity manager factory is very much like a session factory. Actually,
         * an entity manager factory is a wrapper on top of a session factory.
         * Calls to the entityManagerFactory are thread safe."
         */
        return persistenceProvider
                .createEntityManagerFactory(PERSISTENCE_UNIT_NAME, config);
    }

}
