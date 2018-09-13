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
package org.savapage.ext.papercut;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.savapage.core.circuitbreaker.CircuitBreakerException;
import org.savapage.core.circuitbreaker.CircuitBreakerOperation;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class PaperCutDbProxy extends PaperCutDb {

    /** */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(PaperCutDbProxy.class);

    /** */
    private Connection connection;

    /**
     * Constructor.
     *
     * @param driver
     *            The JDBC driver like "org.postgresql.Driver".
     * @param url
     *            The JDBC url.
     * @param user
     *            Database user.
     * @param password
     *            Database user password.
     * @param useBreaker
     *            If {@code true} a {@link CircuitBreakerOperation} is used.
     */
    protected PaperCutDbProxy(final String driver, final String url,
            final String user, final String password,
            final boolean useBreaker) {
        super(driver, url, user, password, useBreaker);
    }

    /**
     * Creates a {@link PaperCutDbProxy} instance.
     *
     * @param dbDriver
     *            The JDBC driver like "org.postgresql.Driver".
     * @param dbUrl
     *            The JDBC url.
     * @param user
     *            The database user.
     * @param password
     *            The user password.
     * @param useCircuitBreaker
     *            If {@code true} a {@link CircuitBreakerOperation} is used.
     * @return The {@link PaperCutDbProxy} instance.
     */
    public static PaperCutDbProxy create(final String dbDriver,
            final String dbUrl, final String user, final String password,
            final boolean useCircuitBreaker) {

        return new PaperCutDbProxy(dbDriver, dbUrl, user, password,
                useCircuitBreaker);
    }

    /**
     * Creates a {@link PaperCutDbProxy} instance from the application
     * configuration.
     *
     * @param cm
     *            The {@link ConfigManager}.
     * @param useCircuitBreaker
     *            If {@code true} a {@link CircuitBreakerOperation} is used.
     * @return The {@link PaperCutDbProxy} instance.
     */
    public static PaperCutDbProxy create(final ConfigManager cm,
            final boolean useCircuitBreaker) {

        return create(cm.getConfigValue(Key.PAPERCUT_DB_JDBC_DRIVER),
                cm.getConfigValue(Key.PAPERCUT_DB_JDBC_URL),
                cm.getConfigValue(Key.PAPERCUT_DB_USER),
                cm.getConfigValue(Key.PAPERCUT_DB_PASSWORD), useCircuitBreaker);
    }

    /**
     *
     * @return The opened connection, or {@code null} when connection is closed.
     */
    public Connection getConnection() {
        return this.connection;
    }

    @Override
    public Connection openConnection() {

        closeConnection(this.connection);

        final PaperCutDbExecutor exec = new PaperCutDbExecutor(this, null) {

            @Override
            public Object execute() throws PaperCutException {

                final PaperCutDbProxy parent =
                        (PaperCutDbProxy) this.papercutDb;

                try {
                    /*
                     * Before you can connect to a PostgreSQL database, you need
                     * to load the JDBC driver. We use the Class.forName()
                     * construct.
                     */
                    Class.forName(this.papercutDb.getDbDriver());

                    parent.connection = DriverManager.getConnection(
                            this.papercutDb.getDbUrl(),
                            this.papercutDb.getDbUser(),
                            this.papercutDb.getDbPassword());
                } catch (SQLException | ClassNotFoundException e) {
                    throw new PaperCutConnectException(e.getMessage(), e);
                }
                return this;
            }
        };

        try {
            if (this.isUseCircuitBreaker()) {
                CIRCUIT_BREAKER
                        .execute(new PaperCutCircuitBreakerOperation(exec));
            } else {
                exec.execute();
            }
        } catch (PaperCutException e) {
            throw new PaperCutConnectException(e.getMessage(), e);
        } catch (InterruptedException | CircuitBreakerException e) {
            throw new PaperCutConnectException(e.getMessage(), e);
        }

        return this.connection;
    }

    @Override
    public void closeConnection(final Connection conn) {

        if (conn == null) {
            return;
        }
        if (conn != this.connection) {
            throw new IllegalStateException("Connection to close is not "
                    + "the same as opened connection.");
        }
        this.silentClose(conn);
        this.connection = null;
    }

    /**
     * Tests the PaperCut database connection.
     * <p>
     * Note:
     * </p>
     * <ul>
     * <li>No {@link CircuitBreakerOperation} is used.</li>
     * <li>The connection is closed after the test.</li>
     * </ul>
     *
     * @return {@code null} when connection succeeded, otherwise a string with
     *         the error message.
     */
    public String testConnection() {

        this.closeConnection(this.connection);

        String error = null;
        Connection connectionWlk = null;

        try {
            connectionWlk = openConnection();
        } catch (Exception e) {
            /*
             * Make sure a non-null error message is produced.
             */
            error = "" + e.getMessage();
        } finally {
            this.closeConnection(connectionWlk);
        }

        return error;
    }

}
