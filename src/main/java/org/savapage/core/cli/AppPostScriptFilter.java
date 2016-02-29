/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2016 Datraverse B.V.
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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.savapage.core.print.server.PostScriptFilter;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class AppPostScriptFilter extends AbstractApp {

    private static final String CLI_SWITCH_NEGLECT_DRM = "neglect-drm";
    private static final String CLI_SWITCH_RESPECT_DRM = "respect-drm";
    private static final String CLI_OPTION_IN = "in";
    private static final String CLI_OPTION_OUT = "out";

    private static final int RETURN_DRM_FAILURE = 1;
    private static final int RETURN_DRM_NO = 10;
    private static final int RETURN_DRM_YES = 20;
    private static final int RETURN_DRM_NEGLECTED = 30;

    /**
     * Streams the lines from the PostScript reader to the writer.
     *
     * @param reader
     *            The PostScript reader.
     * @param writer
     *            The PostScript writer.
     * @param fRespectDRM
     *            If {@code false}, any DRM signature is omitted from the
     *            stream. If {@code true}, the function immediately returns
     *            {@link EXIT_FAILURE} when a DRM signature is encountered.
     * @return The process exit code.
     * @throws IOException
     */
    private int process(BufferedReader reader, BufferedWriter writer,
            boolean fRespectDRM) throws IOException {

        int ret = RETURN_DRM_FAILURE;

        switch (PostScriptFilter.process(reader, writer, fRespectDRM)) {
        case DRM_NEGLECTED:
            ret = RETURN_DRM_NEGLECTED;
            break;
        case DRM_NO:
            ret = RETURN_DRM_NO;
            break;
        case DRM_YES:
            ret = RETURN_DRM_YES;
            break;
        default:
            break;
        }

        return ret;
    }

    @Override
    protected int run(String[] args) throws Exception {

        final String cmdLineSyntax = "[OPTION] <file>";

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

        /*
         * Initialize this application.
         */
        init();

        int ret = RETURN_DRM_FAILURE;

        if (args.length == 0) {
            return ret;
        }

        boolean fRespectDRM = cmd.hasOption(CLI_SWITCH_RESPECT_DRM);
        boolean fNeglectDRM = cmd.hasOption(CLI_SWITCH_NEGLECT_DRM);

        if (fRespectDRM || fNeglectDRM) {

            int retDrm = RETURN_DRM_FAILURE;

            BufferedReader reader = null;
            BufferedWriter writer = null;

            if (cmd.hasOption(CLI_OPTION_IN)) {

                File file = new File(cmd.getOptionValue(CLI_OPTION_IN));
                if (file.exists()) {
                    reader = new BufferedReader(new FileReader(file));
                }

            } else {
                reader = new BufferedReader(new InputStreamReader(System.in));
            }

            if (cmd.hasOption(CLI_OPTION_OUT)) {

                File file = new File(cmd.getOptionValue(CLI_OPTION_OUT));
                writer = new BufferedWriter(new FileWriter(file));

            } else {
                writer = new BufferedWriter(
                        new OutputStreamWriter(getDisplayStream()));
            }

            if (reader != null && writer != null) {
                retDrm = process(reader, writer, fRespectDRM);
            }

            /*
             * Write retDrm as reason code to stderr.
             */
            System.err.println(retDrm);

            /*
             * Map retDrm to 'regular' success/failure
             */
            if (retDrm == RETURN_DRM_YES) {
                ret = RETURN_DRM_FAILURE;
            } else {
                ret = retDrm;
            }
        }

        return ret;
    }

    @Override
    protected Options createCliOptions() throws Exception {
        final Options options = new Options();

        options.addOption(CLI_SWITCH_HELP, CLI_SWITCH_HELP_LONG, false,
                "Displays this help text.");

        options.addOption(
                Option.builder().hasArg(false).longOpt(CLI_SWITCH_NEGLECT_DRM)
                        .desc("Neglects the DRM.").build());

        options.addOption(
                Option.builder().hasArg(false).longOpt(CLI_SWITCH_RESPECT_DRM)
                        .desc("Respects the DRM.").build());

        options.addOption(Option.builder().hasArg(true).argName("FILE")
                .longOpt(CLI_OPTION_IN)
                .desc("Input PostScript File (default stdin)").build());

        options.addOption(Option.builder().hasArg(true).argName("FILE")
                .longOpt(CLI_OPTION_OUT)
                .desc("Output PostScript File (default stdout)").build());

        return options;
    }

    @Override
    protected void onInit() throws Exception {
        // no code intended
    }

    /**
     *
     * @param args
     */
    public static void main(String[] args) {
        int status = EXIT_CODE_EXCEPTION;
        AbstractApp app = new AppPostScriptFilter();
        try {
            status = app.run(args);
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
        System.exit(status);
    }

}
