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
package org.savapage.ext.papercut;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.savapage.core.SpException;
import org.savapage.core.circuitbreaker.CircuitBreaker;
import org.savapage.core.circuitbreaker.CircuitBreakerException;
import org.savapage.core.circuitbreaker.CircuitBreakerOperation;
import org.savapage.core.circuitbreaker.CircuitNonTrippingException;
import org.savapage.core.circuitbreaker.CircuitTrippingException;
import org.savapage.core.config.CircuitBreakerEnum;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.dao.helpers.SQLHelper;
import org.savapage.core.util.IOHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.bytecode.opencsv.CSVWriter;

/**
 * PaperCut database accessor.
 *
 * @author Rijk Ravestein
 *
 */
public abstract class PaperCutDb {

    /**
     * Field identifiers used for select and sort.
     */
    public enum Field {
        /**
         * Transaction date.
         */
        TRX_DATE,

        /**
         * Transaction type.
         */
        TRX_TYPE
    }

    /**
     * Account transaction filter.
     */
    public static class TrxFilter {

        private String username;
        private PaperCutAccountTrxTypeEnum trxType;
        private Date dateFrom;
        private Date dateTo;
        private String containingCommentText;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public PaperCutAccountTrxTypeEnum getTrxType() {
            return trxType;
        }

        public void setTrxType(PaperCutAccountTrxTypeEnum trxType) {
            this.trxType = trxType;
        }

        public Date getDateFrom() {
            return dateFrom;
        }

        public void setDateFrom(Date dateFrom) {
            this.dateFrom = dateFrom;
        }

        public Date getDateTo() {
            return dateTo;
        }

        public void setDateTo(Date dateTo) {
            this.dateTo = dateTo;
        }

        public String getContainingCommentText() {
            return containingCommentText;
        }

        public void setContainingCommentText(String containingCommentText) {
            this.containingCommentText = containingCommentText;
        }

    }

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

    /** */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(PaperCutDb.class);

    /** */
    protected static final CircuitBreaker CIRCUIT_BREAKER = ConfigManager
            .getCircuitBreaker(CircuitBreakerEnum.PAPERCUT_CONNECTION);

    /** */
    private static final int SQL_STRINGBUILDER_CAPACITY = 256;

    /**
     * .
     */
    protected abstract static class PaperCutDbExecutor {

        /**
         * The database accessor.
         */
        private PaperCutDb papercutDb;

        /**
         * The Database connection.
         */
        private Connection connection;

        /**
         * @param proxy
         *            The database proxy.
         * @param conn
         *            The Database connection.
         */
        PaperCutDbExecutor(final PaperCutDb proxy, final Connection conn) {
            this.papercutDb = proxy;
            this.connection = conn;
        }

        /**
         * @return The database accessor.
         */
        public PaperCutDb getPapercutDb() {
            return papercutDb;
        }

        /**
         * @param db
         *            The database accessor.
         */
        public void setPapercutDb(final PaperCutDb db) {
            this.papercutDb = db;
        }

        /**
         * @return The Database connection.
         */
        public Connection getConnection() {
            return connection;
        }

        /**
         * @param conn
         *            The Database connection.
         */
        public void setConnection(final Connection conn) {
            this.connection = conn;
        }

        /**
         * Executes a PaperCut database action.
         *
         * @return The resulting object.
         * @throws PaperCutException
         *             When error.
         */
        public abstract Object execute() throws PaperCutException;

    }

    /**
     * A {@link CircuitBreakerOperation} wrapper for the
     * {@link PaperCutDbExecutor#execute()} method.
     */
    protected static final class PaperCutCircuitBreakerOperation
            implements CircuitBreakerOperation {

        /** */
        private final PaperCutDbExecutor executor;

        /**
         * @param exec
         *            The executor.
         */
        PaperCutCircuitBreakerOperation(final PaperCutDbExecutor exec) {
            this.executor = exec;
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

    /** */
    private final boolean useCircuitBreaker;

    /**
     * E.g. {@code "org.postgresql.Driver"}.
     */
    private final String dbDriver;

    /**
     * E.g. {@code "jdbc:postgresql://localhost/papercut"}.
     */
    private final String dbUrl;
    /** */
    private final String dbUser;
    /** */
    private final String dbPassword;

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
    protected PaperCutDb(final String driver, final String url,
            final String user, final String password,
            final boolean useBreaker) {
        this.dbDriver = driver;
        this.dbUrl = url;
        this.dbUser = user;
        this.dbPassword = password;
        this.useCircuitBreaker = useBreaker;
    }

    /**
     * Creates a {@link PaperCutDbProxy} instance from the application
     * configuration.
     *
     * @param cm
     *            The {@link ConfigManager}.
     * @param useBreaker
     *            If {@code true} a {@link CircuitBreakerOperation} is used.
     */
    protected PaperCutDb(final ConfigManager cm, final boolean useBreaker) {
        this(cm.getConfigValue(Key.PAPERCUT_DB_JDBC_DRIVER),
                cm.getConfigValue(Key.PAPERCUT_DB_JDBC_URL),
                cm.getConfigValue(Key.PAPERCUT_DB_USER),
                cm.getConfigValue(Key.PAPERCUT_DB_PASSWORD), useBreaker);
    }

    public final boolean isUseCircuitBreaker() {
        return useCircuitBreaker;
    }

    public final String getDbDriver() {
        return dbDriver;
    }

    public final String getDbUrl() {
        return dbUrl;
    }

    public final String getDbUser() {
        return dbUser;
    }

    public final String getDbPassword() {
        return dbPassword;
    }

    /**
     * Opens a PaperCut database connection.
     * <p>
     * Note: use {@link #closeConnection(Connection)} to close the connection
     * again.
     * </p>
     *
     * @return A connection to the data source.
     */
    public abstract Connection openConnection();

    /**
     * Closes a database connection.
     *
     * @param conn
     *            A database connection.
     */
    public abstract void closeConnection(Connection conn);

    /**
     * Closes resources while numbing exceptions.
     *
     * @param conn
     *            A {@link Connection} to close.
     */
    protected final void silentClose(final Connection conn) {
        try {
            conn.close();
        } catch (SQLException e) {
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn(e.getMessage());
            }
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
    private static void silentClose(final ResultSet resultset,
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
     *
     * @param filter
     * @return
     */
    public long getAccountTrxCount(final Connection connection,
            final PaperCutDb.TrxFilter filter) {

        final PaperCutDbExecutor exec =
                new PaperCutDbExecutor(this, connection) {

                    @Override
                    public Object execute() throws PaperCutException {
                        try {
                            return PaperCutDb.getAccountTrxCountSQL(connection,
                                    filter);
                        } catch (SQLException e) {
                            throw new PaperCutConnectException(e.getMessage(),
                                    e);
                        }
                    }
                };

        final Object result;

        try {
            if (this.useCircuitBreaker) {
                result = CIRCUIT_BREAKER
                        .execute(new PaperCutCircuitBreakerOperation(exec));
            } else {
                result = exec.execute();
            }
        } catch (PaperCutException e) {
            throw new PaperCutConnectException(e.getMessage(), e);
        } catch (InterruptedException | CircuitBreakerException e) {
            throw new PaperCutConnectException(e.getMessage(), e);
        }

        return ((Long) result).longValue();
    }

    /**
     * Appends WHERE clause.
     *
     * @param sql
     *            SQL to append to.
     * @param filter
     *            The filter.
     */
    private static void appendAccountTrxCountWhere(final StringBuilder sql,
            final PaperCutDb.TrxFilter filter) {

        sql.append("\nWHERE A.account_name = ?");

        if (StringUtils.isNotBlank(filter.getContainingCommentText())) {
            sql.append(" AND X.txn_comment LIKE ?");
        }

        if (filter.getTrxType() != null) {
            sql.append(" AND X.transaction_type = ?");
        }

        if (filter.getDateFrom() != null) {
            sql.append(" AND X.transaction_date >= ?");
        }

        if (filter.getDateTo() != null) {
            sql.append(" AND X.transaction_date <= ?");
        }
    }

    /**
     * Prepares WHERE clause.
     *
     * @param stm
     *            The statement.
     * @param filter
     *            The filter.
     * @throws SQLException
     *             If SQL error.
     */
    private static void prepareAccountTrxCountWhere(final PreparedStatement stm,
            final PaperCutDb.TrxFilter filter) throws SQLException {

        int parameterIndex = 0;

        stm.setString(++parameterIndex, filter.getUsername());

        if (StringUtils.isNotBlank(filter.getContainingCommentText())) {
            stm.setString(++parameterIndex,
                    String.format("%%%s%%", filter.getContainingCommentText()));
        }

        if (filter.getTrxType() != null) {
            stm.setString(++parameterIndex, filter.getTrxType().toString());
        }

        if (filter.getDateFrom() != null) {
            stm.setTimestamp(++parameterIndex,
                    new java.sql.Timestamp(filter.getDateFrom().getTime()));
        }

        if (filter.getDateTo() != null) {
            stm.setTimestamp(++parameterIndex,
                    new java.sql.Timestamp(filter.getDateTo().getTime()));
        }
    }

    /**
     *
     * @param connection
     *            The connection.
     * @param filter
     *            The filter.
     * @return
     * @throws SQLException
     */
    private static Long getAccountTrxCountSQL(final Connection connection,
            final PaperCutDb.TrxFilter filter) throws SQLException {

        PreparedStatement stm = null;

        try {
            final StringBuilder sql =
                    new StringBuilder(SQL_STRINGBUILDER_CAPACITY);

            sql.append("SELECT COUNT(X.account_id)"
                    + "\nFROM tbl_account_transaction X"
                    + "\nLEFT JOIN tbl_account A"
                    + " ON A.account_id = X.account_id");

            appendAccountTrxCountWhere(sql, filter);
            stm = connection.prepareStatement(sql.toString());
            prepareAccountTrxCountWhere(stm, filter);

            final ResultSet result = stm.executeQuery();
            result.next();
            return Long.valueOf(result.getLong(1));

        } finally {
            if (stm != null) {
                /*
                 * When a Statement object is closed, its current ResultSet
                 * object, if one exists, is also closed.
                 */
                stm.close();
            }
        }
    }

    /**
     *
     * @param connection
     * @param filter
     * @param startPosition
     * @param maxResults
     * @param orderBy
     * @param sortAscending
     * @return
     */
    @SuppressWarnings("unchecked")
    public List<PaperCutAccountTrx> getAccountTrxChunk(
            final Connection connection, PaperCutDb.TrxFilter filter,
            Integer startPosition, Integer maxResults, PaperCutDb.Field orderBy,
            boolean sortAscending) {

        final PaperCutDbExecutor exec =
                new PaperCutDbExecutor(this, connection) {

                    @Override
                    public Object execute() throws PaperCutException {
                        try {
                            return PaperCutDb.getAccountTrxChunkSQL(connection,
                                    filter, startPosition, maxResults, orderBy,
                                    sortAscending);

                        } catch (SQLException e) {
                            throw new PaperCutConnectException(e.getMessage(),
                                    e);
                        }
                    }
                };

        final Object result;

        try {
            if (this.useCircuitBreaker) {
                result = CIRCUIT_BREAKER
                        .execute(new PaperCutCircuitBreakerOperation(exec));
            } else {
                result = exec.execute();
            }
        } catch (PaperCutException e) {
            throw new PaperCutConnectException(e.getMessage(), e);
        } catch (InterruptedException | CircuitBreakerException e) {
            throw new PaperCutConnectException(e.getMessage(), e);
        }

        return (List<PaperCutAccountTrx>) result;
    }

    /**
     * Uses PaperCut rounding mode to create a BigDecimal from a amount double.
     *
     * @param amount
     *            The amount. The scale of the return value.
     * @param scale
     *            The scale of the return value.
     * @return The {@link BigDecimal} amount.
     */
    public static BigDecimal getAmountBigBecimal(final Double amount,
            final int scale) {

        return BigDecimal.valueOf(amount.doubleValue()).setScale(scale,
                RoundingMode.UP);
    }

    /**
     *
     * @param connection
     * @param filter
     * @param startPosition
     * @param maxResults
     * @param orderBy
     * @param sortAscending
     * @return
     * @throws SQLException
     */
    private static List<PaperCutAccountTrx> getAccountTrxChunkSQL(
            final Connection connection, PaperCutDb.TrxFilter filter,
            Integer startPosition, Integer maxResults, PaperCutDb.Field orderBy,
            boolean sortAscending) throws SQLException {

        final StringBuilder sql = new StringBuilder(SQL_STRINGBUILDER_CAPACITY);

        sql.append("SELECT X.transaction_date, X.transacted_by, "
                + "X.transaction_type, X.amount, X.balance, X.txn_comment,"
                + "\nL.document_name, L.total_pages, L.total_sheets, "
                + "L.total_color_pages,"
                + "\nL.copies, L.paper_size, L.denied_reason, L.duplex, "
                + "L.gray_scale," + "\nL.printed, L.cancelled, L.refunded"
                + "\nFROM tbl_account_transaction X "
                + "\nLEFT JOIN tbl_account A"
                + " ON A.account_id = X.account_id"
                + "\nLEFT JOIN tbl_printer_usage_log L"
                + " ON L.printer_usage_log_id = X.usage_log_id");

        appendAccountTrxCountWhere(sql, filter);

        sql.append(" ORDER BY ");

        if (orderBy == Field.TRX_DATE) {
            sql.append("X.transaction_date");
        } else if (orderBy == Field.TRX_TYPE) {
            sql.append("X.transaction_type");
        } else {
            throw new SpException("Unhandled ORDER BY field.");
        }

        if (!sortAscending) {
            sql.append(" DESC");
        }

        if (startPosition != null) {
            sql.append(" OFFSET ").append(startPosition);
        }
        if (maxResults != null) {
            sql.append(" LIMIT ").append(maxResults);
        }

        final List<PaperCutAccountTrx> usageLogList = new ArrayList<>();

        PreparedStatement stm = null;
        ResultSet rs = null;

        boolean finished = false;

        try {

            stm = connection.prepareStatement(sql.toString());
            prepareAccountTrxCountWhere(stm, filter);

            rs = stm.executeQuery();

            while (rs.next()) {

                final PaperCutAccountTrx trx = new PaperCutAccountTrx();

                trx.setTransactionDate(new Date(
                        rs.getTimestamp("transaction_date").getTime()));

                trx.setTransactedBy(rs.getString("transacted_by"));
                trx.setDocumentName(rs.getString("document_name"));

                trx.setPrinted(
                        StringUtils.defaultString(rs.getString("printed"))
                                .equalsIgnoreCase("Y"));

                trx.setCancelled(
                        StringUtils.defaultString(rs.getString("cancelled"))
                                .equalsIgnoreCase("Y"));

                trx.setDeniedReason(rs.getString("denied_reason"));

                trx.setAmount(rs.getDouble("amount"));
                trx.setBalance(rs.getDouble("balance"));
                trx.setComment(rs.getString("txn_comment"));
                trx.setTransactionType(rs.getString("transaction_type"));

                //
                usageLogList.add(trx);
            }

            finished = true;

        } finally {

            silentClose(rs, stm);

            if (!finished) {
                LOGGER.error(sql.toString());
            }
        }

        return usageLogList;

    }

    /**
     * Gets the {@link PaperCutPrinterUsageLog} for unique document names.
     *
     * @param connection
     *            The database connection.
     * @param uniqueDocNames
     *            A set with document names.
     * @return A list with an {@link PaperCutPrinterUsageLog} object for each
     *         title in the input set.
     */
    @SuppressWarnings("unchecked")
    public List<PaperCutPrinterUsageLog> getPrinterUsageLog(
            final Connection connection, final Set<String> uniqueDocNames) {

        final PaperCutDbExecutor exec =
                new PaperCutDbExecutor(this, connection) {

                    @Override
                    public Object execute() throws PaperCutException {
                        try {
                            return PaperCutDb.getPrinterUsageLogSQL(
                                    this.getConnection(), uniqueDocNames);
                        } catch (SQLException e) {
                            throw new PaperCutConnectException(e.getMessage(),
                                    e);
                        }
                    }
                };

        final Object result;

        try {
            if (this.useCircuitBreaker) {
                result = CIRCUIT_BREAKER
                        .execute(new PaperCutCircuitBreakerOperation(exec));
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
     * Gets the {@link PaperCutPrinterUsageLog} for unique document names using
     * SQL query.
     *
     * @param connection
     *            The database connection.
     * @param uniqueDocNames
     *            A set with document names.
     * @return A list with an {@link PaperCutPrinterUsageLog} object for each
     *         title in the input set.
     * @throws SQLException
     *             When an SQL error occurs.
     */
    private static List<PaperCutPrinterUsageLog> getPrinterUsageLogSQL(
            final Connection connection, final Set<String> uniqueDocNames)
            throws SQLException {

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

            final String escapedTitle =
                    SQLHelper.escapeForSql(iterUniqueTitle.next());

            sql.append('\'').append(escapedTitle).append('\'');
            nCounter++;
        }

        sql.append(")");

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(sql.toString());
        }

        final List<PaperCutPrinterUsageLog> usageLogList = new ArrayList<>();

        Statement statement = null;
        ResultSet resultset = null;

        boolean finished = false;

        try {

            statement = connection.createStatement();
            resultset = statement.executeQuery(sql.toString());

            while (resultset.next()) {

                final PaperCutPrinterUsageLog usageLog =
                        new PaperCutPrinterUsageLog();

                usageLog.setDocumentName(resultset.getString("document_name"));

                usageLog.setPrinted(
                        resultset.getString("printed").equalsIgnoreCase("Y"));
                usageLog.setCancelled(
                        resultset.getString("cancelled").equalsIgnoreCase("Y"));

                usageLog.setDeniedReason(resultset.getString("denied_reason"));

                usageLog.setUsageCost(resultset.getDouble("usage_cost"));

                usageLog.setAccountIdAssoc(
                        resultset.getLong("assoc_with_account_id"));
                usageLog.setAccountIdCharged(
                        resultset.getLong("charged_to_account_id"));

                //
                usageLogList.add(usageLog);
            }

            finished = true;

        } finally {

            silentClose(resultset, statement);

            if (!finished) {
                LOGGER.error(sql.toString());
            }
        }

        return usageLogList;
    }

    /**
     * Creates a CSV file with Delegator Print costs.
     *
     * @param connection
     *            The database connection.
     * @param file
     *            The CSV file to create.
     * @param dto
     *            The {@link DelegatedPrintPeriodDto}.
     * @throws IOException
     *             When IO errors occur while writing the file.
     */
    public final void getDelegatorPrintCostCsv(final Connection connection,
            final File file, final DelegatedPrintPeriodDto dto)
            throws IOException {

        final PaperCutDbExecutor exec =
                new PaperCutDbExecutor(this, connection) {

                    @Override
                    public Object execute() throws PaperCutException {
                        try {
                            PaperCutDb.getDelegatorPrintCostCsvSQL(
                                    this.getConnection(), file, dto);
                        } catch (SQLException e) {
                            throw new PaperCutConnectException(e.getMessage(),
                                    e);
                        } catch (IOException e) {
                            throw new PaperCutException(e.getMessage(), e);
                        }
                        return null;
                    }
                };

        @SuppressWarnings("unused")
        final Object result;
        try {
            if (this.useCircuitBreaker) {
                result = CIRCUIT_BREAKER
                        .execute(new PaperCutCircuitBreakerOperation(exec));
            } else {
                result = exec.execute();
            }
        } catch (PaperCutException e) {
            throw new PaperCutConnectException(e.getMessage(), e);
        } catch (InterruptedException | CircuitBreakerException e) {
            throw new PaperCutConnectException(e.getMessage(), e);
        }
    }

    /**
     * Creates a CSV file with Delegator Print cost using SQL query.
     *
     * @param connection
     *            The database connection.
     * @param file
     *            The CSV file to create.
     * @param dto
     *            The {@link DelegatedPrintPeriodDto}.
     * @throws IOException
     *             When IO errors occur while writing the file.
     * @throws SQLException
     *             When an SQL error occurs.
     */
    private static void getDelegatorPrintCostCsvSQL(final Connection connection,
            final File file, final DelegatedPrintPeriodDto dto)
            throws IOException, SQLException {

        final StringBuilder sql = new StringBuilder(256);

        sql.append("SELECT");

        sql.append(" ACC.account_name_lower as Userid");
        sql.append(", U.full_name as Name");

        sql.append(", MAX(CASE WHEN " + "length(split_part(TRX.txn_comment, '"
                + PaperCutPrintCommentSyntax.FIELD_SEPARATOR + "', 1)) < 25 "
                + "THEN split_part(TRX.txn_comment, '"
                + PaperCutPrintCommentSyntax.FIELD_SEPARATOR + "', 1) "
                + "ELSE '' END) as Klas");

        sql.append(", -SUM(TRX.amount) as Amount");
        sql.append(", COUNT(Trx.amount) as Transactions");

        //
        final String syntaxVersionTestClause =
                "LENGTH(split_part(TRX.txn_comment, ' | ', 8) ) > 0 ";

        final String calcTotalPagesClause = "CAST(split_part(TRX.txn_comment, "
                + "' | ', 3) AS INT)" + " * CAST(split_part(TRX.txn_comment, "
                + "' | ', 4) AS INT)" + " ELSE 0 END)";

        // Copies.
        sql.append(", SUM(case when ").append(syntaxVersionTestClause)
                .append(" THEN CAST(split_part(TRX.txn_comment, "
                        + "' | ', 3) AS INT) " + "ELSE 0 END) as copies");

        // Pages.
        sql.append(", SUM(case when ").append(syntaxVersionTestClause)
                .append(" THEN ").append(calcTotalPagesClause)
                .append(" as pages");

        // Indicator Totals.
        final String[][] sumColInfo =
                { { "5", "A4", "pages_a4" }, { "5", "A3", "pages_a3" },
                        { "6", PaperCutPrintCommentSyntax.INDICATOR_DUPLEX_OFF,
                                "pages_simplex" },
                        { "6", PaperCutPrintCommentSyntax.INDICATOR_DUPLEX_ON,
                                "pages_duplex" },
                        { "7", PaperCutPrintCommentSyntax.INDICATOR_COLOR_OFF,
                                "pages_bw" },
                        { "7", PaperCutPrintCommentSyntax.INDICATOR_COLOR_ON,
                                "pages_color" } };

        for (final String[] info : sumColInfo) {
            sql.append(", SUM(case when ").append(syntaxVersionTestClause)
                    .append(" AND split_part(TRX.txn_comment, ' | ', ")
                    .append(info[0]).append(") = '").append(info[1])
                    .append("' then ").append(calcTotalPagesClause)
                    .append(" as ").append(info[2]);
        }

        sql.append(", MIN(TRX.transaction_date) as Date_From");
        sql.append(", MAX(TRX.transaction_date) as Date_To");

        //
        sql.append(" FROM tbl_account_transaction as TRX"
                + " LEFT JOIN tbl_account as ACC"
                + " ON ACC.account_id = TRX.account_id"
                + " LEFT JOIN tbl_user_account as UACC"
                + " ON UACC.account_id = ACC.account_id"
                + " LEFT JOIN tbl_user as U" + " ON UACC.user_id = U.user_id");

        sql.append(" WHERE TRX.transaction_type = 'ADJUST'"
                + " and ACC.account_type ");

        if (StringUtils.isBlank(dto.getPersonalAccountType())) {
            sql.append("LIKE 'USER-%'");
        } else {
            sql.append("= '").append(dto.getPersonalAccountType()).append("'");
        }

        final List<String> klassen = dto.getClasses();

        if (klassen != null && !klassen.isEmpty()) {

            sql.append(" and (");
            int i = 0;
            for (final String klas : klassen) {
                if (i > 0) {
                    sql.append(" OR ");
                }
                sql.append("TRX.txn_comment like '")
                        .append(SQLHelper.escapeForSql(klas))
                        .append(PaperCutPrintCommentSyntax.FIELD_SEPARATOR)
                        .append("%'");
                i++;
            }
            sql.append(")");
        }

        if (dto.getTimeFrom() != null) {
            sql.append(" and TRX.transaction_date >= '").append(
                    new java.sql.Date(dto.getTimeFrom().longValue()).toString())
                    .append("'::DATE");
        }

        if (dto.getTimeTo() != null) {
            // next day 0:00
            final Date dateTo = DateUtils.truncate(
                    DateUtils.addDays(new Date(dto.getTimeTo().longValue()), 1),
                    Calendar.DAY_OF_MONTH);

            sql.append(" and TRX.transaction_date < '")
                    .append(new java.sql.Date(dateTo.getTime()))
                    .append("'::DATE");
        }

        sql.append(" GROUP BY ACC.account_name_lower, U.full_name"
                + " ORDER BY ACC.account_name_lower");

        //
        final FileWriter writer = new FileWriter(file);
        final CSVWriter csvWriter = new CSVWriter(writer);

        Statement statement = null;
        ResultSet resultset = null;
        boolean finished = false;

        try {
            statement = connection.createStatement();
            resultset = statement.executeQuery(sql.toString());
            csvWriter.writeAll(resultset, true);
            finished = true;

        } finally {

            IOHelper.closeQuietly(csvWriter);
            IOHelper.closeQuietly(writer);

            silentClose(resultset, statement);

            if (!finished) {
                LOGGER.error(sql.toString());
            }
        }
    }
}
