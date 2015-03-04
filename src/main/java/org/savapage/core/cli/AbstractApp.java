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

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.SimpleLayout;
import org.savapage.core.services.ServiceEntryPoint;

/**
 * Command Line Interface (CLI) Application.
 *
 * @author Datraverse B.V.
 *
 */
public abstract class AbstractApp implements ServiceEntryPoint {

    /**
     * Width (number of characters) of the usage text block.
     */
    private static final int HELP_FORMATTER_WIDTH = 85;

    /** */
    protected static final String CLI_SWITCH_HELP = "h";
    /** */
    protected static final String CLI_SWITCH_HELP_LONG = "help";

    // ........................................................
    // Exit codes
    // ........................................................
    /** */
    public static final int EXIT_CODE_OK = 0;
    /** */
    public static final int EXIT_CODE_ERROR = 1;
    /** */
    public static final int EXIT_CODE_MISSING_PARMS = 2;
    /** */
    public static final int EXIT_CODE_PARMS_PARSE_ERROR = 3;
    /** */
    public static final int EXIT_CODE_ERROR_AFTER_BATCH_CONTINUE = 5;
    /** */
    public static final int EXIT_CODE_EXCEPTION = 9;

    /**
     * Runs the app.
     *
     * @param args
     *            The commandline arguments.
     * @return The return code.
     * @throws Exception
     *             When something goes wrong.
     */
    protected abstract int run(final String[] args) throws Exception;

    /**
     *
     * @return The options.
     * @throws Exception
     *             When something goes wrong.
     */
    protected abstract Options createCliOptions() throws Exception;

    /**
     * Notifies the initialization event, so a custom initialization can be
     * implemented by concrete classes.
     *
     * @throws Exception
     *             When something goes wrong.
     */
    protected abstract void onInit() throws Exception;

    /**
     * Shows usage and options of program on stdout.
     * <p>
     * Note: the option ordering is ascending alphabetic.
     * </p>
     *
     * @param cmdLineSyntax
     *            The syntax for this application.
     * @param options
     *            The command line options.
     *
     */
    protected final void
            usage(final String cmdLineSyntax, final Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(cmdLineSyntax, options);
    }

    /**
     *
     * @return
     */
    public static final PrintStream getDisplayStream() {
        return System.out;
    }

    /**
     *
     * @return
     */
    public static final PrintStream getErrorDisplayStream() {
        return System.err;
    }

    /**
     * Shows usage and options of program on stdout.
     * <p>
     * Note: the option ordering is ascending alphabetic.
     * </p>
     *
     * @param cmdLineSyntax
     *            The syntax for this application.
     * @param descript
     *            Description of the application.
     * @param options
     *            The command line options.
     */
    protected final void usage(final String cmdLineSyntax,
            final String descript, final Options options) {
        HelpFormatter formatter = new HelpFormatter();
        getDisplayStream().println(descript);
        getDisplayStream().println();
        formatter.printHelp(cmdLineSyntax + System.lineSeparator(), options);
    }

    /**
     * Shows usage and <i>long</i> options of program on stdout.
     * <p>
     * Note: the option ordering is according to the order of optionNames.
     * </p>
     *
     * @param cmdLineSyntax
     *            The syntax for this application.
     * @param descript
     *            Description of the application.
     * @param options
     *            The command line options.
     * @param optionNames
     *            The <i>long</i> option names in the order in which they should
     *            appear in the usage help.
     */
    protected final void usage(final String cmdLineSyntax,
            final String descript, final Options options,
            final List<String> optionNames) {

        HelpFormatter formatter = new HelpFormatter();

        formatter.setWidth(HELP_FORMATTER_WIDTH);

        formatter.setOptionComparator(new Comparator<Option>() {

            @Override
            public int compare(final Option o1, final Option o2) {

                int i1 = optionNames.indexOf(o1.getLongOpt());
                int i2 = optionNames.indexOf(o2.getLongOpt());

                if (i1 < 0) {
                    i1 = optionNames.indexOf(o1.getOpt());
                }
                if (i2 < 0) {
                    i2 = optionNames.indexOf(o2.getOpt());
                }
                return i1 - i2;
            }
        });

        getDisplayStream().println(descript);
        getDisplayStream().println();
        formatter.printHelp(cmdLineSyntax + System.lineSeparator(), options);
    }

    /**
     * Initialized the application so it can be run.
     * <ul>
     * <li>Sets up a simple log4j configuration that logs on the console.</li>
     * <li>Sends the {@link #onInit()} event.</li>
     * </ul>
     *
     * @throws Exception
     *             When something goes wrong.
     */
    protected final void init() throws Exception {

        /*
         * Set up a simple log4j configuration that logs on the console.
         */
        ConsoleAppender appender = new ConsoleAppender();
        appender.setThreshold(Level.ERROR);
        appender.setName("myAppender");
        appender.setLayout(new SimpleLayout());
        Writer writer = new PrintWriter(getDisplayStream());
        appender.setWriter(writer);
        BasicConfigurator.configure(appender);

        onInit();
    }

}
