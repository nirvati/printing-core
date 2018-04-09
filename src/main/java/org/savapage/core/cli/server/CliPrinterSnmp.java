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
package org.savapage.core.cli.server;

import org.apache.commons.cli.CommandLine;
import org.savapage.core.json.rpc.AbstractJsonRpcMethodParms;
import org.savapage.core.json.rpc.ErrorDataBasic;
import org.savapage.core.json.rpc.JsonRpcError;
import org.savapage.core.json.rpc.JsonRpcMethodName;
import org.savapage.core.json.rpc.JsonRpcResult;
import org.savapage.core.json.rpc.impl.ParamsPrinterSnmp;
import org.savapage.core.json.rpc.impl.ResultAttribute;
import org.savapage.core.json.rpc.impl.ResultPrinterSnmp;
import org.savapage.core.snmp.SnmpClientSession;
import org.savapage.core.snmp.SnmpVersionEnum;

/**
 *
 * @author Datraverse B.V.
 *
 */
public class CliPrinterSnmp extends AbstractAppApi {

    /**
     *
     */
    private static final String API_VERSION = "0.20";

    /**
     *
     */
    private static final String METHOD_SHORT_DESCRIPT =
            "Reads SNMP info from a printer.";

    /**
     *
     */
    private static final String CLI_OPT_PRINTERNAME = "printername";

    /**
    *
    */
    private static final String CLI_OPT_HOST = "host";

    /**
    *
    */
    private static final String CLI_OPT_PORT = "port";

    /**
    *
    */
    private static final String CLI_OPT_COMMUNITY = "community";

    /**
    *
    */
    private static final String CLI_OPT_VERSION = "version";

    /**
     *
     */
    private static final SnmpVersionEnum SNMP_VERSION_DEFAULT = SnmpVersionEnum.V1;

    /**
     *
     */
    private static Object[][] theOptions =
            new Object[][] {

                    {
                            ARG_TEXT + "(255)",
                            CLI_OPT_PRINTERNAME,
                            "CUPS printer name used to resolve host name "
                                    + "(required when --" + CLI_OPT_HOST
                                    + " is not set).", Boolean.FALSE },

                    {
                            ARG_TEXT,
                            CLI_OPT_HOST,
                            "Host name or IP address of the printer "
                                    + "(required when --" + CLI_OPT_PRINTERNAME
                                    + " is not set).", Boolean.FALSE },

                    {
                            ARG_NUMBER,
                            CLI_OPT_PORT,
                            String.format("SNMP port number (default %d).",
                                    SnmpClientSession.DEFAULT_PORT_READ),
                            Boolean.FALSE },

                    {
                            ARG_TEXT,
                            CLI_OPT_COMMUNITY,
                            String.format("SNMP community (default \"%s\").",
                                    SnmpClientSession.DEFAULT_COMMUNITY),
                            Boolean.FALSE },

                    {
                            SnmpVersionEnum.formattedCmdLineOptions(),
                            CLI_OPT_VERSION,
                            String.format("SNMP version (default \"%s\").",
                                    SNMP_VERSION_DEFAULT.getCmdLineOption()),
                            Boolean.FALSE }, //
            };

    @Override
    protected final String getApiVersion() {
        return API_VERSION;
    }

    @Override
    protected final String getMethodName() {
        return JsonRpcMethodName.PRINTER_SNMP.getMethodName();
    }

    @Override
    protected final Object[][] getOptionDictionary() {
        return theOptions;
    }

    @Override
    protected final String getShortDecription() {
        return METHOD_SHORT_DESCRIPT;
    }

    @Override
    protected final boolean hasBatchOptions() {
        return false;
    }

    @Override
    protected final boolean hasLocaleOption() {
        return false;
    }

    @Override
    protected final boolean isValidCliInput(final CommandLine cmd) {
        /*
         * Printer name or host is required.
         */
        if (!cmd.hasOption(CLI_OPT_PRINTERNAME) && !cmd.hasOption(CLI_OPT_HOST)) {
            return false;
        }

        /*
         * Mutual exclusive switches.
         */
        int count = 0;

        for (final String opt : new String[] { CLI_OPT_PRINTERNAME,
                CLI_OPT_HOST }) {
            if (cmd.hasOption(opt)) {
                count++;
            }
        }

        if (count > 1) {
            return false;
        }

        /*
         * Other.
         */
        if (cmd.hasOption(CLI_OPT_VERSION)
                && SnmpVersionEnum.enumFromCmdLineOption(cmd
                        .getOptionValue(CLI_OPT_VERSION)) == null) {
            return false;
        }

        //
        return true;
    }

    @Override
    protected final AbstractJsonRpcMethodParms createMethodParms(
            final CommandLine cmd) {

        final ParamsPrinterSnmp parms = new ParamsPrinterSnmp();

        parms.setPrinterName(cmd.getOptionValue(CLI_OPT_PRINTERNAME));
        parms.setHost(cmd.getOptionValue(CLI_OPT_HOST));
        parms.setPort(cmd.getOptionValue(CLI_OPT_PORT));
        parms.setCommunity(cmd.getOptionValue(CLI_OPT_COMMUNITY));

        if (cmd.hasOption(CLI_OPT_VERSION)) {
            parms.setVersion(SnmpVersionEnum.enumFromCmdLineOption(cmd
                    .getOptionValue(CLI_OPT_VERSION)));
        } else {
            parms.setVersion(SNMP_VERSION_DEFAULT);
        }

        return parms;
    }

    @Override
    protected final void onErrorResponse(final JsonRpcError error) {

        final ErrorDataBasic data = error.data(ErrorDataBasic.class);

        getErrorDisplayStream().println(
                "Error [" + error.getCode() + "]: " + error.getMessage());

        if (data.getReason() != null) {
            getErrorDisplayStream().println("Reason: " + data.getReason());
        }
    }

    @Override
    protected final boolean onResultResponse(final JsonRpcResult result) {

        final ResultPrinterSnmp printerSnmp =
                result.data(ResultPrinterSnmp.class);

        for (ResultAttribute attr : printerSnmp.getAttributes()) {
            getDisplayStream().println(
                    String.format("%-25s: %s", attr.getKey(), attr.getValue()));
        }

        return false;
    }

    @Override
    protected final boolean isSwitchOption(final String optionName) {
        return false;
    }

    @Override
    protected void onInit() throws Exception {
        // no code intended
    }

}
