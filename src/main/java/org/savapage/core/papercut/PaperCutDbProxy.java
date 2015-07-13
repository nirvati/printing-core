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
package org.savapage.core.papercut;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.savapage.core.circuitbreaker.CircuitBreaker;
import org.savapage.core.circuitbreaker.CircuitBreakerException;
import org.savapage.core.circuitbreaker.CircuitBreakerOperation;
import org.savapage.core.circuitbreaker.CircuitNonTrippingException;
import org.savapage.core.circuitbreaker.CircuitTrippingException;
import org.savapage.core.config.CircuitBreakerEnum;
import org.savapage.core.config.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Datraverse B.V.
 *
 */
public final class PaperCutDbProxy {

    /**
     * The varchar length of the {@code "document_name"} column in the
     * {@code "tbl_printer_usage_log"} table.
     */
    public static final int COL_LEN_DOCUMENT_NAME = 255;

    /**
     * The varchar length of the {@code "txn_comment"} column in the
     * {@code "tbl_account_transaction"} table.
     */
    public static final int COL_LEN_TXN_COMMENT = 255;

    /**
     * .
     */
    private static final Logger LOGGER = LoggerFactory
            .getLogger(PaperCutDbProxy.class);

    /**
    *
    */
    private static final CircuitBreaker CIRCUIT_BREAKER = ConfigManager
            .getCircuitBreaker(CircuitBreakerEnum.PAPERCUT_CONNECTION);

    /**
    *
    */
    private final boolean useCircuitBreaker;

    /**
     * E.g. {@code "jdbc:postgresql://localhost/papercut"}.
     */
    private final String dbUrl;
    private final String dbUser;
    private final String dbPassword;

    private Connection connection;

    /**
     *
     */
    private static abstract class PaperCutDbExecutor {

        protected final PaperCutDbProxy dbProxy;

        public PaperCutDbExecutor(PaperCutDbProxy dbProxy) {
            this.dbProxy = dbProxy;
        }

        abstract Object execute() throws PaperCutException;

    }

    /**
     * A {@link CircuitBreakerOperation} wrapper for the
     * {@link PaperCutProxy#execute(String, Vector))} method.
     *
     * @author Datraverse B.V.
     *
     */
    private static class PaperCutCircuitBreakerOperation implements
            CircuitBreakerOperation {

        private final PaperCutDbExecutor executor;

        /**
         * Constructor.
         *
         * @param executor
         *            The executor.
         */
        public PaperCutCircuitBreakerOperation(final PaperCutDbExecutor executor) {
            this.executor = executor;
        }

        @Override
        public Object execute(final CircuitBreaker circuitBreaker) {

            try {
                return executor.execute();
            } catch (PaperCutException e) {
                throw new CircuitNonTrippingException(e.getMessage(), e);
            } catch (PaperCutConnectException e) {
                throw new CircuitTrippingException(e.getMessage(), e);
            }
        }

    };

    /**
     * Constructor.
     *
     * @param url
     * @param user
     * @param password
     * @param useCircuitBreaker
     *            If {@code true} a {@link CircuitBreakerOperation} is used.
     */
    private PaperCutDbProxy(final String url, final String user,
            final String password, final boolean useCircuitBreaker) {

        this.dbUrl = url;
        this.dbUser = user;
        this.dbPassword = password;
        this.useCircuitBreaker = useCircuitBreaker;
    }

    /**
     * Creates a {@link PaperCutDbProxy} instance.
     *
     * @param dbUrl
     *            The JDBC url.
     * @param user
     * @param password
     * @param useCircuitBreaker
     *            If {@code true} a {@link CircuitBreakerOperation} is used.
     * @return The {@link PaperCutDbProxy} instance.
     */
    public static final PaperCutDbProxy create(final String dbUrl,
            final String user, final String password,
            final boolean useCircuitBreaker) {

        return new PaperCutDbProxy(dbUrl, user, password, useCircuitBreaker);
    }

    /**
     * Closes the current connection (if present) and re-establishes the
     * connection.
     */
    public void connect() {

        disconnect();

        final PaperCutDbExecutor exec = new PaperCutDbExecutor(this) {

            @Override
            public Object execute() throws PaperCutException {
                try {
                    this.dbProxy.connection =
                            DriverManager.getConnection(this.dbProxy.dbUrl,
                                    this.dbProxy.dbUser,
                                    this.dbProxy.dbPassword);
                } catch (SQLException e) {
                    throw new PaperCutConnectException(e.getMessage(), e);
                }
                return this;
            }
        };

        try {
            if (this.useCircuitBreaker) {
                CIRCUIT_BREAKER.execute(new PaperCutCircuitBreakerOperation(
                        exec));
            } else {
                exec.execute();
            }
        } catch (PaperCutException e) {
            throw new PaperCutConnectException(e.getMessage(), e);
        } catch (InterruptedException | CircuitBreakerException e) {
            throw new PaperCutConnectException(e.getMessage(), e);
        }
    }

    /**
     * Closes the connection (silently).
     */
    public void disconnect() {

        if (this.connection != null) {
            try {
                this.connection.close();
            } catch (SQLException e) {
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn(e.getMessage());
                }
            }
            this.connection = null;
        }
    }

    /**
     * Closes resources while numbing exceptions.
     *
     * @param resultset
     *            A {@link ResultSet} to close.
     * @param statement
     *            A {@link Statement} to close.
     */
    private void silentClose(final ResultSet resultset,
            final Statement statement) {

        if (resultset != null) {
            try {
                resultset.close();
            } catch (SQLException e) {
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn(e.getMessage());
                }
            }
        }
        if (statement != null) {
            try {
                statement.close();
            } catch (SQLException e) {
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn(e.getMessage());
                }
            }
        }
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

        String error = null;

        disconnect();

        try {
            connect();
        } catch (Exception e) {
            /*
             * Make sure a non-null error message is produced.
             */
            error = "" + e.getMessage();
        } finally {
            disconnect();
        }

        return error;
    }

    /**
     * Gets the {@link PaperCutPrinterUsageLog} for unique document names.
     *
     * @param uniqueDocNames
     *            A set with document names.
     * @return A list with an {@link PaperCutPrinterUsageLog} object for each
     *         title in the input set.
     */
    @SuppressWarnings("unchecked")
    public List<PaperCutPrinterUsageLog> getPrinterUsageLog(
            final Set<String> uniqueDocNames) {

        final PaperCutDbExecutor exec = new PaperCutDbExecutor(this) {

            @Override
            public Object execute() throws PaperCutException {
                try {
                    return this.dbProxy.getPrinterUsageLogSql(uniqueDocNames);
                } catch (SQLException e) {
                    throw new PaperCutConnectException(e.getMessage(), e);
                }
            }
        };

        final Object result;

        try {
            if (this.useCircuitBreaker) {
                result =
                        CIRCUIT_BREAKER
                                .execute(new PaperCutCircuitBreakerOperation(
                                        exec));
            } else {
                result = exec.execute();
            }
        } catch (PaperCutException e) {
            throw new PaperCutConnectException(e.getMessage(), e);
        } catch (InterruptedException | CircuitBreakerException e) {
            throw new PaperCutConnectException(e.getMessage(), e);
        }

        return (List<PaperCutPrinterUsageLog>) result;
    }

    /**
     * Gets the {@link PaperCutPrinterUsageLog} for unique document names.
     *
     * @param uniqueDocNames
     *            A set with document names.
     * @return A list with an {@link PaperCutPrinterUsageLog} object for each
     *         title in the input set.
     * @throws SQLException
     *             When an SQL error occurs.
     */
    private List<PaperCutPrinterUsageLog> getPrinterUsageLogSql(
            final Set<String> uniqueDocNames) throws SQLException {

        final StringBuilder sql = new StringBuilder(256);

        sql.append("SELECT document_name, printed, cancelled, denied_reason, "
                + "usage_cost, charged_to_account_id, assoc_with_account_id"
                + " FROM tbl_printer_usage_log WHERE job_type = 'PRINT'"
                + " AND document_name IN(");

        final Iterator<String> iterUniqueTitle = uniqueDocNames.iterator();
        int nCounter = 0;

        while (iterUniqueTitle.hasNext()) {
            if (nCounter > 0) {
                sql.append(", ");
            }
            sql.append('\'');
            sql.append(iterUniqueTitle.next()).append('\'');
            nCounter++;
        }

        sql.append(")");

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(sql.toString());
        }

        final List<PaperCutPrinterUsageLog> usageLogList = new ArrayList<>();

        Statement statement = null;
        ResultSet resultset = null;

        try {

            statement = this.connection.createStatement();
            resultset = statement.executeQuery(sql.toString());

            while (resultset.next()) {

                final PaperCutPrinterUsageLog usageLog =
                        new PaperCutPrinterUsageLog();

                usageLog.setDocumentName(resultset.getString("document_name"));

                usageLog.setPrinted(resultset.getString("printed")
                        .equalsIgnoreCase("Y"));
                usageLog.setCancelled(resultset.getString("cancelled")
                        .equalsIgnoreCase("Y"));

                usageLog.setDeniedReason(resultset.getString("denied_reason"));

                usageLog.setUsageCost(resultset.getDouble("usage_cost"));

                usageLog.setAccountIdAssoc(resultset
                        .getLong("assoc_with_account_id"));
                usageLog.setAccountIdCharged(resultset
                        .getLong("charged_to_account_id"));

                //
                usageLogList.add(usageLog);
            }

        } finally {
            silentClose(resultset, statement);
        }

        return usageLogList;
    }
}
