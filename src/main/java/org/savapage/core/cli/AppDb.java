/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2019 Datraverse B.V.
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
package org.savapage.core.cli;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.RunModeEnum;
import org.savapage.core.dao.DaoContext;
import org.savapage.core.dao.impl.DaoContextImpl;
import org.savapage.core.job.DocLogClean;
import org.savapage.core.jpa.tools.DatabaseTypeEnum;
import org.savapage.core.jpa.tools.DbProcessListener;
import org.savapage.core.jpa.tools.DbTools;
import org.savapage.core.jpa.tools.DbUpgManager;
import org.savapage.core.jpa.tools.DbVersionInfo;
import org.savapage.core.json.rpc.AbstractJsonRpcMethodResponse;
import org.savapage.core.json.rpc.ErrorDataBasic;
import org.savapage.core.json.rpc.ResultString;
import org.savapage.core.json.rpc.impl.ParamsNameValue;
import org.savapage.core.services.ConfigPropertyService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.impl.ConfigPropertyServiceImpl;

/**
 * End-user command-line application for database operations.
 *
 * @author Rijk Ravestein
 *
 */
public final class AppDb extends AbstractApp {

    /** */
    private static final String NOTE_AS_REQUESTED_BY_SUPPORT =
            "NOTE: Only perform as requested by SavaPage Support.";

    /** */
    private static final int SQL_ERROR_TRX_ROLLBACK = 40000;

    /** */
    private static final String CLI_SWITCH_DBINIT = "db-init";
    /** */
    private static final String CLI_OPTION_DBIMPORT = "db-import";
    /** */
    private static final String CLI_SWITCH_DBEXPORT = "db-export";
    /** */
    private static final String CLI_OPTION_DBEXPORT_TO = "db-export-to";
    /** */
    private static final String CLI_OPTION_DB_DEL_LOGS = "db-delete-logs";

    /** */
    private static final String CLI_OPTION_DB_RUN_SQL_SCRIPT = "db-run-script";
    /** */
    private static final String CLI_OPTION_DB_RUN_SQL = "db-run-sql";

    /** */
    private static final String CLI_OPTION_CONFIG_PROP_GET = "db-config-get";
    /** */
    private static final String CLI_OPTION_CONFIG_PROP_SET = "db-config-set";

    /** */
    private static final String CLI_SWITCH_DBCHECK = "db-check";
    /** */
    private static final String CLI_SWITCH_DBCHECK_FIX = "db-check-fix";

    /**
     * The number of rows in the result set for export.
     */
    private static final int QUERY_MAX_RESULTS = 1000;

    /**
     *
     */
    private AppDb() {
        super();
    }

    @Override
    protected Options createCliOptions() throws Exception {

        final Options options = new Options();

        //
        options.addOption(CLI_SWITCH_HELP, CLI_SWITCH_HELP_LONG, false,
                "Displays this help text.");

        options.addOption(
                Option.builder().hasArg(false).longOpt(CLI_SWITCH_DBINIT)
                        .desc("Re-initializes the database even "
                                + "if it already exists.")
                        .build());

        options.addOption(
                Option.builder().hasArg(false).longOpt(CLI_SWITCH_DBEXPORT)
                        .desc("Exports the database to the "
                                + "default backup location.")
                        .build());

        options.addOption(Option.builder().hasArg(true).argName("FILE|DIR")
                .longOpt(CLI_OPTION_DBEXPORT_TO)
                .desc("Exports the database to the "
                        + "specified file or directory.")
                .build());

        options.addOption(Option.builder().hasArg(true).argName("FILE")
                .longOpt(CLI_OPTION_DBIMPORT)
                .desc("Imports the database from " + "the specified file. "
                        + "Deletes any existing data before loading the data.")
                .build());

        options.addOption(Option.builder().hasArg(true).argName("DAYS")
                .longOpt(CLI_OPTION_DB_DEL_LOGS)
                .desc("Deletes application, account transaction "
                        + "and document log data "
                        + "older than DAYS. A DAYS value of zero (0) will "
                        + "remove all log data from the system.")
                .build());

        options.addOption(Option.builder().hasArg(true).argName("FILE")
                .longOpt(CLI_OPTION_DB_RUN_SQL_SCRIPT)
                .desc("Runs SQL statements from the "
                        + "specified script file. "
                        + NOTE_AS_REQUESTED_BY_SUPPORT)
                .build());

        options.addOption(Option.builder().hasArg(true).argName("STATEMENT")
                .longOpt(CLI_OPTION_DB_RUN_SQL)
                .desc("Runs an SQL statement. " + NOTE_AS_REQUESTED_BY_SUPPORT)
                .build());

        options.addOption(Option.builder().hasArg(true).argName("NAME")
                .longOpt(CLI_OPTION_CONFIG_PROP_GET)
                .desc("Gets configuration property value. "
                        + SENTENCE_ADV_OPTION)
                .build());

        options.addOption(Option.builder().hasArg(false)
                .longOpt(CLI_SWITCH_DBCHECK)
                .desc("Checks database integrity. " + SENTENCE_ADV_OPTION)
                .build());

        options.addOption(
                Option.builder().hasArg(false).longOpt(CLI_SWITCH_DBCHECK_FIX)
                        .desc("Checks and fixes database integrity. "
                                + NOTE_AS_REQUESTED_BY_SUPPORT)
                        .build());

        final Option opt = Option.builder().hasArg(true).argName("NAME=VALUE")
                .longOpt(CLI_OPTION_CONFIG_PROP_SET)
                .desc("Sets configuration property value. "
                        + SENTENCE_ADV_OPTION)
                .build();
        opt.setArgs(2);
        opt.setValueSeparator('=');

        options.addOption(opt);

        return options;
    }

    /**
     *
     * @param file
     *            The file.
     * @throws IOException
     *             When file path error.
     */
    private static void displayExportedFilePath(final File file)
            throws IOException {
        getDisplayStream().println(
                "Database exported to ...\n" + file.getCanonicalPath());
    }

    @Override
    protected int run(final String[] args) throws Exception {

        final String cmdLineSyntax = "[OPTION]";

        // ......................................................
        // Parse parameters from CLI
        // ......................................................
        Options options = createCliOptions();
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        } catch (org.apache.commons.cli.ParseException e) {
            getDisplayStream().println(e.getMessage());
            usage(cmdLineSyntax, options);
            return EXIT_CODE_PARMS_PARSE_ERROR;
        }

        // ......................................................
        // Help needed?
        // ......................................................
        if (args.length == 0 || cmd.hasOption(CLI_SWITCH_HELP)
                || cmd.hasOption(CLI_SWITCH_HELP_LONG)) {
            usage(cmdLineSyntax, options);
            return EXIT_CODE_OK;
        }

        // ...................................................................
        // Hardcoded default parameter values
        // ...................................................................

        /*
         * Init this app
         */
        init();

        int ret = EXIT_CODE_EXCEPTION;

        final PrintStream logOut = getDisplayStream();
        final DbProcessListener listener = new DbProcessListener() {

            @Override
            public void onLogEvent(final String message) {
                logOut.println(message);
            }
        };

        try {

            ConfigManager.setServerProps(ConfigManager.loadServerProperties());

            if (cmd.hasOption(CLI_SWITCH_DBINIT)
                    || cmd.hasOption(CLI_OPTION_DBIMPORT)) {

                listener.onLogEvent("Starting ...");

                ConfigManager.instance().init(RunModeEnum.CORE,
                        DatabaseTypeEnum.Internal);

            } else {

                ConfigManager.instance().init(RunModeEnum.LIB,
                        DatabaseTypeEnum.Internal);

                DbVersionInfo info =
                        ConfigManager.instance().getDbVersionInfo();

                if (!cmd.hasOption(CLI_OPTION_CONFIG_PROP_GET)
                        && !cmd.hasOption(CLI_OPTION_CONFIG_PROP_SET)) {
                    getDisplayStream().println("Connecting to "
                            + info.getProdName() + " " + info.getProdVersion());
                }
            }

            /*
             *
             */
            ServiceContext.open();
            final DaoContext daoContext = ServiceContext.getDaoContext();

            /*
             *
             */
            if (cmd.hasOption(CLI_SWITCH_DBINIT)) {

                DbTools.initDb(listener, false);

            } else if (cmd.hasOption(CLI_OPTION_DB_DEL_LOGS)) {

                int daysBackInTime = Integer
                        .valueOf(cmd.getOptionValue(CLI_OPTION_DB_DEL_LOGS));

                /*
                 * Application log.
                 */
                daoContext.beginTransaction();

                boolean rollback = true;

                try {

                    daoContext.getAppLogDao().clean(daysBackInTime);
                    rollback = false;

                } finally {

                    if (rollback) {
                        daoContext.rollback();
                    } else {
                        daoContext.commit();
                    }
                }

                /*
                 * Document log
                 */
                DocLogClean.clean(daysBackInTime);

            } else if (cmd.hasOption(CLI_SWITCH_DBEXPORT)) {

                displayExportedFilePath(DbTools.exportDb(
                        DaoContextImpl.peekEntityManager(), QUERY_MAX_RESULTS));

            } else if (cmd.hasOption(CLI_OPTION_DBEXPORT_TO)) {

                displayExportedFilePath(DbTools.exportDb(
                        DaoContextImpl.peekEntityManager(), QUERY_MAX_RESULTS,
                        new File(cmd.getOptionValue(CLI_OPTION_DBEXPORT_TO))));

            } else if (cmd.hasOption(CLI_OPTION_DBIMPORT)) {

                DbTools.importDb(
                        new File(cmd.getOptionValue(CLI_OPTION_DBIMPORT)),
                        listener);

            } else if (cmd.hasOption(CLI_SWITCH_DBCHECK)) {

                DbTools.checkSequences(getDisplayStream(),
                        getErrorDisplayStream(), false);

            } else if (cmd.hasOption(CLI_SWITCH_DBCHECK_FIX)) {

                DbTools.checkSequences(getDisplayStream(),
                        getErrorDisplayStream(), true);

            } else if (cmd.hasOption(CLI_OPTION_DB_RUN_SQL_SCRIPT)) {

                final File script = new File(
                        cmd.getOptionValue(CLI_OPTION_DB_RUN_SQL_SCRIPT));

                daoContext.beginTransaction();
                boolean rollback = true;
                try {
                    DbUpgManager.runSqlScript(
                            DaoContextImpl.peekEntityManager(), script);
                    rollback = false;
                } finally {
                    if (rollback) {
                        daoContext.rollback();
                    } else {
                        daoContext.commit();
                    }
                }

            } else if (cmd.hasOption(CLI_OPTION_DB_RUN_SQL)) {

                final String statement =
                        cmd.getOptionValue(CLI_OPTION_DB_RUN_SQL);

                daoContext.beginTransaction();
                boolean rollback = true;
                try {
                    DbUpgManager.runSqlStatement(
                            DaoContextImpl.peekEntityManager(), statement);
                    rollback = false;
                } finally {
                    if (rollback) {
                        daoContext.rollback();
                    } else {
                        daoContext.commit();
                    }
                }

            } else if (cmd.hasOption(CLI_OPTION_CONFIG_PROP_GET)) {

                getConfigProperty(
                        cmd.getOptionValue(CLI_OPTION_CONFIG_PROP_GET));

            } else if (cmd.hasOption(CLI_OPTION_CONFIG_PROP_SET)) {

                final String[] argsWlk =
                        cmd.getOptionValues(CLI_OPTION_CONFIG_PROP_SET);

                daoContext.beginTransaction();
                boolean rollback = true;
                try {
                    setConfigProperty(argsWlk[0], argsWlk[1]);
                    rollback = false;
                } finally {
                    if (rollback) {
                        daoContext.rollback();
                    } else {
                        daoContext.commit();
                    }
                }

            } else {

                usage(cmdLineSyntax, options);
            }

            ret = EXIT_CODE_OK;

        } catch (org.hibernate.exception.GenericJDBCException ex) {

            getErrorDisplayStream().println(ex.getMessage());

            final SQLException e = ex.getSQLException();

            if (e.getErrorCode() == SQL_ERROR_TRX_ROLLBACK
                    && e.getSQLState().equalsIgnoreCase("XJ040")) {

                getErrorDisplayStream()
                        .println("The database is currently in use. "
                                + "Shutdown the Application Server "
                                + "and try again.");
            }

        } catch (Exception ex) {

            getErrorDisplayStream().println(ex.getMessage());

        } finally {
            ServiceContext.close();
        }

        return ret;
    }

    /**
     * Initialize as basic library. See
     * {@link ConfigManager#initAsBasicLibrary(Properties)}.
     */
    @Override
    protected void onInit() throws Exception {
        // no code intended
    }

    /**
     *
     * @param name
     *            The config property name.
     * @throws IllegalArgumentException
     *             If property is unknown.
     */
    private static void getConfigProperty(final String name) {
        final ConfigPropertyService svc = new ConfigPropertyServiceImpl();
        final AbstractJsonRpcMethodResponse rsp = svc.getPropertyValue(name);
        if (rsp.isError()) {
            throw new IllegalArgumentException(rsp.asError().getError()
                    .data(ErrorDataBasic.class).getReason());
        } else {
            getDisplayStream().println(rsp.asResult().getResult()
                    .data(ResultString.class).getValue());
        }
    }

    /**
     *
     * @param name
     *            The config property name.
     * @param value
     *            The config property value.
     * @throws IllegalArgumentException
     *             If property is unknown, or cannot be accessed for update.
     */
    private static void setConfigProperty(final String name,
            final String value) {
        final ConfigPropertyService svc = new ConfigPropertyServiceImpl();

        final ParamsNameValue parm = new ParamsNameValue();
        parm.setName(name);
        parm.setValue(value);

        final AbstractJsonRpcMethodResponse rsp = svc.setPropertyValue(parm);
        if (rsp.isError()) {
            throw new IllegalArgumentException(rsp.asError().getError()
                    .data(ErrorDataBasic.class).getReason());
        }
    }

    /**
     * IMPORTANT: MUST return void, use System.exit() to get an exit code on JVM
     * execution.
     *
     * @param args
     */
    public static void main(String[] args) {
        int status = EXIT_CODE_EXCEPTION;
        AppDb app = new AppDb();
        try {
            status = app.run(args);
        } catch (Exception e) {
            AppDb.getErrorDisplayStream().println(e.getMessage());
        }
        System.exit(status);
    }

}
