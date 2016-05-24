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

import java.io.IOException;
import java.net.URL;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.savapage.core.config.ConfigManager;

/**
 * <p>
 * This class implements a command-line tool for executing PRIVATE server
 * functions via XML-RPC.
 * </p>
 * <p>
 * It accepts the commands as arguments and outputs the results of the command
 * on the console (standard-out). For security reasons only users with read
 * access to the server.properties (normally only the Administrators group) have
 * rights to execute the commands.
 * </p>
 * <p>
 * See
 * <a href="http://ws.apache.org/xmlrpc/client.html">http://ws.apache.org/xmlrpc
 * /client.html</a>
 * </p>
 *
 * @author Rijk Ravestein
 *
 */
public final class AppPrivate extends AbstractApp {

    /** */
    private static final String CLI_SWITCH_CUPS_SUBS_STOP =
            "cups-subscription-stop";

    /** */
    private static final String CLI_SWITCH_CUPS_SUBS_START =
            "cups-subscription-start";

    private AppPrivate() {

    }

    @Override
    protected final Options createCliOptions() throws Exception {

        Options options = new Options();

        options.addOption(CLI_SWITCH_HELP, CLI_SWITCH_HELP_LONG, false,
                "Displays this help text.");

        options.addOption(Option.builder().hasArg(false)
                .longOpt(CLI_SWITCH_CUPS_SUBS_STOP)
                .desc("Stops the CUPS event subscription.").build());

        options.addOption(Option.builder().hasArg(false)
                .longOpt(CLI_SWITCH_CUPS_SUBS_START)
                .desc("Starts the CUPS event subscription.").build());

        return options;
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
         * Initialize this application.
         */
        init();

        int ret = EXIT_CODE_EXCEPTION;

        try {
            /*
             *
             */
            if (cmd.hasOption(CLI_SWITCH_CUPS_SUBS_START)) {

                XmlRpcClient client = createClient();
                ret = (Integer) client.execute("admin.cupsSubscriptionStart",
                        new Object[] {});

            } else if (cmd.hasOption(CLI_SWITCH_CUPS_SUBS_STOP)) {

                XmlRpcClient client = createClient();
                ret = (Integer) client.execute("admin.cupsSubscriptionStop",
                        new Object[] {});

            } else {

                usage(cmdLineSyntax, options);
            }

            ret = EXIT_CODE_OK;

        } catch (Exception ex) {

            getErrorDisplayStream().println(ex.getMessage());

        } finally {
            ConfigManager.instance().exit();
        }

        return ret;
    }

    /**
     *
     * @return
     * @throws IOException
     * @throws XmlRpcException
     */
    @SuppressWarnings("unused")
    private void test() throws XmlRpcException, IOException {

        XmlRpcClient client = createClient();

        Object[] params =
                new Object[] { Integer.valueOf(33), Integer.valueOf(9) };
        Integer result = (Integer) client.execute("api.add", params);
        getDisplayStream().println("add: " + result + "");
    }

    /**
     *
     * @return
     * @throws IOException
     */
    private XmlRpcClient createClient() throws IOException {

        Properties serverProps = ConfigManager.loadServerProperties();

        XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
        config.setServerURL(new URL("http://localhost:"
                + ConfigManager.getServerPort(serverProps) + "/xmlrpc"));
        XmlRpcClient client = new XmlRpcClient();
        client.setConfig(config);
        return client;
    }

    /**
     */
    @Override
    protected void onInit() throws Exception {
    }

    /**
     * IMPORTANT: MUST return void, use System.exit() to get an exit code on JVM
     * execution.
     *
     * @param args
     */
    public static void main(String[] args) {
        int status = EXIT_CODE_EXCEPTION;
        AppPrivate app = new AppPrivate();
        try {
            status = app.run(args);
        } catch (Exception e) {
            getErrorDisplayStream().println(e.getMessage());
        }
        System.exit(status);
    }
}
