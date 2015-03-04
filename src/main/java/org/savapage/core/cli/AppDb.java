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
package org.savapage.core.cli;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.RunMode;
import org.savapage.core.dao.DaoContext;
import org.savapage.core.dao.impl.DaoContextImpl;
import org.savapage.core.job.DocLogClean;
import org.savapage.core.jpa.tools.DatabaseTypeEnum;
import org.savapage.core.jpa.tools.DbProcessListener;
import org.savapage.core.jpa.tools.DbTools;
import org.savapage.core.jpa.tools.DbUpgManager;
import org.savapage.core.jpa.tools.DbVersionInfo;
import org.savapage.core.services.ServiceContext;

/**
 * End-user command-line application for database operations.
 *
 * @author Datraverse B.V.
 *
 */
public final class AppDb extends AbstractApp {

    /**
     *
     */
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

        //
        OptionBuilder.hasArg(false);
        OptionBuilder.withLongOpt(CLI_SWITCH_DBINIT);
        OptionBuilder.withDescription("Re-initializes the database even "
                + "if it already exists.");
        options.addOption(OptionBuilder.create());

        //
        OptionBuilder.hasArg(false);
        OptionBuilder.withLongOpt(CLI_SWITCH_DBEXPORT);
        OptionBuilder.withDescription("Exports the database to the default "
                + "backup location.");

        options.addOption(OptionBuilder.create());

        //
        OptionBuilder.hasArg(true);
        OptionBuilder.withArgName("FILE|DIR");
        OptionBuilder.withLongOpt(CLI_OPTION_DBEXPORT_TO);
        OptionBuilder.withDescription("Exports the database to the "
                + "specified file or directory.");
        options.addOption(OptionBuilder.create());

        //
        OptionBuilder.hasArg(true);
        OptionBuilder.withArgName("FILE");
        OptionBuilder.withLongOpt(CLI_OPTION_DBIMPORT);
        OptionBuilder.withDescription("Imports the database from "
                + "the specified file. "
                + "Deletes any existing data before loading the data.");
        options.addOption(OptionBuilder.create());

        //
        OptionBuilder.hasArg(true);
        OptionBuilder.withArgName("DAYS");
        OptionBuilder.withLongOpt(CLI_OPTION_DB_DEL_LOGS);
        OptionBuilder
                .withDescription("Deletes application, account transaction "
                        + "and document log data "
                        + "older than DAYS. A DAYS value of zero (0) will "
                        + "remove all log data from the system.");
        options.addOption(OptionBuilder.create());

        //
        OptionBuilder.hasArg(true);
        OptionBuilder.withArgName("FILE");
        OptionBuilder.withLongOpt(CLI_OPTION_DB_RUN_SQL_SCRIPT);
        OptionBuilder.withDescription("Runs SQL statements from the "
                + "specified script file. "
                + "NOTE: Only if requested by support.");
        options.addOption(OptionBuilder.create());

        //
        OptionBuilder.hasArg(true);
        OptionBuilder.withArgName("STATEMENT");
        OptionBuilder.withLongOpt(CLI_OPTION_DB_RUN_SQL);
        OptionBuilder.withDescription("Runs an SQL statement. "
                + "NOTE: Only if requested by support.");
        options.addOption(OptionBuilder.create());

        //
        return options;
    }

    /**
     *
     * @param file
     * @throws IOException
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
        CommandLineParser parser = new PosixParser();
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
            public void onLogEvent(String message) {
                logOut.println(message);
            }
        };

        try {

            ConfigManager.setServerProps(ConfigManager.readServerProperties());

            if (cmd.hasOption(CLI_SWITCH_DBINIT)
                    || cmd.hasOption(CLI_OPTION_DBIMPORT)) {

                listener.onLogEvent("Starting ...");

                ConfigManager.instance().init(RunMode.CORE,
                        DatabaseTypeEnum.Internal);

            } else {

                ConfigManager.instance().init(RunMode.LIB,
                        DatabaseTypeEnum.Internal);

                DbVersionInfo info =
                        ConfigManager.instance().getDbVersionInfo();

                getDisplayStream().println(
                        "Connecting to " + info.getProdName() + " "
                                + info.getProdVersion());
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

                DbTools.initDb(listener);

            } else if (cmd.hasOption(CLI_OPTION_DB_DEL_LOGS)) {

                int daysBackInTime =
                        Integer.valueOf(cmd
                                .getOptionValue(CLI_OPTION_DB_DEL_LOGS));

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

                displayExportedFilePath(DbTools.exportDb(DaoContextImpl
                        .peekEntityManager()));

            } else if (cmd.hasOption(CLI_OPTION_DBEXPORT_TO)) {

                displayExportedFilePath(DbTools.exportDb(
                        DaoContextImpl.peekEntityManager(),
                        new File(cmd.getOptionValue(CLI_OPTION_DBEXPORT_TO))));

            } else if (cmd.hasOption(CLI_OPTION_DBIMPORT)) {

                DbTools.importDb(
                        new File(cmd.getOptionValue(CLI_OPTION_DBIMPORT)),
                        listener);

            } else if (cmd.hasOption(CLI_OPTION_DB_RUN_SQL_SCRIPT)) {

                final File script =
                        new File(
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

            } else {

                usage(cmdLineSyntax, options);
            }

            ret = EXIT_CODE_OK;

        } catch (org.hibernate.exception.GenericJDBCException ex) {

            getErrorDisplayStream().println(ex.getMessage());

            final SQLException e = ex.getSQLException();

            if (e.getErrorCode() == SQL_ERROR_TRX_ROLLBACK
                    && e.getSQLState().equalsIgnoreCase("XJ040")) {

                getErrorDisplayStream().println(
                        "The database is currently in use. "
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
     * IMPORTANT: MUST return void, use System.exit() to get an exit code on JVM
     * execution.
     *
     * @param args
     */
    public static void main(String[] args) {
        int status = EXIT_CODE_EXCEPTION;
        AppDb app = new AppDb();
        // try {
        try {
            status = app.run(args);
        } catch (Exception e) {
            // e.printStackTrace();
            AppDb.getErrorDisplayStream().println(e.getMessage());
        }
        System.exit(status);
    }

}
