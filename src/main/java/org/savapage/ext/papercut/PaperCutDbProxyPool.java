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

import java.beans.PropertyVetoException;
import java.sql.Connection;
import java.sql.SQLException;

import org.savapage.core.circuitbreaker.CircuitBreakerException;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;

import com.mchange.v2.c3p0.ComboPooledDataSource;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class PaperCutDbProxyPool extends PaperCutDb {

    /**
     * Connection pool.
     */
    private final ComboPooledDataSource cpds;

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
     *            If {@code true} a {@link PaperCutCircuitBreakerOperation} is
     *            used.
     */
    public PaperCutDbProxyPool(final String driver, final String url,
            final String user, final String password,
            final boolean useBreaker) {

        super(driver, url, user, password, useBreaker);

        this.cpds = new ComboPooledDataSource();

        try {
            cpds.setDriverClass(driver);
        } catch (PropertyVetoException e) {
            throw new PaperCutConnectException(e.getMessage(), e);
        }
        cpds.setJdbcUrl(url);
        cpds.setUser(user);
        cpds.setPassword(password);
    }

    /**
     * Creates a {@link PaperCutDbProxyPool} instance from the application
     * configuration.
     *
     * @param cm
     *            The {@link ConfigManager}.
     * @param useCircuitBreaker
     *            If {@code true} a {@link PaperCutCircuitBreakerOperation} is
     *            used.
     * @return The {@link PaperCutDbProxy} instance.
     */
    public static PaperCutDbProxyPool create(final ConfigManager cm,
            final boolean useCircuitBreaker) {

        return new PaperCutDbProxyPool(
                cm.getConfigValue(Key.PAPERCUT_DB_JDBC_DRIVER),
                cm.getConfigValue(Key.PAPERCUT_DB_JDBC_URL),
                cm.getConfigValue(Key.PAPERCUT_DB_USER),
                cm.getConfigValue(Key.PAPERCUT_DB_PASSWORD), useCircuitBreaker);
    }

    @Override
    public Connection openConnection() {

        final PaperCutDbExecutor exec = new PaperCutDbExecutor(this, null) {

            @Override
            public Object execute() throws PaperCutException {

                try {
                    this.setConnection(cpds.getConnection());
                } catch (SQLException e) {
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

        return exec.getConnection();
    }

    @Override
    public void closeConnection(final Connection conn) {
        this.silentClose(conn);
    }

    /**
     * Cleans up associated resources quickly. Use this method when you will no
     * longer be using this DataSource.
     */
    public void close() {
        cpds.close();
    }
}
